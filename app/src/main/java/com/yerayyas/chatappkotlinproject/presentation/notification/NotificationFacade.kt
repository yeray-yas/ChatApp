package com.yerayyas.chatappkotlinproject.presentation.notification

import android.util.Log
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelAllNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelUserNotificationsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationFacade"

/**
 * Facade class that provides a simplified interface for notification operations.
 *
 * This class maintains compatibility with the existing codebase while
 * internally using the new Clean Architecture notification system.
 */
@Singleton
class NotificationFacade @Inject constructor(
    private val cancelUserNotificationsUseCase: CancelUserNotificationsUseCase,
    private val cancelAllNotificationsUseCase: CancelAllNotificationsUseCase
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Cancels all notifications for a specific user.
     *
     * @param userId The unique identifier of the user whose notifications should be canceled
     */
    fun cancelNotificationsForUser(userId: String) {
        scope.launch {
            try {
                cancelUserNotificationsUseCase(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling notifications for user: $userId", e)
            }
        }
    }

    /**
     * Cancels all active chat notifications.
     */
    fun cancelAllNotifications() {
        scope.launch {
            try {
                cancelAllNotificationsUseCase()
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling all notifications", e)
            }
        }
    }
}