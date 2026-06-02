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

## Stage 2 — Safe Networking Experiments

Status:
Planned documentation stage.

Tasks:
- VpnService.protect() experiment plan
- controlled dummy socket protection test plan, if safe
- route experiment planning
- DNS handling planning
- IPv6 handling planning
- leak and loop safety checklist

Out of scope:
- production traffic tunneling
- sing-box/libbox integration
- Xray, VLESS, Reality, or Hysteria2 integration
- packet forwarding runtime
- Android code changes
- dependency or Gradle changes

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
