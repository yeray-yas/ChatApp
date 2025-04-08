package com.yerayyas.chatappkotlinproject.notifications

import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseInstanceIdService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { newToken ->
                if (newToken.isSuccessful && newToken.result != null && !TextUtils.isEmpty(newToken.result)) {
                    val myToken: String = newToken.result!!
                    if (firebaseUser != null) {
                        updateToken(myToken)
                    }
                }
            }
    }

    private fun updateToken(myToken: String) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().getReference("Tokens")
        val token = Token.create(myToken)
        reference.child(firebaseUser!!.uid).setValue(token)
    }
}