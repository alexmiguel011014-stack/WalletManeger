package com.example.allufinancas

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: DemoRepository

    @Before fun resetDemoState() = runBlocking {
        repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        withTimeout(15_000) { repository.ready.first { it } }
        seedLocalLedger(repository, InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After fun restoreDemoStateAfterTest() = runBlocking { restoreDemoState() }

    @Test fun detailShowsInstallmentAndCategoryCorrectionPersistsInRoom() {
        if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Começar a cadastrar").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Extrato").performClick()
        rule.onNodeWithText("2026-09").performScrollTo().performClick()
        rule.onNodeWithText("Buscar estabelecimento").performScrollTo().performTextInput("Notebook")
        rule.onNodeWithText("Loja Exemplo").performScrollTo().performClick()

        rule.onNodeWithText("Parcela 3 de 10").assertExists()
        rule.onNodeWithText("DADOS LOCAIS", substring = true).assertExists()
        rule.onNodeWithText("Fonte: lançamentos cadastrados neste dispositivo", substring = true).assertExists()
        rule.onNodeWithText("Escopo: Detalhe · Loja Exemplo", substring = true).assertExists()
        rule.onNodeWithText("Categoria: Compras").assertExists()

        val casaNodes = rule.onAllNodesWithText("Casa").fetchSemanticsNodes()
        assertTrue("O seletor de categoria deve estar disponível no detalhe", casaNodes.isNotEmpty())
        rule.onAllNodesWithText("Casa")[casaNodes.lastIndex].performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Casa · 2026-09-12", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Loja Exemplo").performClick()
        rule.onNodeWithText("Categoria: Casa").assertExists()
        rule.onNodeWithText("Data: 2026-09-12").assertExists()
        rule.onNodeWithText("Valor: ${com.example.allufinancas.core.brl(-12_990, true)}").assertExists()

        rule.activityRule.scenario.recreate()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Extrato").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Estabelecimento: Loja Exemplo").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Estabelecimento: Loja Exemplo").assertExists()
        rule.onNodeWithText("Categoria: Casa").assertExists()
        rule.onNodeWithText("Parcela 3 de 10").assertExists()
    }

    @Test fun selectingAllPeriodClearsDateRange() {
        if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Começar a cadastrar").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Extrato").performClick()
        rule.onNodeWithText("De · dd/MM/aaaa").performScrollTo().performTextInput("10/09/2026")
        rule.onNodeWithText("Até · dd/MM/aaaa").performScrollTo().performTextInput("20/09/2026")
        assertTrue(rule.onAllNodesWithText("10/09/2026", substring = true).fetchSemanticsNodes().size >= 2)
        assertTrue(rule.onAllNodesWithText("20/09/2026", substring = true).fetchSemanticsNodes().size >= 2)

        rule.onNodeWithText("Todo período").performScrollTo().performClick()
        rule.onNodeWithText("Todos os movimentos", substring = true).performScrollTo().assertExists()
        assertTrue(rule.onAllNodesWithText("10/09/2026").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("2026-08-26", substring = true).performScrollTo().assertExists()
    }

    private suspend fun restoreDemoState() {
        repository.resetDemoData()
        repository.setDarkTheme(false)
        repository.setReduceMotion(false)
        repository.setShowCents(true)
    }
}
