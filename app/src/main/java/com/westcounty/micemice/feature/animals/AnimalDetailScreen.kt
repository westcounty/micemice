package com.westcounty.micemice.feature.animals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.data.model.canTransitTo
import com.westcounty.micemice.ui.util.formatDate
import com.westcounty.micemice.ui.util.formatDateTime
import com.westcounty.micemice.ui.util.writeBitmapPngToAppFiles

@Composable
fun AnimalDetailScreen(
    animalId: String,
    snapshot: LabSnapshot,
    onUpdateStatus: (animalId: String, status: AnimalStatus) -> Unit,
    onAddAnimalEvent: (animalId: String, eventType: String, note: String, weightGram: Float?) -> Unit,
    onAddAttachment: (animalId: String, label: String, filePath: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animal = snapshot.animals.firstOrNull { it.id.equals(animalId, ignoreCase = true) }
    val samples = snapshot.samples.filter { it.animalId == animalId }.sortedByDescending { it.sampledAtMillis }
    val sampleIds = samples.map { it.id }.toSet()
    val genotypingResults = snapshot.genotypingResults
        .filter { it.sampleId in sampleIds }
        .sortedByDescending { it.reviewedAtMillis }
    val experimentIds = snapshot.experiments
        .filter { exp -> snapshot.cohorts.firstOrNull { it.id == exp.cohortId }?.animalIds?.contains(animalId) == true }
        .map { it.id }
        .toSet()
    val experimentEvents = snapshot.experimentEvents
        .filter { it.experimentId in experimentIds }
        .sortedByDescending { it.createdAtMillis }
    val attachments = snapshot.animalAttachments
        .filter { it.animalId == animalId }
        .sortedByDescending { it.createdAtMillis }
    val animalEvents = snapshot.animalEvents
        .filter { it.animalId == animalId }
        .sortedByDescending { it.createdAtMillis }
    val timeline = snapshot.auditEvents
        .filter { it.entityId.contains(animalId, ignoreCase = true) || (animal?.identifier?.let { id -> it.summary.contains(id) } == true) }
        .sortedByDescending { it.createdAtMillis }

    var selectedStatus by remember(animal?.status) { mutableStateOf(animal?.status ?: AnimalStatus.Active) }
    var selectedEventType by remember { mutableStateOf("weight") }
    var eventNote by remember { mutableStateOf("") }
    var eventWeightText by remember { mutableStateOf("") }
    var attachmentLabel by remember { mutableStateOf("") }
    var attachmentPath by remember { mutableStateOf("") }
    val context = LocalContext.current
    val pickAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                attachmentPath = uri.toString()
                if (attachmentLabel.isBlank()) {
                    attachmentLabel = "附件-${uri.lastPathSegment?.takeLast(12).orEmpty()}"
                }
            }
        },
    )
    val captureAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                val saved = writeBitmapPngToAppFiles(context, "animal_${animalId}_camera", bitmap)
                saved.getOrNull()?.let { path ->
                    attachmentPath = path
                    if (attachmentLabel.isBlank()) attachmentLabel = "现场拍照"
                }
            }
        },
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (animal == null) {
            item {
                SectionHeader(title = "个体不存在", subtitle = "未找到 $animalId，请检查条码或列表状态")
            }
            return@LazyColumn
        }

        item {
            SectionHeader(
                title = "个体详情 ${animal.identifier}",
                subtitle = "${animal.id} · ${animal.strain} · ${animal.genotype}",
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("基础信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("性别: ${animal.sex.name} · 状态: ${animal.status.displayName}", style = MaterialTheme.typography.bodyMedium)
                    Text("出生日期: ${formatDate(animal.birthAtMillis)}", style = MaterialTheme.typography.bodyMedium)
                    Text("当前笼位: ${animal.cageId}", style = MaterialTheme.typography.bodyMedium)
                    Text("协议: ${animal.protocolId ?: "未绑定"}", style = MaterialTheme.typography.bodyMedium)
                    val father = animal.fatherId?.let { fid -> snapshot.animals.firstOrNull { it.id == fid } }
                    val mother = animal.motherId?.let { mid -> snapshot.animals.firstOrNull { it.id == mid } }
                    Text(
                        "家系: 父本 ${father?.identifier ?: "-"} / 母本 ${mother?.identifier ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AnimalStatus.Active to "在笼",
                            AnimalStatus.Breeding to "繁育",
                            AnimalStatus.InExperiment to "实验",
                            AnimalStatus.Retired to "退役",
                            AnimalStatus.Dead to "死亡",
                        ).forEach { (status, label) ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = {
                                    selectedStatus = status
                                    onUpdateStatus(animal.id, status)
                                },
                                label = { Text(label) },
                                enabled = animal.status.canTransitTo(status),
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "谱系关系", subtitle = "父母与直接子代")
        }
        item {
            val father = animal.fatherId?.let { fid -> snapshot.animals.firstOrNull { it.id == fid } }
            val mother = animal.motherId?.let { mid -> snapshot.animals.firstOrNull { it.id == mid } }
            val offspring = snapshot.animals
                .filter { it.fatherId == animal.id || it.motherId == animal.id }
                .sortedByDescending { it.birthAtMillis }
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
                    Text("父本: ${father?.identifier ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                    Text("母本: ${mother?.identifier ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                    val offspringText = if (offspring.isEmpty()) {
                        "无"
                    } else {
                        offspring.take(8).joinToString("、") { it.identifier }
                    }
                    Text("直接子代: $offspringText", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            SectionHeader(title = "处理事件", subtitle = "体重/健康/处理记录")
        }
        item {
            val parsedWeight = eventWeightText.toFloatOrNull()
            val canSubmit = if (selectedEventType == "weight") {
                (parsedWeight ?: 0f) > 0f || eventNote.isNotBlank()
            } else {
                eventNote.isNotBlank()
            }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "weight" to "体重",
                            "health" to "健康",
                            "handling" to "处理",
                            "procedure" to "实验处理",
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = selectedEventType == type,
                                onClick = { selectedEventType = type },
                                label = { Text(label) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = eventNote,
                        onValueChange = { eventNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("事件备注") },
                        minLines = 2,
                        maxLines = 3,
                    )
                    OutlinedTextField(
                        value = eventWeightText,
                        onValueChange = { input ->
                            eventWeightText = input.filter { it.isDigit() || it == '.' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("体重(g，可选)") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            onAddAnimalEvent(
                                animal.id,
                                selectedEventType,
                                eventNote.ifBlank { "常规记录" },
                                parsedWeight?.takeIf { it > 0f },
                            )
                            eventNote = ""
                            eventWeightText = ""
                        },
                        enabled = canSubmit,
                    ) {
                        Text("添加事件")
                    }
                }
            }
        }
        if (animalEvents.isEmpty()) {
            item {
                Text("暂无处理事件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(animalEvents.take(12), key = { it.id }) { event ->
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
                        val weightText = event.weightGram?.let { " · ${"%.1f".format(it)} g" }.orEmpty()
                        Text("${event.eventType}$weightText", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(event.note, style = MaterialTheme.typography.bodySmall)
                        Text("${event.operator} · ${formatDateTime(event.createdAtMillis)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            SectionHeader(title = "附件", subtitle = "图片/报告等本地文件引用")
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
                    OutlinedTextField(
                        value = attachmentLabel,
                        onValueChange = { attachmentLabel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("附件名称") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = attachmentPath,
                        onValueChange = { attachmentPath = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("文件路径") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { pickAttachmentLauncher.launch("*/*") }) {
                            Text("选择文件")
                        }
                        Button(onClick = { captureAttachmentLauncher.launch(null) }) {
                            Text("拍照直传")
                        }
                    }
                    Button(
                        onClick = {
                            onAddAttachment(animal.id, attachmentLabel, attachmentPath)
                            attachmentLabel = ""
                            attachmentPath = ""
                        },
                        enabled = attachmentLabel.isNotBlank() && attachmentPath.isNotBlank(),
                    ) {
                        Text("添加附件")
                    }
                }
            }
        }
        if (attachments.isEmpty()) {
            item {
                Text("暂无附件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(attachments.take(12), key = { it.id }) { attachment ->
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
                        Text(attachment.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(attachment.filePath, style = MaterialTheme.typography.bodySmall)
                        Text("${attachment.operator} · ${formatDateTime(attachment.createdAtMillis)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            SectionHeader(title = "分型记录", subtitle = "最近 ${genotypingResults.size} 条")
        }
        if (genotypingResults.isEmpty()) {
            item {
                Text("暂无分型记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(genotypingResults.take(10), key = { it.id }) { result ->
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
                        Text("${result.marker}: ${result.callValue}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("版本 v${result.version} · ${if (result.conflict) "冲突待确认" else "已确认"}", style = MaterialTheme.typography.bodySmall)
                        Text(formatDateTime(result.reviewedAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            SectionHeader(title = "实验事件", subtitle = "关联实验事件 ${experimentEvents.size} 条")
        }
        if (experimentEvents.isEmpty()) {
            item {
                Text("暂无实验事件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(experimentEvents.take(10), key = { it.id }) { event ->
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
                        Text(event.eventType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(event.note, style = MaterialTheme.typography.bodySmall)
                        Text("${event.operator} · ${formatDateTime(event.createdAtMillis)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            SectionHeader(title = "审计时间线", subtitle = "关键操作可追溯")
        }
        if (timeline.isEmpty()) {
            item {
                Text("暂无审计记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(timeline.take(12), key = { it.id }) { audit ->
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
                        Text(audit.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(audit.summary, style = MaterialTheme.typography.bodySmall)
                        Text("${audit.operator} · ${formatDateTime(audit.createdAtMillis)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
