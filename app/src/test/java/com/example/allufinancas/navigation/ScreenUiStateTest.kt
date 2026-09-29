package com.example.allufinancas.navigation

import com.example.allufinancas.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ScreenUiStateTest {
    @Test
    fun reportsLoadingUntilTheDemoRepositoryIsReady() = runBlocking {
        val source = FakeDemoDataSource()

        val state = observeScreenUiState(
            source.ready, source.error, source.transactions,
        ) { it.isEmpty() }.first()

        assertEquals(ScreenUiState.Loading, state)
    }

    @Test
    fun reportsContentAfterReadinessAndDataArrival() = runBlocking {
        val source = FakeDemoDataSource().apply {
            ready.value = true
            transactions.value = listOf(sampleTransaction)
        }

        val state = observeScreenUiState(
            source.ready, source.error, source.transactions,
        ) { it.isEmpty() }.first()

        assertEquals(ScreenUiState.Content(listOf(sampleTransaction)), state)
    }

    @Test
    fun reportsEmptyAsADistinctReadyState() = runBlocking {
        val source = FakeDemoDataSource().apply { ready.value = true }

        val state = observeScreenUiState(
            source.ready, source.error, source.transactions,
        ) { it.isEmpty() }.first()

        assertEquals(ScreenUiState.Empty(emptyList<Transaction>()), state)
    }

    @Test
    fun startupAndFlowFailuresAreExplicitErrors() = runBlocking {
        val source = FakeDemoDataSource().apply { error.value = "Room indisponível" }
        val startupState = observeScreenUiState(
            source.ready, source.error, source.transactions,
        ) { it.isEmpty() }.first()
        val streamState = observeScreenUiState(
            source.ready, MutableStateFlow<String?>(null),
            flow<List<Transaction>> { error("falha simulada") },
        ) { it.isEmpty() }.first()

        assertEquals(ScreenUiState.Error("Room indisponível"), startupState)
        assertEquals(ScreenUiState.Error("falha simulada"), streamState)
    }
}

private val sampleTransaction = Transaction(
    id = "tx-test", accountId = "account-test", merchant = "Loja Exemplo",
    description = "Compra fictícia", amountCents = -1250, date = "2026-09-01",
    categoryId = "food",
)

private class FakeDemoDataSource : DemoDataSource {
    override val ready = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)
    override val accounts = MutableStateFlow(emptyList<AccountSummary>())
    override val transactions = MutableStateFlow(emptyList<Transaction>())
    override val categories = MutableStateFlow(emptyList<Category>())
    override val budgets = MutableStateFlow(emptyList<Budget>())
    override val alerts = MutableStateFlow(emptyList<Alert>())
    override val reconciliationCases = MutableStateFlow(emptyList<ReconciliationCase>())
    override val goals = MutableStateFlow(emptyList<Goal>())
    override val messages = MutableStateFlow(emptyList<AssistantMessage>())
    override val preferences = MutableStateFlow(DisplayPreferences())
    override val materializedAt = MutableStateFlow<Instant?>(null)

    override suspend fun updateBudget(categoryId: String, limitCents: Long) = Unit
    override suspend fun setTransactionCategory(transactionId: String, categoryId: String) = Unit
    override suspend fun decideReconciliationCase(caseId: String) = Unit
    override suspend fun reopenReconciliationCase(caseId: String) = Unit
    override suspend fun setAlertDismissed(alertId: String, dismissed: Boolean) = Unit
    override suspend fun sendMessage(text: String) = AssistantAnswer("Resposta fictícia")
    override suspend fun resetDemoData() = Unit
    override suspend fun setDarkTheme(enabled: Boolean) = Unit
    override suspend fun setReduceMotion(enabled: Boolean) = Unit
    override suspend fun setShowCents(enabled: Boolean) = Unit
}
