package com.westcounty.micemice.data.local

import com.google.gson.Gson
import com.westcounty.micemice.data.model.LabSnapshot

class SnapshotStore(
    private val dao: AppStateDao,
    private val gson: Gson = Gson(),
) {
    suspend fun load(): LabSnapshot? {
        val state = dao.getState() ?: return null
        val parsed = runCatching { gson.fromJson(state.payload, LabSnapshot::class.java) }.getOrNull() ?: return null
        return if (isSnapshotUsable(parsed)) parsed else null
    }

    suspend fun save(snapshot: LabSnapshot) {
        val payload = gson.toJson(snapshot)
        dao.upsertState(
            AppStateEntity(
                id = 1,
                payload = payload,
                updatedAtMillis = System.currentTimeMillis(),
            )
        )
    }

    private fun isSnapshotUsable(snapshot: LabSnapshot): Boolean {
        return runCatching {
            snapshot.currentRole.name
            snapshot.rolePermissionOverrides.researcherDenied.size
            snapshot.protocols.size
            snapshot.cages.size
            snapshot.animals.size
            snapshot.breedingPlans.size
            snapshot.samples.size
            snapshot.genotypingBatches.size
            snapshot.genotypingResults.size
            snapshot.cohorts.size
            snapshot.cohortTemplates.size
            snapshot.strainCatalog.size
            snapshot.genotypeCatalog.size
            snapshot.animalAttachments.size
            snapshot.animalEvents.size
            snapshot.trainingRecords.size
            snapshot.experiments.size
            snapshot.experimentEvents.size
            snapshot.tasks.size
            snapshot.taskTemplates.size
            snapshot.tasks.forEach { task ->
                task.assignee.length
                task.title.length
            }
            snapshot.taskEscalationConfig.enable24hEscalation
            snapshot.taskEscalationConfig.priorityAt24h.name
            snapshot.syncEvents.size
            snapshot.auditEvents.size
        }.isSuccess
    }
}
