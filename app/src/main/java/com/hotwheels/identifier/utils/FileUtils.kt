package com.hotwheels.identifier.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        // Changed from cacheDir to filesDir for persistent storage
        // This prevents images from being deleted by Android's cache cleanup
        val storageDir = File(context.filesDir, "captured_images")

        // Create directory if it doesn't exist
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    fun copyUriToTempFile(context: Context, uri: Uri): File {
        val tempFile = createTempImageFile(context)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return tempFile
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }

    fun createAppDirectory(context: Context, dirName: String): File {
        val appDir = File(context.filesDir, dirName)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }

    /**
     * Clean up old captured images (older than 7 days)
     * This prevents the app from filling up storage with old photos
     */
    fun cleanupOldCapturedImages(context: Context) {
        try {
            val storageDir = File(context.filesDir, "captured_images")
            if (!storageDir.exists()) return

            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val files = storageDir.listFiles() ?: return

            var deletedCount = 0
            files.forEach { file ->
                if (file.lastModified() < sevenDaysAgo) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            if (deletedCount > 0) {
                android.util.Log.d("FileUtils", "Cleaned up $deletedCount old image(s)")
            }
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "Error cleaning old images: ${e.message}")
        }
    }

    /**
     * Get all captured images
     */
    fun getCapturedImages(context: Context): List<File> {
        val storageDir = File(context.filesDir, "captured_images")
        if (!storageDir.exists()) return emptyList()

        return storageDir.listFiles()?.toList() ?: emptyList()
    }
}