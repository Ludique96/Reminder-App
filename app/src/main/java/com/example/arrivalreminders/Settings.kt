package com.example.arrivalreminders

import android.content.Context

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** User-adjustable settings, kept in SharedPreferences. */
object AppSettings {
    private const val PREFS = "settings"
    private const val KEY_THEME = "theme"
    private const val KEY_COOLDOWN = "cooldown_minutes"

    const val DEFAULT_COOLDOWN_MINUTES = 10

    /** Choices for how long to wait before reminding about the same place again. */
    val COOLDOWN_OPTIONS_MINUTES = listOf(1, 5, 10, 30, 60, 180, 360, 1440)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun themeMode(ctx: Context): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs(ctx).getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(ctx: Context, mode: ThemeMode) {
        prefs(ctx).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun cooldownMinutes(ctx: Context): Int =
        prefs(ctx).getInt(KEY_COOLDOWN, DEFAULT_COOLDOWN_MINUTES)

    fun setCooldownMinutes(ctx: Context, minutes: Int) {
        prefs(ctx).edit().putInt(KEY_COOLDOWN, minutes).apply()
    }

    fun cooldownLabel(minutes: Int): String = when {
        minutes < 60 -> if (minutes == 1) "1 minute" else "$minutes minutes"
        minutes < 1440 -> if (minutes == 60) "1 hour" else "${minutes / 60} hours"
        else -> "24 hours"
    }
}
