package com.yerayyas.chatappkotlinproject.domain.usecases

import android.content.Intent
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A use case that processes an incoming [Intent] to extract deep-link navigation data.
 *
 * This class is responsible for parsing the extras from an intent, typically one originating
 * from a user tapping on a notification. It validates the required data and, if successful,
 * transforms it into a structured [NotificationNavigationState] object. This decouples the
 * Activity from the specific knowledge of intent extras.
 *
 * Supports both individual and group chat navigation.
 */
@Singleton
class ProcessNotificationIntentUseCase @Inject constructor() {

    /**
     * Parses the provided [Intent] to find and validate chat-related navigation extras.
     *
     * For individual chats: intent must contain "navigateTo" = "chat", "userId", and "username"
     * For group chats: intent must contain "navigateTo" = "group_chat", "groupId", "groupName", and sender info
     *
     * @param intent The incoming intent to process, which can be null.
     * @return A [NotificationNavigationState] instance if the intent contains valid navigation data,
     *         otherwise `null`.
     */
    operator fun invoke(intent: Intent?): NotificationNavigationState? {
        intent ?: return null

        val destination = intent.getStringExtra("navigateTo")

        return when (destination) {
            "chat" -> {
                // Individual chat navigation
                val userId = intent.getStringExtra("userId")
                val username = intent.getStringExtra("username")

                if (userId != null && username != null) {
                    NotificationNavigationState(
                        navigateTo = destination,
                        userId = userId,
                        username = username,
                        eventId = System.currentTimeMillis()
                    )
                } else null
            }

            "group_chat" -> {
                // Group chat navigation
                val groupId = intent.getStringExtra("groupId")
                val groupName = intent.getStringExtra("groupName")
                val senderId = intent.getStringExtra("senderId")
                val senderName = intent.getStringExtra("senderName")

                if (groupId != null && groupName != null && senderId != null && senderName != null) {
                    NotificationNavigationState(
                        navigateTo = destination,
                        userId = senderId, // Keep for context
                        username = senderName, // Keep for context
                        eventId = System.currentTimeMillis(),
                        groupId = groupId,
                        groupName = groupName
                    )
                } else null
            }

            else -> null
        }
    }
}
