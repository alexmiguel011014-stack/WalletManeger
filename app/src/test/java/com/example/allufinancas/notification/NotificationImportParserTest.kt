package com.example.allufinancas.notification

import com.example.allufinancas.data.ImportNoticeKind
import com.example.allufinancas.data.NotificationImportSource
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationImportParserTest {
  private val postedAt = Instant.parse("2026-09-30T14:00:00Z")
  private val zone = ZoneId.of("America/Sao_Paulo")

  @Test
  fun unsupportedOrDisabledPackagesNeverReadNotificationContent() {
    var read = false

    val unsupported =
        NotificationImportParser.parseIfEnabled(
            sourcePackage = "com.example.other",
            sourceEnabled = true,
            sourceEventKey = "ignored",
            postedAt = postedAt,
            readContent = {
              read = true
              NotificationText(text = "Compra aprovada R$ 8,89")
            },
        )

    assertNull(unsupported)
    assertFalse(read)

    val disabled =
        NotificationImportParser.parseIfEnabled(
            sourcePackage = NotificationImportSource.RICO.packageName,
            sourceEnabled = false,
            sourceEventKey = "disabled",
            postedAt = postedAt,
            readContent = {
              read = true
              NotificationText(text = "Compra aprovada R$ 8,89")
            },
        )

    assertNull(disabled)
    assertFalse(read)
  }

  @Test
  fun classifiesSafeKindsWithoutCopyingNotificationText() {
    val purchaseText = "Compra aprovada na Padaria de Seu Zé CPF 123.456.789-00 no valor de R$ 8,89"
    val purchase = parse("purchase", purchaseText)
    val income = parse("income", "Pix recebido de ana@example.com no valor de R$ 150,00")
    val bill = parse("bill", "Pagamento de fatura aprovado de R$ 1.250,00")
    val transfer = parse("transfer", "Transferência interna concluída de R$ 90,00")
    val refund = parse("refund", "Estorno de R$ 42,10")

    assertEquals(ImportNoticeKind.EXPENSE, purchase?.kind)
    assertEquals(-889L, purchase?.amountCents)
    assertEquals("Estabelecimento a confirmar", purchase?.merchant)
    assertEquals(ImportNoticeKind.INCOME, income?.kind)
    assertEquals(15_000L, income?.amountCents)
    assertEquals("Origem a confirmar", income?.merchant)
    assertEquals(ImportNoticeKind.NEUTRAL, bill?.kind)
    assertEquals(125_000L, bill?.amountCents)
    assertEquals(ImportNoticeKind.NEUTRAL, transfer?.kind)
    assertEquals(9_000L, transfer?.amountCents)
    assertEquals(ImportNoticeKind.REFUND_REVIEW, refund?.kind)
    assertEquals(4_210L, refund?.amountCents)
    assertFalse(purchase.toString().contains("Padaria de Seu Zé"))
    assertFalse(purchase.toString().contains("123.456.789-00"))
    assertFalse(income.toString().contains("ana@example.com"))
  }

  @Test
  fun selectedFourDigitFeedMustAppearInARecognizedAccountOrCardContext() {
    val matching =
        parse(
            key = "matching-feed",
            text = "Compra aprovada cartão final 1234 de R$ 8,89",
            requiredFeedSuffix = "1234",
        )
    val amountOnly =
        parse(
            key = "amount-only",
            text = "Compra aprovada de R$ 12,34",
            requiredFeedSuffix = "1234",
        )
    val otherCard =
        parse(
            key = "other-card",
            text = "Compra aprovada cartão final 9876 de R$ 8,89",
            requiredFeedSuffix = "1234",
        )

    assertNotNull(matching)
    assertEquals("1234", matching?.feedSuffix)
    assertNull(amountOnly)
    assertNull(otherCard)
  }

  @Test
  fun installmentsNeedAnExplicitCurrentAmountAndAmbiguousOrForeignNoticesAreRejected() {
    val explicitLabel = parse("installment-label", "Compra aprovada: valor da parcela de R$ 30,00")
    val explicitOrdinal = parse("installment-ordinal", "Compra aprovada: parcela 1 de 3: R$ 30,00")

    assertEquals(-3_000L, explicitLabel?.amountCents)
    assertEquals(-3_000L, explicitOrdinal?.amountCents)
    assertNull(parse("declined", "Compra recusada de R$ 8,89"))
    assertNull(parse("otp", "Seu código de segurança é 123456"))
    assertNull(parse("unknown", "Você tem uma novidade no app Rico"))
    assertNull(parse("missing", "Compra aprovada na loja sem valor"))
    assertNull(parse("installment", "Compra aprovada: total R$ 90,00, parcela 1 de 3: R$ 30,00"))
    assertNull(parse("installment-single", "Compra aprovada de R$ 30,00; parcela 1 de 3"))
    assertNull(parse("installment-total-count", "Compra aprovada em 3x; total de R$ 90,00"))
    assertNull(parse("installment-count-only", "Compra aprovada em 3 vezes de R$ 30,00"))
    assertNull(parse("multiple-values", "Compra aprovada de R$ 8,89; saldo R$ 100,00"))
    assertNull(parse("foreign-usd", "Compra aprovada de US$ 2,00 (R$ 10,00)"))
    assertNull(parse("foreign-converted", "Compra aprovada de R$ 10,00 em USD"))
  }

  @Test
  fun groupsOnlyOneClearAmountButRejectsMalformedTruncatedAndContradictoryContent() {
    val grouped =
        parse(
            key = "grouped",
            content =
                NotificationText(
                    title = "Rico",
                    text = "Compra aprovada",
                    bigText = "No valor de R$ 8,89",
                ),
        )
    val truncated = parse("truncated", "Compra aprovada no cartão final 1234 por R$ 8,89")

    assertEquals(-889L, grouped?.amountCents)
    assertEquals(-889L, truncated?.amountCents)
    assertNull(parse("malformed-separator", "Compra aprovada de R$ 8.89"))
    assertNull(parse("malformed-cents", "Compra aprovada de R$ 8,9"))
    assertNull(parse("contradictory", "Compra aprovada de R$ 8,89; total corrigido R$ 9,89"))
    assertNull(parse("truncated-amount", "Compra aprovada de R$ 8,"))
  }

  @Test
  fun usesObservedTimeForDateWhenNotificationTextHasAnAmbiguousDate() {
    val candidate =
        parse(
            key = "ambiguous-date",
            text = "Compra aprovada em 01/10 de R$ 8,89",
            instant = Instant.parse("2026-10-01T01:30:00Z"),
        )

    assertEquals("2026-09-30", candidate?.date)
  }

  @Test
  fun blankEventKeysAndUnrecognizedForeignOrMissingValuesStayOutsideImport() {
    var read = false
    val blankKey =
        NotificationImportParser.parseIfEnabled(
            sourcePackage = NotificationImportSource.RICO.packageName,
            sourceEnabled = true,
            sourceEventKey = " ",
            postedAt = postedAt,
            readContent = {
              read = true
              NotificationText(text = "Compra aprovada de R$ 8,89")
            },
        )

    assertNull(blankKey)
    assertFalse(read)
    assertNull(parse("foreign-eur", "Compra aprovada de EUR 2,00 (R$ 12,00)"))
    assertNull(parse("unrecognized", "Seu resumo mensal está disponível: R$ 8,89"))
    assertNull(parse("missing-value", "Pix recebido de Ana"))
  }

  @Test
  fun sourceEventFingerprintSeparatesChangedOrReusedAndroidKeys() {
    val first = parse("reused-key", "Compra aprovada no cartão final 1234 de R$ 8,89")
    val replay = parse("reused-key", "Compra aprovada no cartão final 1234 de R$ 8,89")
    val changedContent =
        parse("reused-key", "Compra aprovada na outra loja no cartão final 1234 de R$ 8,89")
    val changedTime =
        parse(
            key = "reused-key",
            text = "Compra aprovada no cartão final 1234 de R$ 8,89",
            instant = postedAt.plusMillis(1),
        )
    val distinctAndroidKey =
        parse(
            key = "new-android-key",
            text = "Compra aprovada no cartão final 1234 de R$ 8,89",
        )

    assertEquals(first?.sourceEventHash, replay?.sourceEventHash)
    assertNotEquals(first?.sourceEventHash, changedContent?.sourceEventHash)
    assertNotEquals(first?.sourceEventHash, changedTime?.sourceEventHash)
    assertNotEquals(first?.sourceEventHash, distinctAndroidKey?.sourceEventHash)
    assertEquals(first?.fingerprintHash, replay?.fingerprintHash)
    assertEquals(first?.fingerprintHash, changedContent?.fingerprintHash)
    assertEquals(first?.fingerprintHash, distinctAndroidKey?.fingerprintHash)
  }

  private fun parse(
      key: String,
      text: String,
      instant: Instant = postedAt,
      requiredFeedSuffix: String? = null,
  ) = parse(key, NotificationText(text = text), instant, requiredFeedSuffix)

  private fun parse(
      key: String,
      content: NotificationText,
      instant: Instant = postedAt,
      requiredFeedSuffix: String? = null,
  ) =
      NotificationImportParser.parse(
          source = NotificationImportSource.RICO,
          sourceEventKey = key,
          postedAt = instant,
          content = content,
          zoneId = zone,
          requiredFeedSuffix = requiredFeedSuffix,
      )
}
