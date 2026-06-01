# AegisNet Architecture

## Core Principle

AegisNet separates Android product layer from network runtime layer.

## V1 Layers

- Android UI App: Jetpack Compose, profiles, settings, diagnostics.
- Android VPN Layer: VpnService, foreground service, per-app routing, DNS/IPv6 leak control.
- Network Runtime Layer: isolated local core service/process where possible.
- Server Layer: Reality/Hysteria2 compatible endpoints.

## Constraints

- Licensing must be resolved before depending on GPL components.
- libbox is a feasibility risk, not a guaranteed stable SDK.
- WireGuard mesh must not depend on sing-box WireGuard outbound.
- OpenZiti is future hidden control plane only, not user VPN transport.
- Yggdrasil is experimental and not part of MVP.

## V1 Non-Goals

- no full decentralization
- no token economy
- no public relay marketplace
- no Tor-like multi-hop
- no VoIP/WebRTC tunneling as primary transport
