package com.hotwheels.identifier.ui.selection

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.hotwheels.identifier.R
import com.hotwheels.identifier.data.entities.HotWheelModel
import com.hotwheels.identifier.data.repository.HotWheelsRepository
import com.hotwheels.identifier.databinding.ActivitySelectResultBinding
import com.hotwheels.identifier.ml.IdentificationMatch
import com.hotwheels.identifier.ui.result.ResultActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class SelectResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectResultBinding
    private lateinit var repository: HotWheelsRepository
    private lateinit var adapter: MatchSelectionAdapter

    private var capturedImagePath: String = ""
    private var matches: List<IdentificationMatch> = emptyList()
    private var capturedPhotos: List<String> = emptyList()
    private var yearFilterStart: Int? = null
    private var yearFilterEnd: Int? = null
    private val allExcludedIds = mutableSetOf<String>()  // Accumulate ALL excluded IDs across retries

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "🏁 onCreate START")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🏁 super.onCreate() complete")

        try {
            Log.d(TAG, "🏁 Inflating binding...")
            binding = ActivitySelectResultBinding.inflate(layoutInflater)
            Log.d(TAG, "🏁 Binding inflated, setting content view...")
            setContentView(binding.root)
            Log.d(TAG, "🏁 Content view set")

            repository = HotWheelsRepository(this)
            Log.d(TAG, "🏁 Repository created")

            // Get data from intent
            capturedImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: ""
            matches = intent.getParcelableArrayListExtra<IdentificationMatch>(EXTRA_MATCHES) ?: emptyList()
            capturedPhotos = intent.getStringArrayListExtra("EXTRA_CAPTURED_PHOTOS") ?: emptyList()
            yearFilterStart = intent.getIntExtra("EXTRA_YEAR_FILTER_START", -1).takeIf { it != -1 }
            yearFilterEnd = intent.getIntExtra("EXTRA_YEAR_FILTER_END", -1).takeIf { it != -1 }

            Log.d(TAG, "🏁 Received ${matches.size} matches for selection")
            Log.d(TAG, "🏁 Captured photos: ${capturedPhotos.size}, year filter: $yearFilterStart-$yearFilterEnd")

            setupUI()
            Log.d(TAG, "🏁 setupUI() complete")
            setupRecyclerView()
            Log.d(TAG, "🏁 setupRecyclerView() complete")
            loadMatches()
            Log.d(TAG, "🏁 loadMatches() called")
            Log.d(TAG, "🏁 onCreate END")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRASH in onCreate: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }

    private fun setupUI() {
        // Botón: Volver a intentar con el mismo auto (excluir top 3 actual)
        binding.btnRetrySame.setOnClickListener {
            showRetryOptionsDialog()
        }

        // Botón: Intentar con otro auto (limpiar exclusiones)
        binding.btnTryDifferent.setOnClickListener {
            com.hotwheels.identifier.viewmodel.CameraViewModel.clearExcludedModels()
            Log.d(TAG, "Cleared exclusions, returning to camera")
            finish()
        }

        // Botón: Ingresar nombre manualmente
        binding.btnManualSearch.setOnClickListener {
            showManualSearchDialog()
        }

        // Show captured image with correct orientation
        if (capturedImagePath.isNotEmpty()) {
            val bitmap = loadBitmapWithCorrectOrientation(capturedImagePath)
            binding.imgCaptured.setImageBitmap(bitmap)
        }
    }

    private fun setupRecyclerView() {
        adapter = MatchSelectionAdapter { match, model ->
            onMatchSelected(match, model)
        }

        binding.recyclerMatches.layoutManager = LinearLayoutManager(this)
        binding.recyclerMatches.adapter = adapter
    }

    private fun loadMatches() {
        Log.d(TAG, "=== LOAD MATCHES START ===")
        Log.d(TAG, "Total matches received: ${matches.size}")

        lifecycleScope.launch {
            val matchesWithModels = matches.mapNotNull { match ->
                Log.d(TAG, "  Processing match: modelId=${match.modelId}, confidence=${match.confidence}")
                // Remove .jpg extension if present
                val modelIdWithoutExtension = match.modelId.removeSuffix(".jpg")
                val model = repository.getModelById(modelIdWithoutExtension)
                if (model != null) {
                    Log.d(TAG, "    ✓ Model found: ${model.name}, image=${model.localImagePath}")
                    Pair(match, model)
                } else {
                    Log.e(TAG, "    ✗ Model NOT found for ID: ${match.modelId}")
                    null
                }
            }

            Log.d(TAG, "=== ADAPTER SUBMIT ===")
            Log.d(TAG, "Submitting ${matchesWithModels.size} items to adapter")
            adapter.submitList(matchesWithModels)
            Log.d(TAG, "Adapter item count after submit: ${adapter.itemCount}")

            if (matchesWithModels.isEmpty()) {
                binding.tvNoResults.visibility = View.VISIBLE
                binding.recyclerMatches.visibility = View.GONE
                Log.d(TAG, "❌ No matches to display - showing no results message")
            } else {
                binding.tvNoResults.visibility = View.GONE
                binding.recyclerMatches.visibility = View.VISIBLE
                Log.d(TAG, "✅ RecyclerView visible with ${matchesWithModels.size} items")
            }

            Log.d(TAG, "=== LOAD MATCHES END ===")
        }
    }

    private fun onMatchSelected(match: IdentificationMatch, model: HotWheelModel) {
        Log.d(TAG, "User selected: ${model.name} (confidence: ${match.confidence})")
        // Show all variants with the same name to increase chance of exact match
        showAllVariantsDialog(model.name, match.confidence)
    }

    private fun showAllVariantsDialog(modelName: String, originalConfidence: Float) {
        Log.d(TAG, "Fetching all variants for: $modelName")

        lifecycleScope.launch {
            // Query all models with the exact same name
            val allModels: List<HotWheelModel> = repository.getAllModels().first()
            val variants = allModels.filter { it.name.equals(modelName, ignoreCase = true) }
                .sortedByDescending { it.year }  // Sort by year, newest first

            Log.d(TAG, "Found ${variants.size} variants of '$modelName'")

            if (variants.isEmpty()) {
                // Shouldn't happen, but fallback
                androidx.appcompat.app.AlertDialog.Builder(this@SelectResultActivity)
                    .setTitle("Error")
                    .setMessage("No se encontraron variantes de este modelo")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            if (variants.size == 1) {
                // Only one variant, go directly to result
                Log.d(TAG, "Only 1 variant found, going directly to result")
                saveAndShowResult(variants[0], originalConfidence)
            } else {
                // Multiple variants, show selection dialog
                Log.d(TAG, "Multiple variants found, showing selection dialog")
                showVariantSelectionDialog(variants, originalConfidence)
            }
        }
    }

    private fun showVariantSelectionDialog(variants: List<HotWheelModel>, originalConfidence: Float) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_results, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_search_results)
        val noResultsText = dialogView.findViewById<TextView>(R.id.tv_no_results)
        val btnNoneMatch = dialogView.findViewById<MaterialButton>(R.id.btn_none_match)

        // Change button text to be more contextual
        btnNoneMatch.text = "Ninguno coincide"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Selecciona el año exacto")
            .setMessage("Se encontraron ${variants.size} variantes de este modelo. Selecciona el año correcto:")
            .setView(dialogView)
            .create()

        val variantsAdapter = SearchResultsAdapter { model ->
            dialog.dismiss()
            saveAndShowResult(model, originalConfidence)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = variantsAdapter
        variantsAdapter.submitList(variants)

        recyclerView.visibility = View.VISIBLE
        noResultsText.visibility = View.GONE

        btnNoneMatch.setOnClickListener {
            dialog.dismiss()
            // User says none match, go back to selection screen
        }

        dialog.show()
    }

    private fun saveAndShowResult(model: HotWheelModel, confidence: Float) {
        lifecycleScope.launch {
            // Guardar resultado en historial
            val identificationResult = com.hotwheels.identifier.data.entities.IdentificationResult(
                hotWheelModelId = model.id,
                imagePath = capturedImagePath,
                confidence = confidence,
                identificationMethod = "MANUAL_SELECTION",
                processingTimeMs = 0
            )
            repository.insertResult(identificationResult)

            // Limpiar exclusiones
            com.hotwheels.identifier.viewmodel.CameraViewModel.clearExcludedModels()

            // Abrir ResultActivity y limpiar stack
            val intent = Intent(this@SelectResultActivity, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_IMAGE_PATH, capturedImagePath)
                putExtra(ResultActivity.EXTRA_MODEL_ID, model.id)
                putExtra(ResultActivity.EXTRA_CONFIDENCE, confidence)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finishAffinity()  // Cierra toda la pila de actividades
        }
    }

    private fun showManualSearchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_search, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_search_query)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Buscar") { _, _ ->
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performManualSearch(query)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun performManualSearch(query: String) {
        Log.d(TAG, "Performing manual search for: $query")

        lifecycleScope.launch {
            val allModels: List<HotWheelModel> = repository.getAllModels().first()

            // Búsqueda fuzzy: buscar en nombre, año y serie
            val results: List<HotWheelModel> = allModels.filter { model ->
                model.name.contains(query, ignoreCase = true) ||
                model.year.toString().contains(query) ||
                model.series?.contains(query, ignoreCase = true) == true
            }.take(5)  // Máximo 5 resultados

            Log.d(TAG, "Found ${results.size} results for query: $query")

            if (results.isEmpty()) {
                androidx.appcompat.app.AlertDialog.Builder(this@SelectResultActivity)
                    .setTitle("Sin resultados")
                    .setMessage("No se encontraron modelos con: \"$query\"")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                showSearchResultsDialog(results)
            }
        }
    }

    private fun showSearchResultsDialog(results: List<HotWheelModel>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_results, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_search_results)
        val noResultsText = dialogView.findViewById<TextView>(R.id.tv_no_results)
        val btnNoneMatch = dialogView.findViewById<MaterialButton>(R.id.btn_none_match)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val searchAdapter = SearchResultsAdapter { model ->
            dialog.dismiss()
            saveAndShowResult(model, 1.0f)  // Confianza 100% porque es selección manual
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = searchAdapter
        searchAdapter.submitList(results)

        if (results.isEmpty()) {
            noResultsText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }

        btnNoneMatch.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRetryOptionsDialog() {
        // Add current matches to the accumulated excluded list
        val currentMatchIds = matches.map { it.modelId }
        allExcludedIds.addAll(currentMatchIds)

        Log.d(TAG, "Total excluded IDs accumulated: ${allExcludedIds.size}")
        Log.d(TAG, "Excluded IDs: ${allExcludedIds.joinToString(", ")}")

        // First ask if they want to change the year filter
        val currentFilterText = if (yearFilterStart != null && yearFilterEnd != null) {
            "Filtro actual: $yearFilterStart-$yearFilterEnd"
        } else {
            "Sin filtro de año"
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Volver a intentar")
            .setMessage("$currentFilterText\n\n¿Deseas cambiar el rango de años antes de buscar de nuevo?")
            .setPositiveButton("Cambiar filtro") { _, _ ->
                showYearFilterDialogForRetry(allExcludedIds.toList())
            }
            .setNegativeButton("Usar mismo filtro") { _, _ ->
                showRetryMethodDialog(allExcludedIds.toList())
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun showRetryMethodDialog(excludedIds: List<String>) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Retomar fotos o usar las actuales?")
            .setPositiveButton("Retomar fotos") { _, _ ->
                // Return to camera to take new photos
                com.hotwheels.identifier.viewmodel.CameraViewModel.addExcludedModels(excludedIds)
                Log.d(TAG, "Excluded ${excludedIds.size} models, returning to camera to retake photos")
                finish()
            }
            .setNegativeButton("Usar fotos actuales") { _, _ ->
                // Retry with same photos
                if (capturedPhotos.isNotEmpty()) {
                    retryWithSamePhotos(excludedIds)
                } else {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("No hay fotos guardadas. Por favor retoma las fotos.")
                        .setPositiveButton("OK") { _, _ ->
                            com.hotwheels.identifier.viewmodel.CameraViewModel.addExcludedModels(excludedIds)
                            finish()
                        }
                        .show()
                }
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun showYearFilterDialogForRetry(excludedIds: List<String>) {
        val options = arrayOf(
            "🔍 Todos los años (1968-2025)",
            "⚡ Últimos 5 años (2021-2025)",
            "📅 Últimos 10 años (2016-2025)",
            "🎯 Año actual (2024-2025) - Por defecto"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Seleccionar rango de años")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        yearFilterStart = null
                        yearFilterEnd = null
                    }
                    1 -> {
                        yearFilterStart = 2021
                        yearFilterEnd = 2025
                    }
                    2 -> {
                        yearFilterStart = 2016
                        yearFilterEnd = 2025
                    }
                    3 -> {
                        yearFilterStart = 2024
                        yearFilterEnd = 2025
                    }
                }
                // After selecting filter, show retry method dialog
                showRetryMethodDialog(excludedIds)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun retryWithSamePhotos(excludedIds: List<String>) {
        Log.d(TAG, "🔄 Retrying identification with ${capturedPhotos.size} existing photos, excluding ${excludedIds.size} models")
        Log.d(TAG, "Excluded IDs: ${excludedIds.joinToString(", ")}")

        // Show loading dialog
        val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Procesando...")
            .setMessage("Buscando nuevas coincidencias con las fotos existentes...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting background identification with MobileNetV3...")
                // Perform heavy processing on background thread
                val newMatches = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val mobileNetIdentifier = com.hotwheels.identifier.ml.MobileNetIdentifier.getInstance(this@SelectResultActivity)

                    mobileNetIdentifier.identifyTopMatchesMultiImage(
                        imagePaths = capturedPhotos,
                        topN = 3,
                        excludeModelIds = excludedIds.toSet(),
                        yearStart = yearFilterStart,
                        yearEnd = yearFilterEnd
                    )
                }

                // Dismiss loading dialog
                loadingDialog.dismiss()

                Log.d(TAG, "✅ Got ${newMatches.size} new matches")
                newMatches.forEachIndexed { index, match ->
                    Log.d(TAG, "  Match ${index + 1}: ${match.modelId}, confidence: ${match.confidence}")
                }

                if (newMatches.isEmpty()) {
                    Log.d(TAG, "⚠️ No new matches found")
                    androidx.appcompat.app.AlertDialog.Builder(this@SelectResultActivity)
                        .setTitle("Sin resultados")
                        .setMessage("No se encontraron más coincidencias. Intenta retomar las fotos o buscar manualmente.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // Update UI with new matches
                    Log.d(TAG, "📝 Updating matches and reloading UI")
                    matches = newMatches
                    loadMatches()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Log.e(TAG, "❌ Error retrying with same photos", e)
                androidx.appcompat.app.AlertDialog.Builder(this@SelectResultActivity)
                    .setTitle("Error")
                    .setMessage("Error al procesar: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun loadBitmapWithCorrectOrientation(imagePath: String): android.graphics.Bitmap? {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null

        try {
            val exif = androidx.exifinterface.media.ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = android.graphics.Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            }

            return android.graphics.Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF orientation: ${e.message}")
            return bitmap
        }
    }

    companion object {
        private const val TAG = "SelectResultActivity"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_MATCHES = "extra_matches"
    }
}

class MatchSelectionAdapter(
    private val onMatchClick: (IdentificationMatch, HotWheelModel) -> Unit
) : RecyclerView.Adapter<MatchSelectionAdapter.MatchViewHolder>() {

    private var items: List<Pair<IdentificationMatch, HotWheelModel>> = emptyList()

    fun submitList(newItems: List<Pair<IdentificationMatch, HotWheelModel>>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_selection, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val (match, model) = items[position]
        holder.bind(match, model, position + 1)
    }

    override fun getItemCount(): Int = items.size

    // CRITICAL: Disable view recycling to prevent image issues
    override fun getItemViewType(position: Int): Int = position

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgModel: ImageView = itemView.findViewById(R.id.img_model)
        private val tvRank: TextView = itemView.findViewById(R.id.tv_rank)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvYear: TextView = itemView.findViewById(R.id.tv_year)
        private val tvSeries: TextView = itemView.findViewById(R.id.tv_series)
        private val tvConfidence: TextView = itemView.findViewById(R.id.tv_confidence)
        private val btnSelect: MaterialButton = itemView.findViewById(R.id.btn_select)

        fun bind(match: IdentificationMatch, model: HotWheelModel, rank: Int) {
            // Set text fields
            tvRank.text = "#$rank"
            tvName.text = model.name
            tvYear.text = model.year.toString()
            tvSeries.text = model.series
            tvConfidence.text = "${(match.confidence * 100).toInt()}% match"

            // Load image - Direct approach without Glide for RecyclerView
            val imagePath = model.localImagePath
            Log.d("MATCH", "Rank $rank: Starting load for path: $imagePath")

            // Clear previous image first
            imgModel.setImageDrawable(null)

            if (!imagePath.isNullOrEmpty()) {
                try {
                    // Load from assets using input stream - synchronous, immediate display
                    val inputStream = itemView.context.assets.open(imagePath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (bitmap != null) {
                        Log.d("MATCH", "Rank $rank: ✅ Bitmap decoded ${bitmap.width}x${bitmap.height}")

                        // Images are already physically rotated - no runtime rotation needed
                        imgModel.setImageBitmap(bitmap)
                        imgModel.visibility = View.VISIBLE

                        // Verify after setting
                        imgModel.post {
                            val currentDrawable = imgModel.drawable
                            val currentWidth = imgModel.width
                            val currentHeight = imgModel.height
                            Log.d("MATCH", "Rank $rank: 🔍 VERIFY - ImageView: ${currentWidth}x${currentHeight}, " +
                                    "Drawable: ${currentDrawable?.javaClass?.simpleName}, " +
                                    "Visibility: ${imgModel.visibility}")
                        }

                        Log.d("MATCH", "Rank $rank: ✅ Image set successfully")
                    } else {
                        Log.e("MATCH", "Rank $rank: ❌ Bitmap is null")
                        imgModel.setImageResource(R.drawable.ic_car_placeholder)
                    }
                } catch (e: Exception) {
                    Log.e("MATCH", "Rank $rank: ❌ Error loading: ${e.message}", e)
                    imgModel.setImageResource(R.drawable.ic_car_placeholder)
                }
            } else {
                Log.w("MATCH", "Rank $rank: ⚠️ No image path")
                imgModel.setImageResource(R.drawable.ic_car_placeholder)
            }

            btnSelect.setOnClickListener {
                onMatchClick(match, model)
            }

            itemView.setOnClickListener {
                onMatchClick(match, model)
            }
        }
    }
}

class SearchResultsAdapter(
    private val onModelClick: (HotWheelModel) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.SearchViewHolder>() {

    private var items: List<HotWheelModel> = emptyList()

    fun submitList(newItems: List<HotWheelModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgModel: ImageView = itemView.findViewById(R.id.img_model)
        private val tvName: TextView = itemView.findViewById(R.id.tv_model_name)
        private val tvYear: TextView = itemView.findViewById(R.id.tv_model_year)
        private val tvSeries: TextView = itemView.findViewById(R.id.tv_model_series)
        private val card: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.card_search_result)

        fun bind(model: HotWheelModel) {
            tvName.text = model.name
            tvYear.text = model.year.toString()
            tvSeries.text = model.series ?: "Serie desconocida"

            // Load image
            loadModelImage(model)

            card.setOnClickListener {
                onModelClick(model)
            }
        }

        private fun loadModelImage(model: HotWheelModel) {
            val imagePath = model.localImagePath
            if (!imagePath.isNullOrEmpty()) {
                try {
                    val context = itemView.context
                    val inputStream = context.assets.open(imagePath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    // Images are already physically rotated
                    imgModel.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("SearchViewHolder", "Error loading image: ${e.message}")
                    imgModel.setImageResource(R.drawable.ic_car_placeholder)
                }
            } else {
                imgModel.setImageResource(R.drawable.ic_car_placeholder)
            }
        }
    }
}

