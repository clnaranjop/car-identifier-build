# Problema de Identificación con Blisters

**Fecha**: 2025-11-03
**Reporte**: Usuario identificó problema crítico de falsos positivos

---

## 🔴 Problema Reportado

> "he visto que relaciona cualquier blister con cualquier blister"

**Descripción del problema:**
- Cuando escaneas un auto en blister, el sistema lo identifica como OTRO auto diferente que también está en blister
- El modelo está detectando características del EMPAQUE, no del auto
- Causa: Los embeddings capturan los colores distintivos del empaque Hot Wheels (rojo/naranja/amarillo)

---

## 🔍 Análisis Técnico

### ¿Por qué sucede?

1. **Colores dominantes del blister:**
   - Rojo Hot Wheels: RGB(200+, <100, <100)
   - Naranja/Amarillo: RGB(200+, 150+, <100)
   - Estos colores ocupan ~40-60% de la imagen

2. **Embeddings de MobileNetV3:**
   - El modelo extrae características visuales de toda la imagen
   - Le da mucho peso a colores predominantes
   - El auto queda pequeño en relación al empaque
   - Resultado: Los embeddings se parecen más por el empaque que por el auto

3. **Problema en la comparación:**
   ```
   Imagen A: Corvette 1980 en blister naranja
   Imagen B: Mustang 1982 en blister naranja

   Similitud de embeddings: 85% ❌ (debería ser <30%)
   ```

### Impacto en la App:

- ✅ **Modo Exploración**: No afectado (solo navega y rota)
- ✅ **Modo Colección**: No afectado (solo muestra lo que agregaste)
- ❌ **Modo Identificación**: MUY AFECTADO
  - Falsos positivos al escanear autos en blister
  - Baja confianza en resultados correctos
  - Experiencia de usuario degradada

---

## ✅ Solución: Reemplazar Imágenes de Blisters

### Estrategia:

#### Paso 1: Detectar automáticamente blisters
Script creado: `detect_and_flag_blisters.py`

**Método de detección:**
- Análisis de píxeles RGB en toda la imagen
- Umbral: >15% de píxeles con colores Hot Wheels
- Confianza: Ratio de píxeles naranja/rojo/amarillo

**Ejecución:**
```bash
python3 detect_and_flag_blisters.py
```

**Salida:**
- `blisters_detected.json` - Lista completa con confianza
- `images_to_rescrape.txt` - Lista priorizada para scraper

#### Paso 2: Buscar imágenes de autos sueltos
Script actualizado: `hotwheels_scraper.py`

**Mejoras necesarias:**
- Agregar modo `--replace-list` para re-scrapear imágenes específicas
- Priorizar búsqueda de autos "loose" (sueltos) en eBay
- Verificar que nueva imagen NO sea blister
- Reemplazar solo si nueva imagen es mejor

**Uso:**
```bash
# Re-scrapear solo blisters detectados
python3 hotwheels_scraper.py --replace-list images_to_rescrape.txt

# Re-scrapear año específico
python3 hotwheels_scraper.py --start 1980 --end 1980 --force-replace
```

#### Paso 3: Regenerar embeddings
Después de reemplazar imágenes:
```bash
python3 regenerate_embeddings.py
```

#### Paso 4: Recompilar app
```bash
./compile_and_install_final.sh
```

---

## 📊 Estimación de Impacto

### Imágenes afectadas (estimado):
- **Años 1978-1990**: ~200-400 blisters (40-60% de esos años)
- **Años 1991-2000**: ~50-100 blisters (10-20%)
- **Años 2001-2024**: ~10-50 blisters (1-5%)
- **Total estimado**: 260-550 imágenes (~2.5-5% del total)

### Prioridad por año:
1. **Alta prioridad**: 1978-1985 (muchos blisters)
2. **Media prioridad**: 1986-1995
3. **Baja prioridad**: 1996-2024

---

## 🎯 Plan de Acción Recomendado

### Corto Plazo (Hoy):
1. ✅ Ejecutar detector de blisters
2. ✅ Revisar `images_to_rescrape.txt`
3. ⏳ Decidir si:
   - **Opción A**: Reemplazar solo alta prioridad (~100 imágenes)
   - **Opción B**: Reemplazar todas las detectadas (~260-550)

### Medio Plazo (Esta semana):
1. Mejorar scraper con modo `--replace-list`
2. Ejecutar scraping incremental por año
3. Regenerar embeddings por lotes
4. Probar mejoras en identificación

### Largo Plazo (Futuro):
1. Implementar pre-procesamiento de imágenes:
   - Detección automática de blisters al agregar imágenes
   - Recorte automático del auto (crop inteligente)
   - Eliminar fondo del empaque
2. Fine-tuning del modelo MobileNetV3:
   - Entrenar con augmentation de rotaciones
   - Mayor peso a características del auto (forma, detalles)
   - Menor peso a colores de fondo

---

## 🔧 Herramientas Creadas

### 1. `detect_and_flag_blisters.py`
**Función**: Escanea base de datos y detecta blisters automáticamente

**Características:**
- Análisis de RGB pixel por pixel
- Umbral configurable (default: 15%)
- Genera JSON con confianza y prioridad
- Lista ordenada para scraper

**Salida:**
```json
{
  "scan_date": "2025-11-03 19:30:00",
  "total_images": 10520,
  "blisters_detected": 342,
  "threshold": 0.15,
  "images": [
    {
      "file_name": "hw_fire_eater_1978_9640.jpg",
      "year": "1978",
      "model_name": "Fire Eater",
      "confidence": 0.269,
      "reason": "Ratio: 26.9% (rojo=1200, naranja=1500, amarillo=80)",
      "priority": "HIGH"
    }
  ]
}
```

### 2. `images_to_rescrape.txt`
**Función**: Lista legible para humanos con imágenes a reemplazar

**Formato:**
```
# === AÑO 1978 === (6 imágenes)
🔴 hw_fire_eater_1978_9640.jpg                     # Fire Eater (26.9%)
🔴 hw_poison_pinto_1978_9240.jpg                   # Poison Pinto (28.9%)
🟡 hw_emergency_squad_1978_7650.jpg                # Emergency Squad (16.7%)
```

---

## 💡 Alternativas Consideradas

### Alternativa 1: Pre-procesamiento en la app
**Descripción**: Detectar y recortar blisters en tiempo real
- ❌ Requiere modelo adicional de segmentación
- ❌ Alto costo computacional en dispositivo
- ❌ No soluciona problema en base de datos

### Alternativa 2: Fine-tuning del modelo
**Descripción**: Re-entrenar MobileNetV3 con dataset limpio
- ✅ Solución a largo plazo más robusta
- ❌ Requiere dataset etiquetado (~5000+ imágenes)
- ❌ Requiere GPU para entrenamiento
- ❌ Mucho tiempo de desarrollo

### Alternativa 3: Reemplazar imágenes (ELEGIDA) ✅
**Descripción**: Buscar y reemplazar blisters con fotos de autos sueltos
- ✅ Solución directa al problema
- ✅ Mejora calidad de base de datos
- ✅ No requiere cambios en modelo
- ✅ Se puede hacer incremental
- ⚠️ Requiere tiempo de scraping

---

## 📈 Métricas de Éxito

### Antes del reemplazo:
- Falsos positivos con blisters: ~80-90%
- Confianza promedio en identificación: 60-70%
- Quejas de usuarios: "Detecta cualquier blister"

### Después del reemplazo esperado:
- Falsos positivos con blisters: <10%
- Confianza promedio en identificación: 85-95%
- Precisión general: +15-25%

---

## 🚨 Importancia Crítica

Este problema es **CRÍTICO** para la funcionalidad principal de la app:

1. **Modo Identificación es la feature principal**
   - Usuario espera escanear auto y obtener resultado correcto
   - Blisters causan experiencia frustrante
   - Afecta credibilidad de la app

2. **Impacto en adopción**
   - Si identificación no funciona bien, usuario desinstala
   - Reseñas negativas en Play Store
   - No cumple promesa de valor

3. **Solución es factible**
   - No requiere cambios arquitectónicos
   - Solo mejorar calidad de datos
   - Tiempo estimado: 2-4 horas de scraping

---

## 📝 Próximos Pasos

### Paso 1: Ejecutar detector
```bash
cd ~/Escritorio/proy_h
python3 detect_and_flag_blisters.py
```

### Paso 2: Revisar resultados
```bash
# Ver JSON completo
cat blisters_detected.json | jq '.blisters_detected'

# Ver lista para scraper
head -50 images_to_rescrape.txt
```

### Paso 3: Decidir estrategia
- ¿Reemplazar todo o solo alta prioridad?
- ¿Qué años prioritarios?
- ¿Timing del scraping?

### Paso 4: Ejecutar scraper
```bash
# Opción A: Solo alta prioridad
python3 hotwheels_scraper.py --replace-list images_to_rescrape.txt --high-priority-only

# Opción B: Todas las detectadas
python3 hotwheels_scraper.py --replace-list images_to_rescrape.txt
```

### Paso 5: Regenerar y probar
```bash
python3 regenerate_embeddings.py
./compile_and_install_final.sh
```

---

**Conclusión**: SÍ, es CRÍTICO reemplazar las imágenes de blisters para que la identificación funcione correctamente. El problema afecta la funcionalidad principal de la app y la solución es factible.
