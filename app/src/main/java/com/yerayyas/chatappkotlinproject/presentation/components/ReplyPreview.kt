package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType

/**
 * Component for displaying image thumbnails in reply previews.
 *
 * @param imageUrl URL of the image to display.
 * @param isMyMessage Whether this is in a message from the current user (affects styling).
 * @param modifier Modifier for styling and layout.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ReplyImageThumbnail(
    imageUrl: String,
    isMyMessage: Boolean = false,
    modifier: Modifier = Modifier
) {
    GlideImage(
        model = imageUrl,
        contentDescription = "Reply image thumbnail",
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = if (isMyMessage) {
                    Color.White.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            ),
        contentScale = ContentScale.Crop
    )
}

/**
 * Component for displaying reply information in the input area.
 * Shows the original message being replied to with a close button.
 *
 * @param replyToMessage The message being replied to.
 * @param onClearReply Callback to clear the reply.
 * @param modifier Modifier for styling and layout.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ReplyInputPreview(
    replyToMessage: ChatMessage,
    onClearReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored line indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .size(height = 40.dp, width = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = "Replying to",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (replyToMessage.messageType == MessageType.IMAGE) {
                    // Show image thumbnail instead of icon
                    replyToMessage.imageUrl?.let { imageUrl ->
                        ReplyImageThumbnail(
                            imageUrl = imageUrl,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp)
                        )
                    } ?: run {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Image",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = when (replyToMessage.messageType) {
                        MessageType.IMAGE -> "Image"
                        MessageType.TEXT -> replyToMessage.message
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onClearReply,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear reply",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Component for displaying reply information within a chat message bubble.
 * Shows the original message being replied to in a compact format.
 * Now includes image thumbnails for image replies and click support for scrolling.
 *
 * @param replyToMessage The original message content.
 * @param replyToMessageType The type of the original message.
 * @param replyToImageUrl The URL of the original image if it's an image message.
 * @param replyToMessageId The ID of the original message being replied to.
 * @param currentUserId ID of the current user.
 * @param isMyMessage Whether this is the current user's message.
 * @param onReplyClick Callback when the reply preview is clicked to scroll to original message.
 * @param modifier Modifier for styling and layout.
 */
@Composable
fun ReplyMessagePreview(
    replyToMessage: String,
    replyToMessageType: MessageType,
    replyToImageUrl: String? = null,
    replyToMessageId: String? = null,
    currentUserId: String,
    isMyMessage: Boolean,
    onReplyClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                color = if (isMyMessage) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                }
            )
            .clickable {
                replyToMessageId?.let { messageId ->
                    onReplyClick(messageId)
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored line indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .size(height = 30.dp, width = 3.dp)
                .background(
                    color = if (isMyMessage) {
                        Color.White.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    shape = RoundedCornerShape(1.5.dp)
                )
        )

        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (replyToMessageType == MessageType.IMAGE) {
                // Show image thumbnail instead of icon
                replyToImageUrl?.let { imageUrl ->
                    ReplyImageThumbnail(
                        imageUrl = imageUrl,
                        isMyMessage = isMyMessage,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 8.dp)
                    )
                } ?: run {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Image",
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp),
                        tint = if (isMyMessage) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Text(
                text = when (replyToMessageType) {
                    MessageType.IMAGE -> "Image"
                    MessageType.TEXT -> replyToMessage
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isMyMessage) {
                    Color.White.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
