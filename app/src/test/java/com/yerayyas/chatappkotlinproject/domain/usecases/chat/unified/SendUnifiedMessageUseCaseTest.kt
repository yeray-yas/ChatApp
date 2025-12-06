package com.yerayyas.chatappkotlinproject.domain.usecases.chat.unified

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.SendGroupImageMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.SendGroupMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SendUnifiedMessageUseCaseTest {

    // Dependency mocks
    private val sendTextMessageUseCase: SendTextMessageUseCase = mock()
    private val sendImageMessageUseCase: SendImageMessageUseCase = mock()
    private val sendTextMessageReplyUseCase: SendTextMessageReplyUseCase = mock()
    private val sendImageMessageReplyUseCase: SendImageMessageReplyUseCase = mock()
    private val sendGroupMessageUseCase: SendGroupMessageUseCase = mock()
    private val sendGroupImageMessageUseCase: SendGroupImageMessageUseCase = mock()
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase = mock()
    private val userRepository: UserRepository = mock()
    private lateinit var sendUnifiedMessageUseCase: SendUnifiedMessageUseCase

    @Before
    fun setUp() {
        sendUnifiedMessageUseCase = SendUnifiedMessageUseCase(
            sendTextMessageUseCase,
            sendImageMessageUseCase,
            sendTextMessageReplyUseCase,
            sendImageMessageReplyUseCase,
            sendGroupMessageUseCase,
            sendGroupImageMessageUseCase,
            getCurrentUserIdUseCase,
            userRepository
        )
    }

    @Test
    fun `invoke sends TEXT to INDIVIDUAL chat when uri is null`() = runTest {
        // Given
        val chatId = "user_123"
        val text = "Hello User"

        // When
        sendUnifiedMessageUseCase(
            chatId = chatId,
            chatType = ChatType.Individual,
            messageText = text,
            imageUri = null,
            replyTo = null
        )

        // Then
        verify(sendTextMessageUseCase).invoke(chatId, text)
        verify(sendGroupMessageUseCase, never()).sendTextMessage(any(), any(), any(), any(), any(), any(), any())
        verify(sendImageMessageUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `invoke sends TEXT to GROUP chat requires current user info`() = runTest {
        // Given
        val groupId = "group_123"
        val text = "Hello Group"
        val myUserId = "me_123"
        val myUser = User(id = myUserId, username = "Yeray", email = "test@test.com")

        whenever(getCurrentUserIdUseCase()).thenReturn(myUserId)
        whenever(userRepository.getCurrentUser()).thenReturn(myUser)

        // When
        sendUnifiedMessageUseCase(
            chatId = groupId,
            chatType = ChatType.Group,
            messageText = text,
            imageUri = null,
            replyTo = null
        )

        // Then
        verify(sendGroupMessageUseCase).sendTextMessage(
            groupId = eq(groupId),
            message = eq(text),
            senderName = eq("Yeray"),
            senderImageUrl = eq(""),
            mentionedUsers = any(),
            replyToMessageId = isNull(),
            replyToMessage = isNull()
        )
    }


    @Test
    fun `invoke sends IMAGE to INDIVIDUAL chat when uri is present`() = runTest {
        // Given
        val chatId = "user_123"
        val mockUri: Uri = mock()

        // When
        sendUnifiedMessageUseCase(
            chatId = chatId,
            chatType = ChatType.Individual,
            messageText = null,
            imageUri = mockUri,
            replyTo = null
        )

        // Then
        verify(sendImageMessageUseCase).invoke(chatId, mockUri)
        verify(sendTextMessageUseCase, never()).invoke(any(), any())
    }
}
