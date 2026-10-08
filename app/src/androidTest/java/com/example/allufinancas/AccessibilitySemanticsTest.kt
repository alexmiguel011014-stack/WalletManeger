package com.example.allufinancas

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  @Before
  fun resetDemoData() = runBlocking {
    val repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    withTimeout(15_000) { repository.ready.first { it } }
    repository.resetDemoData()
  }

  @Test
  fun bottomNavigationDestinationsExposeLabelsActionsAndSelectedState() {
    enterDashboard()

    assertDestination("Início", "Saldo líquido dos lançamentos")
    assertDestination("Extrato", "Buscar estabelecimento")
    assertDestination("Planos", "Defina limites para seus gastos registrados.")
    assertDestination("Avisos", "Caixa de revisão")
    assertDestination("Assistente", "Pergunte sobre seus lançamentos")
  }

  @Test
  fun coreActionsAndLocalDataDisclosuresRemainAvailableToAssistiveTechnology() {
    enterDashboard()

    rule.onNodeWithText("DADOS LOCAIS", substring = true).assertExists()
    rule.onNodeWithContentDescription("Extrato").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Adicionar lançamento").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onAllNodesWithText("Adicionar lançamento").assertAny(hasClickAction())
    rule.onNodeWithText("Buscar estabelecimento").assertExists()

    rule.onNodeWithContentDescription("Planos").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Adicionar limite").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Adicionar limite").assertHasClickAction()

    rule.onNodeWithContentDescription("Avisos").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Revisar lançamentos").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Revisar lançamentos").assertHasClickAction()

    rule.onNodeWithContentDescription("Assistente").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Pergunte sobre seus lançamentos").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Sua pergunta").assertExists()
    rule.onNodeWithText("Enviar").assertHasClickAction()
  }

  private fun assertDestination(label: String, visibleContent: String) {
    val destination = rule.onNodeWithContentDescription(label)
    destination.assertHasClickAction().performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText(visibleContent).fetchSemanticsNodes().isNotEmpty()
    }
    destination.assertIsSelected()
    rule.onNodeWithText(visibleContent).assertExists()
  }

  private fun enterDashboard() {
    if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Começar a cadastrar").performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty()
    }
  }
}
