package com.example.allufinancas.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** wallet Manager layout, shape, icon, and motion values. Spacing follows a 4 dp grid. */
internal object AlluTokens {
  object Spacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val page: Dp = 20.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val touchTarget: Dp = 48.dp
  }

  object Radius {
    val chip: Dp = 8.dp
    val control: Dp = 10.dp
    val card: Dp = 14.dp
    val hero: Dp = 18.dp
    val sheet: Dp = 24.dp
  }

  object Elevation {
    val card: Dp = 2.dp
    val raised: Dp = 4.dp
  }

  object IconSize {
    val small: Dp = 16.dp
    val standard: Dp = 20.dp
    val large: Dp = 24.dp
    val brand: Dp = 30.dp
    val avatar: Dp = 40.dp
  }

  object Size {
    val assistantBubbleMaxWidth: Dp = 320.dp
    val skeletonTitleHeight: Dp = 20.dp
    val skeletonLineHeight: Dp = 14.dp
  }

  object Motion {
    const val pressMillis = 100
    const val reducedMillis = 100
    const val contentMillis = 180
    const val sheetMillis = 240
    val enterEasing: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
  }
}

internal object AlluSemanticColors {
  val success
    get() = AlluSuccess

  val warning
    get() = AlluWarning

  val critical
    get() = AlluCritical

  val neutral
    get() = AlluNeutral
}
