package com.hotwheels.identifier.ui.exploration

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.hotwheels.identifier.R
import com.hotwheels.identifier.data.ReferenceImage
import com.hotwheels.identifier.data.ReferenceImageRepository
import com.hotwheels.identifier.data.SimpleDataStorage
import com.hotwheels.identifier.databinding.ActivityExplorationBinding
import com.hotwheels.identifier.utils.RotationLogger
import com.hotwheels.identifier.utils.ImageReplacementLogger
import kotlinx.coroutines.launch

/**
 * Activity para explorar todas las imágenes de referencia.
 * Permite rotar imágenes y agregar a la colección.
 */
class ExplorationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExplorationBinding
    private lateinit var adapter: ExplorationAdapter
    private lateinit var repository: ReferenceImageRepository
    private lateinit var rotationLogger: RotationLogger
    private lateinit var replacementLogger: ImageReplacementLogger
    private lateinit var dataStorage: SimpleDataStorage

    private var allImages = listOf<ReferenceImage>()
    private var currentSortMode = SortMode.BY_YEAR
    private var selectedYear: String? = null
    private var availableYears = listOf<String>()

    // Developer mode (código secreto: tocar 7 veces el título)
    private var developerModeClickCount = 0
    private var developerModeEnabled = false
    private var lastClickTime = 0L

    private enum class SortMode {
        BY_YEAR,
        ALPHABETICALLY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExplorationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Exploración de Imágenes"

        // Setup secret code for developer mode (tap toolbar title 7 times)
        setupDeveloperModeSecret()

        // Initialize components
        repository = ReferenceImageRepository(this)
        rotationLogger = RotationLogger(this)
        replacementLogger = ImageReplacementLogger(this)
        dataStorage = SimpleDataStorage(this)

        // Setup RecyclerView
        adapter = ExplorationAdapter(
            assets = assets,
            onRotateClicked = { image -> handleRotateImage(image) },
            onAddToCollectionClicked = { image -> handleAddToCollection(image) },
            onReplacementFlagChanged = { image, isFlagged -> handleReplacementFlagChanged(image, isFlagged) }
        )

        binding.recyclerViewImages.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewImages.adapter = adapter

        // Load images
        loadImages()

        // Setup buttons
        binding.btnSortByYear.setOnClickListener {
            currentSortMode = SortMode.BY_YEAR
            updateSortButtons()
            sortAndDisplayImages()
        }

        binding.btnSortAlphabetically.setOnClickListener {
            currentSortMode = SortMode.ALPHABETICALLY
            updateSortButtons()
            sortAndDisplayImages()
        }

        binding.fabRotationSummary.setOnClickListener {
            showRotationSummary()
        }

        binding.btnSelectYear.setOnClickListener {
            showYearSelectionDialog()
        }

        binding.btnClearFilter.setOnClickListener {
            selectedYear = null
            updateYearFilterUI()
            sortAndDisplayImages()
        }

        // Set initial sort mode UI
        updateSortButtons()
        updateYearFilterUI()

        // Hide FAB initially (only visible in developer mode)
        binding.fabRotationSummary.visibility = android.view.View.GONE

        // Initialize ads
        initializeAds()
    }

    private fun initializeAds() {
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        } catch (e: Exception) {
            android.util.Log.e("ExplorationActivity", "Error initializing ads", e)
        }
    }

    private fun loadImages() {
        // Load all reference images
        allImages = repository.loadAllReferenceImages().toMutableList()

        // Apply saved rotations from logger
        allImages.forEach { image ->
            image.currentRotation = rotationLogger.getRotation(image.fileName)
        }

        // Load flagged images status
        replacementLogger.getAllFlaggedImages().forEach { flagInfo ->
            adapter.updateFlagStatus(flagInfo.fileName, true)
        }

        // Extract available years and sort them
        availableYears = allImages.map { it.year }.distinct().sortedBy { it.toIntOrNull() ?: 9999 }

        sortAndDisplayImages()
    }

    private fun sortAndDisplayImages() {
        // Filter by year if selected
        val filteredImages = if (selectedYear != null) {
            allImages.filter { it.year == selectedYear }
        } else {
            allImages
        }

        val sortedImages = when (currentSortMode) {
            SortMode.BY_YEAR -> filteredImages.sortedWith(
                compareBy<ReferenceImage> { it.year }
                    .thenBy { it.fileName }
            )
            SortMode.ALPHABETICALLY -> filteredImages.sortedBy { it.displayName }
        }

        adapter.submitList(sortedImages)

        // Update title with count
        val countText = if (selectedYear != null) {
            "Año $selectedYear: ${sortedImages.size} imágenes"
        } else {
            "Total: ${sortedImages.size} imágenes"
        }
        supportActionBar?.subtitle = countText
    }

    private fun updateSortButtons() {
        when (currentSortMode) {
            SortMode.BY_YEAR -> {
                binding.btnSortByYear.setBackgroundColor(getColor(R.color.primary_orange))
                binding.btnSortAlphabetically.setBackgroundColor(getColor(R.color.background_card))
            }
            SortMode.ALPHABETICALLY -> {
                binding.btnSortAlphabetically.setBackgroundColor(getColor(R.color.primary_orange))
                binding.btnSortByYear.setBackgroundColor(getColor(R.color.background_card))
            }
        }
    }

    private fun updateYearFilterUI() {
        if (selectedYear != null) {
            binding.btnSelectYear.text = "Año: $selectedYear"
            binding.btnSelectYear.setBackgroundColor(getColor(R.color.primary_orange))
            binding.btnClearFilter.visibility = android.view.View.VISIBLE
        } else {
            binding.btnSelectYear.text = "Todos los años"
            binding.btnSelectYear.setBackgroundColor(getColor(R.color.background_card))
            binding.btnClearFilter.visibility = android.view.View.GONE
        }
    }

    private fun showYearSelectionDialog() {
        // Create year options with counts
        val yearOptions = availableYears.map { year ->
            val count = allImages.count { it.year == year }
            "$year ($count imágenes)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Año")
            .setItems(yearOptions) { _, which ->
                selectedYear = availableYears[which]
                updateYearFilterUI()
                sortAndDisplayImages()
                Toast.makeText(
                    this,
                    "Mostrando imágenes del año $selectedYear",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleRotateImage(image: ReferenceImage) {
        // Rotate 90 degrees
        image.currentRotation = (image.currentRotation + 90) % 360

        // Log rotation
        rotationLogger.logRotation(image.fileName)

        // Refresh display
        adapter.notifyDataSetChanged()

        // Show feedback
        val message = if (image.currentRotation == 0) {
            "Imagen restaurada a orientación original"
        } else {
            "Imagen rotada ${image.currentRotation}°"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleAddToCollection(image: ReferenceImage) {
        // Add to collection using UserCollection entity
        val collection = com.hotwheels.identifier.data.entities.UserCollection(
            hotWheelModelId = image.fileName.removeSuffix(".jpg"),
            quantity = 1,
            condition = "Mint",
            acquiredDate = System.currentTimeMillis(),
            notes = "Added from Exploration mode"
        )

        // Insert collection asynchronously
        lifecycleScope.launch {
            try {
                dataStorage.insertCollection(collection)
                Toast.makeText(
                    this@ExplorationActivity,
                    "${image.displayName} agregado a la colección",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExplorationActivity,
                    "Error al agregar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleReplacementFlagChanged(image: ReferenceImage, isFlagged: Boolean) {
        // Toggle flag in logger
        replacementLogger.toggleImageFlag(image.fileName, image.year, image.displayName)

        // Show feedback
        val message = if (isFlagged) {
            "Marcada para reemplazo: ${image.displayName}"
        } else {
            "Desmarcada: ${image.displayName}"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showRotationSummary() {
        val rotationSummary = rotationLogger.getSummary()
        val rotationLogPath = rotationLogger.getLogFilePath()

        val flaggedCount = replacementLogger.getFlaggedCount()
        val replacementLogPath = replacementLogger.getLogFilePath()

        val message = StringBuilder()
        message.append("=== ROTACIONES ===\n")
        message.append("$rotationSummary\n\n")
        message.append("=== IMÁGENES PARA REEMPLAZO ===\n")
        message.append("Total marcadas: $flaggedCount\n\n")

        if (flaggedCount > 0) {
            message.append("Para extraer logs:\n")
            message.append("adb pull $rotationLogPath\n")
            message.append("adb pull $replacementLogPath")
        } else {
            message.append("Para extraer log de rotaciones:\n")
            message.append("adb pull $rotationLogPath")
        }

        AlertDialog.Builder(this)
            .setTitle("Resumen de Cambios")
            .setMessage(message.toString())
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Ver Marcadas") { _, _ ->
                if (flaggedCount > 0) {
                    showFlaggedImagesList()
                } else {
                    Toast.makeText(this, "No hay imágenes marcadas", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Limpiar") { _, _ ->
                showClearOptionsDialog()
            }
            .show()
    }

    private fun showFlaggedImagesList() {
        val flaggedImages = replacementLogger.getAllFlaggedImages()
        val textExport = replacementLogger.exportAsText()

        AlertDialog.Builder(this)
            .setTitle("Imágenes Marcadas (${ flaggedImages.size})")
            .setMessage(textExport)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showClearOptionsDialog() {
        val options = arrayOf(
            "Limpiar rotaciones",
            "Limpiar marcas de reemplazo",
            "Limpiar todo"
        )

        AlertDialog.Builder(this)
            .setTitle("¿Qué deseas limpiar?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmClearAllRotations()
                    1 -> confirmClearReplacementFlags()
                    2 -> confirmClearAll()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmClearReplacementFlags() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Deseas eliminar todas las marcas de reemplazo?")
            .setPositiveButton("Sí") { _, _ ->
                replacementLogger.clearAllFlags()
                allImages.forEach { adapter.updateFlagStatus(it.fileName, false) }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Marcas eliminadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Deseas eliminar TODAS las rotaciones y marcas de reemplazo?")
            .setPositiveButton("Sí") { _, _ ->
                rotationLogger.clearAll()
                replacementLogger.clearAllFlags()
                allImages.forEach {
                    it.currentRotation = 0
                    adapter.updateFlagStatus(it.fileName, false)
                }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Todo limpiado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmClearAllRotations() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Deseas eliminar todas las rotaciones registradas?")
            .setPositiveButton("Sí") { _, _ ->
                rotationLogger.clearAll()
                allImages.forEach { it.currentRotation = 0 }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Rotaciones eliminadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupDeveloperModeSecret() {
        binding.toolbar.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            // Reset counter if more than 2 seconds passed since last click
            if (currentTime - lastClickTime > 2000) {
                developerModeClickCount = 0
            }

            developerModeClickCount++
            lastClickTime = currentTime

            // Show feedback for clicks 3-6
            if (developerModeClickCount in 3..6) {
                Toast.makeText(
                    this,
                    "Toques: $developerModeClickCount/7",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Activate developer mode after 7 taps
            if (developerModeClickCount >= 7) {
                developerModeEnabled = !developerModeEnabled
                developerModeClickCount = 0

                // Update adapter
                adapter.developerModeEnabled = developerModeEnabled
                adapter.notifyDataSetChanged()

                // Show/hide FAB based on developer mode
                binding.fabRotationSummary.visibility = if (developerModeEnabled) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                val message = if (developerModeEnabled) {
                    "🔧 Modo Desarrollador ACTIVADO"
                } else {
                    "Modo Desarrollador DESACTIVADO"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
