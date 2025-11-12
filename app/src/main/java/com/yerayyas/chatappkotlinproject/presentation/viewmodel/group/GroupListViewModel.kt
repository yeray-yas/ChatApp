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
 * ViewModel for the user's group list
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
     * Loads the user's groups
     */
    fun loadUserGroups() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                getUserGroupsUseCase.execute()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error loading groups: ${exception.message}"
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
                    error = "Error loading groups: ${e.message}"
                )
            }
        }
    }

    /**
     * Updates the search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applySearchFilter()
    }

    /**
     * Clears the search
     */
    fun clearSearch() {
        _searchQuery.value = ""
        applySearchFilter()
    }

    /**
     * Leaves a group
     */
    fun leaveGroup(groupId: String, userName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = manageGroupMembersUseCase.leaveGroup(groupId, userName)

                if (result.isSuccess) {
                    // Remove the group from the local list
                    val updatedGroups = _groups.value.filter { it.id != groupId }
                    _groups.value = updatedGroups
                    applySearchFilter()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "You have successfully left the group"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Error leaving group"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error leaving group: ${e.message}"
                )
            }
        }
    }

    /**
     * Refreshes the group list
     */
    fun refreshGroups() {
        loadUserGroups()
    }

    /**
     * Gets group statistics
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
     * Gets groups by category
     */
    fun getGroupsByCategory(): Map<String, List<GroupChat>> {
        val allGroups = _filteredGroups.value
        val currentUserId = getUserGroupsUseCase.getCurrentUserId()

        return mapOf(
            "Administered" to allGroups.filter { it.isAdmin(currentUserId) },
            "Member" to allGroups.filter { !it.isAdmin(currentUserId) },
            "Recent" to allGroups.filter { group ->
                val dayInMillis = 24 * 60 * 60 * 1000
                System.currentTimeMillis() - group.lastActivity < dayInMillis
            }
        )
    }

    /**
     * Checks if a group has unread messages
     */
    fun hasUnreadMessages(groupId: String): Boolean {
        // TODO: Implement unread messages logic
        // For now returns false
        return false
    }

    /**
     * Gets the number of unread messages for a group
     */
    fun getUnreadCount(groupId: String): Int {
        // TODO: Implement unread message count
        // For now returns 0
        return 0
    }

    /**
     * Applies search filter
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
     * Gets information for a specific group
     */
    suspend fun getGroupInfo(groupId: String): GroupChat? {
        return getUserGroupsUseCase.getGroupById(groupId)
    }

    /**
     * Checks if the user is an admin of a group
     */
    suspend fun isUserAdmin(groupId: String): Boolean {
        return getUserGroupsUseCase.isUserAdminOfGroup(groupId)
    }

    /**
     * Clears errors and messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}

/**
 * UI state for the group list
 */
data class GroupListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Group statistics
 */
data class GroupsStats(
    val totalGroups: Int = 0,
    val activeGroups: Int = 0,
    val adminGroups: Int = 0,
    val recentActivity: Int = 0
)

/**
 * Extension function to get the current user's ID
 */
private fun GetUserGroupsUseCase.getCurrentUserId(): String {
    // Temporary implementation until GetUserGroupsUseCase has access to FirebaseAuth
    return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
}