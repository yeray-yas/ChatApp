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
 * Helper for sending and managing chat notifications.
 * Tracks active notifications and displays grouped summaries.
 *
 * @param context Application context for notification operations.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activeNotifications = Collections.synchronizedSet(HashSet<String>())

    /**
     * Sends a high-priority chat notification for an incoming message.
     * Creates notification channel if needed and updates summary.
     *
     * @param senderId Unique identifier of the message sender.
     * @param senderName Display name of the sender.
     * @param messageBody Preview text of the message.
     */
    fun sendChatNotification(
        senderId: String,
        senderName: String,
        messageBody: String
    ) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission; skipping notification.")
            return
        }

        createChannelIfNeeded()
        val pendingIntent = buildChatPendingIntent(senderId, senderName)
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
     * Cancels notifications for a specific user and updates summary.
     *
     * @param userId Identifier of the user whose notifications will be canceled.
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
     * Cancels all chat notifications and clears internal tracking.
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

    private fun buildChatPendingIntent(senderId: String, senderName: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateTo", "chat")
            putExtra("userId", senderId)
            putExtra("username", senderName)
        }
        return PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createChannelIfNeeded() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(CHANNEL_ID) == null
        ) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for chat notifications"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendSummaryNotification(manager: NotificationManagerCompat) {
        if (!hasNotificationPermission()) return

        try {
            val count = getActiveChatNotificationsCount()
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("You have $count unread messages")
                .setSmallIcon(R.drawable.ic_chat)
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .setBigContentTitle("$count new messages")
                        .setSummaryText("Chat messages summary")
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

    private fun getActiveChatNotificationsCount(): Int {
        return try {
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.activeNotifications
                .count { it.notification.group == GROUP_KEY && it.id != SUMMARY_ID }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving active notifications count.", e)
            activeNotifications.size
        }
    }

    /**
     * Checks if the app has POST_NOTIFICATIONS permission (Android 13+).
     * Returns true on older platforms where permission is not required.
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
