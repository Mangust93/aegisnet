package net.aegisnet.app.vpn

sealed interface VpnState {
    val label: String
    val isIdle: Boolean
        get() = this is Idle
    val isActive: Boolean
        get() = !isIdle

    data object Idle : VpnState {
        override val label = "Disconnected"
    }

    data object PreparingConsent : VpnState {
        override val label = "Preparing consent"
    }

    data object StartingService : VpnState {
        override val label = "Starting service"
    }

    data object EstablishingTunnel : VpnState {
        override val label = "Establishing tunnel"
    }

    data object Running : VpnState {
        override val label = "Connected"
    }

    data object Stopping : VpnState {
        override val label = "Stopping"
    }

    data object Revoked : VpnState {
        override val label = "Revoked"
    }

    data class Error(val message: String) : VpnState {
        override val label = "Error"
    }
}
