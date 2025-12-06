package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class SendTextMessageUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: SendTextMessageUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = SendTextMessageUseCase(repository)
    }

    @Test
    fun `invoke should call repository once with correct parameters`() = runTest {
        // Given
        val receiverId = "receiver_123"
        val message = "Hi, how are you?"

        // When
        useCase(receiverId, message)

        // Then
        // Verify that the repository was called exactly once with expected parameters
        verify(repository).sendTextMessage(receiverId, message)
        verifyNoMoreInteractions(repository)
    }
}
