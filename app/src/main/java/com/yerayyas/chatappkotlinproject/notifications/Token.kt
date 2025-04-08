package com.yerayyas.chatappkotlinproject.notifications

import android.util.Log

class Token private constructor(
    private var _token: String
) {
    companion object {
        private const val TAG = "TokenClass"

        fun create(token: String? = null): Token {
            val safeToken = token?.takeIf { it.isNotBlank() } ?: ""
            Log.d(TAG, "Token creado: ${safeToken.take(3)}...") // Log parcial por seguridad
            return Token(safeToken)
        }
    }

    val value: String
        get() = _token.ifEmpty {
            Log.w(TAG, "Acceso a token vacío")
            ""
        }

    fun update(newToken: String) {
        _token = newToken
        Log.d(TAG, "Token actualizado")
    }

    override fun toString(): String = "Token[${_token.take(3)}...]" // Seguridad en logs
}

/*
class Token(token: String = "") {
    var token: String = token
        private set // Solo permitimos modificación interna si es necesario
        get() = field.ifEmpty { "InvalidToken" } // Ejemplo de validación
}
 */