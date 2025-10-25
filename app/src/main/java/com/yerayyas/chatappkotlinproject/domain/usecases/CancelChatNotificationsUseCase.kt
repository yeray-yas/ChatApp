package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CancelChatNotifications"

/**
 * A use case for dismissing all active notifications related to a specific user.
 *
 * This is typically invoked when the user navigates into a chat screen, as notifications for that
 * particular conversation are no longer necessary. It acts as a bridge between the ViewModel
 * and the notification system, promoting a clean separation of concerns.
 *
 * @property notificationHelper The underlying helper class that handles the actual notification removal.
 */
@Singleton
class CancelChatNotificationsUseCase @Inject constructor(
    private val notificationHelper: NotificationHelper
) {
    /**
     * Executes the use case to cancel all notifications for the given user ID.
     *
     * This operation is designed to be safe and will not throw exceptions. If an error occurs
     * during notification cancellation, it will be logged, but the exception will be suppressed
     * to prevent crashing the application.
     *
     * @param userId The unique identifier of the user whose notifications should be cancelled.
     */
    operator fun invoke(userId: String) {
        try {
            Log.d(TAG, "Executing use case to cancel notifications for user: $userId")
            notificationHelper.cancelNotificationsForUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notifications for user: $userId", e)
            // Exceptions are suppressed to ensure this operation does not disrupt the main flow (e.g., screen navigation).
        }
    }
}
