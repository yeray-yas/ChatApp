package com.yerayyas.chatappkotlinproject.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.activity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.utils.AppState
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_ID
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_NAME

private const val TAG = "MyFirebaseMsgService"


@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    // --- Inyección de Dependencias ---
    @Inject
    lateinit var userRepository: UserRepository // Para guardar/actualizar el token FCM

    @Inject
    lateinit var appState: AppState // Para saber si la app está en foreground/background

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
                sendNotification(senderId, senderName, messagePreview, chatId)
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
     * Crea y muestra una notificación en el sistema si el permiso está concedido.
     */
    private fun sendNotification(
        senderId: String,
        senderName: String,
        messageBody: String,
        chatId: String // Podrías usarlo en el intent si tu navegación lo necesita
    ) {
        // 1. Crear Intent para abrir MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Comportamiento estándar
            // Añadir extras para que MainActivity sepa a qué chat navegar
            putExtra("navigateTo", "chat")
            putExtra("userId", senderId)
            putExtra("username", senderName) // Necesario para la ChatScreen TopAppBar
        }

        // Request code único por chat para que PendingIntents sean distintos
        val requestCode = senderId.hashCode()
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            // FLAG_IMMUTABLE es requerido para API 31+ y buena práctica general
            // FLAG_UPDATE_CURRENT asegura que los extras del Intent se actualicen si la notificación se actualiza
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. Crear el canal de notificación (si no existe)
        createNotificationChannel()

        // 3. Construir la notificación individual
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat) // ¡TU icono de notificación!
            .setContentTitle(senderName) // Título: Nombre del remitente
            .setContentText(messageBody) // Cuerpo: Vista previa del mensaje
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Importancia alta para mensajes
            .setContentIntent(pendingIntent) // Acción al tocarla
            .setAutoCancel(true) // Desaparece al tocarla
            .setGroup(CHAT_NOTIFICATION_GROUP) // Agrupar notificaciones de chat

        // 4. Obtener el NotificationManagerCompat
        val notificationManager = NotificationManagerCompat.from(this)

        // ID único por chat para la notificación (permite actualizarla)
        val notificationId = senderId.hashCode()

        // 5. --- Verificación de Permiso (Android 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // En API 33+ necesitamos el permiso POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permiso OK: Mostrar notificación individual y actualizar resumen
                try {
                    Log.d(
                        TAG,
                        "Permission granted (API 33+). Sending notification $notificationId for $senderId"
                    )
                    notificationManager.notify(notificationId, notificationBuilder.build())
                    showSummaryNotification(notificationManager) // Actualiza/muestra el resumen
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException trying to notify (API 33+): ${e.message}", e)
                }
            } else {
                // Permiso NO OK: No mostrar notificación
                Log.w(
                    TAG,
                    "Notification suppressed for senderId $senderId. POST_NOTIFICATIONS permission not granted (API 33+)."
                )
            }
        } else {
            // Antes de API 33: No se requiere permiso en tiempo de ejecución
            try {
                Log.d(TAG, "Pre-API 33. Sending notification $notificationId for $senderId")
                notificationManager.notify(notificationId, notificationBuilder.build())
                showSummaryNotification(notificationManager) // Actualiza/muestra el resumen
            } catch (e: Exception) {
                Log.e(TAG, "Error trying to notify (pre-API 33): ${e.message}", e)
            }
        }
        // --- Fin Verificación de Permiso ---
    }

    /**
     * Crea (si no existe) o actualiza la notificación resumen para el grupo de chat.
     */
    private fun showSummaryNotification(notificationManager: NotificationManagerCompat) {
        // Construye la notificación resumen
        val summaryNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name)) // O un título genérico como "New Messages"
            .setContentText("You have new chat messages") // Texto por defecto
            .setSmallIcon(R.drawable.ic_chat) // Icono
            // Podrías usar InboxStyle para mostrar líneas de los últimos mensajes si lo gestionas
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle("${getActiveNotificationCount(notificationManager)}+ new messages") // Título expandido dinámico
                    .setSummaryText("New messages")
            ) // Texto en la parte inferior
            .setGroup(CHAT_NOTIFICATION_GROUP) // Pertenece al mismo grupo
            .setGroupSummary(true) // ¡ESENCIAL! Indica que es la notificación resumen
            .setAutoCancel(true) // También se cancela al tocar
            // Podrías poner un PendingIntent aquí que abra la lista de chats, no un chat específico
            // .setContentIntent(createOpenChatListPendingIntent())
            .build()

        // Usa un ID fijo (como 0) para la notificación resumen
        // La notificación se mostrará/actualizará automáticamente por el sistema
        // cuando haya 2 o más notificaciones individuales en el grupo.
        try {
            // La verificación de permiso ya se hizo antes de llamar a esta función
            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
            Log.d(TAG, "Summary notification ($SUMMARY_NOTIFICATION_ID) updated/sent.")
        } catch (e: SecurityException) {
            // Es poco probable llegar aquí si se verifica antes, pero por seguridad
            Log.e(TAG, "SecurityException trying to notify summary: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception trying to notify summary: ${e.message}", e)
        }
    }

    /**
     * (Opcional pero útil) Cuenta cuántas notificaciones de chat están activas.
     * Nota: Requiere acceso al NotificationManager del sistema.
     */
    private fun getActiveNotificationCount(compatManager: NotificationManagerCompat): Int {
        // Necesitamos el NotificationManager del sistema para esto
        val systemNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (systemNotificationManager != null) {
            // Contamos las notificaciones activas que pertenecen a nuestro grupo
            return systemNotificationManager.activeNotifications
                .count {
                    it.notification.group == CHAT_NOTIFICATION_GROUP && it.notification.flags.and(
                        NotificationCompat.FLAG_GROUP_SUMMARY
                    ) == 0
                }
        }
        return 1 // Devuelve al menos 1 si no podemos contar
    }


    /**
     * Crea el canal de notificación para Android 8.0 (Oreo) y superior.
     * Es seguro llamar a esto múltiples veces; el sistema lo ignora si ya existe.
     */
    private fun createNotificationChannel() {
        // Solo necesario en API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, // ID del canal (debe coincidir con el builder)
                CHANNEL_NAME, // Nombre visible para el usuario en la config de la app
                NotificationManager.IMPORTANCE_HIGH // Importancia (HIGH para notificaciones heads-up)
            ).apply {
                description =
                    "Channel for incoming chat message notifications." // Descripción opcional
                // Puedes habilitar luces, vibración, etc. aquí si quieres un comportamiento por defecto
                // enableLights(true)
                // lightColor = Color.CYAN
                // enableVibration(true)
                // vibrationPattern = longArrayOf(0, 400, 200, 400) // Ejemplo patrón vibración
            }

            // Registra el canal con el sistema
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager // Obtiene el servicio del sistema

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel '$CHANNEL_ID' created or already exists.")
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