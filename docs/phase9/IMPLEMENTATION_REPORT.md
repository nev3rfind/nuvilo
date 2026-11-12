# Phase 9: Intelligent Medication Consumption & Real-World Inventory Implementation Report

**Product:** Nuvilo ("*Medication, made manageable.*")  
**Package:** `com.nuvilomed.app` (App ID) / `com.example.nuvilo` (Codebase package)  
**Branch:** `feat/16-intelligent-medication-inventory`  
**GitHub Issue:** [#16](https://github.com/nev3rfind/nuvilo/issues/16)  
**Date:** 2026-11-12  
**Status:** Completed & Verified  

---

## 1. Executive Summary

Phase 9 fundamentally upgrades Nuvilo's domain and inventory model from naive single-quantity counters to a clinical, real-world packaging and consumption architecture. Previously, medications such as eye drops were stored in units like millilitres or drops, and recounts forced users to estimate entire inventory balances rather than counting individual packages.

Under Phase 9, Nuvilo cleanly separates:
1. **What the user purchases and stores**: discrete packages (`PackageType`: multi-dose bottles, single-dose container boxes, blister packs, tubes, inhalers, vials).
2. **Package capacity**: usable contents per sealed package (e.g., 300 drops per 10ml bottle, 30 single-dose containers per box, 40 applications per 5g tube).
3. **Administration consumption**: clinical consumption rules (`AdministrationProfile`: e.g., 1 container across both eyes, or 1 drop per eye = 2 drops per administration).
4. **Package lifecycle & shelf-life**: discrete package states (`UNOPENED`, `OPENED`, `EMPTY`, `EXPIRED`, `DISCARDED`) with automatic start of discard-after-opening countdown timers (e.g., 28 days for multi-dose ophthalmic drops).
5. **Discrete FEFO supply forecasting**: day-by-day simulation computing exact usable runway and expiring units without any floating-point arithmetic.
6. **Haptic-first feedback & 7 notification channels**: centralized vibration orchestrator and fine-grained Android notification categories defaulting to silent + vibration for medication reminders.

---

## 2. Core Architectural Components

### 2.1 Domain Model & Value Objects (`PackagingModels.kt`)
- `PackageType`: `BOTTLE`, `BOX_SINGLE_DOSE`, `SINGLE_DOSE_UNIT`, `TUBE`, `JAR`, `BLISTER_PACK`, `INHALER`, `VIAL`, `PATCH_BOX`, `CUSTOM`.
- `PackageStatus`: `UNOPENED`, `OPENED`, `EMPTY`, `EXPIRED`, `DISCARDED`.
- `AdministrationSite`: `NONE`, `LEFT_EYE`, `RIGHT_EYE`, `BOTH_EYES`, `CUSTOM`.
- `TrackingPrecision`: `EXACT`, `ESTIMATED`, `APPROXIMATE`, `UNKNOWN`.
- `GelTrackingMode`: `GRAMS`, `ESTIMATED_APPLICATIONS`, `TUBE_COUNT_ONLY`.
- `PackagingConfig`: embeds `packageType`, `consumptionUnit`, `contentsPerPackage`, `netContentLabel`, `discardAfterOpeningDays`, `trackingPrecision`, and `gelTrackingMode`.
- `AdministrationProfile`: embeds `site`, `leftEyeQuantity`, `rightEyeQuantity`, and `containersPerAdministration`.
- `StockLot`: extended with package lifecycle metadata (`packageType`, `packageLabel`, `packageStatus`, `openedAtEpochMs`, `discardAfterOpeningDays`, `discardAfterOpeningEpochMs`, `discardedAtEpochMs`, `discardReason`).

### 2.2 Consumption & Dimensional Integrity (`ConsumptionCalculator.kt`, `UnitIntegrityValidator.kt`)
- Decouples clinical treated eyes from physical package consumption: for single-dose containers, selecting `BOTH_EYES` records clinical instructions while deducting exactly 1 container (or the user-configured container count), avoiding erroneous 2x deductions.
- Multi-dose eye drops calculate per-dose consumption as `(leftEyeQuantity ?: 1) + (rightEyeQuantity ?: 1)` when `BOTH_EYES` is selected, fully supporting asymmetric regimens (e.g., 2 drops left, 1 drop right = 3 drops total).
- Rejects unconfigured cross-unit operations (e.g. subtracting `DROPS` from `BOTTLES` without explicit capacity configuration).

### 2.3 Discrete FEFO Simulation (`SupplyForecastCalculator.kt`)
- Replaces naive `totalBalance / dailyRate` division.
- Simulates future calendar days step-by-step:
  - Tracks the currently `OPENED` package and discards it when its `discardAfterOpeningEpochMs` is reached (logging remaining drops as `unitsLostToDiscardOrExpiry`).
  - Automatically simulates opening the next `UNOPENED` package in FEFO order when needed and starts *its* discard timer on that simulated day.
  - Excludes any lot past its manufacturer `expiryDateEpochMs`.
  - Uses exact integer thousandths (`ScaledQuantity`, `Long`); **zero `Float` or `Double` arithmetic**.

### 2.4 Scoped Opened-Package Operations & Auditability (`NoveloRepositoryImpl.kt`)
- **Auto-Open on First Dose**: Taking a dose for a medication with only `UNOPENED` packages automatically transitions the earliest FEFO lot to `OPENED`, records `openedAtEpochMs = now`, and sets `discardAfterOpeningEpochMs = now + days * 86_400_000L`.
- **Scoped Recount**: `recountPackage(lotId, targetRemaining, reason)` adjusts strictly the selected opened package, writing compensating `ADJUSTMENT_IN` or `ADJUSTMENT_OUT` ledger entries. Unopened packages remain completely untouched.
- **Manual Discard**: `discardPackage(lotId, reason)` transitions the lot to `DISCARDED`, writes off remaining balance with a compensating `WASTAGE` ledger entry, and zeros active lot balance.
- **Dose Reversals**: `correctDose` safely restores reversed doses to the original lot, reopening an `EMPTY` lot if needed.

### 2.5 Room Database v4 & Data Migration (`NoveloDatabase.kt`, `MIGRATION_3_4`)
- Incremented `DATABASE_VERSION` from 3 to 4.
- Added package columns to `medications` table: `package_type`, `consumption_unit`, `contents_per_package`, `net_content_label`, `discard_after_opening_days`, `tracking_precision`, `gel_tracking_mode`, `admin_site`, `left_eye_quantity`, `right_eye_quantity`, `containers_per_admin`.
- Added package lifecycle columns to `stock_lots` table: `package_type`, `package_label`, `package_status`, `opened_at_epoch_ms`, `discard_after_opening_days`, `discard_after_opening_epoch_ms`, `discarded_at_epoch_ms`, `discard_reason`.
- Migration `MIGRATION_3_4` preserves 100% of existing medications, schedules, doses, stock lots, and prescriptions, automatically inferring `packageStatus` (`EMPTY` for 0 remaining, `OPENED` for oldest active, `UNOPENED` for remaining).
- Automated migration verification passes in `NoveloDatabaseMigrationTest.kt`.

### 2.6 Backup & Restore Compatibility (`BackupRestoreService.kt`, `BackupDataV4`)
- Added `BackupDataV4` with full support for `PackagingConfigDto`, `AdministrationProfileDto`, and package lot lifecycle fields.
- Backward compatibility: seamlessly imports v1, v2, and v3 backups, applying default package configs without data loss.
- Forward compatibility: v4 exports validate schema and referential integrity with PBKDF2 + AES-256-GCM encryption.

### 2.7 Haptic Feedback & Notification Channels (`NuviloHaptics.kt`, `NotificationChannelRegistry.kt`)
- `NuviloHapticController`: centralized haptic interface with fallback for devices lacking amplitude control or vibration hardware.
- Haptic events: `DOSE_TAKEN`, `DOSE_SNOOZED`, `DOSE_SKIPPED`, `SELECTION_TICK`, `PACKAGE_OPENED`, `PACKAGE_DISCARDED`, `WARNING_ALERT`, `ERROR_BUZZ`.
- Strength levels: `HapticIntensity.LOW` (Gentle), `HapticIntensity.MEDIUM` (Balanced), `HapticIntensity.HIGH` (Distinct).
- Registered 7 granular Android notification channels:
  1. `medication_reminders` (Urgent / High importance, Silent · Vibrate default)
  2. `urgent_dose_alerts` (Critical adherence reminders)
  3. `critical_low_stock` (Low inventory alerts)
  4. `restock_followups` (Prescription / refill reminders)
  5. `batch_expiry` (Shelf-life and expiry notifications)
  6. `refill_prescriptions` (Prescription repeat tracking)
  7. `security_backups` (Local backup reminders and security notices)

---

## 3. UI/UX Implementations & Key Screens

1. **Add Medication 3-Step Wizard** (`MedicationListScreen.kt`):
   - Step 1: Form and Package Type selection (e.g. Eye Drops -> Bottle or Box of single-dose containers).
   - Step 2: Treated site configuration (Left eye, Right eye, Both eyes, Custom site) with single-dose container decoupling and clear explanatory copy.
   - Step 3: Stock configuration in purchase packages with immediate live calculation of total drops/units, expected supply runway, and after-opening discard limit.
2. **Medication Detail Overview** (`MedicationDetailScreen.kt`):
   - Package overview showing count of open vs sealed packages.
   - Active opened bottle progress card displaying remaining drops, capacity, and remaining shelf-life days.
   - Quick action buttons: `+ Add Bottles`, `Recount Open Bottle`, `Open Next Bottle`, `Discard Open Bottle`.
   - Batch lots & FEFO allocation card showing discrete package badges (`OPENED`, `UNOPENED`, `EMPTY`, `DISCARDED`) and expiry dates.
3. **Scoped Dialogs**:
   - `RecountOpenedPackageDialog`: allows adjusting observed drops in the active opened package with preset reason chips ("Physical recount", "Extra drop or spill", "Nearly empty").
   - `DiscardPackageDialog`: shows wastage warning (-WASTAGE ledger entry), remaining drops to be written off, and reason chips ("Opened past discard window", "Contaminated / damaged", "Expired", "Finished early").
4. **Settings Screen** (`SettingsScreen.kt`):
   - Haptic & Tactile Feedback card with enable toggle, intensity chips (Gentle, Balanced, Distinct), and "Test Haptic Feel" button.
   - Notification Categories card displaying the 7 channels with current OS settings and quick link to system channel settings.

---

## 4. Verification Evidence

| Test Suite | Tests Run | Failures | Skipped | Status |
|---|---|---|---|---|
| `connectedDebugAndroidTest` (full suite on Pixel 10 Pro, API 37) | **159** | **0** | **0** | **PASS (100%)** |
| `Phase9RepositoryFlowTest` | 1 | 0 | 0 | **PASS** |
| `Phase9UiAndAccessibilityTest` | 5 | 0 | 0 | **PASS** |
| `NoveloDatabaseMigrationTest` | 2 | 0 | 0 | **PASS** |
| `FullApplicationJourneyRegressionTest` | 10 | 0 | 0 | **PASS** |
| `NuviloCampaignSeedTest` | 1 | 0 | 0 | **PASS** |
| Zero-`INTERNET` Merged Manifest Check | 1 | 0 | 0 | **PASS** |
| Gradle Compilation (`assembleDebug`, `assembleRelease`) | 2 | 0 | 0 | **PASS** |

All interactive elements satisfy WCAG AA touch targets ($\ge 48\text{dp}$), high-contrast text ratios, and both Light and Dark theme verification.
