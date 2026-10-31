# Novelo Security, Privacy & Threat Model

This document outlines the security posture, privacy guarantees, permission policies, and threat assumptions for **Novelo**.

---

## 1. Core Threat Model & Privacy Guarantees

### Threat Assumptions
- **User Device Shared with Family/Friends**: Casual shoulder surfing or physical device sharing where someone borrowing the phone might view private medical details.
- **Lock Screen Exposure**: Notifications displaying sensitive medication names (e.g. psychiatric medicines, HIV treatments, reproductive healthcare) visible to onlookers.
- **Malicious Cloud Backups**: Unintended exfiltration of sensitive health records via automated unencrypted cloud device backups.
- **Supply-Chain Network Leaks**: Third-party dependencies attempting to phone home or collect telemetry.

### Mitigations
1. **Zero-Network Architecture**: Complete architectural absence of internet connectivity.
2. **Notification Privacy Mode**: Configurable lock screen notification masking.
3. **Local App Lock**: Biometric and device-credential authentication gating UI access.
4. **Cloud Backup Exclusions**: Explicit opt-out in Android backup XML manifests.
5. **Dependency Minimization**: Strict prohibition of analytics, ad, or network libraries.

---

## 2. Permission Policy

Novelo strictly limits Android permissions to the bare functional minimum:

| Permission | API Level | Purpose | Justification |
| :--- | :--- | :--- | :--- |
| `android.permission.POST_NOTIFICATIONS` | 33+ | Medication reminder alerts | Required for scheduled dose notifications. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | All | Rescheduling alarms after reboot | Required so reminders continue functioning after phone restarts. |
| `android.permission.SCHEDULE_EXACT_ALARM` | 31+ | Time-critical exact alarm delivery | Required for accurate medication dosing intervals. |
| `android.permission.USE_EXACT_ALARM` | 33+ | Alarm clock & medication reminder apps | Official platform permission for critical medicine reminders. |
| `android.permission.VIBRATE` | All | Haptic notification alert | Alerts users in noisy environments. |
| `android.permission.USE_BIOMETRIC` | All | Optional app unlock | Gates access via biometric authentication. |

### Forbidden Permissions
- **`android.permission.INTERNET`**: **Strictly forbidden**.
- **`android.permission.ACCESS_NETWORK_STATE`**: Forbidden.
- **`android.permission.READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`**: Forbidden (all exports must use Android Storage Access Framework `ActivityResultContracts.CreateDocument`).
- **`android.permission.ACCESS_FINE_LOCATION` / `COARSE_LOCATION`**: Forbidden.

---

## 3. Data Storage, App Lock & Encryption Design

### Storage Architecture
- Primary data is stored in a private SQLite database managed by Room at `/data/data/com.example.nuvilo/databases/novelo.db`.
- All health entities—including prescriptions, pharmacy contacts, doctor details, dose events, and adherence statistics—are persisted locally within this single source of truth.
- Android file-based encryption (FBE) natively encrypts the entire credential-encrypted storage area while the device is locked.

### App Lock Scope
- When enabled in User Settings, Novelo requests authentication via `BiometricPrompt` (fingerprint, face unlock, or device PIN/pattern) before displaying any application screen.
- App lock protects against unauthorized physical inspection when the phone is unlocked.
- **Monotonic Clock Security**: Grace periods are tracked using `SystemClock.elapsedRealtime()` via `MonotonicClock`, preventing wall-clock manipulation, timezone shifts, or DST changes from extending grace periods or bypassing authentication.
- **Visual Gating & TalkBack Security**: When `isLocked == true`, the underlying medication hierarchy is removed from the composition, completely neutralizing TalkBack accessibility scraping or memory snooping.
- **Window `FLAG_SECURE`**: Dynamically applied when locked or when `isRecentsPreviewMasked == true`. Android OS enforces this at the SurfaceFlinger/Window level, blocking unauthorized screenshots, screen recording, and Recent Apps thumbnails.
- *Explicit Limitation*: App lock is an authentication barrier in front of the UI; it does not replace operating-system-level disk encryption.

### Database Encryption (SQLCipher Consideration)
- Novelo relies on Android's hardware-backed Credential Encrypted Storage (CE) by default.
- If raw file-level SQLCipher database encryption is introduced in a future phase, key management must use the Android Keystore backed by StrongBox / TEE. No claims of full database encryption shall be made until SQLCipher integration is implemented and verified.

---

## 4. Backup & Data Extraction Policy

To prevent sensitive medication records from leaking into unencrypted cloud backups or unauthenticated transfers:
- `AndroidManifest.xml` explicitly sets `android:allowBackup="false"`.
- `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` strictly exclude all domains from both cloud backups and device transfers:
```xml
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" />
        <exclude domain="sharedpref" />
        <exclude domain="root" />
        <exclude domain="file" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" />
        <exclude domain="sharedpref" />
        <exclude domain="root" />
        <exclude domain="file" />
    </device-transfer>
</data-extraction-rules>
```
- Cloud backups: 100% disabled and excluded.
- Automatic device-to-device transfers: 100% excluded. Data migration is handled strictly via user-controlled encrypted SAF export/import.

---

## 5. Safe Local Export & Import (Storage Access Framework & Cryptography)

- Export is **user-initiated only** through Android's Storage Access Framework (`ActivityResultContracts.CreateDocument`).
- **Encrypted Backup Format (AES-256-GCM + PBKDF2)**:
  - Key Derivation Function: `PBKDF2WithHmacSHA256` with 100,000 iterations and a 16-byte cryptographically secure random salt (`SecureRandom`).
  - Encryption: AES-256-GCM (`AES/GCM/NoPadding`) with a unique 12-byte random nonce and 128-bit authentication tag.
  - Zero-Knowledge Passphrase: The passphrase is never persisted in storage or logs. Character arrays and derived key bytes are immediately overwritten with zeroes in memory.
- **CSV Data Bundle**:
  - Exported as a ZIP bundle containing `manifest.json` and 7 RFC 4180 CSV tables.
  - User is explicitly warned before export that CSV files are unencrypted plain text.
- **Transactional Restore & Validation**:
  - `BackupValidator` checks schema versions, primary key uniqueness, and foreign key integrity before any database mutation.
  - Restore runs inside `NoveloDatabase.withTransaction` with 100% rollback guarantee if any constraint or deserialization failure occurs.

---

## 6. Logging & Telemetry Rules

- **Zero Remote Telemetry**: No telemetry or error reporting services exist in the app.
- **Release Logging**: In `release` builds, all debug logging (`Log.d`, `Log.v`, `Log.i`) is stripped via R8/ProGuard or gated behind `BuildConfig.DEBUG`.
- Sensitive fields (medication name, dosage, notes, schedule times, prescription numbers, doctor/pharmacy info) **must never** be written to Android logcat.

---

## 7. Android IPC, Intent & Receiver Security

Novelo enforces strict Android IPC security policies across all background receivers and pending intents:

### Receiver Export Isolation
- **`AlarmReceiver` (`android:exported="false"`)**:
  - Handles internal wake alarms fired by `AlarmManager`.
  - Restricted to the application process; cannot be invoked by external applications.
- **`NotificationActionReceiver` (`android:exported="false"`)**:
  - Handles user interactions (`ACTION_TAKE`, `ACTION_SKIP`, `ACTION_SNOOZE`) from notification action buttons.
  - Non-exported prevents malicious apps on the device from forging dose completions, skipping doses, or depleting medication stock balances.
- **`BootAndChangeReceiver` (`android:exported="true"`)**:
  - Listens only for platform-protected system broadcasts (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `ACTION_TIME_CHANGED`).
  - Android OS guarantees that only the system UID (UID 1000) can broadcast these protected intents. External apps attempting to broadcast them will fail with `SecurityException`.

### PendingIntent Immutability & Target Explicitness
- All `PendingIntent` instances specify `PendingIntent.FLAG_IMMUTABLE` (required on API 31+).
- Every pending intent targets an explicit component (`Intent(context, TargetReceiver::class.java)`), preventing Intent Redirection and Intent Hijacking vulnerabilities.
- Stable, deterministic request codes prevent pending intent collision across medication occurrences.

### Notification Privacy Masking
- When Notification Privacy Mode is enabled in settings (`isPrivacyModeEnabled = true`), Novelo masks sensitive health details:
  - **Masked Title**: `"Medication Reminder"` (replaces drug name).
  - **Masked Body**: `"It's time for your scheduled medication."` (replaces form, dosage, and stock balance).
- This prevents shoulder-surfing disclosure and unauthorized viewing of sensitive treatments on the lock screen.

---

## 8. Release Hardening & Manifest Audit Evidence

### Merged Manifest Permission Audit
During Phase 7 release hardening, the production release manifest (`app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`) was verified:
- **`android.permission.INTERNET`**: Confirmed **STRICTLY ABSENT**.
- **`android.permission.ACCESS_NETWORK_STATE`**: Pulled transitively by WorkManager runtime, explicitly **STRIPPED** via `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />`.
- **`android:allowBackup="false"`**: Confirmed preserved on `<application>` tag.
- **Backup Rule Files**: `@xml/backup_rules` and `@xml/data_extraction_rules` actively exclude all database and preference directories from cloud transfers.

### R8 Code Obfuscation & Binary Hardening
- Release builds enforce R8 dead-code elimination, member inlining, and identifier obfuscation.
- Release APK size: `3,097,805 bytes (~3.09 MB)` (86.6% reduction from debug build).
- Sensitive internal algorithms and data models are protected from reverse-engineering through automated keep rules in `rules.keep`.

### Screen Reader Data Isolation Verification
- When the application is locked via `AppLockManager`, the entire underlying Composable tree (medications, dose history, prescriptions, stock levels) is omitted from composition.
- Automated semantics tests (`AccessibilityTest.kt`) verify that screen reader accessibility trees contain zero health nodes while `LockScreen` is displayed.

