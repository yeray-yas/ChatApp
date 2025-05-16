package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CancelChatNotifications"

/**
 * Use case responsible for cancelling all chat notifications associated with a specific user.
 *
 * When a user opens a chat, notifications for that conversation are no longer needed,
 * so this use case instructs the NotificationHelper to dismiss them.
 *
 * @property notificationHelper Helper component for managing notifications.
 */
@Singleton
class CancelChatNotificationsUseCase @Inject constructor(
    private val notificationHelper: NotificationHelper
) {
    /**
     * Cancels all notifications related to the given user ID.
     *
     * @param userId The identifier of the user whose notifications should be cancelled.
     */
    operator fun invoke(userId: String) {
        try {
            Log.d(TAG, "Cancelling notifications for user: $userId")
            notificationHelper.cancelNotificationsForUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notifications for user: $userId", e)
            // Suppress exceptions to avoid disrupting the main flow
        }
    }
}
