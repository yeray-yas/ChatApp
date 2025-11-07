package com.yerayyas.chatappkotlinproject.utils

import androidx.compose.ui.unit.dp

/**
 * This object contains constant values used throughout the application.
 * Constants are organized by domain: UI, Business Logic, and Notifications.
 */
object Constants {

    // ========== UI CONSTANTS ==========

    /**
     * The duration of animations in milliseconds.
     */
    const val ANIMATION_DURATION = 300

    /**
     * Standard height for TopAppBar components in dp.
     */
    val TOP_APP_BAR_HEIGHT = 64.dp

    // ========== BUSINESS LOGIC CONSTANTS ==========

    /**
     * Error message displayed when there is an issue loading messages.
     */
    const val ERROR_LOADING_MESSAGES = "Error loading messages"

    /**
     * Error message displayed when there is an issue sending a message.
     */
    const val ERROR_SENDING_MESSAGE = "Error sending message"

    /**
     * Error message displayed when there is an issue sending an image.
     */
    const val ERROR_SENDING_IMAGE = "Error sending image"

    // ========== NOTIFICATION CONSTANTS ==========

    /**
     * Channel ID for chat message notifications.
     */
    const val CHANNEL_ID = "chat_messages_channel"

    /**
     * Display name for the notification channel.
     */
    const val CHANNEL_NAME = "Chat Messages"

    /**
     * Summary notification ID for grouped notifications.
     */
    const val SUMMARY_ID = 0

    /**
     * Firebase path for storing FCM tokens.
     */
    const val FCM_TOKENS_PATH = "user_fcm_tokens"

    /**
     * Group key for notification grouping.
     */
    const val GROUP_KEY = "com.yerayyas.CHAT_MESSAGES"
}
