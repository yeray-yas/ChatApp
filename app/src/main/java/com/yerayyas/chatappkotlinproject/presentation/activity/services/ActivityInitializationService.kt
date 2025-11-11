package com.yerayyas.chatappkotlinproject.presentation.activity.services

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.messaging.FirebaseMessaging
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelAllNotificationsUseCase
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
 *
 * This service follows Clean Architecture principles by delegating business logic operations
 * to the domain layer through use cases. It coordinates between different managers and use cases
 * to ensure proper app initialization.
 *
 * Responsibilities:
 * - Verifying Google Play Services availability
 * - Managing notification permissions through [NotificationPermissionManager]
 * - Clearing notifications using domain layer [CancelAllNotificationsUseCase]
 * - Updating FCM tokens via [UpdateFcmTokenUseCase]
 * - Providing centralized error handling and logging
 *
 * Architecture Pattern: Service Layer with Use Case delegation
 * - Uses dependency injection for loose coupling
 * - Delegates business logic to domain layer use cases
 * - Provides error handling and recovery mechanisms
 */
@Singleton
class ActivityInitializationService @Inject constructor(
    private val playServicesManager: PlayServicesManager,
    private val notificationPermissionManager: NotificationPermissionManager,
    private val cancelAllNotificationsUseCase: CancelAllNotificationsUseCase,
    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase
) {

    /**
     * Performs all initialization tasks required when the activity is created.
     *
     * This method coordinates the initialization process by:
     * 1. Verifying Google Play Services availability
     * 2. Checking and requesting notification permissions if needed
     * 3. Clearing all existing notifications using domain layer logic
     * 4. Retrieving and updating the FCM token asynchronously
     *
     * All operations include proper error handling to ensure the app can continue
     * functioning even if some initialization steps fail.
     *
     * @param activity The activity instance for permission and service checks
     * @param lifecycleScope The lifecycle-aware coroutine scope for async operations
     * @param onPermissionNeeded Optional callback invoked when permission request is needed
     */
    fun initialize(
        activity: Activity,
        lifecycleScope: LifecycleCoroutineScope,
        onPermissionNeeded: (() -> Unit)? = null
    ) {
        Log.d(TAG, "Starting comprehensive activity initialization")

        try {
            // Verify Google Play Services availability
            verifyPlayServices(activity)

            // Handle notification permissions
            handleNotificationPermissions(activity, onPermissionNeeded)

            // Clear all existing notifications using domain layer
            clearExistingNotifications(lifecycleScope)

            // Retrieve and update FCM token asynchronously
            initializeFcmToken(lifecycleScope)

            Log.d(TAG, "Activity initialization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during activity initialization", e)
            // App can still function with limited capabilities
        }
    }

    /**
     * Provides access to the notification permission manager for direct interaction.
     *
     * This is primarily used by the MainActivity to handle permission result callbacks
     * and maintain proper separation between the service and permission management logic.
     *
     * @return The [NotificationPermissionManager] instance for permission operations
     */
    fun getNotificationPermissionManager(): NotificationPermissionManager {
        return notificationPermissionManager
    }

    /**
     * Verifies Google Play Services availability with proper error handling.
     *
     * @param activity The activity context for verification
     */
    private fun verifyPlayServices(activity: Activity) {
        try {
            playServicesManager.verifyGooglePlayServices(activity)
            Log.d(TAG, "Google Play Services verified successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify Google Play Services", e)
            // Non-critical error, app can continue with limited functionality
        }
    }

    /**
     * Handles notification permission checking and requests.
     *
     * @param activity The activity context for permission checks
     * @param onPermissionNeeded Callback invoked when permission request is needed
     */
    private fun handleNotificationPermissions(
        activity: Activity,
        onPermissionNeeded: (() -> Unit)?
    ) {
        try {
            val permissionAction = notificationPermissionManager.checkPermissionStatus(activity)
            if (permissionAction == PermissionAction.REQUEST_NEEDED) {
                Log.d(TAG, "Notification permission request needed")
                onPermissionNeeded?.invoke() ?: Log.w(
                    TAG,
                    "Permission needed but no callback provided"
                )
            } else {
                Log.d(TAG, "Notification permissions already granted or not needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification permissions", e)
        }
    }

    /**
     * Clears all existing notifications using the domain layer use case.
     * This ensures proper separation of concerns and consistent error handling.
     *
     * @param lifecycleScope The coroutine scope for async operations
     */
    private fun clearExistingNotifications(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            try {
                val result = cancelAllNotificationsUseCase()
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully cleared all existing notifications")
                } else {
                    Log.w(
                        TAG,
                        "Failed to clear notifications: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing existing notifications", e)
                // Non-critical error, initialization continues
            }
        }
    }

    /**
     * Initializes FCM token by retrieving the current token and updating it in the backend.
     *
     * This operation runs asynchronously and includes comprehensive error handling
     * to ensure app functionality is not affected if token operations fail.
     *
     * @param lifecycleScope The coroutine scope for async token operations
     */
    private fun initializeFcmToken(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Retrieving FCM token")
                val token = FirebaseMessaging.getInstance().token.await()

                if (token.isNotBlank()) {
                    Log.d(TAG, "FCM token retrieved successfully")

                    // Update token using domain layer use case
                    updateFcmTokenUseCase(token)
                    Log.d(TAG, "FCM token updated successfully in backend")
                } else {
                    Log.w(TAG, "Retrieved FCM token is empty")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve or update FCM token", e)
                // Non-critical error, app can function without push notifications
            }
        }
    }
}
