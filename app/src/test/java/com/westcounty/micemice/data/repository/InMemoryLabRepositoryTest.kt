package com.westcounty.micemice.data.repository

import com.westcounty.micemice.data.model.AnimalSex
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.CageStatus
import com.westcounty.micemice.data.model.ExperimentStatus
import com.westcounty.micemice.data.model.NotificationPolicy
import com.westcounty.micemice.data.model.PermissionKey
import com.westcounty.micemice.data.model.RepoResult
import com.westcounty.micemice.data.model.SampleType
import com.westcounty.micemice.data.model.SyncStatus
import com.westcounty.micemice.data.model.TaskEscalationConfig
import com.westcounty.micemice.data.model.TaskPriority
import com.westcounty.micemice.data.model.TaskStatus
import com.westcounty.micemice.data.model.TrainingRecord
import com.westcounty.micemice.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryLabRepositoryTest {

    @Test
    fun moveAnimals_updatesAnimalAndCageState() {
        val repo = InMemoryLabRepository.createForTesting()

        val result = repo.moveAnimals(
            animalIds = listOf("A001"),
            targetCageId = "C-103",
            operator = "tester",
        )

        assertTrue(result is RepoResult.Success)

        val snapshot = repo.snapshot.value
        val movedAnimal = snapshot.animals.first { it.id == "A001" }
        val targetCage = snapshot.cages.first { it.id == "C-103" }
        val sourceCage = snapshot.cages.first { it.id == "C-101" }

        assertEquals("C-103", movedAnimal.cageId)
        assertTrue(targetCage.animalIds.contains("A001"))
        assertTrue(sourceCage.animalIds.contains("A001").not())
    }

    @Test
    fun moveAnimals_blocksWhenProtocolDisabled() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.setProtocolState("P-NEURO-2026-001", false, "tester")

        val result = repo.moveAnimals(
            animalIds = listOf("A001"),
            targetCageId = "C-103",
            operator = "tester",
        )

        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun moveAnimals_rejectsWhenTargetCageIsOverCapacity() {
        val repo = InMemoryLabRepository.createForTesting()

        val result = repo.moveAnimals(
            animalIds = listOf("A001", "A002", "A003"),
            targetCageId = "C-103",
            operator = "tester",
        )

        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun createBreedingPlan_createsPlanAndTasks() {
        val repo = InMemoryLabRepository.createForTesting()

        val beforePlans = repo.snapshot.value.breedingPlans.size
        val beforeTasks = repo.snapshot.value.tasks.size

        val result = repo.createBreedingPlan(
            maleId = "A001",
            femaleId = "A002",
            protocolId = "P-NEURO-2026-001",
            operator = "tester",
            notes = "unit-test"
        )

        assertTrue(result is RepoResult.Success)

        val snapshot = repo.snapshot.value
        assertEquals(beforePlans + 1, snapshot.breedingPlans.size)
        assertEquals(beforeTasks + 2, snapshot.tasks.size)
        assertTrue(snapshot.tasks.any { it.status == TaskStatus.Todo && it.title == "断奶分笼" })
    }

    @Test
    fun plugCheckAndWeaning_completeLifecycle() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.createBreedingPlan("A001", "A002", "P-NEURO-2026-001", "tester", "")
        val plan = repo.snapshot.value.breedingPlans.first()

        val plugResult = repo.recordPlugCheck(plan.id, positive = true, operator = "tester")
        assertTrue(plugResult is RepoResult.Success)

        val weanResult = repo.completeWeaning(plan.id, operator = "tester")
        assertTrue(weanResult is RepoResult.Success)

        val refreshed = repo.snapshot.value.breedingPlans.first { it.id == plan.id }
        assertTrue(refreshed.plugCheckedAtMillis != null)
        assertTrue(refreshed.weanedAtMillis != null)
        assertTrue(repo.snapshot.value.tasks.any { it.entityId == plan.id && it.title.contains("断奶") && it.status == TaskStatus.Done })
    }

    @Test
    fun reopenWeaning_restoresPendingState() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.createBreedingPlan("A001", "A002", "P-NEURO-2026-001", "tester", "")
        val plan = repo.snapshot.value.breedingPlans.first()
        repo.recordPlugCheck(plan.id, positive = true, operator = "tester")
        repo.completeWeaning(plan.id, operator = "tester")

        val result = repo.reopenWeaning(plan.id, operator = "tester")
        assertTrue(result is RepoResult.Success)

        val refreshed = repo.snapshot.value.breedingPlans.first { it.id == plan.id }
        assertTrue(refreshed.weanedAtMillis == null)
        assertTrue(repo.snapshot.value.tasks.any { it.entityId == plan.id && it.title.contains("断奶") && it.status == TaskStatus.Todo })
    }

    @Test
    fun createBreedingPlan_blocksExpiredProtocol() {
        val repo = InMemoryLabRepository.createForTesting()

        val result = repo.createBreedingPlan(
            maleId = "A001",
            femaleId = "A002",
            protocolId = "P-METAB-2025-011",
            operator = "tester",
            notes = "should-fail",
        )

        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun createBreedingPlan_blocksWhenTrainingInvalid() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.upsertTrainingRecord(
            TrainingRecord(
                username = "tester",
                expiresAtMillis = System.currentTimeMillis() - 1000L,
                isActive = false,
                note = "expired",
            ),
            operator = "tester",
        )

        val result = repo.createBreedingPlan(
            maleId = "A001",
            femaleId = "A002",
            protocolId = "P-NEURO-2026-001",
            operator = "tester",
            notes = "should-fail",
        )

        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun createCageAndAnimal_success() {
        val repo = InMemoryLabRepository.createForTesting()
        val createCage = repo.createCage("C-999", "A3", "R9", "01", 4, "tester")
        assertTrue(createCage is RepoResult.Success)

        val createAnimal = repo.createAnimal(
            identifier = "UT-NEW-01",
            sex = AnimalSex.Male,
            strain = "C57BL/6J",
            genotype = "+/+",
            cageId = "C-999",
            protocolId = "P-IMMUNE-2026-003",
            operator = "tester",
        )
        assertTrue(createAnimal is RepoResult.Success)
        assertTrue(repo.snapshot.value.animals.any { it.identifier == "UT-NEW-01" })
        assertTrue(repo.snapshot.value.cages.first { it.id == "C-999" }.animalIds.isNotEmpty())
    }

    @Test
    fun createAnimal_rejectsUnknownMasterData() {
        val repo = InMemoryLabRepository.createForTesting()
        val result = repo.createAnimal(
            identifier = "UT-INVALID-01",
            sex = AnimalSex.Male,
            strain = "UNKNOWN_STRAIN",
            genotype = "UNKNOWN_GENO",
            cageId = "C-101",
            protocolId = "P-NEURO-2026-001",
            operator = "tester",
        )
        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun breeding_recordBirth_createsPupsWithLineage() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.createBreedingPlan("A001", "A002", "P-NEURO-2026-001", "tester", "")
        val plan = repo.snapshot.value.breedingPlans.first { it.maleId == "A001" && it.femaleId == "A002" }

        val result = repo.recordBirth(
            planId = plan.id,
            pupCount = 2,
            strain = "C57BL/6J",
            genotype = "+/+",
            operator = "tester",
        )

        assertTrue(result is RepoResult.Success)
        val pups = repo.snapshot.value.animals.filter { it.fatherId == "A001" && it.motherId == "A002" }
        assertEquals(2, pups.size)
    }

    @Test
    fun addAnimalAttachment_success() {
        val repo = InMemoryLabRepository.createForTesting()
        val result = repo.addAnimalAttachment(
            animalId = "A001",
            label = "病理报告",
            filePath = "/tmp/A001_report.pdf",
            operator = "tester",
        )
        assertTrue(result is RepoResult.Success)
        assertTrue(repo.snapshot.value.animalAttachments.any { it.animalId == "A001" && it.label == "病理报告" })
    }

    @Test
    fun addAnimalEvent_success() {
        val repo = InMemoryLabRepository.createForTesting()
        val beforeCount = repo.snapshot.value.animalEvents.size
        val result = repo.addAnimalEvent(
            animalId = "A001",
            eventType = "weight",
            note = "周检称重",
            weightGram = 22.4f,
            operator = "tester",
        )
        assertTrue(result is RepoResult.Success)
        val snapshot = repo.snapshot.value
        assertEquals(beforeCount + 1, snapshot.animalEvents.size)
        val latest = snapshot.animalEvents.first()
        assertEquals("A001", latest.animalId)
        assertEquals("weight", latest.eventType)
        assertEquals(22.4f, latest.weightGram)
    }

    @Test
    fun addAnimalEvent_rejectsMissingAnimal() {
        val repo = InMemoryLabRepository.createForTesting()
        val result = repo.addAnimalEvent(
            animalId = "A9999",
            eventType = "health",
            note = "invalid",
            weightGram = null,
            operator = "tester",
        )
        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun mergeCages_movesAnimalsAndClosesSource() {
        val repo = InMemoryLabRepository.createForTesting()
        val sourceCount = repo.snapshot.value.cages.first { it.id == "C-103" }.animalIds.size

        val result = repo.mergeCages("C-103", "C-101", "tester")

        assertTrue(result is RepoResult.Success)
        val snapshot = repo.snapshot.value
        val source = snapshot.cages.first { it.id == "C-103" }
        val target = snapshot.cages.first { it.id == "C-101" }
        assertTrue(source.animalIds.isEmpty())
        assertTrue(source.status == CageStatus.Closed)
        assertEquals(3 + sourceCount, target.animalIds.size)
    }

    @Test
    fun splitCage_createsNewCageAndReassignsAnimals() {
        val repo = InMemoryLabRepository.createForTesting()

        val result = repo.splitCage(
            sourceCageId = "C-101",
            newCageId = "C-201",
            roomCode = "A1",
            rackCode = "R1",
            slotCode = "09",
            capacityLimit = 3,
            animalIds = listOf("A001"),
            operator = "tester",
        )

        assertTrue(result is RepoResult.Success)
        val snapshot = repo.snapshot.value
        val newCage = snapshot.cages.first { it.id == "C-201" }
        assertTrue(newCage.animalIds.contains("A001"))
        assertEquals("C-201", snapshot.animals.first { it.id == "A001" }.cageId)
    }

    @Test
    fun createCohort_createsLockedGroupWhenMatched() {
        val repo = InMemoryLabRepository.createForTesting()

        val beforeCount = repo.snapshot.value.cohorts.size
        val result = repo.createCohort(
            name = "Cohort-UT",
            strain = "C57BL/6J",
            genotype = "+/-",
            sex = AnimalSex.Female,
            minWeeks = 5,
            maxWeeks = 30,
            blindCodingEnabled = false,
            blindCodePrefix = null,
            operator = "tester",
        )

        assertTrue(result is RepoResult.Success)
        val snapshot = repo.snapshot.value
        assertEquals(beforeCount + 1, snapshot.cohorts.size)
        assertTrue(snapshot.cohorts.first().locked)
    }

    @Test
    fun createCohort_withBlindCoding_generatesBlindCodes() {
        val repo = InMemoryLabRepository.createForTesting()
        val result = repo.createCohort(
            name = "Blind-UT",
            strain = "C57BL/6J",
            genotype = null,
            sex = null,
            minWeeks = null,
            maxWeeks = null,
            blindCodingEnabled = true,
            blindCodePrefix = "UT",
            operator = "tester",
        )

        assertTrue(result is RepoResult.Success)
        val cohort = repo.snapshot.value.cohorts.first { it.name == "Blind-UT" }
        assertTrue(cohort.blindCodes.isNotEmpty())
        assertTrue(cohort.blindCodes.values.all { it.startsWith("UT-") })
    }

    @Test
    fun cohortTemplate_saveAndApply_updatesUsageCount() {
        val repo = InMemoryLabRepository.createForTesting()

        val save = repo.saveCohortTemplate(
            name = "UT-Template",
            strain = "C57BL/6J",
            genotype = "+/-",
            sex = AnimalSex.Female,
            minWeeks = 8,
            maxWeeks = 12,
            operator = "tester",
        )
        assertTrue(save is RepoResult.Success)

        val template = repo.snapshot.value.cohortTemplates.first { it.name == "UT-Template" }
        val apply = repo.applyCohortTemplate(template.id, "tester")
        assertTrue(apply is RepoResult.Success)
        val refreshed = repo.snapshot.value.cohortTemplates.first { it.id == template.id }
        assertEquals(template.usageCount + 1, refreshed.usageCount)
    }

    @Test
    fun cohortTemplate_updateAndDelete_success() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.saveCohortTemplate("UT-T1", "C57BL/6J", "+/-", AnimalSex.Female, 8, 12, "tester")
        val template = repo.snapshot.value.cohortTemplates.first { it.name == "UT-T1" }

        val update = repo.updateCohortTemplate(
            templateId = template.id,
            name = "UT-T1-NEW",
            strain = "BALB/c",
            genotype = "-/+",
            sex = AnimalSex.Female,
            minWeeks = 9,
            maxWeeks = 13,
            operator = "tester",
        )
        assertTrue(update is RepoResult.Success)
        assertTrue(repo.snapshot.value.cohortTemplates.any { it.id == template.id && it.name == "UT-T1-NEW" })

        val delete = repo.deleteCohortTemplate(template.id, "tester")
        assertTrue(delete is RepoResult.Success)
        assertTrue(repo.snapshot.value.cohortTemplates.none { it.id == template.id })
    }

    @Test
    fun genotypingImport_marksConflictWhenCallChanged() {
        val repo = InMemoryLabRepository.createForTesting()

        val csv = "sample_id,marker,call\nSMP-1001,GeneX,-/-"
        val result = repo.importGenotypingResults(
            batchId = "GBT-1001",
            csvText = csv,
            reviewer = "tester",
            operator = "tester",
        )

        assertTrue(result is RepoResult.Success)
        val latest = repo.snapshot.value.genotypingResults.first()
        assertTrue(latest.conflict)
        assertTrue(latest.confirmed.not())
    }

    @Test
    fun genotypingImport_returnsRowLevelIssuesAndRetryCsv() {
        val repo = InMemoryLabRepository.createForTesting()

        val csv = """
            sample_id,marker,call
            SMP-1001,GeneX,+/-
            INVALID_SAMPLE,GeneY,+/+
            ,GeneZ,-/-
        """.trimIndent()
        val result = repo.importGenotypingResults(
            batchId = "GBT-1001",
            csvText = csv,
            reviewer = "tester",
            operator = "tester",
        )

        assertTrue(result is RepoResult.Error)
        val report = repo.snapshot.value.lastGenotypingImportReport
        assertTrue(report != null)
        assertTrue((report?.importedCount ?: 0) >= 1)
        assertTrue((report?.failedCount ?: 0) >= 1)
        assertTrue((report?.failedRowsCsv ?: "").contains("sample_id,marker,call"))
    }

    @Test
    fun registerSample_andCreateBatch_success() {
        val repo = InMemoryLabRepository.createForTesting()

        val registerResult = repo.registerSample("A001", SampleType.Ear, "tester")
        assertTrue(registerResult is RepoResult.Success)

        val sampleId = repo.snapshot.value.samples.first().id
        val batchResult = repo.createGenotypingBatch("UT-Batch", listOf(sampleId), "tester")
        assertTrue(batchResult is RepoResult.Success)
        assertTrue(repo.snapshot.value.genotypingBatches.any { it.name == "UT-Batch" })
        assertTrue(repo.snapshot.value.samples.first { it.id == sampleId }.platePosition != null)
    }

    @Test
    fun experimentLifecycle_createAndArchive() {
        val repo = InMemoryLabRepository.createForTesting()

        val createResult = repo.createExperiment("COH-1501", "UT-Exp", "tester")
        assertTrue(createResult is RepoResult.Success)

        val exp = repo.snapshot.value.experiments.first()
        val addEventResult = repo.addExperimentEvent(exp.id, "dose", "note", "tester")
        assertTrue(addEventResult is RepoResult.Success)

        val archiveResult = repo.archiveExperiment(exp.id, "tester")
        assertTrue(archiveResult is RepoResult.Success)
        assertTrue(repo.snapshot.value.experiments.first { it.id == exp.id }.status == ExperimentStatus.Archived)
    }

    @Test
    fun syncPendingEvents_marksQueueAsSynced() {
        val repo = InMemoryLabRepository.createForTesting()

        val result = repo.syncPendingEvents(operator = "tester")

        assertTrue(result is RepoResult.Success)
        assertTrue(
            repo.snapshot.value.syncEvents.none {
                it.status == SyncStatus.Pending || it.status == SyncStatus.Failed
            }
        )
    }

    @Test
    fun taskReassignAndEscalation_applyRules() {
        val repo = InMemoryLabRepository.createForTesting()
        val overdueTask = repo.snapshot.value.tasks.first { it.status == TaskStatus.Overdue }

        val assign = repo.reassignTask(overdueTask.id, "Alice", "tester")
        assertTrue(assign is RepoResult.Success)
        assertEquals("Alice", repo.snapshot.value.tasks.first { it.id == overdueTask.id }.assignee)

        val config = TaskEscalationConfig(
            enable24hEscalation = true,
            enable48hEscalation = true,
            priorityAt24h = TaskPriority.High,
            priorityAt48h = TaskPriority.Critical,
            autoAssignOverdueTo = "DutyAdmin",
        )
        val setConfig = repo.setTaskEscalationConfig(config, "tester")
        assertTrue(setConfig is RepoResult.Success)

        val apply = repo.applyTaskEscalation("tester")
        assertTrue(apply is RepoResult.Success)
        val refreshed = repo.snapshot.value.tasks.first { it.id == overdueTask.id }
        assertEquals("DutyAdmin", refreshed.assignee)
        assertTrue(refreshed.status == TaskStatus.Overdue)
    }

    @Test
    fun taskTemplate_saveAndCreateTask_success() {
        val repo = InMemoryLabRepository.createForTesting()
        val save = repo.saveTaskTemplate(
            name = "UT-Template",
            detail = "模板任务测试",
            defaultPriority = TaskPriority.High,
            dueInHours = 6,
            entityType = "system",
            operator = "tester",
        )
        assertTrue(save is RepoResult.Success)
        val template = repo.snapshot.value.taskTemplates.first { it.name == "UT-Template" }

        val create = repo.createTaskFromTemplate(template.id, "Alice", "tester")
        assertTrue(create is RepoResult.Success)
        val createdTask = repo.snapshot.value.tasks.first()
        assertEquals("UT-Template", createdTask.title)
        assertEquals("Alice", createdTask.assignee)
    }

    @Test
    fun retrySyncEvent_movesFailedToPending() {
        val repo = InMemoryLabRepository.createForTesting()
        val failed = repo.snapshot.value.syncEvents.first { it.status == SyncStatus.Failed }

        val result = repo.retrySyncEvent(failed.id, "tester")

        assertTrue(result is RepoResult.Success)
        val refreshed = repo.snapshot.value.syncEvents.first { it.id == failed.id }
        assertTrue(refreshed.status == SyncStatus.Pending)
    }

    @Test
    fun setNotificationPolicy_rejectsInvalidLeadDays() {
        val repo = InMemoryLabRepository.createForTesting()
        val result = repo.setNotificationPolicy(
            NotificationPolicy(protocolExpiryLeadDays = 0),
            "tester",
        )
        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun importAndExportCsv_roundTripWorks() {
        val repo = InMemoryLabRepository.createForTesting()
        val importCsv = "identifier,sex,strain,genotype,cage_id,protocol_id\nUT001,male,C57BL/6J,+/+,C-101,P-NEURO-2026-001"

        val importResult = repo.importAnimalsCsv(importCsv, "tester")
        assertTrue(importResult is RepoResult.Success)
        assertTrue(repo.snapshot.value.animals.any { it.identifier == "UT001" })

        val exported = repo.exportAnimalsCsv()
        val compliance = repo.exportComplianceCsv()
        assertTrue(exported.contains("UT001"))
        assertTrue(compliance.contains("audit_id"))
    }

    @Test
    fun updateAnimalStatus_blocksIllegalTransitionFromRetiredToActive() {
        val repo = InMemoryLabRepository.createForTesting()

        val result = repo.updateAnimalStatus(
            animalId = "A012",
            status = AnimalStatus.Active,
            operator = "tester",
        )

        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun updateAnimalStatus_allowsActiveToInExperiment() {
        val repo = InMemoryLabRepository.createForTesting()

        val result = repo.updateAnimalStatus(
            animalId = "A001",
            status = AnimalStatus.InExperiment,
            operator = "tester",
        )

        assertTrue(result is RepoResult.Success)
        val refreshed = repo.snapshot.value.animals.first { it.id == "A001" }
        assertTrue(refreshed.status == AnimalStatus.InExperiment)
    }

    @Test
    fun updateAnimalStatus_blocksCriticalStateWithoutProtocol() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.createCage("C-998", "A1", "R1", "98", 5, "tester")
        repo.createAnimal(
            identifier = "NO-PRO-01",
            sex = AnimalSex.Female,
            strain = "C57BL/6J",
            genotype = "+/+",
            cageId = "C-998",
            protocolId = null,
            operator = "tester",
        )
        val target = repo.snapshot.value.animals.first { it.identifier == "NO-PRO-01" }
        val result = repo.updateAnimalStatus(target.id, AnimalStatus.InExperiment, "tester")
        assertTrue(result is RepoResult.Error)
    }

    @Test
    fun setRolePermission_updatesOverrides() {
        val repo = InMemoryLabRepository.createForTesting()
        val disable = repo.setRolePermission(
            role = UserRole.Researcher,
            permission = PermissionKey.BreedingWrite,
            enabled = false,
            operator = "tester",
        )
        assertTrue(disable is RepoResult.Success)
        assertTrue(
            repo.snapshot.value.rolePermissionOverrides
                .isGranted(UserRole.Researcher, PermissionKey.BreedingWrite)
                .not(),
        )

        val enable = repo.setRolePermission(
            role = UserRole.Researcher,
            permission = PermissionKey.BreedingWrite,
            enabled = true,
            operator = "tester",
        )
        assertTrue(enable is RepoResult.Success)
        assertTrue(repo.snapshot.value.rolePermissionOverrides.isGranted(UserRole.Researcher, PermissionKey.BreedingWrite))
    }

    @Test
    fun exportComplianceCsv_containsBeforeAfterColumns() {
        val repo = InMemoryLabRepository.createForTesting()
        repo.setProtocolState("P-NEURO-2026-001", false, "tester")

        val csv = repo.exportComplianceCsv()
        assertTrue(csv.contains("before_fields"))
        assertTrue(csv.contains("after_fields"))
        assertTrue(csv.contains("isActive"))
    }
}
