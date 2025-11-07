package com.yerayyas.chatappkotlinproject.domain.service

import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility

/**
 * Domain service contract for managing notification channels.
 *
 * This interface abstracts the platform-specific notification channel
 * management, allowing the domain layer to remain independent of Android APIs.
 */
interface NotificationChannelManager {

    /**
     * Ensures that the notification channel exists and is properly configured.
     *
     * @param deviceCompatibility Device information for optimization
     */
    fun ensureChannelExists(deviceCompatibility: DeviceCompatibility)

    /**
     * Checks if the notification channel exists.
     *
     * @return true if the channel exists, false otherwise
     */
    fun channelExists(): Boolean
}
