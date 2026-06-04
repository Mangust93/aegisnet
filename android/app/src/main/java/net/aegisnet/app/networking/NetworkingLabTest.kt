package net.aegisnet.app.networking

enum class NetworkingLabTest(
    val title: String,
) {
    InternetReachability("Internet Reachability Test"),
    DnsResolve("DNS Resolve Test"),
    TcpConnect("TCP Connect Test"),
}

enum class NetworkingLabStatus(
    val label: String,
) {
    NotRun("Not run"),
    Running("Running"),
    Passed("Passed"),
    Failed("Failed"),
}

data class NetworkingLabTestResult(
    val status: NetworkingLabStatus,
    val reason: String,
    val durationMillis: Long? = null,
) {
    val detailLabel: String
        get() = buildString {
            append("Last reason: ")
            append(reason)
            if (durationMillis != null) {
                append(" (")
                append(durationMillis)
                append(" ms)")
            }
        }

    companion object {
        val NotRun = NetworkingLabTestResult(
            status = NetworkingLabStatus.NotRun,
            reason = "Test has not run in this app session.",
        )

        fun running(reason: String) = NetworkingLabTestResult(
            status = NetworkingLabStatus.Running,
            reason = reason,
        )
    }
}

val initialNetworkingLabResults: Map<NetworkingLabTest, NetworkingLabTestResult> =
    NetworkingLabTest.entries.associateWith { NetworkingLabTestResult.NotRun }
