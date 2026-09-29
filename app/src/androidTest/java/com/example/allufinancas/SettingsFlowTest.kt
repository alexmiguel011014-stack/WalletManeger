package com.example.allufinancas

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.DemoSeed
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
class SettingsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: DemoRepository
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val welcomePreferences = context.getSharedPreferences("allu_welcome", 0)
    private val previousWelcomeState = welcomePreferences.getBoolean("entered", false)

    init { welcomePreferences.edit().putBoolean("entered", false).commit() }

    @Before fun resetDemoState() = runBlocking {
        repository = DemoRepository(context)
        withTimeout(15_000) { repository.ready.first { it } }
        restoreDemoState()
    }

    @After fun restoreDemoStateAfterTest() {
        runBlocking {
            restoreDemoState()
            welcomePreferences.edit().putBoolean("entered", previousWelcomeState).commit()
        }
    }

    @Test fun privacyEducationIsReachableFromWelcomeAndSettings() {
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Privacidade e dados de exemplo").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Privacidade e dados de exemplo").performClick()
        assertPrivacyEducationVisible()

        rule.onNodeWithText("Voltar").performClick()
        rule.onNodeWithText("Explorar demonstração").performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ajustes").performClick()
        assertSettingsPrivacyCopy()
        assertNoExportOrDeleteAction()
        rule.onNodeWithText("Privacidade e limitações").performClick()
        assertPrivacyEducationVisible()
    }

    @Test fun preferencesSurviveRecreationAndDataResetCanBeCancelledOrConfirmed() {
        enterSettings()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Preferência de exibição").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Modo escuro").assertIsOff()
        rule.onNodeWithContentDescription("Reduzir movimento").assertIsOff()
        rule.onNodeWithContentDescription("Mostrar centavos").assertIsOn()

        rule.onNodeWithContentDescription("Modo escuro").performClick()
        waitForToggle("Modo escuro", ToggleableState.On)
        rule.onNodeWithContentDescription("Modo escuro").assertIsOn()

        rule.onNodeWithContentDescription("Reduzir movimento").performClick()
        waitForToggle("Reduzir movimento", ToggleableState.On)
        rule.onNodeWithContentDescription("Reduzir movimento").assertIsOn()

        rule.onNodeWithContentDescription("Mostrar centavos").performClick()
        waitForToggle("Mostrar centavos", ToggleableState.Off)
        rule.onNodeWithContentDescription("Mostrar centavos").assertIsOff()

        rule.activityRule.scenario.recreate()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Moeda fixa: BRL (R$), formato pt-BR").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Modo escuro").assertIsOn()
        rule.onNodeWithContentDescription("Reduzir movimento").assertIsOn()
        rule.onNodeWithContentDescription("Mostrar centavos").assertIsOff()
        rule.onNodeWithText("Preferência de exibição").assertExists()

        runBlocking {
            repository.updateBudget("alimentacao", 100_000)
            repository.sendMessage("Maior despesa")
        }
        openResetConfirmation()
        rule.onNodeWithText("Cancelar", useUnmergedTree = true).performClick()
        assertTrue(rule.onAllNodesWithText("Restaurar demonstração?").fetchSemanticsNodes().isEmpty())
        runBlocking {
            assertEquals(100_000, repository.budgets.first().first { it.categoryId == "alimentacao" }.limitCents)
            assertTrue(repository.messages.first().isNotEmpty())
        }

        openResetConfirmation()
        assertEquals(
            2,
            rule.onAllNodesWithText("remove alterações locais da demonstração e mensagens digitadas", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size,
        )
        rule.onNodeWithText("Não afeta bancos, WhatsApp nem outros apps", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Preferências de exibição e a escolha de entrada inicial serão preservadas", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Restaurar", useUnmergedTree = true).performClick()
        runBlocking {
            withTimeout(10_000) {
                repository.budgets.first {
                    it.associateBy { budget -> budget.categoryId } == DemoSeed.budgets.associateBy { budget -> budget.categoryId }
                }
            }
            withTimeout(10_000) { repository.messages.first { it.isEmpty() } }
            val preferences = repository.preferences.first()
            assertTrue(preferences.darkTheme && preferences.reduceMotion && !preferences.showCents)
        }
        assertTrue(welcomePreferences.getBoolean("entered", false))
        rule.activityRule.scenario.recreate()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Moeda fixa: BRL (R$), formato pt-BR").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Modo escuro").assertIsOn()
        rule.onNodeWithContentDescription("Reduzir movimento").assertIsOn()
        rule.onNodeWithContentDescription("Mostrar centavos").assertIsOff()
    }

    private fun assertPrivacyEducationVisible() {
        rule.onNodeWithText("Privacidade e conexões").assertExists()
        rule.onNodeWithText("não se conecta a bancos", substring = true).assertExists()
        rule.onNodeWithText("integração ativa com WhatsApp", substring = true).assertExists()
        rule.onNodeWithText("Não há coleta ou envio de dados externos", substring = true).assertExists()
        rule.onNodeWithText("Dados financeiros são fictícios e ficam neste dispositivo", substring = true).assertExists()
        rule.onNodeWithText("O texto digitado no chat também fica neste dispositivo, mas pode não ser fictício", substring = true).assertExists()
        rule.onNodeWithText("Exportar dados e excluir itens individualmente não estão disponíveis", substring = true).assertExists()
        rule.onNodeWithText("possibilidade conceitual, não uma promessa", substring = true).assertExists()
        rule.onNodeWithText("consentimento explícito", substring = true).assertExists()
        assertNoExportOrDeleteAction()
    }

    private fun assertSettingsPrivacyCopy() {
        rule.onNodeWithText("Os dados financeiros de exemplo são fictícios e ficam neste dispositivo", substring = true).assertExists()
        rule.onNodeWithText("O texto digitado no chat também fica local, mas pode não ser fictício", substring = true).assertExists()
        rule.onNodeWithText("Exportar dados e excluir itens individualmente não estão disponíveis", substring = true).assertExists()
    }

    private fun assertNoExportOrDeleteAction() {
        listOf("Exportar", "Excluir").forEach { actionText ->
            val actionableMatches = rule.onAllNodesWithText(actionText, substring = true)
                .fetchSemanticsNodes()
                .count { it.config.contains(SemanticsActions.OnClick) }
            assertEquals("Não deve existir ação: $actionText", 0, actionableMatches)
        }
    }

    private fun waitForToggle(label: String, expected: ToggleableState) {
        rule.waitUntil("$label deve chegar a $expected", 10_000) {
            rule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().any {
                it.config.contains(SemanticsProperties.ToggleableState) &&
                    it.config[SemanticsProperties.ToggleableState] == expected
            }
        }
    }

    private fun enterSettings() {
        if (rule.onAllNodesWithText("Explorar demonstração").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explorar demonstração").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ajustes").performClick()
    }

    private fun openResetConfirmation() {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Restaurar demonstração", substring = true))
        rule.onNodeWithText("Restaurar demonstração")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private suspend fun restoreDemoState() {
        repository.resetDemoData()
        repository.setDarkTheme(false)
        repository.setReduceMotion(false)
        repository.setShowCents(true)
    }
}
