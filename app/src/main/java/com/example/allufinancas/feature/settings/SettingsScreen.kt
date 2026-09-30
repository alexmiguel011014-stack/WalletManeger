package com.example.allufinancas.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.allufinancas.R
import com.example.allufinancas.core.brl
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*
import java.time.Instant

@Composable internal fun Settings(settings: DisplayPreferences, materializedAt: Instant?, model: AlluViewModel, onPrivacy: () -> Unit) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { DemoStrip("Ajustes", materializedAt) }
        item {
            Panel {
                Text("Preferências", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SettingSwitch("Modo escuro", settings.darkTheme, model::setDarkTheme)
                SettingSwitch("Reduzir movimento", settings.reduceMotion, model::setReduceMotion)
                Text("Moeda fixa: BRL (R$), formato pt-BR")
                SettingSwitch("Mostrar centavos", settings.showCents, model::setShowCents, "Preferência de exibição")
            }
        }
        item {
            Panel {
                Text("Seus dados locais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Lançamentos, limites e mensagens do assistente ficam salvos neste dispositivo. O app não se conecta a bancos nem envia esses dados.")
                Text("Você pode editar ou excluir lançamentos pelo extrato. A exportação de dados ainda não está disponível.")
                TextButton(onClick = onPrivacy) { Text("Privacidade e limitações") }
            }
        }
        item {
            Panel {
                Text("Excluir seus dados locais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Apaga lançamentos, limites, metas, avisos, decisões e mensagens do assistente. As categorias padrão e suas preferências ficam.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { confirmReset = true }) { Text("Excluir todos os dados") }
            }
        }
    }
    ConfirmationSheet(
        visible = confirmReset,
        title = "Excluir todos os dados locais?",
        message = "Esta ação apaga seus lançamentos, limites, metas, avisos, decisões e mensagens do assistente. As categorias padrão, preferências de exibição e escolha de entrada inicial serão preservadas.",
        confirmLabel = "Excluir dados",
        onConfirm = model::resetDemo,
        onDismiss = { confirmReset = false },
    )
}

@Composable internal fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit, supportingText: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(label)
            supportingText?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}
