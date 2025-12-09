# 🚀 Compilar APK con GitHub Actions (en la nube)

## ¿Qué es esto?

GitHub Actions compilará tu APK **en la nube** (servidores de GitHub), evitando completamente el problema de AAPT2 en tu máquina local.

## 📋 Pasos para Configurar

### 1. Subir el código a GitHub

Necesitas subir SOLO el código (sin imágenes pesadas). Crea un nuevo repositorio:

```bash
cd /home/cristhyan/Escritorio/proy_h

# Crear un nuevo repo temporal solo con código
git init
git add .github/
git add app/src/main/java/
git add app/src/main/res/layout/
git add app/src/main/res/values*/
git add app/src/main/res/xml/
git add app/src/main/res/drawable/*.xml
git add app/src/main/res/mipmap*/
git add app/src/main/AndroidManifest.xml
git add app/build.gradle
git add app/proguard-rules.pro
git add build.gradle
git add settings.gradle
git add gradle.properties
git add gradlew*
git add gradle/

git commit -m "Add code for GitHub Actions build"
```

### 2. Crear repositorio en GitHub

1. Ve a: https://github.com/new
2. Nombre: `car-identifier-build` (o el que quieras)
3. **IMPORTANTE:** Selecciona **PRIVADO** ✅
4. NO inicialices con README
5. Click "Create repository"

### 3. Conectar y subir

```bash
git remote add origin git@github.com:clnaranjop/car-identifier-build.git
git branch -M main
git push -u origin main
```

### 4. Configurar Secrets en GitHub

Ve a tu repositorio en GitHub:
- Settings → Secrets and variables → Actions → New repository secret

**Crea estos 4 secrets:**

#### Secret 1: KEYSTORE_BASE64
```bash
# En tu terminal local, ejecuta:
base64 /home/cristhyan/Escritorio/proy_h/diecast-release.keystore | tr -d '\n'

# Copia el resultado completo y pégalo en GitHub como secret
# Nombre: KEYSTORE_BASE64
```

#### Secret 2: KEYSTORE_PASSWORD
```
Valor: DiecastScanner2025!
```

#### Secret 3: KEY_ALIAS
```
Valor: diecastscanner
```

#### Secret 4: KEY_PASSWORD
```
Valor: DiecastScanner2025!
```

### 5. Ejecutar el Workflow

1. Ve a tu repositorio en GitHub
2. Click en la pestaña **"Actions"**
3. Click en **"Build Release APK"** (lado izquierdo)
4. Click en **"Run workflow"** (botón azul a la derecha)
5. Click en **"Run workflow"** de nuevo (popup)

### 6. Esperar Compilación (5-7 minutos)

Verás el progreso en tiempo real. Cuando termine:
- ✅ Verás un check verde
- 📦 Aparecerá "Artifacts" en la página del workflow

### 7. Descargar APK

1. Click en el workflow completado
2. Scroll abajo hasta "Artifacts"
3. Click en `car-identifier-release-v2.0.0`
4. Se descargará un ZIP con tu APK

---

## 🎯 Ventajas de GitHub Actions

✅ **Sin instalaciones** - No necesitas Android Studio ni Java
✅ **Sin problemas de AAPT2** - Compila en servidores limpios de GitHub
✅ **Reproducible** - Puedes compilar cuantas veces quieras
✅ **Automático** - Solo haces click y esperas
✅ **Gratis** - 2000 minutos/mes en plan gratuito

---

## ⚠️ Importante

- Usa un repositorio **PRIVADO** porque contiene tu keystore
- NO compartas los secrets con nadie
- Descarga el APK y elimina el workflow/repositorio después si quieres

---

## 🔒 Alternativa Más Segura (sin subir keystore)

Si prefieres NO subir tu keystore a GitHub:

1. Compila **sin firmar** en GitHub Actions
2. Firma manualmente en tu máquina:

```bash
# Descargar APK sin firmar de GitHub
# Luego firmar localmente:
jarsigner -verbose \
  -sigalg SHA256withRSA \
  -digestalg SHA-256 \
  -keystore diecast-release.keystore \
  app-release-unsigned.apk \
  diecastscanner
```

---

## 📞 Ayuda

Si tienes problemas con algún paso, avísame y te ayudo.

**Tiempo total:** 15-20 minutos (incluyendo lectura)
**Tiempo de compilación:** 5-7 minutos
