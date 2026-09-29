package com.example.allufinancas.feature.budgets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.allufinancas.R
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*
import java.math.BigDecimal
import java.time.Instant

internal object BudgetAmount {
    private val brazilianAmount = Regex("""(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d{1,2})?""")
    internal const val maximumCents = 1_000_000_000_000L

    fun parseCents(input: String): Long? {
        val value = input.trim()
        if (!brazilianAmount.matches(value)) return null
        return runCatching {
            BigDecimal(value.replace(".", "").replace(',', '.'))
                .movePointRight(2).longValueExact()
        }.getOrNull()?.takeIf { it in 1..maximumCents }
    }
}

@Composable internal fun Budgets(
    transactions: List<Transaction>, categories: List<Category>, budgets: List<Budget>,
    goals: List<Goal>, settings: DisplayPreferences, materializedAt: Instant?, onUpdate: (String, Long) -> Unit,
    onRestoreDemo: () -> Unit,
) {
    var editingCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var scenarioCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var scenarioAmount by rememberSaveable { mutableStateOf("") }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val editing = budgets.firstOrNull { it.categoryId == editingCategoryId }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { DemoStrip("Limites e metas · Setembro/2026", materializedAt) }
        item { Text("Planeje sem perder a origem dos números.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            BudgetScenarioPanel(
                transactions = transactions,
                categories = categories,
                budgets = budgets,
                showCents = settings.showCents,
                selectedCategoryId = scenarioCategoryId,
                amount = scenarioAmount,
                onCategorySelected = { scenarioCategoryId = it; scenarioAmount = "" },
                onAmountChange = { scenarioAmount = it },
                onClear = { scenarioCategoryId = null; scenarioAmount = "" },
            )
        }
        if (budgets.isEmpty()) item { Panel { Text("Nenhum limite de exemplo disponível.") } }
        items(budgets) { budget ->
            val category = categories.firstOrNull { it.id == budget.categoryId }
            val spent = DemoAnalysis.categorySpentCents(transactions, budget.categoryId, DemoSeed.MONTH)
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category?.name ?: "Categoria", fontWeight = FontWeight.Bold)
                    StatusPill(
                        label = if (spent > budget.limitCents) "Acima do limite" else "Dentro do limite",
                        status = if (spent > budget.limitCents) AlluStatus.WARNING else AlluStatus.SUCCESS,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(brl(spent, settings.showCents) + " usados de " + brl(budget.limitCents, settings.showCents))
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { (spent.toFloat() / budget.limitCents.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Despesas fictícias de setembro/2026; sem previsão de gastos futuros.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                TextButton(onClick = { editingCategoryId = budget.categoryId; amount = java.math.BigDecimal.valueOf(budget.limitCents, 2).toPlainString().replace('.', ',') }) { Text("Alterar limite") }
            }
        }
        if (goals.isNotEmpty()) item { Heading("Metas de exemplo") }
        items(goals) { goal ->
            Panel {
                Text(goal.name, fontWeight = FontWeight.Bold)
                Text(brl(goal.savedCents, settings.showCents) + " de " + brl(goal.targetCents, settings.showCents))
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(progress = { (goal.savedCents.toFloat() / goal.targetCents.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            Panel {
                Text("Reiniciar demonstração", fontWeight = FontWeight.Bold)
                Text("Restaura TODOS os dados fictícios locais para os exemplos iniciais.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { confirmRestore = true }) { Text("Restaurar dados fictícios") }
            }
        }
    }
    editing?.let { budget ->
        val parsed = BudgetAmount.parseCents(amount)
        AlertDialog(
            onDismissRequest = { editingCategoryId = null },
            title = { Text("Alterar limite fictício") },
            text = {
                Column {
                    Text("Limite aplicado aos gastos fictícios de setembro/2026.")
                    OutlinedTextField(amount, { amount = it }, label = { Text("Valor em reais") }, singleLine = true)
                    if (parsed == null) Text("Informe um valor positivo válido, por exemplo 1.000,00.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = parsed != null && parsed > 0, onClick = {
                    parsed?.let { onUpdate(budget.categoryId, it) }
                    editingCategoryId = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editingCategoryId = null }) { Text("Cancelar") } },
        )
    }
    ConfirmationSheet(
        visible = confirmRestore,
        title = "Restaurar TODOS os dados fictícios?",
        message = "Esta ação substitui todos os dados fictícios locais — contas, lançamentos, limites, metas, categorias, avisos e conversa — pelos exemplos iniciais.",
        confirmLabel = "Restaurar tudo",
        onConfirm = onRestoreDemo,
        onDismiss = { confirmRestore = false },
    )
}

@Composable
private fun BudgetScenarioPanel(
    transactions: List<Transaction>,
    categories: List<Category>,
    budgets: List<Budget>,
    showCents: Boolean,
    selectedCategoryId: String?,
    amount: String,
    onCategorySelected: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBudget = budgets.firstOrNull { it.categoryId == selectedCategoryId }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val spentCents = selectedBudget?.let {
        DemoAnalysis.categorySpentCents(transactions, it.categoryId, DemoSeed.MONTH)
    }
    val proposedCents = BudgetAmount.parseCents(amount)
    val scenario = if (selectedBudget != null && spentCents != null && proposedCents != null) {
        calculateBudgetScenario(selectedBudget.limitCents, spentCents, proposedCents)
    } else null

    Panel {
        Text("E se eu mudasse o limite?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Simulação — não altera seu limite", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(
            "Compare outro limite com as despesas já registradas. Não é uma previsão de gastos futuros.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        Text("Categoria", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = budgets.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedCategory?.name ?: "Escolha uma categoria", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                budgets.forEach { budget ->
                    val name = categories.firstOrNull { it.id == budget.categoryId }?.name ?: "Categoria"
                    DropdownMenuItem(
                        modifier = Modifier.semantics { contentDescription = "Selecionar categoria $name" },
                        text = { Text(name) },
                        onClick = { onCategorySelected(budget.categoryId); expanded = false },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            enabled = selectedBudget != null,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Limite hipotético em reais") },
            placeholder = { Text("Ex.: 1.000,00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(8.dp))
        when {
            selectedBudget == null -> Text(
                if (budgets.isEmpty()) "Nenhuma categoria com limite disponível para simular."
                else "Escolha uma categoria para calcular o cenário.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            amount.isBlank() -> Text(
                "Digite um limite positivo para ver a conta com o gasto de setembro/2026.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            proposedCents == null -> Text(
                "Informe um valor positivo válido até R$ 10.000.000.000,00.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            scenario == null -> Text(
                "Não foi possível calcular com segurança; confira os valores registrados.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            else -> {
                Text(
                    "Limite hipotético: ${brl(scenario.hypotheticalLimitCents, showCents)} − " +
                        "gasto registrado em set/2026: ${brl(scenario.spentCents, showCents)} = " +
                        "saldo hipotético: ${brl(scenario.hypotheticalBalanceCents, showCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val difference = scenario.limitDifferenceCents
                val comparison = when {
                    difference > 0 -> "${brl(difference, showCents)} a mais"
                    difference < 0 -> "${brl(-difference, showCents)} a menos"
                    else -> "igual ao limite atual"
                }
                Text(
                    "Diferença ante limite atual: $comparison (${selectedCategory?.name ?: "Categoria"}).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (scenario.hypotheticalBalanceCents < 0) {
                    Text(
                        "O gasto já registrado supera o limite hipotético.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClear) { Text("Limpar simulação") }
        }
    }
}
