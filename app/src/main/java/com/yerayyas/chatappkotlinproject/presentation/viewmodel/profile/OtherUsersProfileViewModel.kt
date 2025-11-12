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

/**
 * ViewModel for managing and loading the profile data of another user.
 *
 * This ViewModel is responsible for fetching the profile information of another user
 * from Firebase Realtime Database. It loads the user's username and profile image,
 * then stores it in a state flow that can be observed by the UI.
 *
 * @property database Reference to the Firebase Realtime Database for retrieving user data.
 * @property _userData A mutable state flow holding the user profile data.
 * @property userData A read-only state flow exposing the user profile data.
 */
@HiltViewModel
class OtherUsersProfileViewModel @Inject constructor(
    private val database: DatabaseReference
) : ViewModel() {

    private val _userData = MutableStateFlow<OtherUserProfile?>(null)
    val userData: StateFlow<OtherUserProfile?> = _userData

    /**
     * Loads the profile data of a user from Firebase Realtime Database.
     *
     * The method fetches the username and profile image from the "public" node in the
     * user's data. Upon successful retrieval, it updates the [_userData] state flow
     * with the user's information.
     *
     * @param userId The ID of the user whose profile data is being fetched.
     */
    fun loadUserProfile(userId: String) {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val publicData = snapshot.child("public")
                    val user = OtherUserProfile(
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

/**
 * Data class representing another user's profile information.
 *
 * This data class contains the basic public profile information of another user,
 * including their username and profile image URL. This is separate from the main
 * User model to avoid conflicts and provide a focused data structure.
 *
 * @property username The username of the user.
 * @property profileImage The URL of the user's profile image.
 */
data class OtherUserProfile(
    val username: String,
    val profileImage: String
)
