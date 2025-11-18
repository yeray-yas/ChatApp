package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A reusable composable that displays an unread message count badge.
 *
 * This component renders a circular badge with a count, providing a consistent
 * visual indicator for unread items across the application.
 *
 * @param count The number to display in the badge.
 * @param modifier Optional modifier for custom layout.
 */
@Composable
fun UnreadCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}