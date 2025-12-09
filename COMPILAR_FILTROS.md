# Compilar e Instalar - Fix de Filtros de Colección

## ✅ Cambios Listos para Compilar

Se corrigió el problema de los filtros (All, Favorites, TH, STH) en el menú de colección.

### ❌ Problema Anterior:
- Los filtros estaban visibles pero NO filtraban correctamente
- Se podían seleccionar múltiples chips a la vez
- Ejemplo: "Favorites" seleccionado mostraba 4 items cuando debería mostrar 0

### ✅ Solución Aplicada:
- Cambiado `singleSelection="true"` en ChipGroup
- Agregado `style="@style/Widget.MaterialComponents.Chip.Filter"` a todos los chips
- Ahora funcionan como radio buttons (solo uno activo a la vez)

## 🚀 Comandos para Compilar e Instalar

```bash
cd ~/Escritorio/proy_h
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
export PATH=$PATH:$HOME/Android/Sdk/platform-tools
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🧪 Cómo Probar Después de Instalar

### Test 1: Filtro "All"
1. Abre "My Collection"
2. El chip "All" debe estar seleccionado por defecto
3. Debe mostrar todos tus 4 items

### Test 2: Filtro "Favorites"
1. Marca 1-2 items como favoritos (toca el ❤️)
2. El corazón debe ponerse rojo
3. Toca el chip "Favorites"
4. **Resultado esperado:** Solo muestra los que tienen ❤️ rojo
5. Si no marcaste ninguno → pantalla vacía con mensaje

### Test 3: Filtro "TH"
1. Toca el chip "TH"
2. **Resultado esperado:** Solo muestra items con badge verde "TH"
3. Si no tienes ninguno → pantalla vacía (es correcto, tus items son comunes)

### Test 4: Filtro "STH"
1. Toca el chip "STH"
2. **Resultado esperado:** Solo muestra items con badge dorado "STH"
3. Si no tienes ninguno → pantalla vacía (es correcto, tus items son comunes)

### Test 5: Solo Un Chip Activo
1. Toca "Favorites" → debe deseleccionar "All"
2. Toca "TH" → debe deseleccionar "Favorites"
3. **Resultado esperado:** Solo un chip naranja/resaltado a la vez

## 📊 Qué Significan las Estrellas (Rarity)

Las estrellas representan la **rareza** del Hot Wheels:

### ⭐ 1 Estrella - Común
- Producción masiva, fácil de encontrar
- **Todos tus items actuales tienen 1 estrella**

### ⭐⭐ 2 Estrellas - Poco común
- Disponible pero no en todas las tiendas

### ⭐⭐⭐ 3 Estrellas - Raro
- Difícil de encontrar

### ⭐⭐⭐⭐ 4 Estrellas - Muy raro
- Ediciones limitadas, chase cars

### ⭐⭐⭐⭐⭐ 5 Estrellas - Ultra raro
- Super Treasure Hunts (STH)
- Ediciones especiales muy limitadas
- Puede valer $50-$200+ USD

**La rareza se determina automáticamente desde la base de datos cuando identificas el Hot Wheels.**

## 📝 Archivos Modificados

- `app/src/main/res/layout/activity_collection.xml`
  - Línea 174: `singleSelection="true"`
  - Líneas 179, 190, 201, 211: Agregado `style="@style/Widget.MaterialComponents.Chip.Filter"`

## ✅ Resultado Esperado

Después de compilar e instalar:
- ✅ Solo un chip activo a la vez
- ✅ "All" muestra todos (4 items)
- ✅ "Favorites" muestra solo favoritos (los que tienen ❤️ rojo)
- ✅ "TH" muestra solo TH (probablemente 0, tus items son comunes)
- ✅ "STH" muestra solo STH (probablemente 0, tus items son comunes)
- ✅ Cuando un filtro no tiene resultados → pantalla vacía con mensaje

---

**Fecha:** 31 Octubre 2025
**Cambio:** Fix de filtros de colección
**Archivos:** activity_collection.xml
**Estado:** ✅ Listo para compilar
