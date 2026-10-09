package com.example.allufinancas.feature.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.Category
import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.data.DisplayPreferences
import com.example.allufinancas.data.ImportNotice
import com.example.allufinancas.data.ImportNoticeConfirmation
import com.example.allufinancas.data.ImportNoticeKind
import com.example.allufinancas.data.ImportNoticeResolution
import com.example.allufinancas.data.ImportNoticeStatus
import com.example.allufinancas.data.NotificationImportSource
import com.example.allufinancas.data.TransactionCategoryRules
import com.example.allufinancas.designsystem.AlluTokens
import com.example.allufinancas.designsystem.DemoStrip
import com.example.allufinancas.designsystem.EmptyState
import com.example.allufinancas.designsystem.Panel
import com.example.allufinancas.feature.budgets.BudgetAmount
import com.example.allufinancas.navigation.DataScreenViewModel
import com.example.allufinancas.navigation.ScreenUiState
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.flow.combine

internal data class ImportInboxUiData(
    val notices: List<ImportNotice>,
    val categories: List<Category>,
    val preferences: DisplayPreferences,
    val enabledSource: NotificationImportSource?,
)

internal class ImportInboxViewModel(dataSource: DemoDataSource) : DataScreenViewModel(dataSource) {
  private val screenData =
      combine(
              dataSource.importNotices,
              dataSource.categories,
              dataSource.preferences,
          ) { notices, categories, preferences ->
            ImportInboxUiData(notices, categories, preferences, null)
          }
          .combine(dataSource.enabledImportSource) { data, source ->
            data.copy(enabledSource = source)
          }

  internal val state: kotlinx.coroutines.flow.StateFlow<ScreenUiState<ImportInboxUiData>> =
      stateFor(screenData) { false }
}

@Composable
internal fun ImportInbox(
    data: ImportInboxUiData,
    materializedAt: Instant?,
    onOpenSettings: () -> Unit,
    onConfirm: (String, ImportNoticeConfirmation) -> Unit,
    onResolve: (String, ImportNoticeResolution) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var listenerAccessEnabled by
      remember(context) {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        )
      }
  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        listenerAccessEnabled =
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  val pending = data.notices.filter { it.status == ImportNoticeStatus.PENDING }
  var editingNoticeId by rememberSaveable { mutableStateOf<String?>(null) }
  val editingNotice = pending.firstOrNull { it.id == editingNoticeId }

  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      DemoStrip(
          "Importações pendentes · ${data.enabledSource?.displayName ?: if (pending.isNotEmpty()) "captura pausada" else "sem fonte"}",
          materializedAt,
      )
    }
    item {
      Text(
          "Confira antes de lançar",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
      )
      Text(
          "As notificações ficam neste dispositivo e só entram no extrato depois da sua confirmação.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (data.enabledSource == null && pending.isNotEmpty()) {
      item {
        Panel {
          Text(
              "Captura pausada",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
          )
          Text(
              "A Rico foi desconectada, mas estes rascunhos foram mantidos. Você ainda pode revisar, confirmar ou ignorar cada um.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    when {
      pending.isNotEmpty() ->
          items(pending, key = { it.id }) { notice ->
            ImportNoticeCard(
                notice = notice,
                categories = data.categories,
                showCents = data.preferences.showCents,
                onEdit = { editingNoticeId = notice.id },
                onIgnore = { onResolve(notice.id, ImportNoticeResolution.IGNORED) },
                onAcknowledgeNeutral = { onResolve(notice.id, ImportNoticeResolution.NEUTRAL) },
            )
          }
      data.enabledSource == null ->
          item {
            EmptyState(
                title = "Nenhuma fonte selecionada",
                message =
                    "Escolha a Rico em Ajustes e ative o acesso a notificações do Android para receber rascunhos aqui.",
                actionLabel = "Abrir ajustes",
                onAction = onOpenSettings,
            )
          }
      !listenerAccessEnabled ->
          item {
            EmptyState(
                title = "Acesso a notificações não autorizado",
                message =
                    "A Rico está selecionada, mas o Android ainda não liberou o acesso a notificações do wallet Manager. Nenhum aviso novo será capturado até você autorizar e voltar ao app.",
                actionLabel = "Abrir ajustes",
                onAction = onOpenSettings,
            )
          }
      else ->
          item {
            EmptyState(
                title = "Nenhuma importação pendente",
                message =
                    "Quando uma notificação compatível da Rico chegar, ela aparecerá aqui para você revisar. Nada é lançado automaticamente.",
            )
          }
    }
    item {
      Text(
          "Pagamento de fatura e transferências ficam como itens neutros. Estornos e reversões não viram entrada automaticamente.",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  editingNotice?.let { notice ->
    ImportNoticeEditor(
        notice = notice,
        categories = data.categories,
        showCents = data.preferences.showCents,
        onDismiss = { editingNoticeId = null },
        onConfirm = { confirmation ->
          onConfirm(notice.id, confirmation)
          editingNoticeId = null
        },
    )
  }
}

@Composable
private fun ImportNoticeCard(
    notice: ImportNotice,
    categories: List<Category>,
    showCents: Boolean,
    onEdit: () -> Unit,
    onIgnore: () -> Unit,
    onAcknowledgeNeutral: () -> Unit,
) {
  val suggestedCategory =
      categories.firstOrNull { it.id == suggestedCategoryId(notice, categories) }?.name
  Panel {
    Column(verticalArrangement = Arrangement.spacedBy(AlluTokens.Spacing.xs)) {
      Text(
          importTitle(notice.kind),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
      )
      Text(
          "Origem: ${notice.source.displayName} · Data: ${notice.date}",
          style = MaterialTheme.typography.labelMedium,
      )
      Text(
          notice.merchant.ifBlank { "Estabelecimento não identificado" },
          fontWeight = FontWeight.SemiBold,
      )
      Text("Valor: ${brl(notice.amountCents, showCents)}")
      if (notice.description.isNotBlank()) Text("Descrição: ${notice.description}")
      if (notice.kind == ImportNoticeKind.EXPENSE || notice.kind == ImportNoticeKind.INCOME) {
        Text(
            "Sugestão inicial: ${suggestedCategory ?: "escolha uma categoria"}",
            style = MaterialTheme.typography.labelMedium,
        )
      }
      if (notice.possibleDuplicate) {
        Text(
            "Possível duplicidade: confira se esta é outra compra antes de confirmar. O app não descarta compras parecidas automaticamente.",
            color = MaterialTheme.colorScheme.error,
        )
      }
      when (notice.kind) {
        ImportNoticeKind.EXPENSE -> {
          Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text(if (notice.possibleDuplicate) "Revisar e confirmar saída" else "Confirmar saída")
          }
          TextButton(onClick = onIgnore, modifier = Modifier.fillMaxWidth()) {
            Text("Ignorar rascunho")
          }
        }
        ImportNoticeKind.INCOME -> {
          Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (notice.possibleDuplicate) "Revisar e confirmar entrada" else "Confirmar entrada"
            )
          }
          TextButton(onClick = onIgnore, modifier = Modifier.fillMaxWidth()) {
            Text("Ignorar rascunho")
          }
        }
        ImportNoticeKind.NEUTRAL -> {
          Text("Este aviso é informativo e não altera entrada, saída ou saldo.")
          OutlinedButton(onClick = onAcknowledgeNeutral, modifier = Modifier.fillMaxWidth()) {
            Text("Entendi, não lançar")
          }
          TextButton(onClick = onIgnore, modifier = Modifier.fillMaxWidth()) {
            Text("Ignorar rascunho")
          }
        }
        ImportNoticeKind.REFUND_REVIEW -> {
          Text(
              "Estorno ou reversão precisa ser conferido no extrato. Ele não será lançado como entrada automaticamente."
          )
          TextButton(onClick = onIgnore, modifier = Modifier.fillMaxWidth()) {
            Text("Ignorar rascunho")
          }
        }
      }
    }
  }
}

@Composable
private fun ImportNoticeEditor(
    notice: ImportNotice,
    categories: List<Category>,
    showCents: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ImportNoticeConfirmation) -> Unit,
) {
  val id = notice.id
  var merchant by rememberSaveable(id) { mutableStateOf(notice.merchant) }
  var description by rememberSaveable(id) { mutableStateOf(notice.description) }
  var amount by rememberSaveable(id) { mutableStateOf(importAmountInput(notice.amountCents)) }
  var date by rememberSaveable(id) { mutableStateOf(notice.date) }
  val sign = if (notice.kind == ImportNoticeKind.INCOME) 1 else -1
  val categoryOptions = TransactionCategoryRules.compatible(categories, sign.toLong())
  var categoryId by
      rememberSaveable(id) {
        mutableStateOf(
            suggestedCategoryId(notice, categories).takeIf { candidate ->
              categoryOptions.any { it.id == candidate }
            } ?: categoryOptions.firstOrNull()?.id.orEmpty()
        )
      }
  var categoryMenu by rememberSaveable(id) { mutableStateOf(false) }
  var duplicateAccepted by rememberSaveable(id) { mutableStateOf(false) }
  val cents = BudgetAmount.parseCents(amount)
  val validDate = runCatching { LocalDate.parse(date).toString() == date }.getOrDefault(false)
  val validMerchant = merchant.trim().isNotEmpty() && merchant.trim().length <= 120
  val validDescription = description.trim().length <= 300
  val valid =
      cents != null &&
          validDate &&
          validMerchant &&
          validDescription &&
          categoryOptions.any { it.id == categoryId } &&
          (!notice.possibleDuplicate || duplicateAccepted)

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(if (notice.kind == ImportNoticeKind.INCOME) "Confirmar entrada" else "Confirmar saída")
      },
      text = {
        Column(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
              "A origem é ${notice.source.displayName}. Revise os dados antes de criar o lançamento.",
              style = MaterialTheme.typography.bodyMedium,
          )
          if (notice.possibleDuplicate) {
            Text(
                "Possível duplicidade: confirme somente se este aviso representa outra compra.",
                color = MaterialTheme.colorScheme.error,
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
              Checkbox(checked = duplicateAccepted, onCheckedChange = { duplicateAccepted = it })
              Text("Conferi: é outro lançamento e desejo confirmar mesmo assim.")
            }
          }
          OutlinedTextField(
              value = amount,
              onValueChange = { amount = it },
              label = { Text("Valor em reais") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              isError = amount.isNotBlank() && cents == null,
              modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
              value = merchant,
              onValueChange = { merchant = it },
              label = { Text("Estabelecimento") },
              singleLine = true,
              isError = merchant.isNotBlank() && !validMerchant,
              modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Descrição (opcional)") },
              isError = !validDescription,
              modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
              value = date,
              onValueChange = { date = it },
              label = { Text("Data · AAAA-MM-DD") },
              singleLine = true,
              isError = date.isNotBlank() && !validDate,
              modifier = Modifier.fillMaxWidth(),
          )
          Box {
            OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
              Text(
                  categoryOptions.firstOrNull { it.id == categoryId }?.name ?: "Escolher categoria"
              )
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
          if (cents == null)
              Text(
                  "Informe um valor positivo válido, por exemplo 8,89.",
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
          if (categoryOptions.isEmpty())
              Text(
                  "Nenhuma categoria compatível está disponível.",
                  color = MaterialTheme.colorScheme.error,
              )
        }
      },
      confirmButton = {
        TextButton(
            enabled = valid,
            onClick = {
              cents?.let { value ->
                onConfirm(
                    ImportNoticeConfirmation(
                        merchant = merchant.trim(),
                        description = description.trim(),
                        amountCents = if (sign > 0) value else -value,
                        date = date,
                        categoryId = categoryId,
                        acceptPossibleDuplicate = duplicateAccepted,
                    ),
                )
              }
            },
        ) {
          Text("Confirmar no extrato")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}

private fun importTitle(kind: ImportNoticeKind): String =
    when (kind) {
      ImportNoticeKind.EXPENSE -> "Saída a confirmar"
      ImportNoticeKind.INCOME -> "Entrada a confirmar"
      ImportNoticeKind.NEUTRAL -> "Aviso neutro"
      ImportNoticeKind.REFUND_REVIEW -> "Estorno ou reversão a conferir"
    }

private fun suggestedCategoryId(notice: ImportNotice, categories: List<Category>): String {
  if (notice.kind == ImportNoticeKind.INCOME) return TransactionCategoryRules.INCOME_CATEGORY_ID
  val copy = "${notice.merchant} ${notice.description}".lowercase()
  val suggested =
      when {
        copy.contains("padaria") ||
            copy.contains("mercado") ||
            copy.contains("restaurante") ||
            copy.contains("lanche") -> "alimentacao"
        copy.contains("uber") ||
            copy.contains("99") ||
            copy.contains("posto") ||
            copy.contains("ônibus") ||
            copy.contains("onibus") -> "transporte"
        copy.contains("farmácia") ||
            copy.contains("farmacia") ||
            copy.contains("médic") ||
            copy.contains("medic") -> "saude"
        copy.contains("aluguel") ||
            copy.contains("energia") ||
            copy.contains("água") ||
            copy.contains("agua") -> "casa"
        copy.contains("spotify") || copy.contains("netflix") || copy.contains("assinatura") ->
            "assinaturas"
        else -> "compras"
      }
  return suggested.takeIf { candidate -> categories.any { it.id == candidate } }
      ?: TransactionCategoryRules.compatible(categories, -1).firstOrNull()?.id.orEmpty()
}

private fun importAmountInput(amountCents: Long): String {
  val value = abs(amountCents)
  return "${value / 100},${(value % 100).toString().padStart(2, '0')}"
}
