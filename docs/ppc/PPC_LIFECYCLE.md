# NHS Prescription Prepayment Certificate (PPC) Lifecycle & Date Rules

## 1. Validity Calculation Rules (`PpcCalculator`)

All date math uses `java.time.LocalDate` in the user's local calendar—never floating-point approximations or fixed 30/365-day constants.

### 3-Month and 12-Month PPCs
Following the NHSBSA inclusive validity convention:
$$\text{expiryDate} = \text{startDate}.\text{plusMonths}(N).\text{minusDays}(1)$$
- **Example (Standard)**: Start `2024-04-01` + 12 months → `2025-03-31` (inclusive).
- **Example (Year Transition)**: Start `2026-11-15` + 3 months → `2027-02-14`.
- **Example (Month-End Clamping)**: Start `2026-01-31` + 3 months → `plusMonths(3)` clamps to `2026-04-30`, minus 1 day = `2026-04-29`.
- **Example (Leap Year)**:
  - `2023-03-01` + 12 months → `2024-02-29` (leap day).
  - `2024-02-29` + 12 months → `2025-02-28` minus 1 day = `2025-02-27`.

### Manual Expiry Override
Users can enable **Override expiry date manually** in Step 2 of the wizard to match an exact date printed on their certificate. When `manualExpiryOverride != null`, it takes precedence over calculated calendar-month math.

### Free NHS Prescription Exemptions
- **Ongoing Exemptions** (`isOngoing = true`, e.g., age-related 60+ or permanent qualifying exemption): `expiryDate = null`, `remainingDays = null`. Status is `ACTIVE` (or `NOT_YET_VALID` if `startDate > today`).
- **Fixed-Term Exemptions** (`isOngoing = false`, e.g., Maternity or HC2 certificate): User specifies the certificate expiry date.

---

## 2. Status Evaluation State Machine

Relative to `today` (local `LocalDate`):
1. **`NO_RECORD`**: No non-archived certificates exist.
2. **`NOT_YET_VALID`**: `today < startDate`. Displays `daysUntilStart`.
3. **`ACTIVE`**: `today >= startDate` and (`effectiveExpiry == null` or `remainingDays > 30`).
4. **`EXPIRING_SOON`**: `0 <= remainingDays <= 30` (includes `remainingDays == 0`, i.e. expires today).
5. **`EXPIRED`**: `remainingDays < 0` (i.e. `today > effectiveExpiry`).

### Primary Certificate Selection (`selectPrimaryEvaluation`)
When multiple non-archived certificates exist, the Dashboard and Management header select the primary record in priority order:
1. `ACTIVE` (preferring ongoing or longest remaining validity)
2. `EXPIRING_SOON` (longest remaining validity)
3. `NOT_YET_VALID` (nearest upcoming start date)
4. `EXPIRED` (most recently expired)
5. `NO_RECORD`

---

## 3. Overlap Detection & Renewal
- **Overlap Warning**: `findOverlaps` checks `[startDate, expiryDate]` against all other non-archived certificates. Back-to-back certificates (e.g., ending `2026-03-31` and starting `2026-04-01`) do **not** trigger an overlap warning.
- **Mark Renewed**: `buildRenewalDraft` pre-fills a new certificate of the same coverage type starting `previous.expiryDate + 1 day` (or `today` if already expired/ongoing), links `renewedFromId = previous.id`, and archives the previous record upon save.
