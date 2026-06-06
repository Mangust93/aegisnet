package net.aegisnet.app.firewall

enum class AppVpnMode(val label: String) {
    Diagnostics("Diagnostics"),
    AppFirewall("Local App Firewall"),
    NetworkMonitor("Network Monitor"),
    RealProxyRuntime("Real Proxy Runtime Experimental"),
}
