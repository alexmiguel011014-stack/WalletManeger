package com.example.allufinancas

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.ImportNoticeKind
import com.example.allufinancas.data.NotificationImportCandidate
import com.example.allufinancas.data.NotificationImportSource
import com.example.allufinancas.notification.NotificationImportCapture
import com.example.allufinancas.notification.NotificationImportFingerprints
import java.time.Instant
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
class FirstSessionFlowTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()
  private lateinit var repository: DemoRepository

  @Before
  fun resetFirstSession() {
    runBlocking {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      repository = DemoRepository(context)
      withTimeout(15_000) { repository.ready.first { it } }
      repository.resetDemoData()
      repository.setDarkTheme(false)
      repository.setReduceMotion(true)
      repository.setShowCents(true)
      context.getSharedPreferences("allu_welcome", 0).edit().putBoolean("entered", false).commit()
    }
  }

  @After
  fun restoreFirstSession() {
    runBlocking {
      repository.resetDemoData()
      InstrumentationRegistry.getInstrumentation()
          .targetContext
          .getSharedPreferences("allu_welcome", 0)
          .edit()
          .putBoolean("entered", false)
          .commit()
    }
  }

  @Test
  fun welcomePrimaryActionOpensTheManualEntryEditor() {
    rule.activityRule.scenario.recreate()

    rule.onNodeWithText("Adicionar lançamento").assertHasClickAction().performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Novo lançamento").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithTag("transaction-editor").assertExists()
    rule.onNodeWithTag("transaction-editor-amount").assertExists()
    rule.onNodeWithTag("transaction-editor-merchant").assertExists()
  }

  @Test
  fun emptyTabsExposeOneUsefulActionWithoutPrematureControls() {
    enterEmptyApp()

    rule.onNodeWithTag("first-session-local-disclosure").assertExists()
    rule.onNodeWithTag("home-first-entry").assertHasClickAction()
    assertBottomNavigationLabels()

    rule.onNodeWithContentDescription("Extrato").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Comece seu extrato").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithTag("transactions-add").assertHasClickAction()
    rule.onNodeWithTag("transaction-search").assertDoesNotExist()
    rule.onNodeWithTag("transaction-categories").assertDoesNotExist()
    rule.onNodeWithTag("transactions-imports-shortcut").assertDoesNotExist()

    rule.onNodeWithContentDescription("Planos").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Nenhum limite cadastrado").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithTag("budget-add").assertHasClickAction()
    rule.onNodeWithTag("budget-scenario").assertDoesNotExist()

    rule.onNodeWithContentDescription("Avisos").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Nenhum aviso gerado").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Adicionar lançamento").assertHasClickAction()
    rule.onNodeWithTag("alerts-review-shortcut").assertDoesNotExist()
    rule.onNodeWithText("Ativos").assertDoesNotExist()
  }

  @Test
  fun localizedDateSavesAsCanonicalIsoAndRevealsStatementFilters() {
    openEmptyStatementEditor()

    rule
        .onNodeWithText("Informe um valor positivo válido, por exemplo 125,50.")
        .assertDoesNotExist()
    rule.onNodeWithText("Informe uma data válida em dd/MM/aaaa.").assertDoesNotExist()
    rule.onNodeWithTag("transaction-editor-amount").performTextInput("12,34")
    rule.onNodeWithTag("transaction-editor-merchant").performTextInput("Padaria de teste")
    rule.onNodeWithTag("transaction-editor-date").performTextClearance()
    rule.onNodeWithTag("transaction-editor-date").performTextInput("15/10/2026")
    rule.onNodeWithTag("transaction-editor-save").performClick()

    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Padaria de teste").fetchSemanticsNodes().isNotEmpty()
    }
    val transaction =
        runBlocking {
              withTimeout(10_000) {
                repository.transactions.first { rows ->
                  rows.any { it.merchant == "Padaria de teste" }
                }
              }
            }
            .single { it.merchant == "Padaria de teste" }
    assertEquals("2026-10-15", transaction.date)
    rule.onNodeWithTag("transaction-search").assertExists()
    rule.onNodeWithTag("transaction-categories").assertExists()
  }

  @Test
  fun invalidManualEntryKeepsTypedFieldsAndExplainsEachInvalidField() {
    openEmptyStatementEditor()

    rule.onNodeWithTag("transaction-editor-merchant").performTextInput("Padaria inválida")
    rule.onNodeWithTag("transaction-editor-date").performTextClearance()
    rule.onNodeWithTag("transaction-editor-date").performTextInput("31/02/2026")
    rule.onNodeWithTag("transaction-editor-save").performClick()

    rule.onNodeWithTag("transaction-editor").assertExists()
    rule.onNodeWithText("Informe um valor positivo válido, por exemplo 125,50.").assertExists()
    rule.onNodeWithText("Informe uma data válida em dd/MM/aaaa.").assertExists()
    rule.onNodeWithText("Padaria inválida").assertExists()
    rule.onNodeWithText("31/02/2026").assertExists()
  }

  @Test
  fun pendingImportsArePrioritizedWithoutChangingLedgerTotals() {
    runBlocking {
      repository.enableImportSource(NotificationImportSource.RICO)
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      assertTrue(NotificationImportCapture(context).capture(pendingExpense()))
      assertTrue(repository.transactions.first().isEmpty())
      enterEmptyApp()

      rule.waitUntil(15_000) {
        rule
            .onAllNodesWithText("Importações aguardam sua revisão")
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      rule.onNodeWithText("Ainda não há lançamentos salvos.").assertExists()
      rule.onNodeWithTag("home-imports-shortcut").assertHasClickAction()

      rule.onNodeWithContentDescription("Extrato").performClick()
      rule.waitUntil(15_000) {
        rule
            .onAllNodesWithText("Revisar importações pendentes (1)")
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      rule.onNodeWithText("Comece seu extrato").assertExists()
      rule.onNodeWithTag("transactions-imports-shortcut").assertHasClickAction()
    }
  }

  private fun enterEmptyApp() {
    rule.onNodeWithText("Adicionar lançamento").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Novo lançamento").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Cancelar", useUnmergedTree = true).performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Comece seu extrato").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithContentDescription("Início").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Seus primeiros passos").fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun openEmptyStatementEditor() {
    enterEmptyApp()
    rule.onNodeWithContentDescription("Extrato").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Comece seu extrato").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithTag("transactions-add").performClick()
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Novo lançamento").fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun assertBottomNavigationLabels() {
    listOf("Início", "Extrato", "Planos", "Avisos").forEach { label ->
      rule.onNodeWithContentDescription(label).assertHasClickAction()
    }
  }

  private fun pendingExpense(): NotificationImportCandidate {
    val date = "2026-10-15"
    return NotificationImportCandidate(
        source = NotificationImportSource.RICO,
        sourceEventHash =
            NotificationImportFingerprints.sourceEvent(
                NotificationImportSource.RICO,
                "first-session-pending-expense",
                "Compra aprovada R$ 8,89",
                Instant.parse("2026-10-15T12:00:00Z"),
            ),
        fingerprintHash =
            NotificationImportFingerprints.notice(
                NotificationImportSource.RICO,
                ImportNoticeKind.EXPENSE,
                "Estabelecimento a confirmar",
                -889,
                date,
            ),
        merchant = "Padaria",
        description = "Compra aprovada pela Rico",
        amountCents = -889,
        date = date,
        kind = ImportNoticeKind.EXPENSE,
        observedAt = Instant.parse("2026-10-15T12:00:00Z").toString(),
    )
  }
}
