package com.yerayyas.chatappkotlinproject.domain.usecases.notification

import android.util.Log
import com.yerayyas.chatappkotlinproject.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CancelUserNotifications"

/**
 * Use case for cancelling notifications for a specific user.
 *
 * This use case encapsulates the business logic for cancelling user-specific
 * notifications, typically when the user enters a chat.
 */
@Singleton
class CancelUserNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Executes the use case to cancel notifications for a specific user.
     *
     * @param userId The unique identifier of the user whose notifications should be cancelled
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Attempting to cancel notifications for user: $userId")

            val result = notificationRepository.cancelNotificationsForUser(userId)

            if (result.isSuccess) {
                Log.d(TAG, "Successfully cancelled notifications for user: $userId")
            } else {
                Log.e(
                    TAG,
                    "Failed to cancel notifications for user: $userId",
                    result.exceptionOrNull()
                )
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error cancelling notifications for user: $userId", e)
            Result.failure(e)
        }
    }
}
