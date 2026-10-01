package com.example.allufinancas.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.Category
import com.example.allufinancas.data.Transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable internal fun DemoStrip(
    scope: String,
    materializedAt: Instant?,
) {
    val localTimestamp = materializedAt?.atZone(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")))
        ?.plus(" (hora local)") ?: "indisponível"
    Surface(color = AlluTint, shape = RoundedCornerShape(AlluTokens.Radius.chip), modifier = Modifier.fillMaxWidth()) {
        Text(
            "DADOS LOCAIS · Fonte: lançamentos confirmados neste dispositivo · Atualizado: $localTimestamp · Escopo: $scope",
            Modifier.padding(AlluTokens.Spacing.xs), color = AlluNavy, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable internal fun Panel(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AlluTokens.Radius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = AlluTokens.Elevation.card),
    ) {
        Column(Modifier.padding(AlluTokens.Spacing.md)) { content() }
    }
}

@Composable internal fun Heading(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable internal fun TransactionLine(item: Transaction, categories: List<Category>, showCents: Boolean, onClick: (() -> Unit)? = null) =
    TransactionRow(
        merchant = item.merchant,
        category = categories.firstOrNull { it.id == item.categoryId }?.name ?: "Sem categoria",
        date = item.date,
        amount = brl(item.amountCents, showCents),
        positiveAmount = item.amountCents >= 0,
        modifier = Modifier.padding(vertical = AlluTokens.Spacing.xs),
        onClick = onClick,
    )

@Composable
internal fun AlluAppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(modifier = modifier, topBar = topBar, bottomBar = bottomBar, content = content)
}

@Composable
internal fun GreetingHeader(
    greeting: String,
    name: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(name, style = MaterialTheme.typography.headlineMedium)
        supportingText?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun AmountRow(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    amountDescription: String = "$label: $amount",
) {
    Row(
        modifier.fillMaxWidth().defaultMinSize(minHeight = AlluTokens.Spacing.touchTarget)
            .semantics(mergeDescendants = true) { contentDescription = amountDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            supportingText?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        val reduceMotion = LocalAlluReduceMotion.current
        Crossfade(
            targetState = amount,
            animationSpec = tween(
                durationMillis = if (reduceMotion) AlluTokens.Motion.reducedMillis else AlluTokens.Motion.contentMillis,
                easing = AlluTokens.Motion.enterEasing,
            ),
            label = "AmountRow value",
        ) { currentAmount ->
            Text(
                currentAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
internal fun AlluContentEntrance(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val reduceMotion = LocalAlluReduceMotion.current
    val duration = if (reduceMotion) AlluTokens.Motion.reducedMillis else AlluTokens.Motion.contentMillis
    val delay = if (reduceMotion) 0 else delayMillis.coerceIn(0, AlluTokens.Motion.sheetMillis - duration)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) {
            fadeIn(animationSpec = tween(durationMillis = duration, easing = AlluTokens.Motion.enterEasing))
        } else {
            fadeIn(animationSpec = tween(durationMillis = duration, delayMillis = delay, easing = AlluTokens.Motion.enterEasing)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = duration, delayMillis = delay, easing = AlluTokens.Motion.enterEasing),
                    initialOffsetY = { fullHeight -> fullHeight / 40 },
                )
        },
        exit = if (reduceMotion) {
            fadeOut(animationSpec = tween(durationMillis = duration, easing = AlluTokens.Motion.enterEasing))
        } else {
            fadeOut(animationSpec = tween(durationMillis = duration, easing = AlluTokens.Motion.enterEasing)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = duration, easing = AlluTokens.Motion.enterEasing),
                    targetOffsetY = { fullHeight -> fullHeight / 40 },
                )
        },
        label = "ALLU content entrance",
        content = { content() },
    )
}

internal enum class AlluStatus { SUCCESS, WARNING, CRITICAL, NEUTRAL }

@Composable
internal fun StatusPill(label: String, status: AlluStatus, modifier: Modifier = Modifier) {
    val (background, foreground) = when (status) {
        AlluStatus.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        AlluStatus.WARNING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        AlluStatus.CRITICAL -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AlluStatus.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusDescription = when (status) {
        AlluStatus.SUCCESS -> "sucesso"
        AlluStatus.WARNING -> "atenção"
        AlluStatus.CRITICAL -> "crítico"
        AlluStatus.NEUTRAL -> "informativo"
    }
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label, $statusDescription" },
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(AlluTokens.Radius.chip),
    ) {
        Text(label, Modifier.padding(horizontal = AlluTokens.Spacing.xs, vertical = AlluTokens.Spacing.xxs), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun TransactionRow(
    merchant: String,
    category: String,
    date: String,
    amount: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    positiveAmount: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalAlluReduceMotion.current
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = tween(AlluTokens.Motion.pressMillis, easing = AlluTokens.Motion.enterEasing),
        label = "TransactionRow press alpha",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(AlluTokens.Motion.pressMillis, easing = AlluTokens.Motion.enterEasing),
        label = "TransactionRow press scale",
    )
    val rowModifier = if (onClick == null) modifier else modifier.clickable(
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
    Row(
        rowModifier.fillMaxWidth().defaultMinSize(minHeight = AlluTokens.Spacing.touchTarget)
            .alpha(if (enabled) 1f else 0.54f)
            .graphicsLayer { alpha = pressAlpha; scaleX = pressScale; scaleY = pressScale }
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(AlluTokens.IconSize.avatar).background(AlluTint, CircleShape).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Text(merchant.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = AlluNavy)
        }
        Spacer(Modifier.width(AlluTokens.Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(merchant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$category · $date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, color = if (positiveAmount) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun InsightCard(
    title: String,
    description: String,
    status: AlluStatus = AlluStatus.NEUTRAL,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Panel {
        Column(modifier.semantics(mergeDescendants = true) {}) {
            StatusPill(label = status.name.lowercase().replaceFirstChar { it.uppercase() }, status = status)
            Spacer(Modifier.height(AlluTokens.Spacing.xs))
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null) TextButton(onClick = onAction, modifier = Modifier.defaultMinSize(minHeight = AlluTokens.Spacing.touchTarget)) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun ChartContainer(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Panel {
        Column(modifier.fillMaxWidth()) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(AlluTokens.Spacing.sm))
            content()
        }
    }
}

@Composable
internal fun AssistantBubble(message: String, isUser: Boolean, modifier: Modifier = Modifier, supportingText: String? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.widthIn(max = AlluTokens.Size.assistantBubbleMaxWidth).semantics(mergeDescendants = true) {},
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(AlluTokens.Radius.card),
        ) {
            Column(Modifier.padding(AlluTokens.Spacing.md)) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
                supportingText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoadingSkeleton(modifier: Modifier = Modifier, lines: Int = 3) {
    Column(
        modifier.semantics { contentDescription = "Carregando conteúdo" },
        verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.xs),
    ) {
        repeat(lines.coerceAtLeast(1)) { index ->
            Box(
                Modifier.fillMaxWidth(if (index == lines - 1) 0.62f else 1f)
                    .height(if (index == 0) AlluTokens.Size.skeletonTitleHeight else AlluTokens.Size.skeletonLineHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(AlluTokens.Radius.chip)),
            )
        }
    }
}

@Composable
internal fun ErrorNotice(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String? = null,
    onRetry: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(AlluTokens.Radius.card)) {
        Column(Modifier.padding(AlluTokens.Spacing.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            if (retryLabel != null) TextButton(onClick = onRetry, modifier = Modifier.defaultMinSize(minHeight = AlluTokens.Spacing.touchTarget)) { Text(retryLabel) }
        }
    }
}

@Composable
internal fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(AlluTokens.Spacing.lg).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.xs),
    ) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null) TextButton(onClick = onAction, modifier = Modifier.defaultMinSize(minHeight = AlluTokens.Spacing.touchTarget)) { Text(actionLabel) }
    }
}

internal data class AlluNavigationItem(val label: String, val icon: @Composable () -> Unit, val selected: Boolean, val onClick: () -> Unit)

@Composable
internal fun AlluBottomNavigation(items: List<AlluNavigationItem>, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = item.icon,
                label = { Text(item.label) },
                modifier = Modifier.semantics { contentDescription = item.label },
            )
        }
    }
}

@Composable
internal fun ConfirmationSheet(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String = "Cancelar",
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (LocalAlluReduceMotion.current) {
        if (visible) AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { onConfirm(); onDismiss() }, enabled = confirmEnabled) { Text(confirmLabel) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
            )
        return
    }
    val sheetVisibility = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { sheetVisibility.targetState = visible }
    if (visible || sheetVisibility.currentState) Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f))
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .semantics { contentDescription = "Fechar confirmação" },
            )
            AnimatedVisibility(
                visibleState = sheetVisibility,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(AlluTokens.Motion.sheetMillis, easing = AlluTokens.Motion.enterEasing)) +
                    slideInVertically(tween(AlluTokens.Motion.sheetMillis, easing = AlluTokens.Motion.enterEasing)) { it },
                exit = fadeOut(tween(AlluTokens.Motion.contentMillis, easing = AlluTokens.Motion.enterEasing)) +
                    slideOutVertically(tween(AlluTokens.Motion.contentMillis, easing = AlluTokens.Motion.enterEasing)) { it },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = AlluTokens.Radius.sheet, topEnd = AlluTokens.Radius.sheet),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = AlluTokens.Spacing.page).padding(bottom = AlluTokens.Spacing.xl)
                            .semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.sm),
                    ) {
                        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        Text(message, style = MaterialTheme.typography.bodyLarge)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = AlluTokens.Spacing.touchTarget)) { Text(cancelLabel) }
                            Button(
                                enabled = confirmEnabled,
                                onClick = { onConfirm(); onDismiss() },
                                modifier = Modifier.defaultMinSize(minHeight = AlluTokens.Spacing.touchTarget),
                            ) { Text(confirmLabel) }
                        }
                    }
                }
            }
        }
    }
}
