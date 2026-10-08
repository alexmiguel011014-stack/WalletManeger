package com.example.allufinancas.notification

import android.content.Context
import androidx.room.withTransaction
import com.example.allufinancas.data.DemoDatabase
import com.example.allufinancas.data.DemoPreferences
import com.example.allufinancas.data.ImportNoticeRow
import com.example.allufinancas.data.ImportNoticeStatus
import com.example.allufinancas.data.NotificationImportCandidate
import com.example.allufinancas.data.NotificationImportGate
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Writes only fixed-label, parser-produced notification candidates into the isolated review queue.
 */
class NotificationImportCapture(
    context: Context,
    private val afterConfigurationCheck: suspend () -> Unit = {},
) {
  private val database = DemoDatabase.open(context.applicationContext)
  private val dao = database.dao()
  private val preferences = DemoPreferences(context.applicationContext)

  /** Returns false for a disabled source, repeated source event, or malformed candidate. */
  suspend fun capture(candidate: NotificationImportCandidate): Boolean {
    return NotificationImportGate.fence {
      // Recheck the source and filter while fenced with disable/discard/reset operations.
      if (preferences.enabledImportSource.first() != candidate.source) return@fence false
      if (preferences.importFeedSuffix.first() != candidate.feedSuffix) return@fence false
      val safe = candidate.safeOrNull() ?: return@fence false
      afterConfigurationCheck()
      database.withTransaction {
        if (dao.importNotice(safe.id) != null) return@withTransaction false
        val possibleDuplicate =
            dao.allTransactions().any { transaction ->
              transaction.amountCents == safe.amountCents && transaction.date == safe.date
            } ||
                dao.allImportNotices().any { notice ->
                  notice.fingerprintHash == safe.fingerprintHash
                }
        dao.insertImportNotice(safe.copy(possibleDuplicate = possibleDuplicate)) != -1L
      }
    }
  }

  private fun NotificationImportCandidate.safeOrNull(): ImportNoticeRow? {
    if (!sourceEventHash.isSha256() || !fingerprintHash.isSha256()) return null
    if (amountCents == 0L || amountCents !in -MAX_IMPORT_AMOUNT_CENTS..MAX_IMPORT_AMOUNT_CENTS)
        return null
    if (
        (kind == com.example.allufinancas.data.ImportNoticeKind.EXPENSE && amountCents >= 0) ||
            (kind != com.example.allufinancas.data.ImportNoticeKind.EXPENSE && amountCents <= 0)
    )
        return null
    if (runCatching { LocalDate.parse(date).toString() == date }.getOrDefault(false).not())
        return null
    if (runCatching { Instant.parse(observedAt) }.isFailure) return null
    val safeMerchant = kind.safeMerchantPlaceholder()
    val expectedFingerprint =
        NotificationImportFingerprints.notice(source, kind, safeMerchant, amountCents, date)
    if (fingerprintHash != expectedFingerprint) return null
    return ImportNoticeRow(
        id = "notice-$sourceEventHash",
        source = source.name,
        sourceEventHash = sourceEventHash,
        fingerprintHash = fingerprintHash,
        merchant = safeMerchant,
        description = kind.safeDescription(),
        amountCents = amountCents,
        date = date,
        kind = kind.name,
        status = ImportNoticeStatus.PENDING.name,
        possibleDuplicate = false,
        observedAt = observedAt,
        confirmedTransactionId = null,
    )
  }

  private fun String.isSha256(): Boolean = matches(SHA_256)

  private companion object {
    const val MAX_IMPORT_AMOUNT_CENTS = 1_000_000_000_000L
    val SHA_256 = Regex("[0-9a-f]{64}")
  }
}
