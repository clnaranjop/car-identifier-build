package com.hotwheels.identifier.utils

import android.content.Context
import android.util.Log
import com.hotwheels.identifier.data.models.UpdateInfo
import com.hotwheels.identifier.data.models.UpdateManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Handles incremental database updates
 * Downloads only new collections instead of the entire database
 */
class IncrementalAssetDownloader(private val context: Context) {

    companion object {
        private const val TAG = "IncrementalDownloader"
        private const val MANIFEST_URL = "https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/latest/manifest.json"
        private const val PREFS_NAME = "database_updates"
        private const val KEY_DATABASE_VERSION = "database_version"
        private const val KEY_INSTALLED_COLLECTIONS = "installed_collections"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if there are updates available
     */
    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates...")

            // Download manifest.json
            val manifest = downloadManifest()

            // Get installed version and collections
            val installedVersion = prefs.getString(KEY_DATABASE_VERSION, "0.0") ?: "0.0"
            val installedCollections = prefs.getStringSet(KEY_INSTALLED_COLLECTIONS, emptySet()) ?: emptySet()

            Log.d(TAG, "Installed version: $installedVersion")
            Log.d(TAG, "Installed collections: ${installedCollections.joinToString(", ")}")

            // Determine missing collections
            val missingCollections = manifest.collections.filter { (key, collection) ->
                if (installedCollections.contains(key)) {
                    false
                } else {
                    // Check if all required collections are installed
                    collection.requires.all { installedCollections.contains(it) }
                }
            }

            // Calculate total size and model count
            val totalSize = missingCollections.values.sumOf { it.sizeMb }
            val newModelCount = missingCollections.values.sumOf { it.modelCount }

            Log.d(TAG, "Available updates: ${missingCollections.size} collections")
            Log.d(TAG, "Total size: ${totalSize}MB, New models: $newModelCount")

            UpdateInfo(
                hasUpdates = missingCollections.isNotEmpty(),
                collections = missingCollections,
                totalSizeMB = totalSize,
                newModelCount = newModelCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            UpdateInfo(
                hasUpdates = false,
                collections = emptyMap(),
                totalSizeMB = 0,
                newModelCount = 0
            )
        }
    }

    /**
     * Download and install updates
     */
    suspend fun downloadUpdates(
        updateInfo: UpdateInfo,
        onProgress: (Int, String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val collections = updateInfo.collections
            var completedCollections = 0

            for ((name, collection) in collections) {
                Log.d(TAG, "Processing collection: $name")

                // Download images for this collection
                onProgress(
                    (completedCollections * 100) / collections.size,
                    "Descargando $name..."
                )

                val imagesFile = File(context.cacheDir, "${name}_images.tar.gz")
                downloadFile(collection.files.images, imagesFile) { progress ->
                    val overallProgress = (completedCollections * 100 + progress / 2) / collections.size
                    onProgress(overallProgress, "Descargando imágenes de $name... $progress%")
                }

                // Verify checksum
                if (!verifyChecksum(imagesFile, collection.checksum.images)) {
                    imagesFile.delete()
                    throw Exception("Checksum verification failed for images of $name")
                }

                // Extract images
                onProgress(
                    (completedCollections * 100 + 50) / collections.size,
                    "Extrayendo imágenes de $name..."
                )
                extractTarGz(imagesFile, File(context.filesDir, "reference_images"))
                imagesFile.delete()

                // Download embeddings for this collection
                val embeddingsFile = File(context.cacheDir, "${name}_embeddings.json.gz")
                downloadFile(collection.files.embeddings, embeddingsFile) { progress ->
                    val overallProgress = (completedCollections * 100 + 50 + progress / 2) / collections.size
                    onProgress(overallProgress, "Descargando embeddings de $name... $progress%")
                }

                // Verify checksum
                if (!verifyChecksum(embeddingsFile, collection.checksum.embeddings)) {
                    embeddingsFile.delete()
                    throw Exception("Checksum verification failed for embeddings of $name")
                }

                // Merge embeddings with main file
                onProgress(
                    ((completedCollections + 1) * 100) / collections.size,
                    "Procesando $name..."
                )
                mergeEmbeddings(embeddingsFile, collection.years)
                embeddingsFile.delete()

                // Mark collection as installed
                val installed = prefs.getStringSet(KEY_INSTALLED_COLLECTIONS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                installed.add(name)
                prefs.edit().putStringSet(KEY_INSTALLED_COLLECTIONS, installed).apply()

                Log.d(TAG, "Collection $name installed successfully")
                completedCollections++
            }

            // Update database version
            val manifest = downloadManifest()
            prefs.edit()
                .putString(KEY_DATABASE_VERSION, manifest.version)
                .putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis())
                .apply()

            Log.d(TAG, "All updates installed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading updates", e)
            Result.failure(e)
        }
    }

    /**
     * Download manifest.json from GitHub
     */
    private fun downloadManifest(): UpdateManifest {
        val url = URL(MANIFEST_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        try {
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            return UpdateManifest.fromJson(jsonObject)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Download a file with progress callback
     */
    private fun downloadFile(
        urlString: String,
        outputFile: File,
        onProgress: (Int) -> Unit
    ) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        try {
            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (inputStream.read(buffer).also { count = it } != -1) {
                total += count
                outputStream.write(buffer, 0, count)

                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Verify file checksum (MD5)
     */
    private fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        if (expectedChecksum.isEmpty() || expectedChecksum == "CALCULATE_MD5_OF_TAR_GZ" ||
            expectedChecksum == "CALCULATE_MD5_OF_JSON_GZ") {
            // Skip verification if checksum is placeholder
            Log.w(TAG, "Skipping checksum verification (placeholder value)")
            return true
        }

        try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }

            inputStream.close()

            val digest = md.digest()
            val checksum = digest.joinToString("") { "%02x".format(it) }

            val matches = checksum.equals(expectedChecksum, ignoreCase = true)
            if (!matches) {
                Log.e(TAG, "Checksum mismatch! Expected: $expectedChecksum, Got: $checksum")
            }

            return matches
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying checksum", e)
            return false
        }
    }

    /**
     * Extract tar.gz file
     */
    private fun extractTarGz(tarGzFile: File, outputDir: File) {
        // For now, we'll use a simple implementation
        // In production, consider using Apache Commons Compress or similar
        Log.d(TAG, "Extracting ${tarGzFile.name} to ${outputDir.absolutePath}")

        try {
            val process = ProcessBuilder(
                "tar", "-xzf", tarGzFile.absolutePath, "-C", outputDir.absolutePath
            ).start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                throw Exception("tar extraction failed: $error")
            }

            Log.d(TAG, "Extraction completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting tar.gz", e)
            throw e
        }
    }

    /**
     * Merge new embeddings with existing file
     */
    private fun mergeEmbeddings(newEmbeddingsGz: File, years: String) {
        Log.d(TAG, "Merging embeddings for years: $years")

        // Extract new embeddings
        val newEmbeddings = File(context.cacheDir, "temp_embeddings.json")
        extractGz(newEmbeddingsGz, newEmbeddings)

        // Read existing embeddings
        val existingFile = File(context.filesDir, "embeddings_mobilenetv3.json")
        val existingJson = if (existingFile.exists()) {
            JSONObject(existingFile.readText())
        } else {
            JSONObject()
        }

        // Read new embeddings
        val newJson = JSONObject(newEmbeddings.readText())

        // Merge models arrays
        val newModels = newJson.getJSONArray("models")
        val existingModels = existingJson.optJSONArray("models") ?: JSONArray()

        for (i in 0 until newModels.length()) {
            existingModels.put(newModels.getJSONObject(i))
        }

        existingJson.put("models", existingModels)
        existingJson.put("version", newJson.optString("version", "unknown"))
        existingJson.put("last_updated", System.currentTimeMillis())

        // Save merged file
        existingFile.writeText(existingJson.toString(2))

        newEmbeddings.delete()
        Log.d(TAG, "Embeddings merged successfully. Total models: ${existingModels.length()}")
    }

    /**
     * Extract .gz file
     */
    private fun extractGz(gzFile: File, outputFile: File) {
        val inputStream = GZIPInputStream(FileInputStream(gzFile))
        val outputStream = FileOutputStream(outputFile)

        val buffer = ByteArray(8192)
        var len: Int

        while (inputStream.read(buffer).also { len = it } > 0) {
            outputStream.write(buffer, 0, len)
        }

        inputStream.close()
        outputStream.close()
    }

    /**
     * Get installed database version
     */
    fun getInstalledVersion(): String {
        return prefs.getString(KEY_DATABASE_VERSION, "0.0") ?: "0.0"
    }

    /**
     * Get installed collections
     */
    fun getInstalledCollections(): Set<String> {
        return prefs.getStringSet(KEY_INSTALLED_COLLECTIONS, emptySet()) ?: emptySet()
    }

    /**
     * Mark base collection as installed (for migration from old version)
     */
    fun markBaseCollectionInstalled() {
        val installed = prefs.getStringSet(KEY_INSTALLED_COLLECTIONS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        installed.add("base")
        prefs.edit()
            .putStringSet(KEY_INSTALLED_COLLECTIONS, installed)
            .putString(KEY_DATABASE_VERSION, "1.0")
            .apply()
        Log.d(TAG, "Base collection marked as installed")
    }
}
