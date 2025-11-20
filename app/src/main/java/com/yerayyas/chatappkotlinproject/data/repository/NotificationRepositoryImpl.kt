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
 * Concrete implementation of [NotificationRepository] that orchestrates notification logic.
 *
 * Unlike simple data repositories, this class acts as a **Coordinator** between:
 * 1. [DeviceInfoProvider] to understand device capabilities.
 * 2. [NotificationChannelManager] to ensure infrastructure (channels) exists.
 * 3. [NotificationDataSource] to actually display or manage the notifications.
 *
 * **Key Responsibilities:**
 * - **Lifecycle Management:** Ensures notification channels exist before display.
 * - **Thread Safety:** Offloads all operations to [Dispatchers.IO] to prevent UI blocking.
 * - **Error Handling:** Wraps exceptions in [Result] types for safe consumption by the Domain layer.
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

    /**
     * Orchestrates the process of displaying a notification.
     *
     * **Process Flow:**
     * 1. Retrieves device compatibility info via [DeviceInfoProvider].
     * 2. Ensures the required Notification Channel exists via [NotificationChannelManager].
     * 3. Delegates the actual display to [NotificationDataSource].
     *
     * Executed on [Dispatchers.IO] for thread safety.
     *
     * @param notificationData Data content for the notification.
     * @return [Result.success] if displayed, [Result.failure] otherwise.
     */
    override suspend fun showNotification(notificationData: NotificationData): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Showing notification for: ${notificationData.senderName}")

                // Step 1 & 2: Prepare infrastructure (Channel) based on Device Info
                val deviceCompatibility = deviceInfoProvider.getDeviceCompatibility()
                notificationChannelManager.ensureChannelExists(deviceCompatibility)

                // Step 3: Show the notification
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

    /**
     * Cancels notifications for a specific user safely on an IO thread.
     *
     * @param userId The unique identifier of the user.
     */
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

    /**
     * Cancels all application notifications safely on an IO thread.
     */
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

    /**
     * Retrieves the active notification count safely.
     *
     * @return The count of notifications, or 0 if an error occurs (Safe Fallback).
     */
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