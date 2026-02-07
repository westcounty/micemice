package com.westcounty.micemice.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateTimeFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

fun formatDateTime(millis: Long): String = dateTimeFormatter.format(Date(millis))

fun formatDate(millis: Long): String = dateFormatter.format(Date(millis))

fun daysUntil(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    val day = 24L * 60L * 60L * 1000L
    return (targetMillis - nowMillis) / day
}
