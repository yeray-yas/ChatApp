package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupSettings
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ManageGroupMembersUseCaseTest {

    @Mock
    private lateinit var repository: GroupChatRepository

    @Mock
    private lateinit var auth: FirebaseAuth

    @Mock
    private lateinit var sendGroupMessageUseCase: SendGroupMessageUseCase

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var useCase: ManageGroupMembersUseCase

    private val currentUserId = "currentUser"
    private val otherUserId = "otherUser"
    private val groupId = "groupId"

    @Before
    fun setUp() {
        whenever(auth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(currentUserId)
        useCase = ManageGroupMembersUseCase(repository, auth, sendGroupMessageUseCase)
    }

    //region addMember Tests
    @Test
    fun `addMember should succeed when user has permission`() {
        runBlocking {
            // Given
            val group = GroupChat(
                id = groupId,
                memberIds = listOf(currentUserId),
                adminIds = listOf(currentUserId),
                settings = GroupSettings(onlyAdminsCanAddMembers = true)
            )
            whenever(repository.getGroupById(groupId)).thenReturn(group)
            whenever(repository.addMemberToGroup(groupId, otherUserId)).thenReturn(Result.success(Unit))
            whenever(sendGroupMessageUseCase.sendSystemMessage(any(), any(), any())).thenReturn(Result.success(Unit))

            // When
            val result = useCase.addMember(groupId, otherUserId, "UserName", "AdminName")

            // Then
            assertTrue(result.isSuccess)
            verify(repository).addMemberToGroup(groupId, otherUserId)
            verify(sendGroupMessageUseCase).sendSystemMessage(eq(groupId), any(), any())
        }
    }

    @Test
    fun `addMember should fail when user does not have permission`() {
        runBlocking {
            // Given
            val group = GroupChat(
                id = groupId,
                memberIds = listOf(currentUserId),
                adminIds = emptyList(), // Not admin
                settings = GroupSettings(onlyAdminsCanAddMembers = true)
            )
            whenever(repository.getGroupById(groupId)).thenReturn(group)

            // When
            val result = useCase.addMember(groupId, otherUserId, "UserName", "AdminName")

            // Then
            assertTrue(result.isFailure)
            assertEquals("You don't have permission to add members", result.exceptionOrNull()?.message)
        }
    }
    //endregion

    //region removeMember Tests
    @Test
    fun `removeMember should succeed when user is admin`() {
        runBlocking {
             // Given
            val group = GroupChat(
                id = groupId,
                memberIds = listOf(currentUserId, otherUserId),
                adminIds = listOf(currentUserId),
                createdBy = "creatorId"
            )
            whenever(repository.getGroupById(groupId)).thenReturn(group)
            whenever(repository.removeMemberFromGroup(groupId, otherUserId)).thenReturn(Result.success(Unit))
            whenever(sendGroupMessageUseCase.sendSystemMessage(any(), any(), any())).thenReturn(Result.success(Unit))

            // When
            val result = useCase.removeMember(groupId, otherUserId, "UserName", "AdminName")

            // Then
            assertTrue(result.isSuccess)
            verify(repository).removeMemberFromGroup(groupId, otherUserId)
        }
    }

    @Test
    fun `removeMember should fail when trying to remove group creator`() {
        runBlocking {
            // Given
            val creatorId = "creatorId"
            val group = GroupChat(
                id = groupId,
                memberIds = listOf(currentUserId, creatorId),
                adminIds = listOf(currentUserId),
                createdBy = creatorId
            )
            whenever(repository.getGroupById(groupId)).thenReturn(group)

            // When
            val result = useCase.removeMember(groupId, creatorId, "CreatorName", "AdminName")

            // Then
            assertTrue(result.isFailure)
            assertEquals("Cannot remove the group creator", result.exceptionOrNull()?.message)
        }
    }
    //endregion

    //region promoteToAdmin Tests
    @Test
    fun `promoteToAdmin should succeed when current user is admin`() {
        runBlocking {
             // Given
            val group = GroupChat(
                id = groupId,
                memberIds = listOf(currentUserId, otherUserId),
                adminIds = listOf(currentUserId)
            )
            whenever(repository.getGroupById(groupId)).thenReturn(group)
            whenever(repository.makeAdmin(groupId, otherUserId)).thenReturn(Result.success(Unit))
            whenever(sendGroupMessageUseCase.sendSystemMessage(any(), any(), any())).thenReturn(Result.success(Unit))

            // When
            val result = useCase.promoteToAdmin(groupId, otherUserId, "UserName", "AdminName")

            // Then
            assertTrue(result.isSuccess)
            verify(repository).makeAdmin(groupId, otherUserId)
        }
    }
    //endregion
    
    //region leaveGroup Tests
    @Test
    fun `leaveGroup should fail if user is the creator`() {
        runBlocking {
            // Given
            val group = GroupChat(
                id = groupId,
                createdBy = currentUserId
            )
            whenever(repository.getGroupById(groupId)).thenReturn(group)

            // When
            val result = useCase.leaveGroup(groupId, "MyName")

            // Then
            assertTrue(result.isFailure)
            assertEquals("The group creator cannot leave. Must transfer ownership first.", result.exceptionOrNull()?.message)
        }
    }
    //endregion
}