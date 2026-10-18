package com.example.allufinancas

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
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

  init {
    welcomePreferences.edit().putBoolean("entered", false).commit()
  }

  @Before
  fun resetDemoState() = runBlocking {
    repository = DemoRepository(context)
    withTimeout(15_000) { repository.ready.first { it } }
    restoreDemoState()
  }

  @After
  fun restoreDemoStateAfterTest() {
    runBlocking {
      restoreDemoState()
      welcomePreferences.edit().putBoolean("entered", previousWelcomeState).commit()
    }
  }

  @Test
  fun privacyEducationIsReachableFromWelcomeAndSettings() {
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Privacidade e dados locais").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Privacidade e dados locais").performClick()
    assertPrivacyEducationVisible()

    rule.onNodeWithText("Voltar").performClick()
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
    rule.waitUntil(15_000) {
      rule.onAllNodesWithText("Saldo líquido dos lançamentos").fetchSemanticsNodes().isNotEmpty()
    }
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Ajustes").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Ajustes").performClick()
    assertSettingsPrivacyCopy()
    assertNoExportAction()
    rule.onNodeWithText("Privacidade e limitações").performClick()
    assertPrivacyEducationVisible()
  }

  @Test
  fun preferencesSurviveRecreationAndLocalDataDeletionCanBeCancelledOrConfirmed() {
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
    ensureSettingsVisible()
    rule.onNodeWithContentDescription("Modo escuro").assertIsOn()
    rule.onNodeWithContentDescription("Reduzir movimento").assertIsOn()
    rule.onNodeWithContentDescription("Mostrar centavos").assertIsOff()
    rule.onNodeWithText("Preferência de exibição").assertExists()

    runBlocking {
      repository.updateBudget("alimentacao", 100_000)
    }
    openResetConfirmation()
    rule.onNodeWithText("Cancelar", useUnmergedTree = true).performClick()
    assertTrue(
        rule.onAllNodesWithText("Excluir todos os dados locais?").fetchSemanticsNodes().isEmpty()
    )
    runBlocking {
      assertEquals(
          100_000,
          repository.budgets.first().first { it.categoryId == "alimentacao" }.limitCents,
      )
    }

    openResetConfirmation()
    assertTrue(
        rule
            .onAllNodesWithText("rascunhos importados", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty(),
    )
    rule
        .onNodeWithText(
            "As categorias padrão e preferências de exibição serão preservadas",
            substring = true,
            useUnmergedTree = true,
        )
        .assertExists()
    rule.onNodeWithText("Excluir dados", useUnmergedTree = true).performClick()
    runBlocking {
      withTimeout(10_000) {
        repository.budgets.first { it.isEmpty() }
      }
      withTimeout(10_000) { repository.transactions.first { it.isEmpty() } }
      withTimeout(10_000) { repository.categories.first { it.isNotEmpty() } }
      val preferences = repository.preferences.first()
      assertTrue(preferences.darkTheme && preferences.reduceMotion && !preferences.showCents)
    }
    assertTrue(welcomePreferences.getBoolean("entered", false))
    rule.waitUntil(10_000) {
      rule
          .onAllNodesWithText("Excluir todos os dados locais?", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Modo escuro"))
    rule.onNodeWithContentDescription("Modo escuro").assertIsOn()
    rule.onNodeWithContentDescription("Reduzir movimento").assertIsOn()
    rule.onNodeWithContentDescription("Mostrar centavos").assertIsOff()
  }

  @Test
  fun sourceConfigurationEnablesSaveOnlyForFourDigitFilter() {
    val dataSource = (rule.activity.application as AlluApplication).demoDataSource
    runBlocking { dataSource.enableImportSource(NotificationImportSource.RICO) }
    enterSettings()
    rule.waitUntil(10_000) {
      rule
          .onAllNodesWithContentDescription("Filtro por quatro dígitos finais")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    val filter = rule.onNodeWithContentDescription("Filtro por quatro dígitos finais")
    filter.performTextReplacement("12")
    rule.onNodeWithText("Salvar filtro").assertIsNotEnabled()

    filter.performTextReplacement("1234")
    rule.waitForIdle()
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Salvar filtro").fetchSemanticsNodes().any {
        it.config.contains(SemanticsProperties.Disabled).not()
      }
    }
    rule.onNodeWithText("Salvar filtro").assertIsEnabled()
  }

  @Test
  fun pendingDraftsRemainUsableAfterSourceDisconnect() {
    val date = LocalDate.now().toString()
    runBlocking {
      repository.enableImportSource(NotificationImportSource.RICO)
      assertTrue(
          NotificationImportCapture(context)
              .capture(
                  NotificationImportCandidate(
                      source = NotificationImportSource.RICO,
                      sourceEventHash =
                          NotificationImportFingerprints.sourceEvent(
                              NotificationImportSource.RICO,
                              "kept-draft",
                              "Compra aprovada R$ 8,89",
                              Instant.parse("2026-09-30T14:00:00Z"),
                          ),
                      fingerprintHash =
                          NotificationImportFingerprints.notice(
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
    rule.waitUntil(10_000) {
      rule
          .onAllNodesWithText("Revisar importações pendentes (1)")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    rule.onNodeWithText("Revisar importações pendentes (1)").performClick()
    rule.onNodeWithText("Captura desconectada").assertExists()
    rule.onNodeWithText("Saída a confirmar").assertExists()
    rule.onNodeWithText("Confirmar saída").performClick()
    rule.onNodeWithText("A origem é Rico", substring = true).assertExists()
    rule.onNodeWithText("Confirmar no extrato").assertExists()
  }

  private fun assertPrivacyEducationVisible() {
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Privacidade e conexões").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Privacidade e conexões").assertExists()
    rule.onNodeWithText("O app não usa API bancária", substring = true).assertExists()
    rule.onNodeWithText("Importação opcional da Rico", substring = true).assertExists()
    rule.onNodeWithText("permissão do sistema é ampla", substring = true).assertExists()
    rule.onNodeWithText("Nada altera saldo", substring = true).assertExists()
    rule.onNodeWithText("Ainda não há exportação", substring = true).assertExists()
    rule.onNodeWithText("Desconectar a Rico", substring = true).assertExists()
    assertNoExportAction()
  }

  private fun assertSettingsPrivacyCopy() {
    val disconnectedSourceStatus =
        if (ricoIsInstalled()) "Captura desconectada." else "Fonte indisponível:"
    rule
        .onNode(hasScrollToIndexAction())
        .performScrollToNode(hasText(disconnectedSourceStatus, substring = true))
    rule.onNodeWithText(disconnectedSourceStatus, substring = true).assertExists()
    rule
        .onNodeWithText("A exportação de dados ainda não está disponível", substring = true)
        .assertExists()
    rule
        .onNode(hasScrollToIndexAction())
        .performScrollToNode(hasText("Excluir todos os dados", substring = true))
    rule.onNodeWithText("Excluir todos os dados").assertExists()
  }

  private fun ricoIsInstalled(): Boolean =
      runCatching {
            context.packageManager.getApplicationInfo(NotificationImportSource.RICO.packageName, 0)
          }
          .isSuccess

  private fun assertNoExportAction() {
    val exportActions =
        rule.onAllNodesWithText("Exportar", substring = true).fetchSemanticsNodes().count {
          it.config.contains(SemanticsActions.OnClick)
        }
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
    rule.waitUntil(10_000) {
      rule.onAllNodesWithText("Ajustes").fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("Ajustes").performClick()
  }

  private fun ensureSettingsVisible() {
    rule.waitUntil(15_000) {
      rule.onAllNodesWithContentDescription("Modo escuro").fetchSemanticsNodes().isNotEmpty() ||
          rule
              .onAllNodes(hasText("Ajustes") and hasClickAction())
              .fetchSemanticsNodes()
              .isNotEmpty()
    }
    if (rule.onAllNodesWithContentDescription("Modo escuro").fetchSemanticsNodes().isEmpty()) {
      rule.onNode(hasText("Ajustes") and hasClickAction()).performClick()
    }
    rule.waitUntil(15_000) {
      rule.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Modo escuro"))
    rule.waitUntil(15_000) {
      rule.onAllNodesWithContentDescription("Modo escuro").fetchSemanticsNodes().isNotEmpty()
    }
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

  private fun openResetConfirmation() {
    rule
        .onNode(hasScrollToIndexAction())
        .performScrollToNode(hasText("Excluir todos os dados", substring = true))
    rule.onNodeWithText("Excluir todos os dados").performSemanticsAction(SemanticsActions.OnClick) {
      it()
    }
  }

  private suspend fun restoreDemoState() {
    repository.resetDemoData()
    repository.setDarkTheme(false)
    repository.setReduceMotion(false)
    repository.setShowCents(true)
  }
}
