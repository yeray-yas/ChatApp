package com.yerayyas.chatappkotlinproject.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.yerayyas.chatappkotlinproject.R

class OreoNotification(base: Context) : ContextWrapper(base) {

    private var notificationManager: NotificationManager? = null


    companion object {
        private const val CHANNEL_ID = "com.yerayyas.chatappkotlinproject"
        private const val CHANNEL_NAME = "Message"
    }


    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.enableLights(false)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager?.createNotificationChannel(channel)


    }

    val getManager: NotificationManager?
        get() {
            if (notificationManager == null) {
                notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            return notificationManager
        }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getOreoNotification(
        title: String,
        body: String,
        pendingIntent: PendingIntent?,
        uriSound: Uri?, // TODO establecerlo en CHANNEL en vez de en URI (channelId: String = CHANNEL_ID,)
        icon: String?
    ):Notification.Builder {


        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(icon?.toIntOrNull() ?: R.drawable.ic_chat)
            .setSound(uriSound) // TODO establecerlo en CHANNEL en vez de en URI (.setSmallIcon(iconResId))
            .setAutoCancel(true)
    }
}
