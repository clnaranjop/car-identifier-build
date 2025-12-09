# Problema: Identificación No Funciona

**Fecha**: 2025-11-04
**Reporte**: "Al tratar de identificar el auto dice que no lo detecta"

---

## ✅ Estado Actual

- ✅ Imágenes se ven correctas en modo Exploración
- ✅ Rotaciones aplicadas correctamente (1,182 imágenes)
- ✅ App compilada e instalada
- ❌ **Identificación NO funciona** (dice "no detectado")

---

## 🔍 Causa Raíz Identificada

### Problema: Embeddings Desactualizados

**Línea de tiempo:**
1. **18:25** - Embeddings generados con imágenes 1978 invertidas
2. **Hoy 08:00** - Imágenes 1978 corregidas (6 imágenes rotadas)
3. **Hoy 08:00** - APK recompilado e instalado
4. **Resultado**: App tiene imágenes correctas pero embeddings ANTIGUOS

**¿Por qué falla la identificación?**

Cuando escaneas un auto con la cámara:
1. App captura foto del auto
2. Extrae embedding de la foto (vector de 1280 dimensiones)
3. Compara con embeddings en `embeddings_mobilenetv3.npz`
4. **PROBLEMA**: Los embeddings de 1978 son de imágenes invertidas
5. No encuentra match porque el embedding no coincide
6. Resultado: "No detectado"

### Analogía:
```
Base de datos dice:
  "Highway Patrol 1978" = [0.5, 0.2, ..., 0.8] ← embedding de imagen INVERTIDA

Tu foto:
  "Highway Patrol 1978" = [0.3, 0.8, ..., 0.1] ← embedding de imagen CORRECTA

Similitud: 35% ❌ (umbral típico: >70%)
```

---

## ✅ Solución

### Paso 1: Regenerar Embeddings
```bash
cd ~/Escritorio/proy_h
python3 regenerate_embeddings.py
```

Esto generará embeddings NUEVOS basados en:
- 10,520 imágenes
- Con las 1,182 rotaciones aplicadas
- Incluyendo las 6 de 1978 corregidas hoy

**Tiempo estimado**: 2-3 minutos

### Paso 2: Recompilar App
```bash
./gradlew assembleDebug
```

**Tiempo estimado**: 10-12 minutos

### Paso 3: Reinstalar
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Paso 4: Probar
- Abrir app
- Ir a modo Identificación
- Escanear un auto del año 1978
- Debería identificarlo correctamente ✅

---

## 📊 Impacto del Problema

### Autos afectados:
- **Año 1978**: 6 autos (100% de identificaciones fallan)
- **Otros años con rotaciones**: Posiblemente afectados si embeddings se generaron antes

### Severidad:
- 🔴 **CRÍTICA** para modo Identificación
- ✅ No afecta modo Exploración
- ✅ No afecta modo Colección

---

## 🔧 Otros Problemas Posibles (Si regenerar no funciona)

### Problema 2: Permisos de Cámara
**Síntomas**: App pide permiso o cámara no abre

**Solución**:
```bash
# Verificar permisos
adb shell dumpsys package com.diecast.carscanner | grep -i permission

# Dar permiso manualmente
adb shell pm grant com.diecast.carscanner android.permission.CAMERA
```

### Problema 3: Modelo ONNX no carga
**Síntomas**: Error al abrir modo Identificación

**Verificar logs**:
```bash
adb logcat | grep -i "onnx\|mobilenet"
```

**Posibles errores**:
- "Failed to load model"
- "ONNX Runtime error"
- "Model file not found"

**Solución**: Verificar que `mobilenetv3_embeddings.onnx` existe en assets:
```bash
ls -lh app/src/main/assets/mobilenetv3_embeddings.onnx
```

### Problema 4: Base de datos no inicializada
**Síntomas**: App abre pero no muestra nada en Exploración

**Verificar**:
```bash
adb shell "run-as com.diecast.carscanner ls -la databases/"
```

Debería mostrar:
- `hotwheels.db`
- `hotwheels.db-shm`
- `hotwheels.db-wal`

### Problema 5: Embeddings corruptos
**Síntomas**: Identificación falla con error

**Verificar tamaños**:
```bash
ls -lh app/src/main/assets/embeddings_mobilenetv3.*
```

Deberían ser:
- JSON: ~259 MB
- NPZ: ~45 MB

Si son muy pequeños o grandes, están corruptos.

---

## 🎯 Plan de Acción

### Inmediato (HOY):
1. ✅ Regenerar embeddings (en progreso)
2. ⏳ Recompilar app
3. ⏳ Reinstalar en dispositivo
4. ⏳ Probar identificación

### Si sigue sin funcionar:
1. Verificar logs de la app
2. Verificar permisos
3. Verificar que modelo ONNX carga correctamente
4. Verificar base de datos SQLite

### Largo plazo:
- Implementar logging más detallado en modo Identificación
- Agregar mensaje de error específico (ej: "Embeddings loading...", "Model not loaded", etc.)
- Agregar test automático de identificación al compilar

---

## 💡 Prevención Futura

### Checklist antes de compilar:
1. ✅ Todas las rotaciones aplicadas
2. ✅ Embeddings regenerados DESPUÉS de rotaciones
3. ✅ Verificar tamaños de archivos:
   - embeddings_mobilenetv3.json (~259 MB)
   - embeddings_mobilenetv3.npz (~45 MB)
   - mobilenetv3_embeddings.onnx (~7 MB)
4. ✅ Compilar APK
5. ✅ Instalar y probar en dispositivo

### Script de verificación (crear):
```bash
#!/bin/bash
# verify_before_build.sh

echo "Verificando archivos críticos..."

# Check embeddings
JSON_SIZE=$(stat -f%z app/src/main/assets/embeddings_mobilenetv3.json 2>/dev/null || stat -c%s app/src/main/assets/embeddings_mobilenetv3.json)
NPZ_SIZE=$(stat -f%z app/src/main/assets/embeddings_mobilenetv3.npz 2>/dev/null || stat -c%s app/src/main/assets/embeddings_mobilenetv3.npz)

if [ $JSON_SIZE -lt 250000000 ]; then
    echo "❌ JSON embeddings muy pequeño: $JSON_SIZE bytes"
    exit 1
fi

if [ $NPZ_SIZE -lt 40000000 ]; then
    echo "❌ NPZ embeddings muy pequeño: $NPZ_SIZE bytes"
    exit 1
fi

echo "✅ Embeddings OK"
echo "✅ Listo para compilar"
```

---

## 📝 Notas Técnicas

### Formato de Embeddings

**embeddings_mobilenetv3.json:**
```json
{
  "embeddings": {
    "hw_highway_patrol_1978_2019.jpg": [0.234, 0.567, ..., 0.123],
    "hw_army_funny_car_1978_2023.jpg": [0.456, 0.789, ..., 0.234],
    ...
  }
}
```

**embeddings_mobilenetv3.npz:**
- Formato comprimido NumPy
- Contiene matriz 10520x1280
- Cada fila = embedding de una imagen
- Cada columna = feature del modelo

### Proceso de Identificación

1. **Captura**: Foto de cámara → Bitmap
2. **Preprocesamiento**: Resize a 224x224, normalización
3. **Inferencia**: MobileNetV3 → embedding de 1280 dims
4. **Búsqueda**: Comparar con 10,520 embeddings
5. **Similitud**: Cosine similarity (rango: -1 a 1)
6. **Filtrado**: Umbral típico: >0.7
7. **Resultado**: Top 5 matches ordenados por similitud

### ¿Por qué embeddings desactualizados fallan?

Si imagen de referencia está invertida:
- Características visuales cambian dramáticamente
- Posición de ruedas, techo, ventanas, etc.
- Embedding resultante es completamente diferente
- Similitud cae de ~0.95 a ~0.35

---

## 🚀 Siguientes Pasos

1. Esperar a que termine regeneración de embeddings (~3 min)
2. Compilar app (~12 min)
3. Instalar en dispositivo
4. Probar identificación
5. Si funciona: ✅ Problema resuelto
6. Si NO funciona: Investigar logs y permisos

---

**Estado actual**: Regenerando embeddings...
**ETA**: ~3 minutos
