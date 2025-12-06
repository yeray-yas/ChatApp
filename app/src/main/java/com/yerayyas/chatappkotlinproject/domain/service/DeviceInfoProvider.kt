package com.yerayyas.chatappkotlinproject.domain.service

import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility

/**
 * Domain service contract for providing device information.
 *
 * This interface abstracts device information retrieval, allowing
 * the domain layer to remain independent of Android Build APIs.
 */
interface DeviceInfoProvider {

    /**
     * Retrieves the current device compatibility information.
     *
     * @return Device compatibility information for notification optimization
     */
    fun getDeviceCompatibility(): DeviceCompatibility
}
