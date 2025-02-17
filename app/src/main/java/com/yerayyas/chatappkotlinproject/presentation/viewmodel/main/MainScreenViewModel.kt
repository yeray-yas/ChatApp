package com.yerayyas.chatappkotlinproject.presentation.viewmodel.main

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(private val auth: FirebaseAuth) : ViewModel() {

    private val _isUserAuthenticated = MutableStateFlow(auth.currentUser != null)
    val isUserAuthenticated = _isUserAuthenticated.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener {
        _isUserAuthenticated.value = auth.currentUser != null
    }

    init {
        auth.addAuthStateListener(authListener)
    }
  
    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}


