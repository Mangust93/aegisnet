package net.aegisnet.app.vpn.experiment

import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import net.aegisnet.app.diagnostics.DiagnosticsStore
import org.junit.Assert.assertEquals
import org.junit.Test

class DummySocketProtectExperimentTest {
    @Test
    fun runEmitsSkeletonStartAndCompletedDiagnosticsOnly() {
        val store = DiagnosticsStore()
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            runId = "run-1",
            vpnLifecycleStateProvider = { "Running" },
            timestampProvider = { 100L },
        )

        val result = experiment.run()

        assertEquals(DummySocketProtectExperimentRunResult.Completed, result)
        assertEquals(
            listOf(
                "protect_experiment_started runId=run-1 vpnLifecycleState=Running reason=skeleton no socket created",
                "protect_experiment_completed runId=run-1 vpnLifecycleState=Running reason=skeleton no socket created",
            ),
            store.snapshot().map { it.message },
        )
        assertEquals(
            listOf(DiagnosticLevel.Info, DiagnosticLevel.Info),
            store.snapshot().map { it.level },
        )
        assertEquals(
            listOf(DiagnosticSource.Vpn, DiagnosticSource.Vpn),
            store.snapshot().map { it.source },
        )
    }

    @Test
    fun cleanupEmitsSkeletonCleanupDiagnosticsOnce() {
        val store = DiagnosticsStore()
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            runId = "run-2",
            vpnLifecycleStateProvider = { "Stopping" },
            timestampProvider = { 200L },
        )

        val firstResult = experiment.cleanup()
        val secondResult = experiment.cleanup()

        assertEquals(DummySocketProtectExperimentCleanupResult.Completed, firstResult)
        assertEquals(DummySocketProtectExperimentCleanupResult.AlreadyCompleted, secondResult)
        assertEquals(
            listOf(
                "protect_experiment_cleanup_started runId=run-2 vpnLifecycleState=Stopping reason=skeleton no socket created",
                "protect_experiment_cleanup_completed runId=run-2 vpnLifecycleState=Stopping reason=skeleton no socket created",
            ),
            store.snapshot().map { it.message },
        )
    }

    @Test
    fun runDoesNotCreateSocketProtectionOrFailureDiagnostics() {
        val store = DiagnosticsStore()
        val experiment = DummySocketProtectExperiment(
            diagnosticsStore = store,
            runId = "run-3",
            vpnLifecycleStateProvider = { "EstablishingTunnel" },
            timestampProvider = { 300L },
        )

        experiment.run()

        val messages = store.snapshot().map { it.message }
        assertEquals(2, messages.size)
        assertEquals(false, messages.any { it.startsWith("dummy_socket_create_") })
        assertEquals(false, messages.any { it.startsWith("socket_protect_") })
        assertEquals(false, messages.any { it.startsWith("protect_experiment_failed") })
    }
}
