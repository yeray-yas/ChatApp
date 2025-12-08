package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.data.model.GroupActivityType
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class SendGroupMessageUseCaseTest {

    @Mock
    private lateinit var groupRepository: GroupChatRepository

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    @Mock
    private lateinit var uri: Uri

    private lateinit var sendGroupMessageUseCase: SendGroupMessageUseCase

    private val currentUserId = "currentUserId"
    private val groupId = "testGroupId"

    @Before
    fun setUp() {
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(currentUserId)
        sendGroupMessageUseCase = SendGroupMessageUseCase(groupRepository, firebaseAuth)
    }

    //region sendTextMessage tests
    @Test
    fun `sendTextMessage should succeed with valid data`() = runBlocking {
        // Given
        val message = "Hello, World!"
        val senderName = "Test User"
        whenever(groupRepository.sendMessageToGroup(any(), any())).thenReturn(Result.success(Unit))

        // When
        val result = sendGroupMessageUseCase.sendTextMessage(groupId, message, senderName)

        // Then
        assertTrue(result.isSuccess)
        val messageCaptor = argumentCaptor<GroupMessage>()
        verify(groupRepository).sendMessageToGroup(any(), messageCaptor.capture())
        assertEquals(message, messageCaptor.firstValue.message)
        assertEquals(senderName, messageCaptor.firstValue.senderName)
        assertEquals(currentUserId, messageCaptor.firstValue.senderId)
    }

    @Test
    fun `sendTextMessage should fail when user is not authenticated`() = runBlocking {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(null)

        // When
        val result = sendGroupMessageUseCase.sendTextMessage(groupId, "test", "sender")

        // Then
        assertTrue(result.isFailure)
        assertEquals("User not authenticated", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendTextMessage should fail with empty message`() = runBlocking {
        // When
        val result = sendGroupMessageUseCase.sendTextMessage(groupId, "", "sender")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Message cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendTextMessage should fail with message exceeding max length`() = runBlocking {
        // Given
        val longMessage = "a".repeat(1001)

        // When
        val result = sendGroupMessageUseCase.sendTextMessage(groupId, longMessage, "sender")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Message cannot exceed 1000 characters", result.exceptionOrNull()?.message)
    }
    //endregion

    //region sendImageMessage tests
    @Test
    fun `sendImageMessage should succeed with valid data`() = runBlocking {
        // Given
        val caption = "Test image"
        val senderName = "Test User"
        val imageUrl = "http://example.com/image.jpg"
        whenever(groupRepository.uploadGroupMessageImage(any(), any())).thenReturn(Result.success(imageUrl))
        whenever(groupRepository.sendMessageToGroup(any(), any())).thenReturn(Result.success(Unit))


        // When
        val result = sendGroupMessageUseCase.sendImageMessage(groupId, uri, caption, senderName)

        // Then
        assertTrue(result.isSuccess)
        val messageCaptor = argumentCaptor<GroupMessage>()
        verify(groupRepository).sendMessageToGroup(any(), messageCaptor.capture())
        assertEquals(caption, messageCaptor.firstValue.message)
        assertEquals(imageUrl, messageCaptor.firstValue.imageUrl)
        assertEquals(currentUserId, messageCaptor.firstValue.senderId)
    }

    @Test
    fun `sendImageMessage should fail if image upload fails`() = runBlocking {
        // Given
        val exception = Exception("Upload failed")
        whenever(groupRepository.uploadGroupMessageImage(any(), any())).thenReturn(Result.failure(exception))

        // When
        val result = sendGroupMessageUseCase.sendImageMessage(groupId, uri, "caption", "sender")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Upload failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendImageMessage should fail with caption exceeding max length`() = runBlocking {
        // Given
        val longCaption = "a".repeat(1001)

        // When
        val result = sendGroupMessageUseCase.sendImageMessage(groupId, uri, longCaption, "sender")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Caption cannot exceed 1000 characters", result.exceptionOrNull()?.message)
    }
    //endregion

    //region sendSystemMessage tests
    @Test
    fun `sendSystemMessage should succeed`() = runBlocking {
        // Given
        val message = "User joined"
        whenever(groupRepository.sendMessageToGroup(any(), any())).thenReturn(Result.success(Unit))

        // When
        val result = sendGroupMessageUseCase.sendSystemMessage(groupId, message, GroupActivityType.USER_ADDED)

        // Then
        assertTrue(result.isSuccess)
        val messageCaptor = argumentCaptor<GroupMessage>()
        verify(groupRepository).sendMessageToGroup(any(), messageCaptor.capture())
        assertEquals(message, messageCaptor.firstValue.message)
        assertTrue(messageCaptor.firstValue.isSystemMessage)
        assertEquals(GroupActivityType.USER_ADDED, messageCaptor.firstValue.systemMessageType)
    }

    @Test
    fun `sendSystemMessage should fail when user is not authenticated`() = runBlocking {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(null)

        // When
        val result = sendGroupMessageUseCase.sendSystemMessage(groupId, "message", GroupActivityType.USER_ADDED)

        // Then
        assertTrue(result.isFailure)
        assertEquals("User not authenticated", result.exceptionOrNull()?.message)
    }
    //endregion
}