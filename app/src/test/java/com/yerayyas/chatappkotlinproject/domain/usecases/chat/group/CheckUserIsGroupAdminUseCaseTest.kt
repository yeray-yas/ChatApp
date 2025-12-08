package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class CheckUserIsGroupAdminUseCaseTest {

    @Mock
    private lateinit var groupRepository: GroupChatRepository

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var useCase: CheckUserIsGroupAdminUseCase

    private val currentUserId = "admin-user"
    private val otherUserId = "normal-user"
    private val groupId = "group-id"

    @Before
    fun setUp() {
        useCase = CheckUserIsGroupAdminUseCase(groupRepository, firebaseAuth)
    }

    @Test
    fun `invoke should return true when user is admin`() {
        runBlocking {
            // Given
            val group = GroupChat(id = groupId, adminIds = listOf(currentUserId))
            
            whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
            whenever(firebaseUser.uid).thenReturn(currentUserId)
            whenever(groupRepository.getGroupById(groupId)).thenReturn(group)

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result)
        }
    }

    @Test
    fun `invoke should return false when user is not admin`() {
        runBlocking {
            // Given
            val group = GroupChat(id = groupId, adminIds = listOf("another-admin"))
            
            whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
            whenever(firebaseUser.uid).thenReturn(otherUserId)
            whenever(groupRepository.getGroupById(groupId)).thenReturn(group)

            // When
            val result = useCase(groupId)

            // Then
            assertFalse(result)
        }
    }

    @Test
    fun `invoke should return false when user is not authenticated`() {
        runBlocking {
            // Given
            whenever(firebaseAuth.currentUser).thenReturn(null)

            // When
            val result = useCase(groupId)

            // Then
            assertFalse(result)
        }
    }

    @Test
    fun `invoke should return false when group does not exist`() {
        runBlocking {
            // Given
            whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
            whenever(firebaseUser.uid).thenReturn(currentUserId)
            whenever(groupRepository.getGroupById(groupId)).thenReturn(null)

            // When
            val result = useCase(groupId)

            // Then
            assertFalse(result)
        }
    }
}