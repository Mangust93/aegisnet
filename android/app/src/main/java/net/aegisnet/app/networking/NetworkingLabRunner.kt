package net.aegisnet.app.networking

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource

class NetworkingLabRunner(
    private val dnsResolver: DnsResolver = JvmDnsResolver,
    private val socketConnector: SocketConnector = JvmSocketConnector,
    private val nanoTimeProvider: () -> Long = System::nanoTime,
) {
    fun run(
        test: NetworkingLabTest,
        target: NetworkingLabTarget = NetworkingLabTarget(),
        emitDiagnostic: (DiagnosticEvent) -> Unit,
    ): NetworkingLabTestResult {
        val startedAtNanos = nanoTimeProvider()
        emitDiagnostic(test.event("started", DiagnosticLevel.Info, "test started"))

        val result = try {
            when (test) {
                NetworkingLabTest.InternetReachability -> runInternetReachability(target)
                NetworkingLabTest.DnsResolve -> runDnsResolve(target)
                NetworkingLabTest.TcpConnect -> runTcpConnect(target)
            }
            val durationMillis = elapsedMillis(startedAtNanos)
            val reason = test.successReason(target)
            emitDiagnostic(test.event("succeeded", DiagnosticLevel.Info, "$reason durationMillis=$durationMillis"))
            NetworkingLabTestResult(
                status = NetworkingLabStatus.Passed,
                reason = reason,
                durationMillis = durationMillis,
            )
        } catch (error: Exception) {
            val durationMillis = elapsedMillis(startedAtNanos)
            val reason = "${error.message ?: error.javaClass.simpleName}"
            emitDiagnostic(test.event("failed", DiagnosticLevel.Error, "$reason durationMillis=$durationMillis"))
            NetworkingLabTestResult(
                status = NetworkingLabStatus.Failed,
                reason = reason,
                durationMillis = durationMillis,
            )
        } finally {
            emitDiagnostic(test.event("cleanup", DiagnosticLevel.Info, "test cleanup completed"))
        }

        return result
    }

    private fun runInternetReachability(target: NetworkingLabTarget) {
        socketConnector.connect(
            host = target.internetReachabilityHost,
            port = target.internetReachabilityPort,
            timeoutMillis = target.timeoutMillis,
        )
    }

    private fun runDnsResolve(target: NetworkingLabTarget) {
        val addresses = dnsResolver.resolve(target.dnsHost)
        require(addresses.isNotEmpty()) { "no addresses returned for ${target.dnsHost}" }
    }

    private fun runTcpConnect(target: NetworkingLabTarget) {
        socketConnector.connect(
            host = target.tcpHost,
            port = target.tcpPort,
            timeoutMillis = target.timeoutMillis,
        )
    }

    private fun elapsedMillis(startedAtNanos: Long): Long {
        return ((nanoTimeProvider() - startedAtNanos).coerceAtLeast(0L) / NANOS_PER_MILLIS)
            .coerceAtLeast(0L)
    }

    private fun NetworkingLabTest.successReason(target: NetworkingLabTarget): String {
        return when (this) {
            NetworkingLabTest.InternetReachability ->
                "connected to ${target.internetReachabilityHost}:${target.internetReachabilityPort}"
            NetworkingLabTest.DnsResolve ->
                "resolved ${target.dnsHost} with Android/JVM resolver"
            NetworkingLabTest.TcpConnect ->
                "connected to ${target.tcpHost}:${target.tcpPort}"
        }
    }

    private fun NetworkingLabTest.event(
        suffix: String,
        level: DiagnosticLevel,
        reason: String,
    ): DiagnosticEvent {
        return DiagnosticEvent(
            level = level,
            source = DiagnosticSource.Networking,
            message = "${eventPrefix()}_$suffix reason=$reason",
        )
    }

    private fun NetworkingLabTest.eventPrefix(): String {
        return when (this) {
            NetworkingLabTest.InternetReachability -> "internet_reachability_test"
            NetworkingLabTest.DnsResolve -> "dns_resolve_test"
            NetworkingLabTest.TcpConnect -> "tcp_connect_test"
        }
    }

    private companion object {
        const val NANOS_PER_MILLIS = 1_000_000L
    }
}

data class NetworkingLabTarget(
    val internetReachabilityHost: String = "1.1.1.1",
    val internetReachabilityPort: Int = 443,
    val dnsHost: String = "example.com",
    val tcpHost: String = "example.com",
    val tcpPort: Int = 443,
    val timeoutMillis: Int = 1_500,
)

fun interface DnsResolver {
    fun resolve(host: String): List<InetAddress>
}

fun interface SocketConnector {
    fun connect(host: String, port: Int, timeoutMillis: Int)
}

private object JvmDnsResolver : DnsResolver {
    override fun resolve(host: String): List<InetAddress> = InetAddress.getAllByName(host).toList()
}

private object JvmSocketConnector : SocketConnector {
    override fun connect(host: String, port: Int, timeoutMillis: Int) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMillis)
        }
    }
}
