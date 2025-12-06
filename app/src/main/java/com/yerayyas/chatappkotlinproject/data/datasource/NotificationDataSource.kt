package com.yerayyas.chatappkotlinproject.data.datasource

import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.model.NotificationPermissionState

/**
 * Data source contract for handling notification operations.
 *
 * This interface abstracts the underlying notification platform specifics, providing
 * a clean contract for the data layer. It adheres to Clean Architecture principles,
 * allowing for platform-specific implementations (Android, wrappers, etc.).
 *
 * **Key responsibilities:**
 * - Permission state checking and validation.
 * - Notification display with device-specific optimization.
 * - Management of active notifications (cancellation, counting).
 *
 * **Implementation considerations:**
 * - Implementations must be thread-safe.
 * - Platform-specific permission handling (e.g., Android 13+) is required.
 * - Errors should be encapsulated within the returned [Result] types.
 */
interface NotificationDataSource {

    /**
     * Checks the current notification permission state.
     *
     * Determines if the application has the necessary privileges to post notifications.
     * Handles platform-specific logic (e.g., `POST_NOTIFICATIONS` permission on Android 13+).
     *
     * @return The current [NotificationPermissionState].
     */
    fun checkNotificationPermission(): NotificationPermissionState

    /**
     * Displays a notification to the user.
     *
     * Handles the construction and dispatch of a notification. Implementations should
     * validate permissions and apply device-specific optimizations before displaying.
     *
     * @param notificationData The content and metadata for the notification.
     * @param deviceCompatibility Configuration for device-specific behavior and optimization.
     * @return A [Result] indicating success or containing an exception on failure.
     */
    fun displayNotification(
        notificationData: NotificationData,
        deviceCompatibility: DeviceCompatibility
    ): Result<Unit>

    /**
     * Cancels all notifications associated with a specific user.
     *
     * Typically used when the user opens a chat with a specific contact or when
     * user-specific events (like blocking) occur to clear relevant clutter.
     *
     * @param userId The unique identifier of the target user.
     * @return A [Result] indicating the outcome of the operation.
     */
    fun cancelUserNotifications(userId: String): Result<Unit>

    /**
     * Cancels all active notifications issued by the application.
     *
     * Useful for global cleanup operations or when the app comes to the foreground.
     *
     * @return A [Result] indicating the outcome of the operation.
     */
    fun cancelAllNotifications(): Result<Unit>

    /**
     * Retrieves the count of currently active notifications.
     *
     * @return The number of notifications currently displayed in the system tray,
     * or 0 if the count cannot be determined.
     */
    fun getActiveNotificationsCount(): Int
}