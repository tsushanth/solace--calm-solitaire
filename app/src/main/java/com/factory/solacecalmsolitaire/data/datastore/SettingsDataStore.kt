package com.factory.solacecalmsolitaire.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.factory.solacecalmsolitaire.ui.theme.FeltTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "solace_settings")

data class GameSettings(
    val drawCount: Int = 3,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoCompleteEnabled: Boolean = true,
    val feltTheme: FeltTheme = FeltTheme.CLASSIC,
    val dynamicColorEnabled: Boolean = true
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DRAW_COUNT = intPreferencesKey("draw_count")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val AUTO_COMPLETE_ENABLED = booleanPreferencesKey("auto_complete_enabled")
        val FELT_THEME = stringPreferencesKey("felt_theme")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    }

    val settings: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        GameSettings(
            drawCount = prefs[Keys.DRAW_COUNT] ?: 3,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            autoCompleteEnabled = prefs[Keys.AUTO_COMPLETE_ENABLED] ?: true,
            feltTheme = FeltTheme.fromStorageKey(prefs[Keys.FELT_THEME] ?: FeltTheme.CLASSIC.storageKey),
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true
        )
    }

    suspend fun setDrawCount(drawCount: Int) {
        context.dataStore.edit { it[Keys.DRAW_COUNT] = drawCount }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setAutoCompleteEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_COMPLETE_ENABLED] = enabled }
    }

    suspend fun setFeltTheme(theme: FeltTheme) {
        context.dataStore.edit { it[Keys.FELT_THEME] = theme.storageKey }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }
}
