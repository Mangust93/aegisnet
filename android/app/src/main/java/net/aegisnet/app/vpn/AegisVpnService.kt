package net.aegisnet.app.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource

class AegisVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            else -> AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Debug,
                source = DiagnosticSource.Vpn,
                message = "VPN service ignored empty action",
            )
        }

        return START_STICKY
    }

    override fun onRevoke() {
        closeVpnInterface()
        AegisVpnController.revoked()
        AegisVpnController.revokeHandled()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        if (vpnInterface != null) {
            closeVpnInterface()
            AegisVpnController.disconnect()
            AegisVpnController.stopped()
        }
        super.onDestroy()
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Debug,
                source = DiagnosticSource.Vpn,
                message = "VPN service start ignored because interface is already open",
            )
            return
        }

        AegisVpnController.serviceStarted()

        try {
            vpnInterface = Builder()
                .setSession("AegisNet")
                .setMtu(DUMMY_MTU)
                .addAddress(DUMMY_IPV4_ADDRESS, DUMMY_IPV4_PREFIX_LENGTH)
                .establish()

            if (vpnInterface == null) {
                failAndStop("VPN interface was not established")
                return
            }

            AegisVpnController.tunnelEstablished()
        } catch (error: RuntimeException) {
            failAndStop("VPN interface error: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun stopVpn() {
        AegisVpnController.disconnect()
        closeVpnInterface()
        AegisVpnController.stopped()
        stopSelf()
    }

    private fun failAndStop(message: String) {
        closeVpnInterface()
        AegisVpnController.fail(message)
        AegisVpnController.cleanupError()
        stopSelf()
    }

    private fun closeVpnInterface() {
        val currentInterface = vpnInterface ?: return
        vpnInterface = null
        try {
            currentInterface.close()
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Info,
                source = DiagnosticSource.Vpn,
                message = "VPN interface closed",
            )
        } catch (error: Exception) {
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Error,
                source = DiagnosticSource.Vpn,
                message = "VPN interface close failed: ${error.message ?: error.javaClass.simpleName}",
            )
        }
    }

    companion object {
        const val ACTION_START = "net.aegisnet.app.vpn.START"
        const val ACTION_STOP = "net.aegisnet.app.vpn.STOP"

        private const val DUMMY_MTU = 1280
        private const val DUMMY_IPV4_ADDRESS = "10.255.0.2"
        private const val DUMMY_IPV4_PREFIX_LENGTH = 32
    }
}
