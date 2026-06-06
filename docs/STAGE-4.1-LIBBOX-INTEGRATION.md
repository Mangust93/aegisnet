# Stage 4.1 Libbox Android Integration

## Goal

Make the exact path for real VLESS/Hysteria2 runtime work explicit without pretending runtime startup works before a vetted Android libbox artifact is present and wired.

Current app adapter:

- Runtime class: `net.aegisnet.app.runtime.ExperimentalSingBoxRuntime`
- Service caller: `net.aegisnet.app.vpn.AegisVpnService`
- Runtime contract: `net.aegisnet.app.runtime.NetworkRuntime`
- Config carrier: `net.aegisnet.app.runtime.RuntimeConfig`
- Expected class candidates currently probed by reflection:
  - `io.nekohasekai.libbox.Libbox`
  - `io.nekohasekai.libbox.BoxService`
  - `libbox.Libbox`

## Local Artifact Convention

Place a locally built or otherwise vetted Android libbox artifact at:

```text
android/local-libs/libbox.aar
```

The AAR is intentionally ignored by git. Do not commit the binary artifact unless there is an explicit licensing and provenance justification recorded in the repository.

The Android Gradle build adds this AAR only when the file exists. Normal CI and developer builds must continue to pass without it.

## Build Path: Windows Host With WSL

Use WSL for the Go/gomobile build steps and copy only the resulting AAR into the Android project.

1. Install WSL Ubuntu, Android Studio, and Android SDK/NDK on the Windows host.
2. In WSL, install Go and gomobile:

```bash
go install golang.org/x/mobile/cmd/gomobile@latest
go install golang.org/x/mobile/cmd/gobind@latest
gomobile init
```

3. Clone the selected sing-box/libbox source in WSL and checkout an explicit tag or commit.
4. Build the Android binding following that upstream source's current Android/libbox instructions. The exact command depends on the selected upstream layout, but it is normally a `gomobile bind` or project-provided Android build script that emits `libbox.aar`.
5. Copy the produced AAR into this repo:

```bash
mkdir -p /mnt/c/Users/melch/aegisnet/android/local-libs
cp /path/to/libbox.aar /mnt/c/Users/melch/aegisnet/android/local-libs/libbox.aar
```

6. Rebuild the Android app from Windows PowerShell:

```powershell
cd C:\Users\melch\aegisnet\android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

## Validation Commands

From `android/`, validate the no-artifact path:

```powershell
Test-Path .\local-libs\libbox.aar
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

`Test-Path` should return `False` for the default repository state.

After placing `android/local-libs/libbox.aar`, validate the optional artifact path:

```powershell
Test-Path .\local-libs\libbox.aar
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

`Test-Path` should return `True`. Build success only proves the artifact packages classes and native libraries cleanly; it does not prove runtime traffic works.

Phone/runtime validation still requires:

- A known-good raw sing-box JSON config compatible with the selected libbox binding.
- A known-good VLESS Reality config.
- A known-good Hysteria2 config.
- Adapter work that maps the packaged binding API to `ExperimentalSingBoxRuntime.start()` and `stop()`.
- Packet-flow validation through Android `VpnService.protect()` and app diagnostics.

## GPLv3 Warning

sing-box is GPLv3. Android libbox artifacts derived from sing-box may create GPLv3 source disclosure, license notice, distribution, and app-store compatibility obligations. Before distributing any build that includes `libbox.aar`, record:

- Upstream repository URL.
- Exact tag or commit SHA.
- Build commands and flags.
- Included native ABIs.
- Full license texts and notices.
- Whether the app distribution model is compatible with GPLv3 obligations.

Do not ship a binary with libbox until this review is complete.

## Next Implementation Step

Once the AAR is present and the app builds, inspect the packaged public API and replace the current reflection-only blocker with concrete start/stop calls in `ExperimentalSingBoxRuntime`. Keep the state machine strict: report `RuntimeState.Running` only after the libbox runtime has actually accepted the config and started on the supplied TUN file descriptor.
