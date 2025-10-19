package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yerayyas.chatappkotlinproject.data.model.ChatListItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * A composable function that displays a single chat item in the chat list.
 *
 * It shows the other user's name, the last message sent in the conversation,
 * the number of unread messages (if any), and a formatted timestamp.
 * The whole item is clickable and triggers the [onClick] callback when tapped.
 *
 * @param chat The [ChatListItem] containing the data to be displayed.
 * @param onClick Lambda function to be executed when the item is clicked.
 * @param modifier Optional [Modifier] for customizing layout and styling.
 */
@Composable
fun ChatListItem(
    chat: ChatListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = chat.otherUsername,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatTimestamp(chat.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Formats a given timestamp into a human-readable string.
 *
 * - "Now" if less than an hour ago
 * - "{x} h" if less than 24 hours ago
 * - "{x} d" if within the last 7 days
 * - "dd/MM/yy" format otherwise
 *
 * @param timestamp The timestamp in milliseconds.
 * @return A formatted time string representing the recency of the message.
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 60 -> "Now"
        hours < 24 -> "$hours h"
        days < 7 -> "$days d"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
    }
}
