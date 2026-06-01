package net.aegisnet.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.runtime.RuntimeState
import net.aegisnet.app.runtime.label
import net.aegisnet.app.vpn.VpnState

@Composable
fun AegisNetApp(
    vpnState: StateFlow<VpnState>,
    diagnostics: StateFlow<List<DiagnosticEvent>>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ConnectionShell(
                vpnState = vpnState,
                diagnostics = diagnostics,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
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
    diagnostics: StateFlow<List<DiagnosticEvent>>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentVpnState by vpnState.collectAsState()
    val currentDiagnostics by diagnostics.collectAsState()
    val runtimeState = RuntimeState.Stopped
    val isConnected = currentVpnState.isActive
    val latestDiagnostic = currentDiagnostics.lastOrNull()?.message ?: "No events"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
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

            Spacer(modifier = Modifier.height(24.dp))

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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaceholderCard(
                title = "Status",
                detail = currentVpnState.label,
            )
            PlaceholderCard(
                title = "Runtime",
                detail = "Dummy runtime: ${runtimeState.label}",
            )
            PlaceholderCard(
                title = "Diagnostics",
                detail = latestDiagnostic,
            )
        }
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
            color = Color(0xFF8A94A6),
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
private fun PlaceholderCard(
    title: String,
    detail: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AegisNetAppPreview() {
    AegisNetApp(
        vpnState = MutableStateFlow(VpnState.Idle),
        diagnostics = MutableStateFlow(emptyList()),
        onConnect = {},
        onDisconnect = {},
    )
}
