package com.example.allufinancas.feature.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.allufinancas.R
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*
import java.time.Instant
import kotlin.math.roundToInt

@Composable internal fun Home(
    accounts: List<AccountSummary>, transactions: List<Transaction>, categories: List<Category>,
    budgets: List<Budget>, alerts: List<Alert>, goals: List<Goal>, settings: DisplayPreferences,
    materializedAt: Instant?,
    navigate: (Page) -> Unit,
    onOpenTransaction: (String) -> Unit,
) {
    val months = transactions.map { it.date.take(7) }.distinct().sortedDescending()
    var savedMonth by rememberSaveable { mutableStateOf("") }
    val month = savedMonth.takeIf { it in months } ?: months.firstOrNull().orEmpty()
    val projection = DemoAnalysis.homeProjection(accounts, transactions, categories, budgets, alerts, goals, month)
    val observedSummary = financialHealthSummary(transactions, categories, budgets, month)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            val monthScope = month.ifEmpty { "mês indisponível" }
            DemoStrip(
                "Início · saldos snapshot + entradas/saídas $monthScope + limites/metas",
                materializedAt,
            )
        }
        item { GreetingHeader("Visão geral", "Seu panorama", "Saldos e gastos fictícios deste período.") }
        item {
            Surface(color = AlluNavy, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.allu_mark_white), null, Modifier.size(22.dp, 30.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("ALLU FINANÇAS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(25.dp))
                    Text("Saldo das contas · snapshot", color = AlluTint, fontSize = 13.sp)
                    Text(brl(accounts.sumOf { it.balanceCents }, settings.showCents), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Valor atual do pacote fictício; não representa um histórico mensal.", color = AlluTint, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Row {
                        Column(Modifier.weight(1f)) { Text("Entradas no período", color = AlluTint, fontSize = 12.sp); Text(brl(projection.monthSummary.incomeCents, settings.showCents), color = Color.White, fontWeight = FontWeight.SemiBold) }
                        Column(Modifier.weight(1f)) { Text("Saídas no período", color = AlluTint, fontSize = 12.sp); Text(brl(projection.monthSummary.expenseCents, settings.showCents), color = Color.White, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
        if (months.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                months.take(3).forEach { choice ->
                    OutlinedButton(
                        onClick = { savedMonth = choice },
                        modifier = Modifier.weight(1f).semantics { selected = choice == month },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (choice == month) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            contentColor = if (choice == month) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(choice.takeLast(2) + "/" + choice.take(4), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
        item {
            ChartContainer("Contas de exemplo", description = "Saldos fictícios armazenados neste aparelho.") {
                if (accounts.isEmpty()) EmptyState("Ainda preparando", "Os dados locais serão exibidos em instantes.")
                accounts.forEachIndexed { index, account ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 9.dp))
                    AmountRow(account.name, brl(account.balanceCents, settings.showCents))
                }
            }
        }
        item {
            ChartContainer("Despesas por categoria", description = "Valores e proporção das despesas do mês selecionado.") {
                if (projection.expensesByCategory.isEmpty() || projection.monthSummary.expenseCents == 0L) {
                    EmptyState("Sem despesas neste período", "Não há despesas fictícias para este mês.")
                } else projection.expensesByCategory.forEach { category ->
                    val share = (category.spentCents.toFloat() / projection.monthSummary.expenseCents).coerceIn(0f, 1f)
                    val percentage = (share * 100).roundToInt()
                    val amount = brl(category.spentCents, settings.showCents)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AmountRow(
                            category.categoryName,
                            amount,
                            supportingText = "$percentage% das despesas",
                            amountDescription = "${category.categoryName}: $amount, $percentage% das despesas",
                        )
                        LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            Heading("Seu panorama", "Ver planos") { navigate(Page.BUDGETS) }
            Panel {
                val budget = budgets.firstOrNull()
                if (budget == null) Text("Nenhum limite neste exemplo.")
                else {
                    val spent = projection.expensesByCategory.firstOrNull { it.categoryId == budget.categoryId }?.spentCents ?: 0L
                    Text(categories.firstOrNull { it.id == budget.categoryId }?.name ?: "Categoria", fontWeight = FontWeight.Bold)
                    Text(brl(spent, settings.showCents) + " de " + brl(budget.limitCents, settings.showCents), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (spent > budget.limitCents) Text(
                        "Acima do limite por " + brl(spent - budget.limitCents, settings.showCents),
                        color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { (spent.toFloat() / budget.limitCents.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialHealthSummarySection(
                    observedSummary,
                    settings.showCents,
                    onOpenTransaction,
                    onOpenPlans = { navigate(Page.BUDGETS) },
                )
            }
        }
        item {
            Heading("Insights do período", "Ver avisos") { navigate(Page.ALERTS) }
        }
        val activeInsights = projection.insights.filterNot { it.dismissed }.take(3)
        if (activeInsights.isEmpty()) item {
            EmptyState("Sem destaques neste período", "Os insights foram recalculados para o mês selecionado.")
        } else items(activeInsights, key = { it.insight.id }) { homeInsight ->
            val status = when (homeInsight.insight.severity) {
                InsightSeverity.CRITICAL -> AlluStatus.CRITICAL
                InsightSeverity.WARNING -> AlluStatus.WARNING
                InsightSeverity.INFO -> AlluStatus.NEUTRAL
            }
            InsightCard(homeInsight.insight.title, homeInsight.insight.explanation, status)
        }
        item {
            Heading("Movimentos recentes", "Ver extrato") { navigate(Page.TRANSACTIONS) }
            Panel {
                if (projection.recentTransactions.isEmpty()) EmptyState("Nenhum movimento neste período", "Selecione um mês com dados fictícios.")
                projection.recentTransactions.take(3).forEach { TransactionLine(it, categories, settings.showCents) }
            }
        }
        if (goals.isNotEmpty()) item {
            Heading("Meta em andamento")
            Panel {
                val goal = goals.first()
                Text(goal.name, fontWeight = FontWeight.Bold)
                Text("Snapshot atual; o valor não varia por mês.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(brl(goal.savedCents, settings.showCents) + " de " + brl(goal.targetCents, settings.showCents), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { (goal.savedCents.toFloat() / goal.targetCents.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
