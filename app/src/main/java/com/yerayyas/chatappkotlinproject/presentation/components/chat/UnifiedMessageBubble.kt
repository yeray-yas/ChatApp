package com.yerayyas.chatappkotlinproject.presentation.components.chat

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.cache.ImageUrlStore
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.presentation.components.MessageStatusIndicator
import com.yerayyas.chatappkotlinproject.presentation.util.bubbleFormatTimestamp
import com.yerayyas.chatappkotlinproject.presentation.util.formatMessageWithMentions
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.MessageDeliveryStatus
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage

/**
 * Unified message bubble component that displays message content, sender, timestamp,
 * status, and reply context, adapting its appearance based on the sender and chat type.
 *
 * This component is highly coupled with application-specific models (UnifiedMessage, ImageUrlStore)
 * and navigates directly, which is common for Composable screens but should ideally be delegated
 * to a parent composable or ViewModel callback for cleaner architecture.
 *
 * @param message The [UnifiedMessage] data model to display.
 * @param isFromCurrentUser True if the message was sent by the current user.
 * @param isGroup True if the chat is a group chat.
 * @param currentUserId The ID of the currently logged-in user.
 * @param onLongPress Callback for handling long-press gestures (e.g., initiating reply).
 * @param onReplyClick Callback to navigate/scroll to the original quoted message.
 * @param highlightedMessageId ID of the message currently being highlighted (e.g., from a scroll-to-reply action).
 * @param navController The Navigation controller for navigating to image fullscreen view.
 * @param chatName The name of the chat partner/group.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun UnifiedMessageBubble(
    message: UnifiedMessage,
    isFromCurrentUser: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    onLongPress: () -> Unit,
    onReplyClick: () -> Unit,
    highlightedMessageId: String?,
    navController: NavHostController,
    chatName: String
) {
    val isHighlighted = message.id == highlightedMessageId
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart

    // --- Animation Logic for Background and Text Color ---

    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            Color(0xFFFFE082) // Highlight color
        } else {
            if (isFromCurrentUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 500),
        label = "MessageHighlight"
    )

    val textColor by animateColorAsState(
        targetValue = if (isHighlighted) Color.Black
        else if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 500),
        label = "TextColorHighlight"
    )

    // --- Layout ---

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 2.dp)
                .combinedClickable(
                    onClick = { /* Standard click currently does nothing */ },
                    onLongClick = onLongPress
                ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Reply Bubble Content (Quoted Message)
                if (message.isReply) {
                    ReplyBubbleContent( // Now an external component
                        message = message,
                        isFromCurrentUser = isFromCurrentUser,
                        currentUserId = currentUserId,
                        chatName = chatName,
                        onReplyClick = onReplyClick,
                        isParentHighlighted = isHighlighted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Sender Name (for Group Chats, not own messages)
                if (isGroup && !isFromCurrentUser) {
                    val senderName = message.getSenderName() ?: "User"
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Message Content (Text or Image)
                when (message.messageType) {
                    MessageType.TEXT -> {
                        val content = if (isGroup) {
                            formatMessageWithMentions(message.content) // Extracted utility
                        } else {
                            AnnotatedString(message.content)
                        }

                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    MessageType.IMAGE -> {
                        if (message.imageUrl == "PENDING_UPLOAD") {
                            // Display progress indicator for messages being received
                            ImagePlaceholderWithIndicator(
                                message = "Receiving photo...",
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else {
                            ImageContent(
                                message = message,
                                navController = navController,
                                isFromCurrentUser = isFromCurrentUser,
                                textColor = textColor
                            )
                        }
                    }
                }

                // Timestamp and message status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFromCurrentUser) {
                        // Show status indicator or time for own messages
                        if (message.deliveryStatus == MessageDeliveryStatus.SENT) {
                            MessageStatusIndicator(
                                readStatus = message.getReadStatus(),
                                timestamp = message.timestamp,
                                isOwnMessage = true,
                                showTime = true,
                                animated = true
                            )
                        } else if (message.deliveryStatus == MessageDeliveryStatus.SENDING) {
                            // For images/pending messages, just show time
                            Text(
                                text = bubbleFormatTimestamp(message.timestamp), // Extracted utility
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        // Show only time for received messages
                        Text(
                            text = bubbleFormatTimestamp(message.timestamp), // Extracted utility
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}


/**
 * Helper composable to encapsulate image rendering logic (Image Type.IMAGE case).
 * This could be further extracted to its own file later.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ImageContent(
    message: UnifiedMessage,
    navController: NavHostController,
    isFromCurrentUser: Boolean,
    textColor: Color
) {
    val imageModel = message.localUri ?: message.imageUrl

    if (imageModel != null) {
        Box(contentAlignment = Alignment.Center) {
            GlideImage(
                model = imageModel,
                contentDescription = "Message image",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = message.deliveryStatus == MessageDeliveryStatus.SENT) {
                        try {
                            val imageId = imageModel.hashCode().toString()
                            message.imageUrl?.let { url ->
                                if (url != "PENDING_UPLOAD") {
                                    // Store URL in a temporary cache for the destination screen
                                    ImageUrlStore.addImageUrl(imageId, url)
                                    navController.navigate("fullScreenImage/$imageId")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("UnifiedMessageBubble", "Navigation error", e)
                        }
                    }
            )

            // Overlay for messages currently being sent
            if (message.deliveryStatus == MessageDeliveryStatus.SENDING) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

/**
 * Helper composable to show a placeholder with a loading indicator for received images
 * that are still being downloaded.
 */
@Composable
private fun ImagePlaceholderWithIndicator(message: String, textColor: Color) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}