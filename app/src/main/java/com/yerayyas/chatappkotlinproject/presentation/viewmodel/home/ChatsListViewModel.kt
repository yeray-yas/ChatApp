package com.yerayyas.chatappkotlinproject.presentation.viewmodel.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.ChatListItem
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.individual.GetUserChatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChatsListViewModel"

/**
 * Represents the different states for the chat list screen UI.
 */
sealed interface ChatsUiState {
    /** The UI is in a loading state, typically on first load. */
    object Loading : ChatsUiState

    /** The UI has successfully loaded the chat data. */
    data class Success(val chats: List<ChatListItem>) : ChatsUiState

    /** An error occurred while loading data. */
    data class Error(val message: String) : ChatsUiState
}

/**
 * Manages the UI state for the user's list of individual chats.
 *
 * This ViewModel adheres to Clean Architecture principles by orchestrating data flow
 * from the domain layer (via UseCases) to the UI. It is responsible for fetching the
 * list of chats and calculating the total number of unread messages, exposing them
 * as reactive [StateFlow]s for the UI to observe.
 *
 * @property getUserChatsUseCase The use case responsible for fetching a real-time stream
 * of the user's chat list, encapsulating all business logic for data aggregation.
 */
@HiltViewModel
class ChatsListViewModel @Inject constructor(
    private val getUserChatsUseCase: GetUserChatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    /** Exposes the overall UI state for the chat list screen (Loading, Success, Error). */
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    /** Exposes the total count of unread messages across all individual chats. */
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    init {
        // Renamed for clarity to match its single responsibility.
        loadUserChatsAndUnreadCount()
    }

    /**
     * Fetches the user's chat list and updates both the chat list and the total
     * unread count in a single, efficient operation.
     *
     * It subscribes to a flow from [GetUserChatsUseCase] which provides a real-time
     * list of [ChatListItem] objects. For each emitted list, it updates the UI state
     * and simultaneously calculates the total unread message count by summing up
     * the `unreadCount` of each chat item.
     */
    fun loadUserChatsAndUnreadCount() {
        viewModelScope.launch {
            getUserChatsUseCase()
                .onStart {
                    Log.d(TAG, "Starting to collect user chats flow.")
                    _uiState.value = ChatsUiState.Loading
                }
                .catch { e ->
                    Log.e(TAG, "Error collecting user chats flow", e)
                    _uiState.value = ChatsUiState.Error("Failed to load chats: ${e.message}")
                }
                .collect { chatList ->
                    Log.d(TAG, "Received updated chat list with ${chatList.size} items.")
                    // The use case already provides the sorted list.
                    _uiState.value = ChatsUiState.Success(chatList)

                    // Calculate the total unread count from the already processed list.
                    // This is highly efficient as it avoids a second database listener.
                    val totalCount = chatList.sumOf { it.unreadCount }
                    _totalUnreadCount.value = totalCount
                    Log.d(TAG, "Total unread individual messages count updated to: $totalCount")
                }
        }
    }
}
