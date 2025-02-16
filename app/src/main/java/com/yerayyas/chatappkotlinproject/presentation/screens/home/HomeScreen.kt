package com.yerayyas.chatappkotlinproject.presentation.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.Routes
import com.yerayyas.chatappkotlinproject.presentation.components.UserListItem
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Users", "Chats")
    var showMenu by remember { mutableStateOf(false) }
    val username by viewModel.username.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // Creamos el PagerState indicando la cantidad de páginas (en este ejemplo: 2)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }  // Establece el número total de páginas
    )

    // Mueve la app al background al presionar el botón "back"
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
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.menu)
                        )
                    }
                },
                actions = {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.user_profile_btn)) },
                            onClick = {
                                showMenu = false // Cierra el menú
                                navController.navigate(Routes.UserProfile.route)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.user_about_btn)) },
                            onClick = { /*viewModel.aboutUser()*/ Toast.makeText(
                                context,
                                "About user",
                                Toast.LENGTH_SHORT
                            ).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.sign_out_btn)) },
                            onClick = { viewModel.signOut() }
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
            // TabRow para las pestañas
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title) }
                    )
                }
            }

            // HorizontalPager oficial (ExperimentalFoundationApi)
            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> UsersScreen(viewModel, navController)
                        1 -> ChatsList()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersScreen(
    viewModel: HomeViewModel,
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
            inputField = {
                // Campo de entrada del SearchBar usando TextField
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Find users...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            expanded = isActive,
            onExpandedChange = { isActive = it },
            modifier = Modifier.fillMaxWidth(),
            shape = SearchBarDefaults.inputFieldShape,
            colors = SearchBarDefaults.colors(),
            tonalElevation = SearchBarDefaults.TonalElevation,
            shadowElevation = SearchBarDefaults.ShadowElevation,
            windowInsets = SearchBarDefaults.windowInsets,
            content = {
                // Contenido que se muestra cuando el SearchBar está expandido
                UsersList(viewModel, navController)
            }
        )
        if (!isActive) {
            // Cuando no está activo, también se muestra la lista
            UsersList(viewModel, navController)
        }
    }
}


@Composable
private fun UsersList(
    viewModel: HomeViewModel,
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
                onItemClick = { navController.navigate("chat/${user.id}?username=${user.username}")
                }
            )
        }
    }
}

@Composable
private fun ChatsList() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(10) { index ->
            Text(
                text = "Chat $index",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

