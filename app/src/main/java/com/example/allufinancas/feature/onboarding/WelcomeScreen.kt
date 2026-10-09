package com.example.allufinancas.feature.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.allufinancas.R
import com.example.allufinancas.data.*
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.navigation.*

@Composable
internal fun Welcome(onExplore: () -> Unit, onPrivacy: () -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize().background(AlluNavy)) {
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Spacer(Modifier.height(44.dp))
        Image(
            painterResource(R.drawable.wallet_mark),
            "Símbolo wallet Manager",
            Modifier.size(44.dp, 58.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "wallet\nManager",
            color = Color.White,
            fontSize = 42.sp,
            lineHeight = 46.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))
        Text("Clareza para acompanhar cada valor.", color = AlluTint, fontSize = 18.sp)
      }
      Column {
        Surface(color = Color.White.copy(alpha = .1f), shape = RoundedCornerShape(14.dp)) {
          Column(Modifier.padding(18.dp)) {
            Text(
                "SEUS DADOS FICAM NO APARELHO",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Cadastre suas entradas, saídas e limites. Sem conta, conexão bancária ou envio de dados.",
                color = Color.White,
            )
          }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onExplore, modifier = Modifier.fillMaxWidth().height(54.dp)) {
          Text("Começar a cadastrar")
        }
        TextButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
          Text("Privacidade e dados locais", color = Color.White)
        }
      }
    }
  }
}
