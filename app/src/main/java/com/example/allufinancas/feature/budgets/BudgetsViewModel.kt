package com.example.allufinancas.feature.budgets

import com.example.allufinancas.data.*
import com.example.allufinancas.navigation.DataScreenViewModel
import com.example.allufinancas.navigation.ScreenUiState
import kotlinx.coroutines.flow.*

internal data class BudgetsUiData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val goals: List<Goal>,
    val preferences: DisplayPreferences,
)

internal class BudgetsViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
    private val screenData = combine(
        dataSource.transactions, dataSource.categories, dataSource.budgets, dataSource.goals,
    ) { transactions, categories, budgets, goals ->
        BudgetsUiData(transactions, categories, budgets, goals, DisplayPreferences())
    }.combine(dataSource.preferences) { data, settings -> data.copy(preferences = settings) }

    internal val state = stateFor(screenData) { it.budgets.isEmpty() && it.goals.isEmpty() }
}
