package com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for emitting one-shot navigation events
 * triggered by incoming notifications.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    // SharedFlow for one-time navigation emissions (no replay)
    private val _pendingNavigation = MutableSharedFlow<NotificationNavigationState>(replay = 0)
    val pendingNavigation: SharedFlow<NotificationNavigationState> = _pendingNavigation.asSharedFlow()

    /**
     * Emits a navigation event when a valid "chat" notification arrives.
     * Only non-null userId and username are accepted.
     * @param navigateTo expected "chat" to trigger navigation.
     * @param userId identifier of the chat recipient.
     * @param username display name of the chat recipient.
     */
    fun setPendingNavigation(
        navigateTo: String?,
        userId: String?,
        username: String?
    ) {
        if (navigateTo == "chat" && !userId.isNullOrEmpty() && !username.isNullOrEmpty()) {
            viewModelScope.launch {
                _pendingNavigation.emit(
                    NotificationNavigationState(
                        navigateTo = navigateTo,
                        userId = userId,
                        username = username,
                        eventId = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
