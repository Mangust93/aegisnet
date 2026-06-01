# Runtime Strategy

## Principle

AegisNet must separate Android product logic from network runtime implementation.

## Target Abstraction

Android UI
↓
VPN Controller
↓
Runtime Interface
↓
Runtime Implementation

## Stage 1 Runtime

Stage 1 uses dummy runtime only.

The dummy runtime should support:

- start
- stop
- state reporting
- error reporting
- diagnostics events

## Future Runtime Candidates

- sing-box/libbox
- standalone sing-box process
- user-supplied runtime
- alternative runtime
- custom permissive runtime

## Rules

- UI must not depend directly on one runtime.
- VpnService lifecycle must be validated before real protocols.
- Real protocol integration must be isolated behind runtime abstraction.
- Mesh and relay systems are separate future subsystems.