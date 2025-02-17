package com.yerayyas.chatappkotlinproject.presentation.viewmodel.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.yerayyas.chatappkotlinproject.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : ViewModel() {

    // Estado de carga
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Nombre de usuario (para mostrar en el TopAppBar)
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    // Listener para la gestión de presencia
    private var connectionListener: ValueEventListener? = null

    // StateFlow para la lista de usuarios (se excluye al usuario actual)
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    // StateFlow para el término de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        setupPresenceManagement()
        loadCurrentUserData() // Cargamos los datos del usuario para actualizar el nombre y finalizar la carga
        // Se lanza una corrutina para observar los cambios en el término de búsqueda
        viewModelScope.launch {
            _searchQuery
                .debounce(300)         // Espera 300ms tras la última escritura
                .distinctUntilChanged() // Evita emitir el mismo valor de forma consecutiva
                .collect { query ->
                    loadUsers(query.lowercase())
                }
        }
    }

    /**
     * Actualiza el StateFlow con la nueva consulta de búsqueda.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Configura la gestión de presencia del usuario, actualizando su estado a "online"
     * y programando su cambio a "offline" cuando se desconecte.
     */
    private fun setupPresenceManagement() {
        auth.currentUser?.uid?.let { uid ->
            val userRef = database.child("Users").child(uid)
            val connectedRef = database.child(".info/connected")
            connectionListener = connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) == true) {
                        // Configura los valores que se actualizarán al desconectarse
                        userRef.child("status").onDisconnect().setValue("offline")
                        userRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
                        // Actualiza el estado a "online" de forma inmediata
                        userRef.updateChildren(
                            mapOf(
                                "status" to "online",
                                "lastSeen" to ServerValue.TIMESTAMP
                            )
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Presence", "Connection error", error.toException())
                }
            })
        }
    }

    /**
     * Carga los datos del usuario actual desde Firebase (por ejemplo, el nombre de usuario)
     * y actualiza el StateFlow correspondiente.
     */
    private fun loadCurrentUserData() {
        auth.currentUser?.uid?.let { uid ->
            database.child("Users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        _username.value = snapshot.child("username").getValue(String::class.java)
                            ?.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.ROOT
                                ) else it.toString()
                            }
                            ?: ""
                        _isLoading.value = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("LoadUser", "Error loading user data", error.toException())
                        _isLoading.value = false
                    }
                })
        } ?: run {
            _isLoading.value = false
        }
    }

    /**
     * Carga la lista de usuarios desde Firebase.
     * Si [searchQuery] no está vacío, realiza una búsqueda parcial (basada en el campo "find").
     */
    private fun loadUsers(searchQuery: String = "") {
        auth.currentUser?.uid?.let { currentUserId ->
            val baseRef = database.child("Users")
            val queryRef: Query = if (searchQuery.isNotEmpty()) {
                baseRef.orderByChild("find")
                    .startAt(searchQuery)
                    .endAt("$searchQuery\uf8ff")
            } else {
                baseRef
            }
            queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usersList = snapshot.children.mapNotNull { userSnapshot ->
                        val userId = userSnapshot.key ?: return@mapNotNull null
                        // Se excluye el usuario actual
                        if (userId == currentUserId) return@mapNotNull null
                        User(
                            id = userId,
                            username = userSnapshot.child("username").getValue(String::class.java) ?: "",
                            email = userSnapshot.child("email").getValue(String::class.java) ?: "",
                            profileImage = userSnapshot.child("image").getValue(String::class.java) ?: "",
                            status = userSnapshot.child("status").getValue(String::class.java) ?: "offline",
                            isOnline = userSnapshot.child("status").getValue(String::class.java) == "online",
                            lastSeen = userSnapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                        )
                    }
                    // Ordena los usuarios: primero los online y luego por el último visto
                    _users.value = usersList.sortedWith(
                        compareByDescending<User> { it.isOnline }
                            .thenByDescending { it.lastSeen }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LoadUsers", "Error loading users", error.toException())
                }
            })
        }
    }

    /**
     * Función para cerrar la sesión del usuario.
     * Actualiza el estado a "offline" antes de llamar a [FirebaseAuth.signOut].
     */
    fun signOut() {
        auth.currentUser?.uid?.let { uid ->
            database.child("Users").child(uid).updateChildren(
                mapOf(
                    "status" to "offline",
                    "lastSeen" to ServerValue.TIMESTAMP
                )
            ).addOnCompleteListener {
                auth.signOut()
                cleanupListeners()
            }
        } ?: run {
            auth.signOut()
            cleanupListeners()
        }
    }

    /**
     * Elimina los listeners de Firebase para evitar fugas de memoria.
     */
    private fun cleanupListeners() {
        connectionListener?.let { database.removeEventListener(it) }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }
}
