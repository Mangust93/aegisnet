package net.aegisnet.app.vpn

import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnStateMachineTest {
    @Test
    fun connectFlowReachesRunning() {
        val machine = VpnStateMachine()

        assertEquals(VpnState.PreparingConsent, machine.connect().currentState)
        assertEquals(VpnState.StartingService, machine.consentPrepared().currentState)
        assertEquals(VpnState.EstablishingTunnel, machine.serviceStarted().currentState)
        val result = machine.tunnelEstablished()

        assertEquals(VpnState.Running, result.currentState)
        assertEquals(DiagnosticSource.Vpn, result.diagnostic.source)
        assertTrue(result.changed)
    }

    @Test
    fun connectIsAcceptedOnlyFromIdleOrError() {
        val machine = VpnStateMachine(VpnState.Running)

        val ignored = machine.connect()

        assertEquals(VpnState.Running, ignored.currentState)
        assertFalse(ignored.changed)

        machine.fail("boom")
        val retry = machine.connect()

        assertEquals(VpnState.PreparingConsent, retry.currentState)
        assertTrue(retry.changed)
    }

    @Test
    fun disconnectIsAcceptedFromAnyNonIdleState() {
        val machine = VpnStateMachine(VpnState.PreparingConsent)

        val result = machine.disconnect()

        assertEquals(VpnState.Stopping, result.currentState)
        assertTrue(result.changed)
    }

    @Test
    fun revokeWinsOverActiveState() {
        val machine = VpnStateMachine(VpnState.Stopping)

        val result = machine.revoked()

        assertEquals(VpnState.Revoked, result.currentState)
        assertEquals(DiagnosticLevel.Warning, result.diagnostic.level)
    }

    @Test
    fun activeErrorTransitionsToErrorThenIdleAfterCleanup() {
        val machine = VpnStateMachine(VpnState.EstablishingTunnel)

        val failed = machine.fail("fd closed")

        assertEquals(VpnState.Error("fd closed"), failed.currentState)
        assertEquals(DiagnosticLevel.Error, failed.diagnostic.level)
        assertEquals(VpnState.Idle, machine.cleanupError().currentState)
    }
}
