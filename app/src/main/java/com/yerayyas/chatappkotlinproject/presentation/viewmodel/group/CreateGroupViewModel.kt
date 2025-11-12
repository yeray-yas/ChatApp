package com.yerayyas.chatappkotlinproject.presentation.viewmodel.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.group.CreateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CreateGroupViewModel"

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val groupChatRepository: GroupChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _availableUsers = MutableStateFlow<List<User>>(emptyList())
    val availableUsers: StateFlow<List<User>> = _availableUsers.asStateFlow()

    private val _selectedUsers = MutableStateFlow<List<User>>(emptyList())
    val selectedUsers: StateFlow<List<User>> = _selectedUsers.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
        loadUsers()
    }

    /**
     * Loads the current user information first
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _currentUser.value = user
                Log.d(TAG, "Current user loaded: ${user?.username}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current user: ${e.message}")
            }
        }
    }

    /**
     * Loads all available users from Firebase
     */
    private fun loadUsers() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading users from Firebase...")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Collect users from Firebase using Flow
                userRepository.getAllUsers().collect { users ->
                    Log.d(TAG, "Received ${users.size} users from Firebase")

                    // Filter the current user so they don't appear in the list
                    val currentUserId = _currentUser.value?.id
                    val filteredUsers = if (currentUserId != null) {
                        users.filter { it.id != currentUserId }
                    } else {
                        users
                    }

                    _availableUsers.value = filteredUsers
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading users: ${e.message}"
                )
            }
        }
    }

    /**
     * Searches users by username
     */
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            loadUsers() // Reload all users if no query
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Searching users with query: '$query'")
                _uiState.value = _uiState.value.copy(isLoading = true)

                val searchResults = userRepository.searchUsers(query)

                // Filter the current user
                val currentUserId = _currentUser.value?.id
                val filteredResults = if (currentUserId != null) {
                    searchResults.filter { it.id != currentUserId }
                } else {
                    searchResults
                }

                _availableUsers.value = filteredResults
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )

                Log.d(TAG, "Found ${filteredResults.size} users matching '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching users: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error searching users: ${e.message}"
                )
            }
        }
    }

    fun updateGroupName(name: String) {
        _uiState.value = _uiState.value.copy(groupName = name)
    }

    fun updateGroupDescription(description: String) {
        _uiState.value = _uiState.value.copy(groupDescription = description)
    }

    fun toggleUserSelection(user: User) {
        val currentSelected = _selectedUsers.value.toMutableList()

        if (currentSelected.contains(user)) {
            currentSelected.remove(user)
            Log.d(TAG, "Removed user: ${user.username}")
        } else {
            currentSelected.add(user)
            Log.d(TAG, "Added user: ${user.username}")
        }

        _selectedUsers.value = currentSelected
        Log.d(TAG, "Total selected users: ${currentSelected.size}")
    }

    fun createGroup() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val currentUserData = _currentUser.value

                // Validaciones
                if (state.groupName.isBlank()) {
                    _uiState.value = state.copy(error = "Group name is required")
                    return@launch
                }

                if (_selectedUsers.value.isEmpty()) {
                    _uiState.value = state.copy(error = "At least one member must be selected")
                    return@launch
                }

                if (currentUserData == null) {
                    _uiState.value = state.copy(error = "Current user not found")
                    return@launch
                }

                Log.d(
                    TAG,
                    "Creating group '${state.groupName}' with ${_selectedUsers.value.size} members"
                )
                _uiState.value = state.copy(isLoading = true, error = null)

                // Crear lista de miembros (incluir usuario actual + seleccionados)
                val allMembers = mutableListOf<String>().apply {
                    add(currentUserData.id) // Agregar usuario actual
                    addAll(_selectedUsers.value.map { it.id }) // Agregar usuarios seleccionados
                }

                // Crear el grupo
                val groupChat = GroupChat(
                    id = "", // Firebase generará el ID
                    name = state.groupName,
                    description = state.groupDescription,
                    memberIds = allMembers,
                    adminIds = listOf(currentUserData.id), // Usuario actual es admin
                    createdBy = currentUserData.id,
                    createdAt = System.currentTimeMillis(),
                    lastActivity = System.currentTimeMillis(),
                    isActive = true
                )

                val result = createGroupUseCase.execute(
                    name = state.groupName,
                    description = state.groupDescription,
                    memberIds = allMembers,
                    imageUri = null
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Group created successfully!")
                    _uiState.value = state.copy(
                        isLoading = false,
                        isGroupCreated = true,
                        error = null
                    )
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Error creating group: $errorMessage")
                    _uiState.value = state.copy(
                        isLoading = false,
                        error = "Error creating group: $errorMessage"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating group: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error creating group: ${e.message}"
                )
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
 * Estado de UI para la creación de grupos
 */
data class CreateGroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGroupCreated: Boolean = false,
    val groupName: String = "",
    val groupDescription: String = ""
)