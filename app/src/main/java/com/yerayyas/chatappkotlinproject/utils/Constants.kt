package com.yerayyas.chatappkotlinproject.utils

import androidx.compose.ui.unit.dp

/**
 * Central repository for application-wide constant values.
 *
 * This object contains constant values that are actively used throughout the application,
 * organized by domain to maintain clean separation of concerns. Following Clean Architecture
 * principles and the YAGNI (You Aren't Gonna Need It) principle, only constants that are
 * actually referenced in the codebase are included here.
 *
 * Architecture Pattern: Constants Repository
 * - Centralized constant management for actively used values
 * - Domain-based organization for better maintainability
 * - Type-safe constant definitions with comprehensive documentation
 * - Immutable values ensuring consistency across the application
 * - Clear separation between UI, business logic, and infrastructure constants
 *
 * Usage Guidelines:
 * - Import specific constants rather than the entire object when possible
 * - Use these constants instead of magic numbers or hardcoded strings
 * - Only add new constants when they are actually needed and used
 * - Remove constants that are no longer referenced in the codebase
 * - Document the context and usage of each constant clearly
 */
object Constants {

    // ========== UI CONSTANTS ==========

    /**
     * Standard duration for UI animations throughout the application.
     *
     * Used for:
     * - Screen transitions
     * - Component animations (expand/collapse, fade in/out)
     * - Loading state animations
     * - Interactive feedback animations
     *
     * Value: 300 milliseconds provides smooth animations without feeling sluggish
     */
    const val ANIMATION_DURATION = 300

    /**
     * Standard height for TopAppBar components across the application.
     *
     * Used for:
     * - Main screen app bars
     * - Chat screen headers
     * - Settings screen headers
     * - Consistent vertical spacing calculations
     *
     * Value: 64dp follows Material Design guidelines for app bar height
     */
    val TOP_APP_BAR_HEIGHT = 64.dp

    // ========== BUSINESS LOGIC CONSTANTS ==========

    /**
     * Error message displayed when message loading operations fail.
     *
     * Used for:
     * - Chat message retrieval failures
     * - Network connectivity issues
     * - Database query errors
     * - User-facing error notifications
     */
    const val ERROR_LOADING_MESSAGES = "Error loading messages"

    /**
     * Error message displayed when message sending operations fail.
     *
     * Used for:
     * - Text message send failures
     * - Network transmission errors
     * - Server-side processing errors
     * - User feedback for failed send attempts
     */
    const val ERROR_SENDING_MESSAGE = "Error sending message"

    /**
     * Error message displayed when image sending operations fail.
     *
     * Used for:
     * - Image upload failures
     * - File size or format validation errors
     * - Network timeout during image transmission
     * - Storage service unavailability
     */
    const val ERROR_SENDING_IMAGE = "Error sending image"

    // ========== NOTIFICATION CONSTANTS ==========

    /**
     * Unique identifier for the chat messages notification channel.
     *
     * Used for:
     * - Creating notification channel on Android 8.0+
     * - Categorizing chat-related notifications
     * - User notification preference management
     * - System notification channel settings
     *
     * Note: Changing this value will create a new channel, orphaning existing settings
     */
    const val CHANNEL_ID = "chat_messages_channel"

    /**
     * Human-readable name for the chat messages notification channel.
     *
     * Used for:
     * - Displaying channel name in system notification settings
     * - User-facing channel identification
     * - Accessibility and user experience
     *
     * Note: This appears in the device's notification settings UI
     */
    const val CHANNEL_NAME = "Chat Messages"

    /**
     * Notification ID used for summary notifications in grouped displays.
     *
     * Used for:
     * - Grouping multiple chat notifications
     * - Summary notification management
     * - Android notification bundling
     * - Clearing grouped notifications
     *
     * Value: 0 is reserved for summary notifications by convention
     */
    const val SUMMARY_ID = 0

    /**
     * Group key for bundling related chat notifications together.
     *
     * Used for:
     * - Grouping multiple chat notifications
     * - Preventing notification spam
     * - Organized notification display
     * - Bulk notification management
     *
     * Format: Reverse domain notation for uniqueness
     */
    const val GROUP_KEY = "com.yerayyas.CHAT_MESSAGES"

    // ========== FIREBASE CONSTANTS ==========

    /**
     * Firebase Realtime Database path for storing user FCM tokens.
     *
     * Used for:
     * - Storing push notification tokens
     * - Token retrieval for message targeting
     * - User device management
     * - Notification delivery routing
     *
     * Path structure: /user_fcm_tokens/{userId}/{tokenId}
     */
    const val FCM_TOKENS_PATH = "user_fcm_tokens"
}
