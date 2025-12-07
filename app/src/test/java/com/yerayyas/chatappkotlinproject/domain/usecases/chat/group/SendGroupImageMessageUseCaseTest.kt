package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SendGroupImageMessageUseCaseTest {

    @Mock
    private lateinit var repository: GroupChatRepository

    @Mock
    private lateinit var imageUri: Uri

    private lateinit var useCase: SendGroupImageMessageUseCase

    private val groupId = "test-group"
    private val senderId = "test-sender"
    private val senderName = "Test Sender"
    private val senderImageUrl = "http://example.com/sender.jpg"
    private val uploadedImageUrl = "http://example.com/uploaded.jpg"

    @Before
    fun setUp() {
        useCase = SendGroupImageMessageUseCase(repository)
    }

    @Test
    fun `invoke should succeed when image upload and message sending are successful`() = runBlocking {
        // Given
        whenever(repository.uploadGroupMessageImage(groupId, imageUri)).thenReturn(Result.success(uploadedImageUrl))
        whenever(repository.sendMessageToGroup(any(), any())).thenReturn(Result.success(Unit))

        // When
        val result = useCase(groupId, senderId, senderName, senderImageUrl, imageUri, null)

        // Then
        assertTrue(result.isSuccess)
        val messageCaptor = argumentCaptor<GroupMessage>()
        verify(repository).sendMessageToGroup(any(), messageCaptor.capture())

        val capturedMessage = messageCaptor.firstValue
        assertEquals(groupId, capturedMessage.groupId)
        assertEquals(senderId, capturedMessage.senderId)
        assertEquals(senderName, capturedMessage.senderName)
        assertEquals(senderImageUrl, capturedMessage.senderImageUrl)
        assertEquals(uploadedImageUrl, capturedMessage.imageUrl)
        assertEquals("Image", capturedMessage.message)
    }

    @Test
    fun `invoke should fail when image upload fails`() = runBlocking {
        // Given
        val exception = Exception("Upload failed")
        whenever(repository.uploadGroupMessageImage(groupId, imageUri)).thenReturn(Result.failure(exception))

        // When
        val result = useCase(groupId, senderId, senderName, senderImageUrl, imageUri, null)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Upload failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should correctly handle replyToMessage`() = runBlocking {
        // Given
        val replyMessage = GroupMessage(id = "reply-id", message = "This is a message to be replied to")
        whenever(repository.uploadGroupMessageImage(groupId, imageUri)).thenReturn(Result.success(uploadedImageUrl))
        whenever(repository.sendMessageToGroup(any(), any())).thenReturn(Result.success(Unit))

        // When
        val result = useCase(groupId, senderId, senderName, senderImageUrl, imageUri, replyMessage)

        // Then
        assertTrue(result.isSuccess)
        val messageCaptor = argumentCaptor<GroupMessage>()
        verify(repository).sendMessageToGroup(any(), messageCaptor.capture())

        val capturedMessage = messageCaptor.firstValue
        assertEquals(replyMessage.id, capturedMessage.replyToMessageId)
        assertEquals(replyMessage, capturedMessage.replyToMessage)
    }
}