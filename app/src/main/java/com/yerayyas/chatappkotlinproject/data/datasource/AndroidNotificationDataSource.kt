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
 * Android-specific implementation of [NotificationDataSource].
 *
 * This class handles the actual display and management of notifications
 * using Android's notification system, with proper error handling and
 * state tracking.
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

    /**
     * Tracks active notifications to manage grouping and summary notifications.
     */
    private val activeNotifications = mutableSetOf<String>()

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

            // Build the notification
            val notification = notificationBuilder.buildNotification(
                notificationData,
                deviceCompatibility,
                pendingIntentFactory
            ) as android.app.Notification

            // Display the notification
            notificationManagerCompat.notify(notificationData.notificationId, notification)

            // Track this notification using the appropriate ID
            // For individual chats: senderId, for group chats: chatId (groupId)
            val trackingId = if (notificationData.isGroupMessage) {
                notificationData.chatId // This is the groupId for group messages
            } else {
                notificationData.senderId // This is the senderId for individual messages
            }
            activeNotifications.add(trackingId)

            // Update or create summary notification
            updateSummaryNotification()

            Log.d(TAG, "Notification displayed successfully for: $displayName")
            Log.d(TAG, "Active notifications count: ${activeNotifications.size}")

            Result.success(Unit)

        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "Security exception when displaying notification - permission may have been revoked",
                e
            )
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error displaying notification", e)
            Result.failure(e)
        }
    }

    override fun cancelUserNotifications(userId: String): Result<Unit> {
        return cancelChatNotifications(userId)
    }

    /**
     * Cancels notifications for a specific chat (individual or group)
     * @param chatId The ID to cancel notifications for (userId for individual, groupId for group)
     */
    fun cancelChatNotifications(chatId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling notifications for chat: $chatId")

            if (activeNotifications.contains(chatId)) {
                val notificationId = chatId.hashCode()
                notificationManagerCompat.cancel(notificationId)
                activeNotifications.remove(chatId)

                Log.d(TAG, "Canceled notification for chat: $chatId")
                Log.d(TAG, "Remaining active notifications: ${activeNotifications.size}")

                // Update or remove summary notification
                updateSummaryNotification()

            } else {
                Log.d(TAG, "No active notification found for chat: $chatId")
            }

            Result.success(Unit)

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when canceling notification", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notifications for chat: $chatId", e)
            Result.failure(e)
        }
    }

    override fun cancelAllNotifications(): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling all notifications")

            notificationManagerCompat.cancelAll()
            activeNotifications.clear()

            Log.d(TAG, "All notifications canceled and tracking cleared")

            Result.success(Unit)

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when canceling all notifications", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling all notifications", e)
            Result.failure(e)
        }
    }

    override fun getActiveNotificationsCount(): Int {
        return try {
            // Try to get count from system first
            val systemCount = systemNotificationManager.activeNotifications
                .count { it.notification.group == GROUP_KEY && it.id != SUMMARY_ID }

            if (systemCount > 0) {
                systemCount
            } else {
                // Fallback to our internal tracker
                activeNotifications.size
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving active notifications count", e)
            activeNotifications.size // Fallback to our internal tracker
        }
    }

    private fun updateSummaryNotification() {
        try {
            if (activeNotifications.isNotEmpty()) {
                val summaryNotification = notificationBuilder.buildSummaryNotification(
                    activeNotifications.size,
                    context.getString(com.yerayyas.chatappkotlinproject.R.string.app_name)
                ) as android.app.Notification

                notificationManagerCompat.notify(SUMMARY_ID, summaryNotification)
            } else {
                notificationManagerCompat.cancel(SUMMARY_ID)
                Log.d(TAG, "Removed summary notification - no active notifications remain")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when updating summary notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating summary notification", e)
        }
    }
}
