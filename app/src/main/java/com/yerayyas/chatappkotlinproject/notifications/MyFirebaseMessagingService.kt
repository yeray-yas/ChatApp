package com.yerayyas.chatappkotlinproject.notifications

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.utils.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyFirebaseMsgService"


@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    // --- Inyección de Dependencias ---
    @Inject
    lateinit var userRepository: UserRepository // Para guardar/actualizar el token FCM

    @Inject
    lateinit var appState: AppState // Para saber si la app está en foreground/background

    @Inject
    lateinit var notifHelper: NotificationHelper // Para mostrar notificaciones

    // --- Coroutine Scope para el Servicio ---
    private val job = SupervisorJob()

    // Usamos Dispatchers.IO porque las operaciones de red/DB deben estar fuera del hilo ppal
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

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
                    userRepository.updateCurrentUserFCMToken(it)
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
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM Message Received From: ${remoteMessage.from}")

        // Priorizamos el payload 'data'
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val senderId = remoteMessage.data["senderId"]
            val senderName = remoteMessage.data["senderName"] ?: "Someone"
            val messagePreview = remoteMessage.data["messagePreview"] ?: "Sent you a message"
            val chatId = remoteMessage.data["chatId"] // Importante para la navegación

            if (senderId == null || chatId == null) {
                Log.e(TAG, "Received FCM message with missing senderId or chatId in data payload.")
                return // No podemos procesar sin esta info
            }

            // --- Lógica para decidir si mostrar notificación ---
            val isAppBackground = !appState.isAppInForeground
            val isChatScreenOpenForSender = appState.currentOpenChatUserId == senderId
            val shouldShowNotification = isAppBackground || !isChatScreenOpenForSender

            if (shouldShowNotification) {
                Log.d(
                    TAG,
                    "Condition met to show notification (App Background: $isAppBackground, Chat Screen Open for $senderId: $isChatScreenOpenForSender)"
                )
                notifHelper.sendChatNotification(senderId, senderName, messagePreview, chatId)
            } else {
                Log.d(
                    TAG,
                    "Notification suppressed. App is in foreground and chat screen for $senderId is open."
                )
                // La actualización de la UI debería ocurrir a través del listener de Realtime Database
            }

        } else if (remoteMessage.notification != null) {
            // Si solo llega payload 'notification' (menos control)
            Log.d(TAG, "Message Notification Body: ${remoteMessage.notification?.body}")
            // Android maneja esto automáticamente si la app está en background.
            // Si la app está en foreground, este método se llama, pero generalmente
            // preferimos el payload 'data' para consistencia.
        }
    }

    /**
     * Llamado cuando el servicio está siendo destruido.
     * Cancela las coroutines para evitar memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // Cancela el scope de coroutines del servicio
        Log.d(TAG, "Service destroyed, coroutine scope cancelled.")
    }

    // --- Constantes estáticas para IDs y Grupo ---
    companion object {
        private const val CHAT_NOTIFICATION_GROUP =
            "com.yourpackage.CHAT_MESSAGES" // Grupo único para tus notificaciones de chat
        private const val SUMMARY_NOTIFICATION_ID =
            0 // ID fijo y estándar para la notificación resumen
    }
}