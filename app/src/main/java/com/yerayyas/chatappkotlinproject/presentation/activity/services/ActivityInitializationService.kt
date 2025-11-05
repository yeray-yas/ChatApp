package com.yerayyas.chatappkotlinproject.presentation.activity.services

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.messaging.FirebaseMessaging
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
import com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions.NotificationPermissionManager
import com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions.PermissionAction
import com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions.PlayServicesManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ActivityInitService"

/**
 * Service responsible for handling the initialization logic of MainActivity.
 * This includes verifying Google Play Services, managing notification permissions,
 * updating FCM tokens, and clearing notifications.
 */
@Singleton
class ActivityInitializationService @Inject constructor(
    private val playServicesManager: PlayServicesManager,
    private val notificationPermissionManager: NotificationPermissionManager,
    private val notificationHelper: NotificationHelper,
    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase
) {

    /**
     * Performs all initialization tasks required when the activity is created.
     *
     * @param activity The activity instance
     * @param lifecycleScope The lifecycle scope for coroutines
     * @param onPermissionNeeded Callback when permission request is needed
     */
    fun initialize(
        activity: Activity,
        lifecycleScope: LifecycleCoroutineScope,
        onPermissionNeeded: (() -> Unit)? = null
    ) {
        Log.d(TAG, "Starting activity initialization")

        // Verify Google Play Services
        playServicesManager.verifyGooglePlayServices(activity)

        // Check notification permissions
        val permissionAction = notificationPermissionManager.checkPermissionStatus(activity)
        if (permissionAction == PermissionAction.REQUEST_NEEDED) {
            onPermissionNeeded?.invoke()
        }

        // Clear all notifications when app starts
        notificationHelper.cancelAllNotifications()

        // Get and update FCM token
        updateFcmToken(lifecycleScope)

        Log.d(TAG, "Activity initialization completed")
    }

    /**
     * Gets the notification permission manager for direct access.
     */
    fun getNotificationPermissionManager(): NotificationPermissionManager {
        return notificationPermissionManager
    }

    /**
     * Retrieves and updates the FCM token
     */
    private fun updateFcmToken(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM token retrieved successfully")
                updateFcmTokenUseCase(token)
                Log.d(TAG, "FCM token updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve or update FCM token: ${e.message}")
            }
        }
    }
}