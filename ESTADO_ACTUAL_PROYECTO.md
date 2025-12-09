# Estado Actual del Proyecto - Hot Wheels Identifier
**Fecha**: 6 de Noviembre 2025
**Última sesión**: Preparación para revisión manual de rotaciones de imágenes

---

## 📋 Resumen Ejecutivo

El proyecto está en estado **LISTO PARA REVISIÓN MANUAL** de las 10,520 imágenes de referencia. Todas las imágenes fueron restauradas a su orientación original para que el usuario pueda revisarlas y rotarlas correctamente usando el explorador integrado en la app.

---

## 🎯 Estado Actual

### ✅ Completado en esta sesión:

1. **Imágenes restauradas al estado original**
   - Todas las 10,520 imágenes en `app/src/main/assets/reference_images/` están en su orientación original (desde git)
   - Sin rotaciones aplicadas
   - Listas para revisión manual

2. **Bug del checkbox arreglado**
   - Archivo: `app/src/main/java/com/hotwheels/identifier/ui/exploration/ExplorationAdapter.kt`
   - Línea 61: Agregado `getItemViewType(position): Int = position`
   - Ahora los checkboxes no se deseleccionan al seleccionar múltiples imágenes

3. **Fix de orientación EXIF para fotos capturadas**
   - Archivo: `app/src/main/java/com/hotwheels/identifier/ml/MobileNetIdentifier.kt`
   - Línea 251-253: Modificado `generateEmbedding()` para usar `loadBitmapWithCorrectOrientation()`
   - Línea 327-356: Nueva función que respeta metadatos EXIF de fotos de cámara
   - Esto soluciona el bug de identificación en reintentos

4. **Embeddings regenerados con imágenes originales**
   - 10,520 embeddings generados
   - Reducidos a 3 decimales de precisión (83.6 MB)
   - Formato correcto para Android
   - Archivo: `app/src/main/assets/embeddings_mobilenetv3.json`

5. **APK compilado e instalado**
   - Explorador de imágenes funcionando
   - Sistema de rotación y logging operativo
   - Listo para revisión manual

---

## 📂 Archivos Importantes

### Código Modificado:
```
app/src/main/java/com/hotwheels/identifier/ml/MobileNetIdentifier.kt
  - Fix EXIF orientation (líneas 251-253, 327-356)

app/src/main/java/com/hotwheels/identifier/ui/exploration/ExplorationAdapter.kt
  - Fix checkbox recycling (línea 61)
```

### Datos:
```
app/src/main/assets/embeddings_mobilenetv3.json (83.6 MB)
  - Embeddings con 3 decimales de precisión
  - Generados desde imágenes originales

app/src/main/assets/reference_images/**/*.jpg (10,520 imágenes)
  - Todas en orientación original (restauradas con git restore)
```

### Scripts de utilidad:
```
regenerate_embeddings.py
  - Regenera embeddings desde imágenes de referencia
  - Usa ONNX Runtime (CPU)
  - Tiempo: ~2.6 minutos para 10,520 imágenes

rotation_log_new.json
  - Log vacío listo para revisión manual
  - Contiene metadata con lista de todas las 10,520 imágenes
```

---

## 🔄 Flujo de Trabajo Actual

### Usuario debe hacer:
1. Abrir la app en el dispositivo Android
2. Ir al explorador de imágenes
3. Revisar las 10,520 imágenes y rotarlas según sea necesario
4. El sistema guarda automáticamente cada rotación
5. Al finalizar, extraer el log:
   ```bash
   adb pull /data/data/com.diecast.carscanner/files/rotation_log.json
   ```

### Después de obtener el log:
1. Aplicar rotaciones físicamente a las imágenes usando script Python
2. Regenerar embeddings con imágenes rotadas
3. Reducir precisión si es necesario (para evitar integer overflow)
4. Compilar e instalar APK actualizado

---

## 🛠️ Comandos Críticos

### Compilación:
```bash
cd /home/cristhyan/Escritorio/proy_h
./gradlew assembleDebug
```

### Instalación:
```bash
export PATH=$HOME/Android/Sdk/platform-tools:$PATH
adb uninstall com.diecast.carscanner
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Regenerar embeddings:
```bash
python3 regenerate_embeddings.py
```

### Reducir precisión de embeddings (si integer overflow):
```python
import json
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'r') as f:
    data = json.load(f)
for entry in data['embeddings']:
    entry['embedding'] = [round(v, 3) for v in entry['embedding']]
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'w') as f:
    json.dump(data, f, separators=(',', ':'))
```

### Restaurar imágenes al original:
```bash
git restore app/src/main/assets/reference_images/
```

### Aplicar rotaciones desde log:
```python
import json
from PIL import Image
from pathlib import Path

with open('rotation_log.json', 'r') as f:
    log = json.load(f)

base_path = Path('app/src/main/assets/reference_images')

for entry in log['rotations']:
    img_name = entry['image_name']
    degrees = entry['rotation_degrees']

    img_files = list(base_path.rglob(img_name))
    if img_files:
        img_path = img_files[0]
        img = Image.open(img_path)
        if img.mode in ('RGBA', 'LA', 'P'):
            img = img.convert('RGB')
        img_rotated = img.rotate(-degrees, expand=True)
        img_rotated.save(img_path, 'JPEG', quality=95, exif=b'')
```

---

## 🐛 Problemas Conocidos y Soluciones

### 1. Integer overflow al compilar APK
**Causa**: Archivo embeddings_mobilenetv3.json > 100 MB
**Solución**: Reducir decimales a 3 o menos (ver comando arriba)

### 2. Imágenes aparecen invertidas después de rotación
**Causa**: Dirección de rotación PIL vs expectativa del usuario
**Solución**: Usar `-degrees` en PIL.rotate() (negativo para clockwise)

### 3. Checkbox se deselecciona al seleccionar otros
**Causa**: RecyclerView recicla ViewHolders
**Solución**: ✅ RESUELTO - `getItemViewType(position): Int = position`

### 4. Retry bug - imágenes invertidas en segundo intento
**Causa**: EXIF metadata ignorado al generar embeddings
**Solución**: ✅ RESUELTO - `loadBitmapWithCorrectOrientation()`

---

## 📦 Dependencias del Sistema

### Android SDK:
- Platform tools (adb)
- Ruta: `$HOME/Android/Sdk/platform-tools`

### Python:
```bash
pip3 install pillow onnxruntime numpy
```

### Java:
- OpenJDK 17 (para compilar con Gradle)

---

## 🗂️ Estructura del Proyecto

```
proy_h/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── reference_images/     # 10,520 imágenes (ORIGINAL STATE)
│   │   │   ├── embeddings_mobilenetv3.json  # 83.6 MB, 3 decimals
│   │   │   ├── hotwheels.db          # Metadata de modelos
│   │   │   └── mobilenetv3_embeddings.onnx
│   │   └── java/com/hotwheels/identifier/
│   │       ├── ml/MobileNetIdentifier.kt  # Fix EXIF
│   │       └── ui/exploration/ExplorationAdapter.kt  # Fix checkbox
├── regenerate_embeddings.py
├── rotation_log_new.json   # Log vacío para revisión manual
└── ESTADO_ACTUAL_PROYECTO.md  # ESTE ARCHIVO
```

---

## 📝 Siguiente Sesión

### Al continuar el proyecto:

1. **Verificar estado de revisión manual**
   ```bash
   adb pull /data/data/com.diecast.carscanner/files/rotation_log.json
   ```

2. **Si el log está completo**:
   - Aplicar rotaciones a las imágenes
   - Regenerar embeddings
   - Compilar e instalar

3. **Si el log no existe o está incompleto**:
   - El usuario debe continuar revisando en la app
   - Las rotaciones ya guardadas persisten

---

## 🔑 Información Crítica para Recuperación

### Si se reinstala el sistema operativo:

1. **Clonar repositorio**:
   ```bash
   git clone [URL_REPO] proy_h
   cd proy_h
   ```

2. **Instalar dependencias**:
   ```bash
   # Python
   pip3 install pillow onnxruntime numpy

   # Java (para Gradle)
   sudo apt install openjdk-17-jdk

   # Android SDK
   # Descargar desde: https://developer.android.com/studio
   ```

3. **Configurar rutas**:
   ```bash
   export PATH=$HOME/Android/Sdk/platform-tools:$PATH
   ```

4. **Verificar estado**:
   - Leer este archivo (ESTADO_ACTUAL_PROYECTO.md)
   - Las imágenes deben estar en orientación original
   - El APK debe compilarse sin integer overflow

---

## 📊 Estadísticas del Proyecto

- **Total de imágenes**: 10,520
- **Imágenes revisadas manualmente**: 0 (pendiente)
- **Embeddings generados**: 10,520
- **Tamaño embeddings**: 83.6 MB (3 decimales)
- **Dimensión embeddings**: 1280
- **Modelos en database**: 10,934

---

## 🎯 Objetivo Final

Identificar correctamente Hot Wheels mostrando las imágenes de referencia en la orientación correcta (ruedas hacia abajo) tanto en el primer intento como en los reintentos.

**Estado**: ⏳ PENDIENTE REVISIÓN MANUAL DE ROTACIONES
