package net.aegisnet.app.runtime

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource

class DummyRuntime(
    private val startupDelayMillis: Long = 50L,
) : NetworkRuntime {
    private val mutableState = MutableStateFlow<RuntimeState>(RuntimeState.Stopped)
    private val mutableDiagnostics = MutableSharedFlow<DiagnosticEvent>(
        replay = 16,
        extraBufferCapacity = 16,
    )

    override val state = mutableState.asStateFlow()
    override val diagnostics = mutableDiagnostics.asSharedFlow()

    override suspend fun start(config: RuntimeConfig) {
        if (mutableState.value == RuntimeState.Running || mutableState.value == RuntimeState.Starting) {
            emitDiagnostic(DiagnosticLevel.Debug, "Dummy runtime start ignored from ${mutableState.value.name}")
            return
        }

        transitionTo(RuntimeState.Starting, "Dummy runtime starting session ${config.sessionId}")
        delay(startupDelayMillis)
        transitionTo(RuntimeState.Running, "Dummy runtime running session ${config.sessionId}")
    }

    override suspend fun stop() {
        if (mutableState.value == RuntimeState.Stopped) {
            emitDiagnostic(DiagnosticLevel.Debug, "Dummy runtime stop ignored from Stopped")
            return
        }

        transitionTo(RuntimeState.Stopping, "Dummy runtime stopping")
        transitionTo(RuntimeState.Stopped, "Dummy runtime stopped")
    }

    private suspend fun transitionTo(
        nextState: RuntimeState,
        message: String,
    ) {
        val previousState = mutableState.value
        mutableState.value = nextState
        emitDiagnostic(DiagnosticLevel.Info, "$message (${previousState.name} -> ${nextState.name})")
    }

    private suspend fun emitDiagnostic(
        level: DiagnosticLevel,
        message: String,
    ) {
        mutableDiagnostics.emit(
            DiagnosticEvent(
                level = level,
                source = DiagnosticSource.Runtime,
                message = message,
            ),
        )
    }
}
