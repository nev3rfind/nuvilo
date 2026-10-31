# ADR 0002: Offline-First Local Storage Architecture

- **Status**: Accepted
- **Date**: 2026-10-29
- **Deciders**: Senior Android Lead, Privacy Engineer

## Context
Medication tracking involves highly sensitive personal health data (PHI). Users expect privacy, immediate responsiveness, and complete functionality regardless of network conditions (e.g., in hospitals, airplanes, or rural areas without mobile signal).

## Decision
Novelo is strictly **local-only and offline-first**. All data is stored locally on the device using **Room (SQLite)** for relational domain entities and **Jetpack DataStore** for user preferences. The application will not request `android.permission.INTERNET`, will have no cloud sync backend, and will include no third-party network SDKs.

## Alternatives Considered
1. **Cloud-First with Firebase/Supabase**: Allows multi-device sync, but requires user accounts, privacy policy disclosures, GDPR/HIPAA compliance overhead, cloud infrastructure costs, and risks data leakage or vendor lock-in.
2. **Hybrid Local Cache with Sync**: High synchronization complexity (conflict resolution between offline dose recordings on multiple devices), latency, and privacy compromises.

## Consequences
- **Positive**: Absolute user privacy; instantaneous zero-latency reads/writes; no server operating costs; immune to remote server outages or network drops; zero GDPR/HIPAA cloud liability.
- **Negative**: No automatic cloud multi-device sync; data recovery depends on local user backups via the Android Storage Access Framework.
