# Stage 4.2 SFA Libbox Runtime

## Goal

Use the official SFA Android libbox runtime shape for the experimental Real Proxy Runtime mode without committing GPL binaries or pretending runtime startup succeeds.

This stage targets the SagerNet/sing-box release artifact:

```text
SFA-1.13.13-universal.apk
Release source: https://github.com/SagerNet/sing-box/releases
Expected asset name: SFA-1.13.13-universal.apk
```

Record the downloaded APK SHA-256 before using it locally.

## Local Artifact Convention

All extracted runtime files are local-only and ignored by git:

```text
android/local-libs/sfa-libbox/
android/local-libs/sfa-libbox/jniLibs/arm64-v8a/libbox.so
android/local-libs/sfa-libbox/jniLibs/armeabi-v7a/libbox.so
android/local-libs/sfa-libbox/jniLibs/x86/libbox.so
android/local-libs/sfa-libbox/jniLibs/x86_64/libbox.so
android/local-libs/sfa-libbox/java/io/nekohasekai/libbox/*.java
android/local-libs/sfa-libbox/java/go/*.java
```

If you extract binding bytecode instead of source, place the local jar here:

```text
android/local-libs/sfa-libbox/classes.jar
```

The Android build consumes `sfa-libbox/jniLibs` when present. For Java bindings, use exactly one local form:

- preferred: `sfa-libbox/classes.jar`
- fallback: `sfa-libbox/java`

When `classes.jar` exists, Gradle does not compile `sfa-libbox/java`, avoiding duplicate binding classes. Default builds continue without these files.

## Required Extracted Native Libraries

The official APK contains these required files:

```text
lib/arm64-v8a/libbox.so
lib/armeabi-v7a/libbox.so
lib/x86/libbox.so
lib/x86_64/libbox.so
```

Copy them into the matching `jniLibs` ABI directories.

## Required Binding Classes

The runtime bridge requires these classes at app runtime:

```text
io.nekohasekai.libbox.Libbox
io.nekohasekai.libbox.SetupOptions
io.nekohasekai.libbox.CommandClient
go.Seq
```

The Stage 4.2 bridge calls:

```text
Libbox.setup(SetupOptions)
Libbox.newStandaloneCommandClient()
CommandClient.connectWithFD(int fd)
CommandClient.disconnect()
CommandClient.serviceClose()
```

`SetupOptions` must expose writable `basePath`, `workingPath`, and `tempPath` values through public fields or JavaBean setters.

## Manual Copy Commands

From Windows PowerShell, assuming the APK is in `C:\tmp\SFA-1.13.13-universal.apk`:

```powershell
cd C:\Users\melch\aegisnet
New-Item -ItemType Directory -Force android\local-libs\sfa-libbox\jniLibs\arm64-v8a
New-Item -ItemType Directory -Force android\local-libs\sfa-libbox\jniLibs\armeabi-v7a
New-Item -ItemType Directory -Force android\local-libs\sfa-libbox\jniLibs\x86
New-Item -ItemType Directory -Force android\local-libs\sfa-libbox\jniLibs\x86_64
New-Item -ItemType Directory -Force C:\tmp\sfa-apk
Copy-Item C:\tmp\SFA-1.13.13-universal.apk C:\tmp\sfa-apk\SFA-1.13.13-universal.zip
Expand-Archive -Force C:\tmp\sfa-apk\SFA-1.13.13-universal.zip C:\tmp\sfa-apk\expanded
Copy-Item C:\tmp\sfa-apk\expanded\lib\arm64-v8a\libbox.so android\local-libs\sfa-libbox\jniLibs\arm64-v8a\libbox.so
Copy-Item C:\tmp\sfa-apk\expanded\lib\armeabi-v7a\libbox.so android\local-libs\sfa-libbox\jniLibs\armeabi-v7a\libbox.so
Copy-Item C:\tmp\sfa-apk\expanded\lib\x86\libbox.so android\local-libs\sfa-libbox\jniLibs\x86\libbox.so
Copy-Item C:\tmp\sfa-apk\expanded\lib\x86_64\libbox.so android\local-libs\sfa-libbox\jniLibs\x86_64\libbox.so
```

Java binding extraction requires a DEX decompiler or dex-to-jar tool. With jadx installed locally:

```powershell
cd C:\Users\melch\aegisnet
New-Item -ItemType Directory -Force C:\tmp\sfa-apk\jadx
jadx -d C:\tmp\sfa-apk\jadx C:\tmp\SFA-1.13.13-universal.apk
New-Item -ItemType Directory -Force android\local-libs\sfa-libbox\java\io\nekohasekai
New-Item -ItemType Directory -Force android\local-libs\sfa-libbox\java\go
Copy-Item -Recurse C:\tmp\sfa-apk\jadx\sources\io\nekohasekai\libbox android\local-libs\sfa-libbox\java\io\nekohasekai\libbox
Copy-Item -Recurse C:\tmp\sfa-apk\jadx\sources\go\Seq.java android\local-libs\sfa-libbox\java\go\Seq.java
```

Prefer a vetted local `classes.jar` from the APK DEX when possible. If decompiled Java is used as a fallback, do not keep `classes.jar` in the same directory unless you intend Gradle to ignore the source copy. Place the jar at:

```powershell
android\local-libs\sfa-libbox\classes.jar
```

## What AegisNet Does

Real Proxy Runtime mode now:

- creates the Android `VpnService` TUN fd in `AegisVpnService`
- writes the imported raw sing-box JSON to `filesDir/sfa-libbox/working/config.json`
- initializes SFA libbox with `SetupOptions(basePath, workingPath, tempPath)`
- creates `Libbox.newStandaloneCommandClient()`
- calls `CommandClient.connectWithFD(tunFd)`
- calls `disconnect()` and `serviceClose()` on stop when those methods exist
- reports missing classes, setup failures, and native connection failures in Diagnostics

VLESS and Hysteria2 URI imports are still stored but not converted to sing-box JSON in this stage.

## GPLv3 Warning

sing-box and SFA libbox artifacts are GPLv3-derived. Do not distribute an APK containing these local runtime files until licensing review records:

- exact upstream URL, release, and APK checksum
- included native ABIs
- Java binding extraction method
- full GPLv3 notices and source-offer obligations
- app store and commercial distribution compatibility

## Current Blockers

The repository does not commit the official APK, extracted native libraries, extracted Java bindings, or generated `classes.jar`.

If decompiled Java bindings cannot compile directly, the blocker is the DEX-to-source conversion quality, not the AegisNet bridge. Use a local `classes.jar` or create minimal source stubs that exactly preserve the official method names and native signatures listed above.

Successful `connectWithFD` means the official native binding accepted the TUN fd. It does not by itself prove proxy traffic works; that requires phone validation.

## Phone-Test Checklist

1. Place SFA native libs and Java bindings under `android/local-libs/sfa-libbox`.
2. Build and install a debug APK.
3. Import a known-good raw sing-box JSON config.
4. Select `Real Proxy Runtime Experimental`.
5. Start VPN and grant Android VPN consent.
6. Confirm Diagnostics show `sfa_libbox_runtime_connected`.
7. Confirm Android shows the AegisNet VPN as active.
8. Visit an IP-check endpoint and compare expected proxy egress.
9. Run DNS leak checks.
10. Stop VPN and confirm Diagnostics show command client stop methods.
11. Reconnect twice to catch stale fd or service lifecycle issues.
12. Test airplane mode, network switch, and app process kill recovery.
