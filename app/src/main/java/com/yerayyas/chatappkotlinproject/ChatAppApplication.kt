package com.yerayyas.chatappkotlinproject

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.yerayyas.chatappkotlinproject.domain.usecases.user.ManageUserPresenceUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point and Dependency Injection root.
 *
 * This class initializes the Hilt dependency graph and manages the application-wide
 * lifecycle to track the user's online presence. By implementing [DefaultLifecycleObserver]
 * and observing [ProcessLifecycleOwner], it can detect when the app as a whole moves
 * between foreground and background states, regardless of individual Activity transitions.
 *
 * Key responsibilities:
 * - bootstrapping Hilt ([@HiltAndroidApp]).
 * - Global lifecycle monitoring (App-level vs Activity-level).
 * - delegating presence logic (Online/Offline) to the Domain layer.
 */
@HiltAndroidApp
class ChatAppApplication : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var manageUserPresenceUseCase: ManageUserPresenceUseCase

    /**
     * Called when the application process is starting.
     *
     * Registers this class as an observer of the process lifecycle. This ensures
     * that `onStart` and `onStop` are triggered based on the application's
     * composite visibility state.
     */
    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Triggered when the application moves to the **foreground**.
     *
     * This occurs when the first Activity becomes visible. We delegate to the
     * use case to mark the user as "online" in the remote database and cancel
     * any pending "last seen" timestamps.
     *
     * @param owner The lifecycle owner (ProcessLifecycleOwner).
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        manageUserPresenceUseCase.startPresenceUpdates()
    }

    /**
     * Triggered when the application moves to the **background**.
     *
     * This occurs when the last visible Activity is stopped (user presses Home,
     * switches apps, or locks the screen). We delegate to the use case to mark
     * the user as "offline" and update the "last seen" timestamp.
     *
     * @param owner The lifecycle owner (ProcessLifecycleOwner).
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        manageUserPresenceUseCase.stopPresenceUpdates()
    }
}
