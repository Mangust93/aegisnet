package net.aegisnet.app.runtime

data class RuntimeConfig(
    val sessionId: String,
    val tunFd: Int?,
    val proxyConfig: ImportedProxyConfig? = null,
)
