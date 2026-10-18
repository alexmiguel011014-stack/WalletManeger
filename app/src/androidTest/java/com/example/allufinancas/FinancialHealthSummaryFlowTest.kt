package com.example.allufinancas

import androidx.compose.ui.test.assertCountEquals
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
import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.DemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialHealthSummaryFlowTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()
  private lateinit var repository: DemoRepository

  @Before
  fun seedLedger() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    repository = DemoRepository(context)
    withTimeout(15_000) { repository.ready.first { it } }
    seedLocalLedger(repository, context)
  }

  @After fun clearLedger() = runBlocking { repository.resetDemoData() }

  @Test
  fun observedSignalsExposeEvidenceAndOpenExistingDestinations() {
    if (rule.onAllNodesWithText("Adicionar lançamento").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Adicionar lançamento").performClick()
      rule.waitUntil(15_000) {
        rule.onAllNodesWithText("Novo lançamento").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNodeWithText("Cancelar").performClick()
      rule.onNodeWithContentDescription("Início").performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText(LocalLedgerFixtures.MONTH_LABEL).fetchSemanticsNodes().isNotEmpty()
    }
    val homeList = rule.onNode(hasScrollToIndexAction())
    homeList.performScrollToNode(hasText(LocalLedgerFixtures.PREVIOUS_MONTH_LABEL))
    rule.onNodeWithText(LocalLedgerFixtures.PREVIOUS_MONTH_LABEL).performClick()
    homeList.performScrollToNode(hasText("Nenhum registro corresponde às regras"))
    rule.onNodeWithText("Nenhum registro corresponde às regras").assertExists()

    homeList.performScrollToNode(hasText(LocalLedgerFixtures.MONTH_LABEL))
    rule.onNodeWithText(LocalLedgerFixtures.MONTH_LABEL).performClick()
    homeList.performScrollToNode(hasText("Lançamentos marcados como recorrentes"))
    rule
        .onAllNodesWithText(
            "Período: ${LocalLedgerFixtures.MONTH} · fonte: lançamentos locais cadastrados"
        )
        .assertCountEquals(3)
    rule
        .onNodeWithText(
            "t06 · ${LocalLedgerFixtures.date(17)} · ${DemoAnalysis.money(-3_990)}",
            substring = true,
        )
        .assertExists()
    homeList.performScrollToNode(hasText("Abrir lançamento t06"))
    rule.onNodeWithText("Abrir lançamento t06").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Estabelecimento: Vídeo Exemplo").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Data: ${LocalLedgerFixtures.date(17)}").assertExists()
    rule.onNodeWithText("Voltar").performClick()

    rule.onNodeWithContentDescription("Início").performClick()
    val returnedHome = rule.onNode(hasScrollToIndexAction())
    returnedHome.performScrollToNode(hasText("Ver limite em Planos"))
    rule.onNodeWithText("Ver limite em Planos").performClick()
    rule.waitUntil(15_000) {
      rule
          .onAllNodesWithText("Defina limites para seus gastos registrados.")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    rule.onNodeWithText("Defina limites para seus gastos registrados.").assertExists()
  }
}
