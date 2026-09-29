package com.example.allufinancas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAssistantScopeTest {
    @Test fun realtimeAndOtherMonthQuestionsReturnNoSeptemberCalculationsOrSources() {
        listOf(
            "Quanto gastei em agosto?",
            "Quanto gastei em 2025?",
            "Quanto gastei em 2026?",
            "Qual meu saldo hoje?",
            "Mostre os gastos em tempo real",
        ).forEach { prompt ->
            val answer = LocalAssistant.answer(prompt, DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
            assertTrue(prompt, answer.citationTransactionIds.isEmpty())
            assertFalse(prompt, DemoAnalysis.money(280_462) in answer.text)
        }
        assertTrue(LocalAssistant.answer("Quanto gastei em agosto?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
            .text.contains("Não consigo responder sobre outro período"))
        assertTrue(LocalAssistant.answer("Quanto gastei em setembro de 2026?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
            .text.contains(DemoAnalysis.money(280_462)))
        assertTrue(LocalAssistant.answer("Quanto gastei em set/2026?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
            .text.contains(DemoAnalysis.money(280_462)))
        assertTrue(LocalAssistant.answer("Qual meu saldo hoje?", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
            .text.contains("tempo real"))
    }

    @Test fun septemberLargestExpenseCitesOnlyItsKnownSeedId() {
        val answer = LocalAssistant.answer("Maior despesa", DemoSeed.transactions, DemoSeed.categories, DemoSeed.budgets)
        assertEquals(listOf("t11"), answer.citationTransactionIds)
        assertTrue(answer.text.contains("Moradia Exemplo"))
    }
}
