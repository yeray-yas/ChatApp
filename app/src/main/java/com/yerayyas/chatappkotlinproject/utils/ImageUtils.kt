package com.yerayyas.chatappkotlinproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Converts a Bitmap into a temporary file and returns its URI.
 *
 * This function takes a Bitmap object, saves it as a temporary file in the app's cache directory,
 * and returns the URI of the saved file. The URI is generated using FileProvider to securely share
 * the file with other applications.
 *
 * @param context The application or activity context.
 * @param bitmap The Bitmap object to be converted.
 * @return The URI of the temporary file, or null if an error occurs.
 */
fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
    val filename = "temp_profile_image_${System.currentTimeMillis()}.png"
    val file = File(context.cacheDir, filename)

    return try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
