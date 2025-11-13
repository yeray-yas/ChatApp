package com.yerayyas.chatappkotlinproject.data.datasource

import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.model.NotificationPermissionState

/**
 * Data source contract for notification operations.
 *
 * This interface defines the operations that can be performed on the notification system
 * at the data layer level. It abstracts the underlying notification platform specifics
 * and provides a clean contract for notification management across different implementations.
 *
 * The interface follows Clean Architecture principles by defining domain-agnostic operations
 * that can be implemented by platform-specific data sources (Android, iOS, etc.).
 *
 * Key responsibilities:
 * - Permission state checking and validation
 * - Notification display with device optimization
 * - Notification cancellation and management
 * - Active notification tracking and counting
 * - Error handling through Result types
 *
 * Implementation considerations:
 * - Should handle platform-specific permission requirements
 * - Must provide proper error handling for all operations
 * - Should support both individual and batch notification operations
 * - Must be thread-safe for concurrent access
 * - Should optimize for device-specific capabilities
 */
interface NotificationDataSource {

    /**
     * Checks the current notification permission state.
     *
     * This method determines whether the application has the necessary permissions
     * to display notifications to the user. The implementation should handle
     * platform-specific permission requirements (e.g., Android 13+ explicit permissions).
     *
     * @return The current [NotificationPermissionState] indicating permission status
     */
    fun checkNotificationPermission(): NotificationPermissionState

    /**
     * Displays a notification to the user.
     *
     * This method handles the display of a single notification with proper device
     * optimization and compatibility handling. The implementation should:
     * - Validate permission state before displaying
     * - Apply device-specific optimizations
     * - Handle notification grouping and management
     * - Provide proper error handling for display failures
     *
     * @param notificationData The notification data to display including content and metadata
     * @param deviceCompatibility Device information for optimization and compatibility
     * @return [Result] indicating success or failure with error details
     */
    fun displayNotification(
        notificationData: NotificationData,
        deviceCompatibility: DeviceCompatibility
    ): Result<Unit>

    /**
     * Cancels all notifications for a specific user.
     *
     * This method removes all active notifications associated with a particular user.
     * Useful for clearing notifications when entering a chat with that user or
     * when user-specific events occur (blocking, etc.).
     *
     * @param userId The unique identifier of the user whose notifications should be cancelled
     * @return [Result] indicating success or failure with error details
     */
    fun cancelUserNotifications(userId: String): Result<Unit>

    /**
     * Cancels all active notifications.
     *
     * This method removes all notifications displayed by the application.
     * Commonly used when the app becomes active or when performing bulk cleanup operations.
     *
     * @return [Result] indicating success or failure with error details
     */
    fun cancelAllNotifications(): Result<Unit>

    /**
     * Gets the count of currently active notifications.
     *
     * This method returns the number of notifications currently displayed by the application.
     * Useful for UI indicators, notification management, and analytics tracking.
     *
     * @return The number of active notifications, or 0 if unable to determine count
     */
    fun getActiveNotificationsCount(): Int
}
