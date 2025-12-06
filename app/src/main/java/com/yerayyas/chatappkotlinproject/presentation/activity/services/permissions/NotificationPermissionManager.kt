package com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationPermissionManager"

/**
 * Manager responsible for handling notification permissions in Android applications.
 *
 * This manager encapsulates all the logic related to notification permission management,
 * including permission checking, requesting, and result handling. It follows Android's
 * modern permission model and handles API level differences appropriately.
 *
 * Key responsibilities:
 * - Checking notification permission status based on Android version
 * - Handling permission requests using modern Activity Result API
 * - Processing permission grant/denial results
 * - Providing comprehensive logging for debugging permission flows
 * - Managing API level compatibility (Android 13+ requires explicit permission)
 *
 * Architecture Pattern: Manager Pattern
 * - Encapsulates permission-related operations
 * - Provides a clean interface for permission management
 * - Handles Android version compatibility transparently
 * - Includes comprehensive error handling and logging
 * - Follows single responsibility principle
 */
@Singleton
class NotificationPermissionManager @Inject constructor() {

    /**
     * Checks the current notification permission status and determines the required action.
     *
     * This method handles the complexity of Android's permission model, where notification
     * permissions are only required for Android 13 (API 33) and above. For older versions,
     * notifications are allowed by default.
     *
     * The method performs:
     * - API level checking to determine if permission is required
     * - Current permission status verification
     * - Appropriate action recommendation based on status
     *
     * @param activity The activity context required for permission checking
     * @return [PermissionAction] indicating the recommended action to take
     * @throws SecurityException if permission checking fails unexpectedly
     */
    fun checkPermissionStatus(activity: Activity): PermissionAction {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ requires explicit notification permission
                checkNotificationPermissionForModernAndroid(activity)
            } else {
                // Pre-Android 13 doesn't require explicit notification permission
                Log.d(
                    TAG,
                    "Notification permission not required for API level ${Build.VERSION.SDK_INT}"
                )
                PermissionAction.NOT_REQUIRED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification permission status", e)
            // Default to request needed to ensure notifications work
            PermissionAction.REQUEST_NEEDED
        }
    }

    /**
     * Requests notification permission using the modern Activity Result API.
     *
     * This method safely requests the POST_NOTIFICATIONS permission using the provided
     * launcher. It includes API level checking to ensure the request is only made on
     * compatible Android versions.
     *
     * @param launcher The [ActivityResultLauncher] configured for permission requests
     * @throws IllegalArgumentException if launcher is not properly configured
     */
    fun requestPermission(launcher: ActivityResultLauncher<String>) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d(
                    TAG,
                    "Requesting POST_NOTIFICATIONS permission for API ${Build.VERSION.SDK_INT}"
                )
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d(
                    TAG,
                    "Permission request skipped - not required for API level ${Build.VERSION.SDK_INT}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting notification permission", e)
        }
    }

    /**
     * Processes the result of a notification permission request.
     *
     * This method handles both granted and denied permission scenarios, providing
     * appropriate logging and potential fallback strategies. It includes guidance
     * for handling denial scenarios.
     *
     * Permission handling strategy:
     * - Granted: Log success and continue normal operation
     * - Denied: Log warning and continue with limited notification capability
     * - Error: Log error and attempt graceful degradation
     *
     * @param isGranted Boolean indicating whether the permission was granted
     */
    fun handlePermissionResult(isGranted: Boolean) {
        try {
            Log.d(TAG, "Processing notification permission result: $isGranted")

            if (isGranted) {
                Log.i(
                    TAG,
                    "POST_NOTIFICATIONS permission granted - Full notification functionality available"
                )
                onPermissionGranted()
            } else {
                Log.w(
                    TAG,
                    "POST_NOTIFICATIONS permission denied - Limited notification functionality"
                )
                onPermissionDenied()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling permission result", e)
        }
    }

    /**
     * Checks notification permission specifically for Android 13+ devices.
     *
     * This method isolates the permission checking logic for modern Android versions
     * where explicit notification permission is required.
     *
     * @param activity The activity context for permission checking
     * @return [PermissionAction] based on current permission status
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermissionForModernAndroid(activity: Activity): PermissionAction {
        return when (ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        )) {
            PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted")
                PermissionAction.ALREADY_GRANTED
            }

            PackageManager.PERMISSION_DENIED -> {
                Log.d(TAG, "POST_NOTIFICATIONS permission required - need to request")
                PermissionAction.REQUEST_NEEDED
            }

            else -> {
                Log.w(TAG, "Unexpected permission state - defaulting to request needed")
                PermissionAction.REQUEST_NEEDED
            }
        }
    }

    /**
     * Handles successful permission grant scenario.
     * Can be extended for additional setup when permission is granted.
     */
    private fun onPermissionGranted() {
        // Future: Could trigger notification setup, analytics events, etc.
        Log.d(TAG, "Notification permission granted - ready for full notification features")
    }

    /**
     * Handles permission denial scenario.
     * Provides graceful degradation and user guidance.
     */
    private fun onPermissionDenied() {
        // Future: Could show user education, enable alternative notification methods, etc.
        Log.d(TAG, "Notification permission denied - app will function with limited notifications")
    }
}

/**
 * Enum representing the different actions that may be required for notification permissions.
 *
 * This enum provides a clear contract for permission status and helps coordinate
 * between different components handling permission workflows.
 *
 * @property ALREADY_GRANTED Permission is already granted, no action needed
 * @property REQUEST_NEEDED Permission needs to be requested from the user
 * @property NOT_REQUIRED Permission is not required for this Android version
 */
enum class PermissionAction {
    /** Notification permission is already granted */
    ALREADY_GRANTED,

    /** Notification permission needs to be requested */
    REQUEST_NEEDED,

    /** Notification permission is not required for this Android version */
    NOT_REQUIRED
}
