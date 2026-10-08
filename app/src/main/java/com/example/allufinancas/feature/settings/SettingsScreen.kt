package com.example.allufinancas.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*
import java.time.Instant

@Composable
internal fun Settings(
    settings: DisplayPreferences,
    enabledImportSource: NotificationImportSource?,
    importFeedSuffix: String?,
    materializedAt: Instant?,
    model: AlluViewModel,
    onPrivacy: () -> Unit,
) {
  var confirmReset by rememberSaveable { mutableStateOf(false) }
  LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { DemoStrip("Ajustes", materializedAt) }
    item {
      Panel {
        Text(
            "Preferências",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        SettingSwitch("Modo escuro", settings.darkTheme, model::setDarkTheme)
        SettingSwitch("Reduzir movimento", settings.reduceMotion, model::setReduceMotion)
        Text("Moeda fixa: BRL (R$), formato pt-BR")
        SettingSwitch(
            "Mostrar centavos",
            settings.showCents,
            model::setShowCents,
            "Preferência de exibição",
        )
      }
    }
    item { ImportSettings(enabledImportSource, importFeedSuffix, model) }
    item {
      Panel {
        Text(
            "Seus dados locais",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Lançamentos, limites e mensagens do assistente ficam salvos neste dispositivo. A importação opcional da Rico usa apenas notificações que você selecionar e confirmar; não há API bancária, credenciais ou rede."
        )
        Text(
            "Você pode editar ou excluir lançamentos pelo extrato. A exportação de dados ainda não está disponível."
        )
        TextButton(onClick = onPrivacy) { Text("Privacidade e limitações") }
      }
    }
    item {
      Panel {
        Text(
            "Excluir seus dados locais",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Apaga lançamentos, limites, metas, avisos, decisões, rascunhos importados e mensagens do assistente. As categorias padrão e suas preferências ficam.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { confirmReset = true }) { Text("Excluir todos os dados") }
      }
    }
  }
  ConfirmationSheet(
      visible = confirmReset,
      title = "Excluir todos os dados locais?",
      message =
          "Esta ação apaga seus lançamentos, limites, metas, avisos, decisões, rascunhos importados e mensagens do assistente. As categorias padrão e preferências de exibição serão preservadas. A permissão de notificações continua nas configurações do Android.",
      confirmLabel = "Excluir dados",
      onConfirm = {
        confirmReset = false
        model.resetDemo()
      },
      onDismiss = { confirmReset = false },
  )
}

@Composable
private fun ImportSettings(
    source: NotificationImportSource?,
    importFeedSuffix: String?,
    model: AlluViewModel,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var listenerAccessEnabled by remember {
    mutableStateOf(notificationListenerAccessEnabled(context))
  }
  var sourceInstalled by
      remember(source, context) {
        mutableStateOf(source?.let { notificationSourceInstalled(context, it) } ?: false)
      }
  var disconnecting by rememberSaveable { mutableStateOf(false) }
  fun refreshNotificationStatus() {
    listenerAccessEnabled = notificationListenerAccessEnabled(context)
    sourceInstalled = source?.let { notificationSourceInstalled(context, it) } ?: false
  }
  DisposableEffect(lifecycleOwner, context, source) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) refreshNotificationStatus()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Panel {
    Text(
        "Importação local de notificações",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "O Android concede acesso amplo às notificações. ALLU só lê e guarda campos mínimos de avisos da fonte escolhida; os demais apps são descartados antes de abrir o conteúdo."
    )
    Spacer(Modifier.height(8.dp))
    if (source == null) {
      val ricoInstalled = notificationSourceInstalled(context, NotificationImportSource.RICO)
      if (ricoInstalled) {
        Text("Nenhuma fonte selecionada. A única opção desta versão é a Rico.")
        Button(
            onClick = { model.enableImportSource(NotificationImportSource.RICO) },
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Selecionar Rico")
        }
      } else {
        Text(
            "A Rico não foi encontrada neste aparelho. Instale ou abra a Rico e volte aqui para selecioná-la.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      Text("Fonte selecionada: ${source.displayName}")
      Text(
          when {
            !sourceInstalled ->
                "A Rico não foi encontrada neste aparelho. A captura está pausada até que ela esteja disponível."
            listenerAccessEnabled -> "Acesso a notificações: ativo no Android."
            else ->
                "Acesso a notificações revogado ou ainda não autorizado no Android. A captura está pausada."
          },
          color =
              if (sourceInstalled && listenerAccessEnabled) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      ImportFeedSuffixSettings(importFeedSuffix, model::setImportFeedSuffix)
      TextButton(
          onClick = {
            context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
          },
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Abrir acesso a notificações")
      }
      TextButton(onClick = ::refreshNotificationStatus, modifier = Modifier.fillMaxWidth()) {
        Text("Atualizar status")
      }
      TextButton(onClick = { disconnecting = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Desconectar a Rico")
      }
      Text(
          "Desconectar interrompe novas capturas. Você pode manter ou descartar os rascunhos pendentes; lançamentos já confirmados ficam no extrato.",
          style = MaterialTheme.typography.labelSmall,
      )
    }
  }
  if (disconnecting) {
    AlertDialog(
        onDismissRequest = { disconnecting = false },
        title = { Text("Desconectar a Rico?") },
        text = {
          Text(
              "O acesso do Android precisa ser revogado nas configurações do sistema se você também quiser removê-lo. Escolha o que fazer com rascunhos pendentes."
          )
        },
        confirmButton = {
          TextButton(
              onClick = {
                model.disconnectImport(discardPending = false)
                disconnecting = false
              }
          ) {
            Text("Manter rascunhos")
          }
        },
        dismissButton = {
          Row {
            TextButton(onClick = { disconnecting = false }) { Text("Cancelar") }
            TextButton(
                onClick = {
                  model.disconnectImport(discardPending = true)
                  disconnecting = false
                }
            ) {
              Text("Descartar rascunhos")
            }
          }
        },
    )
  }
}

private fun notificationListenerAccessEnabled(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private fun notificationSourceInstalled(
    context: Context,
    source: NotificationImportSource,
): Boolean =
    try {
      context.packageManager.getApplicationInfo(source.packageName, 0)
      true
    } catch (_: PackageManager.NameNotFoundException) {
      false
    }

@Composable
private fun ImportFeedSuffixSettings(
    savedSuffix: String?,
    onSave: (String?) -> Unit,
) {
  var suffixInput by rememberSaveable { mutableStateOf(savedSuffix.orEmpty()) }
  var hasUserInput by rememberSaveable { mutableStateOf(false) }
  var awaitingInitialHydration by rememberSaveable { mutableStateOf(savedSuffix == null) }
  LaunchedEffect(savedSuffix) {
    val saved = savedSuffix.orEmpty()
    if (hasUserInput && awaitingInitialHydration) {
      // The initial DataStore value may arrive after the user starts typing.
      if (savedSuffix != null) awaitingInitialHydration = false
    } else {
      suffixInput = saved
      hasUserInput = false
      if (savedSuffix != null) awaitingInitialHydration = false
    }
  }
  val valid = suffixInput.isEmpty() || suffixInput.length == 4

  Spacer(Modifier.height(12.dp))
  Text("Filtro opcional da conta ou cartão", style = MaterialTheme.typography.titleSmall)
  Text(
      "Informe apenas os quatro dígitos finais que aparecem visivelmente na notificação da Rico. Se o aviso não mostrar esse final, deixe o campo vazio para usar somente a fonte Rico.",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  OutlinedTextField(
      value = suffixInput,
      onValueChange = { proposed ->
        if (proposed.length <= 4 && proposed.all(Char::isDigit)) {
          suffixInput = proposed
          hasUserInput = true
        }
      },
      label = { Text("Últimos 4 dígitos (opcional)") },
      singleLine = true,
      isError = !valid,
      modifier =
          Modifier.fillMaxWidth().semantics {
            contentDescription = "Filtro por quatro dígitos finais"
          },
  )
  if (!valid)
      Text("Use exatamente quatro dígitos ou deixe vazio.", color = MaterialTheme.colorScheme.error)
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TextButton(
        onClick = { onSave(suffixInput.ifEmpty { null }) },
        enabled = valid && suffixInput != savedSuffix.orEmpty(),
    ) {
      Text("Salvar filtro")
    }
    if (savedSuffix != null) {
      TextButton(onClick = { onSave(null) }) { Text("Remover filtro") }
    }
  }
  savedSuffix?.let {
    Text("Filtro ativo: final •••• $it", style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
internal fun SettingSwitch(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    supportingText: String? = null,
) {
  Row(
      Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column {
      Text(label)
      supportingText?.let {
        Text(
            it,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.semantics { contentDescription = label },
    )
  }
}
