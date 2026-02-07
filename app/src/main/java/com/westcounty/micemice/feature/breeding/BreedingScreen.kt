package com.westcounty.micemice.feature.breeding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.AnimalSex
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.CageStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDate

@Composable
fun BreedingScreen(
    snapshot: LabSnapshot,
    onCreatePlan: (maleId: String, femaleId: String, protocolId: String?, notes: String) -> Unit,
    onRecordBirth: (planId: String, pupCount: Int, strain: String?, genotype: String?) -> Unit,
    onRecordPlugCheck: (planId: String, positive: Boolean) -> Unit,
    onCompleteWeaning: (planId: String) -> Unit,
    onRunWeaningWizard: (planId: String, animalIds: List<String>, targetCageIds: List<String>) -> Unit,
    onUndoWeaningWizard: () -> Unit,
    canUndoWeaningWizard: Boolean,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val dayMillis = 24L * 60L * 60L * 1000L
    var selectedMaleId by remember { mutableStateOf<String?>(null) }
    var selectedFemaleId by remember { mutableStateOf<String?>(null) }
    var selectedProtocolId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    val pendingWeaningPlans = snapshot.breedingPlans.filter { it.weanedAtMillis == null }
    var selectedBirthPlanId by remember { mutableStateOf(pendingWeaningPlans.firstOrNull()?.id) }
    var birthCountText by remember { mutableStateOf("6") }
    var birthStrain by remember { mutableStateOf(snapshot.strainCatalog.firstOrNull().orEmpty()) }
    var birthGenotype by remember { mutableStateOf(snapshot.genotypeCatalog.firstOrNull().orEmpty()) }
    var selectedWizardPlanId by remember { mutableStateOf(pendingWeaningPlans.firstOrNull()?.id) }
    var selectedPupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTargetCages by remember { mutableStateOf<Set<String>>(emptySet()) }

    val maleCandidates = snapshot.animals.filter { it.sex == AnimalSex.Male && it.status != AnimalStatus.Dead }
    val femaleCandidates = snapshot.animals.filter { it.sex == AnimalSex.Female && it.status != AnimalStatus.Dead }
    val inbreedingRisk = inferInbreedingRisk(snapshot, selectedMaleId, selectedFemaleId)
    val selectedWizardPlan = pendingWeaningPlans.firstOrNull { it.id == selectedWizardPlanId }
    val sourceCageId = selectedWizardPlan
        ?.femaleId
        ?.let { femaleId -> snapshot.animals.firstOrNull { it.id == femaleId }?.cageId }
    val sourcePups = if (selectedWizardPlan == null || sourceCageId == null) {
        emptyList()
    } else {
        snapshot.animals.filter { animal ->
            animal.cageId == sourceCageId &&
                animal.id != selectedWizardPlan.maleId &&
                animal.id != selectedWizardPlan.femaleId &&
                animal.status == AnimalStatus.Active
        }
    }
    val targetCages = snapshot.cages.filter { it.status == CageStatus.Active && it.id != sourceCageId }
    val calendarItems = snapshot.breedingPlans
        .flatMap { plan ->
            val expectedBirthAtMillis = plan.matingAtMillis + 19L * dayMillis
            listOf(
                BreedingCalendarItem(
                    planId = plan.id,
                    nodeType = "查栓",
                    dueAtMillis = plan.expectedPlugCheckAtMillis,
                    completed = plan.plugCheckedAtMillis != null,
                ),
                BreedingCalendarItem(
                    planId = plan.id,
                    nodeType = "预产",
                    dueAtMillis = expectedBirthAtMillis,
                    completed = plan.weanedAtMillis != null || plan.plugPositive == false,
                ),
                BreedingCalendarItem(
                    planId = plan.id,
                    nodeType = "断奶",
                    dueAtMillis = plan.expectedWeanAtMillis,
                    completed = plan.weanedAtMillis != null,
                ),
            )
        }
        .filter { item ->
            val withinHorizon = item.dueAtMillis in (now - 7L * dayMillis)..(now + 21L * dayMillis)
            withinHorizon || !item.completed
        }
        .sortedBy { it.dueAtMillis }

    LaunchedEffect(pendingWeaningPlans.map { it.id }) {
        if (selectedWizardPlanId == null || pendingWeaningPlans.none { it.id == selectedWizardPlanId }) {
            selectedWizardPlanId = pendingWeaningPlans.firstOrNull()?.id
        }
        if (selectedBirthPlanId == null || pendingWeaningPlans.none { it.id == selectedBirthPlanId }) {
            selectedBirthPlanId = pendingWeaningPlans.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedWizardPlanId) {
        selectedPupIds = emptySet()
        selectedTargetCages = emptySet()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "繁育计划",
                subtitle = "向导式配对并自动生成查栓/断奶任务",
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("1) 选择雄鼠", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(maleCandidates, key = { it.id }) { animal ->
                            FilterChip(
                                selected = selectedMaleId == animal.id,
                                onClick = { selectedMaleId = animal.id },
                                label = { Text(animal.identifier) },
                            )
                        }
                    }

                    Text("2) 选择雌鼠", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(femaleCandidates, key = { it.id }) { animal ->
                            FilterChip(
                                selected = selectedFemaleId == animal.id,
                                onClick = { selectedFemaleId = animal.id },
                                label = { Text(animal.identifier) },
                            )
                        }
                    }

                    Text("3) 绑定协议（可选）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedProtocolId == null,
                                onClick = { selectedProtocolId = null },
                                label = { Text("不绑定") },
                            )
                        }
                        items(snapshot.protocols, key = { it.id }) { protocol ->
                            FilterChip(
                                selected = selectedProtocolId == protocol.id,
                                onClick = { selectedProtocolId = protocol.id },
                                label = { Text(protocol.id) },
                                enabled = protocol.isActive,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("备注") },
                        minLines = 2,
                        maxLines = 3,
                    )

                    Button(
                        onClick = {
                            val maleId = selectedMaleId
                            val femaleId = selectedFemaleId
                            if (maleId != null && femaleId != null) {
                                onCreatePlan(maleId, femaleId, selectedProtocolId, notes)
                                notes = ""
                            }
                        },
                        enabled = selectedMaleId != null && selectedFemaleId != null,
                    ) {
                        Text("创建配种计划")
                    }
                    if (inbreedingRisk != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                            ) {
                                Text(
                                    "近交风险提示",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    inbreedingRisk,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "断奶向导",
                subtitle = "批量选择幼鼠并自动分笼，支持一次撤销",
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("产仔登记", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pendingWeaningPlans, key = { it.id }) { plan ->
                            FilterChip(
                                selected = selectedBirthPlanId == plan.id,
                                onClick = { selectedBirthPlanId = plan.id },
                                label = { Text(plan.id) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = birthCountText,
                        onValueChange = { birthCountText = it.filter(Char::isDigit) },
                        label = { Text("幼鼠数量 1-30") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text("品系", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.strainCatalog, key = { it }) { strain ->
                            FilterChip(
                                selected = birthStrain == strain,
                                onClick = { birthStrain = strain },
                                label = { Text(strain) },
                            )
                        }
                    }
                    Text("基因型", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.genotypeCatalog, key = { it }) { genotype ->
                            FilterChip(
                                selected = birthGenotype == genotype,
                                onClick = { birthGenotype = genotype },
                                label = { Text(genotype) },
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val planId = selectedBirthPlanId ?: return@Button
                            val count = birthCountText.toIntOrNull() ?: return@Button
                            onRecordBirth(
                                planId,
                                count,
                                birthStrain.ifBlank { null },
                                birthGenotype.ifBlank { null },
                            )
                        },
                        enabled = selectedBirthPlanId != null && (birthCountText.toIntOrNull() ?: 0) in 1..30,
                    ) {
                        Text("登记产仔")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("1) 选择计划", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pendingWeaningPlans, key = { it.id }) { plan ->
                            FilterChip(
                                selected = selectedWizardPlanId == plan.id,
                                onClick = { selectedWizardPlanId = plan.id },
                                label = { Text(plan.id) },
                            )
                        }
                    }

                    Text(
                        text = if (sourceCageId != null) "2) 来源笼位: $sourceCageId" else "2) 来源笼位: 未识别",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sourcePups, key = { it.id }) { animal ->
                            FilterChip(
                                selected = animal.id in selectedPupIds,
                                onClick = {
                                    selectedPupIds = if (animal.id in selectedPupIds) {
                                        selectedPupIds - animal.id
                                    } else {
                                        selectedPupIds + animal.id
                                    }
                                },
                                label = { Text(animal.identifier) },
                            )
                        }
                    }

                    Text("3) 选择目标笼位", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(targetCages.take(8), key = { it.id }) { cage ->
                            FilterChip(
                                selected = cage.id in selectedTargetCages,
                                onClick = {
                                    selectedTargetCages = if (cage.id in selectedTargetCages) {
                                        selectedTargetCages - cage.id
                                    } else {
                                        selectedTargetCages + cage.id
                                    }
                                },
                                label = { Text(cage.id) },
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val planId = selectedWizardPlanId ?: return@Button
                            onRunWeaningWizard(planId, selectedPupIds.toList(), selectedTargetCages.toList())
                        },
                        enabled = selectedWizardPlanId != null && selectedPupIds.isNotEmpty() && selectedTargetCages.isNotEmpty(),
                    ) {
                        Text("一键断奶分笼并完成")
                    }
                    Button(
                        onClick = onUndoWeaningWizard,
                        enabled = canUndoWeaningWizard,
                    ) {
                        Text("撤销最近一次断奶分笼")
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "进行中计划",
                subtitle = "预估节点自动同步到任务中心",
            )
        }

        item {
            SectionHeader(
                title = "繁育日历（未来21天）",
                subtitle = "查栓/预产/断奶节点一屏查看",
            )
        }
        if (calendarItems.isEmpty()) {
            item {
                Text("未来21天无关键节点", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(calendarItems.take(24), key = { "${it.planId}-${it.nodeType}-${it.dueAtMillis}" }) { item ->
                val statusText = when {
                    item.completed -> "已完成"
                    item.dueAtMillis < now -> "已逾期"
                    else -> "待执行"
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${item.planId} · ${item.nodeType}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(formatDate(item.dueAtMillis), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (statusText == "已逾期") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        items(snapshot.breedingPlans, key = { it.id }) { plan ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(plan.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("配对: ${plan.maleId} x ${plan.femaleId}", style = MaterialTheme.typography.bodyMedium)
                    Text("查栓: ${formatDate(plan.expectedPlugCheckAtMillis)}", style = MaterialTheme.typography.bodySmall)
                    Text("断奶: ${formatDate(plan.expectedWeanAtMillis)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "查栓结果: ${
                            when (plan.plugPositive) {
                                true -> "阳性"
                                false -> "阴性"
                                null -> "待记录"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "断奶状态: ${if (plan.weanedAtMillis == null) "待完成" else "已完成"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (plan.protocolId != null) {
                        Text(
                            text = "协议: ${plan.protocolId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onRecordPlugCheck(plan.id, true) },
                            enabled = plan.plugCheckedAtMillis == null,
                        ) {
                            Text("查栓阳性")
                        }
                        Button(
                            onClick = { onRecordPlugCheck(plan.id, false) },
                            enabled = plan.plugCheckedAtMillis == null,
                        ) {
                            Text("查栓阴性")
                        }
                    }
                    Button(
                        onClick = { onCompleteWeaning(plan.id) },
                        enabled = plan.weanedAtMillis == null && plan.plugPositive != false,
                    ) {
                        Text("完成断奶")
                    }
                }
            }
        }
    }
}

private data class BreedingCalendarItem(
    val planId: String,
    val nodeType: String,
    val dueAtMillis: Long,
    val completed: Boolean,
)

private fun inferInbreedingRisk(snapshot: LabSnapshot, maleId: String?, femaleId: String?): String? {
    if (maleId == null || femaleId == null) return null
    val male = snapshot.animals.firstOrNull { it.id == maleId } ?: return null
    val female = snapshot.animals.firstOrNull { it.id == femaleId } ?: return null
    if (male.id == female.id) return "同一个体不可用于配对。"
    if (male.id == female.fatherId || male.id == female.motherId || female.id == male.fatherId || female.id == male.motherId) {
        return "当前配对为直系亲缘（父母-子代），建议更换亲本。"
    }
    val sameFather = male.fatherId != null && male.fatherId == female.fatherId
    val sameMother = male.motherId != null && male.motherId == female.motherId
    return when {
        sameFather && sameMother -> "当前配对为同父同母同窝个体，近交风险高。"
        sameFather || sameMother -> "当前配对共享单亲缘（半同胞），存在近交风险。"
        else -> null
    }
}
