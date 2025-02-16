package com.yerayyas.chatappkotlinproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Convierte un Bitmap en un archivo temporal y devuelve su URI.
 *
 * @param context El contexto de la aplicación o actividad.
 * @param bitmap El Bitmap a convertir.
 * @return La URI del archivo temporal o null si ocurre algún error.
 */
fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
    // Genera un nombre único para el archivo temporal.
    val filename = "temp_profile_image_${System.currentTimeMillis()}.png"
    val file = File(context.cacheDir, filename)

    return try {
        // Guarda el bitmap en el archivo como PNG.
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        // Retorna la URI usando FileProvider.
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Asegúrate de declarar este provider en el AndroidManifest.xml
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
