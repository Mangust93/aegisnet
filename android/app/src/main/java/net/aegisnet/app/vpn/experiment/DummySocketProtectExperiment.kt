package net.aegisnet.app.vpn.experiment

import java.util.UUID
import net.aegisnet.app.diagnostics.DiagnosticsStore
import net.aegisnet.app.diagnostics.ProtectExperimentDiagnosticContext
import net.aegisnet.app.diagnostics.ProtectExperimentDiagnosticName
import net.aegisnet.app.diagnostics.toDiagnosticEvent

class DummySocketProtectExperiment(
    private val diagnosticsStore: DiagnosticsStore,
    private val runId: String = UUID.randomUUID().toString(),
    private val vpnLifecycleStateProvider: () -> String = { "unknown" },
    private val timestampProvider: () -> Long = { System.currentTimeMillis() },
) {
    private var cleanupCompleted = false

    fun run(): DummySocketProtectExperimentRunResult {
        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentStarted,
            reason = SKELETON_REASON,
        )
        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentCompleted,
            reason = SKELETON_REASON,
        )
        return DummySocketProtectExperimentRunResult.Completed
    }

    fun cleanup(): DummySocketProtectExperimentCleanupResult {
        if (cleanupCompleted) {
            return DummySocketProtectExperimentCleanupResult.AlreadyCompleted
        }

        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentCleanupStarted,
            reason = SKELETON_REASON,
        )
        cleanupCompleted = true
        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentCleanupCompleted,
            reason = SKELETON_REASON,
        )
        return DummySocketProtectExperimentCleanupResult.Completed
    }

    private fun emit(
        name: ProtectExperimentDiagnosticName,
        reason: String,
    ) {
        diagnosticsStore.add(
            name.toDiagnosticEvent(
                context = ProtectExperimentDiagnosticContext(
                    runId = runId,
                    vpnLifecycleState = vpnLifecycleStateProvider(),
                    reason = reason,
                ),
                timestampMillis = timestampProvider(),
            ),
        )
    }

    private companion object {
        const val SKELETON_REASON = "skeleton no socket created"
    }
}

enum class DummySocketProtectExperimentRunResult {
    Completed,
}

enum class DummySocketProtectExperimentCleanupResult {
    Completed,
    AlreadyCompleted,
}
