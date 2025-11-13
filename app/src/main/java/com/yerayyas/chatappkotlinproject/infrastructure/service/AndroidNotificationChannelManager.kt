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
 * Android-specific implementation of [NotificationChannelManager] domain service.
 *
 * This infrastructure service manages Android notification channels for API 26+ devices,
 * implementing device-specific optimizations to ensure reliable notification delivery
 * across different manufacturers. The service addresses the unique challenges posed by
 * aggressive power management policies on various Android devices.
 *
 * Key responsibilities:
 * - **Channel Creation**: Configure notification channels for Android 8.0+ requirements
 * - **Device Optimization**: Apply manufacturer-specific channel settings for better delivery
 * - **Platform Isolation**: Keep Android NotificationManager dependencies out of domain layer
 * - **Compatibility Handling**: Manage API level differences gracefully
 * - **Channel Validation**: Ensure channels exist and are properly configured
 *
 * The service follows Clean Architecture principles by:
 * - Implementing domain service interface without domain dependencies
 * - Providing platform-specific implementations for abstract contracts
 * - Enabling comprehensive testing through dependency injection
 * - Supporting device-specific optimization strategies
 *
 * Channel optimization strategies:
 * - **Xiaomi/MIUI**: Maximum importance with aggressive bypass settings
 * - **Huawei/EMUI**: Maximum importance with power management considerations
 * - **OnePlus/OxygenOS**: High importance with balanced system integration
 * - **Google Pixel**: Standard high importance for clean Android experience
 * - **Generic Devices**: Conservative high importance for broad compatibility
 *
 * The manager automatically detects existing channels and applies appropriate settings
 * based on device type, while providing warnings for channels with suboptimal configurations.
 */
@Singleton
class AndroidNotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationChannelManager {

    /**
     * Lazy-initialized Android NotificationManager for channel operations.
     * Provides access to system notification services while maintaining
     * efficient resource usage through lazy initialization.
     */
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Ensures the notification channel exists with device-specific optimizations.
     *
     * This method handles the complexity of Android notification channels by:
     * - Checking API level compatibility (channels required for Android 8.0+)
     * - Creating channels with device-specific importance levels
     * - Applying manufacturer-specific settings for better delivery reliability
     * - Logging channel creation and configuration for debugging
     *
     * For Android versions below API 26, this method gracefully handles the
     * absence of channel requirements while maintaining consistent behavior.
     *
     * @param deviceCompatibility Device information for optimization selection
     */
    override fun ensureChannelExists(deviceCompatibility: DeviceCompatibility) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannelForDevice(deviceCompatibility)
        } else {
            Log.d(TAG, "Android version ${Build.VERSION.SDK_INT} - No channel needed")
        }
    }

    /**
     * Checks whether the notification channel exists in the system.
     *
     * This method provides a clean way to verify channel existence across
     * different Android versions:
     * - For Android 8.0+: Queries the system for the specific channel
     * - For older versions: Returns true since channels are not required
     *
     * @return true if the channel exists or is not required, false otherwise
     */
    override fun channelExists(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(CHANNEL_ID) != null
        } else {
            true // Channels not required on older versions
        }
    }

    /**
     * Creates or validates notification channel with device-specific optimizations.
     *
     * This method implements the core channel management logic for Android 8.0+ devices:
     *
     * **Channel Creation Process:**
     * 1. Check if channel already exists
     * 2. Determine appropriate importance level based on device type
     * 3. Create channel with device-specific settings
     * 4. Apply manufacturer-specific optimizations
     * 5. Register channel with the system
     *
     * **Device-Specific Importance Levels:**
     * - **Xiaomi/MIUI**: IMPORTANCE_MAX to bypass aggressive notification management
     * - **Huawei/EMUI**: IMPORTANCE_MAX to combat power management restrictions
     * - **Other Devices**: IMPORTANCE_HIGH for standard reliable delivery
     *
     * **Channel Features Configured:**
     * - **Basic Settings**: Name, description, and importance level
     * - **Visual Indicators**: LED lights and badge display
     * - **Audio/Vibration**: Sound and vibration patterns
     * - **System Integration**: DND bypass and lock screen visibility
     * - **Device Optimizations**: Manufacturer-specific enhancements
     *
     * **Existing Channel Validation:**
     * For existing channels, the method validates configuration and provides
     * warnings if the importance level is insufficient for reliable delivery
     * on problematic devices.
     *
     * @param deviceCompatibility Device information for optimization selection
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelForDevice(deviceCompatibility: DeviceCompatibility) {
        var channel = notificationManager.getNotificationChannel(CHANNEL_ID)

        if (channel == null) {
            Log.d(TAG, "Creating new notification channel: $CHANNEL_ID")

            // Determine importance level based on device type for optimal delivery
            val importance = when (deviceCompatibility.deviceType) {
                DeviceType.XIAOMI, DeviceType.HUAWEI -> {
                    Log.d(TAG, "Using IMPORTANCE_MAX for ${deviceCompatibility.deviceType} device")
                    NotificationManager.IMPORTANCE_MAX
                }
                else -> NotificationManager.IMPORTANCE_HIGH
            }

            // Create channel with device-optimized configuration
            channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Channel for incoming chat messages"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)

                // Apply device-specific optimizations for better delivery
                when (deviceCompatibility.deviceType) {
                    DeviceType.XIAOMI, DeviceType.HUAWEI -> {
                        Log.d(
                            TAG,
                            "Applying aggressive settings for ${deviceCompatibility.deviceType}"
                        )
                        // Enable DND bypass for critical chat notifications
                        setBypassDnd(true)
                        // Set public visibility for lock screen display
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        // Configure custom sound for attention
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            null
                        )
                        // Enhanced vibration pattern for power-saving mode attention
                        vibrationPattern = longArrayOf(0, 300, 300, 300)
                    }
                    else -> {
                        // Standard settings for other devices
                        setBypassDnd(false)
                    }
                }
            }

            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID with importance: $importance")

        } else {
            // Validate existing channel configuration
            Log.d(TAG, "Notification channel already exists: $CHANNEL_ID")
            Log.d(TAG, "Channel importance: ${channel.importance}")
            Log.d(TAG, "Channel can show badge: ${channel.canShowBadge()}")

            // Warn about suboptimal configurations on problematic devices
            if ((deviceCompatibility.deviceType == DeviceType.XIAOMI ||
                        deviceCompatibility.deviceType == DeviceType.HUAWEI) &&
                channel.importance < NotificationManager.IMPORTANCE_MAX
            ) {
                Log.w(
                    TAG,
                    "${deviceCompatibility.deviceType} channel has low importance (${channel.importance}), " +
                            "consider updating manually in device settings for better notification delivery"
                )
            }
        }
    }
}
