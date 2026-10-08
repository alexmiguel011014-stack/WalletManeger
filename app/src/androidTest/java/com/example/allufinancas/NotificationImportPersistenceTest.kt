package com.example.allufinancas

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.allufinancas.data.DemoDatabase
import com.example.allufinancas.data.DemoPreferences
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.ImportNoticeConfirmation
import com.example.allufinancas.data.ImportNoticeKind
import com.example.allufinancas.data.ImportNoticeStatus
import com.example.allufinancas.data.NotificationImportCandidate
import com.example.allufinancas.data.NotificationImportSource
import com.example.allufinancas.data.Transaction
import com.example.allufinancas.data.TransactionOrigin
import com.example.allufinancas.notification.NotificationImportCapture
import com.example.allufinancas.notification.NotificationImportFingerprints
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationImportPersistenceTest {
  @Test
  fun versionThreeMigrationKeepsLedgerRowsAndAddsAnEmptyImportQueue() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val name = "allu_import_migration_v3_test.db"
    val preferences = DemoPreferences(context)
    val previousPreferences = preferences.state.first()
    try {
      preferences.setDarkTheme(true)
      preferences.setReduceMotion(true)
      preferences.setShowCents(false)
      context.deleteDatabase(name)
      val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
      createVersionThreeSchema(legacy)
      legacy.execSQL("INSERT INTO categories VALUES ('alimentacao', 'Alimentação', '#000000')")
      legacy.execSQL(
          "INSERT INTO transactions VALUES ('legacy', 'manual-ledger', 'Padaria', 'Compra', -889, '2026-09-30', 'alimentacao', NULL, NULL, 0)"
      )
      legacy.version = 3
      legacy.close()

      val migrated = DemoDatabase.open(context, name)
      try {
        val transaction = migrated.dao().allTransactions().single()
        assertEquals("legacy", transaction.id)
        assertEquals(TransactionOrigin.MANUAL.name, transaction.origin)
        assertNull(transaction.importNoticeId)
        assertTrue(migrated.dao().allImportNotices().isEmpty())
        assertEquals(4, migrated.openHelper.writableDatabase.version)
        val restoredPreferences = preferences.state.first()
        assertTrue(restoredPreferences.darkTheme)
        assertTrue(restoredPreferences.reduceMotion)
        assertFalse(restoredPreferences.showCents)
      } finally {
        migrated.close()
      }
    } finally {
      context.deleteDatabase(name)
      preferences.setDarkTheme(previousPreferences.darkTheme)
      preferences.setReduceMotion(previousPreferences.reduceMotion)
      preferences.setShowCents(previousPreferences.showCents)
    }
  }

  @Test
  fun importedDraftIsSeparateUntilAcknowledgedCollisionConfirmationAndReplayCreatesOnlyOneRow() =
      runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DemoRepository(context)
        withTimeout(15_000) { repository.ready.first { it } }
        repository.resetDemoData()
        repository.enableImportSource(NotificationImportSource.RICO)
        val date = LocalDate.now().toString()
        val candidate = candidate("purchase-1", "Padaria de Seu Zé", -889, date)
        val capture = NotificationImportCapture(context)

        assertTrue(capture.capture(candidate))
        val draft = repository.importNotices.first().single()
        assertFalse(draft.possibleDuplicate)
        assertEquals("Estabelecimento a confirmar", draft.merchant)
        assertEquals(0, repository.transactions.first().size)
        repository.saveTransaction(
            Transaction(
                "manual-duplicate",
                "manual-ledger",
                "Padaria de Seu Zé",
                "Compra manual",
                -889,
                date,
                "alimentacao",
            ),
        )

        val confirmation =
            ImportNoticeConfirmation(
                merchant = "Padaria de Seu Zé",
                description = "Compra revisada",
                amountCents = -889,
                date = date,
                categoryId = "alimentacao",
            )
        assertNull(repository.confirmImportNotice(draft.id, confirmation))
        assertTrue(repository.importNotices.first().single().possibleDuplicate)
        assertEquals(1, repository.transactions.first().size)

        val firstConfirmation =
            repository.confirmImportNotice(
                draft.id,
                confirmation.copy(acceptPossibleDuplicate = true),
            )
        val replayConfirmation = repository.confirmImportNotice(draft.id, confirmation)

        assertEquals(firstConfirmation, replayConfirmation)
        val imported =
            repository.transactions.first().filter {
              it.origin == TransactionOrigin.NOTIFICATION_IMPORT
            }
        assertEquals(1, imported.size)
        assertEquals(draft.id, imported.single().importNoticeId)
        assertEquals(
            1,
            repository.importNotices.first().count {
              it.confirmedTransactionId == firstConfirmation
            },
        )
        repository.saveTransaction(imported.single().copy(merchant = "Padaria revisada"))
        val editedImport =
            repository.transactions.first().single {
              it.origin == TransactionOrigin.NOTIFICATION_IMPORT
            }
        assertEquals("Padaria revisada", editedImport.merchant)
        assertEquals(draft.id, editedImport.importNoticeId)
        try {
          repository.saveTransaction(editedImport.copy(amountCents = 889, categoryId = "receita"))
          fail("Uma despesa importada não pode se tornar uma entrada ao editar")
        } catch (_: IllegalArgumentException) {
          // Expected: source kind remains authoritative after the first confirmation.
        }
        assertEquals(
            -889L,
            repository.transactions.first().single { it.id == firstConfirmation }.amountCents,
        )

        val importedId = requireNotNull(firstConfirmation)
        repository.deleteTransaction(importedId)
        val reopened = repository.importNotices.first().single()
        assertEquals(ImportNoticeStatus.PENDING, reopened.status)
        assertNull(reopened.confirmedTransactionId)
        assertFalse(capture.capture(candidate))

        val reconfirmedId =
            repository.confirmImportNotice(
                reopened.id,
                confirmation.copy(acceptPossibleDuplicate = true),
            )
        assertEquals(importedId, reconfirmedId)
        assertEquals(
            importedId,
            repository.transactions
                .first()
                .single { it.origin == TransactionOrigin.NOTIFICATION_IMPORT }
                .id,
        )
        assertEquals(
            importedId,
            repository.importNotices.first().single().confirmedTransactionId,
        )

        repository.resetDemoData()
        assertTrue(repository.transactions.first().isEmpty())
        assertTrue(repository.importNotices.first().isEmpty())
        assertNull(repository.enabledImportSource.first())
      }

  @Test
  fun suffixMismatchAndWrongExpenseSignCannotReachTheLedger() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val repository = DemoRepository(context)
    withTimeout(15_000) { repository.ready.first { it } }
    repository.resetDemoData()
    repository.enableImportSource(NotificationImportSource.RICO)
    repository.setImportFeedSuffix("1234")
    val date = LocalDate.now().toString()
    val capture = NotificationImportCapture(context)
    val candidate = candidate("suffix-1", "qualquer", -889, date)

    assertFalse(capture.capture(candidate))
    assertTrue(capture.capture(candidate.copy(feedSuffix = "1234")))
    val draft = repository.importNotices.first().single()
    try {
      repository.confirmImportNotice(
          draft.id,
          ImportNoticeConfirmation(
              merchant = "Padaria de Seu Zé",
              description = "Valor com sinal errado",
              amountCents = 889,
              date = date,
              categoryId = "receita",
          ),
      )
      fail("A despesa não pode ser confirmada como receita")
    } catch (_: IllegalArgumentException) {
      // Expected: the repository, not only the UI, protects the ledger sign.
    }
    assertTrue(repository.transactions.first().isEmpty())
    repository.resetDemoData()
  }

  @Test
  fun inFlightCaptureCannotSurviveDiscardOrReset() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val repository = DemoRepository(context)
    withTimeout(15_000) { repository.ready.first { it } }
    val captureReady = CompletableDeferred<Unit>()
    val releaseCapture = CompletableDeferred<Unit>()
    val capture =
        NotificationImportCapture(context) {
          captureReady.complete(Unit)
          releaseCapture.await()
        }

    repository.resetDemoData()
    repository.enableImportSource(NotificationImportSource.RICO)
    val discardCapture = async {
      capture.capture(candidate("discard-race", "qualquer", -889, LocalDate.now().toString()))
    }
    captureReady.await()
    val discard =
        async(start = CoroutineStart.UNDISPATCHED) {
          repository.disableImportSource()
          repository.clearPendingImports()
        }
    assertFalse(discard.isCompleted)
    releaseCapture.complete(Unit)
    assertTrue(discardCapture.await())
    discard.await()
    assertNull(repository.enabledImportSource.first())
    assertTrue(repository.importNotices.first().isEmpty())

    val resetReady = CompletableDeferred<Unit>()
    val releaseResetCapture = CompletableDeferred<Unit>()
    val resetCapture =
        NotificationImportCapture(context) {
          resetReady.complete(Unit)
          releaseResetCapture.await()
        }
    repository.enableImportSource(NotificationImportSource.RICO)
    val resetCaptureJob = async {
      resetCapture.capture(candidate("reset-race", "qualquer", -890, LocalDate.now().toString()))
    }
    resetReady.await()
    val reset = async(start = CoroutineStart.UNDISPATCHED) { repository.resetDemoData() }
    assertFalse(reset.isCompleted)
    releaseResetCapture.complete(Unit)
    assertTrue(resetCaptureJob.await())
    reset.await()
    assertNull(repository.enabledImportSource.first())
    assertTrue(repository.importNotices.first().isEmpty())
    assertTrue(repository.transactions.first().isEmpty())
  }

  private fun candidate(
      eventKey: String,
      merchant: String,
      amountCents: Long,
      date: String,
  ): NotificationImportCandidate =
      NotificationImportCandidate(
          source = NotificationImportSource.RICO,
          sourceEventHash =
              NotificationImportFingerprints.sourceEvent(
                  NotificationImportSource.RICO,
                  eventKey,
                  "Compra aprovada R$ 8,89",
                  Instant.parse("2026-09-30T14:00:00Z"),
              ),
          fingerprintHash =
              NotificationImportFingerprints.notice(
                  NotificationImportSource.RICO,
                  ImportNoticeKind.EXPENSE,
                  "Estabelecimento a confirmar",
                  amountCents,
                  date,
              ),
          merchant = merchant,
          description = "Compra aprovada pela Rico",
          amountCents = amountCents,
          date = date,
          kind = ImportNoticeKind.EXPENSE,
          observedAt = Instant.now().toString(),
      )

  private fun createVersionThreeSchema(db: android.database.sqlite.SQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE accounts (id TEXT NOT NULL, name TEXT NOT NULL, kind TEXT NOT NULL, balanceCents INTEGER NOT NULL, PRIMARY KEY(id))"
    )
    db.execSQL(
        "CREATE TABLE transactions (id TEXT NOT NULL, accountId TEXT NOT NULL, merchant TEXT NOT NULL, description TEXT NOT NULL, amountCents INTEGER NOT NULL, date TEXT NOT NULL, categoryId TEXT NOT NULL, installmentNumber INTEGER, installmentCount INTEGER, recurring INTEGER NOT NULL, PRIMARY KEY(id))"
    )
    db.execSQL(
        "CREATE TABLE categories (id TEXT NOT NULL, name TEXT NOT NULL, colorHex TEXT NOT NULL, PRIMARY KEY(id))"
    )
    db.execSQL(
        "CREATE TABLE budgets (categoryId TEXT NOT NULL, limitCents INTEGER NOT NULL, PRIMARY KEY(categoryId))"
    )
    db.execSQL(
        "CREATE TABLE alerts (id TEXT NOT NULL, title TEXT NOT NULL, explanation TEXT NOT NULL, severity TEXT NOT NULL, transactionIds TEXT NOT NULL, generatedDate TEXT NOT NULL, dismissed INTEGER NOT NULL, PRIMARY KEY(id))"
    )
    db.execSQL(
        "CREATE TABLE goals (id TEXT NOT NULL, name TEXT NOT NULL, targetCents INTEGER NOT NULL, savedCents INTEGER NOT NULL, PRIMARY KEY(id))"
    )
    db.execSQL(
        "CREATE TABLE assistant_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, createdAt TEXT NOT NULL, citationTransactionIds TEXT NOT NULL)"
    )
    db.execSQL(
        "CREATE TABLE reconciliation_decisions (caseId TEXT NOT NULL, kind TEXT NOT NULL, transactionIds TEXT NOT NULL, categoryId TEXT, PRIMARY KEY(caseId))"
    )
  }
}
