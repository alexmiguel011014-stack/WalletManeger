package com.example.allufinancas.data

import java.text.NumberFormat
import java.util.Locale

data class CategorySpending(val categoryId: String, val categoryName: String, val spentCents: Long)

data class HomeInsight(val insight: Insight, val dismissed: Boolean)

data class HomeProjection(
    val monthSummary: MonthSummary,
    val expensesByCategory: List<CategorySpending>,
    val recentTransactions: List<Transaction>,
    val insights: List<HomeInsight>,
    val accounts: List<AccountSummary>,
    val goals: List<Goal>,
)

object DemoAnalysis {
  fun balanceCents(transactions: List<Transaction>): Long = transactions.sumOf { it.amountCents }

  fun monthSummary(transactions: List<Transaction>, month: String): MonthSummary {
    val selected = transactionsInMonth(transactions, month)
    val income = selected.filter { it.amountCents > 0 }.sumOf { it.amountCents }
    val expense = selected.filter { it.amountCents < 0 }.sumOf { -it.amountCents }
    return MonthSummary(month, income, expense, income - expense)
  }

  fun categorySpentCents(transactions: List<Transaction>, categoryId: String, month: String): Long =
      transactionsInMonth(transactions, month)
          .filter { it.categoryId == categoryId && it.amountCents < 0 }
          .sumOf { -it.amountCents }

  fun homeProjection(
      accounts: List<AccountSummary>,
      transactions: List<Transaction>,
      categories: List<Category>,
      budgets: List<Budget>,
      alerts: List<Alert>,
      goals: List<Goal>,
      month: String,
  ): HomeProjection {
    val selected = transactionsInMonth(transactions, month)
    val categoryNames = categories.associate { it.id to it.name }
    val expenses =
        selected
            .filter { it.amountCents < 0 }
            .groupBy { it.categoryId }
            .map { (categoryId, rows) ->
              CategorySpending(
                  categoryId = categoryId,
                  categoryName = categoryNames[categoryId] ?: "Sem categoria",
                  spentCents = rows.sumOf { -it.amountCents },
              )
            }
            .sortedWith(
                compareByDescending<CategorySpending> { it.spentCents }.thenBy { it.categoryId }
            )
    val matchingDismissals =
        alerts
            .asSequence()
            .filter { month.isNotBlank() && it.dismissed && it.generatedDate.take(7) == month }
            .map { it.id }
            .toSet()

    return HomeProjection(
        monthSummary = monthSummary(selected, month),
        expensesByCategory = expenses,
        recentTransactions =
            selected.sortedWith(compareByDescending<Transaction> { it.date }.thenBy { it.id }),
        insights =
            if (month.isBlank()) emptyList()
            else
                insights(transactions, categories, budgets, month).map {
                  HomeInsight(it, it.id in matchingDismissals)
                },
        accounts = accounts,
        goals = goals,
    )
  }

  fun insights(
      transactions: List<Transaction>,
      categories: List<Category>,
      budgets: List<Budget>,
      month: String,
  ): List<Insight> {
    val selected =
        transactionsInMonth(transactions, month)
            .filter { it.amountCents < 0 }
            .sortedWith(compareBy<Transaction> { it.date }.thenBy { it.id })
    val names = categories.associate { it.id to it.name }
    val result = mutableListOf<Insight>()

    budgets
        .sortedBy { it.categoryId }
        .forEach { budget ->
          val spent = categorySpentCents(selected, budget.categoryId, month)
          if (spent > budget.limitCents) {
            val sources = selected.filter { it.categoryId == budget.categoryId }.map { it.id }
            result +=
                Insight(
                    id = "budget-${budget.categoryId}",
                    title = "Orçamento acima do limite",
                    explanation =
                        "${names[budget.categoryId] ?: "Categoria"}: ${money(spent)} em despesas registradas, " +
                            "${money(budget.limitCents)} de limite; diferença de ${money(spent - budget.limitCents)}.",
                    severity = InsightSeverity.WARNING,
                    transactionIds = sources,
                )
          }
        }

    selected
        .groupBy { Triple(it.merchant.lowercase(Locale.ROOT), it.amountCents, it.date) }
        .values
        .filter { it.size > 1 }
        .forEach { group ->
          result +=
              Insight(
                  id = "duplicate-${group.map { it.id }.sorted().joinToString("-")}",
                  title = "Possível lançamento repetido",
                  explanation =
                      "${group.size} compras de ${money(-group.first().amountCents)} " +
                          "em ${group.first().merchant} na mesma data. Confira os lançamentos; isto não confirma cobrança dupla.",
                  severity = InsightSeverity.WARNING,
                  transactionIds = group.map { it.id }.sorted(),
              )
        }

    selected
        .filter { it.recurring }
        .takeIf { it.isNotEmpty() }
        ?.let { recurring ->
          result +=
              Insight(
                  id = "recurring-$month",
                  title = "Compromissos recorrentes",
                  explanation =
                      "${recurring.size} despesas marcadas como recorrentes somam " +
                          "${money(recurring.sumOf { -it.amountCents })} neste mês.",
                  severity = InsightSeverity.INFO,
                  transactionIds = recurring.map { it.id },
              )
        }

    if (selected.size >= 4) {
      val values = selected.map { -it.amountCents }.sorted()
      val middle = values.size / 2
      val median =
          if (values.size % 2 == 0) {
            values[middle - 1] + (values[middle] - values[middle - 1]) / 2
          } else values[middle]
      val threshold = median.takeIf { it <= Long.MAX_VALUE / 3 }?.times(3)
      val unusual = threshold?.let { limit ->
        selected
            .filter { !it.recurring && it.amountCents < -limit }
            .minWithOrNull(compareBy<Transaction> { it.amountCents }.thenBy { it.id })
      }
      unusual?.let {
        result +=
            Insight(
                id = "unusual-${it.id}",
                title = "Valor acima do padrão deste mês",
                explanation =
                    "${it.merchant}: ${money(-it.amountCents)}, mais de três vezes " +
                        "a mediana ${money(median)} das despesas deste mês. Não indica fraude.",
                severity = InsightSeverity.INFO,
                transactionIds = listOf(it.id),
            )
      }
    }
    return prioritizeInsights(result)
  }

  internal fun prioritizeInsights(insights: List<Insight>): List<Insight> =
      insights.sortedWith(
          compareBy<Insight> {
                when (it.severity) {
                  InsightSeverity.CRITICAL -> 0
                  InsightSeverity.WARNING -> 1
                  InsightSeverity.INFO -> 2
                }
              }
              .thenBy { it.id }
              .thenBy { it.title }
              .thenBy { it.explanation }
              .thenBy { it.transactionIds.sorted().joinToString("|") },
      )

  private fun transactionsInMonth(
      transactions: List<Transaction>,
      month: String,
  ): List<Transaction> =
      if (month.isBlank()) emptyList() else transactions.filter { it.date.take(7) == month }

  fun money(cents: Long): String =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(cents / 100.0)
}
