package com.yerayyas.chatappkotlinproject.presentation.screens.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.components.ChatListItem
import com.yerayyas.chatappkotlinproject.presentation.components.UserListItem
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.HomeViewModel
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.ChatsListViewModel
import kotlinx.coroutines.launch

/**
 * Composable function representing the main Home screen of the app.
 *
 * Displays a top app bar with user info, menu actions (profile, about, delete user, sign out),
 * and a tab layout for navigating between Users and Chats sections.
 *
 * @param navController Navigation controller for navigating between screens.
 * @param viewModel ViewModel providing user-related state and actions.
 * @param chatsListViewModel ViewModel managing chat list state, including unread messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
    chatsListViewModel: ChatsListViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Users", "Chats")
    var showMenu by remember { mutableStateOf(false) }
    val username by viewModel.username.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadMessagesCount by chatsListViewModel.unreadMessagesCount.collectAsState()
    val context = LocalContext.current

    // --- Inicio: Lógica para solicitar permiso de notificación ---
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.i("HomeScreen", "POST_NOTIFICATIONS permission granted.")
                // Puedes mostrar un mensaje de agradecimiento si quieres
                // Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("HomeScreen", "POST_NOTIFICATIONS permission denied.")
                // Informar al usuario por qué son útiles las notificaciones (opcional pero recomendado)
                Toast.makeText(
                    context,
                    "Notifications disabled. You might miss new messages.",
                    Toast.LENGTH_LONG
                ).show()
                // Aquí podrías mostrar un diálogo explicando cómo habilitarlas manualmente en ajustes
            }
        }
    )

    // Efecto para solicitar el permiso UNA VEZ cuando HomeScreen se muestra por primera vez (si es necesario)
    LaunchedEffect(Unit) { // Clave Unit para que se ejecute solo una vez
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Solo para Android 13+
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

    val pagerState = rememberPagerState(
        initialPage = 0,
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
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.user_profile_btn)) },
                            onClick = {
                                showMenu = false
                                navController.navigate(Routes.UserProfile.route)
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
                                            "Error erasing the count: $error",
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
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            if (index == 1 && unreadMessagesCount > 0) {
                                Text("[$unreadMessagesCount] $title")
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> UsersScreen(viewModel, navController)
                    1 -> ChatsList(navController, hiltViewModel())
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            onSearch = { /* Implement search functionality */ },
            active = isActive,
            onActiveChange = { isActive = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Find users...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Delete search")
                    }
                }
            },
            content = {
                UsersList(viewModel, navController)
            }
        )
        if (!isActive) {
            UsersList(viewModel, navController)
        }
    }
}

/**
 * Displays a list of users in a vertically scrollable column.
 *
 * @param viewModel ViewModel providing user data.
 * @param navController Navigation controller for navigating to individual chats.
 */
@Composable
private fun UsersList(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val users by viewModel.users.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users) { user ->
            UserListItem(
                user = user,
                onItemClick = {   // --- CORRECCIÓN AQUÍ ---
                    // Construye la ruta usando slashes, igual que en NavHost
                    val route = "chat/${user.id}/${user.username}"
                    Log.d("UsersList", "Navigating to route: $route") // Log para verificar
                    try { // Añadir try-catch por si acaso durante la depuración
                        navController.navigate(route)
                    } catch (e: Exception) {
                        Log.e("UsersList", "Navigation failed for route: $route", e)
                        // Puedes mostrar un Toast o mensaje si falla
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
    navController: NavHostController,
    viewModel: ChatsListViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chats) { chat ->
            ChatListItem(
                chat = chat,
                onClick = {
                    val route = "chat/${chat.otherUserId}/${chat.otherUsername}"
                    Log.d("ChatsList", "Navigating to route: $route") // Log para verificar
                    try {
                        navController.navigate(route)
                    } catch (e: Exception) {
                        Log.e("ChatsList", "Navigation failed for route: $route", e)
                    }
                }
            )
        }
    }
}