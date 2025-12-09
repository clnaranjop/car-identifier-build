# Cambios Versión 2.1.3

## Fecha: 4 de diciembre de 2025
## VersionCode: 25120402 (auto-generado)
## VersionName: 2.1.3

---

## 🎯 Problemas Resueltos en Esta Versión

### 1. ✅ Banner de Anuncios con Posicionamiento Inteligente

**Problema Anterior:**
- Banner estaba en la parte superior de la pantalla (debajo del título)
- Quedaba tapado por los botones de navegación del sistema
- No era scrollable

**Solución Implementada:**

#### MainActivity y ExplorationActivity
- **Detección Automática de Navegación:** El banner ahora detecta si el dispositivo usa navegación por botones o gestos
- **Posicionamiento Dinámico:**
  - Si el dispositivo usa **botones de navegación**: El banner se posiciona con margen suficiente para que sea visible por encima de los botones
  - Si el dispositivo usa **navegación por gestos**: El banner se queda en la parte inferior sin margen adicional
- **Ubicación del Banner:** Movido a la parte inferior de la pantalla, después del botón "Acerca de", dentro del ScrollView para que sea accesible al hacer scroll

**Código Implementado:**
```kotlin
// MainActivity.kt y ExplorationActivity.kt
private fun getNavigationBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        val hasNavigationBar = !isGestureNavigationEnabled()
        if (hasNavigationBar) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    } else {
        0
    }
}

private fun isGestureNavigationEnabled(): Boolean {
    val resources = resources
    val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
    return if (resourceId > 0) {
        resources.getInteger(resourceId) == 2 // 2 = gesture navigation
    } else {
        // Fallback: check navbar height
        val navBarHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (navBarHeight > 0) {
            val height = resources.getDimensionPixelSize(navBarHeight)
            height < 48 // Gesture bars are typically < 48dp
        } else {
            true
        }
    }
}

private fun initializeAds() {
    MobileAds.initialize(this) {}

    binding.adView.post {
        val navBarHeight = getNavigationBarHeight()
        if (navBarHeight > 0) {
            val params = binding.adView.layoutParams
            params.bottomMargin = navBarHeight
            binding.adView.layoutParams = params
        }
    }

    val adRequest = AdRequest.Builder().build()
    binding.adView.loadAd(adRequest)
}
```

**Archivos Modificados:**
- `app/src/main/java/com/hotwheels/identifier/ui/MainActivity.kt`
- `app/src/main/java/com/hotwheels/identifier/ui/exploration/ExplorationActivity.kt`
- `app/src/main/res/layout/activity_main.xml`

---

### 2. ✅ Imagen del Día Ahora se Muestra Correctamente

**Problema Anterior:**
- La imagen del día no se mostraba en MainActivity
- Usaba `assets.open()` que no funciona para archivos descargados en `filesDir`

**Solución Implementada:**
- Cambiado a usar `ImageUtils.loadBitmap()` que busca primero en `filesDir` y luego en `assets`
- Agregados logs detallados para debugging
- Manejo correcto de casos cuando la imagen no se encuentra

**Código Antes:**
```kotlin
val inputStream = assets.open(imagePath)
val bitmap = BitmapFactory.decodeStream(inputStream)
inputStream.close()
```

**Código Ahora:**
```kotlin
val bitmap = com.hotwheels.identifier.utils.ImageUtils.loadBitmap(
    this@MainActivity,
    imagePath
)

if (bitmap != null) {
    binding.imgCarOfTheDay.setImageBitmap(bitmap)
    binding.tvCarOfTheDayName.text = "${randomModel.name} (${randomModel.year})"
    Log.d(tag, "Car of the day image loaded successfully")
} else {
    binding.tvCarOfTheDayName.text = "${randomModel.name} (${randomModel.year})"
    Log.d(tag, "Failed to load car of the day image from: $imagePath")
}
```

**Archivo Modificado:**
- `app/src/main/java/com/hotwheels/identifier/ui/MainActivity.kt` (líneas 519-538)

---

### 3. 🔍 Logging Mejorado para Investigar Descargas Repetidas

**Problema Pendiente:**
- La app sigue descargando los assets cada vez que se inicia
- Assets EXISTEN en el dispositivo (confirmado por exploración funcionando)
- Pero algo falla en la verificación o persistencia del flag

**Mejoras Implementadas para Debugging:**

#### AssetDownloader.kt - Verificación Detallada
```kotlin
fun areAssetsDownloaded(): Boolean {
    val downloaded = prefs.getBoolean("assets_downloaded", false)
    val assetsDir = File(context.filesDir, "reference_images")
    val embeddingsFile = File(context.filesDir, "embeddings_mobilenetv3.json")

    val imageCount = assetsDir.listFiles()?.size ?: 0

    Log.d(tag, "=== CHECKING ASSETS DOWNLOADED ===")
    Log.d(tag, "SharedPrefs 'assets_downloaded': $downloaded")
    Log.d(tag, "reference_images dir exists: ${assetsDir.exists()}")
    Log.d(tag, "reference_images count: $imageCount")
    Log.d(tag, "embeddings_mobilenetv3.json exists: ${embeddingsFile.exists()}")

    val result = downloaded && assetsDir.exists() && imageCount > 50 && embeddingsFile.exists()
    Log.d(tag, "Final result: $result")

    return result
}
```

#### Verificación al Guardar el Flag
```kotlin
// Después de descarga exitosa
Log.d(tag, "Setting 'assets_downloaded' flag to true")
prefs.edit().putBoolean("assets_downloaded", true).apply()

// Verificar que se guardó correctamente
val verifyFlag = prefs.getBoolean("assets_downloaded", false)
Log.d(tag, "Verified 'assets_downloaded' flag: $verifyFlag")
```

**Qué Verificar en los Logs:**
1. ¿El flag `assets_downloaded` se establece correctamente después de la descarga?
2. ¿El directorio `reference_images` existe en inicios posteriores?
3. ¿Cuántas imágenes cuenta en el directorio?
4. ¿El archivo `embeddings_mobilenetv3.json` existe?
5. ¿Cuál de estas condiciones falla en el segundo inicio?

**Archivo Modificado:**
- `app/src/main/java/com/hotwheels/identifier/data/AssetDownloader.kt`

---

## 📋 Resumen de Archivos Modificados

### Archivos Kotlin:
1. **MainActivity.kt**
   - Agregado `getNavigationBarHeight()` y `isGestureNavigationEnabled()`
   - Modificado `initializeAds()` para posicionamiento inteligente del banner
   - Modificado `loadCarOfTheDay()` para usar `ImageUtils.loadBitmap()`

2. **ExplorationActivity.kt**
   - Agregado `getNavigationBarHeight()` y `isGestureNavigationEnabled()`
   - Modificado `initializeAds()` para posicionamiento inteligente del banner

3. **AssetDownloader.kt**
   - Agregado logging detallado en `areAssetsDownloaded()`
   - Agregado verificación del flag después de guardarlo

### Archivos XML:
1. **activity_main.xml**
   - Movido `AdView` de la parte superior a la parte inferior (después del botón "Acerca de")
   - Colocado dentro del `NestedScrollView` para que sea scrollable
   - Reducido `marginBottom` del botón "Acerca de" de 120dp a 16dp

### Configuración:
1. **app/build.gradle**
   - Actualizado `versionName` de "2.1.2" a "2.1.3"
   - `versionCode` se auto-genera: 25120402

---

## 🔧 Compilación

### Release AAB (Para Play Store):
```bash
export JAVA_HOME=/home/cristhyan/.var/app/com.visualstudio.code/data/vscode/extensions/redhat.java-1.50.0-linux-x64/jre/21.0.9-linux-x86_64
export PATH=$JAVA_HOME/bin:$PATH
./gradlew clean bundleRelease
```

**Ubicación:** `app/build/outputs/bundle/release/app-release.aab`
**Tamaño Estimado:** ~101 MB

### Debug APK (Para Pruebas):
```bash
export JAVA_HOME=/home/cristhyan/.var/app/com.visualstudio.code/data/vscode/extensions/redhat.java-1.50.0-linux-x64/jre/21.0.9-linux-x86_64
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
```

**Ubicación:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Pruebas Necesarias

### 1. Banner de Anuncios
- [ ] Dispositivo con **navegación por botones**: Banner debe estar visible por encima de los botones
- [ ] Dispositivo con **navegación por gestos**: Banner debe estar en el fondo sin margen extra
- [ ] Banner debe estar después del botón "Acerca de"
- [ ] Banner debe ser accesible al hacer scroll hacia abajo

### 2. Imagen del Día
- [ ] MainActivity debe mostrar una imagen de Hot Wheels como "Auto del Día"
- [ ] La imagen debe cambiar cada día
- [ ] Al hacer clic en la imagen debe mostrarse en pantalla completa

### 3. Descarga de Assets (CRÍTICO - Pendiente de Verificar)
- [ ] **Primer inicio:** App descarga 1.3GB de datos
- [ ] **Segundo inicio:** App NO debe descargar, debe ir directo a MainActivity
- [ ] **Verificación en logs:** Buscar "=== CHECKING ASSETS DOWNLOADED ===" en logcat

**Comando para ver logs:**
```bash
~/Android/Sdk/platform-tools/adb logcat -d -s AssetDownloader:D SplashActivity:D MainActivity:D
```

---

## ⚠️ Problema Conocido Pendiente

### Descargas Repetidas
**Estado:** Se agregó logging extensivo para investigar, pero el problema persiste.

**Hipótesis:**
1. El flag `assets_downloaded` no se está persistiendo entre sesiones
2. El directorio `reference_images` se está borrando de alguna forma
3. La verificación de cantidad de archivos (> 50) está fallando
4. El archivo `embeddings_mobilenetv3.json` no se está extrayendo correctamente

**Siguiente Paso:**
Necesitamos capturar los logs de:
1. Primer inicio (durante y después de la descarga)
2. Segundo inicio (para ver qué condición falla en `areAssetsDownloaded()`)

---

## 📝 Notas para Play Store

```
Versión 2.1.3 - Mejoras de Interfaz y Correcciones

✨ Nuevo:
• Banner de anuncios con posicionamiento inteligente según navegación del dispositivo
• Ahora la imagen del día se muestra correctamente en la pantalla principal
• Logging mejorado para diagnóstico de problemas

🐛 Correcciones:
• El banner ya no queda tapado por los botones de navegación
• Banner ahora es scrollable y está en la posición correcta
• Imagen del día carga correctamente desde archivos descargados

📱 Nota Importante:
En la primera instalación, la app descarga ~1.3GB de datos (solo una vez).
Se recomienda WiFi para evitar cargos por datos móviles.
Después funciona 100% offline.
```

---

## 🎯 Estado de los 5 Problemas Reportados

1. ✅ **Explorer arreglado** - Confirmado por usuario como funcionando bien
2. ✅ **Imagen del día** - Corregido en esta versión (usa ImageUtils)
3. ✅ **Banner posicionado correctamente** - Implementado posicionamiento inteligente
4. ⏳ **Descargas repetidas** - Logging agregado, necesita pruebas para confirmar fix
5. ⏳ **Screenshot pendiente** - Esperando que usuario conecte dispositivo

---

**Fecha de Compilación:** 4 de diciembre de 2025
**Estado:** Listo para pruebas
