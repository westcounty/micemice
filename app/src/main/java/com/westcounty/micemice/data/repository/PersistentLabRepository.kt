package com.westcounty.micemice.data.repository

import com.westcounty.micemice.data.local.SnapshotStore
import com.westcounty.micemice.data.model.AnimalSex
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.NotificationPolicy
import com.westcounty.micemice.data.model.PermissionKey
import com.westcounty.micemice.data.model.RepoResult
import com.westcounty.micemice.data.model.SampleType
import com.westcounty.micemice.data.model.TaskEscalationConfig
import com.westcounty.micemice.data.model.TaskPriority
import com.westcounty.micemice.data.model.TrainingRecord
import com.westcounty.micemice.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PersistentLabRepository(
    private val delegate: InMemoryLabRepository,
    private val store: SnapshotStore,
) : LabRepository {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val snapshot: StateFlow<LabSnapshot> = delegate.snapshot

    override fun switchRole(role: UserRole): RepoResult = persist(delegate.switchRole(role))

    override fun completeTask(taskId: String, operator: String): RepoResult = persist(delegate.completeTask(taskId, operator))

    override fun saveTaskTemplate(
        name: String,
        detail: String,
        defaultPriority: TaskPriority,
        dueInHours: Int,
        entityType: String,
        operator: String,
    ): RepoResult = persist(delegate.saveTaskTemplate(name, detail, defaultPriority, dueInHours, entityType, operator))

    override fun deleteTaskTemplate(templateId: String, operator: String): RepoResult =
        persist(delegate.deleteTaskTemplate(templateId, operator))

    override fun createTaskFromTemplate(templateId: String, assignee: String?, operator: String): RepoResult =
        persist(delegate.createTaskFromTemplate(templateId, assignee, operator))

    override fun reassignTask(taskId: String, assignee: String, operator: String): RepoResult =
        persist(delegate.reassignTask(taskId, assignee, operator))

    override fun reassignTasks(taskIds: List<String>, assignee: String, operator: String): RepoResult =
        persist(delegate.reassignTasks(taskIds, assignee, operator))

    override fun setTaskEscalationConfig(config: TaskEscalationConfig, operator: String): RepoResult =
        persist(delegate.setTaskEscalationConfig(config, operator))

    override fun applyTaskEscalation(operator: String): RepoResult =
        persist(delegate.applyTaskEscalation(operator))

    override fun moveAnimals(animalIds: List<String>, targetCageId: String, operator: String): RepoResult =
        persist(delegate.moveAnimals(animalIds, targetCageId, operator))

    override fun mergeCages(sourceCageId: String, targetCageId: String, operator: String): RepoResult =
        persist(delegate.mergeCages(sourceCageId, targetCageId, operator))

    override fun splitCage(
        sourceCageId: String,
        newCageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        animalIds: List<String>,
        operator: String,
    ): RepoResult = persist(
        delegate.splitCage(
            sourceCageId = sourceCageId,
            newCageId = newCageId,
            roomCode = roomCode,
            rackCode = rackCode,
            slotCode = slotCode,
            capacityLimit = capacityLimit,
            animalIds = animalIds,
            operator = operator,
        )
    )

    override fun createCage(
        cageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        operator: String,
    ): RepoResult = persist(delegate.createCage(cageId, roomCode, rackCode, slotCode, capacityLimit, operator))

    override fun createAnimal(
        identifier: String,
        sex: AnimalSex,
        strain: String,
        genotype: String,
        cageId: String,
        protocolId: String?,
        operator: String,
    ): RepoResult = persist(delegate.createAnimal(identifier, sex, strain, genotype, cageId, protocolId, operator))

    override fun addStrainToCatalog(strain: String, operator: String): RepoResult =
        persist(delegate.addStrainToCatalog(strain, operator))

    override fun removeStrainFromCatalog(strain: String, operator: String): RepoResult =
        persist(delegate.removeStrainFromCatalog(strain, operator))

    override fun addGenotypeToCatalog(genotype: String, operator: String): RepoResult =
        persist(delegate.addGenotypeToCatalog(genotype, operator))

    override fun removeGenotypeFromCatalog(genotype: String, operator: String): RepoResult =
        persist(delegate.removeGenotypeFromCatalog(genotype, operator))

    override fun createBreedingPlan(
        maleId: String,
        femaleId: String,
        protocolId: String?,
        operator: String,
        notes: String,
    ): RepoResult = persist(delegate.createBreedingPlan(maleId, femaleId, protocolId, operator, notes))

    override fun recordBirth(planId: String, pupCount: Int, strain: String?, genotype: String?, operator: String): RepoResult =
        persist(delegate.recordBirth(planId, pupCount, strain, genotype, operator))

    override fun recordPlugCheck(planId: String, positive: Boolean, operator: String): RepoResult =
        persist(delegate.recordPlugCheck(planId, positive, operator))

    override fun completeWeaning(planId: String, operator: String): RepoResult =
        persist(delegate.completeWeaning(planId, operator))

    override fun reopenWeaning(planId: String, operator: String): RepoResult =
        persist(delegate.reopenWeaning(planId, operator))

    override fun registerSample(animalId: String, sampleType: SampleType, operator: String): RepoResult =
        persist(delegate.registerSample(animalId, sampleType, operator))

    override fun createGenotypingBatch(name: String, sampleIds: List<String>, operator: String): RepoResult =
        persist(delegate.createGenotypingBatch(name, sampleIds, operator))

    override fun importGenotypingResults(batchId: String, csvText: String, reviewer: String, operator: String): RepoResult =
        persist(delegate.importGenotypingResults(batchId, csvText, reviewer, operator))

    override fun confirmGenotypingResult(resultId: String, operator: String): RepoResult =
        persist(delegate.confirmGenotypingResult(resultId, operator))

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
    ): RepoResult = persist(
        delegate.createCohort(
            name = name,
            strain = strain,
            genotype = genotype,
            sex = sex,
            minWeeks = minWeeks,
            maxWeeks = maxWeeks,
            blindCodingEnabled = blindCodingEnabled,
            blindCodePrefix = blindCodePrefix,
            operator = operator,
        )
    )

    override fun saveCohortTemplate(
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        operator: String,
    ): RepoResult = persist(delegate.saveCohortTemplate(name, strain, genotype, sex, minWeeks, maxWeeks, operator))

    override fun updateCohortTemplate(
        templateId: String,
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        operator: String,
    ): RepoResult = persist(delegate.updateCohortTemplate(templateId, name, strain, genotype, sex, minWeeks, maxWeeks, operator))

    override fun deleteCohortTemplate(templateId: String, operator: String): RepoResult =
        persist(delegate.deleteCohortTemplate(templateId, operator))

    override fun applyCohortTemplate(templateId: String, operator: String): RepoResult =
        persist(delegate.applyCohortTemplate(templateId, operator))

    override fun createExperiment(cohortId: String, title: String, operator: String): RepoResult =
        persist(delegate.createExperiment(cohortId, title, operator))

    override fun addExperimentEvent(experimentId: String, eventType: String, note: String, operator: String): RepoResult =
        persist(delegate.addExperimentEvent(experimentId, eventType, note, operator))

    override fun archiveExperiment(experimentId: String, operator: String): RepoResult =
        persist(delegate.archiveExperiment(experimentId, operator))

    override fun importAnimalsCsv(csvText: String, operator: String): RepoResult =
        persist(delegate.importAnimalsCsv(csvText, operator))

    override fun exportAnimalsCsv(): String = delegate.exportAnimalsCsv()

    override fun exportComplianceCsv(): String = delegate.exportComplianceCsv()

    override fun exportCohortBlindCsv(cohortId: String): String = delegate.exportCohortBlindCsv(cohortId)

    override fun retrySyncEvent(syncEventId: String, operator: String): RepoResult =
        persist(delegate.retrySyncEvent(syncEventId, operator))

    override fun syncPendingEvents(operator: String): RepoResult = persist(delegate.syncPendingEvents(operator))

    override fun updateAnimalStatus(animalId: String, status: AnimalStatus, operator: String): RepoResult =
        persist(delegate.updateAnimalStatus(animalId, status, operator))

    override fun addAnimalEvent(animalId: String, eventType: String, note: String, weightGram: Float?, operator: String): RepoResult =
        persist(delegate.addAnimalEvent(animalId, eventType, note, weightGram, operator))

    override fun addAnimalAttachment(animalId: String, label: String, filePath: String, operator: String): RepoResult =
        persist(delegate.addAnimalAttachment(animalId, label, filePath, operator))

    override fun setProtocolState(protocolId: String, isActive: Boolean, operator: String): RepoResult =
        persist(delegate.setProtocolState(protocolId, isActive, operator))

    override fun upsertTrainingRecord(record: TrainingRecord, operator: String): RepoResult =
        persist(delegate.upsertTrainingRecord(record, operator))

    override fun removeTrainingRecord(username: String, operator: String): RepoResult =
        persist(delegate.removeTrainingRecord(username, operator))

    override fun setNotificationPolicy(policy: NotificationPolicy, operator: String): RepoResult =
        persist(delegate.setNotificationPolicy(policy, operator))

    override fun setRolePermission(role: UserRole, permission: PermissionKey, enabled: Boolean, operator: String): RepoResult =
        persist(delegate.setRolePermission(role, permission, enabled, operator))

    private fun persist(result: RepoResult): RepoResult {
        if (result is RepoResult.Success) {
            ioScope.launch {
                runCatching { store.save(delegate.snapshot.value) }
            }
        }
        return result
    }
}
