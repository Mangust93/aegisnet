# Stage 2 Safe Networking Plan

## Status

Planned.

Stage 2 is a documentation and experiment-design stage for safe Android VPN networking mechanics.

## Purpose

Stage 2 researches and validates the Android VPN networking behaviors that must be understood before AegisNet integrates any real proxy runtime, packet forwarding loop, or production traffic tunneling path.

The goal is to reduce VPN correctness risk while preserving the Stage 1 runtime boundary.

## Scope

Stage 2 covers planning and controlled validation for:

- VpnService.protect() behavior
- controlled dummy socket protection testing, if the test can be performed without production traffic tunneling
- route experiment planning
- DNS handling planning
- IPv6 handling planning
- leak and loop safety checks
- diagnostics needed to evaluate the experiments

## Explicit Non-Scope

Stage 2 must not include:

- production traffic tunneling
- sing-box or libbox
- Xray, VLESS, Reality, or Hysteria2
- packet forwarding runtime
- protocol profile generation
- proxy runtime lifecycle integration
- dependency additions
- Gradle changes

## Safety Principles

- Experiments must be reversible and limited to synthetic or dummy traffic.
- No experiment may claim production VPN safety.
- No upstream socket may be used without explicit VpnService.protect() handling.
- No route or DNS experiment may be treated as leak-safe until verified on a real Android device.
- IPv6 must be either explicitly supported in the experiment or explicitly excluded and blocked by design.
- Each experiment must document expected behavior, observed behavior, cleanup behavior, and failure handling.

## Experiment Tracks

### Socket Protection

Research questions:

- Can AegisNet reliably call VpnService.protect() before a socket is connected?
- How should failures be reported through diagnostics?
- What happens when protect() is called after service revocation or tunnel teardown?
- Can a dummy socket protection test validate call ordering without forwarding production traffic?

Acceptance evidence:

- documented test setup
- documented expected and observed protect() results
- diagnostic event requirements
- cleanup behavior after connect, disconnect, error, and revoke

### Route Planning

Research questions:

- Which route configurations are safe to test before packet forwarding exists?
- How should full-tunnel, split-tunnel, and per-app routing experiments be staged?
- Which configurations risk blackholing traffic, leaking traffic, or trapping AegisNet runtime sockets in the VPN?

Acceptance evidence:

- route matrix covering no-route, limited-route, and full-route experiments
- explicit rollback behavior for every route experiment
- interaction notes for Android per-app allow-list and deny-list modes

### DNS Planning

Research questions:

- Which DNS servers should be configured for experiments?
- How should Android Private DNS interactions be tested?
- How should app-level DoH or DoT limitations be documented?
- Which checks are needed before claiming DNS leak safety?

Acceptance evidence:

- DNS experiment matrix
- Private DNS test notes
- leak-check checklist
- diagnostic requirements for configured DNS state

### IPv6 Planning

Research questions:

- Should early experiments block IPv6 or configure IPv6 explicitly?
- Which devices and networks are needed for IPv4-only and IPv6-capable validation?
- How should unsupported IPv6 be represented in diagnostics?

Acceptance evidence:

- IPv6 support decision for each experiment
- IPv4-only and IPv6-capable network test plan
- documented leak risks for unsupported IPv6

## Leak and Loop Safety Checklist

Before any Stage 2 experiment is considered complete, document whether it verifies:

- upstream sockets are protected before connect
- AegisNet runtime traffic is not routed back into its own VPN interface
- VPN interface fd is closed on stop, error, and revoke
- foreground service cleanup remains correct
- DNS behavior is explicit and observable
- IPv6 behavior is explicit and observable
- per-app routing mode is documented
- route rollback behavior is documented
- diagnostics are emitted for setup, success, failure, and cleanup

## Completion Criteria

Stage 2 is complete when the project has documentation that defines safe experiments, expected observations, risk boundaries, and acceptance evidence for Android VPN networking mechanics.

Stage 2 completion does not mean AegisNet has production VPN traffic handling.

## Stage 3 Gate

Real protocol runtime work may start only after Stage 2 documents:

- protected upstream socket requirements
- route behavior and rollback plan
- DNS handling plan
- IPv6 handling plan
- leak and loop safety checklist
- remaining unknowns and device validation gaps
