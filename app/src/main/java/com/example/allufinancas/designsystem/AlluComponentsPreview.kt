package com.example.allufinancas.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Claro · 360dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
internal fun AlluLight360Preview() = AlluPhonePreview(darkTheme = false)

@Preview(name = "Escuro · 360dp", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun AlluDark360Preview() = AlluPhonePreview(darkTheme = true)

@Preview(name = "Claro · 412dp", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
internal fun AlluLight412Preview() = AlluPhonePreview(darkTheme = false)

@Preview(name = "Escuro · 412dp", showBackground = true, widthDp = 412, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun AlluDark412Preview() = AlluPhonePreview(darkTheme = true)

@Preview(name = "Fonte grande · 360dp", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
internal fun AlluLargeFontPreview() = AlluPhonePreview(darkTheme = false)

@Preview(name = "Estados · claro", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
internal fun AlluStatesLightPreview() = AlluStatePreview(darkTheme = false)

@Preview(name = "Estados · escuro", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun AlluStatesDarkPreview() = AlluStatePreview(darkTheme = true)

@Preview(name = "Confirmação · folha", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
internal fun AlluConfirmationSheetPreview() = AlluPhonePreview(darkTheme = false, showConfirmation = true)

@Preview(name = "Confirmação · movimento reduzido", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
internal fun AlluReducedMotionConfirmationPreview() = AlluPhonePreview(
    darkTheme = false,
    reduceMotion = true,
    showConfirmation = true,
)

@Composable
private fun AlluPhonePreview(
    darkTheme: Boolean,
    reduceMotion: Boolean = false,
    showConfirmation: Boolean = false,
) {
    AlluTheme(darkTheme = darkTheme, reduceMotion = reduceMotion) {
        AlluAppScaffold(
            topBar = { GreetingHeader("Bom dia,", "Ana", "Visão geral da demonstração") },
            bottomBar = {
                AlluBottomNavigation(
                    listOf(
                        AlluNavigationItem("Início", { Text("⌂") }, selected = true, onClick = {}),
                        AlluNavigationItem("Extrato", { Text("≡") }, selected = false, onClick = {}),
                        AlluNavigationItem("Planos", { Text("◎") }, selected = false, onClick = {}),
                        AlluNavigationItem("Avisos", { Text("◇") }, selected = false, onClick = {}),
                        AlluNavigationItem("Assistente", { Text("✦") }, selected = false, onClick = {}),
                    ),
                )
            },
        ) { insets ->
            Column(
                Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                    .padding(AlluTokens.Spacing.page),
                verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.sm),
            ) {
                DemoStrip("Período atual", null)
                AmountRow(
                    "Saldo de demonstração",
                    "R$ 123.456.789,01",
                    supportingText = "Valor fictício · atualizado agora",
                )
                StatusPill("Dentro do limite", AlluStatus.SUCCESS)
                TransactionRow("Mercado Aurora", "Alimentação", "28 set", "− R$ 128,90")
                InsightCard(
                    "Assinatura recorrente",
                    "Uma cobrança fictícia se repete mensalmente.",
                    AlluStatus.WARNING,
                    actionLabel = "Ver movimento",
                )
                ChartContainer("Orçamento", description = "Uso no período") {
                    LinearProgressIndicator(progress = { 0.62f }, modifier = Modifier.fillMaxWidth())
                }
                AssistantBubble(
                    "Posso resumir os dados fictícios deste mês.",
                    isUser = false,
                    supportingText = "Resposta local",
                )
            }
        }
        if (showConfirmation) {
            ConfirmationSheet(
                visible = true,
                title = "Redefinir demonstração?",
                message = "Os dados fictícios editados serão restaurados.",
                confirmLabel = "Redefinir",
                confirmEnabled = false,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@Composable
private fun AlluStatePreview(darkTheme: Boolean) {
    AlluTheme(darkTheme = darkTheme) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AlluTokens.Spacing.page),
            verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.md),
        ) {
            Heading("Carregando")
            LoadingSkeleton()
            Heading("Erro")
            ErrorNotice(
                "Não foi possível carregar",
                "Os dados fictícios permanecem no aparelho.",
                retryLabel = "Tentar novamente",
            )
            Heading("Vazio")
            EmptyState("Sem movimentos", "Não há movimentos neste período.", actionLabel = "Ver outro período")
            AmountRow("Valor longo", "R$ 123.456.789,01")
        }
    }
}
