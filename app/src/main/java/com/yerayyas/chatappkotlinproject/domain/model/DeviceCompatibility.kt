package com.yerayyas.chatappkotlinproject.domain.model

/**
 * Domain model representing device-specific compatibility information.
 *
 * This encapsulates device detection logic without Android Build dependencies
 * in the domain layer.
 */
data class DeviceCompatibility(
    val manufacturer: String,
    val brand: String,
    val model: String
) {
    /**
     * Determines the device type based on manufacturer and brand information.
     */
    val deviceType: DeviceType
        get() = when {
            isXiaomiDevice() -> DeviceType.XIAOMI
            isHuaweiDevice() -> DeviceType.HUAWEI
            isOnePlusDevice() -> DeviceType.ONEPLUS
            isPixelDevice() -> DeviceType.PIXEL
            else -> DeviceType.GENERIC
        }

    private fun isXiaomiDevice(): Boolean =
        manufacturer.equals("Xiaomi", ignoreCase = true) ||
                brand.equals("Xiaomi", ignoreCase = true) ||
                brand.equals("Redmi", ignoreCase = true)

    private fun isHuaweiDevice(): Boolean =
        manufacturer.equals("HUAWEI", ignoreCase = true) ||
                brand.equals("HONOR", ignoreCase = true)

    private fun isOnePlusDevice(): Boolean =
        manufacturer.equals("OnePlus", ignoreCase = true)

    private fun isPixelDevice(): Boolean =
        model.contains("Pixel", ignoreCase = true)
}

/**
 * Enum representing different device types that require specific notification handling.
 */
enum class DeviceType {
    XIAOMI,
    HUAWEI,
    ONEPLUS,
    PIXEL,
    GENERIC
}
