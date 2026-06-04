package net.aegisnet.app.diagnostics

data class DiagnosticEvent(
    val level: DiagnosticLevel,
    val source: DiagnosticSource,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
)

enum class DiagnosticLevel {
    Debug,
    Info,
    Warning,
    Error,
}

enum class DiagnosticSource {
    Ui,
    Vpn,
    Runtime,
    Networking,
    System,
}
