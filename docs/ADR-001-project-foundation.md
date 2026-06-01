# ADR-001: Project Foundation

## Status

Accepted

## Context

The project aims to build an Android-first adaptive VPN / overlay networking application.

Initial architecture research identified major risks:

- GPLv3 implications around sing-box/libbox
- unstable mobile bindings
- Android VPN lifecycle complexity
- DNS/IPv6 leak handling requirements
- Android 14/15 foreground service restrictions

## Decision

The project will follow a feasibility-first approach.

AegisNet separates:

- Android product layer
- Android VPN layer
- network runtime layer

No tight coupling to one VPN core is allowed before technical and legal validation.

## Consequences

Positive:
- reduced lock-in
- safer architecture evolution
- easier runtime replacement
- better long-term maintainability

Negative:
- slower MVP start
- more upfront research
- more architectural work before UI

## MVP Non-Goals

- full decentralization
- token economy
- Tor-like anonymity
- public relay marketplace
- OpenZiti as VPN transport
- Yggdrasil-first architecture
