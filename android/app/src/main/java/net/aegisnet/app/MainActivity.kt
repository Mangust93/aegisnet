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
import net.aegisnet.app.firewall.AppFirewallSelectionStore
import net.aegisnet.app.firewall.AppVpnMode
import net.aegisnet.app.firewall.FirewallProfile
import net.aegisnet.app.firewall.InstalledAppInfo
import net.aegisnet.app.firewall.InstalledAppsRepository
import net.aegisnet.app.networking.CheckStatus
import net.aegisnet.app.networking.DeviceNetworkInfo
import net.aegisnet.app.networking.NetworkMonitorRunner
import net.aegisnet.app.networking.NetworkMonitorSnapshot
import net.aegisnet.app.networking.NetworkingLabRunner
import net.aegisnet.app.networking.NetworkingLabStatus
import net.aegisnet.app.networking.NetworkingLabTest
import net.aegisnet.app.networking.NetworkingLabTestResult
import net.aegisnet.app.runtime.ImportedProxyConfig
import net.aegisnet.app.runtime.ProxyConfigStore
import net.aegisnet.app.runtime.ProxyConfigValidator
import net.aegisnet.app.ui.AegisNetApp
import net.aegisnet.app.vpn.AegisVpnController
import net.aegisnet.app.vpn.AegisVpnService

class MainActivity : ComponentActivity() {
    private val networkingLabRunner = NetworkingLabRunner()
    private val networkMonitorRunner = NetworkMonitorRunner()
    private val deviceNetworkInfo = MutableStateFlow(DeviceNetworkInfo.Unknown)
    private lateinit var appFirewallSelectionStore: AppFirewallSelectionStore
    private lateinit var installedAppsRepository: InstalledAppsRepository
    private val installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val selectedFirewallPackages = MutableStateFlow<Set<String>>(emptySet())
    private val activeFirewallProfile = MutableStateFlow(FirewallProfile.Custom)
    private val networkMonitorHistory = MutableStateFlow<List<NetworkMonitorSnapshot>>(emptyList())
    private val selectedMode = MutableStateFlow(AppVpnMode.Diagnostics)
    private lateinit var proxyConfigStore: ProxyConfigStore
    private val proxyConfigValidator = ProxyConfigValidator()
    private val importedProxyConfigs = MutableStateFlow<List<ImportedProxyConfig>>(emptyList())
    private val activeProxyConfig = MutableStateFlow<ImportedProxyConfig?>(null)
    private val proxyConfigValidationMessage = MutableStateFlow("")
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
        appFirewallSelectionStore = AppFirewallSelectionStore(this)
        installedAppsRepository = InstalledAppsRepository(this)
        proxyConfigStore = ProxyConfigStore(this)
        appFirewallSelectionStore.initializeProfiles().forEach { profile ->
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Info,
                source = DiagnosticSource.Ui,
                message = "firewall_profile_created profile=${profile.name}",
            )
        }
        activeFirewallProfile.value = appFirewallSelectionStore.loadActiveProfile()
        selectedFirewallPackages.value = appFirewallSelectionStore.load(activeFirewallProfile.value)
        reloadProxyConfigs()
        refreshDeviceNetworkInfo()
        loadInstalledApps()

        setContent {
            AegisNetApp(
                vpnState = AegisVpnController.state,
                runtimeState = AegisVpnController.runtimeState,
                diagnostics = AegisVpnController.diagnostics,
                sessionStartedAtMillis = AegisVpnController.sessionStartedAtMillis,
                foregroundNotificationActive = AegisVpnController.foregroundNotificationActive,
                networkingLabResults = AegisVpnController.networkingLabResults,
                deviceNetworkInfo = deviceNetworkInfo,
                selectedMode = selectedMode,
                installedApps = installedApps,
                selectedFirewallPackages = selectedFirewallPackages,
                activeFirewallProfile = activeFirewallProfile,
                currentMode = AegisVpnController.currentMode,
                activeBlockedAppCount = AegisVpnController.activeBlockedAppCount,
                lastFirewallResult = AegisVpnController.lastFirewallResult,
                lastRuntimeError = AegisVpnController.lastRuntimeError,
                networkMonitorHistory = networkMonitorHistory,
                importedProxyConfigs = importedProxyConfigs,
                activeProxyConfig = activeProxyConfig,
                proxyConfigValidationMessage = proxyConfigValidationMessage,
                onModeSelected = ::selectMode,
                onFirewallProfileSelected = ::selectFirewallProfile,
                onFirewallPackageToggled = ::toggleFirewallPackage,
                onImportProxyConfig = ::importProxyConfig,
                onSelectProxyConfig = ::selectProxyConfig,
                onConnect = ::connect,
                onDisconnect = ::disconnect,
                onClearDiagnostics = AegisVpnController::clearDiagnostics,
                onRunNetworkingTest = ::runNetworkingTest,
                onRunFullNetworkCheck = ::runFullNetworkCheck,
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
        if (selectedMode.value == AppVpnMode.AppFirewall && selectedFirewallPackages.value.isEmpty()) {
            AegisVpnController.updateLastFirewallResult("Select at least one app before connecting")
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Warning,
                source = DiagnosticSource.Ui,
                message = "firewall_error reason=no selected apps",
            )
            return
        }

        if (selectedMode.value == AppVpnMode.RealProxyRuntime && activeProxyConfig.value == null) {
            proxyConfigValidationMessage.value = "Import and select a proxy config before connecting"
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Warning,
                source = DiagnosticSource.Ui,
                message = "real_proxy_error reason=no active config",
            )
            return
        }

        if (selectedMode.value == AppVpnMode.NetworkMonitor) {
            runFullNetworkCheck()
            return
        }

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

        val activePackages = appFirewallSelectionStore.load(activeFirewallProfile.value)
        val intent = Intent(this, AegisVpnService::class.java)
            .setAction(AegisVpnService.ACTION_START)
            .putExtra(AegisVpnService.EXTRA_MODE, selectedMode.value.name)
            .putStringArrayListExtra(
                AegisVpnService.EXTRA_SELECTED_PACKAGES,
                ArrayList(activePackages.sorted()),
            )
        startForegroundService(intent)
    }

    private fun selectMode(mode: AppVpnMode) {
        selectedMode.value = mode
        AegisVpnController.selectMode(mode)
    }

    private fun importProxyConfig(rawInput: String) {
        val result = proxyConfigValidator.validate(rawInput)
        val config = result.config
        if (config == null) {
            proxyConfigValidationMessage.value = result.error ?: "Config validation failed"
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Warning,
                source = DiagnosticSource.Ui,
                message = "proxy_config_import_failed reason=${proxyConfigValidationMessage.value}",
            )
            return
        }

        proxyConfigStore.save(config)
        reloadProxyConfigs()
        proxyConfigValidationMessage.value = "Stored ${config.name} (${config.type.label})"
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Ui,
            message = "proxy_config_imported id=${config.id} type=${config.type.name}",
        )
    }

    private fun selectProxyConfig(configId: String) {
        proxyConfigStore.setActive(configId)
        reloadProxyConfigs()
        val active = activeProxyConfig.value ?: return
        proxyConfigValidationMessage.value = "Selected ${active.name} (${active.type.label})"
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Ui,
            message = "proxy_config_selected id=${active.id} type=${active.type.name}",
        )
    }

    private fun reloadProxyConfigs() {
        importedProxyConfigs.value = proxyConfigStore.loadAll()
        activeProxyConfig.value = proxyConfigStore.loadActive()
    }

    private fun selectFirewallProfile(profile: FirewallProfile) {
        activeFirewallProfile.value = profile
        appFirewallSelectionStore.saveActiveProfile(profile)
        val packages = appFirewallSelectionStore.load(profile)
        selectedFirewallPackages.value = packages
        AegisVpnController.updateLastFirewallResult("${profile.label} profile selected (${packages.size} app(s))")
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Ui,
            message = "firewall_profile_switched profile=${profile.name} selectedAppCount=${packages.size}",
        )
    }

    private fun toggleFirewallPackage(packageName: String, selected: Boolean) {
        val updated = if (selected) {
            selectedFirewallPackages.value + packageName
        } else {
            selectedFirewallPackages.value - packageName
        }
        selectedFirewallPackages.value = updated
        appFirewallSelectionStore.save(activeFirewallProfile.value, updated)
        AegisVpnController.updateLastFirewallResult("${activeFirewallProfile.value.label}: ${updated.size} app(s) selected")
        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Ui,
            message = "firewall_profile_updated profile=${activeFirewallProfile.value.name} selectedAppCount=${updated.size}",
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

    private fun runFullNetworkCheck() {
        refreshDeviceNetworkInfo()
        val runningSnapshot = NetworkMonitorSnapshot(
            publicIp = networkMonitorHistory.value.firstOrNull()?.publicIp,
            networkType = deviceNetworkInfo.value.networkType,
            vpnActive = AegisVpnController.state.value.isActive,
            status = CheckStatus.Running,
            dnsStatus = CheckStatus.Running,
            httpsStatus = CheckStatus.Running,
            durationMillis = 0L,
            lastError = null,
            checkedAtMillis = System.currentTimeMillis(),
        )
        networkMonitorHistory.value = listOf(runningSnapshot) +
            networkMonitorHistory.value.take(NETWORK_MONITOR_HISTORY_LIMIT - 1)
        lifecycleScope.launch {
            val networkType = deviceNetworkInfo.value.networkType
            val vpnActive = AegisVpnController.state.value.isActive
            val result = withContext(Dispatchers.IO) {
                networkMonitorRunner.runFullCheck(
                    networkType = networkType,
                    vpnActive = vpnActive,
                ) { event ->
                    AegisVpnController.addDiagnostic(event)
                }
            }
            networkMonitorHistory.value = listOf(result) + networkMonitorHistory.value
                .filterNot { it.status == CheckStatus.Running }
                .take(NETWORK_MONITOR_HISTORY_LIMIT - 1)
            refreshDeviceNetworkInfo()
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

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                installedAppsRepository.loadLaunchableApps()
            }
            installedApps.value = apps
            AegisVpnController.addDiagnostic(
                level = DiagnosticLevel.Info,
                source = DiagnosticSource.System,
                message = "installed_apps_loaded count=${apps.size}",
            )
        }
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

    private companion object {
        const val NETWORK_MONITOR_HISTORY_LIMIT = 8
    }
}
