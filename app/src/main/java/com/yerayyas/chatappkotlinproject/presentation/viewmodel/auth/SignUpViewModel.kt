package com.yerayyas.chatappkotlinproject.presentation.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference.child("Users")

    private var errorMessage by mutableStateOf<String?>(null)

    fun signUp(username: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Error: UID es null")

                val userMap = mapOf(
                    "userId" to uid,
                    "username" to username,
                    "email" to email,
                    "image" to "",
                    "find" to username.lowercase(),
                    "names" to "",
                    "lastNames" to "",
                    "age" to "",
                    "profession" to "",
                    "address" to "",
                    "status" to "offline",
                    "phone" to ""
                )

                database.child(uid).setValue(userMap).await()
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



