package com.yerayyas.chatappkotlinproject.presentation.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DatabaseReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel responsible for handling the sign-up process in the application.
 *
 * This ViewModel interacts with Firebase Authentication and Firebase Realtime Database
 * to create a new user, store their data, and handle any errors that may arise during
 * the registration process.
 *
 * It uses the Hilt dependency injection framework to inject instances of FirebaseAuth
 * and DatabaseReference.
 *
 * @property auth FirebaseAuth instance used for user authentication.
 * @property database Firebase Realtime Database reference for storing user data.
 * @property errorMessage A mutable state variable to hold error messages if any occur.
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : ViewModel() {

    private var errorMessage by mutableStateOf<String?>(null)

    /**
     * Signs up a new user with the provided username, email, and password.
     *
     * This method creates a new user in Firebase Authentication and stores their public
     * and private data in Firebase Realtime Database.
     *
     * @param username The username chosen by the user.
     * @param email The email address provided by the user.
     * @param password The password chosen by the user.
     * @param onResult A callback function to handle the result of the sign-up attempt.
     *                 It takes a Boolean indicating success and an optional error message.
     */
    fun signUp(username: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Error: UID is null")

                val publicData = mapOf(
                    "username" to username,
                    "profileImage" to "",
                    "find" to username.lowercase()
                )

                val privateData = mapOf(
                    "email" to email,
                    "status" to "offline",
                    "lastSeen" to 0L
                )

                val userRef = database.child("Users").child(uid)
                userRef.child("public").setValue(publicData).await()
                userRef.child("private").setValue(privateData).await()

                onResult(true, null)
            } catch (e: FirebaseAuthException) {
                errorMessage = e.localizedMessage ?: "Unknown error"
                onResult(false, errorMessage)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Unknown error"
                onResult(false, errorMessage)
            }
        }
    }
}
