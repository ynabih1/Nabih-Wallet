package com.nabih.wallet.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ColorPalette {
    PIXEL_BLUE,
    EMERALD,
    TERRACOTTA,
    INDIGO
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorsEnabled: Boolean = false,
    val colorPalette: ColorPalette = ColorPalette.PIXEL_BLUE,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
)

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(readCurrent())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun readCurrent(): UserPreferences {
        val themeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val paletteName = prefs.getString(KEY_COLOR_PALETTE, ColorPalette.PIXEL_BLUE.name)
        return UserPreferences(
            themeMode = runCatching { ThemeMode.valueOf(themeName ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            dynamicColorsEnabled = prefs.getBoolean(KEY_DYNAMIC_COLORS, false),
            colorPalette = runCatching { ColorPalette.valueOf(paletteName ?: ColorPalette.PIXEL_BLUE.name) }
                .getOrDefault(ColorPalette.PIXEL_BLUE),
            dailyReminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 21).coerceIn(0, 23),
            reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59)
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _preferences.value = _preferences.value.copy(themeMode = mode)
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply()
        _preferences.value = _preferences.value.copy(dynamicColorsEnabled = enabled)
    }

    fun setColorPalette(palette: ColorPalette) {
        prefs.edit().putString(KEY_COLOR_PALETTE, palette.name).apply()
        _preferences.value = _preferences.value.copy(colorPalette = palette)
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        _preferences.value = _preferences.value.copy(dailyReminderEnabled = enabled)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, safeHour)
            .putInt(KEY_REMINDER_MINUTE, safeMinute)
            .apply()
        _preferences.value = _preferences.value.copy(
            reminderHour = safeHour,
            reminderMinute = safeMinute
        )
    }

    companion object {
        private const val PREFS_NAME = "nabih_wallet_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        private const val KEY_COLOR_PALETTE = "color_palette"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
