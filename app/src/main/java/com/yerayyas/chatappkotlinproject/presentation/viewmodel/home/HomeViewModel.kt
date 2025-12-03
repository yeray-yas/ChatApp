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
import com.yerayyas.chatappkotlinproject.domain.usecases.user.ManageUserPresenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val TAG = "HomeViewModel"

@HiltViewModel
@OptIn(FlowPreview::class)
class HomeViewModel @Inject constructor(
    private val loadUserProfileUseCase: LoadUserProfileUseCase,
    private val fetchUsersUseCase: FetchUsersUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val manageUserPresenceUseCase: ManageUserPresenceUseCase,
    private val getUnreadGroupMessagesCountUseCase: GetUnreadGroupMessagesCountUseCase,
    private val cancelAllNotificationsUseCase: CancelAllNotificationsUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadCurrentUserProfile()
        setupPresenceManagement()
        clearAllNotificationsOnStart()
        loadUnreadGroupMessagesCount()
        observeUsersAndChats()
    }

    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = loadUserProfileUseCase()
            result.onSuccess { user ->
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
    private fun observeUsersAndChats() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
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

                    val chatActivityMap = recentChats.associate { it.otherUserId to it.timestamp }

                    val filteredUsers = allUsers.filter { user ->
                        user.id != currentUserId &&
                                (lowercasedQuery.isEmpty() || user.username.lowercase().contains(lowercasedQuery))
                    }

                    val sortedUsers = filteredUsers.sortedWith(
                        compareByDescending<User> { it.status.lowercase() == "online" }
                            .thenByDescending { user ->
                                chatActivityMap[user.id] ?: user.lastSeen
                            }
                    )

                    _users.value = sortedUsers
                }
        }
    }


    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

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

    private fun setupPresenceManagement() {
        viewModelScope.launch {
            manageUserPresenceUseCase.startPresenceUpdates()
        }
    }

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

    private fun clearAllNotificationsOnStart() {
        viewModelScope.launch {
            cancelAllNotificationsUseCase()
        }
    }

    override fun onCleared() {
        super.onCleared()
        manageUserPresenceUseCase.stopPresenceUpdates()
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val username: String = "Loading...",
    val error: String? = null,
    val unreadGroupMessagesCount: Int = 0
)

