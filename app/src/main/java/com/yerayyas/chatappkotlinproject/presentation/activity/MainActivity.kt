package com.yerayyas.chatappkotlinproject.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.navigation.AppContainer
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity is the entry point of the Chat App.
 *
 * This activity is annotated with @AndroidEntryPoint to enable dependency injection via Hilt.
 * It sets up the main content of the app using Jetpack Compose and applies edge-to-edge rendering.
 *
 * The UI content is composed using the [AppContainer] composable function.
 */

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inyecta el ViewModel usando el delegado de KTX
    private val activityViewModel: MainActivityViewModel by viewModels()

    // No necesitamos guardar el NavController aquí ahora
    // private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Pasamos el ViewModel a AppContainer/NavigationWrapper si es necesario
            // o puede obtenerlo directamente usando hiltViewModel() si el scope es correcto
            AppContainer(activityViewModel = activityViewModel)
        }

        Log.d(TAG, "onCreate: Handling initial intent.")
        handleIntent(intent)
    }

    // --- SOLUCIÓN BASADA EN EL ISSUE ---
    // Añade la anotación @RequiresApi(Build.VERSION_CODES.HONEYCOMB) -> Puede variar, pero es común para cosas relacionadas con Intent.
    // ¡¡CAMBIO CLAVE: Usa Intent SIN el '?' (no nulo)!!
     // O la API que sea necesaria si el compilador lo pide
    override fun onNewIntent(intent: Intent) { // <-- QUITA el '?' de Intent
        super.onNewIntent(intent) // Llama a super con el Intent no nulo
        setIntent(intent)
        Log.d(TAG, "onNewIntent: Received and set new NON-NULL intent.")
        // Ahora puedes llamar a handleIntent asumiendo que intent no es nulo aquí,
        // aunque handleIntent aún puede manejarlo internamente por seguridad para onCreate.
        handleIntent(intent)
    }
    // --- FIN SOLUCIÓN ---

    /**
     * Procesa el intent y delega la lógica de navegación al ViewModel.
     */
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val navigateTo = it.getStringExtra("navigateTo")
            val userId = it.getStringExtra("userId")
            val username = it.getStringExtra("username")

            Log.d(TAG, "handleIntent: Received navigateTo=$navigateTo, userId=$userId, username=$username")

            if (navigateTo == "chat" && userId != null && username != null) {
                // --- Delegar al ViewModel ---
                activityViewModel.setPendingNavigation(navigateTo, userId, username)

                // --- Limpiar los extras del Intent (sigue siendo buena idea) ---
                it.removeExtra("navigateTo")
                it.removeExtra("userId")
                it.removeExtra("username")
                Log.d(TAG, "Cleared navigation extras from intent after sending to ViewModel.")
            }
        }
    }
}