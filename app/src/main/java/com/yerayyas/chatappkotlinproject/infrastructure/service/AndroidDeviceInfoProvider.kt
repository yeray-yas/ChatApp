package com.yerayyas.chatappkotlinproject.infrastructure.service

import android.os.Build
import com.yerayyas.chatappkotlinproject.domain.model.DeviceCompatibility
import com.yerayyas.chatappkotlinproject.domain.service.DeviceInfoProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [DeviceInfoProvider] domain service.
 *
 * This infrastructure service provides device information by accessing Android's Build class,
 * effectively isolating Android platform dependencies from the domain layer. The service
 * enables device-specific notification optimizations and compatibility handling throughout
 * the application.
 *
 * Key responsibilities:
 * - **Device Detection**: Retrieve manufacturer, brand, and model information
 * - **Platform Isolation**: Keep Android Build dependencies out of domain layer
 * - **Compatibility Support**: Enable device-specific feature optimization
 * - **Type Safety**: Convert raw device strings to domain model structures
 *
 * The service follows Clean Architecture principles by:
 * - Implementing the domain service interface without domain layer dependencies
 * - Providing platform-specific implementations for abstract domain contracts
 * - Enabling testability through dependency injection and interface abstraction
 * - Supporting multiple device types for notification optimization strategies
 *
 * Device information provided:
 * - **Manufacturer**: Hardware manufacturer (e.g., "Xiaomi", "HUAWEI", "OnePlus")
 * - **Brand**: Marketing brand name (e.g., "Redmi", "HONOR", "Pixel")
 * - **Model**: Specific device model for detailed compatibility handling
 *
 * This information is used primarily for notification delivery optimization on
 * devices with aggressive power management policies that may affect notification display.
 */
@Singleton
class AndroidDeviceInfoProvider @Inject constructor() : DeviceInfoProvider {

    /**
     * Retrieves device compatibility information from Android Build properties.
     *
     * This method accesses Android's Build class to gather device identification
     * information, which is then packaged into a domain model for use throughout
     * the application. The information is used primarily for notification delivery
     * optimization and device-specific feature handling.
     *
     * Build properties accessed:
     * - **Build.MANUFACTURER**: The manufacturer of the product/hardware (e.g., "Xiaomi")
     * - **Build.BRAND**: The consumer-visible brand name (e.g., "Redmi", "Mi")
     * - **Build.MODEL**: The end-user-visible device model name (e.g., "Redmi Note 10")
     *
     * The domain model automatically determines device type based on these properties
     * to enable appropriate notification delivery strategies for different manufacturers.
     *
     * @return DeviceCompatibility object containing manufacturer, brand, and model information
     */
    override fun getDeviceCompatibility(): DeviceCompatibility {
        return DeviceCompatibility(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL
        )
    }
}
