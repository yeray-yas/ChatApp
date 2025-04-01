package com.yerayyas.chatappkotlinproject.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modelo de datos que representa un mensaje en un chat.
 *
 * @property id Identificador único del mensaje
 * @property senderId ID del usuario que envía el mensaje
 * @property receiverId ID del usuario que recibe el mensaje
 * @property message Contenido textual del mensaje
 * @property timestamp Marca temporal del mensaje en milisegundos
 * @property imageUrl URL de la imagen asociada al mensaje (si es un mensaje de tipo imagen)
 * @property messageType Tipo de mensaje (texto o imagen)
 * @property readStatus Estado de lectura del mensaje (enviado, entregado o leído)
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val messageType: MessageType = MessageType.TEXT,
    val readStatus: ReadStatus = ReadStatus.SENT
) {
    /**
     * Comprueba si el mensaje es enviado por un usuario específico.
     *
     * @param userId ID del usuario a comprobar
     * @return true si el mensaje fue enviado por el usuario, false en caso contrario
     */
    fun isSentBy(userId: String): Boolean = senderId == userId

    /**
     * Comprueba si el mensaje es recibido por un usuario específico.
     *
     * @param userId ID del usuario a comprobar
     * @return true si el mensaje fue recibido por el usuario, false en caso contrario
     */
    fun isReceivedBy(userId: String): Boolean = receiverId == userId

    /**
     * Formatea la marca temporal del mensaje en un formato legible.
     *
     * @param pattern Patrón de formato de fecha (por defecto "HH:mm")
     * @return Cadena con la fecha formateada
     */
    fun getFormattedTime(pattern: String = "HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Comprueba si el mensaje contiene una imagen.
     *
     * @return true si el mensaje es de tipo imagen y tiene URL de imagen, false en caso contrario
     */
    fun hasImage(): Boolean = messageType == MessageType.IMAGE && imageUrl != null
}

/**
 * Tipos de mensaje soportados.
 */
enum class MessageType {
    TEXT,
    IMAGE
}

/**
 * Estados de lectura de los mensajes.
 */
enum class ReadStatus {
    SENT,     // Mensaje enviado pero no entregado
    DELIVERED, // Mensaje entregado pero no leído
    READ      // Mensaje leído por el receptor
}
