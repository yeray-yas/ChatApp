package com.yerayyas.chatappkotlinproject.utils

/**
 * This object contains constant values used throughout the application.
 *
 * Constants include error messages for loading and sending messages or images,
 * as well as a constant for animation duration.
 */
object Constants {
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

    /**
     * The duration of animations in milliseconds.
     */
    const val ANIMATION_DURATION = 300


    const val CHANNEL_ID = "chat_messages_channel"
    const val CHANNEL_NAME = "Chat Messages"
    const val SUMMARY_NOTIFICATION_ID = 0

    const val FCM_TOKENS_PATH = "user_fcm_tokens"
}
