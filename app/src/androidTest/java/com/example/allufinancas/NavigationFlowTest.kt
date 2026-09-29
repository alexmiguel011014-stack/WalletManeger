package com.example.allufinancas

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.DemoSeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: DemoRepository

    @Before fun resetDemoFixture() = runBlocking {
        repository = DemoRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        withTimeout(15_000) { repository.ready.first { it } }
        repository.resetDemoData()
    }

    @Test fun filtersSurviveTabSwitchBackStackAndActivityRecreation() {
        enterDashboard()
        assertProvenance("Início · saldos snapshot + entradas/saídas 2026-09 + limites/metas")
        rule.onNodeWithContentDescription("Extrato").performClick()
        assertProvenance("Todos os movimentos")
        rule.onNodeWithTag("transaction-categories").performScrollToNode(hasText("Compras"))
        rule.onNodeWithText("Compras").performClick()
        rule.onNodeWithText("2026-09").performScrollTo().performClick()
        rule.onNodeWithText("Buscar estabelecimento").performScrollTo().performTextInput("Notebook")
        assertProvenance("Período: 2026-09 · Busca: Notebook · Categoria: Compras")
        rule.onNodeWithText("Loja Exemplo").performScrollTo().assertExists()

        rule.onNodeWithContentDescription("Avisos").performClick()
        assertProvenance("Avisos · regras locais · período do pacote ${DemoSeed.MONTH}")
        rule.onNodeWithContentDescription("Extrato").performClick()
        assertProvenance("Período: 2026-09 · Busca: Notebook · Categoria: Compras")
        rule.onNodeWithText("Notebook").assertExists()
        assertMonthSelected()
        rule.onNodeWithText("Loja Exemplo").assertExists()

        rule.onNodeWithText("Ajustes").performClick()
        rule.onNodeWithText("Privacidade e limitações").performClick()
        rule.onNodeWithText("não se conecta a bancos", substring = true).assertExists()
        pressBack()
        rule.onNodeWithText("Preferência de exibição").assertExists()
        pressBack()
        rule.onNodeWithText("Notebook").assertExists()
        assertMonthSelected()

        rule.activityRule.scenario.recreate()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Buscar estabelecimento").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Notebook").assertExists()
        rule.onNodeWithText("Loja Exemplo").assertExists()
        assertMonthSelected()

        rule.onNodeWithText("Loja Exemplo").performScrollTo().performClick()
        assertProvenance("Detalhe · Loja Exemplo")
        rule.onNodeWithText("Estabelecimento: Loja Exemplo").assertExists()
        pressBack()
        rule.onNodeWithText("Notebook").assertExists()
        rule.onNodeWithText("Loja Exemplo").assertExists()
    }

    @Test fun navigationSwitchesBetweenBottomBarAndRailAtWideWidth() {
        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        enterDashboard()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("bottom-navigation").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("navigation-rail").assertDoesNotExist()

        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("navigation-rail").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("bottom-navigation").assertDoesNotExist()
        rule.onNodeWithContentDescription("Avisos").performClick()
        assertProvenance("Avisos · regras locais · período do pacote ${DemoSeed.MONTH}")
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Ativos").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ativos").assertExists()
        rule.onNodeWithContentDescription("Início").performClick()
        assertProvenance("Início · saldos snapshot + entradas/saídas 2026-09 + limites/metas")
        rule.onNodeWithContentDescription("Planos").performClick()
        assertProvenance("Limites e metas · Setembro/2026")
    }

    private fun enterDashboard() {
        if (rule.onAllNodesWithText("Explorar demonstração").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explorar demonstração").performClick()
        }
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Visão geral").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun pressBack() {
        Espresso.pressBack()
    }

    private fun assertMonthSelected() {
        assertTrue(rule.onAllNodesWithText("2026-09", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    private fun assertProvenance(scope: String) {
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Escopo: $scope", substring = true).fetchSemanticsNodes().isNotEmpty() &&
                rule.onAllNodesWithText("(hora local)", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Fonte local: exemplos fictícios", substring = true).assertExists()
        assertTrue(
            rule.onAllNodesWithText("Data editorial do pacote: ${DemoSeed.GENERATED_DATE}", substring = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        rule.onNodeWithText("Materializado neste aparelho:", substring = true).assertExists()
    }
}
