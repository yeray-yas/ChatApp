package com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivityVM"

/**
 * ViewModel for MainActivity responsible for managing notification-driven navigation events.
 *
 * This ViewModel follows the MVVM pattern and serves as a bridge between the service layer
 * and the UI layer for handling deep-link navigation from notifications. It manages one-shot
 * navigation events and provides a reactive interface for the Compose UI to observe and consume.
 *
 * Key responsibilities:
 * - Managing pending navigation states from notification intents
 * - Providing reactive navigation state through StateFlow
 * - Supporting both individual and group chat navigation
 * - Ensuring navigation events are consumed only once
 * - Maintaining navigation state during configuration changes
 * - Providing comprehensive logging for debugging navigation flows
 *
 * Architecture Pattern: MVVM with Reactive State Management
 * - Uses StateFlow for reactive state management
 * - Follows single source of truth principle
 * - Provides lifecycle-aware navigation state
 * - Supports both cold and warm app starts
 * - Ensures navigation events are not lost during configuration changes
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    /**
     * Private mutable state flow for internal state management.
     * Only this ViewModel can modify the navigation state.
     */
    private val _pendingNavigation = MutableStateFlow<NotificationNavigationState?>(null)

    /**
     * Public read-only state flow for UI observation.
     *
     * This StateFlow emits [NotificationNavigationState] instances when valid navigation
     * parameters are provided through notification intents. The UI layer should observe
     * this flow and handle navigation accordingly.
     *
     * Key characteristics:
     * - Emits null when no navigation is pending
     * - Emits navigation state when valid notification data is received
     * - Maintains state during configuration changes
     * - Should be consumed and cleared after handling navigation
     *
     * @return StateFlow containing the current pending navigation state or null
     */
    val pendingNavigation: StateFlow<NotificationNavigationState?> = _pendingNavigation.asStateFlow()

    /**
     * Queues an individual chat navigation event based on notification parameters.
     *
     * This method validates the provided parameters and creates a navigation state
     * for individual chat navigation. It includes comprehensive validation and
     * error handling to ensure only valid navigation requests are processed.
     *
     * Validation rules:
     * - navigateTo must equal "chat"
     * - userId must be non-null and non-blank
     * - username must be non-null and non-blank
     *
     * @param navigateTo Expected destination identifier (must be "chat" for individual chats)
     * @param userId Unique identifier of the chat recipient (must be non-blank)
     * @param username Display name of the chat recipient (must be non-blank)
     * @param skipSplash If true, bypasses the splash screen during navigation
     */
    fun setPendingNavigation(
        navigateTo: String?,
        userId: String?,
        username: String?,
        skipSplash: Boolean = false
    ) {
        Log.d(
            TAG,
            "Attempting to set individual chat navigation - navigateTo: $navigateTo, userId: $userId, skipSplash: $skipSplash"
        )

        try {
            // Validate required parameters
            if (!isValidIndividualChatNavigation(navigateTo, userId, username)) {
                Log.w(TAG, "Invalid individual chat navigation parameters - ignoring request")
                return
            }

            // Create and emit navigation state
            val navigationState = createIndividualChatNavigationState(
                userId = userId!!,
                username = username!!,
                skipSplash = skipSplash
            )

            emitNavigationState(navigationState)
            Log.d(TAG, "Individual chat navigation queued successfully for user: $username")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting individual chat navigation", e)
        }
    }

    /**
     * Queues a group chat navigation event based on notification parameters.
     *
     * This method validates the provided parameters and creates a navigation state
     * for group chat navigation. It includes comprehensive validation and error
     * handling to ensure only valid navigation requests are processed.
     *
     * Validation rules:
     * - groupId must be non-blank
     * - groupName must be non-blank
     *
     * @param groupId Unique identifier of the group chat (must be non-blank)
     * @param groupName Display name of the group chat (must be non-blank)
     * @param skipSplash If true, bypasses the splash screen during navigation
     */
    fun setPendingGroupNavigation(
        groupId: String,
        groupName: String,
        skipSplash: Boolean = false
    ) {
        Log.d(
            TAG,
            "Attempting to set group chat navigation - groupId: $groupId, groupName: $groupName, skipSplash: $skipSplash"
        )

        try {
            // Validate required parameters
            if (!isValidGroupChatNavigation(groupId, groupName)) {
                Log.w(TAG, "Invalid group chat navigation parameters - ignoring request")
                return
            }

            // Create and emit navigation state
            val navigationState = createGroupChatNavigationState(
                groupId = groupId,
                groupName = groupName,
                skipSplash = skipSplash
            )

            emitNavigationState(navigationState)
            Log.d(TAG, "Group chat navigation queued successfully for group: $groupName")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting group chat navigation", e)
        }
    }

    /**
     * Clears the current pending navigation state.
     *
     * This method should be called after the navigation event has been consumed
     * by the UI layer to prevent duplicate navigation attempts. It safely resets
     * the navigation state to null.
     */
    fun clearPendingNavigation() {
        try {
            Log.d(TAG, "Clearing pending navigation state")
            _pendingNavigation.value = null
            Log.d(TAG, "Pending navigation state cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing pending navigation state", e)
        }
    }

    /**
     * Validates parameters for individual chat navigation.
     *
     * @param navigateTo The navigation destination (should be "chat")
     * @param userId The user ID to validate
     * @param username The username to validate
     * @return true if all parameters are valid, false otherwise
     */
    private fun isValidIndividualChatNavigation(
        navigateTo: String?,
        userId: String?,
        username: String?
    ): Boolean {
        return navigateTo == NotificationNavigationState.ROUTE_INDIVIDUAL_CHAT &&
                !userId.isNullOrBlank() &&
                !username.isNullOrBlank()
    }

    /**
     * Validates parameters for group chat navigation.
     *
     * @param groupId The group ID to validate
     * @param groupName The group name to validate
     * @return true if all parameters are valid, false otherwise
     */
    private fun isValidGroupChatNavigation(
        groupId: String,
        groupName: String
    ): Boolean {
        return groupId.isNotBlank() && groupName.isNotBlank()
    }

    /**
     * Creates a navigation state for individual chat navigation using factory method.
     *
     * @param userId The user ID
     * @param username The username
     * @param skipSplash Whether to skip the splash screen
     * @return NotificationNavigationState configured for individual chat
     */
    private fun createIndividualChatNavigationState(
        userId: String,
        username: String,
        skipSplash: Boolean
    ): NotificationNavigationState {
        return NotificationNavigationState.forIndividualChat(
            userId = userId,
            username = username,
            skipSplash = skipSplash
        )
    }

    /**
     * Creates a navigation state for group chat navigation using factory method.
     *
     * @param groupId The group ID
     * @param groupName The group name
     * @param skipSplash Whether to skip the splash screen
     * @return NotificationNavigationState configured for group chat
     */
    private fun createGroupChatNavigationState(
        groupId: String,
        groupName: String,
        skipSplash: Boolean
    ): NotificationNavigationState {
        return NotificationNavigationState.forGroupChat(
            groupId = groupId,
            groupName = groupName,
            senderId = "", // Not needed for this navigation context
            senderName = "", // Not needed for this navigation context
            skipSplash = skipSplash
        )
    }

    /**
     * Safely emits a navigation state to the StateFlow.
     *
     * @param navigationState The navigation state to emit
     */
    private fun emitNavigationState(navigationState: NotificationNavigationState) {
        viewModelScope.launch {
            try {
                _pendingNavigation.value = navigationState
                Log.d(
                    TAG,
                    "Navigation state emitted successfully - EventId: ${navigationState.eventId}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error emitting navigation state", e)
            }
        }
    }
}
