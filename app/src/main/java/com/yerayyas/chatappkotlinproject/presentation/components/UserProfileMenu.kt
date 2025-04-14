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
 * Displays a user action menu for a chat, including options to view the profile,
 * report the user, or block the user.
 *
 * This composable shows a vertical three-dot icon button. When clicked, it expands
 * a dropdown menu with user-related actions. It uses a [NavHostController] to
 * navigate to the other user's profile screen.
 *
 * @param navController The navigation controller used to perform navigation actions.
 * @param userId The ID of the user for whom the actions apply.
 * @param username The username of the user, used when navigating to the profile.
 */
@Composable
fun UserStatusAndActions(
    navController: NavHostController,
    userId: String,
    username: String
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.see_profile)) },
                onClick = {
                    showMenu = false
                    navController.navigate(Routes.OtherUsersProfile.createRoute(userId, username))
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.report_user)) },
                onClick = { showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.block_user)) },
                onClick = { showMenu = false }
            )
        }
    }
}
