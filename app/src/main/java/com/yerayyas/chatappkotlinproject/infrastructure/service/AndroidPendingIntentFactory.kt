package com.yerayyas.chatappkotlinproject.infrastructure.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.yerayyas.chatappkotlinproject.domain.service.PendingIntentFactory
import com.yerayyas.chatappkotlinproject.presentation.activity.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [PendingIntentFactory] domain service.
 *
 * This infrastructure service creates Android PendingIntent objects for notification actions,
 * effectively isolating Android platform dependencies from the domain layer. The service
 * handles deep-link navigation from notifications to specific chat screens with proper
 * intent management and collision avoidance.
 *
 * Key responsibilities:
 * - **Intent Creation**: Generate Android PendingIntent objects for notifications
 * - **Deep-link Navigation**: Configure intents for direct navigation to chat screens
 * - **Collision Avoidance**: Ensure unique request codes prevent intent conflicts
 * - **Platform Isolation**: Keep Android Intent dependencies out of domain layer
 * - **Multi-chat Support**: Handle both individual and group chat navigation
 *
 * The service follows Clean Architecture principles by:
 * - Implementing the domain service interface without domain dependencies
 * - Providing platform-specific implementations for abstract contracts
 * - Enabling testability through dependency injection and interface abstraction
 * - Supporting clean separation between notification and navigation concerns
 *
 * Navigation scenarios supported:
 * - **Individual Chats**: Direct navigation to one-on-one chat conversations
 * - **Group Chats**: Direct navigation to group chat conversations with context
 * - **Context Preservation**: Maintain sender and chat information for proper display
 * - **Activity Management**: Handle proper activity stack management for notifications
 *
 * The factory generates unique request codes to prevent PendingIntent collisions
 * when multiple notifications are active simultaneously.
 */
@Singleton
class AndroidPendingIntentFactory @Inject constructor(
    @ApplicationContext private val context: Context
) : PendingIntentFactory {

    /**
     * Creates an Android PendingIntent for notification tap actions with chat navigation.
     *
     * This method generates platform-specific PendingIntent objects that enable direct
     * navigation from notifications to the appropriate chat screen. The intent configuration
     * varies based on whether the target is an individual chat or group chat.
     *
     * Intent configuration features:
     * - **Activity Flags**: NEW_TASK and CLEAR_TASK for proper notification handling
     * - **Navigation Data**: Chat type, IDs, and display names for screen routing
     * - **Context Preservation**: Maintain sender information for UI display
     * - **Unique Request Codes**: Prevent PendingIntent collisions across notifications
     *
     * Individual chat navigation:
     * - Uses sender ID as primary navigation parameter
     * - Configures intent extras for individual chat screen
     * - Preserves chat context for message continuation
     *
     * Group chat navigation:
     * - Uses group ID as primary navigation parameter
     * - Includes group name and sender context
     * - Maintains participant information for group display
     *
     * @param senderId Unique identifier of the message sender for context
     * @param senderName Display name of the sender for UI presentation
     * @param chatId Unique identifier for the chat (user ID for individual, group ID for group)
     * @param isGroupMessage Whether this intent targets a group chat (default: false)
     * @param groupName Display name of the group (required for group chats)
     * @return Android PendingIntent configured for the appropriate chat navigation
     */
    override fun createChatPendingIntent(
        senderId: String,
        senderName: String,
        chatId: String,
        isGroupMessage: Boolean,
        groupName: String?
    ): Any {
        val intent = Intent(context, MainActivity::class.java).apply {
            // Configure activity flags for proper notification handling
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            if (isGroupMessage) {
                // Configure intent extras for group chat navigation
                putExtra("navigateTo", "group_chat")
                putExtra("groupId", chatId)
                putExtra("groupName", groupName ?: "Group")
                putExtra("senderId", senderId) // Include sender context for group display
                putExtra("senderName", senderName)
            } else {
                // Configure intent extras for individual chat navigation
                putExtra("navigateTo", "chat")
                putExtra("userId", senderId)
                putExtra("username", senderName)
                putExtra("chatId", chatId)
            }
        }

        // Generate unique request code to prevent PendingIntent collisions
        // Uses combination of sender, chat ID, and chat type for uniqueness
        val requestCode = generateUniqueRequestCode(senderId, chatId, isGroupMessage)

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Generates a unique request code for PendingIntent creation.
     *
     * This method creates deterministic request codes based on chat parameters
     * to ensure each notification gets a unique PendingIntent while maintaining
     * consistency for the same chat across notification updates.
     *
     * @param senderId The message sender identifier
     * @param chatId The chat identifier (user ID or group ID)
     * @param isGroupMessage Whether this is for a group chat
     * @return Unique integer request code for PendingIntent creation
     */
    private fun generateUniqueRequestCode(
        senderId: String,
        chatId: String,
        isGroupMessage: Boolean
    ): Int {
        val chatType = if (isGroupMessage) "group" else "individual"
        return (senderId + chatId + chatType).hashCode()
    }
}
