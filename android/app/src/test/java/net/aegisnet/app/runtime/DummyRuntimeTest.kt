package net.aegisnet.app.runtime

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DummyRuntimeTest {
    @Test
    fun startTransitionsToRunningAndEmitsDiagnostics() = runBlocking {
        val runtime = DummyRuntime(startupDelayMillis = 1L)

        runtime.start(RuntimeConfig(sessionId = "test-session", tunFd = null))
        val diagnostics = runtime.diagnostics.take(2).toList()

        assertEquals(RuntimeState.Running, runtime.state.value)
        assertEquals(
            listOf(
                "Dummy runtime starting session test-session (Stopped -> Starting)",
                "Dummy runtime running session test-session (Starting -> Running)",
            ),
            diagnostics.map { it.message },
        )
        assertTrue(diagnostics.all { it.source == DiagnosticSource.Runtime })
        assertTrue(diagnostics.all { it.level == DiagnosticLevel.Info })
    }

    @Test
    fun stopTransitionsToStoppedAndEmitsDiagnostics() = runBlocking {
        val runtime = DummyRuntime(startupDelayMillis = 1L)

        runtime.start(RuntimeConfig(sessionId = "test-session", tunFd = null))
        runtime.stop()
        val diagnostics = runtime.diagnostics.take(4).toList()

        assertEquals(RuntimeState.Stopped, runtime.state.value)
        assertEquals(
            listOf(
                "Dummy runtime starting session test-session (Stopped -> Starting)",
                "Dummy runtime running session test-session (Starting -> Running)",
                "Dummy runtime stopping (Running -> Stopping)",
                "Dummy runtime stopped (Stopping -> Stopped)",
            ),
            diagnostics.map { it.message },
        )
    }

    @Test
    fun startAcceptsTunFdWithoutChangingDummyBehavior() = runBlocking {
        val runtime = DummyRuntime(startupDelayMillis = 1L)

        runtime.start(RuntimeConfig(sessionId = "fd-session", tunFd = 42))
        val diagnostics = runtime.diagnostics.take(2).toList()

        assertEquals(RuntimeState.Running, runtime.state.value)
        assertEquals(
            listOf(
                "Dummy runtime starting session fd-session (Stopped -> Starting)",
                "Dummy runtime running session fd-session (Starting -> Running)",
            ),
            diagnostics.map { it.message },
        )
    }
}
