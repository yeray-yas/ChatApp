package com.yerayyas.chatappkotlinproject.presentation.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.SearchMessagesUseCase
import com.yerayyas.chatappkotlinproject.presentation.components.SearchFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFilters: Boolean = false,
    val activeFilters: List<SearchFilter> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val searchMessagesUseCase: SearchMessagesUseCase
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _allMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    private val _searchResults = MutableStateFlow<List<ChatMessage>>(emptyList())
    val searchResults: StateFlow<List<ChatMessage>> = _searchResults.asStateFlow()

    fun initializeSearch(chatId: String) {
        if (chatId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    chatRepository.getMessages(chatId).collect { messages ->
                        _allMessages.value = messages
                        performSearch()
                    }
                } catch (e: Exception) {
                    _searchState.value = _searchState.value.copy(
                        error = "Error al cargar mensajes: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateQuery(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        performSearch()
    }

    fun search(query: String) {
        updateQuery(query)
    }

    fun toggleFilters() {
        _searchState.value = _searchState.value.copy(
            showFilters = !_searchState.value.showFilters
        )
    }

    fun toggleFilter(filter: SearchFilter) {
        val currentFilters = _searchState.value.activeFilters.toMutableList()

        if (currentFilters.contains(filter)) {
            currentFilters.remove(filter)
        } else {
            // Para filtros mutuamente excluyentes
            when (filter) {
                is SearchFilter.TextMessages -> {
                    currentFilters.removeAll { it is SearchFilter.ImageMessages }
                }

                is SearchFilter.ImageMessages -> {
                    currentFilters.removeAll { it is SearchFilter.TextMessages }
                }

                else -> { /* No hay exclusiones para otros filtros */
                }
            }
            currentFilters.add(filter)
        }

        _searchState.value = _searchState.value.copy(activeFilters = currentFilters)
        performSearch()
    }

    private fun performSearch() {
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(isLoading = true, error = null)

            try {
                val query = _searchState.value.query
                val filters = _searchState.value.activeFilters
                var results = _allMessages.value

                // Aplicar búsqueda por texto
                if (query.isNotBlank()) {
                    results = results.filter { message ->
                        message.message.contains(query, ignoreCase = true) ||
                                message.replyToMessage?.contains(query, ignoreCase = true) == true
                    }
                }

                // Aplicar filtros
                filters.forEach { filter ->
                    results = when (filter) {
                        is SearchFilter.TextMessages -> {
                            results.filter { it.messageType == MessageType.TEXT }
                        }

                        is SearchFilter.ImageMessages -> {
                            results.filter { it.messageType == MessageType.IMAGE }
                        }

                        is SearchFilter.RepliedMessages -> {
                            results.filter { it.isReply() }
                        }

                        is SearchFilter.BySender -> {
                            results.filter { it.senderId == filter.senderId }
                        }

                        is SearchFilter.DateRange -> {
                            results.filter { message ->
                                message.timestamp in filter.startTimestamp..filter.endTimestamp
                            }
                        }
                    }
                }

                // Ordenar por timestamp descendente (más recientes primero)
                results = results.sortedByDescending { it.timestamp }

                _searchResults.value = results
                _searchState.value = _searchState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _searchState.value = _searchState.value.copy(
                    error = "Error en la búsqueda: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearSearch() {
        _searchState.value = SearchState()
        _searchResults.value = emptyList()
    }
}