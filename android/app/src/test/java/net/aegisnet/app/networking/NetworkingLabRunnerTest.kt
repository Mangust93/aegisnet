package net.aegisnet.app.networking

import java.io.IOException
import java.net.InetAddress
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkingLabRunnerTest {
    @Test
    fun dnsResolveUsesJvmResolverAndReportsSuccessCleanup() {
        var dnsHost: String? = null
        val events = mutableListOf<String>()
        val runner = NetworkingLabRunner(
            dnsResolver = DnsResolver {
                dnsHost = it
                listOf(InetAddress.getLoopbackAddress())
            },
            socketConnector = SocketConnector { _, _, _ -> error("socket should not be used") },
            nanoTimeProvider = sequenceOf(1_000_000L, 6_000_000L).iterator()::next,
        )

        val result = runner.run(
            test = NetworkingLabTest.DnsResolve,
            target = NetworkingLabTarget(dnsHost = "example.test"),
        ) { event ->
            assertEquals(DiagnosticSource.Networking, event.source)
            events += event.message.substringBefore(" reason=")
        }

        assertEquals("example.test", dnsHost)
        assertEquals(NetworkingLabStatus.Passed, result.status)
        assertEquals(5L, result.durationMillis)
        assertEquals(
            listOf(
                "dns_resolve_test_started",
                "dns_resolve_test_succeeded",
                "dns_resolve_test_cleanup",
            ),
            events,
        )
    }

    @Test
    fun tcpConnectFailureReportsFailureAndCleanup() {
        val levels = mutableListOf<DiagnosticLevel>()
        val runner = NetworkingLabRunner(
            dnsResolver = DnsResolver { emptyList() },
            socketConnector = SocketConnector { host, port, timeoutMillis ->
                assertEquals("example.test", host)
                assertEquals(9443, port)
                assertEquals(100, timeoutMillis)
                throw IOException("connection refused")
            },
            nanoTimeProvider = sequenceOf(10_000_000L, 13_000_000L).iterator()::next,
        )

        val result = runner.run(
            test = NetworkingLabTest.TcpConnect,
            target = NetworkingLabTarget(
                tcpHost = "example.test",
                tcpPort = 9443,
                timeoutMillis = 100,
            ),
        ) { event ->
            levels += event.level
        }

        assertEquals(NetworkingLabStatus.Failed, result.status)
        assertEquals("connection refused", result.reason)
        assertTrue(result.detailLabel.contains("3 ms"))
        assertEquals(
            listOf(DiagnosticLevel.Info, DiagnosticLevel.Error, DiagnosticLevel.Info),
            levels,
        )
    }

    @Test
    fun internetReachabilityUsesConfiguredIpEndpoint() {
        var endpoint: String? = null
        val runner = NetworkingLabRunner(
            dnsResolver = DnsResolver { emptyList() },
            socketConnector = SocketConnector { host, port, timeoutMillis ->
                endpoint = "$host:$port/$timeoutMillis"
            },
            nanoTimeProvider = sequenceOf(100_000_000L, 101_000_000L).iterator()::next,
        )

        val result = runner.run(
            test = NetworkingLabTest.InternetReachability,
            target = NetworkingLabTarget(
                internetReachabilityHost = "203.0.113.10",
                internetReachabilityPort = 443,
                timeoutMillis = 250,
            ),
        ) {}

        assertEquals("203.0.113.10:443/250", endpoint)
        assertEquals(NetworkingLabStatus.Passed, result.status)
    }
}
