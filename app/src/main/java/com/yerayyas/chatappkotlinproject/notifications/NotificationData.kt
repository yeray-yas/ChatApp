package com.yerayyas.chatappkotlinproject.notifications

import android.util.Log
import com.yerayyas.chatappkotlinproject.R

/**
 * Representa los datos de una notificación en la aplicación.
 * @property username Nombre del usuario asociado a la notificación
 * @property iconResId Recurso de icono (debe ser un ID de recurso válido)
 * @property body Contenido principal de la notificación
 * @property title Título de la notificación
 * @property sentTimestamp Fecha de envío en formato ISO 8601
 */
data class NotificationData(
    var username: String = "",
    var iconResId: Int = R.drawable.ic_chat,
    var body: String = "",
    var title: String = "",
    var sentTimestamp: String = ""
) {
    init {
        validateIconResource()
    }

    /**
     * Actualiza múltiples propiedades de forma atómica
     * @param block Lambda con las modificaciones a aplicar
     */
    inline fun update(block: NotificationData.() -> Unit) {
        this.apply(block)
        validateIconResource()
    }

     fun validateIconResource() {
        try {
            require(iconResId != 0) { "Icon resource must be valid" }
        } catch (e: IllegalArgumentException) {
            Log.e("NotificationData", "Invalid icon resource", e)
            iconResId = R.drawable.ic_chat
        }
    }

    companion object {
        /**
         * Crea una instancia desde un Map
         * @param map Mapa con claves: username, iconResId, body, title, sentTimestamp
         */
        fun fromMap(map: Map<String, Any>): NotificationData {
            return NotificationData(
                username = map["username"] as? String ?: "",
                iconResId = (map["iconResId"] as? Int) ?: R.drawable.ic_chat,
                body = map["body"] as? String ?: "",
                title = map["title"] as? String ?: "",
                sentTimestamp = map["sentTimestamp"] as? String ?: ""
            )
        }
    }
}