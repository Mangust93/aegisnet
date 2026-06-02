package net.aegisnet.app.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectExperimentDiagnosticsTest {
    @Test
    fun eventNamesMatchStageSpecification() {
        assertEquals(
            listOf(
                "protect_experiment_started",
                "dummy_socket_create_started",
                "dummy_socket_create_succeeded",
                "dummy_socket_create_failed",
                "socket_protect_started",
                "socket_protect_succeeded",
                "socket_protect_failed",
                "protect_experiment_failed",
                "protect_experiment_completed",
                "protect_experiment_cleanup_started",
                "protect_experiment_cleanup_completed",
                "protect_experiment_cleanup_failed",
                "protect_experiment_revoked",
                "protect_experiment_disconnected",
            ),
            ProtectExperimentDiagnosticName.entries.map { it.eventName },
        )
    }

    @Test
    fun createsCompatibleDiagnosticEventWithContext() {
        val event = ProtectExperimentDiagnosticName.SocketProtectFailed.toDiagnosticEvent(
            context = ProtectExperimentDiagnosticContext(
                runId = "run-1",
                vpnLifecycleState = "EstablishingTunnel",
                reason = "protect returned false",
            ),
            timestampMillis = 42L,
        )

        assertEquals(DiagnosticLevel.Error, event.level)
        assertEquals(DiagnosticSource.Vpn, event.source)
        assertEquals(
            "socket_protect_failed runId=run-1 vpnLifecycleState=EstablishingTunnel reason=protect returned false",
            event.message,
        )
        assertEquals(42L, event.timestampMillis)
    }
}
