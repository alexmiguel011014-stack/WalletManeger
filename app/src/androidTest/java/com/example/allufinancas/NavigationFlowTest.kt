package com.example.allufinancas

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()
  private lateinit var repository: DemoRepository

  @Before
  fun resetDemoFixture() = runBlocking {
    repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    withTimeout(15_000) { repository.ready.first { it } }
    seedLocalLedger(repository, InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun filtersSurviveTabSwitchBackStackAndActivityRecreation() {
    enterDashboard()
    assertScreenReady("Saldo líquido dos lançamentos")
    rule.onNodeWithContentDescription("Extrato").performClick()
    assertScreenReady("Buscar estabelecimento")
    rule.onNodeWithTag("transaction-categories").performScrollToNode(hasText("Compras"))
    rule.onNodeWithText("Compras").performClick()
    rule.onNodeWithText(LocalLedgerFixtures.MONTH).performScrollTo().performClick()
    rule.onNodeWithText("Buscar estabelecimento").performScrollTo().performTextInput("Notebook")
    assertScreenReady("Loja Exemplo")
    rule.onNodeWithText("Loja Exemplo").performScrollTo().assertExists()

    rule.onNodeWithContentDescription("Avisos").performClick()
    assertScreenReady("Gerado localmente em")
    rule.onNodeWithContentDescription("Extrato").performClick()
    assertScreenReady("Loja Exemplo")
    rule.onNodeWithText("Notebook").assertExists()
    assertMonthSelected()
    rule.onNodeWithText("Loja Exemplo").assertExists()

    Espresso.closeSoftKeyboard()
    rule.waitForIdle()
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Ajustes").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Ajustes").performClick()
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Preferência de exibição").fetchSemanticsNodes().isNotEmpty()
    }
    val privacyAction = hasText("Privacidade e limitações") and hasClickAction()
    rule.onNode(hasScrollToIndexAction()).performScrollToNode(privacyAction)
    rule.onNode(privacyAction).performClick()
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Privacidade e conexões").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Privacidade e conexões").assertExists()
    rule.onNodeWithText("O app não usa API bancária", substring = true).assertExists()
    pressBack()
    rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Preferência de exibição"))
    rule.onNodeWithText("Preferência de exibição").assertExists()
    pressBack()
    rule.onNodeWithText("Notebook").assertExists()
    assertMonthSelected()

    rule.activityRule.scenario.recreate()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Buscar estabelecimento").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Notebook").assertExists()
    rule.onNodeWithText("Loja Exemplo").assertExists()
    assertMonthSelected()

    rule.onNodeWithText("Loja Exemplo").performScrollTo().performClick()
    assertScreenReady("Estabelecimento: Loja Exemplo")
    rule.onNodeWithText("Estabelecimento: Loja Exemplo").assertExists()
    pressBack()
    rule.onNodeWithText("Notebook").assertExists()
    rule.onNodeWithText("Loja Exemplo").assertExists()
  }

  @Test
  fun navigationSwitchesBetweenBottomBarAndRailAtWideWidth() {
    rule.activityRule.scenario.onActivity {
      it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    enterDashboard()
    rule.waitUntil(10_000) {
      rule.onAllNodesWithTag("bottom-navigation").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithTag("navigation-rail").assertDoesNotExist()

    rule.activityRule.scenario.onActivity {
      it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithTag("navigation-rail").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithTag("bottom-navigation").assertDoesNotExist()
    rule.onNodeWithContentDescription("Avisos").performClick()
    assertScreenReady("Gerado localmente em")
    rule.onNodeWithContentDescription("Início").performClick()
    assertScreenReady("Saldo líquido dos lançamentos")
    rule.onNodeWithContentDescription("Planos").performClick()
    assertScreenReady("Defina limites para seus gastos registrados.")
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

  private fun pressBack() {
    Espresso.pressBack()
  }

  private fun assertMonthSelected() {
    assertTrue(
        rule
            .onAllNodesWithText(LocalLedgerFixtures.MONTH, substring = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    )
  }

  private fun assertScreenReady(text: String) {
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
  }
}
