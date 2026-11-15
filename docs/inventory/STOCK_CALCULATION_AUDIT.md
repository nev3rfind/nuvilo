# Nuvilo Stock Calculation & Mathematics Audit
**Phase 10 Formal Domain Audit**  
**Author:** Lead Architect & Domain Auditor  
**Status:** Approved & Verified  
**Date:** 2026-11-14  

---

## 1. Mathematical Foundation: Zero Floating-Point Arithmetic

Medical inventory calculations cannot tolerate floating-point imprecision (such as IEEE 754 rounding artifacts like `0.1 + 0.2 = 0.30000000000000004`). In Nuvilo:
- **`ScaledQuantity`** represents every medication balance, dose quantity, package capacity, and movement.
- **Fixed-point integer representation**: Internally stored as a 64-bit integer (`rawUnits`) scaled by $10^3$ ($1,000$ raw units = $1.000$ display unit).
- **Zero Floats/Doubles**: Under no circumstances is `Float` or `Double` utilized in ledger math, balance summation, or package deductions.

```kotlin
@JvmInline
value class ScaledQuantity(val rawUnits: Long) : Comparable<ScaledQuantity> {
    operator fun plus(other: ScaledQuantity): ScaledQuantity = ScaledQuantity(rawUnits + other.rawUnits)
    operator fun minus(other: ScaledQuantity): ScaledQuantity = ScaledQuantity(rawUnits - other.rawUnits)
    operator fun times(multiplier: Long): ScaledQuantity = ScaledQuantity(rawUnits * multiplier)
    operator fun div(divisor: Long): ScaledQuantity = ScaledQuantity(rawUnits / divisor)
    
    companion object {
        const val SCALE: Long = 1_000L
        val ZERO = ScaledQuantity(0L)
        fun fromWholeUnits(units: Long): ScaledQuantity = ScaledQuantity(units * SCALE)
        fun fromFractional(units: Double): ScaledQuantity = ScaledQuantity((units * SCALE).roundToLong())
    }
}
```

---

## 2. Inventory Metrics Definitions

The system derives 5 distinct inventory metrics for every tracked medication:

| Metric | Formula | Clinical Meaning |
|---|---|---|
| **Opened Package Balance** | $\sum_{\text{lot} \in \text{OPENED}} \text{lot.remainingQuantity}$ | Medication immediately accessible without opening new packages. |
| **Unopened Package Count** | $\text{count}(\text{lot} \in \text{UNOPENED})$ | Discrete sealed containers stored in reserve. |
| **Total Physical Packages** | $\text{openedCount} + \text{unopenedCount}$ | Total number of physical boxes/bottles in the medicine cabinet. |
| **Total Usable Supply** | $\text{openedBalance} + \sum_{\text{lot} \in \text{UNOPENED}} \text{lot.remainingQuantity}$ | Cumulative usable units across all intact, unexpired packages. |
| **Estimated Days of Supply** | $\lfloor \frac{\text{Total Usable Supply}}{\text{Daily Consumption Rate}} \rfloor$ | Number of calendar days before all inventory is completely exhausted. |

### Invariant: Physical Package Conservation
Opening an unopened package:
- Decrements `unopenedCount` by $1$.
- Increments `openedCount` by $1$.
- Leaves `totalPhysicalPackages` unchanged:
  $$\Delta (\text{openedCount} + \text{unopenedCount}) = +1 - 1 = 0$$

---

## 3. Daily Consumption Rate Derivation

The daily consumption rate is calculated deterministically from active schedules:

$$\text{Daily Consumption} = \sum_{\text{rule} \in \text{ActiveSchedules}} \left( \text{DosesPerDay}(\text{rule}) \times \text{QuantityPerDose} \right)$$

### Example (Hydro Forte Scenario):
- Regimen: 1 drop in both eyes, 5 times daily.
- Quantity per dose: $1 \times 2 = 2\text{ drops}$.
- Doses per day: $5$.
- Daily consumption rate: $5 \times 2 = 10\text{ drops/day}$ ($10,000$ raw units).
- Initial supply: 7 bottles $\times 300\text{ drops} = 2,100\text{ drops}$.
- Initial days of supply: $\lfloor 2,100 / 10 \rfloor = 210\text{ days}$.

---

## 4. FEFO Lot Allocation & Consumption Isolation

When a dose is recorded:
1. **Target Lot Selection**:
   - The system queries for lots where `packageStatus == PackageStatus.OPENED`.
   - If a package type requires explicit unsealing (`packageType.requiresExplicitOpeningConfirmation == true`), **unopened lots are excluded**.
   - If no opened package is available, the dose deduction is halted or flagged, preventing silent automated unsealing of reserve containers.
2. **Exhaustion Handling**:
   - If a dose of $2$ drops is taken from an opened package with exactly $2$ drops remaining:
     - The package's `remainingQuantity` becomes $0$.
     - `packageStatus` transitions to `PackageStatus.EMPTY`.
     - `depletedAtEpochMs` is stamped.
     - `findStagedNextPackage(lots)` selects the earliest expiring `UNOPENED` lot via FEFO (`expiryDateEpochMs ASC, receivedDateEpochMs ASC`).
     - The UI transitions to the "Ready to Open" state for the staged bottle.

---

## 5. Early Depletion & Wastage Mathematics

Real-world containers often run dry before nominal capacity is exhausted.

### Scenario:
- Nominal bottle capacity: $300\text{ drops}$.
- Doses administered: $120\text{ doses} \times 2\text{ drops} = 240\text{ drops}$.
- Calculated remaining: $60\text{ drops}$.
- Patient physical state: Bottle is empty (lost drops, leakage).

### Transactional Ledger Adjustment:
1. Deduct remaining $60\text{ drops}$ as:
   - `MovementType.WASTAGE`
   - `quantity = ScaledQuantity(60_000L)`
   - `isEstimate = true`
   - `doseOccurrenceId = null` (no dose event!)
   - `notes = "Marked finished early: BOTTLE_EMPTY"`
2. Set `lot.remainingQuantity = ScaledQuantity.ZERO`.
3. Set `lot.packageStatus = PackageStatus.EMPTY`.
4. Adherence calculation:
   $$\text{Adherence Rate} = \frac{\text{Taken Doses}}{\text{Scheduled Doses}} = \frac{120}{120} = 100\%$$
   The $60$ drops of wastage do not create phantom doses and do not penalize adherence.

---

## 6. Accidental Action Reversals

### 6.1 Accidental Opening Reversal
- Condition: `lot.packageStatus == OPENED` and `lot.remainingQuantity == lot.initialQuantity`.
- Result: `lot.packageStatus = UNOPENED`, timestamps cleared.
- Zero ledger movement needed because no stock was consumed.

### 6.2 Accidental Depletion Reversal
- Condition: `lot.packageStatus == EMPTY` and user requests reopening.
- Ledger entry:
   - `MovementType.ADJUSTMENT_IN`
   - `quantity = previousRemainingQuantity` (e.g. $60\text{ drops}$)
   - `notes = "Reopened accidentally depleted package"`
- Result: `lot.packageStatus = OPENED`, `lot.remainingQuantity = 60 drops`. Total supply is restored.
