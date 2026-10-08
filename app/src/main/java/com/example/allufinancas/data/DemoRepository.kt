package com.example.allufinancas.data

import android.content.Context
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The app's only financial source: user-entered data in a local Room database. */
class DemoRepository(context: Context, private val clock: Clock = Clock.systemUTC()) :
    DemoDataSource {
  private val database = DemoDatabase.open(context)
  private val dao = database.dao()
  private val settings = DemoPreferences(context)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val _ready = MutableStateFlow(false)
  private val _error = MutableStateFlow<String?>(null)

  override val ready: StateFlow<Boolean> = _ready
  override val error: StateFlow<String?> = _error
  override val accounts: Flow<List<AccountSummary>> =
      dao.accounts().map { rows -> rows.map { it.toModel() } }
  private val sourceTransactions: Flow<List<Transaction>> =
      dao.transactions().map { rows -> rows.map { it.toModel() } }
  private val reviewDecisions = dao.reconciliationDecisions()
  override val transactions: Flow<List<Transaction>> =
      kotlinx.coroutines.flow.combine(sourceTransactions, reviewDecisions) { rows, decisions ->
        ReconciliationRules.effectiveTransactions(rows, decisions)
      }
  override val categories: Flow<List<Category>> =
      dao.categories().map { rows -> rows.map { it.toModel() } }
  override val budgets: Flow<List<Budget>> = dao.budgets().map { rows -> rows.map { it.toModel() } }
  override val alerts: Flow<List<Alert>> = dao.alerts().map { rows -> rows.map { it.toModel() } }
  override val reconciliationCases: Flow<List<ReconciliationCase>> =
      kotlinx.coroutines.flow.combine(sourceTransactions, reviewDecisions) { rows, decisions ->
        ReconciliationRules.cases(rows, decisions)
      }
  override val goals: Flow<List<Goal>> = dao.goals().map { rows -> rows.map { it.toModel() } }
  override val messages: Flow<List<AssistantMessage>> =
      dao.messages().map { rows -> rows.map { it.toModel() } }
  override val preferences: Flow<DisplayPreferences> = settings.state
  override val materializedAt: Flow<Instant?> = settings.materializedAt
  override val enabledImportSource: Flow<NotificationImportSource?> = settings.enabledImportSource
  override val importFeedSuffix: Flow<String?> = settings.importFeedSuffix
  override val importNotices: Flow<List<ImportNotice>> =
      dao.importNotices().map { rows -> rows.map { it.toModel() } }

  init {
    scope.launch {
      try {
        val initialized = database.withTransaction {
          if (dao.categoryCount() == 0) {
            putDefaultCategoriesLocked()
            true
          } else false
        }
        if (initialized) recordMaterializedAt()
        _ready.value = true
      } catch (exception: Exception) {
        _error.value = exception.message ?: "Falha ao abrir os dados locais"
      }
    }
  }

  override suspend fun saveTransaction(transaction: Transaction) {
    val merchant = transaction.merchant.trim()
    val description = transaction.description.trim()
    require(transaction.id.isNotBlank()) { "Identificador inválido" }
    require(
        transaction.amountCents != 0L &&
            transaction.amountCents in -MAX_AMOUNT_CENTS..MAX_AMOUNT_CENTS
    ) {
      "Informe um valor diferente de zero dentro do limite permitido"
    }
    require(merchant.isNotEmpty() && merchant.length <= MAX_MERCHANT_LENGTH) {
      "Informe um estabelecimento com até $MAX_MERCHANT_LENGTH caracteres"
    }
    require(description.length <= MAX_DESCRIPTION_LENGTH) {
      "A descrição deve ter até $MAX_DESCRIPTION_LENGTH caracteres"
    }
    val installmentNumber = transaction.installmentNumber
    val installmentCount = transaction.installmentCount
    require(
        (installmentNumber == null && installmentCount == null) ||
            (installmentNumber != null &&
                installmentCount != null &&
                installmentCount in 1..MAX_INSTALLMENTS &&
                installmentNumber in 1..installmentCount)
    ) {
      "Informe a parcela atual e o total: 1 ≤ atual ≤ total ≤ $MAX_INSTALLMENTS"
    }
    require(!transaction.recurring || transaction.amountCents < 0) {
      "Marque recorrência apenas em uma despesa"
    }
    require(
        runCatching { LocalDate.parse(transaction.date).toString() == transaction.date }
            .getOrDefault(false)
    ) {
      "Informe uma data válida no formato AAAA-MM-DD"
    }
    NotificationImportGate.fence {
      database.withTransaction {
        val existing = dao.allTransactions().firstOrNull { it.id == transaction.id }
        val importedExisting = existing?.origin == TransactionOrigin.NOTIFICATION_IMPORT.name
        require(
            if (importedExisting) {
              transaction.origin == TransactionOrigin.NOTIFICATION_IMPORT &&
                  transaction.importNoticeId == existing?.importNoticeId
            } else {
              transaction.origin == TransactionOrigin.MANUAL && transaction.importNoticeId == null
            },
        ) {
          "A origem do lançamento não pode ser alterada"
        }
        if (importedExisting) {
          val importNoticeId =
              requireNotNull(existing?.importNoticeId) {
                "Lançamento importado sem referência de origem"
              }
          val kind =
              dao.importNotice(importNoticeId)?.kind?.let(ImportNoticeKind::valueOf)
                  ?: error("Importação de origem não encontrada")
          require(
              (kind == ImportNoticeKind.EXPENSE && transaction.amountCents < 0) ||
                  (kind == ImportNoticeKind.INCOME && transaction.amountCents > 0),
          ) {
            "O valor precisa manter o tipo indicado pela notificação"
          }
        }
        require(dao.allCategories().any { it.id == transaction.categoryId }) {
          "Categoria desconhecida"
        }
        require(
            TransactionCategoryRules.isCompatible(transaction.categoryId, transaction.amountCents)
        ) {
          "A categoria precisa corresponder a uma entrada ou saída"
        }
        val cleaned =
            transaction.copy(
                accountId = if (importedExisting) IMPORT_ACCOUNT_ID else MANUAL_ACCOUNT_ID,
                merchant = merchant,
                description = description,
                sourceCategoryId = transaction.categoryId,
                recurring = transaction.recurring,
                installmentNumber = installmentNumber,
                installmentCount = installmentCount,
                origin =
                    if (importedExisting) TransactionOrigin.NOTIFICATION_IMPORT
                    else TransactionOrigin.MANUAL,
                importNoticeId = if (importedExisting) existing?.importNoticeId else null,
            )
        dao.putTransactions(listOf(cleaned.toRow()))
        dao.deleteReconciliationDecision("uncategorized-${transaction.id}")
        dao.deleteReconciliationDecision("category-${transaction.id}")
        dao.clearRuleDecisions()
        refreshAlertsLocked()
      }
    }
    recordMaterializedAt()
  }

  override suspend fun deleteTransaction(id: String) {
    NotificationImportGate.fence {
      database.withTransaction {
        val existing = dao.transaction(id) ?: error("Lançamento desconhecido")
        val importNoticeId =
            if (existing.origin == TransactionOrigin.NOTIFICATION_IMPORT.name) {
              requireNotNull(existing.importNoticeId) {
                "Lançamento importado sem referência de origem"
              }
            } else null
        require(dao.deleteTransaction(id) == 1) { "Lançamento desconhecido" }
        if (importNoticeId != null) {
          require(dao.reopenConfirmedImportNotice(importNoticeId, id) == 1) {
            "Não foi possível reabrir a importação removida"
          }
        }
        dao.deleteReconciliationDecision("uncategorized-$id")
        dao.deleteReconciliationDecision("category-$id")
        dao.clearRuleDecisions()
        refreshAlertsLocked()
      }
    }
    recordMaterializedAt()
  }

  override suspend fun updateBudget(categoryId: String, limitCents: Long) {
    require(limitCents in 1..MAX_AMOUNT_CENTS) { "Informe um limite positivo válido" }
    database.withTransaction {
      require(dao.allCategories().any { it.id == categoryId }) { "Categoria desconhecida" }
      dao.putBudgets(listOf(BudgetRow(categoryId, limitCents)))
      refreshAlertsLocked()
    }
    recordMaterializedAt()
  }

  override suspend fun setTransactionCategory(transactionId: String, categoryId: String) {
    database.withTransaction {
      val transaction =
          dao.allTransactions().firstOrNull { it.id == transactionId }
              ?: error("Lançamento desconhecido")
      require(dao.allCategories().any { it.id == categoryId }) { "Categoria desconhecida" }
      require(TransactionCategoryRules.isCompatible(categoryId, transaction.amountCents)) {
        "A categoria precisa corresponder a uma entrada ou saída"
      }
      val caseId =
          if (transaction.categoryId.isBlank()) "uncategorized-$transactionId"
          else "category-$transactionId"
      val kind =
          if (transaction.categoryId.isBlank()) ReviewKind.UNCATEGORIZED.name
          else "CATEGORY_OVERRIDE"
      dao.putReconciliationDecision(
          ReconciliationDecisionRow(caseId, kind, listOf(transactionId), categoryId),
      )
      refreshAlertsLocked()
    }
    recordMaterializedAt()
  }

  override suspend fun decideReconciliationCase(caseId: String) {
    database.withTransaction {
      val transactions = dao.allTransactions().map { it.toModel() }
      val knownCase =
          ReconciliationRules.cases(transactions).firstOrNull { it.id == caseId }
              ?: error("Item de revisão desconhecido")
      require(knownCase.kind != ReviewKind.UNCATEGORIZED) {
        "Escolha uma categoria para este lançamento"
      }
      dao.putReconciliationDecision(
          ReconciliationDecisionRow(
              knownCase.id,
              knownCase.kind.name,
              knownCase.transactionIds,
              null,
          ),
      )
    }
    recordMaterializedAt()
  }

  override suspend fun reopenReconciliationCase(caseId: String) {
    database.withTransaction {
      require(dao.deleteReconciliationDecision(caseId) == 1) { "Decisão desconhecida" }
      refreshAlertsLocked()
    }
    recordMaterializedAt()
  }

  override suspend fun setAlertDismissed(alertId: String, dismissed: Boolean) {
    require(dao.setAlertDismissed(alertId, dismissed) == 1) { "Alerta desconhecido" }
    recordMaterializedAt()
  }

  override suspend fun sendMessage(text: String): AssistantAnswer {
    val prompt = text.trim()
    require(prompt.isNotEmpty()) { "Digite uma pergunta" }
    require(prompt.length <= 500) { "Pergunta muito longa" }
    val answer = database.withTransaction {
      val answer =
          LocalAssistant.answer(
              prompt,
              effectiveTransactionsLocked(),
              dao.allCategories().map { it.toModel() },
              dao.allBudgets().map { it.toModel() },
          )
      val now = LocalDateTime.now().toString()
      dao.putMessage(
          MessageRow(
              role = MessageRole.USER.name,
              text = prompt,
              createdAt = now,
              citationTransactionIds = emptyList(),
          )
      )
      dao.putMessage(
          MessageRow(
              role = MessageRole.ASSISTANT.name,
              text = answer.text,
              createdAt = now,
              citationTransactionIds = answer.citationTransactionIds,
          )
      )
      answer
    }
    recordMaterializedAt()
    return answer
  }

  override suspend fun resetDemoData() {
    NotificationImportGate.fence {
      // Disable capture and clear all drafts in one fence, so a capture cannot survive reset.
      settings.setEnabledImportSource(null)
      database.withTransaction {
        dao.clearImportNotices()
        dao.clearMessages()
        dao.clearAlerts()
        dao.clearReconciliationDecisions()
        dao.clearBudgets()
        dao.clearTransactions()
        dao.clearCategories()
        dao.clearGoals()
        dao.clearAccounts()
        putDefaultCategoriesLocked()
      }
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

  override suspend fun enableImportSource(source: NotificationImportSource) {
    require(source == NotificationImportSource.RICO) { "Fonte de importação não suportada" }
    NotificationImportGate.fence { settings.setEnabledImportSource(source) }
  }

  override suspend fun disableImportSource() {
    NotificationImportGate.fence { settings.setEnabledImportSource(null) }
  }

  override suspend fun setImportFeedSuffix(suffix: String?) {
    NotificationImportGate.fence {
      if (!suffix.isNullOrBlank()) {
        require(settings.enabledImportSource.first() == NotificationImportSource.RICO) {
          "Selecione a Rico antes de definir os quatro últimos dígitos"
        }
      }
      settings.setImportFeedSuffix(suffix)
    }
  }

  override suspend fun confirmImportNotice(
      noticeId: String,
      confirmation: ImportNoticeConfirmation,
  ): String? {
    val cleaned = confirmation.cleaned()
    val result: String? = NotificationImportGate.fence {
      database.withTransaction {
        val notice = dao.importNotice(noticeId) ?: error("Importação desconhecida")
        when (ImportNoticeStatus.valueOf(notice.status)) {
          ImportNoticeStatus.CONFIRMED ->
              return@withTransaction notice.confirmedTransactionId
                  ?: dao.transactionIdForImportNotice(notice.id)
                  ?: error("Importação confirmada sem lançamento")
          ImportNoticeStatus.IGNORED,
          ImportNoticeStatus.NEUTRAL -> error("Esta importação já foi resolvida")
          ImportNoticeStatus.PENDING -> Unit
        }
        val kind = ImportNoticeKind.valueOf(notice.kind)
        require(kind in setOf(ImportNoticeKind.EXPENSE, ImportNoticeKind.INCOME)) {
          "Somente uma entrada ou saída revisada pode entrar no extrato"
        }
        require(
            (kind == ImportNoticeKind.EXPENSE && cleaned.amountCents < 0) ||
                (kind == ImportNoticeKind.INCOME && cleaned.amountCents > 0),
        ) {
          "O valor confirmado precisa manter o tipo indicado pela notificação"
        }
        require(dao.allCategories().any { it.id == cleaned.categoryId }) {
          "Categoria desconhecida"
        }
        require(TransactionCategoryRules.isCompatible(cleaned.categoryId, cleaned.amountCents)) {
          "A categoria precisa corresponder a uma entrada ou saída"
        }

        val existingId = dao.transactionIdForImportNotice(notice.id)
        if (existingId != null) {
          dao.resolvePendingImportNotice(notice.id, ImportNoticeStatus.CONFIRMED.name, existingId)
          return@withTransaction existingId
        }
        if (hasLedgerCollision(notice.id, cleaned) && !cleaned.acceptPossibleDuplicate) {
          dao.markImportNoticePossibleDuplicate(notice.id)
          return@withTransaction null
        }
        val transactionId = "import-${notice.sourceEventHash}"
        val inserted =
            dao.insertImportedTransaction(
                TransactionRow(
                    id = transactionId,
                    accountId = IMPORT_ACCOUNT_ID,
                    merchant = cleaned.merchant,
                    description = cleaned.description,
                    amountCents = cleaned.amountCents,
                    date = cleaned.date,
                    categoryId = cleaned.categoryId,
                    installmentNumber = null,
                    installmentCount = null,
                    recurring = false,
                    origin = TransactionOrigin.NOTIFICATION_IMPORT.name,
                    importNoticeId = notice.id,
                ),
            )
        val storedId =
            if (inserted != -1L) transactionId
            else {
              dao.transactionIdForImportNotice(notice.id)
                  ?: error("Não foi possível confirmar a importação")
            }
        require(
            dao.resolvePendingImportNotice(
                notice.id,
                ImportNoticeStatus.CONFIRMED.name,
                storedId,
            ) == 1
        ) {
          "Não foi possível finalizar a importação"
        }
        dao.clearRuleDecisions()
        refreshAlertsLocked()
        storedId
      }
    }
    recordMaterializedAt()
    return result
  }

  override suspend fun resolveImportNotice(noticeId: String, resolution: ImportNoticeResolution) {
    val status =
        when (resolution) {
          ImportNoticeResolution.IGNORED -> ImportNoticeStatus.IGNORED
          ImportNoticeResolution.NEUTRAL -> ImportNoticeStatus.NEUTRAL
        }
    val changed = NotificationImportGate.fence {
      database.withTransaction {
        val notice = dao.importNotice(noticeId) ?: error("Importação desconhecida")
        val current = ImportNoticeStatus.valueOf(notice.status)
        when {
          current == status -> false
          current != ImportNoticeStatus.PENDING -> error("Esta importação já foi resolvida")
          resolution == ImportNoticeResolution.NEUTRAL &&
              ImportNoticeKind.valueOf(notice.kind) != ImportNoticeKind.NEUTRAL ->
              error("Apenas pagamentos de fatura ou transferências podem ser neutros")
          else -> dao.resolvePendingImportNotice(notice.id, status.name) == 1
        }
      }
    }
    if (changed) recordMaterializedAt()
  }

  override suspend fun clearPendingImports() {
    NotificationImportGate.fence {
      database.withTransaction { dao.clearPendingImportNotices() }
    }
    recordMaterializedAt()
  }

  private suspend fun putDefaultCategoriesLocked() {
    dao.putCategories(DefaultCategories.all.map { CategoryRow(it.id, it.name, it.colorHex) })
  }

  private suspend fun refreshAlertsLocked() {
    val dismissed = dao.allAlerts().associate { it.id to it.dismissed }
    val calculated =
        DemoAnalysis.insights(
            effectiveTransactionsLocked(),
            dao.allCategories().map { it.toModel() },
            dao.allBudgets().map { it.toModel() },
            YearMonth.now(clock).toString(),
        )
    dao.clearAlerts()
    dao.putAlerts(
        calculated.map {
          AlertRow(
              it.id,
              it.title,
              it.explanation,
              it.severity.name,
              it.transactionIds,
              LocalDate.now(clock).toString(),
              dismissed[it.id] ?: false,
          )
        }
    )
  }

  private suspend fun effectiveTransactionsLocked(): List<Transaction> =
      ReconciliationRules.effectiveTransactions(
          dao.allTransactions().map { it.toModel() },
          dao.allReconciliationDecisions(),
      )

  private fun ImportNoticeConfirmation.cleaned(): ImportNoticeConfirmation {
    val merchant = merchant.trim()
    val description = description.trim()
    require(merchant.isNotEmpty() && merchant.length <= MAX_MERCHANT_LENGTH) {
      "Informe um estabelecimento com até $MAX_MERCHANT_LENGTH caracteres"
    }
    require(description.length <= MAX_DESCRIPTION_LENGTH) {
      "A descrição deve ter até $MAX_DESCRIPTION_LENGTH caracteres"
    }
    require(amountCents != 0L && amountCents in -MAX_AMOUNT_CENTS..MAX_AMOUNT_CENTS) {
      "Informe um valor diferente de zero dentro do limite permitido"
    }
    require(runCatching { LocalDate.parse(date).toString() == date }.getOrDefault(false)) {
      "Informe uma data válida no formato AAAA-MM-DD"
    }
    return copy(merchant = merchant, description = description)
  }

  private suspend fun hasLedgerCollision(
      noticeId: String,
      confirmation: ImportNoticeConfirmation,
  ): Boolean {
    val merchant = confirmation.merchant.normalizedForDuplicate()
    return dao.allTransactions().any { transaction ->
      transaction.importNoticeId != noticeId &&
          transaction.amountCents == confirmation.amountCents &&
          transaction.date == confirmation.date &&
          transaction.merchant.normalizedForDuplicate() == merchant
    }
  }
}

private const val MANUAL_ACCOUNT_ID = "manual-ledger"
private const val IMPORT_ACCOUNT_ID = "rico-notification"
private const val MAX_AMOUNT_CENTS = 1_000_000_000_000L
private const val MAX_INSTALLMENTS = 120
private const val MAX_MERCHANT_LENGTH = 120
private const val MAX_DESCRIPTION_LENGTH = 300

private fun String.normalizedForDuplicate(): String =
    trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

private fun AccountRow.toModel() = AccountSummary(id, name, kind, balanceCents)

private fun TransactionRow.toModel() =
    Transaction(
        id,
        accountId,
        merchant,
        description,
        amountCents,
        date,
        categoryId,
        installmentNumber,
        installmentCount,
        recurring,
        sourceCategoryId = categoryId,
        origin = TransactionOrigin.valueOf(origin),
        importNoticeId = importNoticeId,
    )

private fun Transaction.toRow() =
    TransactionRow(
        id,
        accountId,
        merchant,
        description,
        amountCents,
        date,
        sourceCategoryId,
        installmentNumber,
        installmentCount,
        recurring,
        origin.name,
        importNoticeId,
    )

private fun ImportNoticeRow.toModel() =
    ImportNotice(
        id = id,
        source = NotificationImportSource.valueOf(source),
        sourceEventHash = sourceEventHash,
        fingerprintHash = fingerprintHash,
        merchant = merchant,
        description = description,
        amountCents = amountCents,
        date = date,
        kind = ImportNoticeKind.valueOf(kind),
        status = ImportNoticeStatus.valueOf(status),
        possibleDuplicate = possibleDuplicate,
        observedAt = observedAt,
        confirmedTransactionId = confirmedTransactionId,
    )

private fun CategoryRow.toModel() = Category(id, name, colorHex)

private fun BudgetRow.toModel() = Budget(categoryId, limitCents)

private fun AlertRow.toModel() =
    Alert(
        id,
        title,
        explanation,
        InsightSeverity.valueOf(severity),
        transactionIds,
        generatedDate,
        dismissed,
    )

private fun GoalRow.toModel() = Goal(id, name, targetCents, savedCents)

private fun MessageRow.toModel() =
    AssistantMessage(
        id,
        MessageRole.valueOf(role),
        text,
        createdAt,
        citationTransactionIds,
    )
