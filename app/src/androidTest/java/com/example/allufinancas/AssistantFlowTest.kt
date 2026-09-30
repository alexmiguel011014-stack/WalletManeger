package com.example.allufinancas

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.MessageRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class AssistantFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: DemoRepository

    @Before fun resetDemoState() = runBlocking {
        repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        withTimeout(15_000) { repository.ready.first { it } }
        seedLocalLedger(repository, InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After fun restoreDemoStateAfterTest() = runBlocking { restoreDemoState() }

    @Test fun citedAnswerOpensReadableSourcesAndInputBlocksOver500Characters() {
        enterAssistant()
        rule.onNodeWithText("Sua pergunta").performTextInput("Maior despesa")
        rule.onNodeWithText("Enviar").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Ver fontes").fetchSemanticsNodes().isNotEmpty()
        }
        val answer = runBlocking {
            withTimeout(10_000) { repository.messages.first { messages -> messages.any { it.role == MessageRole.ASSISTANT } } }
                .last { it.role == MessageRole.ASSISTANT }
        }
        assertTrue(answer.citationTransactionIds.isNotEmpty())
        rule.onNodeWithText("Ver fontes").performClick()
        rule.onNodeWithText("ID t11 · Moradia Exemplo", substring = true).assertExists()
        rule.onNodeWithText("Moradia Exemplo · Casa", substring = true).assertExists()
        assertTrue(rule.onAllNodesWithText("2026-09-07", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("1.800,00", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("Os registros podem ter mudado", substring = true).fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithText("Fechar").performClick()

        val field = rule.onNodeWithText("Sua pergunta")
        field.performTextInput("x".repeat(501))
        rule.onNodeWithText("Enviar").assertIsNotEnabled()
        field.performTextClearance()
        field.performTextInput("x".repeat(500))
        rule.onNodeWithText("Enviar").assertIsEnabled().performClick()
        rule.waitUntil(10_000) {
            runBlocking { repository.messages.first().count { it.role == MessageRole.USER } >= 2 }
        }
    }

    @Test fun unsupportedMonthAndRealtimeQuestionsDoNotReturnSeptemberFigures() {
        enterAssistant()
        rule.onNodeWithText("Sua pergunta").performTextInput("Quanto gastei em agosto?")
        rule.onNodeWithText("Enviar").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Não encontrei dados cadastrados para esse período", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val answer = runBlocking {
            withTimeout(10_000) { repository.messages.first { it.any { row -> row.role == MessageRole.ASSISTANT } } }
                .last { it.role == MessageRole.ASSISTANT }
        }
        assertTrue(answer.citationTransactionIds.isEmpty())
        assertFalse(answer.text.contains("280.462"))
        assertFalse(answer.text.contains("72,50"))
    }

    private fun enterAssistant() {
        if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Começar a cadastrar").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Assistente").performClick()
        rule.onNodeWithText("DADOS LOCAIS", substring = true).assertExists()
        rule.onNodeWithText("Escopo: Assistente local · período ${YearMonth.now()}", substring = true).assertExists()
    }

    private suspend fun restoreDemoState() {
        repository.resetDemoData()
        repository.setDarkTheme(false)
        repository.setReduceMotion(false)
        repository.setShowCents(true)
    }
}
