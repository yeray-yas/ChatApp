package com.yerayyas.chatappkotlinproject.domain.model

/**
 * Domain model representing the notification permission state.
 *
 * This encapsulates the permission logic in a type-safe way without
 * Android dependencies in the domain layer.
 */
sealed class NotificationPermissionState {
    /**
     * Notifications are allowed and can be displayed.
     */
    object Granted : NotificationPermissionState()

    /**
     * Notifications are not allowed due to missing permissions.
     */
    object Denied : NotificationPermissionState()

    /**
     * Permission state is not applicable (older Android versions).
     */
    object NotRequired : NotificationPermissionState()

    /**
     * Returns true if notifications can be displayed.
     */
    val isAllowed: Boolean
        get() = this is Granted || this is NotRequired
}
