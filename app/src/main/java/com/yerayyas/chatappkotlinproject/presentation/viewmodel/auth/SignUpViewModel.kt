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

@HiltViewModel
class SignUpViewModel @Inject constructor(private val auth : FirebaseAuth, private val database: DatabaseReference) : ViewModel() {

    private var errorMessage by mutableStateOf<String?>(null)

    fun signUp(username: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Error: UID es null")

                // Datos públicos
                val publicData = mapOf(
                    "username" to username,
                    "profileImage" to "",
                    "find" to username.lowercase()
                )

                // Datos privados
                val privateData = mapOf(
                    "email" to email,
                    "status" to "offline",
                    "lastSeen" to 0L
                )

                // Crear la estructura completa del usuario
                val userRef = database.child("Users").child(uid)
                userRef.child("public").setValue(publicData).await()
                userRef.child("private").setValue(privateData).await()

                onResult(true, null)
            } catch (e: FirebaseAuthException) {
                errorMessage = e.localizedMessage ?: "Error desconocido"
                onResult(false, errorMessage)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Error desconocido"
                onResult(false, errorMessage)
            }
        }
    }
}



