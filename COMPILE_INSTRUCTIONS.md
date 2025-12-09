# 🔨 Instrucciones para Compilar el APK de Release

## Problema Actual

El entorno VSCode Flatpak tiene restricciones que impiden que AAPT2 (Android Asset Packaging Tool) funcione correctamente. Esto es un problema conocido con Flatpak y Android SDK.

## ✅ Solución Recomendada: Compilar con Android Studio Nativo

### Opción 1: Android Studio instalado directamente en el sistema (NO Flatpak)

Si tienes Android Studio instalado fuera de Flatpak:

1. **Abrir el proyecto:**
   - Inicia Android Studio (versión nativa, no Flatpak)
   - File → Open → `/home/cristhyan/Escritorio/proy_h`
   - Espera a que Gradle sincronice

2. **Generar APK firmado:**
   - Build → Generate Signed Bundle / APK...
   - Selecciona: **APK** (no AAB)
   - Next

3. **Configurar keystore:**
   - Key store path: `/home/cristhyan/Escritorio/proy_h/diecast-release.keystore`
   - Key store password: `DiecastScanner2025!`
   - Key alias: `diecastscanner`
   - Key password: `DiecastScanner2025!`
   - Next

4. **Build:**
   - Build Variants: **release**
   - Signature Versions: ✅ V1 y ✅ V2
   - Finish

5. **Ubicación del APK:**
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

---

### Opción 2: Terminal del sistema (sin Flatpak)

Abre una **terminal nativa del sistema** (Ctrl+Alt+T), NO desde VSCode:

```bash
cd /home/cristhyan/Escritorio/proy_h

# Si tienes Android Studio instalado, busca Java
export JAVA_HOME=$(find ~/android-studio -name "jbr" -type d 2>/dev/null | head -1)

# O usa Java del sistema
# export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

export PATH=$JAVA_HOME/bin:$PATH

# Verificar Java
java -version

# Compilar
chmod +x gradlew
./gradlew clean assembleRelease

# El APK estará en:
# app/build/outputs/apk/release/app-release.apk
```

---

### Opción 3: Docker (Si tienes Docker instalado)

Crea un archivo `build-docker.sh`:

```bash
#!/bin/bash

docker run --rm \
  -v $(pwd):/project \
  -w /project \
  mingc/android-build-box:latest \
  bash -c "./gradlew clean assembleRelease"
```

Ejecuta:
```bash
chmod +x build-docker.sh
./build-docker.sh
```

---

### Opción 4: Usar una VM o máquina sin Flatpak

Si tienes acceso a otra máquina Linux sin Flatpak, o una VM:

1. Copia el proyecto a esa máquina
2. Instala Android SDK
3. Compila con el script `build_apk_release.sh`

---

## 🆘 Opción de Emergencia: Enviar Debug APK

Si necesitas publicar YA y no puedes compilar release:

**IMPORTANTE:** Esto NO es recomendable para producción, pero funciona:

1. Compila debug APK:
```bash
./gradlew assembleDebug
```

2. Firma manualmente con jarsigner:
```bash
# Ubicación del debug APK
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"

# Descomprimir, firmar y realinear
jarsigner -verbose \
  -sigalg SHA256withRSA \
  -digestalg SHA-256 \
  -keystore diecast-release.keystore \
  -storepass DiecastScanner2025! \
  $DEBUG_APK \
  diecastscanner

# Verificar firma
jarsigner -verify -verbose -certs $DEBUG_APK

# Copiar como release
cp $DEBUG_APK app-release-manual.apk
```

**⚠️ NO recomendado:** El APK debug tiene ProGuard desactivado y será más grande.

---

## 📦 Alternativa: Generar AAB (si Android Studio nativo funciona)

Si logras usar Android Studio nativo:

- Selecciona **Android App Bundle** en lugar de APK
- Google Play prefiere AAB (genera APKs optimizados automáticamente)
- Mismo proceso de firma

---

## ✅ Verificar que el APK esté correctamente firmado

Después de compilar:

```bash
# Ver información del APK
aapt dump badging app/build/outputs/apk/release/app-release.apk | grep -E "package|version"

# Verificar firma
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk

# Debe decir: "jar verified"
```

---

## 🎯 Lo que necesitas entregar a Play Store

### APK o AAB:
- **Tamaño esperado:** 50-100 MB (por ONNX y assets)
- **Formato:** APK o AAB firmado con keystore de release
- **Configuración:** ProGuard habilitado, minificado

### Verificaciones antes de subir:
```bash
# 1. Verificar que esté firmado
jarsigner -verify app/build/outputs/apk/release/app-release.apk

# 2. Ver versión
aapt dump badging app/build/outputs/apk/release/app-release.apk | grep version
# Debe mostrar: versionCode='5' versionName='2.0.0'

# 3. Ver package
aapt dump badging app/build/outputs/apk/release/app-release.apk | grep package
# Debe mostrar: package: name='com.diecast.carscanner'
```

---

## 🔄 Si todo falla: Compilar en otro lugar

**Última opción:** Pide ayuda a alguien con:
- Android Studio nativo (no Flatpak)
- O Ubuntu/Linux sin restricciones de Flatpak

Puedes enviar:
1. El código (sube a GitHub privado)
2. El keystore (¡MUY IMPORTANTE! solo a personas de confianza)
3. Las contraseñas

Ellos compilan y te envían el APK/AAB firmado.

---

## 📞 Ayuda

- **Foro Android:** https://stackoverflow.com/questions/tagged/android-gradle
- **Issue Gradle:** https://github.com/gradle/gradle/issues
- **Play Console Ayuda:** https://support.google.com/googleplay/android-developer

---

## 🎯 Resumen de archivos que necesitas

Después de compilar exitosamente:

- ✅ `app/build/outputs/apk/release/app-release.apk` (o AAB)
- ✅ Firmado con diecastscanner keystore
- ✅ Version 2.0.0 (versionCode 5)
- ✅ Package: com.diecast.carscanner
- ✅ ProGuard habilitado

**Este APK/AAB es lo que subirás a Google Play Console.**

---

**Buena suerte!** 🚀
