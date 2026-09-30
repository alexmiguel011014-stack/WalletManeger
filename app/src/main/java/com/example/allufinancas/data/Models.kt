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
)

object TransactionCategoryRules {
    const val INCOME_CATEGORY_ID = "receita"

    fun isCompatible(categoryId: String, amountCents: Long): Boolean =
        (amountCents > 0) == (categoryId == INCOME_CATEGORY_ID)

    fun compatible(categories: List<Category>, amountCents: Long): List<Category> =
        categories.filter { isCompatible(it.id, amountCents) }
}

enum class ReviewKind { UNCATEGORIZED, POSSIBLE_DUPLICATE, UNUSUAL_AMOUNT }
enum class ReviewStrength { LIMITED, MODERATE }

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

enum class InsightSeverity { INFO, WARNING, CRITICAL }

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

data class AssistantMessage(
    val id: Long = 0,
    val role: MessageRole,
    val text: String,
    val createdAt: String,
    val citationTransactionIds: List<String> = emptyList(),
)

enum class MessageRole { USER, ASSISTANT }

data class AssistantAnswer(
    val text: String,
    val citationTransactionIds: List<String> = emptyList(),
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
