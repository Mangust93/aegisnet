package net.aegisnet.app.runtime

data class RuntimeConfig(
    val sessionId: String,
    val tunFd: Int?,
    val tunFdCloser: TunFdCloser? = null,
    val proxyConfig: ImportedProxyConfig? = null,
    val runtimeBasePath: String? = null,
    val runtimeWorkingPath: String? = null,
    val runtimeTempPath: String? = null,
)

fun interface TunFdCloser {
    fun close(fd: Int)
}
