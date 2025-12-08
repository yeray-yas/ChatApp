package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class GetGroupByIdUseCaseTest {

    @Mock
    private lateinit var repository: GroupChatRepository

    private lateinit var useCase: GetGroupByIdUseCase

    @Before
    fun setUp() {
        useCase = GetGroupByIdUseCase(repository)
    }

    @Test
    fun `invoke should return group when id is valid and group exists`() {
        runBlocking {
            // Given
            val groupId = "valid-id"
            val expectedGroup = GroupChat(id = groupId, name = "Test Group")
            whenever(repository.getGroupById(groupId)).thenReturn(expectedGroup)

            // When
            val result = useCase(groupId)

            // Then
            assertEquals(expectedGroup, result)
            verify(repository).getGroupById(groupId)
        }
    }

    @Test
    fun `invoke should return null when group id is blank`() {
        runBlocking {
            // Given
            val groupId = ""

            // When
            val result = useCase(groupId)

            // Then
            assertNull(result)
            verifyNoInteractions(repository)
        }
    }

    @Test
    fun `invoke should return null when repository returns null`() {
        runBlocking {
            // Given
            val groupId = "non-existent-id"
            whenever(repository.getGroupById(groupId)).thenReturn(null)

            // When
            val result = useCase(groupId)

            // Then
            assertNull(result)
            verify(repository).getGroupById(groupId)
        }
    }
}