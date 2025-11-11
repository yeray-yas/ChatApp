package com.yerayyas.chatappkotlinproject.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelAllNotificationsUseCase
import com.yerayyas.chatappkotlinproject.presentation.activity.services.ActivityInitializationService
import com.yerayyas.chatappkotlinproject.presentation.activity.services.NotificationIntentService
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.navigation.AppContainer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivity"

/**
 * Main and single activity of the application, serving as the entry point for the user interface.
 *
 * This activity follows the Clean Architecture principles and delegates responsibilities to specialized services:
 * - [ActivityInitializationService]: Handles Google Play Services, permissions, and FCM token management
 * - [NotificationIntentService]: Processes notification intents and manages navigation state
 * - [CancelAllNotificationsUseCase]: Manages notification clearing using domain layer logic
 *
 * The activity focuses primarily on:
 * - Setting up the Compose UI container
 * - Managing the activity lifecycle
 * - Coordinating between services and the presentation layer
 * - Handling permission requests through result contracts
 *
 * Architecture Pattern: Clean Architecture with MVVM
 * - Uses dependency injection with Hilt for loose coupling
 * - Leverages use cases for business logic operations
 * - Maintains separation between UI, domain, and data layers
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val activityViewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var activityInitializationService: ActivityInitializationService

    @Inject
    lateinit var notificationIntentService: NotificationIntentService

    @Inject
    lateinit var handleNotificationNavigation: HandleNotificationNavigationUseCase

    @Inject
    lateinit var handleDefaultNavigation: HandleDefaultNavigationUseCase

    @Inject
    lateinit var cancelAllNotificationsUseCase: CancelAllNotificationsUseCase

    /**
     * Activity result launcher for notification permission requests.
     * Uses the modern Activity Result API for better lifecycle management.
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        activityInitializationService.getNotificationPermissionManager()
            .handlePermissionResult(isGranted)
    }

    companion object {
        /**
         * Tracks whether the application has been initialized to distinguish between
         * cold start (first app launch) and warm start (returning from background).
         * This helps optimize the user experience by skipping splash screens appropriately.
         */
        private var isAppInitialized = false
    }

    /**
     * Called when the activity is first created.
     *
     * This method handles:
     * - Activity configuration (edge-to-edge display)
     * - Service initialization with proper lifecycle management
     * - Initial intent processing for deep-link navigation
     * - Notification clearing using domain layer use case
     * - UI composition setup with dependency injection
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state, or null
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isAppAlreadyRunning = isAppInitialized || savedInstanceState != null
        Log.d(TAG, "onCreate: isAppAlreadyRunning=$isAppAlreadyRunning, isAppInitialized=$isAppInitialized, savedInstanceState=${savedInstanceState != null}")

        // Mark app as initialized on its first creation to track warm/cold starts
        isAppInitialized = true

        // Initialize core services with permission callback delegation
        initializeServices()

        // Process any notification or deep-link from the initial intent before rendering the UI
        val initialNavState =
            notificationIntentService.processInitialIntent(intent, isAppAlreadyRunning)

        // Clear all notifications when the app opens using domain layer use case
        clearAllNotifications()

        // Set up the Compose UI with all required dependencies
        setContent {
            AppContainer(
                activityViewModel = activityViewModel,
                handleNotificationNavigation = handleNotificationNavigation,
                handleDefaultNavigation = handleDefaultNavigation,
                skipSplash = isAppAlreadyRunning,
                initialNavState = initialNavState
            )
        }
    }

    /**
     * Called when the activity receives a new intent while already running.
     *
     * This typically occurs when:
     * - User taps on a notification while app is in background
     * - App receives a deep-link while already active
     * - System triggers the activity with new parameters
     *
     * @param intent The new intent that was received
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's current intent
        Log.d(TAG, "onNewIntent: Received new intent while app is running")

        // Clear all notifications when opening from a notification using domain layer
        clearAllNotifications()

        // Process the new intent and handle any navigation requirements
        notificationIntentService.handleNotificationIntent(intent, activityViewModel)
    }

    /**
     * Called when the activity is becoming visible to the user.
     *
     * We clear notifications here as well to handle cases where the user
     * returns to the app without triggering a new intent (e.g., through task switcher).
     */
    override fun onStart() {
        super.onStart()
        // Clear notifications when app becomes visible using domain layer use case
        clearAllNotifications()
    }

    /**
     * Initializes all required services with proper error handling and lifecycle management.
     * Uses dependency injection to maintain loose coupling between components.
     */
    private fun initializeServices() {
        try {
            activityInitializationService.initialize(
                activity = this,
                lifecycleScope = lifecycleScope,
                onPermissionNeeded = {
                    activityInitializationService.getNotificationPermissionManager()
                        .requestPermission(notificationPermissionLauncher)
                }
            )
            Log.d(TAG, "Services initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize services", e)
            // App can still function with limited capabilities
        }
    }

    /**
     * Clears all active notifications using the domain layer use case.
     * This follows Clean Architecture principles by using business logic
     * instead of directly calling infrastructure services.
     */
    private fun clearAllNotifications() {
        lifecycleScope.launch {
            try {
                val result = cancelAllNotificationsUseCase()
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully cleared all notifications")
                } else {
                    Log.w(
                        TAG,
                        "Failed to clear notifications: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing notifications", e)
                // Non-critical error, app continues functioning
            }
        }
    }
}
