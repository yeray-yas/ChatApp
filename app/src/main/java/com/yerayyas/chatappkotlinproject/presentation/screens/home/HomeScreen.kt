package com.yerayyas.chatappkotlinproject.presentation.screens.home

import android.Manifest
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.presentation.components.ChatListItem
import com.yerayyas.chatappkotlinproject.presentation.components.GroupChatItem
import com.yerayyas.chatappkotlinproject.presentation.components.UserListItem
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.ChatsListViewModel
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.ChatsUiState

/**
 * Composable function representing the main Home screen of the app.
 *
 * Displays a top app bar with user info, menu actions (profile, about, delete user, sign out),
 * and a tab layout for navigating between Users and Chats sections.
 *
 * @param navController Navigation controller for navigating between screens.
 * @param selectedTab Initial tab to be selected (0=Users, 1=Chats, 2=Groups). Default is 0.
 * @param viewModel ViewModel providing user-related state and actions.
 * @param chatsListViewModel ViewModel managing chat list state, including unread messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    selectedTab: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
    chatsListViewModel: ChatsListViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Users", "Chats", "Groups")
    var showMenu by remember { mutableStateOf(false) }
    val username by viewModel.username.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadMessagesCount by chatsListViewModel.totalUnreadCount.collectAsState()
    val unreadGroupMessagesCount by viewModel.unreadGroupMessagesCount.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.i("HomeScreen", "POST_NOTIFICATIONS permission granted.")
                // Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("HomeScreen", "POST_NOTIFICATIONS permission denied.")
                // Inform the user why notifications are useful (optional but recommended)
                Toast.makeText(
                    context,
                    "Notifications disabled. You might miss new messages.",
                    Toast.LENGTH_LONG
                ).show()
                // Here you could show a dialog explaining how to enable them manually in settings
            }
        }
    )

    // Effect to request permission ONCE when HomeScreen is first displayed (if needed)
    LaunchedEffect(Unit) { // The 'Unit' key ensures this runs only once
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Only for Android 13+
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus == PackageManager.PERMISSION_DENIED) {
                Log.d("HomeScreen", "Requesting POST_NOTIFICATIONS permission...")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("HomeScreen", "POST_NOTIFICATIONS permission already granted.")
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadUnreadGroupMessagesCount()
                chatsListViewModel.loadUserChatsAndUnreadCount()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = selectedTab,
        pageCount = { tabs.size }
    )

    BackHandler { (context as Activity).moveTaskToBack(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.menu)
                        )
                    }
                },
                actions = {
                    // Button to access groups
                    IconButton(onClick = {
                        navController.navigate(Routes.GroupList.route)
                    }) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = "Groups"
                        )
                    }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.user_profile_btn)) },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.UserProfile.route)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.Settings.route)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.user_about_btn)) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "About user", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete user") },
                            onClick = {
                                viewModel.deleteUser { success, error ->
                                    if (success) {
                                        navController.navigate(Routes.Main.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error deleting account: $error",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.sign_out_btn)) },
                            onClick = {
                                viewModel.signOut {
                                    navController.navigate(Routes.Main.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CreateGroup.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.GroupAdd,
                    contentDescription = "Create group"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            val count = when (title) {
                                "Chats" -> unreadMessagesCount
                                "Groups" -> unreadGroupMessagesCount
                                else -> 0
                            }

                            val textToShow = if (count > 0) {
                                "[$count] $title"
                            } else {
                                title
                            }

                            Text(text = textToShow)
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> UsersScreen(viewModel, navController)
                    1 -> ChatsList(navController, hiltViewModel())
                    2 -> GroupsList(navController, modifier = Modifier, viewModel)
                }
            }
        }
    }
}

/**
 * Composable function displaying the user search and user list UI.
 *
 * @param viewModel ViewModel managing user data and search state.
 * @param navController Navigation controller for navigating to user chat screens.
 */
@Composable
private fun UsersScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val users by viewModel.users.collectAsState()
    var showNoUsersFound by remember { mutableStateOf(false) }

    // Show "No users found" after a short delay only if search query is active & no users
    LaunchedEffect(searchQuery, users) {
        if (searchQuery.isNotEmpty() && users.isEmpty()) {
            delay(600) // 600 ms delay before showing "No users found"
            showNoUsersFound = true
        } else {
            showNoUsersFound = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text("Find users...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Find")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Delete search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { /* Hide keyboard or perform search if needed */ }
            )
        )
        if (showNoUsersFound) {
            // Special empty/search state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "No users found",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Oops! The user you're looking for doesn't exist.\nTry searching with a different username.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        } else {
            UsersList(users, navController)
        }
    }
}

/**
 * Displays a list of users in a vertically scrollable column.
 *
 * @param users List of users to display.
 * @param navController Navigation controller for navigating to individual chats.
 */
@Composable
private fun UsersList(
    users: List<com.yerayyas.chatappkotlinproject.data.model.User>,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users) { user ->
            UserListItem(
                user = user,
                onItemClick = {
                    // Build the route using slashes, same as in the NavHost
                    val route = "chat/${user.id}/${user.username}"
                    try {
                        navController.navigate(route)
                    } catch (e: Exception) {
                        Log.e("UsersList", "Navigation failed for route: $route", e)
                    }
                }
            )
        }
    }
}

/**
 * Displays a list of recent chats in a vertically scrollable column.
 *
 * @param navController Navigation controller for navigating to specific chat screens.
 * @param viewModel ViewModel providing chat data.
 */
@Composable
private fun ChatsList(
    navController: NavController,
    viewModel: ChatsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is ChatsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ChatsUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        is ChatsUiState.Success -> {
            if (state.chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "You have no chats yet.\nStart a conversation from the Users tab.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.chats, key = { it.chatId }) { chatItem ->
                        ChatListItem(
                            chat = chatItem,
                            onClick = {
                                navController.navigate(
                                    Routes.Chat.createRoute(
                                        userId = chatItem.otherUserId,
                                        username = chatItem.otherUsername
                                    )
                                )
                            }

                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the list of groups available to the user
 */
@Composable
private fun GroupsList(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentUserId = viewModel.getCurrentUserId() ?: ""

    // Get real user groups from Firebase
    val userGroups by viewModel.getUserGroups(currentUserId).collectAsState(initial = emptyList())

    // Sample data as fallback if there are no real groups
    val sampleGroups = remember {
        listOf(
            GroupChat(
                id = "group1",
                name = "Family",
                description = "Family chat",
                memberIds = listOf(currentUserId, "user2", "user3", "user4"),
                adminIds = listOf(currentUserId),
                createdBy = currentUserId,
                lastActivity = System.currentTimeMillis() - 3600000, // 1 hour ago
                lastMessage = ChatMessage(
                    message = "How's everyone?",
                    senderId = "user2",
                    timestamp = System.currentTimeMillis() - 3600000
                )
            ),
            GroupChat(
                id = "group2",
                name = "Work - Dev Team",
                description = "Development team",
                memberIds = listOf(currentUserId, "user5", "user6", "user7", "user8"),
                adminIds = listOf(currentUserId, "user5"),
                createdBy = currentUserId,
                lastActivity = System.currentTimeMillis() - 7200000, // 2 hours ago
                lastMessage = ChatMessage(
                    message = "The new feature is ready for testing",
                    senderId = "user5",
                    timestamp = System.currentTimeMillis() - 7200000
                )
            ),
            GroupChat(
                id = "group3",
                name = "University Friends",
                description = "University group",
                memberIds = listOf(currentUserId, "user9", "user10", "user11"),
                adminIds = listOf(currentUserId),
                createdBy = currentUserId,
                lastActivity = System.currentTimeMillis() - 86400000, // 1 day ago
                lastMessage = ChatMessage(
                    message = "Want to grab lunch?",
                    senderId = "user9",
                    timestamp = System.currentTimeMillis() - 86400000
                )
            )
        )
    }

    // Use real groups if they exist, otherwise use sample data
    val groupsToShow = userGroups.ifEmpty { sampleGroups }

    Column(modifier = modifier.fillMaxSize()) {
        if (groupsToShow.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(groupsToShow) { group ->
                    val unreadCount by viewModel.getUnreadCountForGroup(group.id).collectAsState(initial = 0)
                    GroupChatItem(
                        groupChat = group,
                        unreadCount = unreadCount,
                        onGroupClick = { groupId ->
                            navController.navigate(Routes.GroupChat.createRoute(groupId))
                        }
                    )
                }
            }
        } else {
            // Empty state when there are no groups
            EmptyGroupsState(
                onCreateGroupClick = {
                    navController.navigate(Routes.CreateGroup.route)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Empty state when there are no groups
 */
@Composable
private fun EmptyGroupsState(
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You don't have any groups yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create your first group to chat with multiple people",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateGroupClick,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create group")
        }
    }
}
