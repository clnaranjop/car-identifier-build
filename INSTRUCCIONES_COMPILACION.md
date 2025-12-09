# Instrucciones para Compilar e Instalar la App

## ⚠️ IMPORTANTE

No puedo compilar la app automáticamente desde este entorno porque **no tengo acceso a Java**. 
Necesitas compilarla manualmente desde tu sistema.

---

## 📋 PREPARACIÓN

### Archivos listos:
✅ `embeddings_mobilenetv3.json` - Formato correcto (275 MB)
✅ 11,132 imágenes rotadas y sin duplicados
✅ Código actualizado

### Verificar que el dispositivo está conectado:
```bash
~/Android/Sdk/platform-tools/adb devices
```

Deberías ver:
```
List of devices attached
AB5XVB3A13000834    device
```

---

## 🚀 OPCIÓN 1: ANDROID STUDIO (Recomendado)

Es la forma más confiable:

1. **Abrir Android Studio**

2. **Abrir el proyecto**:
   - File → Open
   - Navegar a: `/home/cristhyan/Escritorio/proy_h`
   - Hacer click en "OK"

3. **Esperar a que sincronice** (puede tardar unos minutos)

4. **Limpiar el proyecto**:
   - Build → Clean Project
   - Esperar a que termine

5. **Reconstruir todo**:
   - Build → Rebuild Project
   - Esperar a que compile (puede tardar 5-10 minutos por el tamaño de assets)

6. **Instalar en el dispositivo**:
   - Asegurarte que el dispositivo está conectado
   - Click en el botón ▶️ (Run) verde
   - O: Run → Run 'app'

---

## 🚀 OPCIÓN 2: TERMINAL CON ANDROID STUDIO INSTALADO

Si Android Studio está instalado, puedes compilar desde terminal:

```bash
cd /home/cristhyan/Escritorio/proy_h

# Exportar JAVA_HOME (Android Studio trae su propio JDK)
export JAVA_HOME=$HOME/android-studio/jbr
# O si está en otra ubicación:
# export JAVA_HOME=/opt/android-studio/jbr

export PATH=$JAVA_HOME/bin:$PATH

# Verificar Java
java -version

# Limpiar y compilar
./gradlew clean
./gradlew assembleDebug

# Instalar
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 OPCIÓN 3: SCRIPT AUTOMÁTICO (Si tienes Java 17 instalado)

```bash
cd /home/cristhyan/Escritorio/proy_h
./compile_and_install.sh
```

**Nota:** Este script requiere que Java 17 esté instalado en tu sistema.

---

## 🔍 VERIFICAR JAVA

Para saber dónde está Java:

```bash
# Buscar instalaciones de Java
find /opt -name "java" -type f 2>/dev/null
find $HOME -name "java" -type f 2>/dev/null | grep -E "jdk|jbr"

# Si Android Studio está instalado, su JDK suele estar en:
ls -la $HOME/android-studio/jbr/bin/java
# O:
ls -la /opt/android-studio/jbr/bin/java
```

---

## ⏱️ TIEMPOS ESPERADOS

- **Primera compilación**: 10-15 minutos
- **Compilaciones posteriores**: 2-5 minutos
- **Instalación**: 1-2 minutos
- **APK final**: ~2.3 GB (debido a las 11,132 imágenes incluidas)

---

## ✅ DESPUÉS DE INSTALAR

1. **Abrir la app** en el dispositivo

2. **Probar el Modo Exploración**:
   - Verificar que las imágenes están bien orientadas
   - No debería haber duplicados

3. **Probar la Identificación**:
   - Escanear un Hot Wheels
   - Tomar 2 fotos
   - **Debería identificarlo correctamente** (sin "No detecta el auto")

4. **Verificar resultados**:
   - Debe mostrar nombre, año y porcentaje de similitud
   - Las imágenes deben estar bien orientadas

---

## 🐛 SI HAY PROBLEMAS

### Error: "JAVA_HOME is not set"
Necesitas exportar JAVA_HOME apuntando a tu JDK:
```bash
export JAVA_HOME=/ruta/a/tu/jdk
export PATH=$JAVA_HOME/bin:$PATH
```

### Error: "Execution failed for task"
- Hacer `./gradlew clean` primero
- Verificar que tienes espacio en disco (el APK es grande)

### Error al instalar APK
```bash
# Desinstalar versión anterior
~/Android/Sdk/platform-tools/adb uninstall com.diecast.carscanner

# Reinstalar
~/Android/Sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk
```

### Sigue diciendo "No detecta el auto"
- Verificar que realmente compilaste con el nuevo embeddings
- Verificar fecha del APK: `ls -lh app/build/outputs/apk/debug/app-debug.apk`
- Limpiar datos de la app en el dispositivo

---

## 📦 TAMAÑO DEL APK

El APK es **muy grande (~2.3 GB)** porque incluye:
- 11,132 imágenes de referencia
- Embeddings de 275 MB
- Modelo ONNX de 17 MB

**Nota:** Esto es normal para una app con una base de datos de imágenes tan grande.

---

## 📞 SI NECESITAS AYUDA

Si tienes problemas, avísame y te ayudo a:
1. Encontrar JAVA_HOME en tu sistema
2. Configurar las variables de entorno
3. Depurar errores de compilación

---

**Fecha:** 10 de Noviembre, 2025
**Proyecto:** Hot Wheels Car Scanner
**Cambios:** Embeddings con formato correcto para fix de identificación
