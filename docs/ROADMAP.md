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
In progress documentation stage.

Tasks:
- Stage 2.1 VpnService.protect() experiment specification documented
- controlled dummy socket protection test plan, if safe
- route experiment planning
- DNS handling planning
- IPv6 handling planning
- leak and loop safety checklist

Stage 2.1 scope:
- document protect-before-connect requirements
- document controlled dummy socket lifecycle
- document cleanup and diagnostics expectations
- document device validation and rollback expectations
- no Android code changes

Out of scope:
- production traffic tunneling
- sing-box/libbox integration
- Xray, VLESS, Reality, or Hysteria2 integration
- packet forwarding runtime
- DNS changes
- route changes
- real proxy runtime
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
