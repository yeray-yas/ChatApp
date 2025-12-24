package com.yerayyas.chatappkotlinproject.data.service

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.interfaces.AuthenticationService
import jakarta.inject.Inject

/**
 * Concrete implementation of [AuthenticationService] backed by the Firebase Authentication SDK.
 *
 * This class acts as an infrastructure adapter, isolating the application's domain layer
 * from direct dependencies on the Firebase framework. It provides the actual execution
 * logic for authentication state checks defined in the abstraction.
 *
 * Key features:
 * - Wraps [FirebaseAuth] interactions.
 * - Provides a lightweight check for session persistence.
 */
class FirebaseAuthenticationService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthenticationService {

    /**
     * Determines if a user currently has an active session.
     *
     * This method checks the local cache of the Firebase SDK. A return value of `true`
     * implies that [FirebaseAuth.getCurrentUser] is not null, meaning the app has
     * a persisted session token (which may be refreshed automatically by the SDK).
     *
     * @return `true` if a user is currently signed in, `false` otherwise.
     */
    override fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
