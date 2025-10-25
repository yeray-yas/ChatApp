package com.yerayyas.chatappkotlinproject.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.activity.MainActivity
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_ID
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_NAME
import com.yerayyas.chatappkotlinproject.utils.Constants.GROUP_KEY
import com.yerayyas.chatappkotlinproject.utils.Constants.SUMMARY_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationHelper"

/**
 * A singleton helper class responsible for creating, displaying, and managing chat notifications.
 *
 * This class handles the complexities of:
 * - Checking for notification permissions.
 * - Creating a notification channel for Android 8.0+.
 * - Building and displaying individual chat notifications.
 * - Grouping notifications under a single summary notification.
 * - Canceling individual or all notifications.
 * - Tracking the set of active notifications to manage the summary state correctly.
 *
 * @property context The application context, injected by Hilt.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activeNotifications = Collections.synchronizedSet(HashSet<String>())

    /**
     * Displays a high-priority notification for an incoming chat message and updates the summary.
     *
     * Before sending, it checks for notification permissions. It creates the notification channel if it doesn't exist.
     * After displaying the notification, it calls [sendSummaryNotification] to update the grouped notification.
     *
     * @param senderId A unique identifier for the sender, used as the notification tag and for tracking.
     * @param senderName The name of the sender to be displayed in the notification.
     * @param messageBody The content of the message to be displayed.
     * @param chatId The ID of the chat, used to construct the navigation intent.
     */
    fun sendChatNotification(
        senderId: String,
        senderName: String,
        messageBody: String,
        chatId: String
    ) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission; skipping notification.")
            return
        }

        createChannelIfNeeded()
        val pendingIntent = buildChatPendingIntent(senderId, senderName, chatId)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(senderName)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .build()

        try {
            activeNotifications.add(senderId)
            val manager = NotificationManagerCompat.from(context)
            // The tag and ID are derived from senderId to ensure uniqueness per user
            manager.notify(senderId.hashCode(), notification)
            sendSummaryNotification(manager)
            Log.d(TAG, "Notification sent for user: $senderId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when sending notification.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification.", e)
        }
    }

    /**
     * Cancels a specific user's notification and updates the summary notification.
     *
     * If the specified user has an active notification, it is removed. If other notifications remain,
     * the summary is updated. If no notifications are left, the summary is also removed.
     *
     * @param userId The unique identifier of the user whose notifications should be canceled.
     */
    fun cancelNotificationsForUser(userId: String) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission; skipping cancellation.")
            return
        }

        try {
            if (activeNotifications.contains(userId)) {
                val id = userId.hashCode()
                Log.d(TAG, "Canceling notification for user: $userId (ID: $id)")
                NotificationManagerCompat.from(context).cancel(id)
                activeNotifications.remove(userId)

                if (activeNotifications.isNotEmpty()) {
                    sendSummaryNotification(NotificationManagerCompat.from(context))
                } else {
                    // If no more notifications, cancel the summary as well
                    NotificationManagerCompat.from(context).cancel(SUMMARY_ID)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when canceling notification.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification.", e)
        }
    }

    /**
     * Cancels all chat-related notifications shown by this helper and clears the active notification tracker.
     */
    fun cancelAllNotifications() {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission; skipping all cancellations.")
            return
        }

        try {
            Log.d(TAG, "Canceling all chat notifications.")
            NotificationManagerCompat.from(context).cancelAll()
            activeNotifications.clear()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when canceling all notifications.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling all notifications.", e)
        }
    }

    /**
     * Builds a [PendingIntent] that navigates to the [MainActivity] and instructs it to open the relevant chat screen.
     *
     * @param senderId The ID of the user who sent the message.
     * @param senderName The name of the sender.
     * @param chatId The ID of the chat.
     * @return A configured [PendingIntent].
     */
    private fun buildChatPendingIntent(
        senderId: String,
        senderName: String,
        chatId: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateTo", "chat")
            putExtra("userId", senderId)
            putExtra("username", senderName)
            putExtra("chatId", chatId)
        }

        // Ensure a unique request code for each sender/chat combination to avoid PendingIntent collisions
        val requestCode = (senderId + chatId).hashCode()

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Creates the notification channel for chat messages if it does not already exist.
     * This is required for Android 8.0 (API level 26) and higher.
     */
    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for incoming chat messages"
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $CHANNEL_ID")
            }
        }
    }

    /**
     * Creates and displays a summary notification for all active chat notifications.
     *
     * @param manager The [NotificationManagerCompat] instance used to send the notification.
     */
    private fun sendSummaryNotification(manager: NotificationManagerCompat) {
        if (!hasNotificationPermission()) return

        try {
            val count = getActiveChatNotificationsCount()
            val summaryText = "You have $count unread messages"

            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(summaryText)
                .setSmallIcon(R.drawable.ic_chat)
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .setBigContentTitle("$count new messages")
                        .setSummaryText("Chat messages")
                )
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            manager.notify(SUMMARY_ID, summary)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when sending summary notification.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending summary notification.", e)
        }
    }

    /**
     * Safely retrieves the count of currently visible notifications belonging to the chat group.
     *
     * @return The number of active chat notifications.
     */
    private fun getActiveChatNotificationsCount(): Int {
        return try {
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.activeNotifications
                .count { it.notification.group == GROUP_KEY && it.id != SUMMARY_ID }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving active notifications count.", e)
            activeNotifications.size // Fallback to our internal tracker
        }
    }

    /**
     * Checks if the app has the necessary permission to post notifications.
     *
     * On Android 13 (API 33) and higher, this checks for [Manifest.permission.POST_NOTIFICATIONS].
     * On older versions, this always returns true as the permission is granted at install time.
     *
     * @return `true` if notifications can be posted, `false` otherwise.
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
