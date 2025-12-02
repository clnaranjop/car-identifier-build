package com.hotwheels.identifier.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hotwheels.identifier.databinding.ActivityModelDetailsBinding
import com.hotwheels.identifier.ui.fullscreen.FullScreenImageActivity
import com.hotwheels.identifier.viewmodel.ResultViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ModelDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelDetailsBinding
    private val viewModel: ResultViewModel by viewModels()
    private var modelId: String? = null

    companion object {
        const val EXTRA_MODEL_ID = "extra_model_id"
        private const val PREFS_NAME = "price_updates"
        private const val KEY_LAST_UPDATE = "last_update_"

        // Feature flags for stores (set to false to hide, true to show)
        private const val ENABLE_EBAY = false      // TODO: Enable when eBay API is ready
        private const val ENABLE_AMAZON = false    // TODO: Enable when Amazon API is ready
        private const val ENABLE_COLLECTOR = false // TODO: Enable when collector data is ready
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityModelDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modelId = intent.getStringExtra(EXTRA_MODEL_ID)

        setupToolbar()
        setupRefreshButton()
        setupStoreVisibility()
        observeViewModel()
        loadModelData()
    }

    private fun setupStoreVisibility() {
        // Hide stores based on feature flags
        binding.layoutPriceEbay.visibility = if (ENABLE_EBAY) View.VISIBLE else View.GONE
        binding.layoutPriceAmazon.visibility = if (ENABLE_AMAZON) View.VISIBLE else View.GONE
        binding.layoutPriceCollector.visibility = if (ENABLE_COLLECTOR) View.VISIBLE else View.GONE
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Model Details"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRefreshButton() {
        binding.btnRefreshPrices.setOnClickListener {
            modelId?.let { id ->
                refreshPrices(id)
            }
        }
    }

    private fun refreshPrices(modelId: String) {
        Toast.makeText(this, "Actualizando precios...", Toast.LENGTH_SHORT).show()

        // Update the last update timestamp
        saveLastUpdateTime(modelId)

        // Reload the model data to recalculate prices
        viewModel.loadHotWheelModel(modelId)

        Toast.makeText(this, "Precios actualizados", Toast.LENGTH_SHORT).show()
    }

    private fun saveLastUpdateTime(modelId: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_UPDATE + modelId, System.currentTimeMillis()).apply()
    }

    private fun getLastUpdateTime(modelId: String): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_UPDATE + modelId, 0)
    }

    private fun formatLastUpdateTime(timestamp: Long): String {
        if (timestamp == 0L) {
            return "Última actualización: Nunca"
        }

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "Última actualización: Hace ${days} día${if (days > 1) "s" else ""}"
            hours > 0 -> "Última actualización: Hace ${hours} hora${if (hours > 1) "s" else ""}"
            minutes > 0 -> "Última actualización: Hace ${minutes} minuto${if (minutes > 1) "s" else ""}"
            else -> "Última actualización: Hace unos segundos"
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.hotWheelModel.collect { model ->
                model?.let { displayModelInfo(it) }
            }
        }
    }

    private fun loadModelData() {
        modelId?.let { id ->
            viewModel.loadHotWheelModel(id)
        }
    }

    private fun displayModelInfo(model: com.hotwheels.identifier.data.entities.HotWheelModel) {
        Log.d("ModelDetailsActivity", "Displaying model: ${model.name}")
        Log.d("ModelDetailsActivity", "📋 Series: '${model.series}', Year: ${model.year}, Category: '${model.category}', Manufacturer: '${model.manufacturer}'")

        binding.apply {
            tvModelName.text = model.name
            tvSeries.text = if (model.series.isNullOrBlank()) "Unknown" else model.series
            tvYear.text = model.year?.toString() ?: "Unknown"
            tvCategory.text = if (model.category.isNullOrBlank()) "Unknown" else model.category
            tvManufacturer.text = if (model.manufacturer.isNullOrBlank()) "Mattel" else model.manufacturer

            // Set estimated prices (in the future, fetch from API)
            loadPriceEstimates(model)

            // Display last update time
            val lastUpdate = getLastUpdateTime(model.id)
            if (lastUpdate == 0L) {
                // First time viewing this model, save current time
                saveLastUpdateTime(model.id)
                tvLastUpdated.text = "Última actualización: Ahora"
            } else {
                tvLastUpdated.text = formatLastUpdateTime(lastUpdate)
            }

            // Load reference image from assets
            model.localImagePath?.let { imagePath ->
                try {
                    Log.d("ModelDetailsActivity", "📸 Loading image from assets: $imagePath")
                    val inputStream = assets.open(imagePath)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (bitmap != null) {
                        // Images are already physically rotated
                        Glide.with(this@ModelDetailsActivity)
                            .load(bitmap)
                            .centerCrop()
                            .into(ivModelImage)
                        Log.d("ModelDetailsActivity", "✅ Image loaded successfully")

                        // Add click listener to open fullscreen viewer
                        ivModelImage.setOnClickListener {
                            val intent = Intent(this@ModelDetailsActivity, FullScreenImageActivity::class.java)
                            intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_PATH, imagePath)
                            intent.putExtra(FullScreenImageActivity.EXTRA_MODEL_NAME, model.name)
                            startActivity(intent)
                        }
                    } else {
                        Log.w("ModelDetailsActivity", "⚠️ Failed to decode bitmap")
                        Glide.with(this@ModelDetailsActivity)
                            .load(android.R.drawable.ic_menu_gallery)
                            .into(ivModelImage)
                    }
                } catch (e: Exception) {
                    Log.e("ModelDetailsActivity", "❌ Error loading image: ${e.message}", e)
                    Glide.with(this@ModelDetailsActivity)
                        .load(android.R.drawable.ic_menu_gallery)
                        .into(ivModelImage)
                }
            } ?: run {
                Log.w("ModelDetailsActivity", "⚠️ No image path")
                Glide.with(this@ModelDetailsActivity)
                    .load(android.R.drawable.ic_menu_gallery)
                    .into(ivModelImage)
            }
        }
    }

    private fun loadPriceEstimates(model: com.hotwheels.identifier.data.entities.HotWheelModel) {
        // Calculate estimated prices based on year and rarity
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val age = model.year?.let { currentYear - it } ?: 0

        // Base price calculation (older = more valuable)
        val basePrice = when {
            age > 30 -> 15.0  // Vintage (pre-1995)
            age > 20 -> 10.0  // Classic (1995-2005)
            age > 10 -> 5.0   // Modern (2005-2015)
            else -> 3.0       // Recent (2015+)
        }

        // Add variance for different marketplaces
        val ebayMin = String.format("$%.2f", basePrice * 0.8)
        val ebayMax = String.format("$%.2f", basePrice * 1.5)

        val mercadoLibreMin = String.format("$%.2f", basePrice * 1.0)
        val mercadoLibreMax = String.format("$%.2f", basePrice * 1.8)

        val amazonMin = String.format("$%.2f", basePrice * 0.9)
        val amazonMax = String.format("$%.2f", basePrice * 1.6)

        val collectorMin = String.format("$%.2f", basePrice * 1.5)
        val collectorMax = String.format("$%.2f", basePrice * 3.0)

        binding.apply {
            tvPriceEbay.text = "$ebayMin - $ebayMax"
            tvPriceMercadoLibre.text = "$mercadoLibreMin - $mercadoLibreMax"
            tvPriceAmazon.text = "$amazonMin - $amazonMax"
            tvPriceCollector.text = "$collectorMin - $collectorMax"
        }

        Log.d("ModelDetailsActivity", "💰 Estimated prices - eBay: $ebayMin-$ebayMax, ML: $mercadoLibreMin-$mercadoLibreMax")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
