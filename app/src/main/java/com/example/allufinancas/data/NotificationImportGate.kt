package com.example.allufinancas.data

import kotlinx.coroutines.sync.Mutex

/**
 * Serializes local notification capture with changes that disable, discard, reset, confirm,
 * or reopen the import queue. It is intentionally process-local: Room remains authoritative.
 */
internal object NotificationImportGate {
    private val mutex = Mutex()

    suspend fun <T> fence(block: suspend () -> T): T {
        mutex.lock()
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}
