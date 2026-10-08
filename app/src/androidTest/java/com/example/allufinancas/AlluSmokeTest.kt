package com.example.allufinancas

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.DemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlluSmokeTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  @Test
  fun disclosureAndMainDestinations() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val repository = DemoRepository(context)
    runBlocking {
      withTimeout(15_000) { repository.ready.first { it } }
      repository.resetDemoData()
      repository.setDarkTheme(false)
      repository.setReduceMotion(false)
      repository.setShowCents(true)
    }
    if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Começar a cadastrar").performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("DADOS LOCAIS", substring = true).assertExists()
    assertTrue(rule.onAllNodesWithText(brl(0)).fetchSemanticsNodes().isNotEmpty())
    rule.onNodeWithText("Extrato").performClick()
    rule.onNodeWithText("Buscar estabelecimento").assertExists()
    rule.onNodeWithText("Comece seu extrato").assertExists()
    rule.onNodeWithText("Planos").performClick()
    rule.onNodeWithText("Adicionar limite").assertExists()
    rule.onNodeWithText("Avisos").performClick()
    rule
        .onNode(hasScrollToIndexAction())
        .performScrollToNode(
            hasText(
                "Esses avisos não confirmam fraude nem substituem orientação profissional.",
                substring = true,
            ),
        )
    rule
        .onNodeWithText(
            "Esses avisos não confirmam fraude nem substituem orientação profissional.",
            substring = true,
        )
        .assertExists()
    rule.onNodeWithText("Assistente").performClick()
    rule.onNodeWithText("Pergunte sobre seus lançamentos").assertExists()
  }

  @Test
  fun homeMonthProjectionStaysConsistentAndRestoresSelection() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val repository = DemoRepository(context)
    runBlocking {
      withTimeout(15_000) { repository.ready.first { it } }
      seedLocalLedger(repository, context)
    }
    if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Começar a cadastrar").performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText(LocalLedgerFixtures.MONTH_LABEL).fetchSemanticsNodes().isNotEmpty()
    }
    val homeList = rule.onNode(hasScrollToIndexAction())

    rule.onNodeWithText(LocalLedgerFixtures.MONTH_LABEL).performClick()
    homeList.performScrollToNode(
        hasContentDescription("Alimentação: ${DemoAnalysis.money(65_542)}", substring = true)
    )
    rule
        .onNodeWithContentDescription(
            "Alimentação: ${DemoAnalysis.money(65_542)}",
            substring = true,
        )
        .assertExists()
    homeList.performScrollToNode(hasText("Possível lançamento repetido", substring = true))
    rule.onNodeWithText("Possível lançamento repetido", substring = true).assertExists()
    homeList.performScrollToNode(hasText(LocalLedgerFixtures.date(21), substring = true))
    rule.onNodeWithText(LocalLedgerFixtures.date(21), substring = true).assertExists()

    homeList.performScrollToNode(hasText(LocalLedgerFixtures.PREVIOUS_MONTH_LABEL))
    rule.onNodeWithText(LocalLedgerFixtures.PREVIOUS_MONTH_LABEL).performClick().assertIsSelected()
    homeList.performScrollToNode(
        hasContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true)
    )
    rule
        .onNodeWithContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true)
        .assertExists()
    homeList.performScrollToNode(hasText("Sem destaques neste período"))
    rule.onNodeWithText("Sem destaques neste período").assertExists()
    homeList.performScrollToNode(
        hasText(LocalLedgerFixtures.previousMonthDate(26), substring = true)
    )
    rule.onNodeWithText(LocalLedgerFixtures.previousMonthDate(26), substring = true).assertExists()

    rule.activityRule.scenario.recreate()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty()
    }
    val restoredHomeList = rule.onNode(hasScrollToIndexAction())
    restoredHomeList.performScrollToNode(hasText("Despesas por categoria"))
    rule.onNodeWithText("Despesas por categoria").assertExists()
    restoredHomeList.performScrollToNode(hasText(LocalLedgerFixtures.PREVIOUS_MONTH_LABEL))
    rule.onNodeWithText(LocalLedgerFixtures.PREVIOUS_MONTH_LABEL).assertIsSelected()
    restoredHomeList.performScrollToNode(
        hasContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true)
    )
    rule
        .onNodeWithContentDescription("Compras: ${DemoAnalysis.money(7_250)}", substring = true)
        .assertExists()
    restoredHomeList.performScrollToNode(hasText("Sem destaques neste período"))
    rule.onNodeWithText("Sem destaques neste período").assertExists()
    restoredHomeList.performScrollToNode(
        hasText(LocalLedgerFixtures.previousMonthDate(26), substring = true)
    )
    rule.onNodeWithText(LocalLedgerFixtures.previousMonthDate(26), substring = true).assertExists()
  }
}
