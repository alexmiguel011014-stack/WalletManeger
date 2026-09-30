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

    @Test fun localSourceAndScopeAreShown() {
        val materializedAt = Instant.parse("2026-09-29T12:00:00Z")
        rule.setContent {
            AlluTheme {
                Column {
                    DemoStrip("Sem adicionais", materializedAt)
                    DemoStrip("Com adicionais", materializedAt)
                }
            }
        }

        assertEquals(2, rule.onAllNodesWithText("DADOS LOCAIS", substring = true).fetchSemanticsNodes().size)
        assertEquals(2, rule.onAllNodesWithText("Fonte: lançamentos cadastrados neste dispositivo", substring = true).fetchSemanticsNodes().size)
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
                    onSave = {},
                    onDelete = {},
                )
            }
        }
        rule.onNodeWithText("Lançamento não encontrado").assertExists()
        rule.onNodeWithText("Voltar ao extrato").performClick()
        assertTrue(returnedToTransactions.get())
    }
}
