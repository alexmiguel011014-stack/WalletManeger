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
        item { DemoStrip("Ajustes da demonstração", materializedAt) }
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
                Text("Seus dados nesta demonstração", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Os dados financeiros de exemplo são fictícios e ficam neste dispositivo. O texto digitado no chat também fica local, mas pode não ser fictício.")
                Text("Exportar dados e excluir itens individualmente não estão disponíveis neste MVP.")
                TextButton(onClick = onPrivacy) { Text("Privacidade e limitações") }
            }
        }
        item {
            Panel {
                Text("Restaurar dados locais da demonstração", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Recria o pacote fictício e remove alterações locais da demonstração e mensagens digitadas. Preferências de exibição e a escolha de entrada inicial permanecem.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { confirmReset = true }) { Text("Restaurar demonstração") }
            }
        }
    }
    ConfirmationSheet(
        visible = confirmReset,
        title = "Restaurar demonstração?",
        message = "Esta ação remove alterações locais da demonstração e mensagens digitadas, e recria o pacote fictício. Não afeta bancos, WhatsApp nem outros apps. Preferências de exibição e a escolha de entrada inicial serão preservadas.",
        confirmLabel = "Restaurar",
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
