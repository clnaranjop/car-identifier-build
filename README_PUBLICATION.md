# 🚀 Car Identifier - Guía de Publicación en Play Store

## 📊 Estado Actual: 85% Completado

### ✅ Completado
- Política de privacidad (Markdown y HTML)
- Textos de descripción en 3 idiomas (EN, ES, ZH)
- Configuración del proyecto (versionCode, targetSdk, keystore)
- ProGuard y optimizaciones
- AdMob configurado
- Scripts de compilación y captura
- Guías detalladas para assets gráficos

### ⏳ Pendiente
- Compilar APK/AAB de release (ver COMPILE_INSTRUCTIONS.md)
- Activar GitHub Pages para la política de privacidad
- Crear Feature Graphic 1024x500
- Crear icono 512x512
- Capturar screenshots

---

## 📁 Archivos Importantes

### Documentación
- **[PUBLICATION_CHECKLIST.md](PUBLICATION_CHECKLIST.md)** - Checklist completo paso a paso
- **[COMPILE_INSTRUCTIONS.md](COMPILE_INSTRUCTIONS.md)** - Cómo compilar el APK
- **[PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)** - Textos para Play Store
- **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)** - Política de privacidad
- **[docs/index.html](docs/index.html)** - Política en HTML para GitHub Pages

### Assets
- **[play_store_assets/](play_store_assets/)** - Directorio para guardar gráficos
- **[play_store_assets/GRAPHIC_ASSETS_GUIDE.md](play_store_assets/GRAPHIC_ASSETS_GUIDE.md)** - Guía para crear assets

### Scripts
- **[build_apk_release.sh](build_apk_release.sh)** - Compilar APK de release
- **[take_screenshots.sh](take_screenshots.sh)** - Capturar screenshots automáticamente

---

## 🎯 Próximos Pasos

### 1. Compilar APK/AAB
**Problema:** Restricciones de Flatpak impiden compilar en este entorno.

**Solución:** Ver [COMPILE_INSTRUCTIONS.md](COMPILE_INSTRUCTIONS.md)

Opciones:
- Android Studio nativo (no Flatpak)
- Terminal del sistema sin Flatpak
- Docker
- Otra máquina

### 2. Activar GitHub Pages
```bash
# Hacer push
git push origin main

# Luego en GitHub:
# Settings → Pages → Source: main branch, /docs folder
```

URL resultante: `https://clnaranjop.github.io/proy_h/`

### 3. Crear Assets Gráficos
Ver guía completa en: [play_store_assets/GRAPHIC_ASSETS_GUIDE.md](play_store_assets/GRAPHIC_ASSETS_GUIDE.md)

**Feature Graphic (1024x500):**
- Herramienta recomendada: [Canva](https://canva.com) (gratis)
- Contenido: Logo + "Car Identifier" + fondo naranja
- Guardar en: `play_store_assets/feature_graphic_1024x500.png`

**Icono 512x512:**
- Redimensionar: `app/src/main/res/drawable/icon_logo.png`
- Guardar en: `play_store_assets/high_res_icon_512.png`

**Screenshots:**
- Cuando tengas dispositivo, ejecuta: `./take_screenshots.sh`
- Mínimo 2, recomendado 8

### 4. Subir a Play Console
1. Ir a: https://play.google.com/console
2. Crear nueva app o seleccionar existente
3. Production → Create new release
4. Subir APK/AAB
5. Completar Store Listing con textos de [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)
6. Subir assets gráficos
7. Configurar Data Safety (respuestas en PLAY_STORE_LISTING.md)
8. Privacy Policy URL: `https://clnaranjop.github.io/proy_h/`
9. Enviar a revisión

---

## 📝 Información de la App

- **Package Name:** com.diecast.carscanner
- **Version:** 2.0.0 (versionCode: 5)
- **Target SDK:** 35
- **Developer:** Digitizing Collections
- **Email:** digitizingcollections@gmail.com
- **Category:** Tools (o Lifestyle)
- **Content Rating:** Everyone
- **Pricing:** Free (con ads)

---

## 🔑 Keystore

- **Ubicación:** `/home/cristhyan/Escritorio/proy_h/diecast-release.keystore`
- **Store Password:** `DiecastScanner2025!`
- **Key Alias:** `diecastscanner`
- **Key Password:** `DiecastScanner2025!`

**⚠️ IMPORTANTE:** Guarda el keystore en un lugar seguro. Si lo pierdes, no podrás actualizar la app en Play Store.

---

## 📧 Soporte

¿Preguntas? Contacta al desarrollador: digitizingcollections@gmail.com

---

## ✅ Checklist Rápido

Antes de subir a Play Console:

- [ ] APK/AAB de release firmado
- [ ] GitHub Pages activado (URL de privacidad)
- [ ] Feature Graphic 1024x500
- [ ] Icono 512x512
- [ ] Mínimo 2 screenshots
- [ ] Textos de descripción listos
- [ ] Data Safety responses preparadas

**Progreso actual:** 5/6 documentos ✅ | Falta: Compilación + Assets gráficos

---

**¡Casi listo para publicar!** 🎉

Solo falta compilar el APK (fuera de Flatpak) y crear los assets visuales.
Todo el contenido, textos y configuración está completo.
