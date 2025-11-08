package com.yerayyas.chatappkotlinproject.infrastructure.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.yerayyas.chatappkotlinproject.domain.service.PendingIntentFactory
import com.yerayyas.chatappkotlinproject.presentation.activity.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-specific implementation of [PendingIntentFactory].
 *
 * This class creates Android PendingIntents for notification actions,
 * keeping the Android-specific dependencies isolated from the domain layer.
 *
 * Supports both individual and group chat notifications.
 */
@Singleton
class AndroidPendingIntentFactory @Inject constructor(
    @ApplicationContext private val context: Context
) : PendingIntentFactory {

    override fun createChatPendingIntent(
        senderId: String,
        senderName: String,
        chatId: String,
        isGroupMessage: Boolean,
        groupName: String?
    ): Any {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            if (isGroupMessage) {
                // Navigate to group chat
                putExtra("navigateTo", "group_chat")
                putExtra("groupId", chatId)
                putExtra("groupName", groupName ?: "Group")
                putExtra("senderId", senderId) // Still include sender info for context
                putExtra("senderName", senderName)
            } else {
                // Navigate to individual chat (original behavior)
                putExtra("navigateTo", "chat")
                putExtra("userId", senderId)
                putExtra("username", senderName)
                putExtra("chatId", chatId)
            }
        }

        // Ensure a unique request code for each sender/chat combination to avoid PendingIntent collisions
        val requestCode =
            (senderId + chatId + if (isGroupMessage) "group" else "individual").hashCode()

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
