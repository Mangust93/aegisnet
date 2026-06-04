# Stage 3.0 Local App Firewall

## Status

Implemented.

Sprint 3.0 adds AegisNet's first practical local VPN feature: user-selected app blocking through Android `VpnService`.

## Boundaries

- One Android `:app` module.
- No sing-box or libbox.
- No proxy protocols.
- No remote server.
- No packet forwarding.
- No DNS proxy.
- Diagnostics mode remains available.
- Default mode remains Diagnostics and does not block traffic.

## Modes

### Diagnostics

Diagnostics mode preserves the existing visible VPN lab behavior. It establishes the local VPN interface used by the dummy runtime and diagnostics experiments, but it does not install a VPN default route and does not route other apps into a blocking firewall.

### App Firewall

App Firewall mode blocks only packages selected by the user.

The implementation uses `VpnService.Builder.addAllowedApplication(packageName)` for each selected app. Android's `VpnService` app-routing semantics are important:

- If no allowed/disallowed application list is configured, apps matching the VPN routes can use the VPN.
- If `addAllowedApplication()` is used, only those app UIDs are allowed to use the VPN.
- If `addDisallowedApplication()` is used, all apps except those UIDs are allowed to use the VPN.
- The allow-list and disallow-list models are mutually exclusive.

AegisNet uses the allow-list model because it is safer for a non-forwarding firewall:

- selected apps are explicitly allowed into the VPN;
- App Firewall mode adds the IPv4 default route `0.0.0.0/0`;
- the VPN service intentionally does not read packets from the TUN file descriptor and does not forward packets anywhere;
- selected app traffic enters the VPN and fails;
- unselected apps are not allowed into this VPN and should remain on the device's normal network path.

The service refuses to start App Firewall mode when no apps are selected. This prevents an empty allow-list/default-route configuration from accidentally affecting more traffic than intended.

## UI

The app now shows a mode selector at the top:

- Diagnostics
- App Firewall

App Firewall mode shows:

- installed launchable apps;
- app name;
- package name;
- checkbox selection;
- search/filter field;
- selected count;
- warning that selected apps will lose internet while the firewall is active;
- visible Connect/Disconnect control;
- current mode;
- active blocked app count;
- last firewall result.

Selections are stored locally in `SharedPreferences` and restored after app restart.

## Diagnostics

The firewall emits these events:

- `app_firewall_mode_selected`
- `installed_apps_loaded`
- `firewall_selected_apps_updated`
- `firewall_vpn_started`
- `firewall_allowed_application_added`
- `firewall_route_added_ipv4_default`
- `firewall_no_packet_forwarding`
- `firewall_vpn_stopped`
- `firewall_error`

## Safety Notes

- Diagnostics mode must not block traffic.
- App Firewall mode clearly warns that selected apps will lose internet.
- Disconnect closes the VPN interface and restores normal connectivity.
- Apps that cannot be added to the VPN builder are reported with `firewall_error`.
- AegisNet does not claim anonymity, remote VPN transport, packet forwarding, proxying, or DNS protection.

## Manual Phone-Test Checklist

- Install and open the app on a physical Android phone.
- Confirm the default selected mode is Diagnostics.
- In Diagnostics mode, tap Connect and grant VPN consent if requested.
- Confirm VPN state reaches Connected and diagnostics lab remains available.
- Confirm ordinary device/app internet is not blocked by Diagnostics mode.
- Tap Disconnect and confirm VPN state returns Disconnected.
- Select App Firewall mode.
- Confirm the installed app list appears with app name, package name, checkbox, search field, selected count, and warning.
- Choose one app with an easy network check, for example a browser or messaging app.
- Confirm selected count increments and selection persists after leaving/reopening the app.
- Tap Connect and grant VPN consent if requested.
- Confirm current mode shows App Firewall.
- Confirm active blocked app count equals the number of packages accepted by the VPN builder.
- Confirm diagnostics include `firewall_allowed_application_added`, `firewall_route_added_ipv4_default`, `firewall_no_packet_forwarding`, and `firewall_vpn_started`.
- Open the selected app and verify its internet access fails while the firewall is active.
- Open an unselected app and verify it still has internet if the device honors the allow-list routing model.
- Return to AegisNet and tap Disconnect.
- Confirm diagnostics include `firewall_vpn_stopped`.
- Confirm the selected app's internet access is restored after disconnect.

