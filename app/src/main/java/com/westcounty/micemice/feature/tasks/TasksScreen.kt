package com.westcounty.micemice.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.LabTask
import com.westcounty.micemice.data.model.TaskFilter
import com.westcounty.micemice.data.model.TaskPriority
import com.westcounty.micemice.data.model.TaskStatus
import com.westcounty.micemice.data.model.TaskEscalationConfig
import com.westcounty.micemice.data.model.TaskTemplate
import com.westcounty.micemice.data.model.overdueDurationMillis
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDateTime

@Composable
fun TasksScreen(
    tasks: List<LabTask>,
    taskTemplates: List<TaskTemplate>,
    filterTasks: (List<LabTask>, TaskFilter) -> List<LabTask>,
    onCompleteTask: (String) -> Unit,
    onCompleteBatch: (List<String>) -> Unit,
    onCreateFromTemplate: (templateId: String, assignee: String?) -> Unit,
    onSaveTaskTemplate: (
        name: String,
        detail: String,
        priority: TaskPriority,
        dueInHours: Int,
        entityType: String,
    ) -> Unit,
    onDeleteTaskTemplate: (templateId: String) -> Unit,
    onReassignTask: (String, String) -> Unit,
    onReassignBatch: (List<String>, String) -> Unit,
    escalationConfig: TaskEscalationConfig,
    onSaveEscalationConfig: (
        enable24h: Boolean,
        enable48h: Boolean,
        priority24h: TaskPriority,
        priority48h: TaskPriority,
        assignee: String?,
    ) -> Unit,
    onApplyEscalationConfig: () -> Unit,
    canManage: Boolean,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(TaskFilter.Pending) }
    var batchAssignee by remember { mutableStateOf("") }
    var enable24h by remember(escalationConfig) { mutableStateOf(escalationConfig.enable24hEscalation) }
    var enable48h by remember(escalationConfig) { mutableStateOf(escalationConfig.enable48hEscalation) }
    var priority24h by remember(escalationConfig) { mutableStateOf(escalationConfig.priorityAt24h) }
    var priority48h by remember(escalationConfig) { mutableStateOf(escalationConfig.priorityAt48h) }
    var escalationAssignee by remember(escalationConfig) { mutableStateOf(escalationConfig.autoAssignOverdueTo.orEmpty()) }
    var selectedTemplateId by remember(taskTemplates) { mutableStateOf(taskTemplates.firstOrNull()?.id) }
    var templateAssignee by remember { mutableStateOf("") }
    var templateName by remember { mutableStateOf("") }
    var templateDetail by remember { mutableStateOf("") }
    var templatePriority by remember { mutableStateOf(TaskPriority.Medium) }
    var templateDueHoursText by remember { mutableStateOf("24") }
    var templateEntityType by remember { mutableStateOf("system") }
    val now = System.currentTimeMillis()
    val filteredTasks = filterTasks(tasks, filter)
    val batchTaskIds = filteredTasks.filter { it.status != TaskStatus.Done }.map { it.id }
    val overdue24Count = tasks.count { it.overdueDurationMillis(now) >= ONE_DAY }
    val overdue48Count = tasks.count { it.overdueDurationMillis(now) >= 2 * ONE_DAY }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "任务中心",
                subtitle = "统一追踪繁育节点、合规动作与异常处理",
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                    TaskFilter.Pending to "待处理",
                    TaskFilter.Overdue to "已逾期",
                    TaskFilter.Overdue24h to "24h+",
                    TaskFilter.Overdue48h to "48h+",
                    TaskFilter.Today to "今日",
                    TaskFilter.Done to "已完成",
                )
                ) { (value, label) ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { filter = value },
                        label = { Text(label) },
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "逾期24h+: $overdue24Count",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD84315),
                )
                Text(
                    text = "逾期48h+: $overdue48Count",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { onCompleteBatch(batchTaskIds) },
                enabled = batchTaskIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("批量完成当前筛选任务（${batchTaskIds.size}）")
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
                    Text("模板任务", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(taskTemplates, key = { it.id }) { template ->
                            FilterChip(
                                selected = selectedTemplateId == template.id,
                                onClick = { selectedTemplateId = template.id },
                                label = { Text(template.name) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = templateAssignee,
                        onValueChange = { templateAssignee = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("指派给（可空）") },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = {
                            val templateId = selectedTemplateId ?: return@OutlinedButton
                            onCreateFromTemplate(templateId, templateAssignee.ifBlank { null })
                        },
                        enabled = selectedTemplateId != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("创建模板任务")
                    }
                    if (canManage && selectedTemplateId != null) {
                        TextButton(
                            onClick = { onDeleteTaskTemplate(selectedTemplateId!!) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("删除当前模板")
                        }
                    }
                }
            }
        }

        if (canManage) {
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
                        Text("新建任务模板", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("模板名称") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = templateDetail,
                            onValueChange = { templateDetail = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("模板描述") },
                            minLines = 2,
                            maxLines = 3,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TaskPriority.entries, key = { it.name }) { p ->
                                FilterChip(
                                    selected = templatePriority == p,
                                    onClick = { templatePriority = p },
                                    label = { Text(p.displayName) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = templateDueHoursText,
                            onValueChange = { templateDueHoursText = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("到期小时（1-720）") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = templateEntityType,
                            onValueChange = { templateEntityType = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("实体类型") },
                            singleLine = true,
                        )
                        OutlinedButton(
                            onClick = {
                                val due = templateDueHoursText.toIntOrNull() ?: return@OutlinedButton
                                onSaveTaskTemplate(
                                    templateName,
                                    templateDetail,
                                    templatePriority,
                                    due,
                                    templateEntityType,
                                )
                                templateName = ""
                                templateDetail = ""
                                templateDueHoursText = "24"
                                templateEntityType = "system"
                            },
                            enabled = templateName.isNotBlank() && (templateDueHoursText.toIntOrNull() ?: 0) in 1..720 && templateEntityType.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("保存模板")
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
                        Text("批量指派", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = batchAssignee,
                            onValueChange = { batchAssignee = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("指派给") },
                            singleLine = true,
                        )
                        OutlinedButton(
                            onClick = { onReassignBatch(batchTaskIds, batchAssignee) },
                            enabled = batchTaskIds.isNotEmpty() && batchAssignee.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("将当前筛选任务批量指派给 ${if (batchAssignee.isBlank()) "--" else batchAssignee}")
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
                        Text("逾期升级规则", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("启用24h升级")
                            Switch(checked = enable24h, onCheckedChange = { enable24h = it })
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TaskPriority.entries, key = { it.name }) { p ->
                                FilterChip(
                                    selected = priority24h == p,
                                    onClick = { priority24h = p },
                                    label = { Text("24h:${p.displayName}") },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("启用48h升级")
                            Switch(checked = enable48h, onCheckedChange = { enable48h = it })
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TaskPriority.entries, key = { it.name }) { p ->
                                FilterChip(
                                    selected = priority48h == p,
                                    onClick = { priority48h = p },
                                    label = { Text("48h:${p.displayName}") },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = escalationAssignee,
                            onValueChange = { escalationAssignee = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("逾期自动指派给（可空）") },
                            singleLine = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    onSaveEscalationConfig(
                                        enable24h,
                                        enable48h,
                                        priority24h,
                                        priority48h,
                                        escalationAssignee.ifBlank { null },
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("保存规则")
                            }
                            OutlinedButton(
                                onClick = onApplyEscalationConfig,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("立即应用")
                            }
                        }
                    }
                }
            }
        }

        items(filteredTasks, key = { it.id }) { task ->
            val accent = when (task.priority) {
                TaskPriority.Critical -> MaterialTheme.colorScheme.error
                TaskPriority.High -> Color(0xFFEF6C00)
                TaskPriority.Medium -> Color(0xFF1565C0)
                TaskPriority.Low -> Color(0xFF00796B)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = task.priority.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = task.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = "截止 ${formatDateTime(task.dueAtMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "责任人 ${task.assignee}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val overdueMillis = task.overdueDurationMillis(now)
                    if (overdueMillis >= ONE_DAY) {
                        val levelText = if (overdueMillis >= 2 * ONE_DAY) "逾期48h+" else "逾期24h+"
                        Text(
                            text = levelText,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (overdueMillis >= 2 * ONE_DAY) MaterialTheme.colorScheme.error else Color(0xFFD84315),
                        )
                    }

                    if (task.status != TaskStatus.Done) {
                        TextButton(
                            onClick = { onCompleteTask(task.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("标记完成")
                        }
                        if (canManage && batchAssignee.isNotBlank()) {
                            TextButton(
                                onClick = { onReassignTask(task.id, batchAssignee) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("指派给 $batchAssignee")
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val ONE_DAY = 24L * 60L * 60L * 1000L
