package com.example.allufinancas

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.DemoPreferences
import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.DemoSeed
import com.example.allufinancas.data.DemoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import android.content.Context

@RunWith(AndroidJUnit4::class)
class RepositoryPersistenceTest {
    @Test fun reconciliationCategoryAndReviewDecisionsPersistUndoAndResetWithoutTouchingSourceRows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = DemoRepository(context)
        withTimeout(15_000) { first.ready.first { it } }
        first.resetDemoData()
        val source = first.transactions.first().first { it.id == "t07" }

        first.setTransactionCategory("t07", "transporte")
        val corrected = first.transactions.first().first { it.id == "t07" }
        assertEquals("transporte", corrected.categoryId)
        assertEquals("", corrected.sourceCategoryId)
        assertEquals(source.amountCents, corrected.amountCents)
        assertEquals(source.date, corrected.date)
        assertEquals(2_890, DemoAnalysis.categorySpentCents(first.transactions.first(), "transporte", DemoSeed.MONTH))
        val assistantAnswer = first.sendMessage("Quanto gastei com transporte?")
        assertTrue(assistantAnswer.text.contains(DemoAnalysis.money(2_890)))
        assertEquals(listOf("t07"), assistantAnswer.citationTransactionIds)
        assertTrue(first.reconciliationCases.first().first { it.id == "uncategorized-t07" }.decided)

        first.decideReconciliationCase("duplicate-t04-t05")
        val reopenedRepository = DemoRepository(context)
        withTimeout(15_000) { reopenedRepository.ready.first { it } }
        assertTrue(reopenedRepository.reconciliationCases.first().first { it.id == "duplicate-t04-t05" }.decided)
        reopenedRepository.reopenReconciliationCase("duplicate-t04-t05")
        reopenedRepository.reopenReconciliationCase("uncategorized-t07")
        val restored = reopenedRepository.transactions.first().first { it.id == "t07" }
        assertEquals("", restored.categoryId)
        assertEquals(source.amountCents, restored.amountCents)
        assertEquals(source.date, restored.date)
        assertFalse(reopenedRepository.reconciliationCases.first().first { it.id == "uncategorized-t07" }.decided)

        reopenedRepository.setTransactionCategory("t07", "transporte")
        reopenedRepository.decideReconciliationCase("unusual-t02")
        reopenedRepository.resetDemoData()
        val reset = reopenedRepository.transactions.first().first { it.id == "t07" }
        assertEquals("", reset.categoryId)
        assertFalse(reopenedRepository.reconciliationCases.first().first { it.id == "unusual-t02" }.decided)
    }

    @Test fun migrationFromVersionOneAddsDecisionTableWithoutChangingTransactions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "allu_migration_v1_test.db"
        context.deleteDatabase(name)
        val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        legacy.execSQL("CREATE TABLE accounts (id TEXT NOT NULL, name TEXT NOT NULL, kind TEXT NOT NULL, balanceCents INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE transactions (id TEXT NOT NULL, accountId TEXT NOT NULL, merchant TEXT NOT NULL, description TEXT NOT NULL, amountCents INTEGER NOT NULL, date TEXT NOT NULL, categoryId TEXT NOT NULL, installmentNumber INTEGER, installmentCount INTEGER, recurring INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE categories (id TEXT NOT NULL, name TEXT NOT NULL, colorHex TEXT NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE budgets (categoryId TEXT NOT NULL, limitCents INTEGER NOT NULL, PRIMARY KEY(categoryId))")
        legacy.execSQL("CREATE TABLE alerts (id TEXT NOT NULL, title TEXT NOT NULL, explanation TEXT NOT NULL, severity TEXT NOT NULL, transactionIds TEXT NOT NULL, generatedDate TEXT NOT NULL, dismissed INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE goals (id TEXT NOT NULL, name TEXT NOT NULL, targetCents INTEGER NOT NULL, savedCents INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE assistant_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, createdAt TEXT NOT NULL, citationTransactionIds TEXT NOT NULL)")
        legacy.execSQL("INSERT INTO transactions VALUES ('legacy-tx', 'account', 'Fonte', 'Demonstração', -1234, '2026-09-01', 'origem', NULL, NULL, 0)")
        legacy.version = 1
        legacy.close()

        val migrated = DemoDatabase.open(context, name)
        try {
            val row = migrated.dao().allTransactions().single()
            assertEquals("legacy-tx", row.id)
            assertEquals(-1234, row.amountCents)
            assertEquals("2026-09-01", row.date)
            assertEquals("origem", row.categoryId)
            assertEquals(2, migrated.openHelper.writableDatabase.version)
            assertTrue(migrated.dao().allReconciliationDecisions().isEmpty())
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun materializationInstantPersistsUpdatesAndDoesNotBackfillLegacyData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstInstant = Instant.parse("2026-09-29T12:34:56Z")
        val first = DemoRepository(context, Clock.fixed(firstInstant, ZoneOffset.UTC))
        withTimeout(15_000) { first.ready.first { it } }
        first.resetDemoData()
        assertEquals(firstInstant, first.materializedAt.first())

        val updatedInstant = Instant.parse("2026-09-29T13:45:00Z")
        val reopened = DemoRepository(context, Clock.fixed(updatedInstant, ZoneOffset.UTC))
        withTimeout(15_000) { reopened.ready.first { it } }
        assertEquals(firstInstant, reopened.materializedAt.first())
        reopened.resetDemoData()
        assertEquals(updatedInstant, reopened.materializedAt.first())

        DemoPreferences(context).clearMaterializedAt()
        val legacy = DemoRepository(context, Clock.fixed(Instant.parse("2026-09-29T14:00:00Z"), ZoneOffset.UTC))
        withTimeout(15_000) { legacy.ready.first { it } }
        assertNull(legacy.materializedAt.first())
    }

    @Test fun localEditsSurviveRepositoryRecreationAndResetExactly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = DemoRepository(context)
        withTimeout(15_000) { first.ready.first { it } }
        first.resetDemoData()

        first.updateBudget("alimentacao", 123_456)
        first.setTransactionCategory("t02", "casa")
        val alertId = first.alerts.first().first().id
        first.setAlertDismissed(alertId, true)
        first.sendMessage("Quanto gastei no mês?")
        first.setDarkTheme(true)
        first.setReduceMotion(true)
        first.setShowCents(false)

        val reopened = DemoRepository(context)
        withTimeout(15_000) { reopened.ready.first { it } }
        assertEquals(123_456, reopened.budgets.first().first { it.categoryId == "alimentacao" }.limitCents)
        assertEquals("casa", reopened.transactions.first().first { it.id == "t02" }.categoryId)
        assertEquals("alimentacao", reopened.transactions.first().first { it.id == "t02" }.sourceCategoryId)
        assertTrue(reopened.alerts.first().first { it.id == alertId }.dismissed)
        assertEquals(2, reopened.messages.first().size)
        assertTrue(reopened.preferences.first().darkTheme)
        assertTrue(reopened.preferences.first().reduceMotion)
        assertTrue(!reopened.preferences.first().showCents)

        reopened.resetDemoData()
        assertEquals(DemoSeed.budgets.first { it.categoryId == "alimentacao" }.limitCents,
            reopened.budgets.first().first { it.categoryId == "alimentacao" }.limitCents)
        assertEquals("alimentacao", reopened.transactions.first().first { it.id == "t02" }.categoryId)
        assertTrue(reopened.messages.first().isEmpty())
        reopened.setDarkTheme(false)
        reopened.setReduceMotion(false)
        reopened.setShowCents(true)
    }
}
