# Configuración de eBay API para Precios

## ¿Por qué eBay?
eBay tiene un API gratuito que permite consultar **precios de ventas completadas** de Hot Wheels. Esto te da precios reales del mercado basados en transacciones finalizadas.

**Límite gratuito:** 5,000 llamadas por día (más que suficiente)

---

## Paso 1: Crear Cuenta de Desarrollador eBay

1. **Ve a:** https://developer.ebay.com/
2. **Click en:** "Get Started" o "Sign In"
3. **Inicia sesión** con tu cuenta de eBay (o crea una nueva - es gratis)

---

## Paso 2: Crear una App

1. Una vez dentro del dashboard de desarrollador:
   - Ve a: **"My Account"** → **"Application Keys"**
   - O directamente: https://developer.ebay.com/my/keys

2. **Click en:** "Create a Keyset"

3. **Completa el formulario:**
   - **Application Title:** `HotWheels Price Checker` (o el nombre que quieras)
   - **Application Type:** Selecciona **"Production"**
   - **Primary Contact:** Tu email
   - **Agreement:** Acepta los términos

4. **Click:** "Continue"

---

## Paso 3: Obtener tu App ID (API Key)

Después de crear la app, verás una pantalla con tus credenciales:

```
App ID (Client ID): XXXXXXX-XXXXXXX-PRD-XXXXXXXXX-XXXXXXXX
```

**⚠️ IMPORTANTE:**
- Copia el **App ID** (también llamado Client ID)
- Guárdalo en un lugar seguro
- NO lo compartas públicamente

---

## Paso 4: Configurar en la App

### Opción A: Editar directamente strings.xml

1. Abre el archivo: `app/src/main/res/values/strings.xml`

2. Busca la línea 234 (aproximadamente):
   ```xml
   <string name="ebay_app_id" translatable="false">YOUR_EBAY_APP_ID_HERE</string>
   ```

3. Reemplaza `YOUR_EBAY_APP_ID_HERE` con tu App ID:
   ```xml
   <string name="ebay_app_id" translatable="false">XXXXXXX-XXXXXXX-PRD-XXXXXXXXX-XXXXXXXX</string>
   ```

4. Guarda el archivo

### Opción B: Usar un archivo local.properties (Más seguro)

Si planeas subir el código a GitHub, es mejor usar `local.properties`:

1. Crea/edita `local.properties` en la raíz del proyecto
2. Agrega:
   ```properties
   ebay.app.id=XXXXXXX-XXXXXXX-PRD-XXXXXXXXX-XXXXXXXX
   ```

3. Modifica `build.gradle` para leer desde ahí (opcional - te ayudo después)

---

## Paso 5: Recompilar e Instalar

```bash
cd ~/Escritorio/proy_h
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Paso 6: Probar

1. Abre la app
2. Ve a tu colección
3. Selecciona un Hot Wheels
4. Toca **"Ver Precios"**
5. Ahora deberías ver precios reales de eBay! 🎉

---

## ¿Qué datos obtienes de eBay?

- **Precio mínimo** de ventas completadas
- **Precio promedio**
- **Precio máximo**
- **Cantidad de ventas** analizadas
- Solo incluye artículos **nuevos** (New condition)
- Solo **ventas completadas** (no listings activos)

---

## Ejemplo de Resultado

```
eBay:
  Rango: $5 - $25 USD
  Promedio: $12 USD
  Basado en: 47 ventas
```

---

## Solución de Problemas

### Error: "API key not configured"
- Verifica que pegaste correctamente el App ID
- Asegúrate de recompilar la app después de editar strings.xml

### Error: "Invalid App ID"
- Confirma que usaste el **App ID** (no el Cert ID)
- Verifica que seleccionaste "Production" al crear la app

### Error: "Request limit exceeded"
- Límite: 5,000 llamadas/día
- Espera 24 horas o crea otra app con diferente App ID

### No aparecen precios
- El modelo puede no tener ventas recientes en eBay
- Intenta con modelos más populares

---

## Notas Importantes

- ✅ Es **100% GRATIS**
- ✅ No requiere tarjeta de crédito
- ✅ 5,000 llamadas/día son suficientes
- ✅ Precios basados en **transacciones reales**
- ⚠️ Solo funciona con **internet activo**
- ⚠️ Puede no tener datos para modelos muy raros o antiguos

---

## Siguiente Paso Opcional: Amazon

Si quieres agregar precios de Amazon también, necesitas:
1. Cuenta de Amazon Affiliate (gratis)
2. Product Advertising API key
3. Implementación de firma AWS (más complejo)

**Recomendación:** Por ahora usa eBay + Mercado Libre, son suficientes.

---

## Recursos

- eBay Developer Portal: https://developer.ebay.com/
- API Documentation: https://developer.ebay.com/api-docs/buy/browse/overview.html
- Finding API: https://developer.ebay.com/devzone/finding/callref/index.html
- Soporte: https://developer.ebay.com/support

---

¿Listo? ¡Obtén tu API key y configúrala! 🚀
