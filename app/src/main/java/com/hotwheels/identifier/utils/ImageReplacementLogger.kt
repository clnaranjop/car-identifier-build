package com.hotwheels.identifier.utils

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logger para registrar imágenes que necesitan ser reemplazadas/re-scrapeadas.
 * Las imágenes marcadas se guardan en un archivo JSON para su posterior procesamiento.
 */
class ImageReplacementLogger(private val context: Context) {

    private val tag = "ImageReplacementLogger"
    private val logFileName = "images_to_replace.json"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Set de imágenes marcadas para reemplazo (fileName -> ImageFlagInfo)
    private val flaggedImages = mutableMapOf<String, ImageFlagInfo>()

    data class ImageFlagInfo(
        val fileName: String,
        val year: String,
        val modelName: String,
        val flaggedDate: String,
        val reason: String = "user_flagged"
    )

    init {
        loadLog()
    }

    /**
     * Marca o desmarca una imagen para reemplazo.
     */
    fun toggleImageFlag(fileName: String, year: String, modelName: String): Boolean {
        return if (flaggedImages.containsKey(fileName)) {
            // Ya está marcada, desmarcar
            flaggedImages.remove(fileName)
            saveLog()
            false // Retorna false = desmarcada
        } else {
            // No está marcada, marcar
            val flagInfo = ImageFlagInfo(
                fileName = fileName,
                year = year,
                modelName = modelName,
                flaggedDate = dateFormat.format(Date()),
                reason = "user_flagged"
            )
            flaggedImages[fileName] = flagInfo
            saveLog()
            true // Retorna true = marcada
        }
    }

    /**
     * Verifica si una imagen está marcada para reemplazo.
     */
    fun isImageFlagged(fileName: String): Boolean {
        return flaggedImages.containsKey(fileName)
    }

    /**
     * Obtiene el número total de imágenes marcadas.
     */
    fun getFlaggedCount(): Int {
        return flaggedImages.size
    }

    /**
     * Obtiene la lista de todas las imágenes marcadas.
     */
    fun getAllFlaggedImages(): List<ImageFlagInfo> {
        return flaggedImages.values.toList()
    }

    /**
     * Obtiene la ruta del archivo de log.
     */
    fun getLogFilePath(): String {
        return File(context.filesDir, logFileName).absolutePath
    }

    /**
     * Limpia todas las marcas (útil después de re-scrapear).
     */
    fun clearAllFlags() {
        flaggedImages.clear()
        saveLog()
    }

    /**
     * Guarda el log en formato JSON.
     */
    private fun saveLog() {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("version", "1.0")
            jsonObject.put("total_flagged", flaggedImages.size)
            jsonObject.put("last_updated", dateFormat.format(Date()))

            val imagesArray = JSONArray()
            for (flagInfo in flaggedImages.values) {
                val imageObject = JSONObject()
                imageObject.put("file_name", flagInfo.fileName)
                imageObject.put("year", flagInfo.year)
                imageObject.put("model_name", flagInfo.modelName)
                imageObject.put("flagged_date", flagInfo.flaggedDate)
                imageObject.put("reason", flagInfo.reason)
                imagesArray.put(imageObject)
            }
            jsonObject.put("images_to_replace", imagesArray)

            val file = File(context.filesDir, logFileName)
            file.writeText(jsonObject.toString(2))

            Log.d(tag, "Replacement log saved: ${flaggedImages.size} images flagged")
        } catch (e: Exception) {
            Log.e(tag, "Error saving replacement log", e)
        }
    }

    /**
     * Carga el log desde el archivo JSON.
     */
    private fun loadLog() {
        try {
            val file = File(context.filesDir, logFileName)
            if (!file.exists()) {
                Log.d(tag, "No existing replacement log found")
                return
            }

            val jsonString = file.readText()
            val jsonObject = JSONObject(jsonString)
            val imagesArray = jsonObject.getJSONArray("images_to_replace")

            flaggedImages.clear()
            for (i in 0 until imagesArray.length()) {
                val imageObject = imagesArray.getJSONObject(i)
                val flagInfo = ImageFlagInfo(
                    fileName = imageObject.getString("file_name"),
                    year = imageObject.getString("year"),
                    modelName = imageObject.getString("model_name"),
                    flaggedDate = imageObject.getString("flagged_date"),
                    reason = imageObject.getString("reason")
                )
                flaggedImages[flagInfo.fileName] = flagInfo
            }

            Log.d(tag, "Replacement log loaded: ${flaggedImages.size} images flagged")
        } catch (e: Exception) {
            Log.e(tag, "Error loading replacement log", e)
        }
    }

    /**
     * Exporta el log como texto legible para compartir.
     */
    fun exportAsText(): String {
        val sb = StringBuilder()
        sb.append("IMÁGENES MARCADAS PARA REEMPLAZO\n")
        sb.append("=================================\n")
        sb.append("Total: ${flaggedImages.size}\n")
        sb.append("Fecha: ${dateFormat.format(Date())}\n\n")

        val sortedByYear = flaggedImages.values.sortedBy { it.year }
        var currentYear = ""

        for (flagInfo in sortedByYear) {
            if (flagInfo.year != currentYear) {
                currentYear = flagInfo.year
                sb.append("\n--- Año $currentYear ---\n")
            }
            sb.append("  • ${flagInfo.modelName} (${flagInfo.fileName})\n")
            sb.append("    Marcado: ${flagInfo.flaggedDate}\n")
        }

        return sb.toString()
    }
}
