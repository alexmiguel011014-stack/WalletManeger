package com.example.allufinancas.feature.transactions

import com.example.allufinancas.data.*
import com.example.allufinancas.navigation.DataScreenViewModel
import com.example.allufinancas.navigation.ScreenUiState
import kotlinx.coroutines.flow.*

internal data class TransactionsUiData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val preferences: DisplayPreferences,
)

internal class TransactionsViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
    private val screenData = combine(dataSource.transactions, dataSource.categories) { transactions, categories ->
        TransactionsUiData(transactions, categories, DisplayPreferences())
    }.combine(dataSource.preferences) { data, settings -> data.copy(preferences = settings) }

    internal val state = stateFor(screenData) { it.transactions.isEmpty() }
}
