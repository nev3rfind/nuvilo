# Phase 9 Domain Model & Stock Accounting Design

**Date:** 2026-11-12  
**Status:** Architecture Specification (Stage A)

---

## 1. Core Concept: Four-Layer Supply Chain

Nuvilo separates medication supply into four explicit concepts:

$$\text{Purchase Package} \longrightarrow \text{Package Contents} \longrightarrow \text{Administration Consumption} \longrightarrow \text{Usable Supply}$$

Each layer uses domain-appropriate units with **zero implicit medical conversions** (e.g., never silently converting `ml` to `drops`, or `tubes` to `applications`, unless the user explicitly configures package capacity in that consumption unit).

---

## 2. Domain Enums & Value Types

### 2.1 `StockUnit` (Extended)
Retains all existing enum values for full backward compatibility and adds real-world packaging/consumption units:
- `TABLETS("tab", "Tablets")`
- `CAPSULES("cap", "Capsules")`
- `ML("ml", "Millilitres (ml)")`
- `DROPS("drops", "Drops")`
- `CONTAINERS("cont", "Single-dose containers")` *(New)*
- `GRAMS("g", "Grams (g)")` *(New)*
- `PUFFS("puffs", "Puffs")`
- `APPLICATIONS("app", "Applications")`
- `UNITS("IU", "Units (IU)")`
- `PATCHES("patch", "Patches")`
- `BOTTLES("btl", "Bottles")`
- `OTHER("units", "Other units")`

### 2.2 `PackageType`
- `BOTTLE("Bottle")` — multi-dose eye/ear drops, oral liquid bottle
- `BOX_SINGLE_DOSE("Box of single-dose containers")` — box of single-use droppers/vials
- `SINGLE_DOSE_UNIT("Single-dose container")` — individual unboxed single-use containers
- `TUBE("Tube")` — ophthalmic gel/ointment or topical cream/gel
- `JAR("Jar")` — cream/ointment jar
- `BLISTER_PACK("Pack / Blister pack")` — tablets/capsules pack
- `INHALER("Inhaler")` — metered-dose or dry-powder inhaler
- `VIAL("Vial / Pen")` — injectable vial/pen
- `PATCH_BOX("Box of patches")`
- `CUSTOM("Package")`

### 2.3 `PackageStatus`
- `UNOPENED` — sealed; `openedAtEpochMs == null`; discard-after-opening timer has **not** started.
- `OPENED` — currently open/in use; `openedAtEpochMs != null`; discard-after-opening timer active if `discardAfterOpeningDays != null`.
- `EMPTY` — `remainingQuantity == ScaledQuantity.ZERO` (retained in history for auditability).
- `EXPIRED` — past manufacturer `expiryDateEpochMs` (never eligible for automatic dose allocation).
- `DISCARDED` — manually discarded (contaminated, damaged, expired after opening); has `discardedAtEpochMs` and `discardReason`; remaining balance offset by an immutable `WASTAGE` ledger movement.

### 2.4 `AdministrationSite` & `GelTrackingMode` & `TrackingPrecision`
- `AdministrationSite`: `NONE`, `LEFT_EYE`, `RIGHT_EYE`, `BOTH_EYES`, `CUSTOM`
- `GelTrackingMode`: `GRAMS`, `ESTIMATED_APPLICATIONS`, `TUBE_COUNT_ONLY`
- `TrackingPrecision`: `EXACT`, `ESTIMATED`, `APPROXIMATE`, `UNKNOWN`

---

## 3. Domain Model Extensions

### 3.1 `PackagingConfig` (on `Medication`)
```kotlin
data class PackagingConfig(
    val packageType: PackageType = PackageType.CUSTOM,
    val consumptionUnit: StockUnit = StockUnit.TABLETS,
    val contentsPerPackage: ScaledQuantity? = null,      // e.g. 300 drops/bottle, 200 containers/box, 40 app/tube
    val netContentLabel: String? = null,               // Optional label metadata e.g. "10 ml", "5 g" (never used for fake unit math)
    val discardAfterOpeningDays: Int? = null,          // e.g. 28 days for multi-dose eye drops
    val trackingPrecision: TrackingPrecision = TrackingPrecision.EXACT,
    val gelTrackingMode: GelTrackingMode? = null
)
```

### 3.2 `AdministrationProfile` (on `Medication`)
```kotlin
data class AdministrationProfile(
    val site: AdministrationSite = AdministrationSite.NONE,
    val leftEyeQuantity: ScaledQuantity? = null,               // e.g. 1 drop in left eye
    val rightEyeQuantity: ScaledQuantity? = null,              // e.g. 1 drop in right eye
    val containersPerAdministration: ScaledQuantity? = null    // Explicit decoupler for single-dose eye drops!
)
```

### 3.3 `StockLot` (Package Instance)
Extended non-destructively with package lifecycle metadata:
- `packageType: PackageType?`
- `packageLabel: String?` (e.g., `"Bottle #1"`, `"Box #2"`)
- `packageStatus: PackageStatus` (`UNOPENED` by default for newly added packages; `OPENED` / `EMPTY` inferred during legacy migration only where appropriate)
- `openedAtEpochMs: Long?`
- `discardAfterOpeningDays: Int?`
- `discardAfterOpeningEpochMs: Long?`
- `discardedAtEpochMs: Long?`
- `discardReason: String?`

Note: The sum of `remainingQuantity` across active/open/unopened lots must reconcile with ledger balance once package-tracked. Legacy medications with pre-v4 opening balances that have zero lots retain `TrackingPrecision.UNKNOWN` package breakdown while preserving exact ledger arithmetic.

---

## 4. Consumption & Unit Integrity Rules

### 4.1 `ConsumptionCalculator.consumptionPerOccurrence(...)`
1. **Single-dose ophthalmic / multi-site box (`BOX_SINGLE_DOSE` or `SINGLE_DOSE_UNIT`, `unit == CONTAINERS`)**:
   - Uses `administrationProfile.containersPerAdministration ?: schedule.doseQuantityPerOccurrence`.
   - Treated eyes (`LEFT_EYE`, `RIGHT_EYE`, `BOTH_EYES`) remain visible in clinical instructions (e.g. `"1 container across both eyes"` or `"2 containers (1 per eye)"`), **never multiplying container deduction automatically** just because `BOTH_EYES` is selected!
2. **Multi-dose ophthalmic drops (`DROPS`)**:
   - `LEFT_EYE` -> `leftEyeQuantity ?: schedule.doseQuantityPerOccurrence`
   - `RIGHT_EYE` -> `rightEyeQuantity ?: schedule.doseQuantityPerOccurrence`
   - `BOTH_EYES` -> `(leftEyeQuantity ?: 1) + (rightEyeQuantity ?: 1)` (supports asymmetric regimens, e.g., `2` drops Left + `1` drop Right = `3` drops/admin)
   - `NONE`/`CUSTOM` -> `schedule.doseQuantityPerOccurrence`
3. **Ophthalmic Gel / Ointment (`TOPICAL_CREAM`)**:
   - `ESTIMATED_APPLICATIONS`: consumes `schedule.doseQuantityPerOccurrence` in `APPLICATIONS`, `TrackingPrecision.ESTIMATED`.
   - `GRAMS`: consumes `schedule.doseQuantityPerOccurrence` in `GRAMS`.
   - `TUBE_COUNT_ONLY`: tracked primarily at package open/empty/discard level (`0` per-dose deduction unless specified), `TrackingPrecision.UNKNOWN` for per-application runway.
4. **Standard forms (`TABLET`, `CAPSULE`, `LIQUID_ML`, `INHALER_PUFFS`, `PATCH`, `INJECTION`, `OTHER`)**:
   - Uses `schedule.doseQuantityPerOccurrence` in `consumptionUnit`.

### 4.2 Dimensional Integrity (`UnitIntegrityValidator`)
- Rejects unconfigured unit mismatches (`BOTTLES` vs `DROPS`, `ML` vs `DROPS`, `TUBES` vs `APPLICATIONS`).
- Adding $N$ sealed packages of configured capacity $C$ adds $N$ discrete `StockLot`s (each initialized with `initialQuantity = C`, `remainingQuantity = C`, `packageStatus = UNOPENED`) alongside a single auditable `RESTOCK` ledger movement of $N \times C$ in the medication's `consumptionUnit`.
- If a user purchases an unmeasured unit (`ML` bottle without drop count configured) and chooses `DROPS` consumption, UI requires user-provided drops-per-bottle OR switches precision to `APPROXIMATE`/`UNKNOWN` rather than inventing `20 drops/ml`.

---

## 5. FEFO Package Lifecycle & Atomic Operations

1. **Eligible Packages for Consumption**:
   - `status in {OPENED, UNOPENED}` AND `remainingQuantity > 0`
   - AND `(expiryDateEpochMs == null || expiryDateEpochMs > nowEpochMs)`
   - AND `(status != OPENED || discardAfterOpeningEpochMs == null || discardAfterOpeningEpochMs > nowEpochMs)`
2. **Consumption Order**:
   - First consume from already-`OPENED` eligible packages (ordered by earliest `discardAfterOpeningEpochMs ?: Long.MAX_VALUE`, then `expiryDateEpochMs ?: Long.MAX_VALUE`, then `createdAtEpochMs`).
   - Next consume from `UNOPENED` eligible packages in FEFO order (`expiryDateEpochMs ?: Long.MAX_VALUE`, then `createdAtEpochMs`). When an `UNOPENED` package is tapped for consumption, transition `UNOPENED -> OPENED`, stamp `openedAtEpochMs = nowEpochMs`, and compute `discardAfterOpeningEpochMs = discardAfterOpeningDays?.let { nowEpochMs + it * 86_400_000L }`.
   - When `remainingQuantity` reaches `0`, transition status to `EMPTY`.
3. **Dose Reversal (`correctDose`)**:
   - Restores the reversed quantity using the original ledger `lotId` (falling back safely if already deleted/repacked), appending `DOSE_REVERSAL` and reopening an `EMPTY` package to `OPENED` if appropriate.
4. **Scoped Opened-Package Recount (`recountPackage`)**:
   - Operates strictly on the selected `OPENED` package (`lotId`).
   - Computes exact delta = `targetRemaining - package.remainingQuantity`, writes `ADJUSTMENT_IN` or `ADJUSTMENT_OUT` anchored to `lotId`, and updates only that package (`EMPTY` if `targetRemaining == 0`). Unopened packages are **never** mutated.
5. **Manual Discard (`discardPackage`)**:
   - Sets `packageStatus = DISCARDED`, `discardedAtEpochMs = nowEpochMs`, `discardReason = reason`, appends a `WASTAGE` ledger movement for the exact remaining balance (`remainingQuantity`), and zeros `remainingQuantity` so usable inventory and ledger match cleanly.

---

## 6. Shelf-Life-Aware Supply Forecasting (`SupplyForecastCalculator`)

Simple division `totalBalance / dailyRate` overstates runway when a bottle expires 28 days after opening or contains doses past its expiry date.

Phase 9 replaces naive division with a deterministic discrete day-by-day FEFO simulation (`Long` thousandths only, zero `Float`/`Double`):
- Simulates day `d = 0, 1, 2, ...` from `today`:
  - Drops any package whose `expiryDateEpochMs` has elapsed.
  - Drops the currently `OPENED` package once `discardAfterOpeningEpochMs` has elapsed (counting unused drops as `unitsLostToDiscardOrExpiry` rather than carrying them forward!).
  - Deducts `dailyScheduledConsumption(date)` using scheduled weekdays / active schedules.
  - Opens the next `UNOPENED` FEFO package whenever needed and starts *its* `discardAfterOpening` clock on that simulated day!
- Outputs `SupplyForecast`:
  - `totalUsableQuantity: ScaledQuantity`
  - `daysOfSupply: Int?`
  - `expectedRunoutEpochDay: Long?`
  - `unitsExpectedToExpireUnused: ScaledQuantity`
  - `precision: TrackingPrecision`
  - `openedPackagesCount: Int`, `unopenedPackagesCount: Int`
  - `soonestExpiryEpochMs: Long?`, `soonestDiscardAfterOpeningEpochMs: Long?`
  - `expiredOrWastedActiveLotsCount: Int`
