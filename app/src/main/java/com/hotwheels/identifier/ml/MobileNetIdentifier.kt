package com.hotwheels.identifier.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import ai.onnxruntime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.PriorityQueue
import kotlin.math.sqrt

/**
 * Hot Wheels identifier using MobileNetV3 embeddings and ONNX Runtime
 * Singleton pattern to avoid reloading embeddings and prevent ANR
 */
class MobileNetIdentifier private constructor(private val context: Context) {

    private val tag = "MobileNetIdentifier"

    // ONNX Runtime session
    private var ortSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null

    // Embeddings database
    private val embeddings = mutableListOf<EmbeddingEntry>()
    private var embeddingDim = 960

    // Loading state
    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _loadingStatus = MutableStateFlow("Initializing...")
    val loadingStatus: StateFlow<String> = _loadingStatus.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    data class EmbeddingEntry(
        val id: String,
        val name: String,
        val year: Int,
        val embedding: FloatArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EmbeddingEntry
            return id == other.id
        }

        override fun hashCode(): Int = id.hashCode()
    }

    companion object {
        @Volatile
        private var INSTANCE: MobileNetIdentifier? = null

        fun getInstance(context: Context): MobileNetIdentifier {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MobileNetIdentifier(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val MODEL_FILENAME = "mobilenetv3_embeddings.onnx"
        private const val EMBEDDINGS_FILENAME = "embeddings_mobilenetv3.json"
        private const val IMAGE_SIZE = 224

        // ImageNet normalization
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        // Matching thresholds
        private const val MIN_SIMILARITY = 0.20f  // Minimum cosine similarity (20%) - lowered for testing
        private const val TOP_K = 100  // Number of top results to return
    }

    init {
        Log.d(tag, "🚀 MobileNetIdentifier created (Singleton)")
        Log.d(tag, "⚠️ Call initializeAsync() to load embeddings in background")
    }

    /**
     * Initialize the identifier asynchronously to avoid blocking the UI thread
     * Must be called before using any identification methods
     */
    suspend fun initializeAsync() {
        if (_isReady.value) {
            Log.d(tag, "✅ Already initialized, skipping")
            return
        }

        Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(tag, "🚀 MobileNetIdentifier ASYNC INIT START")
        Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        try {
            _loadingProgress.value = 5
            _loadingStatus.value = "Inicializando ONNX Runtime..."
            Log.d(tag, "Step 1: Initialize ONNX Runtime...")
            ortEnvironment = OrtEnvironment.getEnvironment()
            Log.d(tag, "✅ ONNX Runtime initialized")

            _loadingProgress.value = 10
            _loadingStatus.value = "Cargando modelo MobileNetV3..."
            Log.d(tag, "Step 2: Load ONNX model...")
            loadOnnxModel()
            Log.d(tag, "✅ ONNX model loaded")

            _loadingProgress.value = 15
            _loadingStatus.value = "Cargando base de datos (11,257 modelos)..."
            Log.d(tag, "Step 3: Load embeddings database...")
            loadEmbeddings()
            Log.d(tag, "✅ Embeddings loaded")

            _loadingProgress.value = 100
            _loadingStatus.value = "¡Listo!"
            _isReady.value = true

            Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(tag, "✅ MobileNetIdentifier INIT COMPLETE")
            Log.d(tag, "   Embeddings count: ${embeddings.size}")
            Log.d(tag, "   Embedding dimension: $embeddingDim")
            Log.d(tag, "   ONNX Session: ${if (ortSession != null) "Ready" else "NULL"}")
            Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            _loadingStatus.value = "Error: ${e.message}"
            _isReady.value = false
            Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(tag, "❌❌❌ FATAL ERROR IN ASYNC INIT ❌❌❌")
            Log.e(tag, "Exception: ${e.javaClass.simpleName}")
            Log.e(tag, "Message: ${e.message}")
            Log.e(tag, "Stack trace:")
            e.printStackTrace()
            Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            throw e
        }
    }

    private fun loadOnnxModel() {
        try {
            Log.d(tag, "📦 Loading ONNX model: $MODEL_FILENAME")

            val modelBytes = context.assets.open(MODEL_FILENAME).use { it.readBytes() }

            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setInterOpNumThreads(4)
            sessionOptions.setIntraOpNumThreads(4)

            ortSession = ortEnvironment!!.createSession(modelBytes, sessionOptions)

            Log.d(tag, "✅ ONNX model loaded successfully")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error loading ONNX model", e)
            throw e
        }
    }

    private fun loadEmbeddings() {
        try {
            Log.d(tag, "📂 Loading embeddings using streaming JSON parser: $EMBEDDINGS_FILENAME")

            // Copy asset to internal storage to avoid compression issues
            val embeddingsFile = File(context.filesDir, "embeddings_cache.json")
            if (!embeddingsFile.exists()) {
                Log.d(tag, "📋 Copying embeddings to internal storage...")
                context.assets.open(EMBEDDINGS_FILENAME).use { input ->
                    embeddingsFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(tag, "✅ Embeddings copied to: ${embeddingsFile.absolutePath}")
            }

            // Now read from internal storage (no compression)
            val reader = android.util.JsonReader(java.io.InputStreamReader(java.io.FileInputStream(embeddingsFile), "UTF-8"))
            reader.isLenient = true  // Allow malformed JSON (BOM, extra whitespace, etc.)

            reader.beginObject()

            var version = ""
            var model = ""
            var totalEmbeddings = 0

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "version" -> version = reader.nextString()
                    "model" -> model = reader.nextString()
                    "embedding_dim" -> embeddingDim = reader.nextInt()
                    "total_embeddings" -> totalEmbeddings = reader.nextInt()
                    "embeddings" -> {
                        // Read embeddings array
                        reader.beginArray()
                        var count = 0
                        while (reader.hasNext()) {
                            reader.beginObject()

                            var id = ""
                            var name = ""
                            var year = 0
                            val embeddingList = mutableListOf<Float>()

                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "id" -> id = reader.nextString()
                                    "name" -> name = reader.nextString()
                                    "year" -> year = reader.nextInt()
                                    "embedding" -> {
                                        reader.beginArray()
                                        while (reader.hasNext()) {
                                            embeddingList.add(reader.nextDouble().toFloat())
                                        }
                                        reader.endArray()
                                    }
                                    else -> reader.skipValue()
                                }
                            }

                            reader.endObject()

                            embeddings.add(EmbeddingEntry(id, name, year, embeddingList.toFloatArray()))
                            count++

                            if (count % 1000 == 0) {
                                val progress = 15 + ((count.toFloat() / totalEmbeddings) * 85).toInt()
                                _loadingProgress.value = progress
                                _loadingStatus.value = "Cargando modelos: $count/$totalEmbeddings"
                                Log.d(tag, "   Loaded: $count/$totalEmbeddings embeddings...")
                            }
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }

            reader.endObject()
            reader.close()

            Log.d(tag, "✅ Embeddings loaded successfully: ${embeddings.size} entries")
            Log.d(tag, "   Version: $version")
            Log.d(tag, "   Model: $model")
            Log.d(tag, "   Embedding dimension: $embeddingDim")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error loading embeddings", e)
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Generate embedding for a captured image
     */
    private fun generateEmbedding(imagePath: String): FloatArray {
        try {
            // Load image with correct EXIF orientation
            val bitmap = loadBitmapWithCorrectOrientation(imagePath)
                ?: throw IllegalArgumentException("Failed to load image: $imagePath")
            val preprocessed = preprocessImage(bitmap)

            // Run inference
            val inputName = ortSession!!.inputNames.iterator().next()

            // Convert ByteBuffer to FloatArray for ONNX Runtime
            preprocessed.rewind()
            val floatBuffer = preprocessed.asFloatBuffer()
            val inputArray = FloatArray(floatBuffer.remaining())
            floatBuffer.get(inputArray)

            val inputTensor = OnnxTensor.createTensor(
                ortEnvironment,
                FloatBuffer.wrap(inputArray),
                longArrayOf(1, 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong())
            )

            val output = ortSession!!.run(mapOf(inputName to inputTensor))

            // Get embedding
            val outputTensor = output[0].value as Array<FloatArray>
            val embedding = outputTensor[0]

            // Normalize (L2 norm)
            normalizeL2(embedding)

            output.close()
            inputTensor.close()

            return embedding
        } catch (e: Exception) {
            Log.e(tag, "❌ Error generating embedding", e)
            throw e
        }
    }

    /**
     * Preprocess image for MobileNetV3
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize to 224x224
        val resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)

        // Convert to float buffer (NCHW format: batch, channels, height, width)
        val buffer = ByteBuffer.allocateDirect(1 * 3 * IMAGE_SIZE * IMAGE_SIZE * 4)
        buffer.order(ByteOrder.nativeOrder())
        val floatBuffer = buffer.asFloatBuffer()

        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        resized.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        // Normalize using ImageNet stats (CHW format)
        for (c in 0 until 3) {
            for (h in 0 until IMAGE_SIZE) {
                for (w in 0 until IMAGE_SIZE) {
                    val pixel = pixels[h * IMAGE_SIZE + w]
                    val channelValue = when (c) {
                        0 -> ((pixel shr 16) and 0xFF) / 255.0f  // R
                        1 -> ((pixel shr 8) and 0xFF) / 255.0f   // G
                        else -> (pixel and 0xFF) / 255.0f        // B
                    }
                    val normalized = (channelValue - MEAN[c]) / STD[c]
                    floatBuffer.put(normalized)
                }
            }
        }

        buffer.rewind()
        return buffer
    }

    /**
     * Load bitmap with correct EXIF orientation applied
     */
    private fun loadBitmapWithCorrectOrientation(imagePath: String): Bitmap? {
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
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            return rotated
        } catch (e: Exception) {
            Log.e(tag, "Error reading EXIF orientation, using bitmap as-is", e)
            return bitmap
        }
    }

    /**
     * L2 normalization for cosine similarity
     */
    private fun normalizeL2(vector: FloatArray) {
        var norm = 0f
        for (v in vector) {
            norm += v * v
        }
        norm = sqrt(norm)

        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
    }

    /**
     * Compute cosine similarity between two normalized vectors
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot
    }

    /**
     * Find top K most similar embeddings
     */
    suspend fun identifyTopMatches(
        imagePath: String,
        topN: Int = 100,
        excludeModelIds: Set<String> = emptySet(),
        yearStart: Int? = null,
        yearEnd: Int? = null
    ): List<IdentificationMatch> {
        try {
            Log.d(tag, "═══════════════════════════════════════════════════════")
            Log.d(tag, "🔍 MOBILENETV3 IDENTIFICATION START")
            Log.d(tag, "   Image: $imagePath")
            Log.d(tag, "   Searching for top $topN matches")
            if (yearStart != null && yearEnd != null) {
                Log.d(tag, "   Year filter: $yearStart-$yearEnd")
            }
            Log.d(tag, "═══════════════════════════════════════════════════════")

            // Generate embedding for captured image
            val queryEmbedding = generateEmbedding(imagePath)
            Log.d(tag, "✅ Generated query embedding (${queryEmbedding.size}D)")

            // Filter embeddings by year if specified
            val filteredEmbeddings = if (yearStart != null && yearEnd != null) {
                embeddings.filter { it.year in yearStart..yearEnd && it.id !in excludeModelIds }
            } else {
                embeddings.filter { it.id !in excludeModelIds }
            }

            Log.d(tag, "📊 Searching ${filteredEmbeddings.size} embeddings...")

            // Find top K matches using priority queue
            val topMatches = PriorityQueue<Pair<Float, EmbeddingEntry>>(
                compareBy { it.first }
            )

            filteredEmbeddings.forEachIndexed { index, entry ->
                val similarity = cosineSimilarity(queryEmbedding, entry.embedding)

                if (similarity >= MIN_SIMILARITY) {
                    topMatches.add(Pair(similarity, entry))

                    if (topMatches.size > topN) {
                        topMatches.poll()
                    }
                }

                if ((index + 1) % 1000 == 0) {
                    Log.d(tag, "   Processed: ${index + 1}/${filteredEmbeddings.size}")
                }
            }

            // Convert to IdentificationMatch and sort by similarity (descending)
            val results = topMatches.map { (similarity, entry) ->
                IdentificationMatch(
                    modelId = entry.id,
                    confidence = similarity,
                    method = "MobileNetV3",
                    details = "${entry.name} (${entry.year})"
                )
            }.sortedByDescending { it.confidence }

            Log.d(tag, "═══════════════════════════════════════════════════════")
            Log.d(tag, "✅ IDENTIFICATION COMPLETE")
            Log.d(tag, "   Found ${results.size} matches above threshold (${(MIN_SIMILARITY * 100).toInt()}%)")
            if (results.isNotEmpty()) {
                Log.d(tag, "   🏆 Best match: ${results.first().details}")
                Log.d(tag, "   Similarity: ${(results.first().confidence * 100).toInt()}%")
            } else {
                // Show top 5 matches for debugging even if below threshold
                Log.d(tag, "   ℹ️ No matches above ${(MIN_SIMILARITY * 100).toInt()}% threshold")
                Log.d(tag, "   📊 Top 5 similarities (for debugging):")
                topMatches.sortedByDescending { it.first }.take(5).forEachIndexed { idx, (sim, entry) ->
                    Log.d(tag, "      ${idx + 1}. ${entry.name} (${entry.year}) - ${(sim * 100).toInt()}%")
                }
            }
            Log.d(tag, "═══════════════════════════════════════════════════════")

            return results
        } catch (e: Exception) {
            Log.e(tag, "❌ Error during identification", e)
            return emptyList()
        }
    }

    /**
     * Multi-image identification (combines results from multiple photos)
     */
    suspend fun identifyTopMatchesMultiImage(
        imagePaths: List<String>,
        topN: Int = 3,
        excludeModelIds: Set<String> = emptySet(),
        yearStart: Int? = null,
        yearEnd: Int? = null
    ): List<IdentificationMatch> {
        Log.d(tag, "═══════════════════════════════════════════════════════")
        Log.d(tag, "🚀 MULTI-IMAGE IDENTIFICATION START")
        Log.d(tag, "   Processing ${imagePaths.size} photos")
        Log.d(tag, "═══════════════════════════════════════════════════════")

        // Collect all matches from all images
        val allMatchesByModel = mutableMapOf<String, MutableList<Float>>()

        imagePaths.forEachIndexed { index, imagePath ->
            Log.d(tag, "📸 Processing photo ${index + 1}/${imagePaths.size}")

            val matches = identifyTopMatches(
                imagePath = imagePath,
                topN = 50,  // Get more matches per image
                excludeModelIds = excludeModelIds,
                yearStart = yearStart,
                yearEnd = yearEnd
            )

            matches.forEach { match ->
                allMatchesByModel.getOrPut(match.modelId) { mutableListOf() }.add(match.confidence)
            }
        }

        // Aggregate scores (use maximum similarity across all images)
        val aggregated = allMatchesByModel.map { (modelId, similarities) ->
            val entry = embeddings.first { it.id == modelId }
            val maxSimilarity = similarities.maxOrNull() ?: 0f
            val numAppearances = similarities.size
            IdentificationMatch(
                modelId = modelId,
                confidence = maxSimilarity,
                method = "MobileNetV3",
                details = "${entry.name} (${entry.year}) - Appeared in $numAppearances/${imagePaths.size} photos"
            )
        }.sortedByDescending { it.confidence }.take(topN)

        Log.d(tag, "═══════════════════════════════════════════════════════")
        Log.d(tag, "✅ MULTI-IMAGE IDENTIFICATION COMPLETE")
        if (aggregated.isNotEmpty()) {
            Log.d(tag, "   🏆 Best match: ${aggregated.first().details}")
            Log.d(tag, "   Similarity: ${(aggregated.first().confidence * 100).toInt()}%")
        }
        Log.d(tag, "═══════════════════════════════════════════════════════")

        return aggregated
    }

    fun isReady(): Boolean {
        return ortSession != null && embeddings.isNotEmpty()
    }

    fun cleanup() {
        try {
            ortSession?.close()
            ortEnvironment?.close()
            embeddings.clear()
            Log.d(tag, "✅ MobileNetIdentifier cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error during cleanup", e)
        }
    }
}
