package com.yerayyas.chatappkotlinproject.presentation.viewmodel.main

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the main screen of the chat application.
 *
 * This ViewModel is responsible for managing user authentication state.
 * It uses FirebaseAuth to check if the user is authenticated and updates
 * the state accordingly. It also listens to authentication state changes
 * and updates the `isUserAuthenticated` flow.
 *
 * @property auth The FirebaseAuth instance used to check the user's authentication status.
 * @property _isUserAuthenticated A private MutableStateFlow that holds the authentication state.
 * @property isUserAuthenticated A public immutable StateFlow that exposes the authentication state.
 * @property authListener A listener that listens for changes in the authentication state.
 */
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

    /**
     * Called when the ViewModel is about to be destroyed.
     * Removes the authentication state listener to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}
