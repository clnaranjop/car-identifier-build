package com.hotwheels.identifier.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hotwheels.identifier.R
import com.hotwheels.identifier.data.AssetDownloader
import com.hotwheels.identifier.utils.IncrementalAssetDownloader
import kotlinx.coroutines.launch
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2500L // 2.5 segundos

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocale(newBase))
    }

    private fun updateLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "es") ?: "es"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Ocultar status bar y navigation bar para pantalla completa
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // Check if assets need to be downloaded
        val downloader = AssetDownloader(this)
        val incrementalDownloader = IncrementalAssetDownloader(this)

        if (!downloader.areAssetsDownloaded()) {
            // Check if connected to WiFi
            if (!isConnectedToWiFi()) {
                showWiFiWarningDialog(downloader)
            } else {
                startDownload(downloader)
            }
        } else {
            // Assets already downloaded
            // For migration: Mark base collection as installed if not already marked
            if (incrementalDownloader.getInstalledCollections().isEmpty()) {
                incrementalDownloader.markBaseCollectionInstalled()
            }

            // Proceed normally
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMain()
            }, SPLASH_DELAY)
        }
    }

    private fun isConnectedToWiFi(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    private fun showWiFiWarningDialog(downloader: AssetDownloader) {
        AlertDialog.Builder(this)
            .setTitle("Descarga requerida")
            .setMessage("Esta aplicación necesita descargar aproximadamente 1.3 GB de datos para funcionar.\n\n" +
                    "Se recomienda conectarse a WiFi para evitar cargos por datos móviles.\n\n" +
                    "¿Desea continuar con la descarga?")
            .setPositiveButton("Descargar") { _, _ ->
                startDownload(downloader)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun startDownload(downloader: AssetDownloader) {
        // Show download UI
        val downloadContainer = findViewById<View>(R.id.downloadContainer)
        val progressBar = findViewById<ProgressBar>(R.id.downloadProgress)
        val statusText = findViewById<TextView>(R.id.downloadStatus)

        downloadContainer.visibility = View.VISIBLE

        // Start download
        lifecycleScope.launch {
            statusText.text = "Descargando imágenes de referencia..."

            val result = downloader.downloadAssets { progress ->
                runOnUiThread {
                    progressBar.progress = progress
                    if (progress == 100) {
                        statusText.text = "Extrayendo archivos..."
                    } else {
                        statusText.text = "Descargando: $progress%"
                    }
                }
            }

            result.onSuccess {
                runOnUiThread {
                    statusText.text = "¡Listo!"

                    // Mark base collection as installed for incremental updates
                    val incrementalDownloader = IncrementalAssetDownloader(this@SplashActivity)
                    incrementalDownloader.markBaseCollectionInstalled()

                    navigateToMain()
                }
            }.onFailure { error ->
                runOnUiThread {
                    statusText.text = "Error: ${error.message}\n\nPor favor, verifica tu conexión e intenta nuevamente."

                    // Show retry button
                    AlertDialog.Builder(this@SplashActivity)
                        .setTitle("Error de descarga")
                        .setMessage("No se pudieron descargar los archivos necesarios.\n\n" +
                                "Error: ${error.message}\n\n" +
                                "Por favor, verifica tu conexión a internet e intenta nuevamente.")
                        .setPositiveButton("Reintentar") { _, _ ->
                            // Retry download
                            startDownload(downloader)
                        }
                        .setNegativeButton("Salir") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        // Animación de transición suave
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
