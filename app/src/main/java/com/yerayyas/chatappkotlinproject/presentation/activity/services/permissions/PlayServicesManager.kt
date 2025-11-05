package com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions

import android.app.Activity
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlayServicesManager"

/**
 * Manager responsible for verifying Google Play Services availability.
 */
@Singleton
class PlayServicesManager @Inject constructor() {

    /**
     * Verifies Google Play Services availability and shows dialog if update is needed.
     *
     * @param activity The activity context
     */
    fun verifyGooglePlayServices(activity: Activity) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)

        Log.d(TAG, "Google Play Services check - Result code: $resultCode")

        when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services is ready")
            }

            else -> {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    Log.w(TAG, "Google Play Services needs user action, showing dialog")
                    googleApiAvailability.getErrorDialog(activity, resultCode, 1001)?.show()
                } else {
                    Log.e(TAG, "Google Play Services error cannot be resolved")
                }
            }
        }
    }
}