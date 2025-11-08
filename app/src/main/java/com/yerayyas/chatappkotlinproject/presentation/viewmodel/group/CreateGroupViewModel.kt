package com.yerayyas.chatappkotlinproject.presentation.viewmodel.group

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
     * Carga la información del usuario actual primero
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _currentUser.value = user
                println("DEBUG: CreateGroupViewModel - Current user loaded: ${user?.username}")
            } catch (e: Exception) {
                println("DEBUG: CreateGroupViewModel - Error loading current user: ${e.message}")
            }
        }
    }

    /**
     * Carga todos los usuarios disponibles de Firebase
     */
    private fun loadUsers() {
        viewModelScope.launch {
            try {
                println("DEBUG: CreateGroupViewModel - Loading users from Firebase...")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Recopilar usuarios desde Firebase usando Flow
                userRepository.getAllUsers().collect { users ->
                    println("DEBUG: CreateGroupViewModel - Received ${users.size} users from Firebase")

                    // Filtrar el usuario actual para que no aparezca en la lista
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
                println("DEBUG: CreateGroupViewModel - Error loading users: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading users: ${e.message}"
                )
            }
        }
    }

    /**
     * Busca usuarios por nombre de usuario
     */
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            loadUsers() // Recargar todos los usuarios si no hay query
            return
        }

        viewModelScope.launch {
            try {
                println("DEBUG: CreateGroupViewModel - Searching users with query: '$query'")
                _uiState.value = _uiState.value.copy(isLoading = true)

                val searchResults = userRepository.searchUsers(query)

                // Filtrar el usuario actual
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

                println("DEBUG: CreateGroupViewModel - Found ${filteredResults.size} users matching '$query'")
            } catch (e: Exception) {
                println("DEBUG: CreateGroupViewModel - Error searching users: ${e.message}")
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
            println("DEBUG: CreateGroupViewModel - Removed user: ${user.username}")
        } else {
            currentSelected.add(user)
            println("DEBUG: CreateGroupViewModel - Added user: ${user.username}")
        }

        _selectedUsers.value = currentSelected
        println("DEBUG: CreateGroupViewModel - Total selected users: ${currentSelected.size}")
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

                println("DEBUG: CreateGroupViewModel - Creating group '${state.groupName}' with ${_selectedUsers.value.size} members")
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
                    println("DEBUG: CreateGroupViewModel - Group created successfully!")
                    _uiState.value = state.copy(
                        isLoading = false,
                        isGroupCreated = true,
                        error = null
                    )
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    println("DEBUG: CreateGroupViewModel - Error creating group: $errorMessage")
                    _uiState.value = state.copy(
                        isLoading = false,
                        error = "Error creating group: $errorMessage"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: CreateGroupViewModel - Exception creating group: ${e.message}")
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