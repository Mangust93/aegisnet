# Dummy Runtime

## Purpose

Dummy runtime validates the runtime abstraction without integrating real proxy cores.

## Stage 1 Rule

No real network runtime is allowed in Stage 1.

## Runtime Interface

interface NetworkRuntime {
    val state: StateFlow<RuntimeState>
    val diagnostics: Flow<DiagnosticEvent>

    suspend fun start(config: RuntimeConfig)
    suspend fun stop()
}

## Runtime Config

data class RuntimeConfig(
    val sessionId: String,
    val tunFd: Int?
)

## Runtime State

sealed interface RuntimeState {
    data object Stopped : RuntimeState
    data object Starting : RuntimeState
    data object Running : RuntimeState
    data object Stopping : RuntimeState
    data class Failed(val message: String) : RuntimeState
}

## Dummy Behavior

DummyRuntime should:

- emit Starting
- emit diagnostics
- delay shortly
- emit Running
- stop cleanly
- emit Stopping and Stopped
- never open sockets
- never generate protocol configs
- never depend on sing-box/libbox