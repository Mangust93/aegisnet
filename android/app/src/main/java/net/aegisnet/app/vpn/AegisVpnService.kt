package net.aegisnet.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import net.aegisnet.app.MainActivity
import net.aegisnet.app.R
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource

class AegisVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var foregroundActive = false

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
        stopForegroundNotification()
        AegisVpnController.revoked()
        AegisVpnController.revokeHandled()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        if (vpnInterface != null) {
            closeVpnInterface()
            stopForegroundNotification()
            AegisVpnController.disconnect()
            AegisVpnController.stopped()
        }
        stopForegroundNotification()
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
        startForegroundNotification(VpnState.EstablishingTunnel)

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
            updateForegroundNotification(VpnState.Running)
        } catch (error: RuntimeException) {
            failAndStop("VPN interface error: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun stopVpn() {
        AegisVpnController.disconnect()
        updateForegroundNotification(VpnState.Stopping)
        closeVpnInterface()
        stopForegroundNotification()
        AegisVpnController.stopped()
        stopSelf()
    }

    private fun failAndStop(message: String) {
        closeVpnInterface()
        stopForegroundNotification()
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

    private fun startForegroundNotification(status: VpnState) {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(status))
        foregroundActive = true
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Vpn,
            message = "VPN foreground notification started",
        )
    }

    private fun updateForegroundNotification(status: VpnState) {
        if (!foregroundActive) return
        startForeground(NOTIFICATION_ID, buildNotification(status))
    }

    private fun stopForegroundNotification() {
        if (!foregroundActive) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundActive = false
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Vpn,
            message = "VPN foreground notification stopped",
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "AegisNet VPN service status"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(status: VpnState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AegisNet")
            .setContentText("VPN status: ${status.label}")
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)

    companion object {
        const val ACTION_START = "net.aegisnet.app.vpn.START"
        const val ACTION_STOP = "net.aegisnet.app.vpn.STOP"

        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "aegis_vpn_service"
        private const val NOTIFICATION_CHANNEL_NAME = "VPN service"
        private const val DUMMY_MTU = 1280
        private const val DUMMY_IPV4_ADDRESS = "10.255.0.2"
        private const val DUMMY_IPV4_PREFIX_LENGTH = 32
    }
}
