# Estructura de Releases en GitHub para Actualizaciones Incrementales

## 📋 Objetivo

Organizar los assets de la base de datos en GitHub Releases para permitir actualizaciones incrementales.

## 📂 Estructura Propuesta

```
GitHub Releases:
├── v1.0-assets (Base completa)
│   ├── manifest.json
│   ├── reference_images_v1.0.tar.gz (1.3GB)
│   └── embeddings_v1.0.json.gz (5MB)
│
├── v1.1-update-2018 (Solo año 2018)
│   ├── manifest.json (actualizado)
│   ├── reference_images_2018.tar.gz (30MB)
│   └── embeddings_2018.json.gz (300KB)
│
└── v1.2-update-2019 (Solo año 2019)
    ├── manifest.json (actualizado)
    ├── reference_images_2019.tar.gz (35MB)
    └── embeddings_2019.json.gz (350KB)
```

## 🔧 Pasos para Configurar

### 1. Renombrar Assets Actuales

Los archivos actuales deben renombrarse para reflejar que son la versión base:

```bash
# En tu sistema local, renombrar:
reference_images.tar.gz → reference_images_v1.0.tar.gz
embeddings_mobilenetv3.json.gz → embeddings_v1.0.json.gz
```

### 2. Crear manifest.json Base

Crear `manifest.json` con la colección base:

```json
{
  "version": "1.0",
  "base_version": "1.0",
  "last_updated": "2024-12-08",
  "collections": {
    "base": {
      "version": "1.0",
      "years": "1968-2017",
      "model_count": 11132,
      "size_mb": 1300,
      "files": {
        "images": "https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/v1.0-assets/reference_images_v1.0.tar.gz",
        "embeddings": "https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/v1.0-assets/embeddings_v1.0.json.gz"
      },
      "checksum": {
        "images": "CALCULATE_MD5",
        "embeddings": "CALCULATE_MD5"
      }
    }
  }
}
```

### 3. Crear Release v1.0-assets en GitHub

1. Ve a tu repositorio en GitHub
2. Click en "Releases" → "Create a new release"
3. Tag: `v1.0-assets`
4. Title: `Database v1.0 - Base Collection (1968-2017)`
5. Description:
   ```
   Base database collection containing 11,132+ Hot Wheels models from 1968-2017.

   **Files:**
   - reference_images_v1.0.tar.gz (1.3GB) - All reference images
   - embeddings_v1.0.json.gz (5MB) - MobileNetV3 embeddings
   - manifest.json - Version manifest for incremental updates

   **First Install:** Download all files
   **Updates:** Only download new collections as they become available
   ```
6. Attach files:
   - `manifest.json`
   - `reference_images_v1.0.tar.gz`
   - `embeddings_v1.0.json.gz`
7. Publish release

### 4. Actualizar URL del Manifest en el Código

Editar `IncrementalAssetDownloader.kt`:

```kotlin
companion object {
    private const val TAG = "IncrementalDownloader"
    // Actualizar esta URL con la URL real de tu release
    private const val MANIFEST_URL = "https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/v1.0-assets/manifest.json"
}
```

**Cómo obtener la URL correcta:**
1. Ve al release en GitHub
2. Click derecho en `manifest.json` → "Copy link address"
3. Pega esa URL en el código

## 🔄 Agregar Actualizaciones (Ejemplo: Hot Wheels 2018)

### Paso 1: Preparar Archivos 2018

```bash
# 1. Crear directorio para imágenes de 2018
mkdir reference_images_2018
cd reference_images_2018
mkdir 2018

# 2. Agregar imágenes de Hot Wheels 2018
cp /path/to/2018/*.jpg 2018/

# 3. Crear tar.gz
tar -czf reference_images_2018.tar.gz 2018/

# 4. Generar embeddings para 2018 (usar tu script de generación)
python generate_embeddings.py --year 2018 --output embeddings_2018.json

# 5. Comprimir embeddings
gzip embeddings_2018.json
```

### Paso 2: Calcular Checksums

```bash
md5sum reference_images_2018.tar.gz
# Anotar el hash resultante

md5sum embeddings_2018.json.gz
# Anotar el hash resultante
```

### Paso 3: Actualizar manifest.json

```json
{
  "version": "1.1",
  "base_version": "1.0",
  "last_updated": "2024-12-15",
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
        "images": "HASH_ORIGINAL",
        "embeddings": "HASH_ORIGINAL"
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
        "images": "HASH_2018_IMAGES",
        "embeddings": "HASH_2018_EMBEDDINGS"
      }
    }
  }
}
```

### Paso 4: Crear Release v1.1-update-2018

1. Tag: `v1.1-update-2018`
2. Title: `Database v1.1 - Hot Wheels 2018 Update`
3. Description:
   ```
   Incremental update adding Hot Wheels models from 2018.

   **New Models:** 250 models
   **Download Size:** ~30 MB

   **Requirements:** Base collection (v1.0) must be installed first

   **Files:**
   - reference_images_2018.tar.gz (30MB)
   - embeddings_2018.json.gz (300KB)
   - manifest.json (updated with 2018 collection)
   ```
4. Attach files y publicar

## 📱 Comportamiento en la App

### Primera Instalación (Usuario Nuevo)

1. App v2.2.0 instalada desde Play Store
2. SplashActivity: Descarga colección "base" (1.3GB)
3. MainActivity: Verifica actualizaciones
4. Detecta "update_2018" disponible
5. Muestra banner: "🎉 250 nuevos modelos disponibles (30 MB)"
6. Usuario decide descargar o posponer

### Usuario con Versión Anterior (v2.1.4)

1. Actualiza app a v2.2.0 desde Play Store
2. Ya tiene base descargada de v2.1.4
3. SplashActivity: Marca "base" como instalada (migración)
4. MainActivity: Verifica actualizaciones
5. Detecta "update_2018" disponible
6. Muestra banner con descarga incremental

### Usuario con Base Actualizada

1. Tiene base + update_2018
2. Desarrollador publica update_2019
3. App verifica manifest.json
4. Detecta solo "update_2019" faltante
5. Descarga solo 35MB en lugar de 1.3GB

## 🧪 Testing

### Test 1: Migración desde v2.1.4
```
1. Instalar app v2.1.4
2. Descargar base completa (1.3GB)
3. Actualizar a v2.2.0
4. Verificar que NO descarga nada
5. Verificar que marca "base" como instalada
```

### Test 2: Primera Instalación v2.2.0
```
1. Instalar app v2.2.0 en dispositivo limpio
2. Verificar descarga de base (1.3GB)
3. Verificar que marca "base" como instalada
4. Verificar detección de actualizaciones
```

### Test 3: Descarga Incremental
```
1. Tener base instalada
2. Publicar update_2018 en GitHub
3. Abrir app
4. Verificar banner de actualización
5. Click en "Descargar"
6. Verificar que descarga solo 30MB
7. Verificar que embeddings se combinan correctamente
```

## 📊 Ventajas del Sistema

1. **Ahorro de Datos:**
   - Antes: 1.3GB cada vez que agregabas modelos
   - Ahora: Solo 30-70MB por año nuevo

2. **Actualizaciones Frecuentes:**
   - Puedes agregar nuevos modelos sin molestar a usuarios
   - Usuarios deciden cuándo descargar

3. **Escalabilidad:**
   - Puedes agregar años indefinidamente
   - Cada actualización es independiente

4. **Backward Compatibility:**
   - Apps viejas siguen descargando completo
   - Apps nuevas migran automáticamente

## 🚀 Roadmap

### Versión 2.2.0 (Actual)
- ✅ Sistema de actualizaciones incrementales
- ✅ Migración automática desde v2.1.4
- ✅ UI de verificación y descarga
- ⏳ Testing completo
- ⏳ Publicar en Play Store

### Versión 2.3.0 (Futuro)
- 📅 Agregar Hot Wheels 2018 (250 modelos)
- 📅 Primera actualización incremental real
- 📅 Monitorear adopción

### Versión 2.4.0 (Futuro)
- 📅 Agregar Hot Wheels 2019 (280 modelos)
- 📅 Agregar Hot Wheels 2020 (300 modelos)

## 📝 Notas Importantes

1. **GitHub Release Size Limit:** 2GB por archivo
   - Base actual (1.3GB) está bien
   - Actualizaciones incrementales (~30-70MB) sin problema

2. **Checksums:** Importante calcularlos para verificar integridad
   ```bash
   md5sum archivo.tar.gz
   ```

3. **URL del Manifest:** Debe actualizarse en el código después de crear el release

4. **Testing:** Probar exhaustivamente antes de publicar en Play Store
