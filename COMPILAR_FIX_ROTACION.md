# Compilar e Instalar - Fix de Imágenes Rotadas

## ✅ Cambios Completados

### Problema Original:
Al tomar foto de un carro en blister (horizontal), los resultados mostraban imágenes de referencia **rotadas verticalmente** (de lado).

### Solución Aplicada:
1. ✅ Rotadas **295 imágenes portrait** 90° en sentido horario
2. ✅ Regenerados **10,687 embeddings** en 3.3 minutos
3. ✅ Formato de embeddings verificado y corregido

### Detalles:
- **Imágenes rotadas:** 295 (de 480x640 → 640x480)
- **Embeddings regenerados:** 10,687 exitosos, 0 fallidos
- **Tiempo:** 3.3 minutos
- **Tamaño JSON:** 264 MB
- **Tamaño NPZ:** 46 MB
- **Formato:** Estructurado con metadata ✅

## 🚀 Comandos para Compilar e Instalar

```bash
cd ~/Escritorio/proy_h
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
export PATH=$PATH:$HOME/Android/Sdk/platform-tools
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🧪 Cómo Probar Después de Instalar

### Test 1: Foto de Blister Horizontal
1. Toma foto del mismo carro rosa en blister
2. **Resultado esperado:** Las imágenes de referencia deben aparecer **horizontales** (no de lado)
3. El match debe ser más preciso porque las imágenes están en la orientación correcta

### Test 2: Comparar con Screenshots Anteriores
En `imagenes_revision/` tienes 3 screenshots del problema:
- Screenshot_20251031_180041 - Resultados mostraban blisters verticales ❌
- Screenshot_20251031_180053 - Resultados mostraban blisters verticales ❌
- Screenshot_20251031_180102 - Resultados mostraban blisters verticales ❌

Después de instalar:
- Los mismos carros deben mostrar imágenes **horizontales** ✅

### Test 3: Verificar Orientación en Resultados
Cuando veas los resultados:
- Las imágenes deben tener el logo "Hot Wheels" **horizontal** (no vertical)
- Los blisters deben verse igual que cuando tomas la foto
- El carro debe estar en la misma orientación que tu foto

### Test 4: Probar con Diferentes Carros
1. Toma fotos de varios carros en blister
2. Verifica que **TODAS** las imágenes de referencia aparezcan horizontales
3. Si ves alguna imagen de lado → reportar cuál es

## 📊 Estadísticas Antes vs Después

### Antes (Primera Rotación):
- Rotadas: 267 imágenes
- **Problema:** Quedaron 295 imágenes sin rotar

### Ahora (Segunda Rotación):
- Rotadas: 295 imágenes adicionales
- **Total rotadas:** 267 + 295 = 562 imágenes
- **Landscape correctas:** 10,026 imágenes
- **Square (pueden ser correctas):** 366 imágenes

### Orientación Final:
- ❌ Portrait (alto > ancho): 0 imágenes
- ✅ Landscape (ancho > alto): 10,321 imágenes (96.6%)
- ⚠️ Square (similar): 366 imágenes (3.4%)

## 🔍 Por Qué Quedaron Imágenes Sin Rotar la Primera Vez

La primera rotación usó ratio < 0.7 (muy estricto):
```python
if aspect_ratio < 0.7:  # Solo capturó imágenes MUY verticales
```

La segunda rotación usó ratio < 0.9 (más inclusivo):
```python
if aspect_ratio < 0.9:  # Capturó TODAS las portrait
```

**Ejemplos:**
- 480x640 = ratio 0.75 → ❌ Primera pasada NO rotó
- 480x640 = ratio 0.75 → ✅ Segunda pasada SÍ rotó
- 455x576 = ratio 0.79 → ❌ Primera pasada NO rotó
- 455x576 = ratio 0.79 → ✅ Segunda pasada SÍ rotó

## 📝 Archivos Modificados

### Scripts Creados:
- `rotate_remaining_portrait_images.py` - Rotar 295 imágenes
- `regenerate_embeddings.py` - Regenerar embeddings
- `fix_embeddings_format.py` - Corregir formato JSON

### Imágenes Rotadas:
- 295 imágenes en `app/src/main/assets/reference_images/`
- Principalmente del año 2001 y otros años

### Embeddings:
- `embeddings_mobilenetv3.json` - 264 MB ✅ Formato estructurado
- `embeddings_mobilenetv3.npz` - 46 MB ✅ Versión comprimida

### Backups Creados:
- `embeddings_mobilenetv3_old.json` - Backup pre-rotación
- `embeddings_mobilenetv3_old.npz` - Backup NPZ
- `embeddings_mobilenetv3_broken.json` - Formato incorrecto descartado

## ✅ Resultado Esperado

Después de compilar e instalar:
- ✅ Imágenes de referencia aparecen **horizontales**
- ✅ No más imágenes "de lado" en los resultados
- ✅ Mejor precisión al identificar carros en blister
- ✅ Logos "Hot Wheels" se ven horizontales
- ✅ Orientación consistente con la foto tomada

## 🐛 Si Sigues Viendo Imágenes de Lado

Si después de instalar aún ves imágenes rotadas:
1. Toma screenshot del resultado
2. Anota el nombre del modelo que aparece de lado
3. Reportar para investigar esas imágenes específicas

Es posible que algunas imágenes "square" (366 imágenes con ratio ~1.0) necesiten rotarse también, pero primero hay que verificar caso por caso.

---

**Fecha:** 2 Noviembre 2025
**Cambio:** Rotación de 295 imágenes portrait + regeneración de embeddings
**Archivos:** 295 JPGs rotados + embeddings_mobilenetv3.json (10,687 embeddings)
**Estado:** ✅ Listo para compilar y probar
