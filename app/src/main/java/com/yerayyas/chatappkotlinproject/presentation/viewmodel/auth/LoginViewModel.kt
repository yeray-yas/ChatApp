package com.yerayyas.chatappkotlinproject.presentation.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel for managing the login process.
 *
 * This ViewModel handles the authentication logic using FirebaseAuth to sign in the user
 * with email and password. It uses coroutines to perform the authentication operation
 * asynchronously and provides the result through a callback function.
 *
 * @property auth The FirebaseAuth instance used for authentication.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: FirebaseAuth) : ViewModel() {

    /**
     * Attempts to log in the user with the provided email and password.
     *
     * This function calls FirebaseAuth's signInWithEmailAndPassword method asynchronously
     * and invokes the provided callback with the result. If the login is successful,
     * the callback is triggered with a success flag and no error message. If there is an error,
     * the callback is triggered with a failure flag and the error message.
     *
     * @param email The email of the user trying to log in.
     * @param password The password of the user trying to log in.
     * @param onResult A callback function that returns a boolean indicating success or failure,
     *                 and an optional error message in case of failure.
     */
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}
