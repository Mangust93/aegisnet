package net.aegisnet.app.diagnostics

class DiagnosticsStore {
    private val events = mutableListOf<DiagnosticEvent>()

    fun add(event: DiagnosticEvent) {
        events += event
    }

    fun snapshot(): List<DiagnosticEvent> = events.toList()

    fun clear() {
        events.clear()
    }
}
