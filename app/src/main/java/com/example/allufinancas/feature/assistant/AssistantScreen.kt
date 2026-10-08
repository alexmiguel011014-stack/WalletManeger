package com.example.allufinancas.feature.assistant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*
import java.time.Instant
import java.time.YearMonth

@Composable
internal fun Assistant(
    messages: List<AssistantMessage>,
    transactions: List<Transaction>,
    categories: List<Category>,
    materializedAt: Instant?,
    onSend: (String) -> Unit,
) {
  var text by rememberSaveable { mutableStateOf("") }
  var sourceMessageId by rememberSaveable { mutableStateOf<Long?>(null) }
  val transactionsById = remember(transactions) { transactions.associateBy(Transaction::id) }
  val categoriesById = remember(categories) { categories.associateBy(Category::id) }
  val month = YearMonth.now().toString()
  Column(Modifier.fillMaxSize().padding(20.dp)) {
    DemoStrip("Assistente local · período $month", materializedAt)
    Spacer(Modifier.height(12.dp))
    Text(
        "Pergunte sobre seus lançamentos",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        "Cálculos neste dispositivo; sem IA online, conexão bancária ou aconselhamento financeiro.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )
    Spacer(Modifier.height(12.dp))
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(listOf("Quanto gastei no mês?", "Maior despesa", "Assinaturas recorrentes")) {
          suggestion ->
        OutlinedButton(onClick = { onSend(suggestion) }) { Text(suggestion) }
      }
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
      if (messages.isEmpty())
          item {
            EmptyState(
                title = "Comece uma conversa",
                message =
                    if (transactions.isEmpty())
                        "Cadastre sua primeira entrada ou saída no extrato. Depois, pergunte sobre seus gastos e limites."
                    else
                        "Escolha uma sugestão ou escreva uma pergunta. As respostas usam somente dados salvos neste dispositivo.",
            )
          }
      items(messages, key = { it.id }) { message ->
        val isUser = message.role == MessageRole.USER
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
          Surface(
              modifier = Modifier.widthIn(max = AlluTokens.Size.assistantBubbleMaxWidth),
              color =
                  if (isUser) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant,
              contentColor =
                  if (isUser) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
              shape = RoundedCornerShape(AlluTokens.Radius.card),
          ) {
            Column(Modifier.padding(AlluTokens.Spacing.md)) {
              Text(
                  if (isUser) "Você" else "Assistente local · $month",
                  style = MaterialTheme.typography.labelSmall,
                  color =
                      if (isUser) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(message.text, style = MaterialTheme.typography.bodyLarge)
              if (!isUser) {
                Text(
                    "Os registros podem ter mudado desde esta resposta.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (message.citationTransactionIds.isNotEmpty()) {
                  TextButton(onClick = { sourceMessageId = message.id }) { Text("Ver fontes") }
                }
              }
            }
          }
        }
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Sua pergunta") },
          supportingText = {
            Text(if (text.length > 500) "Limite: 500 caracteres" else "${text.length}/500")
          },
          isError = text.length > 500,
          modifier = Modifier.weight(1f),
          maxLines = 3,
      )
      Spacer(Modifier.width(8.dp))
      Button(
          enabled = text.isNotBlank() && text.length <= 500,
          onClick = {
            if (text.isNotBlank() && text.length <= 500) {
              onSend(text.trim())
              text = ""
            }
          },
      ) {
        Text("Enviar")
      }
    }
  }
  val selectedMessage = messages.firstOrNull { it.id == sourceMessageId }
  if (selectedMessage != null) {
    val sources = selectedMessage.citationTransactionIds.mapNotNull(transactionsById::get)
    AlertDialog(
        onDismissRequest = { sourceMessageId = null },
        title = { Text("Fontes desta resposta") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Lançamentos locais usados para calcular esta resposta:")
            if (sources.isEmpty()) Text("As fontes não estão disponíveis neste momento.")
            sources.forEach { transaction ->
              val categoryName =
                  categoriesById[transaction.categoryId]?.name ?: "Categoria não disponível"
              Text(
                  "ID ${transaction.id} · ${transaction.merchant} · $categoryName\n${transaction.date} · ${brl(transaction.amountCents, true)}",
                  style = MaterialTheme.typography.bodyMedium,
              )
            }
            Text(
                "Os registros podem ter mudado desde a resposta. Não há sincronização em tempo real."
            )
          }
        },
        confirmButton = { TextButton(onClick = { sourceMessageId = null }) { Text("Fechar") } },
    )
  }
}
