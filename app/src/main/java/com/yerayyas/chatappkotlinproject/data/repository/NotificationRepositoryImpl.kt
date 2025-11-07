package com.yerayyas.chatappkotlinproject.data.repository

import android.util.Log
import com.yerayyas.chatappkotlinproject.data.datasource.NotificationDataSource
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.model.NotificationPermissionState
import com.yerayyas.chatappkotlinproject.domain.repository.NotificationRepository
import com.yerayyas.chatappkotlinproject.domain.service.DeviceInfoProvider
import com.yerayyas.chatappkotlinproject.domain.service.NotificationChannelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationRepositoryImpl"

/**
 * Implementation of [NotificationRepository] that coordinates between
 * different data sources and services to provide notification functionality.
 *
 * This class follows Clean Architecture principles by depending on
 * abstractions rather than concrete implementations.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDataSource: NotificationDataSource,
    private val notificationChannelManager: NotificationChannelManager,
    private val deviceInfoProvider: DeviceInfoProvider
) : NotificationRepository {

    override fun getNotificationPermissionState(): NotificationPermissionState {
        return notificationDataSource.checkNotificationPermission()
    }

    override suspend fun showNotification(notificationData: NotificationData): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Showing notification for: ${notificationData.senderName}")

                // Ensure notification channel exists
                val deviceCompatibility = deviceInfoProvider.getDeviceCompatibility()
                notificationChannelManager.ensureChannelExists(deviceCompatibility)

                // Show the notification
                val result = notificationDataSource.displayNotification(
                    notificationData,
                    deviceCompatibility
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Notification displayed successfully")
                } else {
                    Log.e(TAG, "Failed to display notification", result.exceptionOrNull())
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun cancelNotificationsForUser(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Cancelling notifications for user: $userId")

                val result = notificationDataSource.cancelUserNotifications(userId)

                if (result.isSuccess) {
                    Log.d(TAG, "Notifications cancelled successfully for user: $userId")
                } else {
                    Log.e(
                        TAG,
                        "Failed to cancel notifications for user: $userId",
                        result.exceptionOrNull()
                    )
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling notifications for user: $userId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun cancelAllNotifications(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Cancelling all notifications")

                val result = notificationDataSource.cancelAllNotifications()

                if (result.isSuccess) {
                    Log.d(TAG, "All notifications cancelled successfully")
                } else {
                    Log.e(TAG, "Failed to cancel all notifications", result.exceptionOrNull())
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling all notifications", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getActiveNotificationsCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                notificationDataSource.getActiveNotificationsCount()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting active notifications count", e)
                0 // Return 0 as fallback
            }
        }
    }
}
