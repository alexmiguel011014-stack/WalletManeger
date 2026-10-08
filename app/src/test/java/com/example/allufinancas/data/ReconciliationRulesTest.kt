package com.example.allufinancas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliationRulesTest {
  @Test
  fun seedInboxUsesLocalExplainableRulesAndKeepsSourceEvidence() {
    val cases =
        ReconciliationRules.cases(DemoSeed.transactions, month = DemoSeed.MONTH).associateBy {
          it.id
        }

    assertEquals(setOf("uncategorized-t07", "duplicate-t04-t05", "unusual-t02"), cases.keys)
    assertEquals(ReviewKind.UNCATEGORIZED, cases.getValue("uncategorized-t07").kind)
    assertEquals(ReviewStrength.MODERATE, cases.getValue("duplicate-t04-t05").strength)
    assertEquals(ReviewStrength.LIMITED, cases.getValue("unusual-t02").strength)
    assertTrue(
        cases.getValue("duplicate-t04-t05").explanation.contains("Não confirma cobrança duplicada")
    )
    assertTrue(cases.getValue("unusual-t02").explanation.contains("não indica fraude"))

    val duplicate = DemoSeed.transactions.filter { it.id in setOf("t04", "t05") }
    assertEquals(2, duplicate.size)
    assertEquals(duplicate[0].amountCents, duplicate[1].amountCents)
    assertEquals(duplicate[0].date, duplicate[1].date)
  }

  @Test
  fun categoryOverrideChangesAnalysisButNeverSourceAmountOrDate() {
    val source = DemoSeed.transactions.single { it.id == "t07" }
    val decision =
        ReconciliationDecisionRow("uncategorized-t07", "UNCATEGORIZED", listOf("t07"), "transporte")
    val effective =
        ReconciliationRules.effectiveTransactions(DemoSeed.transactions, listOf(decision)).single {
          it.id == "t07"
        }

    assertEquals("", source.sourceCategoryId)
    assertEquals("transporte", effective.categoryId)
    assertEquals("", effective.sourceCategoryId)
    assertEquals(source.amountCents, effective.amountCents)
    assertEquals(source.date, effective.date)
    assertEquals(
        0,
        DemoAnalysis.categorySpentCents(DemoSeed.transactions, "transporte", DemoSeed.MONTH),
    )
    assertEquals(
        2_890,
        DemoAnalysis.categorySpentCents(effectiveList(effective), "transporte", DemoSeed.MONTH),
    )

    val movedToFood =
        ReconciliationRules.effectiveTransactions(
            DemoSeed.transactions,
            listOf(decision.copy(categoryId = "alimentacao")),
        )
    val sourceAlert =
        DemoAnalysis.insights(
                DemoSeed.transactions,
                DemoSeed.categories,
                DemoSeed.budgets,
                DemoSeed.MONTH,
            )
            .single { it.id == "budget-alimentacao" }
    val correctedAlert =
        DemoAnalysis.insights(movedToFood, DemoSeed.categories, DemoSeed.budgets, DemoSeed.MONTH)
            .single { it.id == "budget-alimentacao" }
    assertFalse("t07" in sourceAlert.transactionIds)
    assertTrue("t07" in correctedAlert.transactionIds)
  }

  @Test
  fun decisionsAppearInDecidedListAndDeletingThemReopensTheBaseCase() {
    val category =
        ReconciliationDecisionRow("uncategorized-t07", "UNCATEGORIZED", listOf("t07"), "transporte")
    val duplicate =
        ReconciliationDecisionRow(
            "duplicate-t04-t05",
            "POSSIBLE_DUPLICATE",
            listOf("t04", "t05"),
            null,
        )
    val decided =
        ReconciliationRules.cases(
                DemoSeed.transactions,
                listOf(category, duplicate),
                DemoSeed.MONTH,
            )
            .associateBy { it.id }

    assertTrue(decided.getValue("uncategorized-t07").decided)
    assertEquals("transporte", decided.getValue("uncategorized-t07").selectedCategoryId)
    assertTrue(decided.getValue("duplicate-t04-t05").decided)
    assertFalse(
        ReconciliationRules.cases(DemoSeed.transactions, month = DemoSeed.MONTH)
            .single { it.id == "duplicate-t04-t05" }
            .decided
    )
  }

  @Test
  fun decisionWithOnlyUnknownTransactionIdLeavesEverySourceRowUnchanged() {
    val decision =
        ReconciliationDecisionRow(
            "uncategorized-missing",
            "UNCATEGORIZED",
            listOf("missing-tx"),
            "transporte",
        )

    assertEquals(
        DemoSeed.transactions,
        ReconciliationRules.effectiveTransactions(DemoSeed.transactions, listOf(decision)),
    )
  }

  @Test
  fun emptySourceDoesNotGenerateCasesFromStoredDecisions() {
    val decision =
        ReconciliationDecisionRow(
            "uncategorized-missing",
            "UNCATEGORIZED",
            listOf("missing-tx"),
            "transporte",
        )

    assertTrue(ReconciliationRules.cases(emptyList(), listOf(decision)).isEmpty())
    assertTrue(ReconciliationRules.effectiveTransactions(emptyList(), listOf(decision)).isEmpty())
  }

  private fun effectiveList(transaction: Transaction) =
      DemoSeed.transactions.map {
        if (it.id == transaction.id) transaction else it
      }
}
