package com.yerayyas.chatappkotlinproject.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a given timestamp into a human-readable, relative time string.
 *
 * This utility function provides a consistent way to display time across the application,
 * following a progressive pattern based on recency.
 *
 * - "Now" if less than an hour ago.
 * - "{x}h" if less than 24 hours ago (e.g., "3h").
 * - "Yesterday" if within the last 48 hours.
 * - "{x}d" if within the last 7 days (e.g., "4d").
 * - "dd/MM/yy" for any older dates (e.g., "21/09/25").
 *
 * @param timestamp The timestamp in milliseconds since the epoch.
 * @return A formatted, human-readable time string. Returns an empty string if the timestamp is invalid (<= 0).
 */
fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = diff / (60 * 1000)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes" + "m"
        hours < 24 -> "$hours" + "h"
        days < 2 -> "Yesterday"
        days < 7 -> "$days" + "d"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}
