package com.hotwheels.identifier.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Extrae frames de un video para identificación
 */
class VideoFrameExtractor(private val context: Context) {

    companion object {
        private const val TAG = "VideoFrameExtractor"
        private const val FRAMES_TO_EXTRACT = 5  // Número de frames a extraer
        private const val MIN_FRAME_INTERVAL_MS = 200L  // Mínimo 200ms entre frames
    }

    /**
     * Extrae frames del video y los guarda como imágenes
     * @param videoPath Ruta del archivo de video
     * @return Lista de rutas de las imágenes extraídas
     */
    fun extractFrames(videoPath: String): List<String> {
        Log.d(TAG, "═══════════════════════════════════════════════════════")
        Log.d(TAG, "🎬 VIDEO FRAME EXTRACTION START")
        Log.d(TAG, "   Video: $videoPath")
        Log.d(TAG, "═══════════════════════════════════════════════════════")

        val framePaths = mutableListOf<String>()
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(videoPath)

            // Obtener duración del video en microsegundos
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            Log.d(TAG, "📊 Video duration: ${durationMs}ms (${durationMs / 1000.0}s)")

            if (durationMs == 0L) {
                Log.e(TAG, "❌ Invalid video duration")
                return emptyList()
            }

            // Calcular intervalo entre frames
            val intervalMs = if (durationMs > FRAMES_TO_EXTRACT * MIN_FRAME_INTERVAL_MS) {
                durationMs / FRAMES_TO_EXTRACT
            } else {
                MIN_FRAME_INTERVAL_MS
            }

            Log.d(TAG, "⏱️ Frame interval: ${intervalMs}ms")
            Log.d(TAG, "🎞️ Extracting $FRAMES_TO_EXTRACT frames...")

            // Extraer frames
            val outputDir = context.filesDir
            var frameIndex = 0

            for (i in 0 until FRAMES_TO_EXTRACT) {
                val timeMs = i * intervalMs

                // No extraer más allá de la duración del video
                if (timeMs >= durationMs) break

                try {
                    // Extraer frame en el tiempo especificado (en microsegundos)
                    val timeMicros = timeMs * 1000
                    val bitmap = retriever.getFrameAtTime(
                        timeMicros,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )

                    if (bitmap != null) {
                        // Guardar frame como archivo
                        val frameFile = File(outputDir, "video_frame_${System.currentTimeMillis()}_$frameIndex.jpg")
                        FileOutputStream(frameFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }

                        framePaths.add(frameFile.absolutePath)
                        frameIndex++

                        Log.d(TAG, "  ✅ Frame $frameIndex extracted at ${timeMs}ms (${bitmap.width}x${bitmap.height})")

                        bitmap.recycle()
                    } else {
                        Log.w(TAG, "  ⚠️ Frame at ${timeMs}ms is null")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "  ❌ Error extracting frame at ${timeMs}ms: ${e.message}")
                }
            }

            Log.d(TAG, "═══════════════════════════════════════════════════════")
            Log.d(TAG, "✅ EXTRACTION COMPLETE: $frameIndex frames extracted")
            Log.d(TAG, "═══════════════════════════════════════════════════════")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during frame extraction", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing retriever: ${e.message}")
            }
        }

        return framePaths
    }

    /**
     * Limpia los frames temporales extraídos
     */
    fun cleanupFrames(framePaths: List<String>) {
        Log.d(TAG, "🧹 Cleaning up ${framePaths.size} temporary frames...")
        var deletedCount = 0

        framePaths.forEach { path ->
            try {
                val file = File(path)
                if (file.exists() && file.delete()) {
                    deletedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete frame: $path")
            }
        }

        Log.d(TAG, "✅ Cleaned up $deletedCount frames")
    }
}
