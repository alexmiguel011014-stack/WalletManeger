package com.example.allufinancas.feature.alerts

import com.example.allufinancas.data.*
import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.navigation.DataScreenViewModel
import kotlinx.coroutines.flow.combine

internal data class AlertsUiData(val alerts: List<Alert>, val pendingReviewCount: Int)

internal data class ReviewUiData(
    val cases: List<ReconciliationCase>,
    val transactions: List<Transaction>,
    val categories: List<Category>,
)

internal class AlertsViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
  internal val state =
      stateFor(
          combine(dataSource.alerts, dataSource.reconciliationCases) { alerts, cases ->
            AlertsUiData(alerts, cases.count { !it.decided })
          }
      ) {
        it.alerts.isEmpty() && it.pendingReviewCount == 0
      }
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
