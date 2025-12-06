package com.yerayyas.chatappkotlinproject.domain.usecases.chat.unified

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
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
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage
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
import org.mockito.kotlin.verifyNoMoreInteractions
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

    // System Under Test
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

    // --- INDIVIDUAL TESTS ---

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
        verify(sendImageMessageUseCase, never()).invoke(any(), any())
        verifyNoMoreInteractions(sendGroupMessageUseCase)
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
            messageText = null, // Text is ignored if URI is present in current logic (or is empty)
            imageUri = mockUri,
            replyTo = null
        )

        // Then
        verify(sendImageMessageUseCase).invoke(chatId, mockUri)
        verify(sendTextMessageUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `invoke sends TEXT REPLY to INDIVIDUAL chat`() = runTest {
        // Given
        val chatId = "user_123"
        val text = "Reply Text"
        // Create a fake original message to reply to
        val originalMessage = ChatMessage(id = "msg_original", message = "Original")
        val unifiedReply = UnifiedMessage.Individual(originalMessage)

        // When
        sendUnifiedMessageUseCase(
            chatId = chatId,
            chatType = ChatType.Individual,
            messageText = text,
            imageUri = null,
            replyTo = unifiedReply
        )

        // Then
        verify(sendTextMessageReplyUseCase).invoke(chatId, text, originalMessage)
        verify(sendTextMessageUseCase, never()).invoke(any(), any())
    }

    // --- GROUP TESTS ---

    @Test
    fun `invoke sends TEXT to GROUP chat requires current user info`() = runTest {
        // Given
        val groupId = "group_123"
        val text = "Hello Group"
        val myUserId = "me_123"
        val myUser = User(id = myUserId, username = "Yeray", email = "test@test.com", profileImage = "")

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
            senderImageUrl = eq(""), // Assuming it comes empty in the User object above
            mentionedUsers = any(), // Or null, depending on your default implementation
            replyToMessageId = isNull(),
            replyToMessage = isNull()
        )
    }

    @Test
    fun `invoke sends IMAGE to GROUP chat`() = runTest {
        // Given
        val groupId = "group_123"
        val mockUri: Uri = mock()
        val myUserId = "me_123"
        // Note: Ensure User has a default value for profileImage if use case sends ""
        val myUser = User(id = myUserId, username = "Yeray", profileImage = "")

        whenever(getCurrentUserIdUseCase()).thenReturn(myUserId)
        whenever(userRepository.getCurrentUser()).thenReturn(myUser)

        // When
        sendUnifiedMessageUseCase(
            chatId = groupId,
            chatType = ChatType.Group,
            messageText = null,
            imageUri = mockUri,
            replyTo = null
        )

        // Then
        // FIX: The order of arguments must match the real implementation (Data Layer)
        // Based on previous error log: groupId -> senderId -> senderName -> senderImageUrl -> imageUri -> replyToMessage
        verify(sendGroupImageMessageUseCase).invoke(
            groupId = eq(groupId),
            senderId = eq(myUserId),
            senderName = eq("Yeray"),
            senderImageUrl = any(), // The log showed that "" was arriving
            imageUri = eq(mockUri),
            replyToMessage = isNull()
        )
    }

    @Test
    fun `invoke sends TEXT REPLY to GROUP chat`() = runTest {
        // Given
        val groupId = "group_123"
        val text = "Reply to group"
        val myUserId = "me_123"
        val myUser = User(id = myUserId, username = "Yeray")

        // Original group message
        val originalGroupMsg = GroupMessage(id = "msg_group_orig", message = "Original Group Msg")
        val unifiedReply = UnifiedMessage.Group(originalGroupMsg)

        whenever(getCurrentUserIdUseCase()).thenReturn(myUserId)
        whenever(userRepository.getCurrentUser()).thenReturn(myUser)

        // When
        sendUnifiedMessageUseCase(
            chatId = groupId,
            chatType = ChatType.Group,
            messageText = text,
            imageUri = null,
            replyTo = unifiedReply
        )

        // Then
        verify(sendGroupMessageUseCase).sendTextMessage(
            groupId = eq(groupId),
            message = eq(text),
            senderName = eq("Yeray"),
            senderImageUrl = any(),
            mentionedUsers = any(),
            replyToMessageId = eq("msg_group_orig"),
            replyToMessage = eq(originalGroupMsg)
        )
    }
}
