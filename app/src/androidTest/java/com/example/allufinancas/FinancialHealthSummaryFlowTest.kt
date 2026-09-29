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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialHealthSummaryFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun observedSignalsExposeEvidenceAndOpenExistingDestinations() {
        if (rule.onAllNodesWithText("Explorar demonstração").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explorar demonstração").performClick()
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
        rule.onAllNodesWithText("Período: 2026-09 · fonte: amostra fictícia local").assertCountEquals(3)
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
            rule.onAllNodesWithText("Planeje sem perder a origem dos números.").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Planeje sem perder a origem dos números.").assertExists()
    }
}
