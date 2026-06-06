# Stage 3.1: App Firewall Profiles

## Scope

Stage 3.1 adds local App Firewall profiles to the existing Android `:app` module:

- Work
- Focus
- Sleep
- Custom

Each profile stores its own selected package set in local `SharedPreferences`. The active profile is also persisted locally, and App Firewall mode uses the active profile's selected packages when starting `AegisVpnService`.

## User-visible behavior

- App Firewall mode shows the active profile.
- App Firewall mode shows the selected app count for the active profile.
- Users can switch profiles before connecting.
- Users can select apps independently for each profile.
- Existing legacy App Firewall selections migrate into the Custom profile on first profile initialization.

## Diagnostics

The app emits diagnostics for:

- `firewall_profile_created`
- `firewall_profile_updated`
- `firewall_profile_switched`

Existing App Firewall diagnostics and behavior are preserved.

## Non-goals

This stage does not add a remote VPN server, proxy protocol support, packet forwarding, sing-box/libbox, or VPN/proxy tunneling. App Firewall remains a local Android `VpnService` control surface that applies selected-app routing and intentionally does not forward packets.
