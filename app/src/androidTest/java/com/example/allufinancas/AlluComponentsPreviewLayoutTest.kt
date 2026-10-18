package com.example.allufinancas

import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.allufinancas.designsystem.AlluPhonePreview
import com.example.allufinancas.designsystem.AlluPreviewViewportTestTag
import com.example.allufinancas.designsystem.AlluStatePreview
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlluComponentsPreviewLayoutTest {
  @get:Rule val rule = createAndroidComposeRule<TransactionFallbackTestActivity>()

  @Test
  fun phonePreview360LightKeepsAmountAndNavigationInsideViewport() =
      assertPhonePreviewFits(widthDp = 360, darkTheme = false, fontScale = 1f)

  @Test
  fun phonePreview360DarkKeepsAmountAndNavigationInsideViewport() =
      assertPhonePreviewFits(widthDp = 360, darkTheme = true, fontScale = 1f)

  @Test
  fun phonePreview412LightKeepsAmountAndNavigationInsideViewport() =
      assertPhonePreviewFits(widthDp = 412, darkTheme = false, fontScale = 1f)

  @Test
  fun phonePreview412DarkKeepsAmountAndNavigationInsideViewport() =
      assertPhonePreviewFits(widthDp = 412, darkTheme = true, fontScale = 1f)

  @Test
  fun phonePreview360LargeFontKeepsAmountAndNavigationInsideViewport() =
      assertPhonePreviewFits(widthDp = 360, darkTheme = false, fontScale = 1.5f)

  @Test
  fun phonePreview412DarkLargeFontKeepsAmountAndNavigationInsideViewport() =
      assertPhonePreviewFits(widthDp = 412, darkTheme = true, fontScale = 1.5f)

  @Test
  fun statePreview360LightKeepsLoadingErrorEmptyAndLongBrlReadable() =
      assertStatePreviewFits(widthDp = 360, darkTheme = false, fontScale = 1f)

  @Test
  fun statePreview360DarkKeepsLoadingErrorEmptyAndLongBrlReadable() =
      assertStatePreviewFits(widthDp = 360, darkTheme = true, fontScale = 1f)

  @Test
  fun statePreview412LightKeepsLoadingErrorEmptyAndLongBrlReadable() =
      assertStatePreviewFits(widthDp = 412, darkTheme = false, fontScale = 1f)

  @Test
  fun statePreview412DarkKeepsLoadingErrorEmptyAndLongBrlReadable() =
      assertStatePreviewFits(widthDp = 412, darkTheme = true, fontScale = 1f)

  @Test
  fun statePreview360LargeFontKeepsLoadingErrorEmptyAndLongBrlReadable() =
      assertStatePreviewFits(widthDp = 360, darkTheme = false, fontScale = 1.5f)

  @Test
  fun statePreview412DarkLargeFontKeepsLoadingErrorEmptyAndLongBrlReadable() =
      assertStatePreviewFits(widthDp = 412, darkTheme = true, fontScale = 1.5f)

  private fun assertPhonePreviewFits(widthDp: Int, darkTheme: Boolean, fontScale: Float) {
    rule.setContent {
      DeviceConfigurationOverride(previewConfiguration(widthDp, darkTheme, fontScale)) {
        AlluPhonePreview(darkTheme = darkTheme)
      }
    }

    rule
        .onNodeWithContentDescription("Saldo de demonstração: R$ 123.456.789,01")
        .also(::assertInsideViewport)

    listOf("Início", "Extrato", "Planos", "Avisos").forEach { label ->
      val destination = rule.onNodeWithContentDescription(label).assertHasClickAction()
      assertInsideViewport(destination)
      assertInsideViewport(rule.onNodeWithText(label, useUnmergedTree = true))
    }
  }

  private fun assertStatePreviewFits(widthDp: Int, darkTheme: Boolean, fontScale: Float) {
    rule.setContent {
      DeviceConfigurationOverride(previewConfiguration(widthDp, darkTheme, fontScale)) {
        AlluStatePreview(darkTheme = darkTheme)
      }
    }

    rule
        .onNodeWithContentDescription("Carregando conteúdo")
        .performScrollTo()
        .also(::assertInsideViewport)
    assertVisibleText("Carregando")

    assertVisibleText("Não foi possível carregar")
    assertVisibleText("Os dados fictícios permanecem no aparelho.")
    val retry = rule.onNodeWithText("Tentar novamente").performScrollTo().assertHasClickAction()
    assertInsideViewport(retry)
    assertInsideViewport(retry)

    assertVisibleText("Sem movimentos")
    assertVisibleText("Não há movimentos neste período.")
    val emptyAction =
        rule.onNodeWithText("Ver outro período").performScrollTo().assertHasClickAction()
    assertInsideViewport(emptyAction)
    assertInsideViewport(emptyAction)

    rule
        .onNodeWithContentDescription("Valor longo: R$ 123.456.789,01")
        .performScrollTo()
        .also(::assertInsideViewport)
  }

  private fun previewConfiguration(
      widthDp: Int,
      darkTheme: Boolean,
      fontScale: Float,
  ) =
      DeviceConfigurationOverride.ForcedSize(
              DpSize(widthDp.dp, if (widthDp == 360) 800.dp else 900.dp)
          )
          .then(DeviceConfigurationOverride.FontScale(fontScale))
          .then(DeviceConfigurationOverride.DarkMode(darkTheme))

  private fun assertInsideViewport(node: SemanticsNodeInteraction) {
    val viewport = rule.onNodeWithTag(AlluPreviewViewportTestTag).fetchSemanticsNode().boundsInRoot
    val bounds = node.fetchSemanticsNode().boundsInRoot
    val tolerancePx = 1f

    assertTrue(
        "Node must have visible area: $bounds",
        bounds.width > 0f && bounds.height > 0f,
    )
    assertTrue(
        "Node starts outside the viewport: $bounds vs $viewport",
        bounds.left >= viewport.left - tolerancePx,
    )
    assertTrue(
        "Node ends outside the viewport: $bounds vs $viewport",
        bounds.right <= viewport.right + tolerancePx,
    )
    assertTrue(
        "Node starts above the viewport: $bounds vs $viewport",
        bounds.top >= viewport.top - tolerancePx,
    )
    assertTrue(
        "Node ends below the viewport: $bounds vs $viewport",
        bounds.bottom <= viewport.bottom + tolerancePx,
    )
  }

  private fun assertVisibleText(text: String) {
    val node = rule.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
    assertInsideViewport(node)
  }
}
