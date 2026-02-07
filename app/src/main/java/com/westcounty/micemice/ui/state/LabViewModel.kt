package com.westcounty.micemice.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.westcounty.micemice.data.model.AnimalSex
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.LabTask
import com.westcounty.micemice.data.model.NotificationItem
import com.westcounty.micemice.data.model.NotificationPolicy
import com.westcounty.micemice.data.model.NotificationSeverity
import com.westcounty.micemice.data.model.PermissionKey
import com.westcounty.micemice.data.model.RepoResult
import com.westcounty.micemice.data.model.SampleType
import com.westcounty.micemice.data.model.TaskFilter
import com.westcounty.micemice.data.model.TaskPriority
import com.westcounty.micemice.data.model.TaskStatus
import com.westcounty.micemice.data.model.TaskEscalationConfig
import com.westcounty.micemice.data.model.TrainingRecord
import com.westcounty.micemice.data.model.UserRole
import com.westcounty.micemice.data.model.overdueDurationMillis
import com.westcounty.micemice.data.repository.LabRepository
import com.westcounty.micemice.di.RepositoryProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LabViewModel(
    private val repository: LabRepository,
) : ViewModel() {

    val snapshot: StateFlow<LabSnapshot> = repository.snapshot
        .map { state ->
            val now = System.currentTimeMillis()
            val config = state.taskEscalationConfig
            state.copy(
                tasks = state.tasks.map { task ->
                    val overdueStatus = if (task.status == TaskStatus.Todo && task.dueAtMillis < now) {
                        TaskStatus.Overdue
                    } else {
                        task.status
                    }
                    val overdueMillis = (now - task.dueAtMillis).coerceAtLeast(0L)
                    val escalatedPriority = when {
                        overdueStatus == TaskStatus.Overdue &&
                            config.enable48hEscalation &&
                            overdueMillis >= 2 * OVERDUE_ESCALATION_WINDOW -> moreUrgent(task.priority, config.priorityAt48h)
                        overdueStatus == TaskStatus.Overdue &&
                            config.enable24hEscalation &&
                            overdueMillis >= OVERDUE_ESCALATION_WINDOW -> moreUrgent(task.priority, config.priorityAt24h)
                        else -> task.priority
                    }
                    task.copy(
                        status = overdueStatus,
                        priority = escalatedPriority,
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.snapshot.value,
        )

    private val _session = MutableStateFlow(LoggedOutSession)
    val session: StateFlow<SessionState> = _session

    private val _exportPreview = MutableStateFlow("")
    val exportPreview: StateFlow<String> = _exportPreview

    private val _notificationReadAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    val notifications: StateFlow<List<NotificationItem>> = combine(
        snapshot,
        _notificationReadAt,
    ) { state, readMarks ->
        buildNotifications(state, readMarks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { items -> items.count { it.readAtMillis == null } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private val _canUndoWeaning = MutableStateFlow(false)
    val canUndoWeaning: StateFlow<Boolean> = _canUndoWeaning

    private var lastWeaningUndo: WeaningUndoOperation? = null

    fun login(username: String, password: String, orgCode: String, role: UserRole) {
        if (username.isBlank()) {
            sendMessage("请输入账号")
            return
        }
        if (password.length < 4) {
            sendMessage("密码长度至少 4 位")
            return
        }
        if (orgCode.isBlank()) {
            sendMessage("请输入组织编码")
            return
        }

        val now = System.currentTimeMillis()
        _session.value = SessionState(
            isLoggedIn = true,
            username = username.trim(),
            orgCode = orgCode.trim(),
            role = role,
            loginAtMillis = now,
            lastActionAtMillis = now,
        )
        switchRole(role)
        sendMessage("欢迎，$username")
    }

    fun logout() {
        _session.value = LoggedOutSession
        lastWeaningUndo = null
        _canUndoWeaning.value = false
        _notificationReadAt.value = emptyMap()
        sendMessage("已退出登录")
    }

    fun checkSessionTimeout(nowMillis: Long = System.currentTimeMillis()) {
        val current = _session.value
        if (!current.isLoggedIn) return
        if (nowMillis - current.lastActionAtMillis <= SESSION_TIMEOUT_MILLIS) return
        _session.value = LoggedOutSession
        lastWeaningUndo = null
        _canUndoWeaning.value = false
        _notificationReadAt.value = emptyMap()
        sendMessage("会话超时，请重新登录")
    }

    fun switchRole(role: UserRole) {
        _session.update { current -> if (current.isLoggedIn) current.copy(role = role) else current }
        handleResult(repository.switchRole(role), success = "当前角色：${role.displayName}")
    }

    fun completeTask(taskId: String) = handleResult(
        withPermission(PermissionKey.TaskComplete) { repository.completeTask(taskId, actorName()) },
        "任务已完成",
    )

    fun saveTaskTemplate(
        name: String,
        detail: String,
        defaultPriority: TaskPriority,
        dueInHours: Int,
        entityType: String,
    ) = handleResult(
        withPermission(PermissionKey.TaskManage) {
            repository.saveTaskTemplate(name, detail, defaultPriority, dueInHours, entityType, actorName())
        },
        "任务模板已保存",
    )

    fun deleteTaskTemplate(templateId: String) = handleResult(
        withPermission(PermissionKey.TaskManage) { repository.deleteTaskTemplate(templateId, actorName()) },
        "任务模板已删除",
    )

    fun createTaskFromTemplate(templateId: String, assignee: String?) = handleResult(
        withPermission(PermissionKey.TaskManage) { repository.createTaskFromTemplate(templateId, assignee, actorName()) },
        "模板任务已创建",
    )

    fun completeTasks(taskIds: List<String>) {
        if (taskIds.isEmpty()) {
            sendMessage("没有可批量完成的任务")
            return
        }
        if (!hasPermission(PermissionKey.TaskComplete)) {
            sendMessage("缺少权限: ${PermissionKey.TaskComplete.displayName}")
            return
        }
        var success = 0
        var failure = 0
        taskIds.distinct().forEach { id ->
            when (repository.completeTask(id, actorName())) {
                RepoResult.Success -> success += 1
                is RepoResult.Error -> failure += 1
            }
        }
        sendMessage("批量处理完成：成功 $success，失败 $failure")
    }

    fun reassignTask(taskId: String, assignee: String) = handleResult(
        withPermission(PermissionKey.TaskManage) { repository.reassignTask(taskId, assignee, actorName()) },
        "任务已重新指派",
    )

    fun reassignTasks(taskIds: List<String>, assignee: String) = handleResult(
        withPermission(PermissionKey.TaskManage) { repository.reassignTasks(taskIds, assignee, actorName()) },
        "批量指派完成",
    )

    fun saveTaskEscalationConfig(
        enable24hEscalation: Boolean,
        enable48hEscalation: Boolean,
        priorityAt24h: TaskPriority,
        priorityAt48h: TaskPriority,
        autoAssignOverdueTo: String?,
    ) = handleResult(
        withPermission(PermissionKey.TaskManage) {
            repository.setTaskEscalationConfig(
                TaskEscalationConfig(
                    enable24hEscalation = enable24hEscalation,
                    enable48hEscalation = enable48hEscalation,
                    priorityAt24h = priorityAt24h,
                    priorityAt48h = priorityAt48h,
                    autoAssignOverdueTo = autoAssignOverdueTo?.trim()?.ifBlank { null },
                ),
                actorName(),
            )
        },
        "升级规则已保存",
    )

    fun applyTaskEscalation() = handleResult(
        withPermission(PermissionKey.TaskManage) { repository.applyTaskEscalation(actorName()) },
        "已应用升级规则",
    )

    fun moveAnimals(animalIds: List<String>, targetCageId: String) = handleResult(
        withPermission(PermissionKey.MoveAnimal) { repository.moveAnimals(animalIds, targetCageId, actorName()) },
        "转笼成功",
    )

    fun mergeCages(sourceCageId: String, targetCageId: String) = handleResult(
        withPermission(PermissionKey.MoveAnimal) { repository.mergeCages(sourceCageId, targetCageId, actorName()) },
        "并笼成功",
    )

    fun splitCage(
        sourceCageId: String,
        newCageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        animalIds: List<String>,
    ) = handleResult(
        withPermission(PermissionKey.MoveAnimal) {
            repository.splitCage(
                sourceCageId = sourceCageId,
                newCageId = newCageId,
                roomCode = roomCode,
                rackCode = rackCode,
                slotCode = slotCode,
                capacityLimit = capacityLimit,
                animalIds = animalIds,
                operator = actorName(),
            )
        },
        "拆笼成功",
    )

    fun createCage(cageId: String, roomCode: String, rackCode: String, slotCode: String, capacityLimit: Int) = handleResult(
        withPermission(PermissionKey.CreateResources) {
            repository.createCage(cageId, roomCode, rackCode, slotCode, capacityLimit, actorName())
        },
        "笼位已创建",
    )

    fun createAnimal(
        identifier: String,
        sex: AnimalSex,
        strain: String,
        genotype: String,
        cageId: String,
        protocolId: String?,
    ) = handleResult(
        withPermission(PermissionKey.CreateResources) {
            repository.createAnimal(identifier, sex, strain, genotype, cageId, protocolId, actorName())
        },
        "个体已创建",
    )

    fun addStrainToCatalog(strain: String) = handleResult(
        withPermission(PermissionKey.MasterDataEdit) { repository.addStrainToCatalog(strain, actorName()) },
        "品系字典已更新",
    )

    fun removeStrainFromCatalog(strain: String) = handleResult(
        withPermission(PermissionKey.MasterDataEdit) { repository.removeStrainFromCatalog(strain, actorName()) },
        "品系字典已更新",
    )

    fun addGenotypeToCatalog(genotype: String) = handleResult(
        withPermission(PermissionKey.MasterDataEdit) { repository.addGenotypeToCatalog(genotype, actorName()) },
        "基因型模板已更新",
    )

    fun removeGenotypeFromCatalog(genotype: String) = handleResult(
        withPermission(PermissionKey.MasterDataEdit) { repository.removeGenotypeFromCatalog(genotype, actorName()) },
        "基因型模板已更新",
    )

    fun createBreedingPlan(maleId: String, femaleId: String, protocolId: String?, notes: String) = handleResult(
        withPermission(PermissionKey.BreedingWrite) {
            repository.createBreedingPlan(maleId, femaleId, protocolId, actorName(), notes)
        },
        "配种计划已创建",
    )

    fun recordPlugCheck(planId: String, positive: Boolean) = handleResult(
        withPermission(PermissionKey.BreedingWrite) { repository.recordPlugCheck(planId, positive, actorName()) },
        "查栓结果已记录",
    )

    fun recordBirth(planId: String, pupCount: Int, strain: String?, genotype: String?) = handleResult(
        withPermission(PermissionKey.BreedingWrite) { repository.recordBirth(planId, pupCount, strain, genotype, actorName()) },
        "产仔登记成功",
    )

    fun completeWeaning(planId: String) = handleResult(
        withPermission(PermissionKey.BreedingWrite) { repository.completeWeaning(planId, actorName()) },
        "断奶流程已完成",
    )

    fun runWeaningWizard(planId: String, animalIds: List<String>, targetCageIds: List<String>) {
        if (!hasPermission(PermissionKey.BreedingWrite) || !hasPermission(PermissionKey.MoveAnimal)) {
            sendMessage("缺少权限: ${PermissionKey.BreedingWrite.displayName} / ${PermissionKey.MoveAnimal.displayName}")
            return
        }
        if (animalIds.isEmpty()) {
            sendMessage("请至少选择 1 只幼鼠")
            return
        }
        val state = snapshot.value
        val targets = targetCageIds.distinct()
        if (targets.isEmpty()) {
            sendMessage("请至少选择 1 个目标笼位")
            return
        }

        val selectedAnimals = state.animals.filter { it.id in animalIds.toSet() }
        if (selectedAnimals.size != animalIds.toSet().size) {
            sendMessage("部分幼鼠不存在，请重新选择")
            return
        }

        val assignment = recommendWeaningAssignments(
            animalIds = animalIds.distinct(),
            targetCageIds = targets,
            state = state,
        )
        if (assignment == null) {
            sendMessage("目标笼位容量不足，无法自动分笼")
            return
        }

        val originalCages = selectedAnimals.associate { it.id to it.cageId }
        val moved = mutableSetOf<String>()
        assignment.forEach { (targetCageId, ids) ->
            val result = repository.moveAnimals(ids, targetCageId, actorName())
            if (result is RepoResult.Error) {
                rollbackMoves(moved, originalCages)
                sendMessage(result.message)
                return
            }
            moved.addAll(ids)
        }

        when (val wean = repository.completeWeaning(planId, actorName())) {
            RepoResult.Success -> {
                lastWeaningUndo = WeaningUndoOperation(planId = planId, originalCageByAnimal = originalCages)
                _canUndoWeaning.value = true
                sendMessage("断奶分笼完成，已支持撤销")
            }
            is RepoResult.Error -> {
                rollbackMoves(moved, originalCages)
                sendMessage(wean.message)
            }
        }
    }

    fun undoLastWeaningWizard() {
        if (!hasPermission(PermissionKey.BreedingWrite) || !hasPermission(PermissionKey.MoveAnimal)) {
            sendMessage("缺少权限: ${PermissionKey.BreedingWrite.displayName} / ${PermissionKey.MoveAnimal.displayName}")
            return
        }
        val undo = lastWeaningUndo
        if (undo == null) {
            sendMessage("没有可撤销的断奶操作")
            return
        }

        val movedAnimals = undo.originalCageByAnimal.keys
        rollbackMoves(movedAnimals, undo.originalCageByAnimal)
        val reopen = repository.reopenWeaning(undo.planId, actorName())
        if (reopen is RepoResult.Error) {
            sendMessage(reopen.message)
            return
        }
        lastWeaningUndo = null
        _canUndoWeaning.value = false
        sendMessage("已撤销最近一次断奶分笼")
    }

    fun registerSample(animalId: String, sampleType: SampleType) = handleResult(
        withPermission(PermissionKey.GenotypingWrite) { repository.registerSample(animalId, sampleType, actorName()) },
        "采样登记成功",
    )

    fun createGenotypingBatch(name: String, sampleIds: List<String>) = handleResult(
        withPermission(PermissionKey.GenotypingWrite) { repository.createGenotypingBatch(name, sampleIds, actorName()) },
        "分型批次已创建",
    )

    fun importGenotypingResults(batchId: String, csvText: String, reviewer: String) = handleResult(
        withPermission(PermissionKey.GenotypingWrite) {
            repository.importGenotypingResults(batchId, csvText, reviewer, actorName())
        },
        "分型结果导入成功",
    )

    fun confirmGenotypingResult(resultId: String) = handleResult(
        withPermission(PermissionKey.GenotypingWrite) { repository.confirmGenotypingResult(resultId, actorName()) },
        "冲突结果已确认",
    )

    fun createCohort(
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        blindCodingEnabled: Boolean,
        blindCodePrefix: String?,
    ) = handleResult(
        withPermission(PermissionKey.CohortWrite) {
            repository.createCohort(
                name = name,
                strain = strain,
                genotype = genotype,
                sex = sex,
                minWeeks = minWeeks,
                maxWeeks = maxWeeks,
                blindCodingEnabled = blindCodingEnabled,
                blindCodePrefix = blindCodePrefix,
                operator = actorName(),
            )
        },
        "Cohort 已创建",
    )

    fun saveCohortTemplate(
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
    ) = handleResult(
        withPermission(PermissionKey.CohortWrite) {
            repository.saveCohortTemplate(name, strain, genotype, sex, minWeeks, maxWeeks, actorName())
        },
        "模板已保存",
    )

    fun applyCohortTemplate(templateId: String) = handleResult(
        withPermission(PermissionKey.CohortWrite) { repository.applyCohortTemplate(templateId, actorName()) },
        "模板已应用",
    )

    fun updateCohortTemplate(
        templateId: String,
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
    ) = handleResult(
        withPermission(PermissionKey.CohortWrite) {
            repository.updateCohortTemplate(templateId, name, strain, genotype, sex, minWeeks, maxWeeks, actorName())
        },
        "模板已更新",
    )

    fun deleteCohortTemplate(templateId: String) = handleResult(
        withPermission(PermissionKey.CohortWrite) { repository.deleteCohortTemplate(templateId, actorName()) },
        "模板已删除",
    )

    fun createExperiment(cohortId: String, title: String) = handleResult(
        withPermission(PermissionKey.ExperimentWrite) { repository.createExperiment(cohortId, title, actorName()) },
        "实验已创建",
    )

    fun addExperimentEvent(experimentId: String, eventType: String, note: String) = handleResult(
        withPermission(PermissionKey.ExperimentWrite) {
            repository.addExperimentEvent(experimentId, eventType, note, actorName())
        },
        "实验事件已记录",
    )

    fun archiveExperiment(experimentId: String) = handleResult(
        withPermission(PermissionKey.ExperimentWrite) { repository.archiveExperiment(experimentId, actorName()) },
        "实验已归档",
    )

    fun importAnimalsCsv(csvText: String) = handleResult(
        withPermission(PermissionKey.ImportExportManage) { repository.importAnimalsCsv(csvText, actorName()) },
        "CSV 导入成功",
    )

    fun exportAnimalsCsv() {
        if (!hasPermission(PermissionKey.ImportExportManage)) {
            sendMessage("当前角色无权限执行此操作")
            return
        }
        _exportPreview.value = repository.exportAnimalsCsv()
        sendMessage("已生成个体 CSV")
    }

    fun exportComplianceCsv() {
        if (!hasPermission(PermissionKey.ImportExportManage)) {
            sendMessage("当前角色无权限执行此操作")
            return
        }
        _exportPreview.value = repository.exportComplianceCsv()
        sendMessage("已生成合规导出 CSV")
    }

    fun exportCohortBlindCsv(cohortId: String) {
        val csv = repository.exportCohortBlindCsv(cohortId)
        if (csv.isBlank()) {
            sendMessage("该 Cohort 暂无盲法编码可导出")
            return
        }
        _exportPreview.value = csv
        sendMessage("已生成盲法编码 CSV")
    }

    fun saveNotificationPolicy(
        enableProtocolExpiry: Boolean,
        protocolExpiryLeadDays: Int,
        enableOverdueTask: Boolean,
        enableCageCapacity: Boolean,
        enableSyncFailure: Boolean,
    ) = handleResult(
        withPermission(PermissionKey.NotificationPolicyManage) {
            repository.setNotificationPolicy(
                NotificationPolicy(
                    enableProtocolExpiry = enableProtocolExpiry,
                    protocolExpiryLeadDays = protocolExpiryLeadDays,
                    enableOverdueTask = enableOverdueTask,
                    enableCageCapacity = enableCageCapacity,
                    enableSyncFailure = enableSyncFailure,
                ),
                actorName(),
            )
        },
        "通知策略已保存",
    )

    fun markNotificationRead(notificationId: String) {
        if (notificationId.isBlank()) return
        _notificationReadAt.update { current ->
            if (notificationId in current) current else current + (notificationId to System.currentTimeMillis())
        }
    }

    fun markAllNotificationsRead() {
        val now = System.currentTimeMillis()
        val pending = notifications.value.filter { it.readAtMillis == null }.map { it.id }
        if (pending.isEmpty()) return
        _notificationReadAt.update { current ->
            current.toMutableMap().apply {
                pending.forEach { id -> this[id] = now }
            }
        }
    }

    fun retrySyncEvent(syncEventId: String) = handleResult(
        withPermission(PermissionKey.SyncManage) { repository.retrySyncEvent(syncEventId, actorName()) },
        "同步事件已重试",
    )

    fun syncPendingEvents() = handleResult(
        withPermission(PermissionKey.SyncManage) { repository.syncPendingEvents(actorName()) },
        "同步完成",
    )

    fun updateAnimalStatus(animalId: String, status: AnimalStatus) = handleResult(
        withPermission(PermissionKey.UpdateAnimalStatus) { repository.updateAnimalStatus(animalId, status, actorName()) },
        "个体状态已更新",
    )

    fun updateAnimalStatusBatch(animalIds: List<String>, status: AnimalStatus) {
        if (animalIds.isEmpty()) {
            sendMessage("请先选择个体")
            return
        }
        var success = 0
        var failure = 0
        animalIds.distinct().forEach { id ->
            when (repository.updateAnimalStatus(id, status, actorName())) {
                RepoResult.Success -> success += 1
                is RepoResult.Error -> failure += 1
            }
        }
        sendMessage("批量状态更新完成：成功 $success，失败 $failure")
    }

    fun addAnimalEvent(animalId: String, eventType: String, note: String, weightGram: Float?) = handleResult(
        withPermission(PermissionKey.WriteAnimalEvent) {
            repository.addAnimalEvent(animalId, eventType, note, weightGram, actorName())
        },
        "个体事件已记录",
    )

    fun addAnimalAttachment(animalId: String, label: String, filePath: String) = handleResult(
        withPermission(PermissionKey.WriteAnimalAttachment) {
            repository.addAnimalAttachment(animalId, label, filePath, actorName())
        },
        "附件已添加",
    )

    fun setProtocolState(protocolId: String, active: Boolean) = handleResult(
        withPermission(PermissionKey.ProtocolManage) { repository.setProtocolState(protocolId, active, actorName()) },
        if (active) "协议已启用" else "协议已停用",
    )

    fun upsertTrainingRecord(username: String, expiresInDays: Int, active: Boolean, note: String) = handleResult(
        withPermission(PermissionKey.TrainingManage) {
            if (expiresInDays !in 1..3650) {
                RepoResult.Error("培训有效期天数需在 1-3650")
            } else {
                repository.upsertTrainingRecord(
                    TrainingRecord(
                        username = username.trim(),
                        expiresAtMillis = System.currentTimeMillis() + expiresInDays * ONE_DAY,
                        isActive = active,
                        note = note.trim(),
                    ),
                    actorName(),
                )
            }
        },
        "培训资质已更新",
    )

    fun removeTrainingRecord(username: String) = handleResult(
        withPermission(PermissionKey.TrainingManage) { repository.removeTrainingRecord(username, actorName()) },
        "培训资质已删除",
    )

    fun setRolePermission(role: UserRole, permission: PermissionKey, enabled: Boolean) = handleResult(
        withPermission(PermissionKey.RbacManage) {
            repository.setRolePermission(role, permission, enabled, actorName())
        },
        "权限矩阵已更新",
    )

    fun filterTasks(tasks: List<LabTask>, filter: TaskFilter): List<LabTask> {
        val now = System.currentTimeMillis()
        return tasks
            .filter { task ->
                when (filter) {
                    TaskFilter.All -> true
                    TaskFilter.Pending -> task.status == TaskStatus.Todo || task.status == TaskStatus.Overdue
                    TaskFilter.Overdue -> task.status == TaskStatus.Overdue
                    TaskFilter.Overdue24h -> task.overdueDurationMillis(now) >= ONE_DAY
                    TaskFilter.Overdue48h -> task.overdueDurationMillis(now) >= 2 * ONE_DAY
                    TaskFilter.Done -> task.status == TaskStatus.Done
                    TaskFilter.Today -> {
                        val start = now - (now % ONE_DAY)
                        task.dueAtMillis in start..(start + ONE_DAY)
                    }
                }
            }
            .sortedWith(compareBy<LabTask>({ it.priority.rank }, { it.dueAtMillis }))
    }

    private fun actorName(): String = if (_session.value.isLoggedIn) _session.value.username else snapshot.value.currentRole.displayName

    private fun handleResult(result: RepoResult, success: String) {
        touchSession()
        when (result) {
            RepoResult.Success -> sendMessage(success)
            is RepoResult.Error -> sendMessage(result.message)
        }
    }

    private fun sendMessage(message: String) {
        touchSession()
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    private fun touchSession() {
        _session.update { current ->
            if (!current.isLoggedIn) current else current.copy(lastActionAtMillis = System.currentTimeMillis())
        }
    }

    private fun hasPermission(permission: PermissionKey): Boolean {
        val role = _session.value.role
        return snapshot.value.rolePermissionOverrides.isGranted(role, permission)
    }

    private inline fun withPermission(permission: PermissionKey, block: () -> RepoResult): RepoResult {
        return if (hasPermission(permission)) {
            block()
        } else {
            RepoResult.Error("缺少权限: ${permission.displayName}")
        }
    }

    private fun requireMinRole(required: UserRole): Boolean {
        val currentRole = _session.value.role
        return roleLevel(currentRole) >= roleLevel(required)
    }

    private fun roleLevel(role: UserRole): Int = when (role) {
        UserRole.Researcher -> 0
        UserRole.PrincipalInvestigator -> 1
        UserRole.Admin -> 2
    }

    private fun moreUrgent(current: TaskPriority, target: TaskPriority): TaskPriority {
        return if (target.rank < current.rank) target else current
    }

    private fun recommendWeaningAssignments(
        animalIds: List<String>,
        targetCageIds: List<String>,
        state: LabSnapshot,
    ): Map<String, List<String>>? {
        val targetCages = targetCageIds
            .distinct()
            .mapNotNull { id -> state.cages.firstOrNull { it.id == id } }
        if (targetCages.isEmpty()) return null

        val remaining = targetCages.associate { cage ->
            val free = (cage.capacityLimit - cage.animalIds.size).coerceAtLeast(0)
            cage.id to free
        }.toMutableMap()

        val assignment = linkedMapOf<String, MutableList<String>>()
        animalIds.forEach { animalId ->
            val target = remaining.entries
                .filter { it.value > 0 }
                .maxByOrNull { it.value }
                ?.key ?: return null
            assignment.getOrPut(target) { mutableListOf() }.add(animalId)
            remaining[target] = (remaining[target] ?: 0) - 1
        }
        return assignment
    }

    private fun rollbackMoves(movedAnimals: Iterable<String>, originalCageByAnimal: Map<String, String>) {
        val groups = movedAnimals
            .mapNotNull { animalId -> originalCageByAnimal[animalId]?.let { cage -> cage to animalId } }
            .groupBy({ it.first }, { it.second })
        groups.forEach { (sourceCageId, animalIds) ->
            repository.moveAnimals(animalIds, sourceCageId, actorName())
        }
    }

    private fun buildNotifications(
        state: LabSnapshot,
        readMarks: Map<String, Long>,
    ): List<NotificationItem> {
        val now = System.currentTimeMillis()
        val items = mutableListOf<NotificationItem>()
        val policy = state.notificationPolicy
        val readAt = readMarks

        if (policy.enableProtocolExpiry) {
            val leadMillis = policy.protocolExpiryLeadDays * ONE_DAY
            state.protocols
                .filter { it.isActive }
                .forEach { protocol ->
                    val remaining = protocol.expiresAtMillis - now
                    val shouldNotify = remaining <= leadMillis
                    if (!shouldNotify) return@forEach
                    val isExpired = remaining < 0L
                    val id = "protocol:${protocol.id}"
                    items += NotificationItem(
                        id = id,
                        title = if (isExpired) "协议已过期" else "协议即将到期",
                        content = if (isExpired) {
                            "${protocol.id} 已过期，请立即处理"
                        } else {
                            "${protocol.id} 将在 ${(remaining / ONE_DAY).coerceAtLeast(0)} 天内到期"
                        },
                        severity = if (isExpired) NotificationSeverity.Critical else NotificationSeverity.High,
                        createdAtMillis = now,
                        entityType = "protocol",
                        entityId = protocol.id,
                        readAtMillis = readAt[id],
                    )
                }
        }

        if (policy.enableOverdueTask) {
            state.tasks
                .filter { it.status == TaskStatus.Overdue || (it.status == TaskStatus.Todo && it.dueAtMillis < now) }
                .sortedByDescending { now - it.dueAtMillis }
                .take(20)
                .forEach { task ->
                    val overdueHours = ((now - task.dueAtMillis) / (60L * 60L * 1000L)).coerceAtLeast(0L)
                    val id = "task:${task.id}"
                    items += NotificationItem(
                        id = id,
                        title = "任务逾期提醒",
                        content = "${task.title} 已逾期 ${overdueHours}h",
                        severity = when {
                            overdueHours >= 48L -> NotificationSeverity.Critical
                            overdueHours >= 24L -> NotificationSeverity.High
                            else -> NotificationSeverity.Medium
                        },
                        createdAtMillis = now,
                        entityType = "task",
                        entityId = task.id,
                        readAtMillis = readAt[id],
                    )
                }
        }

        if (policy.enableCageCapacity) {
            state.cages
                .filter { it.status == com.westcounty.micemice.data.model.CageStatus.Active }
                .forEach { cage ->
                    val over = cage.animalIds.size - cage.capacityLimit
                    val near = cage.occupancyRatio >= 0.9f
                    if (over <= 0 && !near) return@forEach
                    val id = "cage:${cage.id}"
                    items += NotificationItem(
                        id = id,
                        title = if (over > 0) "笼位超容量" else "笼位接近容量上限",
                        content = "${cage.id} 当前 ${cage.animalIds.size}/${cage.capacityLimit}",
                        severity = if (over > 0) NotificationSeverity.Critical else NotificationSeverity.Medium,
                        createdAtMillis = now,
                        entityType = "cage",
                        entityId = cage.id,
                        readAtMillis = readAt[id],
                    )
                }
        }

        if (policy.enableSyncFailure) {
            state.syncEvents
                .filter { it.status == com.westcounty.micemice.data.model.SyncStatus.Failed }
                .take(20)
                .forEach { event ->
                    val id = "sync:${event.id}"
                    items += NotificationItem(
                        id = id,
                        title = "同步失败",
                        content = "${event.eventType} 失败，已重试 ${event.retryCount} 次",
                        severity = NotificationSeverity.High,
                        createdAtMillis = event.createdAtMillis,
                        entityType = "sync",
                        entityId = event.id,
                        readAtMillis = readAt[id],
                    )
                }
        }

        return items.sortedByDescending { it.createdAtMillis }
    }

    companion object {
        private const val ONE_DAY = 24L * 60L * 60L * 1000L
        private const val OVERDUE_ESCALATION_WINDOW = ONE_DAY
        private const val SESSION_TIMEOUT_MILLIS = 30L * 60L * 1000L

        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LabViewModel(RepositoryProvider.get()) as T
            }
        }
    }
}

private data class WeaningUndoOperation(
    val planId: String,
    val originalCageByAnimal: Map<String, String>,
)
