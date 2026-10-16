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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.allufinancas.R
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
internal fun Home(
    transactions: List<Transaction>,
    categories: List<Category>,
    budgets: List<Budget>,
    alerts: List<Alert>,
    goals: List<Goal>,
    settings: DisplayPreferences,
    pendingImportCount: Int,
    importSourceEnabled: Boolean,
    navigate: (Page) -> Unit,
    onAddTransaction: () -> Unit,
    onOpenImports: () -> Unit,
    onOpenTransaction: (String) -> Unit,
) {
  if (transactions.isEmpty() && budgets.isEmpty() && goals.isEmpty()) {
    FirstSessionHome(
        pendingImportCount = pendingImportCount,
        importSourceEnabled = importSourceEnabled,
        onAddTransaction = onAddTransaction,
        onOpenImports = onOpenImports,
    )
    return
  }
  val currentMonth = YearMonth.now().toString()
  val months = (transactions.map { it.date.take(7) } + currentMonth).distinct().sortedDescending()
  var savedMonth by rememberSaveable { mutableStateOf("") }
  val month = savedMonth.takeIf { it in months } ?: currentMonth
  val projection =
      DemoAnalysis.homeProjection(
          emptyList(),
          transactions,
          categories,
          budgets,
          alerts,
          goals,
          month,
      )
  val observedSummary = financialHealthSummary(transactions, categories, budgets, month)
  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      GreetingHeader(
          "Resumo do período",
          "Seu panorama",
          "Seus lançamentos e limites cadastrados neste dispositivo.",
      )
    }
    item {
      Surface(
          color = AlluNavy,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
      ) {
        Column(Modifier.padding(22.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.wallet_mark), null, Modifier.size(22.dp, 30.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "wallet Manager",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
          }
          Spacer(Modifier.height(25.dp))
          Text("Saldo líquido dos lançamentos", color = AlluTint, fontSize = 13.sp)
          Text(
              brl(DemoAnalysis.balanceCents(transactions), settings.showCents),
              color = Color.White,
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
          )
          Text(
              "Soma acumulada das entradas e saídas cadastradas.",
              color = AlluTint,
              fontSize = 12.sp,
          )
          Spacer(Modifier.height(20.dp))
          Row {
            Column(Modifier.weight(1f)) {
              Text("Entradas no período", color = AlluTint, fontSize = 12.sp)
              Text(
                  brl(projection.monthSummary.incomeCents, settings.showCents),
                  color = Color.White,
                  fontWeight = FontWeight.SemiBold,
              )
            }
            Column(Modifier.weight(1f)) {
              Text("Saídas no período", color = AlluTint, fontSize = 12.sp)
              Text(
                  brl(projection.monthSummary.expenseCents, settings.showCents),
                  color = Color.White,
                  fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }
    }
    if (importSourceEnabled || pendingImportCount > 0)
        item {
          OutlinedButton(
              onClick = onOpenImports,
              modifier = Modifier.fillMaxWidth().testTag("home-imports-shortcut"),
          ) {
            Text(
                if (pendingImportCount > 0) "Revisar importações pendentes ($pendingImportCount)"
                else "Importações pendentes"
            )
          }
        }
    if (months.isNotEmpty())
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            months.take(3).forEach { choice ->
              OutlinedButton(
                  onClick = { savedMonth = choice },
                  modifier = Modifier.weight(1f).semantics { selected = choice == month },
                  colors =
                      ButtonDefaults.outlinedButtonColors(
                          containerColor =
                              if (choice == month) MaterialTheme.colorScheme.secondaryContainer
                              else MaterialTheme.colorScheme.surface,
                          contentColor =
                              if (choice == month) MaterialTheme.colorScheme.onSecondaryContainer
                              else MaterialTheme.colorScheme.primary,
                      ),
              ) {
                Text(choice.takeLast(2) + "/" + choice.take(4), fontSize = 11.sp, maxLines = 1)
              }
            }
          }
        }
    item {
      ChartContainer(
          "Despesas por categoria",
          description = "Valores e proporção das despesas do mês selecionado.",
      ) {
        if (projection.expensesByCategory.isEmpty() || projection.monthSummary.expenseCents == 0L) {
          EmptyState(
              "Sem despesas neste período",
              "Cadastre uma saída no extrato para acompanhar seus gastos.",
              actionLabel = "Ir ao extrato",
              onAction = { navigate(Page.TRANSACTIONS) },
          )
        } else
            projection.expensesByCategory.forEach { category ->
              val share =
                  (category.spentCents.toFloat() / projection.monthSummary.expenseCents).coerceIn(
                      0f,
                      1f,
                  )
              val percentage = (share * 100).roundToInt()
              val amount = brl(category.spentCents, settings.showCents)
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AmountRow(
                    category.categoryName,
                    amount,
                    supportingText = "$percentage% das despesas",
                    amountDescription =
                        "${category.categoryName}: $amount, $percentage% das despesas",
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
        if (budget == null)
            EmptyState(
                "Nenhum limite cadastrado",
                "Crie um limite por categoria em Planos.",
                actionLabel = "Criar limite",
                onAction = { navigate(Page.BUDGETS) },
            )
        else {
          val spent = DemoAnalysis.categorySpentCents(transactions, budget.categoryId, currentMonth)
          Text(
              categories.firstOrNull { it.id == budget.categoryId }?.name ?: "Categoria",
              fontWeight = FontWeight.Bold,
          )
          Text(
              brl(spent, settings.showCents) + " de " + brl(budget.limitCents, settings.showCents),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (spent > budget.limitCents)
              Text(
                  "Acima do limite por " + brl(spent - budget.limitCents, settings.showCents),
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.SemiBold,
              )
          Spacer(Modifier.height(10.dp))
          LinearProgressIndicator(
              progress = {
                (spent.toFloat() / budget.limitCents.coerceAtLeast(1)).coerceIn(0f, 1f)
              },
              modifier = Modifier.fillMaxWidth(),
          )
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
    if (activeInsights.isEmpty())
        item {
          EmptyState(
              "Sem destaques neste período",
              "Os insights foram recalculados para o mês selecionado.",
          )
        }
    else
        items(activeInsights, key = { it.insight.id }) { homeInsight ->
          val status =
              when (homeInsight.insight.severity) {
                InsightSeverity.CRITICAL -> AlluStatus.CRITICAL
                InsightSeverity.WARNING -> AlluStatus.WARNING
                InsightSeverity.INFO -> AlluStatus.NEUTRAL
              }
          InsightCard(homeInsight.insight.title, homeInsight.insight.explanation, status)
        }
    item {
      Heading("Movimentos recentes", "Ver extrato") { navigate(Page.TRANSACTIONS) }
      Panel {
        if (projection.recentTransactions.isEmpty())
            EmptyState(
                "Nenhum lançamento neste período",
                "Adicione sua primeira entrada ou saída para ver o extrato aqui.",
                actionLabel = "Ir ao extrato",
                onAction = { navigate(Page.TRANSACTIONS) },
            )
        projection.recentTransactions.take(3).forEach {
          TransactionLine(it, categories, settings.showCents)
        }
      }
    }
    if (goals.isNotEmpty())
        item {
          Heading("Meta em andamento")
          Panel {
            val goal = goals.first()
            Text(goal.name, fontWeight = FontWeight.Bold)
            Text(
                "Snapshot atual; o valor não varia por mês.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                brl(goal.savedCents, settings.showCents) +
                    " de " +
                    brl(goal.targetCents, settings.showCents),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = {
                  (goal.savedCents.toFloat() / goal.targetCents.coerceAtLeast(1)).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
            )
          }
        }
  }
}

@Composable
private fun FirstSessionHome(
    pendingImportCount: Int,
    importSourceEnabled: Boolean,
    onAddTransaction: () -> Unit,
    onOpenImports: () -> Unit,
) {
  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding =
          androidx.compose.foundation.layout.PaddingValues(
              horizontal = 20.dp,
              vertical = 14.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
          color = AlluNavy,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
      ) {
        Column(Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.wallet_mark), null, Modifier.size(22.dp, 30.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "wallet Manager",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
          }
          Spacer(Modifier.height(14.dp))
          Text("Saldo líquido dos lançamentos", color = AlluTint, fontSize = 13.sp)
          Text("—", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
          Text("Ainda não há lançamentos salvos.", color = AlluTint, fontSize = 13.sp)
        }
      }
    }
    item {
      Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("first-session-local-disclosure"),
      ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text("Dados ficam neste dispositivo", fontWeight = FontWeight.SemiBold)
          Text(
              "Sem conta ou conexão bancária.",
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }
    item {
      Panel {
        Text("Seus primeiros passos", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("1 · Primeiro lançamento", fontWeight = FontWeight.SemiBold)
        Text("2 · Primeiro limite", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("3 · Avisos e Assistente", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    item {
      Button(
          onClick = onAddTransaction,
          modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home-first-entry"),
      ) {
        Text("Adicionar lançamento")
      }
    }
    if (importSourceEnabled || pendingImportCount > 0)
        item {
          OutlinedButton(
              onClick = onOpenImports,
              modifier = Modifier.fillMaxWidth().testTag("home-imports-shortcut"),
          ) {
            Text(
                if (pendingImportCount > 0) "Revisar importações pendentes ($pendingImportCount)"
                else "Importações pendentes"
            )
          }
        }
  }
}
