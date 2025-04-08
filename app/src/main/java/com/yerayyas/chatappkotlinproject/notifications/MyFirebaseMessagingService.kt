package com.yerayyas.chatappkotlinproject.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.activity.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val sent = message.data["sent"]
        val user = message.data["user"]
        val sharedPref = getSharedPreferences("PREFS", MODE_PRIVATE)
        val currentOnlineUser = sharedPref.getString("currentUser", "none")
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser != null && sent == firebaseUser.uid) {
            if (currentOnlineUser != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sendOreoNotification(message)

                } else {
                    sendNotification(message)
                }
            }
        }
    }

    private fun sendNotification(message: RemoteMessage) {
        val user = message.data["user"]
        val icon = message.data["icon"]
        val title = message.data["title"]
        val body = message.data["body"]

        val messageNotification = message.notification
        val j = user!!.replace("[\\D]".toRegex(), "").toInt()
        val intent = Intent(this, MainActivity::class.java)

        val bundle = Bundle()
        bundle.putString("userid", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, j, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this)
            .setSmallIcon(icon?.toIntOrNull() ?: R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setSound(sound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager



        var i = 0
        if (j > 0) {
            i = j
        }
        oreoNotification.getManager!!.notify(i, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendOreoNotification(message: RemoteMessage) {
        val user = message.data["user"]
        val icon = message.data["icon"]
        val title = message.data["title"]
        val body = message.data["body"]

        val notification = message.notification
        val j = user!!.replace("[\\D]".toRegex(), "").toInt()
        val intent = Intent(this, MainActivity::class.java)

        val bundle = Bundle()
        bundle.putString("userid", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, j, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val oreoNotification = OreoNotification(base = this)
        val builder = oreoNotification.getOreoNotification(
            title = title!!,
            body = body!!,
            pendingIntent = pendingIntent,
            uriSound = sound,
            icon = icon
        )
        var i = 0
        if (j > 0) {
            i = j
        }
        oreoNotification.getManager!!.notify(i, builder.build())
    }
}