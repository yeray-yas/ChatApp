package com.yerayyas.chatappkotlinproject.domain.usecases.chat.unified

import android.net.Uri
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.SendGroupImageMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.SendGroupMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage
import javax.inject.Inject

class SendUnifiedMessageUseCase @Inject constructor(
    // Individual Use Cases
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val sendTextMessageReplyUseCase: SendTextMessageReplyUseCase,
    private val sendImageMessageReplyUseCase: SendImageMessageReplyUseCase,

    // Group Use Cases
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val sendGroupImageMessageUseCase: SendGroupImageMessageUseCase,

    // Helpers
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        chatId: String,
        chatType: ChatType,
        messageText: String?,
        imageUri: Uri?,
        replyTo: UnifiedMessage? = null
    ) {
        if (messageText.isNullOrBlank() && imageUri == null) return

        when (chatType) {
            is ChatType.Individual -> {
                handleIndividualMessage(chatId, messageText, imageUri, replyTo)
            }
            is ChatType.Group -> {
                handleGroupMessage(chatId, messageText, imageUri, replyTo)
            }
        }
    }

    private suspend fun handleIndividualMessage(
        receiverId: String,
        text: String?,
        uri: Uri?,
        replyTo: UnifiedMessage?
    ) {
        val replyToChatMessage = (replyTo as? UnifiedMessage.Individual)?.message

        if (uri != null) {
            if (replyToChatMessage != null) {
                sendImageMessageReplyUseCase(receiverId, uri, replyToChatMessage)
            } else {
                sendImageMessageUseCase(receiverId, uri)
            }
        } else if (text != null) {
            if (replyToChatMessage != null) {
                sendTextMessageReplyUseCase(receiverId, text, replyToChatMessage)
            } else {
                sendTextMessageUseCase(receiverId, text)
            }
        }
    }

    private suspend fun handleGroupMessage(
        groupId: String,
        text: String?,
        uri: Uri?,
        replyTo: UnifiedMessage?
    ) {
        val currentUserId = getCurrentUserIdUseCase() ?: return

        val replyToGroupMessage = (replyTo as? UnifiedMessage.Group)?.message

        val currentUser = userRepository.getCurrentUser()
        val senderName = currentUser?.username ?: "Unknown"
        val senderImageUrl = currentUser?.profileImage

        if (uri != null) {
            sendGroupImageMessageUseCase(
                groupId = groupId,
                imageUri = uri,
                senderId = currentUserId,
                senderName = senderName,
                senderImageUrl = senderImageUrl,
                replyToMessage = replyToGroupMessage
            )
        } else if (text != null) {
            sendGroupMessageUseCase.sendTextMessage(
                groupId = groupId,
                message = text,
                senderName = senderName,
                senderImageUrl = senderImageUrl,
                replyToMessageId = replyToGroupMessage?.id,
                replyToMessage = replyToGroupMessage
            )
        }
    }
}