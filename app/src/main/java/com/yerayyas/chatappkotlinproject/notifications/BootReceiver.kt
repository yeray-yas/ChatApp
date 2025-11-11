package com.yerayyas.chatappkotlinproject.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.usecases.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that handles system events critical for notification functionality.
 *
 * This receiver is essential for maintaining reliable push notifications, especially on
 * devices with aggressive battery optimization (like Xiaomi, OnePlus, etc.) that may
 * kill background processes. It ensures FCM tokens remain valid after system events.
 *
 * Key responsibilities:
 * - Refreshing FCM tokens after device boot completion
 * - Updating tokens after app package updates or replacements
 * - Ensuring authenticated users maintain notification connectivity
 * - Using domain layer use cases for business logic operations
 * - Providing comprehensive logging for debugging system event handling
 *
 * Architecture Pattern: Infrastructure Layer Component with Use Case Integration
 * - Responds to Android system broadcast events
 * - Delegates business logic to domain layer use cases
 * - Uses dependency injection for loose coupling
 * - Handles asynchronous operations with proper coroutine scope
 * - Follows Clean Architecture principles for system integration
 *
 * Supported system events:
 * - ACTION_BOOT_COMPLETED: Device has finished booting
 * - ACTION_MY_PACKAGE_REPLACED: This app package was replaced
 * - ACTION_PACKAGE_REPLACED: Any package was replaced (filtered to this app)
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getCurrentUserIdUseCase: GetCurrentUserIdUseCase

    @Inject
    lateinit var updateFcmTokenUseCase: UpdateFcmTokenUseCase

    /**
     * Coroutine scope for handling asynchronous operations.
     * Uses SupervisorJob to prevent child coroutine failures from affecting others.
     */
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootReceiver"
    }

    /**
     * Called when a broadcast is received.
     *
     * This method handles various system events that may affect notification functionality.
     * It processes each event type appropriately and ensures FCM tokens are refreshed
     * when necessary for authenticated users.
     *
     * @param context The context in which the receiver is running
     * @param intent The intent containing the broadcast action and data
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received system broadcast: ${intent.action}")

        try {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    Log.i(TAG, "Device boot completed - checking FCM token refresh needs")
                    handleBootCompleted()
                }

                Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    Log.i(TAG, "App package replaced - refreshing FCM token")
                    handlePackageReplaced("My package replacement")
                }

                Intent.ACTION_PACKAGE_REPLACED -> {
                    Log.i(TAG, "Package replaced event received")
                    handlePackageReplaced("Package replacement")
                }

                else -> {
                    Log.d(TAG, "Unhandled broadcast action: ${intent.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling broadcast intent: ${intent.action}", e)
        }
    }

    /**
     * Handles device boot completion by checking user authentication and refreshing FCM token.
     *
     * This is critical for maintaining notification functionality after device restarts,
     * especially on devices that clear app state during boot.
     */
    private fun handleBootCompleted() {
        receiverScope.launch {
            try {
                Log.d(TAG, "Processing boot completion event")

                val currentUserId = getCurrentUserIdUseCase()
                if (currentUserId.isNotEmpty()) {
                    Log.d(TAG, "User is authenticated after boot - refreshing FCM token")
                    refreshFcmTokenUsingUseCase("Boot completion")
                } else {
                    Log.d(TAG, "User not authenticated after boot - skipping FCM token refresh")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling boot completion", e)
            }
        }
    }

    /**
     * Handles package replacement events by checking user authentication and refreshing FCM token.
     *
     * This ensures notification functionality is restored after app updates or reinstalls.
     *
     * @param eventDescription Description of the package replacement event for logging
     */
    private fun handlePackageReplaced(eventDescription: String) {
        receiverScope.launch {
            try {
                Log.d(TAG, "Processing package replacement: $eventDescription")

                val currentUserId = getCurrentUserIdUseCase()
                if (currentUserId.isNotEmpty()) {
                    Log.d(
                        TAG,
                        "User is authenticated after $eventDescription - refreshing FCM token"
                    )
                    refreshFcmTokenUsingUseCase(eventDescription)
                } else {
                    Log.d(
                        TAG,
                        "User not authenticated after $eventDescription - skipping FCM token refresh"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling package replacement: $eventDescription", e)
            }
        }
    }

    /**
     * Refreshes the FCM token using domain layer use case.
     *
     * This method uses the Clean Architecture approach by delegating the token
     * refresh operation to the domain layer instead of directly calling Firebase APIs.
     * This ensures proper error handling, logging, and maintainability.
     *
     * @param eventContext Description of the event that triggered this refresh for logging
     */
    private fun refreshFcmTokenUsingUseCase(eventContext: String) {
        try {
            Log.d(TAG, "Initiating FCM token refresh for: $eventContext")

            // Get current token directly from Firebase (infrastructure concern)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "User authentication lost during token refresh")
                return
            }

            // Use Firebase directly for token retrieval (this is infrastructure layer)
            // The token update will be handled by the use case (domain layer)
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNotBlank()) {
                        receiverScope.launch {
                            try {
                                updateFcmTokenUseCase(token)
                                Log.i(TAG, "FCM token refreshed successfully for: $eventContext")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update FCM token using use case", e)
                            }
                        }
                    } else {
                        Log.w(TAG, "Received empty FCM token for: $eventContext")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to retrieve FCM token for: $eventContext", exception)
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error in FCM token refresh process for: $eventContext", e)
        }
    }
}