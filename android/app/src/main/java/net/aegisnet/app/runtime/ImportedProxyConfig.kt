package net.aegisnet.app.runtime

data class ImportedProxyConfig(
    val id: String,
    val name: String,
    val type: ProxyConfigType,
    val content: String,
    val importedAtMillis: Long,
)

enum class ProxyConfigType(val label: String) {
    VlessUri("VLESS URI"),
    Hysteria2Uri("Hysteria2 URI"),
    SingBoxJson("sing-box JSON"),
}

data class ProxyConfigValidationResult(
    val config: ImportedProxyConfig?,
    val error: String?,
) {
    val isValid: Boolean
        get() = config != null && error == null
}
