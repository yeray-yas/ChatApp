package com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.domain.usecases.GetStartDestinationUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivityVM"

/**
 * ViewModel for MainActivity responsible for orchestrating the app's entry point and navigation events.
 *
 * This ViewModel acts as the bridge between the Android Framework (Intents/Notifications) and the
 * Compose UI layer. It manages two critical aspects of the app startup:
 * 1. **Start Destination Resolution:** Deciding whether to show Splash, Login, or Home based on auth state.
 * 2. **Deep Link Handling:** Processing navigation events triggered by push notifications.
 *
 * Architecture Pattern: MVVM with Reactive State Management.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val getStartDestinationUseCase: GetStartDestinationUseCase
) : ViewModel() {

    // --- State Management ---

    /**
     * Internal mutable state for notification-driven navigation.
     * Stores the parsed navigation intent waiting to be consumed by the UI.
     */
    private val _pendingNavigation = MutableStateFlow<NotificationNavigationState?>(null)

    /**
     * Public stream of pending navigation events.
     * Observed by the [NavigationWrapper] to trigger deep-link navigation.
     *
     * Contract:
     * - Emits `null` when idle.
     * - Emits [NotificationNavigationState] when a notification is tapped.
     * - Must be cleared via [clearPendingNavigation] after consumption.
     */
    val pendingNavigation: StateFlow<NotificationNavigationState?> =
        _pendingNavigation.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NotificationNavigationState?>(null)

    /**
     * Secondary navigation stream for generic runtime events.
     * (Note: distinct from [pendingNavigation] which is specialized for Deep Links handling).
     */
    val navigationEvent = _navigationEvent.asStateFlow()

    private val _startDestinationState = MutableStateFlow<StartNavigationState>(StartNavigationState.Loading)

    /**
     * Represents the current state of the app's entry point determination.
     * Used by the UI to toggle between the Splash Screen and the main NavHost.
     */
    val startDestinationState = _startDestinationState.asStateFlow()

    // --- Actions ---

    fun onNavigationHandled() {
        _navigationEvent.value = null
    }

    /**
     * Resolves the initial screen of the application.
     *
     * Delegates to [GetStartDestinationUseCase] to apply business rules (Auth status,
     * Deep Link priority, Splash delay) and updates [startDestinationState].
     *
     * @param skipSplash If true, indicates a warm start where the splash screen should be skipped.
     * @param initialNavState Optional navigation state if the app was launched via notification.
     */
    fun resolveStartDestination(skipSplash: Boolean, initialNavState: NotificationNavigationState?) {
        viewModelScope.launch {
            val destination = getStartDestinationUseCase(initialNavState, skipSplash)
            _startDestinationState.value = StartNavigationState.Ready(destination)
        }
    }

    // --- Notification Handling Logic ---

    /**
     * Queues an individual chat navigation event from a raw service payload.
     *
     * Called by the NotificationService when a user taps a "New Message" notification.
     * Validates parameters before emitting state to avoid broken navigation.
     *
     * @param navigateTo Must be [NotificationNavigationState.ROUTE_INDIVIDUAL_CHAT].
     * @param userId The recipient's ID.
     * @param username The recipient's display name.
     * @param skipSplash Whether to bypass the splash screen.
     */
    fun setPendingNavigation(
        navigateTo: String?,
        userId: String?,
        username: String?,
        skipSplash: Boolean = false
    ) {
        Log.d(TAG, "Attempting to set individual chat navigation: $username ($userId)")

        try {
            if (!isValidIndividualChatNavigation(navigateTo, userId, username)) {
                Log.w(TAG, "Invalid individual chat params - ignoring.")
                return
            }

            val navigationState = NotificationNavigationState.forIndividualChat(
                userId = userId!!,
                username = username!!,
                skipSplash = skipSplash
            )

            emitNavigationState(navigationState)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting individual chat navigation", e)
        }
    }

    /**
     * Queues a group chat navigation event from a raw service payload.
     *
     * Called by the NotificationService when a user taps a "Group Message" notification.
     *
     * @param groupId The group's ID.
     * @param groupName The group's display name.
     * @param skipSplash Whether to bypass the splash screen.
     */
    fun setPendingGroupNavigation(
        groupId: String,
        groupName: String,
        skipSplash: Boolean = false
    ) {
        Log.d(TAG, "Attempting to set group navigation: $groupName ($groupId)")

        try {
            if (!isValidGroupChatNavigation(groupId, groupName)) {
                Log.w(TAG, "Invalid group chat params - ignoring.")
                return
            }

            // Sender info is not strictly required for opening the chat, so we use empty strings
            // or specific logic if needed by the destination screen.
            val navigationState = NotificationNavigationState.forGroupChat(
                groupId = groupId,
                groupName = groupName,
                senderId = "",
                senderName = "",
                skipSplash = skipSplash
            )

            emitNavigationState(navigationState)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting group chat navigation", e)
        }
    }

    /**
     * Resets the pending navigation state to null.
     * Must be called by the UI (NavigationWrapper) immediately after processing the event.
     */
    fun clearPendingNavigation() {
        _pendingNavigation.value = null
    }

    // --- Private Helpers ---

    private fun isValidIndividualChatNavigation(
        navigateTo: String?,
        userId: String?,
        username: String?
    ): Boolean {
        return navigateTo == NotificationNavigationState.ROUTE_INDIVIDUAL_CHAT &&
                !userId.isNullOrBlank() &&
                !username.isNullOrBlank()
    }

    private fun isValidGroupChatNavigation(
        groupId: String,
        groupName: String
    ): Boolean {
        return groupId.isNotBlank() && groupName.isNotBlank()
    }

    private fun emitNavigationState(navigationState: NotificationNavigationState) {
        _pendingNavigation.value = navigationState
        Log.d(TAG, "Navigation state emitted: ${navigationState.destinationName}")
    }
}

/**
 * Sealed interface representing the state of the initial navigation resolution.
 */
sealed interface StartNavigationState {
    /**
     * The app is calculating the destination (e.g. checking Firebase Auth).
     * The UI should show the Splash Screen.
     */
    data object Loading : StartNavigationState

    /**
     * The destination is resolved.
     * The UI should mount the NavHost with the provided [startDestination].
     */
    data class Ready(val startDestination: String) : StartNavigationState
}
