package com.example.allufinancas.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/** UI-facing contract; Android Context stays inside the repository. */
interface DemoDataSource {
    val ready: StateFlow<Boolean>
    val error: StateFlow<String?>
    val accounts: Flow<List<AccountSummary>>
    val transactions: Flow<List<Transaction>>
    val categories: Flow<List<Category>>
    val budgets: Flow<List<Budget>>
    val alerts: Flow<List<Alert>>
    val reconciliationCases: Flow<List<ReconciliationCase>>
    val goals: Flow<List<Goal>>
    val messages: Flow<List<AssistantMessage>>
    val preferences: Flow<DisplayPreferences>
    val materializedAt: Flow<Instant?>

    suspend fun updateBudget(categoryId: String, limitCents: Long)
    suspend fun setTransactionCategory(transactionId: String, categoryId: String)
    suspend fun decideReconciliationCase(caseId: String)
    suspend fun reopenReconciliationCase(caseId: String)
    suspend fun setAlertDismissed(alertId: String, dismissed: Boolean)
    suspend fun sendMessage(text: String): AssistantAnswer
    suspend fun resetDemoData()
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setReduceMotion(enabled: Boolean)
    suspend fun setShowCents(enabled: Boolean)
}
