package com.yerayyas.chatappkotlinproject.presentation.screens.chat


import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.IndividualAndGroupChatViewModel

/**
 * Individual chat screen using the unified implementation
 */
@Composable
fun IndividualChatScreen(
    userId: String,
    username: String,
    navController: NavHostController,
    viewModel: IndividualAndGroupChatViewModel = hiltViewModel()
) {
    UnifiedChatScreen(
        chatId = userId,
        chatType = ChatType.Individual,
        chatName = username,
        navController = navController,
        viewModel = viewModel
    )
}

/**
 * Group chat screen using the unified implementation
 */
@Composable
fun GroupChatScreenUnified(
    groupId: String,
    groupName: String = "Group", // Default name
    navController: NavHostController,
    viewModel: IndividualAndGroupChatViewModel = hiltViewModel()
) {
    UnifiedChatScreen(
        chatId = groupId,
        chatType = ChatType.Group,
        chatName = groupName,
        navController = navController,
        viewModel = viewModel
    )
}