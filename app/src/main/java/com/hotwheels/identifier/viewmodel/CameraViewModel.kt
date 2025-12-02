package com.hotwheels.identifier.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheels.identifier.data.entities.IdentificationResult
import com.hotwheels.identifier.data.repository.HotWheelsRepository
import com.hotwheels.identifier.ml.MobileNetIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HotWheelsRepository
    private val mobileNetIdentifier: MobileNetIdentifier = MobileNetIdentifier.getInstance(getApplication())  // Singleton instance
    private val tag = "CameraViewModel"

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingStatus = MutableStateFlow("")
    val processingStatus: StateFlow<String> = _processingStatus.asStateFlow()

    private val _processingProgress = MutableStateFlow(0)
    val processingProgress: StateFlow<Int> = _processingProgress.asStateFlow()

    private val _identificationResult = MutableStateFlow<IdentificationResult?>(null)
    val identificationResult: StateFlow<IdentificationResult?> = _identificationResult.asStateFlow()

    private val _topMatches = MutableStateFlow<List<com.hotwheels.identifier.ml.IdentificationMatch>>(emptyList())
    val topMatches: StateFlow<List<com.hotwheels.identifier.ml.IdentificationMatch>> = _topMatches.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Expose MobileNetIdentifier loading state
    val mobileNetLoadingProgress: StateFlow<Int> = mobileNetIdentifier.loadingProgress
    val mobileNetLoadingStatus: StateFlow<String> = mobileNetIdentifier.loadingStatus
    val mobileNetIsReady: StateFlow<Boolean> = mobileNetIdentifier.isReady

    companion object {
        // Lista de IDs excluidos que persiste entre reinicios de la actividad
        private val excludedModelIds = mutableSetOf<String>()

        fun addExcludedModels(modelIds: List<String>) {
            excludedModelIds.addAll(modelIds)
            Log.d("CameraViewModel", "Excluded models: ${excludedModelIds.size} total")
        }

        fun clearExcludedModels() {
            excludedModelIds.clear()
            Log.d("CameraViewModel", "Cleared excluded models")
        }

        fun getExcludedModelIds(): Set<String> = excludedModelIds.toSet()
    }

    init {
        repository = HotWheelsRepository(application)
        Log.d(tag, "CameraViewModel initialized")
        Log.d(tag, "Using MobileNetV3 deep learning identification")

        // Initialize MobileNet asynchronously in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "Starting MobileNetV3 async initialization...")
                mobileNetIdentifier.initializeAsync()
                Log.d(tag, "✅ MobileNetV3 ready")
            } catch (e: Exception) {
                Log.e(tag, "❌ Failed to initialize MobileNetV3", e)
            }
        }
    }

    fun processMultipleImages(imagePaths: List<String>, yearStart: Int? = null, yearEnd: Int? = null) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _processingProgress.value = 0
                val yearMsg = if (yearStart != null && yearEnd != null) " filtrado por años $yearStart-$yearEnd" else ""
                _processingStatus.value = "Analizando ${imagePaths.size} fotos$yearMsg..."

                val startTime = System.currentTimeMillis()

                // Update progress: Starting
                _processingProgress.value = 10
                _processingStatus.value = "Preparando análisis... (10%)"

                // Perform identification with multiple images
                Log.d(tag, "🚀 Starting MULTI-IMAGE identification process (${imagePaths.size} photos, Top 3)...")
                Log.d(tag, "Excluding ${excludedModelIds.size} models from search")
                if (yearStart != null && yearEnd != null) {
                    Log.d(tag, "Year filter: $yearStart-$yearEnd")
                }

                // Update progress: Processing images
                _processingProgress.value = 30
                _processingStatus.value = "Extrayendo características... (30%)"

                val topResults = withContext(Dispatchers.Default) {
                    // Simulate progress updates during identification
                    launch {
                        kotlinx.coroutines.delay(500)
                        _processingProgress.value = 50
                        _processingStatus.value = "Comparando con base de datos... (50%)"
                        kotlinx.coroutines.delay(500)
                        _processingProgress.value = 70
                        _processingStatus.value = "Buscando mejores coincidencias... (70%)"
                    }

                    // Use MobileNetV3 deep learning identification
                    Log.d(tag, "🧠 Using MobileNetV3 deep learning identification")
                    mobileNetIdentifier.identifyTopMatchesMultiImage(
                        imagePaths = imagePaths,
                        topN = 3,
                        excludeModelIds = excludedModelIds.toSet(),
                        yearStart = yearStart,
                        yearEnd = yearEnd
                    )
                }

                // Update progress: Almost done
                _processingProgress.value = 90
                _processingStatus.value = "Finalizando... (90%)"

                val processingTime = System.currentTimeMillis() - startTime
                Log.d(tag, "⏱️ Multi-image processing took ${processingTime}ms")

                if (topResults.isNotEmpty()) {
                    Log.d(tag, "✅ Found ${topResults.size} potential matches from ${imagePaths.size} photos")
                    _processingProgress.value = 100
                    _processingStatus.value = "¡Encontrados ${topResults.size} resultados! (100%)"

                    // Save best result
                    val bestResult = topResults.first()
                    val identificationResult = IdentificationResult(
                        hotWheelModelId = bestResult.modelId,
                        imagePath = imagePaths.first(), // Use first photo as reference
                        confidence = bestResult.confidence,
                        identificationMethod = "MOBILENETV3_MULTI_IMAGE_${imagePaths.size}_PHOTOS",
                        processingTimeMs = processingTime
                    )

                    val resultId = repository.insertResult(identificationResult)
                    val savedResult = identificationResult.copy(id = resultId)

                    Log.d(tag, "📤 Emitting topMatches: ${topResults.size} matches")
                    _topMatches.value = topResults

                    Log.d(tag, "⏳ Waiting 50ms before emitting identificationResult...")
                    kotlinx.coroutines.delay(50)

                    Log.d(tag, "📤 Emitting identificationResult: ID=$resultId, ModelId=${savedResult.hotWheelModelId}")
                    _identificationResult.value = savedResult

                    Log.d(tag, "✅ Both values emitted successfully")
                } else {
                    Log.e(tag, "❌ No matches found from ${imagePaths.size} photos")
                    _processingStatus.value = "No Hot Wheels detected"
                    _topMatches.value = emptyList()

                    val identificationResult = IdentificationResult(
                        hotWheelModelId = "",
                        imagePath = imagePaths.first(),
                        confidence = 0f,
                        identificationMethod = "NO_MATCH_MULTI_IMAGE",
                        processingTimeMs = processingTime
                    )
                    repository.insertResult(identificationResult)
                    _identificationResult.value = identificationResult
                }

            } catch (e: Exception) {
                Log.e(tag, "❌ Error during multi-image processing", e)
                _errorMessage.value = "Error: ${e.message}"
                _topMatches.value = emptyList()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processImage(imagePath: String) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _processingStatus.value = "Analyzing image..."

                val startTime = System.currentTimeMillis()

                // Perform identification on background thread - GET TOP 3 MATCHES
                Log.d(tag, "🚀 Starting MobileNetV3 identification process (Top 3)...")
                Log.d(tag, "Excluding ${excludedModelIds.size} models from search")
                val topResults = withContext(Dispatchers.Default) {
                    mobileNetIdentifier.identifyTopMatches(imagePath, topN = 3, excludeModelIds = excludedModelIds.toSet())
                }

                val processingTime = System.currentTimeMillis() - startTime
                Log.d(tag, "⏱️ Processing took ${processingTime}ms")

                if (topResults.isNotEmpty()) {
                    Log.d(tag, "✅ Found ${topResults.size} potential matches")
                    _processingStatus.value = "Found ${topResults.size} potential matches!"

                    // Save best result to database FIRST
                    val bestResult = topResults.first()
                    val identificationResult = IdentificationResult(
                        hotWheelModelId = bestResult.modelId,
                        imagePath = imagePath,
                        confidence = bestResult.confidence,
                        identificationMethod = "MOBILENETV3_SINGLE_IMAGE",
                        processingTimeMs = processingTime
                    )

                    val resultId = repository.insertResult(identificationResult)
                    val savedResult = identificationResult.copy(id = resultId)

                    // IMPORTANT: Set topMatches BEFORE identificationResult
                    // This ensures UI has matches when result arrives
                    Log.d(tag, "📋 Setting ${topResults.size} top matches")
                    _topMatches.value = topResults

                    // Small delay to ensure topMatches propagates first
                    kotlinx.coroutines.delay(50)

                    Log.d(tag, "💾 Setting identification result with image: ${savedResult.imagePath}")
                    _identificationResult.value = savedResult
                } else {
                    Log.e(tag, "❌ No matches found")
                    _processingStatus.value = "No Hot Wheels detected"
                    _topMatches.value = emptyList()

                    // Still save the attempt with no match
                    val identificationResult = IdentificationResult(
                        hotWheelModelId = "",
                        imagePath = imagePath,
                        confidence = 0f,
                        identificationMethod = "NO_MATCH",
                        processingTimeMs = processingTime
                    )

                    val resultId = repository.insertResult(identificationResult)
                    val savedResult = identificationResult.copy(id = resultId)

                    _identificationResult.value = savedResult
                }

            } catch (e: Exception) {
                _errorMessage.value = "Failed to process image: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearIdentificationResult() {
        _identificationResult.value = null
        _topMatches.value = emptyList()
        Log.d(tag, "🧹 Cleared identification result and top matches")
    }

    override fun onCleared() {
        super.onCleared()
        // MobileNetIdentifier Singleton persists across ViewModels
        Log.d(tag, "CameraViewModel cleared")
    }
}