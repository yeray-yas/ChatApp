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

    /**
     * Initializes the search functionality for a specific chat.
     *
     * Loads all messages from the specified chat and sets up the message collection
     * for search operations. This method should be called when entering the search screen.
     *
     * @param chatId The ID of the chat to search within
     */
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
                        error = "Error loading messages: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Updates the search query and triggers a new search.
     *
     * @param query The new search query text
     */
    fun updateQuery(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        performSearch()
    }

    /**
     * Executes a search with the provided query.
     *
     * @param query The search query text
     */
    fun search(query: String) {
        updateQuery(query)
    }

    /**
     * Toggles the visibility of the filters section.
     */
    fun toggleFilters() {
        _searchState.value = _searchState.value.copy(
            showFilters = !_searchState.value.showFilters
        )
    }

    /**
     * Toggles a specific filter on or off.
     *
     * Handles mutually exclusive filters (e.g., TextMessages and ImageMessages)
     * by automatically removing conflicting filters when a new one is applied.
     *
     * @param filter The filter to toggle
     */
    fun toggleFilter(filter: SearchFilter) {
        val currentFilters = _searchState.value.activeFilters.toMutableList()

        if (currentFilters.contains(filter)) {
            currentFilters.remove(filter)
        } else {
            // Handle mutually exclusive filters
            when (filter) {
                is SearchFilter.TextMessages -> {
                    currentFilters.removeAll { it is SearchFilter.ImageMessages }
                }

                is SearchFilter.ImageMessages -> {
                    currentFilters.removeAll { it is SearchFilter.TextMessages }
                }

                else -> { /* No exclusions for other filters */
                }
            }
            currentFilters.add(filter)
        }

        _searchState.value = _searchState.value.copy(activeFilters = currentFilters)
        performSearch()
    }

    /**
     * Performs the actual search operation.
     *
     * Applies text search and filters to the loaded messages, then updates the search results.
     * The search is case-insensitive and includes both message content and reply content.
     * Results are sorted by timestamp in descending order (most recent first).
     */
    private fun performSearch() {
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(isLoading = true, error = null)

            try {
                val query = _searchState.value.query
                val filters = _searchState.value.activeFilters
                var results = _allMessages.value

                // Apply text search
                if (query.isNotBlank()) {
                    results = results.filter { message ->
                        message.message.contains(query, ignoreCase = true) ||
                                message.replyToMessage?.contains(query, ignoreCase = true) == true
                    }
                }

                // Apply filters
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

                // Sort by timestamp descending (most recent first)
                results = results.sortedByDescending { it.timestamp }

                _searchResults.value = results
                _searchState.value = _searchState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _searchState.value = _searchState.value.copy(
                    error = "Search error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Clears the current search state and results.
     *
     * Resets the search query, filters, and results to their initial state.
     */
    fun clearSearch() {
        _searchState.value = SearchState()
        _searchResults.value = emptyList()
    }
}