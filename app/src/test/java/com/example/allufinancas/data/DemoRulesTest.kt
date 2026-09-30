package com.example.allufinancas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoRulesTest {
    @Test fun categoryMustMatchTransactionAmountSign() {
        assertTrue(TransactionCategoryRules.isCompatible("receita", 1))
        assertFalse(TransactionCategoryRules.isCompatible("alimentacao", 1))
        assertTrue(TransactionCategoryRules.isCompatible("alimentacao", -1))
        assertFalse(TransactionCategoryRules.isCompatible("receita", -1))
        assertEquals(listOf("receita"), TransactionCategoryRules.compatible(DefaultCategories.all, 1).map { it.id })
        assertFalse(TransactionCategoryRules.compatible(DefaultCategories.all, -1).any { it.id == "receita" })
    }

    @Test fun seededMonthHasReproducibleTotalsAndEmptyMonth() {
        val total = DemoAnalysis.monthSummary(DemoSeed.transactions, DemoSeed.MONTH)
        assertEquals(820_000, total.incomeCents)
        assertEquals(280_462, total.expenseCents)
        assertEquals(539_538, total.balanceCents)
        assertEquals(65_542, DemoAnalysis.categorySpentCents(DemoSeed.transactions, "alimentacao", DemoSeed.MONTH))
        assertEquals(0, DemoAnalysis.monthSummary(DemoSeed.transactions, "2026-06").expenseCents)
    }

    @Test fun insightsAreTraceableAndBudgetChangesRecompute() {
        val original = DemoAnalysis.insights(DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets, DemoSeed.MONTH)
        assertTrue(original.any { it.id == "budget-alimentacao" && it.transactionIds.contains("t02") })
        assertTrue(original.any { it.id == "duplicate-t04-t05" && it.transactionIds == listOf("t04", "t05") })
        val raised = DemoSeed.budgets.map { if (it.categoryId == "alimentacao") it.copy(limitCents = 100_000) else it }
        assertFalse(DemoAnalysis.insights(DemoSeed.transactions, DemoSeed.categories, raised, DemoSeed.MONTH)
            .any { it.id == "budget-alimentacao" })
    }

    @Test fun homeProjectionUsesOneMonthAndStableCategoryRecentAndInsightOrder() {
        val transactions = listOf(
            tx("sep-food", "food", -6_000, "2026-09-02", recurring = true),
            tx("sep-income", "income", 100_000, "2026-09-05"),
            tx("zeta", "zeta", -2_000, "2026-08-31"),
            tx("food-old", "food", -5_000, "2026-08-01"),
            tx("alpha", "alpha", -2_000, "2026-08-31"),
            tx("food-new", "food", -1_000, "2026-08-28"),
            tx("aug-income", "income", 15_000, "2026-08-15"),
        )
        val categories = listOf(
            Category("food", "Alimentação", "#000000"),
            Category("zeta", "Zeta", "#000000"),
            Category("alpha", "Alfa", "#000000"),
        )
        val budgets = listOf(Budget("food", 5_000))
        val storedSeptemberDismissal = Alert(
            id = "budget-food",
            title = "Aviso persistido",
            explanation = "Aviso salvo para setembro.",
            severity = InsightSeverity.WARNING,
            transactionIds = listOf("sep-food"),
            generatedDate = "2026-09-23",
            dismissed = true,
        )
        val accounts = listOf(AccountSummary("account", "Conta", "Fictícia", 50_000))
        val goals = listOf(Goal("goal", "Reserva", 100_000, 20_000))

        val august = DemoAnalysis.homeProjection(
            accounts, transactions, categories, budgets, listOf(storedSeptemberDismissal), goals, "2026-08",
        )
        assertEquals(10_000, august.monthSummary.expenseCents)
        assertEquals(15_000, august.monthSummary.incomeCents)
        assertEquals(5_000, august.monthSummary.balanceCents)
        assertEquals(listOf("food", "alpha", "zeta"), august.expensesByCategory.map { it.categoryId })
        assertEquals(listOf(6_000L, 2_000L, 2_000L), august.expensesByCategory.map { it.spentCents })
        assertEquals(listOf("alpha", "zeta", "food-new", "aug-income", "food-old"), august.recentTransactions.map { it.id })
        assertEquals(listOf("budget-food"), august.insights.map { it.insight.id })
        assertFalse(august.insights.single().dismissed)

        val september = DemoAnalysis.homeProjection(
            accounts, transactions, categories, budgets, listOf(storedSeptemberDismissal), goals, "2026-09",
        )
        assertEquals(listOf("budget-food", "recurring-2026-09"), september.insights.map { it.insight.id })
        assertTrue(september.insights.first().dismissed)
        assertFalse(september.insights.last().dismissed)
        assertEquals(accounts, august.accounts)
        assertEquals(goals, august.goals)
    }

    @Test fun homeProjectionEmptyMonthDoesNotLeakTransactionsOrPersistedAlerts() {
        val alerts = listOf(
            Alert("budget-alimentacao", "Aviso", "Setembro", InsightSeverity.WARNING, emptyList(), "2026-09-23", true),
        )
        val projection = DemoAnalysis.homeProjection(
            DemoSeed.accounts,
            DemoSeed.transactions,
            DemoSeed.categories,
            DemoSeed.budgets,
            alerts,
            DemoSeed.goals,
            "",
        )

        assertEquals(MonthSummary("", 0, 0, 0), projection.monthSummary)
        assertTrue(projection.expensesByCategory.isEmpty())
        assertTrue(projection.recentTransactions.isEmpty())
        assertTrue(projection.insights.isEmpty())
        assertEquals(DemoSeed.accounts, projection.accounts)
        assertEquals(DemoSeed.goals, projection.goals)
        assertEquals(0, DemoAnalysis.categorySpentCents(DemoSeed.transactions, "alimentacao", ""))
        assertTrue(DemoAnalysis.insights(DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets, "").isEmpty())
    }

    @Test fun insightPriorityUsesSeverityThenStableIdOrder() {
        val insights = listOf(
            Insight("z-info", "Info", "", InsightSeverity.INFO),
            Insight("z-warning", "Aviso Z", "", InsightSeverity.WARNING),
            Insight("critical", "Crítico", "", InsightSeverity.CRITICAL),
            Insight("a-warning", "Aviso A", "", InsightSeverity.WARNING),
        )

        val expected = listOf("critical", "a-warning", "z-warning", "z-info")
        assertEquals(expected, DemoAnalysis.prioritizeInsights(insights).map { it.id })
        assertEquals(expected, DemoAnalysis.prioritizeInsights(insights.reversed()).map { it.id })
    }

    @Test fun unusualSpendUsesConventionalMedianAndHighlightsLargestOutlier() {
        val transactions = listOf(
            tx("ordinary-a", "other", -10_000, "2026-09-01"),
            tx("ordinary-b", "other", -10_000, "2026-09-02"),
            tx("ordinary-c", "other", -20_000, "2026-09-03"),
            tx("ordinary-d", "other", -100_000, "2026-09-04"),
            tx("outlier-smaller", "other", -200_000, "2026-09-05"),
            tx("outlier-largest", "other", -250_000, "2026-09-06"),
        )

        val unusual = DemoAnalysis.insights(transactions, emptyList(), emptyList(), "2026-09")
            .single { it.id.startsWith("unusual-") }

        assertEquals("unusual-outlier-largest", unusual.id)
        assertEquals(listOf("outlier-largest"), unusual.transactionIds)
        assertTrue(unusual.explanation.contains(DemoAnalysis.money(60_000)))
    }

    @Test fun assistantExplainsSupportedIntentsAndRejectsUnknownOnes() {
        val category = LocalAssistant.answer("Quanto gastei com alimentação?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertTrue(category.text.contains(DemoAnalysis.money(65_542)))
        assertEquals(listOf("t02", "t03", "t10"), category.citationTransactionIds)

        val budget = LocalAssistant.answer("Quanto resta do orçamento de alimentação?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertTrue(budget.text.contains(DemoAnalysis.money(-15_542)))

        val unsupported = LocalAssistant.answer("Qual ação devo comprar?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertTrue(unsupported.text.contains("Só consigo calcular"))
        assertTrue(unsupported.citationTransactionIds.isEmpty())
    }

    @Test fun assistantCoversMonthlyLargestRecurringAndDemoDisclosure() {
        val month = LocalAssistant.answer("Quanto gastei no mês?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertTrue(month.text.contains(DemoAnalysis.money(280_462)))
        assertEquals(10, month.citationTransactionIds.size)

        val largest = LocalAssistant.answer("Qual foi a maior despesa?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertEquals(listOf("t11"), largest.citationTransactionIds)

        val recurring = LocalAssistant.answer("Quais assinaturas recorrentes?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertEquals(listOf("t06", "t09", "t11"), recurring.citationTransactionIds)

        val disclosure = LocalAssistant.answer("Esses dados são reais do banco?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertTrue(disclosure.text.contains("seus lançamentos, categorias e limites salvos neste aparelho"))
        assertTrue(disclosure.citationTransactionIds.isEmpty())
    }

    @Test fun unsupportedAssistantQuestionKeepsItsLimitWhenMonthHasNoData() {
        val answer = LocalAssistant.answer("Qual é a minha cor favorita?", emptyList(), DemoSeed.categories, emptyList())
        assertTrue(answer.text.contains("Só consigo calcular"))
        assertFalse(answer.text.contains("Cadastre uma entrada ou saída"))
        val supported = LocalAssistant.answer("Quanto gastei no mês?", emptyList(), DemoSeed.categories, emptyList())
        assertTrue(supported.text.contains("Cadastre uma entrada ou saída"))
    }

    private fun tx(id: String, categoryId: String, amount: Long, date: String, recurring: Boolean = false) =
        Transaction(id, "account", id, "Fictício", amount, date, categoryId, recurring = recurring)
}
