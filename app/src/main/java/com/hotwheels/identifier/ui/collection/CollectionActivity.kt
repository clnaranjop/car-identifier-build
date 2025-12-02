package com.hotwheels.identifier.ui.collection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.hotwheels.identifier.R
import com.hotwheels.identifier.databinding.ActivityCollectionBinding
import com.hotwheels.identifier.ui.details.ModelDetailsActivity
import com.hotwheels.identifier.utils.CollectionExporter
import com.hotwheels.identifier.utils.CollectionImporter
import com.hotwheels.identifier.viewmodel.CollectionViewModel
import com.hotwheels.identifier.viewmodel.CollectionItemWithModel
import kotlinx.coroutines.launch

class CollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionBinding
    private val viewModel: CollectionViewModel by viewModels()
    private lateinit var adapter: CollectionAdapterModern
    private val tag = "CollectionActivity"

    // Filter state
    private var allItems = listOf<CollectionItemWithModel>()
    private var searchQuery = ""
    private var showOnlyFavorites = false
    private var showOnlyTH = false
    private var showOnlySTH = false

    // File picker for import
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImportFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(tag, "onCreate started")
            super.onCreate(savedInstanceState)

            binding = ActivityCollectionBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(tag, "ViewBinding setup completed")

            setupToolbar()
            Log.d(tag, "setupToolbar completed")

            setupRecyclerView()
            Log.d(tag, "setupRecyclerView completed")

            setupSearchAndFilters()
            Log.d(tag, "setupSearchAndFilters completed")

            observeCollectionItems()
            Log.d(tag, "observeCollectionItems completed")

            observeStats()
            Log.d(tag, "observeStats completed")

            Log.d(tag, "CollectionActivity created successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error in onCreate", e)
            // Don't crash, just show basic layout
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.apply {
                title = getString(R.string.title_my_collection)
                setDisplayHomeAsUpEnabled(true)
            }
            Log.d(tag, "Toolbar setup successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error setting up toolbar", e)
        }
    }

    private fun setupRecyclerView() {
        adapter = CollectionAdapterModern(
            onItemClick = { item ->
                Log.d(tag, "Item clicked: ${item.modelName}, modelId: ${item.collectionItem.hotWheelModelId}")
                // Open ModelDetailsActivity
                val intent = Intent(this, ModelDetailsActivity::class.java).apply {
                    putExtra(ModelDetailsActivity.EXTRA_MODEL_ID, item.collectionItem.hotWheelModelId)
                }
                startActivity(intent)
            },
            onFavoriteClick = { item ->
                Log.d(tag, "Favorite clicked for: ${item.modelName}")
                toggleFavorite(item)
            }
            // Removed onViewPricesClick - shopping not available from collection
        )

        binding.rvCollection.adapter = adapter
        binding.rvCollection.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
    }

    private fun toggleFavorite(item: CollectionItemWithModel) {
        lifecycleScope.launch {
            try {
                val updatedItem = item.collectionItem.copy(
                    isFavorite = !item.collectionItem.isFavorite
                )
                viewModel.updateCollectionItem(updatedItem)
                Log.d(tag, "Favorite toggled for: ${item.modelName}")
            } catch (e: Exception) {
                Log.e(tag, "Error toggling favorite", e)
            }
        }
    }

    // Removed showPriceDialog - shopping not available from collection

    private fun setupSearchAndFilters() {
        // Search bar text change listener
        binding.etSearch.addTextChangedListener { text ->
            searchQuery = text?.toString()?.lowercase() ?: ""
            applyFilters()
        }

        // Filter chips
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showOnlyFavorites = false
                showOnlyTH = false
                showOnlySTH = false
                binding.chipFavorites.isChecked = false
                binding.chipTreasureHunt.isChecked = false
                binding.chipSuperTreasureHunt.isChecked = false
                applyFilters()
            }
        }

        binding.chipFavorites.setOnCheckedChangeListener { _, isChecked ->
            showOnlyFavorites = isChecked
            if (isChecked) {
                binding.chipAll.isChecked = false
            }
            applyFilters()
        }

        binding.chipTreasureHunt.setOnCheckedChangeListener { _, isChecked ->
            showOnlyTH = isChecked
            if (isChecked) {
                binding.chipAll.isChecked = false
            }
            applyFilters()
        }

        binding.chipSuperTreasureHunt.setOnCheckedChangeListener { _, isChecked ->
            showOnlySTH = isChecked
            if (isChecked) {
                binding.chipAll.isChecked = false
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        var filteredItems = allItems

        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filteredItems = filteredItems.filter { item ->
                item.modelName.lowercase().contains(searchQuery) ||
                item.modelSeries.lowercase().contains(searchQuery) ||
                item.modelYear.toString().contains(searchQuery)
            }
        }

        // Apply favorite filter
        if (showOnlyFavorites) {
            filteredItems = filteredItems.filter { it.collectionItem.isFavorite }
        }

        // Apply TH filter
        if (showOnlyTH) {
            filteredItems = filteredItems.filter {
                it.collectionItem.isTreasureHunt && !it.collectionItem.isSuperTreasureHunt
            }
        }

        // Apply STH filter
        if (showOnlySTH) {
            filteredItems = filteredItems.filter { it.collectionItem.isSuperTreasureHunt }
        }

        Log.d(tag, "Filtered items: ${filteredItems.size} of ${allItems.size}")
        adapter.submitList(filteredItems)

        // Show/hide empty state
        if (filteredItems.isEmpty()) {
            binding.emptyStateLayout.visibility = android.view.View.VISIBLE
            binding.rvCollection.visibility = android.view.View.GONE
        } else {
            binding.emptyStateLayout.visibility = android.view.View.GONE
            binding.rvCollection.visibility = android.view.View.VISIBLE
        }
    }

    private fun observeCollectionItems() {
        lifecycleScope.launch {
            viewModel.collectionItems.collect { items ->
                Log.d(tag, "📦 Collection items updated: ${items.size} items")
                allItems = items
                applyFilters()
            }
        }
    }

    private fun observeStats() {
        lifecycleScope.launch {
            viewModel.stats.collect { stats ->
                Log.d(tag, "📊 Stats updated: $stats")
                binding.tvTotalItems.text = stats.totalItems.toString()
                binding.tvUniqueModels.text = stats.uniqueModels.toString()
                binding.tvFavorites.text = stats.favorites.toString()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_collection, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_csv -> {
                exportCollectionToCSV()
                true
            }
            R.id.action_export_json -> {
                exportCollectionToJSON()
                true
            }
            R.id.action_import -> {
                showImportDialog()
                true
            }
            R.id.action_share -> {
                shareCollection()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportCollectionToCSV() {
        if (allItems.isEmpty()) {
            Toast.makeText(this, "Collection is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val exporter = CollectionExporter(this)
        val result = exporter.exportToCSV(allItems)

        result.onSuccess { file ->
            Toast.makeText(
                this,
                "✅ Exported to:\n${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            Log.d(tag, "CSV export successful: ${file.absolutePath}")
        }.onFailure { error ->
            Toast.makeText(
                this,
                "❌ Export failed: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(tag, "CSV export failed", error)
        }
    }

    private fun exportCollectionToJSON() {
        if (allItems.isEmpty()) {
            Toast.makeText(this, "Collection is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val exporter = CollectionExporter(this)
        val result = exporter.exportToJSON(allItems)

        result.onSuccess { file ->
            Toast.makeText(
                this,
                "✅ Exported to:\n${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            Log.d(tag, "JSON export successful: ${file.absolutePath}")
        }.onFailure { error ->
            Toast.makeText(
                this,
                "❌ Export failed: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(tag, "JSON export failed", error)
        }
    }

    private fun shareCollection() {
        if (allItems.isEmpty()) {
            Toast.makeText(this, "Collection is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Export to CSV first
        val exporter = CollectionExporter(this)
        val result = exporter.exportToCSV(allItems)

        result.onSuccess { file ->
            try {
                val fileUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "My Hot Wheels Collection")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Here's my Hot Wheels collection!\n\n${CollectionExporter.getExportStats(allItems)}"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Share Collection"))
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(tag, "Share failed", e)
            }
        }.onFailure { error ->
            Toast.makeText(this, "Failed to export: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImportDialog() {
        val inputOwner = EditText(this).apply {
            hint = getString(R.string.import_owner_name_hint)
        }
        val inputName = EditText(this).apply {
            hint = getString(R.string.import_collection_name_hint)
            setText(getString(R.string.import_default_name))
        }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(inputName)
            addView(inputOwner)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.import_dialog_title)
            .setMessage(R.string.import_dialog_message)
            .setView(layout)
            .setPositiveButton(R.string.import_button_select_file) { _, _ ->
                val collectionName = inputName.text.toString().ifBlank { getString(R.string.import_imported_collection) }
                val ownerName = inputOwner.text.toString().ifBlank { null }

                // Store for later use
                getSharedPreferences("import_temp", MODE_PRIVATE).edit().apply {
                    putString("collection_name", collectionName)
                    putString("owner_name", ownerName)
                    apply()
                }

                // Launch file picker
                importFileLauncher.launch("*/*")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleImportFile(uri: android.net.Uri) {
        // Get stored collection info
        val prefs = getSharedPreferences("import_temp", MODE_PRIVATE)
        val collectionName = prefs.getString("collection_name", "Imported Collection") ?: "Imported Collection"
        val ownerName = prefs.getString("owner_name", null)

        val importer = CollectionImporter(this)
        val fileName = uri.toString()

        lifecycleScope.launch {
            try {
                val result = when {
                    fileName.contains(".csv", ignoreCase = true) -> {
                        importer.importFromCSV(uri, collectionName, ownerName)
                    }
                    fileName.contains(".json", ignoreCase = true) -> {
                        importer.importFromJSON(uri, collectionName, ownerName)
                    }
                    else -> {
                        // Try to detect format
                        if (CollectionImporter.validateJSON(uri, this@CollectionActivity)) {
                            importer.importFromJSON(uri, collectionName, ownerName)
                        } else {
                            importer.importFromCSV(uri, collectionName, ownerName)
                        }
                    }
                }

                result.onSuccess { items ->
                    // Insert items into repository
                    items.forEach { item ->
                        viewModel.insertCollectionItem(item)
                    }

                    Toast.makeText(
                        this@CollectionActivity,
                        "✅ Imported ${items.size} items from \"$collectionName\"",
                        Toast.LENGTH_LONG
                    ).show()

                    Log.d(tag, "Import successful: ${items.size} items from $collectionName")
                }.onFailure { error ->
                    Toast.makeText(
                        this@CollectionActivity,
                        "❌ Import failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(tag, "Import failed", error)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CollectionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(tag, "Import error", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d(tag, "Navigate up pressed")
        onBackPressed()
        return true
    }
}
