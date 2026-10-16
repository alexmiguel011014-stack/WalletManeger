package com.example.allufinancas.feature.home

import com.example.allufinancas.data.*
import com.example.allufinancas.navigation.DataScreenViewModel
import kotlinx.coroutines.flow.*

internal data class HomeUiData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val alerts: List<Alert>,
    val goals: List<Goal>,
    val preferences: DisplayPreferences,
    val pendingImportCount: Int,
    val importSourceEnabled: Boolean,
)

internal class HomeViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
  private val homeData =
      combine(
              dataSource.transactions,
              dataSource.categories,
              dataSource.budgets,
              dataSource.alerts,
              dataSource.importNotices,
          ) { transactions, categories, budgets, alerts, notices ->
            HomeUiData(
                transactions,
                categories,
                budgets,
                alerts,
                emptyList(),
                DisplayPreferences(),
                notices.count { it.status == ImportNoticeStatus.PENDING },
                false,
            )
          }
          .combine(dataSource.goals) { data, goals -> data.copy(goals = goals) }
          .combine(dataSource.preferences) { data, settings -> data.copy(preferences = settings) }
          .combine(dataSource.enabledImportSource) { data, source ->
            data.copy(importSourceEnabled = source != null)
          }

  internal val state = stateFor(homeData) { it.transactions.isEmpty() }
}
