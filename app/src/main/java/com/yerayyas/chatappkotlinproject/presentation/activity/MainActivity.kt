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
 * Single-Activity entry point for the application.
 *
 * This Activity serves as the host container for the Jetpack Compose UI and orchestrates
 * the initialization of core services. It strictly follows Clean Architecture principles
 * by delegating business logic to UseCases and specific infrastructure services.
 *
 * Key Responsibilities:
 * - **Host:** Configures the Compose content via [AppContainer].
 * - **Initialization:** Bootstraps [ActivityInitializationService] (Permissions, FCM, Google Play Services).
 * - **Navigation Routing:** Acts as the traffic controller for Deep Links and Notifications via [NotificationIntentService].
 * - **Lifecycle Management:** Handles `onNewIntent` to support navigation updates while the app is running.
 *
 * Architecture: Clean Architecture + MVVM (Entry Point)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val activityViewModel: MainActivityViewModel by viewModels()

    // --- Services & Use Cases Injection ---

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
     * Handles the runtime permission request flow for Notifications (Android 13+).
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        activityInitializationService.getNotificationPermissionManager()
            .handlePermissionResult(isGranted)
    }

    companion object {
        /**
         * Static flag to track process lifecycle.
         * Used to differentiate between a Cold Start (app killed) and a Warm Start (config change/rotation).
         * This allows the Splash Screen to be skipped on rotation to improve UX.
         */
        private var isAppInitialized = false
    }

    /**
     * Initializes the Activity, services, and UI content.
     *
     * Workflow:
     * 1. Configure Edge-to-Edge display.
     * 2. Determine start type (Cold vs Warm).
     * 3. Initialize infrastructure services.
     * 4. Process any pending intent (Deep Link).
     * 5. Delegate start destination calculation to ViewModel.
     * 6. Render Compose UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Detect if this is a recreation (e.g., rotation) or a warm start to skip Splash
        val isAppAlreadyRunning = isAppInitialized || savedInstanceState != null
        Log.d(TAG, "onCreate: WarmStart=$isAppAlreadyRunning (Initialized=$isAppInitialized)")

        // Mark as initialized for future recreations within the same process
        isAppInitialized = true

        initializeServices()

        // Parse deep-link/notification data from the intent
        val initialNavState = notificationIntentService.processInitialIntent(
            intent,
            isAppAlreadyRunning
        )

        // Critical: Hand off navigation decision to ViewModel
        activityViewModel.resolveStartDestination(
            skipSplash = isAppAlreadyRunning,
            initialNavState = initialNavState
        )

        clearAllNotifications()

        setContent {
            AppContainer(
                activityViewModel = activityViewModel,
                handleNotificationNavigation = handleNotificationNavigation,
                handleDefaultNavigation = handleDefaultNavigation
            )
        }
    }

    /**
     * Handles new intents received while the Activity is already in the foreground.
     *
     * This is crucial for "SingleTop" behavior where tapping a notification
     * updates the existing activity rather than creating a new stack.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent property to the new one
        Log.d(TAG, "onNewIntent: Processing new deep link/notification")

        clearAllNotifications()

        // Delegate the new navigation request to the service -> ViewModel pipeline
        notificationIntentService.handleNotificationIntent(intent, activityViewModel)
    }

    override fun onStart() {
        super.onStart()
        // Ensure notifications are cleared when user manually returns to the app
        clearAllNotifications()
    }

    /**
     * Safely initializes infrastructure services.
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
            Log.d(TAG, "Services initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Service initialization failed", e)
        }
    }

    /**
     * Executes the [CancelAllNotificationsUseCase] to clean up the system tray.
     */
    private fun clearAllNotifications() {
        lifecycleScope.launch {
            try {
                cancelAllNotificationsUseCase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear notifications", e)
            }
        }
    }
}
