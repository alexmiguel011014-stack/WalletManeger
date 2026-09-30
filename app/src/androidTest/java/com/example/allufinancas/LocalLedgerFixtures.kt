package com.example.allufinancas

import android.content.Context
import com.example.allufinancas.data.DemoDatabase
import com.example.allufinancas.data.DemoRepository
import com.example.allufinancas.data.Transaction
import com.example.allufinancas.data.TransactionRow

internal suspend fun seedLocalLedger(repository: DemoRepository, context: Context) {
    repository.resetDemoData()
    DemoDatabase.open(context).dao().putTransactions(LocalLedgerFixtures.transactions.map(Transaction::toRow))
    LocalLedgerFixtures.budgets.forEach { repository.updateBudget(it.categoryId, it.limitCents) }
}

private fun Transaction.toRow() = TransactionRow(
    id, "manual-ledger", merchant, description, amountCents, date, categoryId,
    installmentNumber, installmentCount, recurring,
)

internal object LocalLedgerFixtures {
    const val MONTH = "2026-09"

    val transactions = listOf(
        Transaction("t01", "manual-ledger", "Empresa Exemplo", "Salário", 820_000, "2026-09-05", "receita"),
        Transaction("t02", "manual-ledger", "Mercado Exemplo", "Compra da semana", -35_642, "2026-09-21", "alimentacao"),
        Transaction("t03", "manual-ledger", "Café Exemplo", "Café", -1_890, "2026-09-20", "alimentacao"),
        Transaction("t04", "manual-ledger", "Farmácia Exemplo", "Compra", -4_780, "2026-09-19", "saude"),
        Transaction("t05", "manual-ledger", "Farmácia Exemplo", "Compra", -4_780, "2026-09-19", "saude"),
        Transaction("t06", "manual-ledger", "Vídeo Exemplo", "Assinatura mensal", -3_990, "2026-09-17", "assinaturas", recurring = true),
        Transaction("t07", "manual-ledger", "Transporte Exemplo", "Corrida", -2_890, "2026-09-15", ""),
        Transaction("t08", "manual-ledger", "Loja Exemplo", "Notebook", -12_990, "2026-09-12", "compras", 3, 10),
        Transaction("t09", "manual-ledger", "Celular Exemplo", "Plano mensal", -5_490, "2026-09-10", "assinaturas", recurring = true),
        Transaction("t10", "manual-ledger", "Mercado Exemplo", "Compra do mês", -28_010, "2026-09-08", "alimentacao"),
        Transaction("t11", "manual-ledger", "Moradia Exemplo", "Aluguel", -180_000, "2026-09-07", "casa", recurring = true),
        Transaction("t12", "manual-ledger", "Loja Exemplo", "Compra", -7_250, "2026-08-26", "compras"),
    )
    val budgets = listOf(
        com.example.allufinancas.data.Budget("alimentacao", 50_000),
        com.example.allufinancas.data.Budget("assinaturas", 12_000),
        com.example.allufinancas.data.Budget("transporte", 30_000),
    )
}
