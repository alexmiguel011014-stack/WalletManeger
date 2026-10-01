package com.example.allufinancas.feature.settings

import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.data.DisplayPreferences
import com.example.allufinancas.data.NotificationImportSource
import com.example.allufinancas.navigation.DataScreenViewModel
import kotlinx.coroutines.flow.combine

internal data class SettingsUiData(
    val preferences: DisplayPreferences,
    val enabledImportSource: NotificationImportSource?,
    val importFeedSuffix: String?,
)

internal class SettingsViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
    internal val state = stateFor(
        combine(dataSource.preferences, dataSource.enabledImportSource, dataSource.importFeedSuffix) {
                preferences,
                source,
                suffix,
            ->
            SettingsUiData(preferences, source, suffix)
        },
    ) { false }
}
