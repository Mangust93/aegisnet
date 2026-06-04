package net.aegisnet.app.vpn.experiment

import java.io.IOException
import java.net.Socket
import java.net.SocketAddress
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import net.aegisnet.app.diagnostics.DiagnosticsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DummySocketProtectExperimentTest {
    @Test
    fun runCreatesProtectsSkipsConnectClosesAndCompletes() {
        val store = DiagnosticsStore()
        val socket = TrackingSocket()
        var protectedSocket: Socket? = null
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            socketProtector = DummySocketProtector {
                protectedSocket = it
                assertFalse(it.isConnected)
                true
            },
            runId = "run-1",
            vpnLifecycleStateProvider = { "Running" },
            timestampProvider = { 100L },
            socketFactory = DummySocketFactory { socket },
        )

        val result = experiment.run()
        val secondCleanupResult = experiment.cleanup()

        assertEquals(DummySocketProtectExperimentRunResult.Completed, result)
        assertEquals(DummySocketProtectExperimentCleanupResult.AlreadyCompleted, secondCleanupResult)
        assertSame(socket, protectedSocket)
        assertEquals(0, socket.connectCalls)
        assertTrue(socket.closed)
        assertEquals(
            listOf(
                "protect_experiment_started runId=run-1 vpnLifecycleState=Running reason=dummy socket protect experiment started",
                "dummy_socket_create_started runId=run-1 vpnLifecycleState=Running reason=creating unconnected TCP socket",
                "dummy_socket_create_succeeded runId=run-1 vpnLifecycleState=Running reason=unconnected TCP socket created",
                "socket_protect_started runId=run-1 vpnLifecycleState=Running reason=protecting socket before connect",
                "socket_protect_succeeded runId=run-1 vpnLifecycleState=Running reason=socket protected before connect",
                "socket_connect_skipped runId=run-1 vpnLifecycleState=Running reason=stage 2.1 does not connect dummy socket",
                "protect_experiment_completed runId=run-1 vpnLifecycleState=Running reason=dummy socket protect experiment completed",
                "protect_experiment_cleanup_started runId=run-1 vpnLifecycleState=Running reason=cleanup started",
                "dummy_socket_closed runId=run-1 vpnLifecycleState=Running reason=dummy socket closed",
                "protect_experiment_cleanup_completed runId=run-1 vpnLifecycleState=Running reason=cleanup completed",
            ),
            store.snapshot().map { it.message },
        )
        assertEquals(
            List(10) { DiagnosticLevel.Info },
            store.snapshot().map { it.level },
        )
        assertEquals(
            List(10) { DiagnosticSource.Vpn },
            store.snapshot().map { it.source },
        )
    }

    @Test
    fun protectReturningFalseFailsAndClosesSocket() {
        val store = DiagnosticsStore()
        val socket = TrackingSocket()
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            socketProtector = DummySocketProtector { false },
            runId = "run-2",
            vpnLifecycleStateProvider = { "EstablishingTunnel" },
            timestampProvider = { 200L },
            socketFactory = DummySocketFactory { socket },
        )

        val result = experiment.run()

        assertEquals(DummySocketProtectExperimentRunResult.Failed, result)
        assertEquals(0, socket.connectCalls)
        assertTrue(socket.closed)
        assertEquals(
            listOf(
                "protect_experiment_started",
                "dummy_socket_create_started",
                "dummy_socket_create_succeeded",
                "socket_protect_started",
                "socket_protect_failed",
                "protect_experiment_failed",
                "protect_experiment_cleanup_started",
                "dummy_socket_closed",
                "protect_experiment_cleanup_completed",
            ),
            store.snapshot().map { it.message.substringBefore(" runId=") },
        )
        assertEquals(DiagnosticLevel.Error, store.snapshot()[4].level)
        assertEquals(DiagnosticLevel.Error, store.snapshot()[5].level)
    }

    @Test
    fun socketCreateFailureFailsSkipsProtectAndCompletesCleanup() {
        val store = DiagnosticsStore()
        var protectCalls = 0
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            socketProtector = DummySocketProtector {
                protectCalls += 1
                true
            },
            runId = "run-3",
            vpnLifecycleStateProvider = { "Running" },
            timestampProvider = { 300L },
            socketFactory = DummySocketFactory { throw IOException("allocation denied") },
        )

        val result = experiment.run()

        assertEquals(DummySocketProtectExperimentRunResult.Failed, result)
        assertEquals(0, protectCalls)
        assertEquals(
            listOf(
                "protect_experiment_started",
                "dummy_socket_create_started",
                "dummy_socket_create_failed",
                "protect_experiment_failed",
                "protect_experiment_cleanup_started",
                "protect_experiment_cleanup_completed",
            ),
            store.snapshot().map { it.message.substringBefore(" runId=") },
        )
        assertEquals(DiagnosticLevel.Error, store.snapshot()[2].level)
        assertEquals(DiagnosticLevel.Error, store.snapshot()[3].level)
    }

    @Test
    fun cleanupIsIdempotentBeforeRun() {
        val store = DiagnosticsStore()
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            socketProtector = DummySocketProtector { true },
            runId = "run-4",
            vpnLifecycleStateProvider = { "Stopping" },
            timestampProvider = { 400L },
        )

        val firstResult = experiment.cleanup()
        val secondResult = experiment.cleanup()

        assertEquals(DummySocketProtectExperimentCleanupResult.Completed, firstResult)
        assertEquals(DummySocketProtectExperimentCleanupResult.AlreadyCompleted, secondResult)
        assertEquals(
            listOf(
                "protect_experiment_cleanup_started runId=run-4 vpnLifecycleState=Stopping reason=cleanup started",
                "protect_experiment_cleanup_completed runId=run-4 vpnLifecycleState=Stopping reason=cleanup completed",
            ),
            store.snapshot().map { it.message },
        )
    }

    @Test
    fun cleanupFailureIsReportedAndNotRetried() {
        val store = DiagnosticsStore()
        val socket = TrackingSocket(closeFailure = IOException("close denied"))
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            socketProtector = DummySocketProtector { true },
            runId = "run-5",
            vpnLifecycleStateProvider = { "Running" },
            timestampProvider = { 500L },
            socketFactory = DummySocketFactory { socket },
        )

        val runResult = experiment.run()
        val secondCleanupResult = experiment.cleanup()

        assertEquals(DummySocketProtectExperimentRunResult.Completed, runResult)
        assertEquals(DummySocketProtectExperimentCleanupResult.AlreadyCompleted, secondCleanupResult)
        assertEquals(
            "protect_experiment_cleanup_failed",
            store.snapshot().last().message.substringBefore(" runId="),
        )
        assertEquals(DiagnosticLevel.Error, store.snapshot().last().level)
    }
}

private class TrackingSocket(
    private val closeFailure: IOException? = null,
) : Socket() {
    var connectCalls = 0
    var closed = false

    override fun connect(endpoint: SocketAddress?) {
        connectCalls += 1
    }

    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        connectCalls += 1
    }

    override fun close() {
        closed = true
        closeFailure?.let { throw it }
        super.close()
    }
}
