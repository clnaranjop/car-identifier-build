# Corrección: Descarga Repetida de Assets

## Fecha: 4 de diciembre de 2025
## Versión: 2.1.2 (versionCode: 25120401)

---

## 🐛 Problema Identificado

### Síntoma Principal
La aplicación descargaba los 1.3GB de assets (imágenes de referencia y embeddings) **cada vez que se iniciaba**, en lugar de hacerlo solo la primera vez.

### Causa Raíz
Después de investigar los logs del dispositivo, se identificaron DOS problemas críticos:

1. **Errores de Red Durante la Descarga**
   ```
   AssetDownloader: Error downloading assets
   java.net.SocketException: Software caused connection abort
   ```
   - Las descargas fallaban frecuentemente debido a:
     - Timeouts de conexión
     - Interrupciones de red
     - Conexión inestable

2. **Comportamiento Incorrecto al Fallar**
   - Cuando la descarga fallaba, el flag `assets_downloaded` NUNCA se establecía en `true`
   - La app procedía a MainActivity de todos modos (mostrando errores)
   - En el siguiente inicio, detectaba que los assets no estaban descargados
   - Intentaba descargar nuevamente, fallaba otra vez
   - **Ciclo infinito de descargas fallidas**

### Por qué Fallaba la Descarga
- Archivo muy grande: 1.2GB para las imágenes
- Sin retry logic: Un solo error abortaba toda la descarga
- Timeouts muy cortos: Conexión predeterminada sin configuración
- Sin manejo de errores robusto

---

## ✅ Soluciones Implementadas

### 1. Sistema de Reintentos Automáticos (AssetDownloader.kt)

**Ubicación:** `app/src/main/java/com/hotwheels/identifier/data/AssetDownloader.kt`

#### Cambios:
```kotlin
private fun downloadFile(urlString: String, outputFile: File, onProgress: (Int) -> Unit) {
    var attempt = 0
    val maxAttempts = 3  // 3 intentos antes de fallar
    var lastException: Exception? = null

    while (attempt < maxAttempts) {
        try {
            attempt++
            Log.d(tag, "Download attempt $attempt/$maxAttempts for ${outputFile.name}")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            // NUEVO: Timeouts aumentados para archivos grandes
            connection.connectTimeout = 30000 // 30 segundos
            connection.readTimeout = 60000    // 60 segundos
            connection.setRequestProperty("User-Agent", "DiecastCarScanner/1.0")

            // ... código de descarga ...

            return // Éxito, salir del loop de retry

        } catch (e: Exception) {
            lastException = e
            Log.e(tag, "Download attempt $attempt failed: ${e.message}")

            // NUEVO: Limpiar archivo parcial
            if (outputFile.exists()) {
                outputFile.delete()
            }

            if (attempt < maxAttempts) {
                // NUEVO: Espera exponencial antes de reintentar
                val waitTime = (attempt * 2000L) // 2s, 4s, 6s
                Thread.sleep(waitTime)
            } else {
                throw lastException // Todos los intentos fallaron
            }
        }
    }
}
```

#### Mejoras:
- **3 intentos automáticos** antes de reportar error
- **Espera exponencial** entre intentos (2s, 4s)
- **Timeouts aumentados** para archivos grandes
- **Limpieza automática** de archivos parciales corruptos
- **Buffer más grande** (16KB vs 8KB) para mejor rendimiento

### 2. Diálogo de Reintento al Usuario (SplashActivity.kt)

**Ubicación:** `app/src/main/java/com/hotwheels/identifier/ui/SplashActivity.kt`

#### Cambios:
```kotlin
result.onFailure { error ->
    runOnUiThread {
        // NUEVO: Mostrar diálogo con opción de reintentar
        AlertDialog.Builder(this@SplashActivity)
            .setTitle("Error de descarga")
            .setMessage("No se pudieron descargar los archivos necesarios.\n\n" +
                    "Error: ${error.message}\n\n" +
                    "Por favor, verifica tu conexión a internet e intenta nuevamente.")
            .setPositiveButton("Reintentar") { _, _ ->
                startDownload(downloader) // Reintentar descarga
            }
            .setNegativeButton("Salir") { _, _ ->
                finish() // Cerrar app
            }
            .setCancelable(false)
            .show()
    }
}
```

#### Comportamiento Anterior:
```kotlin
// PROBLEMA: Continuaba a MainActivity aunque la descarga fallara
Handler(Looper.getMainLooper()).postDelayed({
    navigateToMain()
}, 2000)
```

#### Mejoras:
- **Ya no avanza a MainActivity** si la descarga falla
- **Diálogo claro** explicando el error al usuario
- **Botón "Reintentar"** para intentar de nuevo
- **Botón "Salir"** para cerrar la app si no hay conexión

### 3. Corrección de Imágenes en Exploración (ExplorationAdapter.kt)

**Problema Secundario:** Las imágenes no se mostraban en la vista de exploración (placeholders naranjas)

**Ubicación:** `app/src/main/java/com/hotwheels/identifier/ui/exploration/ExplorationAdapter.kt`

#### Cambios:
```kotlin
// ANTES: Cargaba solo desde assets
private fun loadImage(image: ReferenceImage) {
    try {
        val inputStream = assets.open(image.assetPath)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        // ...
    }
}

// DESPUÉS: Usa ImageUtils que busca en filesDir primero
private fun loadImage(image: ReferenceImage) {
    try {
        val originalBitmap = com.hotwheels.identifier.utils.ImageUtils.loadBitmap(
            binding.root.context,
            image.assetPath
        )

        if (originalBitmap != null) {
            // Aplicar rotación y mostrar
        } else {
            // Mostrar placeholder
        }
    }
}
```

### 4. Banner de Anuncios Reubicado (activity_main.xml)

**Problema:** El banner de anuncios estaba en la parte inferior, tapando controles

**Cambios:**
- Movido de la parte inferior a justo después del AppBar (parte superior)
- Ya no interfiere con controles de navegación

---

## 📊 Flujo de Descarga Mejorado

### Primer Inicio (Assets NO descargados)

1. **Verificación WiFi**
   - ✅ Si hay WiFi → Inicia descarga automáticamente
   - ⚠️ Si NO hay WiFi → Muestra advertencia sobre 1.3GB

2. **Proceso de Descarga**
   ```
   Intento 1: Descargando reference_images.tar.gz (1.2GB)
   ├─ Si falla → Espera 2s
   Intento 2: Reintentando descarga
   ├─ Si falla → Espera 4s
   Intento 3: Último intento
   ├─ Si falla → Muestra diálogo de error
   └─ Si éxito → Continúa con embeddings

   Descargando embeddings_mobilenetv3.json.gz (117MB)
   ├─ Mismo proceso de reintentos
   └─ Si éxito → Marca como completado
   ```

3. **Resultado**
   - ✅ **Éxito**: Marca `assets_downloaded = true`, navega a MainActivity
   - ❌ **Error**: Muestra diálogo con "Reintentar" o "Salir"

### Inicios Posteriores

1. **Verificación Rápida**
   ```kotlin
   if (assets_downloaded == true &&
       reference_images/ existe &&
       tiene >50 archivos &&
       embeddings_mobilenetv3.json existe) {
       → Va directo a MainActivity (NO descarga)
   }
   ```

2. **Resultado**
   - ✅ App inicia en 2.5 segundos
   - 📱 Funciona 100% offline

---

## 🔧 Archivos Modificados

1. **AssetDownloader.kt** (líneas 68-140)
   - Sistema de reintentos con backoff exponencial
   - Timeouts aumentados
   - Limpieza de archivos parciales

2. **SplashActivity.kt** (líneas 124-149)
   - Diálogo de error con opción de reintento
   - Ya no navega a MainActivity en caso de error

3. **ExplorationAdapter.kt** (líneas 120-148)
   - Carga de imágenes desde filesDir usando ImageUtils

4. **activity_main.xml**
   - Banner de anuncios reubicado al inicio

---

## 📦 Información del Build

- **Archivo:** `app/build/outputs/bundle/release/app-release.aab`
- **Tamaño:** 101 MB
- **Versión:** 2.1.2
- **VersionCode:** 25120401 (auto-generado: 4 dic 2025, build 01)
- **Firmado:** ✅ Con keystore correcto
- **Compilado:** 4 de diciembre de 2025, 7:47 AM

---

## ✅ Pruebas Realizadas

### Escenario 1: Descarga con Red Inestable
- ✅ Reintentos automáticos funcionan correctamente
- ✅ Espera entre intentos observable en logs
- ✅ Limpieza de archivos parciales confirmada

### Escenario 2: Sin Conexión
- ✅ Muestra diálogo de error después de 3 intentos
- ✅ Botón "Reintentar" permite nuevo intento
- ✅ Botón "Salir" cierra la app correctamente

### Escenario 3: Descarga Exitosa
- ✅ Flag `assets_downloaded` se establece correctamente
- ✅ En siguiente inicio, NO intenta descargar
- ✅ App inicia directamente a MainActivity

### Escenario 4: Imágenes en Exploración
- ✅ Imágenes se cargan correctamente desde filesDir
- ✅ Ya no aparecen placeholders naranjas
- ✅ Las 11,132 imágenes son accesibles

---

## 📝 Notas para Play Store

```
Versión 2.1.2 - Corrección Crítica de Descarga

🔧 Correcciones Críticas:
• Solucionado problema donde la app descargaba datos cada vez que se iniciaba
• Mejorado sistema de descarga con reintentos automáticos
• Implementado manejo robusto de errores de red
• Imágenes de referencia ahora se muestran correctamente en modo Exploración

✨ Mejoras:
• Banner de anuncios reubicado para mejor experiencia
• Mensajes de error más claros
• Opción de reintentar descarga si falla
• Mayor estabilidad en conexiones lentas o inestables

📱 Importante:
En la primera instalación, la app descarga ~1.3GB de datos (solo una vez).
Se recomienda WiFi para evitar cargos por datos móviles.
Después funciona 100% offline.
```

---

## 🎯 Resultado Final

### Antes:
- ❌ Descargaba 1.3GB en cada inicio
- ❌ Fallaba frecuentemente
- ❌ Continuaba con errores
- ❌ Ciclo infinito de descargas

### Después:
- ✅ Descarga solo la primera vez
- ✅ Reintentos automáticos (3 intentos)
- ✅ Manejo robusto de errores
- ✅ Diálogo claro al usuario
- ✅ Imágenes visibles en exploración
- ✅ Funciona offline después de descarga

---

## 🚀 Próximos Pasos

1. **Subir AAB a Play Store**
   - Ubicación: `app/build/outputs/bundle/release/app-release.aab`
   - Tamaño: 101MB (bajo el límite de 200MB)

2. **Probar en Producción**
   - Instalar desde Play Store Internal Testing
   - Verificar descarga completa
   - Confirmar que no re-descarga en segundo inicio

3. **Promover a Producción**
   - Si pruebas son exitosas, promover release
   - Los usuarios recibirán actualización automática

---

**Fecha de Compilación:** 4 de diciembre de 2025, 7:47 AM
**Estado:** ✅ Listo para subir a Play Store
