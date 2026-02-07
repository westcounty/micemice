package com.westcounty.micemice.feature.experiment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.ExperimentStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDateTime

@Composable
fun ExperimentScreen(
    snapshot: LabSnapshot,
    onCreateExperiment: (cohortId: String, title: String) -> Unit,
    onAddEvent: (experimentId: String, eventType: String, note: String) -> Unit,
    onArchiveExperiment: (experimentId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCohortId by remember { mutableStateOf<String?>(snapshot.cohorts.firstOrNull()?.id) }
    var experimentTitle by remember { mutableStateOf("New Experiment") }
    var eventType by remember { mutableStateOf("dose") }
    var eventNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "实验管理",
                subtitle = "从 Cohort 建立实验并记录关键事件",
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
                    Text("创建实验", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.cohorts, key = { it.id }) { cohort ->
                            FilterChip(
                                selected = selectedCohortId == cohort.id,
                                onClick = { selectedCohortId = cohort.id },
                                label = { Text(cohort.name) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = experimentTitle,
                        onValueChange = { experimentTitle = it },
                        label = { Text("实验标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Button(
                        onClick = { selectedCohortId?.let { onCreateExperiment(it, experimentTitle) } },
                        enabled = selectedCohortId != null && experimentTitle.isNotBlank(),
                    ) {
                        Text("创建实验")
                    }
                }
            }
        }

        items(snapshot.experiments, key = { it.id }) { experiment ->
            val eventCount = snapshot.experimentEvents.count { it.experimentId == experiment.id }
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
                    Text(experiment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${experiment.id} · ${experiment.status.displayName} · 事件 $eventCount", style = MaterialTheme.typography.bodySmall)
                    Text("开始 ${formatDateTime(experiment.startedAtMillis)}", style = MaterialTheme.typography.bodySmall)

                    if (experiment.status == ExperimentStatus.Active) {
                        OutlinedTextField(
                            value = eventType,
                            onValueChange = { eventType = it },
                            label = { Text("事件类型") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = eventNote,
                            onValueChange = { eventNote = it },
                            label = { Text("事件备注") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                        )
                        Button(onClick = { onAddEvent(experiment.id, eventType, eventNote) }) {
                            Text("追加事件")
                        }
                        Button(onClick = { onArchiveExperiment(experiment.id) }) {
                            Text("归档实验")
                        }
                    }

                    val latestEvents = snapshot.experimentEvents
                        .filter { it.experimentId == experiment.id }
                        .sortedByDescending { it.createdAtMillis }
                        .take(3)

                    latestEvents.forEach { event ->
                        Text("${event.eventType}: ${event.note}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
