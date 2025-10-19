package com.yerayyas.chatappkotlinproject.utils

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton que rastrea el estado global de la aplicación.
 * Proporciona información sobre si la app está en primer plano
 * y qué pantalla de chat está actualmente visible (si alguna).
 * Es crucial para decidir si se debe mostrar una notificación push.
 */
@Singleton // Asegura que solo haya una instancia en toda la app (gracias a Hilt)
class AppState @Inject constructor() { // Hilt se encarga de la creación

    // Indica si algún componente de la app (Activity) está visible.
    @Volatile // Asegura visibilidad entre hilos
    var isAppInForeground: Boolean = false
        private set // Solo modificable desde dentro de esta clase

    // Almacena el ID del usuario con el que se está chateando activamente.
    // Se establece desde ChatScreen y se limpia al salir. Null si no hay chat abierto.
    @Volatile // Asegura visibilidad entre hilos
    var currentOpenChatUserId: String? = null

    // Observador del ciclo de vida de to-do el proceso de la aplicación
    private val lifecycleEventObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                isAppInForeground = true
                Log.d("AppState1", "App entered foreground.")
            }
            Lifecycle.Event.ON_STOP -> {
                isAppInForeground = false
                Log.d("AppState1", "App entered background.")
            }
            // Otros eventos del ciclo de vida no son necesarios para este propósito
            else -> Unit
        }
    }

    init {
        // Registra el observador para recibir eventos del ciclo de vida del proceso
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
        Log.d("AppState", "AppState Initialized and Lifecycle Observer added.")
    }

    // Méto-do para limpiar el observador si fuera necesario (aunque como Singleton, vive con la app)
    // fun cleanup() {
    //     ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleEventObserver)
    // }
}