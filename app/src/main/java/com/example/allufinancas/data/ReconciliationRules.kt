package com.example.allufinancas.data

import java.time.YearMonth

/** Explainable rules over local user-entered transactions; these flags are not fraud findings. */
object ReconciliationRules {
  fun cases(
      transactions: List<Transaction>,
      decisions: List<ReconciliationDecisionRow> = emptyList(),
      month: String = YearMonth.now().toString(),
  ): List<ReconciliationCase> {
    val sample =
        if (month.isBlank()) emptyList() else transactions.filter { it.date.startsWith(month) }
    val decidedById = decisions.associateBy { it.caseId }
    val missingCategory =
        sample
            .filter { it.sourceCategoryId.isBlank() }
            .map { transaction ->
              val id = "uncategorized-${transaction.id}"
              ReconciliationCase(
                  id = id,
                  kind = ReviewKind.UNCATEGORIZED,
                  title = "Escolha uma categoria",
                  explanation =
                      "Este lançamento não tem categoria. A escolha fica salva localmente; valor e data não mudam.",
                  transactionIds = listOf(transaction.id),
                  strength = ReviewStrength.LIMITED,
                  decided = id in decidedById,
                  selectedCategoryId = decidedById[id]?.categoryId,
              )
            }
    val ruleCases =
        DemoAnalysis.insights(
                sample.map { it.copy(categoryId = it.sourceCategoryId) },
                DefaultCategories.all,
                emptyList(),
                month,
            )
            .mapNotNull { insight ->
              val kind =
                  when {
                    insight.id.startsWith("duplicate-") -> ReviewKind.POSSIBLE_DUPLICATE
                    insight.id.startsWith("unusual-") -> ReviewKind.UNUSUAL_AMOUNT
                    else -> null
                  } ?: return@mapNotNull null
              val extraExplanation =
                  when (kind) {
                    ReviewKind.POSSIBLE_DUPLICATE ->
                        " Coincidência de estabelecimento, valor e data; confira os registros. Não confirma cobrança duplicada."
                    ReviewKind.UNUSUAL_AMOUNT ->
                        " Sinal qualitativo limitado aos lançamentos deste mês; não indica fraude ou irregularidade."
                    ReviewKind.UNCATEGORIZED -> ""
                  }
              val existing = decidedById[insight.id]
              ReconciliationCase(
                  id = insight.id,
                  kind = kind,
                  title = insight.title,
                  explanation = insight.explanation + extraExplanation,
                  transactionIds = insight.transactionIds,
                  strength =
                      if (kind == ReviewKind.POSSIBLE_DUPLICATE) ReviewStrength.MODERATE
                      else ReviewStrength.LIMITED,
                  decided = existing != null,
                  selectedCategoryId = existing?.categoryId,
              )
            }
    return (missingCategory + ruleCases).sortedWith(
        compareBy<ReconciliationCase> { it.decided }.thenBy { it.id }
    )
  }

  fun effectiveTransactions(
      transactions: List<Transaction>,
      decisions: List<ReconciliationDecisionRow>,
  ): List<Transaction> {
    val overrides =
        decisions
            .asSequence()
            .filter { it.categoryId != null }
            .flatMap { decision ->
              decision.transactionIds.asSequence().map { it to decision.categoryId!! }
            }
            .toMap()
    return transactions.map { transaction ->
      transaction.copy(categoryId = overrides[transaction.id] ?: transaction.sourceCategoryId)
    }
  }
}
