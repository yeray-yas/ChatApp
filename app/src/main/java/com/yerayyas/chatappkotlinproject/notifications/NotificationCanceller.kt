package com.yerayyas.chatappkotlinproject.notifications

import android.util.Log
import com.yerayyas.chatappkotlinproject.presentation.notification.NotificationFacade
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationCanceller"

/**
 * Helper class for cancelling chat notifications.
 *
 * Provides functionality to cancel notifications for specific users or all active notifications.
 */
@Singleton
class NotificationCanceller @Inject constructor(
    private val notificationFacade: NotificationFacade
) {

    /**
     * Cancels all notifications for a specific user.
     *
     * @param userId The unique identifier of the user whose notifications should be canceled
     */
    fun cancelNotificationsForUser(userId: String) {
        Log.d(TAG, "Cancelling notifications for user: $userId")
        notificationFacade.cancelNotificationsForUser(userId)
    }

    /**
     * Cancels all active chat notifications.
     */
    fun cancelAllNotifications() {
        Log.d(TAG, "Cancelling all notifications")
        notificationFacade.cancelAllNotifications()
    }
}
