package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yerayyas.chatappkotlinproject.data.model.ChatListItem
import com.yerayyas.chatappkotlinproject.utils.formatTimestamp
/**
 * A Composable that displays a single chat conversation preview in a list.
 *
 * This component is designed to be a self-contained row in a chat list, showing
 * essential information like the participant's name, the last message, a timestamp,
 * and a badge for unread messages. It's built for reusability and follows Material Design 3 guidelines.
 *
 * Key features:
 * - **Data-driven**: Populated by a [ChatListItem] model.
 * - **Interactive**: The entire card is clickable, with a callback for navigation.
 * - **Informative**: Clearly displays unread count and a relative timestamp.
 * - **Accessible**: Provides meaningful content descriptions for UI elements (implicitly via Text).
 * - **Consistent Styling**: Uses [MaterialTheme] for colors and typography.
 *
 * @param chat The [ChatListItem] data model containing the information to display.
 * @param onClick A lambda function that is invoked when the user clicks on the chat item.
 * @param modifier An optional [Modifier] to be applied to the root Card element.
 */
@Composable
fun ChatListItem(
    chat: ChatListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Future: Add Avatar Composable here
            // e.g., UserAvatar(user = ...)

            // Column for Name, Last Message, and Badge
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Row for Username and Unread Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false) // Prevents pushing timestamp
                    ) {
                        Text(
                            text = chat.otherUsername,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            UnreadCountBadge(count = chat.unreadCount)
                        }
                    }
                    // Timestamp aligned to the right of the username row
                    Text(
                        text = formatTimestamp(chat.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
