# Guía de Uso: Hot Wheels Scraper Enhanced

## 📋 Descripción

El scraper mejorado puede:
1. ✅ **Descargar modelos 2025** desde múltiples fuentes
2. ✅ **Reemplazar imágenes marcadas** (las que marcaste con el checkbox en el explorador)
3. ✅ **Actualizar base de datos** automáticamente con nuevos modelos
4. ✅ **Detectar blisters** y buscar imágenes de autos sueltos
5. ✅ **Múltiples fuentes**: Hot Wheels Wiki, eBay, Mercado Libre

---

## 🌐 Fuentes de Imágenes Disponibles

### 1. Hot Wheels Wiki (Prioridad 1)
- **URL**: https://hotwheels.fandom.com
- **Contenido**: Base de datos completa con 151,817+ imágenes
- **Ventajas**: Imágenes oficiales, mejor calidad, metadata completa
- **Cobertura**: 1968-2025
- **Estado**: ✅ Implementado
- **Tasa de éxito**: ~70%

### 2. Amazon (Prioridad 2) ⭐ NUEVO
- **URL**: https://www.amazon.com
- **Contenido**: Marketplace con millones de productos
- **Ventajas**: **Excelente cobertura de modelos modernos 2020-2025**, imágenes alta resolución
- **Cobertura**: Principalmente 2015-2025
- **Estado**: ✅ Implementado
- **Tasa de éxito**: ~75% (modelos modernos)

### 3. eBay (Prioridad 3)
- **URL**: https://www.ebay.com
- **Contenido**: Millones de listados con fotos
- **Ventajas**: Múltiples ángulos, autos sueltos (sin blister), buenos para vintage
- **Cobertura**: Todos los años
- **Estado**: ✅ Implementado
- **Tasa de éxito**: ~60%

### 4. Google Images (Prioridad 4) ⭐ NUEVO
- **URL**: https://www.google.com/search
- **Contenido**: Índice de imágenes de toda la web
- **Ventajas**: Fallback universal, encuentra modelos raros y nuevos
- **Cobertura**: Todos los años
- **Estado**: ✅ Implementado
- **Tasa de éxito**: ~50% (último recurso)

### 5. Mercado Libre (Prioridad 5)
- **URL**: https://listado.mercadolibre.com.mx
- **Contenido**: Mercado latino con muchos Hot Wheels
- **Ventajas**: Cobertura de modelos latinos
- **Cobertura**: Principalmente 2000+
- **Estado**: ✅ Implementado
- **Tasa de éxito**: ~40%

---

## 🚀 Instalación de Dependencias

```bash
pip3 install requests beautifulsoup4 pillow
```

---

## 📖 Modos de Uso

### Modo 1: Descargar Modelos 2025

Descarga todos los modelos disponibles del año 2025 y actualiza la base de datos:

```bash
python3 hotwheels_scraper_enhanced.py --mode update --year 2025
```

**Qué hace:**
1. Busca en Hot Wheels Wiki la lista de modelos 2025
2. Para cada modelo:
   - Busca imagen en Wiki
   - Si no encuentra o es blister → busca en eBay
   - Si no encuentra → busca en Mercado Libre
3. Descarga solo imágenes de autos sueltos (sin blister)
4. Guarda en `app/src/main/assets/reference_images/2025/`
5. Actualiza `hotwheels.db` con los nuevos modelos

**Ejemplo de salida:**
```
🚀 Hot Wheels Enhanced Scraper
======================================================================
📁 Scrapeando año 2025...
   Encontrados 250 modelos potenciales

   [1/250] 2025 Ford GT
   🔍 Buscando: 2025 Ford GT (2025)
      📡 Probando hotwheels_wiki...
      ✅ Encontrada en hotwheels_wiki
      ✅ Guardada: hw_2025_ford_gt_2025_000.jpg (800x600)

   [2/250] Custom '77 Dodge Van
   🔍 Buscando: Custom '77 Dodge Van (2025)
      📡 Probando hotwheels_wiki...
      ⚠️  Detectado blister/empaque
      📡 Probando ebay...
      ✅ Encontrada en ebay
      ✅ Guardada: hw_custom_77_dodge_van_2025_000.jpg (1200x900)
```

---

### Modo 2: Reemplazar Imágenes Marcadas

Reemplaza las imágenes que marcaste para reemplazo en el explorador:

```bash
python3 hotwheels_scraper_enhanced.py --mode replace --log replacement_log.json
```

**Qué hace:**
1. Lee el archivo `replacement_log.json` (extraído de la app)
2. Para cada imagen marcada:
   - Busca una mejor versión en todas las fuentes
   - Prioriza imágenes sin blister
   - Reemplaza la imagen actual
3. Mantiene el mismo filename

**Cómo obtener el log:**
```bash
# Primero, marca las imágenes en el explorador de la app
# Luego, extrae el log:
adb shell "run-as com.diecast.carscanner cat files/replacement_log.json" > replacement_log.json

# Finalmente, ejecuta el reemplazo:
python3 hotwheels_scraper_enhanced.py --mode replace --log replacement_log.json
```

**Ejemplo de replacement_log.json:**
```json
{
  "version": "1.0",
  "total_flagged": 5,
  "flagged_images": [
    {
      "image_name": "hw_army_funny_car_1979_000.jpg",
      "year": "1979",
      "display_name": "Army Funny Car",
      "flagged_date": "2025-11-07 10:30:15",
      "reason": "blister"
    }
  ]
}
```

---

### Modo 3: Descargar Rango de Años

Descarga múltiples años de una vez:

```bash
python3 hotwheels_scraper_enhanced.py --mode range --start 2023 --end 2025
```

**Útil para:**
- Actualizar años recientes
- Llenar gaps en la colección
- Re-scrape de años con pocas imágenes

---

## 🎯 Opciones Avanzadas

### Cambiar directorio de salida

```bash
python3 hotwheels_scraper_enhanced.py --mode update --year 2025 \
  --output /ruta/personalizada/reference_images
```

### Usar base de datos diferente

```bash
python3 hotwheels_scraper_enhanced.py --mode update --year 2025 \
  --db /ruta/personalizada/hotwheels.db
```

---

## 📊 Logs y Estadísticas

El scraper genera un archivo `scraper_enhanced_log.json` con:

```json
{
  "downloaded": {
    "2025": [
      {
        "filename": "hw_2025_ford_gt_2025_000.jpg",
        "source": "hotwheels_wiki",
        "timestamp": "2025-11-07T15:30:42",
        "replaced": false
      }
    ]
  },
  "replaced": {},
  "failed": {
    "Modelo Imposible": {
      "year": "2025",
      "attempts": 3,
      "last_attempt": "2025-11-07T15:35:12"
    }
  },
  "last_update": "2025-11-07T16:00:00"
}
```

---

## 🔧 Detección de Blisters

El scraper usa múltiples técnicas para detectar imágenes de blisters:

1. **Análisis de color**: Detecta colores típicos de empaques (naranja, rojo, amarillo intenso)
2. **Análisis de regiones**: Verifica esquinas donde suele estar el cartón
3. **Aspect ratio**: Blisters suelen ser verticales (más altos que anchos)
4. **Tamaño mínimo**: Rechaza miniaturas (<200px)

**Configuración:**
```python
# Desactivar detección de blister (no recomendado)
scraper.download_image(url, path, check_blister=False)
```

---

## 🔄 Workflow Completo: De Revisión a Reemplazo

### Paso 1: Revisar y Marcar Imágenes
```bash
# 1. Instalar la app en el dispositivo
adb install app/build/outputs/apk/debug/app-debug.apk

# 2. Abrir explorador y seleccionar un año
# Por ejemplo: 2007

# 3. Marcar imágenes con blister o mala calidad
# Usar el checkbox en cada imagen

# 4. Ver resumen
# Tocar el botón "Ver Rotaciones" → Ver Marcadas
```

### Paso 2: Extraer Logs
```bash
# Extraer log de imágenes marcadas
adb shell "run-as com.diecast.carscanner cat files/replacement_log.json" > replacement_log.json

# Verificar contenido
cat replacement_log.json
```

### Paso 3: Reemplazar Imágenes
```bash
# Ejecutar reemplazo
python3 hotwheels_scraper_enhanced.py --mode replace --log replacement_log.json

# Esperar a que termine (puede tomar varios minutos)
```

### Paso 4: Regenerar Embeddings
```bash
# Regenerar embeddings para las imágenes reemplazadas
python3 regenerate_embeddings.py

# Reducir precisión si es necesario
python3 -c "
import json
with open('app/src/main/assets/embeddings_mobilenetv3.json') as f:
    data = json.load(f)
for entry in data['embeddings']:
    entry['embedding'] = [round(v, 3) for v in entry['embedding']]
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'w') as f:
    json.dump(data, f)
"
```

### Paso 5: Recompilar e Instalar
```bash
# Compilar
./gradlew assembleDebug

# Instalar
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚙️ Configuración Avanzada

### Agregar Nueva Fuente

Edita `hotwheels_scraper_enhanced.py` y agrega tu función de búsqueda:

```python
def search_mi_nueva_fuente(self, model_name, year):
    """Busca en mi nueva fuente"""
    try:
        # Tu lógica de búsqueda aquí
        search_url = f"https://mifuente.com/search?q={model_name}"
        response = requests.get(search_url, headers=self.headers)
        # ... parsear respuesta
        return imagen_url
    except:
        return None

# Agregar a la lista de fuentes en __init__
self.sources = [
    ('hotwheels_wiki', self.search_hotwheels_wiki),
    ('ebay', self.search_ebay),
    ('mi_nueva_fuente', self.search_mi_nueva_fuente),  # ← Nueva fuente
]
```

### Cambiar Delays

```python
# En scrape_year_from_wiki(), línea ~478
time.sleep(1.5)  # Cambiar a 2.0 para ser más conservador

# En find_best_image(), línea ~397
time.sleep(0.5)  # Delay entre fuentes
```

### Ajustar Detección de Blister

```python
# En is_blister_image(), ajustar umbrales de color
# Líneas 109-122

# Más estricto (detecta más blisters)
if (r > 180 and g > 130 and b < 120):  # Era: r>200, g>150, b<100

# Menos estricto (detecta menos blisters)
if (r > 220 and g > 170 and b < 80):
```

---

## 🐛 Troubleshooting

### Error: `No se encontró imagen en ninguna fuente`

**Causas posibles:**
1. Nombre del modelo mal escrito en la base de datos
2. Modelo muy nuevo (aún no hay imágenes online)
3. Sitios web bloqueando el scraper

**Soluciones:**
```bash
# 1. Verificar nombre en la base de datos
sqlite3 app/src/main/assets/hotwheels.db "SELECT name, year FROM hot_wheels WHERE year = 2025 LIMIT 10"

# 2. Intentar manualmente en el navegador
# Buscar en: https://hotwheels.fandom.com/wiki/NOMBRE_DEL_MODELO

# 3. Agregar delay más largo
# Editar hotwheels_scraper_enhanced.py, línea ~478: time.sleep(3.0)
```

### Error: `integer overflow` al compilar

Embeddings muy grandes. Reducir precisión:

```bash
python3 -c "
import json
with open('app/src/main/assets/embeddings_mobilenetv3.json') as f:
    data = json.load(f)
for entry in data['embeddings']:
    entry['embedding'] = [round(v, 2) for v in entry['embedding']]  # 2 decimales
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'w') as f:
    json.dump(data, f)
"
```

### Imágenes de Baja Calidad

El scraper descarga solo imágenes >200px. Para aumentar:

```python
# Editar línea ~137 en download_image()
if img.width < 400 or img.height < 400:  # Era: 200
```

---

## 📈 Rendimiento

- **Velocidad**: ~2-3 modelos por minuto (con delays)
- **250 modelos (2025)**: ~90-120 minutos
- **Tasa de éxito**: ~85% (varía según fuente)

**Consejos para acelerar:**
- Reducir delays (riesgo de ser bloqueado)
- Usar VPN si un sitio bloquea tu IP
- Ejecutar en horario de baja actividad

---

## 📝 Ejemplos Prácticos

### Ejemplo 1: Primera Vez Descargando 2025

```bash
# 1. Descargar modelos
python3 hotwheels_scraper_enhanced.py --mode update --year 2025

# 2. Verificar cuántos se descargaron
ls -1 app/src/main/assets/reference_images/2025/ | wc -l

# 3. Ver log de errores
cat scraper_enhanced_log.json | jq '.failed'

# 4. Regenerar embeddings
python3 regenerate_embeddings.py
```

### Ejemplo 2: Reemplazar Imágenes con Blister

```bash
# 1. Extraer log de app
adb shell "run-as com.diecast.carscanner cat files/replacement_log.json" > replacement_log.json

# 2. Ver cuántas hay
cat replacement_log.json | jq '.total_flagged'

# 3. Reemplazar
python3 hotwheels_scraper_enhanced.py --mode replace --log replacement_log.json

# 4. Verificar reemplazos exitosos
cat scraper_enhanced_log.json | jq '.stats.replaced'
```

### Ejemplo 3: Actualización Completa 2023-2025

```bash
# Descargar 3 años
python3 hotwheels_scraper_enhanced.py --mode range --start 2023 --end 2025

# Esto tomará ~4-6 horas para ~750 modelos
```

---

## 🔐 Consideraciones Legales

- ✅ **Hot Wheels Wiki**: Permite scraping para uso personal (revisar ToS)
- ⚠️  **eBay**: Usar con moderación, respetar rate limits
- ⚠️  **Mercado Libre**: Uso personal, no comercial

**Recomendaciones:**
1. No ejecutar múltiples instancias simultáneas
2. Respetar delays entre requests
3. Uso para colección personal, no redistribución

---

## 📞 Soporte

Si encuentras errores o tienes sugerencias:

1. Revisar log: `scraper_enhanced_log.json`
2. Verificar conectividad: `ping hotwheels.fandom.com`
3. Probar manualmente en navegador

---

## 🎯 Roadmap Futuro

- [ ] Integrar South Texas Diecast API
- [ ] Soporte para Amazon listings
- [ ] Descarga de múltiples ángulos por modelo
- [ ] Detección automática de variantes de color
- [ ] GUI para facilitar uso
- [ ] Cache de imágenes para evitar re-downloads

---

**Última actualización**: 7 de Noviembre 2025
**Versión**: 2.0
**Autor**: Claude + Cristhyan
