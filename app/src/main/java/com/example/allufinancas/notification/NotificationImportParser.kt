package com.example.allufinancas.notification

import com.example.allufinancas.data.ImportNoticeKind
import com.example.allufinancas.data.NotificationImportCandidate
import com.example.allufinancas.data.NotificationImportSource
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Android-free, deliberately narrow parser for Rico notices.
 *
 * [readContent] is invoked only after the source package and the local opt-in have both
 * passed. The content is used transiently for classification, amount, suffix matching, and an
 * opaque event hash. It is never copied into a persisted candidate.
 */
object NotificationImportParser {
    fun parseIfEnabled(
        sourcePackage: String,
        sourceEnabled: Boolean,
        sourceEventKey: String,
        postedAt: Instant,
        readContent: () -> NotificationText,
        zoneId: ZoneId = ZoneId.systemDefault(),
        requiredFeedSuffix: String? = null,
    ): NotificationImportCandidate? {
        val source = NotificationImportSource.fromPackage(sourcePackage) ?: return null
        if (!sourceEnabled || sourceEventKey.isBlank()) return null
        return parse(source, sourceEventKey, postedAt, readContent(), zoneId, requiredFeedSuffix)
    }

    fun parse(
        source: NotificationImportSource,
        sourceEventKey: String,
        postedAt: Instant,
        content: NotificationText,
        zoneId: ZoneId = ZoneId.systemDefault(),
        requiredFeedSuffix: String? = null,
    ): NotificationImportCandidate? {
        if (sourceEventKey.isBlank()) return null
        val suffix = requiredFeedSuffix.normalizedFeedSuffix()
        if (!requiredFeedSuffix.isNullOrBlank() && suffix == null) return null

        val summary = content.summary().take(MAX_INPUT_LENGTH)
        if (summary.isBlank() || (suffix != null && !summary.hasFeedSuffix(suffix))) return null
        val normalized = summary.normalizedForMatching()
        if (ignored(normalized) || hasForeignCurrency(normalized)) return null

        val kind = when {
            neutral(normalized) -> ImportNoticeKind.NEUTRAL
            refund(normalized) -> ImportNoticeKind.REFUND_REVIEW
            income(normalized) -> ImportNoticeKind.INCOME
            purchase(normalized) -> ImportNoticeKind.EXPENSE
            else -> return null
        }
        // Multiple values (including total + installment) are ambiguous by design.
        val absoluteCents = parseBrlCents(summary) ?: return null
        if (hasInstallmentInformation(normalized) && !hasExplicitCurrentInstallmentAmount(normalized)) return null
        val merchant = kind.safeMerchantPlaceholder()
        val date = postedAt.atZone(zoneId).toLocalDate().toString()
        val amountCents = when (kind) {
            ImportNoticeKind.EXPENSE -> -absoluteCents
            ImportNoticeKind.INCOME, ImportNoticeKind.REFUND_REVIEW, ImportNoticeKind.NEUTRAL -> absoluteCents
        }
        return NotificationImportCandidate(
            source = source,
            sourceEventHash = NotificationImportFingerprints.sourceEvent(source, sourceEventKey, summary, postedAt),
            fingerprintHash = NotificationImportFingerprints.notice(source, kind, merchant, amountCents, date),
            merchant = merchant,
            description = kind.safeDescription(),
            amountCents = amountCents,
            date = date,
            kind = kind,
            observedAt = postedAt.toString(),
            feedSuffix = suffix,
        )
    }

    /** Returns one unambiguous Brazilian-real amount only. */
    internal fun parseBrlCents(text: String): Long? {
        val matches = BRL_AMOUNT.findAll(text).toList()
        if (matches.size != 1) return null
        val match = matches.single()
        val whole = match.groupValues[1].replace(".", "").toLongOrNull() ?: return null
        val fractional = match.groupValues[2].toLongOrNull() ?: return null
        return (whole * 100 + fractional).takeIf { it in 1..MAX_IMPORT_AMOUNT_CENTS }
    }

    private fun ignored(text: String): Boolean = IGNORED_TERMS.any(text::contains)
    private fun neutral(text: String): Boolean = NEUTRAL_TERMS.any(text::contains)
    private fun refund(text: String): Boolean = REFUND_TERMS.any(text::contains)
    private fun income(text: String): Boolean = INCOME_TERMS.any(text::contains)
    private fun purchase(text: String): Boolean = PURCHASE_TERMS.any(text::contains)
    private fun hasInstallmentInformation(text: String): Boolean = INSTALLMENT_TERMS.any(text::contains)
    private fun hasExplicitCurrentInstallmentAmount(text: String): Boolean =
        CURRENT_INSTALLMENT_AMOUNT.any { pattern -> pattern.containsMatchIn(text) }
    private fun hasForeignCurrency(text: String): Boolean = FOREIGN_CURRENCY_PATTERN.containsMatchIn(text)

    private fun NotificationText.summary(): String = listOfNotNull(title, text, bigText)
        .joinToString(" ") { it.toString() }
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun String.hasFeedSuffix(suffix: String): Boolean {
        val contextualSuffix = Regex(
            """(?:\b(?:cartao|conta|final|terminad[oa]\s+em|feed)\b[^0-9]{0,16}|(?:\*|x){2,}\s*)${Regex.escape(suffix)}\b""",
        )
        return contextualSuffix.containsMatchIn(normalizedForMatching())
    }

    private fun String?.normalizedFeedSuffix(): String? = this?.trim()?.takeIf { it.matches(FEED_SUFFIX) }

    private fun String.normalizedForMatching(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)

    private const val MAX_INPUT_LENGTH = 2_000
    private const val MAX_IMPORT_AMOUNT_CENTS = 1_000_000_000_000L
    private val FEED_SUFFIX = Regex("\\d{4}")
    private val BRL_AMOUNT = Regex("""R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*|[0-9]+),([0-9]{2})""", RegexOption.IGNORE_CASE)
    private val IGNORED_TERMS = listOf(
        "recusada", "recusado", "negada", "negado", "nao aprovada",
        "pendente", "em analise", "senha", "codigo", "token",
        "promocao", "oferta", "marketing",
    )
    private val NEUTRAL_TERMS = listOf(
        "fatura paga", "fatura quitada", "pagamento da fatura", "pagamento de fatura",
        "pagamento fatura", "quitacao da fatura", "quitacao de fatura", "pagou a fatura",
        "pagou sua fatura", "fatura do cartao paga", "fatura do cartao quitada",
        "transferencia entre contas", "transferencia interna", "transferencia propria",
        "transferencia entre suas contas", "transferencia de mesma titularidade", "movimentacao interna",
    )
    private val REFUND_TERMS = listOf("estorno", "reembolso", "valor devolvido", "compra cancelada")
    private val INCOME_TERMS = listOf("pix recebido", "voce recebeu", "valor recebido", "dinheiro recebido")
    private val PURCHASE_TERMS = listOf("compra aprovada", "compra realizada", "compra no cartao", "pagamento aprovado")
    private val INSTALLMENT_TERMS = listOf("parcela", "parcelado", "parcelada")
    private val CURRENT_INSTALLMENT_AMOUNT = listOf(
        Regex("""\b(?:valor\s+d[ae]\s+parcela|valor\s+parcela|parcela\s+atual)\b(?:\s+(?:de|no\s+valor\s+de))?\s*[:\-]?\s*r\$\s*"""),
        Regex("""\bparcela\s+\d+\s+(?:de|/)\s+\d+\b(?:\s+(?:no\s+)?valor(?:\s+de)?|\s+de)?\s*[:\-]?\s*r\$\s*"""),
    )
    private val FOREIGN_CURRENCY_PATTERN = Regex("""(?:\bus\$|\busd\b|\beur\b|€|\bdolar(?:es)?\b|\beuro(?:s)?\b|\bgbp\b|£)""")
}

/** Fixed labels ensure notification text cannot enter Room through parsed merchant or description. */
internal fun ImportNoticeKind.safeMerchantPlaceholder(): String = when (this) {
    ImportNoticeKind.EXPENSE -> "Estabelecimento a confirmar"
    ImportNoticeKind.INCOME -> "Origem a confirmar"
    ImportNoticeKind.REFUND_REVIEW -> "Estorno ou reembolso"
    ImportNoticeKind.NEUTRAL -> "Fatura ou transferência"
}

internal fun ImportNoticeKind.safeDescription(): String = when (this) {
    ImportNoticeKind.EXPENSE -> "Compra aprovada pela Rico"
    ImportNoticeKind.INCOME -> "Valor recebido pela Rico"
    ImportNoticeKind.NEUTRAL -> "Fatura paga ou transferência"
    ImportNoticeKind.REFUND_REVIEW -> "Estorno ou reembolso para revisão"
}

/** Only opaque SHA-256 values cross the parser/persistence boundary. */
object NotificationImportFingerprints {
    /** Includes normalized transient content and post time, so an updated/reused Android key is distinct. */
    fun sourceEvent(
        source: NotificationImportSource,
        sourceEventKey: String,
        notificationContent: String,
        postedAt: Instant,
    ): String = sha256(
        listOf(
            source.packageName,
            sourceEventKey,
            notificationContent.normalizedForEventHash(),
            postedAt.toEpochMilli().toString(),
        ).joinToString("\u0000"),
    )

    /** Retained for callers that only need the legacy opaque source/key hash. */
    fun sourceEvent(source: NotificationImportSource, sourceEventKey: String): String =
        sha256("${source.packageName}\u0000$sourceEventKey")

    fun notice(
        source: NotificationImportSource,
        kind: ImportNoticeKind,
        merchant: String,
        amountCents: Long,
        date: String,
    ): String = sha256(
        listOf(source.name, kind.name, merchant.lowercase(Locale.ROOT), amountCents.toString(), date)
            .joinToString("\u0000"),
    )

    private fun String.normalizedForEventHash(): String = trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

/** Ephemeral Android notification fields, read only after the source guard. */
data class NotificationText(
    val title: CharSequence? = null,
    val text: CharSequence? = null,
    val bigText: CharSequence? = null,
)
