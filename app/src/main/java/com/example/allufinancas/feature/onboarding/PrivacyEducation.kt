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
            Text("Funcionamento local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("O app não usa API bancária, Open Finance, credenciais ou rede. Lançamentos, limites e mensagens ficam neste dispositivo.")
        }
        Panel {
            Text("Importação opcional da Rico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Você pode escolher a Rico em Ajustes e autorizar o acesso a notificações no Android. Essa permissão do sistema é ampla; o ALLU confere primeiro o app de origem e lê somente a Rico selecionada. Se a notificação mostrar um final de conta ou cartão confiável, você pode informar apenas os quatro últimos dígitos para limitar ainda mais a captura.")
        }
        Panel {
            Text("Confirmação antes do extrato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Uma notificação vira um rascunho pendente. Nada altera saldo, entradas ou saídas até você revisar e confirmar. O app guarda apenas tipo, valor, data e identificadores técnicos opacos; o texto original, códigos, chaves Pix e números completos não são salvos.")
        }
        Panel {
            Text("O que entra na fila", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Compras e valores recebidos compatíveis vão para revisão. Fatura paga e transferência são avisos neutros. Estorno precisa de conferência. Recusas, pendências, códigos, promoções, moeda estrangeira, valores múltiplos ou parcelamento ambíguo são descartados.")
        }
        Panel {
            Text("Limitações deste MVP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Lançamentos confirmados podem ser editados ou excluídos no extrato. Ainda não há exportação, recuperação de conta ou sincronização entre dispositivos.")
        }
        Panel {
            Text("Desconexão e exclusão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Desconectar a Rico interrompe novas importações e permite manter ou descartar rascunhos pendentes. Rascunhos mantidos continuam revisáveis. Excluir os dados remove os rascunhos e a escolha da fonte no app; a permissão de notificações é revogada nas configurações do Android.")
        }
    }
}
