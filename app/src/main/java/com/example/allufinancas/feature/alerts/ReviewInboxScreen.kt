package com.example.allufinancas.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.Category
import com.example.allufinancas.data.ReconciliationCase
import com.example.allufinancas.data.ReviewKind
import com.example.allufinancas.data.ReviewStrength
import com.example.allufinancas.data.Transaction
import com.example.allufinancas.data.TransactionCategoryRules
import com.example.allufinancas.designsystem.DemoStrip
import com.example.allufinancas.designsystem.EmptyState
import com.example.allufinancas.designsystem.Panel
import com.example.allufinancas.designsystem.AlluTokens
import java.time.Instant
import java.time.YearMonth

@Composable
internal fun ReviewInbox(
    cases: List<ReconciliationCase>,
    transactions: List<Transaction>,
    categories: List<Category>,
    materializedAt: Instant?,
    onDecide: (String) -> Unit,
    onChooseCategory: (String, String) -> Unit,
    onReopen: (String) -> Unit,
) {
    var showDecided by rememberSaveable { mutableStateOf(false) }
    val visible = cases.filter { it.decided == showDecided }
    val transactionsById = transactions.associateBy { it.id }
    val month = YearMonth.now().toString()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DemoStrip("Revisão · regras locais · período $month", materializedAt) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showDecided, onClick = { showDecided = false }, label = { Text("Pendentes (${cases.count { !it.decided }})") })
                FilterChip(selected = showDecided, onClick = { showDecided = true }, label = { Text("Decididos (${cases.count { it.decided }})") })
            }
        }
        if (visible.isEmpty()) item {
            EmptyState(
                title = if (showDecided) "Nenhuma decisão ainda" else "Tudo revisado",
                message = if (showDecided) "As decisões que você tomar aparecerão aqui e poderão ser desfeitas." else "Não há itens pendentes nos lançamentos disponíveis.",
            )
        }
        itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.sm)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Regra local: ${item.id}", style = MaterialTheme.typography.labelMedium)
                    Text(item.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Força qualitativa: ${if (item.strength == ReviewStrength.MODERATE) "moderada" else "limitada"} · Regra local, não conclusão sobre fraude.",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    item.transactionIds.forEach { id ->
                        val tx = transactionsById[id]
                        if (tx != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(AlluTokens.Radius.card),
                                modifier = Modifier.fillMaxWidth().semantics {
                                    contentDescription = "Lançamento $id, ${tx.merchant}, ${tx.date}, ${brl(tx.amountCents)}"
                                },
                            ) {
                                Column(Modifier.padding(AlluTokens.Spacing.sm), verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.xxs)) {
                                    Text("ID $id · ${tx.merchant}", fontWeight = FontWeight.SemiBold)
                                    Text("Data: ${tx.date} · Valor: ${brl(tx.amountCents)}")
                                }
                            }
                        }
                    }
                    if (showDecided) {
                        if (item.kind == ReviewKind.UNCATEGORIZED) {
                            val name = categories.firstOrNull { it.id == item.selectedCategoryId }?.name ?: "indisponível"
                            Text("Categoria local escolhida: $name")
                        } else {
                            Text("Conferido por você. Os lançamentos de origem continuam intactos.")
                        }
                        OutlinedButton(onClick = { onReopen(item.id) }) { Text("Desfazer e reabrir") }
                    } else when (item.kind) {
                        ReviewKind.UNCATEGORIZED -> {
                            Text("Escolha uma categoria para este lançamento:")
                            val transaction = item.transactionIds.singleOrNull()?.let(transactionsById::get)
                            val compatibleCategories = transaction?.let {
                                TransactionCategoryRules.compatible(categories, it.amountCents)
                            }.orEmpty()
                            Column(verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.xs)) {
                                compatibleCategories.chunked(2).forEach { rowCategories ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.xs)) {
                                        rowCategories.forEach { category ->
                                            OutlinedButton(
                                                onClick = { onChooseCategory(item.transactionIds.single(), category.id) },
                                                modifier = Modifier.weight(1f).semantics { contentDescription = "Usar categoria ${category.name}" },
                                            ) { Text(category.name, style = MaterialTheme.typography.labelSmall) }
                                        }
                                        if (rowCategories.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        ReviewKind.POSSIBLE_DUPLICATE -> Button(onClick = { onDecide(item.id) }) {
                            Text("Conferir e manter os dois")
                        }
                        ReviewKind.UNUSUAL_AMOUNT -> Button(onClick = { onDecide(item.id) }) {
                            Text("Marcar como conferido")
                        }
                    }
                }
            }
        }
        item { Text("Ajustes e decisões ficam neste dispositivo e podem ser desfeitos. Eles não alteram valores nem datas.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) }
    }
}
