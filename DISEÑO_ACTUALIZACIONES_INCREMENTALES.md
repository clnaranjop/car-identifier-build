# Sistema de Actualizaciones Incrementales - Hot Wheels Identifier

## 📋 Objetivo

Permitir agregar nuevos modelos de Hot Wheels sin que los usuarios tengan que descargar toda la base de datos nuevamente.

## 🎯 Requisitos

1. **Primera instalación:** Descargar base completa (1.3GB) una sola vez
2. **Actualización de app (solo código):** No descargar nada
3. **Actualización de base de datos:** Descargar SOLO los nuevos autos
4. **Backward compatibility:** Apps antiguas siguen funcionando

## 🏗️ Arquitectura Propuesta

### 1. Sistema de Versionado

Crear un archivo `manifest.json` en GitHub que contenga:

```json
{
  "version": "2.0",
  "base_version": "1.0",
  "last_updated": "2024-12-08",
  "collections": {
    "base": {
      "version": "1.0",
      "years": "1968-2017",
      "model_count": 11132,
      "size_mb": 1300,
      "files": {
        "images": "https://github.com/.../reference_images_v1.0.tar.gz",
        "embeddings": "https://github.com/.../embeddings_v1.0.json.gz"
      },
      "checksum": {
        "images": "abc123...",
        "embeddings": "def456..."
      }
    },
    "update_2018": {
      "version": "1.1",
      "years": "2018",
      "model_count": 250,
      "size_mb": 30,
      "files": {
        "images": "https://github.com/.../reference_images_2018.tar.gz",
        "embeddings": "https://github.com/.../embeddings_2018.json.gz"
      },
      "requires": ["base"],
      "checksum": {
        "images": "ghi789...",
        "embeddings": "jkl012..."
      }
    },
    "update_2019": {
      "version": "1.2",
      "years": "2019",
      "model_count": 280,
      "size_mb": 35,
      "files": {
        "images": "https://github.com/.../reference_images_2019.tar.gz",
        "embeddings": "https://github.com/.../embeddings_2019.json.gz"
      },
      "requires": ["base"],
      "checksum": {
        "images": "mno345...",
        "embeddings": "pqr678..."
      }
    }
  }
}
```

### 2. Estructura de SharedPreferences

```kotlin
// Almacenar versión instalada
prefs.putString("database_version", "1.2")

// Almacenar colecciones instaladas
prefs.putStringSet("installed_collections", setOf("base", "update_2018", "update_2019"))

// Última verificación de actualizaciones
prefs.putLong("last_update_check", System.currentTimeMillis())
```

### 3. Lógica de Descarga

```kotlin
class IncrementalAssetDownloader(private val context: Context) {

    suspend fun checkForUpdates(): UpdateInfo {
        // 1. Descargar manifest.json desde GitHub
        val manifest = downloadManifest()

        // 2. Obtener versión instalada
        val installedVersion = prefs.getString("database_version", "0.0")
        val installedCollections = prefs.getStringSet("installed_collections", emptySet())

        // 3. Determinar qué colecciones faltan
        val missingCollections = manifest.collections.filter {
            !installedCollections.contains(it.key)
        }

        // 4. Calcular tamaño total de descarga
        val totalSize = missingCollections.values.sumOf { it.size_mb }

        return UpdateInfo(
            hasUpdates = missingCollections.isNotEmpty(),
            collections = missingCollections,
            totalSizeMB = totalSize,
            newModelCount = missingCollections.values.sumOf { it.model_count }
        )
    }

    suspend fun downloadUpdates(
        updateInfo: UpdateInfo,
        onProgress: (Int, String) -> Unit
    ): Result<Unit> {
        try {
            val collections = updateInfo.collections
            var completedCollections = 0

            for ((name, collection) in collections) {
                // Descargar imágenes de esta colección
                val imagesFile = File(context.cacheDir, "${name}_images.tar.gz")
                downloadFile(collection.files.images, imagesFile) { progress ->
                    val overallProgress = (completedCollections * 100 + progress) / collections.size
                    onProgress(overallProgress, "Descargando $name...")
                }

                // Extraer imágenes
                extractTarGz(imagesFile, context.filesDir)
                imagesFile.delete()

                // Descargar embeddings de esta colección
                val embeddingsFile = File(context.cacheDir, "${name}_embeddings.json.gz")
                downloadFile(collection.files.embeddings, embeddingsFile) { progress ->
                    val overallProgress = (completedCollections * 100 + progress) / collections.size
                    onProgress(overallProgress, "Procesando $name...")
                }

                // Merge embeddings con el archivo principal
                mergeEmbeddings(embeddingsFile, collection.years)
                embeddingsFile.delete()

                // Marcar colección como instalada
                val installed = prefs.getStringSet("installed_collections", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                installed.add(name)
                prefs.edit().putStringSet("installed_collections", installed).apply()

                completedCollections++
            }

            // Actualizar versión de la base de datos
            prefs.edit().putString("database_version", manifest.version).apply()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mergeEmbeddings(newEmbeddingsGz: File, years: String) {
        // 1. Extraer nuevo archivo de embeddings
        val newEmbeddings = File(context.cacheDir, "temp_embeddings.json")
        extractGz(newEmbeddingsGz, newEmbeddings)

        // 2. Leer embeddings existentes
        val existingFile = File(context.filesDir, "embeddings_mobilenetv3.json")
        val existingJson = if (existingFile.exists()) {
            JSONObject(existingFile.readText())
        } else {
            JSONObject()
        }

        // 3. Leer nuevos embeddings
        val newJson = JSONObject(newEmbeddings.readText())

        // 4. Merge (agregar nuevos modelos)
        val newModels = newJson.getJSONArray("models")
        val existingModels = existingJson.optJSONArray("models") ?: JSONArray()

        for (i in 0 until newModels.length()) {
            existingModels.put(newModels.getJSONObject(i))
        }

        existingJson.put("models", existingModels)
        existingJson.put("version", newJson.getString("version"))
        existingJson.put("last_updated", System.currentTimeMillis())

        // 5. Guardar archivo combinado
        existingFile.writeText(existingJson.toString(2))

        newEmbeddings.delete()
    }
}
```

### 4. UI de Actualización

Agregar en MainActivity un botón/banner que:
- Verifica actualizaciones en segundo plano
- Muestra: "🎉 250 nuevos modelos disponibles (30 MB)"
- Al hacer click: Descarga incremental
- Muestra progreso: "Descargando Hot Wheels 2018... 45%"

### 5. Compatibilidad Backward

El sistema actual seguirá funcionando:
- Si `manifest.json` no existe → Descarga completa (como ahora)
- Si existe → Descarga incremental
- Apps viejas → Siguen descargando completo sin problemas

## 📂 Estructura de Archivos en GitHub Releases

```
v1.0-assets/
├── manifest.json                        # Nuevo
├── reference_images_v1.0.tar.gz        # Base completa (renombrado)
├── embeddings_v1.0.json.gz             # Base completa (renombrado)

v1.1-update-2018/
├── reference_images_2018.tar.gz        # Solo 2018
├── embeddings_2018.json.gz             # Solo 2018
├── manifest.json                        # Actualizado

v1.2-update-2019/
├── reference_images_2019.tar.gz        # Solo 2019
├── embeddings_2019.json.gz             # Solo 2019
├── manifest.json                        # Actualizado
```

## 🔄 Flujo de Usuario

### Primera Instalación (Usuario Nuevo)
1. Instala app v2.1.4
2. Abre app
3. Descarga base completa (1.3GB) - colección "base"
4. Detecta updates disponibles (2018, 2019)
5. Muestra: "500 nuevos modelos disponibles (65 MB)"
6. Usuario decide si descargar ahora o después

### Actualización de App (Solo Código)
1. Usuario ya tiene app v2.1.3 con base completa
2. Play Store actualiza a v2.1.4
3. Abre app
4. **NO descarga nada** (ya tiene todo)
5. Funciona inmediatamente

### Actualización de Base de Datos (Nuevos Autos)
1. Usuario tiene v2.1.4 con base + 2018
2. Desarrollador agrega 2019 y 2020
3. App detecta: "530 nuevos modelos (70 MB)"
4. Usuario descarga solo 2019 + 2020 (70 MB)
5. Embeddings se combinan automáticamente
6. Listo - ahora tiene base + 2018 + 2019 + 2020

## ⚡ Ventajas

1. **Ahorro de datos:** Usuario solo descarga lo nuevo
2. **Más rápido:** 70 MB vs 1.3 GB
3. **Mejor UX:** Actualizaciones frecuentes sin molestar
4. **Escalable:** Puedes agregar años indefinidamente
5. **Backward compatible:** Apps viejas siguen funcionando

## 🛠️ Implementación

### Fase 1: Preparación (Sin cambiar app)
1. Crear `manifest.json` en GitHub
2. Renombrar archivos actuales a `v1.0`
3. Preparar estructura de releases

### Fase 2: App v2.2.0
1. Implementar `IncrementalAssetDownloader`
2. Agregar UI de verificación de actualizaciones
3. Mantener compatibilidad con descarga completa
4. Testing exhaustivo

### Fase 3: Agregar Contenido Nuevo
1. Scrape Hot Wheels 2018
2. Generar embeddings para 2018
3. Crear release `v1.1-update-2018`
4. Actualizar `manifest.json`
5. Usuarios descargan solo 2018

## 📝 Código de Ejemplo - Verificar Actualizaciones

```kotlin
// En MainActivity.kt
private fun checkForDatabaseUpdates() {
    lifecycleScope.launch {
        try {
            val downloader = IncrementalAssetDownloader(this@MainActivity)
            val updateInfo = downloader.checkForUpdates()

            if (updateInfo.hasUpdates) {
                showUpdateBanner(
                    "🎉 ${updateInfo.newModelCount} nuevos modelos disponibles (${updateInfo.totalSizeMB} MB)",
                    onClick = { downloadUpdates(updateInfo) }
                )
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking updates", e)
        }
    }
}

private fun downloadUpdates(updateInfo: UpdateInfo) {
    lifecycleScope.launch {
        val downloader = IncrementalAssetDownloader(this@MainActivity)

        showProgressDialog { dialog, progressBar, textView ->
            downloader.downloadUpdates(updateInfo) { progress, message ->
                runOnUiThread {
                    progressBar.progress = progress
                    textView.text = message
                }
            }.onSuccess {
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "✅ Base de datos actualizada", Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "❌ Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
```

## 🎯 Resumen

**Situación Actual:**
- ✅ Primera instalación: Descarga 1.3GB una vez
- ✅ Update de app: No descarga nada
- ❌ Agregar autos: Descarga TODO de nuevo

**Con Sistema Incremental:**
- ✅ Primera instalación: Descarga 1.3GB una vez
- ✅ Update de app: No descarga nada
- ✅ Agregar autos: Descarga SOLO los nuevos (ej: 30-70 MB)

¿Te gustaría que implemente este sistema en la próxima versión?
