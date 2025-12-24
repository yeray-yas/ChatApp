package com.yerayyas.chatappkotlinproject.notifications

import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes

/**
 * Represents a pending navigation event triggered by push notifications or deep-links.
 *
 * This data class encapsulates all necessary information required to perform navigation actions
 * from notification intents to specific chat screens. It acts as a bridge between the raw
 * data received from Firebase Messaging and the app's internal navigation system.
 *
 * Key features:
 * - Supports both individual and group chat navigation flows.
 * - Prevents duplicate navigation via unique [eventId].
 * - Handles splash screen skip logic via [skipSplash].
 * - dynamic route generation compatible with [Routes].
 *
 * Architecture Pattern: Data Transfer Object (DTO)
 * - Immutable data structure for passing navigation state.
 * - Follows single responsibility principle for navigation data.
 * - Provides a clear contract between notification services and the UI layer.
 *
 * @property navigateTo The destination route identifier (matches [ROUTE_INDIVIDUAL_CHAT] or [ROUTE_GROUP_CHAT]).
 * @property userId In individual chats: the recipient's ID. In group chats: the sender's ID (for context/avatar).
 * @property username In individual chats: the recipient's name. In group chats: the sender's name (for notification display).
 * @property eventId Unique timestamp identifier to prevent duplicate navigation consumption.
 * @property skipSplash Flag indicating whether to bypass the splash screen (e.g., app was already running).
 * @property isInitialDestination Flag indicating if this is the entry point of the app (cold start).
 * @property groupId Unique identifier for group chats (must be null for individual chats).
 * @property groupName Display name for group chats (must be null for individual chats).
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
     * - The [navigateTo] matches [ROUTE_GROUP_CHAT].
     * - The [groupId] is present and not blank.
     *
     * @return `true` if this is a group chat navigation, `false` for individual chat.
     */
    val isGroupChat: Boolean
        get() = navigateTo == ROUTE_GROUP_CHAT && !groupId.isNullOrBlank()

    /**
     * Provides the appropriate display name for the navigation destination.
     *
     * Logic:
     * - **Group Chat**: Returns [groupName] (or fallback "Group").
     * - **Individual Chat**: Returns [username].
     *
     * @return The display name to show in UI elements (titles, notification summaries, etc.).
     */
    val destinationName: String
        get() = if (isGroupChat) groupName ?: "Group" else username

    /**
     * Constructs the full navigation route string required by the NavController.
     *
     * This property dynamically builds the route based on the chat type:
     * - **Group Chat**: Uses [Routes.GroupChat.createRoute] with the [groupId].
     * - **Individual Chat**: Constructs the `direct_chat/{userId}/{username}` pattern.
     *
     * @return A valid route string ready to be passed to `navController.navigate()`.
     */
    val destinationRoute: String
        get() = if (isGroupChat) {
            Routes.GroupChat.createRoute(groupId ?: "")
        } else {
            "direct_chat/$userId/$username"
        }

    /**
     * Provides a descriptive string representation for debugging purposes.
     *
     * @return A formatted string containing key navigation information (excludes PII like full user IDs if strict logging is needed).
     */
    override fun toString(): String {
        return if (isGroupChat) {
            "NotificationNavigationState(type=GROUP, group='$groupName', groupId='$groupId', sender='$username', eventId=$eventId)"
        } else {
            "NotificationNavigationState(type=INDIVIDUAL, user='$username', userId='$userId', eventId=$eventId)"
        }
    }

    companion object {
        /**
         * Route identifier constant for individual chat navigation.
         */
        const val ROUTE_INDIVIDUAL_CHAT = "chat"

        /**
         * Route identifier constant for group chat navigation.
         */
        const val ROUTE_GROUP_CHAT = "group_chat"
        /**
         * Factory method to create a navigation state for individual chats.
         *
         * @param userId The unique identifier of the chat recipient.
         * @param username The display name of the chat recipient.
         * @param skipSplash Whether to skip the splash screen.
         * @param isInitialDestination Whether this is the initial navigation after app launch.
         * @return A properly configured [NotificationNavigationState].
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
         * Factory method to create a navigation state for group chats.
         *
         * @param groupId The unique identifier of the group.
         * @param groupName The display name of the group.
         * @param senderId The identifier of the message sender (for context).
         * @param senderName The display name of the message sender (for context).
         * @param skipSplash Whether to skip the splash screen.
         * @param isInitialDestination Whether this is the initial navigation after app launch.
         * @return A properly configured [NotificationNavigationState].
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
