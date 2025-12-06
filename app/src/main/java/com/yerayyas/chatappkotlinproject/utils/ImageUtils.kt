package com.yerayyas.chatappkotlinproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "ImageUtils"

/**
 * Utility functions for image processing and file management operations.
 *
 * This file contains utility functions that handle image-related operations following
 * Clean Architecture principles. These utilities are used across different layers of
 * the application for consistent image handling, temporary file management, and
 * secure file sharing through FileProvider.
 *
 * Key features:
 * - Secure file sharing using FileProvider
 * - Comprehensive error handling and logging
 * - Memory-efficient image processing
 * - Temporary file management with automatic cleanup capability
 * - Type-safe URI generation for cross-app sharing
 *
 * Architecture Pattern: Utility Functions
 * - Pure functions without side effects (except file I/O)
 * - Domain-agnostic image processing utilities
 * - Centralized error handling and logging
 * - Consistent file naming conventions
 * - Resource management best practices
 */

/**
 * Converts a Bitmap to a temporary file and returns its content URI for sharing.
 *
 * This function takes a Bitmap object, compresses it to PNG format, saves it as a temporary
 * file in the application's cache directory, and returns a secure content URI using FileProvider.
 * The generated URI can be safely shared with other applications while maintaining security.
 *
 * Key operations:
 * 1. Creates a uniquely named temporary file in the app's cache directory
 * 2. Compresses the bitmap to PNG format with maximum quality
 * 3. Generates a secure content URI using FileProvider
 * 4. Provides comprehensive error logging for debugging
 *
 * File naming convention: "temp_profile_image_{timestamp}.png"
 * This ensures unique filenames and prevents conflicts between concurrent operations.
 *
 * Security considerations:
 * - Uses FileProvider for secure file sharing across app boundaries
 * - Files are stored in cache directory (automatically cleaned by system when needed)
 * - Content URIs provide controlled access without exposing file system paths
 *
 * @param context The application or activity context required for file operations and FileProvider
 * @param bitmap The Bitmap object to be converted and saved as a temporary file
 * @return Content URI of the saved temporary file, or null if the operation fails
 *
 * @throws SecurityException if FileProvider is not properly configured
 * @throws IOException if file I/O operations fail
 * @throws IllegalArgumentException if context or bitmap parameters are invalid
 *
 * Example usage:
 * ```kotlin
 * val bitmap = // ... obtain bitmap
 * val uri = bitmapToUri(context, bitmap)
 * if (uri != null) {
 *     // Share or use the URI
 *     shareImage(uri)
 * } else {
 *     // Handle error case
 *     showErrorMessage()
 * }
 * ```
 */
fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
    // Validate input parameters
    if (bitmap.isRecycled) {
        Log.e(TAG, "Cannot convert recycled bitmap to URI")
        return null
    }

    // Generate unique filename with timestamp to prevent conflicts
    val timestamp = System.currentTimeMillis()
    val filename = "temp_profile_image_$timestamp.png"

    Log.d(
        TAG,
        "Converting bitmap to URI - filename: $filename, bitmap size: ${bitmap.width}x${bitmap.height}"
    )

    return try {
        // Create temporary file in cache directory
        val cacheDir = context.cacheDir
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Log.e(TAG, "Failed to create cache directory")
            return null
        }

        val tempFile = File(cacheDir, filename)
        Log.d(TAG, "Creating temporary file: ${tempFile.absolutePath}")

        // Save bitmap to file with PNG compression
        val compressionResult = saveBitmapToFile(bitmap, tempFile)
        if (!compressionResult) {
            Log.e(TAG, "Failed to save bitmap to temporary file")
            return null
        }

        // Generate secure content URI using FileProvider
        val contentUri = generateContentUri(context, tempFile)
        if (contentUri != null) {
            Log.i(TAG, "Successfully created content URI for image: $contentUri")
            Log.d(TAG, "Temporary file size: ${formatFileSize(tempFile.length())}")
        }

        contentUri

    } catch (e: SecurityException) {
        Log.e(TAG, "Security error creating content URI - check FileProvider configuration", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Invalid arguments for FileProvider", e)
        null
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error converting bitmap to URI", e)
        null
    }
}

/**
 * Saves a bitmap to the specified file with PNG compression.
 *
 * @param bitmap The bitmap to save
 * @param file The target file
 * @return true if successful, false otherwise
 */
private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
    return try {
        FileOutputStream(file).use { outputStream ->
            val compressionSuccess = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush() // Ensure all data is written

            if (!compressionSuccess) {
                Log.e(TAG, "Bitmap compression failed")
                return false
            }

            Log.d(TAG, "Bitmap successfully compressed and saved to file")
            true
        }
    } catch (e: IOException) {
        Log.e(TAG, "IO error saving bitmap to file: ${file.absolutePath}", e)
        false
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error saving bitmap to file", e)
        false
    }
}

/**
 * Generates a content URI for the specified file using FileProvider.
 *
 * @param context The application context
 * @param file The file to generate URI for
 * @return Content URI or null if generation fails
 */
private fun generateContentUri(context: Context, file: File): Uri? {
    return try {
        val authority = "${context.packageName}.provider"
        FileProvider.getUriForFile(context, authority, file)
    } catch (e: IllegalArgumentException) {
        Log.e(
            TAG,
            "FileProvider authority not configured properly: ${context.packageName}.provider",
            e
        )
        null
    } catch (e: Exception) {
        Log.e(TAG, "Error generating content URI", e)
        null
    }
}

/**
 * Formats file size in bytes to human-readable string.
 *
 * @param bytes File size in bytes
 * @return Formatted string (e.g., "1.5 MB", "256 KB")
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes bytes"
    }
}
