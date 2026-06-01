# VPN State Machine

## Purpose

AegisNet needs an explicit VPN state machine before real protocol integration.

## States

- Idle
- PreparingConsent
- StartingService
- EstablishingTunnel
- Running
- Stopping
- Revoked
- Error

## Normal Flow

Idle -> PreparingConsent -> StartingService -> EstablishingTunnel -> Running

## Stop Flow

Running -> Stopping -> Idle

## Revoke Flow

Running -> Revoked -> Idle

## Error Flow

Any active state may transition to Error.

After cleanup, Error may return to Idle.

## Rules

- Connect is accepted only from Idle or Error.
- Disconnect is accepted from any non-idle state.
- onRevoke() wins over UI state.
- Every error must close the VPN interface fd.
- Every stop must stop foreground notification.
- Every transition must emit a diagnostic event.