package com.example.allufinancas

import com.example.allufinancas.data.DemoAnalysis
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlluSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun disclosureAndMainDestinations() {
        if (rule.onAllNodesWithText("Explorar demonstração").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explorar demonstração").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("DADOS FICTÍCIOS", substring = true).assertExists()
        rule.onNodeWithText("Extrato").performClick()
        rule.onNodeWithText("Buscar estabelecimento").assertExists()
        rule.onNodeWithText("Planos").performClick()
        rule.onNodeWithText("Planeje sem perder a origem dos números.").assertExists()
        rule.onNodeWithText("Avisos").performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(
            hasText("Esses avisos não confirmam fraude nem substituem orientação profissional.", substring = true),
        )
        rule.onNodeWithText("Esses avisos não confirmam fraude nem substituem orientação profissional.", substring = true).assertExists()
        rule.onNodeWithText("Assistente").performClick()
        rule.onNodeWithText("Pergunte sobre os exemplos").assertExists()
    }

    @Test fun homeMonthProjectionStaysConsistentAndRestoresSelection() {
        if (rule.onAllNodesWithText("Explorar demonstração").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explorar demonstração").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("09/2026").fetchSemanticsNodes().isNotEmpty() }
        val homeList = rule.onNode(hasScrollToIndexAction())

        rule.onNodeWithText("09/2026").performClick()
        homeList.performScrollToNode(hasContentDescription("Alimentação: ${DemoAnalysis.money(65_542)}", substring = true))
        rule.onNodeWithContentDescription("Alimentação: ${DemoAnalysis.money(65_542)}", substring = true).assertExists()
        homeList.performScrollToNode(hasText("Possível lançamento repetido", substring = true))
        rule.onNodeWithText("Possível lançamento repetido", substring = true).assertExists()
        homeList.performScrollToNode(hasText("2026-09-21", substring = true))
        rule.onNodeWithText("2026-09-21", substring = true).assertExists()

        homeList.performScrollToNode(hasText("08/2026"))
        rule.onNodeWithText("08/2026").performClick().assertIsSelected()
        homeList.performScrollToNode(hasContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true))
        rule.onNodeWithContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true).assertExists()
        homeList.performScrollToNode(hasText("Sem destaques neste período"))
        rule.onNodeWithText("Sem destaques neste período").assertExists()
        homeList.performScrollToNode(hasText("2026-08-26", substring = true))
        rule.onNodeWithText("2026-08-26", substring = true).assertExists()

        rule.activityRule.scenario.recreate()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        val restoredHomeList = rule.onNode(hasScrollToIndexAction())
        restoredHomeList.performScrollToNode(hasText("Despesas por categoria"))
        rule.onNodeWithText("Despesas por categoria").assertExists()
        restoredHomeList.performScrollToNode(hasText("08/2026"))
        rule.onNodeWithText("08/2026").assertIsSelected()
        restoredHomeList.performScrollToNode(hasContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true))
        rule.onNodeWithContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true).assertExists()
        restoredHomeList.performScrollToNode(hasText("Sem destaques neste período"))
        rule.onNodeWithText("Sem destaques neste período").assertExists()
        restoredHomeList.performScrollToNode(hasText("2026-08-26", substring = true))
        rule.onNodeWithText("2026-08-26", substring = true).assertExists()
    }
}
