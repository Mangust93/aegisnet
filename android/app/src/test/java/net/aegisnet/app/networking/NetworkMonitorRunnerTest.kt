package net.aegisnet.app.networking

import java.io.IOException
import java.net.InetAddress
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkMonitorRunnerTest {
    @Test
    fun fullCheckReportsPublicIpAndPassedStatuses() {
        val events = mutableListOf<String>()
        val runner = NetworkMonitorRunner(
            publicIpClient = PublicIpClient { url, timeoutMillis ->
                assertEquals("https://ip.example.test", url)
                assertEquals(250, timeoutMillis)
                "203.0.113.20"
            },
            dnsResolver = DnsResolver { host ->
                assertEquals("dns.example.test", host)
                listOf(InetAddress.getLoopbackAddress())
            },
            httpsReachabilityClient = HttpsReachabilityClient { url, timeoutMillis ->
                assertEquals("https://www.example.test", url)
                assertEquals(250, timeoutMillis)
            },
            nanoTimeProvider = sequenceOf(
                1_000_000L,
                2_000_000L,
                3_000_000L,
                4_000_000L,
                5_000_000L,
                6_000_000L,
                7_000_000L,
                8_000_000L,
            ).iterator()::next,
        )

        val result = runner.runFullCheck(
            networkType = "Wi-Fi",
            vpnActive = true,
            target = NetworkMonitorTarget(
                publicIpUrl = "https://ip.example.test",
                dnsHost = "dns.example.test",
                httpsUrl = "https://www.example.test",
                timeoutMillis = 250,
            ),
        ) { event ->
            assertEquals(DiagnosticSource.Networking, event.source)
            events += event.message.substringBefore(" reason=")
        }

        assertEquals("203.0.113.20", result.publicIp)
        assertEquals("Wi-Fi", result.networkType)
        assertEquals(true, result.vpnActive)
        assertEquals(CheckStatus.Passed, result.status)
        assertEquals(CheckStatus.Passed, result.dnsStatus)
        assertEquals(CheckStatus.Passed, result.httpsStatus)
        assertNull(result.lastError)
        assertEquals(
            listOf(
                "network_monitor_full_check_started",
                "network_monitor_public_ip_check_started",
                "network_monitor_public_ip_check_succeeded",
                "network_monitor_dns_resolve_check_started",
                "network_monitor_dns_resolve_check_succeeded",
                "network_monitor_https_reachability_check_started",
                "network_monitor_https_reachability_check_succeeded",
                "network_monitor_full_check_completed",
            ),
            events,
        )
    }

    @Test
    fun failedStepKeepsLastErrorAndFailsOverallCheck() {
        val levels = mutableListOf<DiagnosticLevel>()
        val runner = NetworkMonitorRunner(
            publicIpClient = PublicIpClient { _, _ -> throw IOException("public IP refused") },
            dnsResolver = DnsResolver { listOf(InetAddress.getLoopbackAddress()) },
            httpsReachabilityClient = HttpsReachabilityClient { _, _ -> },
            nanoTimeProvider = generateSequence(1_000_000L) { it + 1_000_000L }.iterator()::next,
        )

        val result = runner.runFullCheck(
            networkType = "Cellular",
            vpnActive = false,
        ) { event ->
            levels += event.level
        }

        assertNull(result.publicIp)
        assertEquals(CheckStatus.Failed, result.status)
        assertEquals(CheckStatus.Passed, result.dnsStatus)
        assertEquals(CheckStatus.Passed, result.httpsStatus)
        assertEquals("public IP refused", result.lastError)
        assertEquals(DiagnosticLevel.Error, levels.last())
    }
}
