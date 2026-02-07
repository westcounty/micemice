package com.westcounty.micemice.feature.genotyping

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.SampleType
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDateTime
import com.westcounty.micemice.ui.util.writeCsvToAppFiles

@Composable
fun GenotypingScreen(
    snapshot: LabSnapshot,
    onRegisterSample: (animalId: String, sampleType: SampleType) -> Unit,
    onCreateBatch: (name: String, sampleIds: List<String>) -> Unit,
    onImportResults: (batchId: String, csvText: String, reviewer: String) -> Unit,
    onConfirmResult: (resultId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedAnimalId by remember { mutableStateOf<String?>(null) }
    var selectedSampleType by remember { mutableStateOf(SampleType.Ear) }

    var batchName by remember { mutableStateOf("Batch-${snapshot.genotypingBatches.size + 1}") }
    val selectedSampleIds = remember { mutableStateListOf<String>() }

    var selectedBatchId by remember { mutableStateOf<String?>(snapshot.genotypingBatches.firstOrNull()?.id) }
    var reviewer by remember { mutableStateOf("") }
    var csvText by remember { mutableStateOf("sample_id,marker,call\n") }
    var latestTemplateCsv by remember { mutableStateOf("") }
    val importReport = snapshot.lastGenotypingImportReport

    val unbatchedSamples = snapshot.samples.filter { it.batchId == null }
    val samplesById = snapshot.samples.associateBy { it.id }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "分型管理",
                subtitle = "采样 -> 批次 -> 结果回填 -> 冲突确认",
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
                    Text("1) 采样登记", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.animals.take(8), key = { it.id }) { animal ->
                            FilterChip(
                                selected = selectedAnimalId == animal.id,
                                onClick = { selectedAnimalId = animal.id },
                                label = { Text(animal.identifier) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SampleType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedSampleType == type,
                                onClick = { selectedSampleType = type },
                                label = { Text(type.displayName) },
                            )
                        }
                    }
                    Button(onClick = { selectedAnimalId?.let { onRegisterSample(it, selectedSampleType) } }, enabled = selectedAnimalId != null) {
                        Text("登记采样")
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
                    Text("2) 分型批次", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("批次名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(unbatchedSamples, key = { it.id }) { sample ->
                            FilterChip(
                                selected = sample.id in selectedSampleIds,
                                onClick = {
                                    if (sample.id in selectedSampleIds) selectedSampleIds.remove(sample.id) else selectedSampleIds.add(sample.id)
                                },
                                label = { Text("${sample.id}${sample.platePosition?.let { "($it)" } ?: ""}") },
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onCreateBatch(batchName, selectedSampleIds.toList())
                            selectedSampleIds.clear()
                        },
                        enabled = selectedSampleIds.isNotEmpty() && batchName.isNotBlank(),
                    ) {
                        Text("创建批次")
                    }
                    val selectedBatch = snapshot.genotypingBatches.firstOrNull { it.id == selectedBatchId }
                    val batchSamples = selectedBatch?.sampleIds
                        ?.mapNotNull { samplesById[it] }
                        .orEmpty()
                    if (batchSamples.isNotEmpty()) {
                        Text("板位映射", style = MaterialTheme.typography.labelLarge)
                        batchSamples.forEach { sample ->
                            Text(
                                text = "${sample.id} -> ${sample.platePosition ?: "--"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    Text("3) 结果回填", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.genotypingBatches, key = { it.id }) { batch ->
                            FilterChip(
                                selected = selectedBatchId == batch.id,
                                onClick = { selectedBatchId = batch.id },
                                label = { Text(batch.name) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reviewer,
                        onValueChange = { reviewer = it },
                        label = { Text("Reviewer") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        label = { Text("CSV: sample_id,marker,call") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                    )
                    Button(
                        onClick = {
                            val batchId = selectedBatchId
                            if (batchId != null) onImportResults(batchId, csvText, reviewer)
                        },
                        enabled = selectedBatchId != null && reviewer.isNotBlank() && csvText.isNotBlank(),
                    ) {
                        Text("导入分型结果")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val batch = snapshot.genotypingBatches.firstOrNull { it.id == selectedBatchId }
                                if (batch != null) {
                                    val template = buildString {
                                        append("sample_id,marker,call\n")
                                        batch.sampleIds.forEach { sampleId ->
                                            append("$sampleId,GeneX,+/-\n")
                                        }
                                    }.trim()
                                    latestTemplateCsv = template
                                    csvText = template
                                }
                            },
                            enabled = selectedBatchId != null,
                        ) {
                            Text("生成导入模板")
                        }
                        Button(
                            onClick = {
                                val payload = latestTemplateCsv.ifBlank { csvText }
                                if (payload.isNotBlank()) {
                                    writeCsvToAppFiles(context, "genotyping_template", payload)
                                }
                            },
                            enabled = csvText.isNotBlank(),
                        ) {
                            Text("下载模板CSV")
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "分型结果",
                subtitle = "冲突项需人工确认",
            )
        }

        if (importReport != null) {
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
                        Text("最近导入报告", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("批次 ${importReport.batchId} · reviewer ${importReport.reviewer}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "成功 ${importReport.importedCount}，冲突 ${importReport.conflictCount}，失败 ${importReport.failedCount}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (importReport.failedCount > 0) {
                            Button(onClick = { csvText = importReport.failedRowsCsv }) {
                                Text("将失败行填入导入框重试")
                            }
                            importReport.issues.take(8).forEach { issue ->
                                Text(
                                    text = "行${issue.lineNumber}: ${issue.reason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Text(formatDateTime(importReport.importedAtMillis), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        items(snapshot.genotypingResults.take(20), key = { it.id }) { result ->
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
                    Text("${result.sampleId} · ${result.marker}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("call=${result.callValue} · v${result.version} · reviewer=${result.reviewer}", style = MaterialTheme.typography.bodySmall)
                    Text("${if (result.conflict) "冲突" else "正常"} · ${formatDateTime(result.reviewedAtMillis)}", style = MaterialTheme.typography.bodySmall)
                    if (result.conflict) {
                        Button(onClick = { onConfirmResult(result.id) }) {
                            Text("确认该结果")
                        }
                    }
                }
            }
        }
    }
}
