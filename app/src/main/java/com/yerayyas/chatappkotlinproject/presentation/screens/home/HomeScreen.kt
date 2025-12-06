package com.yerayyas.chatappkotlinproject.presentation.screens.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.presentation.components.ChatListItem
import com.yerayyas.chatappkotlinproject.presentation.components.UserListItem
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.ChatsListViewModel
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.ChatsUiState
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    chatsListViewModel: ChatsListViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Users", "Chats")
    var showMenu by remember { mutableStateOf(false) }


    val uiState by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val chatsUiState by chatsListViewModel.uiState.collectAsState()
    val unreadMessagesCount by chatsListViewModel.totalUnreadCount.collectAsState()
    // -------------------------------------------------------------

    val context = LocalContext.current

    RequestNotificationPermission()
    ObserveLifecycleEvents( chatsListViewModel = chatsListViewModel)

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    BackHandler { (context as Activity).moveTaskToBack(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = uiState.username,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.GroupList.route) }) {
                        BadgedBox(
                            badge = {
                                if (uiState.unreadGroupMessagesCount > 0) {
                                    Badge { Text("${uiState.unreadGroupMessagesCount}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = "Groups")
                        }
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
                        // Erased the Delete user button (for the moment)
                        // DropdownMenuItem(text = { Text("Delete user") }, onClick = { ... })

                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.sign_out_btn)) },
                            onClick = {
                                // Correct call to the new viewmodel
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
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            val count = if (title == "Chats") unreadMessagesCount else 0
                            Text(text = if (count > 0) "[$count] $title" else title)
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> UsersContent(
                        users = users,
                        searchQuery = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onUserClick = { user -> navController.navigate(Routes.Chat.createRoute(user.id, user.username)) }
                    )
                    1 -> ChatsContent(
                        uiState = chatsUiState,
                        onChatClick = { chatItem ->
                            navController.navigate(Routes.Chat.createRoute(chatItem.otherUserId, chatItem.otherUsername))
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun UsersContent(
    users: List<User>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onUserClick: (User) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(users) {
        if (users.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Find users...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        if (users.isEmpty() && searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No users found for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    UserListItem(
                        user = user,
                        onItemClick = { onUserClick(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsContent(
    uiState: ChatsUiState,
    onChatClick: (com.yerayyas.chatappkotlinproject.data.model.ChatListItem) -> Unit
) {
    when (uiState) {
        is ChatsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ChatsUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        is ChatsUiState.Success -> {
            if (uiState.chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "You have no chats yet.\nStart a conversation from the Users tab.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.chats, key = { it.chatId }) { chatItem ->
                        ChatListItem(
                            chat = chatItem,
                            onClick = { onChatClick(chatItem) }
                        )
                    }
                }
            }
        }
    }
}


// --- HELPERS  ---

@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Notifications disabled. You might miss messages.", Toast.LENGTH_LONG).show()
            }
        }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun ObserveLifecycleEvents(
    chatsListViewModel: ChatsListViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                chatsListViewModel.loadUserChatsAndUnreadCount()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
