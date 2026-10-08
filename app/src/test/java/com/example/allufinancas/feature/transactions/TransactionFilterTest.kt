package com.example.allufinancas.feature.transactions

import com.example.allufinancas.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFilterTest {
  private val rows =
      listOf(
          transaction("before", "2026-09-09", "food", "Mercado"),
          transaction("start", "2026-09-10", "food", "Mercado"),
          transaction("middle", "2026-09-15", "health", "Farmácia"),
          transaction("end", "2026-09-20", "food", "Mercado"),
          transaction("after", "2026-09-21", "food", "Mercado"),
      )

  @Test
  fun dateRangeIncludesBothBoundariesAndCombinesWithSearchAndCategory() {
    val filter =
        TransactionFilter(
            query = "mercado",
            categoryId = "food",
            fromDate = "10/09/2026",
            throughDate = "20/09/2026",
        )
    assertEquals(listOf("start", "end"), filter.apply(rows).map { it.id })
  }

  @Test
  fun invalidIncompleteAndReversedRangesHaveNoResults() {
    assertEquals(
        emptyList<Transaction>(),
        TransactionFilter(fromDate = "31/02/2026", throughDate = "05/03/2026").apply(rows),
    )
    assertEquals(emptyList<Transaction>(), TransactionFilter(fromDate = "10/09/2026").apply(rows))
    assertEquals(
        emptyList<Transaction>(),
        TransactionFilter(fromDate = "20/09/2026", throughDate = "10/09/2026").apply(rows),
    )
    assertEquals(
        emptyList<Transaction>(),
        TransactionFilter(month = "2026-09", fromDate = "10/09/2026", throughDate = "20/09/2026")
            .apply(rows),
    )
  }

  private fun transaction(id: String, date: String, category: String, merchant: String) =
      Transaction(id, "account", merchant, "purchase", -100, date, category)
}
