# Stage 0 Validation

## Status

Accepted as project direction.

## Feasibility Verdict

AegisNet is feasible as a staged Android VPN / overlay networking project.

The correct first goal is not to build a full VPN product immediately, but to validate Android VPN lifecycle, runtime separation, foreground service behavior, DNS/IPv6 safety, and licensing strategy.

## Key Decision

Do not integrate sing-box/libbox in Stage 1.

Stage 1 must use a dummy runtime abstraction first.

## Confirmed Risks

- sing-box/libbox GPLv3 licensing risk
- libbox is not a stable Android SDK assumption
- Android VpnService lifecycle complexity
- per-app routing limitations
- DNS/IPv6 leak risk
- Android 14/15 foreground service restrictions
- real protocol integration should be postponed

## Accepted Stage 1 Direction

Stage 1 should implement:

- Kotlin Android project
- Jetpack Compose shell
- Connect / Disconnect UI
- VpnService.prepare() flow
- Foreground service skeleton
- VPN state machine
- Diagnostics log screen
- Dummy runtime abstraction

Stage 1 must not implement:

- libbox
- sing-box config generation
- VLESS Reality
- Hysteria2
- mesh
- relays
- OpenZiti
- Yggdrasil
- adaptive failover
