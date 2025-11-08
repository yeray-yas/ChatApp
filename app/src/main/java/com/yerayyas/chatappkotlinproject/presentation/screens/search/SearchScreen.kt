package com.yerayyas.chatappkotlinproject.presentation.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.presentation.components.*
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.search.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    onMessageClick: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val searchState by searchViewModel.searchState.collectAsStateWithLifecycle()
    val searchResults by searchViewModel.searchResults.collectAsStateWithLifecycle()

    LaunchedEffect(chatId) {
        searchViewModel.initializeSearch(chatId)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Buscar mensajes") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Barra de búsqueda avanzada
            AdvancedSearchBar(
                query = searchState.query,
                onQueryChange = searchViewModel::updateQuery,
                onSearch = searchViewModel::search,
                showFilters = searchState.showFilters,
                onToggleFilters = searchViewModel::toggleFilters,
                activeFilters = searchState.activeFilters,
                onFilterChange = searchViewModel::toggleFilter,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Resultados
            when {
                searchState.isLoading -> {
                    LoadingState(
                        message = "Buscando mensajes...",
                        modifier = Modifier.weight(1f)
                    )
                }

                searchState.error != null -> {
                    ErrorState(
                        message = searchState.error ?: "Error en la búsqueda",
                        onRetry = { searchViewModel.search(searchState.query) },
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    SearchResults(
                        results = searchResults,
                        query = searchState.query,
                        onMessageClick = onMessageClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Versión simplificada para navegación general
    SearchScreen(
        chatId = "",
        onNavigateBack = onNavigateBack,
        onMessageClick = { /* No-op para búsqueda general */ },
        modifier = modifier
    )
}