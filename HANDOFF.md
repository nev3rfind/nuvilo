# Novelo Session Handoff

## 1. Current State Summary
- **Current Phase**: Phase 6 (Privacy, Biometric Lock & Local Backup/Restore) **VERIFIED & COMPLETE**.
- **Project Name**: Novelo (package `com.example.nuvilo`).
- **Build Status**: 100% green across all test and build suites:
  - `.\gradlew.bat testDebugUnitTest` (Unit tests): **PASSED** (49 tests executed across 9 suites, 0 failures, 0 errors in 32s).
  - `.\gradlew.bat connectedDebugAndroidTest` (On-device tests on `emulator-5554` API 37): **PASSED** (18 tests executed across 5 suites, 0 failures, 100% pass rate in 29s).
  - `.\gradlew.bat lintDebug` (Static analysis): **PASSED** (0 errors in 1m 27s).
  - `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL in 27s**.
  - App installed and actively running on `emulator-5554` with cold-start lock and `FLAG_SECURE` verified.
- **JDK Configuration**: OpenJDK 25 (`C:\Program Files\Android\Android Studio\jbr`).

---

## 2. Completed Work in Phase 6
- [x] **AndroidX BiometricPrompt & Device Credential Authentication (`security/BiometricAuthManager.kt`)**:
  - `FragmentActivity` architecture supporting biometric prompts across all Android API levels (24+).
  - Evaluation of `BIOMETRIC_STRONG` (fingerprint, 3D face) and `DEVICE_CREDENTIAL` (PIN/pattern/password).
  - Strict omission of negative button text when `DEVICE_CREDENTIAL` is set, avoiding runtime crashes.
  - Returns structured `BiometricStatus` (`AVAILABLE`, `DEVICE_CREDENTIAL_ONLY`, `NONE_ENROLLED`, `NO_HARDWARE`, `UNAVAILABLE`).
- [x] **Monotonic Clock App Lock Manager (`security/AppLockManager.kt`)**:
  - Monotonic elapsed time calculation via `SystemClock.elapsedRealtime()` (`MonotonicClock`), impervious to wall-clock manipulation, timezone shifts, and DST transitions.
  - Configurable grace periods: `Immediately` (0s), `30 seconds` (30s), `1 minute` (60s), `5 minutes` (300s), `15 minutes` (900s).
  - System dialog suppression (`setSystemDialogActive(true)`) preventing relocking during biometric prompts or SAF file pickers.
  - Cold-start lock enforcement (`initialize(lockEnabled = true)`).
- [x] **Visual Gating & Privacy Shield (`ui/screens/LockScreen.kt`, `MainActivity.kt`)**:
  - Full-screen `LockScreen` overlay replaces underlying composable tree when `isLocked == true`, neutralizing TalkBack accessibility scraping and memory inspection.
  - Back button intercept (`BackHandler { finish() }`) exits the app rather than revealing underlying screens.
  - Dynamic `FLAG_SECURE` window management: automatically applied when app is locked or when user enables "Mask in Android Recents".
- [x] **Cryptographic Local JSON Backup (`data/backup/BackupCrypto.kt`, `JsonBackupManager.kt`)**:
  - PBKDF2-HMAC-SHA256 key derivation with 100,000 iterations and 16-byte cryptographically secure random salt (`SecureRandom`).
  - AES-256-GCM symmetric encryption with 12-byte random nonce and 128-bit authentication tag.
  - In-memory passphrase and derived key byte zeroization (`Arrays.fill(..., 0)`).
  - Envelope JSON specification with tamper detection and corrupted data rejection.
- [x] **Full-Data CSV ZIP Bundle Export (`data/backup/CsvZipBackupManager.kt`)**:
  - Full-fidelity ZIP export containing `manifest.json` and 7 RFC 4180 CSV tables covering all Room entities.
  - Consistent null token `\N` distinguishing empty strings from SQL nulls.
- [x] **Storage Access Framework (SAF) & Settings UI (`SettingsScreen.kt`, `ViewModels.kt`)**:
  - Zero broad storage permissions requested (uses `CreateDocument` and `OpenDocument`).
  - "App Lock & Security" card: hardware status badge, biometric toggle, grace period chips, recents preview toggle.
  - "Local Data & Offline Backup" card: last export timestamp, passphrase creation modal dialog, CSV unencrypted warning modal dialog.
  - Pre-restore summary dialog with counts (medications, schedules, doses, lots, movements, prescriptions) and replacement warning.
- [x] **Transactional Restore & Safety Invariants (`BackupRestoreRepositoryImpl.kt`, `BackupValidator.kt`)**:
  - Structural and referential integrity validation before database modification.
  - Atomic execution inside `NoveloDatabase.withTransaction` (reverse-dependency deletion, forward-dependency insertion, 100% rollback guarantee on failure).
  - Post-restore alarm reconciliation sync with `AlarmScheduler`.
- [x] **Strict Offline & Backup Privacy Rules**:
  - `AndroidManifest.xml` enforces `android:allowBackup="false"`.
  - `backup_rules.xml` and `data_extraction_rules.xml` strictly exclude `database`, `sharedpref`, `root`, and `file` from cloud backups and device transfers.
  - Zero network permissions verified.

---

## 3. Discrepancies & Resolutions
- **`BiometricPrompt` Activity Requirement**:
  - AndroidX `BiometricPrompt` requires `FragmentActivity`. `MainActivity` was upgraded from `ComponentActivity` to `FragmentActivity`, preserving all Jetpack Compose edge-to-edge and ViewModel capabilities.
- **`FLAG_SECURE` Screen Capture Behavior**:
  - When `FLAG_SECURE` is active, Android OS blocks `adb exec-out screencap`, resulting in a completely black image (`06_app_lock_screen.png`). This confirmed that window protection is 100% enforced. Default `isRecentsPreviewMasked` was set to `false` so users can capture normal screenshots unless they explicitly enable recents masking.

---

## 4. Failing Tests or Regressions
- **None**:
  - JVM Unit Tests: **49/49 PASSED** (0 failures, 0 errors).
  - Connected Android Tests: **18/18 PASSED** on `emulator-5554` (100% pass rate).
  - Lint: **0 errors**.
  - Runtime: APK installed and verified on `emulator-5554`.

---

## 5. Next Recommended Phase: Phase 7
- **Phase 7: Release Hardening, Final Verification & Polishing**:
  - Accessibility audit and TalkBack verification across all screens.
  - Performance profiling (recomposition counts, memory leak audit).
  - R8 / ProGuard rules verification and Release APK assembly.
  - Final documentation freeze.
