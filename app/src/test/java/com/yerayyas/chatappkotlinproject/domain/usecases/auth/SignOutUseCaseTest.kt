package com.yerayyas.chatappkotlinproject.domain.usecases.auth

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.user.ManageUserPresenceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SignOutUseCaseTest {
    // Usamos relaxed=true para que no fallen si se llaman métodos sin stubbing previo
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val manageUserPresenceUseCase: ManageUserPresenceUseCase = mockk(relaxed = true)

    // System Under Test (SUT)
    private lateinit var signOutUseCase: SignOutUseCase

    @Before
    fun setUp() {
        signOutUseCase = SignOutUseCase(firebaseAuth, userRepository, manageUserPresenceUseCase)
    }

    @Test
    fun `invoke should stop presence updates, clear token and sign out successfully`() = runTest {
        // GIVEN
        coEvery { userRepository.clearCurrentUserFCMToken() } returns Unit

        // WHEN
        signOutUseCase()

        // THEN
        // Verificamos que se detengan las actualizaciones de presencia
        verify(exactly = 1) { manageUserPresenceUseCase.stopPresenceUpdates() }

        // Verificamos que el token FCM se limpie
        coVerify(exactly = 1) { userRepository.clearCurrentUserFCMToken() }

        // Verificamos el sign out final de Firebase
        verify(exactly = 1) { firebaseAuth.signOut() }
    }

    @Test
    fun `invoke should sign out even if stopPresenceUpdates fails`() = runTest {
        // GIVEN (stopPresenceUpdates falla lanzando una excepción)
        every { manageUserPresenceUseCase.stopPresenceUpdates() } throws RuntimeException("Firebase error")

        // WHEN
        signOutUseCase()

        // THEN
        verify(exactly = 1) { manageUserPresenceUseCase.stopPresenceUpdates() }

        // A pesar del error arriba, debe intentar limpiar token y cerrar sesión (gracias al try-catch)
        coVerify(exactly = 1) { userRepository.clearCurrentUserFCMToken() }
        verify(exactly = 1) { firebaseAuth.signOut() }
    }

    @Test
    fun `invoke should sign out even if clearToken fails`() = runTest {
        // GIVEN (clearToken falla)
        coEvery { userRepository.clearCurrentUserFCMToken() } throws RuntimeException("Database error")

        // WHEN
        signOutUseCase()

        // THEN
        verify(exactly = 1) { manageUserPresenceUseCase.stopPresenceUpdates() }
        coVerify(exactly = 1) { userRepository.clearCurrentUserFCMToken() }

        // El signOut final DEBE ocurrir siempre
        verify(exactly = 1) { firebaseAuth.signOut() }
    }
}