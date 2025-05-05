package com.yerayyas.chatappkotlinproject.data.model

import androidx.compose.ui.focus.FocusRequester

data class ChatInputState(
    val messageText: String,
    val onMessageChange: (String) -> Unit,
    val focusRequester: FocusRequester
)
