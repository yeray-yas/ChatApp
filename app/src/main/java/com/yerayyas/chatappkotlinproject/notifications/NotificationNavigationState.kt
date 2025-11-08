package com.yerayyas.chatappkotlinproject.notifications

/**
 * Represents a pending navigation event, typically triggered by a push notification.
 *
 * This data class holds all the necessary information to perform a navigation action,
 * such as the target destination route and any required parameters. The `eventId` is used
 * by the UI layer to ensure that the navigation event is consumed only once, preventing
 * duplicate navigations on configuration changes.
 *
 * Supports both individual and group chat navigation.
 *
 * @property navigateTo The destination route key (e.g., "chat", "group_chat").
 * @property userId The unique identifier of the user to navigate to (for individual chats) or the sender (for group chats).
 * @property username The display name of the user, passed as an argument to the destination screen.
 * @property eventId A unique timestamp or identifier for this specific navigation event to prevent re-consumption.
 * @property skipSplash A flag indicating whether the splash screen should be bypassed during navigation.
 * @property isInitialDestination A flag indicating if this navigation is the first one after app launch.
 * @property groupId The unique identifier of the group (for group chats, null for individual chats).
 * @property groupName The display name of the group (for group chats, null for individual chats).
 */
data class NotificationNavigationState(
    val navigateTo: String,
    val userId: String,
    val username: String,
    val eventId: Long = System.currentTimeMillis(),
    val skipSplash: Boolean = false,
    val isInitialDestination: Boolean = false,
    val groupId: String? = null,
    val groupName: String? = null
) {
    /**
     * Determines if this is a group chat navigation
     */
    val isGroupChat: Boolean
        get() = navigateTo == "group_chat" && !groupId.isNullOrBlank()

    /**
     * Gets the appropriate display name for the destination
     */
    val destinationName: String
        get() = if (isGroupChat) groupName ?: "Group" else username
}
