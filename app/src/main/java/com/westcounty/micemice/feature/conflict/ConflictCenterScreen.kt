package com.westcounty.micemice.feature.conflict

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.SyncStatus
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.util.formatDateTime

@Composable
fun ConflictCenterScreen(
    snapshot: LabSnapshot,
    onConfirmGenotyping: (String) -> Unit,
    onRetrySyncEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val genotypingConflicts = snapshot.genotypingResults.filter { it.conflict }
    val failedSync = snapshot.syncEvents.filter { it.status == SyncStatus.Failed }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "冲突中心",
                subtitle = "统一处理分型冲突与同步失败事件",
            )
        }

        item {
            Text("分型冲突 ${genotypingConflicts.size} 条", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        items(genotypingConflicts, key = { it.id }) { result ->
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
                    Text("${result.sampleId} / ${result.marker}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("call=${result.callValue} · version=${result.version}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onConfirmGenotyping(result.id) }) {
                        Text("确认分型结果")
                    }
                }
            }
        }

        item {
            Text("同步失败 ${failedSync.size} 条", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        items(failedSync, key = { it.id }) { sync ->
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
                    Text(sync.eventType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(sync.payloadSummary, style = MaterialTheme.typography.bodySmall)
                    Text("失败时间 ${formatDateTime(sync.createdAtMillis)} · 重试 ${sync.retryCount} 次", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onRetrySyncEvent(sync.id) }) {
                        Text("重试该事件")
                    }
                }
            }
        }
    }
}
