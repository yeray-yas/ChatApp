package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType

/**
 * Advanced search bar component with comprehensive filter capabilities.
 *
 * This component provides a sophisticated search interface that includes text input,
 * search execution, and expandable filter options. It's designed to handle complex
 * search scenarios with multiple filter types and real-time query updates.
 *
 * Key features:
 * - Real-time query updates and search execution
 * - Expandable filter section with smooth animations
 * - Visual indicators for active filters
 * - Keyboard handling with proper IME actions
 * - Clear functionality for easy query reset
 * - Focus management and keyboard controller integration
 * - Customizable placeholder text and filter options
 *
 * The component maintains its own focus state and integrates with the system keyboard
 * to provide a seamless search experience.
 *
 * @param query Current text in the search input field
 * @param onQueryChange Callback invoked when the search query text changes
 * @param onSearch Callback invoked when search is executed (Enter key or search action)
 * @param modifier Optional [Modifier] for customizing layout and styling
 * @param placeholder Placeholder text displayed when search field is empty
 * @param showFilters Whether the expandable filter section is currently visible
 * @param onToggleFilters Callback to toggle the visibility of the filter section
 * @param activeFilters List of currently active search filters
 * @param onFilterChange Callback invoked when a filter is toggled on/off
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search messages...",
    showFilters: Boolean = false,
    onToggleFilters: () -> Unit = {},
    activeFilters: List<SearchFilter> = emptyList(),
    onFilterChange: (SearchFilter) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier) {
        SearchInputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            placeholder = placeholder,
            showFilters = showFilters,
            onToggleFilters = onToggleFilters,
            activeFilters = activeFilters,
            focusRequester = focusRequester,
            keyboardController = keyboardController
        )

        // Expandable filters section
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilterSection(
                activeFilters = activeFilters,
                onFilterChange = onFilterChange,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Search input field component with integrated controls.
 *
 * This component handles the text input, clear button, and filter toggle button
 * in a unified interface with proper keyboard handling.
 *
 * @param query Current search query text
 * @param onQueryChange Callback for query text changes
 * @param onSearch Callback for search execution
 * @param placeholder Placeholder text for the input field
 * @param showFilters Whether filters are currently shown
 * @param onToggleFilters Callback to toggle filter visibility
 * @param activeFilters List of active filters for visual indication
 * @param focusRequester Focus requester for input management
 * @param keyboardController Controller for keyboard operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: String,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    activeFilters: List<SearchFilter>,
    focusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingIcon = {
            SearchTrailingIcons(
                query = query,
                onQueryChange = onQueryChange,
                showFilters = showFilters,
                onToggleFilters = onToggleFilters,
                activeFilters = activeFilters
            )
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch(query)
                keyboardController?.hide()
            }
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Trailing icons for the search input field.
 *
 * This component manages the clear button and filter toggle button
 * in the trailing icon area of the search field.
 *
 * @param query Current query text
 * @param onQueryChange Callback to clear the query
 * @param showFilters Whether filters are shown
 * @param onToggleFilters Callback to toggle filters
 * @param activeFilters List of active filters for visual indication
 */
@Composable
private fun SearchTrailingIcons(
    query: String,
    onQueryChange: (String) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    activeFilters: List<SearchFilter>
) {
    Row {
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") }
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        IconButton(
            onClick = onToggleFilters
        ) {
            Icon(
                imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                contentDescription = if (showFilters) "Hide filters" else "Show filters",
                tint = if (activeFilters.isNotEmpty()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Filter section component with message type filters.
 *
 * Displays filter chips for different message types in an organized layout.
 * Provides visual feedback for selected filters and handles filter toggle actions.
 *
 * @param activeFilters List of currently active filters
 * @param onFilterChange Callback invoked when a filter is toggled
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@Composable
private fun FilterSection(
    activeFilters: List<SearchFilter>,
    onFilterChange: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Message type filters
            Text(
                text = "Message Type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilters.contains(SearchFilter.TextMessages),
                    onClick = { onFilterChange(SearchFilter.TextMessages) },
                    label = { Text("Text") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                FilterChip(
                    selected = activeFilters.contains(SearchFilter.ImageMessages),
                    onClick = { onFilterChange(SearchFilter.ImageMessages) },
                    label = { Text("Images") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * Search results display component.
 *
 * Shows search results in a scrollable list or displays an empty state when no results are found.
 * Each result item is interactive and provides visual context about the message type and content.
 *
 * Key features:
 * - Lazy loading for performance with large result sets
 * - Empty state with informative message and icon
 * - Clickable result items with message preview
 * - Visual indicators for message types and reply status
 * - Proper spacing and content padding
 *
 * @param results List of chat messages matching the search criteria
 * @param query Current search query for empty state display
 * @param onMessageClick Callback invoked when a search result is clicked
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@Composable
fun SearchResults(
    results: List<ChatMessage>,
    query: String,
    onMessageClick: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty() && query.isNotEmpty()) {
        EmptyState(
            message = "No messages found for \"$query\"",
            icon = Icons.Default.SearchOff,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { message ->
                SearchResultItem(
                    message = message,
                    query = query,
                    onClick = { onMessageClick(message) }
                )
            }
        }
    }
}

/**
 * Individual search result item component.
 *
 * Displays a single message result with contextual information including:
 * - Message type indicator (text/image icon)
 * - Timestamp with relative formatting
 * - Reply indicator if the message is a reply
 * - Message content preview with text overflow handling
 *
 * @param message The chat message to display
 * @param query Current search query (for potential highlighting in future)
 * @param onClick Callback invoked when the item is clicked
 * @param modifier Optional [Modifier] for customizing layout and styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultItem(
    message: ChatMessage,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (message.messageType == MessageType.IMAGE)
                            Icons.Default.Image else Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (message.isReply()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Available filters for search functionality.
 *
 * This sealed class defines the different types of filters that can be applied to search results:
 * - Message type filters (text, images, replies)
 * - Sender-based filtering
 * - Date range filtering for temporal searches
 *
 * Each filter type encapsulates its specific parameters and can be easily extended
 * with additional filter criteria in the future.
 */
sealed class SearchFilter {
    /** Filter for text-only messages */
    object TextMessages : SearchFilter()

    /** Filter for image messages */
    object ImageMessages : SearchFilter()

    /** Filter for messages that are replies to other messages */
    object RepliedMessages : SearchFilter()

    /** Filter messages by a specific sender */
    data class BySender(val senderId: String) : SearchFilter()

    /** Filter messages within a specific date range */
    data class DateRange(val startTimestamp: Long, val endTimestamp: Long) : SearchFilter()
}

/**
 * Formats timestamp to a human-readable relative time string.
 *
 * Provides user-friendly time representations:
 * - "Just now" for very recent messages
 * - Minutes/hours/days for recent messages
 * - Can be enhanced with more sophisticated date formatting
 *
 * @param timestamp The timestamp in milliseconds to format
 * @return A formatted, user-friendly time string
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}