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
 * This class provides the concrete implementation for notification operations on the Android platform.
 * It handles the actual display, management, and cancellation of notifications using Android's
 * notification system with proper error handling, permission checking, and state tracking.
 *
 * Key features:
 * - Android notification system integration
 * - Permission state checking for Android 13+ compatibility
 * - Notification grouping and summary management
 * - Active notification tracking for state consistency
 * - Robust error handling with Result types
 * - Thread-safe operations for concurrent access
 * - Integration with domain services for business logic
 *
 * The class maintains internal state to track active notifications for proper grouping
 * and summary notification management, ensuring a consistent user experience across
 * different notification scenarios.
 *
 * Dependencies:
 * - [NotificationBuilder]: Creates platform-specific notifications
 * - [PendingIntentFactory]: Generates proper intents for notification actions
 * - Android Context: Required for system service access
 *
 * Architecture pattern: Data Layer - Platform-specific DataSource
 * - Implements abstract notification operations for Android
 * - Provides error handling and state management
 * - Integrates with domain layer services
 * - Maintains platform-specific optimizations
 */
@Singleton
class AndroidNotificationDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationBuilder: NotificationBuilder,
    private val pendingIntentFactory: PendingIntentFactory
) : NotificationDataSource {

    /**
     * NotificationManagerCompat instance for cross-version compatibility.
     * Provides consistent notification operations across different Android versions.
     */
    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    /**
     * System NotificationManager for advanced operations and active notification queries.
     * Used for operations that require direct system integration.
     */
    private val systemNotificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Thread-safe set tracking active notifications for grouping and summary management.
     * Maps notification tracking IDs to maintain consistent state across operations.
     */
    private val activeNotifications = mutableSetOf<String>()

    /**
     * Checks notification permission state with Android version compatibility.
     *
     * This method handles the evolution of notification permissions across Android versions:
     * - Android 13+ (API 33+): Requires explicit POST_NOTIFICATIONS permission
     * - Earlier versions: Notifications allowed by default
     *
     * @return [NotificationPermissionState] indicating current permission status
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
     * Displays a notification with comprehensive error handling and state management.
     *
     * This method performs the following operations:
     * 1. Builds the notification using domain services
     * 2. Displays the notification through the system
     * 3. Tracks the notification for grouping purposes
     * 4. Updates or creates summary notifications
     * 5. Provides detailed logging for debugging
     *
     * The method uses different tracking strategies for individual vs group chats
     * to ensure proper notification management and user experience.
     *
     * @param notificationData The notification content and metadata to display
     * @param deviceCompatibility Device information for optimization
     * @return [Result] indicating success or failure with detailed error information
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

            // Build the notification using domain service
            val notification = notificationBuilder.buildNotification(
                notificationData,
                deviceCompatibility,
                pendingIntentFactory
            ) as android.app.Notification

            // Display the notification
            notificationManagerCompat.notify(notificationData.notificationId, notification)

            // Track notification using appropriate strategy
            val trackingId = getTrackingId(notificationData)
            synchronized(activeNotifications) {
                activeNotifications.add(trackingId)
            }

            // Update summary notification for grouped display
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

    /**
     * Determines the appropriate tracking ID for notification state management.
     *
     * Uses different strategies based on chat type:
     * - Individual chats: Use senderId for user-based grouping
     * - Group chats: Use chatId (groupId) for group-based grouping
     *
     * @param notificationData The notification data containing chat information
     * @return String identifier for tracking this notification type
     */
    private fun getTrackingId(notificationData: NotificationData): String {
        return if (notificationData.isGroupMessage) {
            notificationData.chatId // This is the groupId for group messages
        } else {
            notificationData.senderId // This is the senderId for individual messages
        }
    }

    override fun cancelUserNotifications(userId: String): Result<Unit> {
        return cancelChatNotifications(userId)
    }

    /**
     * Cancels notifications for a specific chat (individual or group).
     *
     * This method handles both individual and group chat notification cancellation
     * with proper state management and summary notification updates.
     *
     * @param chatId The ID to cancel notifications for (userId for individual, groupId for group)
     * @return [Result] indicating success or failure
     */
    fun cancelChatNotifications(chatId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling notifications for chat: $chatId")

            synchronized(activeNotifications) {
                if (activeNotifications.contains(chatId)) {
                    val notificationId = chatId.hashCode()
                    notificationManagerCompat.cancel(notificationId)
                    activeNotifications.remove(chatId)

                    Log.d(TAG, "Canceled notification for chat: $chatId")
                    Log.d(TAG, "Remaining active notifications: ${activeNotifications.size}")

                    // Update summary notification after cancellation
                    updateSummaryNotification()
                } else {
                    Log.d(TAG, "No active notification found for chat: $chatId")
                }
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
            synchronized(activeNotifications) {
                activeNotifications.clear()
            }

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
            // Try to get count from system first for accuracy
            val systemCount = systemNotificationManager.activeNotifications
                .count { it.notification.group == GROUP_KEY && it.id != SUMMARY_ID }

            if (systemCount > 0) {
                systemCount
            } else {
                // Fallback to internal tracker
                synchronized(activeNotifications) {
                    activeNotifications.size
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving active notifications count", e)
            // Return internal tracker count as last resort
            synchronized(activeNotifications) {
                activeNotifications.size
            }
        }
    }

    /**
     * Updates or removes the summary notification based on active notification count.
     *
     * Android notification grouping requires a summary notification to properly display
     * multiple notifications from the same app. This method:
     * - Creates/updates summary when notifications are active
     * - Removes summary when no notifications remain
     * - Handles permission and security errors gracefully
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
