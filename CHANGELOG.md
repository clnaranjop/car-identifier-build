# 📝 Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [2.0.1] - 2025-10-28

### 🐛 Corregido
- **[CRÍTICO]** Imágenes capturadas se borraban antes de poder reutilizarlas (Fix completo en 2 partes)
  - **Parte 1:** Cambiado almacenamiento de `cacheDir` a `filesDir/captured_images/`
  - **Parte 2:** Removido `resetCaptureSession()` prematuro que borraba las rutas
  - Agregada limpieza automática de imágenes >7 días para evitar llenar almacenamiento
  - Ahora las fotos persisten correctamente entre búsquedas
  - Funcionalidad de "retry con mismas fotos" funciona correctamente
  - Error "No hay fotos guardadas" resuelto
  - Afecta: `FileUtils.kt`, `CameraActivity.kt`

### 📚 Agregado
- Documentación completa de recuperación del proyecto (`RECOVERY.md`)
- Documentación de arquitectura (`ARCHITECTURE.md`)
- Changelog para seguimiento de cambios
- Script de inicialización `.init` para verificar configuración
- Funciones de utilidad en `FileUtils`:
  - `cleanupOldCapturedImages()` - Limpia imágenes antiguas
  - `getCapturedImages()` - Lista todas las imágenes capturadas

---

## [2.0.0] - 2025-10-12

### 🎉 Agregado - Versión Mayor
- **Machine Learning con MobileNetV3:**
  - Modelo ONNX Runtime para identificación visual
  - Base de datos de 11,257 modelos con embeddings
  - Cosine similarity para matching
  - Top 100 resultados por búsqueda
  - Soporte multi-imagen (2 ángulos)
  - Filtrado por año (2020-2025 por defecto)

- **Nuevas Pantallas:**
  - `SelectResultActivity` - Selección de modelo correcto entre múltiples resultados
  - `ModelDetailsActivity` - Detalles completos del modelo

- **Funcionalidades:**
  - Búsqueda manual por nombre
  - Reintentar búsqueda con exclusión de modelos incorrectos
  - Grabación de video con extracción de frames
  - Captura multi-foto con guías visuales
  - Soporte multiidioma (Español/Inglés)
  - Importar/Exportar colección (JSON)
  - Seguimiento de precios

### 🔄 Cambiado
- Reemplazado sistema de identificación ORB por MobileNetV3
- Redesign completo con Material Design 3
- Mejorada UI de colección con badges modernos
- Optimizada carga de modelo ML (Singleton pattern)
- Arquitectura MVVM completa con ViewModels

### 🗑️ Deprecado
- Código ORB movido a `.deprecated_orb/`
- OpenCV ya no se usa para identificación (solo para procesamiento básico)

---

## [1.0.3] - 2025-10-07

### 🐛 Corregido
- Crash en `HotWheelsApplication` al guardar idioma
- Mejoras en estabilidad general
- Fix en permisos de cámara en Android 13+

### 🔄 Cambiado
- Actualizado `compileSdk` a 35
- Actualizado `targetSdk` a 35
- Dependencias actualizadas

---

## [1.0.2] - 2025-09-15

### 🎨 Cambiado
- Nuevo ícono de la aplicación
- Mejoras en splash screen
- Tema actualizado con colores Hot Wheels

### 🐛 Corregido
- Fix en rotación EXIF de imágenes
- Mejoras en crop de imágenes

---

## [1.0.1] - 2025-08-20

### 🐛 Corregido
- ANR (Application Not Responding) al cargar base de datos
- Optimización de consultas a base de datos
- Fix en memoria al cargar imágenes grandes

### 🔄 Cambiado
- Mejorada performance de `CollectionActivity`
- Optimizada carga de imágenes de referencia

---

## [1.0.0] - 2025-07-15

### 🎉 Release Inicial

#### ✨ Características
- **Identificación básica con OpenCV:**
  - Detección de características ORB
  - Template matching
  - Análisis de formas y contornos
  - Base de datos inicial de 8 modelos

- **Pantallas principales:**
  - MainActivity - Dashboard con estadísticas
  - CameraActivity - Captura de fotos
  - ResultActivity - Mostrar resultado de identificación
  - CollectionActivity - Gestión de colección personal
  - SettingsActivity - Configuración básica

- **Funcionalidades:**
  - Captura con CameraX
  - Almacenamiento local con SharedPreferences
  - AdMob para monetización
  - Material Design básico

#### 🛠️ Tecnologías
- Kotlin como lenguaje principal
- OpenCV 4.8.0 para visión computacional
- CameraX para captura de imágenes
- MVVM architecture pattern
- SharedPreferences para persistencia

---

## Tipos de Cambios

- `🎉 Agregado` - Nueva funcionalidad
- `🔄 Cambiado` - Cambios en funcionalidad existente
- `🗑️ Deprecado` - Funcionalidad que será removida
- `🐛 Corregido` - Bug fixes
- `🔒 Seguridad` - Vulnerabilidades corregidas
- `📚 Documentación` - Cambios en documentación

---

## Versionado

El proyecto sigue [Semantic Versioning](https://semver.org/):

- **MAJOR** (X.0.0): Cambios incompatibles con versiones anteriores
- **MINOR** (0.X.0): Nueva funcionalidad compatible con versiones anteriores
- **PATCH** (0.0.X): Bug fixes compatibles con versiones anteriores

**Ejemplo:**
- `1.0.0` → `1.1.0`: Agregada nueva funcionalidad (búsqueda por nombre)
- `1.1.0` → `1.1.1`: Corregido bug en búsqueda
- `1.1.1` → `2.0.0`: Cambio de ORB a MobileNetV3 (breaking change)

---

## Roadmap

### Próxima Versión (2.1.0)
- [ ] Room Database (migrar de SharedPreferences)
- [ ] Backup automático en la nube
- [ ] Compartir colección en redes sociales
- [ ] Estadísticas avanzadas de colección
- [ ] Modo oscuro

### Versión Futura (2.2.0)
- [ ] Reconocimiento de texto en el auto (OCR)
- [ ] Detección de variantes de color
- [ ] Base de datos expandida (15,000+ modelos)
- [ ] Marketplace integrado

### Versión Futura (3.0.0)
- [ ] Jetpack Compose migration
- [ ] Realidad aumentada para identificación
- [ ] Red social de coleccionistas
- [ ] Sincronización multi-dispositivo
- [ ] Progressive Web App (PWA)

---

## Soporte de Versiones

| Versión | Soportada | Fecha de Release | Fecha Fin de Soporte |
|---------|-----------|------------------|----------------------|
| 2.0.x   | ✅ Sí     | 2025-10-12       | TBD                  |
| 1.0.x   | ⚠️ Limitado| 2025-07-15      | 2025-12-31           |

---

**Última actualización:** 2025-10-28
