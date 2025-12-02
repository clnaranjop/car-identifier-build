package com.hotwheels.identifier.ui.result

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.hotwheels.identifier.R
import com.hotwheels.identifier.databinding.ActivityResultBinding
import com.hotwheels.identifier.ui.camera.CameraActivity
import com.hotwheels.identifier.ui.MainActivity
import com.hotwheels.identifier.viewmodel.ResultViewModel
import kotlinx.coroutines.launch
import java.io.File

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: ResultViewModel by viewModels()

    private var imagePath: String? = null
    private var modelId: String? = null
    private var confidence: Float = 0f

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_MODEL_ID = "extra_model_id"
        const val EXTRA_CONFIDENCE = "extra_confidence"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentData()
        setupToolbar()
        setupClickListeners()
        observeViewModel()
        displayResult()
        initializeAds()
    }

    private fun extractIntentData() {
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        modelId = intent.getStringExtra(EXTRA_MODEL_ID)
        confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Identification Result"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveToCollection.setOnClickListener {
            modelId?.let { id ->
                if (id.isNotEmpty()) {
                    viewModel.saveToCollection(id, imagePath)
                } else {
                    showMessage("Cannot save unidentified item")
                }
            }
        }

        binding.btnTryAgain.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnShareResult.setOnClickListener {
            shareResult()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.hotWheelModel.collect { model ->
                model?.let { displayModelInfo(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.saveStatus.collect { status ->
                status?.let {
                    showMessage(it)
                    viewModel.clearSaveStatus()

                    // Return to main screen after successfully adding to collection
                    if (it.contains("Added", ignoreCase = true) && it.contains("collection", ignoreCase = true)) {
                        kotlinx.coroutines.delay(1500) // Show message for 1.5 seconds
                        val intent = Intent(this@ResultActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    showMessage(it)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun displayResult() {
        // Display captured image
        imagePath?.let { path ->
            Glide.with(this)
                .load(File(path))
                .centerCrop()
                .into(binding.ivCapturedImage)
        }

        // Display confidence
        val confidenceText = "${(confidence * 100).toInt()}%"
        binding.tvConfidenceBadge.text = confidenceText
        binding.tvConfidence.text = confidenceText

        // Load model data if identified
        if (!modelId.isNullOrEmpty() && confidence > 0) {
            binding.resultCard.visibility = android.view.View.VISIBLE
            binding.noResultCard.visibility = android.view.View.GONE
            viewModel.loadHotWheelModel(modelId!!)
        } else {
            binding.resultCard.visibility = android.view.View.GONE
            binding.noResultCard.visibility = android.view.View.VISIBLE
            binding.btnSaveToCollection.isEnabled = false
        }
    }

    private fun displayModelInfo(model: com.hotwheels.identifier.data.entities.HotWheelModel) {
        Log.d("ResultActivity", "Displaying model info:")
        Log.d("ResultActivity", "  Model ID: '${model.id}'")
        Log.d("ResultActivity", "  Model name: '${model.name}'")
        Log.d("ResultActivity", "  Model series: '${model.series}'")
        Log.d("ResultActivity", "  Model year: '${model.year}'")
        Log.d("ResultActivity", "  Model category: '${model.category}'")
        Log.d("ResultActivity", "  Local image path: '${model.localImagePath}'")

        binding.apply {
            // Limpiar el nombre por seguridad adicional
            val cleanName = cleanDisplayName(model.name)
            Log.d("ResultActivity", "  Final display name: '$cleanName'")

            tvModelName.text = cleanName
            tvSeries.text = model.series
            tvYear.text = model.year.toString()
            tvCategory.text = model.category

            // Load reference image from database (stored in assets)
            model.localImagePath?.let { imagePath ->
                try {
                    Log.d("ResultActivity", "📸 Loading reference image from assets: $imagePath")

                    // Load from assets using input stream
                    val inputStream = assets.open(imagePath)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (bitmap != null) {
                        // Images are already physically rotated
                        Glide.with(this@ResultActivity)
                            .load(bitmap)
                            .centerCrop()
                            .into(ivReferenceImage)
                        Log.d("ResultActivity", "✅ Reference image loaded successfully from assets")
                    } else {
                        Log.w("ResultActivity", "⚠️ Failed to decode bitmap from assets: $imagePath")
                        // Load placeholder
                        Glide.with(this@ResultActivity)
                            .load(R.drawable.ic_car_placeholder)
                            .into(ivReferenceImage)
                    }
                } catch (e: Exception) {
                    Log.e("ResultActivity", "❌ Error loading reference image from assets: ${e.message}", e)
                    // Load placeholder on error
                    Glide.with(this@ResultActivity)
                        .load(R.drawable.ic_car_placeholder)
                        .into(ivReferenceImage)
                }
            } ?: run {
                Log.w("ResultActivity", "⚠️ No local image path available for model")
                // Load placeholder if no image path
                Glide.with(this@ResultActivity)
                    .load(R.drawable.ic_car_placeholder)
                    .into(ivReferenceImage)
            }
        }
    }

    private fun shareResult() {
        val imagePath = this.imagePath ?: return

        try {
            val imageFile = File(imagePath)
            val imageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )

            val shareText = if (!modelId.isNullOrEmpty() && confidence > 0) {
                "I identified this Hot Wheels car with ${(confidence * 100).toInt()}% confidence using Hot Wheels Identifier!"
            } else {
                "I scanned this car with Hot Wheels Identifier - can you help identify it?"
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Result"))

        } catch (e: Exception) {
            showMessage("Failed to share result")
        }
    }

    private fun initializeAds() {
        MobileAds.initialize(this) { }

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Limpia nombres de modelos para mostrar en UI
     */
    private fun cleanDisplayName(name: String?): String {
        if (name.isNullOrBlank()) return "Unknown Model"

        return name
            // Limpiar entidades HTML que puedan haberse escapado
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#160;", " ")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8230;", "...")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            // Limpiar fragmentos de HTML
            .replace(Regex("""<[^>]*>"""), "")
            .replace(Regex("""&[a-zA-Z0-9#]+;"""), "")
            .replace(Regex("""\s+"""), " ")
            .replace("Please see the", "")
            .replace("Please see", "")
            .trim()
            .takeIf { it.isNotBlank() && it.length >= 2 } ?: "Unknown Model"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temporary image file
        imagePath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}