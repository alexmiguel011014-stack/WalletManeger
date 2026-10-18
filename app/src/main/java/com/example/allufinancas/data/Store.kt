package com.example.allufinancas.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "accounts")
data class AccountRow(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val balanceCents: Long,
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["importNoticeId"], unique = true)],
)
data class TransactionRow(
    @PrimaryKey val id: String,
    val accountId: String,
    val merchant: String,
    val description: String,
    val amountCents: Long,
    val date: String,
    val categoryId: String,
    val installmentNumber: Int?,
    val installmentCount: Int?,
    val recurring: Boolean,
    @androidx.room.ColumnInfo(defaultValue = "'MANUAL'")
    val origin: String = TransactionOrigin.MANUAL.name,
    val importNoticeId: String? = null,
)

@Entity(tableName = "categories")
data class CategoryRow(@PrimaryKey val id: String, val name: String, val colorHex: String)

@Entity(tableName = "budgets")
data class BudgetRow(@PrimaryKey val categoryId: String, val limitCents: Long)

@Entity(tableName = "alerts")
data class AlertRow(
    @PrimaryKey val id: String,
    val title: String,
    val explanation: String,
    val severity: String,
    val transactionIds: List<String>,
    val generatedDate: String,
    val dismissed: Boolean,
)

@Entity(tableName = "goals")
data class GoalRow(
    @PrimaryKey val id: String,
    val name: String,
    val targetCents: Long,
    val savedCents: Long,
)

@Entity(tableName = "reconciliation_decisions")
data class ReconciliationDecisionRow(
    @PrimaryKey val caseId: String,
    val kind: String,
    val transactionIds: List<String>,
    val categoryId: String?,
)

/** Redacted local queue entries. Full notification payloads are never stored. */
@Entity(
    tableName = "import_notices",
    indices =
        [Index(value = ["sourceEventHash"], unique = true), Index(value = ["fingerprintHash"])],
)
data class ImportNoticeRow(
    @PrimaryKey val id: String,
    val source: String,
    val sourceEventHash: String,
    val fingerprintHash: String,
    val merchant: String,
    val description: String,
    val amountCents: Long,
    val date: String,
    val kind: String,
    val status: String,
    val possibleDuplicate: Boolean,
    val observedAt: String,
    val confirmedTransactionId: String?,
)

class DemoConverters {
  @TypeConverter fun idsToString(ids: List<String>): String = ids.joinToString("|")

  @TypeConverter
  fun stringToIds(raw: String): List<String> =
      raw.takeIf { it.isNotEmpty() }?.split('|') ?: emptyList()
}

@Dao
interface DemoDao {
  @Query("SELECT * FROM accounts ORDER BY id") fun accounts(): Flow<List<AccountRow>>

  @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
  fun transactions(): Flow<List<TransactionRow>>

  @Query("SELECT * FROM categories ORDER BY name") fun categories(): Flow<List<CategoryRow>>

  @Query("SELECT * FROM budgets ORDER BY categoryId") fun budgets(): Flow<List<BudgetRow>>

  @Query("SELECT * FROM alerts ORDER BY generatedDate DESC, id") fun alerts(): Flow<List<AlertRow>>

  @Query("SELECT * FROM goals ORDER BY id") fun goals(): Flow<List<GoalRow>>

  @Query("SELECT * FROM reconciliation_decisions ORDER BY caseId")
  fun reconciliationDecisions(): Flow<List<ReconciliationDecisionRow>>

  @Query("SELECT * FROM import_notices ORDER BY observedAt DESC, id DESC")
  fun importNotices(): Flow<List<ImportNoticeRow>>

  @Query("SELECT COUNT(*) FROM accounts") suspend fun accountCount(): Int

  @Query("SELECT COUNT(*) FROM categories") suspend fun categoryCount(): Int

  @Query("SELECT * FROM transactions") suspend fun allTransactions(): List<TransactionRow>

  @Query("SELECT * FROM categories") suspend fun allCategories(): List<CategoryRow>

  @Query("SELECT * FROM budgets") suspend fun allBudgets(): List<BudgetRow>

  @Query("SELECT * FROM alerts") suspend fun allAlerts(): List<AlertRow>

  @Query("SELECT * FROM reconciliation_decisions")
  suspend fun allReconciliationDecisions(): List<ReconciliationDecisionRow>

  @Query("SELECT * FROM import_notices") suspend fun allImportNotices(): List<ImportNoticeRow>

  @Query("SELECT * FROM import_notices WHERE id = :id LIMIT 1")
  suspend fun importNotice(id: String): ImportNoticeRow?

  @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
  suspend fun transaction(id: String): TransactionRow?

  @Query("SELECT id FROM transactions WHERE importNoticeId = :noticeId LIMIT 1")
  suspend fun transactionIdForImportNotice(noticeId: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAccounts(rows: List<AccountRow>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun putTransactions(rows: List<TransactionRow>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun putCategories(rows: List<CategoryRow>)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudgets(rows: List<BudgetRow>)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAlerts(rows: List<AlertRow>)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoals(rows: List<GoalRow>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun putReconciliationDecision(row: ReconciliationDecisionRow)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertImportNotice(row: ImportNoticeRow): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertImportedTransaction(row: TransactionRow): Long

  @Query("DELETE FROM reconciliation_decisions WHERE caseId = :caseId")
  suspend fun deleteReconciliationDecision(caseId: String): Int

  @Query(
      "DELETE FROM reconciliation_decisions WHERE kind IN ('POSSIBLE_DUPLICATE', 'UNUSUAL_AMOUNT')"
  )
  suspend fun clearRuleDecisions()

  @Query("UPDATE alerts SET dismissed = :dismissed WHERE id = :id")
  suspend fun setAlertDismissed(id: String, dismissed: Boolean): Int

  @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTransaction(id: String): Int

  @Query("DELETE FROM accounts") suspend fun clearAccounts()

  @Query("DELETE FROM transactions") suspend fun clearTransactions()

  @Query("DELETE FROM categories") suspend fun clearCategories()

  @Query("DELETE FROM budgets") suspend fun clearBudgets()

  @Query("DELETE FROM alerts") suspend fun clearAlerts()

  @Query("DELETE FROM goals") suspend fun clearGoals()

  @Query("DELETE FROM reconciliation_decisions") suspend fun clearReconciliationDecisions()

  @Query(
      "UPDATE import_notices SET status = :status, confirmedTransactionId = :confirmedTransactionId WHERE id = :id AND status = 'PENDING'"
  )
  suspend fun resolvePendingImportNotice(
      id: String,
      status: String,
      confirmedTransactionId: String? = null,
  ): Int

  @Query(
      "UPDATE import_notices SET status = 'PENDING', confirmedTransactionId = NULL WHERE id = :id AND status = 'CONFIRMED' AND confirmedTransactionId = :transactionId"
  )
  suspend fun reopenConfirmedImportNotice(id: String, transactionId: String): Int

  @Query("UPDATE import_notices SET possibleDuplicate = 1 WHERE id = :id AND status = 'PENDING'")
  suspend fun markImportNoticePossibleDuplicate(id: String): Int

  @Query("DELETE FROM import_notices WHERE status = 'PENDING'")
  suspend fun clearPendingImportNotices()

  @Query("DELETE FROM import_notices") suspend fun clearImportNotices()
}

@Database(
    entities =
        [
            AccountRow::class,
            TransactionRow::class,
            CategoryRow::class,
            BudgetRow::class,
            AlertRow::class,
            GoalRow::class,
            ReconciliationDecisionRow::class,
            ImportNoticeRow::class,
        ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(DemoConverters::class)
abstract class DemoDatabase : RoomDatabase() {
  abstract fun dao(): DemoDao

  companion object {
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS reconciliation_decisions (
                                    caseId TEXT NOT NULL,
                                    kind TEXT NOT NULL,
                                    transactionIds TEXT NOT NULL,
                                    categoryId TEXT,
                                    PRIMARY KEY(caseId)
                                )
                """
                    .trimIndent()
            )
          }
        }

    val MIGRATION_2_3 =
        object : Migration(2, 3) {
          override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                    "accounts",
                    "transactions",
                    "budgets",
                    "alerts",
                    "goals",
                    "assistant_messages",
                    "reconciliation_decisions",
                    "categories",
                )
                .forEach { db.execSQL("DELETE FROM $it") }
            DefaultCategories.all.forEach { category ->
              db.execSQL(
                  "INSERT INTO categories (id, name, colorHex) VALUES (?, ?, ?)",
                  arrayOf(category.id, category.name, category.colorHex),
              )
            }
          }
        }

    /** Keeps every v3 ledger row, while adding a separate owner-confirmed import queue. */
    val MIGRATION_3_4 =
        object : Migration(3, 4) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN origin TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN importNoticeId TEXT")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_importNoticeId ON transactions (importNoticeId)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS import_notices (
                                    id TEXT NOT NULL,
                                    source TEXT NOT NULL,
                                    sourceEventHash TEXT NOT NULL,
                                    fingerprintHash TEXT NOT NULL,
                                    merchant TEXT NOT NULL,
                                    description TEXT NOT NULL,
                                    amountCents INTEGER NOT NULL,
                                    date TEXT NOT NULL,
                                    kind TEXT NOT NULL,
                                    status TEXT NOT NULL,
                                    possibleDuplicate INTEGER NOT NULL,
                                    observedAt TEXT NOT NULL,
                                    confirmedTransactionId TEXT,
                                    PRIMARY KEY(id)
                                )
                """
                    .trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_import_notices_sourceEventHash ON import_notices (sourceEventHash)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_import_notices_fingerprintHash ON import_notices (fingerprintHash)"
            )
          }
        }

    /** Removes the retired local-assistant conversation without affecting financial records. */
    val MIGRATION_4_5 =
        object : Migration(4, 5) {
          override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS assistant_messages")
          }
        }

    @Volatile private var instance: DemoDatabase? = null

    fun open(context: Context, name: String = "allu_demo.db"): DemoDatabase {
      if (name != "allu_demo.db") {
        return Room.databaseBuilder(context.applicationContext, DemoDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
      }
      return instance
          ?: synchronized(this) {
            instance
                ?: Room.databaseBuilder(context.applicationContext, DemoDatabase::class.java, name)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
          }
    }
  }
}
