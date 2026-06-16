# Stage 4.3 Libbox AAR Runtime

## Goal

Make `libbox.aar` the canonical embedded runtime artifact for the experimental Real Proxy Runtime path.

The local artifact path is:

```text
android/local-libs/libbox/libbox.aar
```

The repository must not commit the AAR or any extracted binary derivative. That includes `.aar`, `.so`, `.apk`, and `classes.jar` files.

## Build Behavior

Gradle checks for `android/local-libs/libbox/libbox.aar` during configuration.

- If the AAR exists, `:app` adds it as a local `implementation(files(...))` dependency.
- If the AAR is missing, `:app` does not add a libbox dependency and normal compile, lint, unit test, and debug APK builds still pass.

The Android build no longer treats `android/local-libs/sfa-libbox/java`, `android/local-libs/sfa-libbox/classes.jar`, or `android/local-libs/sfa-libbox/jniLibs` as primary inputs. This avoids duplicate binding classes and split source/native conventions.

## Runtime Behavior

`ExperimentalSingBoxRuntime` still uses `SfaLibboxRuntimeBridge` as the reflective adapter for the SFA-shaped libbox API:

```text
io.nekohasekai.libbox.Libbox
io.nekohasekai.libbox.SetupOptions
io.nekohasekai.libbox.CommandClient
go.Seq
```

When those classes are unavailable, the runtime fails startup and Diagnostics emits exactly:

```text
libbox.aar missing at android/local-libs/libbox/libbox.aar
```

The runtime must not report `Running` unless the bridge creates a command client and `connectWithFD(tunFd)` succeeds. Failed startup closes rejected duplicated TUN file descriptors through the existing lifecycle hardening path.

## Source Options

Use one of these sources for `libbox.aar`:

1. CI-built AAR from a pinned sing-box tag or commit.
2. Trusted prebuilt AAR with a recorded `sha256` checksum.

A checksum file may be committed later, but this stage does not require it.

## SFA APK Extraction

SFA APK extraction was exploratory. It proved the expected Android libbox API shape and native TUN fd lifecycle, but it is no longer the primary project workflow.

Do not add decompiled Java sources, extracted `.so` files, APK assets, or generated `classes.jar` files as project inputs. Keep any such files local-only while investigating compatibility.

## GPLv3 Warning

sing-box and libbox are GPLv3-derived. Distributing an APK that embeds `libbox.aar` can create GPLv3 source disclosure, notice, distribution, and app-store compatibility obligations.

Before distributing any APK containing libbox, record the exact upstream source, tag or commit, build method, artifact checksum, included ABIs, license notices, and source-offer plan.
