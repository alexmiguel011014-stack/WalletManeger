package com.example.allufinancas.feature.transactions

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import java.time.Instant

@Composable internal fun Transactions(
    transactions: List<Transaction>, categories: List<Category>, settings: DisplayPreferences,
    materializedAt: Instant?, onCategory: (String, String) -> Unit,
    onTransactionClick: (String) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var fromDate by rememberSaveable { mutableStateOf("") }
    var throughDate by rememberSaveable { mutableStateOf("") }
    val months = transactions.map { it.date.take(7) }.distinct().sortedDescending()
    val filter = TransactionFilter(search, category, month, fromDate, throughDate)
    val filtered = filter.apply(transactions)
    val rangeEntered = fromDate.isNotBlank() || throughDate.isNotBlank()
    val rangeStart = TransactionFilter.parseDate(fromDate)
    val rangeEnd = TransactionFilter.parseDate(throughDate)
    val rangeProblem = when {
        !rangeEntered -> null
        month.isNotBlank() -> "Escolha um mês ou um intervalo, não os dois."
        rangeStart == null || rangeEnd == null -> "Informe as duas datas válidas no formato dd/MM/aaaa."
        rangeStart != null && rangeEnd != null && rangeStart > rangeEnd -> "A data inicial deve ser anterior ou igual à final."
        else -> null
    }
    val periodScope = when {
        rangeEntered && rangeProblem != null -> "Intervalo inválido"
        rangeEntered -> "Período: $fromDate – $throughDate"
        month.isNotBlank() -> "Período: $month"
        else -> "Todos os movimentos"
    }
    val scope = buildList {
        add(periodScope)
        search.trim().takeIf { it.isNotEmpty() }?.let { query ->
            add("Busca: ${query.take(32)}${if (query.length > 32) "…" else ""}")
        }
        category.takeIf { it.isNotBlank() }?.let {
            add("Categoria: ${categories.firstOrNull { item -> item.id == category }?.name ?: "não disponível"}")
        }
    }.joinToString(" · ")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { DemoStrip(scope, materializedAt) }
        item { OutlinedTextField(search, { search = it }, label = { Text("Buscar estabelecimento") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.testTag("transaction-categories"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item { OutlinedButton(onClick = { category = "" }) { Text("Todas") } }
                items(categories) { item -> OutlinedButton(onClick = { category = item.id }) { Text(item.name) } }
            }
        }
        item {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { OutlinedButton(onClick = { month = ""; fromDate = ""; throughDate = "" }) { Text("Todo período") } }
                items(months) { item -> OutlinedButton(onClick = { month = item; fromDate = ""; throughDate = "" }) { Text(item) } }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    fromDate,
                    { fromDate = it; month = "" },
                    label = { Text("De · dd/MM/aaaa") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    throughDate,
                    { throughDate = it; month = "" },
                    label = { Text("Até · dd/MM/aaaa") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            TextButton(onClick = { search = ""; category = ""; month = ""; fromDate = ""; throughDate = "" }) {
                Text("Limpar filtros")
            }
        }
        if (filtered.isEmpty()) item { Panel { Text(rangeProblem ?: "Nenhum movimento corresponde aos filtros.") } }
        else item { Panel { filtered.forEachIndexed { index, transaction ->
            if (index > 0) HorizontalDivider()
            TransactionLine(transaction, categories, settings.showCents) { onTransactionClick(transaction.id) }
        } } }
    }
}

internal fun transactionById(transactions: List<Transaction>, id: String): Transaction? =
    transactions.firstOrNull { it.id == id }

@Composable internal fun TransactionDetail(
    transaction: Transaction?, categories: List<Category>, settings: DisplayPreferences,
    onBack: () -> Unit, onCategory: (String, String) -> Unit,
    materializedAt: Instant? = null,
) {
    if (transaction == null) {
        EmptyState(
            "Lançamento não encontrado",
            "Este movimento não está disponível no extrato atual.",
            actionLabel = "Voltar ao extrato",
            onAction = onBack,
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
        item {
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoStrip("Detalhe · ${transaction.merchant}", materializedAt)
                    Text("Estabelecimento: " + transaction.merchant, fontWeight = FontWeight.SemiBold)
                    Text("Valor: " + brl(transaction.amountCents, settings.showCents), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Data: " + transaction.date)
                    Text("Categoria: " + (categories.firstOrNull { it.id == transaction.categoryId }?.name ?: "Sem categoria"))
                    if (transaction.categoryId != transaction.sourceCategoryId) {
                        Text("Categoria da origem: " + (categories.firstOrNull { it.id == transaction.sourceCategoryId }?.name ?: "Sem categoria") + " · ajuste local reversível")
                    }
                    Text("Descrição: " + transaction.description)
                    if (transaction.installmentNumber != null && transaction.installmentCount != null) Text("Parcela " + transaction.installmentNumber + " de " + transaction.installmentCount)
                    Text("Corrigir categoria", fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { option ->
                            OutlinedButton(onClick = { onCategory(transaction.id, option.id) }) { Text(option.name) }
                        }
                    }
                }
            }
        }
    }
}
