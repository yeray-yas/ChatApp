package com.yerayyas.chatappkotlinproject.domain.repository

import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.model.NotificationPermissionState

/**
 * Domain repository contract for notification management.
 *
 * This interface defines the core notification operations without
 * any Android-specific dependencies, following Clean Architecture principles.
 */
interface NotificationRepository {

    /**
     * Checks the current notification permission state.
     *
     * @return The current permission state encapsulated in a domain model.
     */
    fun getNotificationPermissionState(): NotificationPermissionState

    /**
     * Displays a chat notification to the user.
     *
     * @param notificationData The notification data to display.
     * @return Result indicating success or failure with error details.
     */
    suspend fun showNotification(notificationData: NotificationData): Result<Unit>

    /**
     * Cancels all notifications for a specific user.
     *
     * @param userId The unique identifier of the user whose notifications should be canceled.
     * @return Result indicating success or failure with error details.
     */
    suspend fun cancelNotificationsForUser(userId: String): Result<Unit>

    /**
     * Cancels all active notifications.
     *
     * @return Result indicating success or failure with error details.
     */
    suspend fun cancelAllNotifications(): Result<Unit>

    /**
     * Gets the count of currently active notifications.
     *
     * @return The number of active notifications.
     */
    suspend fun getActiveNotificationsCount(): Int
}