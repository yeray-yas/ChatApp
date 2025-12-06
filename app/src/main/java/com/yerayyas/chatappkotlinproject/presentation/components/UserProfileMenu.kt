package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes

/**
 * Displays a user action menu for a chat conversation.
 *
 * This component provides a dropdown menu with user-related actions accessible through
 * a vertical three-dot icon button. It integrates with the navigation system to allow
 * users to perform actions related to another user in a chat context.
 *
 * Key features:
 * - Three-dot menu icon with Material Design 3 styling
 * - Dropdown menu with user action options
 * - Navigation integration for profile viewing
 * - Localized text using string resources
 * - State management for menu visibility
 * - User-specific actions (profile, report, block)
 *
 * Available actions:
 * - View user profile (navigates to profile screen)
 * - Report user (placeholder functionality)
 * - Block user (placeholder functionality)
 *
 * @param navController The navigation controller used to perform navigation actions
 * @param userId The ID of the user for whom the actions apply
 * @param username The username of the user, used when navigating to the profile
 */
@Composable
fun UserStatusAndActions(
    navController: NavHostController,
    userId: String,
    username: String
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        UserMenuTrigger(
            onShowMenu = { showMenu = true }
        )

        UserActionsDropdown(
            showMenu = showMenu,
            onDismiss = { showMenu = false },
            onProfileClick = {
                showMenu = false
                navController.navigate(Routes.OtherUsersProfile.createRoute(userId, username))
            },
            onReportClick = {
                showMenu = false
                // TODO: Implement report user functionality
            },
            onBlockClick = {
                showMenu = false
                // TODO: Implement block user functionality
            }
        )
    }
}

/**
 * Menu trigger icon button component.
 *
 * This component renders the three-dot vertical menu icon that users click
 * to open the user actions dropdown menu.
 *
 * @param onShowMenu Callback invoked when the menu trigger is clicked
 */
@Composable
private fun UserMenuTrigger(onShowMenu: () -> Unit) {
    IconButton(onClick = onShowMenu) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "User options",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * User actions dropdown menu component.
 *
 * This component displays the dropdown menu with available user actions.
 * It handles menu state and provides callbacks for each action.
 *
 * @param showMenu Whether the menu should be visible
 * @param onDismiss Callback invoked when the menu should be dismissed
 * @param onProfileClick Callback invoked when profile option is selected
 * @param onReportClick Callback invoked when report option is selected
 * @param onBlockClick Callback invoked when block option is selected
 */
@Composable
private fun UserActionsDropdown(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    onProfileClick: () -> Unit,
    onReportClick: () -> Unit,
    onBlockClick: () -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.see_profile)) },
            onClick = onProfileClick
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.report_user)) },
            onClick = onReportClick
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.block_user)) },
            onClick = onBlockClick
        )
    }
}
