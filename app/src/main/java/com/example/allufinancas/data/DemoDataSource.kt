package com.example.allufinancas.data

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

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
  val preferences: Flow<DisplayPreferences>
  val materializedAt: Flow<Instant?>
  /** The one source the owner has enabled locally, separate from Android's system permission. */
  val enabledImportSource: Flow<NotificationImportSource?>
    get() = flowOf(null)

  /**
   * Optional last four digits identifying the selected source feed; never a full account number.
   */
  val importFeedSuffix: Flow<String?>
    get() = flowOf(null)

  /** Draft imports stay separate from [transactions] until the owner confirms one. */
  val importNotices: Flow<List<ImportNotice>>
    get() = flowOf(emptyList())

  suspend fun saveTransaction(transaction: Transaction)

  suspend fun deleteTransaction(id: String)

  suspend fun updateBudget(categoryId: String, limitCents: Long)

  suspend fun setTransactionCategory(transactionId: String, categoryId: String)

  suspend fun decideReconciliationCase(caseId: String)

  suspend fun reopenReconciliationCase(caseId: String)

  suspend fun setAlertDismissed(alertId: String, dismissed: Boolean)

  suspend fun resetDemoData()

  suspend fun setDarkTheme(enabled: Boolean)

  suspend fun setReduceMotion(enabled: Boolean)

  suspend fun setShowCents(enabled: Boolean)

  suspend fun enableImportSource(source: NotificationImportSource) = Unit

  suspend fun disableImportSource() = Unit

  suspend fun setImportFeedSuffix(suffix: String?) = Unit

  /** Returns the one resulting ledger ID, including when a repeated confirmation is replayed. */
  suspend fun confirmImportNotice(
      noticeId: String,
      confirmation: ImportNoticeConfirmation,
  ): String? = null

  suspend fun resolveImportNotice(noticeId: String, resolution: ImportNoticeResolution) = Unit

  suspend fun clearPendingImports() = Unit
}
