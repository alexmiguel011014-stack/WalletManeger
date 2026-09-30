package com.example.allufinancas.feature.home

import com.example.allufinancas.data.*
import com.example.allufinancas.navigation.DataScreenViewModel
import com.example.allufinancas.navigation.ScreenUiState
import kotlinx.coroutines.flow.*

internal data class HomeUiData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val alerts: List<Alert>,
    val goals: List<Goal>,
    val preferences: DisplayPreferences,
)

internal class HomeViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
    private val homeData = combine(
        dataSource.transactions, dataSource.categories, dataSource.budgets, dataSource.alerts,
    ) { transactions, categories, budgets, alerts ->
        HomeUiData(transactions, categories, budgets, alerts, emptyList(), DisplayPreferences())
    }.combine(dataSource.goals) { data, goals -> data.copy(goals = goals) }
        .combine(dataSource.preferences) { data, settings -> data.copy(preferences = settings) }

    internal val state = stateFor(homeData) { it.transactions.isEmpty() }
}
