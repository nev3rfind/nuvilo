# ADR 0001: Native Kotlin and Jetpack Compose

- **Status**: Accepted
- **Date**: 2026-10-29
- **Deciders**: Senior Android Lead, Product Architect

## Context
Novelo is a personal medication tracking application requiring high reliability, battery efficiency, precise system alarm triggers, and modern accessible user interfaces. We needed to choose the UI framework and core programming language.

## Decision
We will build Novelo purely in **idiomatic Kotlin with Jetpack Compose and Material 3**, targeting modern Android (minSdk 24, compileSdk/targetSdk 37).

## Alternatives Considered
1. **Kotlin with Traditional XML Views**: Well understood and mature, but significantly more boilerplate, more prone to UI state desynchronization, and harder to maintain with complex dynamic lists and stateful schedules.
2. **Cross-Platform Frameworks (Flutter, React Native)**: High cross-platform reuse, but adds large runtime overhead, complicates deep Android platform integration (exact alarms, power management exemptions, Doze handling, wake locks, biometric authentication), and introduces third-party dependency vulnerabilities.

## Consequences
- **Positive**: Clean declarative UI with Unidirectional Data Flow; concise code; native performance; first-class accessibility semantics; direct integration with Android platform APIs (`AlarmManager`, `NotificationManager`, `BiometricPrompt`).
- **Negative**: Requires careful handling of Compose recomposition stability; preview tooling requires modern JDK.
