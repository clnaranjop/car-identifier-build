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
 * Sistema para registrar rotaciones de imágenes aplicadas por el usuario.
 * Guarda un log en JSON que puede ser extraído con adb pull.
 */
class RotationLogger(private val context: Context) {

    private val tag = "RotationLogger"
    private val logFileName = "rotation_log.json"

    data class RotationEntry(
        val imageName: String,
        val rotationDegrees: Int,  // 90, 180, 270, o 360 (=0)
        val timestamp: String
    )

    private val rotations = mutableMapOf<String, Int>() // imageName -> total rotation

    init {
        loadExistingLog()
    }

    /**
     * Registra una rotación de 90° para una imagen.
     * Las rotaciones son acumulativas.
     */
    fun logRotation(imageName: String) {
        val currentRotation = rotations[imageName] ?: 0
        val newRotation = (currentRotation + 90) % 360

        if (newRotation == 0) {
            // Si llegó a 360°, significa que volvió a la posición original
            rotations.remove(imageName)
            Log.d(tag, "Image $imageName rotated back to original (360°)")
        } else {
            rotations[imageName] = newRotation
            Log.d(tag, "Image $imageName rotated to $newRotation°")
        }

        saveLog()
    }

    /**
     * Obtiene la rotación actual de una imagen.
     */
    fun getRotation(imageName: String): Int {
        return rotations[imageName] ?: 0
    }

    /**
     * Guarda el log en formato JSON.
     */
    private fun saveLog() {
        try {
            val jsonArray = JSONArray()

            for ((imageName, degrees) in rotations) {
                val entry = JSONObject()
                entry.put("image_name", imageName)
                entry.put("rotation_degrees", degrees)
                entry.put("timestamp", getCurrentTimestamp())
                jsonArray.put(entry)
            }

            val jsonObject = JSONObject()
            jsonObject.put("version", "1.0")
            jsonObject.put("total_rotations", rotations.size)
            jsonObject.put("last_updated", getCurrentTimestamp())
            jsonObject.put("rotations", jsonArray)

            val file = File(context.filesDir, logFileName)
            file.writeText(jsonObject.toString(2))

            Log.d(tag, "Rotation log saved: ${rotations.size} images, path: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(tag, "Error saving rotation log", e)
        }
    }

    /**
     * Carga el log existente si existe.
     * Primero intenta desde filesDir (editable), luego desde assets (read-only).
     */
    private fun loadExistingLog() {
        try {
            // Primero intentar desde filesDir (log modificable por el usuario)
            val file = File(context.filesDir, logFileName)
            val content = if (file.exists()) {
                Log.d(tag, "Loading rotations from filesDir: ${file.absolutePath}")
                file.readText()
            } else {
                // Si no existe en filesDir, cargar desde assets (log pre-incluido)
                Log.d(tag, "Loading rotations from assets")
                context.assets.open(logFileName).bufferedReader().use { it.readText() }
            }

            val jsonObject = JSONObject(content)
            val jsonArray = jsonObject.getJSONArray("rotations")

            for (i in 0 until jsonArray.length()) {
                val entry = jsonArray.getJSONObject(i)
                val imageName = entry.getString("image_name")
                val degrees = entry.getInt("rotation_degrees")
                rotations[imageName] = degrees
            }

            Log.d(tag, "Loaded ${rotations.size} rotations from log")
        } catch (e: Exception) {
            Log.e(tag, "Error loading rotation log", e)
        }
    }

    /**
     * Obtiene el path absoluto del archivo de log para extraerlo con adb.
     */
    fun getLogFilePath(): String {
        return File(context.filesDir, logFileName).absolutePath
    }

    /**
     * Limpia todas las rotaciones registradas.
     */
    fun clearAll() {
        rotations.clear()
        saveLog()
        Log.d(tag, "All rotations cleared")
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Obtiene un resumen del log para mostrar al usuario.
     */
    fun getSummary(): String {
        return "Total de imágenes rotadas: ${rotations.size}\n" +
               "Archivo: ${getLogFilePath()}"
    }
}
