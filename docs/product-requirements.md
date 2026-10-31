# Novelo Product Requirements Document (PRD)

**Product Name**: Novelo  
**Category**: Personal Medication Tracking & Prescription Management  
**Tagline**: "Medication, made manageable."  
**Platform**: Native Android (API 24+)  
**Version Target**: 1.0 (MVP)  

---

## 1. Product Vision & Principles

Novelo is an offline-first, privacy-respecting Android application that empowers individuals to manage complex medication schedules, track physical inventory without stock drift, and manage prescriptions with confidence.

### Core Tenets
1. **Clinical & Mathematical Integrity Over Decoration**: Exact ledger calculations, deterministic dose occurrence identities, and dependable alarm delivery take absolute priority over decorative animations.
2. **Strict Privacy by Architecture**: Zero network connectivity, no analytics, no advertising, and local storage sovereignty.
3. **Calm, Healthcare-Inspired UX**: Clean, NHS-inspired aesthetics that reduce cognitive load for users dealing with illness, chronic conditions, or multi-drug regimens.

---

## 2. Functional Requirements

### 2.1 Medication Management
- **FR-MED-01**: Users can create, view, edit, and archive medications.
- **FR-MED-02**: Fields: Name (required), Strength (e.g. "500 mg"), Form (`TABLET`, `CAPSULE`, `LIQUID_ML`, `INHALER_PUFFS`, `DROPS`, `INJECTION`, `TOPICAL_CREAM`, `PATCH`, `OTHER`), Stock Unit (e.g. `TABLETS`, `ML`, `PUFFS`, `BOTTLES`), Custom Notes, Color/Icon Tag.
- **FR-MED-03**: Initial Opening Stock quantity with exact non-negative integer representation.
- **FR-MED-04**: Soft-archive support: Archived medications cease generating future alarms but preserve complete historical dose records and ledger movements.

### 2.2 Flexible Schedules
- **FR-SCH-01**: Supports multiple schedule types:
  - *Daily*: Specific fixed times each day (e.g., 08:00, 14:00, 20:00).
  - *Specific Days of Week*: Selected weekdays (e.g., Mon, Wed, Fri at 09:00).
  - *Cyclical*: $X$ days on, $Y$ days off (e.g. 21 days on, 7 days off).
  - *As-Needed (`PRN`)*: Unscheduled doses logged on-demand with maximum daily dose safety limit warnings.
- **FR-SCH-02**: Optional start date, optional end date, or ongoing duration.
- **FR-SCH-03**: Dynamic occurrence generation: Computes deterministic `occurrenceId` tokens for upcoming occurrences without rewriting past completed occurrences.

### 2.3 Dose Recording & Idempotency
- **FR-DOSE-01**: Configurable per-medication recording preference:
  - `CONFIRM`: Requires explicit user confirmation via notification action or UI button.
  - `AUTO_ASSUME`: Records assumed adherence upon scheduled time arrival; clearly tagged in history as `ASSUMED`.
- **FR-DOSE-02**: Actionable statuses: `TAKEN`, `ASSUMED`, `SKIPPED`, `MISSED`.
- **FR-DOSE-03**: Idempotency: Multiple taps or duplicate intent broadcasts for the same `occurrenceId` must execute at most once.
- **FR-DOSE-04**: Stock linkage: Only `TAKEN` and `ASSUMED` events trigger stock deductions. `SKIPPED` and `MISSED` events do not deduct stock.
- **FR-DOSE-05**: Historical editing: Users can change a recorded status retrospectively; adjustments trigger compensating ledger movements.
- **FR-DOSE-06**: Missed dose safety guardrail: The app must **never** recommend taking a double or catch-up dose after a missed occurrence.

### 2.4 Stock Ledger & Inventory
- **FR-STK-01**: Immutable double-entry stock ledger table tracking every inventory delta.
- **FR-STK-02**: Exact arithmetic: Quantities stored as scaled integers or exact representations. No floating-point math.
- **FR-STK-03**: Restocking: Users can record a refill/restock event (`IN` movement), with optional batch/lot identifier and shelf-life expiration date.
- **FR-STK-04**: Inventory adjustments: Users can log manual corrections (`ADJUSTMENT_IN` / `ADJUSTMENT_OUT`) with a reason note (e.g. "Dropped tablet", "Recounted bottle").
- **FR-STK-05**: Low-stock alerts: Trigger warning when stock balance falls below a user-configured threshold or below the refill lead time.
- **FR-STK-06**: Estimated days remaining: Computed as $\lfloor \text{Stock} / \text{Daily Consumption} \rfloor$ for regular schedules; marked `ESTIMATED` or `UNKNOWN` for PRN or indeterminate items.

### 2.5 Prescriptions & Refill Tracking
- **FR-RX-01**: Track prescription details: Prescribing Doctor/Clinic, Pharmacy name/phone, Script reference number, Date issued, Prescription expiry date.
- **FR-RX-02**: Refill counting: Total authorized repeats/refills, number of refills collected, number of refills remaining.
- **FR-RX-03**: Status tracking: `ACTIVE`, `PENDING_REFILL`, `EXPIRED`, `COMPLETED`.
- **FR-RX-04**: Advance warning: Alerts user when a prescription is nearing expiry or when authorized repeats are exhausted.

### 2.6 Notifications & Reminders
- **FR-NOTIF-01**: Exact alarm delivery via `AlarmManager.setExactAndAllowWhileIdle()`.
- **FR-NOTIF-02**: Interactive notification shade actions: "Take" (deducts stock immediately), "Snooze" (re-alerts in 15 min), "Skip" (records skipped).
- **FR-NOTIF-03**: Notification Privacy Mode: Optional toggle to mask drug name and dosage on lock screens (e.g. "Medication Reminder: Time for your dose").
- **FR-NOTIF-04**: Quiet hours and sound/vibration customization.
- **FR-NOTIF-05**: System event reconciliation: Reschedules alarms automatically on boot, time change, and timezone shifts.

### 2.7 Adherence History & Calendar
- **FR-HIST-01**: Chronological event timeline displaying all completed, assumed, skipped, and missed doses.
- **FR-HIST-02**: Month and week calendar views with color-coded status badges.
- **FR-HIST-03**: Calculated adherence percentage over 7, 30, and 90 days.

### 2.8 Security, App Lock & Export
- **FR-SEC-01**: Optional biometric unlock (`BiometricPrompt`) or device PIN/pattern.
- **FR-SEC-02**: Cloud backup exclusion in manifest (`data_extraction_rules.xml`).
- **FR-SEC-03**: User-initiated local JSON/CSV export and import via Storage Access Framework.

---

## 3. Non-Goals (Out of Scope for MVP)

- **NG-01: No Cloud Accounts or Cloud Synchronization**: No Firebase, AWS, or multi-device cloud synchronization.
- **NG-02: No AI Diagnosis or Conversational Bots**: No LLM APIs or AI prescription scanners in the app bundle.
- **NG-03: No Automated Pharmacy Ordering**: No integration with external pharmacy portals or NHS Electronic Prescription Service (EPS) APIs.
- **NG-04: No Drug Interaction Checking**: No built-in clinical drug-interaction or contraindication engine (users consult their pharmacist/doctor).
- **NG-05: No Family / Caregiver Sharing Network**: No multi-user remote caregiver monitoring platform in the local-first MVP.

---

## 4. Unresolved Technical & Design Decisions (With Defaults)

| Decision | Open Question | Proposed Default | User Review Required? |
| :--- | :--- | :--- | :--- |
| **Package Namespace** | Project was scaffolded as `com.example.nuvilo`. Refactor to `com.novelo.app`? | Retain internal namespace until Phase 1 database entities, or refactor now. | Yes (Pending review) |
| **Dashboard Layout** | Chronological Timeline vs Hero Next-Dose Card? | Provide both in design specs; default to Option A (Timeline-First). | Yes (Prepared in specs) |
| **Grace Period for Missed Doses** | When should an unconfirmed dose transition to `MISSED`? | Default to 2 hours after scheduled dose time or midnight, whichever is earlier. | Yes |
| **Snooze Duration** | Fixed or configurable? | Default 15 minutes, with option in settings for 10, 15, 30 minutes. | No (Standard UX) |
