package com.example.allufinancas.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/** The app's only financial source: bundled examples in a local Room database. */
class DemoRepository(context: Context, private val clock: Clock = Clock.systemUTC()) : DemoDataSource {
    private val database = DemoDatabase.open(context)
    private val dao = database.dao()
    private val settings = DemoPreferences(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _ready = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    override val ready: StateFlow<Boolean> = _ready
    override val error: StateFlow<String?> = _error
    override val accounts: Flow<List<AccountSummary>> = dao.accounts().map { rows -> rows.map { it.toModel() } }
    private val sourceTransactions: Flow<List<Transaction>> = dao.transactions().map { rows -> rows.map { it.toModel() } }
    private val reviewDecisions = dao.reconciliationDecisions()
    override val transactions: Flow<List<Transaction>> = kotlinx.coroutines.flow.combine(sourceTransactions, reviewDecisions) { rows, decisions ->
        ReconciliationRules.effectiveTransactions(rows, decisions)
    }
    override val categories: Flow<List<Category>> = dao.categories().map { rows -> rows.map { it.toModel() } }
    override val budgets: Flow<List<Budget>> = dao.budgets().map { rows -> rows.map { it.toModel() } }
    override val alerts: Flow<List<Alert>> = dao.alerts().map { rows -> rows.map { it.toModel() } }
    override val reconciliationCases: Flow<List<ReconciliationCase>> = kotlinx.coroutines.flow.combine(sourceTransactions, reviewDecisions) { rows, decisions ->
        ReconciliationRules.cases(rows, decisions)
    }
    override val goals: Flow<List<Goal>> = dao.goals().map { rows -> rows.map { it.toModel() } }
    override val messages: Flow<List<AssistantMessage>> = dao.messages().map { rows -> rows.map { it.toModel() } }
    override val preferences: Flow<DisplayPreferences> = settings.state
    override val materializedAt: Flow<Instant?> = settings.materializedAt

    init {
        scope.launch {
            try {
                if (dao.accountCount() == 0) settings.clearMaterializedAt()
                val seeded = database.withTransaction {
                    if (dao.accountCount() == 0) {
                        seedLocked()
                        true
                    } else false
                }
                if (seeded) recordMaterializedAt()
                _ready.value = true
            } catch (exception: Exception) {
                _error.value = exception.message ?: "Falha ao abrir os dados da demonstração"
            }
        }
    }

    override suspend fun updateBudget(categoryId: String, limitCents: Long) {
        require(limitCents >= 0) { "O limite não pode ser negativo" }
        database.withTransaction {
            require(dao.allCategories().any { it.id == categoryId }) { "Categoria desconhecida" }
            dao.putBudgets(listOf(BudgetRow(categoryId, limitCents)))
            refreshAlertsLocked()
        }
    }

    override suspend fun setTransactionCategory(transactionId: String, categoryId: String) {
        database.withTransaction {
            require(dao.allCategories().any { it.id == categoryId }) { "Categoria desconhecida" }
            val transaction = dao.allTransactions().firstOrNull { it.id == transactionId }
                ?: error("Lançamento desconhecido")
            val caseId = if (transaction.categoryId.isBlank()) "uncategorized-$transactionId" else "category-$transactionId"
            val kind = if (transaction.categoryId.isBlank()) ReviewKind.UNCATEGORIZED.name else "CATEGORY_OVERRIDE"
            dao.putReconciliationDecision(
                ReconciliationDecisionRow(caseId, kind, listOf(transactionId), categoryId),
            )
            refreshAlertsLocked()
        }
    }

    override suspend fun decideReconciliationCase(caseId: String) {
        database.withTransaction {
            val transactions = dao.allTransactions().map { it.toModel() }
            val knownCase = ReconciliationRules.cases(transactions).firstOrNull { it.id == caseId }
                ?: error("Item de revisão desconhecido")
            require(knownCase.kind != ReviewKind.UNCATEGORIZED) { "Escolha uma categoria para este lançamento" }
            dao.putReconciliationDecision(
                ReconciliationDecisionRow(knownCase.id, knownCase.kind.name, knownCase.transactionIds, null),
            )
        }
    }

    override suspend fun reopenReconciliationCase(caseId: String) {
        database.withTransaction {
            require(dao.deleteReconciliationDecision(caseId) == 1) { "Decisão desconhecida" }
            refreshAlertsLocked()
        }
    }

    override suspend fun setAlertDismissed(alertId: String, dismissed: Boolean) {
        require(dao.setAlertDismissed(alertId, dismissed) == 1) { "Alerta desconhecido" }
    }

    override suspend fun sendMessage(text: String): AssistantAnswer {
        val prompt = text.trim()
        require(prompt.isNotEmpty()) { "Digite uma pergunta" }
        require(prompt.length <= 500) { "Pergunta muito longa" }
        return database.withTransaction {
            val answer = LocalAssistant.answer(
                prompt,
                effectiveTransactionsLocked(),
                dao.allCategories().map { it.toModel() },
                dao.allBudgets().map { it.toModel() },
            )
            val now = LocalDateTime.now().toString()
            dao.putMessage(MessageRow(role = MessageRole.USER.name, text = prompt, createdAt = now, citationTransactionIds = emptyList()))
            dao.putMessage(MessageRow(role = MessageRole.ASSISTANT.name, text = answer.text, createdAt = now, citationTransactionIds = answer.citationTransactionIds))
            answer
        }
    }

    override suspend fun resetDemoData() {
        settings.clearMaterializedAt()
        database.withTransaction {
            dao.clearMessages()
            dao.clearAlerts()
            dao.clearReconciliationDecisions()
            dao.clearBudgets()
            dao.clearTransactions()
            dao.clearCategories()
            dao.clearGoals()
            dao.clearAccounts()
            seedLocked()
        }
        recordMaterializedAt()
    }

    private suspend fun recordMaterializedAt() {
        try {
            settings.setMaterializedAt(clock.instant())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // The database commit remains authoritative; unavailable is safer than stale metadata.
        }
    }

    override suspend fun setDarkTheme(enabled: Boolean) = settings.setDarkTheme(enabled)
    override suspend fun setReduceMotion(enabled: Boolean) = settings.setReduceMotion(enabled)
    override suspend fun setShowCents(enabled: Boolean) = settings.setShowCents(enabled)

    private suspend fun seedLocked() {
        dao.putAccounts(DemoSeed.accounts.map { AccountRow(it.id, it.name, it.kind, it.balanceCents) })
        dao.putCategories(DemoSeed.categories.map { CategoryRow(it.id, it.name, it.colorHex) })
        dao.putTransactions(DemoSeed.transactions.map { it.toRow() })
        dao.putBudgets(DemoSeed.budgets.map { BudgetRow(it.categoryId, it.limitCents) })
        dao.putGoals(DemoSeed.goals.map { GoalRow(it.id, it.name, it.targetCents, it.savedCents) })
        refreshAlertsLocked()
    }

    private suspend fun refreshAlertsLocked() {
        val dismissed = dao.allAlerts().associate { it.id to it.dismissed }
        val calculated = DemoAnalysis.insights(
            effectiveTransactionsLocked(),
            dao.allCategories().map { it.toModel() },
            dao.allBudgets().map { it.toModel() },
            DemoSeed.MONTH,
        )
        dao.clearAlerts()
        dao.putAlerts(calculated.map {
            AlertRow(it.id, it.title, it.explanation, it.severity.name, it.transactionIds,
                DemoSeed.GENERATED_DATE, dismissed[it.id] ?: false)
        })
    }

    private suspend fun effectiveTransactionsLocked(): List<Transaction> = ReconciliationRules.effectiveTransactions(
        dao.allTransactions().map { it.toModel() },
        dao.allReconciliationDecisions(),
    )
}

private fun AccountRow.toModel() = AccountSummary(id, name, kind, balanceCents)
private fun TransactionRow.toModel() = Transaction(id, accountId, merchant, description, amountCents,
    date, categoryId, installmentNumber, installmentCount, recurring, sourceCategoryId = categoryId)
private fun Transaction.toRow() = TransactionRow(id, accountId, merchant, description, amountCents,
    date, sourceCategoryId, installmentNumber, installmentCount, recurring)
private fun CategoryRow.toModel() = Category(id, name, colorHex)
private fun BudgetRow.toModel() = Budget(categoryId, limitCents)
private fun AlertRow.toModel() = Alert(id, title, explanation, InsightSeverity.valueOf(severity),
    transactionIds, generatedDate, dismissed)
private fun GoalRow.toModel() = Goal(id, name, targetCents, savedCents)
private fun MessageRow.toModel() = AssistantMessage(id, MessageRole.valueOf(role), text, createdAt,
    citationTransactionIds)
