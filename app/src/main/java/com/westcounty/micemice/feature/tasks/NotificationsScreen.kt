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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.NotificationItem
import com.westcounty.micemice.data.model.NotificationSeverity
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDateTime

private enum class NotificationFilter {
    All,
    Unread,
    Critical,
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(NotificationFilter.Unread) }
    val filtered = notifications.filter { item ->
        when (filter) {
            NotificationFilter.All -> true
            NotificationFilter.Unread -> item.readAtMillis == null
            NotificationFilter.Critical -> item.severity == NotificationSeverity.Critical || item.severity == NotificationSeverity.High
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "通知中心",
                subtitle = "聚合逾期任务、协议风险和同步失败提醒",
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        NotificationFilter.Unread to "未读",
                        NotificationFilter.Critical to "高风险",
                        NotificationFilter.All to "全部",
                    ),
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
            Button(
                onClick = onMarkAllRead,
                modifier = Modifier.fillMaxWidth(),
                enabled = notifications.any { it.readAtMillis == null },
            ) {
                Text("全部标记为已读")
            }
        }
        items(filtered, key = { it.id }) { item ->
            val accent = when (item.severity) {
                NotificationSeverity.Critical -> MaterialTheme.colorScheme.error
                NotificationSeverity.High -> Color(0xFFD84315)
                NotificationSeverity.Medium -> Color(0xFF1565C0)
                NotificationSeverity.Low -> Color(0xFF00796B)
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
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(item.severity.displayName, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(item.content, style = MaterialTheme.typography.bodyMedium)
                    Text(formatDateTime(item.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.readAtMillis == null) {
                        Button(onClick = { onMarkRead(item.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("标记已读")
                        }
                    }
                }
            }
        }
    }
}
