package com.example.allufinancas.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.Budget
import com.example.allufinancas.data.Category
import com.example.allufinancas.data.Transaction
import com.example.allufinancas.designsystem.EmptyState
import com.example.allufinancas.designsystem.Heading
import com.example.allufinancas.designsystem.Panel

internal data class FinancialHealthSummary(
    val month: String,
    val periodTransactionCount: Int,
    val recurringTransactions: List<ObservedTransaction>,
    val exceededLimits: List<CategoryLimitSignal>,
    val installments: List<InstallmentSignal>,
) {
  val hasEvidence: Boolean
    get() =
        recurringTransactions.isNotEmpty() ||
            exceededLimits.isNotEmpty() ||
            installments.isNotEmpty()
}

internal data class ObservedTransaction(
    val id: String,
    val date: String,
    val merchant: String,
    val amountCents: Long,
)

internal data class CategoryLimitSignal(
    val categoryId: String,
    val categoryName: String,
    val spentCents: Long,
    val limitCents: Long,
    val sourceTransactions: List<ObservedTransaction>,
)

internal data class InstallmentSignal(
    val transaction: ObservedTransaction,
    val number: Int,
    val count: Int,
    val remaining: Int,
)

/** Derives only facts represented by the selected month's local rows and configured limits. */
internal fun financialHealthSummary(
    transactions: List<Transaction>,
    categories: List<Category>,
    budgets: List<Budget>,
    month: String,
): FinancialHealthSummary {
  val selected =
      if (month.isBlank()) emptyList() else transactions.filter { it.date.take(7) == month }
  fun observed(row: Transaction) =
      ObservedTransaction(row.id, row.date, row.merchant, row.amountCents)

  val recurring =
      selected
          .filter { it.recurring }
          .sortedWith(compareByDescending<Transaction> { it.date }.thenBy { it.id })
          .map(::observed)
  val categoryNames = categories.associate { it.id to it.name }
  val exceeded =
      budgets
          .sortedBy { it.categoryId }
          .mapNotNull { budget ->
            val sources =
                selected
                    .filter { it.categoryId == budget.categoryId && it.amountCents < 0 }
                    .sortedWith(compareByDescending<Transaction> { it.date }.thenBy { it.id })
            val spent = sources.sumOf { -it.amountCents }
            if (budget.limitCents < 0 || spent <= budget.limitCents) null
            else
                CategoryLimitSignal(
                    categoryId = budget.categoryId,
                    categoryName = categoryNames[budget.categoryId] ?: "Sem categoria",
                    spentCents = spent,
                    limitCents = budget.limitCents,
                    sourceTransactions = sources.map(::observed),
                )
          }
  val installments =
      selected
          .mapNotNull { row ->
            val number = row.installmentNumber ?: return@mapNotNull null
            val count = row.installmentCount ?: return@mapNotNull null
            if (number < 1 || count <= number) return@mapNotNull null
            InstallmentSignal(observed(row), number, count, count - number)
          }
          .sortedWith(
              compareByDescending<InstallmentSignal> { it.transaction.date }
                  .thenBy { it.transaction.id }
          )

  return FinancialHealthSummary(month, selected.size, recurring, exceeded, installments)
}

@Composable
internal fun FinancialHealthSummarySection(
    summary: FinancialHealthSummary,
    showCents: Boolean,
    onOpenTransaction: (String) -> Unit,
    onOpenPlans: () -> Unit,
) {
  Heading("Sinais observáveis do período")
  if (!summary.hasEvidence) {
    val noRecords = summary.periodTransactionCount == 0
    EmptyState(
        if (noRecords) "Sem registros para este período"
        else "Nenhum registro corresponde às regras",
        if (noRecords) "Não há lançamentos cadastrados neste mês."
        else "Há lançamentos neste período, mas nenhum corresponde às regras exibidas.",
    )
    return
  }

  summary.recurringTransactions
      .takeIf { it.isNotEmpty() }
      ?.let { rows ->
        Panel {
          Text("Lançamentos marcados como recorrentes", fontWeight = FontWeight.Bold)
          Text(
              "${rows.size} registro(s) com o marcador recorrente no mês.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
              "Regra: contagem dos lançamentos marcados como recorrentes no período.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          EvidenceScope(summary.month)
          rows.forEach { EvidenceRow(it, showCents, onOpenTransaction) }
        }
      }

  summary.exceededLimits.forEach { signal ->
    Panel {
      Text("Despesas acima do limite · ${signal.categoryName}", fontWeight = FontWeight.Bold)
      Text(
          "${brl(signal.spentCents, showCents)} em despesas efetivas · limite ${brl(signal.limitCents, showCents)}"
      )
      Text(
          "Regra: soma das despesas da categoria efetiva no período > limite configurado.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      EvidenceScope(summary.month)
      signal.sourceTransactions.forEach { EvidenceRow(it, showCents, onOpenTransaction) }
      TextButton(onClick = onOpenPlans) { Text("Ver limite em Planos") }
    }
  }

  summary.installments.forEach { signal ->
    Panel {
      Text("Parcela registrada ${signal.number}/${signal.count}", fontWeight = FontWeight.Bold)
      Text("${signal.remaining} restante(s), conforme o contador informado na linha.")
      Text(
          "Regra: parcelas restantes = total informado − parcela informada; sem estimar data ou valor futuro.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      EvidenceScope(summary.month)
      EvidenceRow(signal.transaction, showCents, onOpenTransaction)
    }
  }
}

@Composable
private fun EvidenceScope(month: String) {
  Text(
      "Período: ${month.ifBlank { "indisponível" }} · fonte: lançamentos locais cadastrados",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelSmall,
  )
}

@Composable
private fun EvidenceRow(
    transaction: ObservedTransaction,
    showCents: Boolean,
    onOpenTransaction: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text("${transaction.id} · ${transaction.date} · ${brl(transaction.amountCents, showCents)}")
    Text(transaction.merchant, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { onOpenTransaction(transaction.id) }) {
      Text("Abrir lançamento ${transaction.id}")
    }
  }
  Spacer(Modifier.height(4.dp))
}
