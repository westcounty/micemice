package com.westcounty.micemice.data.repository

import com.westcounty.micemice.data.model.Animal
import com.westcounty.micemice.data.model.AnimalEvent
import com.westcounty.micemice.data.model.AnimalSex
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.AuditEvent
import com.westcounty.micemice.data.model.BatchStatus
import com.westcounty.micemice.data.model.BreedingPlan
import com.westcounty.micemice.data.model.Cage
import com.westcounty.micemice.data.model.CageStatus
import com.westcounty.micemice.data.model.Cohort
import com.westcounty.micemice.data.model.CohortTemplate
import com.westcounty.micemice.data.model.ExperimentEvent
import com.westcounty.micemice.data.model.ExperimentSession
import com.westcounty.micemice.data.model.ExperimentStatus
import com.westcounty.micemice.data.model.GenotypingBatch
import com.westcounty.micemice.data.model.GenotypingImportIssue
import com.westcounty.micemice.data.model.GenotypingImportReport
import com.westcounty.micemice.data.model.GenotypingResult
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.LabTask
import com.westcounty.micemice.data.model.NotificationPolicy
import com.westcounty.micemice.data.model.PermissionKey
import com.westcounty.micemice.data.model.Protocol
import com.westcounty.micemice.data.model.RepoResult
import com.westcounty.micemice.data.model.Sample
import com.westcounty.micemice.data.model.SampleType
import com.westcounty.micemice.data.model.SyncEvent
import com.westcounty.micemice.data.model.SyncStatus
import com.westcounty.micemice.data.model.TaskPriority
import com.westcounty.micemice.data.model.TaskTemplate
import com.westcounty.micemice.data.model.TrainingRecord
import com.westcounty.micemice.data.model.TaskStatus
import com.westcounty.micemice.data.model.TaskEscalationConfig
import com.westcounty.micemice.data.model.UserRole
import com.westcounty.micemice.data.model.ageInWeeks
import com.westcounty.micemice.data.model.canTransitTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

interface LabRepository {
    val snapshot: StateFlow<LabSnapshot>

    fun switchRole(role: UserRole): RepoResult
    fun completeTask(taskId: String, operator: String): RepoResult
    fun saveTaskTemplate(
        name: String,
        detail: String,
        defaultPriority: TaskPriority,
        dueInHours: Int,
        entityType: String,
        operator: String,
    ): RepoResult
    fun deleteTaskTemplate(templateId: String, operator: String): RepoResult
    fun createTaskFromTemplate(templateId: String, assignee: String?, operator: String): RepoResult
    fun reassignTask(taskId: String, assignee: String, operator: String): RepoResult
    fun reassignTasks(taskIds: List<String>, assignee: String, operator: String): RepoResult
    fun setTaskEscalationConfig(config: TaskEscalationConfig, operator: String): RepoResult
    fun applyTaskEscalation(operator: String): RepoResult
    fun moveAnimals(animalIds: List<String>, targetCageId: String, operator: String): RepoResult
    fun mergeCages(sourceCageId: String, targetCageId: String, operator: String): RepoResult
    fun splitCage(
        sourceCageId: String,
        newCageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        animalIds: List<String>,
        operator: String,
    ): RepoResult

    fun createCage(
        cageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        operator: String,
    ): RepoResult

    fun createAnimal(
        identifier: String,
        sex: AnimalSex,
        strain: String,
        genotype: String,
        cageId: String,
        protocolId: String?,
        operator: String,
    ): RepoResult

    fun addStrainToCatalog(strain: String, operator: String): RepoResult
    fun removeStrainFromCatalog(strain: String, operator: String): RepoResult
    fun addGenotypeToCatalog(genotype: String, operator: String): RepoResult
    fun removeGenotypeFromCatalog(genotype: String, operator: String): RepoResult

    fun createBreedingPlan(
        maleId: String,
        femaleId: String,
        protocolId: String?,
        operator: String,
        notes: String = "",
    ): RepoResult
    fun recordBirth(planId: String, pupCount: Int, strain: String?, genotype: String?, operator: String): RepoResult
    fun recordPlugCheck(planId: String, positive: Boolean, operator: String): RepoResult
    fun completeWeaning(planId: String, operator: String): RepoResult
    fun reopenWeaning(planId: String, operator: String): RepoResult

    fun registerSample(animalId: String, sampleType: SampleType, operator: String): RepoResult
    fun createGenotypingBatch(name: String, sampleIds: List<String>, operator: String): RepoResult
    fun importGenotypingResults(batchId: String, csvText: String, reviewer: String, operator: String): RepoResult
    fun confirmGenotypingResult(resultId: String, operator: String): RepoResult

    fun createCohort(
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        blindCodingEnabled: Boolean = false,
        blindCodePrefix: String? = null,
        operator: String,
    ): RepoResult
    fun saveCohortTemplate(
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        operator: String,
    ): RepoResult
    fun updateCohortTemplate(
        templateId: String,
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        operator: String,
    ): RepoResult
    fun deleteCohortTemplate(templateId: String, operator: String): RepoResult
    fun applyCohortTemplate(templateId: String, operator: String): RepoResult

    fun createExperiment(cohortId: String, title: String, operator: String): RepoResult
    fun addExperimentEvent(experimentId: String, eventType: String, note: String, operator: String): RepoResult
    fun archiveExperiment(experimentId: String, operator: String): RepoResult

    fun importAnimalsCsv(csvText: String, operator: String): RepoResult
    fun exportAnimalsCsv(): String
    fun exportComplianceCsv(): String
    fun exportCohortBlindCsv(cohortId: String): String

    fun retrySyncEvent(syncEventId: String, operator: String): RepoResult
    fun syncPendingEvents(operator: String): RepoResult
    fun updateAnimalStatus(animalId: String, status: AnimalStatus, operator: String): RepoResult
    fun addAnimalEvent(animalId: String, eventType: String, note: String, weightGram: Float?, operator: String): RepoResult
    fun addAnimalAttachment(animalId: String, label: String, filePath: String, operator: String): RepoResult
    fun setProtocolState(protocolId: String, isActive: Boolean, operator: String): RepoResult
    fun upsertTrainingRecord(record: TrainingRecord, operator: String): RepoResult
    fun removeTrainingRecord(username: String, operator: String): RepoResult
    fun setNotificationPolicy(policy: NotificationPolicy, operator: String): RepoResult
    fun setRolePermission(role: UserRole, permission: PermissionKey, enabled: Boolean, operator: String): RepoResult
}

class InMemoryLabRepository private constructor(
    initialSnapshot: LabSnapshot? = null,
) : LabRepository {

    private val idSeed = AtomicInteger(3000)
    private val _snapshot = MutableStateFlow(initialSnapshot ?: seedSnapshot())

    override val snapshot: StateFlow<LabSnapshot> = _snapshot.asStateFlow()

    override fun switchRole(role: UserRole): RepoResult {
        _snapshot.update { it.copy(currentRole = role) }
        appendAudit(
            action = "ROLE_SWITCH",
            entityType = "user",
            entityId = "current",
            summary = "当前角色切换为 ${role.displayName}",
            operator = "system",
        )
        return RepoResult.Success
    }

    override fun completeTask(taskId: String, operator: String): RepoResult {
        val state = _snapshot.value
        val target = state.tasks.firstOrNull { it.id == taskId }
            ?: return RepoResult.Error("任务不存在")

        if (target.status == TaskStatus.Done) {
            return RepoResult.Success
        }

        val now = System.currentTimeMillis()
        _snapshot.update { current ->
            current.copy(
                tasks = current.tasks.map { task ->
                    if (task.id == taskId) task.copy(status = TaskStatus.Done, completedAtMillis = now) else task
                }
            )
        }

        appendAudit(
            action = "TASK_COMPLETE",
            entityType = "task",
            entityId = taskId,
            summary = "完成任务: ${target.title}",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.complete", payloadSummary = "task=$taskId")
        return RepoResult.Success
    }

    override fun saveTaskTemplate(
        name: String,
        detail: String,
        defaultPriority: TaskPriority,
        dueInHours: Int,
        entityType: String,
        operator: String,
    ): RepoResult {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return RepoResult.Error("模板名称不能为空")
        if (dueInHours !in 1..24 * 30) return RepoResult.Error("到期小时需在 1-720")
        if (entityType.trim().isBlank()) return RepoResult.Error("实体类型不能为空")
        if (_snapshot.value.taskTemplates.any { it.name.equals(cleanName, ignoreCase = true) }) {
            return RepoResult.Error("模板名称已存在")
        }

        val template = TaskTemplate(
            id = "TTM-${idSeed.incrementAndGet()}",
            name = cleanName,
            detail = detail.trim().ifBlank { "$cleanName 执行项" },
            defaultPriority = defaultPriority,
            dueInHours = dueInHours,
            entityType = entityType.trim(),
        )
        _snapshot.update { current -> current.copy(taskTemplates = listOf(template) + current.taskTemplates) }
        appendAudit(
            action = "TASK_TEMPLATE_SAVE",
            entityType = "task_template",
            entityId = template.id,
            summary = "创建任务模板 ${template.name}",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.template.save", payloadSummary = "template=${template.id}")
        return RepoResult.Success
    }

    override fun deleteTaskTemplate(templateId: String, operator: String): RepoResult {
        val template = _snapshot.value.taskTemplates.firstOrNull { it.id == templateId } ?: return RepoResult.Error("模板不存在")
        _snapshot.update { current ->
            current.copy(taskTemplates = current.taskTemplates.filterNot { it.id == templateId })
        }
        appendAudit(
            action = "TASK_TEMPLATE_DELETE",
            entityType = "task_template",
            entityId = templateId,
            summary = "删除任务模板 ${template.name}",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.template.delete", payloadSummary = "template=$templateId")
        return RepoResult.Success
    }

    override fun createTaskFromTemplate(templateId: String, assignee: String?, operator: String): RepoResult {
        val template = _snapshot.value.taskTemplates.firstOrNull { it.id == templateId } ?: return RepoResult.Error("模板不存在")
        val now = System.currentTimeMillis()
        val task = LabTask(
            id = "TSK-${idSeed.incrementAndGet()}",
            title = template.name,
            detail = template.detail,
            dueAtMillis = now + template.dueInHours * 60L * 60L * 1000L,
            priority = template.defaultPriority,
            status = TaskStatus.Todo,
            entityType = template.entityType,
            entityId = "template:$templateId",
            assignee = assignee?.trim()?.ifBlank { "未指派" } ?: "未指派",
        )
        _snapshot.update { current -> current.copy(tasks = listOf(task) + current.tasks) }
        appendAudit(
            action = "TASK_CREATE_FROM_TEMPLATE",
            entityType = "task",
            entityId = task.id,
            summary = "由模板 ${template.name} 创建任务",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.template.apply", payloadSummary = "template=$templateId task=${task.id}")
        return RepoResult.Success
    }

    override fun reassignTask(taskId: String, assignee: String, operator: String): RepoResult {
        val cleanAssignee = assignee.trim()
        if (cleanAssignee.isBlank()) return RepoResult.Error("指派人不能为空")
        val target = _snapshot.value.tasks.firstOrNull { it.id == taskId } ?: return RepoResult.Error("任务不存在")
        if (target.assignee == cleanAssignee) return RepoResult.Success

        _snapshot.update { current ->
            current.copy(
                tasks = current.tasks.map { task ->
                    if (task.id == taskId) task.copy(assignee = cleanAssignee) else task
                },
            )
        }
        appendAudit(
            action = "TASK_REASSIGN",
            entityType = "task",
            entityId = taskId,
            summary = "任务指派给 $cleanAssignee",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.reassign", payloadSummary = "task=$taskId assignee=$cleanAssignee")
        return RepoResult.Success
    }

    override fun reassignTasks(taskIds: List<String>, assignee: String, operator: String): RepoResult {
        val ids = taskIds.toSet()
        if (ids.isEmpty()) return RepoResult.Error("请选择要指派的任务")
        val cleanAssignee = assignee.trim()
        if (cleanAssignee.isBlank()) return RepoResult.Error("指派人不能为空")
        val state = _snapshot.value
        val existingIds = state.tasks.map { it.id }.toSet()
        if (!existingIds.containsAll(ids)) return RepoResult.Error("存在无效任务")

        _snapshot.update { current ->
            current.copy(
                tasks = current.tasks.map { task ->
                    if (task.id in ids) task.copy(assignee = cleanAssignee) else task
                },
            )
        }
        appendAudit(
            action = "TASK_REASSIGN_BATCH",
            entityType = "task",
            entityId = "batch",
            summary = "批量指派 ${ids.size} 条任务给 $cleanAssignee",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.reassign.batch", payloadSummary = "count=${ids.size} assignee=$cleanAssignee")
        return RepoResult.Success
    }

    override fun setTaskEscalationConfig(config: TaskEscalationConfig, operator: String): RepoResult {
        _snapshot.update { current -> current.copy(taskEscalationConfig = config) }
        appendAudit(
            action = "TASK_ESCALATION_CONFIG",
            entityType = "task_config",
            entityId = "global",
            summary = "更新任务升级规则：24h=${config.enable24hEscalation},48h=${config.enable48hEscalation}",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.config.update", payloadSummary = "24h=${config.enable24hEscalation} 48h=${config.enable48hEscalation}")
        return RepoResult.Success
    }

    override fun applyTaskEscalation(operator: String): RepoResult {
        val state = _snapshot.value
        val config = state.taskEscalationConfig
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60L * 60L * 1000L
        val twoDay = 2 * oneDay
        var changed = 0

        val updated = state.tasks.map { task ->
            if (task.status == TaskStatus.Done) return@map task
            if (task.dueAtMillis >= now) return@map task

            val overdueMillis = now - task.dueAtMillis
            var nextPriority = task.priority
            if (config.enable48hEscalation && overdueMillis >= twoDay) {
                nextPriority = moreUrgent(nextPriority, config.priorityAt48h)
            } else if (config.enable24hEscalation && overdueMillis >= oneDay) {
                nextPriority = moreUrgent(nextPriority, config.priorityAt24h)
            }

            val nextAssignee = if (overdueMillis > 0L) {
                config.autoAssignOverdueTo?.ifBlank { null } ?: task.assignee
            } else {
                task.assignee
            }

            val nextStatus = TaskStatus.Overdue
            val nextTask = task.copy(
                status = nextStatus,
                priority = nextPriority,
                assignee = nextAssignee,
            )
            if (nextTask != task) changed += 1
            nextTask
        }

        if (changed == 0) return RepoResult.Error("没有需要升级的任务")

        _snapshot.update { current -> current.copy(tasks = updated) }
        appendAudit(
            action = "TASK_ESCALATION_APPLY",
            entityType = "task",
            entityId = "batch",
            summary = "应用升级规则，更新 $changed 条任务",
            operator = operator,
        )
        queueSyncEvent(eventType = "task.escalation.apply", payloadSummary = "changed=$changed")
        return RepoResult.Success
    }

    override fun moveAnimals(animalIds: List<String>, targetCageId: String, operator: String): RepoResult {
        val targetIds = animalIds.toSet()
        if (targetIds.isEmpty()) return RepoResult.Error("请先选择要转移的个体")

        val state = _snapshot.value
        val targetCage = state.cages.firstOrNull { it.id == targetCageId }
            ?: return RepoResult.Error("目标笼不存在")

        val movingAnimals = state.animals.filter { it.id in targetIds }
        if (movingAnimals.size != targetIds.size) return RepoResult.Error("部分个体不存在")
        if (movingAnimals.all { it.cageId == targetCageId }) return RepoResult.Error("个体已在目标笼")
        if (movingAnimals.any { it.status == AnimalStatus.Dead }) return RepoResult.Error("死亡个体不可转笼")

        val protocolValidation = validateProtocolsForAnimals(movingAnimals)
        if (protocolValidation is RepoResult.Error) return protocolValidation

        val projectedCount = targetCage.animalIds.toSet().plus(targetIds).size
        if (projectedCount > targetCage.capacityLimit) return RepoResult.Error("目标笼容量不足")

        _snapshot.update { current ->
            val updatedAnimals = current.animals.map { animal ->
                if (animal.id in targetIds) animal.copy(cageId = targetCageId) else animal
            }
            val updatedCages = current.cages.map { cage ->
                when {
                    cage.id == targetCageId -> cage.copy(animalIds = cage.animalIds.plus(targetIds).distinct())
                    cage.animalIds.any { it in targetIds } -> cage.copy(animalIds = cage.animalIds.filterNot { it in targetIds })
                    else -> cage
                }
            }
            current.copy(animals = updatedAnimals, cages = updatedCages)
        }

        appendAudit(
            action = "ANIMAL_MOVE",
            entityType = "animal",
            entityId = targetIds.joinToString(","),
            summary = "转笼到 $targetCageId，数量 ${targetIds.size}",
            operator = operator,
        )
        queueSyncEvent(eventType = "animal.move", payloadSummary = "target=$targetCageId count=${targetIds.size}")
        return RepoResult.Success
    }

    override fun mergeCages(sourceCageId: String, targetCageId: String, operator: String): RepoResult {
        if (sourceCageId == targetCageId) return RepoResult.Error("源笼与目标笼不能相同")
        val state = _snapshot.value
        val source = state.cages.firstOrNull { it.id == sourceCageId } ?: return RepoResult.Error("源笼不存在")
        val target = state.cages.firstOrNull { it.id == targetCageId } ?: return RepoResult.Error("目标笼不存在")
        if (source.status != CageStatus.Active || target.status != CageStatus.Active) return RepoResult.Error("仅支持活跃笼位并笼")
        if (source.animalIds.isEmpty()) return RepoResult.Error("源笼没有可并入个体")

        val mergedAnimalIds = target.animalIds.toSet().plus(source.animalIds).toList()
        if (mergedAnimalIds.size > target.capacityLimit) return RepoResult.Error("目标笼容量不足，无法并笼")

        _snapshot.update { current ->
            current.copy(
                animals = current.animals.map { animal ->
                    if (animal.id in source.animalIds) animal.copy(cageId = targetCageId) else animal
                },
                cages = current.cages.map { cage ->
                    when (cage.id) {
                        sourceCageId -> cage.copy(animalIds = emptyList(), status = CageStatus.Closed)
                        targetCageId -> cage.copy(animalIds = mergedAnimalIds)
                        else -> cage
                    }
                },
            )
        }

        appendAudit(
            action = "CAGE_MERGE",
            entityType = "cage",
            entityId = "$sourceCageId->$targetCageId",
            summary = "并笼完成，转移 ${source.animalIds.size} 只个体",
            operator = operator,
        )
        queueSyncEvent(eventType = "cage.merge", payloadSummary = "source=$sourceCageId,target=$targetCageId")
        return RepoResult.Success
    }

    override fun splitCage(
        sourceCageId: String,
        newCageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        animalIds: List<String>,
        operator: String,
    ): RepoResult {
        if (newCageId.isBlank()) return RepoResult.Error("新笼编号不能为空")
        if (capacityLimit !in 1..99) return RepoResult.Error("笼容量需在 1-99")
        val movingSet = animalIds.toSet()
        if (movingSet.isEmpty()) return RepoResult.Error("请先选择拆分个体")

        val state = _snapshot.value
        if (state.cages.any { it.id == newCageId }) return RepoResult.Error("新笼编号已存在")
        val source = state.cages.firstOrNull { it.id == sourceCageId } ?: return RepoResult.Error("源笼不存在")
        if (source.status != CageStatus.Active) return RepoResult.Error("仅支持从活跃笼位拆笼")
        if (!source.animalIds.containsAll(movingSet)) return RepoResult.Error("存在不在源笼中的个体")
        if (movingSet.size > capacityLimit) return RepoResult.Error("目标新笼容量不足")

        val movingAnimals = state.animals.filter { it.id in movingSet }
        val protocolValidation = validateProtocolsForAnimals(movingAnimals)
        if (protocolValidation is RepoResult.Error) return protocolValidation

        val newCage = Cage(
            id = newCageId,
            roomCode = roomCode.trim().ifBlank { source.roomCode },
            rackCode = rackCode.trim().ifBlank { source.rackCode },
            slotCode = slotCode.trim().ifBlank { source.slotCode },
            capacityLimit = capacityLimit,
            animalIds = movingSet.toList(),
            status = CageStatus.Active,
        )

        _snapshot.update { current ->
            current.copy(
                animals = current.animals.map { animal ->
                    if (animal.id in movingSet) animal.copy(cageId = newCageId) else animal
                },
                cages = listOf(newCage) + current.cages.map { cage ->
                    if (cage.id == sourceCageId) cage.copy(animalIds = cage.animalIds.filterNot { it in movingSet }) else cage
                },
            )
        }

        appendAudit(
            action = "CAGE_SPLIT",
            entityType = "cage",
            entityId = "$sourceCageId->$newCageId",
            summary = "拆笼完成，迁移 ${movingSet.size} 只个体",
            operator = operator,
        )
        queueSyncEvent(eventType = "cage.split", payloadSummary = "source=$sourceCageId,new=$newCageId,count=${movingSet.size}")
        return RepoResult.Success
    }

    override fun createCage(
        cageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        operator: String,
    ): RepoResult {
        val normalizedId = cageId.trim().uppercase()
        if (normalizedId.isBlank()) return RepoResult.Error("笼编号不能为空")
        if (capacityLimit !in 1..99) return RepoResult.Error("笼容量需在 1-99")
        if (_snapshot.value.cages.any { it.id.equals(normalizedId, ignoreCase = true) }) return RepoResult.Error("笼编号已存在")

        val cage = Cage(
            id = normalizedId,
            roomCode = roomCode.trim().uppercase().ifBlank { "A1" },
            rackCode = rackCode.trim().uppercase().ifBlank { "R1" },
            slotCode = slotCode.trim().uppercase().ifBlank { "01" },
            capacityLimit = capacityLimit,
            animalIds = emptyList(),
            status = CageStatus.Active,
        )

        _snapshot.update { current -> current.copy(cages = listOf(cage) + current.cages) }
        appendAudit(
            action = "CAGE_CREATE",
            entityType = "cage",
            entityId = cage.id,
            summary = "创建笼位 ${cage.roomCode}/${cage.rackCode}/${cage.slotCode}",
            operator = operator,
        )
        queueSyncEvent(eventType = "cage.create", payloadSummary = "cage=${cage.id}")
        return RepoResult.Success
    }

    override fun createAnimal(
        identifier: String,
        sex: AnimalSex,
        strain: String,
        genotype: String,
        cageId: String,
        protocolId: String?,
        operator: String,
    ): RepoResult {
        val cleanIdentifier = identifier.trim().uppercase()
        if (cleanIdentifier.isBlank()) return RepoResult.Error("耳号/RFID 不能为空")
        if (_snapshot.value.animals.any { it.identifier.equals(cleanIdentifier, ignoreCase = true) }) {
            return RepoResult.Error("耳号/RFID 已存在")
        }
        val cleanStrain = strain.trim()
        if (cleanStrain.isBlank()) return RepoResult.Error("品系不能为空")
        if (!isValidStrain(cleanStrain)) return RepoResult.Error("品系不在主数据字典中")
        val cleanGenotype = genotype.trim()
        if (cleanGenotype.isBlank()) return RepoResult.Error("基因型不能为空")
        if (!isValidGenotype(cleanGenotype)) return RepoResult.Error("基因型不在模板字典中")

        val state = _snapshot.value
        val targetCage = state.cages.firstOrNull { it.id.equals(cageId, ignoreCase = true) }
            ?: return RepoResult.Error("目标笼不存在")
        if (targetCage.status != CageStatus.Active) return RepoResult.Error("目标笼非活跃状态")
        if (targetCage.animalIds.size >= targetCage.capacityLimit) return RepoResult.Error("目标笼容量不足")

        val normalizedProtocolId = protocolId?.trim()?.ifBlank { null }
        if (normalizedProtocolId != null) {
            val protocol = state.protocols.firstOrNull { it.id == normalizedProtocolId } ?: return RepoResult.Error("协议不存在")
            if (!protocol.isActive || protocol.expiresAtMillis < System.currentTimeMillis()) {
                return RepoResult.Error("协议不可用或已过期")
            }
        }

        val newAnimal = Animal(
            id = "A${idSeed.incrementAndGet()}",
            identifier = cleanIdentifier,
            sex = sex,
            birthAtMillis = System.currentTimeMillis(),
            strain = cleanStrain,
            genotype = cleanGenotype,
            status = AnimalStatus.Active,
            cageId = targetCage.id,
            protocolId = normalizedProtocolId,
        )

        _snapshot.update { current ->
            current.copy(
                animals = listOf(newAnimal) + current.animals,
                cages = current.cages.map { cage ->
                    if (cage.id == targetCage.id) cage.copy(animalIds = cage.animalIds + newAnimal.id) else cage
                },
            )
        }
        appendAudit(
            action = "ANIMAL_CREATE",
            entityType = "animal",
            entityId = newAnimal.id,
            summary = "创建个体 ${newAnimal.identifier} 并入笼 ${targetCage.id}",
            operator = operator,
        )
        queueSyncEvent(eventType = "animal.create", payloadSummary = "animal=${newAnimal.id}")
        return RepoResult.Success
    }

    override fun addStrainToCatalog(strain: String, operator: String): RepoResult {
        val clean = strain.trim()
        if (clean.isBlank()) return RepoResult.Error("品系不能为空")
        if (_snapshot.value.strainCatalog.any { it.equals(clean, ignoreCase = true) }) return RepoResult.Error("品系已存在")
        _snapshot.update { current -> current.copy(strainCatalog = (current.strainCatalog + clean).sorted()) }
        appendAudit(
            action = "STRAIN_CATALOG_ADD",
            entityType = "master_data",
            entityId = clean,
            summary = "新增品系字典项 $clean",
            operator = operator,
        )
        queueSyncEvent(eventType = "master.strain.add", payloadSummary = "strain=$clean")
        return RepoResult.Success
    }

    override fun removeStrainFromCatalog(strain: String, operator: String): RepoResult {
        val clean = strain.trim()
        if (clean.isBlank()) return RepoResult.Error("品系不能为空")
        if (_snapshot.value.animals.any { it.strain.equals(clean, ignoreCase = true) }) {
            return RepoResult.Error("仍有个体使用该品系，无法删除")
        }
        val exists = _snapshot.value.strainCatalog.any { it.equals(clean, ignoreCase = true) }
        if (!exists) return RepoResult.Error("品系不存在")
        _snapshot.update { current ->
            current.copy(strainCatalog = current.strainCatalog.filterNot { it.equals(clean, ignoreCase = true) })
        }
        appendAudit(
            action = "STRAIN_CATALOG_REMOVE",
            entityType = "master_data",
            entityId = clean,
            summary = "移除品系字典项 $clean",
            operator = operator,
        )
        queueSyncEvent(eventType = "master.strain.remove", payloadSummary = "strain=$clean")
        return RepoResult.Success
    }

    override fun addGenotypeToCatalog(genotype: String, operator: String): RepoResult {
        val clean = genotype.trim()
        if (clean.isBlank()) return RepoResult.Error("基因型模板不能为空")
        if (_snapshot.value.genotypeCatalog.any { it.equals(clean, ignoreCase = true) }) return RepoResult.Error("基因型模板已存在")
        _snapshot.update { current -> current.copy(genotypeCatalog = (current.genotypeCatalog + clean).sorted()) }
        appendAudit(
            action = "GENOTYPE_CATALOG_ADD",
            entityType = "master_data",
            entityId = clean,
            summary = "新增基因型模板 $clean",
            operator = operator,
        )
        queueSyncEvent(eventType = "master.genotype.add", payloadSummary = "genotype=$clean")
        return RepoResult.Success
    }

    override fun removeGenotypeFromCatalog(genotype: String, operator: String): RepoResult {
        val clean = genotype.trim()
        if (clean.isBlank()) return RepoResult.Error("基因型模板不能为空")
        if (_snapshot.value.animals.any { it.genotype.equals(clean, ignoreCase = true) }) {
            return RepoResult.Error("仍有个体使用该基因型，无法删除")
        }
        val exists = _snapshot.value.genotypeCatalog.any { it.equals(clean, ignoreCase = true) }
        if (!exists) return RepoResult.Error("基因型模板不存在")
        _snapshot.update { current ->
            current.copy(genotypeCatalog = current.genotypeCatalog.filterNot { it.equals(clean, ignoreCase = true) })
        }
        appendAudit(
            action = "GENOTYPE_CATALOG_REMOVE",
            entityType = "master_data",
            entityId = clean,
            summary = "移除基因型模板 $clean",
            operator = operator,
        )
        queueSyncEvent(eventType = "master.genotype.remove", payloadSummary = "genotype=$clean")
        return RepoResult.Success
    }

    override fun createBreedingPlan(
        maleId: String,
        femaleId: String,
        protocolId: String?,
        operator: String,
        notes: String,
    ): RepoResult {
        val trainingCheck = validateTraining(operator, "繁育操作")
        if (trainingCheck is RepoResult.Error) return trainingCheck
        val state = _snapshot.value
        val male = state.animals.firstOrNull { it.id == maleId } ?: return RepoResult.Error("雄鼠不存在")
        val female = state.animals.firstOrNull { it.id == femaleId } ?: return RepoResult.Error("雌鼠不存在")

        if (male.sex != AnimalSex.Male) return RepoResult.Error("选择的雄鼠性别不正确")
        if (female.sex != AnimalSex.Female) return RepoResult.Error("选择的雌鼠性别不正确")
        if (male.status == AnimalStatus.Dead || female.status == AnimalStatus.Dead) return RepoResult.Error("死亡个体不可用于配种")

        if (!protocolId.isNullOrBlank()) {
            val protocol = state.protocols.firstOrNull { it.id == protocolId }
            if (protocol == null || !protocol.isActive) return RepoResult.Error("协议无效或已禁用")
            if (protocol.expiresAtMillis < System.currentTimeMillis()) return RepoResult.Error("协议已过期，无法执行配种")
        }

        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L
        val breedingId = "BR-${idSeed.incrementAndGet()}"
        val plan = BreedingPlan(
            id = breedingId,
            maleId = maleId,
            femaleId = femaleId,
            protocolId = protocolId,
            matingAtMillis = now,
            expectedPlugCheckAtMillis = now + dayMillis * 3,
            expectedWeanAtMillis = now + dayMillis * 21,
            notes = notes,
        )

        val plugTask = LabTask(
            id = "TSK-${idSeed.incrementAndGet()}",
            title = "查栓检查",
            detail = "配种计划 $breedingId 的查栓检查",
            dueAtMillis = plan.expectedPlugCheckAtMillis,
            priority = TaskPriority.High,
            status = TaskStatus.Todo,
            entityType = "breeding",
            entityId = breedingId,
        )
        val weanTask = LabTask(
            id = "TSK-${idSeed.incrementAndGet()}",
            title = "断奶分笼",
            detail = "配种计划 $breedingId 的断奶安排",
            dueAtMillis = plan.expectedWeanAtMillis,
            priority = TaskPriority.Critical,
            status = TaskStatus.Todo,
            entityType = "breeding",
            entityId = breedingId,
        )

        _snapshot.update { current ->
            current.copy(
                animals = current.animals.map { animal ->
                    when (animal.id) {
                        maleId, femaleId -> animal.copy(status = AnimalStatus.Breeding)
                        else -> animal
                    }
                },
                breedingPlans = listOf(plan) + current.breedingPlans,
                tasks = listOf(plugTask, weanTask) + current.tasks,
            )
        }

        appendAudit(
            action = "BREEDING_CREATE",
            entityType = "breeding",
            entityId = breedingId,
            summary = "创建配种计划：$maleId x $femaleId",
            operator = operator,
        )
        queueSyncEvent(eventType = "breeding.create", payloadSummary = "plan=$breedingId")
        return RepoResult.Success
    }

    override fun recordBirth(planId: String, pupCount: Int, strain: String?, genotype: String?, operator: String): RepoResult {
        val trainingCheck = validateTraining(operator, "繁育操作")
        if (trainingCheck is RepoResult.Error) return trainingCheck
        if (pupCount !in 1..30) return RepoResult.Error("产仔数量需在 1-30")
        val state = _snapshot.value
        val plan = state.breedingPlans.firstOrNull { it.id == planId } ?: return RepoResult.Error("配种计划不存在")
        if (plan.plugPositive == false) return RepoResult.Error("查栓阴性计划不可登记产仔")
        if (plan.weanedAtMillis != null) return RepoResult.Error("已断奶计划不可重复登记产仔")

        val father = state.animals.firstOrNull { it.id == plan.maleId } ?: return RepoResult.Error("雄鼠不存在")
        val mother = state.animals.firstOrNull { it.id == plan.femaleId } ?: return RepoResult.Error("雌鼠不存在")
        val strainValue = (strain?.trim()?.ifBlank { null } ?: mother.strain)
        if (!isValidStrain(strainValue)) return RepoResult.Error("品系不在主数据字典中")
        val genotypeValue = (genotype?.trim()?.ifBlank { null } ?: mother.genotype)
        if (!isValidGenotype(genotypeValue)) return RepoResult.Error("基因型不在模板字典中")

        val targetCage = state.cages.firstOrNull { it.id == mother.cageId } ?: return RepoResult.Error("母鼠所在笼位不存在")
        val remainingCapacity = (targetCage.capacityLimit - targetCage.animalIds.size).coerceAtLeast(0)
        if (remainingCapacity < pupCount) return RepoResult.Error("母鼠笼位容量不足，无法登记产仔")

        val now = System.currentTimeMillis()
        val pups = (1..pupCount).map {
            Animal(
                id = "A${idSeed.incrementAndGet()}",
                identifier = "P${idSeed.incrementAndGet()}",
                sex = AnimalSex.Unknown,
                birthAtMillis = now,
                strain = strainValue,
                genotype = genotypeValue,
                status = AnimalStatus.Active,
                cageId = mother.cageId,
                protocolId = plan.protocolId ?: mother.protocolId,
                fatherId = father.id,
                motherId = mother.id,
            )
        }

        _snapshot.update { current ->
            current.copy(
                animals = pups + current.animals,
                cages = current.cages.map { cage ->
                    if (cage.id == mother.cageId) cage.copy(animalIds = cage.animalIds + pups.map { it.id }) else cage
                },
            )
        }

        appendAudit(
            action = "BREEDING_BIRTH_RECORD",
            entityType = "breeding",
            entityId = planId,
            summary = "登记产仔 ${pups.size} 只，父本 ${father.identifier}，母本 ${mother.identifier}",
            operator = operator,
        )
        queueSyncEvent(eventType = "breeding.birth_record", payloadSummary = "plan=$planId count=${pups.size}")
        return RepoResult.Success
    }

    override fun recordPlugCheck(planId: String, positive: Boolean, operator: String): RepoResult {
        val trainingCheck = validateTraining(operator, "繁育操作")
        if (trainingCheck is RepoResult.Error) return trainingCheck
        val state = _snapshot.value
        val plan = state.breedingPlans.firstOrNull { it.id == planId } ?: return RepoResult.Error("配种计划不存在")
        if (plan.plugCheckedAtMillis != null) return RepoResult.Error("该计划已完成查栓")
        val now = System.currentTimeMillis()

        _snapshot.update { current ->
            current.copy(
                breedingPlans = current.breedingPlans.map {
                    if (it.id == planId) it.copy(plugCheckedAtMillis = now, plugPositive = positive) else it
                },
                animals = current.animals.map { animal ->
                    if (!positive && (animal.id == plan.maleId || animal.id == plan.femaleId)) {
                        animal.copy(status = AnimalStatus.Active)
                    } else {
                        animal
                    }
                },
                tasks = current.tasks.map { task ->
                    if (task.entityType == "breeding" && task.entityId == planId && task.title.contains("查栓")) {
                        task.copy(status = TaskStatus.Done, completedAtMillis = now)
                    } else {
                        task
                    }
                },
            )
        }

        appendAudit(
            action = "BREEDING_PLUG_CHECK",
            entityType = "breeding",
            entityId = planId,
            summary = "查栓结果: ${if (positive) "阳性" else "阴性"}",
            operator = operator,
        )
        queueSyncEvent(eventType = "breeding.plug_check", payloadSummary = "plan=$planId positive=$positive")
        return RepoResult.Success
    }

    override fun completeWeaning(planId: String, operator: String): RepoResult {
        val trainingCheck = validateTraining(operator, "繁育操作")
        if (trainingCheck is RepoResult.Error) return trainingCheck
        val state = _snapshot.value
        val plan = state.breedingPlans.firstOrNull { it.id == planId } ?: return RepoResult.Error("配种计划不存在")
        if (plan.weanedAtMillis != null) return RepoResult.Error("该计划已完成断奶")
        if (plan.plugPositive == false) return RepoResult.Error("查栓阴性计划不可执行断奶")
        val now = System.currentTimeMillis()

        _snapshot.update { current ->
            current.copy(
                breedingPlans = current.breedingPlans.map {
                    if (it.id == planId) it.copy(weanedAtMillis = now) else it
                },
                animals = current.animals.map { animal ->
                    if (animal.id == plan.maleId || animal.id == plan.femaleId) animal.copy(status = AnimalStatus.Active) else animal
                },
                tasks = current.tasks.map { task ->
                    if (task.entityType == "breeding" && task.entityId == planId && task.title.contains("断奶")) {
                        task.copy(status = TaskStatus.Done, completedAtMillis = now)
                    } else {
                        task
                    }
                },
            )
        }

        appendAudit(
            action = "BREEDING_WEAN_COMPLETE",
            entityType = "breeding",
            entityId = planId,
            summary = "断奶流程已完成",
            operator = operator,
        )
        queueSyncEvent(eventType = "breeding.wean", payloadSummary = "plan=$planId")
        return RepoResult.Success
    }

    override fun reopenWeaning(planId: String, operator: String): RepoResult {
        val state = _snapshot.value
        val plan = state.breedingPlans.firstOrNull { it.id == planId } ?: return RepoResult.Error("配种计划不存在")
        if (plan.weanedAtMillis == null) return RepoResult.Error("该计划尚未完成断奶，无需撤销")

        _snapshot.update { current ->
            current.copy(
                breedingPlans = current.breedingPlans.map {
                    if (it.id == planId) it.copy(weanedAtMillis = null) else it
                },
                animals = current.animals.map { animal ->
                    if (animal.id == plan.maleId || animal.id == plan.femaleId) {
                        animal.copy(status = AnimalStatus.Breeding)
                    } else {
                        animal
                    }
                },
                tasks = current.tasks.map { task ->
                    if (task.entityType == "breeding" && task.entityId == planId && task.title.contains("断奶")) {
                        task.copy(status = TaskStatus.Todo, completedAtMillis = null)
                    } else {
                        task
                    }
                },
            )
        }

        appendAudit(
            action = "BREEDING_WEAN_REOPEN",
            entityType = "breeding",
            entityId = planId,
            summary = "断奶流程撤销并恢复待执行",
            operator = operator,
        )
        queueSyncEvent(eventType = "breeding.wean_reopen", payloadSummary = "plan=$planId")
        return RepoResult.Success
    }

    override fun registerSample(animalId: String, sampleType: SampleType, operator: String): RepoResult {
        val trainingCheck = validateTraining(operator, "分型采样")
        if (trainingCheck is RepoResult.Error) return trainingCheck
        val animal = _snapshot.value.animals.firstOrNull { it.id == animalId }
            ?: return RepoResult.Error("个体不存在")

        val sample = Sample(
            id = "SMP-${idSeed.incrementAndGet()}",
            animalId = animalId,
            sampleType = sampleType,
            sampledAtMillis = System.currentTimeMillis(),
            operator = operator,
        )

        _snapshot.update { current -> current.copy(samples = listOf(sample) + current.samples) }

        appendAudit(
            action = "SAMPLE_REGISTER",
            entityType = "sample",
            entityId = sample.id,
            summary = "为 ${animal.identifier} 采样 ${sampleType.displayName}",
            operator = operator,
        )
        queueSyncEvent(eventType = "sample.register", payloadSummary = "sample=${sample.id}")
        return RepoResult.Success
    }

    override fun createGenotypingBatch(name: String, sampleIds: List<String>, operator: String): RepoResult {
        if (name.isBlank()) return RepoResult.Error("批次名称不能为空")
        if (sampleIds.isEmpty()) return RepoResult.Error("请先选择样本")

        val sampleSet = sampleIds.toSet()
        val existing = _snapshot.value.samples.filter { it.id in sampleSet }
        if (existing.size != sampleSet.size) return RepoResult.Error("存在无效样本")

        val batch = GenotypingBatch(
            id = "GBT-${idSeed.incrementAndGet()}",
            name = name,
            sampleIds = sampleIds,
            status = BatchStatus.Submitted,
            createdAtMillis = System.currentTimeMillis(),
        )
        val positionBySampleId = sampleIds
            .distinct()
            .mapIndexed { index, id -> id to toPlatePosition(index) }
            .toMap()

        _snapshot.update { current ->
            current.copy(
                samples = current.samples.map { sample ->
                    if (sample.id in sampleSet) {
                        sample.copy(
                            batchId = batch.id,
                            platePosition = positionBySampleId[sample.id],
                        )
                    } else {
                        sample
                    }
                },
                genotypingBatches = listOf(batch) + current.genotypingBatches,
            )
        }

        appendAudit(
            action = "GENOTYPING_BATCH_CREATE",
            entityType = "genotyping_batch",
            entityId = batch.id,
            summary = "创建分型批次 ${batch.name}，样本 ${sampleIds.size} 个并分配板位",
            operator = operator,
        )
        queueSyncEvent(eventType = "genotyping.batch", payloadSummary = "batch=${batch.id}")
        return RepoResult.Success
    }

    override fun importGenotypingResults(batchId: String, csvText: String, reviewer: String, operator: String): RepoResult {
        val batch = _snapshot.value.genotypingBatches.firstOrNull { it.id == batchId }
            ?: return RepoResult.Error("分型批次不存在")
        if (reviewer.isBlank()) return RepoResult.Error("请填写reviewer")

        data class ParsedRow(val lineNumber: Int, val sampleId: String, val marker: String, val callValue: String)
        val lines = csvText.lineSequence().toList()
        val issues = mutableListOf<GenotypingImportIssue>()
        val parsedRows = mutableListOf<ParsedRow>()
        lines.forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) return@forEachIndexed
            val cols = trimmed.split(',').map { it.trim() }
            if (cols.firstOrNull()?.equals("sample_id", true) == true) return@forEachIndexed
            if (cols.size < 3) {
                issues += GenotypingImportIssue(lineNumber, "列数不足，至少需要 sample_id,marker,call", rawLine)
                return@forEachIndexed
            }
            val sampleId = cols[0]
            val marker = cols[1]
            val callValue = cols[2]
            if (sampleId.isBlank() || marker.isBlank() || callValue.isBlank()) {
                issues += GenotypingImportIssue(lineNumber, "sample_id/marker/call 不可为空", rawLine)
                return@forEachIndexed
            }
            parsedRows += ParsedRow(lineNumber = lineNumber, sampleId = sampleId, marker = marker, callValue = callValue)
        }

        if (parsedRows.isEmpty() && issues.isNotEmpty()) {
            updateGenotypingImportReport(
                batchId = batchId,
                reviewer = reviewer,
                importedCount = 0,
                conflictCount = 0,
                issues = issues,
            )
            return RepoResult.Error("导入失败：所有记录均不合法")
        }
        if (parsedRows.isEmpty()) return RepoResult.Error("没有可导入的结果，请使用 sample_id,marker,call 格式")

        val state = _snapshot.value
        val existingResults = state.genotypingResults
        val created = mutableListOf<GenotypingResult>()
        var conflictCount = 0
        val now = System.currentTimeMillis()

        parsedRows.forEach { row ->
            val sampleId = row.sampleId
            val marker = row.marker
            val callValue = row.callValue
            if (sampleId !in batch.sampleIds) {
                issues += GenotypingImportIssue(row.lineNumber, "样本 $sampleId 不在批次 $batchId 中", "$sampleId,$marker,$callValue")
                return@forEach
            }
            val history = existingResults.filter { it.sampleId == sampleId && it.marker.equals(marker, true) } +
                created.filter { it.sampleId == sampleId && it.marker.equals(marker, true) }

            val version = (history.maxOfOrNull { it.version } ?: 0) + 1
            val conflict = history.any { it.callValue != callValue && it.confirmed }
            if (conflict) conflictCount += 1

            created += GenotypingResult(
                id = "GTR-${idSeed.incrementAndGet()}",
                sampleId = sampleId,
                batchId = batchId,
                marker = marker,
                callValue = callValue,
                version = version,
                reviewer = reviewer,
                reviewedAtMillis = now,
                conflict = conflict,
                confirmed = !conflict,
            )
        }

        if (created.isEmpty()) {
            updateGenotypingImportReport(
                batchId = batchId,
                reviewer = reviewer,
                importedCount = 0,
                conflictCount = 0,
                issues = issues,
            )
            return RepoResult.Error("没有有效结果（样本可能不在该批次）")
        }

        _snapshot.update { current ->
            current.copy(
                genotypingBatches = current.genotypingBatches.map { if (it.id == batch.id) it.copy(status = BatchStatus.Completed) else it },
                genotypingResults = created + current.genotypingResults,
            )
        }
        updateGenotypingImportReport(
            batchId = batchId,
            reviewer = reviewer,
            importedCount = created.size,
            conflictCount = conflictCount,
            issues = issues,
        )

        appendAudit(
            action = "GENOTYPING_IMPORT",
            entityType = "genotyping_batch",
            entityId = batchId,
            summary = "导入分型结果 ${created.size} 条，冲突 $conflictCount 条，失败 ${issues.size} 条",
            operator = operator,
        )
        queueSyncEvent(eventType = "genotyping.import", payloadSummary = "batch=$batchId results=${created.size}")
        return if (issues.isEmpty()) RepoResult.Success else RepoResult.Error("部分导入成功：失败 ${issues.size} 条，可重试失败行")
    }

    override fun confirmGenotypingResult(resultId: String, operator: String): RepoResult {
        val target = _snapshot.value.genotypingResults.firstOrNull { it.id == resultId }
            ?: return RepoResult.Error("结果不存在")
        if (!target.conflict) return RepoResult.Error("该结果无需确认")

        _snapshot.update { current ->
            current.copy(
                genotypingResults = current.genotypingResults.map {
                    if (it.id == resultId) it.copy(conflict = false, confirmed = true) else it
                }
            )
        }

        appendAudit(
            action = "GENOTYPING_CONFIRM",
            entityType = "genotyping_result",
            entityId = resultId,
            summary = "确认冲突分型结果 ${target.sampleId}/${target.marker}",
            operator = operator,
        )
        queueSyncEvent(eventType = "genotyping.confirm", payloadSummary = "result=$resultId")
        return RepoResult.Success
    }

    override fun createCohort(
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        blindCodingEnabled: Boolean,
        blindCodePrefix: String?,
        operator: String,
    ): RepoResult {
        if (name.isBlank()) return RepoResult.Error("Cohort 名称不能为空")

        val now = System.currentTimeMillis()
        val matched = _snapshot.value.animals
            .asSequence()
            .filter { it.status == AnimalStatus.Active || it.status == AnimalStatus.InExperiment }
            .filter { strain.isNullOrBlank() || it.strain.equals(strain, ignoreCase = true) }
            .filter { genotype.isNullOrBlank() || it.genotype.contains(genotype, ignoreCase = true) }
            .filter { sex == null || it.sex == sex }
            .filter { minWeeks == null || it.ageInWeeks(now) >= minWeeks }
            .filter { maxWeeks == null || it.ageInWeeks(now) <= maxWeeks }
            .map { it.id }
            .toList()

        if (matched.isEmpty()) return RepoResult.Error("没有满足条件的个体")

        val criteria = buildString {
            append("strain=${strain ?: "*"}")
            append("; genotype=${genotype ?: "*"}")
            append("; sex=${sex?.name ?: "*"}")
            append("; age=${minWeeks ?: "*"}-${maxWeeks ?: "*"}")
            append("; blind=${if (blindCodingEnabled) "enabled" else "disabled"}")
        }
        val blindCodes = if (blindCodingEnabled) {
            val prefix = blindCodePrefix?.trim()?.uppercase()?.ifBlank { "BL" } ?: "BL"
            matched
                .sorted()
                .mapIndexed { index, animalId ->
                    animalId to "$prefix-${(index + 1).toString().padStart(3, '0')}"
                }
                .toMap()
        } else {
            emptyMap()
        }

        val cohortId = "COH-${idSeed.incrementAndGet()}"
        val cohort = Cohort(
            id = cohortId,
            name = name,
            criteriaSummary = criteria,
            animalIds = matched,
            blindCodes = blindCodes,
            locked = true,
            createdAtMillis = now,
        )

        _snapshot.update { current -> current.copy(cohorts = listOf(cohort) + current.cohorts) }

        appendAudit(
            action = "COHORT_CREATE",
            entityType = "cohort",
            entityId = cohortId,
            summary = "创建 cohort ${cohort.name}，纳入 ${matched.size} 只",
            operator = operator,
        )
        queueSyncEvent(eventType = "cohort.create", payloadSummary = "cohort=$cohortId")
        return RepoResult.Success
    }

    override fun saveCohortTemplate(
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        operator: String,
    ): RepoResult {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return RepoResult.Error("模板名称不能为空")
        if (minWeeks != null && maxWeeks != null && minWeeks > maxWeeks) return RepoResult.Error("周龄范围不合法")
        val now = System.currentTimeMillis()

        val existing = _snapshot.value.cohortTemplates.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        if (existing != null) {
            _snapshot.update { current ->
                current.copy(
                    cohortTemplates = current.cohortTemplates.map { template ->
                        if (template.id == existing.id) {
                            template.copy(
                                strain = strain,
                                genotype = genotype,
                                sex = sex,
                                minWeeks = minWeeks,
                                maxWeeks = maxWeeks,
                                updatedAtMillis = now,
                            )
                        } else {
                            template
                        }
                    },
                )
            }
            appendAudit(
                action = "COHORT_TEMPLATE_UPDATE",
                entityType = "cohort_template",
                entityId = existing.id,
                summary = "更新 Cohort 模板 $cleanName",
                operator = operator,
            )
            return RepoResult.Success
        }

        val template = CohortTemplate(
            id = "CTP-${idSeed.incrementAndGet()}",
            name = cleanName,
            strain = strain,
            genotype = genotype,
            sex = sex,
            minWeeks = minWeeks,
            maxWeeks = maxWeeks,
            usageCount = 0,
            createdAtMillis = now,
            updatedAtMillis = now,
        )

        _snapshot.update { current ->
            current.copy(cohortTemplates = listOf(template) + current.cohortTemplates)
        }
        appendAudit(
            action = "COHORT_TEMPLATE_CREATE",
            entityType = "cohort_template",
            entityId = template.id,
            summary = "创建 Cohort 模板 $cleanName",
            operator = operator,
        )
        queueSyncEvent(eventType = "cohort.template.create", payloadSummary = "template=${template.id}")
        return RepoResult.Success
    }

    override fun updateCohortTemplate(
        templateId: String,
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        operator: String,
    ): RepoResult {
        if (name.isBlank()) return RepoResult.Error("模板名称不能为空")
        if (minWeeks != null && maxWeeks != null && minWeeks > maxWeeks) return RepoResult.Error("周龄范围不合法")
        val existing = _snapshot.value.cohortTemplates.firstOrNull { it.id == templateId } ?: return RepoResult.Error("模板不存在")
        val duplicate = _snapshot.value.cohortTemplates.any { it.id != templateId && it.name.equals(name, ignoreCase = true) }
        if (duplicate) return RepoResult.Error("模板名称已存在")
        val now = System.currentTimeMillis()

        _snapshot.update { current ->
            current.copy(
                cohortTemplates = current.cohortTemplates.map { template ->
                    if (template.id == templateId) {
                        template.copy(
                            name = name.trim(),
                            strain = strain,
                            genotype = genotype,
                            sex = sex,
                            minWeeks = minWeeks,
                            maxWeeks = maxWeeks,
                            updatedAtMillis = now,
                        )
                    } else {
                        template
                    }
                },
            )
        }
        appendAudit(
            action = "COHORT_TEMPLATE_UPDATE",
            entityType = "cohort_template",
            entityId = templateId,
            summary = "更新 Cohort 模板 ${existing.name} -> ${name.trim()}",
            operator = operator,
        )
        queueSyncEvent(eventType = "cohort.template.update", payloadSummary = "template=$templateId")
        return RepoResult.Success
    }

    override fun deleteCohortTemplate(templateId: String, operator: String): RepoResult {
        val existing = _snapshot.value.cohortTemplates.firstOrNull { it.id == templateId } ?: return RepoResult.Error("模板不存在")
        _snapshot.update { current ->
            current.copy(cohortTemplates = current.cohortTemplates.filterNot { it.id == templateId })
        }
        appendAudit(
            action = "COHORT_TEMPLATE_DELETE",
            entityType = "cohort_template",
            entityId = templateId,
            summary = "删除 Cohort 模板 ${existing.name}",
            operator = operator,
        )
        queueSyncEvent(eventType = "cohort.template.delete", payloadSummary = "template=$templateId")
        return RepoResult.Success
    }

    override fun applyCohortTemplate(templateId: String, operator: String): RepoResult {
        val template = _snapshot.value.cohortTemplates.firstOrNull { it.id == templateId }
            ?: return RepoResult.Error("模板不存在")
        val now = System.currentTimeMillis()
        _snapshot.update { current ->
            current.copy(
                cohortTemplates = current.cohortTemplates.map {
                    if (it.id == templateId) it.copy(usageCount = it.usageCount + 1, updatedAtMillis = now) else it
                },
            )
        }
        appendAudit(
            action = "COHORT_TEMPLATE_APPLY",
            entityType = "cohort_template",
            entityId = templateId,
            summary = "应用 Cohort 模板 ${template.name}",
            operator = operator,
        )
        queueSyncEvent(eventType = "cohort.template.apply", payloadSummary = "template=$templateId")
        return RepoResult.Success
    }

    override fun createExperiment(cohortId: String, title: String, operator: String): RepoResult {
        val trainingCheck = validateTraining(operator, "实验操作")
        if (trainingCheck is RepoResult.Error) return trainingCheck
        if (title.isBlank()) return RepoResult.Error("实验标题不能为空")
        val cohort = _snapshot.value.cohorts.firstOrNull { it.id == cohortId }
            ?: return RepoResult.Error("Cohort 不存在")

        val now = System.currentTimeMillis()
        val experiment = ExperimentSession(
            id = "EXP-${idSeed.incrementAndGet()}",
            cohortId = cohortId,
            title = title,
            status = ExperimentStatus.Active,
            startedAtMillis = now,
            endedAtMillis = null,
        )
        val startEvent = ExperimentEvent(
            id = "EVT-${idSeed.incrementAndGet()}",
            experimentId = experiment.id,
            eventType = "start",
            note = "实验启动",
            createdAtMillis = now,
            operator = operator,
        )

        _snapshot.update { current ->
            current.copy(
                animals = current.animals.map {
                    if (it.id in cohort.animalIds && it.status == AnimalStatus.Active) it.copy(status = AnimalStatus.InExperiment) else it
                },
                experiments = listOf(experiment) + current.experiments,
                experimentEvents = listOf(startEvent) + current.experimentEvents,
            )
        }

        appendAudit(
            action = "EXPERIMENT_CREATE",
            entityType = "experiment",
            entityId = experiment.id,
            summary = "创建实验 $title",
            operator = operator,
        )
        queueSyncEvent(eventType = "experiment.create", payloadSummary = "experiment=${experiment.id}")
        return RepoResult.Success
    }

    override fun addExperimentEvent(experimentId: String, eventType: String, note: String, operator: String): RepoResult {
        val experiment = _snapshot.value.experiments.firstOrNull { it.id == experimentId }
            ?: return RepoResult.Error("实验不存在")
        if (experiment.status == ExperimentStatus.Archived) return RepoResult.Error("已归档实验不可追加事件")

        val event = ExperimentEvent(
            id = "EVT-${idSeed.incrementAndGet()}",
            experimentId = experimentId,
            eventType = eventType.ifBlank { "record" },
            note = note.ifBlank { "无备注" },
            createdAtMillis = System.currentTimeMillis(),
            operator = operator,
        )

        _snapshot.update { current -> current.copy(experimentEvents = listOf(event) + current.experimentEvents) }

        appendAudit(
            action = "EXPERIMENT_EVENT_ADD",
            entityType = "experiment",
            entityId = experimentId,
            summary = "追加事件 ${event.eventType}",
            operator = operator,
        )
        queueSyncEvent(eventType = "experiment.event", payloadSummary = "experiment=$experimentId")
        return RepoResult.Success
    }

    override fun archiveExperiment(experimentId: String, operator: String): RepoResult {
        val state = _snapshot.value
        val experiment = state.experiments.firstOrNull { it.id == experimentId }
            ?: return RepoResult.Error("实验不存在")
        if (experiment.status == ExperimentStatus.Archived) return RepoResult.Error("实验已归档")

        val cohort = state.cohorts.firstOrNull { it.id == experiment.cohortId }
        val now = System.currentTimeMillis()

        _snapshot.update { current ->
            current.copy(
                experiments = current.experiments.map {
                    if (it.id == experimentId) it.copy(status = ExperimentStatus.Archived, endedAtMillis = now) else it
                },
                animals = current.animals.map {
                    if (cohort != null && it.id in cohort.animalIds && it.status == AnimalStatus.InExperiment) it.copy(status = AnimalStatus.Active) else it
                }
            )
        }

        appendAudit(
            action = "EXPERIMENT_ARCHIVE",
            entityType = "experiment",
            entityId = experimentId,
            summary = "实验归档",
            operator = operator,
        )
        queueSyncEvent(eventType = "experiment.archive", payloadSummary = "experiment=$experimentId")
        return RepoResult.Success
    }

    override fun importAnimalsCsv(csvText: String, operator: String): RepoResult {
        val rows = csvText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (rows.isEmpty()) return RepoResult.Error("CSV 内容为空")

        val parsed = rows
            .filterNot { it.startsWith("identifier", ignoreCase = true) }
            .mapNotNull { line ->
                val cols = line.split(",").map { it.trim() }
                if (cols.size < 5) return@mapNotNull null
                val sex = when (cols[1].lowercase()) {
                    "male", "m", "雄" -> AnimalSex.Male
                    "female", "f", "雌" -> AnimalSex.Female
                    else -> AnimalSex.Unknown
                }
                ParsedAnimalCsvRow(
                    identifier = cols[0],
                    sex = sex,
                    strain = cols[2],
                    genotype = cols[3],
                    cageId = cols[4],
                    protocolId = cols.getOrNull(5)?.ifBlank { null },
                )
            }

        if (parsed.isEmpty()) return RepoResult.Error("未识别到有效 CSV 行")

        val state = _snapshot.value
        val validCageIds = state.cages.map { it.id }.toSet()
        val validRows = parsed.filter { it.cageId in validCageIds }
        if (validRows.isEmpty()) return RepoResult.Error("CSV 中未包含有效笼位")
        val invalidMasterRow = validRows.firstOrNull { row ->
            !isValidStrain(row.strain) || !isValidGenotype(row.genotype)
        }
        if (invalidMasterRow != null) {
            return RepoResult.Error("CSV存在不在主数据字典中的品系或基因型：${invalidMasterRow.strain}/${invalidMasterRow.genotype}")
        }

        val now = System.currentTimeMillis()
        val createdAnimals = validRows.map { row ->
            Animal(
                id = "A${idSeed.incrementAndGet()}",
                identifier = row.identifier,
                sex = row.sex,
                birthAtMillis = now - 70L * 24L * 60L * 60L * 1000L,
                strain = row.strain,
                genotype = row.genotype,
                status = AnimalStatus.Active,
                cageId = row.cageId,
                protocolId = row.protocolId,
            )
        }

        val invalidProtocol = createdAnimals.firstOrNull { animal ->
            val pid = animal.protocolId ?: return@firstOrNull false
            val protocol = state.protocols.firstOrNull { it.id == pid } ?: return@firstOrNull true
            !protocol.isActive || protocol.expiresAtMillis < now
        }
        if (invalidProtocol != null) return RepoResult.Error("存在协议不可用或过期的导入数据")

        _snapshot.update { current ->
            current.copy(
                animals = createdAnimals + current.animals,
                cages = current.cages.map { cage ->
                    val incoming = createdAnimals.filter { it.cageId == cage.id }.map { it.id }
                    if (incoming.isEmpty()) cage else cage.copy(animalIds = cage.animalIds + incoming)
                }
            )
        }

        appendAudit(
            action = "CSV_IMPORT_ANIMAL",
            entityType = "animal",
            entityId = "batch",
            summary = "导入个体 ${createdAnimals.size} 条",
            operator = operator,
        )
        queueSyncEvent(eventType = "animal.import", payloadSummary = "count=${createdAnimals.size}")
        return RepoResult.Success
    }

    override fun exportAnimalsCsv(): String {
        val header = "animal_id,identifier,sex,strain,genotype,status,cage_id,protocol_id,father_id,mother_id"
        val lines = _snapshot.value.animals.map { animal ->
            listOf(
                animal.id,
                animal.identifier,
                animal.sex.name,
                animal.strain,
                animal.genotype,
                animal.status.name,
                animal.cageId,
                animal.protocolId ?: "",
                animal.fatherId ?: "",
                animal.motherId ?: "",
            ).joinToString(",")
        }
        return listOf(header).plus(lines).joinToString("\n")
    }

    override fun exportComplianceCsv(): String {
        val header = "audit_id,action,entity_type,entity_id,summary,operator,created_at,before_fields,after_fields"
        val lines = _snapshot.value.auditEvents.map { audit ->
            listOf(
                audit.id,
                audit.action,
                audit.entityType,
                audit.entityId,
                audit.summary.replace(",", " "),
                audit.operator,
                audit.createdAtMillis.toString(),
                serializeAuditFields(audit.beforeFields),
                serializeAuditFields(audit.afterFields),
            ).joinToString(",")
        }
        return listOf(header).plus(lines).joinToString("\n")
    }

    override fun exportCohortBlindCsv(cohortId: String): String {
        val state = _snapshot.value
        val cohort = state.cohorts.firstOrNull { it.id == cohortId } ?: return ""
        if (cohort.blindCodes.isEmpty()) return ""
        val animalsById = state.animals.associateBy { it.id }
        val header = "blind_code,animal_id,identifier,strain,genotype,cage_id,status"
        val lines = cohort.blindCodes
            .entries
            .sortedBy { it.value }
            .map { (animalId, blindCode) ->
                val animal = animalsById[animalId]
                listOf(
                    blindCode,
                    animalId,
                    animal?.identifier ?: "",
                    animal?.strain ?: "",
                    animal?.genotype ?: "",
                    animal?.cageId ?: "",
                    animal?.status?.name ?: "",
                ).joinToString(",")
            }
        return listOf(header).plus(lines).joinToString("\n")
    }

    override fun retrySyncEvent(syncEventId: String, operator: String): RepoResult {
        val target = _snapshot.value.syncEvents.firstOrNull { it.id == syncEventId }
            ?: return RepoResult.Error("同步事件不存在")
        if (target.status == SyncStatus.Synced) return RepoResult.Error("已同步事件无需重试")

        _snapshot.update { current ->
            current.copy(
                syncEvents = current.syncEvents.map {
                    if (it.id == syncEventId) {
                        it.copy(
                            status = SyncStatus.Pending,
                            lastTriedAtMillis = System.currentTimeMillis(),
                            retryCount = it.retryCount + 1,
                        )
                    } else {
                        it
                    }
                }
            )
        }

        appendAudit(
            action = "SYNC_RETRY",
            entityType = "sync",
            entityId = syncEventId,
            summary = "手动重试同步事件",
            operator = operator,
        )
        return RepoResult.Success
    }

    override fun syncPendingEvents(operator: String): RepoResult {
        val now = System.currentTimeMillis()
        val pendingCount = _snapshot.value.syncEvents.count { it.status == SyncStatus.Pending || it.status == SyncStatus.Failed }
        if (pendingCount == 0) return RepoResult.Error("没有待同步事件")

        _snapshot.update { current ->
            current.copy(
                syncEvents = current.syncEvents.map { event ->
                    if (event.status == SyncStatus.Pending || event.status == SyncStatus.Failed) {
                        event.copy(status = SyncStatus.Synced, lastTriedAtMillis = now, retryCount = event.retryCount + 1)
                    } else event
                }
            )
        }

        appendAudit(
            action = "SYNC_FLUSH",
            entityType = "sync",
            entityId = "batch",
            summary = "同步队列已处理 $pendingCount 条事件",
            operator = operator,
        )
        return RepoResult.Success
    }

    override fun updateAnimalStatus(animalId: String, status: AnimalStatus, operator: String): RepoResult {
        val state = _snapshot.value
        val animal = state.animals.firstOrNull { it.id == animalId } ?: return RepoResult.Error("个体不存在")
        if (!animal.status.canTransitTo(status)) {
            return RepoResult.Error("状态流转不合法：${animal.status.displayName} -> ${status.displayName}")
        }
        if (animal.status == status) return RepoResult.Success
        if (status == AnimalStatus.Breeding || status == AnimalStatus.InExperiment) {
            val protocolId = animal.protocolId ?: return RepoResult.Error("关键状态变更需要绑定有效协议")
            val protocol = state.protocols.firstOrNull { it.id == protocolId } ?: return RepoResult.Error("协议不存在")
            if (!protocol.isActive || protocol.expiresAtMillis < System.currentTimeMillis()) {
                return RepoResult.Error("协议不可用或已过期，无法进入关键状态")
            }
        }

        _snapshot.update { current ->
            current.copy(animals = current.animals.map { if (it.id == animalId) it.copy(status = status) else it })
        }

        appendAudit(
            action = "ANIMAL_STATUS_UPDATE",
            entityType = "animal",
            entityId = animalId,
            summary = "${animal.identifier} 状态 ${animal.status.displayName} -> ${status.displayName}",
            operator = operator,
            beforeFields = mapOf("status" to animal.status.name),
            afterFields = mapOf("status" to status.name),
        )
        queueSyncEvent(eventType = "animal.status", payloadSummary = "animal=$animalId status=${status.name}")
        return RepoResult.Success
    }

    override fun addAnimalEvent(animalId: String, eventType: String, note: String, weightGram: Float?, operator: String): RepoResult {
        val animal = _snapshot.value.animals.firstOrNull { it.id == animalId } ?: return RepoResult.Error("个体不存在")
        val cleanType = eventType.trim().ifBlank { "record" }
        val cleanNote = note.trim().ifBlank { "无备注" }
        val normalizedWeight = weightGram?.takeIf { it > 0f }
        val event = AnimalEvent(
            id = "AEV-${idSeed.incrementAndGet()}",
            animalId = animalId,
            eventType = cleanType,
            note = cleanNote,
            weightGram = normalizedWeight,
            createdAtMillis = System.currentTimeMillis(),
            operator = operator,
        )
        _snapshot.update { current -> current.copy(animalEvents = listOf(event) + current.animalEvents) }
        appendAudit(
            action = "ANIMAL_EVENT_ADD",
            entityType = "animal_event",
            entityId = event.id,
            summary = "为 ${animal.identifier} 新增事件 $cleanType",
            operator = operator,
        )
        queueSyncEvent(eventType = "animal.event.add", payloadSummary = "animal=$animalId event=${event.id}")
        return RepoResult.Success
    }

    override fun addAnimalAttachment(animalId: String, label: String, filePath: String, operator: String): RepoResult {
        val animal = _snapshot.value.animals.firstOrNull { it.id == animalId } ?: return RepoResult.Error("个体不存在")
        val cleanLabel = label.trim()
        val cleanPath = filePath.trim()
        if (cleanLabel.isBlank()) return RepoResult.Error("附件名称不能为空")
        if (cleanPath.isBlank()) return RepoResult.Error("附件路径不能为空")
        val attachment = com.westcounty.micemice.data.model.AnimalAttachment(
            id = "ATT-${idSeed.incrementAndGet()}",
            animalId = animalId,
            label = cleanLabel,
            filePath = cleanPath,
            createdAtMillis = System.currentTimeMillis(),
            operator = operator,
        )
        _snapshot.update { current -> current.copy(animalAttachments = listOf(attachment) + current.animalAttachments) }
        appendAudit(
            action = "ANIMAL_ATTACHMENT_ADD",
            entityType = "animal_attachment",
            entityId = attachment.id,
            summary = "为 ${animal.identifier} 添加附件 $cleanLabel",
            operator = operator,
        )
        queueSyncEvent(eventType = "animal.attachment.add", payloadSummary = "animal=$animalId attachment=${attachment.id}")
        return RepoResult.Success
    }

    override fun setProtocolState(protocolId: String, isActive: Boolean, operator: String): RepoResult {
        val existing = _snapshot.value.protocols.firstOrNull { it.id == protocolId } ?: return RepoResult.Error("协议不存在")

        _snapshot.update { current ->
            current.copy(protocols = current.protocols.map { if (it.id == protocolId) it.copy(isActive = isActive) else it })
        }

        appendAudit(
            action = "PROTOCOL_TOGGLE",
            entityType = "protocol",
            entityId = protocolId,
            summary = "协议状态更新为 ${if (isActive) "启用" else "停用"}",
            operator = operator,
            beforeFields = mapOf("isActive" to existing.isActive.toString()),
            afterFields = mapOf("isActive" to isActive.toString()),
        )
        queueSyncEvent(eventType = "protocol.toggle", payloadSummary = "protocol=$protocolId active=$isActive")
        return RepoResult.Success
    }

    override fun upsertTrainingRecord(record: TrainingRecord, operator: String): RepoResult {
        val username = record.username.trim()
        if (username.isBlank()) return RepoResult.Error("用户名不能为空")
        if (record.expiresAtMillis <= 0L) return RepoResult.Error("培训有效期不合法")
        val normalized = record.copy(username = username)
        val before = _snapshot.value.trainingRecords.firstOrNull { it.username.equals(username, ignoreCase = true) }
        val exists = _snapshot.value.trainingRecords.any { it.username.equals(username, ignoreCase = true) }
        _snapshot.update { current ->
            current.copy(
                trainingRecords = if (exists) {
                    current.trainingRecords.map { if (it.username.equals(username, ignoreCase = true)) normalized else it }
                } else {
                    listOf(normalized) + current.trainingRecords
                }
            )
        }
        appendAudit(
            action = "TRAINING_RECORD_UPSERT",
            entityType = "training",
            entityId = username,
            summary = "更新培训资质：$username",
            operator = operator,
            beforeFields = mapOf(
                "isActive" to (before?.isActive?.toString() ?: ""),
                "expiresAtMillis" to (before?.expiresAtMillis?.toString() ?: ""),
                "note" to (before?.note ?: ""),
            ),
            afterFields = mapOf(
                "isActive" to normalized.isActive.toString(),
                "expiresAtMillis" to normalized.expiresAtMillis.toString(),
                "note" to normalized.note,
            ),
        )
        queueSyncEvent(eventType = "training.upsert", payloadSummary = "user=$username")
        return RepoResult.Success
    }

    override fun removeTrainingRecord(username: String, operator: String): RepoResult {
        val clean = username.trim()
        if (clean.isBlank()) return RepoResult.Error("用户名不能为空")
        val exists = _snapshot.value.trainingRecords.any { it.username.equals(clean, ignoreCase = true) }
        if (!exists) return RepoResult.Error("培训记录不存在")
        _snapshot.update { current ->
            current.copy(trainingRecords = current.trainingRecords.filterNot { it.username.equals(clean, ignoreCase = true) })
        }
        appendAudit(
            action = "TRAINING_RECORD_REMOVE",
            entityType = "training",
            entityId = clean,
            summary = "删除培训资质：$clean",
            operator = operator,
        )
        queueSyncEvent(eventType = "training.remove", payloadSummary = "user=$clean")
        return RepoResult.Success
    }

    override fun setNotificationPolicy(policy: NotificationPolicy, operator: String): RepoResult {
        if (policy.protocolExpiryLeadDays !in 1..60) return RepoResult.Error("协议提醒天数需在 1-60 之间")
        val before = _snapshot.value.notificationPolicy
        _snapshot.update { current -> current.copy(notificationPolicy = policy) }
        appendAudit(
            action = "NOTIFICATION_POLICY_UPDATE",
            entityType = "notification_policy",
            entityId = "global",
            summary = "更新通知策略：协议${policy.protocolExpiryLeadDays}天提醒",
            operator = operator,
            beforeFields = mapOf(
                "enableProtocolExpiry" to before.enableProtocolExpiry.toString(),
                "protocolExpiryLeadDays" to before.protocolExpiryLeadDays.toString(),
                "enableOverdueTask" to before.enableOverdueTask.toString(),
                "enableCageCapacity" to before.enableCageCapacity.toString(),
                "enableSyncFailure" to before.enableSyncFailure.toString(),
            ),
            afterFields = mapOf(
                "enableProtocolExpiry" to policy.enableProtocolExpiry.toString(),
                "protocolExpiryLeadDays" to policy.protocolExpiryLeadDays.toString(),
                "enableOverdueTask" to policy.enableOverdueTask.toString(),
                "enableCageCapacity" to policy.enableCageCapacity.toString(),
                "enableSyncFailure" to policy.enableSyncFailure.toString(),
            ),
        )
        queueSyncEvent(eventType = "notification.policy.update", payloadSummary = "leadDays=${policy.protocolExpiryLeadDays}")
        return RepoResult.Success
    }

    override fun setRolePermission(role: UserRole, permission: PermissionKey, enabled: Boolean, operator: String): RepoResult {
        val beforeConfig = _snapshot.value.rolePermissionOverrides
        val afterConfig = beforeConfig.withPermission(role, permission, enabled)
        if (beforeConfig == afterConfig) return RepoResult.Success
        _snapshot.update { current -> current.copy(rolePermissionOverrides = afterConfig) }
        appendAudit(
            action = "RBAC_PERMISSION_UPDATE",
            entityType = "rbac",
            entityId = "${role.name}:${permission.name}",
            summary = "权限点更新 ${role.displayName} · ${permission.displayName} -> ${if (enabled) "启用" else "禁用"}",
            operator = operator,
            beforeFields = mapOf("enabled" to beforeConfig.isGranted(role, permission).toString()),
            afterFields = mapOf("enabled" to afterConfig.isGranted(role, permission).toString()),
        )
        queueSyncEvent(
            eventType = "rbac.permission.update",
            payloadSummary = "role=${role.name} permission=${permission.name} enabled=$enabled",
        )
        return RepoResult.Success
    }

    private fun updateGenotypingImportReport(
        batchId: String,
        reviewer: String,
        importedCount: Int,
        conflictCount: Int,
        issues: List<GenotypingImportIssue>,
    ) {
        val failedRowsCsv = buildString {
            append("sample_id,marker,call\n")
            issues.forEach { issue ->
                val cols = issue.rawLine.split(',').map { it.trim() }
                val sample = cols.getOrElse(0) { "" }
                val marker = cols.getOrElse(1) { "" }
                val call = cols.getOrElse(2) { "" }
                append("$sample,$marker,$call\n")
            }
        }.trim()
        val report = GenotypingImportReport(
            batchId = batchId,
            reviewer = reviewer,
            importedCount = importedCount,
            conflictCount = conflictCount,
            failedCount = issues.size,
            issues = issues,
            failedRowsCsv = failedRowsCsv,
            importedAtMillis = System.currentTimeMillis(),
        )
        _snapshot.update { current -> current.copy(lastGenotypingImportReport = report) }
    }

    private data class ParsedAnimalCsvRow(
        val identifier: String,
        val sex: AnimalSex,
        val strain: String,
        val genotype: String,
        val cageId: String,
        val protocolId: String?,
    )

    private fun isValidStrain(strain: String): Boolean {
        val catalog = _snapshot.value.strainCatalog
        if (catalog.isEmpty()) return true
        return catalog.any { it.equals(strain.trim(), ignoreCase = true) }
    }

    private fun isValidGenotype(genotype: String): Boolean {
        val catalog = _snapshot.value.genotypeCatalog
        if (catalog.isEmpty()) return true
        return catalog.any { it.equals(genotype.trim(), ignoreCase = true) }
    }

    private fun toPlatePosition(index: Int): String {
        val normalized = index.coerceAtLeast(0)
        val row = normalized / 12
        val col = (normalized % 12) + 1
        val rowCode = ('A'.code + (row % 8)).toChar()
        return "$rowCode${col.toString().padStart(2, '0')}"
    }

    private fun validateProtocolsForAnimals(animals: List<Animal>): RepoResult {
        val now = System.currentTimeMillis()
        val protocols = _snapshot.value.protocols.associateBy { it.id }
        val invalidAnimal = animals.firstOrNull { animal ->
            val protocolId = animal.protocolId ?: return@firstOrNull false
            val protocol = protocols[protocolId] ?: return@firstOrNull true
            !protocol.isActive || protocol.expiresAtMillis < now
        }
        return if (invalidAnimal != null) {
            RepoResult.Error("个体 ${invalidAnimal.identifier} 的协议不可用或已过期")
        } else {
            RepoResult.Success
        }
    }

    private fun validateTraining(operator: String, operationLabel: String): RepoResult {
        val records = _snapshot.value.trainingRecords
        if (records.isEmpty()) return RepoResult.Success
        val now = System.currentTimeMillis()
        val record = records.firstOrNull { it.username.equals(operator, ignoreCase = true) }
            ?: return RepoResult.Error("$operationLabel 需要有效培训资质")
        if (!record.isActive || record.expiresAtMillis < now) {
            return RepoResult.Error("$operationLabel 需要有效培训资质")
        }
        return RepoResult.Success
    }

    private fun moreUrgent(current: TaskPriority, target: TaskPriority): TaskPriority {
        return if (target.rank < current.rank) target else current
    }

    private fun queueSyncEvent(eventType: String, payloadSummary: String) {
        val now = System.currentTimeMillis()
        _snapshot.update { current ->
            current.copy(
                syncEvents = listOf(
                    SyncEvent(
                        id = "SYNC-${idSeed.incrementAndGet()}",
                        eventType = eventType,
                        payloadSummary = payloadSummary,
                        status = SyncStatus.Pending,
                        createdAtMillis = now,
                        lastTriedAtMillis = null,
                        retryCount = 0,
                    )
                ) + current.syncEvents
            )
        }
    }

    private fun appendAudit(
        action: String,
        entityType: String,
        entityId: String,
        summary: String,
        operator: String,
        beforeFields: Map<String, String> = emptyMap(),
        afterFields: Map<String, String> = emptyMap(),
    ) {
        val now = System.currentTimeMillis()
        val event = AuditEvent(
            id = "AUD-${idSeed.incrementAndGet()}",
            action = action,
            entityType = entityType,
            entityId = entityId,
            summary = summary,
            operator = operator,
            createdAtMillis = now,
            beforeFields = beforeFields,
            afterFields = afterFields,
        )
        _snapshot.update { current -> current.copy(auditEvents = listOf(event) + current.auditEvents) }
    }

    private fun serializeAuditFields(fields: Map<String, String>): String {
        if (fields.isEmpty()) return ""
        return fields.entries.joinToString(";") { (key, value) ->
            val safeKey = key.replace(",", " ").replace(";", " ").replace("=", " ")
            val safeValue = value.replace(",", " ").replace(";", " ").replace("=", " ")
            "$safeKey=$safeValue"
        }
    }

    private fun seedSnapshot(): LabSnapshot {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L

        val protocols = listOf(
            Protocol("P-NEURO-2026-001", "神经退行性疾病模型", now + 10 * dayMillis, true),
            Protocol("P-IMMUNE-2026-003", "免疫治疗评估", now + 40 * dayMillis, true),
            Protocol("P-METAB-2025-011", "代谢与肥胖模型", now - 2 * dayMillis, false),
        )

        val animals = listOf(
            Animal("A001", "E24001", AnimalSex.Male, now - 90 * dayMillis, "C57BL/6J", "+/+", AnimalStatus.Active, "C-101", "P-NEURO-2026-001"),
            Animal("A002", "E24002", AnimalSex.Female, now - 89 * dayMillis, "C57BL/6J", "+/-", AnimalStatus.Active, "C-101", "P-NEURO-2026-001"),
            Animal("A003", "E24003", AnimalSex.Female, now - 88 * dayMillis, "C57BL/6J", "+/+", AnimalStatus.Active, "C-101", "P-NEURO-2026-001"),
            Animal("A004", "E24004", AnimalSex.Male, now - 120 * dayMillis, "BALB/c", "+/+", AnimalStatus.Breeding, "C-102", "P-IMMUNE-2026-003"),
            Animal("A005", "E24005", AnimalSex.Female, now - 117 * dayMillis, "BALB/c", "-/+", AnimalStatus.Breeding, "C-102", "P-IMMUNE-2026-003"),
            Animal("A006", "E24006", AnimalSex.Female, now - 60 * dayMillis, "BALB/c", "-/+", AnimalStatus.Active, "C-102", "P-IMMUNE-2026-003"),
            Animal("A007", "E24007", AnimalSex.Male, now - 50 * dayMillis, "NOD", "+/+", AnimalStatus.InExperiment, "C-103", "P-IMMUNE-2026-003"),
            Animal("A008", "E24008", AnimalSex.Female, now - 52 * dayMillis, "NOD", "+/+", AnimalStatus.InExperiment, "C-103", "P-IMMUNE-2026-003"),
            Animal("A009", "E24009", AnimalSex.Male, now - 42 * dayMillis, "C57BL/6J", "-/+", AnimalStatus.Active, "C-104", "P-NEURO-2026-001"),
            Animal("A010", "E24010", AnimalSex.Female, now - 44 * dayMillis, "C57BL/6J", "-/+", AnimalStatus.Active, "C-104", "P-NEURO-2026-001"),
            Animal("A011", "E24011", AnimalSex.Male, now - 32 * dayMillis, "ICR", "+/+", AnimalStatus.Active, "C-104", "P-IMMUNE-2026-003"),
            Animal("A012", "E24012", AnimalSex.Female, now - 34 * dayMillis, "ICR", "+/+", AnimalStatus.Retired, "C-104", null),
        )

        val cages = listOf(
            Cage("C-101", "A1", "R1", "01", 5, listOf("A001", "A002", "A003"), CageStatus.Active),
            Cage("C-102", "A1", "R1", "02", 5, listOf("A004", "A005", "A006"), CageStatus.Active),
            Cage("C-103", "A1", "R2", "01", 4, listOf("A007", "A008"), CageStatus.Active),
            Cage("C-104", "A2", "R1", "03", 6, listOf("A009", "A010", "A011", "A012"), CageStatus.Active),
        )

        val breedingPlans = listOf(
            BreedingPlan("BR-1999", "A004", "A005", "P-IMMUNE-2026-003", now - 2 * dayMillis, now + dayMillis, now + 19 * dayMillis, "第二轮验证配对")
        )

        val samples = listOf(
            Sample("SMP-1001", "A002", SampleType.Ear, now - 5 * dayMillis, "Alice", "GBT-1001", "A01"),
            Sample("SMP-1002", "A010", SampleType.Tail, now - 5 * dayMillis, "Alice", "GBT-1001", "A02"),
            Sample("SMP-1003", "A003", SampleType.Ear, now - dayMillis, "Alice", null),
        )

        val batches = listOf(
            GenotypingBatch("GBT-1001", "2026-02 Neuro Batch", listOf("SMP-1001", "SMP-1002"), BatchStatus.Completed, now - 5 * dayMillis)
        )

        val genotypingResults = listOf(
            GenotypingResult("GTR-1001", "SMP-1001", "GBT-1001", "GeneX", "+/-", 1, "Dr.Wang", now - 4 * dayMillis, false, true),
            GenotypingResult("GTR-1002", "SMP-1002", "GBT-1001", "GeneX", "-/-", 1, "Dr.Wang", now - 4 * dayMillis, false, true),
        )

        val cohorts = listOf(
            Cohort("COH-1501", "Neuro-Week12-Female", "strain=C57BL/6J; genotype=+/-; sex=Female; age=10-14", listOf("A002", "A010"), emptyMap(), true, now - 3 * dayMillis)
        )
        val strainCatalog = listOf("C57BL/6J", "BALB/c", "NOD", "ICR")
        val genotypeCatalog = listOf("+/+", "+/-", "-/+", "-/-", "WT", "KO")
        val animalAttachments = listOf(
            com.westcounty.micemice.data.model.AnimalAttachment(
                id = "ATT-1001",
                animalId = "A002",
                label = "健康检查报告",
                filePath = "/storage/emulated/0/Download/A002_health.pdf",
                createdAtMillis = now - 3 * dayMillis,
                operator = "Alice",
            )
        )
        val animalEvents = listOf(
            AnimalEvent(
                id = "AEV-1001",
                animalId = "A002",
                eventType = "weight",
                note = "周例行称重",
                weightGram = 21.3f,
                createdAtMillis = now - 2 * dayMillis,
                operator = "Alice",
            ),
            AnimalEvent(
                id = "AEV-1002",
                animalId = "A010",
                eventType = "health",
                note = "皮毛状态正常",
                weightGram = null,
                createdAtMillis = now - dayMillis,
                operator = "Alice",
            ),
        )
        val trainingRecords = listOf(
            TrainingRecord(username = "Alice", expiresAtMillis = now + 60 * dayMillis, isActive = true, note = "动物实验培训"),
            TrainingRecord(username = "tester", expiresAtMillis = now + 60 * dayMillis, isActive = true, note = "测试账号"),
        )
        val cohortTemplates = listOf(
            CohortTemplate(
                id = "CTP-1001",
                name = "Week10-14 Female +/-",
                strain = "C57BL/6J",
                genotype = "+/-",
                sex = AnimalSex.Female,
                minWeeks = 10,
                maxWeeks = 14,
                usageCount = 2,
                createdAtMillis = now - 7 * dayMillis,
                updatedAtMillis = now - 2 * dayMillis,
            ),
        )

        val experiments = listOf(
            ExperimentSession("EXP-1701", "COH-1501", "Morris Water Maze Pilot", ExperimentStatus.Active, now - 2 * dayMillis, null)
        )

        val experimentEvents = listOf(
            ExperimentEvent("EVT-1801", "EXP-1701", "dose", "给予药物 5mg/kg", now - dayMillis, "Alice"),
            ExperimentEvent("EVT-1802", "EXP-1701", "behavior", "第3天行为测试完成", now - 8 * 60L * 60L * 1000L, "Alice"),
        )

        val tasks = listOf(
            LabTask("TSK-1001", "笼位巡检", "检查 A1 房间笼位状态", now + 4 * 60L * 60L * 1000L, TaskPriority.Medium, TaskStatus.Todo, "cage", "A1"),
            LabTask("TSK-1002", "查栓检查", "配种 BR-1999 查栓", now + dayMillis, TaskPriority.High, TaskStatus.Todo, "breeding", "BR-1999"),
            LabTask("TSK-1003", "断奶准备", "为 C-102 配种准备断奶笼", now + 6 * dayMillis, TaskPriority.Critical, TaskStatus.Todo, "cage", "C-102"),
            LabTask("TSK-1004", "协议续期", "P-NEURO-2026-001 将在10天后到期", now + 2 * dayMillis, TaskPriority.Critical, TaskStatus.Todo, "protocol", "P-NEURO-2026-001"),
            LabTask("TSK-1005", "历史导入复核", "确认昨日导入记录", now - 5 * 60L * 60L * 1000L, TaskPriority.Low, TaskStatus.Overdue, "system", "import-job-77"),
        )
        val taskTemplates = listOf(
            TaskTemplate("TTM-1001", "每日笼位巡检", "检查房间内笼位状态并记录异常", TaskPriority.Medium, 24, "cage"),
            TaskTemplate("TTM-1002", "周度合规复核", "检查协议有效期与关键操作留痕", TaskPriority.High, 72, "protocol"),
        )

        val syncEvents = listOf(
            SyncEvent("SYNC-9001", "task.complete", "task=TSK-0999", SyncStatus.Pending, now - 4 * 60L * 60L * 1000L, null, 0),
            SyncEvent("SYNC-9000", "animal.move", "target=C-102 count=1", SyncStatus.Failed, now - 10 * 60L * 60L * 1000L, now - 8 * 60L * 60L * 1000L, 1),
        )

        val audits = listOf(
            AuditEvent("AUD-1001", "SEED_DATA", "system", "bootstrap", "初始化演示数据", "system", now - 12 * 60L * 60L * 1000L)
        )

        return LabSnapshot(
            currentRole = UserRole.Researcher,
            protocols = protocols,
            cages = cages,
            animals = animals,
            breedingPlans = breedingPlans,
            samples = samples,
            genotypingBatches = batches,
            genotypingResults = genotypingResults,
            cohorts = cohorts,
            cohortTemplates = cohortTemplates,
            strainCatalog = strainCatalog,
            genotypeCatalog = genotypeCatalog,
            animalAttachments = animalAttachments,
            animalEvents = animalEvents,
            trainingRecords = trainingRecords,
            experiments = experiments,
            experimentEvents = experimentEvents,
            tasks = tasks,
            taskTemplates = taskTemplates,
            syncEvents = syncEvents,
            auditEvents = audits,
        )
    }

    companion object {
        val instance: InMemoryLabRepository by lazy { InMemoryLabRepository() }

        fun createForTesting(): InMemoryLabRepository = InMemoryLabRepository()

        fun fromSnapshot(snapshot: LabSnapshot): InMemoryLabRepository = InMemoryLabRepository(snapshot)
    }
}
