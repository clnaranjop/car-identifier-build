# Guía Paso a Paso - Publicar en Google Play Store

## ✅ Checklist de Pre-Publicación

### Archivos Preparados:
- [x] **AAB firmado**: `app/build/outputs/bundle/release/app-release.aab` (generándose...)
- [x] **Política de Privacidad**: `privacy_policy.html` ✓
- [x] **Textos de Play Store**: `play_store_listing.md` ✓
- [x] **AdMob configurado**: App ID real integrado ✓
- [x] **Sección "Acerca de" completa**: AboutActivity implementada ✓
- [ ] **Email de contacto**: Agregar tu email real
- [ ] **Feature Graphic**: Crear imagen 1024x500 px
- [ ] **Screenshots**: Tomar 4-8 capturas de pantalla
- [ ] **Política de Privacidad Online**: Subir HTML a un servidor/GitHub Pages

---

## 📋 PASO 1: Preparar Archivos Faltantes

### 1.1 Agregar Email de Contacto
Edita `privacy_policy.html` y reemplaza `[TU_EMAIL_AQUI]` con tu email real.

### 1.2 Subir Política de Privacidad
Opciones:
- **GitHub Pages** (GRATIS y fácil):
  1. Crear repositorio en GitHub
  2. Habilitar GitHub Pages en Settings
  3. Subir `privacy_policy.html`
  4. URL será: `https://TU_USUARIO.github.io/NOMBRE_REPO/privacy_policy.html`

- **Google Drive**:
  1. Subir archivo HTML
  2. Compartir públicamente
  3. Usar URL pública

### 1.3 Crear Feature Graphic (1024x500 px)
Necesitas una imagen destacada con:
- Logo de la app
- Texto: "Car Identifier - Hot Wheels AI"
- Colores de la marca (naranja #FF6F00, azul #1A237E)
- Fondo atractivo con gradiente

Herramientas sugeridas:
- Canva (online, gratis)
- GIMP (gratis)
- Photoshop

### 1.4 Tomar Screenshots (mínimo 2, recomendado 4-8)
Screenshots sugeridas:
1. Pantalla principal (menú)
2. Cámara identificando un auto
3. Resultados con detalles del auto
4. Colección personal
5. Exploración por año
6. Auto del Día
7. Configuración de idioma
8. Vista de detalles

**Cómo tomarlas:**
```bash
# Conectar dispositivo Android
~/Android/Sdk/platform-tools/adb devices

# Instalar APK
~/Android/Sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk

# Tomar screenshot
~/Android/Sdk/platform-tools/adb shell screencap -p > screenshot_1.png

# Repetir para cada pantalla
```

**Requisitos:**
- Formato: PNG o JPG
- Dimensiones mínimas: 320px
- Dimensiones máximas: 3840px
- Orientación: Vertical (la app está en portrait)

---

## 📋 PASO 2: Crear Cuenta de Google Play Console

### 2.1 Acceder a Play Console
1. Ir a: https://play.google.com/console
2. Iniciar sesión con tu cuenta de Google
3. Aceptar Acuerdo de Distribución para Desarrolladores

### 2.2 Pagar Tarifa de Registro
- **Costo**: $25 USD (pago único, de por vida)
- Métodos de pago: Tarjeta de crédito/débito

### 2.3 Completar Perfil de Desarrollador
- Nombre del desarrollador (será público)
- Email de contacto
- Sitio web (opcional)
- Dirección (requerida para pagos si planeas cobrar)

---

## 📋 PASO 3: Crear Nueva Aplicación

### 3.1 Crear App
1. En Play Console → "Crear app"
2. Completar:
   - **Nombre de la app**: "Car Identifier - Hot Wheels AI"
   - **Idioma predeterminado**: Español
   - **Tipo de app**: App
   - **Gratis o de pago**: Gratis
3. Declaraciones:
   - ✓ La aplicación cumple con las políticas de Google Play
   - ✓ La aplicación cumple con las leyes de exportación de EE. UU.

### 3.2 Configurar Detalles de la App

**Panel → Presencia en Play Store → Información principal**

Copiar información de `play_store_listing.md`:

- **Nombre de la app**: Car Identifier - Hot Wheels AI
- **Descripción breve**: (80 caracteres max)
  ```
  Identifica autos Hot Wheels con IA. Base de datos con miles de modelos.
  ```
- **Descripción completa**: (4000 caracteres max)
  ```
  [Copiar desde play_store_listing.md sección "Descripción Completa"]
  ```

### 3.3 Agregar Recursos Gráficos

**Panel → Presencia en Play Store → Recursos gráficos**

1. **Icono de la app** (512x512 px):
   - Extraer: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
   - Si no es 512x512, redimensionar

2. **Feature Graphic** (1024x500 px):
   - Subir imagen creada en Paso 1.3

3. **Capturas de pantalla** (mínimo 2):
   - Subir screenshots tomadas en Paso 1.4
   - **TELÉFONO**: Subir aquí (no tablet)

### 3.4 Categorización y Contacto

**Panel → Presencia en Play Store → Categoría y etiquetas**
- **Categoría**: Herramientas
- **Etiquetas**: (opcional, ayuda con ASO)

**Panel → Presencia en Play Store → Información de contacto**
- **Email**: tu_email@example.com (OBLIGATORIO)
- **Teléfono**: (opcional)
- **Sitio web**: (opcional o GitHub repo)

### 3.5 Política de Privacidad

**Panel → Presencia en Play Store → Políticas de la app**
- **URL de la política de privacidad**:
  ```
  https://tu_usuario.github.io/car-identifier/privacy_policy.html
  ```
  (Reemplazar con tu URL real después de subir a GitHub Pages)

---

## 📋 PASO 4: Configurar Versión de Producción

### 4.1 Crear Nueva Versión

**Panel → Producción → Crear nueva versión**

### 4.2 Subir AAB
1. Click en "Subir" o arrastrar archivo
2. Seleccionar: `app/build/outputs/bundle/release/app-release.aab`
3. Esperar a que se procese (puede tardar varios minutos)

### 4.3 Completar Información de la Versión

- **Nombre de la versión**: 2.0.0
- **Código de versión**: 5 (se detecta automáticamente del AAB)

**Notas de la versión** (en español):
```
Versión 2.0.0 - Lanzamiento Inicial

✨ Características principales:
• Identificación de autos Hot Wheels con IA
• Base de datos con miles de modelos desde 1968
• Gestión de colección personal
• Búsqueda por año de fabricación
• Auto del Día
• Exploración visual de modelos
• Soporte para Español, Inglés y Chino
• Interfaz moderna con Material Design

🚀 Primera versión pública en Google Play Store
```

### 4.4 Revisar Versión
Play Console mostrará:
- ✅ AAB firmado correctamente
- ⚠️ Posibles advertencias (no críticas)
- ❌ Errores (deben resolverse)

---

## 📋 PASO 5: Clasificación de Contenido

**Panel → Clasificación de contenido → Iniciar cuestionario**

### Respuestas Sugeridas para Car Identifier:

**Categoría de la app:**
- Seleccionar: "Herramientas, productividad, comunicación o educación"

**Cuestionario:**

1. **¿La app contiene violencia?** NO
2. **¿La app contiene contenido sexual?** NO
3. **¿La app contiene lenguaje ofensivo?** NO
4. **¿La app contiene contenido de miedo/terror?** NO
5. **¿La app permite interacción entre usuarios?** NO
6. **¿La app permite compartir ubicación?** NO
7. **¿La app permite compras digitales?** NO
8. **¿La app accede a información personal?** SÍ
   - **¿Qué información?**: Imágenes de cámara (procesadas localmente)

**Anuncios:**
- **¿La app contiene anuncios?** SÍ
- **Proveedor**: Google AdMob

**Clasificación resultante:** PEGI 3, ESRB Everyone, USK All Ages

---

## 📋 PASO 6: Público Objetivo y Contenido

**Panel → Público objetivo y contenido**

### 6.1 Público Objetivo
- **Grupo de edad principal**: 13 años o más
- **Grupos de edad secundarios**: Todos los grupos
- **Atractivo para niños**: NO (aunque es apto para todas las edades)

### 6.2 Anuncios
- **¿Contiene anuncios?** SÍ
- **Red publicitaria**: Google AdMob
- **ID de app de AdMob**: ca-app-pub-6811474988371378~3996929251

### 6.3 Acceso a Datos del Usuario
**¿Tu app recopila o comparte datos de usuarios?**

**SÍ** - Seleccionar tipos de datos:

1. **Actividad de la app**
   - Interacciones con la app
   - ✓ Recopilado: SÍ
   - ✓ Compartido: NO
   - ✓ Propósito: Análisis
   - ✓ Es opcional: NO

2. **Fotos y videos**
   - Fotos tomadas con cámara
   - ✓ Recopilado: SÍ
   - ✓ Compartido: NO
   - ✓ Procesamiento: Solo local
   - ✓ Es opcional: NO (es la función principal)

3. **Identificadores de dispositivo**
   - ID de publicidad
   - ✓ Recopilado: SÍ (por AdMob)
   - ✓ Compartido: SÍ (con Google AdMob)
   - ✓ Propósito: Publicidad
   - ✓ Es opcional: SÍ (usuario puede desactivar anuncios personalizados)

**Importante:** Aclarar que las fotos NO se envían a servidores, se procesan 100% localmente.

---

## 📋 PASO 7: Revisión Final y Envío

### 7.1 Panel de Control
Verificar que TODO esté en verde (✓):

- ✅ Presencia en Play Store
  - ✓ Información principal
  - ✓ Recursos gráficos
  - ✓ Categorización
  - ✓ Información de contacto
  - ✓ Políticas de la app

- ✅ Versión de producción
  - ✓ AAB subido
  - ✓ Notas de versión

- ✅ Clasificación de contenido
  - ✓ Cuestionario completado

- ✅ Público objetivo
  - ✓ Configurado

- ✅ Seguridad de datos
  - ✓ Formulario completado

### 7.2 Enviar para Revisión
1. Click en **"Enviar para revisión"**
2. Revisar resumen
3. Confirmar envío

### 7.3 Tiempo de Revisión
- **Promedio**: 1-3 días
- **Máximo**: Hasta 7 días
- **Notificación**: Por email cuando esté aprobada o rechazada

---

## 📋 PASO 8: Después de la Aprobación

### 8.1 App Publicada
- La app estará disponible en Google Play Store
- URL: `https://play.google.com/store/apps/details?id=com.diecast.carscanner`

### 8.2 Monitoreo
En Play Console puedes ver:
- **Instalaciones**: Número de usuarios
- **Calificaciones**: Reseñas y estrellas
- **Estadísticas**: Dispositivos, países, versiones de Android
- **Errores**: Reportes de fallos (ANR, crashes)
- **Ingresos**: De AdMob (en AdMob Console)

### 8.3 Actualizaciones Futuras
Para subir nueva versión:
1. Incrementar `versionCode` y `versionName` en `build.gradle`
2. Generar nuevo AAB: `./gradlew bundleRelease`
3. En Play Console → Producción → Crear nueva versión
4. Subir AAB y agregar notas de versión
5. Enviar para revisión

---

## 🚨 Problemas Comunes y Soluciones

### Error: "El paquete ya existe"
- **Causa**: El applicationId ya está registrado
- **Solución**: Cambiar `applicationId` en `build.gradle` (ej: com.diecast.carscanner2)

### Error: "AAB no firmado"
- **Causa**: Falta firma del keystore
- **Solución**: Verificar que `signingConfig signingConfigs.release` esté en `build.gradle`

### Error: "Política de privacidad no accesible"
- **Causa**: URL inaccesible o no HTTPS
- **Solución**: Usar HTTPS (GitHub Pages es HTTPS por defecto)

### Advertencia: "APK de gran tamaño"
- **Causa**: Assets grandes (imágenes, modelos)
- **Impacto**: Algunos usuarios pueden no descargar
- **Solución**: Considerar App Bundles con dynamic delivery (futuro)

### Rechazo: "Marca registrada"
- **Causa**: Uso de "Hot Wheels" en nombre
- **Solución**: Enfatizar en descripción que es app no oficial

---

## 📊 Métricas de Éxito

**Semana 1:**
- Meta: 50+ instalaciones
- Calificación: > 4.0 estrellas

**Mes 1:**
- Meta: 500+ instalaciones
- Calificación: > 4.2 estrellas
- Retención: > 30% (usuarios que regresan)

**Mes 3:**
- Meta: 2,000+ instalaciones
- Calificación: > 4.5 estrellas
- Reseñas positivas

---

## 🎯 Tips para Aumentar Descargas

### ASO (App Store Optimization):
1. **Título optimizado**: Incluir palabras clave
2. **Screenshots atractivos**: Mostrar features principales
3. **Descripción clara**: Beneficios en primeros 2 párrafos
4. **Palabras clave**: hot wheels, identifier, AI, collection
5. **Feature Graphic impactante**: Primera impresión cuenta

### Marketing:
1. Compartir en grupos de Facebook de coleccionistas Hot Wheels
2. Publicar en Reddit: r/HotWheels, r/Diecast
3. Instagram: #HotWheels #DiecastCollector
4. YouTube: Video demostración de la app

### Engagement:
1. Responder todas las reseñas (buenas y malas)
2. Agregar features solicitadas por usuarios
3. Actualizaciones regulares (cada 2-3 meses)
4. Agregar más modelos a la base de datos

---

## 📞 Recursos y Contactos

**Google Play Console:**
- https://play.google.com/console

**Políticas de Google Play:**
- https://play.google.com/about/developer-content-policy/

**Centro de Ayuda:**
- https://support.google.com/googleplay/android-developer

**AdMob:**
- https://admob.google.com/

**GitHub Pages (hosting gratis):**
- https://pages.github.com/

---

## ✅ Checklist Final Antes de Enviar

- [ ] AAB generado y firmado: `app-release.aab`
- [ ] Email de contacto agregado en privacidad
- [ ] Política de privacidad subida y accesible (HTTPS)
- [ ] Feature Graphic creado (1024x500 px)
- [ ] Mínimo 2 screenshots tomados
- [ ] Descripción completa en español e inglés
- [ ] Clasificación de contenido completada
- [ ] Público objetivo configurado
- [ ] Formulario de seguridad de datos completado
- [ ] AdMob App ID verificado
- [ ] Tarifa de desarrollador pagada ($25 USD)
- [ ] Cuenta de Play Console creada

**Cuando TODO esté ✓ → Click "Enviar para revisión"**

---

¡Buena suerte con la publicación! 🚀

Si tienes dudas, la comunidad de desarrolladores Android en Stack Overflow es muy útil:
https://stackoverflow.com/questions/tagged/google-play-console
