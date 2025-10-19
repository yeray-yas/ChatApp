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
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
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
 * ViewModel for the home screen. Manages loading and searching users,
 * updates presence status in Firebase Realtime Database,
 * and handles sign-out and account deletion.
 *
 * @property auth        FirebaseAuth instance for user session management.
 * @property database    Reference to Firebase Realtime Database.
 * @property userRepository Repository for user-related data operations.
 * @property notificationHelper Helper for managing app notifications.
 */
@HiltViewModel
@OptIn(FlowPreview::class)
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference,
    private val userRepository: UserRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    /** Emits true while loading user data. */
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _username = MutableStateFlow("")
    /** Emits the current user's display name. */
    val username: StateFlow<String> = _username

    private val _users = MutableStateFlow<List<User>>(emptyList())
    /** Emits the list of other users matching the search. */
    val users: StateFlow<List<User>> = _users

    private val _searchQuery = MutableStateFlow("")
    /** Holds the current search query. */
    val searchQuery: StateFlow<String> = _searchQuery

    private var connectionListener: ValueEventListener? = null

    init {
        initializePresenceListener()
        loadCurrentUserProfile()
        notificationHelper.cancelAllNotifications()
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    fetchUsers(query.lowercase(Locale.getDefault()))
                }
        }
    }

    /**
     * Updates the search query to trigger user list refresh.
     *
     * @param query The new search text.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Sets up Firebase presence tracking: marks user online when connected,
     * and schedules offline status on disconnect.
     */
    private fun initializePresenceListener() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = database.child("Users").child(uid)
        val connectedRef = database.child(".info/connected")

        // Schedule offline status on disconnect
        userRef.child("private/status").onDisconnect().setValue("offline")
        userRef.child("private/lastSeen").onDisconnect()
            .setValue(ServerValue.TIMESTAMP)

        // Listen for connection changes
        connectionListener = connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (connected) {
                    userRef.child("private").updateChildren(
                        mapOf(
                            "status" to "online",
                            "lastSeen" to ServerValue.TIMESTAMP
                        )
                    ).addOnFailureListener { e ->
                        Log.e(TAG, "Failed to set user online", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Presence listener cancelled", error.toException())
            }
        })
    }

    /**
     * Retrieves the current user's public profile (username) from Firebase.
     */
    private fun loadCurrentUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _isLoading.value = false
            return
        }
        database.child("Users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val publicData = snapshot.child("public")
                    val rawName = publicData.child("username").getValue(String::class.java) ?: ""
                    _username.value = rawName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    _isLoading.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load user profile", error.toException())
                    _isLoading.value = false
                }
            })
    }

    /**
     * Fetches other users from Firebase, filtering by [searchQuery] if provided.
     * Excludes the current user and sorts by online status and last seen time.
     *
     * @param searchQuery Lowercased text used to match against the "public/find" field.
     */
    private fun fetchUsers(searchQuery: String = "") {
        val currentUserId = auth.currentUser?.uid ?: return
        val usersRef = database.child("Users")
        val queryRef: Query = if (searchQuery.isNotEmpty()) {
            usersRef.orderByChild("public/find")
                .startAt(searchQuery)
                .endAt("$searchQuery\uf8ff")
        } else {
            usersRef
        }
        queryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    if (id == currentUserId) return@mapNotNull null
                    val publicData = child.child("public")
                    val privateData = child.child("private")
                    User(
                        id = id,
                        username = publicData.child("username").getValue(String::class.java) ?: "",
                        email = privateData.child("email").getValue(String::class.java) ?: "",
                        profileImage = publicData.child("profileImage").getValue(String::class.java) ?: "",
                        status = privateData.child("status").getValue(String::class.java) ?: "offline",
                        isOnline = privateData.child("status").getValue(String::class.java) == "online",
                        lastSeen = privateData.child("lastSeen").getValue(Long::class.java) ?: 0L
                    )
                }
                _users.value = list.sortedWith(
                    compareByDescending<User> { it.isOnline }
                        .thenByDescending { it.lastSeen }
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load users", error.toException())
            }
        })
    }

    /**
     * Signs out the current user, updates their presence to offline,
     * clears FCM token, and cleans up listeners.
     *
     * @param onSignOutComplete Callback invoked when sign-out flow finishes.
     */
    fun signOut(onSignOutComplete: () -> Unit) {
        val uid = auth.currentUser?.uid
        viewModelScope.launch {
            if (uid == null) {
                Log.w(TAG, "No user signed in")
                cleanupListeners()
                onSignOutComplete()
                return@launch
            }
            try {
                Log.d(TAG, "Setting user offline: $uid")
                database.child("Users").child(uid).child("private")
                    .updateChildren(
                        mapOf(
                            "status" to "offline",
                            "lastSeen" to ServerValue.TIMESTAMP
                        )
                    ).await()
                Log.d(TAG, "Clearing FCM token for: $uid")
                userRepository.clearCurrentUserFCMToken()
                Log.d(TAG, "Signing out from FirebaseAuth")
                auth.signOut()
                Log.i(TAG, "Sign-out successful: $uid")
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign-out for $uid", e)
                // Ensure auth signOut even if prior steps fail
                if (auth.currentUser?.uid == uid) auth.signOut()
            } finally {
                cleanupListeners()
                onSignOutComplete()
            }
        }
    }

    /**
     * Deletes the current user's account and associated data from Firebase.
     *
     * @param onComplete Callback receiving success flag and error message if any.
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
                        onComplete(false, "Account deletion failed: ${'$'}{e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Data removal failed: ${'$'}{e.message}")
            }
    }

    /**
     * Removes Firebase event listeners to avoid memory leaks.
     */
    private fun cleanupListeners() {
        connectionListener?.let { database.removeEventListener(it) }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }
}
