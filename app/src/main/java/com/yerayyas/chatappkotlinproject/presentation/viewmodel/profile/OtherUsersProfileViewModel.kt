package com.yerayyas.chatappkotlinproject.presentation.viewmodel.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OtherUsersProfileViewModel @Inject constructor(
    private val database: DatabaseReference
) : ViewModel() {

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    fun loadUserProfile(userId: String) {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val publicData = snapshot.child("public")
                    val user = User(
                        username = publicData.child("username").getValue(String::class.java) ?: "",
                        profileImage = publicData.child("profileImage").getValue(String::class.java) ?: ""
                    )
                    _userData.value = user
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("OtherUserProfile", "Error loading user data", error.toException())
                }
            })
    }
}

data class User(
    val username: String,
    val profileImage: String
)