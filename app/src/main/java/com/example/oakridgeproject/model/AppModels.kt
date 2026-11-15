package com.example.oakridgeproject.model

import android.graphics.drawable.Drawable

enum class ThemeMode(val title: String, val subtitle: String) {
    SYSTEM("Adaptado al dispositivo", "Sigue automáticamente el tema claro u oscuro del sistema"),
    LIGHT("Tema claro", "Interfaz clara y minimalista en todo momento"),
    DARK("Tema oscuro", "Interfaz oscura para reducir la fatiga visual")
}

enum class AppCategory(val title: String, val description: String) {
    SOCIAL("Redes Sociales", "Mensajería, comunicación, foros y plataformas sociales"),
    RECREATIONAL("Apps Recreativas", "Juegos, video, música, fotografía y entretenimiento"),
    OTHER("Otras Apps", "Herramientas, navegadores, productividad y utilidades del sistema")
}

/**
 * Representa la regla de bloqueo para una aplicación.
 * Si [limitSeconds] es 0L, la aplicación se bloquea instantáneamente al abrirse
 * (ya que el tiempo de uso >= 0 segundos se cumple de inmediato).
 */
data class AppBlockRule(
    val packageName: String,
    val isBlocked: Boolean = false,
    val limitSeconds: Long = DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS
) {
    fun shouldBlock(usedSecondsToday: Long): Boolean {
        return isBlocked && usedSecondsToday >= limitSeconds
    }

    companion object {
        const val DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS = 0L
    }
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val category: AppCategory,
    val usedSecondsToday: Long = 0L,
    val isSystemApp: Boolean = false
)
