package net.aegisnet.app.networking

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource

class NetworkMonitorRunner(
    private val publicIpClient: PublicIpClient = HttpsPublicIpClient(),
    private val dnsResolver: DnsResolver = DnsResolver { host -> InetAddress.getAllByName(host).toList() },
    private val httpsReachabilityClient: HttpsReachabilityClient = HttpsUrlReachabilityClient(),
    private val nanoTimeProvider: () -> Long = System::nanoTime,
) {
    fun runFullCheck(
        networkType: String,
        vpnActive: Boolean,
        target: NetworkMonitorTarget = NetworkMonitorTarget(),
        emitDiagnostic: (DiagnosticEvent) -> Unit,
    ): NetworkMonitorSnapshot {
        val startedAtNanos = nanoTimeProvider()
        var publicIp: String? = null
        var dnsStatus = CheckStatus.NotRun
        var httpsStatus = CheckStatus.NotRun
        var lastError: String? = null

        emitDiagnostic(networkMonitorEvent("network_monitor_full_check_started", DiagnosticLevel.Info, "networkType=$networkType vpnActive=$vpnActive"))

        publicIp = runStep(
            eventName = "network_monitor_public_ip_check",
            emitDiagnostic = emitDiagnostic,
            onError = { lastError = it },
        ) {
            publicIpClient.fetchPublicIp(target.publicIpUrl, target.timeoutMillis)
        }

        dnsStatus = if (runStep(
                eventName = "network_monitor_dns_resolve_check",
                emitDiagnostic = emitDiagnostic,
                onError = { lastError = it },
            ) {
                dnsResolver.resolve(target.dnsHost).also { addresses ->
                    require(addresses.isNotEmpty()) { "no addresses returned for ${target.dnsHost}" }
                }
            } != null
        ) {
            CheckStatus.Passed
        } else {
            CheckStatus.Failed
        }

        httpsStatus = if (runStep(
                eventName = "network_monitor_https_reachability_check",
                emitDiagnostic = emitDiagnostic,
                onError = { lastError = it },
            ) {
                httpsReachabilityClient.check(target.httpsUrl, target.timeoutMillis)
            } != null
        ) {
            CheckStatus.Passed
        } else {
            CheckStatus.Failed
        }

        val durationMillis = elapsedMillis(startedAtNanos)
        val status = if (publicIp != null && dnsStatus == CheckStatus.Passed && httpsStatus == CheckStatus.Passed) {
            CheckStatus.Passed
        } else {
            CheckStatus.Failed
        }
        emitDiagnostic(
            networkMonitorEvent(
                name = "network_monitor_full_check_completed",
                level = if (status == CheckStatus.Passed) DiagnosticLevel.Info else DiagnosticLevel.Error,
                reason = "status=${status.label} durationMillis=$durationMillis",
            ),
        )

        return NetworkMonitorSnapshot(
            publicIp = publicIp,
            networkType = networkType,
            vpnActive = vpnActive,
            status = status,
            dnsStatus = dnsStatus,
            httpsStatus = httpsStatus,
            durationMillis = durationMillis,
            lastError = lastError,
            checkedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun <T> runStep(
        eventName: String,
        emitDiagnostic: (DiagnosticEvent) -> Unit,
        onError: (String) -> Unit,
        block: () -> T,
    ): T? {
        val startedAtNanos = nanoTimeProvider()
        emitDiagnostic(networkMonitorEvent("${eventName}_started", DiagnosticLevel.Info, "started"))
        return try {
            val result = block()
            val durationMillis = elapsedMillis(startedAtNanos)
            emitDiagnostic(networkMonitorEvent("${eventName}_succeeded", DiagnosticLevel.Info, "durationMillis=$durationMillis"))
            result
        } catch (error: Exception) {
            val durationMillis = elapsedMillis(startedAtNanos)
            val reason = error.message ?: error.javaClass.simpleName
            onError(reason)
            emitDiagnostic(networkMonitorEvent("${eventName}_failed", DiagnosticLevel.Error, "$reason durationMillis=$durationMillis"))
            null
        }
    }

    private fun elapsedMillis(startedAtNanos: Long): Long {
        return ((nanoTimeProvider() - startedAtNanos).coerceAtLeast(0L) / NANOS_PER_MILLIS)
            .coerceAtLeast(0L)
    }

    private fun networkMonitorEvent(
        name: String,
        level: DiagnosticLevel,
        reason: String,
    ): DiagnosticEvent {
        return DiagnosticEvent(
            level = level,
            source = DiagnosticSource.Networking,
            message = "$name reason=$reason",
        )
    }

    private companion object {
        const val NANOS_PER_MILLIS = 1_000_000L
    }
}

data class NetworkMonitorTarget(
    val publicIpUrl: String = "https://api.ipify.org",
    val dnsHost: String = "example.com",
    val httpsUrl: String = "https://example.com",
    val timeoutMillis: Int = 3_000,
)

data class NetworkMonitorSnapshot(
    val publicIp: String?,
    val networkType: String,
    val vpnActive: Boolean,
    val status: CheckStatus,
    val dnsStatus: CheckStatus,
    val httpsStatus: CheckStatus,
    val durationMillis: Long,
    val lastError: String?,
    val checkedAtMillis: Long,
) {
    val publicIpLabel: String
        get() = publicIp ?: "Unavailable"
}

enum class CheckStatus(val label: String) {
    NotRun("Not run"),
    Running("Running"),
    Passed("Passed"),
    Failed("Failed"),
}

fun interface PublicIpClient {
    fun fetchPublicIp(url: String, timeoutMillis: Int): String
}

fun interface HttpsReachabilityClient {
    fun check(url: String, timeoutMillis: Int)
}

private class HttpsPublicIpClient : PublicIpClient {
    override fun fetchPublicIp(url: String, timeoutMillis: Int): String {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout = timeoutMillis
        connection.readTimeout = timeoutMillis
        connection.requestMethod = "GET"
        connection.useCaches = false
        return try {
            val code = connection.responseCode
            require(code in 200..299) { "HTTP $code from public IP endpoint" }
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText().trim().also { ip ->
                    require(ip.isNotEmpty()) { "empty public IP response" }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

private class HttpsUrlReachabilityClient : HttpsReachabilityClient {
    override fun check(url: String, timeoutMillis: Int) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = timeoutMillis
        connection.readTimeout = timeoutMillis
        connection.requestMethod = "HEAD"
        connection.useCaches = false
        try {
            val code = connection.responseCode
            require(code in 200..399) { "HTTP $code from HTTPS target" }
        } finally {
            connection.disconnect()
        }
    }
}
