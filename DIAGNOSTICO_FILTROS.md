# Diagnóstico: Filtros de Colección No Funcionan

## 🐛 Problema Reportado
Los botones de filtro (TH, Favorites, STH) en el menú de colección no están filtrando los items.

## 🔍 Análisis del Código

### Código Revisado:

**CollectionActivity.kt - Líneas 146-168:**
```kotlin
binding.chipFavorites.setOnCheckedChangeListener { _, isChecked ->
    showOnlyFavorites = isChecked
    if (isChecked) {
        binding.chipAll.isChecked = false
    }
    applyFilters()
}

binding.chipTreasureHunt.setOnCheckedChangeListener { _, isChecked ->
    showOnlyTH = isChecked
    if (isChecked) {
        binding.chipAll.isChecked = false
    }
    applyFilters()
}
```

**Función de filtrado - Líneas 171-211:**
```kotlin
private fun applyFilters() {
    var filteredItems = allItems

    // Apply favorite filter
    if (showOnlyFavorites) {
        filteredItems = filteredItems.filter { it.collectionItem.isFavorite }
    }

    // Apply TH filter
    if (showOnlyTH) {
        filteredItems = filteredItems.filter {
            it.collectionItem.isTreasureHunt && !it.collectionItem.isSuperTreasureHunt
        }
    }

    // Apply STH filter
    if (showOnlySTH) {
        filteredItems = filteredItems.filter { it.collectionItem.isSuperTreasureHunt }
    }

    Log.d(tag, "Filtered items: ${filteredItems.size} of ${allItems.size}")
    adapter.submitList(filteredItems)
}
```

✅ El código parece correcto.

## 🔎 Posibles Causas

### Causa 1: Items no tienen las propiedades configuradas
Cuando agregas un item a la colección, las propiedades `isTreasureHunt`, `isSuperTreasureHunt` y `isFavorite` podrían no estar siendo configuradas correctamente.

**Verificación necesaria:**
- ¿Los items en tu colección tienen estos valores en `true`?
- ¿O todos están en `false` por defecto?

### Causa 2: Badges TH/STH no están visibles
Si los badges de TH/STH no se muestran en las tarjetas, significa que la propiedad está en `false`.

**Verifica en la app:**
- ¿Ves badges "TH" o "STH" en las esquinas de las tarjetas?
- Si NO los ves, los items no están marcados como TH/STH

## 🧪 Prueba de Diagnóstico

### Paso 1: Verifica los badges visuales
1. Abre "My Collection"
2. Mira las tarjetas de tus Hot Wheels
3. ¿Ves badges "TH" (verde) o "STH" (dorado) en las esquinas superiores?

**Si NO ves badges:**
→ Los items no tienen `isTreasureHunt = true`
→ Por eso el filtro parece "no funcionar" (no hay nada que filtrar)

### Paso 2: Prueba el filtro de Favorites
1. Marca algunos items como favoritos (toca el ❤️)
2. El corazón debería llenarse de rojo
3. Activa el filtro "Favorites"
4. ¿Solo muestra los que tienen corazón rojo?

**Si el filtro de Favorites SÍ funciona:**
→ El código está bien
→ El problema es que los items TH/STH no están marcados correctamente

## 💡 Solución Propuesta

### Opción A: Los items NO son TH/STH (comportamiento correcto)
Si tus Hot Wheels en la colección NO son Treasure Hunt o Super Treasure Hunt reales:
- ✅ **El filtro funciona correctamente**
- ✅ No muestra nada porque no hay items TH/STH
- ✅ Solo los items que sean TH/STH de verdad deberían aparecer

### Opción B: Los items SÍ son TH/STH pero no están marcados
Si tienes Hot Wheels que SÍ son Treasure Hunt pero no se marcaron al agregarlos:

**Necesitamos verificar el código que detecta TH/STH:**
- ¿Dónde se configura `isTreasureHunt = true`?
- ¿Se está detectando automáticamente desde la base de datos?
- ¿O se debe marcar manualmente?

## 🔧 Cómo Probar si el Filtro Funciona

### Test 1: Favorites (más fácil de probar)
```
1. Ve a "My Collection"
2. Toca el ❤️ en 2-3 items (deberían ponerse rojos)
3. Toca el chip "Favorites" arriba
4. ¿Solo muestra los que tienen ❤️ rojo?
```

**Resultado esperado:**
- ✅ Solo muestra los favoritos
- ❌ Si muestra todos = bug en el filtro

### Test 2: TH/STH
```
1. Ve a "My Collection"
2. ¿Ves badges "TH" o "STH" en las tarjetas?
3. Si SÍ los ves:
   - Toca el chip "TH" arriba
   - ¿Solo muestra los que tienen badge "TH"?
4. Si NO los ves:
   - Los items no están marcados como TH/STH
   - El filtro no mostrará nada (comportamiento correcto)
```

## 📝 Información que Necesito

Para ayudarte mejor, necesito saber:

1. **¿El filtro "Favorites" funciona?**
   - Sí / No

2. **¿Ves badges "TH" o "STH" en tus tarjetas de colección?**
   - Sí, veo algunos
   - No, no veo ninguno

3. **¿Qué pasa cuando activas el filtro "TH"?**
   - No muestra nada (lista vacía)
   - Muestra todos los items (no filtra)
   - Muestra solo algunos (funciona correctamente)

## 🐛 Posibles Bugs Identificados

### Bug Potencial: ChipGroup en modo "single selection"
Si los chips están en un ChipGroup con modo de selección única, solo uno puede estar activo a la vez.

**Verificar en activity_collection.xml:**
```xml
<com.google.android.material.chip.ChipGroup
    android:id="@+id/chipGroup"
    android:singleSelection="true"  <!-- ¿Está esto? -->
    ...>
```

Si `singleSelection="true"`, los chips se comportan como radio buttons.

### Bug Potencial: Estado inicial
Los chips podrían no estar sincronizados con las variables internas.

**Código actual:**
```kotlin
private var showOnlyFavorites = false
private var showOnlyTH = false
private var showOnlySTH = false
```

Si un chip está `checked="true"` en el XML pero la variable está en `false`, no funcionará.

## 🚀 Próximos Pasos

1. **Prueba manual:** Verifica si el filtro "Favorites" funciona
2. **Reporta:** Dime qué observas con cada filtro
3. **Si es necesario:** Modificaré el código para mostrar logs o agregar funcionalidad

---

**Fecha:** 31 Octubre 2025
**Problema:** Filtros TH/Favorites no funcionan
**Estado:** Investigando
