# Stage 2.3 Networking Lab

## Status

Implemented.

Sprint 2.3 makes AegisNet visibly useful as a VPN and network engineering lab while preserving the Stage 1, Stage 2.1, and Stage 2.2 safety boundaries.

## Scope Completed

- Added a Networking Lab section with cards for:
  - Internet Reachability Test
  - DNS Resolve Test
  - TCP Connect Test
  - Protect Experiment
- Added latest result display for each test:
  - Not run
  - Running
  - Passed
  - Failed
  - last reason
  - duration or latency when available
- Added Device / Network information:
  - Android SDK version
  - safely available network type
  - app package and version
  - VPN state
  - runtime state
- Kept diagnostics history with latest events, Clear, and Copy diagnostics.

## Diagnostic Tests Only

These tests are diagnostic probes. They do not make AegisNet a production VPN tunnel.

Preserved boundaries:

- one `:app` module
- no sing-box or libbox
- no real proxy protocols
- no packet forwarding
- no VPN routes
- no DNS configuration changes
- no production tunneling
- existing Connect / Disconnect behavior preserved
- foreground notification behavior preserved
- existing diagnostics preserved

## Test Behavior

### Internet Reachability Test

Attempts a short-timeout TCP connection to a conservative public IP endpoint.

This checks whether the app process can reach the internet through the device's normal networking stack. It does not read VPN packets, forward traffic, configure routes, configure DNS, or start a proxy runtime.

### DNS Resolve Test

Resolves a conservative hostname with the normal Android/JVM resolver.

This does not configure VPN DNS, change system DNS settings, claim DNS leak safety, or force DNS through AegisNet.

### TCP Connect Test

Attempts a short-timeout TCP connection to a conservative configurable host and port.

This validates ordinary app-level socket behavior only. It does not connect through a VPN tunnel or proxy protocol.

### Protect Experiment

Keeps the existing no-connect protect-before-connect experiment.

The dummy socket is created, `VpnService.protect()` is attempted while the VPN service is active, and the socket is closed during cleanup. No `connect()` call is made by this experiment.

## Diagnostics

Each networking test emits:

- start event
- success or failure event
- cleanup event

Diagnostics remain intentionally factual. They must not claim traffic forwarding, route safety, DNS safety, proxying, or production tunneling.

## Phone-Test Checklist

- Start app on a physical Android device.
- Confirm Device / Network shows Android SDK, network type, app package/version, VPN state, and runtime state.
- Run Internet Reachability Test while disconnected.
- Confirm result changes from Running to Passed or Failed with a reason and duration.
- Run DNS Resolve Test while disconnected.
- Confirm result changes from Running to Passed or Failed with a reason and duration.
- Run TCP Connect Test while disconnected.
- Confirm result changes from Running to Passed or Failed with a reason and duration.
- Tap Connect and grant VPN consent if requested.
- Confirm VPN state reaches Connected.
- Confirm foreground notification appears.
- Confirm runtime state reaches Running.
- Run the three networking tests while connected.
- Confirm diagnostics show start, success/failure, and cleanup events for each test.
- Run Protect Experiment while connected.
- Confirm Protect Experiment still skips socket connect and reports cleanup.
- Confirm diagnostics do not claim tunneling, forwarding, proxying, route safety, or DNS safety.
- Use Copy diagnostics and verify text is placed on clipboard.
- Tap Clear and confirm diagnostics are emptied.
- Tap Disconnect.
- Confirm foreground notification disappears.
- Confirm VPN state returns Disconnected and runtime state returns Not started.
