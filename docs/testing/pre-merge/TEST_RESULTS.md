# Nuvilo: Pre-Merge Test Execution Results (TEST_RESULTS.md)

**Product:** Nuvilo – Medication, made manageable.  
**Repository:** https://github.com/nev3rfind/nuvilo  
**Target Branch:** `test/8-pre-merge-regression-200`  
**Execution Timestamp:** 2026-11-06T19:24:46Z  
**Device Environment:** Android 17 / API 37 (`Medium_Phone_API_37.0`, `emulator-5554`)  
**Java / JVM Runtime:** OpenJDK 21.0.6 (Android Studio JBR 17/21 compatibility)  
**Overall Outcome:** **100% PASSING (287 / 287 Automated Tests)**

---

## 1. Executive Summary Table

| Category | Suite | Test Type | Total Cases | Passed | Failed | Skipped | Pass Rate | Execution Time |
|:---|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Baseline** | `com.example.nuvilo.domain.*` | JVM | 56 | 56 | 0 | 0 | 100.0% | 14s |
| **Baseline** | `com.example.nuvilo.data.*` | Connected (AVD) | 29 | 29 | 0 | 0 | 100.0% | 28s |
| **Cat A (Domain/Stock)** | `regression.ScaledQuantity...` | JVM | 55 | 55 | 0 | 0 | 100.0% | 22s |
| **Cat B (Room/DAOs)** | `regression.RoomDao...` | Connected (AVD) | 30 | 30 | 0 | 0 | 100.0% | 35s |
| **Cat C (Security/Crypto)** | `regression.BackupCrypto...` | JVM | 25 | 25 | 0 | 0 | 100.0% | 18s |
| **Cat D (Reminders/Worker)**| `regression.AlarmScheduler...` | Connected (AVD) | 26 | 26 | 0 | 0 | 100.0% | 42s |
| **Cat E (ViewModels)** | `regression.ViewModel...` | Connected (AVD) | 21 | 21 | 0 | 0 | 100.0% | 29s |
| **Cat F (Compose UI/A11y)** | `regression.Compose...` | Connected (AVD) | 35 | 35 | 0 | 0 | 100.0% | 58s |
| **Cat G (E2E Journeys)** | `regression.FullApp...` | Connected (AVD) | 10 | 10 | 0 | 0 | 100.0% | 26s |
| **TOTALS** | **Codebase Total** | **JVM + AVD** | **287** | **287** | **0** | **0** | **100.0%** | **4m 25s** |

---

## 2. Test Execution Details by Group

### Group 1: JVM Unit Tests (136 Tests)
- **Command**: `.\gradlew.bat testDebugUnitTest`
- **Output Status**: `BUILD SUCCESSFUL in 1m 39s`
- **Tests Evaluated**:
  - `ScaledQuantityRegressionTest` (T001–T010): 10 passed
  - `StockLedgerRegressionTest` (T011–T022): 12 passed
  - `FefoLotAllocationRegressionTest` (T023–T030): 8 passed
  - `ScheduleOccurrenceRegressionTest` (T031–T038): 8 passed
  - `AdherenceCalculatorRegressionTest` (T039–T047): 9 passed
  - `PrescriptionManagerRegressionTest` (T048–T055): 8 passed
  - `BackupCryptoRegressionTest` (T086–T093): 8 passed
  - `BackupSerializationRegressionTest` (T094–T101): 8 passed
  - `BackupValidationRegressionTest` (T102–T106): 5 passed
  - `PrivacyAndManifestRegressionTest` (T107–T110): 4 passed
  - Baseline Domain Tests: 56 passed

### Group 2: Connected Instrumentation Tests on Android 17 / API 37 (151 Tests)
- **Command**: `.\gradlew.bat connectedDebugAndroidTest`
- **Output Status**: `BUILD SUCCESSFUL in 3m 46s`
- **Target Device**: `Medium_Phone_API_37.0(AVD) - 17` (`emulator-5554`)
- **Tests Evaluated**:
  - `RoomDaoRegressionTest` (T056–T065): 10 passed
  - `DatabaseInvariantsRegressionTest` (T066–T073): 8 passed
  - `RoomMigrationRegressionTest` (T074–T078): 5 passed
  - `RepositoryBehaviourRegressionTest` (T079–T085): 7 passed
  - `AlarmSchedulerRegressionTest` (T111–T117): 7 passed
  - `NotificationActionRegressionTest` (T118–T125): 8 passed
  - `SystemEventsRegressionTest` (T126–T130, T130b): 6 passed
  - `BackgroundWorkerRegressionTest` (T131–T135): 5 passed
  - `ViewModelRegressionTest` (T136–T155, T148b): 21 passed
  - `ComposeDashboardRegressionTest` (T156–T160): 5 passed
  - `ComposeMedicationListRegressionTest` (T161–T166): 6 passed
  - `ComposeMedicationDetailRegressionTest` (T167–T173): 7 passed
  - `ComposeCalendarRegressionTest` (T174–T178): 5 passed
  - `ComposePrescriptionRegressionTest` (T179–T182): 4 passed
  - `ComposeSettingsRegressionTest` (T183–T186): 4 passed
  - `ComposeAccessibilityRegressionTest` (T187–T190): 4 passed
  - `FullApplicationJourneyRegressionTest` (T191–T200): 10 passed
  - Baseline Room & Migration Connected Tests: 29 passed

---

## 3. Build & Static Analysis Verification

### Android Lint Analysis (`lintDebug`)
- **Command**: `.\gradlew.bat lintDebug`
- **Output Status**: `BUILD SUCCESSFUL in 1m 47s`
- **Results**:
  - `0 errors`, `0 fatal issues`.
  - Privacy checks: Verified `android.permission.INTERNET` is not present in merged manifest.
  - Accessibility checks: All composables comply with minimum touch targets and content descriptions.

### Release Build Assembly (`assembleRelease`)
- **Command**: `.\gradlew.bat assembleRelease`
- **Output Status**: `BUILD SUCCESSFUL`
- **Artifact**: `app/build/outputs/apk/release/app-release-unsigned.apk`
- **Compilation Diagnostics**: R8 minification, ProGuard rules, desugaring, and resource optimization completed with 0 warnings.
