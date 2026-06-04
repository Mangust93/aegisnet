package net.aegisnet.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import net.aegisnet.app.networking.DeviceNetworkInfo
import net.aegisnet.app.networking.NetworkingLabRunner
import net.aegisnet.app.networking.NetworkingLabStatus
import net.aegisnet.app.networking.NetworkingLabTest
import net.aegisnet.app.networking.NetworkingLabTestResult
import net.aegisnet.app.ui.AegisNetApp
import net.aegisnet.app.vpn.AegisVpnController
import net.aegisnet.app.vpn.AegisVpnService

class MainActivity : ComponentActivity() {
    private val networkingLabRunner = NetworkingLabRunner()
    private val deviceNetworkInfo = MutableStateFlow(DeviceNetworkInfo.Unknown)
    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Info,
                source = DiagnosticSource.System,
                message = "VPN consent accepted",
            )
            startVpnServiceAfterConsent()
        } else {
            AegisVpnController.fail("VPN consent denied")
            AegisVpnController.cleanupError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshDeviceNetworkInfo()

        setContent {
            AegisNetApp(
                vpnState = AegisVpnController.state,
                runtimeState = AegisVpnController.runtimeState,
                diagnostics = AegisVpnController.diagnostics,
                sessionStartedAtMillis = AegisVpnController.sessionStartedAtMillis,
                foregroundNotificationActive = AegisVpnController.foregroundNotificationActive,
                networkingLabResults = AegisVpnController.networkingLabResults,
                deviceNetworkInfo = deviceNetworkInfo,
                onConnect = ::connect,
                onDisconnect = ::disconnect,
                onClearDiagnostics = AegisVpnController::clearDiagnostics,
                onRunNetworkingTest = ::runNetworkingTest,
                onRunProtectExperiment = ::runProtectExperiment,
                onCopyDiagnostics = ::copyDiagnostics,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDeviceNetworkInfo()
    }

    private fun connect() {
        val transition = AegisVpnController.connect()
        if (!transition.changed) return

        val consentIntent = VpnService.prepare(this)
        if (consentIntent != null) {
            vpnConsentLauncher.launch(consentIntent)
            return
        }

        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.System,
            message = "VPN consent already granted",
        )
        startVpnServiceAfterConsent()
    }

    private fun startVpnServiceAfterConsent() {
        val transition = AegisVpnController.consentPrepared()
        if (!transition.changed) return

        startForegroundService(
            Intent(this, AegisVpnService::class.java).setAction(AegisVpnService.ACTION_START),
        )
    }

    private fun disconnect() {
        startService(
            Intent(this, AegisVpnService::class.java).setAction(AegisVpnService.ACTION_STOP),
        )
    }

    private fun runProtectExperiment() {
        startService(
            Intent(
                this,
                AegisVpnService::class.java,
            ).setAction(AegisVpnService.ACTION_RUN_PROTECT_EXPERIMENT),
        )
    }

    private fun runNetworkingTest(test: NetworkingLabTest) {
        AegisVpnController.updateNetworkingLabResult(
            test = test,
            result = NetworkingLabTestResult.running("Test is running with a short timeout."),
        )
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                networkingLabRunner.run(test) { event ->
                    AegisVpnController.addDiagnostic(event)
                }
            }
            AegisVpnController.updateNetworkingLabResult(test, result)
            if (result.status == NetworkingLabStatus.Passed) {
                refreshDeviceNetworkInfo()
            }
        }
    }

    private fun copyDiagnostics() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "AegisNet diagnostics",
                AegisVpnController.diagnosticsText(),
            ),
        )
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Ui,
            message = "Diagnostics copied to clipboard",
        )
    }

    private fun refreshDeviceNetworkInfo() {
        deviceNetworkInfo.value = DeviceNetworkInfo(
            networkType = readNetworkType(),
            packageName = packageName,
            versionName = readVersionName(),
            versionCode = readVersionCode(),
        )
    }

    private fun readNetworkType(): String {
        return try {
            val connectivityManager = getSystemService(ConnectivityManager::class.java)
            val network = connectivityManager.activeNetwork ?: return "Unavailable"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unavailable"
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                else -> "Other"
            }
        } catch (error: SecurityException) {
            "Unavailable: permission denied"
        } catch (error: RuntimeException) {
            "Unavailable: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun readVersionName(): String {
        return runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
        }.getOrDefault("Unknown")
    }

    private fun readVersionCode(): Long {
        return runCatching {
            val info = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }.getOrDefault(0L)
    }
}
