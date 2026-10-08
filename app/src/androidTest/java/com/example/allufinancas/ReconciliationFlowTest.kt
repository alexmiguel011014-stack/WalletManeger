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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.DemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReconciliationFlowTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()
  private lateinit var repository: DemoRepository

  @Before
  fun resetDemo() = runBlocking {
    repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    withTimeout(15_000) { repository.ready.first { it } }
    seedLocalLedger(repository, InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun reviewOpensInsideAlertsAndSystemBackReturnsToAlerts() {
    openReview()
    rule.onNodeWithText("Pendentes (3)").assertExists()
    Espresso.pressBack()
    rule.onNodeWithContentDescription("Avisos").assertIsSelected()
    rule.onNodeWithText("Revisar lançamentos").assertExists()
  }

  @Test
  fun categoryDecisionAppearsUnderDecidedAndUndoRestoresPendingSource() {
    openReview()
    val reviewList = rule.onNode(hasScrollToIndexAction())
    reviewList.performScrollToNode(hasText("ID t07", substring = true))
    reviewList.performScrollToNode(hasContentDescription("Usar categoria Transporte"))
    rule.onNodeWithContentDescription("Usar categoria Transporte").performClick()

    val decidedCase = runBlocking {
      withTimeout(10_000) {
        repository.reconciliationCases
            .first { cases ->
              cases.any { it.id == "uncategorized-t07" && it.decided }
            }
            .first { it.id == "uncategorized-t07" }
      }
    }
    assertTrue(decidedCase.decided)
    reviewList.performScrollToNode(hasText("Decididos (1)"))
    rule.onNodeWithText("Decididos (1)").performClick()
    reviewList.performScrollToNode(hasText("Categoria local escolhida: Transporte"))
    rule.onNodeWithText("Categoria local escolhida: Transporte").assertExists()
    rule.onNodeWithText("Desfazer e reabrir").performScrollTo().performClick()

    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Decididos (0)").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Pendentes (3)").performClick()
    val restored = runBlocking { repository.transactions.first().first { it.id == "t07" } }
    val source = runBlocking {
      repository.reconciliationCases.first().first { it.id == "uncategorized-t07" }
    }
    assertEquals("", restored.categoryId)
    assertEquals(-2_890L, restored.amountCents)
    assertEquals(LocalLedgerFixtures.date(15), restored.date)
    assertEquals(false, source.decided)
    reviewList.performScrollToNode(hasText("ID t07", substring = true))
    rule.onNodeWithText("Data: ${LocalLedgerFixtures.date(15)}", substring = true).assertExists()
    rule
        .onNode(
            hasContentDescription(
                "Lançamento t07, Transporte Exemplo, ${LocalLedgerFixtures.date(15)}, ${DemoAnalysis.money(restored.amountCents)}",
            ),
        )
        .assertExists()
  }

  private fun openReview() {
    if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Começar a cadastrar").performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithContentDescription("Avisos").performClick()
    rule.onNodeWithText("Revisar lançamentos").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Pendentes (3)").fetchSemanticsNodes().isNotEmpty()
    }
  }
}
