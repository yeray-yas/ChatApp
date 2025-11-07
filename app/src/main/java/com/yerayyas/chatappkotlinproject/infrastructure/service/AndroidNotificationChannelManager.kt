package com.yerayyas.chatappkotlinproject.infrastructure.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.model.DeviceType
import com.yerayyas.chatappkotlinproject.domain.service.NotificationChannelManager
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_ID
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AndroidNotificationChannelManager"

/**
 * Android-specific implementation of [NotificationChannelManager].
 *
 * This class manages notification channels for Android 8.0 (API 26) and higher,
 * with device-specific optimizations for better notification delivery.
 */
@Singleton
class AndroidNotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationChannelManager {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun ensureChannelExists(deviceCompatibility: DeviceCompatibility) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannelForDevice(deviceCompatibility)
        } else {
            Log.d(TAG, "Android version ${Build.VERSION.SDK_INT} - No channel needed")
        }
    }

    override fun channelExists(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(CHANNEL_ID) != null
        } else {
            true // Channels not required on older versions
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelForDevice(deviceCompatibility: DeviceCompatibility) {
        var channel = notificationManager.getNotificationChannel(CHANNEL_ID)

        if (channel == null) {
            Log.d(TAG, "Creating new notification channel: $CHANNEL_ID")

            val importance = when (deviceCompatibility.deviceType) {
                DeviceType.XIAOMI, DeviceType.HUAWEI -> {
                    Log.d(TAG, "Using IMPORTANCE_MAX for ${deviceCompatibility.deviceType} device")
                    NotificationManager.IMPORTANCE_MAX
                }

                else -> NotificationManager.IMPORTANCE_HIGH
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

                when (deviceCompatibility.deviceType) {
                    DeviceType.XIAOMI, DeviceType.HUAWEI -> {
                        Log.d(
                            TAG,
                            "Applying aggressive settings for ${deviceCompatibility.deviceType}"
                        )
                        setBypassDnd(true)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            null
                        )
                        vibrationPattern = longArrayOf(0, 300, 300, 300)
                    }

                    else -> {
                        setBypassDnd(false)
                    }
                }
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID with importance: $importance")

        } else {
            Log.d(TAG, "Notification channel already exists: $CHANNEL_ID")
            Log.d(TAG, "Channel importance: ${channel.importance}")
            Log.d(TAG, "Channel can show badge: ${channel.canShowBadge()}")

            // For existing channels on problematic devices, log warning if importance is too low
            if ((deviceCompatibility.deviceType == DeviceType.XIAOMI ||
                        deviceCompatibility.deviceType == DeviceType.HUAWEI) &&
                channel.importance < NotificationManager.IMPORTANCE_MAX
            ) {
                Log.w(
                    TAG,
                    "${deviceCompatibility.deviceType} channel has low importance (${channel.importance}), " +
                            "consider updating manually"
                )
            }
        }
    }
}
