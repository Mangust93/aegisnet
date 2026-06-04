package net.aegisnet.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import net.aegisnet.app.ui.AegisNetApp
import net.aegisnet.app.vpn.AegisVpnController
import net.aegisnet.app.vpn.AegisVpnService

class MainActivity : ComponentActivity() {
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

        setContent {
            AegisNetApp(
                vpnState = AegisVpnController.state,
                runtimeState = AegisVpnController.runtimeState,
                diagnostics = AegisVpnController.diagnostics,
                sessionStartedAtMillis = AegisVpnController.sessionStartedAtMillis,
                foregroundNotificationActive = AegisVpnController.foregroundNotificationActive,
                onConnect = ::connect,
                onDisconnect = ::disconnect,
                onClearDiagnostics = AegisVpnController::clearDiagnostics,
                onRunProtectExperiment = ::runProtectExperiment,
                onCopyDiagnostics = ::copyDiagnostics,
            )
        }
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
}
