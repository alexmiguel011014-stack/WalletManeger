package com.example.allufinancas.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.allufinancas.designsystem.DemoStrip
import com.example.allufinancas.designsystem.Panel
import java.time.Instant

@Composable
internal fun PrivacyEducation(materializedAt: Instant?, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("Voltar") }
        DemoStrip("Privacidade e limitações", materializedAt)
        Text(
            "Privacidade e conexões",
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Panel {
            Text("Nenhuma conexão ativa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Esta demonstração não se conecta a bancos e não tem integração ativa com WhatsApp.")
        }
        Panel {
            Text("Sem coleta de dados externos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Não há coleta ou envio de dados externos: não lemos contas bancárias nem mensagens do WhatsApp. Dados financeiros são fictícios e ficam neste dispositivo. O texto digitado no chat também fica neste dispositivo, mas pode não ser fictício.")
        }
        Panel {
            Text("Limitações deste MVP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Exportar dados e excluir itens individualmente não estão disponíveis.")
        }
        Panel {
            Text("Consentimento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Uma eventual integração futura é apenas uma possibilidade conceitual, não uma promessa. Se algum dia fosse oferecida, exigiria informação clara e consentimento explícito antes de qualquer conexão.")
        }
    }
}
