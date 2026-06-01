package net.aegisnet.app.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsStoreTest {
    @Test
    fun snapshotReturnsEventsInInsertionOrder() {
        val store = DiagnosticsStore()
        val first = DiagnosticEvent(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Vpn,
            message = "first",
            timestampMillis = 1L,
        )
        val second = DiagnosticEvent(
            level = DiagnosticLevel.Warning,
            source = DiagnosticSource.Runtime,
            message = "second",
            timestampMillis = 2L,
        )

        store.add(first)
        store.add(second)

        assertEquals(listOf(first, second), store.snapshot())
    }

    @Test
    fun clearRemovesEvents() {
        val store = DiagnosticsStore()
        store.add(
            DiagnosticEvent(
                level = DiagnosticLevel.Info,
                source = DiagnosticSource.System,
                message = "event",
            ),
        )

        store.clear()

        assertTrue(store.snapshot().isEmpty())
    }
}
