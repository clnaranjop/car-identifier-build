package com.hotwheels.identifier

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class HotWheelsApplication : Application() {

    private val tag = "HotWheelsApplication"

    override fun onCreate() {
        super.onCreate()

        // Apply saved language preference
        applySavedLanguage()

        // Initialize OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.d(tag, "✅ OpenCV loaded successfully")
        } else {
            Log.e(tag, "❌ OpenCV initialization failed")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Reapply language when configuration changes
        applySavedLanguage()
    }

    private fun applySavedLanguage() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "es") ?: "es"  // Default to Spanish

        Log.d(tag, "🌍 Applying saved language: $languageCode")

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onTerminate() {
        super.onTerminate()
        exportLogcatToFile()
    }

    private fun exportLogcatToFile() {
        try {
            val downloadDir = File("/sdcard/Download")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val logFile = File(downloadDir, "hotwheels_log_$timestamp.txt")

            val process = Runtime.getRuntime().exec("logcat -d -v time *:V")
            val bufferedReader = process.inputStream.bufferedReader()

            FileOutputStream(logFile).use { output ->
                bufferedReader.use { reader ->
                    reader.lines().forEach { line ->
                        if (line.contains("HotWheels") || line.contains("MobileNetIdentifier") ||
                            line.contains("CameraActivity") || line.contains("SelectResultActivity")) {
                            output.write((line + "\n").toByteArray())
                        }
                    }
                }
            }

            Log.d(tag, "Log exported to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to export log: ${e.message}")
        }
    }
}
