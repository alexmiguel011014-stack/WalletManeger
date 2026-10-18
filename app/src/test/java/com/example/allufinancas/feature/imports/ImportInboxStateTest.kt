package com.example.allufinancas.feature.imports

import com.example.allufinancas.data.NotificationImportSource
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportInboxStateTest {
  @Test
  fun importConnectionStateCoversSourceInstallationAndPermissionCombinations() {
    val source = NotificationImportSource.RICO

    assertEquals(
        ImportConnectionState.DISCONNECTED,
        importConnectionState(null, sourceInstalled = false, listenerAccessEnabled = false),
    )
    assertEquals(
        ImportConnectionState.DISCONNECTED,
        importConnectionState(null, sourceInstalled = false, listenerAccessEnabled = true),
    )
    assertEquals(
        ImportConnectionState.DISCONNECTED,
        importConnectionState(null, sourceInstalled = true, listenerAccessEnabled = false),
    )
    assertEquals(
        ImportConnectionState.DISCONNECTED,
        importConnectionState(null, sourceInstalled = true, listenerAccessEnabled = true),
    )
    assertEquals(
        ImportConnectionState.SOURCE_UNAVAILABLE,
        importConnectionState(source, sourceInstalled = false, listenerAccessEnabled = false),
    )
    assertEquals(
        ImportConnectionState.SOURCE_UNAVAILABLE,
        importConnectionState(source, sourceInstalled = false, listenerAccessEnabled = true),
    )
    assertEquals(
        ImportConnectionState.PERMISSION_UNAVAILABLE,
        importConnectionState(source, sourceInstalled = true, listenerAccessEnabled = false),
    )
    assertEquals(
        ImportConnectionState.READY,
        importConnectionState(source, sourceInstalled = true, listenerAccessEnabled = true),
    )
  }
}
