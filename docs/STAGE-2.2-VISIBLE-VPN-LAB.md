# Stage 2.2 Visible VPN Lab

## Status

Implemented.

Sprint 2.2 makes the current engineering VPN shell visibly useful while preserving the Stage 1 and Stage 2.1 safety boundaries.

## Scope Completed

- Added a Diagnostics Lab section with cards for:
  - Protect Experiment
  - VPN Lifecycle
  - Runtime Lifecycle
- Added latest Protect Experiment result display:
  - Not run
  - Passed
  - Failed
  - last reason
- Added Device / Session information:
  - VPN state
  - runtime state
  - connected session duration
  - foreground notification active state
  - Android SDK version
- Improved diagnostics history:
  - compact rows
  - level and source badges
  - latest 20 events
  - Clear remains available
- Added Copy diagnostics text.
- Added a user-friendly message when `protect()` returns false:
  - "Socket protection failed on this run. No connection was attempted."

## Safety Boundary Preserved

Sprint 2.2 does not add production VPN networking behavior.

Preserved boundaries:

- one `:app` module
- no sing-box or libbox
- no real proxy protocols
- no packet forwarding
- no DNS changes
- no route changes
- no production tunneling
- existing Connect / Disconnect behavior preserved
- foreground notification behavior preserved

## Device Observation

Phone test observation for the protect experiment:

- `protect()` currently returned false on the phone test.
- No `connect()` was attempted after the false result.
- No routing was configured.
- No DNS configuration was changed.
- No packet forwarding was performed.
- The dummy socket was closed during cleanup.

This result means the protect experiment failed closed on that run. It is not evidence of production VPN readiness.

## Phone-Test Checklist

Use this checklist for the next physical-device verification:

- Start app on a physical Android device.
- Tap Connect and grant VPN consent if requested.
- Confirm VPN state reaches Connected.
- Confirm foreground notification appears.
- Confirm Runtime Lifecycle shows Running.
- Confirm Device / Session shows session duration increasing.
- Run Protect Experiment while connected.
- Confirm latest Protect Experiment result is visible.
- If `protect()` returns false, confirm the UI says: "Socket protection failed on this run. No connection was attempted."
- Confirm diagnostics show protect-before-connect ordering and cleanup events.
- Confirm diagnostics do not claim traffic forwarding, DNS safety, routing, proxying, or tunneling.
- Use Copy diagnostics and verify text is placed on clipboard.
- Tap Clear and confirm diagnostics are emptied.
- Tap Disconnect.
- Confirm foreground notification disappears.
- Confirm VPN state returns Disconnected and runtime returns Not started.
- Repeat Connect / Disconnect once to confirm lifecycle stability.
