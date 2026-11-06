# Novelo — Pre-Merge Testing Baseline Report

**Date:** 2026-11-06  
**Product:** Novelo – *Medication, made manageable.*  
**Package:** `com.example.nuvilo`  
**Working Branch:** `test/8-pre-merge-regression-200` (based on `feat/6-uiux-pro-max-implementation` @ `aaed023`)  
**Tracking Issue:** [Issue #8](https://github.com/nev3rfind/nuvilo/issues/8)  
**Execution Environment:** Android Studio JBR 17 (`C:\Program Files\Android\Android Studio\jbr`), Android SDK API 37, Gradle 9.6.0.

---

## 1. Baseline Test Execution Summary

Prior to authoring the 200+ pre-merge regression test campaign, the complete pre-existing test suites, static analysis, and binary assemblies were executed to establish a definitive baseline.

| Test / Verification Task | Command | Total Tests | Passed | Failed | Skipped | Duration | Result |
|---|---|---|---|---|---|---|---|
| **JVM Unit Tests** | `.\gradlew.bat testDebugUnitTest` | **56** | 56 | 0 | 0 | 2m 16s | **PASS** |
| **Connected Android Tests** | `.\gradlew.bat connectedDebugAndroidTest` | **29** | 29 | 0 | 0 | 1m 46s | **PASS** |
| **Android Lint** | `.\gradlew.bat lintDebug` | — | — | 0 errors | 0 warnings | 2m 09s | **PASS** |
| **Debug Build** | `.\gradlew.bat assembleDebug` | — | — | — | — | 31s | **PASS** |
| **Release Build** | `.\gradlew.bat assembleRelease` | — | — | — | — | 55s | **PASS** |

**Connected Device Specifications:**
- **Device Identifier:** `emulator-5554`
- **AVD Name:** `Medium_Phone_API_37.0`
- **Model:** Google `sdk_gphone64_x86_64`
- **Android Platform:** Android 17 / API Level 37

---

## 2. Pre-Existing Test Suite Breakdown

### 2.1 JVM Unit Tests (56 Tests across 11 Suites)
1. `com.example.nuvilo.data.backup.BackupCryptoTest` (3 tests):
   - `encryptAndDecrypt_roundTripSuccess`
   - `wrongPassphrase_throwsInvalidPassphraseOrCorruptedDataException`
   - `tamperedCiphertext_throwsException`
2. `com.example.nuvilo.data.backup.BackupSerializationTest` (4 tests):
   - `jsonBackup_serializationAndDeserialization_roundTripSuccess`
   - `csvZipBackup_exportAndImport_roundTripSuccess`
   - `backupValidator_rejectsOrphanSchedules`
   - `backupValidator_rejectsDuplicateMedicationIds`
3. `com.example.nuvilo.domain.adherence.AdherenceCalculatorTest` (7 tests):
   - `calculateAdherence_noScheduledDoses_returnsZeroAdherence`
   - `calculateAdherence_allDosesTaken_returns100Percent`
   - `calculateAdherence_assumedDoses_countedInNumeratorAndDenominator`
   - `calculateAdherence_skippedDoses_countedInDenominatorOnly`
   - `calculateAdherence_futureDoses_excludedFromCalculation`
   - `calculateAdherence_outsideDateRange_excluded`
   - `calculateAdherence_reversalCompensatingDose_correctsNumerator`
4. `com.example.nuvilo.domain.model.ScaledQuantityTest` (5 tests):
   - `fromScaled_preservesExactValue`
   - `fromInt_scalesBy1000`
   - `addition_computesExactSum`
   - `subtraction_computesExactDifference`
   - `comparison_ordersAccurately`
5. `com.example.nuvilo.domain.prescription.PrescriptionDomainTest` (5 tests):
   - `prescription_initialState_isActive`
   - `prescription_collection_decrementsRepeats`
   - `prescription_zeroRepeats_transitionsToCompleted`
   - `prescription_expiredDate_isIdentified`
   - `prescription_repeatRequest_tracksPendingStatus`
6. `com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculatorTest` (3 tests):
   - `dailySchedule_generatesOccurrencesForHorizon`
   - `specificWeekdays_generatesOnlyOnMatchingDays`
   - `deterministicOccurrenceId_isConsistentAcrossInvocations`
7. `com.example.nuvilo.domain.stock.StockLedgerCalculatorTest` (12 tests):
   - `openingBalance_createsInitialLedgerEntry`
   - `restock_incrementsRunningBalance`
   - `doseDeduction_decrementsRunningBalance`
   - `fefoAllocation_earliestExpiryFirst`
   - `fefoAllocation_tieBreakFifo`
   - `fefoAllocation_partialLotConsumption`
   - `fefoAllocation_multipleLots`
   - `compensatingReversal_restoresStock`
   - `insufficientStock_recordedWithoutCrashing`
   - `manualAdjustment_positiveDelta`
   - `manualAdjustment_negativeDelta`
   - `daysRemaining_calculationPrecision`
8. `com.example.nuvilo.ExampleUnitTest` (1 test):
   - `addition_isCorrect`
9. `com.example.nuvilo.security.AppLockManagerTest` (10 tests):
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
10. `com.example.nuvilo.system.notification.NotificationTextBuilderTest` (3 tests):
    - `buildReminderText_withSingleDose`
    - `buildReminderText_withLowStockAdvisory`
    - `buildReminderText_withPendingRepeatAdvisory`
11. `com.example.nuvilo.ui.components.NoveloUiHelpersTest` (3 tests):
    - `defaultStockUnitForForm_mapsEveryDosageFormToExpectedUnit`
    - `formatMinuteOfDay_formatsZeroTo2359Accurately`
    - `formatScheduleSummaryPill_formatsDailyAndWeekdaySchedulesCleanly`

### 2.2 Connected Android Tests (29 Tests across 6 Suites)
1. `com.example.nuvilo.ExampleInstrumentedTest` (1 test)
2. `com.example.nuvilo.data.BackupRestoreDatabaseTest` (3 tests)
3. `com.example.nuvilo.data.NoveloDatabaseMigrationTest` (1 test)
4. `com.example.nuvilo.data.NoveloDatabaseTest` (8 tests)
5. `com.example.nuvilo.system.NotificationAndSchedulerTest` (5 tests)
6. `com.example.nuvilo.ui.AccessibilityTest` (11 tests)

---

## 3. High-Risk Code Areas Identified for Pre-Merge Testing

Based on clinical safety invariants (`.agents/skills/novelo-medication-safety/SKILL.md`) and architectural boundaries, the following areas carry the highest risk:
1. **Scaled Integer Math & Precision Drift**: Risk of fractional pill quantities losing precision or encountering arithmetic overflow if boundaries are not strictly verified.
2. **Stock Ledger Immutability & Reversals**: Risk of duplicate stock deductions on rapid taps, incorrect FEFO lot allocations when multiple lots expire on the same day, or negative stock balances on manual adjustments.
3. **Deterministic Occurrence Generation & Timezones**: Risk of DST transitions (spring-forward/fall-back) duplicating or skipping scheduled dose occurrences.
4. **Adherence Numerator/Denominator Boundaries**: Risk of division-by-zero or incorrect counting of `AUTO_ASSUME` vs `CONFIRM` doses when doses are skipped or reversed.
5. **Database Referential Integrity & Rollbacks**: Ensuring cascade deletions or foreign key constraints never leave orphan schedule occurrences or ledger movements.
6. **Encrypted SAF Backup & Corrupted Payloads**: Guaranteeing that bit flips, truncated JSON, or altered GCM auth tags throw clean exceptions and trigger atomic transaction rollbacks without wiping user data.
7. **Exact Alarm Expiration & Fallback**: Confirming reminders fire under bounded horizons without exhausting OS alarm manager quotas.
