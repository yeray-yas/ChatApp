package com.yerayyas.chatappkotlinproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
    Log.d("BitmapToUri", "Función llamada") // Verifica que se está llamando
    val filename = "temp_profile_image_${System.currentTimeMillis()}.png"
    val file = File(context.cacheDir, filename)

    if (file.exists()) {
        Log.d("BitmapToUri", "Archivo creado en: ${file.absolutePath}, tamaño: ${file.length()} bytes")
    } else {
        Log.e("BitmapToUri", "El archivo no se creó en: ${file.absolutePath}")
    }

    return try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        Log.d("BitmapToUri", "Imagen guardada en el archivo correctamente.")

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        ).also { uri ->
            Log.d("BitmapToUri", "URI generada: $uri")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("BitmapToUri", "Error al convertir Bitmap a URI: ${e.message}")
        null
    }
}
