package com.yerayyas.chatappkotlinproject.domain.usecases

import android.content.Intent
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case responsible for extracting navigation parameters
 * from notification Intents.
 *
 * Returns a [NotificationNavigationState] if the Intent represents
 * a chat notification tap, or null otherwise.
 */
@Singleton
class ProcessNotificationIntentUseCase @Inject constructor() {

    /**
     * Parses the provided Intent for deep-link navigation extras.
     *
     * @param intent Incoming Intent potentially containing navigation data.
     * @return [NotificationNavigationState] when valid, null if no actionable data.
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
