package com.hotwheels.identifier.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AssetDownloader(private val context: Context) {

    private val tag = "AssetDownloader"
    private val githubReleaseUrlImages = "https://github.com/clnaranjop/car-identifier-build/releases/download/v1.0-assets/reference_images.tar.gz"
    private val githubReleaseUrlEmbeddings = "https://github.com/clnaranjop/car-identifier-build/releases/download/v1.0-assets/embeddings_mobilenetv3.json.gz"
    private val prefs = context.getSharedPreferences("asset_downloader", Context.MODE_PRIVATE)

    fun areAssetsDownloaded(): Boolean {
        val downloaded = prefs.getBoolean("assets_downloaded", false)
        val assetsDir = File(context.filesDir, "reference_images")
        val embeddingsFile = File(context.filesDir, "embeddings_mobilenetv3.json")
        return downloaded && assetsDir.exists() && (assetsDir.listFiles()?.size ?: 0) > 50 && embeddingsFile.exists()
    }

    suspend fun downloadAssets(onProgress: (Int) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Download reference images (1.2GB)
            Log.d(tag, "Starting images download from $githubReleaseUrlImages")
            val tarFile = File(context.cacheDir, "reference_images.tar.gz")
            downloadFile(githubReleaseUrlImages, tarFile) { progress ->
                // Images take 80% of total progress
                onProgress((progress * 0.8).toInt())
            }

            // Extract images
            Log.d(tag, "Extracting images...")
            onProgress(80)
            extractTarGz(tarFile, context.filesDir)
            tarFile.delete()

            // Download embeddings (117MB)
            Log.d(tag, "Starting embeddings download from $githubReleaseUrlEmbeddings")
            val embeddingsGz = File(context.cacheDir, "embeddings_mobilenetv3.json.gz")
            downloadFile(githubReleaseUrlEmbeddings, embeddingsGz) { progress ->
                // Embeddings take 15% of total progress (80% + 15% = 95%)
                onProgress(80 + (progress * 0.15).toInt())
            }

            // Extract embeddings
            Log.d(tag, "Extracting embeddings...")
            onProgress(95)
            extractGz(embeddingsGz, File(context.filesDir, "embeddings_mobilenetv3.json"))
            embeddingsGz.delete()

            // Mark as downloaded
            prefs.edit().putBoolean("assets_downloaded", true).apply()
            onProgress(100)

            Log.d(tag, "All assets downloaded and extracted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error downloading assets", e)
            Result.failure(e)
        }
    }

    private fun downloadFile(urlString: String, outputFile: File, onProgress: (Int) -> Unit) {
        var attempt = 0
        val maxAttempts = 3
        var lastException: Exception? = null

        while (attempt < maxAttempts) {
            try {
                attempt++
                Log.d(tag, "Download attempt $attempt/$maxAttempts for ${outputFile.name}")

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection

                // Configure connection for large file downloads
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 60000 // 60 seconds
                connection.setRequestProperty("User-Agent", "DiecastCarScanner/1.0")
                connection.connect()

                val fileLength = connection.contentLength
                Log.d(tag, "File size: ${fileLength / 1024 / 1024} MB")

                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(16384) // Increased buffer size
                        var total = 0L
                        var count: Int
                        var lastProgressUpdate = System.currentTimeMillis()

                        while (input.read(buffer).also { count = it } != -1) {
                            total += count
                            output.write(buffer, 0, count)

                            // Update progress every 500ms to reduce UI updates
                            val now = System.currentTimeMillis()
                            if (fileLength > 0 && now - lastProgressUpdate > 500) {
                                val progress = ((total * 100) / fileLength).toInt()
                                onProgress(progress)
                                lastProgressUpdate = now
                            }
                        }

                        // Final progress update
                        if (fileLength > 0) {
                            onProgress(100)
                        }
                    }
                }

                Log.d(tag, "Download completed successfully for ${outputFile.name}")
                return // Success, exit retry loop

            } catch (e: Exception) {
                lastException = e
                Log.e(tag, "Download attempt $attempt failed: ${e.message}")

                // Delete partial file if it exists
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                if (attempt < maxAttempts) {
                    // Wait before retry (exponential backoff)
                    val waitTime = (attempt * 2000L)
                    Log.d(tag, "Waiting ${waitTime}ms before retry...")
                    Thread.sleep(waitTime)
                } else {
                    // All attempts failed, throw the last exception
                    throw lastException
                }
            }
        }
    }

    private fun extractTarGz(tarGzFile: File, outputDir: File) {
        val processBuilder = ProcessBuilder(
            "tar", "-xzf", tarGzFile.absolutePath, "-C", outputDir.absolutePath
        )
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().readText()
            throw Exception("Failed to extract tar.gz: $error")
        }
    }

    private fun extractGz(gzFile: File, outputFile: File) {
        val processBuilder = ProcessBuilder(
            "gunzip", "-c", gzFile.absolutePath
        )
        processBuilder.redirectOutput(outputFile)
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().readText()
            throw Exception("Failed to extract gz: $error")
        }
    }
}
