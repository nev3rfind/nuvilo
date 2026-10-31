# Novelo Domain Glossary

This document establishes the ubiquitous language for Novelo. Every term defined here has a precise architectural, database, and user interface meaning that must remain consistent across the codebase, documentation, and agent conversations.

---

### Medication
The top-level entity representing a drug, supplement, or therapeutic item tracked by the user (e.g. "Amoxicillin 500mg Capsule").

### Medication Form
The physical presentation of the medicine (e.g., `TABLET`, `CAPSULE`, `LIQUID_ML`, `INHALER_PUFFS`, `DROPS`, `INJECTION`, `TOPICAL_CREAM`, `PATCH`, `OTHER`). The form dictates allowed stock units and dose representations.

### Stock Unit
The unit of measurement used to quantify physical inventory in the stock ledger (e.g., `TABLETS`, `CAPSULES`, `ML`, `PUFFS`, `UNITS`, `PATCHES`, `BOTTLES`). Stock units must match or be directly convertible to dose quantities.

### Dose Quantity
The specific amount of medication prescribed or scheduled to be administered in a single dose event (e.g., `2` tablets, `10` ml, `1` puff). Represented as a scaled integer or exact fraction, never as a floating-point number.

### Schedule
The programmatic rule defining when doses should occur over time. Can be daily, multiple times per day, specific days of the week, cyclical (e.g. 21 days on, 7 days off), or as-needed (`PRN`). Schedules have an optional start date, end date, or ongoing duration.

### Dose Occurrence
A single planned dose event pinpointed in time according to an active schedule. It possesses a deterministic, unique identity (`occurrenceId`), linking a specific medication to a specific scheduled timestamp.

### Confirmed Dose
A dose occurrence where the user has explicitly taken an action (tapping "Take" in the app or notification) confirming that they ingested or administered the medication. Confirms ingestion and triggers a transactional stock deduction.

### Assumed Dose
A dose occurrence for a medication configured with `AUTO_ASSUME` mode that is automatically marked as administered once the scheduled time has elapsed, without asserting verified clinical ingestion. Triggers an assumed stock deduction.

### Skipped Dose
A dose occurrence where the user deliberately indicated they did not take the scheduled dose (e.g. instructed by doctor, feeling unwell, or fasting). Records adherence history as `SKIPPED` without deducting inventory.

### Missed Dose
A dose occurrence where the scheduled time and grace window elapsed without user confirmation or recording. Displayed as missed in history. Under no circumstances does a missed dose trigger a double dose or catch-up recommendation.

### Stock Movement
An atomic, immutable record in the stock ledger tracking any change to medication inventory. Movements have a direction (`IN` or `OUT`), an exact quantity, a reason, a timestamp, and an optional foreign key link to a `doseEventId` or `restockEventId`.

### Opening Stock
The initial quantity of medication physically in possession when the user first registers or begins tracking the medication in Novelo. Represents the initial `IN` movement in the stock ledger.

### Restock
A stock movement representing new physical inventory acquired by the user (e.g. picking up a prescription refill). Appends an `IN` movement to the ledger.

### Stock Adjustment
An explicit manual correction movement (`ADJUSTMENT_IN` or `ADJUSTMENT_OUT`) added to the ledger when physical inventory differs from calculated balance (e.g., dropped tablet, spilled liquid, or recounting). Preserves audit history without deleting past movements.

### Estimated Days Remaining
A calculated projection:
$$\text{Days Remaining} = \left\lfloor \frac{\text{Current Stock Balance}}{\text{Daily Consumption Rate}} \right\rfloor$$
Only computed and displayed when a medication has a fixed, regular daily schedule. If a schedule is variable or PRN (as-needed), the metric must be explicitly labeled as `UNKNOWN` or `ESTIMATED ONLY`.

### Prescription Status
The current lifecycle state of an official medical prescription:
1. `ACTIVE`: Prescription has refills or is currently being dispensed.
2. `PENDING_REFILL`: User needs to order a renewal from their healthcare provider.
3. `EXPIRED`: Prescription past its legal validity date.
4. `COMPLETED`: All authorized repeats/refills have been dispensed.

### Medication Expiry
The manufacturer's printed shelf-life expiration date on the physical medicine box/bottle. Physical medication should not be consumed past this date.

### Prescription Expiry
The statutory expiration date after which a pharmacy can no longer legally dispense refills from the doctor's prescription script (distinct from medication shelf-life).

### Refill Lead Time
The user-configurable duration (e.g., 3 to 7 days) required between ordering a prescription refill and having it dispensed, used to calculate advance low-stock and refill warning notifications.

### Reminder
An actionable system alert triggered by `AlarmManager` at the scheduled dose time, providing quick actions (`Take`, `Snooze`, `Skip`).

### Snooze
A temporary deferral of an active reminder (e.g. snooze for 15 minutes). Does not alter the scheduled dose time, does not record a dose, and does not alter stock.

### Idempotency
The mathematical property where an operation can be applied multiple times without changing the result beyond the initial application. In Novelo, tapping a notification "Take" action 5 times or encountering race conditions must record exactly one dose and deduct stock exactly once.

### Timezone Policy
The deterministic rule set governing how schedules adjust when the device crosses timezone boundaries:
- **Wall-Clock Alarms (Default)**: A 08:00 AM dose continues to ring at 08:00 AM local time in the new timezone (e.g., morning vitamins).
- **Interval/Duration-Based Alarms**: Doses that require fixed physiological intervals (e.g., antibiotics every 8 hours) retain absolute elapsed hours regardless of local clock shifts.

### Stock Lot
A distinct physical supply batch associated with a medication, capturing initial quantity, remaining quantity, lot/batch number, creation timestamp, and optional supply expiration date.

### FEFO (First Expired, First Out)
The deterministic inventory consumption policy that debits stock lots in order of earliest expiration date first (null expiration dates placed last), with a FIFO fallback by lot creation date.

### Compensating Dose Reversal (`DOSE_REVERSAL`)
An auditable, append-only ledger inflow entry created when a user retrospectively revises a recorded dose from `TAKEN` or `ASSUMED` to `SKIPPED` or `MISSED`. Compensates the historical deduction without deleting or modifying original ledger records.
