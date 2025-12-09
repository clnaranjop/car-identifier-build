# Cómo Mover la Versión de Internal Testing a Production

## 🎯 Problema Actual

- **Internal Testing:** Tiene versionCode 25120301 (2.1.1) ✅
- **Production:** Tiene versionCode 6 (versión vieja de noviembre) ❌
- **Resultado:** Al instalar desde Play Store público, baja la versión vieja

## ✅ Solución: Promover a Production

### Opción 1: Promover el Release Existente (Más Rápido)

1. **Ve a Play Console:**
   - https://play.google.com/console
   - Selecciona tu app "Diecast Car Scanner"

2. **Ir a Testing → Internal Testing:**
   - Verás el release con versionCode 25120301

3. **Promover a Production:**
   - Click en los 3 puntos (⋮) al lado del release
   - Selecciona "Promote release"
   - Selecciona "Production"
   - Click en "Promote"

4. **Revisar y Publicar:**
   - Agrega las notas de la versión:
   ```
   Versión 2.1.1 - Mejoras Importantes

   ✨ Nuevo:
   • Advertencia automática cuando no estás en WiFi
   • Las imágenes de referencia ahora se muestran correctamente

   🐛 Correcciones:
   • Interfaz de resultados mejorada
   • Optimización del espacio de almacenamiento
   • Mejoras de rendimiento

   📱 Nota: En la primera instalación, la app descargará
   aproximadamente 1.3GB de datos (se recomienda WiFi).
   Después funciona 100% offline.
   ```
   - Click en "Review release"
   - Click en "Start rollout to Production"

### Opción 2: Crear Nuevo Release en Production (Manual)

Si no puedes promover, crea uno nuevo:

1. **Ve a Production → Create new release**

2. **Sube el AAB:**
   - Arrastra: `app/build/outputs/bundle/release/app-release.aab`
   - O busca el archivo manualmente

3. **Agrega notas de versión** (mismo texto de arriba)

4. **Review y Publish:**
   - Click en "Review release"
   - Click en "Start rollout to Production"

## ⏱️ Tiempos de Espera

Después de publicar en Production:

- **Procesamiento:** 1-2 horas
- **Disponible para usuarios:** 2-24 horas (usualmente 4-8 horas)
- **Actualización automática:** Hasta 48 horas (los usuarios pueden forzarla)

## 🔍 Verificar que se Publicó Correctamente

1. **En Play Console:**
   - Ve a Production
   - Debes ver "versionCode 25120301 (2.1.1)" con estado "Available"

2. **En Play Store:**
   - Busca tu app en el navegador (sin iniciar sesión)
   - Debe decir "Versión 2.1.1"
   - En "Novedades" debe aparecer tu descripción

3. **En tu celular:**
   - Abre Play Store
   - Busca tu app
   - Si ya la tienes instalada:
     - Debe aparecer botón "Actualizar"
     - Click en actualizar
   - Si no la tienes:
     - Debe mostrar versión 2.1.1

## ⚠️ Importante

- **No desactives Internal Testing:** Puedes tener ambos activos
- **Versión en Production siempre gana:** Los usuarios del público descargarán desde Production
- **Internal Testing es solo para testers:** Necesitan el enlace especial
- **Puedes hacer rollout gradual:** Empieza con 5%, luego 10%, 50%, 100%

## 🎯 Resumen

**Paso a paso simple:**

1. Play Console → Production → Create new release
2. Sube: `app/build/outputs/bundle/release/app-release.aab`
3. Notas de versión (copiar el texto de arriba)
4. Review → Publicar
5. Esperar 2-4 horas
6. ¡Listo! Versión nueva en Play Store público

---

**Después de publicar:**
- Los usuarios con la app verán "Actualizar" en Play Store
- Los nuevos usuarios descargarán la versión 2.1.1
- La descarga de 1.3GB se hará en el primer uso
