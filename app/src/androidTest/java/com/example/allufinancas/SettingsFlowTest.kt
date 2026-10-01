package com.example.allufinancas

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.ImportNoticeKind
import com.example.allufinancas.data.NotificationImportCandidate
import com.example.allufinancas.data.NotificationImportSource
import com.example.allufinancas.notification.NotificationImportCapture
import com.example.allufinancas.notification.NotificationImportFingerprints
import java.time.Instant
import java.time.LocalDate
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
        assertTrue(
            rule.onAllNodesWithText("rascunhos importados", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        rule.onNodeWithText("As categorias padrão e preferências de exibição serão preservadas", substring = true, useUnmergedTree = true).assertExists()
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

    @Test fun sourceConfigurationSavesOnlyACompleteFourDigitFilter() {
        runBlocking { repository.enableImportSource(NotificationImportSource.RICO) }
        enterSettings()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithContentDescription("Filtro por quatro dígitos finais").fetchSemanticsNodes().isNotEmpty()
        }
        val filter = rule.onNodeWithContentDescription("Filtro por quatro dígitos finais")
        filter.performTextReplacement("12")
        rule.onNodeWithText("Salvar filtro").assertIsNotEnabled()

        filter.performTextReplacement("1234")
        rule.onNodeWithText("Salvar filtro").performClick()
        runBlocking {
            withTimeout(10_000) { repository.importFeedSuffix.first { it == "1234" } }
        }
        rule.onNodeWithText("Filtro ativo: final •••• 1234").assertExists()
    }

    @Test fun pendingDraftsRemainUsableAfterSourceDisconnect() {
        val date = LocalDate.now().toString()
        runBlocking {
            repository.enableImportSource(NotificationImportSource.RICO)
            assertTrue(
                NotificationImportCapture(context).capture(
                    NotificationImportCandidate(
                        source = NotificationImportSource.RICO,
                        sourceEventHash = NotificationImportFingerprints.sourceEvent(
                            NotificationImportSource.RICO,
                            "kept-draft",
                            "Compra aprovada R$ 8,89",
                            Instant.parse("2026-09-30T14:00:00Z"),
                        ),
                        fingerprintHash = NotificationImportFingerprints.notice(
                            NotificationImportSource.RICO,
                            ImportNoticeKind.EXPENSE,
                            "Estabelecimento a confirmar",
                            -889,
                            date,
                        ),
                        merchant = "Estabelecimento a confirmar",
                        description = "Compra aprovada pela Rico",
                        amountCents = -889,
                        date = date,
                        kind = ImportNoticeKind.EXPENSE,
                        observedAt = Instant.now().toString(),
                    ),
                ),
            )
            repository.disableImportSource()
            withTimeout(10_000) { repository.importNotices.first { it.size == 1 } }
        }

        enterDashboard()
        rule.onNodeWithContentDescription("Extrato").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Importações pendentes (1)").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Importações pendentes (1)").performClick()
        rule.onNodeWithText("Captura pausada").assertExists()
        rule.onNodeWithText("Saída a confirmar").assertExists()
        rule.onNodeWithText("Confirmar saída").performClick()
        rule.onNodeWithText("A origem é Rico", substring = true).assertExists()
        rule.onNodeWithText("Confirmar no extrato").assertExists()
    }

    private fun assertPrivacyEducationVisible() {
        rule.onNodeWithText("Privacidade e conexões").assertExists()
        rule.onNodeWithText("não usa API bancária", substring = true).assertExists()
        rule.onNodeWithText("Importação opcional da Rico", substring = true).assertExists()
        rule.onNodeWithText("permissão do sistema é ampla", substring = true).assertExists()
        rule.onNodeWithText("Nada altera saldo", substring = true).assertExists()
        rule.onNodeWithText("ainda não há exportação", substring = true).assertExists()
        rule.onNodeWithText("Desconectar a Rico", substring = true).assertExists()
        assertNoExportAction()
    }

    private fun assertSettingsPrivacyCopy() {
        rule.onNodeWithText("A importação opcional da Rico", substring = true).assertExists()
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
        enterDashboard()
        rule.onNodeWithText("Ajustes").performClick()
    }

    private fun enterDashboard() {
        if (rule.onAllNodesWithText("Começar a cadastrar").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Começar a cadastrar").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
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
