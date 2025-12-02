package com.hotwheels.identifier.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheels.identifier.data.repository.HotWheelsRepository
import com.hotwheels.identifier.data.entities.HotWheelModel
import com.hotwheels.identifier.data.HotWheelsDataDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HotWheelsRepository
    private val dataDownloader: HotWheelsDataDownloader
    private val tag = "MainViewModel"

    private val _totalScanned = MutableStateFlow(0)
    val totalScanned: StateFlow<Int> = _totalScanned.asStateFlow()

    private val _collectionSize = MutableStateFlow(0)
    val collectionSize: StateFlow<Int> = _collectionSize.asStateFlow()

    private val _databaseSize = MutableStateFlow(0)
    val databaseSize: StateFlow<Int> = _databaseSize.asStateFlow()

    private val _downloadProgress = MutableStateFlow("")
    val downloadProgress: StateFlow<String> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    init {
        try {
            Log.d(tag, "Initializing MainViewModel")
            repository = HotWheelsRepository(application)
            dataDownloader = HotWheelsDataDownloader(application)
            Log.d(tag, "Repository and downloader initialized successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error initializing repository", e)
            throw e
        }
    }

    fun getAllModels() = repository.getAllModels()

    fun loadStats() {
        viewModelScope.launch {
            try {
                Log.d(tag, "Loading stats...")

                // Load total scanned count
                val scannedCount = repository.getResultsCount()
                _totalScanned.value = scannedCount
                Log.d(tag, "Scanned count: $scannedCount")

                // Load collection size (unique models)
                val collectionCount = repository.getUniqueModelsCount()
                _collectionSize.value = collectionCount
                Log.d(tag, "Collection count: $collectionCount")

                // Load database size (total models available)
                val databaseCount = repository.getModelsCount()
                _databaseSize.value = databaseCount
                Log.d(tag, "Database count: $databaseCount")

                // If database is empty or has few models, start progressive download
                if (databaseCount == 0) {
                    Log.d(tag, "📦 Base de datos vacía, iniciando descarga masiva...")
                    startProgressiveDownload()
                } else if (databaseCount < 20) {
                    Log.d(tag, "📊 Base de datos pequeña ($databaseCount modelos), ampliando...")
                    startProgressiveDownload()
                } else {
                    Log.d(tag, "✅ Base de datos robusta: $databaseCount modelos")
                    val (downloaded, total) = dataDownloader.getDownloadProgress()
                    _downloadProgress.value = "Base completa: $downloaded/$total años"
                }

                Log.d(tag, "Stats loaded successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error loading stats", e)
                // Set default values on error
                _totalScanned.value = 0
                _collectionSize.value = 0

                // Asegurar que al menos tengamos datos de muestra
                try {
                    val currentCount = repository.getModelsCount()
                    if (currentCount == 0) {
                        Log.d(tag, "Error en stats pero sin datos, cargando muestras")
                        initializeSampleData()
                        _databaseSize.value = repository.getModelsCount()
                    } else {
                        _databaseSize.value = currentCount
                    }
                } catch (ex: Exception) {
                    Log.e(tag, "Error incluso cargando datos de muestra", ex)
                    _databaseSize.value = 0
                }
            }
        }
    }

    private suspend fun initializeSampleData() {
        try {
            Log.d(tag, "Initializing sample data")

            // Add more sample Hot Wheels models
            val sampleModels = listOf(
                HotWheelModel(
                    id = "sample_1",
                    name = "Twin Mill",
                    series = "HW Originals",
                    year = 2023,
                    category = "Fantasy",
                    manufacturer = "Mattel",
                    description = "Classic Hot Wheels original design with twin engines"
                ),
                HotWheelModel(
                    id = "sample_2",
                    name = "Bone Shaker",
                    series = "HW Originals",
                    year = 2023,
                    category = "Fantasy",
                    manufacturer = "Mattel",
                    description = "Hot rod with exposed engine and skull motif"
                ),
                HotWheelModel(
                    id = "sample_3",
                    name = "'16 Camaro SS",
                    series = "HW Speed Graphics",
                    year = 2023,
                    category = "Sports Car",
                    manufacturer = "Mattel",
                    description = "2016 Chevrolet Camaro SS muscle car"
                ),
                HotWheelModel(
                    id = "sample_4",
                    name = "Lamborghini Huracán LP 610-4",
                    series = "Exotics",
                    year = 2023,
                    category = "Supercar",
                    manufacturer = "Mattel",
                    description = "Italian supercar with V10 engine"
                ),
                HotWheelModel(
                    id = "sample_5",
                    name = "Tesla Model S",
                    series = "Green Speed",
                    year = 2023,
                    category = "Electric",
                    manufacturer = "Mattel",
                    description = "Electric luxury sedan"
                ),
                HotWheelModel(
                    id = "sample_6",
                    name = "Fast Fish",
                    series = "HW Originals",
                    year = 2023,
                    category = "Fantasy",
                    manufacturer = "Mattel",
                    description = "Fish-shaped fantasy car design"
                ),
                HotWheelModel(
                    id = "sample_7",
                    name = "Ford GT",
                    series = "HW Exotics",
                    year = 2023,
                    category = "Supercar",
                    manufacturer = "Mattel",
                    description = "American supercar with racing heritage"
                ),
                HotWheelModel(
                    id = "sample_8",
                    name = "Volkswagen Beetle",
                    series = "Volkswagen",
                    year = 2023,
                    category = "Classic",
                    manufacturer = "Mattel",
                    description = "Iconic German compact car"
                )
            )

            repository.insertModels(sampleModels)
            _databaseSize.value = sampleModels.size
            Log.d(tag, "Sample data initialized: ${sampleModels.size} models")
        } catch (e: Exception) {
            Log.e(tag, "Error initializing sample data", e)
        }
    }

    private suspend fun clearAndInitializeSampleData() {
        try {
            Log.d(tag, "Clearing old data and initializing new sample data")

            // Clear existing models first
            val existingModels = repository.getAllModels().first()
            for (model in existingModels) {
                repository.deleteModel(model)
            }
            Log.d(tag, "Cleared ${existingModels.size} existing models")

            // Initialize new data
            initializeSampleData()
        } catch (e: Exception) {
            Log.e(tag, "Error clearing and initializing sample data", e)
        }
    }

    /**
     * Inicia descarga progresiva en segundo plano
     * No bloquea la UI y actualiza estadísticas en tiempo real
     */
    private fun startProgressiveDownload() {
        viewModelScope.launch {
            try {
                _isDownloading.value = true
                _downloadProgress.value = "Iniciando descarga masiva..."

                val success = dataDownloader.downloadProgressively { year, current, total, modelsCount ->
                    // Actualizar progreso en tiempo real
                    _downloadProgress.value = "Descargando año $year ($current/$total) - $modelsCount modelos"

                    // Actualizar estadísticas inmediatamente
                    viewModelScope.launch {
                        val currentDbSize = repository.getModelsCount()
                        _databaseSize.value = currentDbSize
                        Log.d(tag, "📊 DB actualizada: $currentDbSize modelos")
                    }
                }

                if (success) {
                    val finalCount = repository.getModelsCount()
                    _databaseSize.value = finalCount
                    _downloadProgress.value = "✅ Descarga completa: $finalCount modelos Hot Wheels"
                    Log.d(tag, "🎉 Descarga masiva exitosa: $finalCount modelos")
                } else {
                    Log.w(tag, "Descarga progresiva falló, usando datos de muestra")
                    _downloadProgress.value = "⚠️ Descarga falló, cargando datos básicos..."

                    // Si la descarga falló, asegurar datos de muestra
                    val currentCount = repository.getModelsCount()
                    if (currentCount == 0) {
                        Log.d(tag, "Agregando datos de muestra como respaldo")
                        initializeSampleData()
                        val sampleCount = repository.getModelsCount()
                        _databaseSize.value = sampleCount
                        _downloadProgress.value = "📦 Datos básicos cargados: $sampleCount modelos"
                    } else {
                        _downloadProgress.value = "⚠️ Error en descarga, usando datos existentes ($currentCount modelos)"
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Error en descarga progresiva", e)
                _downloadProgress.value = "❌ Error de descarga, cargando datos básicos..."

                // Si hay excepción, asegurar datos de muestra
                try {
                    val currentCount = repository.getModelsCount()
                    if (currentCount == 0) {
                        Log.d(tag, "Excepción en descarga y sin datos, cargando muestras")
                        initializeSampleData()
                        val sampleCount = repository.getModelsCount()
                        _databaseSize.value = sampleCount
                        _downloadProgress.value = "📦 Datos de emergencia cargados: $sampleCount modelos"
                    } else {
                        _downloadProgress.value = "⚠️ Error de descarga, usando datos existentes ($currentCount modelos)"
                    }
                } catch (ex: Exception) {
                    Log.e(tag, "Error crítico cargando datos de respaldo", ex)
                    _downloadProgress.value = "❌ Error crítico - sin datos disponibles"
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    /**
     * Descarga años específicos (útil para actualizaciones)
     */
    fun downloadSpecificYears(years: List<Int>) {
        viewModelScope.launch {
            try {
                _isDownloading.value = true
                _downloadProgress.value = "Descargando años específicos: ${years.joinToString(", ")}"

                val success = dataDownloader.downloadSpecificYears(years)

                if (success) {
                    // Recargar estadísticas
                    loadStats()
                    _downloadProgress.value = "✅ Años actualizados: ${years.joinToString(", ")}"
                } else {
                    _downloadProgress.value = "❌ Error actualizando años específicos"
                }

            } catch (e: Exception) {
                Log.e(tag, "Error descargando años específicos", e)
                _downloadProgress.value = "❌ Error en actualización"
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun refreshDatabase() {
        viewModelScope.launch {
            try {
                Log.d(tag, "Force refreshing database from API...")
                val success = dataDownloader.forceRefreshDatabase()

                if (success) {
                    loadStats() // Recargar estadísticas
                    Log.d(tag, "Database refreshed successfully")
                } else {
                    Log.w(tag, "Database refresh failed")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error refreshing database", e)
            }
        }
    }

    /**
     * Fixes corrupted model names in the database
     * Returns the number of models that were fixed
     */
    suspend fun fixCorruptedModels(): Int {
        return try {
            Log.d(tag, "🔧 Starting corrupted model fix...")
            val fixedCount = repository.fixCorruptedModelNames()
            Log.d(tag, "✅ Fixed $fixedCount corrupted models")
            fixedCount
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fixing corrupted models", e)
            throw e
        }
    }
}