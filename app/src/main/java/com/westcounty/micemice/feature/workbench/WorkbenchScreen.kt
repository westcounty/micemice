package com.westcounty.micemice.feature.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.TaskStatus
import com.westcounty.micemice.data.model.dashboardStats
import com.westcounty.micemice.ui.components.MetricCard
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDateTime

@Composable
fun WorkbenchScreen(
    snapshot: LabSnapshot,
    unreadNotificationCount: Int,
    onCompleteTask: (String) -> Unit,
    onOpenCohort: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = snapshot.dashboardStats()
    val pendingTasks = snapshot.tasks
        .filter { it.status != TaskStatus.Done }
        .sortedWith(compareBy({ it.priority.rank }, { it.dueAtMillis }))
        .take(5)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "今日任务全景",
                subtitle = "优先处理合规、断奶与逾期任务",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "活跃笼位",
                    value = stats.activeCages.toString(),
                    accent = Color(0xFF00796B),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "在册个体",
                    value = stats.totalAnimals.toString(),
                    accent = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "高优任务",
                    value = stats.urgentTasks.toString(),
                    accent = Color(0xFFEF6C00),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "待同步",
                    value = stats.pendingSyncCount.toString(),
                    accent = Color(0xFFAD1457),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            MetricCard(
                title = "未读通知",
                value = unreadNotificationCount.toString(),
                accent = if (unreadNotificationCount > 0) Color(0xFFD84315) else Color(0xFF00796B),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onOpenCohort, modifier = Modifier.weight(1f)) {
                    Text("构建 Cohort")
                }
                Button(onClick = onOpenReports, modifier = Modifier.weight(1f)) {
                    Text("查看报表")
                }
                Button(onClick = onOpenNotifications, modifier = Modifier.weight(1f)) {
                    Text("通知中心")
                }
            }
        }

        item {
            SectionHeader(
                title = "待办任务",
                subtitle = "按优先级排序，建议先处理关键事项",
            )
        }

        items(pendingTasks, key = { it.id }) { task ->
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
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = task.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "截止 ${formatDateTime(task.dueAtMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AssistChip(
                            onClick = { onCompleteTask(task.id) },
                            label = { Text("标记完成") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}
