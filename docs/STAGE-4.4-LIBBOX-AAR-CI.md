# Stage 4.4 Libbox AAR CI

## Goal

Build `libbox.aar` reproducibly from a selected `SagerNet/sing-box` source ref in GitHub Actions, publish it as a workflow artifact, and keep all binary runtime artifacts out of the repository.

The local Android artifact path remains:

```text
android/local-libs/libbox/libbox.aar
```

## Workflow

Workflow file:

```text
.github/workflows/build-libbox-aar.yml
```

The workflow is manually triggered with `workflow_dispatch`.

Input:

```text
singbox_ref
```

`singbox_ref` is the `SagerNet/sing-box` tag, branch, or commit to build. The default is:

```text
v1.13.13
```

The job checks out `SagerNet/sing-box` at that ref, configures Go, Java 17, Android SDK/NDK, installs pinned SagerNet gomobile tools, builds `libbox.aar`, writes `libbox.aar.sha256`, and uploads both files as the `libbox-aar` artifact.

## Run On GitHub

1. Open the repository on GitHub.
2. Select the `Actions` tab.
3. Select `Build libbox AAR`.
4. Select `Run workflow`.
5. Choose the branch containing `.github/workflows/build-libbox-aar.yml`.
6. Leave `singbox_ref` as `v1.13.13` or enter a reviewed sing-box tag, branch, or commit.
7. Select `Run workflow`.

## Download Artifact

1. Open the completed `Build libbox AAR` workflow run.
2. Scroll to `Artifacts`.
3. Download `libbox-aar`.
4. Extract the artifact zip locally.
5. Copy `libbox.aar` to:

```powershell
New-Item -ItemType Directory -Force android\local-libs\libbox
Copy-Item .\libbox.aar android\local-libs\libbox\libbox.aar
```

Keep `libbox.aar.sha256` with your local artifact notes or checksum records.

## Verify SHA-256 On Windows

From the directory containing the downloaded files:

```powershell
$expected = (Get-Content .\libbox.aar.sha256).Split(" ")[0]
$actual = (Get-FileHash .\libbox.aar -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) { throw "SHA-256 mismatch: expected $expected actual $actual" }
```

After copying into the Android project, verify the placed file:

```powershell
Get-FileHash android\local-libs\libbox\libbox.aar -Algorithm SHA256
```

## Binary Artifact Rule

Do not commit generated runtime binaries or extracted binary derivatives. This includes:

```text
.aar
.so
.apk
classes.jar
```

The repository `.gitignore` keeps the local `android/local-libs/libbox/libbox.aar` path ignored.

## GPLv3 Warning

sing-box and libbox are GPLv3-derived. Distributing an APK that embeds `libbox.aar` can create GPLv3 source disclosure, license notice, distribution, and app-store compatibility obligations.

Before distributing any APK containing libbox, record the exact upstream source ref, build workflow run, artifact checksum, included ABIs, license notices, and source-offer plan.
