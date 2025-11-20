package com.yerayyas.chatappkotlinproject.data.datasource

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.model.NotificationPermissionState
import com.yerayyas.chatappkotlinproject.domain.service.NotificationBuilder
import com.yerayyas.chatappkotlinproject.domain.service.PendingIntentFactory
import com.yerayyas.chatappkotlinproject.utils.Constants.GROUP_KEY
import com.yerayyas.chatappkotlinproject.utils.Constants.SUMMARY_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AndroidNotificationDataSource"

/**
 * Concrete implementation of [NotificationDataSource] for Android.
 *
 * Handles the display, cancellation, and grouping of system notifications,
 * maintaining an internal state to manage summary notifications correctly.
 */
@Singleton
class AndroidNotificationDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationBuilder: NotificationBuilder,
    private val pendingIntentFactory: PendingIntentFactory
) : NotificationDataSource {

    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }
    private val systemNotificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val activeNotifications = mutableSetOf<String>()

    /**
     * Checks if the POST_NOTIFICATIONS permission is granted (required for Android 13+).
     * Returns [NotificationPermissionState.NotRequired] for older Android versions.
     */
    override fun checkNotificationPermission(): NotificationPermissionState {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationPermissionState.Granted
                } else {
                    NotificationPermissionState.Denied
                }
            }
            else -> NotificationPermissionState.NotRequired
        }
    }

    /**
     * Displays a notification for a specific chat or group and updates the summary.
     *
     * @param notificationData Data model containing message content and sender info.
     * @param deviceCompatibility Info used to optimize notification styling.
     * @return [Result.success] if displayed, [Result.failure] on permission issues or system errors.
     */
    override fun displayNotification(
        notificationData: NotificationData,
        deviceCompatibility: DeviceCompatibility
    ): Result<Unit> {
        return try {
            val displayName = if (notificationData.isGroupMessage) {
                notificationData.groupName ?: "Group"
            } else {
                notificationData.senderName
            }
            Log.d(TAG, "Displaying notification for: $displayName")

            val notification = notificationBuilder.buildNotification(
                notificationData,
                deviceCompatibility,
                pendingIntentFactory
            ) as android.app.Notification

            notificationManagerCompat.notify(notificationData.notificationId, notification)

            val trackingId = getTrackingId(notificationData)
            synchronized(activeNotifications) {
                activeNotifications.add(trackingId)
            }

            updateSummaryNotification()

            Log.d(TAG, "Notification displayed successfully. Active count: ${activeNotifications.size}")
            Result.success(Unit)

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission revoked when displaying notification.", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error displaying notification", e)
            Result.failure(e)
        }
    }

    /**
     * helper method to determine the tracking ID based on chat type.
     * Uses ChatId for groups and SenderId for individual messages.
     */
    private fun getTrackingId(notificationData: NotificationData): String {
        return if (notificationData.isGroupMessage) {
            notificationData.chatId
        } else {
            notificationData.senderId
        }
    }

    /**
     * Cancels all active notifications associated with a specific user.
     * This is a convenience wrapper around [cancelChatNotifications].
     */
    override fun cancelUserNotifications(userId: String): Result<Unit> {
        return cancelChatNotifications(userId)
    }

    /**
     * Cancels notifications for a specific chat (user or group) and updates the summary.
     *
     * @param chatId The unique identifier of the chat (userId or groupId).
     */
    fun cancelChatNotifications(chatId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling notifications for chat: $chatId")

            synchronized(activeNotifications) {
                val notificationId = chatId.hashCode()

                if (activeNotifications.contains(chatId)) {
                    notificationManagerCompat.cancel(notificationId)
                    activeNotifications.remove(chatId)
                    updateSummaryNotification()
                }
            }
            Result.success(Unit)

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission revoked when canceling notification.", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notifications for chat: $chatId", e)
            Result.failure(e)
        }
    }

    /**
     * Cancels all application notifications and clears the internal tracking state.
     */
    override fun cancelAllNotifications(): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling all notifications")
            notificationManagerCompat.cancelAll()
            synchronized(activeNotifications) {
                activeNotifications.clear()
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission revoked when canceling all notifications.", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling all notifications", e)
            Result.failure(e)
        }
    }

    /**
     * Returns the count of active notifications.
     * Tries to query the system first; falls back to internal tracking if the system query fails.
     */
    override fun getActiveNotificationsCount(): Int {
        return try {
            val systemCount = systemNotificationManager.activeNotifications
                .count { it.notification.group == GROUP_KEY && it.id != SUMMARY_ID }

            if (systemCount > 0) systemCount else synchronized(activeNotifications) { activeNotifications.size }
        } catch (e: Exception) {
            synchronized(activeNotifications) { activeNotifications.size }
        }
    }

    /**
     * Updates or cancels the summary notification based on whether there are remaining active notifications.
     */

    private fun updateSummaryNotification() {
        try {
            synchronized(activeNotifications) {
                if (activeNotifications.isNotEmpty()) {
                    val summaryNotification = notificationBuilder.buildSummaryNotification(
                        activeNotifications.size,
                        context.getString(com.yerayyas.chatappkotlinproject.R.string.app_name)
                    ) as android.app.Notification

                    notificationManagerCompat.notify(SUMMARY_ID, summaryNotification)
                    Log.d(
                        TAG,
                        "Updated summary notification for ${activeNotifications.size} active notifications"
                    )
                } else {
                    notificationManagerCompat.cancel(SUMMARY_ID)
                    Log.d(TAG, "Removed summary notification - no active notifications remain")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when updating summary notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating summary notification", e)
        }
    }
}
