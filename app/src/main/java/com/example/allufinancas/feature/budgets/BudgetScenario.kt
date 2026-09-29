package com.example.allufinancas.feature.budgets

internal data class BudgetScenarioResult(
    val hypotheticalLimitCents: Long,
    val spentCents: Long,
    val hypotheticalBalanceCents: Long,
    val limitDifferenceCents: Long,
)

internal fun calculateBudgetScenario(
    currentLimitCents: Long,
    spentCents: Long,
    proposedLimitCents: Long,
): BudgetScenarioResult? {
    val safeCents = 0L..BudgetAmount.maximumCents
    if (currentLimitCents !in safeCents || spentCents !in safeCents || proposedLimitCents !in 1L..BudgetAmount.maximumCents) {
        return null
    }
    return runCatching {
        BudgetScenarioResult(
            hypotheticalLimitCents = proposedLimitCents,
            spentCents = spentCents,
            hypotheticalBalanceCents = Math.subtractExact(proposedLimitCents, spentCents),
            limitDifferenceCents = Math.subtractExact(proposedLimitCents, currentLimitCents),
        )
    }.getOrNull()
}
