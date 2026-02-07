package com.westcounty.micemice.feature.cohort

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.AnimalSex
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.ageInWeeks
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDate
import com.westcounty.micemice.ui.util.writeCsvToAppFiles

@Composable
fun CohortScreen(
    snapshot: LabSnapshot,
    onCreateCohort: (
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
        blindCodingEnabled: Boolean,
        blindCodePrefix: String?,
    ) -> Unit,
    onSaveTemplate: (
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
    ) -> Unit,
    onApplyTemplate: (templateId: String) -> Unit,
    onUpdateTemplate: (
        templateId: String,
        name: String,
        strain: String?,
        genotype: String?,
        sex: AnimalSex?,
        minWeeks: Int?,
        maxWeeks: Int?,
    ) -> Unit,
    onDeleteTemplate: (templateId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var cohortName by remember { mutableStateOf("Cohort-${snapshot.cohorts.size + 1}") }
    var selectedStrain by remember { mutableStateOf<String?>(null) }
    var genotypeText by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf<AnimalSex?>(null) }
    var minWeeksText by remember { mutableStateOf("") }
    var maxWeeksText by remember { mutableStateOf("") }
    var templateName by remember { mutableStateOf("Template-${snapshot.cohortTemplates.size + 1}") }
    var editingTemplateId by remember { mutableStateOf<String?>(null) }
    var templateSort by remember { mutableStateOf(TemplateSort.Recent) }
    var enableBlindCoding by remember { mutableStateOf(true) }
    var blindCodePrefix by remember { mutableStateOf("BL") }
    var blindExportMessage by remember { mutableStateOf("") }

    val strains = snapshot.animals.map { it.strain }.distinct().sorted()

    val candidates = snapshot.animals
        .filter { it.status == AnimalStatus.Active || it.status == AnimalStatus.InExperiment }
        .filter { selectedStrain == null || it.strain == selectedStrain }
        .filter { genotypeText.isBlank() || it.genotype.contains(genotypeText, ignoreCase = true) }
        .filter { selectedSex == null || it.sex == selectedSex }
        .filter {
            val age = it.ageInWeeks()
            val min = minWeeksText.toIntOrNull()
            val max = maxWeeksText.toIntOrNull()
            (min == null || age >= min) && (max == null || age <= max)
        }
        .sortedBy { it.identifier }

    val sortedTemplates = when (templateSort) {
        TemplateSort.Recent -> snapshot.cohortTemplates.sortedByDescending { it.updatedAtMillis }
        TemplateSort.Usage -> snapshot.cohortTemplates.sortedByDescending { it.usageCount }
        TemplateSort.Name -> snapshot.cohortTemplates.sortedBy { it.name.lowercase() }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Cohort 构建",
                subtitle = "按条件筛选并锁定实验队列，确保复现性",
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
                    Text("筛选模板", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("模板名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val editingId = editingTemplateId
                            if (editingId == null) {
                                onSaveTemplate(
                                    templateName,
                                    selectedStrain,
                                    genotypeText.ifBlank { null },
                                    selectedSex,
                                    minWeeksText.toIntOrNull(),
                                    maxWeeksText.toIntOrNull(),
                                )
                            } else {
                                onUpdateTemplate(
                                    editingId,
                                    templateName,
                                    selectedStrain,
                                    genotypeText.ifBlank { null },
                                    selectedSex,
                                    minWeeksText.toIntOrNull(),
                                    maxWeeksText.toIntOrNull(),
                                )
                            }
                        },
                        enabled = templateName.isNotBlank(),
                    ) {
                        Text(if (editingTemplateId == null) "保存当前筛选为模板" else "更新模板")
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.cohortTemplates.take(5), key = { it.id }) { template ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    selectedStrain = template.strain
                                    genotypeText = template.genotype.orEmpty()
                                    selectedSex = template.sex
                                    minWeeksText = template.minWeeks?.toString().orEmpty()
                                    maxWeeksText = template.maxWeeks?.toString().orEmpty()
                                    onApplyTemplate(template.id)
                                },
                                label = { Text("${template.name}(${template.usageCount})") },
                            )
                        }
                    }
                    if (editingTemplateId != null) {
                        Button(
                            onClick = {
                                onDeleteTemplate(editingTemplateId!!)
                                editingTemplateId = null
                                templateName = "Template-${snapshot.cohortTemplates.size + 1}"
                            },
                        ) {
                            Text("删除当前编辑模板")
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
                    Text("模板管理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = templateSort == TemplateSort.Recent,
                            onClick = { templateSort = TemplateSort.Recent },
                            label = { Text("最近更新") },
                        )
                        FilterChip(
                            selected = templateSort == TemplateSort.Usage,
                            onClick = { templateSort = TemplateSort.Usage },
                            label = { Text("使用次数") },
                        )
                        FilterChip(
                            selected = templateSort == TemplateSort.Name,
                            onClick = { templateSort = TemplateSort.Name },
                            label = { Text("名称") },
                        )
                    }
                    sortedTemplates.take(20).forEach { template ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(template.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "strain=${template.strain ?: "*"} genotype=${template.genotype ?: "*"} sex=${template.sex?.name ?: "*"}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text("age=${template.minWeeks ?: "*"}-${template.maxWeeks ?: "*"} | use=${template.usageCount}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            editingTemplateId = template.id
                                            templateName = template.name
                                            selectedStrain = template.strain
                                            genotypeText = template.genotype.orEmpty()
                                            selectedSex = template.sex
                                            minWeeksText = template.minWeeks?.toString().orEmpty()
                                            maxWeeksText = template.maxWeeks?.toString().orEmpty()
                                        },
                                        label = { Text("编辑") },
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            selectedStrain = template.strain
                                            genotypeText = template.genotype.orEmpty()
                                            selectedSex = template.sex
                                            minWeeksText = template.minWeeks?.toString().orEmpty()
                                            maxWeeksText = template.maxWeeks?.toString().orEmpty()
                                            onApplyTemplate(template.id)
                                        },
                                        label = { Text("套用") },
                                    )
                                }
                            }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = cohortName,
                        onValueChange = { cohortName = it },
                        label = { Text("Cohort 名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Text("品系", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedStrain == null,
                            onClick = { selectedStrain = null },
                            label = { Text("全部") },
                        )
                        strains.take(4).forEach { strain ->
                            FilterChip(
                                selected = selectedStrain == strain,
                                onClick = { selectedStrain = strain },
                                label = { Text(strain) },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = genotypeText,
                        onValueChange = { genotypeText = it },
                        label = { Text("基因型包含") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Text("性别", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedSex == null, onClick = { selectedSex = null }, label = { Text("不限") })
                        FilterChip(selected = selectedSex == AnimalSex.Male, onClick = { selectedSex = AnimalSex.Male }, label = { Text("雄") })
                        FilterChip(selected = selectedSex == AnimalSex.Female, onClick = { selectedSex = AnimalSex.Female }, label = { Text("雌") })
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = minWeeksText,
                            onValueChange = { minWeeksText = it.filter(Char::isDigit) },
                            label = { Text("最小周龄") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = maxWeeksText,
                            onValueChange = { maxWeeksText = it.filter(Char::isDigit) },
                            label = { Text("最大周龄") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("盲法编码", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = enableBlindCoding,
                            onCheckedChange = { enableBlindCoding = it },
                        )
                    }
                    OutlinedTextField(
                        value = blindCodePrefix,
                        onValueChange = { blindCodePrefix = it.uppercase().take(6) },
                        label = { Text("盲码前缀（如 BL）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = enableBlindCoding,
                    )

                    Text(
                        text = "候选个体 ${candidates.size} 只",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Button(
                        onClick = {
                            onCreateCohort(
                                cohortName,
                                selectedStrain,
                                genotypeText.ifBlank { null },
                                selectedSex,
                                minWeeksText.toIntOrNull(),
                                maxWeeksText.toIntOrNull(),
                                enableBlindCoding,
                                blindCodePrefix.ifBlank { "BL" },
                            )
                        },
                        enabled = candidates.isNotEmpty() && cohortName.isNotBlank(),
                    ) {
                        Text("创建并锁定 Cohort")
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "已创建 Cohort",
                subtitle = "锁定后成员不可静默变更",
            )
        }

        items(snapshot.cohorts, key = { it.id }) { cohort ->
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
                    Text(cohort.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${cohort.id} · ${if (cohort.locked) "已锁定" else "未锁定"}", style = MaterialTheme.typography.bodySmall)
                    Text("成员数 ${cohort.animalIds.size}", style = MaterialTheme.typography.bodySmall)
                    Text("条件: ${cohort.criteriaSummary}", style = MaterialTheme.typography.bodySmall)
                    Text("盲码: ${if (cohort.blindCodes.isEmpty()) "未启用" else "已启用(${cohort.blindCodes.size})"}", style = MaterialTheme.typography.bodySmall)
                    Text("创建时间: ${formatDate(cohort.createdAtMillis)}", style = MaterialTheme.typography.bodySmall)
                    if (cohort.blindCodes.isNotEmpty()) {
                        Button(
                            onClick = {
                                val animalsById = snapshot.animals.associateBy { it.id }
                                val csv = buildString {
                                    append("blind_code,animal_id,identifier,strain,genotype,cage_id,status\n")
                                    cohort.blindCodes.entries.sortedBy { it.value }.forEach { (animalId, code) ->
                                        val animal = animalsById[animalId]
                                        append(
                                            listOf(
                                                code,
                                                animalId,
                                                animal?.identifier ?: "",
                                                animal?.strain ?: "",
                                                animal?.genotype ?: "",
                                                animal?.cageId ?: "",
                                                animal?.status?.name ?: "",
                                            ).joinToString(",")
                                        )
                                        append('\n')
                                    }
                                }.trim()
                                val result = writeCsvToAppFiles(context, "cohort_blind_${cohort.id}", csv)
                                blindExportMessage = result.getOrNull()?.let { "盲法CSV已保存：$it" } ?: "盲法CSV保存失败"
                            },
                        ) {
                            Text("导出盲法CSV")
                        }
                    }
                    if (blindExportMessage.isNotBlank()) {
                        Text(blindExportMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private enum class TemplateSort {
    Recent,
    Usage,
    Name,
}
