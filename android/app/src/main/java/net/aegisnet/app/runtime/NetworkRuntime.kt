package net.aegisnet.app.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.aegisnet.app.diagnostics.DiagnosticEvent

interface NetworkRuntime {
    val state: StateFlow<RuntimeState>
    val diagnostics: Flow<DiagnosticEvent>

    suspend fun start(config: RuntimeConfig)

    suspend fun stop()
}
