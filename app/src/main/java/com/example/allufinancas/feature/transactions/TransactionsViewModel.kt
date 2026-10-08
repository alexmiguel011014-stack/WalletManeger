package com.example.allufinancas.feature.transactions

import com.example.allufinancas.data.*
import com.example.allufinancas.navigation.DataScreenViewModel
import kotlinx.coroutines.flow.*

internal data class TransactionsUiData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val preferences: DisplayPreferences,
    val pendingImportCount: Int,
)

internal class TransactionsViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
  private val screenData =
      combine(
          dataSource.transactions,
          dataSource.categories,
          dataSource.preferences,
          dataSource.importNotices,
      ) { transactions, categories, settings, notices ->
        TransactionsUiData(
            transactions = transactions,
            categories = categories,
            preferences = settings,
            pendingImportCount = notices.count { it.status == ImportNoticeStatus.PENDING },
        )
      }

  internal val state = stateFor(screenData) { it.transactions.isEmpty() }
}
