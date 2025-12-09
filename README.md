# HotWheels Identifier 🏎️

Una aplicación Android para identificar automáticamente modelos de Hot Wheels usando visión computacional y machine learning.

**Versión actual:** 2.0.1
**Estado:** Producción estable
**Base de datos:** 11,257 modelos de Hot Wheels

## ✨ Características Principales

- ✅ **Machine Learning con MobileNetV3**: Identificación visual de alta precisión usando ONNX Runtime
- ✅ **Base de Datos Masiva**: 11,257 modelos de Hot Wheels con embeddings pre-calculados
- ✅ **Identificación Multi-Foto**: Captura 2 ángulos para mejor precisión
- ✅ **Top 100 Resultados**: Muestra los mejores matches con porcentaje de confianza
- ✅ **Búsqueda Manual**: Busca modelos por nombre si la identificación falla
- ✅ **Filtro por Año**: Optimiza búsqueda filtrando por rango de años (2020-2025 por defecto)
- ✅ **Video Recording**: Graba video de 5 segundos y extrae frames para identificación
- ✅ **Colección Personal**: Gestiona tu colección con cantidades, precios y notas
- ✅ **Importar/Exportar**: Backup de colección en formato JSON
- ✅ **Multiidioma**: Español e Inglés
- ✅ **Material Design 3**: Interfaz moderna y fluida
- ✅ **Persistencia de Imágenes**: Las fotos capturadas persisten para reutilización

## 🔧 Tecnologías Utilizadas

- **Kotlin 2.0.21** - Lenguaje principal
- **MobileNetV3 (ONNX Runtime 1.16.3)** - Machine Learning para identificación visual
- **OpenCV 4.5.3** - Procesamiento de imágenes (legacy, deprecado para identificación)
- **CameraX 1.4.0** - API moderna de cámara con soporte de video
- **Coroutines** - Programación asíncrona
- **StateFlow/LiveData** - Manejo de estado reactivo
- **MVVM Architecture** - Patrón arquitectónico
- **Material Design 3** - Diseño de interfaz
- **Glide 4.16.0** - Carga de imágenes
- **Gson 2.10.1** - Serialización JSON
- **AdMob 22.6.0** - Monetización

## 📋 Requisitos

- Android Studio Hedgehog (2023.3.1) o superior
- Android SDK API 21+ (Android 5.0)
- Dispositivo con cámara
- Mínimo 2GB RAM (recomendado 4GB)

## 🛠️ Instalación

### Opción 1: Setup Rápido (Recomendado)

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/proy_h.git
cd proy_h

# 2. Ejecutar script de inicialización
chmod +x .init
./.init
```

El script `.init` verificará que tienes todo lo necesario instalado.

### Opción 2: Setup Manual

#### Paso 1: Instalar Dependencias

```bash
# Instalar Java 17
sudo apt install openjdk-17-jdk

# Configurar JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Instalar Android Studio
sudo snap install android-studio --classic

# Instalar ADB (para instalar APK)
sudo apt install adb
```

#### Paso 2: Clonar Repositorio

```bash
git clone https://github.com/tu-usuario/proy_h.git
cd proy_h
```

#### Paso 3: Configurar Android SDK

```bash
# Crear archivo local.properties
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

#### Paso 4: Extraer Archivos ML (si no están en assets)

Los archivos ML son muy grandes (>300 MB) y no están en Git. Si tienes el APK instalado:

```bash
# Conectar dispositivo Android
adb devices

# Extraer APK
adb shell pm path com.diecast.carscanner
adb pull /data/app/.../base.apk hotwheels.apk

# Descomprimir y copiar assets
unzip -q hotwheels.apk -d extracted_apk
cp extracted_apk/assets/mobilenetv3_embeddings.onnx app/src/main/assets/
cp extracted_apk/assets/embeddings_mobilenetv3.json app/src/main/assets/

# Limpiar
rm hotwheels.apk
rm -rf extracted_apk
```

Ver [RECOVERY.md](RECOVERY.md) para instrucciones detalladas de recuperación.

### Paso 5: Compilar y Ejecutar
```bash
# Desde terminal (opcional)
./gradlew assembleDebug

# O usar Android Studio
# Build > Make Project
# Run > Run 'app'
```

## 📱 Uso de la Aplicación

1. **Pantalla Principal**: Visualiza estadísticas y accede a funciones principales
2. **Escanear**: Presiona "Scan HotWheel" para abrir la cámara
3. **Capturar**: Apunta a un Hot Wheels y toma la foto
4. **Identificar**: La app analiza la imagen usando OpenCV
5. **Resultado**: Ve la identificación y guárdala en tu colección

## 🧠 Algoritmos de Identificación

La aplicación utiliza múltiples técnicas de visión computacional:

### Análisis de Formas
- Detección de contornos con Canny Edge Detection
- Análisis de proporciones (aspect ratio)
- Identificación de formas geométricas características

### Detección de Características
- ORB (Oriented FAST and Rotated BRIEF) features
- Matching de descriptores
- Análisis de distribución de puntos clave

### Template Matching
- Comparación con plantillas de modelos conocidos
- Correlación normalizada
- Scoring basado en similitud

### Clasificación por Categorías
- **Sports Cars**: Proporciones alargadas (ratio > 2.2)
- **Muscle Cars**: Proporciones medias (1.8 - 2.5)
- **Trucks/SUVs**: Más cuadrados (< 2.0)
- **Fantasy**: Formas complejas y únicas

## 🎯 Precisión y Confianza

- **Umbral mínimo**: 35% de confianza
- **Alta confianza**: 70%+ para identificaciones seguras
- **Factores evaluados**:
  - Proporción del vehículo (25%)
  - Análisis de forma (20%)
  - Características específicas por categoría (20%)
  - Detección de ruedas (15%)
  - Serie y simetría (15%)

## 💰 Monetización

La aplicación incluye AdMob configurado para:
- Banner ads en pantalla principal
- Banner ads en resultados
- Banner ads en colección
- **Política**: Anuncios no intrusivos que no interrumpan la experiencia

## 🗃️ Base de Datos

### Modelos Incluidos:
- **Sports Cars**: Lamborghini Aventador, Ferrari 488 GTB, Porsche 911 GT3
- **Muscle Cars**: Dodge Challenger, Chevrolet Camaro, Ford Mustang GT
- **Trucks**: Ford F-150 Raptor, Jeep Wrangler
- **JDM**: Nissan Skyline GT-R, Toyota Supra, Honda Civic Type R
- **Electric**: Tesla Model S
- **Fantasy**: Bone Shaker, Shark Bite, Twin Mill

## 📊 Arquitectura del Proyecto

```
app/src/main/java/com/hotwheels/identifier/
├── data/
│   ├── database/     # Room database y inicializador
│   ├── entities/     # Entidades de datos
│   ├── dao/          # Data Access Objects
│   └── repository/   # Repository pattern
├── ui/
│   ├── main/         # Pantalla principal
│   ├── camera/       # Captura de imágenes
│   ├── result/       # Mostrar resultados
│   └── collection/   # Gestión de colección
├── viewmodel/        # ViewModels MVVM
├── ml/               # Algoritmos de identificación
└── utils/            # Utilidades generales
```

## 🧪 Testing

```bash
# Tests unitarios
./gradlew test

# Tests instrumentados
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

## 📈 Roadmap

### Versión 1.1
- [ ] Más modelos de Hot Wheels (50+ modelos)
- [ ] Mejoras en algoritmos de detección
- [ ] Funciones de búsqueda y filtrado
- [ ] Estadísticas avanzadas

### Versión 1.2
- [ ] Reconocimiento de texto en el auto
- [ ] Detección de variantes de color
- [ ] Exportar colección
- [ ] Modo offline completo

### Versión 2.0
- [ ] ML mejorado con TensorFlow Lite
- [ ] Realidad aumentada para identificación
- [ ] Red social de coleccionistas
- [ ] Marketplace integrado

## 🤝 Contribuir

1. Fork el proyecto
2. Crear branch para feature (`git checkout -b feature/AmazingFeature`)
3. Commit cambios (`git commit -m 'Add AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Abrir Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver `LICENSE` para más detalles.

## 👥 Autores

- **Tu Nombre** - *Desarrollo inicial* - [TuGitHub](https://github.com/tu-usuario)

## 🙏 Agradecimientos

- Mattel Inc. por crear Hot Wheels
- OpenCV community por las herramientas de visión computacional
- Google por Android y Material Design
- Comunidad de desarrolladores Android

---

## 📚 Documentación Adicional

- **[RECOVERY.md](RECOVERY.md)** - Guía completa de recuperación del proyecto en caso de pérdida
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Documentación detallada de arquitectura y decisiones de diseño
- **[CHANGELOG.md](CHANGELOG.md)** - Historial de cambios y versiones

## 📞 Soporte

¿Problemas o preguntas?

1. Revisa la [Guía de Recuperación](RECOVERY.md) si tienes problemas de configuración
2. Consulta la [Documentación de Arquitectura](ARCHITECTURE.md) para entender el código
3. Abre un [issue](https://github.com/tu-usuario/proy_h/issues) en GitHub

## 🔒 Importante - Respaldo de Archivos

**⚠️ CRÍTICO:** Los siguientes archivos NO están en Git por su tamaño:
- `app/src/main/assets/mobilenetv3_embeddings.onnx` (17 MB)
- `app/src/main/assets/embeddings_mobilenetv3.json` (293 MB)
- `app/src/main/assets/reference_images/` (1.5 GB)
- `diecast-release.keystore` (firma de release)

**Mantén un respaldo** de estos archivos en Google Drive, Dropbox o disco externo. Ver [RECOVERY.md](RECOVERY.md) para más detalles.

---

**¡Feliz coleccionismo! 🏁**