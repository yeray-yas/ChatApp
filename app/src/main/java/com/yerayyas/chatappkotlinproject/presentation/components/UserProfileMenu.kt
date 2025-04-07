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