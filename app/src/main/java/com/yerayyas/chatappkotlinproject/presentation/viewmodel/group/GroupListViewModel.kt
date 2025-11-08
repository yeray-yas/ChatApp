package com.yerayyas.chatappkotlinproject.presentation.viewmodel.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.usecases.group.GetUserGroupsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.group.ManageGroupMembersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la lista de grupos del usuario
 */
@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val getUserGroupsUseCase: GetUserGroupsUseCase,
    private val manageGroupMembersUseCase: ManageGroupMembersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    private val _groups = MutableStateFlow<List<GroupChat>>(emptyList())
    val groups: StateFlow<List<GroupChat>> = _groups.asStateFlow()

    private val _filteredGroups = MutableStateFlow<List<GroupChat>>(emptyList())
    val filteredGroups: StateFlow<List<GroupChat>> = _filteredGroups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadUserGroups()
    }

    /**
     * Carga los grupos del usuario
     */
    fun loadUserGroups() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                getUserGroupsUseCase.execute()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error al cargar grupos: ${exception.message}"
                        )
                    }
                    .collect { groupList ->
                        _groups.value = groupList
                        applySearchFilter()
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar grupos: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualiza la consulta de búsqueda
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applySearchFilter()
    }

    /**
     * Limpia la búsqueda
     */
    fun clearSearch() {
        _searchQuery.value = ""
        applySearchFilter()
    }

    /**
     * Abandona un grupo
     */
    fun leaveGroup(groupId: String, userName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = manageGroupMembersUseCase.leaveGroup(groupId, userName)

                if (result.isSuccess) {
                    // Remover el grupo de la lista local
                    val updatedGroups = _groups.value.filter { it.id != groupId }
                    _groups.value = updatedGroups
                    applySearchFilter()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Has abandonado el grupo exitosamente"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Error al abandonar grupo"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al abandonar grupo: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresca la lista de grupos
     */
    fun refreshGroups() {
        loadUserGroups()
    }

    /**
     * Obtiene estadísticas de los grupos
     */
    fun getGroupsStats(): GroupsStats {
        val allGroups = _groups.value
        return GroupsStats(
            totalGroups = allGroups.size,
            activeGroups = allGroups.count { it.isActive },
            adminGroups = allGroups.count { group ->
                val currentUserId = getUserGroupsUseCase.getCurrentUserId()
                group.isAdmin(currentUserId)
            },
            recentActivity = allGroups.count { group ->
                val dayInMillis = 24 * 60 * 60 * 1000
                System.currentTimeMillis() - group.lastActivity < dayInMillis
            }
        )
    }

    /**
     * Obtiene grupos por categoría
     */
    fun getGroupsByCategory(): Map<String, List<GroupChat>> {
        val allGroups = _filteredGroups.value
        val currentUserId = getUserGroupsUseCase.getCurrentUserId()

        return mapOf(
            "Administrados" to allGroups.filter { it.isAdmin(currentUserId) },
            "Miembro" to allGroups.filter { !it.isAdmin(currentUserId) },
            "Recientes" to allGroups.filter { group ->
                val dayInMillis = 24 * 60 * 60 * 1000
                System.currentTimeMillis() - group.lastActivity < dayInMillis
            }
        )
    }

    /**
     * Verifica si un grupo tiene mensajes no leídos
     */
    fun hasUnreadMessages(groupId: String): Boolean {
        // TODO: Implementar lógica de mensajes no leídos
        // Por ahora retorna false
        return false
    }

    /**
     * Obtiene el número de mensajes no leídos para un grupo
     */
    fun getUnreadCount(groupId: String): Int {
        // TODO: Implementar conteo de mensajes no leídos
        // Por ahora retorna 0
        return 0
    }

    /**
     * Aplica filtro de búsqueda
     */
    private fun applySearchFilter() {
        val query = _searchQuery.value.lowercase().trim()

        _filteredGroups.value = if (query.isEmpty()) {
            _groups.value
        } else {
            _groups.value.filter { group ->
                group.name.lowercase().contains(query) ||
                        group.description.lowercase().contains(query)
            }
        }
    }

    /**
     * Obtiene información de un grupo específico
     */
    suspend fun getGroupInfo(groupId: String): GroupChat? {
        return getUserGroupsUseCase.getGroupById(groupId)
    }

    /**
     * Verifica si el usuario es administrador de un grupo
     */
    suspend fun isUserAdmin(groupId: String): Boolean {
        return getUserGroupsUseCase.isUserAdminOfGroup(groupId)
    }

    /**
     * Limpia errores y mensajes
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}

/**
 * Estado de UI para la lista de grupos
 */
data class GroupListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isRefreshing: Boolean = false
)

/**
 * Estadísticas de grupos del usuario
 */
data class GroupsStats(
    val totalGroups: Int = 0,
    val activeGroups: Int = 0,
    val adminGroups: Int = 0,
    val recentActivity: Int = 0
)

/**
 * Función de extensión para obtener el ID del usuario actual
 */
private fun GetUserGroupsUseCase.getCurrentUserId(): String {
    // Implementación temporal hasta que GetUserGroupsUseCase tenga acceso al FirebaseAuth
    return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
}