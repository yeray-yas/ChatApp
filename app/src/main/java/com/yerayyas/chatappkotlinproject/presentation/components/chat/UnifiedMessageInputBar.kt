package com.yerayyas.chatappkotlinproject.presentation.components.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified message input bar with support for replies and file attachments.
 *
 * This composable handles the user input field, attachment button, send button,
 * and displays the [ReplyPreview] when a message is being replied to.
 * It also includes logic to keep the input focused when typing.
 *
 * @param messageText The current text in the input field.
 * @param onMessageTextChange Callback to update the text in the ViewModel.
 * @param onSendMessage Callback to trigger sending the message.
 * @param onAttachFile Callback to launch the file picker.
 * @param replyToMessage The message being replied to, or null.
 * @param currentUserId The ID of the currently logged-in user.
 * @param chatName The name of the chat partner/group.
 * @param onClearReply Callback to clear the reply state.
 * @param isEnabled Whether the input bar is currently enabled (e.g., not loading).
 * @param modifier The modifier applied to the Card container.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedMessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
    replyToMessage: UnifiedMessage?,
    currentUserId: String,
    chatName: String,
    onClearReply: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isSendEnabled = isEnabled && messageText.trim().isNotEmpty()

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            replyToMessage?.let { replyMsg ->
                ReplyPreview(
                    replyToMessage = replyMsg,
                    currentUserId = currentUserId,
                    chatName = chatName,
                    onClearReply = onClearReply,
                    onReplyClick = { },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = onAttachFile,
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file"
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 140.dp)
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { newValue ->
                            onMessageTextChange(newValue)

                            scope.launch {
                                delay(50)
                                bringIntoViewRequester.bringIntoView()

                                if (newValue.length > messageText.length &&
                                    scrollState.value > scrollState.maxValue - 100) {
                                    scrollState.scrollTo(scrollState.maxValue)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .simpleVerticalScrollbar(
                                state = scrollState,
                                width = 4.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            .verticalScroll(scrollState)
                            .bringIntoViewRequester(bringIntoViewRequester),
                        placeholder = { Text("Type a message...") },
                        singleLine = false,
                        enabled = isEnabled,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSendMessage,
                    enabled = isSendEnabled,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (messageText.trim().isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (messageText.trim().isNotEmpty())
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Custom modifier to draw a simple vertical scrollbar alongside the content.
 *
 * The scrollbar visibility animates in and out based on scroll activity.
 *
 * @param state The [ScrollState] of the scrollable component.
 * @param width The width of the scrollbar thumb.
 * @param minHeight The minimum height of the scrollbar thumb.
 * @param rightPadding Padding from the right edge.
 * @param cornerRadius The corner radius for the scrollbar thumb.
 * @param color The color of the scrollbar thumb.
 */
fun Modifier.simpleVerticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    minHeight: Dp = 20.dp,
    rightPadding: Dp = 4.dp,
    cornerRadius: Dp = 24.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.value, state.isScrollInProgress) {
        if (state.maxValue > 0) {
            isVisible = true
            if (!state.isScrollInProgress) {
                delay(600)
                isVisible = false
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ScrollbarAlpha"
    )

    drawWithContent {
        drawContent()

        if (state.maxValue <= 0 || alpha == 0f) return@drawWithContent

        val radiusPx = cornerRadius.toPx()
        val rightPaddingPx = rightPadding.toPx()
        val minBarHeightPx = minHeight.toPx()

        val viewportHeight = this.size.height - (radiusPx * 2)
        val totalContentHeight = viewportHeight + state.maxValue

        val viewableRatio = if (totalContentHeight > 0) {
            viewportHeight / totalContentHeight
        } else {
            1f
        }

        val scrollBarHeightRaw = viewportHeight * viewableRatio
        val targetBarHeight = scrollBarHeightRaw.coerceIn(minBarHeightPx, viewportHeight)

        val scrollableTrackHeight = viewportHeight - targetBarHeight
        val scrollProgress = state.value.toFloat() / state.maxValue.toFloat()

        val offset = scrollProgress * scrollableTrackHeight
        val finalTopOffset = radiusPx + offset

        drawRoundRect(
            color = color,
            topLeft = Offset(
                x = this.size.width - width.toPx() - rightPaddingPx,
                y = finalTopOffset
            ),
            size = Size(width.toPx(), targetBarHeight),
            cornerRadius = CornerRadius(x = width.toPx() / 2, y = width.toPx() / 2),
            alpha = alpha
        )
    }
}