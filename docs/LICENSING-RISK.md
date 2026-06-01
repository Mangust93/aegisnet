# Licensing Risk

## Core Risk

sing-box is GPLv3-or-later.

Embedding sing-box/libbox directly into an Android APK may require the whole distributed application to be GPLv3-compatible.

## Project Position

AegisNet must not treat libbox as a harmless SDK.

Before integration, the project must decide one of:

- accept GPLv3-compatible open-source distribution
- avoid distributing sing-box/libbox
- use user-supplied runtime
- obtain separate licensing
- use an alternative runtime

## Current Decision

No sing-box/libbox dependency in Stage 1.

Stage 1 will implement Android VPN lifecycle and dummy runtime abstraction only.

## Consequence

This keeps product architecture independent while licensing and runtime feasibility are reviewed.
