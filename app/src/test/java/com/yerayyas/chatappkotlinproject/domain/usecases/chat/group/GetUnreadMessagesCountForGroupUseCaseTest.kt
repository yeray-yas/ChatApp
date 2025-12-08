package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class GetUnreadMessagesCountForGroupUseCaseTest {

    @Mock
    private lateinit var groupRepository: GroupChatRepository

    @Mock
    private lateinit var auth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var useCase: GetUnreadMessagesCountForGroupUseCase
    private lateinit var mockedLog: MockedStatic<Log>

    private val groupId = "test-group-id"
    private val userId = "test-user-id"

    @Before
    fun setUp() {
        // Mock static Log methods to prevent RuntimeException
        mockedLog = Mockito.mockStatic(Log::class.java)
        mockedLog.`when`<Int> { Log.d(any(), any()) }.thenReturn(0)
        mockedLog.`when`<Int> { Log.w(any(), any<String>()) }.thenReturn(0)

        useCase = GetUnreadMessagesCountForGroupUseCase(groupRepository, auth)
    }

    @After
    fun tearDown() {
        // Close the static mock to avoid memory leaks and interfere with other tests
        mockedLog.close()
    }

    @Test
    fun `invoke should return count from repository when user is authenticated`() {
        runBlocking {
            // Given
            val expectedCount = 5
            val countFlow: Flow<Int> = flowOf(expectedCount)
            
            whenever(auth.currentUser).thenReturn(firebaseUser)
            whenever(firebaseUser.uid).thenReturn(userId)
            whenever(groupRepository.getUnreadMessagesCountForGroup(groupId, userId)).thenReturn(countFlow)

            // When
            val resultFlow = useCase(groupId)

            // Then
            assertEquals(expectedCount, resultFlow.first())
            verify(groupRepository).getUnreadMessagesCountForGroup(groupId, userId)
        }
    }

    @Test
    fun `invoke should return 0 when user is not authenticated`() {
        runBlocking {
            // Given
            whenever(auth.currentUser).thenReturn(null)

            // When
            val resultFlow = useCase(groupId)

            // Then
            assertEquals(0, resultFlow.first())
        }
    }
}