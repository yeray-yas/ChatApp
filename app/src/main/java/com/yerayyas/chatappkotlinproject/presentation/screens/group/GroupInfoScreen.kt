package com.yerayyas.chatappkotlinproject.presentation.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.components.ErrorState
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.group.GroupListViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for information and management of a specific group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    navController: NavController,
    viewModel: GroupListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Get group information (for now we use the list ViewModel)
    // In a real implementation, you would have a specific ViewModel for GroupInfo

    LaunchedEffect(uiState.error, uiState.message) {
        val error = uiState.error
        val message = uiState.message

        if (error != null) {
            scope.launch {
                snackbarHostState.showSnackbar(error)
                viewModel.clearMessages()
            }
        } else if (message != null) {
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearMessages()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Group Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // TODO: Navigate to group settings
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: Add new members
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add member",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(
                        message = "Loading group information...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error ?: "Unknown error",
                        onRetry = { viewModel.refreshGroups() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    // For now we show mock data until implementing the specific ViewModel
                    GroupInfoContent(
                        group = getMockGroup(groupId),
                        members = getMockMembers(),
                        currentUserId = getCurrentUserId(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Main content of group information
 */
@Composable
private fun GroupInfoContent(
    group: GroupChat,
    members: List<User>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic group information
        item {
            GroupBasicInfoCard(
                group = group,
                isAdmin = group.isAdmin(currentUserId)
            )
        }

        // Members list
        item {
            Text(
                text = "Members (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(members) { member ->
            MemberListItem(
                member = member,
                isAdmin = group.isAdmin(member.id),
                isCurrentUser = member.id == currentUserId,
                canManageMembers = group.isAdmin(currentUserId)
            )
        }

        // Spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Card with basic group information
 */
@Composable
private fun GroupBasicInfoCard(
    group: GroupChat,
    isAdmin: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Group avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group name
            Text(
                text = group.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = group.description.ifEmpty { "No description" },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Additional information
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InfoItem(
                    label = "Members",
                    value = group.memberIds.size.toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                InfoItem(
                    label = "Created",
                    value = formatCreationDate(group.createdAt),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isAdmin) {
                    InfoItem(
                        label = "Role",
                        value = "Admin",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Item of information
 */
@Composable
private fun InfoItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

/**
 * Member item in the list
 */
@Composable
private fun MemberListItem(
    member: User,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    canManageMembers: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Member avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isCurrentUser) "${member.username} (You)" else member.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isAdmin) {
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = if (member.isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (member.isOnline)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Actions (only for admins)
            if (canManageMembers && !isCurrentUser) {
                IconButton(
                    onClick = {
                        // TODO: Show member management options
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Manage member",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Formats the creation date
 */
private fun formatCreationDate(timestamp: Long): String {
    val date = Date(timestamp)
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
}

/**
 * Gets a mock group for demonstration
 */
private fun getMockGroup(groupId: String): GroupChat {
    return GroupChat(
        id = groupId,
        name = "Test Group",
        description = "This is a test group for the application",
        memberIds = listOf("user1", "user2", "user3", "user4"),
        adminIds = listOf("user1"),
        createdBy = "user1",
        createdAt = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // 7 days ago
        lastActivity = System.currentTimeMillis() - 3600000,
        isActive = true
    )
}

/**
 * Gets mock members for demonstration
 */
private fun getMockMembers(): List<User> {
    return listOf(
        User(
            id = "user1",
            username = "juan_admin",
            email = "juan@example.com",
            profileImage = "",
            status = "online",
            isOnline = true
        ),
        User(
            id = "user2",
            username = "maria_garcia",
            email = "maria@example.com",
            profileImage = "",
            status = "online",
            isOnline = true
        ),
        User(
            id = "user3",
            username = "carlos_rodriguez",
            email = "carlos@example.com",
            profileImage = "",
            status = "offline",
            isOnline = false
        ),
        User(
            id = "user4",
            username = "ana_martinez",
            email = "ana@example.com",
            profileImage = "",
            status = "offline",
            isOnline = false
        )
    )
}

/**
 * Gets the current user ID
 */
private fun getCurrentUserId(): String {
    return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "user1"
}