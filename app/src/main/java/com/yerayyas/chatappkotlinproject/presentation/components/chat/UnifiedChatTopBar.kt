package com.yerayyas.chatappkotlinproject.presentation.components.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import java.util.Locale

/**
 * A unified top app bar component for the chat screen that adapts its content and actions
 * based on whether the conversation is an individual chat or a group chat.
 *
 * This component provides consistent navigation (Back), essential actions (Search),
 * and context-specific information display (Chat Name, Group Members count).
 *
 * @param chatName The primary name of the chat partner or group (used as fallback).
 * @param chatType The type of the current chat, determining UI elements (Individual or Group).
 * @param groupInfo The data model containing detailed group information, non-null if [chatType] is [ChatType.Group].
 * @param onBackClick Callback triggered when the back navigation arrow is clicked.
 * @param onInfoClick Callback triggered when the info icon is clicked (typically navigates to profile/group settings).
 * @param onSearchClick Callback triggered when the search icon is clicked to find messages.
 * @param userActions A composable slot for specific individual chat actions or status indicators.
 * @param modifier The modifier to be applied to the TopAppBar container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedChatTopBar(
    chatName: String,
    chatType: ChatType,
    groupInfo: com.yerayyas.chatappkotlinproject.data.model.GroupChat?,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    onSearchClick: () -> Unit,
    userActions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            when (chatType) {
                is ChatType.Individual -> {
                    Text(
                        text = chatName.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT)
                            else it.toString()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                is ChatType.Group -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = groupInfo?.name ?: chatName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (groupInfo != null) {
                            Text(
                                text = "${groupInfo.memberIds.size} members",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search messages"
                )
            }

            when (chatType) {
                is ChatType.Group -> {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Group info"
                        )
                    }
                }

                is ChatType.Individual -> {
                    userActions()
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}