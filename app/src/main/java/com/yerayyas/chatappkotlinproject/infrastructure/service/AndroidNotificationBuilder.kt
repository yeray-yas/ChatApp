package com.yerayyas.chatappkotlinproject.infrastructure.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
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
 * Android-specific implementation of [NotificationBuilder] domain service.
 *
 * This infrastructure service creates optimized Android notifications with device-specific
 * enhancements to ensure reliable notification delivery across different manufacturers.
 * The service addresses the challenges of modern Android devices with aggressive power
 * management policies that can interfere with notification display.
 *
 * Key responsibilities:
 * - **Notification Creation**: Build Android NotificationCompat objects with proper configuration
 * - **Device Optimization**: Apply manufacturer-specific settings for better delivery
 * - **Platform Isolation**: Keep Android notification dependencies out of domain layer
 * - **Multi-format Support**: Handle individual and group chat notifications
 * - **Summary Management**: Create grouped notification summaries for better UX
 * - **API Compatibility**: Handle Android version differences and permission requirements
 *
 * The service follows Clean Architecture principles by:
 * - Implementing domain service interface without domain dependencies
 * - Providing platform-specific implementations for abstract contracts
 * - Enabling comprehensive testing through dependency injection
 * - Supporting device-specific optimization strategies
 *
 * Device-specific optimizations:
 * - **Xiaomi/MIUI**: Aggressive settings with max priority and enhanced vibration
 * - **Huawei/EMUI**: High priority with power management bypass attempts
 * - **OnePlus/OxygenOS**: Standard high priority with message categorization
 * - **Google Pixel**: Clean Android experience with standard settings
 * - **Generic Devices**: Conservative settings that work across most devices
 *
 * The builder automatically applies appropriate settings based on device detection
 * to maximize notification delivery reliability while respecting user preferences
 * and Android API requirements.
 */
@Singleton
class AndroidNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationBuilder {

    /**
     * Builds an optimized Android notification with device-specific enhancements.
     *
     * This method creates a complete Android notification with manufacturer-specific
     * optimizations to ensure reliable delivery. The notification configuration varies
     * based on the detected device type, applying appropriate priority levels,
     * sound settings, and visual enhancements.
     *
     * Notification features:
     * - **Content Display**: Title and message text with proper formatting
     * - **Tap Actions**: PendingIntent for direct navigation to chat screens
     * - **Grouping**: Automatic grouping for multiple chat notifications
     * - **Timestamps**: Message timing information for user context
     * - **Auto-cancel**: Automatic dismissal when tapped for clean UX
     *
     * Device optimizations applied:
     * - Priority levels adjusted for manufacturer power management
     * - Sound and vibration patterns optimized for attention
     * - Visual enhancements like big text for longer messages
     * - Heads-up notification capabilities where appropriate (API < 34)
     * - Lock screen visibility settings for security and accessibility
     *
     * @param notificationData Domain model containing notification content and metadata
     * @param deviceCompatibility Device information for optimization selection
     * @param pendingIntentFactory Factory for creating navigation intents
     * @return Fully configured Android Notification object ready for display
     */
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

        // Apply device-specific optimizations for better delivery
        applyDeviceSpecificSettings(builder, notificationData, deviceCompatibility, pendingIntent)

        val notification = builder.build()

        // Log final notification properties for debugging and monitoring
        Log.d(TAG, "Final notification properties:")
        Log.d(TAG, "  Flags: ${notification.flags}")
        Log.d(TAG, "  Category: ${notification.category}")
        Log.d(TAG, "  Visibility: ${notification.visibility}")

        return notification
    }

    /**
     * Creates a summary notification for grouped chat notifications.
     *
     * This method builds an Android notification that serves as a summary for multiple
     * chat notifications, providing users with an overview of their unread messages.
     * The summary notification uses InboxStyle for better information presentation.
     *
     * Summary features:
     * - **Count Display**: Total number of unread messages
     * - **Inbox Style**: Expandable format for better information density
     * - **Group Summary**: Acts as the primary notification in the group
     * - **Auto-cancel**: Dismisses when tapped for clean notification management
     *
     * @param notificationCount Total number of active chat notifications
     * @param appName Application name for the summary title
     * @return Android Notification configured as a group summary
     */
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

    /**
     * Applies manufacturer-specific notification settings for optimal delivery.
     *
     * This method implements device-specific optimization strategies to combat
     * aggressive power management policies that may interfere with notification
     * display. Each manufacturer receives tailored settings based on their
     * documented behavior and user community feedback.
     *
     * Optimization strategies by manufacturer:
     *
     * **Xiaomi/MIUI Devices:**
     * - Maximum priority to bypass MIUI's aggressive notification management
     * - Enhanced vibration patterns for attention in power-saving modes
     * - LED light configurations for visual notification indication
     * - Custom sound settings to bypass system limitations
     * - Big text style for longer messages to improve readability
     * - Full-screen intent capability for critical notifications (API < 34 only)
     * - Public visibility for lock screen display
     *
     * **Huawei/EMUI Devices:**
     * - Maximum priority for power management bypass
     * - Standard notification defaults with enhanced visibility
     * - Message categorization for proper handling
     * - Public visibility for accessibility
     *
     * **OnePlus/OxygenOS Devices:**
     * - High priority balanced with system integration
     * - Standard notification features with message categorization
     * - Optimized for OnePlus notification handling
     *
     * **Google Pixel Devices:**
     * - Standard Android settings optimized for clean experience
     * - High priority with proper message categorization
     * - Leverages pure Android notification system
     *
     * **Generic/Other Devices:**
     * - Conservative settings that work across most manufacturers
     * - Balanced priority to avoid conflicts with unknown systems
     * - Standard notification features for broad compatibility
     *
     * Note: Full-screen intents are only applied on Android 13 and below, as Android 14+
     * requires explicit user permission for USE_FULL_SCREEN_INTENT which is not
     * automatically granted for chat applications.
     *
     * @param builder NotificationCompat.Builder to configure with device-specific settings
     * @param notificationData Notification content for context-aware optimizations
     * @param deviceCompatibility Device information for optimization selection
     * @param pendingIntent Intent for enhanced notification features like heads-up display
     */
    @SuppressLint("FullScreenIntentPolicy")
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

                // Add big text style for longer messages to improve readability
                if (notificationData.notificationContent.length > 30) {
                    builder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(notificationData.notificationContent)
                            .setBigContentTitle(notificationData.notificationTitle)
                    )
                }

                // Add heads-up notification capability for critical messages
                // Only on Android 13 and below due to USE_FULL_SCREEN_INTENT permission changes
                if (Build.VERSION.SDK_INT < 34) {
                    Log.d(TAG, "Adding full-screen intent for Xiaomi device (API < 34)")
                    builder.setFullScreenIntent(pendingIntent, false)
                } else {
                    Log.d(
                        TAG,
                        "Skipping full-screen intent on Android 14+ due to permission requirements"
                    )
                }
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
