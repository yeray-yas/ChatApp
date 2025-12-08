package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class GetUnreadGroupMessagesCountUseCaseTest {

    @Mock
    private lateinit var groupRepository: GroupChatRepository

    @Mock
    private lateinit var auth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var useCase: GetUnreadGroupMessagesCountUseCase
    private lateinit var mockedLog: MockedStatic<Log>

    private val currentUserId = "test-user-id"

    @Before
    fun setUp() {
        // Mock static Log methods
        mockedLog = Mockito.mockStatic(Log::class.java)
        mockedLog.`when`<Int> { Log.d(any(), any()) }.thenReturn(0)
        mockedLog.`when`<Int> { Log.w(any(), any<String>()) }.thenReturn(0)

        useCase = GetUnreadGroupMessagesCountUseCase(groupRepository, auth)
    }

    @After
    fun tearDown() {
        mockedLog.close()
    }

    @Test
    fun `invoke should return 0 when user is not authenticated`() {
        runBlocking {
            // Given
            whenever(auth.currentUser).thenReturn(null)

            // When
            val result = useCase().first()

            // Then
            assertEquals(0, result)
        }
    }

    @Test
    fun `invoke should return 0 when user has no groups`() {
        runBlocking {
            // Given
            whenever(auth.currentUser).thenReturn(firebaseUser)
            whenever(firebaseUser.uid).thenReturn(currentUserId)
            whenever(groupRepository.getUserGroups(currentUserId)).thenReturn(flowOf(emptyList()))

            // When
            val result = useCase().first()

            // Then
            assertEquals(0, result)
        }
    }

    @Test
    fun `invoke should return sum of unread messages from all groups`() {
        runBlocking {
            // Given
            val group1 = GroupChat(id = "group1")
            val group2 = GroupChat(id = "group2")
            val groups = listOf(group1, group2)

            whenever(auth.currentUser).thenReturn(firebaseUser)
            whenever(firebaseUser.uid).thenReturn(currentUserId)
            whenever(groupRepository.getUserGroups(currentUserId)).thenReturn(flowOf(groups))

            // Mock unread counts for each group
            whenever(groupRepository.getUnreadMessagesCountForGroup("group1", currentUserId)).thenReturn(flowOf(3))
            whenever(groupRepository.getUnreadMessagesCountForGroup("group2", currentUserId)).thenReturn(flowOf(2))

            // When
            val result = useCase().first()

            // Then
            assertEquals(5, result) // 3 + 2 = 5
        }
    }
}