package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchMessagesUseCase @Inject constructor() {

    /**
     * Busca mensajes que contengan el query especificado
     * @param messages Flow de mensajes donde buscar
     * @param query Texto a buscar (insensible a mayúsculas)
     * @return Flow de mensajes filtrados
     */
    fun execute(
        messages: Flow<List<ChatMessage>>,
        query: String
    ): Flow<List<ChatMessage>> {
        return messages.map { messageList ->
            if (query.isBlank()) {
                messageList
            } else {
                messageList.filter { message ->
                    searchInMessage(message, query.trim())
                }
            }
        }
    }

    private fun searchInMessage(message: ChatMessage, query: String): Boolean {
        val searchQuery = query.lowercase()

        // Buscar en el contenido del mensaje
        val messageMatches = message.message.lowercase().contains(searchQuery)

        // Buscar en el mensaje al que se está respondiendo (si existe)
        val replyMatches = message.replyToMessage?.lowercase()?.contains(searchQuery) == true

        return messageMatches || replyMatches
    }

    /**
     * Busca mensajenes por tipo específico
     */
    fun searchByType(
        messages: Flow<List<ChatMessage>>,
        messageType: MessageType
    ): Flow<List<ChatMessage>> {
        return messages.map { messageList ->
            messageList.filter { it.messageType == messageType }
        }
    }

    /**
     * Busca mensajes de un usuario específico
     */
    fun searchBySender(
        messages: Flow<List<ChatMessage>>,
        senderId: String
    ): Flow<List<ChatMessage>> {
        return messages.map { messageList ->
            messageList.filter { it.senderId == senderId }
        }
    }

    /**
     * Busca mensajes en un rango de fechas
     */
    fun searchByDateRange(
        messages: Flow<List<ChatMessage>>,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<ChatMessage>> {
        return messages.map { messageList ->
            messageList.filter { message ->
                message.timestamp in startTimestamp..endTimestamp
            }
        }
    }
}