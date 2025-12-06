package com.yerayyas.chatappkotlinproject.domain.usecases.auth

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SignOutUseCaseTest {


    // We use relaxed=true so they don't fail if void methods are called without stubbing
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)

    // System Under Test (SUT)
    private lateinit var signOutUseCase: SignOutUseCase

    @Before
    fun setUp() {
        signOutUseCase = SignOutUseCase(firebaseAuth, userRepository)
    }

    @Test
    fun `invoke should set user offline, clear token and sign out successfully`() = runTest {
        // GIVEN
        coEvery { userRepository.updateUserStatus(any()) } returns Unit
        coEvery { userRepository.clearCurrentUserFCMToken() } returns Unit

        // WHEN
        signOutUseCase()

        // THEN
        // Verify that status was set to offline
        coVerify(exactly = 1) { userRepository.updateUserStatus("offline") }

        // Verify that FCM token was cleared
        coVerify(exactly = 1) { userRepository.clearCurrentUserFCMToken() }

        // Verify that the Firebase session was finally signed out
        verify(exactly = 1) { firebaseAuth.signOut() }
    }

    @Test
    fun `invoke should sign out even if updateStatus fails`() = runTest {
        // GIVEN (updateStatus fails by throwing an exception)
        coEvery { userRepository.updateUserStatus(any()) } throws RuntimeException("Network error")

        // WHEN
        signOutUseCase()

        // THEN
        // Should attempt to update status (even if it fails)
        coVerify(exactly = 1) { userRepository.updateUserStatus("offline") }

        // AND YET it should attempt to clear token and sign out (because of the try-catch block)
        coVerify(exactly = 1) { userRepository.clearCurrentUserFCMToken() }
        verify(exactly = 1) { firebaseAuth.signOut() }
    }

    @Test
    fun `invoke should sign out even if clearToken fails`() = runTest {
        // GIVEN (clearToken fails)
        coEvery { userRepository.clearCurrentUserFCMToken() } throws RuntimeException("Database error")

        // WHEN
        signOutUseCase()

        // THEN
        // Attempts to update status
        coVerify(exactly = 1) { userRepository.updateUserStatus("offline") }

        // Attempts to clear token (fails)
        coVerify(exactly = 1) { userRepository.clearCurrentUserFCMToken() }

        // BUT the final signOut must happen no matter what
        verify(exactly = 1) { firebaseAuth.signOut() }
    }
}