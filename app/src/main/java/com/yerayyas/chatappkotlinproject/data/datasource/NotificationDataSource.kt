package com.yerayyas.chatappkotlinproject.data.datasource

import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.model.NotificationPermissionState

/**
 * Data source contract for notification operations.
 *
 * This interface defines the operations that can be performed
 * on the notification system at the data layer level.
 */
interface NotificationDataSource {

    /**
     * Checks the current notification permission state.
     *
     * @return The current permission state
     */
    fun checkNotificationPermission(): NotificationPermissionState

    /**
     * Displays a notification to the user.
     *
     * @param notificationData The notification data to display
     * @param deviceCompatibility Device information for optimization
     * @return Result indicating success or failure
     */
    fun displayNotification(
        notificationData: NotificationData,
        deviceCompatibility: DeviceCompatibility
    ): Result<Unit>

    /**
     * Cancels all notifications for a specific user.
     *
     * @param userId The unique identifier of the user
     * @return Result indicating success or failure
     */
    fun cancelUserNotifications(userId: String): Result<Unit>

    /**
     * Cancels all active notifications.
     *
     * @return Result indicating success or failure
     */
    fun cancelAllNotifications(): Result<Unit>

    /**
     * Gets the count of currently active notifications.
     *
     * @return The number of active notifications
     */
    fun getActiveNotificationsCount(): Int
}