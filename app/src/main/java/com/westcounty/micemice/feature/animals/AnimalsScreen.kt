package com.westcounty.micemice.feature.animals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.ageInWeeks
import com.westcounty.micemice.data.model.canTransitTo
import com.westcounty.micemice.ui.components.SectionHeader

@Composable
fun AnimalsScreen(
    snapshot: LabSnapshot,
    onUpdateStatus: (animalId: String, status: AnimalStatus) -> Unit,
    onOpenAnimalDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<AnimalStatus?>(null) }

    val filteredAnimals = snapshot.animals
        .filter {
            query.isBlank() ||
                it.identifier.contains(query, ignoreCase = true) ||
                it.strain.contains(query, ignoreCase = true) ||
                it.genotype.contains(query, ignoreCase = true)
        }
        .filter { statusFilter == null || it.status == statusFilter }
        .sortedBy { it.identifier }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "个体档案",
                subtitle = "支持快速筛选与状态更新，降低漏标风险",
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("搜索耳号 / 品系 / 基因型") },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("全部") },
                )
                FilterChip(
                    selected = statusFilter == AnimalStatus.Active,
                    onClick = { statusFilter = AnimalStatus.Active },
                    label = { Text("在笼") },
                )
                FilterChip(
                    selected = statusFilter == AnimalStatus.InExperiment,
                    onClick = { statusFilter = AnimalStatus.InExperiment },
                    label = { Text("实验中") },
                )
                FilterChip(
                    selected = statusFilter == AnimalStatus.Breeding,
                    onClick = { statusFilter = AnimalStatus.Breeding },
                    label = { Text("繁育中") },
                )
            }
        }

        items(filteredAnimals, key = { it.id }) { animal ->
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = animal.identifier,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = animal.status.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Text(
                        text = "${animal.strain} | ${animal.genotype} | ${animal.ageInWeeks()} 周龄 | 笼位 ${animal.cageId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onOpenAnimalDetail(animal.id) }) {
                            Text("查看详情")
                        }
                        TextButton(
                            onClick = { onUpdateStatus(animal.id, AnimalStatus.Active) },
                            enabled = animal.status.canTransitTo(AnimalStatus.Active),
                        ) {
                            Text("标记在笼")
                        }
                        TextButton(
                            onClick = { onUpdateStatus(animal.id, AnimalStatus.InExperiment) },
                            enabled = animal.status.canTransitTo(AnimalStatus.InExperiment),
                        ) {
                            Text("标记实验中")
                        }
                        TextButton(
                            onClick = { onUpdateStatus(animal.id, AnimalStatus.Retired) },
                            enabled = animal.status.canTransitTo(AnimalStatus.Retired),
                        ) {
                            Text("标记退役")
                        }
                    }
                }
            }
        }
    }
}
