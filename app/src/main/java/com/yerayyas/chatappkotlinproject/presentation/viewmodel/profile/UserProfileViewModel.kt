package com.yerayyas.chatappkotlinproject.presentation.viewmodel.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(private val auth: FirebaseAuth, private val database: DatabaseReference) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _names = MutableStateFlow("")
    val names: StateFlow<String> = _names

    private val _lastNames = MutableStateFlow("")
    val lastNames: StateFlow<String> = _lastNames

    private val _image = MutableStateFlow("")
    val image: StateFlow<String> = _image

    private val _profession = MutableStateFlow("")
    val profession: StateFlow<String> = _profession

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address

    private val _age = MutableStateFlow("")
    val age: StateFlow<String> = _age

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone

    private var userListener: ValueEventListener? = null

    init {
        setupCurrentUserListener()
    }

    /**
     * Sets up the listener to fetch the current user's data from Firebase
     * and updates the corresponding StateFlows.
     */
    private fun setupCurrentUserListener() {
        auth.currentUser?.uid?.let { uid ->
            userListener = database.child("Users").child(uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val publicData = snapshot.child("public")
                        val privateData = snapshot.child("private")

                        _username.value = publicData.child("username").getValue(String::class.java)?.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.ROOT
                            ) else it.toString()
                        } ?: ""
                        _email.value = privateData.child("email").getValue(String::class.java) ?: ""
                        _names.value = privateData.child("names").getValue(String::class.java) ?: ""
                        _lastNames.value = privateData.child("lastNames").getValue(String::class.java) ?: ""
                        _profession.value = privateData.child("profession").getValue(String::class.java) ?: ""
                        _address.value = privateData.child("address").getValue(String::class.java) ?: ""
                        _age.value = privateData.child("age").getValue(String::class.java) ?: ""
                        _phone.value = privateData.child("phone").getValue(String::class.java) ?: ""
                        _image.value = publicData.child("profileImage").getValue(String::class.java) ?: ""
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("UserListener", "Error fetching user data", error.toException())
                    }
                })
        }
    }

    /**
     * Updates the user's personal information in Firebase.
     *
     * @param names The new names.
     * @param lastNames The new last names.
     * @param profession The new profession.
     * @param address The new address.
     * @param age The new age.
     * @param phone The new phone number.
     */
    fun updatePersonalInformation(
        names: String,
        lastNames: String,
        profession: String,
        address: String,
        age: String,
        phone: String
    ) {
        auth.currentUser?.uid?.let { uid ->
            val updates = mapOf(
                "private/names" to names,
                "private/lastNames" to lastNames,
                "private/profession" to profession,
                "private/address" to address,
                "private/age" to age,
                "private/phone" to phone
            )
            database.child("Users").child(uid).updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Update", "User information updated successfully")
                    } else {
                        Log.e("Update", "Error updating information", task.exception)
                    }
                }
        }
    }

    /**
     * Updates the user's profile image by uploading the image to Firebase Storage
     * and updating the URL in the database under the "profileImage" field.
     *
     * @param imageUri The URI of the new profile image.
     */
    fun updateProfileImage(imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val storageReference = FirebaseStorage.getInstance()
            .reference.child("profileImages/$uid")

        storageReference.putFile(imageUri)
            .addOnSuccessListener { _ ->
                storageReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                    database.child("Users").child(uid).child("public").child("profileImage")
                        .setValue(downloadUrl.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("UpdateImage", "Image updated successfully")
                            } else {
                                Log.e("UpdateImage", "Error updating image", task.exception)
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("UpdateImage", "Error uploading image", it)
            }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove the listener to prevent memory leaks
        userListener?.let { database.removeEventListener(it) }
    }
}
