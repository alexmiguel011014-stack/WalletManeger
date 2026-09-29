package com.example.allufinancas.feature.alerts

import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.DemoSeed
import com.example.allufinancas.data.Budget
import com.example.allufinancas.data.Category
import com.example.allufinancas.data.InsightSeverity
import com.example.allufinancas.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertRulesTest {
    @Test fun septemberSeedGeneratesTheFourExpectedRuleAlertsAndEvidence() {
        val alerts = DemoAnalysis.insights(
            DemoSeed.transactions,
            DemoSeed.categories,
            DemoSeed.budgets,
            DemoSeed.MONTH,
        ).associateBy { it.id }

        assertEquals(
            setOf("budget-alimentacao", "duplicate-t04-t05", "recurring-2026-09", "unusual-t02"),
            alerts.keys,
        )
        assertEquals(InsightSeverity.WARNING, alerts.getValue("budget-alimentacao").severity)
        assertEquals(setOf("t10", "t03", "t02"), alerts.getValue("budget-alimentacao").transactionIds.toSet())
        assertEquals(InsightSeverity.WARNING, alerts.getValue("duplicate-t04-t05").severity)
        assertEquals(setOf("t04", "t05"), alerts.getValue("duplicate-t04-t05").transactionIds.toSet())
        assertEquals(InsightSeverity.INFO, alerts.getValue("recurring-2026-09").severity)
        assertEquals(setOf("t06", "t09", "t11"), alerts.getValue("recurring-2026-09").transactionIds.toSet())
        assertEquals(InsightSeverity.INFO, alerts.getValue("unusual-t02").severity)
        assertEquals(listOf("t02"), alerts.getValue("unusual-t02").transactionIds)
    }

    @Test fun budgetAlertRequiresSpendStrictlyAboveTheLimitAndSelectedMonth() {
        val transaction = Transaction("tx", "account", "Loja", "Exemplo", -5_000, "2026-09-01", "food")
        val categories = listOf(Category("food", "Alimentação", "#000000"))
        val atLimit = DemoAnalysis.insights(listOf(transaction), categories, listOf(Budget("food", 5_000)), "2026-09")
        val aboveLimit = DemoAnalysis.insights(listOf(transaction), categories, listOf(Budget("food", 4_999)), "2026-09")
        val otherMonth = DemoAnalysis.insights(listOf(transaction), categories, listOf(Budget("food", 4_999)), "2026-08")

        assertFalse(atLimit.any { it.id == "budget-food" })
        assertEquals(listOf("tx"), aboveLimit.single { it.id == "budget-food" }.transactionIds)
        assertTrue(otherMonth.isEmpty())
    }

    @Test fun unusualSpendRequiresMoreThanThreeTimesTheSampleMedian() {
        fun sampleWithOutlier(cents: Long) = List(5) { index ->
            Transaction("base-$index", "account", "Loja $index", "Exemplo", -100, "2026-09-0${index + 1}", "food")
        } + Transaction("candidate", "account", "Loja final", "Exemplo", -cents, "2026-09-06", "food")

        val atThreshold = DemoAnalysis.insights(sampleWithOutlier(300), emptyList(), emptyList(), "2026-09")
        val aboveThreshold = DemoAnalysis.insights(sampleWithOutlier(301), emptyList(), emptyList(), "2026-09")

        assertFalse(atThreshold.any { it.id == "unusual-candidate" })
        assertEquals(listOf("candidate"), aboveThreshold.single { it.id == "unusual-candidate" }.transactionIds)
    }
}
