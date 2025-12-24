package com.yerayyas.chatappkotlinproject.presentation.viewmodel.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.usecases.auth.SignOutUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.GetUnreadGroupMessagesCountUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.individual.GetUserChatsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelAllNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.FetchUsersUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.LoadUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val TAG = "HomeViewModel"

/**
 * ViewModel responsible for the state and business logic of the Home Screen.
 *
 * This ViewModel acts as the central hub for the main dashboard, orchestrating data flows
 * from various UseCases to present a cohesive UI. It follows the MVVM pattern and uses
 * Kotlin Flows for reactive state management.
 *
 * Key Responsibilities:
 * - **User List Management:** Fetches, filters (by search query), and sorts users based on
 * availability and recent activity.
 * - **Profile Management:** Loads and formats the current user's profile information.
 * - **Notification Management:** Clears system notifications upon entry and tracks unread message counts.
 * - **Session Management:** Handles the user sign-out process.
 *
 * Sorting Logic:
 * The user list is sorted dynamically:
 * 1. **Online Status:** Users who are "online" appear first.
 * 2. **Recency:** Users are then sorted by their last interaction (chat timestamp) or last seen time.
 *
 * @property loadUserProfileUseCase Fetches the current user's details.
 * @property fetchUsersUseCase Provides a stream of all available users.
 * @property signOutUseCase Handles the authentication sign-out process.
 * @property getUnreadGroupMessagesCountUseCase Observes the total count of unread messages in groups.
 * @property cancelAllNotificationsUseCase Clears active push notifications from the system tray.
 * @property getUserChatsUseCase Provides recent chat activity for sorting purposes.
 * @property getCurrentUserIdUseCase Helper to retrieve the current user's ID for filtering.
 */
@HiltViewModel
@OptIn(FlowPreview::class)
class HomeViewModel @Inject constructor(
    private val loadUserProfileUseCase: LoadUserProfileUseCase,
    private val fetchUsersUseCase: FetchUsersUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getUnreadGroupMessagesCountUseCase: GetUnreadGroupMessagesCountUseCase,
    private val cancelAllNotificationsUseCase: CancelAllNotificationsUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) : ViewModel() {

    // Backing property for UI state
    private val _uiState = MutableStateFlow(HomeUiState())

    /**
     * Exposes the current state of the Home UI (loading, username, errors, badges).
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Backing property for the list of users
    private val _users = MutableStateFlow<List<User>>(emptyList())

    /**
     * Exposes the filtered and sorted list of users to be displayed in the UI.
     * This list is reactive and updates automatically when users go online/offline
     * or when the search query changes.
     */
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Backing property for the search query
    private val _searchQuery = MutableStateFlow("")

    /**
     * Exposes the current text in the search bar.
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadCurrentUserProfile()
        clearAllNotificationsOnStart()
        loadUnreadGroupMessagesCount()
        observeUsersAndChats()
    }

    /**
     * Loads the current user's profile to display their name in the top bar.
     * Sets the [uiState] loading state and formats the username (capitalization).
     */
    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = loadUserProfileUseCase()
            result.onSuccess { user ->
                // Format username: capitalize first letter if it's lowercase
                val username = user?.username?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                } ?: "User"
                _uiState.value = _uiState.value.copy(isLoading = false, username = username)
            }.onFailure { e ->
                Log.e(TAG, "Failed to load user profile", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load profile")
            }
        }
    }

    /**
     * Sets up the reactive pipeline to observe users, chats, and search queries.
     *
     * This complex flow performs the following operations:
     * 1. **Combines** the search query, the list of all users, and recent chat history.
     * 2. **Debounces** the search query to avoid excessive processing during typing.
     * 3. **Filters** the user list based on the query (excluding the current user).
     * 4. **Sorts** the result:
     * - Primary key: Online status (Online users first).
     * - Secondary key: Recency (Uses the latest chat timestamp if available, otherwise Last Seen).
     *
     * Result is emitted to the [_users] state flow.
     */
    private fun observeUsersAndChats() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after the user stops typing
                .distinctUntilChanged()
                .combine(fetchUsersUseCase()) { query, allUsers ->
                    Pair(query, allUsers)
                }
                .combine(getUserChatsUseCase()) { (query, allUsers), recentChats ->
                    Triple(query, allUsers, recentChats)
                }
                .catch { e ->
                    Log.e(TAG, "Error combining user and chat flows", e)
                    _uiState.value = _uiState.value.copy(error = "Failed to load user list")
                }
                .collect { (query, allUsers, recentChats) ->
                    val currentUserId = getCurrentUserIdUseCase() ?: ""
                    val lowercasedQuery = query.trim().lowercase()

                    // Create a map for quick lookup of chat timestamps by user ID
                    val chatActivityMap = recentChats.associate { it.otherUserId to it.timestamp }

                    // Filter out current user and apply search query
                    val filteredUsers = allUsers.filter { user ->
                        user.id != currentUserId &&
                                (lowercasedQuery.isEmpty() || user.username.lowercase().contains(lowercasedQuery))
                    }

                    // Sort logic: Online first, then by most recent activity
                    val sortedUsers = filteredUsers.sortedWith(
                        compareByDescending<User> { it.status.lowercase() == "online" }
                            .thenByDescending { user ->
                                // Use chat timestamp if available, otherwise fallback to lastSeen
                                chatActivityMap[user.id] ?: user.lastSeen
                            }
                    )

                    _users.value = sortedUsers
                }
        }
    }

    /**
     * Updates the search query state.
     * Called by the UI when the text in the search bar changes.
     *
     * @param query The new search text.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Observes the global count of unread group messages.
     * Updates the [uiState] to reflect the badge count in the UI.
     */
    private fun loadUnreadGroupMessagesCount() {
        viewModelScope.launch {
            getUnreadGroupMessagesCountUseCase()
                .catch { exception ->
                    Log.e(TAG, "Error collecting GLOBAL unread group messages count", exception)
                }
                .collect { count ->
                    _uiState.value = _uiState.value.copy(unreadGroupMessagesCount = count)
                }
        }
    }

    /**
     * Signs out the current user and triggers a callback upon success.
     *
     * @param onSignedOut Callback function to navigate the user to Login after successful sign-out.
     */
    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                signOutUseCase()
                onSignedOut()
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out", e)
                _uiState.value = _uiState.value.copy(error = "Sign out failed")
            }
        }
    }

    /**
     * Clears all system notifications when the Home screen is initialized.
     * This ensures the user doesn't see stale notifications for messages they are about to view.
     */
    private fun clearAllNotificationsOnStart() {
        viewModelScope.launch {
            cancelAllNotificationsUseCase()
        }
    }
}

/**
 * Represents the UI state of the Home Screen.
 *
 * @property isLoading Indicates if the profile or initial data is being loaded.
 * @property username The display name of the current user.
 * @property error A localized error message to display if something fails (null if no error).
 * @property unreadGroupMessagesCount The total number of unread messages across all groups (for badge display).
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val username: String = "Loading...",
    val error: String? = null,
    val unreadGroupMessagesCount: Int = 0
)
