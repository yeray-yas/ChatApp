package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class CreateGroupUseCaseTest {

    @Mock
    private lateinit var groupRepository: GroupChatRepository

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    @Mock
    private lateinit var imageUri: Uri

    private lateinit var useCase: CreateGroupUseCase
    private lateinit var mockedLog: MockedStatic<Log>

    private val currentUserId = "creator-id"

    @Before
    fun setUp() {
        // Mock Log
        mockedLog = Mockito.mockStatic(Log::class.java)
        mockedLog.`when`<Int> { Log.w(any(), any<String>()) }.thenReturn(0)

        useCase = CreateGroupUseCase(groupRepository, firebaseAuth)
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(currentUserId)
    }

    @After
    fun tearDown() {
        mockedLog.close()
    }

    @Test
    fun `invoke should create group successfully with valid data`() {
        runBlocking {
            // Given
            val name = "My Group"
            val description = "Description"
            val memberIds = listOf("member1", "member2")
            val newGroupId = "new-group-id"

            whenever(groupRepository.createGroup(any())).thenReturn(Result.success(newGroupId))
            whenever(groupRepository.updateGroupImage(any(), any())).thenReturn(Result.success("url"))

            // When
            val result = useCase(name, description, memberIds, imageUri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(newGroupId, result.getOrNull())

            // Verify group structure
            val groupCaptor = argumentCaptor<GroupChat>()
            verify(groupRepository).createGroup(groupCaptor.capture())
            val capturedGroup = groupCaptor.firstValue

            assertEquals(name, capturedGroup.name)
            assertEquals(description, capturedGroup.description)
            assertTrue(capturedGroup.memberIds.contains(currentUserId)) // Creator added
            assertTrue(capturedGroup.adminIds.contains(currentUserId)) // Creator is admin
            assertEquals(currentUserId, capturedGroup.createdBy)
        }
    }

    @Test
    fun `invoke should fail when user is not authenticated`() {
        runBlocking {
            // Given
            whenever(firebaseAuth.currentUser).thenReturn(null)

            // When
            val result = useCase("Name", "Desc", listOf("member1"))

            // Then
            assertTrue(result.isFailure)
            assertEquals("User not authenticated", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun `invoke should fail when name is empty`() {
        runBlocking {
            // When
            val result = useCase("", "Desc", listOf("member1"))

            // Then
            assertTrue(result.isFailure)
            assertEquals("Group name cannot be empty", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun `invoke should fail when members list is empty`() {
        runBlocking {
            // When
            val result = useCase("Name", "Desc", emptyList())

            // Then
            assertTrue(result.isFailure)
            assertEquals("Must add at least one member to the group", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun `invoke should succeed even if image upload fails`() {
        runBlocking {
            // Given
            val newGroupId = "group-id"
            whenever(groupRepository.createGroup(any())).thenReturn(Result.success(newGroupId))
            whenever(groupRepository.updateGroupImage(any(), any())).thenReturn(Result.failure(Exception("Upload error")))

            // When
            val result = useCase("Name", "Desc", listOf("member1"), imageUri)

            // Then
            assertTrue(result.isSuccess) // Should still succeed
            assertEquals(newGroupId, result.getOrNull())
            verify(groupRepository).createGroup(any())
            verify(groupRepository).updateGroupImage(newGroupId, imageUri)
        }
    }
}