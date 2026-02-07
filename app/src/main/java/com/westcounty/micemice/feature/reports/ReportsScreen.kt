package com.westcounty.micemice.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.SyncStatus
import com.westcounty.micemice.data.model.CageStatus
import com.westcounty.micemice.data.model.AnimalStatus
import com.westcounty.micemice.ui.components.SectionHeader

@Composable
fun ReportsScreen(
    snapshot: LabSnapshot,
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableStateOf(30) }
    var selectedStrain by remember { mutableStateOf<String?>(null) }
    var projectFilter by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val sinceMillis = now - selectedDays * DAY_MILLIS
    val strainCandidates = snapshot.animals.map { it.strain }.distinct().sorted()
    val protocolById = snapshot.protocols.associateBy { it.id }
    val projectKeyword = projectFilter.trim()

    val animalsInScope = snapshot.animals
        .filter { selectedStrain == null || it.strain == selectedStrain }
        .filter { animal ->
            if (projectKeyword.isBlank()) return@filter true
            val protocol = animal.protocolId?.let { protocolById[it] }
            (animal.protocolId?.contains(projectKeyword, ignoreCase = true) == true) ||
                (protocol?.title?.contains(projectKeyword, ignoreCase = true) == true)
        }
    val animalIdsInScope = animalsInScope.map { it.id }.toSet()

    val cagesInScope = snapshot.cages.filter { cage -> cage.animalIds.any { it in animalIdsInScope } || animalIdsInScope.isEmpty() }
    val occupancyRate = if (cagesInScope.isEmpty()) {
        0f
    } else {
        cagesInScope.map { cage ->
            val count = cage.animalIds.count { it in animalIdsInScope }
            if (cage.capacityLimit <= 0) 0f else count.toFloat() / cage.capacityLimit.toFloat()
        }.average().toFloat()
    }

    val tasksInScope = snapshot.tasks.filter { it.dueAtMillis >= sinceMillis }
    val taskDone = tasksInScope.count { it.status.name == "Done" }
    val taskCompletionRate = if (tasksInScope.isEmpty()) 0f else taskDone.toFloat() / tasksInScope.size.toFloat()

    val breedingInScope = snapshot.breedingPlans.filter { plan ->
        plan.matingAtMillis >= sinceMillis &&
            (animalIdsInScope.isEmpty() || plan.maleId in animalIdsInScope || plan.femaleId in animalIdsInScope)
    }
    val doneWeanTasks = tasksInScope.count { it.entityType == "breeding" && it.title.contains("断奶") && it.status.name == "Done" }
    val breedingSuccessRate = if (breedingInScope.isEmpty()) 0f else doneWeanTasks.coerceAtMost(breedingInScope.size).toFloat() / breedingInScope.size.toFloat()

    val survivalRate = if (animalsInScope.isEmpty()) 1f else {
        animalsInScope.count { it.status.name != "Dead" }.toFloat() / animalsInScope.size.toFloat()
    }

    val activeBreedingPlans = breedingInScope.size
    val activeCohorts = snapshot.cohorts.count { it.locked && it.createdAtMillis >= sinceMillis }
    val activeExperiments = snapshot.experiments.count { it.startedAtMillis >= sinceMillis && it.status.name == "Active" }
    val overdueTasks = tasksInScope.count { it.status.name == "Overdue" }
    val pendingSync = snapshot.syncEvents.count { it.status == SyncStatus.Pending || it.status == SyncStatus.Failed }
    val activeAnimalCount = animalsInScope.count { it.status != AnimalStatus.Dead }
    val activeCageCount = cagesInScope.count { cage ->
        cage.status == CageStatus.Active && cage.animalIds.any { it in animalIdsInScope }
    }
    val estimatedCost = activeAnimalCount * COST_PER_ACTIVE_ANIMAL_USD_PER_DAY + activeCageCount * COST_PER_ACTIVE_CAGE_USD_PER_DAY
    val strainDistribution = animalsInScope
        .groupingBy { it.strain }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(6)
        .map { it.key to it.value }
    val taskStatusDistribution = tasksInScope
        .groupingBy { it.status.name }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(4)
        .map { it.key to it.value }
    val dailyTaskDoneRates = (0 until selectedDays).map { dayIndex ->
        val start = sinceMillis + dayIndex * DAY_MILLIS
        val end = start + DAY_MILLIS
        val dayTasks = snapshot.tasks.filter { it.dueAtMillis in start until end }
        if (dayTasks.isEmpty()) 0f else dayTasks.count { it.status.name == "Done" }.toFloat() / dayTasks.size.toFloat()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "报表中心",
                subtitle = "面向 PI 与管理员的资源、执行与风险视图",
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
                    Text("筛选条件", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 30, 90).forEach { day ->
                            FilterChip(
                                selected = selectedDays == day,
                                onClick = { selectedDays = day },
                                label = { Text("${day}天") },
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedStrain == null,
                                onClick = { selectedStrain = null },
                                label = { Text("全部品系") },
                            )
                        }
                        items(strainCandidates, key = { it }) { strain ->
                            FilterChip(
                                selected = selectedStrain == strain,
                                onClick = { selectedStrain = strain },
                                label = { Text(strain) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = projectFilter,
                        onValueChange = { projectFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("课题过滤（协议ID/标题）") },
                        singleLine = true,
                    )
                }
            }
        }

        item {
            MetricProgressCard(
                title = "笼位平均占用率",
                valueText = "${(occupancyRate * 100).toInt()}%",
                progress = occupancyRate,
                accent = Color(0xFF0E5BA8),
                hint = "建议维持在 70%-85% 区间",
            )
        }

        item {
            MetricProgressCard(
                title = "任务完成率",
                valueText = "${(taskCompletionRate * 100).toInt()}%",
                progress = taskCompletionRate,
                accent = Color(0xFF00796B),
                hint = "目标 >= 90%",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricProgressCard(
                    title = "繁育成功率",
                    valueText = "${(breedingSuccessRate * 100).toInt()}%",
                    progress = breedingSuccessRate,
                    accent = Color(0xFFEF6C00),
                    hint = "按断奶节点完成率估算",
                    modifier = Modifier.weight(1f),
                )
                MetricProgressCard(
                    title = "存活率",
                    valueText = "${(survivalRate * 100).toInt()}%",
                    progress = survivalRate,
                    accent = Color(0xFF2E7D32),
                    hint = "在册个体存活比例",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            DistributionChartCard(
                title = "品系分布",
                subtitle = "当前筛选范围内个体数",
                data = strainDistribution,
                accent = Color(0xFF1565C0),
            )
        }

        item {
            DistributionChartCard(
                title = "任务状态分布",
                subtitle = "统计窗口内任务状态",
                data = taskStatusDistribution,
                accent = Color(0xFF00897B),
            )
        }

        item {
            TrendChartCard(
                title = "任务完成率趋势",
                subtitle = "按天观察执行稳定性",
                points = dailyTaskDoneRates,
                accent = Color(0xFFE65100),
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
                    Text("执行概览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("活跃繁育计划: $activeBreedingPlans", style = MaterialTheme.typography.bodyMedium)
                    Text("锁定 Cohort: $activeCohorts", style = MaterialTheme.typography.bodyMedium)
                    Text("活跃实验: $activeExperiments", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "逾期任务: $overdueTasks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdueTasks > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "待同步事件: $pendingSync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (pendingSync > 0) Color(0xFFE67E22) else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "基础成本估算: $${"%.2f".format(estimatedCost)} / 天",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6A1B9A),
                    )
                }
            }
        }
    }
}

@Composable
private fun DistributionChartCard(
    title: String,
    subtitle: String,
    data: List<Pair<String, Int>>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (data.isEmpty()) {
                Text("暂无数据", style = MaterialTheme.typography.bodySmall)
            } else {
                val maxValue = data.maxOf { it.second }.coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                ) {
                    val count = data.size.coerceAtLeast(1)
                    val barGap = size.width * 0.04f
                    val barWidth = ((size.width - barGap * (count + 1)) / count).coerceAtLeast(8f)
                    data.forEachIndexed { index, (_, value) ->
                        val x = barGap + index * (barWidth + barGap)
                        val ratio = value.toFloat() / maxValue.toFloat()
                        val barHeight = size.height * ratio
                        drawRect(
                            color = accent.copy(alpha = 0.25f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, size.height),
                        )
                        drawRect(
                            color = accent,
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                        )
                    }
                }
                data.forEach { (name, value) ->
                    Text(
                        text = "$name: $value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendChartCard(
    title: String,
    subtitle: String,
    points: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (points.isEmpty()) {
                Text("暂无趋势数据", style = MaterialTheme.typography.bodySmall)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        val stepX = if (points.size <= 1) 0f else size.width / (points.size - 1).toFloat()
                        var previous: Offset? = null
                        points.forEachIndexed { index, raw ->
                            val value = raw.coerceIn(0f, 1f)
                            val current = Offset(
                                x = index * stepX,
                                y = size.height - value * size.height,
                            )
                            if (previous != null) {
                                drawLine(
                                    color = accent,
                                    start = previous!!,
                                    end = current,
                                    strokeWidth = 5f,
                                    cap = StrokeCap.Round,
                                )
                            }
                            previous = current
                        }
                        drawLine(
                            color = accent.copy(alpha = 0.2f),
                            start = Offset(0f, size.height * 0.1f),
                            end = Offset(size.width, size.height * 0.1f),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                val latest = (points.lastOrNull() ?: 0f) * 100
                Text("最新完成率: ${latest.toInt()}%", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }
    }
}

@Composable
private fun MetricProgressCard(
    title: String,
    valueText: String,
    progress: Float,
    accent: Color,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(valueText, style = MaterialTheme.typography.headlineSmall, color = accent)
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val COST_PER_ACTIVE_ANIMAL_USD_PER_DAY = 1.25
private const val COST_PER_ACTIVE_CAGE_USD_PER_DAY = 2.50
