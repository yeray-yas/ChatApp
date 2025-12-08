package com.yerayyas.chatappkotlinproject.domain.usecases.chat.individual

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yerayyas.chatappkotlinproject.data.model.ChatListItem
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.Executor

@RunWith(MockitoJUnitRunner::class)
class GetUserChatsUseCaseTest {

    @Mock
    private lateinit var chatRepository: ChatRepository

    @Mock
    private lateinit var firebaseDatabase: FirebaseDatabase

    @Mock
    private lateinit var rootRef: DatabaseReference

    @Mock
    private lateinit var chatsRef: DatabaseReference

    @Mock
    private lateinit var messagesRef: DatabaseReference

    @Mock
    private lateinit var usersRef: DatabaseReference

    @Mock
    private lateinit var userRef: DatabaseReference

    @Mock
    private lateinit var publicRef: DatabaseReference

    @Mock
    private lateinit var usernameRef: DatabaseReference

    private lateinit var useCase: GetUserChatsUseCase
    private lateinit var mockedLog: MockedStatic<Log>

    private val currentUserId = "user1"
    private val otherUserId = "user2"
    private val chatId = "${currentUserId}_${otherUserId}"

    @Before
    fun setUp() {
        // Static mock for Log
        mockedLog = Mockito.mockStatic(Log::class.java)
        mockedLog.`when`<Int> { Log.d(any(), any()) }.thenReturn(0)
        mockedLog.`when`<Int> { Log.e(any(), any(), any()) }.thenReturn(0)

        // Firebase structure configuration
        whenever(firebaseDatabase.reference).thenReturn(rootRef)
        whenever(rootRef.child("Chats")).thenReturn(chatsRef)
        whenever(chatsRef.child("Messages")).thenReturn(messagesRef)

        // Configuration to retrieve username (Users/uid/public/username)
        whenever(rootRef.child("Users")).thenReturn(usersRef)
        whenever(usersRef.child(any())).thenReturn(userRef)
        whenever(userRef.child("public")).thenReturn(publicRef)
        whenever(publicRef.child("username")).thenReturn(usernameRef)

        useCase = GetUserChatsUseCase(chatRepository, firebaseDatabase)
    }

    @After
    fun tearDown() {
        mockedLog.close()
    }

    @Test
    fun `invoke should emit list of chat items when data changes`() = runBlocking {
        // Given
        whenever(chatRepository.getCurrentUserId()).thenReturn(currentUserId)

        // Mock Firebase listener
        val listenerCaptor = argumentCaptor<ValueEventListener>()
        doAnswer { 
            // When addValueEventListener is called, capture the listener but return nothing (Unit)
            null 
        }.whenever(messagesRef).addValueEventListener(listenerCaptor.capture())

        // Configure simulated Firebase data
        val chatsSnapshot = mock<DataSnapshot>()
        val singleChatSnapshot = mock<DataSnapshot>()
        val messageSnapshot = mock<DataSnapshot>()
        
        // Snapshot structure
        whenever(chatsSnapshot.children).thenReturn(listOf(singleChatSnapshot))
        whenever(singleChatSnapshot.key).thenReturn(chatId)
        
        // Messages within the chat
        whenever(singleChatSnapshot.children).thenReturn(listOf(messageSnapshot))
        
        // Message data
        val lastMessage = ChatMessage(
            message = "Hello",
            timestamp = 1000L,
            receiverId = currentUserId,
            readStatus = ReadStatus.SENT
        )
        whenever(messageSnapshot.getValue(ChatMessage::class.java)).thenReturn(lastMessage)

        // Create a mocked DataSnapshot containing the String "Alice"
        val usernameSnapshot = mock<DataSnapshot>()
        whenever(usernameSnapshot.getValue(String::class.java)).thenReturn("Alice")

        // Wrap that DataSnapshot in a Task
        val usernameTask = mockTask(usernameSnapshot)
        whenever(usernameRef.get()).thenReturn(usernameTask)

        // When
        // Launch flow collection in a parallel coroutine
        var resultList: List<ChatListItem>? = null
        val job = launch {
            resultList = useCase().first()
        }

        // Wait a bit until the listener is attached inside callbackFlow
        // This is necessary because the flow starts, reaches addValueEventListener, and waits there.
        // There is no clean way to "wait for the listener to be added" without exposing it, 
        // but in a sequential test like this, verify usually works if the thread advances.
        // Force a small advance or verify the call.
        
        // Verify the listener was added and get the instance
        // Use a timeout for safety
        try {
            withTimeout(1000) {
                while (listenerCaptor.allValues.isEmpty()) {
                    kotlinx.coroutines.delay(10)
                }
            }
        } catch (_: TimeoutCancellationException) {
            // Fallback if verify already captured it
        }
        
        verify(messagesRef).addValueEventListener(listenerCaptor.capture())
        
        // Trigger Firebase event
        listenerCaptor.firstValue.onDataChange(chatsSnapshot)

        // Then
        // Wait for the collection coroutine to finish (first() ends as soon as it receives the first element)
        job.join()

        // Verifications
        assertTrue(resultList != null)
        assertEquals(1, resultList!!.size)
        val item = resultList[0]
        assertEquals(chatId, item.chatId)
        assertEquals("Alice", item.otherUsername) // Verify the name was obtained from the mock task
        assertEquals("Hello", item.lastMessage)
    }

    @Test
    fun `invoke should emit empty list when current user is empty`() = runBlocking {
        // Given
        whenever(chatRepository.getCurrentUserId()).thenReturn("")

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // Helper to mock Firebase Tasks
    @Suppress("UNCHECKED_CAST")
    private fun <T> mockTask(result: T): Task<T> {
        // Use leniency so Mockito doesn't complain if we don't call isSuccessful
        val task = Mockito.mock(Task::class.java) as Task<T>
        
        // Lenient configurations in case the code under test calls them or not
        Mockito.lenient().`when`(task.isSuccessful).thenReturn(true)
        Mockito.lenient().`when`(task.result).thenReturn(result)
        
        // When addOnCompleteListener is called, execute the callback immediately
        // .await() uses addOnCompleteListener(Executor, OnCompleteListener)
        doAnswer { invocation ->
            val listener = invocation.arguments[1] as OnCompleteListener<T>
            listener.onComplete(task)
            task
        }.whenever(task).addOnCompleteListener(any<Executor>(), any())

        return task
    }
}