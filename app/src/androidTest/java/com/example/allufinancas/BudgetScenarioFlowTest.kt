package com.example.allufinancas

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.DemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetScenarioFlowTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()
  private lateinit var repository: DemoRepository

  @Before
  fun resetDemoState() = runBlocking {
    repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    withTimeout(15_000) { repository.ready.first { it } }
    seedLocalLedger(repository, InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @After fun restoreDemoState() = runBlocking { repository.resetDemoData() }

  @Test
  fun scenarioSurvivesRecreationButNeverChangesPersistedBudget() {
    enterDemo()
    val savedLimit = persistedFoodLimit()
    assertEquals(50_000L, savedLimit)

    rule.onNodeWithText("Planos").performClick()
    rule.onNodeWithText("Escolha uma categoria").performClick()
    rule.onNodeWithContentDescription("Selecionar categoria Alimentação").performClick()
    rule.onNodeWithText("Limite hipotético em reais").performTextInput("1.000,00")

    val expectedBalance = "saldo hipotético: ${brl(34_458)}"
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText(expectedBalance, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Simulação — não altera seu limite").assertExists()
    rule
        .onNodeWithText("gasto registrado em ${LocalLedgerFixtures.MONTH}", substring = true)
        .assertExists()
    rule
        .onNodeWithText(
            "Diferença ante limite atual: ${brl(50_000)} a mais (Alimentação).",
            substring = true,
        )
        .assertExists()
    assertEquals(savedLimit, persistedFoodLimit())

    rule.activityRule.scenario.recreate()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText(expectedBalance, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Simulação — não altera seu limite").assertExists()
    assertEquals(savedLimit, persistedFoodLimit())

    rule.onNodeWithText("Limpar simulação").performScrollTo().performClick()
    rule.onNodeWithText("Escolha uma categoria").assertExists()
    assertTrue(
        rule
            .onAllNodesWithText("saldo hipotético", substring = true)
            .fetchSemanticsNodes()
            .isEmpty()
    )
    assertEquals(savedLimit, persistedFoodLimit())
  }

  private fun enterDemo() {
    if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Começar a cadastrar").performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun persistedFoodLimit(): Long = runBlocking {
    withTimeout(10_000) {
      repository.budgets
          .first { rows -> rows.any { it.categoryId == "alimentacao" } }
          .first { it.categoryId == "alimentacao" }
          .limitCents
    }
  }
}
