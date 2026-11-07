package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {

    private const val AVATAR_FILE_NAME = "user_profile_avatar.jpg"

    /**
     * Saves a picked image URI into the app's internal private storage (context.filesDir).
     * This eliminates transient content:// URI permission revocation issues where the photo
     * disappears after minutes or upon process termination.
     */
    fun saveProfilePhotoPermanently(context: Context, sourceUri: Uri): String {
        return try {
            val destinationFile = File(context.filesDir, AVATAR_FILE_NAME)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }
}
