package com.example.allufinancas.data

data class AccountSummary(
    val id: String,
    val name: String,
    val kind: String,
    val balanceCents: Long,
)

data class Transaction(
    val id: String,
    val accountId: String,
    val merchant: String,
    val description: String,
    val amountCents: Long,
    val date: String,
    val categoryId: String,
    val installmentNumber: Int? = null,
    val installmentCount: Int? = null,
    val recurring: Boolean = false,
    /** Category on the source row; categoryId may be a reversible local override. */
    val sourceCategoryId: String = categoryId,
    /** Whether the owner entered this ledger row or confirmed a local notification draft. */
    val origin: TransactionOrigin = TransactionOrigin.MANUAL,
    /** Stable local import reference when [origin] is [TransactionOrigin.NOTIFICATION_IMPORT]. */
    val importNoticeId: String? = null,
)

enum class TransactionOrigin {
  MANUAL,
  NOTIFICATION_IMPORT,
}

/** The deliberately small registry of notification sources supported by this app. */
enum class NotificationImportSource(
    val packageName: String,
    val displayName: String,
) {
  RICO("br.com.rico.mobile", "Rico"),
  ;

  companion object {
    fun fromPackage(packageName: String): NotificationImportSource? = entries.firstOrNull {
      it.packageName == packageName
    }
  }
}

/** Classification is a suggestion only; an owner must resolve every imported notice. */
enum class ImportNoticeKind {
  EXPENSE,
  INCOME,
  NEUTRAL,
  REFUND_REVIEW,
}

enum class ImportNoticeStatus {
  PENDING,
  CONFIRMED,
  IGNORED,
  NEUTRAL,
}

enum class ImportNoticeResolution {
  IGNORED,
  NEUTRAL,
}

/**
 * A local, redacted draft derived from a supported notification. It intentionally contains no raw
 * notification body, card number, account number, or source event key.
 */
data class ImportNotice(
    val id: String,
    val source: NotificationImportSource,
    val sourceEventHash: String,
    val fingerprintHash: String,
    val merchant: String,
    val description: String,
    val amountCents: Long,
    val date: String,
    val kind: ImportNoticeKind,
    val status: ImportNoticeStatus,
    val possibleDuplicate: Boolean,
    val observedAt: String,
    val confirmedTransactionId: String? = null,
)

/** Owner-corrected values used only when a pending draft is explicitly confirmed. */
data class ImportNoticeConfirmation(
    val merchant: String,
    val description: String,
    val amountCents: Long,
    val date: String,
    val categoryId: String,
    /** Explicit acknowledgement when a matching local ledger row still exists at confirmation. */
    val acceptPossibleDuplicate: Boolean = false,
)

/** Safe fields yielded by the pure notification parser for local persistence. */
data class NotificationImportCandidate(
    val source: NotificationImportSource,
    val sourceEventHash: String,
    val fingerprintHash: String,
    val merchant: String,
    val description: String,
    val amountCents: Long,
    val date: String,
    val kind: ImportNoticeKind,
    val observedAt: String,
    /** Transient local filter value; it is never written to the queue. */
    val feedSuffix: String? = null,
)

object TransactionCategoryRules {
  const val INCOME_CATEGORY_ID = "receita"

  fun isCompatible(categoryId: String, amountCents: Long): Boolean =
      (amountCents > 0) == (categoryId == INCOME_CATEGORY_ID)

  fun compatible(categories: List<Category>, amountCents: Long): List<Category> =
      categories.filter {
        isCompatible(it.id, amountCents)
      }
}

enum class ReviewKind {
  UNCATEGORIZED,
  POSSIBLE_DUPLICATE,
  UNUSUAL_AMOUNT,
}

enum class ReviewStrength {
  LIMITED,
  MODERATE,
}

data class ReconciliationCase(
    val id: String,
    val kind: ReviewKind,
    val title: String,
    val explanation: String,
    val transactionIds: List<String>,
    val strength: ReviewStrength,
    val decided: Boolean = false,
    val selectedCategoryId: String? = null,
)

data class Category(
    val id: String,
    val name: String,
    val colorHex: String,
)

data class Budget(
    val categoryId: String,
    val limitCents: Long,
)

data class Insight(
    val id: String,
    val title: String,
    val explanation: String,
    val severity: InsightSeverity,
    val transactionIds: List<String> = emptyList(),
)

enum class InsightSeverity {
  INFO,
  WARNING,
  CRITICAL,
}

data class Alert(
    val id: String,
    val title: String,
    val explanation: String,
    val severity: InsightSeverity,
    val transactionIds: List<String>,
    val generatedDate: String,
    val dismissed: Boolean = false,
)

data class Goal(
    val id: String,
    val name: String,
    val targetCents: Long,
    val savedCents: Long,
)

data class MonthSummary(
    val month: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val balanceCents: Long,
)

data class DisplayPreferences(
    val darkTheme: Boolean = false,
    val reduceMotion: Boolean = false,
    val showCents: Boolean = true,
)
