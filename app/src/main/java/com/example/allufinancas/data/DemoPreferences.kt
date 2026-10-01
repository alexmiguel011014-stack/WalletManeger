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
    private val enabledImportSourceKey = stringPreferencesKey("enabled_import_source")
    private val importFeedSuffixKey = stringPreferencesKey("import_feed_suffix")

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
    val enabledImportSource: Flow<NotificationImportSource?> = store.data.map { values ->
        values[enabledImportSourceKey]?.let { saved ->
            NotificationImportSource.entries.firstOrNull { it.name == saved }
        }
    }
    /** Optional four-digit local filter for one Rico card, account, or feed. */
    val importFeedSuffix: Flow<String?> = store.data.map { values ->
        values[importFeedSuffixKey]?.takeIf(::isFeedSuffix)
    }

    suspend fun setDarkTheme(enabled: Boolean) { store.edit { it[darkThemeKey] = enabled } }
    suspend fun setReduceMotion(enabled: Boolean) { store.edit { it[reduceMotionKey] = enabled } }
    suspend fun setShowCents(enabled: Boolean) { store.edit { it[showCentsKey] = enabled } }
    internal suspend fun setMaterializedAt(instant: Instant) { store.edit { it[materializedAtKey] = instant.toString() } }
    internal suspend fun clearMaterializedAt() { store.edit { it.remove(materializedAtKey) } }
    suspend fun setEnabledImportSource(source: NotificationImportSource?) {
        store.edit { values ->
            if (source == null) {
                values.remove(enabledImportSourceKey)
                values.remove(importFeedSuffixKey)
            } else values[enabledImportSourceKey] = source.name
        }
    }

    suspend fun setImportFeedSuffix(suffix: String?) {
        val normalized = suffix?.trim()?.takeIf(String::isNotEmpty)
        require(normalized == null || isFeedSuffix(normalized)) {
            "Informe somente os quatro últimos dígitos da conta, cartão ou feed"
        }
        store.edit { values ->
            if (normalized == null) values.remove(importFeedSuffixKey)
            else values[importFeedSuffixKey] = normalized
        }
    }

    private companion object {
        val FEED_SUFFIX = Regex("\\d{4}")
        fun isFeedSuffix(value: String): Boolean = value.matches(FEED_SUFFIX)
    }
}
