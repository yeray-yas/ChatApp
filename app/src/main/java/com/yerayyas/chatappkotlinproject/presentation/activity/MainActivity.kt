package com.yerayyas.chatappkotlinproject.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.accompanist.insets.ProvideWindowInsets
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.ProcessNotificationIntentUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.navigation.AppContainer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MainActivity"

/**
 * The main and single activity of the application, serving as the entry point for the user interface.
 *
 * This activity is responsible for:
 * - Setting up the Jetpack Compose content with edge-to-edge display.
 * - Handling the initial intent on app launch (cold or warm start) to process potential notification deep-links.
 * - Managing new intents received while the activity is running (e.g., from a notification click).
 * - Clearing all active chat notifications when the app comes to the foreground to provide a clean state.
 * - Coordinating with various use cases to handle navigation logic based on the app's state and incoming intents.
 *
 * It uses Hilt for dependency injection to get instances of ViewModels and use cases.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val activityViewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var processNotificationIntent: ProcessNotificationIntentUseCase

    @Inject
    lateinit var handleNotificationNavigation: HandleNotificationNavigationUseCase

    @Inject
    lateinit var handleDefaultNavigation: HandleDefaultNavigationUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

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

        // Dismiss all notifications to ensure a clean slate when the app is brought to the foreground.
        notificationHelper.cancelAllNotifications()

        // Process any notification or deep-link from the initial intent before rendering the UI.
        val initialNavState = processInitialIntent(intent, isAppAlreadyRunning)

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
        handleNotificationIntent(intent)
    }

    /**
     * Called when the activity is becoming visible to the user.
     * We clear notifications here as well to handle cases where the user returns to the app without a new intent.
     */
    override fun onStart() {
        super.onStart()
        notificationHelper.cancelAllNotifications()
    }

    /**
     * Processes the initial intent when the activity is created to check for navigation data from a notification.
     *
     * @param intent The intent that started the activity.
     * @param isAppRunning A flag indicating if this is a cold start or a warm start.
     * @return A [NotificationNavigationState] object if navigation data is found, otherwise null.
     */
    private fun processInitialIntent(
        intent: Intent?,
        isAppRunning: Boolean
    ): NotificationNavigationState? {
        Log.d(TAG, "Processing initial intent. isAppRunning: $isAppRunning")
        return processNotificationIntent(intent)?.let { state ->
            val initialState = state.copy(
                skipSplash = isAppRunning,
                isInitialDestination = true
            )
            Log.d(TAG, "Initial navigation state extracted: $initialState")
            clearIntentExtras(intent)
            initialState
        }
    }

    /**
     * Handles a new intent by extracting navigation data and queuing it in the [MainActivityViewModel].
     * The navigation is queued to be consumed by the UI layer safely.
     *
     * @param intent The new intent received.
     */
    private fun handleNotificationIntent(intent: Intent?) {
        Log.d(TAG, "Handling a new notification intent.")
        processNotificationIntent(intent)?.let { state ->
            Log.d(TAG, "Queuing pending navigation state: $state")
            activityViewModel.setPendingNavigation(
                state.navigateTo,
                state.userId,
                state.username,
                skipSplash = true
            )
            clearIntentExtras(intent)
        } ?: Log.d(TAG, "No navigation state could be extracted from the new intent.")
    }

    /**
     * Clears the navigation-related extras from the given intent to prevent them from being processed again
     * on configuration changes or activity recreation.
     *
     * @param intent The intent from which to clear extras.
     */
    private fun clearIntentExtras(intent: Intent?) {
        intent?.removeExtra("navigateTo")
        intent?.removeExtra("userId")
        intent?.removeExtra("username")
    }
}
