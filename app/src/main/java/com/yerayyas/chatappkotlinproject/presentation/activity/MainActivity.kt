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
 * MainActivity serves as the single entry point to the Chat App.
 * It configures edge-to-edge rendering, handles soft input adjustments,
 * clears notifications on startup, and delegates deep-link/intents
 * to the appropriate use cases for navigation.
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
        /** Tracks whether the application has been initialized to distinguish cold start vs warm start. */
        private var isAppInitialized = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isAppAlreadyRunning = isAppInitialized || savedInstanceState != null
        Log.d(TAG, "onCreate: isAppAlreadyRunning=$isAppAlreadyRunning, isAppInitialized=$isAppInitialized, savedInstanceState=${savedInstanceState != null}")

        // Mark app as initialized
        isAppInitialized = true

        // Dismiss all notifications to start fresh
        notificationHelper.cancelAllNotifications()

        // Process any notification or deep-link intent before rendering UI
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
     * Parses the incoming intent for notification navigation state.
     * Clears intent extras once processed.
     *
     * @param intent The incoming intent, which may contain notification data.
     * @param isAppRunning Flag indicating whether the app was already initialized.
     * @return A [NotificationNavigationState] if the intent maps to a notification, or null otherwise.
     */
    private fun processInitialIntent(
        intent: Intent?,
        isAppRunning: Boolean
    ): NotificationNavigationState? {
        Log.d(TAG, "processInitialIntent: extras=${intent?.extras}, isAppRunning=$isAppRunning")

        return processNotificationIntent(intent)?.let { state ->
            val initialState = state.copy(
                skipSplash = isAppRunning,
                isInitialDestination = true
            )
            Log.d(TAG, "processInitialIntent: initialNavState=$initialState")
            clearIntentExtras(intent)
            initialState
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent: app is already running")
        handleNotificationIntent(intent)
    }

    /**
     * Handles any new intents while the app is running, passing navigation state
     * to the ActivityViewModel for deferred navigation.
     *
     * @param intent The new intent received.
     */
    private fun handleNotificationIntent(intent: Intent?) {
        Log.d(TAG, "handleNotificationIntent: extras=${intent?.extras}")

        processNotificationIntent(intent)?.let { state ->
            Log.d(TAG, "handleNotificationIntent: navState=$state, skipSplash=${true}")
            activityViewModel.setPendingNavigation(
                state.navigateTo,
                state.userId,
                state.username,
                skipSplash = true
            )
            clearIntentExtras(intent)
        } ?: Log.d(TAG, "handleNotificationIntent: no navigation state extracted")
    }

    /**
     * Removes processed extras to prevent re-processing the same intent.
     */
    private fun clearIntentExtras(intent: Intent?) {
        intent?.removeExtra("navigateTo")
        intent?.removeExtra("userId")
        intent?.removeExtra("username")
    }

    override fun onStart() {
        super.onStart()
        // Ensure notifications are cleared when returning to foreground
        notificationHelper.cancelAllNotifications()
    }
}
