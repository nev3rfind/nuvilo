# Novelo Testing & Verification Guide

This document specifies the exact, verified commands and test configurations for **Novelo** on this development machine.

---

## 1. Environment & Java Prerequisites

Gradle requires Java 25 or a compatible recent JDK matching AGP 9.4.1. The verified JDK is the Android Studio bundled runtime at `C:\Program Files\Android\Android Studio\jbr`.

Always set `JAVA_HOME` in your PowerShell session before running Gradle tasks:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

---

## 2. Test Execution Commands

### Unit Tests
Runs all pure Kotlin domain tests, use-case tests, and stock ledger calculation tests on the JVM:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
```
*Filter for specific unit test classes:*
```powershell
.\gradlew.bat test --tests "com.example.nuvilo.domain.*"
```

### Build Verification (Debug APK)
Verifies manifest merging, code generation, Kotlin compilation, and APK assembly:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

### Static Analysis & Lint
Checks for Android best practices, accessibility issues, security hazards, and XML errors:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat lintDebug
```
*Lint reports are generated at `app/build/reports/lint-results-debug.html`.*

---

## 3. Connected Device & Emulator Testing

### Checking Connected Devices
Check running emulators and attached hardware:
```powershell
& "C:\Users\ddona\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
```
*Current verified device: `emulator-5554 device`.*

### Instrumentation Tests (Compose UI & Room SQLite)
Runs AndroidJUnit4 instrumentation tests on the connected emulator:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedDebugAndroidTest
```

### Installing and Launching on Device
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installDebug
& "C:\Users\ddona\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.nuvilo/.MainActivity
```

---

## 4. Test Strategy Matrix

| Layer | Test Type | Runner | Target Invariants |
| :--- | :--- | :--- | :--- |
| **Domain** | Unit Tests | JUnit 4 / Kotlin Test | Stock ledger math, exact integer conversions, dose idempotency rules, days remaining formulas. |
| **Data (Room)** | Integration Tests | AndroidJUnit4 (In-Memory DB) | Foreign key cascading, unique constraints on `occurrenceId`, transaction atomicity, migration integrity. |
| **System** | Intent/Receiver & Worker Tests | AndroidJUnit4 on Device | `NotificationAndSchedulerTest`: Alarm scheduling, Take/Skip/Snooze actions, DOSE_REVERSAL on auto-assume skip, low-stock deduplication, 3-day horizon reconciliation. |
| **UI** | Compose UI Tests | `ComposeContentTestRule` | Screen state rendering, button click callbacks, accessibility content descriptions, TalkBack traversal. |
| **Manifest** | Static Check | Gradle Task / Merged Manifest | Verification that `android.permission.INTERNET` is 100% absent. |

---

## 5. Continuous Evidence Standard

No task or phase shall be marked complete without providing direct terminal execution output demonstrating:
1. `BUILD SUCCESSFUL`
2. Zero failed tests (`0 failures, 0 errors, 0 skipped`)
3. Zero unresolved lint errors

---

## 6. Latest Verified Test Execution (Phase 3)

### Unit Tests (`testDebugUnitTest`)
- **Status**: `BUILD SUCCESSFUL in 30s`
- **Result**: All pure Kotlin domain and ledger calculator unit tests passed (0 failures).

### On-Device Connected Tests (`connectedDebugAndroidTest` on `emulator-5554`)
- **Status**: `BUILD SUCCESSFUL in 35s`
- **Executed**: 14 test cases across 4 test suites:
  1. `com.example.nuvilo.system.NotificationAndSchedulerTest`:
     - `confirmMode_doesNotDeductStockUntilExplicitTakeAction` [PASSED]
     - `autoAssumeMode_recordsAssumedAdherence_andCompensatesIfSkipped` [PASSED]
     - `snoozeAction_leavesStockAndDoseRecordsUntouched` [PASSED]
     - `reconcileUpcomingReminders_updatesTimestampAndSchedulesFutureDoses` [PASSED]
     - `lowStockAlertDeduplication_resetsAfterRestock` [PASSED]
  2. `com.example.nuvilo.data.NoveloDatabaseTest`:
     - `createMedication_createsMedicationAndInitialStockLedgerMovement` [PASSED]
     - `recordDose_isStrictlyIdempotent` [PASSED]
     - `skippedDose_doesNotDeductStock` [PASSED]
     - `restockWithLot_createsLotAndUpdatesBalance` [PASSED]
     - `adjustStockToTarget_computesExactDeltaAndAuditLedger` [PASSED]
     - `doseTaking_debitsLotsUsingFEFO` [PASSED]
     - `correctDose_reversesLedgerCompensatingMovementIdempotently` [PASSED]
  3. `com.example.nuvilo.data.NoveloDatabaseMigrationTest`:
     - `migration1To2_preservesExistingDataAndAddsLotTrackingAndDoseCorrections` [PASSED]
  4. `com.example.nuvilo.ExampleInstrumentedTest`:
     - `useAppContext` [PASSED]

### Static Analysis (`lintDebug`)
- **Status**: `BUILD SUCCESSFUL in 31s`
- **Result**: 0 errors.

---

## 7. Latest Verified Test Execution (Phase 4)

### Unit Tests (`testDebugUnitTest`)
- **Status**: `BUILD SUCCESSFUL in 34s`
- **Result**: 32 unit test cases passed with 0 failures:
  - `com.example.nuvilo.domain.adherence.AdherenceCalculatorTest` (7 tests: accurate denominator, confirmed vs assumed, DST boundaries, dose reversal corrections, past unrecorded resolution) [PASSED]
  - `com.example.nuvilo.domain.prescription.PrescriptionDomainTest` (5 tests: repeat request neutrality, collection decrement, non-negative bounds, expiry guards, duplicate action key idempotency) [PASSED]
  - `com.example.nuvilo.domain.stock.StockLedgerCalculatorTest` (8 tests: exact calculations, days remaining, lot allocation) [PASSED]
  - `com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculatorTest` (7 tests: deterministic IDs, reconciliation) [PASSED]
  - `com.example.nuvilo.domain.model.ScaledQuantityTest` (5 tests: integer arithmetic, string formatting) [PASSED]

### On-Device Connected Tests (`connectedDebugAndroidTest` on `SM-S948B - 17`)
- **Status**: `BUILD SUCCESSFUL in 39s`
- **Result**: 15 test cases executed, 0 failures, 0 skipped (100% success rate):
  1. `com.example.nuvilo.data.NoveloDatabaseTest` (8 tests):
     - `createMedication_createsMedicationAndInitialStockLedgerMovement` [PASSED]
     - `recordDose_isStrictlyIdempotent` [PASSED]
     - `skippedDose_doesNotDeductStock` [PASSED]
     - `restockWithLot_createsLotAndUpdatesBalance` [PASSED]
     - `adjustStockToTarget_computesExactDeltaAndAuditLedger` [PASSED]
     - `doseTaking_debitsLotsUsingFEFO` [PASSED]
     - `correctDose_reversesLedgerCompensatingMovementIdempotently` [PASSED]
     - `prescriptionRepository_supportsCreateUpdateRequestCollectAndAuditHistory` [PASSED]
  2. `com.example.nuvilo.data.NoveloDatabaseMigrationTest` (1 test):
     - `migration2To3_preservesExistingDataAndAddsPrescriptionTables` [PASSED]
  3. `com.example.nuvilo.system.NotificationAndSchedulerTest` (5 tests):
     - `confirmMode_doesNotDeductStockUntilExplicitTakeAction` [PASSED]
     - `autoAssumeMode_recordsAssumedAdherence_andCompensatesIfSkipped` [PASSED]
     - `snoozeAction_leavesStockAndDoseRecordsUntouched` [PASSED]
     - `reconcileUpcomingReminders_updatesTimestampAndSchedulesFutureDoses` [PASSED]
     - `lowStockAlertDeduplication_resetsAfterRestock` [PASSED]
  4. `com.example.nuvilo.ExampleInstrumentedTest` (1 test):
     - `useAppContext` [PASSED]

### Static Analysis (`lintDebug`)
- **Status**: `BUILD SUCCESSFUL in 2m`
- **Result**: 0 errors.

### Build Verification & Runtime Launch
- `assembleDebug` and `assembleDebugAndroidTest`: `BUILD SUCCESSFUL in 22s`.
- `adb install -r app-debug.apk`: `Success`.
- `am start -n com.example.nuvilo/.MainActivity`: Activity started, rendered cleanly, zero crashes in logcat.

---

## 8. Latest Verified Test Execution (Phase 5)

### Unit Tests (`testDebugUnitTest`)
- **Status**: `BUILD SUCCESSFUL in 46s`
- **Result**: 32 unit test cases executed across all domain and adherence suites with 0 failures:
  - `AdherenceCalculatorTest` (7 tests) [PASSED]
  - `PrescriptionDomainTest` (5 tests) [PASSED]
  - `StockLedgerCalculatorTest` (8 tests) [PASSED]
  - `ScheduleOccurrenceCalculatorTest` (7 tests) [PASSED]
  - `ScaledQuantityTest` (5 tests) [PASSED]

### On-Device Connected Tests (`connectedDebugAndroidTest` on `emulator-5554`)
- **Status**: `BUILD SUCCESSFUL in 54s`
- **Result**: 15 test cases executed across 4 test suites with 0 failures (100% success rate):
  - `NoveloDatabaseTest` (8 tests) [PASSED]
  - `NoveloDatabaseMigrationTest` (1 test) [PASSED]
  - `NotificationAndSchedulerTest` (5 tests) [PASSED]
  - `ExampleInstrumentedTest` (1 test) [PASSED]

### Static Analysis (`lintDebug`)
- **Status**: `BUILD SUCCESSFUL in 2m 7s`
- **Result**: 0 errors.

### Build Verification & Live Visual Inspection
- `assembleDebug`: `BUILD SUCCESSFUL`.
- `adb install -r app-debug.apk`: Installed cleanly on `emulator-5554` (API 37).
- Before and after visual screenshot comparison captured in `docs/screenshots/phase5/`:
  - `dashboard_after.png`: Verified Hero Dose card, Today's Timeline with vertical track, Greeting + Date header, zero double status bar padding.
  - `dashboard_dark_after.png`: Verified dark mode foundation with high-contrast `#121820` slate navy surfaces.
  - `calendar_after.png`: Verified accessible indicators (`✓`, `~`, `×`, `!`) and 1dp card outlines.
  - `medications_after.png`: Verified treatments list with stock runway badges.
  - `add_med_after.png`: Verified `Modifier.imePadding()` preventing virtual keyboard button overlap.
  - `detail_after.png` & `detail_scrolled_after.png`: Verified FEFO lot badges, inventory cards, and prescription management.
  - `settings_after.png` & `settings_dark_after.png`: Verified appearance controls and exact alarm diagnostics.

---

## 10. Latest Verified Test Execution (Phase 7: Release Hardening)

### Unit Tests (`testDebugUnitTest`)
- **Status**: `BUILD SUCCESSFUL in 1m 11s`
- **Result**: 49 unit tests executed across 9 test suites with 0 failures:
  1. `com.example.nuvilo.security.AppLockManagerTest` (10 tests) [PASSED]
  2. `com.example.nuvilo.data.backup.BackupCryptoTest` (3 tests) [PASSED]
  3. `com.example.nuvilo.data.backup.BackupSerializationTest` (4 tests) [PASSED]
  4. `com.example.nuvilo.domain.adherence.AdherenceCalculatorTest` (7 tests) [PASSED]
  5. `com.example.nuvilo.domain.model.ScaledQuantityTest` (5 tests) [PASSED]
  6. `com.example.nuvilo.domain.prescription.PrescriptionDomainTest` (5 tests) [PASSED]
  7. `com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculatorTest` (3 tests) [PASSED]
  8. `com.example.nuvilo.domain.stock.StockLedgerCalculatorTest` (11 tests) [PASSED]
  9. `com.example.nuvilo.ExampleUnitTest` (1 test) [PASSED]

### On-Device Connected Tests (`connectedDebugAndroidTest` on `emulator-5554`)
- **Status**: `BUILD SUCCESSFUL in 1m 23s`
- **Result**: 23 test cases executed across 6 test suites with 0 failures (100% pass rate):
  1. `com.example.nuvilo.ui.AccessibilityTest` (5 tests) [PASSED]
     - `heroDoseCard_actionButtons_meetMinimumTouchTargetSize_andProvideTalkBackLabels` [PASSED]
     - `timelineDoseItem_actionButtons_meetMinimumTouchTargetSize_andAreIndependentlyAccessible` [PASSED]
     - `sectionHeader_hasHeadingSemantics_forScreenReaderNavigation` [PASSED]
     - `emptyStateCard_meetsAccessibilityTarget_andPresentsClearLabels` [PASSED]
     - `lockScreen_isolatesSensitiveHealthData_andProvidesAccessibleUnlockTarget` [PASSED]
  2. `com.example.nuvilo.data.BackupRestoreDatabaseTest` (3 tests) [PASSED]
  3. `com.example.nuvilo.data.NoveloDatabaseTest` (8 tests) [PASSED]
  4. `com.example.nuvilo.data.NoveloDatabaseMigrationTest` (1 test) [PASSED]
  5. `com.example.nuvilo.system.NotificationAndSchedulerTest` (5 tests) [PASSED]
  6. `com.example.nuvilo.ExampleInstrumentedTest` (1 test) [PASSED]

### Static Analysis (`lintDebug`)
- **Status**: `BUILD SUCCESSFUL in 3m 1s`
- **Result**: 0 errors, 0 actionable warnings.

### Release APK Assembly & Minification (`assembleRelease`)
- **Status**: `BUILD SUCCESSFUL in 4m 53s`
- **Output**: `app/build/outputs/apk/release/app-release-unsigned.apk` (3,097,805 bytes / ~3.09 MB vs 23 MB debug APK, 86.6% reduction).
- **R8 Rules**: Verified via `app/src/main/keepRules/rules.keep`.
- **Zero Network Invariant**: Confirmed `android.permission.INTERNET` absent and `ACCESS_NETWORK_STATE` stripped.


### Unit Tests (`testDebugUnitTest`)
- **Status**: `BUILD SUCCESSFUL in 32s`
- **Result**: 49 unit tests executed across 9 test suites with 0 failures:
  1. `com.example.nuvilo.security.AppLockManagerTest` (10 tests) [PASSED]
     - `appStartsLockedWhenEnabled`
     - `appStartsUnlockedWhenDisabled`
     - `unlockClearsLockState`
     - `immediateGracePeriod_locksImmediatelyOnForeground`
     - `thirtySecondsGracePeriod_remainsUnlockedBefore30Seconds`
     - `thirtySecondsGracePeriod_locksAfter30Seconds`
     - `oneMinuteGracePeriod_boundaryCheck`
     - `fiveMinutesGracePeriod_boundaryCheck`
     - `fifteenMinutesGracePeriod_boundaryCheck`
     - `systemDialogActive_suppressesLocking`
  2. `com.example.nuvilo.data.backup.BackupCryptoTest` (3 tests) [PASSED]
     - `encryptAndDecrypt_roundTripSuccess`
     - `wrongPassphrase_throwsInvalidPassphraseOrCorruptedDataException`
     - `tamperedCiphertext_throwsException`
  3. `com.example.nuvilo.data.backup.BackupSerializationTest` (4 tests) [PASSED]
     - `jsonBackup_serializationAndDeserialization_roundTripSuccess`
     - `csvZipBackup_exportAndImport_roundTripSuccess`
     - `backupValidator_rejectsOrphanSchedules`
     - `backupValidator_rejectsDuplicateMedicationIds`
  4. `com.example.nuvilo.domain.adherence.AdherenceCalculatorTest` (7 tests) [PASSED]
  5. `com.example.nuvilo.domain.model.ScaledQuantityTest` (5 tests) [PASSED]
  6. `com.example.nuvilo.domain.prescription.PrescriptionDomainTest` (5 tests) [PASSED]
  7. `com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculatorTest` (3 tests) [PASSED]
  8. `com.example.nuvilo.domain.stock.StockLedgerCalculatorTest` (11 tests) [PASSED]
  9. `com.example.nuvilo.ExampleUnitTest` (1 test) [PASSED]

### On-Device Connected Tests (`connectedDebugAndroidTest` on `emulator-5554`)
- **Status**: `BUILD SUCCESSFUL in 29s`
- **Result**: 18 test cases executed across 5 test suites with 0 failures (100% pass rate):
  1. `com.example.nuvilo.data.BackupRestoreDatabaseTest` (3 tests) [PASSED]
     - `createBackup_retrievesAllEntitiesFromDatabase` [PASSED]
     - `executeRestore_clearsAndRestoresEntitiesTransactionally` [PASSED]
     - `executeRestore_withInvalidReferentialIntegrity_failsAndRollsBack` [PASSED]
  2. `com.example.nuvilo.data.NoveloDatabaseTest` (8 tests) [PASSED]
     - `createMedication_createsMedicationAndInitialStockLedgerMovement` [PASSED]
     - `doseTaking_debitsLotsUsingFEFO` [PASSED]
     - `skippedDose_doesNotDeductStock` [PASSED]
     - `recordDose_isStrictlyIdempotent` [PASSED]
     - `restockWithLot_createsLotAndUpdatesBalance` [PASSED]
     - `adjustStockToTarget_computesExactDeltaAndAuditLedger` [PASSED]
     - `correctDose_reversesLedgerCompensatingMovementIdempotently` [PASSED]
     - `prescriptionRepository_supportsCreateUpdateRequestCollectAndAuditHistory` [PASSED]
  3. `com.example.nuvilo.data.NoveloDatabaseMigrationTest` (1 test) [PASSED]
     - `migration1To2_preservesExistingDataAndAddsLotTrackingAndDoseCorrections` [PASSED]
  4. `com.example.nuvilo.system.NotificationAndSchedulerTest` (5 tests) [PASSED]
     - `confirmMode_doesNotDeductStockUntilExplicitTakeAction` [PASSED]
     - `autoAssumeMode_recordsAssumedAdherence_andCompensatesIfMissed` [PASSED]
     - `snoozeAction_leavesStockAndDoseRecordsUntouched` [PASSED]
     - `reconcileUpcomingReminders_updatesTimestampAndSchedulesExactAlarms` [PASSED]
     - `lowStockAlertDeduplication_resetsAfterRestock` [PASSED]
  5. `com.example.nuvilo.ExampleInstrumentedTest` (1 test) [PASSED]
     - `useAppContext` [PASSED]

### Static Analysis (`lintDebug`)
- **Status**: `BUILD SUCCESSFUL in 1m 27s`
- **Result**: 0 errors.

### Build Verification & Runtime Launch
- `assembleDebug`: `BUILD SUCCESSFUL in 27s`.
- `adb install -r app-debug.apk`: Installed cleanly on `emulator-5554` (API 37).
- Screenshots captured in `docs/screenshots/phase6/`:
  - `01_dashboard.png`: Today Dashboard.
  - `02_settings_privacy_and_security.png`: Settings Screen showing 100% Offline banner, App Lock & Security card, and Local Backup card.
  - `03_settings_local_backup.png`: Local Backup actions, last export timestamp, and notification privacy.
  - `04_backup_passphrase_dialog.png`: Set Backup Passphrase dialog with AES-256-GCM explanation and password visibility toggle.
  - `05_app_lock_enabled_options.png`: App Lock enabled with Grace Period filter chips (`Immediately`, `30s`, `1m`, `5m`, `15m`) and Recents preview masking toggle.
  - `06_app_lock_screen.png`: Android OS `FLAG_SECURE` window security verification (black surface on screen capture).


