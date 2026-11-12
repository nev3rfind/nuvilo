# Phase 9 Baseline Audit

**Date:** 2026-11-12  
**Branch:** `feat/16-intelligent-medication-inventory` (from `main` @ `10ee0c0`)  
**Issue:** [#16](https://github.com/nev3rfind/nuvilo/issues/16)

---

## 1. Executive Summary

This baseline audit documents the pre-Phase-9 architecture, domain limitations, UX gaps, notification behavior, and safety invariants across Nuvilo (`com.nuvilomed.app` / `com.example.nuvilo`). While existing phases (Phases 0–8 + Pro Max + Pre-Merge QA with 287 passing tests) established strong ledger accounting (`ScaledQuantity`), deterministic dose occurrence IDs (`occ_<medId>_<epochMs>`), offline privacy guarantees (zero `INTERNET`), and encrypted backups, the domain model treats all medications as flat single-unit counters.

---

## 2. Verified Current Implementation

### 2.1 Domain Layer (`app/src/main/java/com/example/nuvilo/domain/`)
- **Arithmetic & Units** (`model/ScaledQuantity.kt`, `model/Enums.kt`):
  - `ScaledQuantity` uses `Long` scaled by `SCALE = 1_000L` (thousandths) — zero `Float`/`Double` in accounting.
  - `MedicationForm` (`TABLET`, `CAPSULE`, `LIQUID_ML`, `DROPS`, `INHALER_PUFFS`, `TOPICAL_CREAM`, `INJECTION`, `PATCH`, `OTHER`) carries `isEstimatedConsumption: Boolean`.
  - `StockUnit` (`TABLETS`, `CAPSULES`, `ML`, `DROPS`, `PUFFS`, `APPLICATIONS`, `UNITS`, `PATCHES`, `BOTTLES`, `OTHER`). Note: no `CONTAINERS` or `GRAMS` enum entries exist yet.
- **Medication & Lot Models** (`model/DomainModels.kt`):
  - `Medication` stores a single `stockUnit`, `openingStock`, `lowStockThreshold`, `recordingMode`. There is no purchase-package definition, usable contents per package, discard-after-opening rule, administration site (`LEFT_EYE`/`RIGHT_EYE`/`BOTH_EYES`), or precision indicator beyond `MedicationForm.isEstimatedConsumption`.
  - `StockLot` stores `id`, `medicationId`, `lotNumber`, `initialQuantity`, `remainingQuantity`, `unit`, `expiryDateEpochMs`, `createdAtEpochMs`, `notes`. There is no package status (`UNOPENED`/`OPENED`/`EMPTY`/`EXPIRED`/`DISCARDED`), no `openedAtEpochMs`, and no `discardAfterOpeningEpochMs`.
- **Ledger & FEFO Engine** (`stock/StockLedgerCalculator.kt`):
  - `calculateBalance()` sums `OPENING_BALANCE`, `RESTOCK`, `ADJUSTMENT_IN`, `DOSE_REVERSAL` inflows minus `DOSE_TAKEN`, `DOSE_ASSUMED`, `ADJUSTMENT_OUT`, `WASTAGE` outflows, and enforces single unit (`IncompatibleUnitsException`) and single net deduction per `occurrenceId` (`DuplicateDoseDeductionException`).
  - `allocateDoseAgainstLots()` sorts `StockLot`s by `(expiryDateEpochMs ?: Long.MAX_VALUE, createdAtEpochMs)`.
  - **Gap:** Does not exclude expired lots (`expiryDateEpochMs <= now`), does not distinguish `OPENED` vs `UNOPENED` packages, does not track or enforce `discardAfterOpening`, and computes `daysRemaining = currentBalance / dailyConsumptionRate` over the entire balance without simulating expiry or discard-after-opening cutoff dates.

### 2.2 Data & Persistence Layer (`app/src/main/java/com/example/nuvilo/data/`)
- **Room Database** (`local/NoveloDatabase.kt`):
  - Current `version = 3`, `exportSchema = true` (`app/schemas/1.json`, `2.json`, `3.json`).
  - 7 entities in `Entities.kt`: `MedicationEntity`, `ScheduleEntity`, `DoseEventEntity`, `StockLotEntity`, `StockLedgerEntryEntity`, `PrescriptionEntity`, `PrescriptionEventEntity`.
- **Repository** (`repository/NoveloRepository.kt`):
  - `createMedication()` records `OPENING_BALANCE` in `stock_ledger_entries` without creating a corresponding `StockLotEntity`.
  - `recordDose()` deducts via `allocateDoseAgainstLots()`, writes `DoseEventEntity` + `StockLedgerEntryEntity`, and updates `StockLotDao`. **Gap:** hardcodes `isEstimate = false` on ledger entries; does not set `openedAtEpochMs` or transition `UNOPENED -> OPENED`.
  - `adjustStockToTarget()` modifies total stock and optionally adjusts one selected `lotId`. **Gap:** manual recount allows editing total stock instead of scoping strictly to an opened bottle/package recount.
  - `correctDose()` restores reversed quantity to `firstOrNull { remaining < initial } ?: firstOrNull()` rather than the originally debited `lotId`.
  - `observeStockBalance(med)` passes `dailyConsumptionRate = null` (so `LowStockCheckWorker` receives `daysRemaining = null`).
- **Backup & Export** (`data/backup/`):
  - `NoveloDataBackup` (`CURRENT_FORMAT_VERSION = 1`, `CURRENT_SCHEMA_VERSION = 3`).
  - `JsonBackupManager` + `BackupCrypto` (AES-256-GCM / PBKDF2) + `CsvZipBackupManager` + `BackupValidator` + `BackupRestoreRepositoryImpl`. Any schema v4 additions must stay backward-compatible with v3 backups and round-trip v4 backups accurately.

### 2.3 UI & Wizard Workflows (`ui/screens/`, `ui/viewmodel/`)
- **Add Medication Wizard** (`MedicationListScreen.kt`):
  - 3 generic steps (Identity & Form, Schedule & Mode, Opening Stock). Allows selecting `DROPS` with `ML` or `BOTTLES` or `DROPS` with no guided separation between "how purchased" vs "package contents" vs "dose consumed", and no Left/Right/Both eyes selector or single-dose container vs eyes decoupling.
- **Medication Detail & Stock UI** (`MedicationDetailScreen.kt`):
  - Hero card displays a single aggregate balance (`28 tablets`, `10 ml`, etc.) and `ESTIMATED BALANCE` pill.
  - `RestockDialog` prompts for raw quantity in `medication.stockUnit` (`+30 drops`, `+100 ml`, etc.) rather than `+ N bottles/boxes/tubes of X usable units`.
  - `AdjustStockDialog` prompts for *total* observed balance in consumption units instead of *remaining amount in currently opened package*.
  - No UI actions for opening a sealed bottle, discarding an expired/contaminated bottle, or viewing opened vs unopened packages separately.

### 2.4 Notifications & Haptics (`system/`, `data/preferences/`, `ui/`)
- **Notification Channels** (`NotificationHelper.kt`):
  - Only 2 channels exist: `novelo_medication_reminders` (`IMPORTANCE_HIGH` with system default sound) and `novelo_low_stock_alerts` (`IMPORTANCE_DEFAULT`).
  - Missing channels for: Upcoming reminders, Expiry warnings, Opened-package discard warnings, Prescription reminders, and Security events.
  - Medication reminders are not configured as silent-with-vibration by default.
- **Maintenance Worker** (`LowStockCheckWorker.kt`):
  - Evaluates low stock every 12 hours using `lowStockNotifiedMedIds` deduplication, but does not notify on package expiry or opened-package discard-after-opening limits.
- **Haptics**:
  - `LocalHapticFeedback` is imported in `NoveloComponents.kt`, with no central haptic controller, no semantic haptic feedback mapping across actions, and no DataStore haptic settings (enable/disable, intensity, test haptic).

---

## 3. Preserved Mandatory Invariants

All existing safety and architecture constraints remain unchanged:
1. Strict offline-first (`INTERNET` absent, `ACCESS_NETWORK_STATE` removed).
2. Zero `Float`/`Double` arithmetic for quantities or stock balances; all quantities in `ScaledQuantity` (`Long` thousandths).
3. Immutable append-only `stock_ledger_entries` remains the authoritative accounting truth; package balances reconcile with ledger movements.
4. Deterministic `occurrenceId`, idempotent dose recording, no deductions on `SKIPPED`/`MISSED`/snooze, compensating `DOSE_REVERSAL`.
5. `CONFIRM` vs `AUTO_ASSUME` distinction preserved everywhere.
6. Non-destructive Room migration (`3 -> 4`) preserving all existing medications, schedules, doses, lots, ledger entries, and prescriptions.
