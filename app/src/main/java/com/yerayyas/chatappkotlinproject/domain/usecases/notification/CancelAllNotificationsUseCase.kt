package com.yerayyas.chatappkotlinproject.domain.usecases.notification

import android.util.Log
import com.yerayyas.chatappkotlinproject.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CancelAllNotifications"

/**
 * Use case for cancelling all active notifications.
 *
 * This use case encapsulates the business logic for clearing all notifications,
 * typically when the user reads all messages or the app needs to reset notification state.
 */
@Singleton
class CancelAllNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Executes the use case to cancel all active notifications.
     *
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(): Result<Unit> {
        return try {
            Log.d(TAG, "Attempting to cancel all notifications")

            val result = notificationRepository.cancelAllNotifications()

            if (result.isSuccess) {
                Log.d(TAG, "Successfully cancelled all notifications")
            } else {
                Log.e(TAG, "Failed to cancel all notifications", result.exceptionOrNull())
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error cancelling all notifications", e)
            Result.failure(e)
        }
    }
}
