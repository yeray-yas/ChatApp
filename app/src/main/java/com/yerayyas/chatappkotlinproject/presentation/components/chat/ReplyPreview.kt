package com.yerayyas.chatappkotlinproject.presentation.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage

/**
 * Displays a preview of the message that the current user is replying to in the input bar.
 *
 * This component handles displaying the sender's name of the original message, the content
 * (text or image thumbnail), and a close button to clear the reply state.
 *
 * @param replyToMessage The [UnifiedMessage] being replied to.
 * @param currentUserId The ID of the currently logged-in user, used to determine if the reply is self-reply.
 * @param chatName The name of the chat partner (Individual chat) or group name, used if the original sender isn't the current user.
 * @param onClearReply Callback to dismiss the reply preview and clear the reply state in the ViewModel.
 * @param onReplyClick Optional callback to scroll to the original message when the preview is clicked.
 * @param modifier The modifier to be applied to the Card container.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ReplyPreview(
    replyToMessage: UnifiedMessage,
    currentUserId: String,
    chatName: String,
    onClearReply: () -> Unit,
    onReplyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Determine content based on the UnifiedMessage type
    val (imageUrl, messageContent, isImageMessage) = when (replyToMessage) {
        is UnifiedMessage.Individual -> Triple(
            replyToMessage.imageUrl,
            replyToMessage.content,
            replyToMessage.messageType == MessageType.IMAGE
        )
        is UnifiedMessage.Group -> Triple(
            replyToMessage.imageUrl,
            replyToMessage.content,
            replyToMessage.messageType == MessageType.IMAGE
        )
        is UnifiedMessage.Pending -> Triple(null, "Loading reply...", false) // Default for pending
    }

    Card(
        modifier = modifier.clickable { onReplyClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val replyText = when (replyToMessage) {
                    is UnifiedMessage.Individual -> {
                        if (replyToMessage.senderId == currentUserId) {
                            "Replying to yourself"
                        } else {
                            "Replying to $chatName"
                        }
                    }
                    is UnifiedMessage.Group -> {
                        if (replyToMessage.senderId == currentUserId) {
                            "Replying to yourself"
                        } else {
                            val senderName = replyToMessage.getSenderName() ?: "User"
                            "Replying to $senderName"
                        }
                    }
                    is UnifiedMessage.Pending -> "Replying..."
                }

                Text(
                    text = replyText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Show message content with thumbnail if it's an image
                if (isImageMessage && !imageUrl.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        GlideImage(
                            model = imageUrl,
                            contentDescription = "Image thumbnail",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = if (messageContent.isNotEmpty() && messageContent.isNotBlank()) {
                                messageContent
                            } else {
                                "📷 Image"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Normal text message
                    Text(
                        text = messageContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Close button
            IconButton(onClick = onClearReply) {
                Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}