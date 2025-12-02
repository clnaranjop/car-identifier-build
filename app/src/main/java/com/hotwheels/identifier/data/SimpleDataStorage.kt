package com.hotwheels.identifier.data

import android.content.Context
import android.content.SharedPreferences
import com.hotwheels.identifier.data.entities.HotWheelModel
import com.hotwheels.identifier.data.entities.IdentificationResult
import com.hotwheels.identifier.data.entities.UserCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class SimpleDataStorage(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hotwheels_data", Context.MODE_PRIVATE)

    // In-memory storage with StateFlow for reactive updates
    private val _models = MutableStateFlow<List<HotWheelModel>>(emptyList())
    private val _results = MutableStateFlow<List<IdentificationResult>>(emptyList())
    private val _collections = MutableStateFlow<List<UserCollection>>(emptyList())

    val models: Flow<List<HotWheelModel>> = _models.asStateFlow()
    val results: Flow<List<IdentificationResult>> = _results.asStateFlow()
    val collections: Flow<List<UserCollection>> = _collections.asStateFlow()

    init {
        android.util.Log.e("SimpleDataStorage", "!!!!! CONSTRUCTOR LLAMADO !!!!!")
        android.util.Log.d("SimpleDataStorage", "🚀 ========== INICIANDO SimpleDataStorage ==========")

        // FORZAR carga desde assets - NUNCA usar SharedPreferences para modelos
        loadModelsFromAssets()

        android.util.Log.d("SimpleDataStorage", "📊 Modelos cargados: ${_models.value.size}")

        if (_models.value.isEmpty()) {
            android.util.Log.e("SimpleDataStorage", "❌ CRÍTICO: No se pudo cargar ningún modelo desde assets")
            android.util.Log.e("SimpleDataStorage", "❌ La identificación NO funcionará")
        } else {
            val firstModel = _models.value.first()
            val lastModel = _models.value.last()
            android.util.Log.d("SimpleDataStorage", "✅ Primer modelo: ${firstModel.name} (ID: ${firstModel.id}, Año: ${firstModel.year})")
            android.util.Log.d("SimpleDataStorage", "✅ Último modelo: ${lastModel.name} (ID: ${lastModel.id}, Año: ${lastModel.year})")
        }

        // Cargar results y collections desde SharedPreferences (no afecta identificación)
        loadResultsAndCollections()

        android.util.Log.d("SimpleDataStorage", "🏁 ========== SimpleDataStorage LISTO ==========")
    }

    private fun loadResultsAndCollections() {
        // Load results
        val resultsJson = prefs.getString("results", "[]")
        _results.value = parseResultsFromJson(resultsJson!!)

        // Load collections
        val collectionsJson = prefs.getString("collections", "[]")
        _collections.value = parseCollectionsFromJson(collectionsJson!!)
    }

    private fun loadData() {
        // Load models
        val modelsJson = prefs.getString("models", "[]")
        _models.value = parseModelsFromJson(modelsJson!!)

        // Load results
        val resultsJson = prefs.getString("results", "[]")
        _results.value = parseResultsFromJson(resultsJson!!)

        // Load collections
        val collectionsJson = prefs.getString("collections", "[]")
        _collections.value = parseCollectionsFromJson(collectionsJson!!)
    }

    private fun loadModelsFromAssets() {
        try {
            android.util.Log.d("SimpleDataStorage", "📂 Intentando abrir assets/hotwheels_models.json...")

            val inputStream = context.assets.open("hotwheels_models.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            android.util.Log.d("SimpleDataStorage", "📄 Archivo JSON leído: ${jsonString.length} caracteres")

            // El JSON exportado tiene formato: {"version": "...", "models": [...]}
            val rootObj = JSONObject(jsonString)
            val version = rootObj.optString("version", "unknown")
            val totalModels = rootObj.optInt("total_models", 0)

            android.util.Log.d("SimpleDataStorage", "📋 Metadata JSON - Version: $version, Total models: $totalModels")

            val modelsArray = rootObj.getJSONArray("models")
            android.util.Log.d("SimpleDataStorage", "📦 Array 'models' encontrado con ${modelsArray.length()} elementos")

            // Convertir a lista de HotWheelModel
            val models = mutableListOf<HotWheelModel>()
            for (i in 0 until modelsArray.length()) {
                try {
                    val obj = modelsArray.getJSONObject(i)
                    models.add(HotWheelModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        series = obj.optString("series", "Unknown"),
                        year = obj.getInt("year"),
                        category = obj.optString("category", "Unknown"),
                        manufacturer = obj.optString("manufacturer", "Mattel"),
                        rarity = obj.optString("rarity", "Common"),
                        keyFeatures = obj.optString("keyFeatures", null),
                        localImagePath = obj.optString("localImagePath", null)
                    ))

                    if (i % 500 == 0) {
                        android.util.Log.d("SimpleDataStorage", "   Procesando modelo $i/${modelsArray.length()}...")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SimpleDataStorage", "Error parseando modelo $i: ${e.message}")
                }
            }

            _models.value = models
            android.util.Log.d("SimpleDataStorage", "✅ Cargados ${models.size} modelos desde assets/hotwheels_models.json")

        } catch (e: java.io.FileNotFoundException) {
            android.util.Log.e("SimpleDataStorage", "❌ Archivo NO ENCONTRADO: assets/hotwheels_models.json")
            android.util.Log.e("SimpleDataStorage", "❌ Verifica que el archivo esté en app/src/main/assets/")
        } catch (e: Exception) {
            android.util.Log.e("SimpleDataStorage", "❌ Error cargando modelos desde assets: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun saveData() {
        val editor = prefs.edit()

        // Save models
        editor.putString("models", modelsToJson(_models.value))

        // Save results
        editor.putString("results", resultsToJson(_results.value))

        // Save collections
        editor.putString("collections", collectionsToJson(_collections.value))

        editor.apply()
    }

    // Model operations
    suspend fun insertModel(model: HotWheelModel): Long {
        val currentModels = _models.value.toMutableList()
        currentModels.add(model)
        _models.value = currentModels
        saveData()
        return model.id.hashCode().toLong()
    }

    suspend fun getAllModels(): List<HotWheelModel> = _models.value

    suspend fun getModelsCount(): Int = _models.value.size

    // Result operations
    suspend fun insertResult(result: IdentificationResult): Long {
        val newResult = result.copy(
            id = System.currentTimeMillis(), // Simple ID generation
            dateIdentified = System.currentTimeMillis()
        )
        val currentResults = _results.value.toMutableList()
        currentResults.add(newResult)
        _results.value = currentResults
        saveData()
        return newResult.id
    }

    suspend fun getResultsCount(): Int = _results.value.size

    // Collection operations
    suspend fun getUniqueModelsCount(): Int {
        return _collections.value.map { it.hotWheelModelId }.distinct().size
    }

    suspend fun insertCollection(collection: UserCollection): Long {
        val currentCollections = _collections.value.toMutableList()
        currentCollections.add(collection)
        _collections.value = currentCollections
        saveData()
        return collection.id
    }

    // JSON conversion methods
    private fun parseModelsFromJson(json: String): List<HotWheelModel> {
        return try {
            val jsonArray = JSONArray(json)
            val models = mutableListOf<HotWheelModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                models.add(HotWheelModel(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    series = obj.getString("series"),
                    year = obj.getInt("year"),
                    category = obj.getString("category"),
                    manufacturer = obj.optString("manufacturer", "Mattel"),
                    rarity = obj.getString("rarity"),
                    keyFeatures = obj.optString("keyFeatures", null)
                ))
            }
            models
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun modelsToJson(models: List<HotWheelModel>): String {
        val jsonArray = JSONArray()
        models.forEach { model ->
            val obj = JSONObject().apply {
                put("id", model.id)
                put("name", model.name)
                put("series", model.series)
                put("year", model.year)
                put("category", model.category)
                put("manufacturer", model.manufacturer)
                put("rarity", model.rarity)
                put("keyFeatures", model.keyFeatures ?: "")
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun parseResultsFromJson(json: String): List<IdentificationResult> {
        return try {
            val jsonArray = JSONArray(json)
            val results = mutableListOf<IdentificationResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(IdentificationResult(
                    id = obj.getLong("id"),
                    hotWheelModelId = obj.getString("hotWheelModelId"),
                    imagePath = obj.getString("imagePath"),
                    confidence = obj.getDouble("confidence").toFloat(),
                    identificationMethod = obj.getString("identificationMethod"),
                    processingTimeMs = obj.getLong("processingTimeMs"),
                    dateIdentified = obj.getLong("dateIdentified"),
                    userConfirmed = obj.optBoolean("userConfirmed", false),
                    notes = obj.optString("notes", null)
                ))
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun resultsToJson(results: List<IdentificationResult>): String {
        val jsonArray = JSONArray()
        results.forEach { result ->
            val obj = JSONObject().apply {
                put("id", result.id)
                put("hotWheelModelId", result.hotWheelModelId)
                put("imagePath", result.imagePath)
                put("confidence", result.confidence)
                put("identificationMethod", result.identificationMethod)
                put("processingTimeMs", result.processingTimeMs)
                put("dateIdentified", result.dateIdentified)
                put("userConfirmed", result.userConfirmed)
                put("notes", result.notes ?: "")
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun parseCollectionsFromJson(json: String): List<UserCollection> {
        return try {
            val jsonArray = JSONArray(json)
            val collections = mutableListOf<UserCollection>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                collections.add(UserCollection(
                    id = obj.getLong("id"),
                    hotWheelModelId = obj.getString("hotWheelModelId"),
                    quantity = obj.optInt("quantity", 1),
                    condition = obj.optString("condition", "Mint"),
                    acquiredDate = obj.getLong("acquiredDate"),
                    acquiredPrice = if (obj.has("acquiredPrice") && !obj.isNull("acquiredPrice")) obj.getDouble("acquiredPrice").toFloat() else null,
                    location = obj.optString("location", null),
                    notes = obj.optString("notes", null),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    imagesPaths = obj.optString("imagesPaths", null)
                ))
            }
            collections
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun collectionsToJson(collections: List<UserCollection>): String {
        val jsonArray = JSONArray()
        collections.forEach { collection ->
            val obj = JSONObject().apply {
                put("id", collection.id)
                put("hotWheelModelId", collection.hotWheelModelId)
                put("quantity", collection.quantity)
                put("condition", collection.condition)
                put("acquiredDate", collection.acquiredDate)
                put("acquiredPrice", collection.acquiredPrice)
                put("location", collection.location ?: "")
                put("notes", collection.notes ?: "")
                put("isFavorite", collection.isFavorite)
                put("imagesPaths", collection.imagesPaths ?: "")
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun initializeSampleData() {
        val sampleModels = listOf(
            HotWheelModel(
                id = "hw_lamborghini_aventador_2023",
                name = "Lamborghini Aventador",
                series = "HW Exotics",
                year = 2023,
                category = "Sports Car",
                manufacturer = "Mattel",
                rarity = "Common",
                keyFeatures = """{"shape": "angular", "doors": "scissor", "wheels": 4, "aspectRatio": 2.8}"""
            ),
            HotWheelModel(
                id = "hw_ford_mustang_gt_2022",
                name = "Ford Mustang GT",
                series = "HW Muscle Mania",
                year = 2022,
                category = "Muscle Car",
                manufacturer = "Mattel",
                rarity = "Common",
                keyFeatures = """{"shape": "aggressive", "doors": "coupe", "wheels": 4, "aspectRatio": 2.4}"""
            ),
            HotWheelModel(
                id = "hw_ferrari_488_gtb_2023",
                name = "Ferrari 488 GTB",
                series = "HW Exotics",
                year = 2023,
                category = "Sports Car",
                manufacturer = "Mattel",
                rarity = "Uncommon",
                keyFeatures = """{"shape": "sleek", "doors": "coupe", "wheels": 4, "aspectRatio": 2.6}"""
            ),
            HotWheelModel(
                id = "hw_chevrolet_corvette_c8_2022",
                name = "Chevrolet Corvette C8",
                series = "HW Muscle Mania",
                year = 2022,
                category = "Sports Car",
                manufacturer = "Mattel",
                rarity = "Uncommon",
                keyFeatures = """{"shape": "angular", "doors": "coupe", "wheels": 4, "aspectRatio": 2.5}"""
            ),
            HotWheelModel(
                id = "hw_dodge_challenger_srt_2021",
                name = "Dodge Challenger SRT",
                series = "HW Muscle Mania",
                year = 2021,
                category = "Muscle Car",
                manufacturer = "Mattel",
                rarity = "Common",
                keyFeatures = """{"shape": "boxy", "doors": "coupe", "wheels": 4, "aspectRatio": 2.2}"""
            )
        )

        _models.value = sampleModels
        saveData()
    }
}