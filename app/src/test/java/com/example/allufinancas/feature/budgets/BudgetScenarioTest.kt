package com.example.allufinancas.feature.budgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetScenarioTest {
    @Test fun calculatesHypotheticalBalanceAndDifferenceFromCurrentLimit() {
        assertEquals(
            BudgetScenarioResult(100_000, 65_542, 34_458, 50_000),
            calculateBudgetScenario(50_000, 65_542, 100_000),
        )
    }

    @Test fun allowsNegativeHypotheticalBalanceWhenRecordedSpendExceedsProposal() {
        assertEquals(
            BudgetScenarioResult(40_000, 65_542, -25_542, -10_000),
            calculateBudgetScenario(50_000, 65_542, 40_000),
        )
    }

    @Test fun rejectsNegativeZeroProposalAndOutOfRangeMoney() {
        assertNull(calculateBudgetScenario(-1, 0, 1))
        assertNull(calculateBudgetScenario(1, -1, 1))
        assertNull(calculateBudgetScenario(1, 0, 0))
        assertNull(calculateBudgetScenario(1, 0, Long.MAX_VALUE))
    }

    @Test fun acceptsMaximumInputsAndOneCentProposalButRejectsMaximumPlusOne() {
        val maximum = BudgetAmount.maximumCents
        assertEquals(BudgetScenarioResult(maximum, maximum, 0, 0), calculateBudgetScenario(maximum, maximum, maximum))
        assertEquals(BudgetScenarioResult(1, 0, 1, 1), calculateBudgetScenario(0, 0, 1))

        assertNull(calculateBudgetScenario(maximum + 1, 0, 1))
        assertNull(calculateBudgetScenario(0, maximum + 1, 1))
        assertNull(calculateBudgetScenario(0, 0, maximum + 1))
    }
}
