package com.yerayyas.chatappkotlinproject.data.model

import androidx.compose.ui.focus.FocusRequester

/**
 * Represents the state of the chat input bar component.
 *
 * This data class is used to hoist the state of the input field, allowing the parent composable
 * to control the text and handle focus requests.
 *
 * @property messageText The current text value of the input field.
 * @property onMessageChange A callback function that is invoked when the user types in the input field.
 * @property focusRequester A [FocusRequester] that can be used to programmatically request focus for the input field.
 */
data class ChatInputState(
    val messageText: String,
    val onMessageChange: (String) -> Unit,
    val focusRequester: FocusRequester
)
