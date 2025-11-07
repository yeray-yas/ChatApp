package com.yerayyas.chatappkotlinproject.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

/**
 * BroadcastReceiver that handles device boot completion and package replacement events.
 *
 * This receiver is particularly important for Xiaomi and other OEM devices that
 * aggressively kill background services. It ensures that FCM tokens are refreshed
 * after device reboot or app updates.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device boot completed")
                handleBootCompleted()
            }

            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Package replaced")
                handlePackageReplaced()
            }
        }
    }

    /**
     * Handles device boot completion by refreshing FCM token if user is authenticated.
     */
    private fun handleBootCompleted() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d(TAG, "User is authenticated, refreshing FCM token")
            refreshFcmToken()
        } else {
            Log.d(TAG, "User not authenticated, skipping FCM token refresh")
        }
    }

    /**
     * Handles package replacement by refreshing FCM token if user is authenticated.
     */
    private fun handlePackageReplaced() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d(TAG, "User is authenticated after package replacement, refreshing FCM token")
            refreshFcmToken()
        }
    }

    /**
     * Refreshes the FCM token to ensure notifications work properly after device reboot
     * or app updates.
     */
    private fun refreshFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM registration token refreshed: $token")
        }
    }
}