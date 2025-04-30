package com.yerayyas.chatappkotlinproject.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.di.ServiceCoroutineScope
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyFirebaseMsgService"


@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    // --- Inyección de Dependencias ---
    @Inject
    @ServiceCoroutineScope
    lateinit var serviceScope: CoroutineScope

    @Inject lateinit var updateFcmToken: UpdateFcmTokenUseCase


    @Inject
    lateinit var appState: AppState // Para saber si la app está en foreground/background

    @Inject
    lateinit var notifHelper: NotificationHelper // Para mostrar notificaciones

    /**
     * Llamado cuando se genera un nuevo token FCM o se actualiza uno existente.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")
        super.onNewToken(token) // Llama a super si es necesario (aunque aquí no hace mucho)
        sendRegistrationToServer(token)
    }

    /**
     * Envía el token FCM al servidor/backend para asociarlo con el usuario actual.
     */
    private fun sendRegistrationToServer(token: String?) {
        token?.let {
            // No lanzar excepciones no controladas desde onNewToken, mejor loggear
            serviceScope.launch {
                try {
                    // Llama a la función del repositorio que guarda el token en la DB
                    updateFcmToken(it)
                    Log.i(TAG, "FCM Token successfully sent to server for current user.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send FCM token to server", e)
                    // Considera implementar una lógica de reintento aquí si es crítico
                }
            }
        }
    }

    /**
     * Llamado cuando se recibe un mensaje FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.takeIf { it.isNotEmpty() }?.let { data ->
            val senderId      = data["senderId"]  ?: return
            val chatId        = data["chatId"]    ?: return
            val senderName    = data["senderName"] ?: "Someone"
            val messagePreview= data["messagePreview"] ?: "New message"

            val isBackground = !appState.isAppInForeground
            val isChatOpen   = appState.currentOpenChatUserId == senderId

            if (isBackground || !isChatOpen) {
                notifHelper.sendChatNotification(
                    senderId    = senderId,
                    senderName  = senderName,
                    messageBody = messagePreview,
                    chatId      = chatId
                )
            } else {
                Log.d(TAG, "Suppressing notification; chat already open.")
            }
        }
    }

    // --- Constantes estáticas para IDs y Grupo ---
 /*   companion object {
        private const val CHAT_NOTIFICATION_GROUP =
            "com.yourpackage.CHAT_MESSAGES" // Grupo único para tus notificaciones de chat
        private const val SUMMARY_NOTIFICATION_ID =
            0 // ID fijo y estándar para la notificación resumen
    }*/
}