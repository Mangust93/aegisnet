# Stage 4.0 Real Runtime Experiment

## Chosen Runtime Approach

This branch adds an experimental `Real Proxy Runtime Experimental` mode beside the existing Diagnostics, Local App Firewall, and Network Monitor modes.

The first integration path is a guarded sing-box/libbox adapter:

- Imported configs are validated and stored locally in Android `SharedPreferences`.
- The VPN service establishes an Android `VpnService` TUN for real-runtime mode.
- The experimental adapter receives the active config and TUN file descriptor.
- The adapter looks for packaged libbox/sing-box Android classes by reflection.
- If a usable binding is not packaged, startup fails and records the exact blocker in runtime diagnostics.

No fake success state is used. The VPN reaches `Running` only if the selected runtime reports `RuntimeState.Running`.

## Supported Config Inputs

The import screen accepts:

- VLESS URI, including VLESS Reality URI format at the URI-validation level.
- Hysteria2 URI using `hysteria2://` or `hy2://`.
- Raw sing-box JSON object text.

Current runtime behavior:

- Raw sing-box JSON is the intended first runtime input once a vetted Android libbox binding is packaged.
- VLESS and Hysteria2 URI imports are stored, but runtime startup fails until URI-to-sing-box JSON conversion is implemented.

## GPL and License Risk

sing-box and Android libbox integration may introduce GPL or other copyleft obligations depending on the selected artifact, linking model, distribution method, and app store/commercial plan.

This branch intentionally documents the risk instead of resolving it:

- Do not ship this branch commercially without legal review.
- Prefer an explicitly versioned, auditable Android runtime artifact.
- Record the exact upstream source, license, build flags, and redistribution terms before enabling real startup.

## Build and Runtime Limitations

Known blockers:

- No libbox/sing-box Android artifact is currently packaged in this repository.
- The adapter does not yet map a concrete libbox API to start/stop calls because no binding is present to compile or test against.
- VLESS and Hysteria2 URI conversion into sing-box JSON is not implemented.
- Packet forwarding has not been phone-tested with a real provider.

Expected current result:

- Import and active-config UI works.
- Starting real runtime with URI config fails with a URI conversion blocker.
- Starting real runtime with raw JSON fails with a missing libbox/sing-box class blocker unless an Android binding is added.

## Test Configs Required

Phone validation needs:

- One known-good VLESS Reality config from a test server.
- One known-good Hysteria2 config from a test server.
- One known-good raw sing-box JSON config that works with the selected Android libbox binding.
- A network-monitor baseline before VPN start and after VPN start.
- Diagnostics export after each failed and successful runtime attempt.

Never use production personal credentials for first validation.

## Rollback

Rollback the experiment commits with:

```powershell
git revert <docs-commit> <runtime-commit> <import-commit>
```

If the commits were squashed, revert the single squashed commit:

```powershell
git revert <squashed-stage-4-commit>
```
