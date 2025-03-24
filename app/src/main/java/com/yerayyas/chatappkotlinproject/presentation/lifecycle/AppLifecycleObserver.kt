package com.yerayyas.chatappkotlinproject.presentation.lifecycle

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppLifecycleObserver : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var database: DatabaseReference

    private var isAppInForeground = false

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!isAppInForeground) {
            isAppInForeground = true
            updateUserStatus("online")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (isAppInForeground) {
            isAppInForeground = false
            updateUserStatus("offline")
        }
    }

    private fun updateUserStatus(status: String) {
        auth.currentUser?.uid?.let { uid ->
            database.child("Users").child(uid).child("private").updateChildren(
                mapOf(
                    "status" to status,
                    "lastSeen" to ServerValue.TIMESTAMP
                )
            ).addOnFailureListener { e ->
                Log.e("AppLifecycle", "Error updating user status to $status", e)
            }
        }
    }
} 