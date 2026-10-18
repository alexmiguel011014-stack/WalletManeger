package com.example.allufinancas

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
    seedLocalLedger(repository, InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun welcomeExposesWalletManagerNameAndBrandMark() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    context.getSharedPreferences("allu_welcome", 0).edit().putBoolean("entered", false).commit()
    rule.activityRule.scenario.recreate()

    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("wallet", substring = true).fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("wallet", substring = true).assertExists()
    rule.onNodeWithContentDescription("Símbolo wallet Manager").assertExists()
  }

  @Test
  fun bottomNavigationHasExactlyFourDestinationsWithoutAssistant() {
    enterDashboard()

    listOf("Início", "Extrato", "Planos", "Avisos").forEach { label ->
      rule.onAllNodesWithContentDescription(label).assertCountEquals(1)
      assertDestination(label)
    }
    rule.onAllNodesWithContentDescription("Assistente").assertCountEquals(0)
  }

  @Test
  fun coreActionsAndLocalDataDisclosuresRemainAvailableToAssistiveTechnology() {
    enterDashboard()

    rule.onNodeWithText("Saldo líquido dos lançamentos").assertExists()
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
  }

  private fun assertDestination(label: String) {
    val destination = rule.onNodeWithContentDescription(label)
    destination.assertHasClickAction().performClick()
    destination.assertIsSelected()
  }

  private fun enterDashboard() {
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
  }
}
