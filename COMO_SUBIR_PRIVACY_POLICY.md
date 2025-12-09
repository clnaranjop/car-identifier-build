# Cómo Subir la Política de Privacidad a GitHub Pages

## Método 1: GitHub Pages (RECOMENDADO - GRATIS)

### Paso 1: Crear Repositorio en GitHub
1. Ve a: https://github.com/new
2. Nombre del repositorio: `car-identifier-privacy` (o cualquier nombre)
3. Visibilidad: **Public**
4. NO marques "Add a README file"
5. Click en **"Create repository"**

### Paso 2: Subir el archivo
Tienes 2 opciones:

#### Opción A: Subir por la web (MÁS FÁCIL)
1. En la página del repositorio, click en **"uploading an existing file"**
2. Arrastra `privacy_policy.html` desde tu carpeta
3. Scroll down y click **"Commit changes"**

#### Opción B: Usar Git (si tienes instalado)
```bash
cd /home/cristhyan/Escritorio/proy_h

# Inicializar repositorio (si no está inicializado)
git init

# Agregar solo privacy_policy.html
git add privacy_policy.html

# Crear commit
git commit -m "Add privacy policy"

# Conectar con GitHub (reemplaza TU_USUARIO y NOMBRE_REPO)
git remote add origin https://github.com/TU_USUARIO/car-identifier-privacy.git

# Subir
git branch -M main
git push -u origin main
```

### Paso 3: Activar GitHub Pages
1. En tu repositorio, ve a **Settings** (Configuración)
2. En el menú lateral izquierdo, click en **Pages**
3. En "Source", selecciona:
   - Branch: **main**
   - Folder: **/ (root)**
4. Click en **Save**
5. Espera 1-2 minutos

### Paso 4: Obtener URL
Después de 1-2 minutos, recarga la página de Settings → Pages.

Verás un mensaje verde:
```
✓ Your site is published at https://TU_USUARIO.github.io/car-identifier-privacy/
```

**Tu URL de Privacy Policy será:**
```
https://TU_USUARIO.github.io/car-identifier-privacy/privacy_policy.html
```

⚠️ **IMPORTANTE**: Guarda esta URL, la necesitarás para Play Console.

---

## Método 2: Google Drive (Alternativa)

### Pasos:
1. Sube `privacy_policy.html` a Google Drive
2. Click derecho → Compartir
3. Cambiar a "Cualquier persona con el enlace"
4. Copiar enlace
5. La URL será algo como: `https://drive.google.com/file/d/XXXXX/view`

⚠️ **Problema**: Google Drive no muestra HTML correctamente, solo permite descargar.

**NO RECOMENDADO** - Play Store puede rechazarlo.

---

## Método 3: Netlify Drop (FÁCIL)

1. Ve a: https://app.netlify.com/drop
2. Arrastra `privacy_policy.html` a la página
3. Netlify te dará una URL como: `https://random-name-123.netlify.app/privacy_policy.html`

✅ **GRATIS, FÁCIL, RÁPIDO**

---

## Método 4: Hosting Pagado (Si ya tienes uno)

Si tienes hosting web propio:
1. Sube `privacy_policy.html` por FTP
2. Accede por: `https://tudominio.com/privacy_policy.html`

---

## ✅ Recomendación

**USA GITHUB PAGES** porque:
- ✅ Es gratis
- ✅ Es HTTPS (requerido por Google Play)
- ✅ Es permanente
- ✅ Puedes actualizar fácilmente
- ✅ Es profesional

---

## 🎯 Después de Subir

Una vez tengas la URL, anótala aquí:

```
URL de Privacy Policy: https://_____________________.github.io/car-identifier-privacy/privacy_policy.html
```

Esta URL la usarás en Play Console en la sección "Políticas de la app".
