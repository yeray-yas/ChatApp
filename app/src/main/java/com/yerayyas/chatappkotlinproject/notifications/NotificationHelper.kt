package com.yerayyas.chatappkotlinproject.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.activity.MainActivity
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_ID
import com.yerayyas.chatappkotlinproject.utils.Constants.GROUP_KEY
import com.yerayyas.chatappkotlinproject.utils.Constants.SUMMARY_ID
import com.yerayyas.chatappkotlinproject.utils.Constants.CHANNEL_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context, // inyectado con @ApplicationContext
) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendChatNotification(
        senderId: String,
        senderName: String,
        messageBody: String,
        chatId: String,
    ) {
        createChannelIfNeeded()
        val pendingIntent = buildChatPendingIntent(senderId, senderName)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(senderName)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .build()

        val manager = NotificationManagerCompat.from(context)
        manager.notify(senderId.hashCode(), notif)
        sendSummaryNotification(manager)
    }

    private fun buildChatPendingIntent(senderId: String, senderName: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateTo", "chat")
            putExtra("userId", senderId)
            putExtra("username", senderName)
        }
        return PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createChannelIfNeeded() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            && nm.getNotificationChannel(CHANNEL_ID) == null
        ) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de chat"
            }
            nm.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendSummaryNotification(manager: NotificationManagerCompat) {
        val activeCount = getActiveChatNotificationsCount()
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Tienes $activeCount mensajes sin leer")
            .setSmallIcon(R.drawable.ic_chat)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle("$activeCount mensajes nuevos")
                    .setSummaryText("Mensajes de chat")
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        manager.notify(SUMMARY_ID, summary)
    }

    private fun getActiveChatNotificationsCount(): Int {
        val systemNm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return systemNm.activeNotifications
            .count { it.notification.group == GROUP_KEY && it.id != SUMMARY_ID }
    }
}
