# Android VPN Networking Experiments

## Purpose

This document defines safe Android VPN networking experiments for Stage 2.

The experiments are intended to validate mechanics and risk boundaries only. They are not production tunneling tests.

## Global Rules

- Use dummy or synthetic traffic only.
- Do not forward packets from the VPN interface.
- Do not add sing-box, libbox, Xray, VLESS, Reality, Hysteria2, or any other proxy runtime.
- Do not add dependencies or change Gradle configuration.
- Treat every route, DNS, and IPv6 result as device- and network-specific until validated on real hardware.
- Record setup, expected behavior, observed behavior, failure behavior, and cleanup behavior.

## Experiment 1: VpnService.protect() Call Ordering

Goal:
Validate the required ordering for protecting an upstream socket from being captured by the VPN.

Planned setup:

- create a controlled dummy socket only if it can be done without packet forwarding or production traffic tunneling
- call VpnService.protect() before connect
- report the protect() result through diagnostics
- close the socket during cleanup

Expected result:

- protect() succeeds while the service is active
- diagnostics show the socket protection attempt and result
- cleanup closes the socket and leaves the VPN lifecycle stable

Failure cases to document:

- protect() returns false
- socket creation fails
- service is revoked before protection
- disconnect occurs during the experiment

Non-goals:

- no proxy connection
- no packet forwarding
- no production traffic through the socket

## Experiment 2: Protected Dummy Socket Cleanup

Goal:
Validate cleanup behavior for a protected dummy socket across lifecycle transitions.

Planned transitions:

- connect then disconnect
- connect then revoke
- connect then simulated experiment error
- repeated connect and disconnect

Expected result:

- every transition closes the dummy socket
- every transition closes the VPN interface fd
- foreground notification cleanup remains correct
- diagnostics include cleanup events

## Experiment 3: Route Configuration Matrix

Goal:
Plan safe route experiments before any traffic forwarding runtime exists.

Route cases:

- no route: lifecycle-only baseline
- limited route: narrow synthetic destination planning only
- full route: planning only until packet forwarding and leak checks exist
- per-app allow-list: planning for package-level inclusion
- per-app deny-list: planning for package-level exclusion

Risks to document:

- blackholed traffic when routes exist without forwarding
- loop risk if AegisNet sockets are not protected
- false confidence from testing only one network type
- allow-list and deny-list mode incompatibility

Required evidence:

- route configuration intent
- expected device behavior
- rollback behavior
- observed connectivity impact
- diagnostics emitted during setup and cleanup

## Experiment 4: DNS Handling Plan

Goal:
Define how DNS will be handled before any production tunnel is claimed.

DNS cases:

- no VPN DNS configured
- explicit VPN DNS configured for experiment
- Android Private DNS enabled
- Android Private DNS disabled
- app-level DoH or DoT present in a client app

Questions to answer:

- Does DNS follow the configured VPN DNS during the experiment?
- Does Private DNS override, bypass, or fail under the experiment conditions?
- Which diagnostics are needed to show configured DNS state?
- Which checks are required before declaring DNS leak safety?

Required evidence:

- configured DNS values
- Private DNS device setting
- network type
- observed DNS behavior
- documented limitations

## Experiment 5: IPv6 Handling Plan

Goal:
Prevent silent IPv6 leaks or false production claims.

IPv6 cases:

- IPv4-only network
- IPv6-capable network
- IPv6 unsupported and intentionally blocked
- IPv6 explicitly configured with address, route, and DNS

Questions to answer:

- Is IPv6 intentionally unsupported for an experiment?
- If unsupported, how is IPv6 blocked or excluded?
- If supported, are IPv6 address, route, and DNS configured together?
- How are IPv6 decisions shown in diagnostics?

Required evidence:

- network IPv6 capability
- configured IPv6 behavior
- observed IPv6 connectivity or blocking
- leak-check result

## Experiment 6: Leak and Loop Safety Review

Goal:
Review every experiment for conditions that could leak traffic or route runtime traffic into the VPN itself.

Checklist:

- upstream socket protected before connect
- no unprotected runtime socket created
- no packet forwarding enabled
- route scope documented
- DNS scope documented
- IPv6 support or block decision documented
- cleanup closes sockets and VPN fd
- onRevoke() cleanup documented
- repeated start and stop remains stable
- diagnostics identify failures clearly

## Device Validation Notes

Every completed experiment should record:

- Android version
- target SDK behavior if relevant
- device model
- network type
- Private DNS setting
- IPv6 availability
- per-app routing mode, if used
- observed diagnostics
- cleanup result

## Production Gate

No Stage 2 experiment authorizes production VPN traffic handling.

Before production tunneling work, the project must have explicit decisions for:

- protected upstream socket policy
- route policy
- DNS policy
- IPv6 policy
- packet forwarding runtime boundary
- protocol runtime boundary
