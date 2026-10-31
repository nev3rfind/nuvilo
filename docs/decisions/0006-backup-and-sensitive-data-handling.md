# ADR 0006: Backup and Sensitive Health Data Handling

- **Status**: Accepted
- **Date**: 2026-10-29
- **Deciders**: Privacy Engineer, Security Lead

## Context
Medication history, dosages, and prescribing physician details are classified as Special Category / Protected Health Information. Default Android behavior includes application databases in automated Google Drive cloud backups, creating potential exposure if a user's cloud account is compromised or subject to lawful intercept. Conversely, users legitimately need to transfer their data when upgrading devices.

## Decision
1. **Cloud Backup Exclusions**:
   - `data_extraction_rules.xml` and `backup_rules.xml` will explicitly exclude `novelo.db` and preferences from automated cloud backups.
   - Device-to-device cable transfers (`<device-transfer>`) remain permitted to support phone migrations.
2. **User-Initiated Local Export / Import**:
   - Users can generate a complete data export at any time.
   - Exports use the Android Storage Access Framework (`Intent.ACTION_CREATE_DOCUMENT`), saving directly to a user-chosen storage URI (local download folder, SD card, or USB drive).
   - Export format: structured JSON with a schema version header, plus an optional CSV export for sharing with doctors.
   - Import requires explicit confirmation and displays a summary of records to be imported before altering the local database.
3. **App Lock Authentication**:
   - Gated via `BiometricPrompt` with fallback to device credentials (PIN/pattern/password).
   - Does not attempt custom software PIN encryption that could lock users out if forgotten; relies on Android Keystore and biometric security.

## Alternatives Considered
1. **Default Android Cloud Backup**: Extremely convenient, but silently uploads plaintext health records to third-party cloud servers without explicit user informed consent.
2. **Proprietary Cloud Sync**: Defeats the zero-network offline-first architectural commitment of Novelo.

## Consequences
- **Positive**: Strict health data sovereignty; zero accidental cloud data leaks; complete compliance with user privacy expectations.
- **Negative**: Users must manually perform an export if they want off-device disaster recovery.
