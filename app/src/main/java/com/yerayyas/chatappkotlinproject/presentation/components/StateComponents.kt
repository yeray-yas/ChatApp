package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Loading state component with circular progress indicator.
 *
 * This component displays a centered loading indicator with an optional message.
 * It's designed to be used when data is being fetched or processed, providing
 * clear visual feedback to users about ongoing operations.
 *
 * Key features:
 * - Centered circular progress indicator with theme colors
 * - Customizable loading message
 * - Consistent spacing and typography
 * - Full-size container for proper centering
 *
 * @param modifier Optional [Modifier] for customizing layout and styling
 * @param message Loading message to display below the progress indicator
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

/**
 * Error state component with retry functionality.
 *
 * This component displays error information with an optional retry action button.
 * It provides a consistent error display pattern across the application with
 * proper visual hierarchy and user-friendly retry mechanisms.
 *
 * Key features:
 * - Error icon with theme-appropriate styling
 * - Scrollable content for long error messages
 * - Optional retry button with customizable text
 * - Consistent spacing and visual design
 * - Proper accessibility support
 *
 * @param message Error message to display to the user
 * @param modifier Optional [Modifier] for customizing layout and styling
 * @param icon Icon to display above the error message (default: error icon)
 * @param onRetry Optional callback invoked when retry button is pressed
 * @param retryText Text to display on the retry button
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Error,
    onRetry: (() -> Unit)? = null,
    retryText: String = "Retry"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = retryText)
            }
        }
    }
}

/**
 * Empty state component for displaying when no content is available.
 *
 * This component provides a user-friendly way to display empty states throughout
 * the application. It supports customizable messages, icons, and optional action
 * buttons to guide users on what to do next.
 *
 * Key features:
 * - Optional icon display with theme-appropriate styling
 * - Centered layout with proper spacing
 * - Customizable message and action button
 * - Consistent visual design with other state components
 * - Full-width action button for easy interaction
 *
 * @param message Message to display explaining the empty state
 * @param modifier Optional [Modifier] for customizing layout and styling
 * @param icon Optional icon to display above the message
 * @param actionText Optional text for the action button
 * @param onAction Optional callback invoked when action button is pressed
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = actionText)
            }
        }
    }
}