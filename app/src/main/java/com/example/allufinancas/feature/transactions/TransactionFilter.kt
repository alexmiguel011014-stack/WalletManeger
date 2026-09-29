package com.example.allufinancas.feature.transactions

import com.example.allufinancas.data.Transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

internal data class TransactionFilter(
    val query: String = "",
    val categoryId: String = "",
    val month: String = "",
    val fromDate: String = "",
    val throughDate: String = "",
) {
    fun apply(transactions: List<Transaction>): List<Transaction> {
        val rangeEntered = fromDate.isNotBlank() || throughDate.isNotBlank()
        if (rangeEntered && month.isNotBlank()) return emptyList()
        val range = if (rangeEntered) {
            val from = parseDate(fromDate) ?: return emptyList()
            val through = parseDate(throughDate) ?: return emptyList()
            if (from > through) return emptyList()
            from to through
        } else null

        return transactions.filter { transaction ->
            (query.isBlank() || transaction.merchant.contains(query, ignoreCase = true) ||
                transaction.description.contains(query, ignoreCase = true)) &&
                (categoryId.isBlank() || transaction.categoryId == categoryId) &&
                (month.isBlank() || transaction.date.startsWith(month)) &&
                (range == null || runCatching { LocalDate.parse(transaction.date) }
                    .getOrNull()?.let { it >= range.first && it <= range.second } == true)
        }
    }

    companion object {
        private val inputDateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)

        internal fun parseDate(value: String): LocalDate? = try {
            LocalDate.parse(value, inputDateFormat)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
