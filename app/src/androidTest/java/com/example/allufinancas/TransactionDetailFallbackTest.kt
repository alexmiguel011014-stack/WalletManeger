package com.example.allufinancas

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.allufinancas.data.DisplayPreferences
import com.example.allufinancas.designsystem.AlluTheme
import com.example.allufinancas.designsystem.DemoStrip
import com.example.allufinancas.feature.transactions.TransactionDetail
import com.example.allufinancas.feature.transactions.transactionById
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TransactionDetailFallbackTest {
    @get:Rule val rule = createAndroidComposeRule<TransactionFallbackTestActivity>()

    @Test fun optionalSyncAndCoverageAppearOnlyWhenProvided() {
        val materializedAt = Instant.parse("2026-09-29T12:00:00Z")
        rule.setContent {
            AlluTheme {
                Column {
                    DemoStrip("Sem adicionais", materializedAt)
                    DemoStrip(
                        "Com adicionais",
                        materializedAt,
                        lastSyncAt = Instant.parse("2026-09-29T12:30:00Z"),
                        coverageNote = "janela de teste",
                    )
                }
            }
        }

        assertEquals(1, rule.onAllNodesWithText("Última sincronização:", substring = true).fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodesWithText("Cobertura:", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun unknownTransactionIdShowsRecoveryActionInsteadOfCrashing() {
        val returnedToTransactions = AtomicBoolean(false)
        rule.setContent {
            AlluTheme {
                TransactionDetail(
                    transaction = transactionById(emptyList(), "missing-id"),
                    categories = emptyList(),
                    settings = DisplayPreferences(),
                    onBack = { returnedToTransactions.set(true) },
                    onCategory = { _, _ -> },
                )
            }
        }
        rule.onNodeWithText("Lançamento não encontrado").assertExists()
        rule.onNodeWithText("Voltar ao extrato").performClick()
        assertTrue(returnedToTransactions.get())
    }
}
