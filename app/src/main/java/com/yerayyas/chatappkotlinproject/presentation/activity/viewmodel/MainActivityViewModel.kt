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
 * ViewModel for MainActivity that manages one-shot navigation events
 * triggered by incoming notification intents.
 *
 * Uses a [MutableStateFlow] to emit [NotificationNavigationState] when valid
 * navigation parameters are provided, and resets the state after handling.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    private val _pendingNavigation = MutableStateFlow<NotificationNavigationState?>(null)
    /**
     * Emits navigation state for pending notification-driven navigation.
     * Observers should consume and clear this state after handling.
     */
    val pendingNavigation: StateFlow<NotificationNavigationState?> = _pendingNavigation.asStateFlow()

    /**
     * Queues a navigation event based on notification parameters.
     * Only valid when [navigateTo] equals "chat" and both [userId] and [username]
     * are non-null and non-empty.
     *
     * @param navigateTo Expected destination identifier (e.g., "chat").
     * @param userId ID of the chat recipient.
     * @param username Display name of the chat recipient.
     * @param skipSplash If true, bypasses the splash screen during navigation.
     */
    fun setPendingNavigation(
        navigateTo: String?,
        userId: String?,
        username: String?,
        skipSplash: Boolean = false
    ) {
        Log.d(TAG, "setPendingNavigation: navigateTo=$navigateTo, userId=$userId, skipSplash=$skipSplash")

        if (navigateTo == "chat" && !userId.isNullOrBlank() && !username.isNullOrBlank()) {
            val state = NotificationNavigationState(
                navigateTo = navigateTo,
                userId = userId,
                username = username,
                eventId = System.currentTimeMillis(),
                skipSplash = skipSplash
            )
            Log.d(TAG, "setPendingNavigation: Emitting state $state")
            // Emit on main scope though no suspend functions are used
            viewModelScope.launch { _pendingNavigation.value = state }
        } else {
            Log.d(TAG, "setPendingNavigation: Invalid parameters, ignoring event")
        }
    }

    /**
     * Queues a group navigation event based on notification parameters.
     *
     * @param groupId ID of the group chat.
     * @param groupName Display name of the group chat.
     * @param skipSplash If true, bypasses the splash screen during navigation.
     */
    fun setPendingGroupNavigation(
        groupId: String,
        groupName: String,
        skipSplash: Boolean = false
    ) {
        Log.d(
            TAG,
            "setPendingGroupNavigation: groupId=$groupId, groupName=$groupName, skipSplash=$skipSplash"
        )

        if (groupId.isNotBlank() && groupName.isNotBlank()) {
            val state = NotificationNavigationState(
                navigateTo = "group_chat",
                userId = "", // Not needed for group navigation
                username = "", // Not needed for group navigation
                eventId = System.currentTimeMillis(),
                skipSplash = skipSplash,
                groupId = groupId,
                groupName = groupName
            )
            Log.d(TAG, "setPendingGroupNavigation: Emitting state $state")
            viewModelScope.launch { _pendingNavigation.value = state }
        } else {
            Log.d(TAG, "setPendingGroupNavigation: Invalid parameters, ignoring event")
        }
    }

    /**
     * Clears the current pending navigation state.
     * Should be called after the navigation event is consumed.
     */
    fun clearPendingNavigation() {
        Log.d(TAG, "clearPendingNavigation: Clearing state")
        _pendingNavigation.value = null
    }
}
