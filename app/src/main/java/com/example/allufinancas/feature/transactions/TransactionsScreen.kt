package com.example.allufinancas.feature.transactions

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.feature.budgets.BudgetAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
internal fun Transactions(
    transactions: List<Transaction>,
    categories: List<Category>,
    settings: DisplayPreferences,
    materializedAt: Instant?,
    pendingImportCount: Int,
    importSourceEnabled: Boolean,
    editorRequest: Int,
    onEditorRequestHandled: (Int) -> Unit,
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
  var handledEditorRequest by rememberSaveable { mutableIntStateOf(0) }
  LaunchedEffect(editorRequest) {
    if (editorRequest > handledEditorRequest) {
      adding = true
      handledEditorRequest = editorRequest
      onEditorRequestHandled(editorRequest)
    }
  }
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
        rangeStart > rangeEnd -> "A data inicial deve ser anterior ou igual à final."
        else -> null
      }
  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (pendingImportCount > 0)
        item {
          Panel {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Importações aguardam sua revisão", fontWeight = FontWeight.Bold)
              Text(
                  "$pendingImportCount lançamento(s) ainda não entram no extrato ou no saldo até você confirmar.",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(
                  onClick = onOpenImports,
                  modifier = Modifier.fillMaxWidth().testTag("transactions-imports-shortcut"),
              ) {
                Text("Revisar importações pendentes ($pendingImportCount)")
              }
            }
          }
        }
    item {
      Button(
          onClick = { adding = true },
          modifier = Modifier.fillMaxWidth().testTag("transactions-add"),
      ) {
        Text("Adicionar lançamento")
      }
    }
    if (importSourceEnabled && pendingImportCount == 0)
        item {
          OutlinedButton(
              onClick = onOpenImports,
              modifier = Modifier.fillMaxWidth().testTag("transactions-imports-shortcut"),
          ) {
            Text(
                if (pendingImportCount > 0) "Importações pendentes ($pendingImportCount)"
                else "Importações pendentes"
            )
          }
        }
    if (transactions.isNotEmpty()) {
      item {
        OutlinedTextField(
            search,
            { search = it },
            label = { Text("Buscar estabelecimento") },
            modifier = Modifier.fillMaxWidth().testTag("transaction-search"),
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
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
    }
    if (transactions.isEmpty())
        item {
          EmptyState(
              "Comece seu extrato",
              "Cadastre uma entrada ou saída para acompanhar seu saldo e seus gastos.",
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

private val brazilianDateFormat =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))
        .withResolverStyle(java.time.format.ResolverStyle.STRICT)

private fun formatBrazilianDate(storedDate: String): String =
    runCatching { LocalDate.parse(storedDate).format(brazilianDateFormat) }.getOrDefault("")

private fun parseBrazilianDate(input: String): LocalDate? =
    runCatching { LocalDate.parse(input, brazilianDateFormat) }.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
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
      rememberSaveable(id) {
        mutableStateOf(formatBrazilianDate(transaction?.date ?: LocalDate.now().toString()))
      }
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
  var detailsExpanded by
      rememberSaveable(id) {
        mutableStateOf(
            transaction?.recurring == true ||
                transaction?.installmentNumber != null ||
                transaction?.installmentCount != null ||
                transaction?.description?.isNotBlank() == true
        )
      }
  var attemptedSave by rememberSaveable(id) { mutableStateOf(false) }
  var amountTouched by rememberSaveable(id) { mutableStateOf(false) }
  var amountFocused by rememberSaveable(id) { mutableStateOf(false) }
  var merchantTouched by rememberSaveable(id) { mutableStateOf(false) }
  var merchantFocused by rememberSaveable(id) { mutableStateOf(false) }
  var descriptionTouched by rememberSaveable(id) { mutableStateOf(false) }
  var detailsFocused by rememberSaveable(id) { mutableStateOf(false) }
  var dateTouched by rememberSaveable(id) { mutableStateOf(false) }
  var categoryMenu by remember { mutableStateOf(false) }
  var datePickerVisible by remember { mutableStateOf(false) }
  val amountFocusRequester = remember { FocusRequester() }
  val merchantFocusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(id) {
    withFrameNanos {}
    runCatching { amountFocusRequester.requestFocus() }
  }

  val cents = BudgetAmount.parseCents(amount)
  val parsedDate = parseBrazilianDate(date)
  val validDate = parsedDate != null
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
      modifier = Modifier.testTag("transaction-editor"),
      onDismissRequest = onDismiss,
      title = { Text(if (transaction == null) "Novo lançamento" else "Editar lançamento") },
      text = {
        Column(
            Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
              value = amount,
              onValueChange = { amount = it },
              label = { Text("Valor em reais") },
              modifier =
                  Modifier.fillMaxWidth()
                      .focusRequester(amountFocusRequester)
                      .onFocusChanged {
                        if (it.isFocused) amountFocused = true
                        else if (amountFocused) amountTouched = true
                      }
                      .testTag("transaction-editor-amount"),
              singleLine = true,
              isError = (amountTouched || attemptedSave) && cents == null,
              keyboardOptions =
                  KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
              keyboardActions = KeyboardActions(onNext = { merchantFocusRequester.requestFocus() }),
          )
          if ((amountTouched || attemptedSave) && cents == null)
              Text(
                  "Informe um valor positivo válido, por exemplo 125,50.",
                  color = MaterialTheme.colorScheme.error,
              )
          OutlinedTextField(
              value = merchant,
              onValueChange = { merchant = it },
              label = { Text("Estabelecimento") },
              modifier =
                  Modifier.fillMaxWidth()
                      .focusRequester(merchantFocusRequester)
                      .onFocusChanged {
                        if (it.isFocused) merchantFocused = true
                        else if (merchantFocused) merchantTouched = true
                      }
                      .testTag("transaction-editor-merchant"),
              singleLine = true,
              isError = (merchantTouched || attemptedSave) && !validMerchant,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
          )
          if ((merchantTouched || attemptedSave) && !validMerchant)
              Text(
                  "Informe um estabelecimento com até 120 caracteres.",
                  color = MaterialTheme.colorScheme.error,
              )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = income,
                onClick = {
                  income = true
                  incomeCategoryId?.let { categoryId = it }
                  recurring = false
                  keyboardController?.hide()
                },
                label = { Text("Entrada") },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
            FilterChip(
                selected = !income,
                onClick = {
                  income = false
                  categoryId = expenseCategoryId
                  keyboardController?.hide()
                },
                label = { Text("Saída") },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
          }
          Box {
            OutlinedButton(
                onClick = {
                  categoryMenu = true
                  keyboardController?.hide()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
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
          if (attemptedSave && categoryId !in categoryOptions.map { it.id })
              Text(
                  "Escolha uma categoria compatível com o tipo.",
                  color = MaterialTheme.colorScheme.error,
              )
          OutlinedTextField(
              value = date,
              onValueChange = { date = it.take(10) },
              label = { Text("Data · dd/MM/aaaa") },
              modifier =
                  Modifier.fillMaxWidth()
                      .onFocusChanged {
                        if (!it.isFocused) {
                          dateTouched = true
                        }
                      }
                      .testTag("transaction-editor-date"),
              singleLine = true,
              isError = (dateTouched || attemptedSave) && !validDate,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
          )
          if ((dateTouched || attemptedSave) && !validDate)
              Text(
                  "Informe uma data válida em dd/MM/aaaa.",
                  color = MaterialTheme.colorScheme.error,
              )
          TextButton(
              onClick = {
                keyboardController?.hide()
                datePickerVisible = true
              },
              modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text("Escolher no calendário")
          }

          TextButton(
              onClick = {
                detailsExpanded = !detailsExpanded
                keyboardController?.hide()
              },
              modifier =
                  Modifier.fillMaxWidth()
                      .heightIn(min = 48.dp)
                      .testTag("transaction-editor-advanced"),
          ) {
            Text(if (detailsExpanded) "Menos detalhes" else "Mais detalhes")
          }
          if (detailsExpanded) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição (opcional)") },
                modifier =
                    Modifier.fillMaxWidth().onFocusChanged {
                      if (it.isFocused) detailsFocused = true
                      else if (detailsFocused) descriptionTouched = true
                    },
                singleLine = true,
                isError = (descriptionTouched || attemptedSave) && !validDescription,
            )
            if ((descriptionTouched || attemptedSave) && !validDescription)
                Text(
                    "A descrição aceita até 300 caracteres.",
                    color = MaterialTheme.colorScheme.error,
                )
            if (!income) {
              Row(
                  Modifier.fillMaxWidth().heightIn(min = 48.dp),
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                Text("Marcar como despesa recorrente")
              }
            }
            OutlinedTextField(
                value = installmentNumber,
                onValueChange = { installmentNumber = it.filter(Char::isDigit).take(3) },
                label = { Text("Parcela atual (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = installmentCount,
                onValueChange = { installmentCount = it.filter(Char::isDigit).take(3) },
                label = { Text("Total de parcelas (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if ((detailsFocused || attemptedSave) && !validInstallments)
                Text(
                    "Informe parcela atual e total válidos: 1 ≤ atual ≤ total ≤ 120.",
                    color = MaterialTheme.colorScheme.error,
                )
          }
        }
      },
      confirmButton = {
        TextButton(
            modifier = Modifier.testTag("transaction-editor-save"),
            onClick = {
              attemptedSave = true
              if (!valid && hasInstallments) detailsExpanded = true
              if (valid) {
                cents?.let { value ->
                  onSave(
                      Transaction(
                          id = id,
                          accountId = transaction?.accountId ?: "manual-ledger",
                          merchant = merchant.trim(),
                          description = description.trim(),
                          amountCents = if (income) value else -value,
                          date = requireNotNull(parsedDate).toString(),
                          categoryId = categoryId,
                          installmentNumber = parsedInstallmentNumber.takeIf { hasInstallments },
                          installmentCount = parsedInstallmentCount.takeIf { hasInstallments },
                          recurring = !income && recurring,
                          origin = transaction?.origin ?: TransactionOrigin.MANUAL,
                          importNoticeId = transaction?.importNoticeId,
                      )
                  )
                }
              }
            },
        ) {
          Text("Salvar")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )

  if (datePickerVisible) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                parsedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
    DatePickerDialog(
        onDismissRequest = { datePickerVisible = false },
        confirmButton = {
          TextButton(
              enabled = pickerState.selectedDateMillis != null,
              onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                  date =
                      java.time.Instant.ofEpochMilli(millis)
                          .atZone(ZoneOffset.UTC)
                          .toLocalDate()
                          .format(brazilianDateFormat)
                  dateTouched = true
                }
                datePickerVisible = false
              },
          ) {
            Text("Selecionar")
          }
        },
        dismissButton = {
          TextButton(onClick = { datePickerVisible = false }) { Text("Cancelar") }
        },
    ) {
      DatePicker(state = pickerState, modifier = Modifier.testTag("transaction-date-picker"))
    }
  }
}
