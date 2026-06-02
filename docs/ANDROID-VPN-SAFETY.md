# Android VPN Safety

## VpnService Requirements

AegisNet must handle:

- user consent through VpnService.prepare()
- foreground service lifecycle
- service revoke through onRevoke()
- idempotent start/stop
- VPN interface fd cleanup
- protected upstream sockets via VpnService.protect()

## Stage 1 Safety Boundary

Stage 1 intentionally validates Android VPN lifecycle behavior with a dummy TUN setup only.

The Stage 1 implementation has no routes, no DNS configuration, no packet forwarding, no upstream sockets, and no real protocol runtime. It must not be treated as a production VPN path or as evidence that traffic tunneling, DNS safety, IPv6 safety, or leak prevention is complete.

## Per-App Routing

Android VpnService supports package-level allow/disallow lists.

Limitations:

- allow-list and deny-list modes cannot be mixed
- changing the list requires tunnel recreation
- routing is package-level, not domain-level
- apps outside whitelist are direct by design

## DNS Safety

DNS must be explicitly handled from the first real VPN implementation.

Rules:

- configure explicit VPN DNS for tunneled traffic
- avoid fallback to system DNS
- test Android Private DNS interaction
- document limitations of app-level DoH/DoT

## IPv6 Safety

IPv6 must not silently leak.

Rules:

- if IPv6 is unsupported, block IPv6
- if IPv6 is supported, configure IPv6 address, route, and DNS
- test IPv4-only and IPv6-capable networks

## Android 14/15 Risks

The app must validate:

- foreground service declaration
- persistent notification
- service start restrictions
- battery optimization behavior
- target SDK behavior on real devices
