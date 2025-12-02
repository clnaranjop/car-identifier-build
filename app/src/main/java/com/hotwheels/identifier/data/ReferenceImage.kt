package com.hotwheels.identifier.data

/**
 * Representa una imagen de referencia en el modo Exploración.
 */
data class ReferenceImage(
    val fileName: String,           // hw_gotta_go_2022_10_10.jpg
    val displayName: String,        // Gotta Go
    val year: String,               // 2022
    val assetPath: String,          // reference_images/2022/hw_gotta_go_2022_10_10.jpg
    var currentRotation: Int = 0    // Rotación temporal aplicada (0, 90, 180, 270)
) {
    companion object {
        /**
         * Extrae el año del nombre de archivo.
         * Formato esperado: hw_modelo_AÑO_numero.jpg
         */
        fun extractYear(fileName: String): String {
            // Intentar extraer año del nombre de archivo
            val parts = fileName.split("_")
            for (part in parts) {
                if (part.length == 4 && part.all { it.isDigit() }) {
                    val year = part.toIntOrNull()
                    if (year != null && year in 1968..2030) {
                        return part
                    }
                }
            }
            return "Unknown"
        }

        /**
         * Extrae el nombre legible del modelo.
         * Formato: hw_modelo_año_numero.jpg -> Modelo
         */
        fun extractDisplayName(fileName: String): String {
            // Remover extensión
            val nameWithoutExt = fileName.removeSuffix(".jpg")

            // Remover prefijo hw_
            val withoutPrefix = nameWithoutExt.removePrefix("hw_")

            // Dividir por _
            val parts = withoutPrefix.split("_")

            // Filtrar partes que no sean años o números
            val nameParts = parts.filter { part ->
                val isYear = part.length == 4 && part.all { it.isDigit() }
                val isNumber = part.all { it.isDigit() }
                !isYear && !isNumber
            }

            // Unir y capitalizar
            return nameParts.joinToString(" ") { it.capitalize() }
        }

        private fun String.capitalize(): String {
            return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
