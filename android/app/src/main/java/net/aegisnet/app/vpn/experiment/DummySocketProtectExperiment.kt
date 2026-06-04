package net.aegisnet.app.vpn.experiment

import android.net.VpnService
import java.net.Socket
import java.util.UUID
import net.aegisnet.app.diagnostics.DiagnosticsStore
import net.aegisnet.app.diagnostics.ProtectExperimentDiagnosticContext
import net.aegisnet.app.diagnostics.ProtectExperimentDiagnosticName
import net.aegisnet.app.diagnostics.toDiagnosticEvent

class DummySocketProtectExperiment(
    private val diagnosticsStore: DiagnosticsStore,
    private val socketProtector: DummySocketProtector,
    private val runId: String = UUID.randomUUID().toString(),
    private val vpnLifecycleStateProvider: () -> String = { "unknown" },
    private val timestampProvider: () -> Long = { System.currentTimeMillis() },
    private val socketFactory: DummySocketFactory = DummySocketFactory { Socket() },
) {
    private var cleanupCompleted = false
    private var dummySocket: Socket? = null

    fun run(): DummySocketProtectExperimentRunResult {
        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentStarted,
            reason = "dummy socket protect experiment started",
        )

        emit(
            name = ProtectExperimentDiagnosticName.DummySocketCreateStarted,
            reason = "creating unconnected TCP socket",
        )
        val socket = try {
            socketFactory.createSocket()
        } catch (error: Exception) {
            val reason = "socket create failed: ${error.message ?: error.javaClass.simpleName}"
            emit(
                name = ProtectExperimentDiagnosticName.DummySocketCreateFailed,
                reason = reason,
            )
            emit(
                name = ProtectExperimentDiagnosticName.ProtectExperimentFailed,
                reason = reason,
            )
            cleanup()
            return DummySocketProtectExperimentRunResult.Failed
        }
        dummySocket = socket
        emit(
            name = ProtectExperimentDiagnosticName.DummySocketCreateSucceeded,
            reason = "unconnected TCP socket created",
        )

        emit(
            name = ProtectExperimentDiagnosticName.SocketProtectStarted,
            reason = "protecting socket before connect",
        )
        val protected = try {
            socketProtector.protect(socket)
        } catch (error: Exception) {
            val reason = "protect failed: ${error.message ?: error.javaClass.simpleName}"
            emit(
                name = ProtectExperimentDiagnosticName.SocketProtectFailed,
                reason = reason,
            )
            emit(
                name = ProtectExperimentDiagnosticName.ProtectExperimentFailed,
                reason = reason,
            )
            cleanup()
            return DummySocketProtectExperimentRunResult.Failed
        }

        if (!protected) {
            val reason = "protect returned false"
            emit(
                name = ProtectExperimentDiagnosticName.SocketProtectFailed,
                reason = reason,
            )
            emit(
                name = ProtectExperimentDiagnosticName.ProtectExperimentFailed,
                reason = reason,
            )
            cleanup()
            return DummySocketProtectExperimentRunResult.Failed
        }

        emit(
            name = ProtectExperimentDiagnosticName.SocketProtectSucceeded,
            reason = "socket protected before connect",
        )
        emit(
            name = ProtectExperimentDiagnosticName.SocketConnectSkipped,
            reason = "stage 2.1 does not connect dummy socket",
        )
        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentCompleted,
            reason = "dummy socket protect experiment completed",
        )
        cleanup()
        return DummySocketProtectExperimentRunResult.Completed
    }

    fun cleanup(): DummySocketProtectExperimentCleanupResult {
        if (cleanupCompleted) {
            return DummySocketProtectExperimentCleanupResult.AlreadyCompleted
        }

        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentCleanupStarted,
            reason = "cleanup started",
        )
        val socket = dummySocket
        dummySocket = null
        try {
            socket?.close()
            if (socket != null) {
                emit(
                    name = ProtectExperimentDiagnosticName.DummySocketClosed,
                    reason = "dummy socket closed",
                )
            }
        } catch (error: Exception) {
            cleanupCompleted = true
            emit(
                name = ProtectExperimentDiagnosticName.ProtectExperimentCleanupFailed,
                reason = "cleanup failed: ${error.message ?: error.javaClass.simpleName}",
            )
            return DummySocketProtectExperimentCleanupResult.Failed
        }
        cleanupCompleted = true
        emit(
            name = ProtectExperimentDiagnosticName.ProtectExperimentCleanupCompleted,
            reason = "cleanup completed",
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

}

fun interface DummySocketFactory {
    fun createSocket(): Socket
}

fun interface DummySocketProtector {
    fun protect(socket: Socket): Boolean
}

class VpnServiceSocketProtector(
    private val vpnService: VpnService,
) : DummySocketProtector {
    override fun protect(socket: Socket): Boolean = vpnService.protect(socket)
}

enum class DummySocketProtectExperimentRunResult {
    Completed,
    Failed,
}

enum class DummySocketProtectExperimentCleanupResult {
    Completed,
    AlreadyCompleted,
    Failed,
}
