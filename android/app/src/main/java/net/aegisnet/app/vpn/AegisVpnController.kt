package net.aegisnet.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import net.aegisnet.app.diagnostics.DiagnosticsStore

object AegisVpnController {
    private val machine = VpnStateMachine()
    private val diagnosticsStore = DiagnosticsStore()
    private val mutableState = MutableStateFlow(machine.state)
    private val mutableDiagnostics = MutableStateFlow<List<DiagnosticEvent>>(emptyList())

    val state: StateFlow<VpnState> = mutableState.asStateFlow()
    val diagnostics: StateFlow<List<DiagnosticEvent>> = mutableDiagnostics.asStateFlow()

    @Synchronized
    fun connect() = apply(machine.connect())

    @Synchronized
    fun consentPrepared() = apply(machine.consentPrepared())

    @Synchronized
    fun serviceStarted() = apply(machine.serviceStarted())

    @Synchronized
    fun tunnelEstablished() = apply(machine.tunnelEstablished())

    @Synchronized
    fun disconnect() = apply(machine.disconnect())

    @Synchronized
    fun stopped() = apply(machine.stopped())

    @Synchronized
    fun revoked() = apply(machine.revoked())

    @Synchronized
    fun revokeHandled() = apply(machine.revokeHandled())

    @Synchronized
    fun fail(message: String) = apply(machine.fail(message))

    @Synchronized
    fun cleanupError() = apply(machine.cleanupError())

    @Synchronized
    fun addDiagnostic(
        level: DiagnosticLevel,
        source: DiagnosticSource,
        message: String,
    ) {
        addEvent(
            DiagnosticEvent(
                level = level,
                source = source,
                message = message,
            ),
        )
    }

    private fun apply(result: VpnTransitionResult): VpnTransitionResult {
        mutableState.value = result.currentState
        addEvent(result.diagnostic)
        return result
    }

    private fun addEvent(event: DiagnosticEvent) {
        diagnosticsStore.add(event)
        mutableDiagnostics.value = diagnosticsStore.snapshot()
    }
}
