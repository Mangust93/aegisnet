package net.aegisnet.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.aegisnet.app.MainActivity
import net.aegisnet.app.R
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import net.aegisnet.app.diagnostics.DiagnosticsStore
import net.aegisnet.app.firewall.AppVpnMode
import net.aegisnet.app.runtime.DummyRuntime
import net.aegisnet.app.runtime.NetworkRuntime
import net.aegisnet.app.runtime.RuntimeConfig
import net.aegisnet.app.runtime.RuntimeState
import net.aegisnet.app.vpn.experiment.DummySocketProtectExperiment
import net.aegisnet.app.vpn.experiment.VpnServiceSocketProtector

class AegisVpnService : VpnService() {
    private val runtime: NetworkRuntime = DummyRuntime()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var foregroundActive = false
    private var runtimeStartJob: Job? = null
    private var activeMode = AppVpnMode.Diagnostics

    override fun onCreate() {
        super.onCreate()
        runtime.state
            .onEach(AegisVpnController::updateRuntimeState)
            .launchIn(serviceScope)
        runtime.diagnostics
            .onEach(AegisVpnController::addDiagnostic)
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn(intent)
            ACTION_STOP -> stopVpn()
            ACTION_RUN_PROTECT_EXPERIMENT -> runProtectExperiment()
            else -> AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Debug,
                source = DiagnosticSource.Vpn,
                message = "VPN service ignored empty action",
            )
        }

        return START_STICKY
    }

    override fun onRevoke() {
        stopRuntimeBlocking()
        closeVpnInterface()
        stopForegroundNotification()
        markFirewallStopped()
        AegisVpnController.revoked()
        AegisVpnController.revokeHandled()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        if (vpnInterface != null || runtime.state.value != RuntimeState.Stopped) {
            stopRuntimeBlocking()
            closeVpnInterface()
            stopForegroundNotification()
            markFirewallStopped()
            AegisVpnController.disconnect()
            AegisVpnController.stopped()
        }
        stopForegroundNotification()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startVpn(intent: Intent) {
        if (vpnInterface != null) {
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Debug,
                source = DiagnosticSource.Vpn,
                message = "VPN service start ignored because interface is already open",
            )
            return
        }

        activeMode = intentMode(intent)
        AegisVpnController.serviceStarted()
        startForegroundNotification(VpnState.EstablishingTunnel)

        try {
            when (activeMode) {
                AppVpnMode.Diagnostics -> startDiagnosticsVpn()
                AppVpnMode.AppFirewall -> startAppFirewallVpn(intentSelectedPackages(intent))
            }
        } catch (error: RuntimeException) {
            failAndStop("VPN interface error: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun startDiagnosticsVpn() {
        vpnInterface = Builder()
            .setSession("AegisNet Diagnostics")
            .setMtu(DUMMY_MTU)
            .addAddress(DUMMY_IPV4_ADDRESS, DUMMY_IPV4_PREFIX_LENGTH)
            .establish()

        if (vpnInterface == null) {
            failAndStop("VPN interface was not established")
            return
        }

        AegisVpnController.updateActiveFirewall(AppVpnMode.Diagnostics, 0)
        startDummyRuntime()
    }

    private fun startAppFirewallVpn(selectedPackages: List<String>) {
        if (selectedPackages.isEmpty()) {
            emitFirewallError("No apps selected for App Firewall mode")
            failAndStop("App Firewall requires at least one selected app")
            return
        }

        val builder = Builder()
            .setSession("AegisNet App Firewall")
            .setMtu(DUMMY_MTU)
            .addAddress(DUMMY_IPV4_ADDRESS, DUMMY_IPV4_PREFIX_LENGTH)

        var allowedCount = 0
        selectedPackages.sorted().forEach { packageName ->
            try {
                builder.addAllowedApplication(packageName)
                allowedCount += 1
                AegisVpnController.addDiagnostic(
                    level = DiagnosticLevel.Info,
                    source = DiagnosticSource.Vpn,
                    message = "firewall_allowed_application_added package=$packageName",
                )
            } catch (error: PackageManager.NameNotFoundException) {
                emitFirewallError("Package unavailable for firewall: $packageName")
            } catch (error: UnsupportedOperationException) {
                emitFirewallError("Allowed-application routing unsupported: ${error.message ?: error.javaClass.simpleName}")
            }
        }

        if (allowedCount == 0) {
            failAndStop("App Firewall could not add any selected app")
            return
        }

        builder.addRoute(IPV4_DEFAULT_ROUTE, IPV4_DEFAULT_PREFIX_LENGTH)
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Vpn,
            message = "firewall_route_added_ipv4_default",
        )

        vpnInterface = builder.establish()
        if (vpnInterface == null) {
            failAndStop("VPN interface was not established")
            return
        }

        AegisVpnController.updateActiveFirewall(AppVpnMode.AppFirewall, allowedCount)
        AegisVpnController.updateLastFirewallResult("Blocking $allowedCount selected app(s)")
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Vpn,
            message = "firewall_vpn_started blocked_app_count=$allowedCount",
        )
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Vpn,
            message = "firewall_no_packet_forwarding",
        )
        AegisVpnController.tunnelEstablished()
        updateForegroundNotification(VpnState.Running)
    }

    private fun stopVpn() {
        AegisVpnController.disconnect()
        updateForegroundNotification(VpnState.Stopping)
        stopRuntimeBlocking()
        closeVpnInterface()
        stopForegroundNotification()
        markFirewallStopped()
        AegisVpnController.stopped()
        stopSelf()
    }

    private fun runProtectExperiment() {
        if (vpnInterface == null) {
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Warning,
                source = DiagnosticSource.Vpn,
                message = "Protect experiment ignored because VPN interface is not open",
            )
            stopSelf()
            return
        }

        val diagnosticsStore = DiagnosticsStore()
        DummySocketProtectExperiment(
            diagnosticsStore = diagnosticsStore,
            socketProtector = VpnServiceSocketProtector(this),
            vpnLifecycleStateProvider = { AegisVpnController.state.value.name },
        ).run()
        diagnosticsStore.snapshot().forEach(AegisVpnController::addDiagnostic)
    }

    private fun failAndStop(message: String) {
        stopRuntimeBlocking()
        closeVpnInterface()
        stopForegroundNotification()
        markFirewallStopped()
        AegisVpnController.fail(message)
        AegisVpnController.cleanupError()
        stopSelf()
    }

    private fun emitFirewallError(message: String) {
        AegisVpnController.updateLastFirewallResult(message)
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Error,
            source = DiagnosticSource.Vpn,
            message = "firewall_error reason=$message",
        )
    }

    private fun markFirewallStopped() {
        if (activeMode == AppVpnMode.AppFirewall) {
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Info,
                source = DiagnosticSource.Vpn,
                message = "firewall_vpn_stopped",
            )
            AegisVpnController.updateLastFirewallResult("Stopped")
        }
        AegisVpnController.updateActiveFirewall(activeMode, 0)
    }

    private fun startDummyRuntime() {
        val currentInterface = vpnInterface ?: run {
            failAndStop("VPN interface was closed before runtime start")
            return
        }
        val sessionId = "aegis-${System.currentTimeMillis()}"
        runtimeStartJob?.cancel()
        runtimeStartJob = serviceScope.launch {
            try {
                runtime.start(
                    RuntimeConfig(
                        sessionId = sessionId,
                        tunFd = currentInterface.fd,
                    ),
                )
                if (vpnInterface != null && runtime.state.value == RuntimeState.Running) {
                    AegisVpnController.tunnelEstablished()
                    updateForegroundNotification(VpnState.Running)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: RuntimeException) {
                failAndStop("Dummy runtime error: ${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    private fun stopRuntimeBlocking() {
        runtimeStartJob?.cancel()
        runtimeStartJob = null
        runBlocking {
            runtime.stop()
        }
        AegisVpnController.updateRuntimeState(runtime.state.value)
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
        AegisVpnController.updateForegroundNotificationActive(true)
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
        AegisVpnController.updateForegroundNotificationActive(false)
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
        const val ACTION_RUN_PROTECT_EXPERIMENT = "net.aegisnet.app.vpn.RUN_PROTECT_EXPERIMENT"
        const val EXTRA_MODE = "net.aegisnet.app.vpn.EXTRA_MODE"
        const val EXTRA_SELECTED_PACKAGES = "net.aegisnet.app.vpn.EXTRA_SELECTED_PACKAGES"

        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "aegis_vpn_service"
        private const val NOTIFICATION_CHANNEL_NAME = "VPN service"
        private const val DUMMY_MTU = 1280
        private const val DUMMY_IPV4_ADDRESS = "10.255.0.2"
        private const val DUMMY_IPV4_PREFIX_LENGTH = 32
        private const val IPV4_DEFAULT_ROUTE = "0.0.0.0"
        private const val IPV4_DEFAULT_PREFIX_LENGTH = 0
    }

    private fun intentMode(intent: Intent): AppVpnMode {
        val rawMode = intent.getStringExtra(EXTRA_MODE) ?: return AppVpnMode.Diagnostics
        return AppVpnMode.entries.firstOrNull { it.name == rawMode } ?: AppVpnMode.Diagnostics
    }

    private fun intentSelectedPackages(intent: Intent): List<String> {
        return intent.getStringArrayListExtra(EXTRA_SELECTED_PACKAGES)
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}
