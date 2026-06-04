package net.aegisnet.app.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.ProtectExperimentDiagnosticName
import net.aegisnet.app.runtime.RuntimeState
import net.aegisnet.app.runtime.label
import net.aegisnet.app.runtime.name
import net.aegisnet.app.vpn.VpnState
import net.aegisnet.app.vpn.name

@Composable
fun AegisNetApp(
    vpnState: StateFlow<VpnState>,
    runtimeState: StateFlow<RuntimeState>,
    diagnostics: StateFlow<List<DiagnosticEvent>>,
    sessionStartedAtMillis: StateFlow<Long?>,
    foregroundNotificationActive: StateFlow<Boolean>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onRunProtectExperiment: () -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ConnectionShell(
                vpnState = vpnState,
                runtimeState = runtimeState,
                diagnostics = diagnostics,
                sessionStartedAtMillis = sessionStartedAtMillis,
                foregroundNotificationActive = foregroundNotificationActive,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onClearDiagnostics = onClearDiagnostics,
                onRunProtectExperiment = onRunProtectExperiment,
                onCopyDiagnostics = onCopyDiagnostics,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun ConnectionShell(
    vpnState: StateFlow<VpnState>,
    runtimeState: StateFlow<RuntimeState>,
    diagnostics: StateFlow<List<DiagnosticEvent>>,
    sessionStartedAtMillis: StateFlow<Long?>,
    foregroundNotificationActive: StateFlow<Boolean>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClearDiagnostics: () -> Unit,
    onRunProtectExperiment: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentVpnState by vpnState.collectAsState()
    val currentRuntimeState by runtimeState.collectAsState()
    val currentDiagnostics by diagnostics.collectAsState()
    val currentSessionStartedAtMillis by sessionStartedAtMillis.collectAsState()
    val currentForegroundNotificationActive by foregroundNotificationActive.collectAsState()
    val isConnected = currentVpnState.isActive
    val latestProtectResult = currentDiagnostics.latestProtectExperimentResult()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Text(
                text = "AegisNet",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Private network shell",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusPill(vpnState = currentVpnState)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = if (isConnected) onDisconnect else onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (isConnected) "Disconnect" else "Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        DeviceSessionInfo(
            vpnState = currentVpnState,
            runtimeState = currentRuntimeState,
            sessionStartedAtMillis = currentSessionStartedAtMillis,
            foregroundNotificationActive = currentForegroundNotificationActive,
        )

        DiagnosticsLab(
            vpnState = currentVpnState,
            runtimeState = currentRuntimeState,
            latestProtectResult = latestProtectResult,
            protectExperimentEnabled = currentVpnState is VpnState.Running,
            onRunProtectExperiment = onRunProtectExperiment,
        )

        DiagnosticsHistory(
            events = currentDiagnostics,
            onClear = onClearDiagnostics,
            onCopyDiagnostics = onCopyDiagnostics,
        )
    }
}

@Composable
private fun StatusPill(vpnState: VpnState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape),
            color = if (vpnState is VpnState.Running) Color(0xFF15803D) else Color(0xFF8A94A6),
            content = {},
        )
        Text(
            text = vpnState.label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun DeviceSessionInfo(
    vpnState: VpnState,
    runtimeState: RuntimeState,
    sessionStartedAtMillis: Long?,
    foregroundNotificationActive: Boolean,
) {
    SectionCard(title = "Device / Session") {
        InfoRow(title = "VPN state", value = vpnState.name)
        InfoRow(title = "Runtime state", value = runtimeState.name)
        InfoRow(
            title = "Session duration",
            value = sessionDurationLabel(
                isConnected = vpnState is VpnState.Running,
                startedAtMillis = sessionStartedAtMillis,
            ),
        )
        InfoRow(
            title = "Foreground notification",
            value = if (foregroundNotificationActive) "Active" else "Inactive",
        )
        InfoRow(title = "Android SDK", value = Build.VERSION.SDK_INT.toString())
    }
}

@Composable
private fun DiagnosticsLab(
    vpnState: VpnState,
    runtimeState: RuntimeState,
    latestProtectResult: ProtectExperimentResult,
    protectExperimentEnabled: Boolean,
    onRunProtectExperiment: () -> Unit,
) {
    SectionCard(title = "Diagnostics Lab") {
        LabCard(
            title = "Protect Experiment",
            status = latestProtectResult.statusLabel,
            detail = latestProtectResult.reasonLabel,
        ) {
            Button(
                onClick = onRunProtectExperiment,
                enabled = protectExperimentEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = "Run Protect Experiment")
            }
        }
        LabCard(
            title = "VPN Lifecycle",
            status = vpnState.label,
            detail = "Current state: ${vpnState.name}",
        )
        LabCard(
            title = "Runtime Lifecycle",
            status = runtimeState.label,
            detail = "Dummy runtime state: ${runtimeState.name}",
        )
    }
}

@Composable
private fun DiagnosticsHistory(
    events: List<DiagnosticEvent>,
    onClear: () -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    SectionCard(title = "Diagnostics") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onCopyDiagnostics,
                enabled = events.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = "Copy")
            }
            TextButton(
                onClick = onClear,
                enabled = events.isNotEmpty(),
            ) {
                Text(text = "Clear")
            }
        }

        if (events.isEmpty()) {
            Text(
                text = "No events",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    items = events.takeLast(DIAGNOSTIC_EVENT_LIMIT).asReversed(),
                ) { event ->
                    DiagnosticEventRow(event = event)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun LabCard(
    title: String,
    status: String,
    detail: String,
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Badge(text = status)
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
            )
            action?.invoke(this)
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DiagnosticEventRow(event: DiagnosticEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(text = event.level.name)
                Badge(text = event.source.name)
            }
            Text(
                text = event.timestampLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f),
            )
        }
        Text(
            text = event.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun sessionDurationLabel(
    isConnected: Boolean,
    startedAtMillis: Long?,
): String {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isConnected, startedAtMillis) {
        while (isConnected && startedAtMillis != null) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    if (!isConnected || startedAtMillis == null) return "Not connected"

    val elapsedSeconds = ((nowMillis - startedAtMillis).coerceAtLeast(0L) / 1000L).toInt()
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun DiagnosticEvent.timestampLabel(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestampMillis))
}

private fun List<DiagnosticEvent>.latestProtectExperimentResult(): ProtectExperimentResult {
    val latestProtectEvent = asReversed().firstOrNull {
        it.message.startsWith(ProtectExperimentDiagnosticName.ProtectExperimentCompleted.eventName) ||
            it.message.startsWith(ProtectExperimentDiagnosticName.ProtectExperimentFailed.eventName)
    } ?: return ProtectExperimentResult.NotRun

    val reason = latestProtectEvent.reason()
    return if (latestProtectEvent.message.startsWith(ProtectExperimentDiagnosticName.ProtectExperimentCompleted.eventName)) {
        ProtectExperimentResult.Passed(reason)
    } else {
        ProtectExperimentResult.Failed(reason)
    }
}

private fun DiagnosticEvent.reason(): String {
    val rawReason = message.substringAfter(" reason=", missingDelimiterValue = "").trim()
    return if (rawReason == "protect returned false") {
        "Socket protection failed on this run. No connection was attempted."
    } else {
        rawReason.ifBlank { "No reason recorded" }
    }
}

private sealed interface ProtectExperimentResult {
    val statusLabel: String
    val reasonLabel: String

    data object NotRun : ProtectExperimentResult {
        override val statusLabel = "Not run"
        override val reasonLabel = "Run while connected to validate protect-before-connect ordering."
    }

    data class Passed(private val reason: String) : ProtectExperimentResult {
        override val statusLabel = "Passed"
        override val reasonLabel = "Last reason: $reason"
    }

    data class Failed(private val reason: String) : ProtectExperimentResult {
        override val statusLabel = "Failed"
        override val reasonLabel = "Last reason: $reason"
    }
}

private const val DIAGNOSTIC_EVENT_LIMIT = 20

@Preview(showBackground = true)
@Composable
private fun AegisNetAppPreview() {
    AegisNetApp(
        vpnState = MutableStateFlow(VpnState.Idle),
        runtimeState = MutableStateFlow(RuntimeState.Stopped),
        diagnostics = MutableStateFlow(emptyList()),
        sessionStartedAtMillis = MutableStateFlow(null),
        foregroundNotificationActive = MutableStateFlow(false),
        onConnect = {},
        onDisconnect = {},
        onClearDiagnostics = {},
        onRunProtectExperiment = {},
        onCopyDiagnostics = {},
    )
}
