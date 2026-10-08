package com.example.allufinancas.feature.budgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetAmountTest {
  @Test
  fun parsesBrazilianThousandsAndDecimalSeparators() {
    assertEquals(100_000L, BudgetAmount.parseCents("1.000,00"))
    assertEquals(123_456L, BudgetAmount.parseCents("1.234,56"))
    assertEquals(100_050L, BudgetAmount.parseCents("1000,5"))
    assertEquals(100L, BudgetAmount.parseCents(" 1,00 "))
  }

  @Test
  fun acceptsTheConfiguredMaximumExactly() {
    assertEquals(1L, BudgetAmount.parseCents("0,01"))
    assertEquals(BudgetAmount.maximumCents, BudgetAmount.parseCents("10.000.000.000,00"))
    assertNull(BudgetAmount.parseCents("10.000.000.000,01"))
  }

  @Test
  fun rejectsEmptyZeroNegativeInvalidAndOverflowValues() {
    listOf(
            "",
            "   ",
            "0",
            "0,00",
            "-1,00",
            "+1,00",
            "1.00,00",
            "1,234",
            "R$ 1.000,00",
            "abc",
            "999999999999999999999999999999999999,99",
        )
        .forEach { assertNull("Expected invalid amount: $it", BudgetAmount.parseCents(it)) }
  }
}
