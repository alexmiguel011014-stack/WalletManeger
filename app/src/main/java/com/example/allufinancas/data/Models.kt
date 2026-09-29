package com.example.allufinancas.data

/** Every financial value in this app belongs to the bundled fictional demonstration. */
interface DemoData {
    val isDemo: Boolean get() = true
    val dataSource: String get() = "Exemplo fictício armazenado neste dispositivo"
}

data class AccountSummary(
    val id: String,
    val name: String,
    val kind: String,
    val balanceCents: Long,
) : DemoData

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
) : DemoData

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
) : DemoData

data class Category(
    val id: String,
    val name: String,
    val colorHex: String,
) : DemoData

data class Budget(
    val categoryId: String,
    val limitCents: Long,
) : DemoData

data class Insight(
    val id: String,
    val title: String,
    val explanation: String,
    val severity: InsightSeverity,
    val transactionIds: List<String> = emptyList(),
) : DemoData

enum class InsightSeverity { INFO, WARNING, CRITICAL }

data class Alert(
    val id: String,
    val title: String,
    val explanation: String,
    val severity: InsightSeverity,
    val transactionIds: List<String>,
    val generatedDate: String,
    val dismissed: Boolean = false,
) : DemoData

data class Goal(
    val id: String,
    val name: String,
    val targetCents: Long,
    val savedCents: Long,
) : DemoData

data class AssistantMessage(
    val id: Long = 0,
    val role: MessageRole,
    val text: String,
    val createdAt: String,
    val citationTransactionIds: List<String> = emptyList(),
) : DemoData

enum class MessageRole { USER, ASSISTANT }

data class AssistantAnswer(
    val text: String,
    val citationTransactionIds: List<String> = emptyList(),
) : DemoData

data class MonthSummary(
    val month: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val balanceCents: Long,
) : DemoData

data class DisplayPreferences(
    val darkTheme: Boolean = false,
    val reduceMotion: Boolean = false,
    val showCents: Boolean = true,
) : DemoData
