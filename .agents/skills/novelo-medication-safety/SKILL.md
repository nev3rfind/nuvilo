---
name: novelo-medication-safety
description: >-
  Mandatory safety and integrity invariants for Novelo (medication tracking, dose recording,
  stock ledger accounting, notification scheduling, and health data privacy).
  Activate whenever creating, modifying, or reviewing code or tests involving medications,
  schedules, dose events, stock calculations, alarms, or health data storage.
---

# Novelo Medication Safety & Domain Invariants

## Core Principle
Novelo is a personal medication tracking and schedule recording tool. It is **not** a prescribing system, medical diagnostic tool, or clinical dosing advisor. Correct stock ledger calculations, idempotent dose recording, scheduled notification reliability, and strict user privacy take precedence over decorative or convenience features.

---

## 1. Dose Recording Invariants

### Recording Modes
Every medication defines a recording preference:
1. **`CONFIRM` (Default)**: A dose is **never** recorded as taken until the user explicitly confirms it in the UI or via an actionable notification.
2. **`AUTO_ASSUME`**: An assumed dose record is logged according to the scheduled time, clearly designated with status `ASSUMED`. The UI must distinctly differentiate verified/confirmed ingestion from assumed adherence.

### Idempotency & Occurrence Identity
- Every scheduled dose has a deterministic, globally unique `occurrenceId`:
  `occurrenceId = hash(medicationId, scheduledDateTimeEpochSeconds)` or a stable UUID generated at schedule generation time.
- Multiple taps on notification actions ("Take Dose"), rapid UI button clicks, or parallel background workers **must never** record multiple dose events for the same occurrence.
- The dose event database table must enforce uniqueness on `occurrenceId` (e.g. SQLite `UNIQUE(occurrence_id)` constraint or upsert semantics with conflict rejection).

### Dose Actions & Stock Impacts
- **Confirmed Taken (`TAKEN`)**: Creates a dose record and transactionally creates a stock deduction ledger entry.
- **Assumed Taken (`ASSUMED`)**: Creates an assumed dose record and transactionally creates an assumed stock deduction ledger entry.
- **Skipped (`SKIPPED`)**: Records the skipped status for adherence history. **Must not** deduct stock.
- **Missed (`MISSED`)**: Evaluated when scheduled time passes a grace period without confirmation. **Must not** deduct stock.
- **Snoozed / Dismissed Notifications**: Dismissing or snoozing an alarm/notification **must never** count as taking or skipping a dose. It only reschedules the reminder.
- **Historical Edits / Deletions**: If a user removes a recorded dose or marks a taken dose as skipped retrospectively, the stock deduction must be reverted via an auditable compensating adjustment transaction.

### Catch-up & Safety Guardrail
- Under **no circumstances** may the application recommend taking a double dose, catch-up dose, or extra medication after a missed or delayed dose.
- If a dose is overdue, the UI displays it as overdue/missed and prompts the user to take their regular prescribed dose only if consistent with their healthcare provider's instructions.

---

## 2. Stock Ledger Accounting Invariants

### The Ledger as Single Source of Truth
- Stock balance is **never** stored as an unbacked mutable integer that can drift.
- Stock is calculated deterministically from:
  $$\text{Current Stock} = \text{Opening Balance} + \sum(\text{Restocks}) - \sum(\text{Deductions}) \pm \sum(\text{Adjustments})$$
- A snapshot/cached balance column may exist on the medication entity for query performance, but it **must** be verified and updated transactionally inside the same Room `@Transaction` that appends the ledger movement.

### Exact Arithmetic (No Floating-Point)
- **Zero Floating-Point math**: Quantities must use scaled integer representations (e.g., base integer representing micro-units, milligrams, milliliters, whole tablets, or scaled by $10^3$ / $10^4$) or exact `BigDecimal`.
- Never use `Float` or `Double` for stock levels, dose quantities, or ledger totals.

### Unit Compatibility & Boundaries
- Never silently convert or combine incompatible stock units (e.g. tablets cannot convert to milliliters; whole inhaler canisters cannot convert to sprays unless explicit container metadata is configured).
- Liquids, ointments, and sprays with indeterminate drops/doses must be flagged as **`ESTIMATED`**.
- Do not fabricate numbers or assume standard drops per eye-drop bottle (clinical bottles vary from 15 to 25 drops/mL).

### Audit Trail & Immutability
- Ledger movements are append-only. Historical movements are **never** silently deleted.
- Manual inventory corrections generate an `ADJUSTMENT` movement with the delta, reason, and timestamp.

---

## 3. Notification & Alarm Scheduling Invariants

### Scheduler Architecture
- **AlarmManager** using exact alarms (`setExactAndAllowWhileIdle()`) is required for time-critical medication reminder alerts.
- **WorkManager** is used for non-time-critical deferrable background maintenance (e.g. low-stock checks, audit integrity scans, log rotations), **not** for real-time dose notifications.
- Exact alarms require checking `canScheduleExactAlarms()` on Android 12+ (API 31+) and graceful fallback handling if `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` is unavailable.

### Reconciliation on System Triggers
- Alarms must be reconciled and rescheduled upon:
  1. System boot (`android.intent.action.BOOT_COMPLETED`).
  2. Timezone change (`android.intent.action.TIMEZONE_CHANGED`).
  3. Clock / Time manual update (`android.intent.action.TIME_SET`).
  4. App update (`android.intent.action.MY_PACKAGE_REPLACED`).
  5. Schedule creation, modification, or deactivation.

### Honest Communication
- Remind users that battery savers, vendor power managers (OEM battery restrictions), and Android Doze can impact alarm timeliness.
- Provide a diagnostic screen indicating exact alarm permission status and battery optimization settings.

---

## 4. Privacy & Offline-Only Invariants

### Zero Network Boundary
- The application **must not** declare `android.permission.INTERNET` in `AndroidManifest.xml`.
- No analytics SDKs (no Firebase Analytics, Crashlytics, Mixpanel, etc.).
- No remote advertising SDKs.
- No cloud authentication or remote backend syncing.
- No third-party AI APIs or web service calls.

### Notification Privacy Masking
- The user can enable **Notification Privacy Mode**:
  - *Standard Mode*: Shows medication name, strength, and action buttons ("Take 500mg Paracetamol").
  - *Private Mode*: Shows generic text ("Medication Reminder: Time for your scheduled dose") to protect health privacy on lock screens.

### Data Security & App Lock
- Support optional biometric (BiometricPrompt) or device credential PIN/pattern authentication on app open.
- Backups: Local export/import must be user-initiated, explicitly saved to a user-chosen URI via the Android Storage Access Framework (SAF).
- Cloud backup rules in `data_extraction_rules.xml` and `backup_rules.xml` must explicitly exclude unencrypted health data from automated Google Drive device backups unless opted in and password-protected.

---

## 5. Verification Checklist for Changes

Before finalizing any PR or code changes involving medications:
1. [ ] Is the dose recording idempotent (checked against `occurrenceId`)?
2. [ ] Does dismissing or snoozing a reminder leave stock and dose records untouched?
3. [ ] Are stock calculations derived from ledger movements using exact integer math?
4. [ ] Are Room database operations touching dose events and stock ledgers wrapped in a transaction?
5. [ ] Is `android.permission.INTERNET` absent from the manifest and merged manifests?
6. [ ] Are alarm receivers registered safely with explicit exported flags and intent verification?
7. [ ] Are unit tests present for dose idempotency, stock deductions, and schedule occurrences?
