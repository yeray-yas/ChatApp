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
class UserProfileViewModel @Inject constructor(private val auth : FirebaseAuth, private val database: DatabaseReference) : ViewModel() {

    // StateFlows para exponer la información del usuario de forma reactiva
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

    // Listener para mantener actualizada la información del usuario
    private var userListener: ValueEventListener? = null

    init {
        setupCurrentUserListener()
    }

    /**
     * Configura el listener para obtener los datos actuales del usuario
     * desde Firebase y actualizarlos en los StateFlow correspondientes.
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
     * Actualiza la información personal del usuario en Firebase.
     *
     * @param names Nuevo nombre(s).
     * @param lastNames Nuevos apellidos.
     * @param profession Nueva profesión.
     * @param address Nueva dirección.
     * @param age Nueva edad.
     * @param phone Nuevo número de teléfono.
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
     * Actualiza la foto de perfil subiendo la imagen a Firebase Storage y actualizando
     * la URL en la base de datos en la rama "profileImage".
     */
    fun updateProfileImage(imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val storageReference = FirebaseStorage.getInstance()
            .reference.child("profileImages/$uid")

        storageReference.putFile(imageUri)
            .addOnSuccessListener { _ ->
                // Una vez subida la imagen, obtenemos la URL de descarga
                storageReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Actualizamos el campo "profileImage" en la carpeta public
                    database.child("Users").child(uid).child("public").child("profileImage")
                        .setValue(downloadUrl.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("UpdateImage", "Imagen actualizada correctamente")
                            } else {
                                Log.e("UpdateImage", "Error actualizando la imagen", task.exception)
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("UpdateImage", "Error al subir la imagen", it)
            }
    }

    override fun onCleared() {
        super.onCleared()
        // Removemos el listener para evitar fugas de memoria
        userListener?.let { database.removeEventListener(it) }
    }
}
