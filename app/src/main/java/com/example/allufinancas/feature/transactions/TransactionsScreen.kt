package com.example.allufinancas.feature.transactions

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.feature.budgets.BudgetAmount
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Composable
internal fun Transactions(
    transactions: List<Transaction>,
    categories: List<Category>,
    settings: DisplayPreferences,
    materializedAt: Instant?,
    pendingImportCount: Int,
    onSave: (Transaction) -> Unit,
    onOpenImports: () -> Unit,
    onTransactionClick: (String) -> Unit,
) {
  var adding by rememberSaveable { mutableStateOf(false) }
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
  val rangeProblem =
      when {
        !rangeEntered -> null
        month.isNotBlank() -> "Escolha um mês ou um intervalo, não os dois."
        rangeStart == null || rangeEnd == null ->
            "Informe as duas datas válidas no formato dd/MM/aaaa."
        rangeStart != null && rangeEnd != null && rangeStart > rangeEnd ->
            "A data inicial deve ser anterior ou igual à final."
        else -> null
      }
  val periodScope =
      when {
        rangeEntered && rangeProblem != null -> "Intervalo inválido"
        rangeEntered -> "Período: $fromDate – $throughDate"
        month.isNotBlank() -> "Período: $month"
        else -> "Todos os movimentos"
      }
  val scope =
      buildList {
            add(periodScope)
            search
                .trim()
                .takeIf { it.isNotEmpty() }
                ?.let { query ->
                  add("Busca: ${query.take(32)}${if (query.length > 32) "…" else ""}")
                }
            category
                .takeIf { it.isNotBlank() }
                ?.let {
                  add(
                      "Categoria: ${categories.firstOrNull { item -> item.id == category }?.name ?: "não disponível"}"
                  )
                }
          }
          .joinToString(" · ")
  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { DemoStrip(scope, materializedAt) }
    item {
      Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Adicionar lançamento")
      }
    }
    item {
      OutlinedButton(onClick = onOpenImports, modifier = Modifier.fillMaxWidth()) {
        Text("Importações pendentes${if (pendingImportCount > 0) " ($pendingImportCount)" else ""}")
      }
    }
    item {
      OutlinedTextField(
          search,
          { search = it },
          label = { Text("Buscar estabelecimento") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
    }
    item {
      androidx.compose.foundation.lazy.LazyRow(
          modifier = Modifier.testTag("transaction-categories"),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        item { OutlinedButton(onClick = { category = "" }) { Text("Todas") } }
        items(categories) { item ->
          OutlinedButton(onClick = { category = item.id }) { Text(item.name) }
        }
      }
    }
    item {
      androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
          OutlinedButton(
              onClick = {
                month = ""
                fromDate = ""
                throughDate = ""
              }
          ) {
            Text("Todo período")
          }
        }
        items(months) { item ->
          OutlinedButton(
              onClick = {
                month = item
                fromDate = ""
                throughDate = ""
              }
          ) {
            Text(item)
          }
        }
      }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            fromDate,
            {
              fromDate = it
              month = ""
            },
            label = { Text("De · dd/MM/aaaa") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            throughDate,
            {
              throughDate = it
              month = ""
            },
            label = { Text("Até · dd/MM/aaaa") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
      }
      TextButton(
          onClick = {
            search = ""
            category = ""
            month = ""
            fromDate = ""
            throughDate = ""
          }
      ) {
        Text("Limpar filtros")
      }
    }
    if (transactions.isEmpty())
        item {
          EmptyState(
              "Comece seu extrato",
              "Cadastre uma entrada ou saída para acompanhar seu saldo e seus gastos.",
              actionLabel = "Adicionar lançamento",
              onAction = { adding = true },
          )
        }
    else if (filtered.isEmpty())
        item { Panel { Text(rangeProblem ?: "Nenhum lançamento corresponde aos filtros.") } }
    else
        item {
          Panel {
            filtered.forEachIndexed { index, transaction ->
              if (index > 0) HorizontalDivider()
              TransactionLine(transaction, categories, settings.showCents) {
                onTransactionClick(transaction.id)
              }
            }
          }
        }
  }
  if (adding)
      TransactionEditor(null, categories, onDismiss = { adding = false }) {
        onSave(it)
        adding = false
      }
}

internal fun transactionById(transactions: List<Transaction>, id: String): Transaction? =
    transactions.firstOrNull {
      it.id == id
    }

@Composable
internal fun TransactionDetail(
    transaction: Transaction?,
    categories: List<Category>,
    settings: DisplayPreferences,
    onBack: () -> Unit,
    onCategory: (String, String) -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
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
  var editing by rememberSaveable { mutableStateOf(false) }
  var confirmingDelete by rememberSaveable { mutableStateOf(false) }
  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
  ) {
    item {
      Panel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          DemoStrip("Detalhe · ${transaction.merchant}", materializedAt)
          Text("Estabelecimento: " + transaction.merchant, fontWeight = FontWeight.SemiBold)
          Text(
              "Valor: " + brl(transaction.amountCents, settings.showCents),
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
          )
          Text("Data: " + transaction.date)
          Text(
              "Categoria: " +
                  (categories.firstOrNull { it.id == transaction.categoryId }?.name
                      ?: "Sem categoria")
          )
          if (transaction.origin == TransactionOrigin.NOTIFICATION_IMPORT) {
            Text("Origem: notificação local da Rico confirmada")
          }
          if (transaction.categoryId != transaction.sourceCategoryId) {
            Text(
                "Categoria da origem: " +
                    (categories.firstOrNull { it.id == transaction.sourceCategoryId }?.name
                        ?: "Sem categoria") +
                    " · ajuste local reversível"
            )
          }
          Text("Descrição: " + transaction.description)
          if (transaction.installmentNumber != null && transaction.installmentCount != null)
              Text(
                  "Parcela " + transaction.installmentNumber + " de " + transaction.installmentCount
              )
          Text("Corrigir categoria", fontWeight = FontWeight.Bold)
          androidx.compose.foundation.lazy.LazyRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(TransactionCategoryRules.compatible(categories, transaction.amountCents)) { option
              ->
              OutlinedButton(onClick = { onCategory(transaction.id, option.id) }) {
                Text(option.name)
              }
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { editing = true }) { Text("Editar lançamento") }
            TextButton(onClick = { confirmingDelete = true }) { Text("Excluir") }
          }
        }
      }
    }
  }
  if (editing)
      TransactionEditor(transaction, categories, onDismiss = { editing = false }) {
        onSave(it)
        editing = false
      }
  ConfirmationSheet(
      visible = confirmingDelete,
      title = "Excluir lançamento?",
      message =
          "${transaction.merchant} · ${brl(transaction.amountCents, settings.showCents)} será removido deste dispositivo.",
      confirmLabel = "Excluir",
      onConfirm = {
        onDelete(transaction.id)
        onBack()
      },
      onDismiss = { confirmingDelete = false },
  )
}

@Composable
private fun TransactionEditor(
    transaction: Transaction?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
) {
  val id = remember(transaction?.id) { transaction?.id ?: UUID.randomUUID().toString() }
  var merchant by rememberSaveable(id) { mutableStateOf(transaction?.merchant.orEmpty()) }
  var description by rememberSaveable(id) { mutableStateOf(transaction?.description.orEmpty()) }
  var amount by
      rememberSaveable(id) {
        mutableStateOf(
            transaction
                ?.amountCents
                ?.let {
                  "${kotlin.math.abs(it) / 100},${(kotlin.math.abs(it) % 100).toString().padStart(2, '0')}"
                }
                .orEmpty()
        )
      }
  var date by
      rememberSaveable(id) { mutableStateOf(transaction?.date ?: LocalDate.now().toString()) }
  val initialIncome = (transaction?.amountCents ?: -1L) > 0
  var income by rememberSaveable(id) { mutableStateOf(initialIncome) }
  val incomeCategoryId =
      categories.firstOrNull { it.id == TransactionCategoryRules.INCOME_CATEGORY_ID }?.id
  val expenseCategoryId =
      TransactionCategoryRules.compatible(categories, -1).firstOrNull()?.id.orEmpty()
  var categoryId by
      rememberSaveable(id) {
        val existingCategory = transaction?.let { current ->
          current.categoryId.takeIf {
            TransactionCategoryRules.isCompatible(it, current.amountCents)
          }
        }
        mutableStateOf(
            existingCategory ?: if (initialIncome) incomeCategoryId.orEmpty() else expenseCategoryId
        )
      }
  var recurring by
      rememberSaveable(id) { mutableStateOf(transaction?.recurring == true && !initialIncome) }
  var installmentNumber by
      rememberSaveable(id) { mutableStateOf(transaction?.installmentNumber?.toString().orEmpty()) }
  var installmentCount by
      rememberSaveable(id) { mutableStateOf(transaction?.installmentCount?.toString().orEmpty()) }
  var categoryMenu by remember { mutableStateOf(false) }
  val cents = BudgetAmount.parseCents(amount)
  val validDate = runCatching { LocalDate.parse(date).toString() == date }.getOrDefault(false)
  val validMerchant = merchant.trim().isNotEmpty() && merchant.trim().length <= 120
  val validDescription = description.trim().length <= 300
  val parsedInstallmentNumber = installmentNumber.toIntOrNull()
  val parsedInstallmentCount = installmentCount.toIntOrNull()
  val categoryOptions = TransactionCategoryRules.compatible(categories, if (income) 1 else -1)
  val hasInstallments = installmentNumber.isNotBlank() || installmentCount.isNotBlank()
  val validInstallments =
      !hasInstallments ||
          (parsedInstallmentNumber != null &&
              parsedInstallmentCount != null &&
              parsedInstallmentCount in 1..120 &&
              parsedInstallmentNumber in 1..parsedInstallmentCount)
  val valid =
      cents != null &&
          validDate &&
          validMerchant &&
          validDescription &&
          categoryId in categoryOptions.map { it.id } &&
          validInstallments

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(if (transaction == null) "Novo lançamento" else "Editar lançamento") },
      text = {
        Column(
            Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = income,
                onClick = {
                  income = true
                  incomeCategoryId?.let { categoryId = it }
                  recurring = false
                },
                label = { Text("Entrada") },
            )
            FilterChip(
                selected = !income,
                onClick = {
                  income = false
                  categoryId = expenseCategoryId
                },
                label = { Text("Saída") },
            )
          }
          OutlinedTextField(
              amount,
              { amount = it },
              label = { Text("Valor em reais") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          )
          OutlinedTextField(
              merchant,
              { merchant = it },
              label = { Text("Estabelecimento") },
              singleLine = true,
          )
          OutlinedTextField(
              description,
              { description = it },
              label = { Text("Descrição (opcional)") },
              singleLine = true,
          )
          OutlinedTextField(
              date,
              { date = it },
              label = { Text("Data · AAAA-MM-DD") },
              singleLine = true,
              isError = date.isNotBlank() && !validDate,
          )
          Box {
            OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
              Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Escolher categoria")
            }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
              categoryOptions.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                      categoryId = category.id
                      categoryMenu = false
                    },
                )
              }
            }
          }
          if (!income) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = recurring, onCheckedChange = { recurring = it })
              Text("Marcar como despesa recorrente")
            }
          }
          OutlinedTextField(
              installmentNumber,
              { installmentNumber = it.filter(Char::isDigit).take(3) },
              label = { Text("Parcela atual (opcional)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )
          OutlinedTextField(
              installmentCount,
              { installmentCount = it.filter(Char::isDigit).take(3) },
              label = { Text("Total de parcelas (opcional)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )
          if (cents == null)
              Text(
                  "Informe um valor positivo válido, por exemplo 125,50.",
                  color = MaterialTheme.colorScheme.error,
              )
          if (!validMerchant)
              Text(
                  "Informe um estabelecimento com até 120 caracteres.",
                  color = MaterialTheme.colorScheme.error,
              )
          if (!validDate)
              Text(
                  "Informe uma data existente no formato AAAA-MM-DD.",
                  color = MaterialTheme.colorScheme.error,
              )
          if (!validDescription)
              Text(
                  "A descrição aceita até 300 caracteres.",
                  color = MaterialTheme.colorScheme.error,
              )
          if (!validInstallments)
              Text(
                  "Informe a parcela atual e o total: 1 ≤ atual ≤ total ≤ 120.",
                  color = MaterialTheme.colorScheme.error,
              )
        }
      },
      confirmButton = {
        TextButton(
            enabled = valid,
            onClick = {
              cents?.let { value ->
                onSave(
                    Transaction(
                        id = id,
                        accountId = transaction?.accountId ?: "manual-ledger",
                        merchant = merchant.trim(),
                        description = description.trim(),
                        amountCents = if (income) value else -value,
                        date = date,
                        categoryId = categoryId,
                        installmentNumber = parsedInstallmentNumber.takeIf { hasInstallments },
                        installmentCount = parsedInstallmentCount.takeIf { hasInstallments },
                        recurring = !income && recurring,
                        origin = transaction?.origin ?: TransactionOrigin.MANUAL,
                        importNoticeId = transaction?.importNoticeId,
                    )
                )
              }
            },
        ) {
          Text("Salvar")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}
