package net.aegisnet.app.vpn

import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource
import org.junit.Assert.assertTrue
import org.junit.Test

class AegisVpnControllerTest {
    @Test
    fun diagnosticsTextExportsReadableEvents() {
        AegisVpnController.clearDiagnostics()

        AegisVpnController.addDiagnostic(
            level = DiagnosticLevel.Info,
            source = DiagnosticSource.Ui,
            message = "copy test",
        )

        val text = AegisVpnController.diagnosticsText()

        assertTrue(text.contains("Info Ui: copy test"))
        AegisVpnController.clearDiagnostics()
    }
}
