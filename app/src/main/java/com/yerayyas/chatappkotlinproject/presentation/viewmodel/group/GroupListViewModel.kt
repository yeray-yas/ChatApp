package com.yerayyas.chatappkotlinproject.presentation.viewmodel.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.CheckUserIsGroupAdminUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.GetGroupByIdUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.GetUnreadMessagesCountForGroupUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.GetUserGroupsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.ManageGroupMembersUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetCurrentUserIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GroupListViewModel"

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val getUserGroupsUseCase: GetUserGroupsUseCase,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val checkUserIsGroupAdminUseCase: CheckUserIsGroupAdminUseCase,
    private val manageGroupMembersUseCase: ManageGroupMembersUseCase,
    private val getUnreadMessagesCountForGroupUseCase: GetUnreadMessagesCountForGroupUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
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

    fun loadUserGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            getUserGroupsUseCase() // Updated call
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error loading groups: ${exception.message}"
                    )
                }
                .collect { groupList ->
                    _groups.value = groupList.sortedByDescending { it.lastActivity }
                    applySearchFilter()
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applySearchFilter()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        applySearchFilter()
    }

    fun leaveGroup(groupId: String, userName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = manageGroupMembersUseCase.leaveGroup(groupId, userName)
            if (result.isSuccess) {
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
        }
    }

    fun refreshGroups() {
        loadUserGroups()
    }

    fun getGroupsStats(): GroupsStats {
        val allGroups = _groups.value
        val currentUserId = getCurrentUserIdUseCase() ?: ""

        return GroupsStats(
            totalGroups = allGroups.size,
            activeGroups = allGroups.count { it.isActive },
            adminGroups = allGroups.count { it.isAdmin(currentUserId) },
            recentActivity = allGroups.count { group ->
                val dayInMillis = 24 * 60 * 60 * 1000
                System.currentTimeMillis() - group.lastActivity < dayInMillis
            }
        )
    }

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

    fun getUnreadCountForGroup(groupId: String): Flow<Int> {
        return getUnreadMessagesCountForGroupUseCase(groupId)
            .onEach { count ->
                Log.d(TAG, "Flow for group $groupId emitted count: $count")
            }
    }

    suspend fun getGroupInfo(groupId: String): GroupChat? {
        return getGroupByIdUseCase(groupId) // Updated call
    }

    suspend fun isUserAdmin(groupId: String): Boolean {
        return checkUserIsGroupAdminUseCase(groupId) // Updated call
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}

// Keep these data classes in the same file as they are specific to this ViewModel's state
data class GroupListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

data class GroupsStats(
    val totalGroups: Int = 0,
    val activeGroups: Int = 0,
    val adminGroups: Int = 0,
    val recentActivity: Int = 0
)
