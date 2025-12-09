package com.hotwheels.identifier.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.hotwheels.identifier.R
import com.hotwheels.identifier.databinding.ActivityMainBinding
import com.hotwheels.identifier.ui.camera.CameraActivity
import com.hotwheels.identifier.ui.collection.CollectionActivity
import com.hotwheels.identifier.utils.IncrementalAssetDownloader
import com.hotwheels.identifier.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Locale
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val tag = "MainActivity"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(tag, "Camera permission granted")
            openCamera()
        } else {
            Log.d(tag, "Camera permission denied")
            showPermissionDeniedMessage()
        }
    }

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
        try {
            Log.d(tag, "onCreate started")
            super.onCreate(savedInstanceState)
            Log.d(tag, "super.onCreate completed")

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(tag, "ViewBinding setup completed")

            setupBasicButtons()
            Log.d(tag, "setupBasicButtons completed")

            observeViewModel()
            Log.d(tag, "observeViewModel completed")

            initializeAds()
            Log.d(tag, "initializeAds completed")

            loadStats()
            Log.d(tag, "loadStats completed")

            loadCarOfTheDay()
            Log.d(tag, "loadCarOfTheDay completed")

            checkForDatabaseUpdates()
            Log.d(tag, "checkForDatabaseUpdates completed")

            Log.d(tag, "MainActivity created successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error in onCreate", e)
            throw e
        }
    }

    private fun setupBasicButtons() {
        try {
            Log.d(tag, "Setting up buttons")

            binding.btnScanHotWheel.setOnClickListener {
                Log.d(tag, "Scan button clicked")
                checkCameraPermissionAndOpen()
            }

            binding.btnManualSearch.setOnClickListener {
                Log.d(tag, "Manual search button clicked")
                showManualSearchFromMain()
            }

            binding.btnViewCollection.setOnClickListener {
                Log.d(tag, "Collection button clicked")
                openCollection()
            }

            binding.btnExploration.setOnClickListener {
                Log.d(tag, "Exploration button clicked")
                openExploration()
            }

            binding.btnSettings.setOnClickListener {
                Log.d(tag, "Settings button clicked")
                openSettings()
            }

            // Hidden feature: Long press Settings to fix corrupted models
            binding.btnSettings.setOnLongClickListener {
                Log.d(tag, "Settings button long pressed - triggering model fix")
                fixCorruptedModels()
                true
            }

            binding.btnAbout.setOnClickListener {
                Log.d(tag, "About button clicked")
                val intent = Intent(this, com.hotwheels.identifier.ui.about.AboutActivity::class.java)
                startActivity(intent)
            }

            // Make car of the day image clickeable to view full size
            binding.imgCarOfTheDay.setOnClickListener {
                Log.d(tag, "Car of the day image clicked")
                showCarOfTheDayFullScreen()
            }

            Log.d(tag, "All buttons set up successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error setting up buttons", e)
        }
    }

    private fun observeViewModel() {
        try {
            Log.d(tag, "Setting up ViewModel observers")

            lifecycleScope.launch {
                viewModel.totalScanned.collect { count ->
                    Log.d(tag, "Total scanned updated: $count")
                    binding.tvTotalScanned.text = count.toString()
                }
            }

            lifecycleScope.launch {
                viewModel.collectionSize.collect { count ->
                    Log.d(tag, "Collection size updated: $count")
                    binding.tvCollectionSize.text = count.toString()
                }
            }

            lifecycleScope.launch {
                viewModel.databaseSize.collect { count ->
                    Log.d(tag, "Database size updated: $count")
                    binding.tvDatabaseSize.text = count.toString()
                }
            }

            lifecycleScope.launch {
                viewModel.downloadProgress.collect { progress ->
                    if (progress.isNotEmpty()) {
                        Log.d(tag, "Download progress: $progress")
                        // Mostrar progreso en el UI si es necesario
                        // binding.tvDownloadStatus.text = progress
                    }
                }
            }

            lifecycleScope.launch {
                viewModel.isDownloading.collect { isDownloading ->
                    Log.d(tag, "Download status: $isDownloading")
                    // Puedes mostrar un indicador de carga
                    // binding.progressBar.visibility = if (isDownloading) View.VISIBLE else View.GONE
                }
            }

            Log.d(tag, "ViewModel observers set up successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error setting up observers", e)
        }
    }

    private fun initializeAds() {
        try {
            Log.d(tag, "Initializing AdMob")
            MobileAds.initialize(this) {
                Log.d(tag, "AdMob initialized successfully")
            }

            // Add bottom padding to AdView if navigation bar is present
            binding.adView.post {
                val navBarHeight = getNavigationBarHeight()
                if (navBarHeight > 0) {
                    val params = binding.adView.layoutParams as android.widget.LinearLayout.LayoutParams
                    params.bottomMargin = navBarHeight
                    binding.adView.layoutParams = params
                    Log.d(tag, "Added bottom margin of ${navBarHeight}px to AdView for navigation bar")
                }
            }

            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
            Log.d(tag, "Ad request sent")
        } catch (e: Exception) {
            Log.e(tag, "Error initializing ads", e)
        }
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            // Check if device has navigation buttons (not gesture navigation)
            val hasNavigationBar = !isGestureNavigationEnabled()
            if (hasNavigationBar) {
                val height = resources.getDimensionPixelSize(resourceId)
                Log.d(tag, "Navigation bar height: ${height}px")
                height
            } else {
                Log.d(tag, "Gesture navigation detected, no bottom margin needed")
                0
            }
        } else {
            0
        }
    }

    private fun isGestureNavigationEnabled(): Boolean {
        val resources = resources
        val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        return if (resourceId > 0) {
            val mode = resources.getInteger(resourceId)
            Log.d(tag, "Navigation mode: $mode (2 = gesture)")
            mode == 2 // 2 = gesture navigation
        } else {
            // Fallback: check if navbar height is very small (gesture navigation usually has thinner bar)
            val navBarHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (navBarHeight > 0) {
                val height = resources.getDimensionPixelSize(navBarHeight)
                height < 48 // Gesture bars are typically < 48dp
            } else {
                true // Assume gesture if we can't determine
            }
        }
    }

    private fun loadStats() {
        try {
            Log.d(tag, "Loading stats from ViewModel")
            viewModel.loadStats()
        } catch (e: Exception) {
            Log.e(tag, "Error loading stats", e)
        }
    }

    private fun checkCameraPermissionAndOpen() {
        try {
            Log.d(tag, "Checking camera permission")
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(tag, "Camera permission already granted")
                    openCamera()
                }
                else -> {
                    Log.d(tag, "Requesting camera permission")
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking camera permission", e)
        }
    }

    private fun openCamera() {
        try {
            Log.d(tag, "Opening camera activity")
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error opening camera", e)
            showComingSoonMessage("Camera functionality not available")
        }
    }

    private fun openCollection() {
        try {
            Log.d(tag, "Opening collection activity")
            val intent = Intent(this, CollectionActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error opening collection", e)
            showComingSoonMessage("Collection functionality not available")
        }
    }

    private fun openExploration() {
        try {
            Log.d(tag, "Opening exploration activity")
            val intent = Intent(this, com.hotwheels.identifier.ui.exploration.ExplorationActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error opening exploration", e)
            showComingSoonMessage("Exploration not available")
        }
    }

    private fun openSettings() {
        try {
            Log.d(tag, "Opening settings activity")
            val intent = Intent(this, com.hotwheels.identifier.ui.settings.SettingsActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error opening settings", e)
            showComingSoonMessage("Settings not available")
        }
    }

    private fun showPermissionDeniedMessage() {
        try {
            Snackbar.make(
                binding.root,
                getString(R.string.camera_permission_required),
                Snackbar.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(tag, "Error showing permission denied message", e)
        }
    }

    private fun showComingSoonMessage(feature: String) {
        try {
            Snackbar.make(
                binding.root,
                "$feature - Coming soon!",
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(tag, "Error showing snackbar", e)
        }
    }

    /**
     * Hidden feature to fix corrupted model data
     * Triggered by long-pressing the Settings button
     */
    private fun fixCorruptedModels() {
        try {
            Log.d(tag, "🔧 Starting corrupted model fix")

            Snackbar.make(
                binding.root,
                "🔧 Fixing corrupted models...",
                Snackbar.LENGTH_SHORT
            ).show()

            lifecycleScope.launch {
                try {
                    // Fix corrupted models directly in repository
                    val fixedCount = viewModel.fixCorruptedModels()

                    val message = if (fixedCount > 0) {
                        "✅ Fixed $fixedCount corrupted models"
                    } else {
                        "✅ No corrupted models found"
                    }

                    Snackbar.make(
                        binding.root,
                        message,
                        Snackbar.LENGTH_LONG
                    ).show()

                    Log.d(tag, "Model fix completed: $message")

                    // Refresh stats after fix
                    loadStats()

                } catch (e: Exception) {
                    Log.e(tag, "Error fixing corrupted models", e)
                    Snackbar.make(
                        binding.root,
                        "❌ Error fixing models: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

        } catch (e: Exception) {
            Log.e(tag, "Error in fixCorruptedModels", e)
        }
    }

    private fun showManualSearchFromMain() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_search, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_search_query)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Buscar") { _, _ ->
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performManualSearchFromMain(query)
                } else {
                    Snackbar.make(binding.root, "Ingresa un nombre para buscar", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun performManualSearchFromMain(query: String) {
        Log.d(tag, "Performing manual search for: $query")

        lifecycleScope.launch {
            try {
                val allModels = viewModel.getAllModels().first()

                val results = allModels.filter { model ->
                    model.name.contains(query, ignoreCase = true) ||
                    model.year.toString().contains(query) ||
                    model.series?.contains(query, ignoreCase = true) == true
                }.take(5)

                Log.d(tag, "Found ${results.size} results for query: $query")

                if (results.isEmpty()) {
                    Snackbar.make(
                        binding.root,
                        "No se encontraron modelos con: \"$query\"",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    showSearchResultsFromMain(results)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error searching models", e)
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showSearchResultsFromMain(results: List<com.hotwheels.identifier.data.entities.HotWheelModel>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_results, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_search_results)
        val noResultsText = dialogView.findViewById<android.widget.TextView>(R.id.tv_no_results)
        val btnNoneMatch = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_none_match)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val searchAdapter = com.hotwheels.identifier.ui.selection.SearchResultsAdapter { model ->
            dialog.dismiss()
            // Open ResultActivity directly with selected model
            val intent = Intent(this, com.hotwheels.identifier.ui.result.ResultActivity::class.java).apply {
                putExtra(com.hotwheels.identifier.ui.result.ResultActivity.EXTRA_MODEL_ID, model.id)
                putExtra(com.hotwheels.identifier.ui.result.ResultActivity.EXTRA_CONFIDENCE, 1.0f)
                putExtra(com.hotwheels.identifier.ui.result.ResultActivity.EXTRA_IMAGE_PATH, "")
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = searchAdapter
        searchAdapter.submitList(results)

        if (results.isEmpty()) {
            noResultsText.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        }

        btnNoneMatch.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadCarOfTheDay() {
        lifecycleScope.launch {
            try {
                // Get all models from database
                val allModels = viewModel.getAllModels().first()

                if (allModels.isEmpty()) {
                    Log.d(tag, "No models available for car of the day")
                    return@launch
                }

                // Use current date as seed for random selection
                val calendar = java.util.Calendar.getInstance()
                val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
                val year = calendar.get(java.util.Calendar.YEAR)
                val seed = (year * 1000L + dayOfYear).toLong()

                val random = java.util.Random(seed)
                val randomModel = allModels[random.nextInt(allModels.size)]

                Log.d(tag, "Car of the day: ${randomModel.name}")

                // Load and display the image using ImageUtils
                val imagePath = randomModel.localImagePath
                if (!imagePath.isNullOrEmpty()) {
                    val bitmap = com.hotwheels.identifier.utils.ImageUtils.loadBitmap(
                        this@MainActivity,
                        imagePath
                    )

                    if (bitmap != null) {
                        binding.imgCarOfTheDay.setImageBitmap(bitmap)
                        binding.tvCarOfTheDayName.text = "${randomModel.name} (${randomModel.year})"
                        Log.d(tag, "Car of the day image loaded successfully")
                    } else {
                        binding.tvCarOfTheDayName.text = "${randomModel.name} (${randomModel.year})"
                        Log.d(tag, "Failed to load car of the day image from: $imagePath")
                    }
                } else {
                    binding.tvCarOfTheDayName.text = "${randomModel.name} (${randomModel.year})"
                    Log.d(tag, "No image path for car of the day")
                }

            } catch (e: Exception) {
                Log.e(tag, "Error loading car of the day", e)
                binding.tvCarOfTheDayName.text = "Car Identifier"
            }
        }
    }

    private fun showCarOfTheDayFullScreen() {
        try {
            // Create dialog with image view to show full screen
            val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
            val imageView = android.widget.ImageView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setImageDrawable(binding.imgCarOfTheDay.drawable)
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(imageView)
                .setPositiveButton("Cerrar", null)
                .create()
                .show()

        } catch (e: Exception) {
            Log.e(tag, "Error showing full screen image", e)
        }
    }

    /**
     * Check if there are database updates available
     */
    private fun checkForDatabaseUpdates() {
        lifecycleScope.launch {
            try {
                Log.d(tag, "Checking for database updates...")
                val downloader = IncrementalAssetDownloader(this@MainActivity)
                val updateInfo = downloader.checkForUpdates()

                if (updateInfo.hasUpdates) {
                    Log.d(tag, "Updates available: ${updateInfo.newModelCount} models, ${updateInfo.totalSizeMB} MB")
                    runOnUiThread {
                        showUpdateBanner(updateInfo)
                    }
                } else {
                    Log.d(tag, "No updates available")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error checking for updates", e)
                // Silently fail - updates are not critical
            }
        }
    }

    /**
     * Show update banner with available updates
     */
    private fun showUpdateBanner(updateInfo: com.hotwheels.identifier.data.models.UpdateInfo) {
        try {
            binding.updateBanner.visibility = View.VISIBLE

            val message = "${updateInfo.newModelCount} nuevos modelos disponibles (${updateInfo.totalSizeMB} MB)"
            binding.tvUpdateMessage.text = message

            binding.btnDownloadUpdate.setOnClickListener {
                Log.d(tag, "Download update button clicked")
                downloadUpdates(updateInfo)
            }

            Log.d(tag, "Update banner shown: $message")
        } catch (e: Exception) {
            Log.e(tag, "Error showing update banner", e)
        }
    }

    /**
     * Download and install database updates
     */
    private fun downloadUpdates(updateInfo: com.hotwheels.identifier.data.models.UpdateInfo) {
        // Create progress dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_download_progress, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val statusText = dialogView.findViewById<android.widget.TextView>(R.id.tvStatus)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Actualizando base de datos")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        lifecycleScope.launch {
            try {
                val downloader = IncrementalAssetDownloader(this@MainActivity)

                downloader.downloadUpdates(updateInfo) { progress, message ->
                    runOnUiThread {
                        progressBar.progress = progress
                        statusText.text = message
                    }
                }.onSuccess {
                    runOnUiThread {
                        dialog.dismiss()

                        // Hide update banner
                        binding.updateBanner.visibility = View.GONE

                        // Show success message
                        Snackbar.make(
                            binding.root,
                            "✅ Base de datos actualizada correctamente",
                            Snackbar.LENGTH_LONG
                        ).show()

                        Log.d(tag, "Database updated successfully")
                    }
                }.onFailure { error ->
                    runOnUiThread {
                        dialog.dismiss()

                        // Show error dialog
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Error de actualización")
                            .setMessage("No se pudo actualizar la base de datos.\n\nError: ${error.message}")
                            .setPositiveButton("Reintentar") { _, _ ->
                                downloadUpdates(updateInfo)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()

                        Log.e(tag, "Error updating database", error)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dialog.dismiss()

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Error")
                        .setMessage("Error inesperado: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()

                    Log.e(tag, "Unexpected error during update", e)
                }
            }
        }
    }
}