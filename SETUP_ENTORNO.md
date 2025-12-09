# 🚀 Setup del Entorno - Hot Wheels Identifier

Este documento contiene las instrucciones completas para configurar el entorno de desarrollo desde cero en caso de reinstalación del sistema operativo.

---

## 📋 Requisitos Previos

### Sistema Operativo
- Linux (Ubuntu/Debian recomendado)
- 8 GB RAM mínimo
- 20 GB espacio libre en disco

---

## 🔧 Instalación de Dependencias

### 1. Java Development Kit (JDK 17)

```bash
# Instalar OpenJDK 17
sudo apt update
sudo apt install openjdk-17-jdk -y

# Verificar instalación
java -version
# Debe mostrar: openjdk version "17.x.x"
```

### 2. Python y Librerías

```bash
# Instalar Python 3
sudo apt install python3 python3-pip -y

# Instalar librerías necesarias
pip3 install pillow onnxruntime numpy

# Verificar instalación
python3 -c "import PIL, onnxruntime, numpy; print('OK')"
```

### 3. Git

```bash
sudo apt install git -y
git --version
```

### 4. Android SDK y Platform Tools

#### Opción A: Instalar Android Studio (Recomendado)

1. Descargar desde: https://developer.android.com/studio
2. Instalar siguiendo el asistente
3. Abrir Android Studio > SDK Manager
4. Instalar:
   - Android SDK Platform-Tools
   - Android SDK Build-Tools
   - Android SDK Platform (API 34 o superior)

#### Opción B: Solo Command Line Tools

```bash
# Crear directorio
mkdir -p $HOME/Android/Sdk
cd $HOME/Android/Sdk

# Descargar Command Line Tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip

# Extraer
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/

# Instalar platform-tools
cmdline-tools/latest/bin/sdkmanager "platform-tools"
```

### 5. Configurar Variables de Entorno

Agregar al archivo `~/.bashrc`:

```bash
# Android SDK
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# Java (si no se detecta automáticamente)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

Aplicar cambios:
```bash
source ~/.bashrc
```

Verificar:
```bash
adb --version
# Debe mostrar la versión de adb
```

---

## 📥 Clonar y Configurar Proyecto

### 1. Clonar Repositorio

```bash
# Navegar al directorio deseado
cd ~/Escritorio

# Clonar repositorio
git clone [URL_DEL_REPOSITORIO] proy_h
cd proy_h
```

### 2. Verificar Estado del Proyecto

```bash
# Ver estado de git
git status

# Leer documento de estado actual
cat ESTADO_ACTUAL_PROYECTO.md
```

### 3. Verificar Archivos Críticos

```bash
# Verificar imágenes de referencia
ls -lh app/src/main/assets/reference_images/ | head

# Verificar embeddings
ls -lh app/src/main/assets/embeddings_mobilenetv3.json

# Verificar scripts
ls -lh *.py
```

---

## 🔨 Compilar el Proyecto

### 1. Primera Compilación

```bash
cd /home/cristhyan/Escritorio/proy_h

# Dar permisos de ejecución a Gradle wrapper
chmod +x gradlew

# Compilar (puede tardar varios minutos la primera vez)
./gradlew assembleDebug
```

### 2. Solucionar Problemas Comunes

#### Error: "JAVA_HOME is not set"
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
```

#### Error: "integer overflow"
El archivo embeddings es demasiado grande. Reducir precisión:
```bash
python3 << 'EOF'
import json
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'r') as f:
    data = json.load(f)
for entry in data['embeddings']:
    entry['embedding'] = [round(v, 3) for v in entry['embedding']]
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'w') as f:
    json.dump(data, f, separators=(',', ':'))
print("Reducido a 3 decimales")
EOF

./gradlew assembleDebug
```

---

## 📱 Conectar Dispositivo Android

### 1. Habilitar Opciones de Desarrollador

En el dispositivo Android:
1. Ir a Ajustes > Acerca del teléfono
2. Tocar "Número de compilación" 7 veces
3. Volver a Ajustes > Opciones de desarrollador
4. Activar "Depuración USB"

### 2. Conectar y Verificar

```bash
# Conectar dispositivo por USB

# Verificar conexión
adb devices

# Debe mostrar:
# List of devices attached
# AB5XVB3A13000834    device
```

### 3. Solucionar "no devices/emulators found"

```bash
# Reiniciar servidor ADB
adb kill-server
adb start-server

# Verificar de nuevo
adb devices

# Si aún no aparece, verificar cable USB y permisos en el teléfono
```

---

## 🚀 Instalar APK en Dispositivo

```bash
# Desinstalar versión anterior (si existe)
adb uninstall com.diecast.carscanner

# Instalar nueva versión
adb install app/build/outputs/apk/debug/app-debug.apk

# Debe mostrar: Success
```

---

## 🔄 Flujo de Trabajo Completo

### Desarrollo Normal:

```bash
# 1. Hacer cambios en el código
# 2. Compilar
./gradlew assembleDebug

# 3. Instalar
adb uninstall com.diecast.carscanner && adb install app/build/outputs/apk/debug/app-debug.apk

# 4. Probar en dispositivo
```

### Regenerar Embeddings:

```bash
# 1. Modificar imágenes si es necesario
# 2. Regenerar embeddings
python3 regenerate_embeddings.py

# 3. Si el archivo es > 100 MB, reducir precisión
python3 << 'EOF'
import json
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'r') as f:
    data = json.load(f)
for entry in data['embeddings']:
    entry['embedding'] = [round(v, 3) for v in entry['embedding']]
with open('app/src/main/assets/embeddings_mobilenetv3.json', 'w') as f:
    json.dump(data, f, separators=(',', ':'))
EOF

# 4. Compilar e instalar
./gradlew assembleDebug
adb uninstall com.diecast.carscanner && adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📊 Verificación del Entorno

Script de verificación completo:

```bash
#!/bin/bash

echo "=== Verificación del Entorno ==="
echo ""

# Java
echo -n "Java: "
if command -v java &> /dev/null; then
    java -version 2>&1 | head -1
else
    echo "❌ NO INSTALADO"
fi

# Python
echo -n "Python: "
if command -v python3 &> /dev/null; then
    python3 --version
else
    echo "❌ NO INSTALADO"
fi

# Git
echo -n "Git: "
if command -v git &> /dev/null; then
    git --version
else
    echo "❌ NO INSTALADO"
fi

# ADB
echo -n "ADB: "
if command -v adb &> /dev/null; then
    adb --version 2>&1 | head -1
else
    echo "❌ NO INSTALADO"
fi

# Librerías Python
echo ""
echo "Librerías Python:"
python3 -c "import PIL; print('✅ Pillow')" 2>/dev/null || echo "❌ Pillow"
python3 -c "import onnxruntime; print('✅ ONNX Runtime')" 2>/dev/null || echo "❌ ONNX Runtime"
python3 -c "import numpy; print('✅ NumPy')" 2>/dev/null || echo "❌ NumPy"

# Proyecto
echo ""
echo "Proyecto:"
if [ -d "app/src/main/assets/reference_images" ]; then
    count=$(find app/src/main/assets/reference_images -name "*.jpg" | wc -l)
    echo "✅ Imágenes de referencia: $count"
else
    echo "❌ Imágenes de referencia no encontradas"
fi

if [ -f "app/src/main/assets/embeddings_mobilenetv3.json" ]; then
    size=$(du -h app/src/main/assets/embeddings_mobilenetv3.json | cut -f1)
    echo "✅ Embeddings: $size"
else
    echo "❌ Embeddings no encontrados"
fi

echo ""
echo "=== Fin de Verificación ==="
```

Guardar como `verificar_entorno.sh` y ejecutar:
```bash
chmod +x verificar_entorno.sh
./verificar_entorno.sh
```

---

## 🆘 Soporte y Troubleshooting

### ADB no detecta dispositivo
1. Verificar cable USB (debe soportar datos, no solo carga)
2. En el teléfono: Verificar que depuración USB esté habilitada
3. Probar con: `adb kill-server && adb start-server`

### Gradle no encuentra Java
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Error de memoria al compilar
```bash
# Editar gradle.properties
echo "org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=1024m" >> gradle.properties
```

### Embeddings demasiado grandes
Ver sección "Error: integer overflow" arriba

---

## 📚 Recursos Adicionales

- Android Developer: https://developer.android.com/
- Gradle: https://gradle.org/
- ONNX Runtime: https://onnxruntime.ai/
- PIL/Pillow: https://pillow.readthedocs.io/

---

## ✅ Checklist de Setup Completo

- [ ] Java 17 instalado
- [ ] Python 3 y librerías (pillow, onnxruntime, numpy)
- [ ] Git instalado
- [ ] Android SDK y platform-tools
- [ ] Variables de entorno configuradas
- [ ] Repositorio clonado
- [ ] Proyecto compila sin errores
- [ ] Dispositivo Android conectado y detectado por ADB
- [ ] APK instalado exitosamente en dispositivo
- [ ] App abre sin errores

---

**Última actualización**: 6 de Noviembre 2025
