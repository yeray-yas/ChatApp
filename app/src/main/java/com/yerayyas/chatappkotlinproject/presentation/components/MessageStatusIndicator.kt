package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Visual indicator of message status with animations.
 *
 * This component displays the delivery and read status of messages with smooth animations.
 * It shows different icons and colors based on the message status (sent, delivered, read)
 * and includes timestamp information. The component only displays for the user's own messages.
 *
 * Key features:
 * - Animated status transitions with fade and scale effects
 * - WhatsApp-style color coding (gray for sent/delivered, green for read)
 * - Timestamp display with customizable formatting
 * - Special glow animation for read messages
 * - Only visible for user's own messages (not for received messages)
 *
 * @param readStatus Current status of the message (SENT, DELIVERED, READ)
 * @param timestamp Message timestamp in milliseconds
 * @param isOwnMessage Whether this message belongs to the current user
 * @param modifier Optional [Modifier] for customizing layout and styling
 * @param showTime Whether to display the timestamp alongside the status
 * @param animated Whether to use animations for status changes
 */
@Composable
fun MessageStatusIndicator(
    readStatus: ReadStatus,
    timestamp: Long,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier,
    showTime: Boolean = true,
    animated: Boolean = true
) {
    if (!isOwnMessage) return // Only show status on own messages

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showTime) {
            Text(
                text = formatTime(timestamp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        AnimatedVisibility(
            visible = true,
            enter = if (animated) fadeIn() + scaleIn() else EnterTransition.None,
            exit = if (animated) fadeOut() + scaleOut() else ExitTransition.None
        ) {
            StatusIcon(
                readStatus = readStatus,
                animated = animated
            )
        }
    }
}

/**
 * Status icon component with animations.
 *
 * Displays the appropriate icon and color based on message read status:
 * - SENT: Single checkmark, gray color
 * - DELIVERED: Double checkmark, gray color
 * - READ: Double checkmark, green color with glow animation
 *
 * @param readStatus Current status of the message
 * @param animated Whether to apply special animations (glow effect for read status)
 */
@Composable
private fun StatusIcon(
    readStatus: ReadStatus,
    animated: Boolean
) {
    val (icon, color, description) = when (readStatus) {
        ReadStatus.SENT -> Triple(
            Icons.Default.Done,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            "Sent"
        )

        ReadStatus.DELIVERED -> Triple(
            Icons.Default.DoneAll,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            "Delivered"
        )

        ReadStatus.READ -> Triple(
            Icons.Default.DoneAll,
            Color(0xFF00BFA5), // WhatsApp-style green
            "Read"
        )
    }

    if (animated && readStatus == ReadStatus.READ) {
        // Special animation for read messages
        val infiniteTransition = rememberInfiniteTransition(label = "read_glow")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_animation"
        )

        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier
                .size(16.dp)
                .alpha(alpha),
            tint = color
        )
    } else {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(16.dp),
            tint = color
        )
    }
}

/**
 * Formats the timestamp to time format (HH:mm).
 *
 * @param timestamp The timestamp in milliseconds to format
 * @return A formatted time string in HH:mm format
 */
private fun formatTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}