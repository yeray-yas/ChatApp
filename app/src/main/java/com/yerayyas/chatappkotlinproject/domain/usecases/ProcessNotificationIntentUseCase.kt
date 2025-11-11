package com.yerayyas.chatappkotlinproject.domain.usecases

import android.content.Intent
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case that processes incoming intents to extract deep-link navigation data.
 *
 * This use case is responsible for parsing intent extras from notification taps,
 * validating the required data, and transforming it into structured navigation state.
 * It decouples the Activity layer from specific knowledge of intent extras and follows
 * Clean Architecture principles.
 *
 * Key responsibilities:
 * - Parsing and validating intent extras for navigation data
 * - Supporting both individual and group chat navigation flows
 * - Creating properly structured NotificationNavigationState objects
 * - Providing null safety and error handling for malformed intents
 * - Using factory methods for consistent object creation
 *
 * Architecture Pattern: Domain Use Case
 * - Encapsulates intent processing business logic
 * - Provides clean interface between infrastructure and domain layers
 * - Uses factory methods for consistent object creation
 * - Handles validation and error scenarios gracefully
 * - Maintains separation of concerns for notification processing
 *
 * Supported navigation types:
 * - Individual chats: Requires "navigateTo"="chat", "userId", and "username"
 * - Group chats: Requires "navigateTo"="group_chat", "groupId", "groupName", "senderId", "senderName"
 */
@Singleton
class ProcessNotificationIntentUseCase @Inject constructor() {

    /**
     * Processes the provided intent to extract and validate navigation data.
     *
     * This method parses intent extras and creates appropriate navigation state objects
     * using factory methods for consistent object creation. It validates all required
     * fields and returns null for malformed or incomplete intents.
     *
     * Intent processing flow:
     * 1. Validate intent is not null
     * 2. Extract destination type from "navigateTo" extra
     * 3. Route to appropriate processing method based on destination
     * 4. Validate required fields for each navigation type
     * 5. Create navigation state using appropriate factory method
     *
     * @param intent The incoming intent to process, may be null
     * @return A [NotificationNavigationState] instance if valid navigation data is found, null otherwise
     */
    operator fun invoke(intent: Intent?): NotificationNavigationState? {
        intent ?: return null

        val destination = intent.getStringExtra("navigateTo")

        return when (destination) {
            NotificationNavigationState.ROUTE_INDIVIDUAL_CHAT -> {
                processIndividualChatIntent(intent)
            }

            NotificationNavigationState.ROUTE_GROUP_CHAT -> {
                processGroupChatIntent(intent)
            }

            else -> {
                // Unknown or unsupported destination
                null
            }
        }
    }

    /**
     * Processes intent data for individual chat navigation.
     *
     * Validates required fields and creates navigation state using the factory method
     * for consistent object creation and proper field validation.
     *
     * @param intent The intent containing individual chat navigation data
     * @return NotificationNavigationState for individual chat or null if invalid
     */
    private fun processIndividualChatIntent(intent: Intent): NotificationNavigationState? {
        val userId = intent.getStringExtra("userId")
        val username = intent.getStringExtra("username")

        return if (userId != null && username != null) {
            NotificationNavigationState.forIndividualChat(
                userId = userId,
                username = username
            )
        } else {
            // Missing required fields for individual chat
            null
        }
    }

    /**
     * Processes intent data for group chat navigation.
     *
     * Validates required fields and creates navigation state using the factory method
     * for consistent object creation and proper field validation.
     *
     * @param intent The intent containing group chat navigation data
     * @return NotificationNavigationState for group chat or null if invalid
     */
    private fun processGroupChatIntent(intent: Intent): NotificationNavigationState? {
        val groupId = intent.getStringExtra("groupId")
        val groupName = intent.getStringExtra("groupName")
        val senderId = intent.getStringExtra("senderId")
        val senderName = intent.getStringExtra("senderName")

        return if (groupId != null && groupName != null && senderId != null && senderName != null) {
            NotificationNavigationState.forGroupChat(
                groupId = groupId,
                groupName = groupName,
                senderId = senderId,
                senderName = senderName
            )
        } else {
            // Missing required fields for group chat
            null
        }
    }
}
