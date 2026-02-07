package com.westcounty.micemice.data.model

import kotlin.math.max

enum class UserRole(val displayName: String) {
    Researcher("博士生/实验员"),
    PrincipalInvestigator("PI"),
    Admin("管理员")
}

enum class PermissionKey(
    val displayName: String,
    val minRole: UserRole,
) {
    TaskComplete("完成任务", UserRole.Researcher),
    MoveAnimal("转笼并笼拆笼", UserRole.Researcher),
    UpdateAnimalStatus("更新个体状态", UserRole.Researcher),
    WriteAnimalEvent("记录个体事件", UserRole.Researcher),
    WriteAnimalAttachment("添加个体附件", UserRole.Researcher),
    BreedingWrite("繁育写操作", UserRole.Researcher),
    GenotypingWrite("分型写操作", UserRole.Researcher),
    CohortWrite("Cohort写操作", UserRole.Researcher),
    ExperimentWrite("实验写操作", UserRole.Researcher),
    TaskManage("任务模板与升级规则", UserRole.Admin),
    MasterDataEdit("主数据维护", UserRole.Admin),
    CreateResources("创建笼位与个体", UserRole.Admin),
    ProtocolManage("协议管理", UserRole.Admin),
    TrainingManage("培训资质管理", UserRole.Admin),
    NotificationPolicyManage("通知策略管理", UserRole.Admin),
    SyncManage("同步与重试", UserRole.Admin),
    ImportExportManage("导入导出", UserRole.Admin),
    RbacManage("权限矩阵管理", UserRole.Admin),
}

data class RolePermissionOverrides(
    val researcherDenied: Set<String> = emptySet(),
    val principalInvestigatorDenied: Set<String> = emptySet(),
    val adminDenied: Set<String> = emptySet(),
) {
    fun isGranted(role: UserRole, permission: PermissionKey): Boolean {
        if (roleLevel(role) < roleLevel(permission.minRole)) return false
        if (role == UserRole.Admin && permission == PermissionKey.RbacManage) return true
        return permission.name !in deniedSet(role)
    }

    fun withPermission(role: UserRole, permission: PermissionKey, enabled: Boolean): RolePermissionOverrides {
        if (role == UserRole.Admin && permission == PermissionKey.RbacManage) return this
        val current = deniedSet(role).toMutableSet()
        if (enabled) current.remove(permission.name) else current.add(permission.name)
        return when (role) {
            UserRole.Researcher -> copy(researcherDenied = current)
            UserRole.PrincipalInvestigator -> copy(principalInvestigatorDenied = current)
            UserRole.Admin -> copy(adminDenied = current)
        }
    }

    fun deniedSet(role: UserRole): Set<String> = when (role) {
        UserRole.Researcher -> researcherDenied
        UserRole.PrincipalInvestigator -> principalInvestigatorDenied
        UserRole.Admin -> adminDenied
    }
}

enum class CageStatus {
    Active,
    Quarantine,
    Closed
}

enum class AnimalSex {
    Male,
    Female,
    Unknown
}

enum class AnimalStatus(val displayName: String) {
    Active("在笼"),
    Breeding("繁育中"),
    InExperiment("实验中"),
    Retired("退役"),
    Dead("死亡")
}

enum class TaskPriority(val displayName: String, val rank: Int) {
    Critical("关键", 0),
    High("高", 1),
    Medium("中", 2),
    Low("低", 3)
}

enum class TaskStatus {
    Todo,
    Done,
    Overdue
}

enum class TaskFilter {
    All,
    Pending,
    Overdue,
    Overdue24h,
    Overdue48h,
    Done,
    Today
}

enum class SampleType(val displayName: String) {
    Ear("耳组织"),
    Tail("尾组织"),
    Blood("血液")
}

enum class BatchStatus(val displayName: String) {
    Draft("草稿"),
    Submitted("已送检"),
    Completed("已完成")
}

enum class ExperimentStatus(val displayName: String) {
    Active("进行中"),
    Archived("已归档")
}

enum class NotificationSeverity(val displayName: String) {
    Critical("关键"),
    High("高"),
    Medium("中"),
    Low("低"),
}

enum class SyncStatus {
    Pending,
    Synced,
    Failed
}

data class Protocol(
    val id: String,
    val title: String,
    val expiresAtMillis: Long,
    val isActive: Boolean = true,
)

data class Cage(
    val id: String,
    val roomCode: String,
    val rackCode: String,
    val slotCode: String,
    val capacityLimit: Int,
    val animalIds: List<String>,
    val status: CageStatus,
) {
    val occupancyRatio: Float
        get() = if (capacityLimit == 0) 0f else animalIds.size.toFloat() / capacityLimit.toFloat()

    val occupancyText: String
        get() = "${animalIds.size}/$capacityLimit"
}

data class Animal(
    val id: String,
    val identifier: String,
    val sex: AnimalSex,
    val birthAtMillis: Long,
    val strain: String,
    val genotype: String,
    val status: AnimalStatus,
    val cageId: String,
    val protocolId: String?,
    val fatherId: String? = null,
    val motherId: String? = null,
)

data class BreedingPlan(
    val id: String,
    val maleId: String,
    val femaleId: String,
    val protocolId: String?,
    val matingAtMillis: Long,
    val expectedPlugCheckAtMillis: Long,
    val expectedWeanAtMillis: Long,
    val notes: String,
    val plugCheckedAtMillis: Long? = null,
    val plugPositive: Boolean? = null,
    val weanedAtMillis: Long? = null,
)

data class Sample(
    val id: String,
    val animalId: String,
    val sampleType: SampleType,
    val sampledAtMillis: Long,
    val operator: String,
    val batchId: String? = null,
    val platePosition: String? = null,
)

data class GenotypingBatch(
    val id: String,
    val name: String,
    val sampleIds: List<String>,
    val status: BatchStatus,
    val createdAtMillis: Long,
)

data class GenotypingResult(
    val id: String,
    val sampleId: String,
    val batchId: String,
    val marker: String,
    val callValue: String,
    val version: Int,
    val reviewer: String,
    val reviewedAtMillis: Long,
    val conflict: Boolean,
    val confirmed: Boolean,
)

data class GenotypingImportIssue(
    val lineNumber: Int,
    val reason: String,
    val rawLine: String,
)

data class GenotypingImportReport(
    val batchId: String,
    val reviewer: String,
    val importedCount: Int,
    val conflictCount: Int,
    val failedCount: Int,
    val issues: List<GenotypingImportIssue>,
    val failedRowsCsv: String,
    val importedAtMillis: Long,
)

data class CohortTemplate(
    val id: String,
    val name: String,
    val strain: String?,
    val genotype: String?,
    val sex: AnimalSex?,
    val minWeeks: Int?,
    val maxWeeks: Int?,
    val usageCount: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class Cohort(
    val id: String,
    val name: String,
    val criteriaSummary: String,
    val animalIds: List<String>,
    val blindCodes: Map<String, String> = emptyMap(),
    val locked: Boolean,
    val createdAtMillis: Long,
)

data class ExperimentSession(
    val id: String,
    val cohortId: String,
    val title: String,
    val status: ExperimentStatus,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
)

data class ExperimentEvent(
    val id: String,
    val experimentId: String,
    val eventType: String,
    val note: String,
    val createdAtMillis: Long,
    val operator: String,
)

data class AnimalAttachment(
    val id: String,
    val animalId: String,
    val label: String,
    val filePath: String,
    val createdAtMillis: Long,
    val operator: String,
)

data class AnimalEvent(
    val id: String,
    val animalId: String,
    val eventType: String,
    val note: String,
    val weightGram: Float? = null,
    val createdAtMillis: Long,
    val operator: String,
)

data class TrainingRecord(
    val username: String,
    val expiresAtMillis: Long,
    val isActive: Boolean,
    val note: String = "",
)

data class SyncEvent(
    val id: String,
    val eventType: String,
    val payloadSummary: String,
    val status: SyncStatus,
    val createdAtMillis: Long,
    val lastTriedAtMillis: Long?,
    val retryCount: Int,
)

data class LabTask(
    val id: String,
    val title: String,
    val detail: String,
    val dueAtMillis: Long,
    val priority: TaskPriority,
    val status: TaskStatus,
    val entityType: String,
    val entityId: String,
    val assignee: String = "未指派",
    val completedAtMillis: Long? = null,
)

data class TaskTemplate(
    val id: String,
    val name: String,
    val detail: String,
    val defaultPriority: TaskPriority,
    val dueInHours: Int,
    val entityType: String,
)

data class TaskEscalationConfig(
    val enable24hEscalation: Boolean = true,
    val enable48hEscalation: Boolean = true,
    val priorityAt24h: TaskPriority = TaskPriority.High,
    val priorityAt48h: TaskPriority = TaskPriority.Critical,
    val autoAssignOverdueTo: String? = null,
)

data class NotificationPolicy(
    val enableProtocolExpiry: Boolean = true,
    val protocolExpiryLeadDays: Int = 14,
    val enableOverdueTask: Boolean = true,
    val enableCageCapacity: Boolean = true,
    val enableSyncFailure: Boolean = true,
)

data class NotificationItem(
    val id: String,
    val title: String,
    val content: String,
    val severity: NotificationSeverity,
    val createdAtMillis: Long,
    val entityType: String,
    val entityId: String,
    val readAtMillis: Long? = null,
)

data class AuditEvent(
    val id: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val summary: String,
    val operator: String,
    val createdAtMillis: Long,
    val beforeFields: Map<String, String> = emptyMap(),
    val afterFields: Map<String, String> = emptyMap(),
)

data class LabSnapshot(
    val currentRole: UserRole,
    val rolePermissionOverrides: RolePermissionOverrides = RolePermissionOverrides(),
    val protocols: List<Protocol>,
    val cages: List<Cage>,
    val animals: List<Animal>,
    val breedingPlans: List<BreedingPlan>,
    val samples: List<Sample>,
    val genotypingBatches: List<GenotypingBatch>,
    val genotypingResults: List<GenotypingResult>,
    val cohorts: List<Cohort>,
    val cohortTemplates: List<CohortTemplate> = emptyList(),
    val strainCatalog: List<String> = emptyList(),
    val genotypeCatalog: List<String> = emptyList(),
    val animalAttachments: List<AnimalAttachment> = emptyList(),
    val animalEvents: List<AnimalEvent> = emptyList(),
    val trainingRecords: List<TrainingRecord> = emptyList(),
    val experiments: List<ExperimentSession>,
    val experimentEvents: List<ExperimentEvent>,
    val tasks: List<LabTask>,
    val taskTemplates: List<TaskTemplate> = emptyList(),
    val taskEscalationConfig: TaskEscalationConfig = TaskEscalationConfig(),
    val notificationPolicy: NotificationPolicy = NotificationPolicy(),
    val syncEvents: List<SyncEvent>,
    val auditEvents: List<AuditEvent>,
    val lastGenotypingImportReport: GenotypingImportReport? = null,
)

data class DashboardStats(
    val activeCages: Int,
    val totalAnimals: Int,
    val urgentTasks: Int,
    val overdueTasks: Int,
    val protocolExpiringSoon: Int,
    val pendingSyncCount: Int,
)

data class ReportSummary(
    val cageOccupancyRate: Float,
    val taskCompletionRate: Float,
    val breedingSuccessRate: Float,
    val survivalRate: Float,
    val estimatedCostUsd: Double,
    val activeBreedingPlans: Int,
    val activeCohorts: Int,
    val activeExperiments: Int,
    val overdueTasks: Int,
)

sealed interface RepoResult {
    data object Success : RepoResult
    data class Error(val message: String) : RepoResult
}

fun Animal.ageInWeeks(nowMillis: Long = System.currentTimeMillis()): Int {
    val diff = max(0L, nowMillis - birthAtMillis)
    val oneWeekMillis = 7L * 24L * 60L * 60L * 1000L
    return (diff / oneWeekMillis).toInt()
}

fun LabSnapshot.dashboardStats(nowMillis: Long = System.currentTimeMillis()): DashboardStats {
    val protocolWindow = 14L * 24L * 60L * 60L * 1000L
    val expiringSoonCount = protocols.count { protocol ->
        protocol.isActive && protocol.expiresAtMillis in nowMillis..(nowMillis + protocolWindow)
    }

    return DashboardStats(
        activeCages = cages.count { it.status == CageStatus.Active },
        totalAnimals = animals.count { it.status != AnimalStatus.Dead },
        urgentTasks = tasks.count { it.status == TaskStatus.Todo && it.priority <= TaskPriority.High },
        overdueTasks = tasks.count { it.status == TaskStatus.Overdue },
        protocolExpiringSoon = expiringSoonCount,
        pendingSyncCount = syncEvents.count { it.status == SyncStatus.Pending || it.status == SyncStatus.Failed },
    )
}

fun LabSnapshot.reportSummary(): ReportSummary {
    val activeCages = cages.filter { it.status == CageStatus.Active }
    val occupancy = if (activeCages.isEmpty()) 0f else {
        activeCages.map { it.occupancyRatio }.average().toFloat()
    }

    val done = tasks.count { it.status == TaskStatus.Done }
    val completion = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size.toFloat()
    val weanDone = tasks.count { it.entityType == "breeding" && it.title.contains("断奶") && it.status == TaskStatus.Done }
    val breedingSuccess = if (breedingPlans.isEmpty()) 0f else weanDone.coerceAtMost(breedingPlans.size).toFloat() / breedingPlans.size.toFloat()
    val aliveAnimals = animals.count { it.status != AnimalStatus.Dead }
    val survival = if (animals.isEmpty()) 1f else aliveAnimals.toFloat() / animals.size.toFloat()
    val estimatedCost = aliveAnimals * COST_PER_ACTIVE_ANIMAL_USD_PER_DAY + activeCages.size * COST_PER_ACTIVE_CAGE_USD_PER_DAY

    return ReportSummary(
        cageOccupancyRate = occupancy,
        taskCompletionRate = completion,
        breedingSuccessRate = breedingSuccess,
        survivalRate = survival,
        estimatedCostUsd = estimatedCost,
        activeBreedingPlans = breedingPlans.size,
        activeCohorts = cohorts.count { it.locked },
        activeExperiments = experiments.count { it.status == ExperimentStatus.Active },
        overdueTasks = tasks.count { it.status == TaskStatus.Overdue },
    )
}

fun roleLevel(role: UserRole): Int = when (role) {
    UserRole.Researcher -> 0
    UserRole.PrincipalInvestigator -> 1
    UserRole.Admin -> 2
}

private operator fun TaskPriority.compareTo(other: TaskPriority): Int = rank.compareTo(other.rank)

fun LabTask.overdueDurationMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    if (status != TaskStatus.Overdue && !(status == TaskStatus.Todo && dueAtMillis < nowMillis)) return 0L
    return (nowMillis - dueAtMillis).coerceAtLeast(0L)
}

fun AnimalStatus.canTransitTo(target: AnimalStatus): Boolean {
    if (this == target) return true
    return when (this) {
        AnimalStatus.Active -> target in setOf(AnimalStatus.Breeding, AnimalStatus.InExperiment, AnimalStatus.Retired, AnimalStatus.Dead)
        AnimalStatus.Breeding -> target in setOf(AnimalStatus.Active, AnimalStatus.Retired, AnimalStatus.Dead)
        AnimalStatus.InExperiment -> target in setOf(AnimalStatus.Active, AnimalStatus.Retired, AnimalStatus.Dead)
        AnimalStatus.Retired -> target == AnimalStatus.Dead
        AnimalStatus.Dead -> false
    }
}

private const val COST_PER_ACTIVE_ANIMAL_USD_PER_DAY = 1.25
private const val COST_PER_ACTIVE_CAGE_USD_PER_DAY = 2.50
