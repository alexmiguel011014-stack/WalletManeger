package com.example.allufinancas

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.DemoPreferences
import com.example.allufinancas.data.DemoAnalysis
import com.example.allufinancas.data.DemoDatabase
import com.example.allufinancas.data.DefaultCategories
import com.example.allufinancas.data.Transaction
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
        seedLocalLedger(first, context)
        val source = first.transactions.first().first { it.id == "t07" }

        first.setTransactionCategory("t07", "transporte")
        val corrected = first.transactions.first().first { it.id == "t07" }
        assertEquals("transporte", corrected.categoryId)
        assertEquals("", corrected.sourceCategoryId)
        assertEquals(source.amountCents, corrected.amountCents)
        assertEquals(source.date, corrected.date)
        assertEquals(2_890, DemoAnalysis.categorySpentCents(first.transactions.first(), "transporte", LocalLedgerFixtures.MONTH))
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
        assertTrue(reopenedRepository.transactions.first().isEmpty())
        assertTrue(reopenedRepository.reconciliationCases.first().isEmpty())
    }

    @Test fun migrationFromVersionTwoClearsOldFinancialDataAndKeepsPreferences() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "allu_migration_v2_test.db"
        context.deleteDatabase(name)
        DemoPreferences(context).setDarkTheme(true)
        DemoPreferences(context).setReduceMotion(true)
        DemoPreferences(context).setShowCents(false)
        val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        legacy.execSQL("CREATE TABLE accounts (id TEXT NOT NULL, name TEXT NOT NULL, kind TEXT NOT NULL, balanceCents INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE transactions (id TEXT NOT NULL, accountId TEXT NOT NULL, merchant TEXT NOT NULL, description TEXT NOT NULL, amountCents INTEGER NOT NULL, date TEXT NOT NULL, categoryId TEXT NOT NULL, installmentNumber INTEGER, installmentCount INTEGER, recurring INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE categories (id TEXT NOT NULL, name TEXT NOT NULL, colorHex TEXT NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE budgets (categoryId TEXT NOT NULL, limitCents INTEGER NOT NULL, PRIMARY KEY(categoryId))")
        legacy.execSQL("CREATE TABLE alerts (id TEXT NOT NULL, title TEXT NOT NULL, explanation TEXT NOT NULL, severity TEXT NOT NULL, transactionIds TEXT NOT NULL, generatedDate TEXT NOT NULL, dismissed INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE goals (id TEXT NOT NULL, name TEXT NOT NULL, targetCents INTEGER NOT NULL, savedCents INTEGER NOT NULL, PRIMARY KEY(id))")
        legacy.execSQL("CREATE TABLE assistant_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, createdAt TEXT NOT NULL, citationTransactionIds TEXT NOT NULL)")
        legacy.execSQL("CREATE TABLE reconciliation_decisions (caseId TEXT NOT NULL, kind TEXT NOT NULL, transactionIds TEXT NOT NULL, categoryId TEXT, PRIMARY KEY(caseId))")
        legacy.execSQL("INSERT INTO accounts VALUES ('legacy-account', 'Conta antiga', 'CHECKING', 10000)")
        legacy.execSQL("INSERT INTO transactions VALUES ('legacy-tx', 'account', 'Fonte', 'Demonstração', -1234, '2026-09-01', 'origem', NULL, NULL, 0)")
        legacy.execSQL("INSERT INTO categories VALUES ('origem', 'Categoria antiga', '#000000')")
        legacy.execSQL("INSERT INTO budgets VALUES ('origem', 10000)")
        legacy.execSQL("INSERT INTO alerts VALUES ('legacy-alert', 'Aviso antigo', 'Texto', 'WARNING', 'legacy-tx', '2026-09-01', 0)")
        legacy.execSQL("INSERT INTO goals VALUES ('legacy-goal', 'Meta antiga', 50000, 1000)")
        legacy.execSQL("INSERT INTO assistant_messages (role, text, createdAt, citationTransactionIds) VALUES ('USER', 'Mensagem antiga', '2026-09-01T00:00:00', '')")
        legacy.execSQL("INSERT INTO reconciliation_decisions VALUES ('legacy-case', 'POSSIBLE_DUPLICATE', 'legacy-tx', NULL)")
        legacy.version = 2
        legacy.close()

        val migrated = DemoDatabase.open(context, name)
        try {
            val db = migrated.openHelper.writableDatabase
            listOf("accounts", "transactions", "budgets", "alerts", "goals", "assistant_messages", "reconciliation_decisions")
                .forEach { table -> assertEquals("$table should be cleared", 0, tableCount(db, table)) }
            assertTrue(migrated.dao().allTransactions().isEmpty())
            assertEquals(DefaultCategories.all.map { it.id }.sorted(), migrated.dao().allCategories().map { it.id }.sorted())
            assertEquals(4, db.version)
            val prefs = DemoPreferences(context).state.first()
            assertTrue(prefs.darkTheme && prefs.reduceMotion && !prefs.showCents)
        } finally {
            migrated.close()
            context.deleteDatabase(name)
            DemoPreferences(context).setDarkTheme(false)
            DemoPreferences(context).setReduceMotion(false)
            DemoPreferences(context).setShowCents(true)
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
        seedLocalLedger(first, context)

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
        assertTrue(reopened.budgets.first().isEmpty())
        assertTrue(reopened.transactions.first().isEmpty())
        assertEquals(DefaultCategories.all.map { it.id }.sorted(), reopened.categories.first().map { it.id }.sorted())
        assertTrue(reopened.messages.first().isEmpty())
        reopened.setDarkTheme(false)
        reopened.setReduceMotion(false)
        reopened.setShowCents(true)
    }

    @Test fun blankLedgerSupportsValidatedTransactionCrudAndDynamicTotals() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DemoRepository(context)
        withTimeout(15_000) { repository.ready.first { it } }
        repository.resetDemoData()

        assertTrue(repository.accounts.first().isEmpty())
        assertTrue(repository.transactions.first().isEmpty())
        assertTrue(repository.budgets.first().isEmpty())
        assertEquals(DefaultCategories.all.map { it.id }.sorted(), repository.categories.first().map { it.id }.sorted())
        val month = java.time.YearMonth.now().toString()
        assertEquals(0, DemoAnalysis.monthSummary(emptyList(), month).incomeCents)
        assertEquals(0, DemoAnalysis.monthSummary(emptyList(), month).expenseCents)
        assertEquals(0, DemoAnalysis.balanceCents(emptyList()))

        val date = java.time.LocalDate.now().toString()
        val original = Transaction(
            "user-entry", "ignored-account", " Mercado ", " Compra ", -12_500, date, "alimentacao",
            installmentNumber = 3, installmentCount = 10, recurring = true,
        )
        repository.saveTransaction(original)
        var stored = repository.transactions.first().single()
        assertEquals("manual-ledger", stored.accountId)
        assertEquals("Mercado", stored.merchant)
        assertEquals("Compra", stored.description)
        assertEquals(3, stored.installmentNumber)
        assertEquals(10, stored.installmentCount)
        assertTrue(stored.recurring)
        assertEquals(12_500, DemoAnalysis.monthSummary(listOf(stored), month).expenseCents)

        val persisted = DemoRepository(context)
        withTimeout(15_000) { persisted.ready.first { it } }
        stored = persisted.transactions.first().single()
        assertEquals(3, stored.installmentNumber)
        assertEquals(10, stored.installmentCount)
        assertTrue(stored.recurring)

        persisted.saveTransaction(original.copy(
            merchant = "Farmácia", amountCents = 7_500, categoryId = "receita",
            recurring = false, installmentNumber = null, installmentCount = null,
        ))
        assertTrue(runCatching { persisted.saveTransaction(original.copy(categoryId = "receita")) }.isFailure)
        val reopened = DemoRepository(context)
        withTimeout(15_000) { reopened.ready.first { it } }
        stored = reopened.transactions.first().single()
        assertEquals("Farmácia", stored.merchant)
        assertEquals(null, stored.installmentNumber)
        assertEquals(null, stored.installmentCount)
        assertFalse(stored.recurring)
        assertEquals(7_500, DemoAnalysis.monthSummary(listOf(stored), month).incomeCents)
        assertTrue(runCatching { reopened.setTransactionCategory("user-entry", "alimentacao") }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(amountCents = 0)) }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(date = "2026-02-30")) }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(merchant = "  ")) }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(categoryId = "missing")) }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(amountCents = 1_000_000_000_001)) }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(installmentNumber = 11)) }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(installmentNumber = 1, installmentCount = 121)) }.isFailure)
        assertTrue(runCatching { reopened.saveTransaction(original.copy(amountCents = 7_500, recurring = true)) }.isFailure)

        reopened.updateBudget("alimentacao", 40_000)
        assertEquals(40_000, reopened.budgets.first().single().limitCents)
        reopened.deleteTransaction("user-entry")
        assertTrue(reopened.transactions.first().isEmpty())
        reopened.resetDemoData()
        assertTrue(reopened.budgets.first().isEmpty())
        assertEquals(DefaultCategories.all.map { it.id }.sorted(), reopened.categories.first().map { it.id }.sorted())
    }

    private fun tableCount(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            check(cursor.moveToFirst())
            cursor.getInt(0)
        }
}
