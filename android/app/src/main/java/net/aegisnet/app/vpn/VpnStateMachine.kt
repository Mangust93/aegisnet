package net.aegisnet.app.vpn

import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource

class VpnStateMachine(
    initialState: VpnState = VpnState.Idle,
) {
    var state: VpnState = initialState
        private set

    fun connect(): VpnTransitionResult {
        return when (state) {
            VpnState.Idle,
            is VpnState.Error,
            -> transitionTo(VpnState.PreparingConsent, "VPN connect requested")

            else -> stay("VPN connect ignored from ${state.name}")
        }
    }

    fun consentPrepared(): VpnTransitionResult {
        return requireState<VpnState.PreparingConsent>(
            nextState = VpnState.StartingService,
            message = "VPN consent prepared",
        )
    }

    fun serviceStarted(): VpnTransitionResult {
        return requireState<VpnState.StartingService>(
            nextState = VpnState.EstablishingTunnel,
            message = "VPN service started",
        )
    }

    fun tunnelEstablished(): VpnTransitionResult {
        return requireState<VpnState.EstablishingTunnel>(
            nextState = VpnState.Running,
            message = "VPN tunnel established",
        )
    }

    fun disconnect(): VpnTransitionResult {
        return if (state.isIdle) {
            stay("VPN disconnect ignored from ${state.name}")
        } else {
            transitionTo(VpnState.Stopping, "VPN disconnect requested")
        }
    }

    fun stopped(): VpnTransitionResult {
        return requireState<VpnState.Stopping>(
            nextState = VpnState.Idle,
            message = "VPN stopped",
        )
    }

    fun revoked(): VpnTransitionResult {
        return if (state.isIdle) {
            stay("VPN revoke ignored from ${state.name}")
        } else {
            transitionTo(VpnState.Revoked, "VPN revoked by system", DiagnosticLevel.Warning)
        }
    }

    fun revokeHandled(): VpnTransitionResult {
        return requireState<VpnState.Revoked>(
            nextState = VpnState.Idle,
            message = "VPN revoke handled",
        )
    }

    fun fail(message: String): VpnTransitionResult {
        return if (state.isIdle) {
            stay("VPN error ignored from ${state.name}: $message", DiagnosticLevel.Warning)
        } else {
            transitionTo(VpnState.Error(message), "VPN error: $message", DiagnosticLevel.Error)
        }
    }

    fun cleanupError(): VpnTransitionResult {
        return requireState<VpnState.Error>(
            nextState = VpnState.Idle,
            message = "VPN error cleaned up",
        )
    }

    private inline fun <reified T : VpnState> requireState(
        nextState: VpnState,
        message: String,
    ): VpnTransitionResult {
        return if (state is T) {
            transitionTo(nextState, message)
        } else {
            stay("VPN transition ignored from ${state.name}: $message")
        }
    }

    private fun transitionTo(
        nextState: VpnState,
        message: String,
        level: DiagnosticLevel = DiagnosticLevel.Info,
    ): VpnTransitionResult {
        val previousState = state
        state = nextState
        return VpnTransitionResult(
            previousState = previousState,
            currentState = nextState,
            diagnostic = diagnostic(level, "$message (${previousState.name} -> ${nextState.name})"),
            changed = true,
        )
    }

    private fun stay(
        message: String,
        level: DiagnosticLevel = DiagnosticLevel.Debug,
    ): VpnTransitionResult {
        return VpnTransitionResult(
            previousState = state,
            currentState = state,
            diagnostic = diagnostic(level, message),
            changed = false,
        )
    }

    private fun diagnostic(
        level: DiagnosticLevel,
        message: String,
    ) = DiagnosticEvent(
        level = level,
        source = DiagnosticSource.Vpn,
        message = message,
    )
}

data class VpnTransitionResult(
    val previousState: VpnState,
    val currentState: VpnState,
    val diagnostic: DiagnosticEvent,
    val changed: Boolean,
)

val VpnState.name: String
    get() = when (this) {
        VpnState.Idle -> "Idle"
        VpnState.PreparingConsent -> "PreparingConsent"
        VpnState.StartingService -> "StartingService"
        VpnState.EstablishingTunnel -> "EstablishingTunnel"
        VpnState.Running -> "Running"
        VpnState.Stopping -> "Stopping"
        VpnState.Revoked -> "Revoked"
        is VpnState.Error -> "Error"
    }
