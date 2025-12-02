package com.hotwheels.identifier.ui.camera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.hotwheels.identifier.R
import com.hotwheels.identifier.databinding.ActivityCameraBinding
import com.hotwheels.identifier.ui.result.ResultActivity
import com.hotwheels.identifier.utils.FileUtils
import com.hotwheels.identifier.utils.VideoFrameExtractor
import com.hotwheels.identifier.viewmodel.CameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private val viewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false

    // Multi-photo capture
    private val capturedPhotos = mutableListOf<String>()
    private var currentPhotoIndex = 0
    private lateinit var photoAngles: List<PhotoAngle>

    // Year filter - Default to all years (null = no filter)
    private var yearFilterStart: Int? = null  // Search all years by default
    private var yearFilterEnd: Int? = null

    data class PhotoAngle(
        val instruction: String,
        val exampleDrawable: Int,
        val detailedHelp: String
    )

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            processImageFromGallery(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ocultar la Action Bar para tener control completo de la UI
        supportActionBar?.hide()

        // Mantener pantalla encendida mientras usa la cámara
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // SIMPLIFIED: Only 2 photos needed (Side + 45° angle)
        // The algorithm will automatically mirror/flip images internally for better matching
        photoAngles = listOf(
            PhotoAngle(
                "📸 Foto 1: Vista Lateral",
                R.drawable.example_pagani_utopia,
                "Coloca el auto de COSTADO - debe verse todo un LADO completo (perfil)"
            ),
            PhotoAngle(
                "📸 Foto 2: Ángulo 45°",
                R.drawable.example_shelby_cobra,
                "Rota 45° - debe verse una ESQUINA (frente/trasera + lado al mismo tiempo)"
            )
        )

        // Cleanup old images on startup (images older than 7 days)
        FileUtils.cleanupOldCapturedImages(this)

        setupClickListeners()
        observeViewModel()
        updatePhotoGuide()
        updateYearFilterButton()  // Show default filter state
        startCamera()
    }

    private fun updatePhotoGuide() {
        val currentAngle = photoAngles[currentPhotoIndex]
        binding.tvPhotoProgress.text = "📷 Foto ${currentPhotoIndex + 1} de ${photoAngles.size}"
        binding.tvAngleInstruction.text = currentAngle.instruction
        binding.tvDetailedHelp.text = currentAngle.detailedHelp
        binding.imgAngleExample.setImageResource(currentAngle.exampleDrawable)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnRecordVideo.setOnClickListener {
            if (isRecording) {
                stopVideoRecording()
            } else {
                startVideoRecording()
            }
        }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        binding.btnYearFilter.setOnClickListener {
            showYearFilterDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isProcessing.collect { isProcessing ->
                binding.processingOverlay.visibility = if (isProcessing) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.processingStatus.collect { status ->
                binding.tvProcessingStatus.text = status
            }
        }

        // Observe identification result - use a flag to avoid processing the same result multiple times
        var lastProcessedResultId: Long? = null
        lifecycleScope.launch {
            viewModel.identificationResult.collect { result ->
                result?.let {
                    // Only process if this is a new result
                    if (lastProcessedResultId == it.id) {
                        Log.d(TAG, "⏭️ Skipping already processed result ID: ${it.id}")
                        return@collect
                    }

                    lastProcessedResultId = it.id
                    Log.d(TAG, "🎯 Identification result received: ModelId=${it.hotWheelModelId}, Confidence=${it.confidence}, ID=${it.id}")

                    // Wait a bit for topMatches to be set
                    kotlinx.coroutines.delay(100)

                    // Check if we have top matches ready
                    val matches = viewModel.topMatches.value
                    Log.d(TAG, "📊 Current topMatches: ${matches.size} matches")

                    if (matches.isNotEmpty()) {
                        Log.d(TAG, "✅ Opening SelectResultActivity with ${matches.size} matches")
                        openSelectionActivity(it.imagePath, matches)
                    } else if (it.hotWheelModelId.isNotEmpty()) {
                        Log.d(TAG, "📝 No top matches, showing simple dialog")
                        showResultDialog(it.hotWheelModelId, it.confidence)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.processingStatus.collect { status ->
                if (status.isNotEmpty()) {
                    Log.d(TAG, "Processing status: $status")
                    showProcessingMessage(status)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isProcessing.collect { isProcessing ->
                Log.d(TAG, "Processing state changed: $isProcessing")
                // You could show/hide a progress indicator here
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Log.e(TAG, "Error message: $it")
                    showError(it)
                    viewModel.clearError()
                }
            }
        }

        // Monitor MobileNetV3 loading progress
        lifecycleScope.launch {
            viewModel.mobileNetIsReady.collect { isReady ->
                if (!isReady) {
                    showLoadingOverlay()
                } else {
                    hideLoadingOverlay()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.mobileNetLoadingStatus.collect { status ->
                updateLoadingStatus(status)
            }
        }

        lifecycleScope.launch {
            viewModel.mobileNetLoadingProgress.collect { progress ->
                updateLoadingProgress(progress)
            }
        }
    }

    private var loadingDialog: android.app.ProgressDialog? = null

    private fun showLoadingOverlay() {
        if (loadingDialog == null) {
            loadingDialog = android.app.ProgressDialog(this).apply {
                setTitle("Cargando Hot Wheels Identifier")
                setMessage("Espere por favor mientras se carga la base de datos...")
                setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
                max = 100
                progress = 0
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }
        }
        loadingDialog?.show()
    }

    private fun hideLoadingOverlay() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun updateLoadingStatus(status: String) {
        // Update dialog text if showing
        runOnUiThread {
            loadingDialog?.setMessage(status)
        }
    }

    private fun updateLoadingProgress(progress: Int) {
        runOnUiThread {
            loadingDialog?.progress = progress
        }
        Log.d(TAG, "MobileNet loading: $progress%")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                showError("Failed to start camera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        // VideoCapture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, videoCapture
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            showError("Failed to bind camera")
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = FileUtils.createTempImageFile(this)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    showError("Failed to capture photo")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo ${currentPhotoIndex + 1} capture succeeded: ${photoFile.absolutePath}")

                    // Add to captured photos list
                    capturedPhotos.add(photoFile.absolutePath)
                    currentPhotoIndex++

                    // Check if we have all photos
                    if (currentPhotoIndex >= photoAngles.size) {
                        // All photos captured, process them
                        processMultipleImages(capturedPhotos)
                    } else {
                        // Update UI for next photo
                        updatePhotoGuide()
                        showMessage("Foto ${currentPhotoIndex}/${photoAngles.size} capturada. Siguiente ángulo...")
                    }
                }
            }
        )
    }

    private fun toggleFlash() {
        camera?.let { camera ->
            val currentFlashMode = camera.cameraInfo.torchState.value ?: TorchState.OFF
            camera.cameraControl.enableTorch(currentFlashMode == TorchState.OFF)

            // Update flash icon
            val iconRes = if (currentFlashMode == TorchState.OFF) {
                R.drawable.ic_flash_off // Will need to create ic_flash_on
            } else {
                R.drawable.ic_flash_off
            }
            binding.btnFlash.setImageResource(iconRes)
        }
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        bindCameraUseCases()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun processMultipleImages(imagePaths: List<String>) {
        Log.d(TAG, "Starting multi-image processing for ${imagePaths.size} photos")
        val filterMsg = if (yearFilterStart != null && yearFilterEnd != null) {
            " (años $yearFilterStart-$yearFilterEnd)"
        } else {
            ""
        }
        showProcessingMessage("Procesando ${imagePaths.size} fotos$filterMsg...")

        // Crop all images
        val croppedPaths = imagePaths.mapNotNull { imagePath ->
            cropImageToGuideArea(imagePath) ?: imagePath
        }

        viewModel.processMultipleImages(croppedPaths, yearFilterStart, yearFilterEnd)

        // DON'T reset capture session here - keep photos for retry
        // They will be reset when user explicitly takes new photos or in onResume()
    }

    private fun resetCaptureSession() {
        Log.d(TAG, "Resetting capture session")
        capturedPhotos.clear()
        currentPhotoIndex = 0
        updatePhotoGuide()
    }

    private fun processImage(imagePath: String) {
        Log.d(TAG, "Starting image processing for: $imagePath")
        showProcessingMessage("Processing image...")

        // Recortar imagen al área del recuadro antes de procesar
        val croppedPath = cropImageToGuideArea(imagePath)
        viewModel.processImage(croppedPath ?: imagePath)
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun cropImageToGuideArea(imagePath: String): String? {
        return try {
            // Primero cargar con orientación correcta
            var bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)

            // Aplicar rotación EXIF
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
                }

                if (orientation != androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL) {
                    val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    bitmap.recycle()
                    bitmap = rotatedBitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading EXIF: ${e.message}")
            }

            // Usar porcentaje fijo del centro de la imagen
            // El recuadro es aprox 280x180dp, proporción 1.56:1 (ancho:alto)
            // Recortar el 50% del centro horizontal y 25% del centro vertical

            val cropWidthPercent = 0.60f  // 60% del ancho
            val cropHeightPercent = 0.35f  // 35% del alto

            val cropWidth = (bitmap.width * cropWidthPercent).toInt()
            val cropHeight = (bitmap.height * cropHeightPercent).toInt()

            // Centrar el crop
            val cropX = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
            val cropY = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)

            // Crear bitmap recortado
            val croppedBitmap = android.graphics.Bitmap.createBitmap(
                bitmap,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )

            bitmap.recycle()

            // Guardar bitmap recortado
            val croppedFile = File(imagePath.replace(".jpg", "_cropped.jpg"))
            java.io.FileOutputStream(croppedFile).use { out ->
                croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }
            croppedBitmap.recycle()

            Log.d(TAG, "Image cropped successfully. Original: ${bitmap.width}x${bitmap.height}, Cropped: ${cropWidth}x${cropHeight}")
            croppedFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop image: ${e.message}", e)
            null
        }
    }

    private fun showProcessingMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun processImageFromGallery(uri: Uri) {
        try {
            val tempFile = FileUtils.copyUriToTempFile(this, uri)
            processImage(tempFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process gallery image", e)
            showError("Failed to process selected image")
        }
    }

    private fun openSelectionActivity(imagePath: String, matches: List<com.hotwheels.identifier.ml.IdentificationMatch>) {
        try {
            Log.d(TAG, "Opening SelectResultActivity with ${matches.size} matches")

            val intent = Intent(this, com.hotwheels.identifier.ui.selection.SelectResultActivity::class.java).apply {
                putExtra(com.hotwheels.identifier.ui.selection.SelectResultActivity.EXTRA_IMAGE_PATH, imagePath)
                putParcelableArrayListExtra(
                    com.hotwheels.identifier.ui.selection.SelectResultActivity.EXTRA_MATCHES,
                    ArrayList(matches)
                )
                // Pass captured photos for retry option
                putStringArrayListExtra("EXTRA_CAPTURED_PHOTOS", ArrayList(capturedPhotos))
                // Pass year filter for retry
                yearFilterStart?.let { putExtra("EXTRA_YEAR_FILTER_START", it) }
                yearFilterEnd?.let { putExtra("EXTRA_YEAR_FILTER_END", it) }
            }
            startActivity(intent)
            // Don't finish() here - allow user to return to camera
        } catch (e: Exception) {
            Log.e(TAG, "Error opening selection activity", e)
            showError("Error showing results: ${e.message}")
        }
    }

    private fun openResultActivity(imagePath: String, modelId: String?, confidence: Float) {
        try {
            Log.d(TAG, "Attempting to open ResultActivity with modelId: $modelId, confidence: $confidence")

            // For now, show result as a simple dialog instead of opening new activity
            showResultDialog(modelId, confidence)

            // Original code commented out for debugging:
            /*
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_IMAGE_PATH, imagePath)
                putExtra(ResultActivity.EXTRA_MODEL_ID, modelId)
                putExtra(ResultActivity.EXTRA_CONFIDENCE, confidence)
            }
            startActivity(intent)
            finish()
            */
        } catch (e: Exception) {
            Log.e(TAG, "Error opening result activity", e)
            showError("Error showing results: ${e.message}")
        }
    }

    private fun showResultDialog(modelId: String?, confidence: Float) {
        lifecycleScope.launch {
            try {
                val repository = com.hotwheels.identifier.data.repository.HotWheelsRepository(this@CameraActivity)

                val message = if (modelId != null && modelId.isNotEmpty()) {
                    val model = repository.getModelById(modelId)
                    if (model != null) {
                        """
                        ✅ Hot Wheels Identificado!

                        🚗 Modelo: ${model.name}
                        📊 Confianza: ${(confidence * 100).toInt()}%
                        🏁 Serie: ${model.series}
                        📅 Año: ${model.year}
                        🎯 Categoría: ${model.category}
                        💎 Rareza: ${model.rarity}

                        ${model.description ?: ""}
                        """.trimIndent()
                    } else {
                        "✅ Hot Wheels detectado!\n\nModel ID: $modelId\nConfianza: ${(confidence * 100).toInt()}%"
                    }
                } else {
                    "❌ No se detectó Hot Wheels\n\nConfianza muy baja: ${(confidence * 100).toInt()}%"
                }

                androidx.appcompat.app.AlertDialog.Builder(this@CameraActivity)
                    .setTitle("Resultado de Identificación")
                    .setMessage(message)
                    .setPositiveButton("Escanear Otro") { _, _ ->
                        // Stay in camera for another try
                    }
                    .setNegativeButton("Volver") { _, _ ->
                        finish()
                    }
                    .show()

            } catch (e: Exception) {
                Log.e(TAG, "Error showing result dialog", e)
                // Fallback to simple dialog
                androidx.appcompat.app.AlertDialog.Builder(this@CameraActivity)
                    .setTitle("Resultado")
                    .setMessage("Modelo: $modelId\nConfianza: ${(confidence * 100).toInt()}%")
                    .setPositiveButton("OK") { _, _ -> }
                    .show()
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showYearFilterDialog() {
        Log.d(TAG, "📅 showYearFilterDialog called")

        val currentFilter = if (yearFilterStart != null && yearFilterEnd != null) {
            "Filtro actual: $yearFilterStart-$yearFilterEnd"
        } else {
            "Sin filtro (todos los años)"
        }

        Log.d(TAG, "Current filter status: $currentFilter")

        val options = arrayOf(
            "🔍 Todos los años (1968-2024)",
            "⚡ Últimos 5 años (2020-2024) - Rápido",
            "⭐ Últimos 10 años (2015-2024) - Recomendado",
            "📅 Últimos 20 años (2005-2024)",
            "✏️ Rango personalizado..."
        )

        val title = "⏱️ Filtrar por año\n\n$currentFilter\nSelecciona un rango:"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                Log.d(TAG, "Option selected: $which")
                when (which) {
                    0 -> {
                        yearFilterStart = null
                        yearFilterEnd = null
                        updateYearFilterButton()
                        showMessage("✓ Buscando en todos los años (11,412 modelos)")
                    }
                    1 -> {
                        yearFilterStart = 2020
                        yearFilterEnd = 2024
                        updateYearFilterButton()
                        showMessage("✓ Filtro aplicado: 2020-2024")
                    }
                    2 -> {
                        yearFilterStart = 2015
                        yearFilterEnd = 2024
                        updateYearFilterButton()
                        showMessage("✓ Filtro aplicado: 2015-2024")
                    }
                    3 -> {
                        yearFilterStart = 2005
                        yearFilterEnd = 2024
                        updateYearFilterButton()
                        showMessage("✓ Filtro aplicado: 2005-2024")
                    }
                    4 -> showCustomYearRangeDialog()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        Log.d(TAG, "About to show dialog")
        dialog.show()
        Log.d(TAG, "Dialog shown successfully")
    }

    private fun updateYearFilterButton() {
        // Change button color when filter is active
        if (yearFilterStart != null && yearFilterEnd != null) {
            binding.btnYearFilter.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            binding.btnYearFilter.clearColorFilter()
        }
    }

    private fun showCustomYearRangeDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.select_dialog_item, null)
        val input = android.widget.EditText(this)
        input.hint = "Ej: 2010-2020"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Rango personalizado")
            .setMessage("Ingresa el rango de años (ej: 2010-2020):")
            .setView(input)
            .setPositiveButton("Aplicar") { _, _ ->
                val text = input.text.toString()
                val parts = text.split("-")
                if (parts.size == 2) {
                    try {
                        yearFilterStart = parts[0].trim().toInt()
                        yearFilterEnd = parts[1].trim().toInt()
                        updateYearFilterButton()
                        showMessage("✓ Filtro aplicado: $yearFilterStart-$yearFilterEnd")
                    } catch (e: Exception) {
                        showMessage("❌ Formato inválido. Usa: YYYY-YYYY")
                    }
                } else {
                    showMessage("❌ Formato inválido. Usa: YYYY-YYYY")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ══════════════════════════════════════════════════════════════════════
    // VIDEO RECORDING FUNCTIONS
    // ══════════════════════════════════════════════════════════════════════

    private fun startVideoRecording() {
        Log.d(TAG, "🎬 Starting countdown before video recording...")

        // Disable buttons during countdown
        binding.btnRecordVideo.isEnabled = false
        binding.btnCapture.isEnabled = false
        binding.btnGallery.isEnabled = false
        binding.btnSwitchCamera.isEnabled = false

        // Countdown 3, 2, 1 before recording
        var countdown = 3
        val preCountdownRunnable = object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    binding.tvGuidance.text = countdown.toString()
                    binding.tvGuidance.textSize = 120f  // Large text
                    countdown--
                    binding.root.postDelayed(this, 1000)
                } else {
                    // Reset text size
                    binding.tvGuidance.textSize = 18f

                    // NOW start actual recording
                    actuallyStartRecording()
                }
            }
        }
        binding.root.post(preCountdownRunnable)
    }

    private fun actuallyStartRecording() {
        Log.d(TAG, "🎥 Actually starting video recording...")

        val videoFile = File(filesDir, "hotwheels_video_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture?.output
            ?.prepareRecording(this, outputOptions)
            ?.apply {
                // Enable audio if permission granted
                if (ContextCompat.checkSelfPermission(
                        this@CameraActivity,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        updateRecordingUI(true)
                        Log.d(TAG, "✅ Recording started")
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val videoPath = videoFile.absolutePath
                            Log.d(TAG, "✅ Video saved: $videoPath")
                            isRecording = false
                            updateRecordingUI(false)

                            // Procesar video
                            processVideo(videoPath)
                        } else {
                            Log.e(TAG, "❌ Video recording error: ${recordEvent.error}")
                            showMessage("Error al grabar video")
                            isRecording = false
                            updateRecordingUI(false)
                            videoFile.delete()
                        }
                    }
                }
            }

        // Auto-stop after 5 seconds with countdown
        var secondsLeft = 5
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (isRecording && secondsLeft > 0) {
                    binding.tvGuidance.text = "🔴 GRABANDO... ${secondsLeft}s"
                    secondsLeft--
                    binding.root.postDelayed(this, 1000)
                } else if (secondsLeft == 0 && isRecording) {
                    stopVideoRecording()
                }
            }
        }
        binding.root.postDelayed(countdownRunnable, 1000)
    }

    private fun stopVideoRecording() {
        Log.d(TAG, "🛑 Stopping video recording...")
        recording?.stop()
        recording = null
    }

    private fun updateRecordingUI(recording: Boolean) {
        binding.btnRecordVideo.apply {
            isEnabled = !recording
            if (recording) {
                setImageResource(android.R.drawable.ic_media_pause)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.RED
                )
            } else {
                setImageResource(android.R.drawable.presence_video_online)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.hotwheels_red)
                )
            }
        }

        // Re-enable buttons when not recording
        binding.btnCapture.isEnabled = !recording
        binding.btnGallery.isEnabled = !recording
        binding.btnSwitchCamera.isEnabled = !recording

        // Reset text size and show recording indicator
        if (!recording) {
            binding.tvGuidance.textSize = 18f
            binding.tvGuidance.text = getString(R.string.guidance_position)
        }
    }

    private fun processVideo(videoPath: String) {
        Log.d(TAG, "🎞️ Processing video: $videoPath")
        showMessage("Extrayendo frames del video...")

        lifecycleScope.launch {
            try {
                // Extract frames in background
                val framePaths = withContext(Dispatchers.IO) {
                    val extractor = VideoFrameExtractor(this@CameraActivity)
                    extractor.extractFrames(videoPath)
                }

                if (framePaths.isEmpty()) {
                    showMessage("❌ No se pudieron extraer frames del video")
                    return@launch
                }

                Log.d(TAG, "✅ Extracted ${framePaths.size} frames, starting identification...")
                showMessage("Identificando con ${framePaths.size} frames...")

                // Use existing multi-image identification
                viewModel.processMultipleImages(
                    imagePaths = framePaths,
                    yearStart = yearFilterStart,
                    yearEnd = yearFilterEnd
                )

                // Cleanup video file
                File(videoPath).delete()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing video", e)
                showMessage("Error al procesar video: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset capture session when returning to camera
        // This ensures we start fresh each time
        resetCaptureSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        recording?.stop()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}