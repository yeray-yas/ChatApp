package com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions

import android.app.Activity
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlayServicesManager"
private const val PLAY_SERVICES_RESOLUTION_REQUEST = 1001

/**
 * Manager responsible for verifying and handling Google Play Services availability.
 *
 * This manager encapsulates all logic related to Google Play Services verification,
 * including availability checking, error handling, and user resolution dialogs.
 * It's essential for apps that rely on Firebase services, Google APIs, or other
 * Google Play Services-dependent functionality.
 *
 * Key responsibilities:
 * - Checking Google Play Services availability on the device
 * - Handling different availability scenarios (missing, outdated, disabled)
 * - Presenting resolution dialogs to users when possible
 * - Providing comprehensive logging for debugging service issues
 * - Managing error recovery and graceful degradation
 *
 * Architecture Pattern: Manager Pattern
 * - Encapsulates Google Play Services-related operations
 * - Provides a clean interface for service verification
 * - Handles different error scenarios transparently
 * - Includes comprehensive error handling and user guidance
 * - Follows single responsibility principle
 *
 * Common use cases:
 * - Firebase Cloud Messaging setup
 * - Google authentication verification
 * - Location services preparation
 * - General Google API prerequisite checking
 */
@Singleton
class PlayServicesManager @Inject constructor() {

    /**
     * Verifies Google Play Services availability and handles resolution if needed.
     *
     * This method performs a comprehensive check of Google Play Services status
     * and attempts to guide the user through resolution if services are missing,
     * outdated, or disabled. It handles various scenarios gracefully.
     *
     * Verification scenarios handled:
     * - SUCCESS: Services are available and up-to-date
     * - SERVICE_MISSING: Play Services not installed
     * - SERVICE_VERSION_UPDATE_REQUIRED: Update needed
     * - SERVICE_DISABLED: Services disabled by user
     * - SERVICE_INVALID: Invalid installation
     * - Other resolvable errors: Shows resolution dialog
     * - Unresolvable errors: Logs error and continues with degraded functionality
     *
     * @param activity The activity context required for displaying resolution dialogs
     * @throws SecurityException if Play Services verification encounters security issues
     */
    fun verifyGooglePlayServices(activity: Activity) {
        try {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)

            Log.d(TAG, "Google Play Services verification - Result code: $resultCode")

            when (resultCode) {
                ConnectionResult.SUCCESS -> {
                    handlePlayServicesAvailable()
                }

                ConnectionResult.SERVICE_MISSING -> {
                    handlePlayServicesMissing(activity, googleApiAvailability, resultCode)
                }

                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    handlePlayServicesUpdateRequired(activity, googleApiAvailability, resultCode)
                }

                ConnectionResult.SERVICE_DISABLED -> {
                    handlePlayServicesDisabled(activity, googleApiAvailability, resultCode)
                }

                ConnectionResult.SERVICE_INVALID -> {
                    handlePlayServicesInvalid(activity, googleApiAvailability, resultCode)
                }

                else -> {
                    handleOtherPlayServicesError(activity, googleApiAvailability, resultCode)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Play Services verification", e)
            // Continue execution with degraded functionality
        }
    }

    /**
     * Handles the scenario where Google Play Services are available and ready.
     */
    private fun handlePlayServicesAvailable() {
        Log.i(TAG, "Google Play Services verified successfully - Full functionality available")
    }

    /**
     * Handles the scenario where Google Play Services are missing from the device.
     */
    private fun handlePlayServicesMissing(
        activity: Activity,
        googleApiAvailability: GoogleApiAvailability,
        resultCode: Int
    ) {
        Log.w(TAG, "Google Play Services are missing - attempting to show resolution dialog")
        showResolutionDialogIfPossible(activity, googleApiAvailability, resultCode)
    }

    /**
     * Handles the scenario where Google Play Services need to be updated.
     */
    private fun handlePlayServicesUpdateRequired(
        activity: Activity,
        googleApiAvailability: GoogleApiAvailability,
        resultCode: Int
    ) {
        Log.w(TAG, "Google Play Services update required - attempting to show update dialog")
        showResolutionDialogIfPossible(activity, googleApiAvailability, resultCode)
    }

    /**
     * Handles the scenario where Google Play Services are disabled by the user.
     */
    private fun handlePlayServicesDisabled(
        activity: Activity,
        googleApiAvailability: GoogleApiAvailability,
        resultCode: Int
    ) {
        Log.w(TAG, "Google Play Services are disabled - attempting to show enable dialog")
        showResolutionDialogIfPossible(activity, googleApiAvailability, resultCode)
    }

    /**
     * Handles the scenario where Google Play Services installation is invalid.
     */
    private fun handlePlayServicesInvalid(
        activity: Activity,
        googleApiAvailability: GoogleApiAvailability,
        resultCode: Int
    ) {
        Log.w(TAG, "Google Play Services installation is invalid - attempting resolution")
        showResolutionDialogIfPossible(activity, googleApiAvailability, resultCode)
    }

    /**
     * Handles other Google Play Services errors not specifically categorized.
     */
    private fun handleOtherPlayServicesError(
        activity: Activity,
        googleApiAvailability: GoogleApiAvailability,
        resultCode: Int
    ) {
        Log.w(TAG, "Google Play Services error (code: $resultCode) - attempting resolution")
        showResolutionDialogIfPossible(activity, googleApiAvailability, resultCode)
    }

    /**
     * Shows a resolution dialog to the user if the error is resolvable.
     *
     * This method centralizes the logic for displaying Play Services resolution dialogs,
     * including error handling for cases where dialogs cannot be shown.
     *
     * @param activity The activity context for dialog display
     * @param googleApiAvailability The Google API availability instance
     * @param resultCode The error code from Play Services check
     */
    private fun showResolutionDialogIfPossible(
        activity: Activity,
        googleApiAvailability: GoogleApiAvailability,
        resultCode: Int
    ) {
        try {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                Log.d(
                    TAG,
                    "Showing Google Play Services resolution dialog for error code: $resultCode"
                )
                val dialog = googleApiAvailability.getErrorDialog(
                    activity,
                    resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                )

                dialog?.show() ?: run {
                    Log.w(TAG, "Unable to create resolution dialog for error code: $resultCode")
                }
            } else {
                Log.e(
                    TAG,
                    "Google Play Services error cannot be resolved by user (code: $resultCode)"
                )
                handleUnresolvableError(resultCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Google Play Services resolution dialog", e)
        }
    }

    /**
     * Handles unresolvable Google Play Services errors.
     *
     * This method provides guidance for scenarios where the user cannot resolve
     * the Play Services issue, such as on devices without Google services.
     * It logs specific error information and could implement fallback strategies
     * based on the error type.
     *
     * @param resultCode The specific error code that cannot be resolved
     */
    private fun handleUnresolvableError(resultCode: Int) {
        val errorDescription = getErrorDescription(resultCode)
        Log.w(
            TAG,
            "Google Play Services error is unresolvable - Error: $errorDescription (code: $resultCode)"
        )
        Log.w(TAG, "App will continue with limited functionality")

        // Future implementations could handle specific error types differently
        when (resultCode) {
            ConnectionResult.SERVICE_MISSING -> {
                Log.i(
                    TAG,
                    "Consider implementing fallback functionality for devices without Google Play Services"
                )
            }

            ConnectionResult.SERVICE_INVALID -> {
                Log.i(TAG, "Consider suggesting manual Play Services reinstallation to user")
            }

            else -> {
                Log.i(TAG, "Continuing with degraded functionality for error code: $resultCode")
            }
        }

        // Future: Could implement fallback strategies, analytics reporting, etc.
        // - reportErrorToAnalytics(resultCode)
        // - enableFallbackMode(resultCode)
        // - showUserGuidanceMessage(resultCode)
    }

    /**
     * Provides human-readable descriptions for Google Play Services error codes.
     *
     * @param resultCode The error code from Google Play Services
     * @return A descriptive string explaining the error
     */
    private fun getErrorDescription(resultCode: Int): String {
        return when (resultCode) {
            ConnectionResult.SERVICE_MISSING -> "Google Play Services not installed"
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Google Play Services update required"
            ConnectionResult.SERVICE_DISABLED -> "Google Play Services disabled"
            ConnectionResult.SERVICE_INVALID -> "Google Play Services installation is invalid"
            ConnectionResult.NETWORK_ERROR -> "Network error occurred"
            ConnectionResult.INTERNAL_ERROR -> "Internal error in Google Play Services"
            ConnectionResult.INVALID_ACCOUNT -> "Invalid account selected"
            ConnectionResult.RESOLUTION_REQUIRED -> "Resolution required but not available"
            ConnectionResult.SIGN_IN_REQUIRED -> "Sign-in required"
            ConnectionResult.SERVICE_UPDATING -> "Google Play Services currently updating"
            ConnectionResult.SERVICE_MISSING_PERMISSION -> "Missing required permissions"
            ConnectionResult.RESTRICTED_PROFILE -> "Restricted profile detected"
            else -> "Unknown Google Play Services error"
        }
    }
}
