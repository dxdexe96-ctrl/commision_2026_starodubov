package com.example.starodubovpavel.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("weather_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val darkTheme = booleanPreferencesKey("dark_theme")
        val fahrenheit = booleanPreferencesKey("fahrenheit")
        val cacheHours = intPreferencesKey("cache_hours")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            darkTheme = preferences[Keys.darkTheme] ?: false,
            useFahrenheit = preferences[Keys.fahrenheit] ?: false,
            cacheHours = preferences[Keys.cacheHours] ?: 24
        )
    }

    suspend fun setDarkTheme(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.darkTheme] = value }
    }

    suspend fun setFahrenheit(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.fahrenheit] = value }
    }

    suspend fun setCacheHours(value: Int) {
        context.settingsDataStore.edit { it[Keys.cacheHours] = value.coerceIn(1, 72) }
    }
}
