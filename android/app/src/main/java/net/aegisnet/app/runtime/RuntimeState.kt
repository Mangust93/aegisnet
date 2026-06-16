package net.aegisnet.app.runtime

sealed interface RuntimeState {
    val name: String
        get() = when (this) {
            Stopped -> "Stopped"
            Starting -> "Starting"
            Running -> "Running"
            Stopping -> "Stopping"
            is Failed -> "Failed"
        }

    data object Stopped : RuntimeState
    data object Starting : RuntimeState
    data object Running : RuntimeState
    data object Stopping : RuntimeState
    data class Failed(val message: String) : RuntimeState
}

val RuntimeState.label: String
    get() = when (this) {
        RuntimeState.Stopped -> "Not started"
        RuntimeState.Starting -> "Starting"
        RuntimeState.Running -> "Running"
        RuntimeState.Stopping -> "Stopping"
        is RuntimeState.Failed -> "Failed"
    }
