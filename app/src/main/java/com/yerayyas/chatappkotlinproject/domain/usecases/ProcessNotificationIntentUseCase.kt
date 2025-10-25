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
 */
@Singleton
class ProcessNotificationIntentUseCase @Inject constructor() {

    /**
     * Parses the provided [Intent] to find and validate chat-related navigation extras.
     *
     * To be considered valid, the intent must contain a "navigateTo" extra with the value "chat",
     * as well as non-null "userId" and "username" extras.
     *
     * @param intent The incoming intent to process, which can be null.
     * @return A [NotificationNavigationState] instance if the intent contains valid navigation data,
     *         otherwise `null`.
     */
    operator fun invoke(intent: Intent?): NotificationNavigationState? {
        intent ?: return null

        val destination = intent.getStringExtra("navigateTo")
        val userId = intent.getStringExtra("userId")
        val username = intent.getStringExtra("username")

        if (destination == "chat" && userId != null && username != null) {
            return NotificationNavigationState(
                navigateTo = destination,
                userId = userId,
                username = username,
                eventId = System.currentTimeMillis()
            )
        }

        return null
    }
}
