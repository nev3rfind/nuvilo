# NHS Prescription Prepayment Certificate (PPC) Test Report

## 1. Unit Test Suites Executed
Command executed:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.example.nuvilo.domain.ppc.PpcCalculatorTest" --tests "com.example.nuvilo.system.alarm.PpcAlarmPlannerTest" --tests "com.example.nuvilo.data.backup.PpcBackupCompatibilityTest" assembleDebug
```
Build result: **BUILD SUCCESSFUL** (0 errors).

| Test Suite | Tests | Passed | Skipped | Failures | Errors | Duration |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `com.example.nuvilo.domain.ppc.PpcCalculatorTest` | 8 | 8 | 0 | 0 | 0 | 0.028s |
| `com.example.nuvilo.system.alarm.PpcAlarmPlannerTest` | 4 | 4 | 0 | 0 | 0 | 0.058s |
| `com.example.nuvilo.data.backup.PpcBackupCompatibilityTest` | 4 | 4 | 0 | 0 | 0 | 0.188s |
| **Total** | **16** | **16** | **0** | **0** | **0** | **0.274s** |

---

## 2. Coverage Summary

### `PpcCalculatorTest` (8 tests)
- `calculateExpiryDate_3MonthPpc_standardAndYearTransition`: Verifies `2026-01-01 -> 2026-03-31` and `2026-11-15 -> 2027-02-14`.
- `calculateExpiryDate_3MonthPpc_monthEndAndLeapYear`: Verifies `2026-01-31 -> 2026-04-29`, `2023-11-30 -> 2024-02-28` (leap year), and `2025-11-30 -> 2026-02-27` (non-leap year).
- `calculateExpiryDate_12MonthPpc_standardAndLeapYear`: Verifies `2024-04-01 -> 2025-03-31`, `2023-03-01 -> 2024-02-29`, and `2024-02-29 -> 2025-02-27`.
- `calculateExpiryDate_manualOverrideAndOngoingExemption`: Verifies manual expiry override precedence and `null` expiry for ongoing `FREE_EXEMPTION`.
- `evaluateCertificate_allStatusesAndBoundaries`: Verifies `NO_RECORD`, `NOT_YET_VALID` (`daysUntilStart`), `ACTIVE` (31 days), `EXPIRING_SOON` (30-day and 0-day boundaries), `EXPIRED` (-1 day), ongoing exemptions, and `"Based on your recorded certificate dates"` basis notice.
- `selectPrimaryEvaluation_prioritizesActiveAndIgnoresArchived`: Verifies archived exclusion and status priority.
- `findOverlaps_detectsOverlapsAndAllowsBackToBack`: Verifies back-to-back non-overlapping dates and overlapping range detection.
- `buildRenewalDraft_startsDayAfterExpiryOrTodayIfExpired`: Verifies renewal draft start date and `renewedFromId` linkage.

### `PpcAlarmPlannerTest` (4 tests)
- `computeTriggers_schedulesConfiguredFutureOffsetsOnly`: Verifies configured 30/14/7/0 day offsets and omission of past triggers.
- `computeTriggers_archivedOrOngoing_returnsEmpty`: Verifies zero triggers for archived or ongoing certificates.
- `requestCodeFor_isDeterministicAndUniquePerCertificateAndOffset`: Verifies duplicate-free deterministic request codes.
- `buildNotificationContent_respectsPrivacyModeAndOfficialTerminology`: Verifies official `"Prescription Prepayment Certificate (PPC)"` wording, `"Based on your recorded certificate dates"` notice, absence of `"BPC"`, and lock-screen privacy masking.

### `PpcBackupCompatibilityTest` (4 tests)
- `jsonBackup_roundTripsPpcCertificates`: Verifies JSON serialization/deserialization of `PpcCertificateEntity` records.
- `csvZipBackup_roundTripsPpcCertificates`: Verifies CSV ZIP (`ppc_certificates.csv`) export/import round-trip.
- `jsonBackup_backwardCompatibleWithSchemaV4WithoutPpcArray`: Verifies v4 backups without `ppcCertificates` restore cleanly.
- `backupValidator_rejectsDuplicatePpcIdAndCountsPreview`: Verifies duplicate ID rejection and `BackupPreview.ppcCertificateCount`.
