package com.example.allufinancas

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.DemoSeed
import com.example.allufinancas.data.InsightSeverity
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
class AlertsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: DemoRepository

    @Before fun resetDemoState() = runBlocking {
        repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        withTimeout(15_000) { repository.ready.first { it } }
        restoreDemoState()
    }

    @After fun restoreDemoStateAfterTest() = runBlocking { restoreDemoState() }

    @Test fun alertsShowRuleEvidenceAndArchiveStateSurvivesReopenAndRecreation() {
        enterAlerts()
        val evidence = listOf(
            Triple("budget-alimentacao", InsightSeverity.WARNING, "t10, t03, t02"),
            Triple("duplicate-t04-t05", InsightSeverity.WARNING, "t04, t05"),
            Triple("recurring-2026-09", InsightSeverity.INFO, "t11, t09, t06"),
            Triple("unusual-t02", InsightSeverity.INFO, "t02"),
        )
        scrollAlertsTo("budget-alimentacao")
        assertEquals(2, rule.onAllNodesWithText("Severidade: WARNING", substring = true).fetchSemanticsNodes().size)
        evidence.forEach { (id, severity, sources) ->
            val list = rule.onNode(hasScrollToIndexAction())
            list.performScrollToNode(hasText(id, substring = true))
            assertTrue(rule.onAllNodesWithText("Severidade: ${severity.name}", substring = true).fetchSemanticsNodes().isNotEmpty())
            rule.onNodeWithText(id, substring = true).assertExists()
            rule.onNodeWithText("Lançamentos-base: $sources", substring = true).assertExists()
            assertTrue(rule.onAllNodesWithText("Data editorial do pacote: ${DemoSeed.GENERATED_DATE}", substring = true)
                .fetchSemanticsNodes().isNotEmpty())
        }

        scrollAlertsToTop()
        rule.onNodeWithText("Ativos").performClick()
        archiveNext()
        val archivedId = runBlocking { repository.alerts.first().first { it.dismissed }.id }
        rule.onNodeWithText("Arquivados").performClick()
        scrollAlertsTo(archivedId)
        rule.onNodeWithText(archivedId, substring = true).assertExists()

        rule.activityRule.scenario.recreate()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Reabrir aviso").fetchSemanticsNodes().isNotEmpty() }
        scrollAlertsTo(archivedId)
        rule.onNodeWithText(archivedId, substring = true).assertExists()
        rule.onNodeWithText("Reabrir aviso").performScrollTo().performClick()
        runBlocking {
            withTimeout(10_000) { repository.alerts.first { rows -> rows.any { it.id == archivedId && !it.dismissed } } }
        }

        scrollAlertsToTop()
        rule.onNodeWithText("Ativos").performClick()
        scrollAlertsTo(archivedId)
        rule.onNodeWithText(archivedId, substring = true).assertExists()
        repeat(4) { archiveNext() }
        rule.onNodeWithText("Sem avisos ativos", useUnmergedTree = true).performScrollTo().assertExists()
        rule.onNodeWithText(
            "Todos os avisos gerados para este pacote foram arquivados",
            substring = true,
            useUnmergedTree = true,
        ).assertExists()
        assertEquals(4, runBlocking { repository.alerts.first().count { it.dismissed } })
    }

    private fun enterAlerts() {
        if (rule.onAllNodesWithText("Explorar demonstração").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explorar demonstração").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Avisos").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Data editorial do pacote: ${DemoSeed.GENERATED_DATE}", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Fonte local: exemplos fictícios", substring = true).assertExists()
        rule.onNodeWithText("Materializado neste aparelho:", substring = true).assertExists()
        rule.onNodeWithText("Escopo: Avisos · regras locais · período do pacote ${DemoSeed.MONTH}", substring = true).assertExists()
    }

    private fun archiveNext() {
        val previousDismissed = runBlocking { repository.alerts.first().count { it.dismissed } }
        scrollAlertsTo("Arquivar aviso")
        rule.onAllNodesWithText("Arquivar aviso")[0].performClick()
        runBlocking {
            withTimeout(10_000) {
                repository.alerts.first { rows -> rows.count { it.dismissed } > previousDismissed }
            }
        }
    }

    private fun scrollAlertsTo(text: String) {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text, substring = true))
    }

    private fun scrollAlertsToTop() {
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
    }

    private suspend fun restoreDemoState() {
        repository.resetDemoData()
        repository.setDarkTheme(false)
        repository.setReduceMotion(false)
        repository.setShowCents(true)
    }
}
