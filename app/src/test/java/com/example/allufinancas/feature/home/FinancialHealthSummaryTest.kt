package com.example.allufinancas.feature.home

import com.example.allufinancas.data.Budget
import com.example.allufinancas.data.DemoSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialHealthSummaryTest {
    @Test fun projectionIsLimitedToTheSelectedMonth() {
        val transactions = DemoSeed.transactions.map {
            if (it.id == "t12") it.copy(recurring = true) else it
        }
        val september = financialHealthSummary(
            transactions, DemoSeed.categories, DemoSeed.budgets, "2026-09",
        )
        val august = financialHealthSummary(
            transactions, DemoSeed.categories, DemoSeed.budgets, "2026-08",
        )

        assertEquals(listOf("t06", "t09", "t11"), september.recurringTransactions.map { it.id })
        assertEquals(listOf("t12"), august.recurringTransactions.map { it.id })
        assertTrue(september.installments.isNotEmpty())
        assertFalse("t12" in september.installments.map { it.transaction.id })
    }

    @Test fun emptyMonthHasNoEvidenceOrLeakedRecords() {
        val summary = financialHealthSummary(
            DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets, "2026-06",
        )

        assertFalse(summary.hasEvidence)
        assertEquals(0, summary.periodTransactionCount)
        assertTrue(summary.recurringTransactions.isEmpty())
        assertTrue(summary.exceededLimits.isEmpty())
        assertTrue(summary.installments.isEmpty())
    }

    @Test fun categoryOverrideMovesSpendingWithoutChangingSourceEvidence() {
        val source = DemoSeed.transactions.single { it.id == "t07" }
        val effective = source.copy(categoryId = "alimentacao")
        val overridden = DemoSeed.transactions.map { if (it.id == source.id) effective else it }
        val summary = financialHealthSummary(overridden, DemoSeed.categories, DemoSeed.budgets, "2026-09")

        val food = summary.exceededLimits.single { it.categoryId == "alimentacao" }
        assertEquals(68_432L, food.spentCents)
        assertTrue("t07" in food.sourceTransactions.map { it.id })
        assertEquals(source.sourceCategoryId, effective.sourceCategoryId)
        assertEquals(source.amountCents, effective.amountCents)
        assertEquals(source.date, effective.date)
        assertEquals(source.merchant, food.sourceTransactions.first { it.id == "t07" }.merchant)
    }

    @Test fun limitSignalsRequireActualSpendAboveLimitAndUseEffectiveCategory() {
        val baseline = financialHealthSummary(
            DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets, "2026-09",
        )
        assertEquals(listOf("alimentacao"), baseline.exceededLimits.map { it.categoryId })
        assertEquals(65_542L, baseline.exceededLimits.single().spentCents)
        assertEquals(50_000L, baseline.exceededLimits.single().limitCents)
        assertEquals(listOf("t02", "t03", "t10"), baseline.exceededLimits.single().sourceTransactions.map { it.id })

        val raised = DemoSeed.budgets.map { if (it.categoryId == "alimentacao") Budget(it.categoryId, 70_000) else it }
        val underLimit = financialHealthSummary(DemoSeed.transactions, DemoSeed.categories, raised, "2026-09")
        assertTrue(underLimit.exceededLimits.isEmpty())
    }

    @Test fun installmentRemainderUsesOnlyStoredCounters() {
        val installment = financialHealthSummary(
            DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets, "2026-09",
        ).installments.single()

        assertEquals("t08", installment.transaction.id)
        assertEquals("2026-09-12", installment.transaction.date)
        assertEquals(-12_990L, installment.transaction.amountCents)
        assertEquals(3, installment.number)
        assertEquals(10, installment.count)
        assertEquals(7, installment.remaining)

        val completed = DemoSeed.transactions.map {
            if (it.id == "t08") it.copy(installmentNumber = 10, installmentCount = 10) else it
        }
        assertTrue(financialHealthSummary(completed, DemoSeed.categories, DemoSeed.budgets, "2026-09")
            .installments.isEmpty())
    }
}
