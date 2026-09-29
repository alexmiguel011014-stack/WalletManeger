package com.example.allufinancas.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.migration.Migration
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "accounts")
data class AccountRow(@PrimaryKey val id: String, val name: String, val kind: String, val balanceCents: Long)

@Entity(tableName = "transactions")
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
data class GoalRow(@PrimaryKey val id: String, val name: String, val targetCents: Long, val savedCents: Long)

@Entity(tableName = "assistant_messages")
data class MessageRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val createdAt: String,
    val citationTransactionIds: List<String>,
)

@Entity(tableName = "reconciliation_decisions")
data class ReconciliationDecisionRow(
    @PrimaryKey val caseId: String,
    val kind: String,
    val transactionIds: List<String>,
    val categoryId: String?,
)

class DemoConverters {
    @TypeConverter fun idsToString(ids: List<String>): String = ids.joinToString("|")
    @TypeConverter fun stringToIds(raw: String): List<String> = raw.takeIf { it.isNotEmpty() }?.split('|') ?: emptyList()
}

@Dao
interface DemoDao {
    @Query("SELECT * FROM accounts ORDER BY id") fun accounts(): Flow<List<AccountRow>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun transactions(): Flow<List<TransactionRow>>
    @Query("SELECT * FROM categories ORDER BY name") fun categories(): Flow<List<CategoryRow>>
    @Query("SELECT * FROM budgets ORDER BY categoryId") fun budgets(): Flow<List<BudgetRow>>
    @Query("SELECT * FROM alerts ORDER BY generatedDate DESC, id") fun alerts(): Flow<List<AlertRow>>
    @Query("SELECT * FROM goals ORDER BY id") fun goals(): Flow<List<GoalRow>>
    @Query("SELECT * FROM assistant_messages ORDER BY id") fun messages(): Flow<List<MessageRow>>
    @Query("SELECT * FROM reconciliation_decisions ORDER BY caseId") fun reconciliationDecisions(): Flow<List<ReconciliationDecisionRow>>

    @Query("SELECT COUNT(*) FROM accounts") suspend fun accountCount(): Int
    @Query("SELECT * FROM transactions") suspend fun allTransactions(): List<TransactionRow>
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<CategoryRow>
    @Query("SELECT * FROM budgets") suspend fun allBudgets(): List<BudgetRow>
    @Query("SELECT * FROM alerts") suspend fun allAlerts(): List<AlertRow>
    @Query("SELECT * FROM reconciliation_decisions") suspend fun allReconciliationDecisions(): List<ReconciliationDecisionRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAccounts(rows: List<AccountRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTransactions(rows: List<TransactionRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCategories(rows: List<CategoryRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudgets(rows: List<BudgetRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAlerts(rows: List<AlertRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoals(rows: List<GoalRow>)
    @Insert suspend fun putMessage(row: MessageRow)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putReconciliationDecision(row: ReconciliationDecisionRow)

    @Query("DELETE FROM reconciliation_decisions WHERE caseId = :caseId") suspend fun deleteReconciliationDecision(caseId: String): Int
    @Query("UPDATE alerts SET dismissed = :dismissed WHERE id = :id") suspend fun setAlertDismissed(id: String, dismissed: Boolean): Int
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM alerts") suspend fun clearAlerts()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM assistant_messages") suspend fun clearMessages()
    @Query("DELETE FROM reconciliation_decisions") suspend fun clearReconciliationDecisions()
}

@Database(
    entities = [AccountRow::class, TransactionRow::class, CategoryRow::class, BudgetRow::class, AlertRow::class, GoalRow::class, MessageRow::class, ReconciliationDecisionRow::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DemoConverters::class)
abstract class DemoDatabase : RoomDatabase() {
    abstract fun dao(): DemoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS reconciliation_decisions (
                    caseId TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    transactionIds TEXT NOT NULL,
                    categoryId TEXT,
                    PRIMARY KEY(caseId)
                )""".trimIndent())
            }
        }

        @Volatile private var instance: DemoDatabase? = null
        fun open(context: Context, name: String = "allu_demo.db"): DemoDatabase {
            if (name != "allu_demo.db") {
                return Room.databaseBuilder(context.applicationContext, DemoDatabase::class.java, name)
                    .addMigrations(MIGRATION_1_2).build()
            }
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, DemoDatabase::class.java, name)
                    .addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
