package com.example.allufinancas.navigation

import com.example.allufinancas.data.AccountSummary
import com.example.allufinancas.data.Alert
import com.example.allufinancas.data.AssistantAnswer
import com.example.allufinancas.data.AssistantMessage
import com.example.allufinancas.data.Budget
import com.example.allufinancas.data.Category
import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.data.DisplayPreferences
import com.example.allufinancas.data.Goal
import com.example.allufinancas.data.ReconciliationCase
import com.example.allufinancas.data.Transaction
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlluViewModelActionsTest {
  @Test
  fun actionsReachTheDataSourceAndResetReportsSuccess() {
    val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    Dispatchers.setMain(dispatcher)
    try {
      runTest(dispatcher) {
        val source = RecordingDemoDataSource()
        val viewModel = AlluViewModel(source)
        var categorySaved = false

        viewModel.updateBudget("food", 12_345)
        viewModel.setCategory("tx", "food") { categorySaved = true }
        viewModel.decideReview("duplicate-tx")
        viewModel.reopenReview("duplicate-tx")
        viewModel.setAlertDismissed("alert", true)
        viewModel.sendMessage("pergunta")
        viewModel.resetDemo()
        viewModel.setDarkTheme(true)
        viewModel.setReduceMotion(true)
        viewModel.setShowCents(false)
        runCurrent()

        assertEquals(
            listOf(
                "budget:food:12345",
                "category:tx:food",
                "decide:duplicate-tx",
                "reopen:duplicate-tx",
                "alert:alert:true",
                "message:pergunta",
                "reset",
                "dark:true",
                "motion:true",
                "cents:false",
            ),
            source.actions,
        )
        assertEquals("Dados financeiros locais excluídos", viewModel.notice.value)
        assertTrue(categorySaved)
        viewModel.clearNotice()
        assertNull(viewModel.notice.value)
      }
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun resetFailureIsReportedWithoutEscapingTheViewModel() {
    val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    Dispatchers.setMain(dispatcher)
    try {
      runTest(dispatcher) {
        val source = RecordingDemoDataSource(failOn = "reset")
        val viewModel = AlluViewModel(source)

        viewModel.resetDemo()
        runCurrent()

        assertEquals(listOf("reset"), source.actions)
        assertEquals("Não foi possível excluir os dados", viewModel.notice.value)
      }
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun importFeedSuffixSaveReachesTheDataSourceAndReportsSuccess() {
    val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    Dispatchers.setMain(dispatcher)
    try {
      runTest(dispatcher) {
        val source = RecordingDemoDataSource()
        val viewModel = AlluViewModel(source)

        viewModel.setImportFeedSuffix("1234")
        runCurrent()

        assertEquals(listOf("suffix:1234"), source.actions)
        assertEquals("Filtro pelos quatro dígitos finais salvo", viewModel.notice.value)
      }
    } finally {
      Dispatchers.resetMain()
    }
  }
}

private class RecordingDemoDataSource(private val failOn: String? = null) : DemoDataSource {
  val actions = mutableListOf<String>()
  override val ready: StateFlow<Boolean> = MutableStateFlow(true)
  override val error: StateFlow<String?> = MutableStateFlow(null)
  override val accounts: Flow<List<AccountSummary>> = MutableStateFlow(emptyList())
  override val transactions: Flow<List<Transaction>> = MutableStateFlow(emptyList())
  override val categories: Flow<List<Category>> = MutableStateFlow(emptyList())
  override val budgets: Flow<List<Budget>> = MutableStateFlow(emptyList())
  override val alerts: Flow<List<Alert>> = MutableStateFlow(emptyList())
  override val reconciliationCases: Flow<List<ReconciliationCase>> = MutableStateFlow(emptyList())
  override val goals: Flow<List<Goal>> = MutableStateFlow(emptyList())
  override val messages: Flow<List<AssistantMessage>> = MutableStateFlow(emptyList())
  override val preferences: Flow<DisplayPreferences> = MutableStateFlow(DisplayPreferences())
  override val materializedAt: Flow<Instant?> = MutableStateFlow(null)

  override suspend fun saveTransaction(transaction: Transaction) = record("save:${transaction.id}")

  override suspend fun deleteTransaction(id: String) = record("delete:$id")

  override suspend fun updateBudget(categoryId: String, limitCents: Long) =
      record("budget:$categoryId:$limitCents")

  override suspend fun setTransactionCategory(transactionId: String, categoryId: String) =
      record("category:$transactionId:$categoryId")

  override suspend fun decideReconciliationCase(caseId: String) = record("decide:$caseId")

  override suspend fun reopenReconciliationCase(caseId: String) = record("reopen:$caseId")

  override suspend fun setAlertDismissed(alertId: String, dismissed: Boolean) =
      record("alert:$alertId:$dismissed")

  override suspend fun sendMessage(text: String): AssistantAnswer {
    record("message:$text")
    return AssistantAnswer("resposta")
  }

  override suspend fun resetDemoData() = record("reset")

  override suspend fun setDarkTheme(enabled: Boolean) = record("dark:$enabled")

  override suspend fun setReduceMotion(enabled: Boolean) = record("motion:$enabled")

  override suspend fun setShowCents(enabled: Boolean) = record("cents:$enabled")

  override suspend fun setImportFeedSuffix(suffix: String?) = record("suffix:$suffix")

  private fun record(action: String) {
    actions += action
    if (action == failOn) error("falha simulada")
  }
}
