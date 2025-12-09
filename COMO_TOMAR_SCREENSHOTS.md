# Cómo Tomar Screenshots para Google Play Store

## 📱 Requisitos de Screenshots

**Obligatorio:**
- Mínimo: **2 screenshots**
- Recomendado: **4-8 screenshots**
- Formato: PNG o JPG
- Dimensiones mínimas: 320 px
- Dimensiones máximas: 3840 px
- Orientación: **Portrait** (vertical)

---

## 🔧 Método 1: Con ADB (Desde Computadora)

### Preparación:

1. **Conectar dispositivo Android**
   ```bash
   # Verificar que el dispositivo está conectado
   ~/Android/Sdk/platform-tools/adb devices
   ```

   Si ves tu dispositivo listado, estás listo. Si no:
   - Activa "Depuración USB" en tu Android
   - Configuración → Acerca del teléfono → Tap 7 veces en "Número de compilación"
   - Vuelve y ve a Sistema → Opciones de desarrollador → Activar "Depuración USB"

2. **Instalar la app**
   ```bash
   ~/Android/Sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk
   ```

   O si ya está instalada, desinstala primero:
   ```bash
   ~/Android/Sdk/platform-tools/adb uninstall com.diecast.carscanner
   ~/Android/Sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Tomar Screenshots:

**Script completo para tomar 8 screenshots:**

```bash
# Crear carpeta para screenshots
mkdir -p screenshots_play_store
cd screenshots_play_store

# Despertar el dispositivo
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_WAKEUP

# Iniciar la app
~/Android/Sdk/platform-tools/adb shell am start -n com.diecast.carscanner/.ui.SplashActivity

# Esperar que cargue
sleep 3

# Screenshot 1: Pantalla principal
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_1_main_menu.png
echo "✓ Screenshot 1 tomado: Main Menu"

# Navegar a Identificación
~/Android/Sdk/platform-tools/adb shell input tap 540 800  # Ajusta coordenadas según tu pantalla
sleep 2

# Screenshot 2: Pantalla de cámara
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_2_camera.png
echo "✓ Screenshot 2 tomado: Camera Screen"

# Volver al menú
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_BACK
sleep 1

# Ir a Exploración
~/Android/Sdk/platform-tools/adb shell input tap 540 1000
sleep 2

# Screenshot 3: Exploración por año
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_3_exploration.png
echo "✓ Screenshot 3 tomado: Exploration"

# Seleccionar un año
~/Android/Sdk/platform-tools/adb shell input tap 540 600
sleep 2

# Screenshot 4: Galería de autos
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_4_gallery.png
echo "✓ Screenshot 4 tomado: Gallery"

# Volver y ir a Colección
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_BACK
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_BACK
sleep 1
~/Android/Sdk/platform-tools/adb shell input tap 540 1200
sleep 2

# Screenshot 5: Mi Colección
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_5_collection.png
echo "✓ Screenshot 5 tomado: My Collection"

# Volver y ir a Auto del Día
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_BACK
sleep 1
~/Android/Sdk/platform-tools/adb shell input tap 540 1400
sleep 2

# Screenshot 6: Auto del Día
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_6_car_of_the_day.png
echo "✓ Screenshot 6 tomado: Car of the Day"

# Volver y ir a Configuración
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_BACK
sleep 1
~/Android/Sdk/platform-tools/adb shell input tap 540 1600
sleep 2

# Screenshot 7: Configuración
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_7_settings.png
echo "✓ Screenshot 7 tomado: Settings"

# Volver y ir a Acerca de
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_BACK
sleep 1
~/Android/Sdk/platform-tools/adb shell input tap 540 1800
sleep 2

# Screenshot 8: Acerca de
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_8_about.png
echo "✓ Screenshot 8 tomado: About"

echo ""
echo "✅ Todos los screenshots tomados!"
echo "📁 Ubicación: $(pwd)"
ls -lh *.png
```

**Uso:**
```bash
cd /home/cristhyan/Escritorio/proy_h
chmod +x tomar_screenshots.sh  # Si lo guardas como script
./tomar_screenshots.sh
```

### Screenshots Mínimos Requeridos:

Si solo quieres 4 screenshots básicos:

```bash
mkdir -p screenshots_play_store
cd screenshots_play_store

# 1. Pantalla principal
~/Android/Sdk/platform-tools/adb shell am start -n com.diecast.carscanner/.ui.SplashActivity
sleep 3
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_1_main_menu.png

# 2. Pantalla de identificación (cámara)
~/Android/Sdk/platform-tools/adb shell input tap 540 800
sleep 2
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_2_camera.png

# 3. Resultados (navega manualmente y toma screenshot)
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_3_results.png

# 4. Colección
~/Android/Sdk/platform-tools/adb shell input keyevent KEYCODE_BACK
~/Android/Sdk/platform-tools/adb shell input tap 540 1200
sleep 2
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_4_collection.png

echo "✅ Screenshots básicos tomados!"
ls -lh *.png
```

---

## 📱 Método 2: Tomar Screenshots Manualmente en el Teléfono

### Pasos:

1. **Instalar la app en tu teléfono**
2. **Abrir la app** y navegar a cada pantalla
3. **Tomar screenshot** en cada pantalla:
   - **Samsung**: Presionar Volumen Abajo + Botón de Encendido
   - **Google Pixel**: Presionar Volumen Abajo + Botón de Encendido
   - **Xiaomi/Redmi**: Volumen Abajo + Botón de Encendido
   - **Huawei**: Volumen Abajo + Botón de Encendido

4. **Transferir screenshots a la PC:**
   ```bash
   ~/Android/Sdk/platform-tools/adb pull /sdcard/Pictures/Screenshots/ screenshots_play_store/
   ```

   O usar cable USB y copiar manualmente.

---

## 📸 Pantallas a Capturar (en orden de importancia)

### Obligatorias (mínimo 2):
1. **Pantalla principal** - Menú con todas las opciones
2. **Pantalla de resultados** - Mostrando un auto identificado con detalles

### Recomendadas (4 total):
3. **Cámara en acción** - Mostrando cámara lista para identificar
4. **Mi Colección** - Lista de autos guardados

### Adicionales (hasta 8):
5. **Exploración por año** - Galería de modelos por década
6. **Auto del Día** - Feature especial
7. **Configuración** - Opciones de idioma
8. **Acerca de** - Información de la app

---

## 🎨 Consejos para Buenos Screenshots

### ✅ HACER:
- Usar pantalla limpia (sin notificaciones)
- Orientación vertical (portrait)
- Mostrar contenido real (autos reales)
- Buena iluminación en las fotos
- Screenshots claros y enfocados
- Mostrar las features principales

### ❌ NO HACER:
- Screenshots con errores o crashes
- Pantallas vacías o sin contenido
- Información personal visible
- Capturas borrosas
- Incluir notificaciones del sistema

---

## 🖼️ Editar Screenshots (Opcional)

Si quieres agregar marcos o texto:

### Usando Canva:
1. Subir screenshot a Canva
2. Agregar marco de teléfono
3. Agregar texto descriptivo
4. Descargar

### Usando GIMP:
1. Abrir screenshot
2. Agregar texto con herramienta de texto
3. Exportar

**NOTA**: Google Play permite screenshots simples sin marcos. No es obligatorio editarlos.

---

## 📋 Checklist de Screenshots

Antes de subir a Play Store:
- [ ] Mínimo 2 screenshots tomados
- [ ] Formato PNG o JPG
- [ ] Orientación portrait (vertical)
- [ ] Sin información personal
- [ ] Sin errores visibles
- [ ] Contenido claro y legible
- [ ] Muestran las features principales
- [ ] Guardados en carpeta `screenshots_play_store/`

---

## 🚀 Siguiente Paso

Una vez tengas los screenshots:
1. Verifica que se vean bien
2. Renombra si es necesario (números del 1-8)
3. Continúa con: **Crear cuenta en Play Console**
