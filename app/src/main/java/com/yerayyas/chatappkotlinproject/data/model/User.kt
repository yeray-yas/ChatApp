package com.yerayyas.chatappkotlinproject.data.model

// Data class para el modelo de usuario
data class User(
    val id: String,          // Mapea con "userId" en Firebase
    val username: String,    // Mapea directo
    val email: String,       // Mapea directo
    val profileImage: String,// Mapea con "image" en Firebase
    val status: String,      // Mapea directo ("online"/"offline")
    val isOnline: Boolean,   // Se calcula desde "status"
    val lastSeen: Long = 0   // No está en Firebase actualmente 
)
