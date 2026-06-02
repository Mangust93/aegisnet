# Stage 2.1 Protect Experiment Specification

## Status

Commit 1 documentation only.

This document defines the Stage 2.1 `VpnService.protect()` experiment before any Android networking code exists.

## Purpose

Stage 2.1 validates the design requirements for protecting an upstream socket from being captured by AegisNet's own VPN interface.

The experiment is limited to a controlled dummy socket lifecycle and diagnostic reporting. It does not authorize production VPN traffic handling.

## Explicit Non-Goals

Stage 2.1 includes:

- no packet forwarding
- no production tunneling
- no DNS changes
- no route changes
- no real proxy runtime
- no sing-box or libbox integration
- no Xray, VLESS, Reality, or Hysteria2 integration
- no dependency or Gradle changes
- no protocol profile generation
- no production traffic through the dummy socket

## Protect Ordering Requirements

Every upstream socket that may connect outside the VPN must be protected before it connects.

Required order:

1. Create the dummy socket while the VPN service is active.
2. Call `VpnService.protect()` on the socket before any connect attempt.
3. Record the protection attempt and result through diagnostics.
4. Only after a successful protection result may any future experiment consider connecting the socket.
5. Close the socket during cleanup.

The Stage 2.1 Commit 1 document does not implement the connect step. The ordering requirement is documented now so later experiments do not accidentally introduce an unprotected connect path.

## No-Connect-First Rule

AegisNet must never connect an upstream socket and then call `protect()`.

Calling `protect()` after connect is invalid for this experiment because the socket may already be routed through the VPN, which can cause loop behavior or false validation results.

If future experiment code cannot guarantee protect-before-connect ordering, the experiment must fail closed, emit diagnostics, close the socket, and avoid any network connection.

## Dummy Socket Lifecycle

The dummy socket exists only to validate protection mechanics and cleanup behavior.

Planned lifecycle:

1. Allocate the dummy socket for the active experiment instance.
2. Protect the socket before connect.
3. Emit diagnostics for allocation and protection result.
4. Keep ownership local to the experiment controller.
5. Close the socket on disconnect, revoke, setup failure, protection failure, or experiment cancellation.
6. Clear any stored socket reference after close.

The dummy socket must not be reused across VPN sessions. Each experiment run owns its own socket and cleanup result.

## Cleanup Rules

Cleanup must be idempotent and safe to call from normal stop, error handling, and `onRevoke()`.

Cleanup requirements:

- close the dummy socket if it was allocated
- tolerate closing an already-closed socket
- clear the dummy socket reference after close
- close the VPN interface fd through the existing VPN lifecycle cleanup path
- emit diagnostics for cleanup start and cleanup result
- avoid starting replacement sockets during cleanup
- avoid retry loops after revoke or explicit disconnect

Cleanup must run after:

- successful protection experiment completion
- `protect()` returning false
- socket allocation failure
- disconnect during experiment setup
- service revoke during experiment setup
- unexpected experiment error
- repeated start/stop validation

## Diagnostics Events

Diagnostics must be structured enough to prove ordering, result, and cleanup.

Required events:

- `protect_experiment_started`
- `dummy_socket_create_started`
- `dummy_socket_create_succeeded`
- `dummy_socket_create_failed`
- `socket_protect_started`
- `socket_protect_succeeded`
- `socket_protect_failed`
- `protect_experiment_failed`
- `protect_experiment_completed`
- `protect_experiment_cleanup_started`
- `protect_experiment_cleanup_completed`
- `protect_experiment_cleanup_failed`
- `protect_experiment_revoked`
- `protect_experiment_disconnected`

Each event should include the experiment run identifier, VPN lifecycle state, and a concise reason when failure or cancellation occurs.

Diagnostics must not claim that traffic was tunneled, forwarded, proxied, DNS-protected, or route-safe.

## Failure Handling

Failure handling must prefer stopping the experiment over continuing with uncertain network state.

Failure rules:

- If socket creation fails, emit failure diagnostics and skip protection.
- If `protect()` returns false, close the socket and fail the experiment.
- If service revoke occurs before or during protection, close the socket and report revoked state.
- If disconnect occurs before or during protection, close the socket and report disconnected state.
- If cleanup fails, emit cleanup failure diagnostics and leave the VPN lifecycle in its normal stopped path.
- If ordering cannot be proven, treat the run as failed.

No failure path may attempt packet forwarding, production tunneling, DNS modification, route modification, or proxy startup.

## Device Validation Checklist

Each device validation run must record:

- Android version
- device model
- target SDK behavior if relevant
- network type
- VPN consent state
- service active state before protection
- result of socket allocation
- result of `protect()`
- whether disconnect was requested during the experiment
- whether `onRevoke()` occurred during the experiment
- cleanup result
- observed diagnostics events
- repeated start/stop result

Validation must be performed on a real Android device before the result is used as evidence for later networking stages.

## Rollback Expectations

Stage 2.1 must be reversible.

Rollback requirements:

- removing the experiment must leave the existing Stage 1 VPN lifecycle behavior intact
- no route state may need rollback because Stage 2.1 performs no route changes
- no DNS state may need rollback because Stage 2.1 performs no DNS changes
- no proxy runtime may need rollback because Stage 2.1 starts no real proxy runtime
- disconnect and revoke must close the dummy socket and VPN interface fd
- diagnostics must clearly show whether cleanup completed

If rollback cannot be verified on device, Stage 2.1 remains incomplete.

## Production Gate

Completing Stage 2.1 only documents and later may validate protect-before-connect behavior for a dummy socket.

It does not prove packet forwarding safety, DNS safety, route safety, IPv6 safety, proxy runtime correctness, or production VPN readiness.
