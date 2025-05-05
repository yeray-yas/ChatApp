package com.yerayyas.chatappkotlinproject.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.ProcessNotificationIntentUseCase
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.navigation.AppContainer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MainActivity"

/**
 * MainActivity is the single entry point for the Chat App.
 *
 * - Configures edge-to-edge rendering.
 * - Ensures the window resizes when the soft keyboard appears (SOFT_INPUT_ADJUST_RESIZE).
 * - Clears all notifications on start.
 * - Delegates deep link and notification intent processing to UseCases.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * ViewModel that holds global navigation state for handling deep links.
     */
    private val activityViewModel: MainActivityViewModel by viewModels()

    /**
     * UseCase to process incoming Intents and extract navigation instructions.
     */
    @Inject
    lateinit var processNotificationIntent: ProcessNotificationIntentUseCase

    /**
     * UseCase to handle navigation when a notification is tapped.
     */
    @Inject
    lateinit var handleNotificationNavigation: HandleNotificationNavigationUseCase

    @Inject
    lateinit var handleDefaultNavigation: HandleDefaultNavigationUseCase

    /**
     * Called when the activity is created.
     *
     * - Enables edge-to-edge support.
     * - Sets soft input mode to ADJUST_RESIZE so the window will shrink and avoid overlapping content when the keyboard appears.
     * - Cancels any active notifications.
     * - Sets the Compose content and triggers initial intent handling.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationManagerCompat.from(this).apply {
            cancelAll()
        }

        setContent {
            ProvideWindowInsets {
                AppContainer(
                    activityViewModel = activityViewModel,
                    handleNotificationNavigation = handleNotificationNavigation,
                    handleDefaultNavigation = handleDefaultNavigation
                )
            }
        }

        Log.d(TAG, "onCreate: processing initial Intent")
        handleIntent(intent)
    }

    /**
     * Handles new Intents delivered to this Activity, such as notification taps.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Delegates Intent processing to the use case, then triggers navigation.
     * Clears notification extras once processed.
     */
    private fun handleIntent(intent: Intent?) {
        processNotificationIntent(intent)?.let { state ->
            activityViewModel.setPendingNavigation(
                state.navigateTo,
                state.userId,
                state.username
            )
            clearIntentExtras(intent)
        }
    }

    /**
     * Removes notification-specific extras to prevent re-processing.
     */
    private fun clearIntentExtras(intent: Intent?) {
        intent?.apply {
            removeExtra("navigateTo")
            removeExtra("userId")
            removeExtra("username")
        }
    }

    override fun onStart() {
        super.onStart()
        NotificationManagerCompat.from(this).cancelAll()
    }
}
