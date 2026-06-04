package net.aegisnet.app.diagnostics

enum class ProtectExperimentDiagnosticName(
    val eventName: String,
) {
    ProtectExperimentStarted("protect_experiment_started"),
    DummySocketCreateStarted("dummy_socket_create_started"),
    DummySocketCreateSucceeded("dummy_socket_create_succeeded"),
    DummySocketCreateFailed("dummy_socket_create_failed"),
    SocketProtectStarted("socket_protect_started"),
    SocketProtectSucceeded("socket_protect_succeeded"),
    SocketProtectFailed("socket_protect_failed"),
    SocketConnectSkipped("socket_connect_skipped"),
    DummySocketClosed("dummy_socket_closed"),
    ProtectExperimentFailed("protect_experiment_failed"),
    ProtectExperimentCompleted("protect_experiment_completed"),
    ProtectExperimentCleanupStarted("protect_experiment_cleanup_started"),
    ProtectExperimentCleanupCompleted("protect_experiment_cleanup_completed"),
    ProtectExperimentCleanupFailed("protect_experiment_cleanup_failed"),
    ProtectExperimentRevoked("protect_experiment_revoked"),
    ProtectExperimentDisconnected("protect_experiment_disconnected"),
}

data class ProtectExperimentDiagnosticContext(
    val runId: String,
    val vpnLifecycleState: String,
    val reason: String? = null,
)

fun ProtectExperimentDiagnosticName.toDiagnosticEvent(
    context: ProtectExperimentDiagnosticContext,
    level: DiagnosticLevel = defaultLevel,
    timestampMillis: Long = System.currentTimeMillis(),
): DiagnosticEvent {
    return DiagnosticEvent(
        level = level,
        source = DiagnosticSource.Vpn,
        message = buildMessage(context),
        timestampMillis = timestampMillis,
    )
}

val ProtectExperimentDiagnosticName.defaultLevel: DiagnosticLevel
    get() = when (this) {
        ProtectExperimentDiagnosticName.DummySocketCreateFailed,
        ProtectExperimentDiagnosticName.SocketProtectFailed,
        ProtectExperimentDiagnosticName.ProtectExperimentFailed,
        ProtectExperimentDiagnosticName.ProtectExperimentCleanupFailed -> DiagnosticLevel.Error
        ProtectExperimentDiagnosticName.ProtectExperimentRevoked,
        ProtectExperimentDiagnosticName.ProtectExperimentDisconnected -> DiagnosticLevel.Warning
        else -> DiagnosticLevel.Info
    }

private fun ProtectExperimentDiagnosticName.buildMessage(
    context: ProtectExperimentDiagnosticContext,
): String {
    val reason = context.reason?.takeIf(String::isNotBlank)
    return buildString {
        append(eventName)
        append(" runId=")
        append(context.runId)
        append(" vpnLifecycleState=")
        append(context.vpnLifecycleState)
        if (reason != null) {
            append(" reason=")
            append(reason)
        }
    }
}
