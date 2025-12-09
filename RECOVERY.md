# 🔧 Guía de Recuperación del Proyecto

Este documento describe cómo recuperar el proyecto completo en caso de pérdida del entorno de desarrollo (PC dañada, cambio de Linux, etc.).

## 📋 Tabla de Contenidos

- [Requisitos del Sistema](#requisitos-del-sistema)
- [Recuperación Rápida](#recuperación-rápida)
- [Recuperación Manual](#recuperación-manual)
- [Archivos Críticos](#archivos-críticos)
- [Troubleshooting](#troubleshooting)

---

## 🖥️ Requisitos del Sistema

### Sistema Operativo
- Ubuntu 20.04+ o cualquier distribución Linux basada en Debian
- Mínimo 8GB RAM
- Mínimo 20GB de espacio libre

### Software Necesario
1. **Java JDK 17**
2. **Android Studio** (o Android SDK)
3. **Git**
4. **ADB** (Android Debug Bridge)

---

## ⚡ Recuperación Rápida

Si tienes el APK instalado en un dispositivo Android, puedes recuperar todo:

### Paso 1: Instalar Herramientas Básicas

```bash
# Actualizar sistema
sudo apt update

# Instalar Java 17
sudo apt install openjdk-17-jdk

# Configurar JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Instalar ADB
sudo apt install adb

# Instalar Git
sudo apt install git
```

### Paso 2: Clonar Repositorio

```bash
# Clonar el proyecto
git clone https://github.com/TU-USUARIO/proy_h.git
cd proy_h
```

### Paso 3: Instalar Android Studio

```bash
# Opción 1: Con snap
sudo snap install android-studio --classic

# Opción 2: Descargar manualmente
# https://developer.android.com/studio
```

### Paso 4: Configurar Android SDK

```bash
# Después de instalar Android Studio, configurar local.properties
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

### Paso 5: Extraer Archivos ML desde APK Instalado

```bash
# Conectar dispositivo Android con USB (habilitar depuración USB)
adb devices

# Buscar la app instalada
adb shell pm list packages | grep diecast

# Obtener ruta del APK
adb shell pm path com.diecast.carscanner

# Extraer APK
adb pull /data/app/.../base.apk hotwheels.apk

# Descomprimir APK
unzip -q hotwheels.apk -d extracted_apk

# Copiar archivos ML al proyecto
cp extracted_apk/assets/mobilenetv3_embeddings.onnx app/src/main/assets/
cp extracted_apk/assets/embeddings_mobilenetv3.json app/src/main/assets/
cp extracted_apk/assets/embeddings_mobilenetv3.npz app/src/main/assets/

# Limpiar archivos temporales
rm hotwheels.apk
rm -rf extracted_apk
```

### Paso 6: Compilar Proyecto

```bash
# Dar permisos de ejecución a gradlew
chmod +x gradlew

# Compilar
./gradlew assembleDebug

# El APK estará en: app/build/outputs/apk/debug/app-debug.apk
```

### Paso 7: Ejecutar Script de Inicialización

```bash
# El script .init verifica que todo esté configurado correctamente
chmod +x .init
./.init
```

---

## 🔍 Recuperación Manual

### Si NO tienes el APK instalado

Los archivos ML son muy grandes (>300 MB) y no están en el repositorio Git. Necesitas:

1. **Archivos de respaldo externos:**
   - Mantén una copia de `app/src/main/assets/` en Google Drive, Dropbox, o disco externo
   - Los archivos críticos son:
     - `mobilenetv3_embeddings.onnx` (17 MB)
     - `embeddings_mobilenetv3.json` (293 MB)
     - `embeddings_mobilenetv3.npz` (55 MB)
     - `hotwheels.db` (5.3 MB)
     - `hotwheels_models.json` (5.2 MB)

2. **Recrear desde cero:**
   - Entrenar nuevo modelo MobileNetV3 con dataset de Hot Wheels
   - Generar embeddings para cada modelo
   - Crear base de datos SQLite con metadatos

---

## 📁 Archivos Críticos

### Archivos que DEBEN estar en Respaldo

```
proy_h/
├── app/src/main/assets/          # ⚠️ CRÍTICO - No están en Git
│   ├── mobilenetv3_embeddings.onnx
│   ├── embeddings_mobilenetv3.json
│   ├── embeddings_mobilenetv3.npz
│   ├── hotwheels.db
│   ├── hotwheels_models.json
│   ├── metadata.json
│   └── reference_images/          # 1.5 GB de imágenes
│
├── debug.keystore                 # Para firma debug consistente
├── diecast-release.keystore      # Para firma de release (SI LO PIERDES, NO PUEDES ACTUALIZAR LA APP EN PLAY STORE)
└── local.properties              # Se genera automáticamente
```

### Dónde hacer Respaldo

1. **Google Drive / Dropbox:**
   ```bash
   # Comprimir assets
   tar -czf hotwheels_assets_backup.tar.gz app/src/main/assets/

   # Subir a la nube manualmente
   ```

2. **Disco Externo:**
   ```bash
   # Copiar directamente
   cp -r app/src/main/assets/ /media/disco_externo/hotwheels_backup/
   ```

3. **Otro Dispositivo:**
   ```bash
   # Transferir por red
   scp -r app/src/main/assets/ usuario@otro-pc:/ruta/backup/
   ```

---

## 🛠️ Troubleshooting

### Problema: "SDK location not found"

```bash
# Solución: Crear local.properties
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

### Problema: "JAVA_HOME is not set"

```bash
# Verificar Java
java -version

# Si no está instalado
sudo apt install openjdk-17-jdk

# Configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
```

### Problema: "Gradle build failed"

```bash
# Limpiar y rebuildar
./gradlew clean
./gradlew assembleDebug --stacktrace
```

### Problema: "Permission denied" al ejecutar gradlew

```bash
# Dar permisos
chmod +x gradlew
```

### Problema: "ADB device unauthorized"

```bash
# En el celular: Aceptar el diálogo "Permitir depuración USB"
# Luego:
adb kill-server
adb start-server
adb devices
```

### Problema: Faltan archivos ML

```bash
# Si tienes el APK instalado, extráelo:
adb shell pm path com.diecast.carscanner
adb pull /data/app/.../base.apk .
unzip base.apk -d extracted
cp extracted/assets/*.onnx app/src/main/assets/
cp extracted/assets/*.json app/src/main/assets/
```

---

## 📦 Estructura de Respaldo Recomendada

```
hotwheels_backup/
├── codigo_fuente/
│   └── proy_h/                    # Clon del repositorio Git
│
├── assets_ml/
│   ├── mobilenetv3_embeddings.onnx
│   ├── embeddings_mobilenetv3.json
│   ├── embeddings_mobilenetv3.npz
│   └── reference_images/
│
├── keystores/
│   ├── debug.keystore
│   └── diecast-release.keystore   # ⚠️ MUY IMPORTANTE
│
├── apks/
│   ├── app-debug-v2.0.0.apk
│   └── app-release-v2.0.0.apk
│
└── docs/
    ├── RECOVERY.md
    ├── ARCHITECTURE.md
    └── CHANGELOG.md
```

---

## 🔐 Seguridad del Keystore

**⚠️ CRÍTICO:** Si pierdes `diecast-release.keystore`, **NO PODRÁS** actualizar la app en Google Play Store.

### Hacer Respaldo del Keystore

```bash
# Copiar a lugar seguro
cp diecast-release.keystore ~/Dropbox/hotwheels_backup/
cp diecast-release.keystore /media/disco_externo/backups/

# Información del keystore:
# - Archivo: diecast-release.keystore
# - Password: DiecastScanner2025!
# - Alias: diecastscanner
# - Key Password: DiecastScanner2025!
```

**NUNCA** subas el keystore a Git o repositorios públicos.

---

## 📞 Contacto

Si tienes problemas recuperando el proyecto:
1. Revisa los logs de error completos
2. Verifica que todos los requisitos estén instalados
3. Consulta este documento de nuevo
4. Busca en los issues del repositorio

---

## ✅ Checklist de Recuperación

- [ ] Java 17 instalado y JAVA_HOME configurado
- [ ] Android Studio o Android SDK instalado
- [ ] Git instalado
- [ ] ADB instalado
- [ ] Repositorio clonado
- [ ] Archivos ML copiados a `app/src/main/assets/`
- [ ] `local.properties` creado con ruta del SDK
- [ ] Proyecto compila exitosamente (`./gradlew assembleDebug`)
- [ ] APK generado en `app/build/outputs/apk/debug/`
- [ ] Script `.init` ejecutado sin errores

---

**Última actualización:** 2025-10-28
**Versión del proyecto:** 2.0.0
