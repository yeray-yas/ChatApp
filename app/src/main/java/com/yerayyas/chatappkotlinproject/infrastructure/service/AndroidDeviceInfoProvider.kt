package com.yerayyas.chatappkotlinproject.infrastructure.service

import android.os.Build
import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.service.DeviceInfoProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [DeviceInfoProvider].
 *
 * This class provides device information by accessing Android's Build class,
 * keeping the Android-specific dependencies isolated from the domain layer.
 */
@Singleton
class AndroidDeviceInfoProvider @Inject constructor() : DeviceInfoProvider {

    override fun getDeviceCompatibility(): DeviceCompatibility {
        return DeviceCompatibility(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL
        )
    }
}