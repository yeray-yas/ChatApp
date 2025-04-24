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
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

private const val TAG = "HomeViewModel"

/**
 * ViewModel responsible for managing user data, presence status, and user list in the home screen.
 * It interacts with Firebase to load the current user's information, manage presence updates,
 * and load other users based on search queries.
 *
 * @param auth Firebase authentication instance used to manage user sessions.
 * @param database Firebase Realtime Database instance used to manage and retrieve user data.
 */
@HiltViewModel
@OptIn(FlowPreview::class)
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private var connectionListener: ValueEventListener? = null

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        setupPresenceManagement()
        loadCurrentUserData()
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    loadUsers(query.lowercase())
                }
        }
    }

    /**
     * Updates the search query and triggers the user list update.
     *
     * @param query The search query entered by the user.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Sets up the user's presence management, updating the status to "online"
     * and scheduling the change to "offline" when disconnected.
     */
    private fun setupPresenceManagement() {
        auth.currentUser?.uid?.let { uid ->
            val userRef = database.child("Users").child(uid)
            val connectedRef = database.child(".info/connected")

            userRef.child("private").child("status").onDisconnect().setValue("offline")
            userRef.child("private").child("lastSeen").onDisconnect()
                .setValue(ServerValue.TIMESTAMP)

            connectionListener = connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        userRef.child("private").updateChildren(
                            mapOf(
                                "status" to "online",
                                "lastSeen" to ServerValue.TIMESTAMP
                            )
                        ).addOnFailureListener { e ->
                            Log.e("Presence", "Error updating online status", e)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Presence", "Connection error", error.toException())
                }
            })
        }
    }

    /**
     * Loads the current user's data from Firebase, such as the username,
     * and updates the corresponding StateFlow.
     */
    private fun loadCurrentUserData() {
        auth.currentUser?.uid?.let { uid ->
            database.child("Users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val publicData = snapshot.child("public")
                        _username.value = publicData.child("username").getValue(String::class.java)
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
     * Loads a list of users from Firebase. If the [searchQuery] is not empty, performs a partial search based on the "find" field.
     *
     * @param searchQuery The query used to filter the user list.
     */
    private fun loadUsers(searchQuery: String = "") {
        auth.currentUser?.uid?.let { currentUserId ->
            val baseRef = database.child("Users")
            val queryRef: Query = if (searchQuery.isNotEmpty()) {
                baseRef.orderByChild("public/find")
                    .startAt(searchQuery)
                    .endAt("$searchQuery\uf8ff")
            } else {
                baseRef
            }
            queryRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usersList = snapshot.children.mapNotNull { userSnapshot ->
                        val userId = userSnapshot.key ?: return@mapNotNull null
                        if (userId == currentUserId) return@mapNotNull null

                        val publicData = userSnapshot.child("public")
                        val privateData = userSnapshot.child("private")

                        User(
                            id = userId,
                            username = publicData.child("username").getValue(String::class.java)
                                ?: "",
                            email = privateData.child("email").getValue(String::class.java) ?: "",
                            profileImage = publicData.child("profileImage")
                                .getValue(String::class.java) ?: "",
                            status = privateData.child("status").getValue(String::class.java)
                                ?: "offline",
                            isOnline = privateData.child("status")
                                .getValue(String::class.java) == "online",
                            lastSeen = privateData.child("lastSeen").getValue(Long::class.java)
                                ?: 0L
                        )
                    }
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
     * Signs out the current user asynchronously using viewModelScope.
     * Updates the user's status to "offline", clears the FCM token,
     * performs the actual sign out, cleans up listeners, and calls the completion callback.
     *
     * @param onSignOutComplete Callback function that is called when the entire sign-out process is finished.
     */
    fun signOut(onSignOutComplete: () -> Unit) {
        val userId = auth.currentUser?.uid

        viewModelScope.launch { // Inicia una coroutine

            if (userId == null) {
                // Si no hay usuario, solo limpiar y completar
                Log.w(TAG, "Sign out called, but no user is currently logged in.")
                cleanupListeners() // Limpia listeners locales si los hubiera
                onSignOutComplete() // Llama al callback
                return@launch // Sale de la coroutine
            }

            try {
                // 1. Actualizar estado en Realtime Database (usando await)
                Log.d(TAG, "Updating user status to offline for userId: $userId")
                val statusUpdate = mapOf(
                    "status" to "offline",
                    "lastSeen" to ServerValue.TIMESTAMP
                )
                database.child("Users").child(userId).child("private")
                    .updateChildren(statusUpdate)
                    .await() // Espera a que la Task de Firebase termine
                Log.d(TAG, "User status updated to offline successfully.")

                // 2. Limpiar el token FCM (llamada suspend dentro de la coroutine)
                Log.d(TAG, "Clearing FCM token for userId: $userId")
                userRepository.clearCurrentUserFCMToken() // ¡Llamada correcta!
                Log.d(TAG, "FCM token cleared successfully.")

                // 3. Realizar el cierre de sesión en Firebase Auth
                Log.d(TAG, "Signing out user from Firebase Auth: $userId")
                auth.signOut()
                Log.i(TAG, "User signed out successfully from Firebase Auth.")

            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out operation for user $userId", e)
                // Opcional: Intentar desloguear de Auth incluso si falló la DB o el token
                try {
                    if (auth.currentUser?.uid == userId) { // Solo si sigue logueado
                        auth.signOut()
                        Log.w(TAG, "Signed out from Auth after encountering an error during DB/Token update.")
                    }
                } catch (signOutError: Exception) {
                    Log.e(TAG, "Error signing out from Auth after previous error", signOutError)
                }
            } finally {
                // 4. Limpiar listeners locales y llamar al callback (SIEMPRE)
                Log.d(TAG, "Cleaning up listeners and calling onSignOutComplete.")
                cleanupListeners() // Asegura la limpieza de listeners
                onSignOutComplete() // Notifica que el proceso terminó (con éxito o error)
            }
        }
    }

    /**
     * Deletes the user's account and associated data from Firebase.
     *
     * @param onComplete Callback function that is called upon completion, with a success flag and an optional error message.
     */
    fun deleteUser(onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false, "No authenticated user")

        database.child("Users").child(user.uid).removeValue()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        cleanupListeners()
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, "Error deleting account: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Error deleting data: ${e.message}")
            }
    }

    /**
     * Cleans up Firebase listeners to prevent memory leaks.
     */
    private fun cleanupListeners() {
        connectionListener?.let { database.removeEventListener(it) }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }
}
