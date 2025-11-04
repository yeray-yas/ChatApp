package com.yerayyas.chatappkotlinproject.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
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
 * - Special handling for Xiaomi and other OEM devices with aggressive battery optimization.
 *
 * @property context The application context, injected by Hilt.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activeNotifications = Collections.synchronizedSet(HashSet<String>())

    init {
        // Verify Google Play Services and notification setup on initialization
        verifyGooglePlayServices()
        verifyNotificationSetup()
        logDeviceSpecificInfo()
    }

    /**
     * Logs device-specific information to help debug notification issues on different OEMs.
     */
    private fun logDeviceSpecificInfo() {
        Log.d(TAG, "=== DEVICE INFORMATION ===")
        Log.d(TAG, "Manufacturer: ${Build.MANUFACTURER}")
        Log.d(TAG, "Brand: ${Build.BRAND}")
        Log.d(TAG, "Model: ${Build.MODEL}")
        Log.d(TAG, "Device: ${Build.DEVICE}")
        Log.d(TAG, "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")

        // Check if it's a Xiaomi device with MIUI
        val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Redmi", ignoreCase = true)

        if (isXiaomi) {
            Log.w(TAG, "XIAOMI DEVICE DETECTED - Special notification handling required")
            Log.w(
                TAG,
                "User may need to manually enable autostart and notifications in MIUI settings"
            )
        }

        Log.d(TAG, "=== END DEVICE INFORMATION ===")
    }

    /**
     * Verifies that Google Play Services is available and up to date.
     */
    private fun verifyGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)

        Log.d(TAG, "=== GOOGLE PLAY SERVICES CHECK ===")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "Android version: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Result code: $resultCode")

        when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services is available and up to date")
            }

            ConnectionResult.SERVICE_MISSING -> {
                Log.e(TAG, "Google Play Services is missing")
            }

            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Log.e(TAG, "Google Play Services needs to be updated")
            }

            ConnectionResult.SERVICE_DISABLED -> {
                Log.e(TAG, "Google Play Services is disabled")
            }

            else -> {
                Log.e(TAG, "Google Play Services error: $resultCode")
            }
        }
    }

    /**
     * Verifies notification setup including permissions and channels.
     */
    private fun verifyNotificationSetup() {
        Log.d(TAG, "=== NOTIFICATION SETUP VERIFICATION ===")
        Log.d(TAG, "Has notification permission: ${hasNotificationPermission()}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Log.d(TAG, "NotificationManager available: ${manager != null}")

            val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
            Log.d(TAG, "Existing notification channel: ${existingChannel != null}")
            if (existingChannel != null) {
                Log.d(TAG, "Channel importance: ${existingChannel.importance}")
                Log.d(TAG, "Channel can bypass DND: ${existingChannel.canBypassDnd()}")
            }
        }

        // Check if notifications are enabled at system level
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        Log.d(TAG, "Notifications enabled: ${notificationManagerCompat.areNotificationsEnabled()}")

        Log.d(TAG, "=== NOTIFICATION SETUP COMPLETE ===")
    }

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
        Log.d(TAG, "=== NOTIFICATION HELPER - SEND CHAT NOTIFICATION ===")
        Log.d(
            TAG,
            "SenderId: $senderId, SenderName: $senderName, MessageBody: $messageBody, ChatId: $chatId"
        )

        if (!hasNotificationPermission()) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission; skipping notification.")
            return
        }
        Log.d(TAG, "Notification permission granted")

        createChannelIfNeeded()
        Log.d(TAG, "Notification channel created/verified")

        val pendingIntent = buildChatPendingIntent(senderId, senderName, chatId)
        Log.d(TAG, "PendingIntent created successfully")

        val notification = createNotificationWithDeviceCompatibility(
            senderName, messageBody, pendingIntent
        )

        Log.d(TAG, "Notification built successfully")

        try {
            activeNotifications.add(senderId)
            val manager = NotificationManagerCompat.from(context)

            val notificationId = senderId.hashCode()
            Log.d(TAG, "Notification ID: $notificationId")

            // Verify notification manager
            Log.d(
                TAG,
                "NotificationManager areNotificationsEnabled: ${manager.areNotificationsEnabled()}"
            )

            // The tag and ID are derived from senderId to ensure uniqueness per user
            manager.notify(notificationId, notification)
            Log.d(TAG, "Notification sent successfully with ID: $notificationId")

            sendSummaryNotification(manager)
            Log.d(TAG, "Summary notification sent")

            Log.d(TAG, "Active notifications count: ${activeNotifications.size}")
            Log.d(TAG, "Notification sent for user: $senderId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when sending notification.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification.", e)
        }

        Log.d(TAG, "=== NOTIFICATION HELPER - COMPLETE ===")
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
     * For Xiaomi devices, applies more aggressive settings to bypass MIUI optimizations.
     */
    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            Log.d(TAG, "Creating notification channel for API ${Build.VERSION.SDK_INT}")

            val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                    Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
                    Build.BRAND.equals("Redmi", ignoreCase = true)

            var channel = manager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                Log.d(TAG, "Creating new notification channel: $CHANNEL_ID")

                // For Xiaomi devices, use IMPORTANCE_MAX to bypass MIUI restrictions
                val importance = if (isXiaomi) {
                    Log.d(TAG, "Using IMPORTANCE_MAX for Xiaomi device")
                    NotificationManager.IMPORTANCE_MAX
                } else {
                    NotificationManager.IMPORTANCE_HIGH
                }

                channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    importance
                ).apply {
                    description = "Channel for incoming chat messages"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)

                    if (isXiaomi) {
                        // Xiaomi-specific settings
                        Log.d(TAG, "Applying Xiaomi-specific channel settings")
                        setBypassDnd(true) // Try to bypass Do Not Disturb
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            null
                        )
                        vibrationPattern = longArrayOf(0, 300, 300, 300)
                    } else {
                        setBypassDnd(false)
                    }
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $CHANNEL_ID with importance: $importance")
            } else {
                Log.d(TAG, "Notification channel already exists: $CHANNEL_ID")
                Log.d(TAG, "Channel importance: ${channel.importance}")
                Log.d(TAG, "Channel can show badge: ${channel.canShowBadge()}")

                // For existing channels on Xiaomi, check if we need to update importance
                if (isXiaomi && channel.importance < NotificationManager.IMPORTANCE_MAX) {
                    Log.w(
                        TAG,
                        "Xiaomi channel has low importance (${channel.importance}), consider updating manually"
                    )
                }
            }
        } else {
            Log.d(TAG, "Android version ${Build.VERSION.SDK_INT} - No channel needed")
        }
    }

    /**
     * Creates a notification with device-specific compatibility adjustments.
     * Some devices (Xiaomi, OnePlus, etc.) require specific settings to show notifications properly.
     */
    private fun createNotificationWithDeviceCompatibility(
        senderName: String,
        messageBody: String,
        pendingIntent: PendingIntent
    ): android.app.Notification {
        val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Redmi", ignoreCase = true)
        val isOnePlus = Build.MANUFACTURER.equals("OnePlus", ignoreCase = true)
        val isPixel = Build.MODEL.contains("Pixel", ignoreCase = true)
        val isHuawei = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
                Build.BRAND.equals("HONOR", ignoreCase = true)

        Log.d(TAG, "Device compatibility check:")
        Log.d(TAG, "  Manufacturer: ${Build.MANUFACTURER}")
        Log.d(TAG, "  Brand: ${Build.BRAND}")
        Log.d(TAG, "  Model: ${Build.MODEL}")
        Log.d(
            TAG,
            "  isXiaomi: $isXiaomi, isOnePlus: $isOnePlus, isPixel: $isPixel, isHuawei: $isHuawei"
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(senderName)
            .setContentText(messageBody)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        // Device-specific optimizations
        when {
            isXiaomi -> {
                Log.d(TAG, "Applying Xiaomi-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_MAX) // Xiaomi needs MAX priority
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVibrate(longArrayOf(0, 300, 300, 300)) // Explicit vibration
                    .setLights(0xFF0000FF.toInt(), 300, 300) // Explicit lights
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen

                // For Xiaomi, add a big text style to make the notification more prominent
                if (messageBody.length > 30) {
                    builder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(messageBody)
                            .setBigContentTitle(senderName)
                    )
                }

                // Add heads-up notification capability for Xiaomi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.setFullScreenIntent(pendingIntent, false)
                }
            }

            isHuawei -> {
                Log.d(TAG, "Applying Huawei-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_MAX) // Huawei also needs high priority
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }

            isOnePlus -> {
                Log.d(TAG, "Applying OnePlus-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            }

            isPixel -> {
                Log.d(TAG, "Applying Pixel-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE) // Important for Pixel
            }

            else -> {
                Log.d(TAG, "Applying standard notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            }
        }

        val notification = builder.build()

        // Log final notification properties for debugging
        Log.d(TAG, "Final notification properties:")
        Log.d(TAG, "  Priority: ${notification.priority}")
        Log.d(TAG, "  Flags: ${notification.flags}")
        Log.d(TAG, "  Category: ${notification.category}")
        Log.d(TAG, "  Visibility: ${notification.visibility}")

        return notification
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
