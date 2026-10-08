package com.example.allufinancas

import android.content.Context
import com.example.allufinancas.data.DemoDatabase
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.Transaction
import com.example.allufinancas.data.TransactionRow
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal suspend fun seedLocalLedger(repository: DemoRepository, context: Context) {
  repository.resetDemoData()
  repository.setDarkTheme(false)
  repository.setReduceMotion(false)
  repository.setShowCents(true)
  DemoDatabase.open(context)
      .dao()
      .putTransactions(LocalLedgerFixtures.transactions.map(Transaction::toRow))
  LocalLedgerFixtures.budgets.forEach { repository.updateBudget(it.categoryId, it.limitCents) }
}

private fun Transaction.toRow() =
    TransactionRow(
        id,
        "manual-ledger",
        merchant,
        description,
        amountCents,
        date,
        categoryId,
        installmentNumber,
        installmentCount,
        recurring,
    )

internal object LocalLedgerFixtures {
  private val currentMonth = YearMonth.now()
  val MONTH = currentMonth.toString()
  val PREVIOUS_MONTH = currentMonth.minusMonths(1).toString()
  val MONTH_LABEL = currentMonth.format(DateTimeFormatter.ofPattern("MM/yyyy"))
  val PREVIOUS_MONTH_LABEL =
      currentMonth.minusMonths(1).format(DateTimeFormatter.ofPattern("MM/yyyy"))

  fun date(day: Int): String = "$MONTH-${day.toString().padStart(2, '0')}"

  fun previousMonthDate(day: Int): String = "$PREVIOUS_MONTH-${day.toString().padStart(2, '0')}"

  fun displayDate(day: Int): String =
      "${day.toString().padStart(2, '0')}/${currentMonth.monthValue.toString().padStart(2, '0')}/${currentMonth.year}"

  val transactions =
      listOf(
          Transaction(
              "t01",
              "manual-ledger",
              "Empresa Exemplo",
              "Salário",
              820_000,
              date(5),
              "receita",
          ),
          Transaction(
              "t02",
              "manual-ledger",
              "Mercado Exemplo",
              "Compra da semana",
              -35_642,
              date(21),
              "alimentacao",
          ),
          Transaction(
              "t03",
              "manual-ledger",
              "Café Exemplo",
              "Café",
              -1_890,
              date(20),
              "alimentacao",
          ),
          Transaction(
              "t04",
              "manual-ledger",
              "Farmácia Exemplo",
              "Compra",
              -4_780,
              date(19),
              "saude",
          ),
          Transaction(
              "t05",
              "manual-ledger",
              "Farmácia Exemplo",
              "Compra",
              -4_780,
              date(19),
              "saude",
          ),
          Transaction(
              "t06",
              "manual-ledger",
              "Vídeo Exemplo",
              "Assinatura mensal",
              -3_990,
              date(17),
              "assinaturas",
              recurring = true,
          ),
          Transaction(
              "t07",
              "manual-ledger",
              "Transporte Exemplo",
              "Corrida",
              -2_890,
              date(15),
              "",
          ),
          Transaction(
              "t08",
              "manual-ledger",
              "Loja Exemplo",
              "Notebook",
              -12_990,
              date(12),
              "compras",
              3,
              10,
          ),
          Transaction(
              "t09",
              "manual-ledger",
              "Celular Exemplo",
              "Plano mensal",
              -5_490,
              date(10),
              "assinaturas",
              recurring = true,
          ),
          Transaction(
              "t10",
              "manual-ledger",
              "Mercado Exemplo",
              "Compra do mês",
              -28_010,
              date(8),
              "alimentacao",
          ),
          Transaction(
              "t11",
              "manual-ledger",
              "Moradia Exemplo",
              "Aluguel",
              -180_000,
              date(7),
              "casa",
              recurring = true,
          ),
          Transaction(
              "t12",
              "manual-ledger",
              "Loja Exemplo",
              "Compra",
              -7_250,
              previousMonthDate(26),
              "compras",
          ),
      )
  val budgets =
      listOf(
          com.example.allufinancas.data.Budget("alimentacao", 50_000),
          com.example.allufinancas.data.Budget("assinaturas", 12_000),
          com.example.allufinancas.data.Budget("transporte", 30_000),
      )
}
