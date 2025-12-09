# Instrucciones Finales para Publicación en Play Store

## ✅ Estado Actual

El AAB ha sido compilado exitosamente y está listo para subir a Play Store:
- **Archivo:** `app/build/outputs/bundle/release/app-release.aab`
- **Tamaño:** 101MB (bajo el límite de 200MB ✅)
- **Versión:** 2.1.0 (versionCode 9)
- **Firmado:** ✅ Con keystore correcto

## 📦 Paso 1: Subir archivo faltante a GitHub Release

Antes de publicar en Play Store, necesitas subir el archivo de embeddings al release de GitHub:

### Ubicación del archivo:
```
/home/cristhyan/Escritorio/proy_h/embeddings_mobilenetv3.json.gz
```
Tamaño: 117MB

### Cómo subirlo:

1. Ve a: https://github.com/clnaranjop/car-identifier-build/releases/tag/v1.0-assets

2. Click en "Edit release"

3. En la sección "Attach binaries", arrastra el archivo:
   - `embeddings_mobilenetv3.json.gz`

4. Click en "Update release"

### Verificar que el release tenga estos 2 archivos:
- ✅ `reference_images.tar.gz` (1.2GB) - Ya subido
- ⏳ `embeddings_mobilenetv3.json.gz` (117MB) - **PENDIENTE DE SUBIR**

## 🚀 Paso 2: Subir AAB a Play Store

Una vez que el archivo de embeddings esté en GitHub Release:

### Ubicación del AAB:
```
/home/cristhyan/Escritorio/proy_h/app/build/outputs/bundle/release/app-release.aab
```

### Cómo subirlo:

1. Ve a: https://play.google.com/console

2. Selecciona tu app "Diecast Car Scanner"

3. En el menú lateral, ve a: **Production** → **Releases**

4. Click en "Create new release"

5. En "App bundles", sube el archivo:
   ```
   app-release.aab
   ```

6. El sistema verificará:
   - ✅ Tamaño: 101MB (bajo límite de 200MB)
   - ✅ Firma: Keystore correcto
   - ✅ Versión: 9 (mayor que la versión actual)

7. Agrega notas de la versión:
   ```
   Version 2.1.0:
   - Optimización de tamaño de la aplicación
   - Mejoras en el rendimiento
   - Correcciones de errores menores
   ```

8. Click en "Review release"

9. Click en "Start rollout to Production"

## ⚠️ IMPORTANTE: Primera ejecución

Cuando los usuarios instalen o actualicen la app por primera vez:

1. La app mostrará una pantalla de descarga en el splash screen
2. Descargará automáticamente:
   - Imágenes de referencia (1.2GB)
   - Embeddings del modelo (117MB)
3. Total de descarga: ~1.3GB
4. El usuario necesitará conexión a internet solo para esta primera descarga
5. Después de la descarga, la app funciona 100% offline

## 📱 Probar antes de publicar (Opcional pero recomendado)

Si quieres probar la descarga antes de publicar:

1. Compila un APK debug:
   ```bash
   export JAVA_HOME=~/.var/app/com.visualstudio.code/data/vscode/extensions/redhat.java-1.50.0-linux-x64/jre/21.0.9-linux-x86_64
   export PATH=$JAVA_HOME/bin:$PATH
   ./gradlew assembleDebug
   ```

2. Instala en un dispositivo con ADB:
   ```bash
   ~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Abre la app y verifica que:
   - Muestra la pantalla de descarga
   - Descarga los archivos correctamente
   - La identificación funciona después de la descarga

## 🔧 Solución de problemas

### Si la descarga falla:

1. Verifica que ambos archivos estén en GitHub Release
2. Verifica las URLs en `AssetDownloader.kt`:
   - `https://github.com/clnaranjop/car-identifier-build/releases/download/v1.0-assets/reference_images.tar.gz`
   - `https://github.com/clnaranjop/car-identifier-build/releases/download/v1.0-assets/embeddings_mobilenetv3.json.gz`

### Si Play Store rechaza el AAB:

- Verifica que el versionCode sea mayor que el anterior
- Verifica que la firma sea correcta
- El tamaño debería ser 101MB (muy por debajo del límite)

## 📊 Cambios técnicos implementados

Para referencia futura:

1. **AssetDownloader**: Descarga assets desde GitHub en primer lanzamiento
2. **SplashActivity**: Muestra UI de descarga con progreso
3. **ImageUtils**: Carga imágenes desde filesDir o assets (fallback)
4. **MobileNetIdentifier**: Busca embeddings en filesDir primero
5. **SelectResultActivity**: Usa ImageUtils para cargar imágenes
6. **Assets eliminados**:
   - `reference_images/` (1.3GB) - Se descarga desde GitHub
   - `embeddings_mobilenetv3.json` (275MB) - Se descarga desde GitHub

## ✅ Lista de verificación final

Antes de publicar, verifica:

- [ ] Archivo `embeddings_mobilenetv3.json.gz` subido a GitHub Release
- [ ] Release de GitHub tiene 2 archivos (reference_images.tar.gz y embeddings_mobilenetv3.json.gz)
- [ ] AAB compilado correctamente (101MB)
- [ ] Versión incrementada a 2.1.0 (versionCode 9)
- [ ] (Opcional) Probado en dispositivo real
- [ ] Notas de versión preparadas
- [ ] Listo para subir a Play Store

## 🎉 Después de publicar

Una vez publicado en Play Store:

1. La app estará disponible en ~24-48 horas
2. Los usuarios existentes recibirán la actualización automáticamente
3. En la primera ejecución después de actualizar, descargarán los assets
4. Monitorea los comentarios en Play Store para detectar problemas

---

**Nota:** Si necesitas hacer cambios después, recuerda:
- Incrementar versionCode en `app/build.gradle`
- Recompilar con `./gradlew clean bundleRelease`
- El keystore está en: `diecast-release.keystore`
