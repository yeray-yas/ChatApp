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

    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // User's name (for showing in the TopAppBar)
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    // Listener for the .info/connected changes
    private var connectionListener: ValueEventListener? = null

    // Listener para el nodo "connections" del usuario
    private var connectionsListener: ValueEventListener? = null

    // Referencia al nodo del usuario actual
    private var currentUserRef: DatabaseReference? = null

    // Referencia a la conexión de este dispositivo
    private var myConnectionRef: DatabaseReference? = null

    // StateFlow for the user's list (excludes the current user)
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    // StateFlow for the search query
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
     * Actualiza el StateFlow con el nuevo término de búsqueda.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Configura el manejo de presencia del usuario usando un nodo "connections" para llevar el conteo
     * de dispositivos conectados.
     */
    private fun setupPresenceManagement() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            currentUserRef = database.child("Users").child(userId)


            // 1️⃣ Eliminar conexiones previas antes de crear una nueva
            val connectionsRef = currentUserRef!!.child("connections")
            connectionsRef.removeValue().addOnCompleteListener {
                // 2️⃣ Ahora sí, crear una nueva conexión
                myConnectionRef = connectionsRef.push()
                myConnectionRef!!.setValue(true)

                // 3️⃣ Asegurar que se elimine al cerrar la app
                myConnectionRef!!.onDisconnect().removeValue()

                // 4️⃣ Listener para contar conexiones activas
                connectionsListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val count = snapshot.childrenCount
                        if (count > 0) {
                            currentUserRef!!.child("status").setValue("online")
                        } else {
                            currentUserRef!!.child("status").setValue("offline")
                        }
                        currentUserRef!!.child("lastSeen").setValue(ServerValue.TIMESTAMP)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error al leer conexiones: ${error.message}")
                    }
                }
                connectionsRef.addValueEventListener(connectionsListener!!)
            }
        }
    }


    /**
     * Carga los datos actuales del usuario (por ejemplo, su nombre) y actualiza el StateFlow correspondiente.
     */
    private fun loadCurrentUserData() {
        auth.currentUser?.uid?.let { uid ->
            database.child("Users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        _username.value = snapshot.child("username").getValue(String::class.java)
                            ?.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                            }
                            ?: ""
                        _isLoading.value = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("LoadUser", "Error al cargar datos del usuario", error.toException())
                        _isLoading.value = false
                    }
                })
        } ?: run {
            _isLoading.value = false
        }
    }

    /**
     * Carga la lista de usuarios desde Firebase.
     * Si [searchQuery] no está vacío, se realiza una búsqueda parcial (basada en el campo "find").
     */
    private fun loadUsers(searchQuery: String = "") {
        auth.currentUser?.uid?.let { currentUserId ->
            val baseRef = database.child("Users")
            // Forzamos la sincronización de este nodo para recibir actualizaciones en tiempo real.
            baseRef.keepSynced(true)

            val queryRef: Query = if (searchQuery.isNotEmpty()) {
                baseRef.orderByChild("find")
                    .startAt(searchQuery)
                    .endAt("$searchQuery\uf8ff")
            } else {
                baseRef
            }
            // Usamos addValueEventListener para que la UI se actualice en tiempo real
            queryRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usersList = snapshot.children.mapNotNull { userSnapshot ->
                        val userId = userSnapshot.key ?: return@mapNotNull null
                        // Se excluye al usuario actual
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
                    _users.value = usersList.sortedWith(
                        compareByDescending<User> { it.isOnline }
                            .thenByDescending { it.lastSeen }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LoadUsers", "Error al cargar usuarios", error.toException())
                }
            })
        }
    }

    /**
     * Cierra sesión del usuario actual.
     * Antes de llamar a [FirebaseAuth.signOut] se actualiza el status a "offline" y se remueven los listeners.
     */
    fun signOut() {
        auth.currentUser?.uid?.let { _ ->
            myConnectionRef?.removeValue()?.addOnCompleteListener {
                auth.signOut()
                cleanupListeners()
            }
        } ?: run {
            auth.signOut()
            cleanupListeners()
        }
    }


    /**
     * Remueve los listeners de Firebase para evitar memory leaks.
     */
    private fun cleanupListeners() {
        connectionListener?.let { database.removeEventListener(it) }
        currentUserRef?.child("connections")?.let { connectionsRef ->
            connectionsListener?.let { connectionsRef.removeEventListener(it) }
        }
    }

    /**
     * Elimina el usuario de la base de datos y, a continuación, borra la cuenta de autenticación.
     * Al finalizar, se cierra la sesión para que se redirija a MainScreen.
     */
    fun deleteUser() {
        auth.currentUser?.uid?.let { uid ->
            // Primero borramos los datos del usuario en la base de datos
            database.child("Users").child(uid).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Luego, intentamos borrar la cuenta de autenticación
                    auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            // Finalmente, cerramos la sesión y limpiamos los listeners
                            auth.signOut()
                            cleanupListeners()
                        } else {
                            Log.e("DeleteUser", "Error al eliminar la cuenta de auth", deleteTask.exception)
                        }
                    }
                } else {
                    Log.e("DeleteUser", "Error al borrar datos del usuario", task.exception)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }
}
