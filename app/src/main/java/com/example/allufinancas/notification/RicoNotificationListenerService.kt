package com.example.allufinancas.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.allufinancas.data.DemoPreferences
import com.example.allufinancas.data.NotificationImportSource
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives Android's notification callback, but reads notification extras only for the locally
 * enabled Rico package. No notification content is logged or sent over the network.
 */
class RicoNotificationListenerService : NotificationListenerService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private lateinit var preferences: DemoPreferences
  private lateinit var capture: NotificationImportCapture

  override fun onCreate() {
    super.onCreate()
    preferences = DemoPreferences(applicationContext)
    capture = NotificationImportCapture(applicationContext)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    // This package guard intentionally precedes access to sbn.notification or its extras.
    val source = NotificationImportSource.fromPackage(sbn.packageName) ?: return
    serviceScope.launch {
      if (preferences.enabledImportSource.first() != source) return@launch
      val requiredFeedSuffix = preferences.importFeedSuffix.first()
      val candidate =
          NotificationImportParser.parseIfEnabled(
              sourcePackage = sbn.packageName,
              sourceEnabled = true,
              sourceEventKey = sbn.key,
              postedAt = Instant.ofEpochMilli(sbn.postTime),
              requiredFeedSuffix = requiredFeedSuffix,
              readContent = {
                val extras = sbn.notification.extras
                NotificationText(
                    title = extras.getCharSequence(Notification.EXTRA_TITLE),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT),
                    bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                )
              },
          )
      if (candidate != null) capture.capture(candidate)
    }
  }

  override fun onDestroy() {
    serviceScope.cancel()
    super.onDestroy()
  }
}
