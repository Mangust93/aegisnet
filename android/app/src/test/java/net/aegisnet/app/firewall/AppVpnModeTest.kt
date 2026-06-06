package net.aegisnet.app.firewall

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVpnModeTest {
    @Test
    fun defaultModeIsDiagnostics() {
        assertEquals(AppVpnMode.Diagnostics, AppVpnMode.entries.first())
        assertEquals("Diagnostics", AppVpnMode.Diagnostics.label)
    }

    @Test
    fun firewallModeHasUserFacingLabel() {
        assertEquals("Local App Firewall", AppVpnMode.AppFirewall.label)
    }

    @Test
    fun experimentalModesHaveUserFacingLabels() {
        assertEquals("Network Monitor", AppVpnMode.NetworkMonitor.label)
        assertEquals("Real Proxy Runtime Experimental", AppVpnMode.RealProxyRuntime.label)
    }
}
