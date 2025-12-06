package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
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
 * - Indicates member count with proper formatting
 * - Formats timestamps in a user-friendly way
 * - Uses Material Design 3 components for consistent styling
 * - Supports click navigation to group chat screen
 * - Displays group avatar with default icon
 *
 * @param groupChat The [GroupChat] data to display
 * @param onGroupClick Callback invoked when the group item is clicked, receives group ID
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@Composable
fun GroupChatItem(
    groupChat: GroupChat,
    unreadCount: Int,
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
                .clickable { onGroupClick(groupChat.id) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupAvatar(
                groupImageUrl = groupChat.imageUrl,
                groupName = groupChat.name,
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            GroupInfoSection(
                groupChat = groupChat,
                unreadCount = unreadCount,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Group information section component.
 *
 * Displays the group name, last message/description, member count, and timestamp
 * in an organized layout with proper text overflow handling.
 *
 * @param groupChat The group chat data to display
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@Composable
private fun GroupInfoSection(
    groupChat: GroupChat,
    modifier: Modifier = Modifier,
    unreadCount: Int
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = groupChat.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatTimestamp(groupChat.lastActivity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = groupChat.lastMessage?.message ?: groupChat.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                UnreadCountBadge(count = unreadCount)
            }
        }
    }
}

/**
 * Group avatar component with default group icon.
 *
 * Currently displays a default group icon since image loading is not implemented.
 * The avatar uses a circular design with consistent styling and proper accessibility.
 *
 * Future enhancement: Add support for custom group images with Glide integration.
 *
 * @param groupImageUrl URL of the group image (currently unused but prepared for future use)
 * @param groupName Name of the group for accessibility purposes
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun GroupAvatar(
    groupImageUrl: String?,
    groupName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(50.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        if (!groupImageUrl.isNullOrEmpty()) {
            GlideImage(
                model = groupImageUrl,
                contentDescription = "Avatar of group $groupName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Default avatar for group $groupName",
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
 * The formatting rules provide intuitive time representations:
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