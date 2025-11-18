package com.yerayyas.chatappkotlinproject.presentation.viewmodel.group

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.CreateGroupUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetAllUsersUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CreateGroupViewModel"

/**
 * ViewModel for the group creation screen.
 *
 * This ViewModel orchestrates the group creation process by delegating all data
 * and business logic operations to specific use cases, adhering to Clean Architecture principles.
 * Its responsibilities are limited to:
 * - Managing UI state (`CreateGroupUiState`).
 * - Handling user input and selection.
 * - Calling use cases to load user lists, search users, and create the group.
 *
 * @property createGroupUseCase Use case for handling the entire group creation logic.
 * @property getAllUsersUseCase Use case for fetching a list of all available users, excluding the current one.
 * @property searchUsersUseCase Use case for searching users by a query, excluding the current one.
 */
@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val searchUsersUseCase: SearchUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _availableUsers = MutableStateFlow<List<User>>(emptyList())
    val availableUsers: StateFlow<List<User>> = _availableUsers.asStateFlow()

    private val _selectedUsers = MutableStateFlow<List<User>>(emptyList())
    val selectedUsers: StateFlow<List<User>> = _selectedUsers.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Call the UseCase, which returns a Flow
            getAllUsersUseCase()
                .catch { e ->
                    Log.e(TAG, "Error loading users", e)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load users: ${e.message}")
                }
                .collect { users ->
                    _availableUsers.value = users
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            loadUsers()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Call the suspend UseCase
                val users = searchUsersUseCase(query)
                _availableUsers.value = users
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error searching users", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Search failed: ${e.message}")
            }
        }
    }

    fun onGroupNameChange(name: String) {
        _uiState.value = _uiState.value.copy(groupName = name, error = null)
    }

    fun onGroupDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(groupDescription = description, error = null)
    }

    fun onUserSelectionToggle(user: User) {
        val currentSelected = _selectedUsers.value.toMutableList()
        if (currentSelected.contains(user)) {
            currentSelected.remove(user)
        } else {
            currentSelected.add(user)
        }
        _selectedUsers.value = currentSelected
    }

    fun createGroup(imageUri: Uri? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // The ViewModel's only job is to collect data from the UI
            // and pass it to the UseCase. All logic is in the UseCase.
            val result = createGroupUseCase(
                name = _uiState.value.groupName,
                description = _uiState.value.groupDescription,
                memberIds = _selectedUsers.value.map { it.id },
                imageUri = imageUri
            )

            if (result.isSuccess) {
                Log.d(TAG, "Group created successfully with ID: ${result.getOrNull()}")
                _uiState.value = _uiState.value.copy(isLoading = false, isGroupCreated = true)
            } else {
                val error = result.exceptionOrNull()?.message ?: "An unknown error occurred"
                Log.e(TAG, "Failed to create group: $error")
                _uiState.value = _uiState.value.copy(isLoading = false, error = error)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetCreationState() {
        _uiState.value = _uiState.value.copy(isGroupCreated = false)
    }
}

/**
 * Represents the UI state for the group creation screen.
 */
data class CreateGroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGroupCreated: Boolean = false,
    val groupName: String = "",
    val groupDescription: String = ""
)