package com.example.allufinancas.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.demoPreferencesDataStore by preferencesDataStore(name = "allu_demo_display")

class DemoPreferences(context: Context) {
    private val store = context.applicationContext.demoPreferencesDataStore
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val reduceMotionKey = booleanPreferencesKey("reduce_motion")
    private val showCentsKey = booleanPreferencesKey("show_cents")
    private val materializedAtKey = stringPreferencesKey("materialized_at")

    val state: Flow<DisplayPreferences> = store.data.map { values ->
        DisplayPreferences(
            darkTheme = values[darkThemeKey] ?: false,
            reduceMotion = values[reduceMotionKey] ?: false,
            showCents = values[showCentsKey] ?: true,
        )
    }
    val materializedAt: Flow<Instant?> = store.data.map { values ->
        values[materializedAtKey]?.let { encoded -> runCatching { Instant.parse(encoded) }.getOrNull() }
    }

    suspend fun setDarkTheme(enabled: Boolean) { store.edit { it[darkThemeKey] = enabled } }
    suspend fun setReduceMotion(enabled: Boolean) { store.edit { it[reduceMotionKey] = enabled } }
    suspend fun setShowCents(enabled: Boolean) { store.edit { it[showCentsKey] = enabled } }
    internal suspend fun setMaterializedAt(instant: Instant) { store.edit { it[materializedAtKey] = instant.toString() } }
    internal suspend fun clearMaterializedAt() { store.edit { it.remove(materializedAtKey) } }
}
