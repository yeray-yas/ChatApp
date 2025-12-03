package com.yerayyas.chatappkotlinproject.presentation.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Format timestamp to show time
 */
fun bubbleFormatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val calendar = java.util.Calendar.getInstance()

    // Set calendar to the message date
    calendar.time = date
    val messageDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val messageYear = calendar.get(java.util.Calendar.YEAR)

    // Set calendar to current date
    calendar.time = now
    val currentDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val currentYear = calendar.get(java.util.Calendar.YEAR)

    return if (messageDay == currentDay && messageYear == currentYear) {
        // Same day - show only time
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        // Different day - show date and time
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    }
}