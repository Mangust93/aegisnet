# Claude Validation Prompt — AegisNet Stage 0

You are validating the feasibility of AegisNet, an Android-first adaptive VPN / overlay networking project.

Repository:
https://github.com/Mangust93/aegisnet.git

Current goal:
Do not write product code yet. Validate architecture, risks, and first implementation path.

Project direction:
- Android app
- Kotlin
- Jetpack Compose
- Android VpnService
- split tunneling / whitelist routing
- DNS and IPv6 leak prevention
- isolated network runtime layer
- sing-box/libbox feasibility research
- VLESS Reality / Hysteria2-compatible profiles later

Important constraints:
- Do not assume libbox is a stable SDK.
- Treat sing-box GPLv3 licensing as a blocking architectural risk.
- Do not tightly couple Android UI to one network core.
- Do not build mesh/decentralized relay network in MVP.
- Do not use OpenZiti as user VPN transport.
- Do not use Yggdrasil in MVP.
- Do not use VoIP/WebRTC/VK-call tunneling as primary architecture.
- WireGuard mesh must be treated as a separate future subsystem, not sing-box WireGuard outbound.

Please review the repository documents:

- README.md
- docs/ARCHITECTURE.md
- docs/ROADMAP.md
- docs/ADR-001-project-foundation.md

Required output:

1. Feasibility verdict
2. Main architectural risks
3. Licensing risk analysis
4. Recommended network runtime strategy
5. Android VpnService lifecycle risks
6. Per-app whitelist routing limitations
7. DNS/IPv6 leak handling requirements
8. Android 14/15 foreground service requirements
9. Recommended minimal repository structure
10. Stage 0 deliverables
11. Stage 1 implementation plan
12. What should NOT be coded yet
13. Questions that must be answered before implementation

Do not modify files yet.
Do not create Android project yet.
Return a structured technical review.
