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
import com.google.accompanist.insets.ProvideWindowInsets
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
import com.yerayyas.chatappkotlinproject.presentation.activity.services.ActivityInitializationService
import com.yerayyas.chatappkotlinproject.presentation.activity.services.NotificationIntentService
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.navigation.AppContainer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MainActivity"

/**
 * The main and single activity of the application, serving as the entry point for the user interface.
 *
 * This refactored activity now delegates most responsibilities to specialized services:
 * - ActivityInitializationService: Handles Google Play Services, permissions, FCM tokens
 * - NotificationIntentService: Processes notification intents and navigation
 * - NotificationHelper: Manages notification clearing
 *
 * The activity now focuses primarily on:
 * - Setting up the Compose UI
 * - Handling the activity lifecycle callbacks
 * - Coordinating between services and the UI layer
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
    lateinit var notificationHelper: NotificationHelper

    // Request notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        activityInitializationService.getNotificationPermissionManager()
            .handlePermissionResult(isGranted)
    }

    companion object {
        /** Tracks whether the application has been initialized to distinguish a cold start from a warm start. */
        private var isAppInitialized = false
    }

    /**
     * Called when the activity is first created. This is where the UI is initialized and the initial intent is processed.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isAppAlreadyRunning = isAppInitialized || savedInstanceState != null
        Log.d(TAG, "onCreate: isAppAlreadyRunning=$isAppAlreadyRunning, isAppInitialized=$isAppInitialized, savedInstanceState=${savedInstanceState != null}")

        // Mark app as initialized on its first creation
        isAppInitialized = true

        // Initialize services with permission callback
        activityInitializationService.initialize(
            activity = this,
            lifecycleScope = lifecycleScope,
            onPermissionNeeded = {
                activityInitializationService.getNotificationPermissionManager()
                    .requestPermission(notificationPermissionLauncher)
            }
        )

        // Process any notification or deep-link from the initial intent before rendering the UI.
        val initialNavState =
            notificationIntentService.processInitialIntent(intent, isAppAlreadyRunning)

        setContent {
            ProvideWindowInsets {
                AppContainer(
                    activityViewModel = activityViewModel,
                    handleNotificationNavigation = handleNotificationNavigation,
                    handleDefaultNavigation = handleDefaultNavigation,
                    skipSplash = isAppAlreadyRunning,
                    initialNavState = initialNavState
                )
            }
        }
    }

    /**
     * Called by the system when the activity is started with a new intent while it is already running.
     * This is common when the user taps on a notification and the app is in the background.
     *
     * @param intent The new intent that was received.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        Log.d(TAG, "onNewIntent: A new intent has been received while the app is running.")
        notificationIntentService.handleNotificationIntent(intent, activityViewModel)
    }

    /**
     * Called when the activity is becoming visible to the user.
     * We clear notifications here as well to handle cases where the user returns to the app without a new intent.
     */
    override fun onStart() {
        super.onStart()
        notificationHelper.cancelAllNotifications()
    }
}
