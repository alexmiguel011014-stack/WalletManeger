package com.example.allufinancas

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
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

  @Before
  fun resetDemoState() = runBlocking {
    repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    withTimeout(15_000) { repository.ready.first { it } }
    seedLocalLedger(repository, InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @After fun restoreDemoStateAfterTest() = runBlocking { restoreDemoState() }

  @Test
  fun categoryCorrectionAppearsInStatementAndPersistsInRoom() {
    enterStatement()
    rule.onNodeWithText(LocalLedgerFixtures.MONTH).performScrollTo().performClick()
    rule.onNodeWithText("Buscar estabelecimento").performScrollTo().performTextInput("Notebook")
    Espresso.closeSoftKeyboard()
    rule.waitForIdle()
    rule.onNodeWithText("Loja Exemplo").performScrollTo().performClick()

    rule.onNodeWithText("Parcela 3 de 10").assertExists()
    rule.onNodeWithText("DADOS LOCAIS", substring = true).assertExists()
    rule
        .onNodeWithText("Fonte: lançamentos confirmados neste dispositivo", substring = true)
        .assertExists()
    rule.onNodeWithText("Escopo: Detalhe · Loja Exemplo", substring = true).assertExists()
    rule.onNodeWithText("Categoria: Compras").assertExists()

    val casaNodes = rule.onAllNodesWithText("Casa").fetchSemanticsNodes()
    assertTrue("O seletor de categoria deve estar disponível no detalhe", casaNodes.isNotEmpty())
    rule.onAllNodesWithText("Casa")[casaNodes.lastIndex].performClick()
    rule.waitUntil(10_000) {
      rule
          .onAllNodesWithText("Casa · ${LocalLedgerFixtures.date(12)}", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    rule.activityRule.scenario.recreate()
    rule.waitUntil(15_000) { rule.onAllNodesWithText("Extrato").fetchSemanticsNodes().isNotEmpty() }
    rule.waitUntil(10_000) {
      rule
          .onAllNodesWithText("Casa · ${LocalLedgerFixtures.date(12)}", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    rule.onNodeWithText("Casa · ${LocalLedgerFixtures.date(12)}", substring = true).assertExists()
  }

  @Test
  fun selectingAllPeriodClearsDateRange() {
    enterStatement()
    val fromDate = LocalLedgerFixtures.displayDate(10)
    val toDate = LocalLedgerFixtures.displayDate(20)
    rule.onNodeWithText("De · dd/MM/aaaa").performScrollTo().performTextInput(fromDate)
    rule.onNodeWithText("Até · dd/MM/aaaa").performScrollTo().performTextInput(toDate)
    assertTrue(
        rule.onAllNodesWithText(fromDate, substring = true).fetchSemanticsNodes().isNotEmpty()
    )
    assertTrue(rule.onAllNodesWithText(toDate, substring = true).fetchSemanticsNodes().isNotEmpty())

    rule.onNodeWithText("Todo período").performScrollTo().performClick()
    rule.onNodeWithText("Buscar estabelecimento").performScrollTo().assertExists()
    rule
        .onNodeWithText(LocalLedgerFixtures.previousMonthDate(26), substring = true)
        .performScrollTo()
        .assertExists()
  }

  private suspend fun restoreDemoState() {
    repository.resetDemoData()
    repository.setDarkTheme(false)
    repository.setReduceMotion(false)
    repository.setShowCents(true)
  }

  private fun enterStatement() {
    if (rule.onAllNodesWithText("Adicionar lançamento").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Adicionar lançamento").performClick()
      rule.waitUntil(15_000) {
        rule.onAllNodesWithText("Novo lançamento").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNodeWithText("Cancelar", useUnmergedTree = true).performClick()
      rule.waitUntil(15_000) {
        rule.onAllNodesWithText("Buscar estabelecimento").fetchSemanticsNodes().isNotEmpty() ||
            rule.onAllNodesWithText("Comece seu extrato").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNodeWithContentDescription("Início").performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Saldo líquido dos lançamentos").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Extrato").performClick()
  }
}
