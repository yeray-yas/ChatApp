package com.yerayyas.chatappkotlinproject.data.model

import androidx.compose.ui.focus.FocusRequester

/**
 * Represents the state of the chat input area.
 *
 * This data class is used to hoist the state of the input field, allowing the parent composable
 * to control the text and handle focus requests.
 *
 * @property messageText The current text value of the input field.
 * @property onMessageChange A callback function that is invoked when the user types in the input field.
 * @property focusRequester A [FocusRequester] that can be used to programmatically request focus for the input field.
 * @property replyToMessage The message being replied to, null if not replying.
 */
data class ChatInputState(
    val messageText: String,
    val onMessageChange: (String) -> Unit,
    val focusRequester: FocusRequester,
    val replyToMessage: ChatMessage? = null
) {
    /**
     * Convenience method to check if currently replying to a message.
     */
    fun isReplying(): Boolean = replyToMessage != null
}
