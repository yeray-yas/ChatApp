package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class GetUserGroupsUseCaseTest {

    @Mock
    private lateinit var groupRepository: GroupChatRepository

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var useCase: GetUserGroupsUseCase

    private val currentUserId = "test-user-id"

    @Before
    fun setUp() {
        useCase = GetUserGroupsUseCase(groupRepository, firebaseAuth)
    }

    @Test
    fun `invoke should return user groups when authenticated`() {
        runBlocking {
            // Given
            val groups = listOf(GroupChat(id = "group1"), GroupChat(id = "group2"))
            val expectedFlow: Flow<List<GroupChat>> = flowOf(groups)
            
            whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
            whenever(firebaseUser.uid).thenReturn(currentUserId)
            whenever(groupRepository.getUserGroups(currentUserId)).thenReturn(expectedFlow)

            // When
            val resultFlow = useCase()

            // Then
            assertEquals(groups, resultFlow.first())
        }
    }

    @Test
    fun `invoke should return empty list when not authenticated`() {
        runBlocking {
            // Given
            whenever(firebaseAuth.currentUser).thenReturn(null)

            // When
            val resultFlow = useCase()

            // Then
            val resultList = resultFlow.first()
            assertEquals(0, resultList.size)
        }
    }
}