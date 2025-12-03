package com.yerayyas.chatappkotlinproject.presentation.components.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage

/**
 * Component that shows the original message content inside a reply bubble.
 *
 * This component is responsible for displaying the quoted message details, including
 * the sender's name of the original message, the content (text or image thumbnail),
 * and applying visual styling (vertical line, background color) based on the theme
 * and highlight state.
 *
 * @param message The [UnifiedMessage] containing the reply metadata.
 * @param isFromCurrentUser True if the parent message (the reply) was sent by the current user.
 * @param currentUserId The ID of the current user.
 * @param chatName The name of the chat partner, used for individual chat sender identity.
 * @param onReplyClick Callback to scroll to the original quoted message.
 * @param isParentHighlighted True if the message bubble containing this content is currently highlighted.
 * @param modifier The modifier to be applied to the container.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ReplyBubbleContent(
    message: UnifiedMessage,
    isFromCurrentUser: Boolean,
    currentUserId: String,
    chatName: String,
    onReplyClick: () -> Unit,
    isParentHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    // Get original message information
    val (originalContent, originalImageUrl, originalMessageType) = when (message) {
        is UnifiedMessage.Individual -> {
            Triple(
                message.message.replyToMessage,
                message.message.replyToImageUrl,
                message.message.replyToMessageType
            )
        }

        is UnifiedMessage.Group -> {
            val replyMsg = message.message.replyToMessage
            Triple(
                replyMsg?.message ?: "Message",
                replyMsg?.imageUrl,
                replyMsg?.messageType
            )
        }

        is UnifiedMessage.Pending -> Triple(
            "Replying...",
            null,
            MessageType.TEXT
        )
    }

    val originalSenderName = when (message) {
        is UnifiedMessage.Individual -> {
            val originalSenderId = message.message.replyToSenderId
            if (originalSenderId == currentUserId) "You" else chatName
        }

        is UnifiedMessage.Group -> {
            val originalSenderId = message.message.replyToMessage?.senderId
            val originalSenderName = message.message.replyToMessage?.senderName

            if (originalSenderId == currentUserId) {
                "You"
            } else {
                originalSenderName ?: "User"
            }
        }

        is UnifiedMessage.Pending -> "..."
    }

    // Determine if the original message is an image
    val isImageReply = when (originalMessageType) {
        MessageType.IMAGE -> true
        com.yerayyas.chatappkotlinproject.data.model.GroupMessageType.IMAGE -> true
        else -> false
    }

    // Colors that adapt to the message theme
    val replyBackgroundColor by animateColorAsState(
        targetValue = if (isFromCurrentUser) {
            if (isParentHighlighted) {
                Color(0xFFFFE082).copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
            }
        } else {
            if (isParentHighlighted) {
                Color(0xFFFFE082).copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "ReplyBackgroundHighlight"
    )

    val replyTextColor by animateColorAsState(
        targetValue = if (isParentHighlighted) {
            Color.Black
        } else {
            if (isFromCurrentUser) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "ReplyTextHighlight"
    )

    val replyLineColor by animateColorAsState(
        targetValue = if (isFromCurrentUser) {
            if (isParentHighlighted) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
        } else {
            if (isParentHighlighted) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.primary
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "ReplyLineHighlight"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = replyBackgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .clickable { onReplyClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical line characteristic of replies
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (isImageReply) 48.dp else 40.dp) // Adjusted height for images
                    .background(
                        color = replyLineColor,
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Original sender name
                Text(
                    text = originalSenderName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = replyLineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Original message content
                if (isImageReply && !originalImageUrl.isNullOrEmpty()) {
                    // Show small row with thumbnail + "Image" text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Thumbnail of the image
                        GlideImage(
                            model = originalImageUrl,
                            contentDescription = "Image thumbnail",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // "Image" text or caption if it exists
                        Text(
                            text = if (!originalContent.isNullOrEmpty() && originalContent != "Message") {
                                originalContent // Show caption if it exists
                            } else {
                                "📷 Image" // Show "Image" with emoji if no caption
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = replyTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Normal text message
                    Text(
                        text = originalContent ?: "Message",
                        style = MaterialTheme.typography.bodySmall,
                        color = replyTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}