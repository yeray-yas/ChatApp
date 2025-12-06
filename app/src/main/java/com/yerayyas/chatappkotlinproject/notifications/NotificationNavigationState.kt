package com.yerayyas.chatappkotlinproject.notifications

/**
 * Represents a pending navigation event triggered by push notifications or deep-links.
 *
 * This data class encapsulates all necessary information required to perform navigation actions
 * from notification intents to specific chat screens. It supports both individual and group chat
 * navigation while ensuring events are consumed only once to prevent duplicate navigations
 * during configuration changes or app state transitions.
 *
 * Key features:
 * - Supports both individual and group chat navigation flows
 * - Prevents duplicate navigation with unique event identifiers
 * - Handles splash screen skip logic for better user experience
 * - Provides intelligent display name resolution based on chat type
 * - Maintains compatibility with existing notification infrastructure
 *
 * Architecture Pattern: Data Transfer Object (DTO)
 * - Immutable data structure for passing navigation state
 * - Contains computed properties for derived values
 * - Follows single responsibility principle for navigation data
 * - Provides clear contract between notification and navigation layers
 *
 * @property navigateTo The destination route identifier (e.g., "chat" for individual, "group_chat" for groups)
 * @property userId User identifier for individual chats, or sender identifier for group chats
 * @property username Display name for individual chats, or sender name for group chats
 * @property eventId Unique timestamp identifier to prevent duplicate navigation consumption
 * @property skipSplash Flag indicating whether to bypass splash screen during navigation
 * @property isInitialDestination Flag indicating if this is the first navigation after app launch
 * @property groupId Unique identifier for group chats (null for individual chats)
 * @property groupName Display name for group chats (null for individual chats)
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
     * Determines if this navigation state represents a group chat.
     *
     * A navigation is considered a group chat if:
     * - The destination route is "group_chat"
     * - The group ID is present and not blank
     *
     * @return true if this is a group chat navigation, false for individual chat
     */
    val isGroupChat: Boolean
        get() = navigateTo == "group_chat" && !groupId.isNullOrBlank()

    /**
     * Provides the appropriate display name for the navigation destination.
     *
     * For group chats, returns the group name with a fallback to "Group".
     * For individual chats, returns the username.
     *
     * @return The display name to show in UI elements (titles, notifications, etc.)
     */
    val destinationName: String
        get() = if (isGroupChat) groupName ?: "Group" else username

    /**
     * Provides a descriptive string representation for debugging purposes.
     *
     * @return A formatted string containing key navigation information
     */
    override fun toString(): String {
        return if (isGroupChat) {
            "NotificationNavigationState(group='$groupName', groupId='$groupId', sender='$username', eventId=$eventId)"
        } else {
            "NotificationNavigationState(user='$username', userId='$userId', eventId=$eventId)"
        }
    }

    companion object {
        /**
         * Route identifier for individual chat navigation.
         */
        const val ROUTE_INDIVIDUAL_CHAT = "chat"

        /**
         * Route identifier for group chat navigation.
         */
        const val ROUTE_GROUP_CHAT = "group_chat"

        /**
         * Creates a navigation state for individual chat.
         *
         * @param userId The unique identifier of the chat recipient
         * @param username The display name of the chat recipient
         * @param skipSplash Whether to skip the splash screen
         * @param isInitialDestination Whether this is the initial navigation after app launch
         * @return A properly configured [NotificationNavigationState] for individual chat
         */
        fun forIndividualChat(
            userId: String,
            username: String,
            skipSplash: Boolean = false,
            isInitialDestination: Boolean = false
        ): NotificationNavigationState {
            return NotificationNavigationState(
                navigateTo = ROUTE_INDIVIDUAL_CHAT,
                userId = userId,
                username = username,
                skipSplash = skipSplash,
                isInitialDestination = isInitialDestination
            )
        }

        /**
         * Creates a navigation state for group chat.
         *
         * @param groupId The unique identifier of the group
         * @param groupName The display name of the group
         * @param senderId The identifier of the message sender
         * @param senderName The display name of the message sender
         * @param skipSplash Whether to skip the splash screen
         * @param isInitialDestination Whether this is the initial navigation after app launch
         * @return A properly configured [NotificationNavigationState] for group chat
         */
        fun forGroupChat(
            groupId: String,
            groupName: String,
            senderId: String,
            senderName: String,
            skipSplash: Boolean = false,
            isInitialDestination: Boolean = false
        ): NotificationNavigationState {
            return NotificationNavigationState(
                navigateTo = ROUTE_GROUP_CHAT,
                userId = senderId,
                username = senderName,
                skipSplash = skipSplash,
                isInitialDestination = isInitialDestination,
                groupId = groupId,
                groupName = groupName
            )
        }
    }
}
