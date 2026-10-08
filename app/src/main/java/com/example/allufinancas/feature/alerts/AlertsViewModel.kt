package com.example.allufinancas.feature.alerts

import com.example.allufinancas.data.*
import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.navigation.DataScreenViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal data class AlertsUiData(val alerts: List<Alert>)

internal data class ReviewUiData(
    val cases: List<ReconciliationCase>,
    val transactions: List<Transaction>,
    val categories: List<Category>,
)

internal class AlertsViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
  internal val state = stateFor(dataSource.alerts.map(::AlertsUiData)) { it.alerts.isEmpty() }
  internal val reviewState =
      stateFor(
          combine(
              dataSource.reconciliationCases,
              dataSource.transactions,
              dataSource.categories,
              ::ReviewUiData,
          ),
      ) {
        it.cases.isEmpty()
      }
}
