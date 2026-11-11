# Nuvilo Deep Privacy & Security Data Audit

**Product:** Nuvilo — *"Medication, made manageable."*  
**Application ID:** `com.nuvilomed.app` (internal namespace: `com.example.nuvilo`)  
**Audit Scope:** Full Application Source Code, Manifests, Merged Build Artifacts, Dependencies, Cryptography, and Storage Layers  
**Audit Date:** 2026-11-11  
**Auditor:** Senior Android Application Architect, Privacy & Security Engineer  

---

## 1. Executive Summary & Verification Statement

This document provides empirical, verifiable evidence regarding the privacy, data handling, and security posture of the **Nuvilo** native Android application. 

Every technical claim in this document is supported by source code inspections and compiler build outputs. Nuvilo is engineered under a strict **Zero-Network, Offline-First** philosophy. It stores all medication schedules, dose histories, stock movements, and prescription records exclusively in a private, on-device SQLite database. It does not transmit health data to any remote server, possesses no network communication permissions, and incorporates zero telemetry, analytics, advertising, or crash-tracking software development kits (SDKs).

---

## 2. Verification of the Offline-First Architecture

### 2.1 Manifest Permission Audit

The Android operating system enforces security sandboxing via manifest permissions declared in `AndroidManifest.xml` and resolved during Gradle compilation into the merged release manifest.

#### Empirical Manifest Analysis
1. **Source Manifest** (`app/src/main/AndroidManifest.xml`):
   - Contains exactly 4 primary permission declarations:
     - `android.permission.SCHEDULE_EXACT_ALARM` (time-critical dose alarms)
     - `android.permission.POST_NOTIFICATIONS` (system reminder notifications)
     - `android.permission.RECEIVE_BOOT_COMPLETED` (rescheduling alarms after phone reboot)
     - `android.permission.USE_BIOMETRIC` (optional app unlock via BiometricPrompt)
   - Explicitly removes transitive network permissions using `tools:node="remove"`:
     ```xml
     <uses-permission
         android:name="android.permission.ACCESS_NETWORK_STATE"
         tools:node="remove" />
     ```

2. **Merged Production Release Manifest** (`app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`):
   - Inspected lines 1–215 of the compiler-generated release manifest.
   - **`android.permission.INTERNET`**: **STRICTLY ABSENT**.
   - **`android.permission.ACCESS_NETWORK_STATE`**: **STRICTLY ABSENT** (successfully stripped).
   - `android.permission.READ_EXTERNAL_STORAGE`: **STRICTLY ABSENT**.
   - `android.permission.WRITE_EXTERNAL_STORAGE`: **STRICTLY ABSENT**.
   - `android.permission.ACCESS_FINE_LOCATION` / `COARSE_LOCATION`: **STRICTLY ABSENT**.

#### Technical Consequence
Because `android.permission.INTERNET` is not present in the merged manifest, **the Android Linux kernel and networking stack actively block the Nuvilo process from creating any TCP/UDP socket or issuing any HTTP/HTTPS network request**. Any attempt by code to open a socket would immediately throw a `SecurityException: Permission denied (missing INTERNET permission)`.

---

### 2.2 Dependency & Third-Party Library Audit

A comprehensive audit of `app/build.gradle.kts` and `gradle/libs.versions.toml` was conducted to verify that no network-capable third-party tracking or communication libraries exist within the binary:

| Dependency Category | Included Libraries | Remote Network Capabilities | Telemetry / Ad Tracking | Verification Status |
| :--- | :--- | :--- | :--- | :--- |
| **UI Framework** | AndroidX Jetpack Compose (BOM, Material3, UI, Graphics, Extended Icons) | None (Local rendering only) | None | **VERIFIED PASS** |
| **Architecture** | AndroidX Activity, Core KTX, Lifecycle Runtime/Compose/ViewModel, Navigation Compose | None | None | **VERIFIED PASS** |
| **Database** | AndroidX Room (Runtime, KTX, KSP Compiler) | None (Local SQLite only) | None | **VERIFIED PASS** |
| **Preferences** | AndroidX Jetpack DataStore (Preferences) | None (Local Protocol Buffers / flat file) | None | **VERIFIED PASS** |
| **Background Work** | AndroidX WorkManager (Runtime KTX) | None (Local SQLite task queue for low-stock checks) | None (`ACCESS_NETWORK_STATE` stripped) | **VERIFIED PASS** |
| **Authentication** | AndroidX Biometric | None (Local OS IPC to Keyguard / BiometricPrompt) | None | **VERIFIED PASS** |
| **Network Clients** | *None* (No Retrofit, OkHttp, Ktor, Volley, Apache HttpClient) | **N/A** | **N/A** | **VERIFIED PASS** (0 network libraries) |
| **Analytics & Ads** | *None* (No Firebase, Google Analytics, AppsFlyer, Mixpanel, Segment, Facebook SDK, AdMob) | **N/A** | **N/A** | **VERIFIED PASS** (0 analytics/ads) |
| **Crash Reporting** | *None* (No Crashlytics, Sentry, Bugsnag) | **N/A** | **N/A** | **VERIFIED PASS** (0 crash reporters) |

All software dependencies are official Google / AndroidX open-source components that execute 100% on-device.

---

## 3. Verified Personal Data Inventory

The following table documents every category of personal and health data stored or processed by Nuvilo:

| Category of Information | Purpose of Processing | Exact Storage Location | External Transmission | User Control & Deletion |
| :--- | :--- | :--- | :--- | :--- |
| **Medication Profiles**<br>(Name, strength, form, stock unit, color tag, notes) | Organizing treatment routines and calculating dosage quantities | Room SQLite table `medications` at `/data/data/com.nuvilomed.app/databases/novelo.db` | **None** (Stays on device) | User can edit, soft-archive, or permanently delete. Deletion triggers `CASCADE` across all tables. |
| **Dose Schedules**<br>(Target times, days of week, intervals, instructions) | Triggering scheduled medication alarms and calculating adherence timelines | Room SQLite table `schedules` | **None** (Stays on device) | User can edit or delete schedules anytime. Automatically deleted if parent medication is deleted. |
| **Dose History & Confirmations**<br>(Occurrence ID, scheduled time, confirmation status, taken time, notes, corrections) | Tracking historical adherence, displaying calendar trends, and calculating stock deductions | Room SQLite table `dose_events` | **None** (Stays on device) | User can view full history, correct erroneous entries, or delete medication records. |
| **Stock Ledger & Lot Records**<br>(Opening balances, deductions, lot numbers, batch expiry dates, adjustments) | Preventing stock-outs via FEFO inventory accounting | Room SQLite tables `stock_lots` and `stock_ledger_entries` | **None** (Stays on device) | User can manually adjust stock, record waste, or delete medication records. |
| **Prescription Details**<br>(Prescription reference numbers, GP/doctor, pharmacy name, repeats remaining, expiry dates) | Managing repeat prescription lifecycle and alerting when repeats are exhausted | Room SQLite tables `prescriptions` and `prescription_events` | **None** (Stays on device; does not connect to NHS/pharmacies) | User can edit, mark as expired/cancelled, or delete prescriptions. |
| **User Application Preferences**<br>(Theme mode, privacy masking toggle, app lock toggle, grace period) | Personalizing visual appearance and physical security behavior | Jetpack DataStore file at `/data/data/com.nuvilomed.app/files/datastore/user_preferences.preferences_pb` | **None** (Stays on device) | User toggles settings directly within Settings screen. Reset on app data clear. |
| **Notification Payloads**<br>(Medication name, dose string, stock balance, or masked generic labels) | Displaying local reminders via Android NotificationManager | In-memory `NotificationCompat.Builder` passed to local OS system server | **None** (Sent only to device notification shade) | User can disable notifications in system settings or enable Notification Privacy Mode. |
| **Biometric & Device Credentials**<br>(Fingerprints, facial geometry, device PIN/pattern) | **Authentication only** (unlocking app UI after grace period) | **NOT STORED BY NUVILO**. Processed strictly inside Android OS hardware TEE / StrongBox. | **None** | Nuvilo receives only a boolean success/failure callback. Zero biometric data is captured or accessible. |
| **Application Diagnostics & Logs**<br>(Debug logcat entries) | Internal developer debugging during development | Stripped in release builds via R8 / ProGuard rules (`-assumenosideeffects class android.util.Log { *; }`) | **None** (Release builds log zero medical details) | Automatically neutralized in production binaries. |
| **Encrypted Backup Files** (`.nuvilo.json`) | User-initiated offline data preservation and device migration | Saved to user-selected location via Android Storage Access Framework (SAF) | **User-directed** (Saved locally or to user's connected cloud drive) | User controls file destination, sharing, and deletion. Protected by AES-256-GCM. |
| **Plaintext CSV Data Bundles** (`.zip`) | User-initiated data portability for spreadsheets or clinical appointments | Saved to user-selected location via Android SAF | **User-directed** | User controls file creation and deletion. Explicit plain text warning displayed before saving. |

---

## 4. Biometric Authentication vs Biometric Data Collection

> [!IMPORTANT]
> **Critical Privacy Distinction for Legal Documentation**:  
> Nuvilo **DOES NOT** collect, store, transmit, or process biometric data.

- **How It Works**: When App Lock is enabled, Nuvilo utilizes the official AndroidX `BiometricPrompt` API ([BiometricAuthManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/security/BiometricAuthManager.kt)).
- **Hardware Isolation**: The user's biometric templates (fingerprint hashes, facial geometry) and device PINs/patterns are stored in the device's hardware-isolated Trusted Execution Environment (TEE) or StrongBox Keymaster.
- **Operating System Boundary**: The Android OS draws the authentication dialog independently.
- **Callback Only**: Nuvilo receives only a discrete callback:
  - `onAuthenticationSucceeded(result)`
  - `onAuthenticationError(errorCode, errString)`
  - `onAuthenticationFailed()`
- Nuvilo never has access to camera streams, fingerprint sensor data, biometric keys, or system credentials.

---

## 5. Cryptography & Backup Architecture

Nuvilo provides two distinct user-initiated export formats via Android's Storage Access Framework (SAF).

```
+-------------------------------------------------------------------------+
|                  NUVILO DATA EXPORT ARCHITECTURE                         |
+-------------------------------------------------------------------------+
                                     |
               +---------------------+---------------------+
               |                                           |
       [Encrypted Backup]                           [Data Portability]
      Format: .nuvilo.json                         Format: .zip (CSV Bundle)
               |                                           |
    +-----------------------+                    +--------------------+
    | PBKDF2-HMAC-SHA256    |                    | manifest.json      |
    | (100,000 iterations)  |                    | medications.csv    |
    | 16-byte random salt   |                    | schedules.csv      |
    +-----------------------+                    | dose_events.csv    |
               |                                 | stock_lots.csv     |
    +-----------------------+                    | stock_ledger.csv   |
    | AES-256-GCM           |                    | prescriptions.csv  |
    | 12-byte random nonce  |                    | events.csv         |
    | 128-bit auth tag      |                    +--------------------+
    +-----------------------+                              |
               |                                           |
    +-----------------------+                    +--------------------+
    | In-Memory Zeroization |                    | Explicit Plaintext |
    | of Passphrase & Keys  |                    | Warning Dialog     |
    +-----------------------+                    +--------------------+
               |                                           |
               +---------------------+---------------------+
                                     |
                         [Android OS SAF Document]
                                     |
                +--------------------+--------------------+
                |                                         |
     (Local Device / SD Card)                (User-Connected Cloud Provider)
```

### 5.1 Military-Grade Encrypted Backup (`.nuvilo.json`)
Verified in [BackupCrypto.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/backup/BackupCrypto.kt):
- **Key Derivation Function (KDF)**: `PBKDF2WithHmacSHA256` with **100,000 iterations** (`KDF_ITERATIONS = 100_000`) and a 16-byte cryptographically secure random salt generated via `java.security.SecureRandom`.
- **Symmetric Cipher**: `AES-256-GCM` (`AES/GCM/NoPadding`) with a unique 12-byte random nonce and a 128-bit authentication tag (`GCM_TAG_LENGTH_BITS = 128`).
- **Memory Zeroization**: The user's passphrase is held in a `CharArray`. Immediately after key derivation, `keySpec.clearPassword()` is invoked, and derived key bytes are wiped via `Arrays.fill(keyBytes, 0.toByte())` to prevent lingering secrets in device RAM.
- **Tamper Protection**: Any bit modification to the ciphertext, salt, or nonce causes AES-GCM tag verification to fail, throwing `InvalidPassphraseOrCorruptedDataException` before any data can be deserialized.
- **Restore Validation**: [BackupValidator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/backup/BackupValidator.kt) performs structural validation (schema version matching, primary key uniqueness, foreign key consistency) and executes restore within `NoveloDatabase.withTransaction` with 100% rollback guarantee if any constraint fails.

### 5.2 Readable CSV ZIP Export (`.zip`)
Verified in [CsvZipBackupManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/backup/CsvZipBackupManager.kt):
- Exports a standard ZIP archive containing `manifest.json` and 7 human-readable CSV files conforming to RFC 4180.
- **Security Notice**: This export is **explicitly unencrypted**. The application displays a mandatory confirmation warning before export:
  *"This CSV zip bundle is unencrypted. Anyone with access to the exported file can view your medication schedules, dose history, and prescription records. Store it securely."*

### 5.3 Storage Access Framework (SAF) Destination Realities
- Exports are executed exclusively via Android OS system picker `ActivityResultContracts.CreateDocument()`.
- **Important Privacy Reality**: Nuvilo writes only to the `Uri` returned by the Android system file picker. If the user selects a local folder (e.g. `Downloads`), the file remains strictly on the device. If the user selects a third-party document provider (such as Google Drive, Microsoft OneDrive, or Dropbox), the upload is executed by that third-party provider's Android document provider service, not by Nuvilo.

---

## 6. Data Retention, Deletion & Cloud Backup Exclusions

### 6.1 Database Cascade Deletion
- Foreign key constraints are declared with `onDelete = ForeignKey.CASCADE` across all related tables:
  - Deleting a medication permanently purges all its child `schedules`, `dose_events`, `stock_lots`, `stock_ledger_entries`, `prescriptions`, and `prescription_events`.
- SQLite `VACUUM` and write-ahead log checkpointing reclaim freed space.

### 6.2 Application Uninstall
- When a user uninstalls Nuvilo, the Android operating system permanently deletes the application's private sandboxed directory (`/data/data/com.nuvilomed.app/`), completely purging:
  - The SQLite database (`novelo.db`, `novelo.db-wal`, `novelo.db-shm`)
  - The DataStore preferences (`user_preferences.preferences_pb`)
  - All local cache files
- Exported backup files saved to external storage or cloud document providers prior to uninstall remain under the user's manual file management control.

### 6.3 Android OS Cloud Backup Exclusions
Verified in [backup_rules.xml](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/res/xml/backup_rules.xml) and [data_extraction_rules.xml](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/res/xml/data_extraction_rules.xml):
- `android:allowBackup="false"` is declared on the `<application>` tag in `AndroidManifest.xml`.
- XML rules explicitly exclude all storage domains from both Google Drive cloud backups and device-to-device transfers:
  ```xml
  <data-extraction-rules>
      <cloud-backup>
          <exclude domain="database" path="." />
          <exclude domain="sharedpref" path="." />
          <exclude domain="root" path="." />
          <exclude domain="file" path="." />
      </cloud-backup>
      <device-transfer>
          <exclude domain="database" path="." />
          <exclude domain="sharedpref" path="." />
          <exclude domain="root" path="." />
          <exclude domain="file" path="." />
      </device-transfer>
  </data-extraction-rules>
  ```
- Result: Android's automated Google Drive backup mechanism **never** copies Nuvilo's database or preferences to Google cloud servers.

---

## 7. Known Privacy Limitations & Edge Cases

To maintain uncompromising legal and technical integrity, the website privacy policy must acknowledge the following technical boundaries:

1. **Android Operating System Notifications**:
   - Notifications posted to the Android notification shade are handled by the Android system server.
   - If the user uses third-party notification listeners, smart watches, or Bluetooth-paired devices (e.g. Wear OS, fitness bands), those external devices receive notification text unless Notification Privacy Mode is enabled.
2. **Device-Level Keyloggers & Screen Recorders**:
   - Nuvilo protects against screen capture using `FLAG_SECURE` when locked or when recent preview masking is enabled.
   - However, a compromised or rooted device with malicious kernel-level software or malicious accessibility services could theoretically intercept user input.
3. **User-Selected SAF Destinations**:
   - If a user exports unencrypted CSV files or encrypted JSON backups to cloud storage providers (Google Drive, OneDrive), those files become subject to the privacy policies and security posture of those cloud storage providers.
4. **Physical Shoulder Surfing**:
   - When the app is actively unlocked and in foreground use, anyone physically looking over the user's shoulder can view on-screen information. App Lock mitigates this when the device is set down or left unattended.

---

## 8. Privacy Policy Recommendations for Website

When drafting the public Privacy Policy for the Nuvilo website, adhere strictly to these guidelines:

1. **Distinguish App vs Website**: Clearly state that the Android application operates completely offline with zero telemetry, while visiting the Nuvilo marketing website involves standard web hosting server logs (IP address, user agent) provided by the web hosting platform (e.g. Vercel, Netlify, Cloudflare).
2. **Do Not Claim Biometric Storage**: Explicitly state that Nuvilo uses Android's native `BiometricPrompt` and **never has access to or stores biometric data**.
3. **Explain User Export Responsibility**: Accurately explain that exported CSV files are unencrypted for spreadsheet portability, while `.nuvilo.json` backups are encrypted with AES-256-GCM.
4. **Use Explicit Placeholders**: For unconfirmed legal details (e.g. registered company name, official contact email, governing jurisdiction), use prominent placeholders:
   - `[LEGAL ENTITY NAME]`
   - `[CONTACT EMAIL]`
   - `[EFFECTIVE DATE]`
   - `[COUNTRY / JURISDICTION]`
