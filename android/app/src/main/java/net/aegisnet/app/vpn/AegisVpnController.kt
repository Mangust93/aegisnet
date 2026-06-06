package net.aegisnet.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import net.aegisnet.app.diagnostics.DiagnosticsStore
import net.aegisnet.app.firewall.AppVpnMode
import net.aegisnet.app.networking.NetworkingLabTest
import net.aegisnet.app.networking.NetworkingLabTestResult
import net.aegisnet.app.networking.initialNetworkingLabResults
import net.aegisnet.app.runtime.RuntimeState

object AegisVpnController {
    private val machine = VpnStateMachine()
    private val diagnosticsStore = DiagnosticsStore()
    private val mutableState = MutableStateFlow(machine.state)
    private val mutableRuntimeState = MutableStateFlow<RuntimeState>(RuntimeState.Stopped)
    private val mutableDiagnostics = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    private val mutableSessionStartedAtMillis = MutableStateFlow<Long?>(null)
    private val mutableForegroundNotificationActive = MutableStateFlow(false)
    private val mutableNetworkingLabResults = MutableStateFlow(initialNetworkingLabResults)
    private val mutableCurrentMode = MutableStateFlow(AppVpnMode.Diagnostics)
    private val mutableActiveBlockedAppCount = MutableStateFlow(0)
    private val mutableLastFirewallResult = MutableStateFlow("Not run")
    private val mutableLastRuntimeError = MutableStateFlow("None")

    val state: StateFlow<VpnState> = mutableState.asStateFlow()
    val runtimeState: StateFlow<RuntimeState> = mutableRuntimeState.asStateFlow()
    val diagnostics: StateFlow<List<DiagnosticEvent>> = mutableDiagnostics.asStateFlow()
    val sessionStartedAtMillis: StateFlow<Long?> = mutableSessionStartedAtMillis.asStateFlow()
    val foregroundNotificationActive: StateFlow<Boolean> = mutableForegroundNotificationActive.asStateFlow()
    val networkingLabResults: StateFlow<Map<NetworkingLabTest, NetworkingLabTestResult>> =
        mutableNetworkingLabResults.asStateFlow()
    val currentMode: StateFlow<AppVpnMode> = mutableCurrentMode.asStateFlow()
    val activeBlockedAppCount: StateFlow<Int> = mutableActiveBlockedAppCount.asStateFlow()
    val lastFirewallResult: StateFlow<String> = mutableLastFirewallResult.asStateFlow()
    val lastRuntimeError: StateFlow<String> = mutableLastRuntimeError.asStateFlow()

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

    @Synchronized
    fun addDiagnostic(event: DiagnosticEvent) {
        addEvent(event)
    }

    @Synchronized
    fun updateRuntimeState(state: RuntimeState) {
        mutableRuntimeState.value = state
        if (state is RuntimeState.Failed) {
            mutableLastRuntimeError.value = state.message
        }
    }

    @Synchronized
    fun updateLastRuntimeError(message: String) {
        mutableLastRuntimeError.value = message
    }

    @Synchronized
    fun updateForegroundNotificationActive(active: Boolean) {
        mutableForegroundNotificationActive.value = active
    }

    @Synchronized
    fun updateNetworkingLabResult(
        test: NetworkingLabTest,
        result: NetworkingLabTestResult,
    ) {
        mutableNetworkingLabResults.value = mutableNetworkingLabResults.value + (test to result)
    }

    @Synchronized
    fun selectMode(mode: AppVpnMode) {
        mutableCurrentMode.value = mode
        if (mode == AppVpnMode.AppFirewall) {
            addDiagnostic(
                level = DiagnosticLevel.Info,
                source = DiagnosticSource.Ui,
                message = "app_firewall_mode_selected",
            )
        }
    }

    @Synchronized
    fun updateActiveFirewall(mode: AppVpnMode, blockedAppCount: Int) {
        mutableCurrentMode.value = mode
        mutableActiveBlockedAppCount.value =
            if (mode == AppVpnMode.AppFirewall) blockedAppCount.coerceAtLeast(0) else 0
    }

    @Synchronized
    fun updateLastFirewallResult(result: String) {
        mutableLastFirewallResult.value = result
    }

    @Synchronized
    fun clearDiagnostics() {
        diagnosticsStore.clear()
        mutableDiagnostics.value = diagnosticsStore.snapshot()
    }

    @Synchronized
    fun diagnosticsText(): String {
        return diagnosticsStore.snapshot().joinToString(separator = "\n") { event ->
            "${event.timestampMillis} ${event.level} ${event.source}: ${event.message}"
        }
    }

    private fun apply(result: VpnTransitionResult): VpnTransitionResult {
        mutableState.value = result.currentState
        if (result.changed) {
            when (result.currentState) {
                VpnState.Running -> mutableSessionStartedAtMillis.value = System.currentTimeMillis()
                VpnState.Idle,
                VpnState.Revoked,
                is VpnState.Error,
                -> {
                    mutableSessionStartedAtMillis.value = null
                    mutableActiveBlockedAppCount.value = 0
                }
                else -> Unit
            }
        }
        addEvent(result.diagnostic)
        return result
    }

    private fun addEvent(event: DiagnosticEvent) {
        diagnosticsStore.add(event)
        mutableDiagnostics.value = diagnosticsStore.snapshot()
    }
}
