package com.yerayyas.chatappkotlinproject.infrastructure.service

import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.model.DeviceType
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.service.NotificationBuilder
import com.yerayyas.chatappkotlinproject.domain.service.PendingIntentFactory
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_ID
import com.yerayyas.chatappkotlinproject.utils.Constants.GROUP_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AndroidNotificationBuilder"

/**
 * Android-specific implementation of [NotificationBuilder].
 *
 * This class builds Android notifications with device-specific optimizations
 * for better notification delivery across different manufacturers.
 */
@Singleton
class AndroidNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationBuilder {

    override fun buildNotification(
        notificationData: NotificationData,
        deviceCompatibility: DeviceCompatibility,
        pendingIntentFactory: PendingIntentFactory
    ): Any {
        Log.d(TAG, "Building notification for ${deviceCompatibility.deviceType} device")

        val pendingIntent = pendingIntentFactory.createChatPendingIntent(
            notificationData.senderId,
            notificationData.senderName,
            notificationData.chatId,
            notificationData.isGroupMessage,
            notificationData.groupName
        ) as PendingIntent

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(notificationData.notificationTitle)
            .setContentText(notificationData.notificationContent)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setWhen(notificationData.timestamp)
            .setShowWhen(true)

        // Apply device-specific optimizations
        applyDeviceSpecificSettings(builder, notificationData, deviceCompatibility, pendingIntent)

        val notification = builder.build()

        // Log final notification properties for debugging
        Log.d(TAG, "Final notification properties:")
        Log.d(TAG, "  Flags: ${notification.flags}")
        Log.d(TAG, "  Category: ${notification.category}")
        Log.d(TAG, "  Visibility: ${notification.visibility}")

        return notification
    }

    override fun buildSummaryNotification(notificationCount: Int, appName: String): Any {
        val summaryText = "You have $notificationCount unread messages"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(appName)
            .setContentText(summaryText)
            .setSmallIcon(R.drawable.ic_chat)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle("$notificationCount new messages")
                    .setSummaryText("Chat messages")
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
    }

    private fun applyDeviceSpecificSettings(
        builder: NotificationCompat.Builder,
        notificationData: NotificationData,
        deviceCompatibility: DeviceCompatibility,
        pendingIntent: PendingIntent
    ) {
        when (deviceCompatibility.deviceType) {
            DeviceType.XIAOMI -> {
                Log.d(TAG, "Applying Xiaomi-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVibrate(longArrayOf(0, 300, 300, 300))
                    .setLights(0xFF0000FF.toInt(), 300, 300)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Add big text style for longer messages
                if (notificationData.notificationContent.length > 30) {
                    builder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(notificationData.notificationContent)
                            .setBigContentTitle(notificationData.notificationTitle)
                    )
                }

                // Add heads-up notification capability
                builder.setFullScreenIntent(pendingIntent, false)
            }

            DeviceType.HUAWEI -> {
                Log.d(TAG, "Applying Huawei-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }

            DeviceType.ONEPLUS -> {
                Log.d(TAG, "Applying OnePlus-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            }

            DeviceType.PIXEL -> {
                Log.d(TAG, "Applying Pixel-specific notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            }

            DeviceType.GENERIC -> {
                Log.d(TAG, "Applying standard notification settings")
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            }
        }
    }
}
