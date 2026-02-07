package com.westcounty.micemice.ui.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.westcounty.micemice.R

private const val CHANNEL_ID_ALERTS = "micemice_alerts"
private const val NOTIFICATION_ID_ALERTS = 12001

fun notifyUnreadAlerts(context: Context, unreadCount: Int) {
    if (unreadCount <= 0) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ALERTS)
        return
    }
    ensureAlertChannel(context)
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Micemice 风险提醒")
        .setContentText("当前有 $unreadCount 条未读通知，请及时处理")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ALERTS, notification)
}

private fun ensureAlertChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val existing = manager.getNotificationChannel(CHANNEL_ID_ALERTS)
    if (existing != null) return
    val channel = NotificationChannel(
        CHANNEL_ID_ALERTS,
        "Micemice 风险通知",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "协议到期、任务逾期、同步失败等高风险提醒"
    }
    manager.createNotificationChannel(channel)
}
