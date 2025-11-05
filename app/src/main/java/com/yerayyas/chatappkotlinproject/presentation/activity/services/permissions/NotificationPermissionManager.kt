package com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationPermissionManager"

/**
 * Manager responsible for handling notification permissions.
 */
@Singleton
class NotificationPermissionManager @Inject constructor() {

    /**
     * Checks if notification permissions are needed and returns the appropriate action.
     *
     * @param activity The activity context
     * @return PermissionAction indicating what action should be taken
     */
    fun checkPermissionStatus(activity: Activity): PermissionAction {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "POST_NOTIFICATIONS permission already granted")
                    PermissionAction.ALREADY_GRANTED
                }
                else -> {
                    Log.d(TAG, "POST_NOTIFICATIONS permission needed")
                    PermissionAction.REQUEST_NEEDED
                }
            }
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS not required for API level < 33")
            PermissionAction.NOT_REQUIRED
        }
    }

    /**
     * Requests notification permission using the provided launcher.
     *
     * @param launcher The permission launcher from the activity
     */
    fun requestPermission(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Handles the permission result.
     *
     * @param isGranted Whether the permission was granted
     */
    fun handlePermissionResult(isGranted: Boolean) {
        Log.d(TAG, "Notification permission result: $isGranted")
        if (isGranted) {
            Log.i(TAG, "POST_NOTIFICATIONS permission granted")
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied")
        }
    }
}

/**
 * Enum representing the different permission actions that may be needed.
 */
enum class PermissionAction {
    ALREADY_GRANTED,
    REQUEST_NEEDED,
    NOT_REQUIRED
}