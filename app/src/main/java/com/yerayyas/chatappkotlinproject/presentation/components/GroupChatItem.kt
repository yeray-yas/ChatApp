package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable that represents a group chat item in the chat list.
 *
 * This component displays essential group chat information including the group name,
 * last message or description, member count, last activity timestamp, and a default
 * group icon. It handles click events to navigate to the specific group chat.
 *
 * Key features:
 * - Displays group name with text overflow handling
 * - Shows last message or group description as fallback
 * - Indicates member count
 * - Formats timestamps in a user-friendly way
 * - Uses Material Design 3 components for consistent styling
 * - Supports click navigation to group chat screen
 *
 * @param groupChat The [GroupChat] data to display
 * @param onGroupClick Callback invoked when the group item is clicked, receives group ID
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@Composable
fun GroupChatItem(
    groupChat: GroupChat,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = { onGroupClick(groupChat.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group avatar
            GroupAvatar(
                groupImageUrl = groupChat.imageUrl,
                groupName = groupChat.name,
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Group information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Group name
                    Text(
                        text = groupChat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Last activity date
                    Text(
                        text = formatTimestamp(groupChat.lastActivity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Additional information
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message or description
                    Text(
                        text = groupChat.lastMessage?.message ?: groupChat.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Member count
                    Text(
                        text = "${groupChat.memberIds.size} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Group avatar component with default group icon.
 *
 * Currently displays a default group icon since image loading is not implemented.
 * Future enhancement: Add support for custom group images.
 *
 * @param groupImageUrl URL of the group image (currently unused)
 * @param groupName Name of the group for accessibility
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@Composable
private fun GroupAvatar(
    groupImageUrl: String?,
    groupName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Default avatar with group icon (no image support yet)
        Card(
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Group $groupName",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Formats timestamp to display date in a user-friendly way.
 *
 * Formatting rules:
 * - "Now" if less than 1 hour ago
 * - "{x}h" if less than 24 hours ago
 * - "Yesterday" if less than 48 hours ago
 * - "dd/MM" format for older dates
 * - Empty string for invalid timestamps (0 or negative)
 *
 * @param timestamp The timestamp in milliseconds to format
 * @return A formatted, user-friendly time string
 */
private fun formatTimestamp(timestamp: Long): String {
    return if (timestamp > 0) {
        val date = Date(timestamp)
        val now = Date()
        val diffInMillis = now.time - date.time
        val diffInHours = diffInMillis / (1000 * 60 * 60)

        when {
            diffInHours < 1 -> "Now"
            diffInHours < 24 -> "${diffInHours}h"
            diffInHours < 48 -> "Yesterday"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
        }
    } else {
        ""
    }
}