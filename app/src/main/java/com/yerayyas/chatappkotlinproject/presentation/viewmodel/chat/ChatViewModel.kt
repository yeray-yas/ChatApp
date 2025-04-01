package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChatViewModel"

/**
 * ViewModel para la pantalla de chat.
 * Gestiona la lógica de presentación y el estado de la UI.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    // Estados para la UI
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var otherUserId: String? = null

    /**
     * Obtiene el ID del usuario actual.
     * @return ID del usuario actual o cadena vacía si no hay usuario autenticado
     */
    fun getCurrentUserId(): String = chatRepository.getCurrentUserId()

    /**
     * Carga los mensajes de un chat con otro usuario.
     * @param otherUserId ID del otro usuario en el chat
     */
    fun loadMessages(otherUserId: String) {
        this.otherUserId = otherUserId
        
        // Inicialmente establecemos isLoading a true
        _isLoading.value = true
        _error.value = null
        
        chatRepository.getMessages(otherUserId)
            .onStart { 
                // Ya establecimos isLoading a true antes, así que no es necesario aquí
            }
            .onEach { messagesList ->
                _messages.value = messagesList
                // Aseguramos que isLoading se establezca a false cuando recibimos mensajes
                _isLoading.value = false
            }
            .catch { e ->
                Log.e(TAG, "Error loading messages", e)
                _error.value = "Error al cargar los mensajes: ${e.message}"
                _isLoading.value = false
            }
            .onCompletion { 
                // Aseguramos que isLoading se establezca a false cuando termina el flujo
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    /**
     * Envía un mensaje de texto al otro usuario.
     * @param receiverId ID del usuario receptor
     * @param messageText Texto del mensaje a enviar
     */
    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return

        viewModelScope.launch {
            try {
                _error.value = null
                chatRepository.sendTextMessage(receiverId, messageText)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _error.value = "Error al enviar el mensaje: ${e.message}"
            }
        }
    }

    /**
     * Envía una imagen al otro usuario.
     * @param receiverId ID del usuario receptor
     * @param imageUri URI de la imagen a enviar
     */
    fun sendImage(receiverId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                chatRepository.sendImageMessage(receiverId, imageUri)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending image", e)
                _error.value = "Error al enviar la imagen: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
