package com.yerayyas.chatappkotlinproject.presentation.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.ProcessNotificationIntentUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.navigation.AppContainer
import com.yerayyas.chatappkotlinproject.utils.XiaomiPermissionHelper
import com.yerayyas.chatappkotlinproject.utils.FCMDiagnostics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.yerayyas.chatappkotlinproject.notifications.diagnostics.NotificationDiagnostics
import com.google.firebase.messaging.FirebaseMessaging

private const val TAG = "MainActivity"

/**
 * The main and single activity of the application, serving as the entry point for the user interface.
 *
 * This activity is responsible for:
 * - Setting up the Jetpack Compose content with edge-to-edge display.
 * - Handling the initial intent on app launch (cold or warm start) to process potential notification deep-links.
 * - Managing new intents received while the activity is running (e.g., from a notification click).
 * - Clearing all active chat notifications when the app comes to the foreground to provide a clean state.
 * - Coordinating with various use cases to handle navigation logic based on the app's state and incoming intents.
 *
 * It uses Hilt for dependency injection to get instances of ViewModels and use cases.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val activityViewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var processNotificationIntent: ProcessNotificationIntentUseCase

    @Inject
    lateinit var handleNotificationNavigation: HandleNotificationNavigationUseCase

    @Inject
    lateinit var handleDefaultNavigation: HandleDefaultNavigationUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    // Request notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(TAG, "Notification permission result: $isGranted")
        if (isGranted) {
            Log.i(TAG, "POST_NOTIFICATIONS permission granted")
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied")
        }
        if (!isGranted) {
            Log.w(TAG, "Notification permission denied by user")
        }
    }

    companion object {
        /** Tracks whether the application has been initialized to distinguish a cold start from a warm start. */
        private var isAppInitialized = false
    }

    /**
     * Called when the activity is first created. This is where the UI is initialized and the initial intent is processed.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TEMPORARY DEBUG LOGGING - VERY VISIBLE
        Log.e("XIAOMI_DEBUG", "=".repeat(60))
        Log.e("XIAOMI_DEBUG", "APP STARTING ON DEVICE: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.e("XIAOMI_DEBUG", "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.e("XIAOMI_DEBUG", "Package: $packageName")
        Log.e("XIAOMI_DEBUG", "Time: ${System.currentTimeMillis()}")
        Log.e("XIAOMI_DEBUG", "=".repeat(60))

        val isAppAlreadyRunning = isAppInitialized || savedInstanceState != null
        Log.d(TAG, "onCreate: isAppAlreadyRunning=$isAppAlreadyRunning, isAppInitialized=$isAppInitialized, savedInstanceState=${savedInstanceState != null}")

        // Mark app as initialized on its first creation
        isAppInitialized = true

        // Log device information for debugging - especially important for Xiaomi
        XiaomiPermissionHelper.logDeviceInfo()

        // Verify notification permissions and Google Play Services
        verifyGooglePlayServices()
        requestNotificationPermissionIfNeeded()

        // Check Xiaomi-specific settings
        checkXiaomiSpecificSettings()

        // Dismiss all notifications to ensure a clean slate when the app is brought to the foreground.
        notificationHelper.cancelAllNotifications()

        // Check notification permissions and setup
        checkAndRequestNotificationPermissions()

        // Run notification diagnostics
        runNotificationDiagnostics()

        // FORCE LOG THE FCM TOKEN IMMEDIATELY
        lifecycleScope.launch {
            try {
                Log.e("XIAOMI_DEBUG", "FORCING FCM TOKEN RETRIEVAL...")
                val token = FirebaseMessaging.getInstance().token.await()
                Log.e("XIAOMI_DEBUG", " FCM TOKEN SUCCESS!")
                Log.e("XIAOMI_DEBUG", "Token length: ${token.length}")
                Log.e("XIAOMI_DEBUG", "Token start: ${token.take(30)}")
                Log.e("XIAOMI_DEBUG", "Token end: ${token.takeLast(30)}")
            } catch (e: Exception) {
                Log.e("XIAOMI_DEBUG", " FCM TOKEN FAILED: ${e.message}", e)
            }
        }

        // Process any notification or deep-link from the initial intent before rendering the UI.
        val initialNavState = processInitialIntent(intent, isAppAlreadyRunning)

        setContent {
            ProvideWindowInsets {
                AppContainer(
                    activityViewModel = activityViewModel,
                    handleNotificationNavigation = handleNotificationNavigation,
                    handleDefaultNavigation = handleDefaultNavigation,
                    skipSplash = isAppAlreadyRunning,
                    initialNavState = initialNavState
                )
            }
        }
    }

    /**
     * Verifies Google Play Services availability and updates if necessary.
     */
    private fun verifyGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        Log.d(TAG, "=== MAIN ACTIVITY - GOOGLE PLAY SERVICES CHECK ===")
        Log.d(TAG, "Result code: $resultCode")

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                Log.w(TAG, "Google Play Services needs user action, showing dialog")
                googleApiAvailability.getErrorDialog(this, resultCode, 1001)?.show()
            } else {
                Log.e(TAG, "Google Play Services error cannot be resolved")
            }
        } else {
            Log.d(TAG, "Google Play Services is ready")
        }
    }

    /**
     * Requests notification permission on Android 13+ if not already granted.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(
                TAG,
                "Notification permission status: $hasPermission (API ${Build.VERSION.SDK_INT})"
            )

            if (!hasPermission) {
                Log.d(TAG, "Requesting notification permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Log.d(
                TAG,
                "Android version ${Build.VERSION.SDK_INT} - notification permission not required"
            )
        }
    }

    /**
     * Checks Xiaomi-specific settings and requests necessary permissions if on a Xiaomi device.
     */
    private fun checkXiaomiSpecificSettings() {
        if (XiaomiPermissionHelper.isXiaomiDevice()) {
            Log.w(TAG, "Xiaomi device detected - checking special settings")

            lifecycleScope.launch {
                try {
                    // Run comprehensive FCM diagnostics
                    Log.d(TAG, "Running comprehensive FCM diagnostics...")
                    val diagnosticsReport = FCMDiagnostics.runCompleteDiagnostics(this@MainActivity)

                    // Also run quick status check
                    FCMDiagnostics.logCurrentStatus(this@MainActivity)

                    // Test FCM connectivity
                    val fcmTest = FCMDiagnostics.testFCMConnectivity()
                    Log.d(
                        TAG,
                        "FCM Connectivity Test: ${if (fcmTest.success) "SUCCESS" else "FAILED"}"
                    )
                    if (fcmTest.success) {
                        Log.d(TAG, "FCM Token Preview: ${fcmTest.token?.take(20)}...")
                        Log.d(TAG, "Token retrieval time: ${fcmTest.responseTimeMs}ms")
                    } else {
                        Log.e(TAG, "FCM Connectivity Error: ${fcmTest.error}")

                        // Try to refresh token on Xiaomi devices if connectivity fails
                        Log.w(TAG, "Attempting FCM token refresh for Xiaomi device...")
                        val refreshSuccess = FCMDiagnostics.forceTokenRefresh()
                        Log.d(
                            TAG,
                            "FCM Token refresh: ${if (refreshSuccess) "SUCCESS" else "FAILED"}"
                        )
                    }

                    val status =
                        XiaomiPermissionHelper.checkXiaomiNotificationSettings(this@MainActivity)

                    Log.d(TAG, "Xiaomi notification status: $status")

                    if (!status.isFullyConfigured()) {
                        val missing = status.getMissingConfigurations()
                        Log.w(TAG, "Missing Xiaomi configurations: $missing")

                        // For now, just log the issue. In a production app, you might want to show a dialog
                        // guiding the user to manually configure these settings
                        Log.w(TAG, "User needs to manually configure Xiaomi settings:")
                        Log.w(
                            TAG,
                            "1. Settings > Apps > ${packageName} > Battery > No restrictions"
                        )
                        Log.w(
                            TAG,
                            "2. Settings > Apps > Manage apps > ${packageName} > Autostart > Enable"
                        )
                        Log.w(TAG, "3. Settings > Notifications > ${packageName} > Enable all")
                        Log.w(TAG, "4. Security app > Apps > ${packageName} > Enable autostart")
                        Log.w(TAG, "5. RESTART DEVICE after making these changes")

                        // Optionally request battery optimization exception
                        if (!status.batteryOptimizationDisabled) {
                            Log.d(
                                TAG,
                                "Requesting battery optimization exception for Xiaomi device"
                            )
                            XiaomiPermissionHelper.requestXiaomiPermissions(this@MainActivity)
                        }
                    } else {
                        Log.d(TAG, "All Xiaomi settings properly configured")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking Xiaomi settings", e)
                }
            }
        } else {
            // For non-Xiaomi devices, still run basic FCM diagnostics
            lifecycleScope.launch {
                try {
                    FCMDiagnostics.logCurrentStatus(this@MainActivity)
                    val fcmTest = FCMDiagnostics.testFCMConnectivity()
                    Log.d(
                        TAG,
                        "FCM Test (Non-Xiaomi): ${if (fcmTest.success) "SUCCESS" else "FAILED"}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error running FCM diagnostics", e)
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "POST_NOTIFICATIONS permission already granted")
                }

                else -> {
                    Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS not required for API level < 33")
        }
    }

    private fun runNotificationDiagnostics() {
        lifecycleScope.launch {
            try {
                val diagnostics = NotificationDiagnostics(this@MainActivity)
                val report = diagnostics.runFullDiagnostics()
                Log.d(TAG, "Notification Diagnostics Report:")
                Log.d(TAG, report)

                // If there are critical issues, try to fix them
                if (!diagnostics.areNotificationsEnabled()) {
                    Log.w(TAG, "Notifications disabled - requesting permissions")
                    checkAndRequestNotificationPermissions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running notification diagnostics", e)
            }
        }
    }

    /**
     * Called by the system when the activity is started with a new intent while it is already running.
     * This is common when the user taps on a notification and the app is in the background.
     *
     * @param intent The new intent that was received.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        Log.d(TAG, "onNewIntent: A new intent has been received while the app is running.")
        handleNotificationIntent(intent)
    }

    /**
     * Called when the activity is becoming visible to the user.
     * We clear notifications here as well to handle cases where the user returns to the app without a new intent.
     */
    override fun onStart() {
        super.onStart()
        notificationHelper.cancelAllNotifications()
    }

    /**
     * Processes the initial intent when the activity is created to check for navigation data from a notification.
     *
     * @param intent The intent that started the activity.
     * @param isAppRunning A flag indicating if this is a cold start or a warm start.
     * @return A [NotificationNavigationState] object if navigation data is found, otherwise null.
     */
    private fun processInitialIntent(
        intent: Intent?,
        isAppAlreadyRunning: Boolean
    ): NotificationNavigationState? {
        Log.d(TAG, "Processing initial intent. isAppAlreadyRunning: $isAppAlreadyRunning")
        return processNotificationIntent(intent)?.let { state ->
            val initialState = state.copy(
                skipSplash = isAppAlreadyRunning,
                isInitialDestination = true
            )
            Log.d(TAG, "Initial navigation state extracted: $initialState")
            clearIntentExtras(intent)
            initialState
        }
    }

    /**
     * Handles a new intent by extracting navigation data and queuing it in the [MainActivityViewModel].
     * The navigation is queued to be consumed by the UI layer safely.
     *
     * @param intent The new intent received.
     */
    private fun handleNotificationIntent(intent: Intent?) {
        Log.d(TAG, "Handling a new notification intent.")
        processNotificationIntent(intent)?.let { state ->
            Log.d(TAG, "Queuing pending navigation state: $state")
            activityViewModel.setPendingNavigation(
                state.navigateTo,
                state.userId,
                state.username,
                skipSplash = true
            )
            clearIntentExtras(intent)
        } ?: Log.d(TAG, "No navigation state could be extracted from the new intent.")
    }

    /**
     * Clears the navigation-related extras from the given intent to prevent them from being processed again
     * on configuration changes or activity recreation.
     *
     * @param intent The intent from which to clear extras.
     */
    private fun clearIntentExtras(intent: Intent?) {
        intent?.removeExtra("navigateTo")
        intent?.removeExtra("userId")
        intent?.removeExtra("username")
    }
}
