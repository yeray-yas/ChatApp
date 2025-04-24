package com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    private val _pendingNavigation = MutableStateFlow<NotificationNavigationState?>(null)
    val pendingNavigation: StateFlow<NotificationNavigationState?> = _pendingNavigation.asStateFlow()

    /**
     * Llamado desde MainActivity cuando llega un intent de notificación relevante.
     */
    fun setPendingNavigation(navigateTo: String?, userId: String?, username: String?) {
        if (navigateTo == "chat" && userId != null && username != null) {
            viewModelScope.launch { // Usar viewModelScope
                _pendingNavigation.update {
                    // Crea un nuevo estado para asegurar que el StateFlow emita
                    NotificationNavigationState(
                        navigateTo = navigateTo,
                        userId = userId,
                        username = username,
                        eventId = System.currentTimeMillis() // Asegura la emisión
                    )
                }
            }
        }
    }

    /**
     * Llamado desde NavigationWrapper después de que la navegación ha sido procesada.
     */
    fun clearPendingNavigation() {
        viewModelScope.launch { // Usar viewModelScope
            if (_pendingNavigation.value != null) { // Solo actualiza si hay algo que limpiar
                _pendingNavigation.update { null }
            }
        }
    }
}