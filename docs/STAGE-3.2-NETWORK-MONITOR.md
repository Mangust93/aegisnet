# Stage 3.2: Network Monitor

## Scope

Stage 3.2 adds a Network Monitor section to the existing Android `:app` module. It provides a single Run Full Network Check action that executes real diagnostics from the app process.

## Checks

The full network check runs:

- Public IP check over HTTPS using `https://api.ipify.org`
- DNS resolve check for `example.com`
- HTTPS reachability check for `https://example.com`
- Active network type read from Android `ConnectivityManager`
- VPN active status read from the app's current VPN lifecycle state

## User-visible results

The Network Monitor shows:

- Latest public IP
- Network type
- VPN active status
- Overall check status
- DNS resolve status
- HTTPS reachability status
- Duration/latency
- Last error
- In-memory history of recent checks

History is intentionally in memory for this stage and resets when the process is recreated.

## Diagnostics

The app emits diagnostics for every full-check lifecycle event and each individual check:

- full check started/completed
- public IP check started/succeeded/failed
- DNS resolve check started/succeeded/failed
- HTTPS reachability check started/succeeded/failed

## Non-goals

This stage does not add a remote VPN server, proxy protocols, packet forwarding, sing-box/libbox, or VPN/proxy tunneling. The Network Monitor performs direct diagnostics only and does not carry user traffic.
