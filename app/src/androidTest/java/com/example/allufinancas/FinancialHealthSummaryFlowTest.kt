package com.example.allufinancas

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.DemoRepository
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialHealthSummaryFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: DemoRepository

    @Before fun seedLedger() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = DemoRepository(context)
        withTimeout(15_000) { repository.ready.first { it } }
        seedLocalLedger(repository, context)
    }

    @After fun clearLedger() = runBlocking { repository.resetDemoData() }

    @Test fun observedSignalsExposeEvidenceAndOpenExistingDestinations() {
        if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Começar a cadastrar").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("09/2026").fetchSemanticsNodes().isNotEmpty() }
        val homeList = rule.onNode(hasScrollToIndexAction())
        homeList.performScrollToNode(hasText("08/2026"))
        rule.onNodeWithText("08/2026").performClick()
        homeList.performScrollToNode(hasText("Nenhum registro corresponde às regras"))
        rule.onNodeWithText("Nenhum registro corresponde às regras").assertExists()

        homeList.performScrollToNode(hasText("09/2026"))
        rule.onNodeWithText("09/2026").performClick()
        homeList.performScrollToNode(hasText("Lançamentos marcados como recorrentes"))
        rule.onAllNodesWithText("Período: 2026-09 · fonte: lançamentos locais cadastrados").assertCountEquals(3)
        rule.onNodeWithText("t06 · 2026-09-17 · ${DemoAnalysis.money(-3_990)}", substring = true).assertExists()
        homeList.performScrollToNode(hasText("Abrir lançamento t06"))
        rule.onNodeWithText("Abrir lançamento t06").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Estabelecimento: Vídeo Exemplo").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Data: 2026-09-17").assertExists()
        rule.onNodeWithText("Voltar").performClick()

        rule.onNodeWithContentDescription("Início").performClick()
        val returnedHome = rule.onNode(hasScrollToIndexAction())
        returnedHome.performScrollToNode(hasText("Ver limite em Planos"))
        rule.onNodeWithText("Ver limite em Planos").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Defina limites para seus gastos registrados.").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Defina limites para seus gastos registrados.").assertExists()
    }
}
