# AegisNet Roadmap

## Stage 0 — Feasibility

Goal:
Validate technical and legal viability before product coding.

Tasks:
- license strategy review
- sing-box/libbox feasibility validation
- Android VpnService lifecycle validation
- native build pipeline validation
- repository structure validation

## Stage 1 — Minimal Android Shell

Tasks:
- Kotlin Android project
- Compose shell
- Connect / Disconnect UI
- foreground service skeleton
- diagnostics log screen

## Stage 2 — VPN Correctness

Tasks:
- whitelist routing
- DNS leak handling
- IPv6 handling
- reconnect logic
- Android 14/15 foreground service compliance

## Stage 3 — Protocol Profiles

Tasks:
- Reality-compatible profile
- Hysteria2-compatible profile
- profile import/export
- endpoint diagnostics

## Stage 4 — Adaptive Routing

Tasks:
- failover
- transport health checks
- safe reconnect
- no-leak transition handling

## Stage 5 — Future Overlay Research

Tasks:
- Headscale/WireGuard mesh research
- community relay research
- OpenZiti hidden control plane research
