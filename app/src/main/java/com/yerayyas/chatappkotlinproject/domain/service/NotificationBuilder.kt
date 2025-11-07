package com.yerayyas.chatappkotlinproject.domain.service

import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData

/**
 * Domain service contract for building platform-specific notifications.
 *
 * This interface abstracts the notification building process, allowing
 * different implementations for different platforms or testing.
 */
interface NotificationBuilder {

    /**
     * Builds a notification from the given data with device-specific optimizations.
     *
     * @param notificationData The data to build the notification from
     * @param deviceCompatibility Device information for optimization
     * @param pendingIntentFactory Factory for creating platform-specific pending intents
     * @return The built notification as a platform-specific object
     */
    fun buildNotification(
        notificationData: NotificationData,
        deviceCompatibility: DeviceCompatibility,
        pendingIntentFactory: PendingIntentFactory
    ): Any // Using Any to avoid Android dependencies in domain layer

    /**
     * Builds a summary notification for grouped notifications.
     *
     * @param notificationCount The number of active notifications
     * @param appName The name of the application
     * @return The built summary notification as a platform-specific object
     */
    fun buildSummaryNotification(
        notificationCount: Int,
        appName: String
    ): Any
}

/**
 * Factory interface for creating pending intents.
 *
 * This abstraction allows the domain layer to request pending intents
 * without depending on Android-specific implementation details.
 */
interface PendingIntentFactory {
    /**
     * Creates a pending intent for opening a chat.
     *
     * @param senderId Unique identifier for the message sender
     * @param senderName Display name of the sender
     * @param chatId Unique identifier for the chat conversation
     * @return A platform-specific pending intent
     */
    fun createChatPendingIntent(
        senderId: String,
        senderName: String,
        chatId: String
    ): Any
}