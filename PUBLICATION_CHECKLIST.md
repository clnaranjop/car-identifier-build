# 📋 Checklist de Publicación - Play Store

## Car Identifier v2.0.0
**Package:** com.diecast.carscanner
**Developer:** Digitizing Collections

---

## ✅ COMPLETADO

### 1. Configuración del Proyecto
- ✅ versionCode: 5
- ✅ versionName: 2.0.0
- ✅ targetSdk: 35
- ✅ Keystore de release configurado
- ✅ ProGuard habilitado
- ✅ AdMob ID de producción configurado
- ✅ Permisos correctamente declarados
- ✅ Traducciones completas (EN, ES, ZH)

### 2. Documentos y Textos
- ✅ Política de privacidad creada ([PRIVACY_POLICY.md](PRIVACY_POLICY.md))
- ✅ HTML de política creado ([docs/index.html](docs/index.html))
- ✅ Descripciones en 3 idiomas ([PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md))
- ✅ Respuestas de Data Safety preparadas
- ✅ Textos de actualización (What's New)
- ✅ Keywords y tags definidos

---

## 🔄 EN PROCESO

### 3. Build de Release
- ⏳ **AAB de release firmado**
  - **Acción:** Compilar en Android Studio
  - **Pasos:**
    1. Build → Generate Signed Bundle / APK
    2. Seleccionar Android App Bundle
    3. Next → Usar keystore existente
    4. Finish
  - **Output:** `app/release/app-release.aab`
  - **Script alternativo:** `./build_release.sh`

---

## ❌ PENDIENTE

### 4. Hosting de Política de Privacidad
- ❌ **Push a GitHub**
  ```bash
  cd /home/cristhyan/Escritorio/proy_h
  git push origin main
  ```

- ❌ **Activar GitHub Pages**
  1. Ir a: https://github.com/clnaranjop/proy_h/settings/pages
  2. Source: Deploy from a branch
  3. Branch: main, folder: /docs
  4. Save
  5. Esperar 2-3 minutos
  6. Verificar: https://clnaranjop.github.io/proy_h/

### 5. Assets Gráficos

#### A. Icono 512x512 (OBLIGATORIO)
- ❌ **Crear icono de alta resolución**
  - Tamaño: 512 x 512 px
  - Formato: PNG de 32 bits, sin transparencia
  - Fuente: `app/src/main/res/drawable/icon_logo.png`
  - Output: `play_store_assets/high_res_icon_512.png`
  - **Ver guía:** [play_store_assets/GRAPHIC_ASSETS_GUIDE.md](play_store_assets/GRAPHIC_ASSETS_GUIDE.md)

#### B. Feature Graphic (OBLIGATORIO)
- ❌ **Crear banner 1024x500**
  - Tamaño: 1024 x 500 px
  - Formato: PNG o JPG
  - Contenido:
    - Logo de la app
    - Texto: "Car Identifier"
    - Subtexto: "Instantly identify 11,000+ Hot Wheels models"
    - Fondo naranja (#FF6B00)
  - Output: `play_store_assets/feature_graphic_1024x500.png`
  - **Herramienta recomendada:** [Canva](https://canva.com) (gratis)
  - **Ver guía completa:** [play_store_assets/GRAPHIC_ASSETS_GUIDE.md](play_store_assets/GRAPHIC_ASSETS_GUIDE.md)

#### C. Screenshots (OBLIGATORIO - mínimo 2)
- ❌ **Capturar screenshots de teléfono**
  - Cantidad: Mínimo 2, recomendado 8
  - Tamaño: 1080 x 1920 px (o resolución del dispositivo)
  - Formato: PNG o JPG
  - Capturas requeridas:
    1. Pantalla principal (Main)
    2. Cámara en acción
    3. Resultados de identificación ⭐
    4. Colección
    5. Búsqueda manual
    6. Car of the Day
    7. Exploración
    8. Configuración
  - Output: `play_store_assets/screenshots/`
  - **Script:** `take_screenshots.sh` (cuando tengas dispositivo)

#### D. Screenshots de Tablet (OPCIONAL)
- ⭕ Screenshots de tablet 7" (mínimo 2)
- ⭕ Screenshots de tablet 10" (mínimo 2)

#### E. Video Promocional (OPCIONAL)
- ⭕ Video de YouTube (30 seg - 2 min)
- ⭕ Demo de la app en acción

---

## 📱 Configuración en Play Console

### 6. Información de la App

#### A. Store Listing (Inglés)
- ❌ **App name:** Car Identifier
- ❌ **Short description:** (80 chars)
  ```
  Instantly identify Hot Wheels & die-cast cars with AI-powered image recognition
  ```
- ❌ **Full description:** Ver [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)
- ❌ **App icon:** high_res_icon_512.png
- ❌ **Feature graphic:** feature_graphic_1024x500.png
- ❌ **Phone screenshots:** Mínimo 2
- ❌ **App category:** Tools (o Lifestyle)
- ❌ **Tags:** hot wheels, die-cast cars, car identifier, AI, collection
- ❌ **Contact details:**
  - Website: (opcional)
  - Email: digitizingcollections@gmail.com
  - Phone: (opcional)
  - **Privacy policy:** https://clnaranjop.github.io/proy_h/

#### B. Traducciones (Español y Chino)
- ❌ Repetir lo mismo para ES y ZH
- ❌ Usar textos de [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)

### 7. Release Management

#### A. Production Release
- ❌ **Upload AAB:** app-release.aab
- ❌ **Release name:** 2.0.0 - AI-Powered Identification
- ❌ **Release notes:** Ver "What's New" en [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)

### 8. Policy and Safety

#### A. App Content
- ❌ **Privacy policy URL:** https://clnaranjop.github.io/proy_h/
- ❌ **Ads:** Yes (AdMob)
- ❌ **Target audience:** Everyone
- ❌ **Content rating questionnaire:** Completar (Everyone rating)

#### B. Data Safety
- ❌ **Data collection:**
  - Photos/Videos: Collected but NOT shared (processed locally)
  - Device IDs: Collected and shared with AdMob for advertising
- ❌ **Data security:**
  - Data encrypted in transit: YES
  - Users can request deletion: YES
- ❌ **Ver respuestas completas:** [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md) sección "DATA SAFETY RESPONSES"

#### C. App Access
- ❌ **Restrictions:** None (available to all users)

### 9. Pricing and Distribution

#### A. Countries
- ❌ **Available in:** All countries (o seleccionar específicos)
- ❌ **Pricing:** Free

#### B. Content Guidelines
- ❌ **Confirm compliance** with:
  - Content policies
  - US export laws
  - Other applicable laws

---

## 🎯 PASOS SIGUIENTES (en orden)

1. **HOY - Compilar y assets:**
   - [ ] Compilar AAB en Android Studio
   - [ ] Hacer push a GitHub
   - [ ] Activar GitHub Pages
   - [ ] Crear Feature Graphic con Canva
   - [ ] Crear icono 512x512

2. **CUANDO TENGAS DISPOSITIVO - Screenshots:**
   - [ ] Conectar dispositivo Android
   - [ ] Ejecutar `take_screenshots.sh`
   - [ ] Capturar mínimo 8 pantallas

3. **SUBIR A PLAY CONSOLE:**
   - [ ] Crear nueva app en Play Console
   - [ ] Subir AAB
   - [ ] Llenar Store Listing
   - [ ] Subir assets gráficos
   - [ ] Completar cuestionarios
   - [ ] Enviar a revisión

4. **DESPUÉS DE REVISIÓN (1-3 días):**
   - [ ] Responder cualquier comentario de Google
   - [ ] Publicar app
   - [ ] ¡Celebrar! 🎉

---

## 📞 Recursos de Ayuda

- **Documentación Play Console:** https://support.google.com/googleplay/android-developer
- **Guía de assets gráficos:** [play_store_assets/GRAPHIC_ASSETS_GUIDE.md](play_store_assets/GRAPHIC_ASSETS_GUIDE.md)
- **Policy Center:** https://play.google.com/about/developer-content-policy/

---

## ⏱️ Tiempo Estimado

- ✅ Documentos y textos: **COMPLETADO**
- ⏳ Compilar AAB: **15 minutos**
- ⏳ GitHub Pages: **5 minutos**
- ⏳ Feature Graphic: **20 minutos** (con Canva)
- ⏳ Icono 512x512: **5 minutos**
- ⏳ Screenshots: **15 minutos** (con dispositivo)
- ⏳ Play Console setup: **30-45 minutos**

**TOTAL RESTANTE:** ~1.5 horas + tiempo de revisión de Google (1-3 días)

---

## ✅ Progreso Actual

```
████████░░ 80% Completado

Falta:
- AAB de release
- Assets gráficos
- GitHub Pages
- Configuración Play Console
```

---

**¡Estás muy cerca de publicar! 🚀**

La parte difícil (configuración, textos, policy) ya está lista.
Solo faltan los assets visuales y la compilación final.
