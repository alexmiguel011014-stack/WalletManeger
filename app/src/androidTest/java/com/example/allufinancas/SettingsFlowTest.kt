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
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Privacidade e dados locais").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Privacidade e dados locais").performClick()
        assertPrivacyEducationVisible()

        rule.onNodeWithText("Voltar").performClick()
        rule.onNodeWithText("Começar a cadastrar").performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ajustes").performClick()
        assertSettingsPrivacyCopy()
        assertNoExportAction()
        rule.onNodeWithText("Privacidade e limitações").performClick()
        assertPrivacyEducationVisible()
    }

    @Test fun preferencesSurviveRecreationAndLocalDataDeletionCanBeCancelledOrConfirmed() {
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
        assertTrue(rule.onAllNodesWithText("Excluir todos os dados locais?").fetchSemanticsNodes().isEmpty())
        runBlocking {
            assertEquals(100_000, repository.budgets.first().first { it.categoryId == "alimentacao" }.limitCents)
            assertTrue(repository.messages.first().isNotEmpty())
        }

        openResetConfirmation()
        assertEquals(
            2,
            rule.onAllNodesWithText("Apaga seus lançamentos, limites, metas, avisos, decisões e mensagens", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size,
        )
        rule.onNodeWithText("As categorias padrão, preferências de exibição e escolha de entrada inicial serão preservadas", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Excluir dados", useUnmergedTree = true).performClick()
        runBlocking {
            withTimeout(10_000) {
                repository.budgets.first { it.isEmpty() }
            }
            withTimeout(10_000) { repository.transactions.first { it.isEmpty() } }
            withTimeout(10_000) { repository.categories.first { it.isNotEmpty() } }
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
        rule.onNodeWithText("Não há conexão com bancos", substring = true).assertExists()
        rule.onNodeWithText("nem integração com WhatsApp", substring = true).assertExists()
        rule.onNodeWithText("O que você cadastrar", substring = true).assertExists()
        rule.onNodeWithText("não envia esses dados para serviços externos", substring = true).assertExists()
        rule.onNodeWithText("ainda não há exportação", substring = true).assertExists()
        rule.onNodeWithText("autorização explícita", substring = true).assertExists()
        assertNoExportAction()
    }

    private fun assertSettingsPrivacyCopy() {
        rule.onNodeWithText("Lançamentos, limites e mensagens do assistente ficam salvos neste dispositivo", substring = true).assertExists()
        rule.onNodeWithText("A exportação de dados ainda não está disponível", substring = true).assertExists()
        rule.onNodeWithText("Excluir todos os dados").assertExists()
    }

    private fun assertNoExportAction() {
        val exportActions = rule.onAllNodesWithText("Exportar", substring = true)
            .fetchSemanticsNodes()
            .count { it.config.contains(SemanticsActions.OnClick) }
        assertEquals("Não deve existir ação de exportação", 0, exportActions)
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
        if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Começar a cadastrar").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ajustes").performClick()
    }

    private fun openResetConfirmation() {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Excluir todos os dados", substring = true))
        rule.onNodeWithText("Excluir todos os dados")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private suspend fun restoreDemoState() {
        repository.resetDemoData()
        repository.setDarkTheme(false)
        repository.setReduceMotion(false)
        repository.setShowCents(true)
    }
}
