# 🏗️ Arquitectura del Proyecto - HotWheels Identifier

## 📋 Índice

- [Visión General](#visión-general)
- [Arquitectura MVVM](#arquitectura-mvvm)
- [Componentes Principales](#componentes-principales)
- [Flujo de Datos](#flujo-de-datos)
- [Machine Learning](#machine-learning)
- [Almacenamiento](#almacenamiento)
- [Decisiones de Diseño](#decisiones-de-diseño)

---

## 🎯 Visión General

**HotWheels Identifier** es una aplicación Android para identificar modelos de Hot Wheels usando visión computacional y machine learning.

### Tecnologías Principales

- **Lenguaje:** Kotlin 2.0.21
- **Compilación:** Gradle 8.9
- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 35 (Android 15)
- **Arquitectura:** MVVM (Model-View-ViewModel)

### Librerías Clave

```kotlin
// ML y Visión Computacional
implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.3'
implementation 'com.quickbirdstudios:opencv:4.5.3.0'

// Cámara
implementation 'androidx.camera:camera-core:1.4.0'
implementation 'androidx.camera:camera-camera2:1.4.0'
implementation 'androidx.camera:camera-lifecycle:1.4.0'
implementation 'androidx.camera:camera-view:1.4.0'

// ViewModel y LiveData
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.8.7'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// JSON
implementation 'com.google.code.gson:gson:2.10.1'

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.16.0'

// AdMob
implementation 'com.google.android.gms:play-services-ads:22.6.0'
```

---

## 🏛️ Arquitectura MVVM

```
┌─────────────────────────────────────────────────┐
│                    VIEW LAYER                   │
│  (Activities, Fragments, XML Layouts)           │
│                                                 │
│  - MainActivity                                 │
│  - CameraActivity                               │
│  - ResultActivity                               │
│  - CollectionActivity                           │
│  - SettingsActivity                             │
│  - ModelDetailsActivity                         │
│  - SelectResultActivity                         │
└──────────────────┬──────────────────────────────┘
                   │
                   ↓ (observes LiveData/StateFlow)
┌─────────────────────────────────────────────────┐
│                 VIEWMODEL LAYER                 │
│  (Business Logic, State Management)             │
│                                                 │
│  - MainViewModel                                │
│  - CameraViewModel                              │
│  - ResultViewModel                              │
│  - CollectionViewModel                          │
│  - SettingsViewModel                            │
└──────────────────┬──────────────────────────────┘
                   │
                   ↓ (uses)
┌─────────────────────────────────────────────────┐
│              REPOSITORY/DATA LAYER              │
│  (Data Access, ML Models)                       │
│                                                 │
│  - HotWheelsRepository                          │
│  - MobileNetIdentifier (Singleton)              │
│  - SimpleRepository                             │
│  - SimpleDataStorage                            │
└──────────────────┬──────────────────────────────┘
                   │
                   ↓ (accesses)
┌─────────────────────────────────────────────────┐
│                 DATA SOURCES                    │
│                                                 │
│  - SQLite Database (hotwheels.db)               │
│  - JSON Files (models, embeddings)              │
│  - ONNX Model (MobileNetV3)                     │
│  - SharedPreferences                            │
│  - FileSystem (images)                          │
└─────────────────────────────────────────────────┘
```

---

## 🧩 Componentes Principales

### 1. UI Layer

#### MainActivity
- **Responsabilidad:** Pantalla principal, dashboard con estadísticas
- **Funciones:**
  - Mostrar estadísticas de colección
  - Navegación a otras pantallas
  - Integración de AdMob
- **ViewModel:** MainViewModel

#### CameraActivity
- **Responsabilidad:** Captura de imágenes/video
- **Funciones:**
  - CameraX para captura de fotos
  - Guías visuales para posicionamiento
  - Captura multi-foto (2 ángulos)
  - Grabación de video con extracción de frames
  - Filtrado por año
  - Limpieza automática de imágenes antiguas (>7 días)
- **ViewModel:** CameraViewModel
- **Tecnologías:** CameraX, VideoCapture

#### SelectResultActivity
- **Responsabilidad:** Selección de modelo correcto entre múltiples resultados
- **Funciones:**
  - Mostrar top 100 matches del ML
  - Búsqueda manual por nombre
  - Reintentar con exclusión de modelos incorrectos
  - Agregar a colección
- **ViewModel:** ResultViewModel

#### CollectionActivity
- **Responsabilidad:** Gestión de colección personal
- **Funciones:**
  - Listar modelos en colección
  - Editar cantidades y precios
  - Buscar y filtrar
  - Exportar/Importar colección (JSON)
  - Material Design 3
- **ViewModel:** CollectionViewModel

#### SettingsActivity
- **Responsabilidad:** Configuración de la app
- **Funciones:**
  - Cambio de idioma (ES/EN)
  - Gestión de datos
  - Acerca de
- **ViewModel:** SettingsViewModel

---

### 2. ViewModel Layer

Todos los ViewModels heredan de `androidx.lifecycle.ViewModel` y usan:
- **StateFlow** para estado reactivo
- **Coroutines** para operaciones asíncronas
- **LiveData** para eventos de UI

#### CameraViewModel
```kotlin
class CameraViewModel : ViewModel() {
    // Estado del ML
    val mobileNetIsReady: StateFlow<Boolean>
    val mobileNetLoadingProgress: StateFlow<Int>
    val mobileNetLoadingStatus: StateFlow<String>

    // Procesamiento
    val isProcessing: StateFlow<Boolean>
    val processingStatus: StateFlow<String>

    // Resultados
    val identificationResult: StateFlow<IdentificationResult?>
    val topMatches: StateFlow<List<IdentificationMatch>>
    val errorMessage: StateFlow<String?>

    // Funciones
    suspend fun processImage(imagePath: String)
    suspend fun processMultipleImages(imagePaths: List<String>, yearStart: Int?, yearEnd: Int?)
}
```

---

### 3. Repository/Data Layer

#### HotWheelsRepository
- **Responsabilidad:** Acceso a datos de modelos
- **Fuentes de datos:**
  - SQLite (`hotwheels.db`)
  - JSON (`hotwheels_models.json`)
- **Funciones:**
  ```kotlin
  suspend fun getModelById(id: String): HotWheelModel?
  suspend fun getAllModels(): List<HotWheelModel>
  suspend fun searchModels(query: String): List<HotWheelModel>
  suspend fun getModelsByYear(year: Int): List<HotWheelModel>
  ```

#### MobileNetIdentifier (Singleton)
- **Responsabilidad:** Identificación ML
- **Modelo:** MobileNetV3 (ONNX Runtime)
- **Base de datos:** 11,257 embeddings
- **Patrón:** Singleton para evitar recargar modelo
- **Funciones:**
  ```kotlin
  suspend fun initializeAsync() // Carga modelo y embeddings
  suspend fun identifyTopMatches(
      imagePath: String,
      topN: Int = 100,
      excludeModelIds: Set<String> = emptySet(),
      yearStart: Int? = null,
      yearEnd: Int? = null
  ): List<IdentificationMatch>

  suspend fun identifyTopMatchesMultiImage(
      imagePaths: List<String>,
      topN: Int = 100,
      excludeModelIds: Set<String> = emptySet(),
      yearStart: Int? = null,
      yearEnd: Int? = null
  ): List<IdentificationMatch>
  ```

---

## 📊 Flujo de Datos

### Flujo de Identificación

```
1. Usuario toma foto en CameraActivity
   ↓
2. CameraActivity guarda imagen en filesDir/captured_images/
   (FileUtils.createTempImageFile())
   ↓
3. Imagen se recorta al área de guía
   (cropImageToGuideArea())
   ↓
4. CameraViewModel.processMultipleImages() se llama
   ↓
5. MobileNetIdentifier.identifyTopMatchesMultiImage()
   ├─ Genera embeddings con MobileNetV3 (ONNX)
   ├─ Calcula cosine similarity con 11,257 modelos
   ├─ Filtra por año si está activo
   └─ Retorna top 100 matches ordenados
   ↓
6. ResultActivity/SelectResultActivity muestra resultados
   ↓
7. Usuario selecciona modelo correcto
   ↓
8. Se agrega a colección (UserCollection)
   ↓
9. Se guarda en SharedPreferences (SimpleDataStorage)
```

### Flujo de Datos de Colección

```
CollectionActivity
   ↓ observes
CollectionViewModel
   ↓ uses
SimpleRepository
   ↓ uses
SimpleDataStorage (SharedPreferences)
   ↓ stores
JSON serializado con Gson:
{
  "models": [
    {
      "modelId": "...",
      "quantity": 2,
      "userPrice": 50.0,
      "condition": "Mint",
      "notes": "..."
    }
  ]
}
```

---

## 🤖 Machine Learning

### Algoritmo de Identificación

**Modelo:** MobileNetV3 Large (embeddings de 960 dimensiones)

**Pipeline:**
1. **Preprocesamiento:**
   ```kotlin
   - Resize: 224x224
   - Normalización: ImageNet stats (mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
   - Formato: NCHW (batch, channels, height, width)
   ```

2. **Inferencia:**
   - ONNX Runtime para ejecutar MobileNetV3
   - Output: Vector de 960 dimensiones (embedding)

3. **Matching:**
   - L2 Normalization del embedding
   - Cosine Similarity con base de datos de embeddings
   - Threshold mínimo: 20% (MIN_SIMILARITY = 0.20f)
   - Retorna top K matches (K=100)

4. **Multi-imagen:**
   - Procesa cada imagen independientemente
   - Agrega scores: usa máxima similitud entre todas las fotos
   - Prioriza modelos que aparecen en múltiples fotos

### Base de Datos de Embeddings

**Archivo:** `embeddings_mobilenetv3.json` (293 MB)

**Estructura:**
```json
{
  "version": "1.0",
  "model": "MobileNetV3-Large",
  "embedding_dim": 960,
  "total_embeddings": 11257,
  "embeddings": [
    {
      "id": "model_unique_id",
      "name": "Lamborghini Aventador",
      "year": 2024,
      "embedding": [0.123, -0.456, ...]  // 960 valores
    }
  ]
}
```

**Carga eficiente:**
- Usa `JsonReader` (streaming) para evitar cargar todo en memoria
- Barra de progreso durante carga (cada 1000 modelos)

---

## 💾 Almacenamiento

### 1. Archivos de Assets (Read-Only)

```
app/src/main/assets/
├── mobilenetv3_embeddings.onnx     # Modelo ONNX (17 MB)
├── embeddings_mobilenetv3.json     # Embeddings (293 MB)
├── embeddings_mobilenetv3.npz      # Embeddings NumPy (55 MB)
├── hotwheels.db                    # SQLite DB (5.3 MB)
├── hotwheels_models.json           # Metadatos (5.2 MB)
├── metadata.json                   # Info general (724 bytes)
└── reference_images/               # Imágenes de referencia (1.5 GB)
    ├── model_001/
    │   ├── front.jpg
    │   ├── side.jpg
    │   └── angle.jpg
    └── ...
```

### 2. Almacenamiento Interno (App Data)

```
context.filesDir/
└── captured_images/               # Imágenes capturadas (persistentes)
    ├── JPEG_20251028_143052_.jpg
    ├── JPEG_20251028_143052__cropped.jpg
    └── ...

context.cacheDir/                  # (Ya no se usa para imágenes)
```

**Decisión de diseño:** Las imágenes se guardan en `filesDir/captured_images/` en lugar de `cacheDir` para evitar que Android las elimine automáticamente. Se implementó limpieza manual de imágenes >7 días.

### 3. SharedPreferences

```
"app_settings"
├── language: String                # "es" o "en"
└── ...

"user_collection"
└── collection_json: String        # JSON serializado de la colección
```

---

## 🎨 Decisiones de Diseño

### Por qué Singleton para MobileNetIdentifier?

**Problema:** Cargar el modelo ONNX y embeddings tomaba 10-15 segundos y bloqueaba la UI.

**Solución:**
- Patrón Singleton
- Carga asíncrona en background con `suspend fun initializeAsync()`
- StateFlow para progreso visible al usuario
- Una sola instancia durante toda la vida de la app

```kotlin
companion object {
    @Volatile
    private var INSTANCE: MobileNetIdentifier? = null

    fun getInstance(context: Context): MobileNetIdentifier {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: MobileNetIdentifier(context.applicationContext).also { INSTANCE = it }
        }
    }
}
```

### Por qué filesDir en lugar de cacheDir?

**Problema:** Las imágenes capturadas se borraban antes de poder reutilizarlas.

**Solución:**
- Cambiar de `context.cacheDir` a `context.filesDir/captured_images/`
- Android respeta `filesDir` y no lo limpia automáticamente
- Implementar limpieza manual de imágenes >7 días

### Por qué SharedPreferences en lugar de Room?

**Decisión:** Simplicidad sobre escalabilidad para MVP

**Ventajas:**
- Más rápido de implementar
- Menos código boilerplate
- Suficiente para <1000 modelos en colección

**Futuro:** Migrar a Room cuando la colección crezca o se necesiten consultas complejas

### Por qué multi-foto (2 ángulos)?

**Razón:** Mejora la precisión del ML

- Diferentes ángulos capturan diferentes características
- Reduce falsos positivos
- Permite detectar variantes de color/detalles

**Ángulos optimizados:**
1. Vista lateral (side view) - Muestra perfil completo
2. Ángulo 45° - Muestra esquina (frente/trasera + lado)

### Por qué filtro de año por defecto (2020-2025)?

**Razón:** Performance

- Base de datos de 11,257 modelos es grande
- Búsqueda exhaustiva toma varios segundos
- La mayoría de usuarios buscan modelos recientes
- Filtro por defecto reduce a ~3000 modelos
- Usuario puede desactivar filtro si necesita

---

## 🔄 Ciclo de Vida y Threading

### Coroutines y Dispatchers

```kotlin
// UI Operations (Main thread)
lifecycleScope.launch {
    // Safe to update UI here
    binding.textView.text = "Updated"
}

// Background Operations (IO thread)
viewModelScope.launch(Dispatchers.IO) {
    val result = repository.getModelById(id)

    withContext(Dispatchers.Main) {
        // Switch back to main for UI
        updateUI(result)
    }
}

// ML Operations (Default/IO)
lifecycleScope.launch(Dispatchers.Default) {
    mobileNetIdentifier.identifyTopMatches(imagePath)
}
```

### StateFlow vs LiveData

**StateFlow:** Para estado continuo que siempre tiene un valor
```kotlin
private val _isProcessing = MutableStateFlow(false)
val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
```

**LiveData:** Para eventos únicos (menos usado)
```kotlin
private val _errorEvent = MutableLiveData<String>()
val errorEvent: LiveData<String> = _errorEvent
```

---

## 📝 Notas de Implementación

### OpenCV (Deprecated)

La app originalmente usaba OpenCV para detección de características (ORB). Esto fue reemplazado por MobileNetV3 en la versión 2.0.0.

**Código legacy:** Se encuentra en `.deprecated_orb/` para referencia.

### AdMob Integration

```kotlin
// MainActivity
MobileAds.initialize(this)
val adView = findViewById<AdView>(R.id.adView)
val adRequest = AdRequest.Builder().build()
adView.loadAd(adRequest)
```

**Test Ad ID:** `ca-app-pub-3940256099942544~3347511713`

---

## 🚀 Performance Optimizations

1. **Lazy Loading:** Assets grandes se cargan solo cuando se necesitan
2. **Image Compression:** Imágenes capturadas se guardan en JPEG 95% quality
3. **Crop antes de ML:** Reducir área de análisis mejora velocidad
4. **Streaming JSON:** Usar `JsonReader` para embeddings evita OutOfMemoryError
5. **Background Processing:** Todo el ML corre en background threads
6. **Singleton Pattern:** Modelo ML se carga una sola vez

---

## 🔮 Arquitectura Futura

### Mejoras Planificadas

1. **Room Database:**
   - Migrar de SharedPreferences a Room
   - Consultas SQL eficientes
   - Relaciones entre tablas

2. **Jetpack Compose:**
   - Migrar de XML layouts a Compose
   - UI más moderna y reactiva

3. **Hilt/Dagger:**
   - Dependency Injection
   - Mejor testability

4. **TensorFlow Lite:**
   - Alternativa a ONNX Runtime
   - Mejor integración con Android

5. **WorkManager:**
   - Background sync de precios
   - Limpieza programada de imágenes

---

**Última actualización:** 2025-10-28
**Versión del proyecto:** 2.0.0
