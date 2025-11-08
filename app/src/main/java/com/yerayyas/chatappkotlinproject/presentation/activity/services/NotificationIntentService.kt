package com.yerayyas.chatappkotlinproject.presentation.activity.services

import android.content.Intent
import android.util.Log
import com.yerayyas.chatappkotlinproject.domain.usecases.ProcessNotificationIntentUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationIntentService"

/**
 * Service responsible for processing notification intents and managing navigation state.
 */
@Singleton
class NotificationIntentService @Inject constructor(
    private val processNotificationIntent: ProcessNotificationIntentUseCase
) {

    /**
     * Processes the initial intent when activity is created.
     *
     * @param intent The intent that started the activity
     * @param isAppAlreadyRunning Flag indicating if this is a cold or warm start
     * @return NotificationNavigationState if navigation data is found, null otherwise
     */
    fun processInitialIntent(
        intent: Intent?,
        isAppAlreadyRunning: Boolean
    ): NotificationNavigationState? {
        Log.d(TAG, "Processing initial intent. isAppAlreadyRunning: $isAppAlreadyRunning")
        return processNotificationIntent(intent)?.let { state ->
            val initialState = state.copy(
                skipSplash = isAppAlreadyRunning,
                isInitialDestination = true
            )

            val destinationType = if (initialState.isGroupChat) "group" else "individual"
            val destinationName = initialState.destinationName
            Log.d(TAG, "Initial $destinationType navigation state extracted for: $destinationName")

            clearIntentExtras(intent)
            initialState
        }
    }

    /**
     * Handles a new intent by extracting navigation data and queuing it in the ViewModel.
     *
     * @param intent The new intent received
     * @param activityViewModel The MainActivity ViewModel to queue navigation
     */
    fun handleNotificationIntent(intent: Intent?, activityViewModel: MainActivityViewModel) {
        Log.d(TAG, "Handling a new notification intent.")
        processNotificationIntent(intent)?.let { state ->
            Log.d(TAG, "Queuing pending navigation state: $state")

            if (state.isGroupChat) {
                // Group chat navigation
                activityViewModel.setPendingGroupNavigation(
                    groupId = state.groupId!!,
                    groupName = state.groupName!!,
                    skipSplash = true
                )
            } else {
                // Individual chat navigation
                activityViewModel.setPendingNavigation(
                    state.navigateTo,
                    state.userId,
                    state.username,
                    skipSplash = true
                )
            }

            clearIntentExtras(intent)
        } ?: Log.d(TAG, "No navigation state could be extracted from the new intent.")
    }

    /**
     * Clears navigation-related extras from the intent to prevent reprocessing.
     *
     * @param intent The intent from which to clear extras
     */
    private fun clearIntentExtras(intent: Intent?) {
        intent?.apply {
            // Individual chat extras
            removeExtra("navigateTo")
            removeExtra("userId")
            removeExtra("username")

            // Group chat extras
            removeExtra("groupId")
            removeExtra("groupName")
            removeExtra("senderId")
            removeExtra("senderName")
        }
    }
}
