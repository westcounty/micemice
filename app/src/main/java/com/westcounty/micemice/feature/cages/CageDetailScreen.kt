package com.westcounty.micemice.feature.cages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.CageStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDateTime

@Composable
fun CageDetailScreen(
    cageId: String,
    snapshot: LabSnapshot,
    onMoveAnimal: (animalId: String, targetCageId: String) -> Unit,
    onMergeCages: (sourceCageId: String, targetCageId: String) -> Unit,
    onSplitCage: (
        sourceCageId: String,
        newCageId: String,
        roomCode: String,
        rackCode: String,
        slotCode: String,
        capacityLimit: Int,
        animalIds: List<String>,
    ) -> Unit,
    onBatchUpdateStatus: (animalIds: List<String>, status: AnimalStatus) -> Unit,
    onOpenAnimalDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cage = snapshot.cages.firstOrNull { it.id.equals(cageId, ignoreCase = true) }
    val cageAnimals = snapshot.animals.filter { it.cageId.equals(cageId, ignoreCase = true) }
    val targets = snapshot.cages.filter { it.id != cageId && it.status == CageStatus.Active }
    val cageTimeline = snapshot.auditEvents
        .filter { event ->
            event.entityType.equals("cage", ignoreCase = true) &&
                event.entityId.contains(cageId, ignoreCase = true)
        }
        .plus(
            snapshot.auditEvents.filter { event ->
                event.action.equals("ANIMAL_MOVE", ignoreCase = true) &&
                    event.summary.contains(cageId, ignoreCase = true)
            }
        )
        .distinctBy { it.id }
        .sortedByDescending { it.createdAtMillis }

    var selectedAnimalId by remember { mutableStateOf<String?>(cageAnimals.firstOrNull()?.id) }
    var selectedTargetCage by remember { mutableStateOf<String?>(targets.firstOrNull()?.id) }
    val selectedSplitAnimals = remember { mutableStateListOf<String>() }
    var newCageId by remember(cageId) { mutableStateOf("${cageId}-S1") }
    var newRoomCode by remember(cageId) { mutableStateOf(cage?.roomCode ?: "A1") }
    var newRackCode by remember(cageId) { mutableStateOf(cage?.rackCode ?: "R1") }
    var newSlotCode by remember(cageId) { mutableStateOf("99") }
    var newCapacityText by remember(cageId) { mutableStateOf("4") }
    val selectedBatchAnimals = remember(cageId) { mutableStateListOf<String>() }
    var targetStatus by remember(cageId) { mutableStateOf(AnimalStatus.Active) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (cage == null) {
            item {
                SectionHeader(title = "笼卡不存在", subtitle = "未找到 $cageId，请检查条码是否正确")
            }
            return@LazyColumn
        }

        item {
            SectionHeader(title = "笼卡详情 ${cage.id}", subtitle = "房间 ${cage.roomCode} / 机架 ${cage.rackCode} / 位点 ${cage.slotCode}")
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
                    Text("占用 ${cage.occupancyText}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = { cage.occupancyRatio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("在笼个体 ${cageAnimals.size} 只", style = MaterialTheme.typography.bodySmall)
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
                    Text("快速转笼", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cageAnimals.forEach { animal ->
                            FilterChip(
                                selected = selectedAnimalId == animal.id,
                                onClick = { selectedAnimalId = animal.id },
                                label = { Text(animal.identifier) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        targets.take(5).forEach { target ->
                            FilterChip(
                                selected = selectedTargetCage == target.id,
                                onClick = { selectedTargetCage = target.id },
                                label = { Text(target.id) },
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val animalId = selectedAnimalId
                            val target = selectedTargetCage
                            if (animalId != null && target != null) onMoveAnimal(animalId, target)
                        },
                        enabled = selectedAnimalId != null && selectedTargetCage != null,
                    ) {
                        Text("确认转笼")
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
                    Text("并笼", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("将当前笼全部个体并入目标笼，当前笼将关闭", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        targets.take(6).forEach { target ->
                            FilterChip(
                                selected = selectedTargetCage == target.id,
                                onClick = { selectedTargetCage = target.id },
                                label = { Text(target.id) },
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val target = selectedTargetCage
                            if (target != null) onMergeCages(cage.id, target)
                        },
                        enabled = selectedTargetCage != null && cageAnimals.isNotEmpty(),
                    ) {
                        Text("确认并笼")
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
                    Text("拆笼", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("选择部分个体迁移到新笼，支持同房间分流", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cageAnimals.forEach { animal ->
                            FilterChip(
                                selected = animal.id in selectedSplitAnimals,
                                onClick = {
                                    if (animal.id in selectedSplitAnimals) {
                                        selectedSplitAnimals.remove(animal.id)
                                    } else {
                                        selectedSplitAnimals.add(animal.id)
                                    }
                                },
                                label = { Text(animal.identifier) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newCageId,
                        onValueChange = { newCageId = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("新笼编号") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newRoomCode,
                            onValueChange = { newRoomCode = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            label = { Text("房间") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newRackCode,
                            onValueChange = { newRackCode = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            label = { Text("机架") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newSlotCode,
                            onValueChange = { newSlotCode = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            label = { Text("位点") },
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = newCapacityText,
                        onValueChange = { newCapacityText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("新笼容量") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val capacity = newCapacityText.toIntOrNull() ?: return@Button
                            onSplitCage(cage.id, newCageId, newRoomCode, newRackCode, newSlotCode, capacity, selectedSplitAnimals.toList())
                        },
                        enabled = selectedSplitAnimals.isNotEmpty() && newCageId.isNotBlank() && (newCapacityText.toIntOrNull() ?: 0) > 0,
                    ) {
                        Text("确认拆笼")
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
                    Text("批量更新个体状态", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cageAnimals.forEach { animal ->
                            FilterChip(
                                selected = animal.id in selectedBatchAnimals,
                                onClick = {
                                    if (animal.id in selectedBatchAnimals) selectedBatchAnimals.remove(animal.id) else selectedBatchAnimals.add(animal.id)
                                },
                                label = { Text(animal.identifier) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AnimalStatus.Active to "在笼",
                            AnimalStatus.InExperiment to "实验中",
                            AnimalStatus.Breeding to "繁育中",
                            AnimalStatus.Retired to "退役",
                            AnimalStatus.Dead to "死亡",
                        ).forEach { (status, label) ->
                            FilterChip(
                                selected = targetStatus == status,
                                onClick = { targetStatus = status },
                                label = { Text(label) },
                            )
                        }
                    }
                    Button(
                        onClick = { onBatchUpdateStatus(selectedBatchAnimals.toList(), targetStatus) },
                        enabled = selectedBatchAnimals.isNotEmpty(),
                    ) {
                        Text("应用到选中个体")
                    }
                }
            }
        }

        items(cageAnimals, key = { it.id }) { animal ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(animal.identifier, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${animal.strain} | ${animal.genotype}", style = MaterialTheme.typography.bodySmall)
                    Text("状态 ${animal.status.displayName}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onOpenAnimalDetail(animal.id) }) {
                        Text("查看个体详情")
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "笼卡历史",
                subtitle = "关键变更操作时间线",
            )
        }

        if (cageTimeline.isEmpty()) {
            item {
                Text(
                    "暂无笼位相关历史记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(cageTimeline.take(20), key = { it.id }) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(event.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(event.summary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${event.operator} · ${formatDateTime(event.createdAtMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
