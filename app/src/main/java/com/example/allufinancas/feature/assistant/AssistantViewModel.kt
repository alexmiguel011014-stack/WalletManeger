package com.example.allufinancas.feature.assistant

import com.example.allufinancas.data.AssistantMessage
import com.example.allufinancas.data.Budget
import com.example.allufinancas.data.Category
import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.data.Transaction
import com.example.allufinancas.navigation.DataScreenViewModel
import kotlinx.coroutines.flow.combine

internal data class AssistantUiData(
    val messages: List<AssistantMessage>,
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
)

internal class AssistantViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
  internal val state =
      stateFor(
          combine(
              dataSource.messages,
              dataSource.transactions,
              dataSource.categories,
              dataSource.budgets,
          ) { messages, transactions, categories, budgets ->
            AssistantUiData(messages, transactions, categories, budgets)
          },
      ) {
        it.messages.isEmpty()
      }
}
