package com.example.allufinancas

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
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.ImportNoticeConfirmation
import com.example.allufinancas.data.ImportNoticeKind
import com.example.allufinancas.data.NotificationImportCandidate
import com.example.allufinancas.data.NotificationImportSource
import com.example.allufinancas.data.TransactionOrigin
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
class ImportInboxFlowTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()
  private lateinit var repository: DemoRepository
  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Before
  fun resetData() {
    runBlocking {
      repository = DemoRepository(context)
      withTimeout(15_000) { repository.ready.first { it } }
      repository.resetDemoData()
      repository.enableImportSource(NotificationImportSource.RICO)
      context.getSharedPreferences("allu_welcome", 0).edit().putBoolean("entered", true).commit()
    }
  }

  @After
  fun clearData() {
    runBlocking {
      repository.resetDemoData()
      context.getSharedPreferences("allu_welcome", 0).edit().putBoolean("entered", false).commit()
    }
  }

  @Test
  fun syntheticDraftsStayOutsideLedgerUntilIndividuallyResolvedOrConfirmed() {
    runBlocking {
      val date = "2026-10-16"
      val neutral = candidate("neutral-bill", ImportNoticeKind.NEUTRAL, 125_000, date)
      val expenseFood = candidate("expense-food", ImportNoticeKind.EXPENSE, -2_500, date)
      val expenseTransport = candidate("expense-transport", ImportNoticeKind.EXPENSE, -3_000, date)
      val income = candidate("income-credit", ImportNoticeKind.INCOME, 5_000, date)
      val capture = NotificationImportCapture(context)

      listOf(neutral, expenseFood, expenseTransport, income).forEach { candidate ->
        assertTrue(capture.capture(candidate))
      }
      assertTrue(repository.transactions.first().isEmpty())

      enterHome()
      rule.onNodeWithContentDescription("Extrato").performClick()
      rule.waitUntil(15_000) {
        rule
            .onAllNodesWithText("Revisar importações pendentes (4)")
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      rule.onNodeWithText("Revisar importações pendentes (4)").performClick()
      rule.waitUntil(15_000) {
        rule.onAllNodesWithText("Importações pendentes").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Saída a confirmar"))
      rule.onNodeWithText("Saída a confirmar").assertExists()
      rule.onNodeWithText("Confirmar saída").assertExists()
      rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Entrada a confirmar"))
      rule.onNodeWithText("Entrada a confirmar").assertExists()
      rule.onNodeWithText("Confirmar entrada").assertExists()
      rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Aviso neutro"))
      rule.onNodeWithText("Aviso neutro").assertExists()
      rule.onNodeWithText("Entendi, não lançar").assertExists().performClick()

      withTimeout(10_000) {
        repository.importNotices.first { notices ->
          notices.single { it.sourceEventHash == neutral.sourceEventHash }.status.name == "NEUTRAL"
        }
      }
      assertTrue(repository.transactions.first().isEmpty())

      rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Confirmar saída"))
      rule.onAllNodesWithText("Confirmar saída")[0].performClick()
      rule.waitUntil(10_000) {
        rule.onAllNodesWithText("Confirmar no extrato").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNodeWithText("Compras").performClick()
      rule.onNodeWithText("Alimentação").performClick()
      rule.onNodeWithText("Confirmar no extrato", useUnmergedTree = true).performClick()
      withTimeout(10_000) { repository.transactions.first { it.size == 1 } }

      rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Confirmar entrada"))
      rule.onNodeWithText("Confirmar entrada").performClick()
      rule.waitUntil(10_000) {
        rule.onAllNodesWithText("Confirmar no extrato").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNodeWithText("Receita").assertExists()
      rule.onNodeWithText("Confirmar no extrato", useUnmergedTree = true).performClick()
      withTimeout(10_000) { repository.transactions.first { it.size == 2 } }

      val remainingExpense =
          repository.importNotices.first().single { notice ->
            notice.kind == ImportNoticeKind.EXPENSE && notice.confirmedTransactionId == null
          }
      confirm(remainingExpense.id, remainingExpense.amountCents, "transporte", date)

      val confirmed = repository.transactions.first()
      assertEquals(3, confirmed.size)
      assertEquals(-500L, confirmed.sumOf { it.amountCents })
      assertEquals(
          setOf("alimentacao", "transporte", "receita"),
          confirmed.map { it.categoryId }.toSet(),
      )
      assertTrue(confirmed.all { it.origin == TransactionOrigin.NOTIFICATION_IMPORT })
      assertEquals(
          "NEUTRAL",
          repository.importNotices
              .first()
              .single { it.sourceEventHash == neutral.sourceEventHash }
              .status
              .name,
      )
      assertEquals(3, repository.importNotices.first().count { it.confirmedTransactionId != null })
    }
  }

  private suspend fun confirm(
      noticeId: String,
      amountCents: Long,
      categoryId: String,
      date: String,
  ) {
    assertTrue(
        repository.confirmImportNotice(
            noticeId,
            ImportNoticeConfirmation(
                merchant = "Revisado",
                description = "Lançamento revisado",
                amountCents = amountCents,
                date = date,
                categoryId = categoryId,
            ),
        ) != null,
    )
  }

  private fun enterHome() {
    if (rule.onAllNodesWithText("Adicionar lançamento").fetchSemanticsNodes().isNotEmpty()) {
      rule.onNodeWithText("Adicionar lançamento").performClick()
      rule.waitUntil(15_000) {
        rule.onAllNodesWithText("Novo lançamento").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNodeWithText("Cancelar", useUnmergedTree = true).performClick()
      rule.waitUntil(15_000) {
        rule.onAllNodesWithText("Comece seu extrato").fetchSemanticsNodes().isNotEmpty()
      }
      rule.onNodeWithContentDescription("Início").performClick()
    }
  }

  private fun candidate(
      eventKey: String,
      kind: ImportNoticeKind,
      amountCents: Long,
      date: String,
  ): NotificationImportCandidate {
    val source = NotificationImportSource.RICO
    val merchant =
        when (kind) {
          ImportNoticeKind.EXPENSE -> "Estabelecimento a confirmar"
          ImportNoticeKind.INCOME -> "Origem a confirmar"
          ImportNoticeKind.NEUTRAL -> "Fatura ou transferência"
          ImportNoticeKind.REFUND_REVIEW -> "Estorno ou reembolso"
        }
    return NotificationImportCandidate(
        source = source,
        sourceEventHash = NotificationImportFingerprints.sourceEvent(source, eventKey),
        fingerprintHash =
            NotificationImportFingerprints.notice(source, kind, merchant, amountCents, date),
        merchant = merchant,
        description = "Rascunho seguro",
        amountCents = amountCents,
        date = date,
        kind = kind,
        observedAt = Instant.parse("2026-10-16T12:00:00Z").toString(),
    )
  }
}
