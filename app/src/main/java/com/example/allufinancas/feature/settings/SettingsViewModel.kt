package com.example.allufinancas.feature.settings

import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.data.DisplayPreferences
import com.example.allufinancas.navigation.DataScreenViewModel
import kotlinx.coroutines.flow.map

internal data class SettingsUiData(val preferences: DisplayPreferences)

internal class SettingsViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
    internal val state = stateFor(dataSource.preferences.map(::SettingsUiData)) { false }
}
