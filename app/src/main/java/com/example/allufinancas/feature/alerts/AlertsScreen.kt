package com.example.allufinancas.feature.alerts

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*

@Composable
internal fun Alerts(
    alerts: List<Alert>,
    pendingReviewCount: Int,
    onDismiss: (String, Boolean) -> Unit,
    onOpenReview: () -> Unit,
) {
  var showDismissed by rememberSaveable { mutableStateOf(false) }
  val visible = alerts.filter { it.dismissed == showDismissed }
  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    if (pendingReviewCount > 0)
        item {
          Box(Modifier.testTag("alerts-review-shortcut")) {
            Panel {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Caixa de revisão · $pendingReviewCount", fontWeight = FontWeight.Bold)
                Text(
                    "Confira categorias ausentes, possíveis repetições e valores acima do padrão dos seus lançamentos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenReview) { Text("Revisar lançamentos") }
              }
            }
          }
        }
    if (alerts.isNotEmpty())
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDismissed = false }) { Text("Ativos") }
            OutlinedButton(onClick = { showDismissed = true }) { Text("Arquivados") }
          }
        }
    if (visible.isEmpty())
        item {
          EmptyState(
              title =
                  when {
                    showDismissed -> "Nenhum aviso arquivado"
                    alerts.isEmpty() -> "Nenhum aviso gerado"
                    else -> "Sem avisos ativos"
                  },
              message =
                  when {
                    showDismissed -> "Os avisos arquivados por você aparecerão aqui."
                    alerts.isEmpty() ->
                        "Avisos aparecem quando regras locais encontram padrões nos lançamentos salvos. A ausência de avisos não garante ausência de risco."
                    else ->
                        "Todos os avisos atuais foram arquivados. A ausência de avisos ativos não é uma avaliação de risco zero."
                  },
          )
        }
    itemsIndexed(visible, key = { _, alert -> alert.id }) { index, alert ->
      val status =
          when (alert.severity) {
            InsightSeverity.INFO -> AlluStatus.NEUTRAL
            InsightSeverity.WARNING -> AlluStatus.WARNING
            InsightSeverity.CRITICAL -> AlluStatus.CRITICAL
          }
      val details = buildString {
        append("Severidade: ${alert.severity.name}")
        append("\nRegra: ${alert.title} · ${alert.id}")
        append("\nLançamentos-base: ")
        append(
            alert.transactionIds.takeIf { it.isNotEmpty() }?.joinToString() ?: "nenhum associado"
        )
        append(
            "\nGerado localmente em ${alert.generatedDate} · sem sincronização ou atualização em tempo real."
        )
        append("\n\n${alert.explanation}")
      }
      AlluContentEntrance(visible = true, delayMillis = index * 40) {
        InsightCard(
            title = alert.title,
            description = details,
            status = status,
            actionLabel = if (alert.dismissed) "Reabrir aviso" else "Arquivar aviso",
            onAction = { onDismiss(alert.id, !alert.dismissed) },
        )
      }
    }
    item {
      Text(
          "Esses avisos não confirmam fraude nem substituem orientação profissional.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
      )
    }
  }
}
