package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupDetails
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class LoadGroupDetailsUseCaseTest {

    @Mock
    private lateinit var groupChatRepository: GroupChatRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var useCase: LoadGroupDetailsUseCase

    @Before
    fun setUp() {
        useCase = LoadGroupDetailsUseCase(groupChatRepository, userRepository)
    }

    @Test
    fun `invoke should return success with details when group exists`() {
        runBlocking {
            // Given
            val groupId = "group-123"
            val memberIds = listOf("user-1", "user-2")
            val group = GroupChat(id = groupId, memberIds = memberIds)
            val members = listOf(
                User(id = "user-1", username = "Alice"),
                User(id = "user-2", username = "Bob")
            )
            val expectedDetails = GroupDetails(group, members)

            whenever(groupChatRepository.getGroupById(groupId)).thenReturn(group)
            whenever(userRepository.getUsersByIds(memberIds)).thenReturn(members)

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(expectedDetails, result.getOrNull())
        }
    }

    @Test
    fun `invoke should return failure when group does not exist`() {
        runBlocking {
            // Given
            val groupId = "missing-group"
            whenever(groupChatRepository.getGroupById(groupId)).thenReturn(null)

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Group not found", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun `invoke should return failure when repository throws exception`() {
        runBlocking {
            // Given
            val groupId = "error-group"
            val exception = RuntimeException("DB Error")
            whenever(groupChatRepository.getGroupById(groupId)).thenThrow(exception)

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result.isFailure)
            assertEquals("DB Error", result.exceptionOrNull()?.message)
        }
    }
}