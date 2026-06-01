# Stage 1 Implementation Plan

## Status

Planned

## Goal

Create a minimal Android VPN shell with dummy runtime.

Stage 1 validates Android application foundation, VpnService lifecycle, foreground notification behavior, diagnostics, and state management.

## Non-Goals

Stage 1 must not include:

- sing-box
- libbox
- Xray
- VLESS Reality
- Hysteria2
- WireGuard
- mesh networking
- OpenZiti
- Yggdrasil
- adaptive routing
- production config import/export

## Minimal Android Structure

android/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  gradle/wrapper/
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/net/aegisnet/app/
        MainActivity.kt
        ui/
        vpn/
        runtime/
        diagnostics/

## Stage 1 Commits

1. Scaffold minimal Kotlin Compose project.
2. Add Compose connection shell.
3. Add VPN state and diagnostics models.
4. Add dummy runtime abstraction.
5. Add VpnService consent and skeleton lifecycle.
6. Add foreground notification plumbing.
7. Wire diagnostics screen to lifecycle events.

## Validation

Each commit must pass:

cd android
.\gradlew.bat :app:assembleDebug

Later commits should also pass:

.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest

## Device Validation

After VPN service commits:

- install debug APK
- verify VPN consent dialog
- verify Connect / Disconnect state changes
- verify foreground notification
- verify service stop
- verify revoke handling from Android VPN settings