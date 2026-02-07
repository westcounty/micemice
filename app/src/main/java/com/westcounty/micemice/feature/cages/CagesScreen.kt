package com.westcounty.micemice.feature.cages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.Cage
import com.westcounty.micemice.data.model.CageStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.ui.components.SectionHeader

private data class MoveDialogState(
    val sourceCage: Cage,
    val selectedAnimalId: String,
)

@Composable
fun CagesScreen(
    snapshot: LabSnapshot,
    onMoveAnimal: (animalId: String, targetCageId: String) -> Unit,
    onOpenCageDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var moveDialog by remember { mutableStateOf<MoveDialogState?>(null) }
    var selectedTargetCageId by remember { mutableStateOf<String?>(null) }

    val cages = snapshot.cages
        .filter { it.status != CageStatus.Closed }
        .filter {
            query.isBlank() ||
                it.id.contains(query, ignoreCase = true) ||
                it.roomCode.contains(query, ignoreCase = true)
        }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "笼位管理",
                subtitle = "支持扫码后快速转笼、并笼和容量监控",
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("搜索笼位 / 房间") },
            )
        }

        items(cages, key = { it.id }) { cage ->
            val cageAnimals = snapshot.animals.filter { it.cageId == cage.id }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = cage.id,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "房间 ${cage.roomCode} / 机架 ${cage.rackCode} / 位点 ${cage.slotCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = cage.occupancyText,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (cage.occupancyRatio > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { cage.occupancyRatio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "个体: ${cageAnimals.joinToString { it.identifier }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenCageDetail(cage.id) }) {
                            Text("查看笼卡")
                        }
                        Button(
                            onClick = {
                                if (cageAnimals.isNotEmpty()) {
                                    moveDialog = MoveDialogState(sourceCage = cage, selectedAnimalId = cageAnimals.first().id)
                                    selectedTargetCageId = null
                                }
                            },
                            enabled = cageAnimals.isNotEmpty(),
                        ) {
                            Text("快速转笼")
                        }
                    }
                }
            }
        }
    }

    val dialog = moveDialog
    if (dialog != null) {
        val sourceAnimals = snapshot.animals.filter { it.cageId == dialog.sourceCage.id }
        val candidateTargets = snapshot.cages.filter { it.id != dialog.sourceCage.id && it.status == CageStatus.Active }
        var selectedAnimal by remember(dialog.sourceCage.id) { mutableStateOf(dialog.selectedAnimalId) }

        AlertDialog(
            onDismissRequest = { moveDialog = null },
            title = { Text("转笼 - ${dialog.sourceCage.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("选择转移个体")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sourceAnimals.forEach { animal ->
                            FilterChip(selected = selectedAnimal == animal.id, onClick = { selectedAnimal = animal.id }, label = { Text(animal.identifier) })
                        }
                    }
                    Text("目标笼位")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        candidateTargets.forEach { cage ->
                            FilterChip(selected = selectedTargetCageId == cage.id, onClick = { selectedTargetCageId = cage.id }, label = { Text(cage.id) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = selectedTargetCageId
                        if (target != null) {
                            onMoveAnimal(selectedAnimal, target)
                            moveDialog = null
                        }
                    },
                    enabled = selectedTargetCageId != null,
                ) {
                    Text("确认转移")
                }
            },
            dismissButton = {
                TextButton(onClick = { moveDialog = null }) {
                    Text("取消")
                }
            },
        )
    }
}
