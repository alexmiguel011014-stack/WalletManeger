package com.example.allufinancas

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.MessageRole
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
class BudgetFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: DemoRepository

    @Before fun resetDemoState() = runBlocking {
        repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        withTimeout(15_000) { repository.ready.first { it } }
        restoreDemoState()
    }

    @After fun restoreDemoStateAfterTest() = runBlocking { restoreDemoState() }

    @Test fun budgetEditUpdatesHomeAndAssistantAndRestoreCanCancelOrConfirm() {
        enterDemo()
        rule.onNodeWithText("Planos").performClick()
        rule.onAllNodesWithText("Alterar limite")[0].performScrollTo().performClick()
        rule.onNodeWithText("Valor em reais").performClick().performTextClearance()
        rule.onNodeWithText("Valor em reais").performTextInput("1.000,00")
        rule.onNodeWithText("Salvar").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("1.000,00", substring = true).fetchSemanticsNodes().isNotEmpty() }

        rule.onNodeWithText("Início").performClick()
        val updatedBudget = "${DemoAnalysis.money(65_542)} de ${DemoAnalysis.money(100_000)}"
        val homeList = rule.onNode(hasScrollToIndexAction())
        homeList.performScrollToNode(hasText(updatedBudget, substring = true))
        rule.waitUntil(10_000) { rule.onAllNodesWithText(updatedBudget, substring = true).fetchSemanticsNodes().isNotEmpty() }

        rule.onNodeWithText("Assistente").performClick()
        rule.onNodeWithText("Sua pergunta").performTextInput("Quanto resta do orçamento de alimentação?")
        rule.onNodeWithText("Enviar").performClick()
        val remaining = DemoAnalysis.money(34_458)
        val persistedMessages = runBlocking {
            withTimeout(10_000) {
                repository.messages.first { messages ->
                    messages.any { it.role == MessageRole.ASSISTANT && remaining in it.text }
                }
            }
        }
        assertTrue(persistedMessages.any { it.role == MessageRole.ASSISTANT && remaining in it.text })

        rule.onNodeWithText("Planos").performClick()
        openRestoreConfirmation()
        rule.onNodeWithText("Cancelar", useUnmergedTree = true).performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Restaurar tudo", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        val updatedPlanBudget = "${DemoAnalysis.money(65_542)} usados de ${DemoAnalysis.money(100_000)}"
        scrollPlansTo(updatedPlanBudget)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("1.000,00", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(updatedPlanBudget, substring = true).assertExists()

        openRestoreConfirmation()
        rule.onNodeWithText("Restaurar tudo", useUnmergedTree = true).performClick()
        val restoredBudget = "${DemoAnalysis.money(65_542)} usados de ${DemoAnalysis.money(50_000)}"
        scrollPlansTo(restoredBudget)
        rule.waitUntil(15_000) { rule.onAllNodesWithText(restoredBudget, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun enterDemo() {
        if (rule.onAllNodesWithText("Explorar demonstração").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explorar demonstração").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openRestoreConfirmation() {
        scrollPlansTo("Restaurar dados fictícios")
        rule.onNodeWithText("Restaurar dados fictícios").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun scrollPlansTo(text: String) {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text, substring = true))
    }

    private suspend fun restoreDemoState() {
        repository.resetDemoData()
        repository.setDarkTheme(false)
        repository.setReduceMotion(false)
        repository.setShowCents(true)
    }
}
