package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class LoadGroupMessagesUseCaseTest {

    @Mock
    private lateinit var repository: GroupChatRepository

    private lateinit var useCase: LoadGroupMessagesUseCase

    @Before
    fun setUp() {
        useCase = LoadGroupMessagesUseCase(repository)
    }

    @Test
    fun `invoke should return flow of messages from repository`() {
        // Given
        val groupId = "test-group"
        val expectedFlow: Flow<List<GroupMessage>> = flowOf(listOf())
        whenever(repository.getGroupMessages(groupId)).thenReturn(expectedFlow)

        // When
        val result = useCase(groupId)

        // Then
        assertEquals(expectedFlow, result)
        verify(repository).getGroupMessages(groupId)
    }
}