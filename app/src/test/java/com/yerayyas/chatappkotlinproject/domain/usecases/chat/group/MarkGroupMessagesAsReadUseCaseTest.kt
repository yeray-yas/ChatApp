package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class MarkGroupMessagesAsReadUseCaseTest {

    @Mock
    private lateinit var groupChatRepository: GroupChatRepository

    @Mock
    private lateinit var auth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var useCase: MarkGroupMessagesAsReadUseCase

    private val groupId = "test-group-id"
    private val userId = "test-user-id"

    @Before
    fun setUp() {
        useCase = MarkGroupMessagesAsReadUseCase(groupChatRepository, auth)
        // By default, we simulate a logged-in user, unless otherwise specified
        whenever(auth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(userId)
    }

    @Test
    fun `invoke should succeed when user is authenticated`() {
        runBlocking {
            // Given
            whenever(groupChatRepository.markGroupMessagesAsRead(groupId, userId)).thenReturn(Result.success(Unit))

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result.isSuccess)
            verify(groupChatRepository).markGroupMessagesAsRead(groupId, userId)
        }
    }

    @Test
    fun `invoke should fail when user is not authenticated`() {
        runBlocking {
            // Given
            whenever(auth.currentUser).thenReturn(null)

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result.isFailure)
            assertEquals("User not authenticated", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun `invoke should fail when repository fails`() {
        runBlocking {
            // Given
            val exception = Exception("Repository error")
            whenever(groupChatRepository.markGroupMessagesAsRead(groupId, userId)).thenReturn(Result.failure(exception))

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Repository error", result.exceptionOrNull()?.message)
        }
    }
}