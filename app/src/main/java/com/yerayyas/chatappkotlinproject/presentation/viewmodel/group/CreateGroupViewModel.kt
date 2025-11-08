package com.yerayyas.chatappkotlinproject.presentation.viewmodel.group

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.group.CreateGroupUseCase
import com.yerayyas.chatappkotlinproject.presentation.screens.group.CreateGroupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateGroupUiState>(CreateGroupUiState.Idle)
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _availableUsers = MutableStateFlow<List<User>>(emptyList())
    val availableUsers: StateFlow<List<User>> = _availableUsers.asStateFlow()

    private val _selectedMembers = MutableStateFlow<List<User>>(emptyList())
    val selectedMembers: StateFlow<List<User>> = _selectedMembers.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _uiState.value = CreateGroupUiState.Loading

                // Aquí cargaríamos los usuarios disponibles
                // Por ahora usamos una lista mock hasta implementar el repository completo
                _availableUsers.value = getMockUsers()

                _uiState.value = CreateGroupUiState.Idle
            } catch (e: Exception) {
                _uiState.value = CreateGroupUiState.Error("Error al cargar usuarios: ${e.message}")
            }
        }
    }

    fun addMember(user: User) {
        val currentMembers = _selectedMembers.value.toMutableList()
        if (!currentMembers.contains(user)) {
            currentMembers.add(user)
            _selectedMembers.value = currentMembers
        }
    }

    fun removeMember(user: User) {
        val currentMembers = _selectedMembers.value.toMutableList()
        currentMembers.remove(user)
        _selectedMembers.value = currentMembers
    }

    fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = CreateGroupUiState.Loading

                val result = createGroupUseCase.execute(
                    name = name,
                    description = description,
                    memberIds = memberIds,
                    imageUri = imageUri
                )

                if (result.isSuccess) {
                    _uiState.value = CreateGroupUiState.Success(result.getOrThrow())
                } else {
                    _uiState.value = CreateGroupUiState.Error(
                        result.exceptionOrNull()?.message ?: "Error desconocido"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = CreateGroupUiState.Error("Error al crear grupo: ${e.message}")
            }
        }
    }

    // Mock data hasta implementar el repository completo
    private fun getMockUsers(): List<User> {
        return listOf(
            User(
                id = "1",
                username = "juan_perez",
                email = "juan@example.com",
                profileImage = "",
                status = "online",
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            ),
            User(
                id = "2",
                username = "maria_garcia",
                email = "maria@example.com",
                profileImage = "",
                status = "offline",
                isOnline = false,
                lastSeen = System.currentTimeMillis() - 3600000 // 1 hora atrás
            ),
            User(
                id = "3",
                username = "carlos_rodriguez",
                email = "carlos@example.com",
                profileImage = "",
                status = "online",
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            ),
            User(
                id = "4",
                username = "ana_martinez",
                email = "ana@example.com",
                profileImage = "",
                status = "offline",
                isOnline = false,
                lastSeen = System.currentTimeMillis() - 7200000 // 2 horas atrás
            )
        )
    }
}