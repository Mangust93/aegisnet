# AegisNet

AegisNet is an Android-first adaptive VPN / overlay networking project.

## Goal

Build a practical Android VPN client focused on:

- high availability
- stable speed
- split tunneling / whitelist routing
- anti-DPI capable transports
- low client-side overhead
- future mesh/community relay expansion

## Current Architecture Decision

V1 is not a decentralized mesh network.

V1 focuses on a stable Android VPN core:

- Kotlin
- Jetpack Compose
- Android VpnService
- isolated network runtime
- sing-box feasibility research
- VLESS Reality / Hysteria2 profile support later
- DNS/IPv6 leak prevention from early stages

## AI Workflow

- ChatGPT: architecture, product/system analysis, roadmap
- Claude Code: main application implementation
- Codex: DevOps, CI, Gradle, Git, review, validation
- User: manual approval and device testing
