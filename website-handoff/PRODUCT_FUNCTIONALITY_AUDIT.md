# Nuvilo Product Functionality Audit

**Product:** Nuvilo — *"Medication, made manageable."*  
**Android Application ID:** `com.nuvilomed.app` (internal Kotlin namespace: `com.example.nuvilo`)  
**Platform:** Native Android (Kotlin 2.2+, Jetpack Compose, Material 3, Room, AndroidX Biometrics)  
**Audit Date:** 2026-11-11  
**Auditor:** Senior Android Application Architect, Product Analyst & Privacy Engineer  

---

## 1. Executive Summary

This document presents a comprehensive technical audit of the genuine, implemented product functionality in **Nuvilo**. Every feature described in this report has been verified directly against the production source code in `app/src/main/java/com/example/nuvilo/`. 

Nuvilo is an **offline-first personal medication tracking, adherence management, and prescription tracking application**. All calculations (dose occurrence generation, adherence metrics, stock deductions via FEFO, prescription repeat decrementing, and cryptographic backup processing) execute entirely on-device with zero network communication, zero third-party telemetry, and zero remote dependencies.

---

## 2. In-Depth Feature Audit (22 Functional Domains)

### 1. Dashboard & Medication Timeline
- **Purpose**: Provides a unified, daily overview of today's medication schedule, real-time adherence KPI hero statistics, and quick-action dose cards.
- **What the user can do**: View past and upcoming dose occurrences for today, inspect the 7-day adherence progress percentage, mark doses as Taken, Skip a dose with notes, or Snooze a dose by 15 minutes.
- **What information is stored**: Today's dose occurrences (generated deterministically), dose confirmation status (`CONFIRMED`, `ASSUMED`, `SKIPPED`, `MISSED`), timestamp of action.
- **Where it is stored**: Room SQLite table `dose_events` (`occurrence_id`, `medication_id`, `scheduled_epoch_ms`, `dose_quantity_raw`, `unit`, `status`, `recorded_epoch_ms`).
- **Processing**: 100% locally evaluated via `ScheduleOccurrenceCalculator.kt` and `AdherenceCalculator.kt`.
- **Transmission**: Zero data transmission. All computation occurs in local memory and SQLite.
- **Permissions**: None for UI rendering; `POST_NOTIFICATIONS` and `SCHEDULE_EXACT_ALARM` if interacting with scheduled reminders.
- **Limitations**: Timeline displays doses for the selected calendar day; past days require navigating to the Calendar or History screens.
- **Source Code**: [DashboardScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/DashboardScreen.kt), [NoveloComponents.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/components/NoveloComponents.kt#L100-L350), [ScheduleOccurrenceCalculator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/schedule/ScheduleOccurrenceCalculator.kt).

---

### 2. Medication Management & Regimen Overview
- **Purpose**: Centralized registry of all active and archived medicines prescribed to or managed by the user.
- **What the user can do**: Search medications by name or form, filter by active vs archived status, inspect current stock balances, see days-of-supply remaining, and tap to view detailed medication records.
- **What information is stored**: Drug name, strength, pharmaceutical form, stock unit, opening stock, low stock alert threshold, recording mode (`CONFIRM` vs `AUTO_ASSUME`), clinical notes, hex color tag, archived flag, and creation timestamp.
- **Where it is stored**: Room table `medications` (`id`, `name`, `strength`, `form`, `stock_unit`, `opening_stock_raw`, `low_stock_threshold_raw`, `recording_mode`, `notes`, `color_tag_hex`, `is_archived`, `created_at_epoch_ms`).
- **Processing**: 100% local reactive Flow query via `MedicationDao.kt` and `NoveloRepository.kt`.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Archiving hides medication from the active dashboard timeline but preserves historical adherence logs and stock ledger records.
- **Source Code**: [MedicationListScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationListScreen.kt), [Entities.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/local/entity/Entities.kt#L9-L53), [MedicationDao.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/local/dao/Daos.kt).

---

### 3. Add & Edit Medication Wizard
- **Purpose**: Multi-step, guided configuration dialog ensuring clinical precision when adding a new medication or updating existing regimens.
- **What the user can do**: Input medication name, strength (e.g. `500mg`, `10ml`), select pharmaceutical form (tablet, capsule, liquid ml, drops, inhaler puffs, topical cream, injection), pick stock unit, specify initial stock, set low-stock warning threshold, choose confirmation mode, select an accent color tag, and attach medical notes.
- **What information is stored**: Medication identity, pharmaceutical parameters, and initial stock lot.
- **Where it is stored**: Room tables `medications`, `stock_lots` (opening lot), and `stock_ledger_entries` (`OPENING_BALANCE` movement).
- **Processing**: Local validation inside `AddEditMedicationViewModel` using scaled integer math (`ScaledQuantity.kt`).
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Name cannot be blank; dose quantities cannot be zero or negative.
- **Source Code**: [MedicationListScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationListScreen.kt#L250-L650), [NoveloComponents.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/components/NoveloComponents.kt), [ScaledQuantity.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/model/ScaledQuantity.kt).

---

### 4. Flexible Medication Schedules
- **Purpose**: Defines recurrence rules for when doses are due throughout days and weeks.
- **What the user can do**: Configure daily schedules (multiple times per day, e.g. 08:00, 14:00, 20:00), specific days of week (e.g. Mon, Wed, Fri), interval schedules (every X days), or cyclical schedules (X days on, Y days off), specify exact dose quantity per trigger, and set optional start and end dates.
- **What information is stored**: Schedule recurrence type (`DAILY`, `DAYS_OF_WEEK`, `INTERVAL`, `CYCLICAL`), comma-separated target times (`HH:mm`), active days of week, dose quantity in scaled units, start/end dates, and patient instructions (e.g. *"Take with food"*).
- **Where it is stored**: Room table `schedules` (`id`, `medication_id`, `schedule_type`, `target_times_csv`, `days_of_week_csv`, `dose_quantity_raw`, `start_date_epoch_day`, `end_date_epoch_day`, `instructions`).
- **Processing**: Evaluated locally via `ScheduleOccurrenceCalculator.kt` with foreign key cascade deletion attached to `medications.id`.
- **Transmission**: None.
- **Permissions**: None for schedule definition; `SCHEDULE_EXACT_ALARM` when registering alarms with Android OS.
- **Limitations**: Times are stored as wall-clock minutes; timezone transitions are handled automatically by `BootAndChangeReceiver.kt`.
- **Source Code**: [ScheduleOccurrenceCalculator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/schedule/ScheduleOccurrenceCalculator.kt), [Entities.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/local/entity/Entities.kt#L55-L97).

---

### 5. Medication Details & Deep Inspection
- **Purpose**: Comprehensive inspection hub for a single medication, consolidating active schedule, stock balance, lot numbers, upcoming doses, and associated prescriptions.
- **What the user can do**: View total units in stock, calculate estimated days remaining based on daily consumption rate, review active batch lot numbers with expiry dates, view repeat prescription balance, trigger manual restock, initiate repeat order, or edit/archive the medicine.
- **What information is stored**: Aggregated view of `medications`, `schedules`, `stock_lots`, `stock_ledger_entries`, and `prescriptions`.
- **Where it is stored**: Queried reactively from Room SQLite database.
- **Processing**: 100% on-device composition.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Dependent on accurate stock logs.
- **Source Code**: [MedicationDetailScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationDetailScreen.kt).

---

### 6. Stock Management & Inventory Ledger
- **Purpose**: Clinical, double-entry style stock ledger accounting for medication units to guarantee zero floating-point rounding errors and 100% auditability.
- **What the user can do**: Inspect total on-hand stock, view remaining days of supply, record manual adjustments (e.g. damaged, lost, or expired units), view movement history, and receive warnings when stock falls below threshold.
- **What information is stored**: Every stock change is stored as an immutable ledger movement (`OPENING_BALANCE`, `RESTOCK`, `DOSE_TAKEN`, `MANUAL_ADJUSTMENT`, `WASTE_EXPIRED`, `WASTE_DAMAGED`), linking the delta quantity, unit, reason, and optional lot reference.
- **Where it is stored**: Room table `stock_ledger_entries` (`id`, `medication_id`, `timestamp_epoch_ms`, `movement_type`, `quantity_raw`, `unit`, `reason`, `dose_occurrence_id`, `lot_id`, `is_estimate`).
- **Processing**: Pure Kotlin ledger calculator in `StockLedgerCalculator.kt` using 64-bit integer fixed-point arithmetic (`ScaledQuantity`).
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Non-countable dosage forms (inhalers, ointments, eye drops) are explicitly tracked as `is_estimate = true` to inform the user of potential variance.
- **Source Code**: [StockLedgerCalculator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/stock/StockLedgerCalculator.kt), [Entities.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/local/entity/Entities.kt#L193-L244).

---

### 7. FEFO (First-Expired, First-Out) Batch & Expiry Tracking
- **Purpose**: Clinical inventory safety mechanism ensuring older or earliest-expiring stock is consumed before newer medication lots.
- **What the user can do**: Record specific lot numbers and expiry dates upon restocking, view active lots ordered by expiry date, see expiry countdown tags (e.g. *"Expires in 42 days"*), and have dose deductions automatically consume earliest-expiring lots first.
- **What information is stored**: Lot ID, lot number/batch string, initial quantity, remaining quantity, unit, expiry date timestamp, creation timestamp, and lot notes.
- **Where it is stored**: Room table `stock_lots` (`id`, `medication_id`, `lot_number`, `initial_quantity_raw`, `remaining_quantity_raw`, `unit`, `expiry_date_epoch_ms`, `created_at_epoch_ms`, `notes`).
- **Processing**: Automated FEFO lot allocation logic inside `StockLedgerCalculator.kt`.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: When lot number is omitted by the user during restock, an auto-generated lot ID is used with the specified expiry date.
- **Source Code**: [StockLedgerCalculator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/stock/StockLedgerCalculator.kt#L85-L160), [Entities.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/local/entity/Entities.kt#L148-L191).

---

### 8. Dose Confirmation & Idempotent Recording
- **Purpose**: Recording when a medication is actually taken, updating adherence metrics and stock balances reliably without double-counting.
- **What the user can do**: Confirm a dose directly from the dashboard, detail screen, or lock screen notification action (`ACTION_TAKE`).
- **What information is stored**: Dose event occurrence ID (`<medicationId>_<scheduledEpochMs>`), recorded timestamp, actual dose quantity taken, and status `TAKEN`.
- **Where it is stored**: Room table `dose_events` and a corresponding `DOSE_TAKEN` entry in `stock_ledger_entries`.
- **Processing**: Atomically wrapped inside Room database transactions (`NoveloDatabase.withTransaction`). Guaranteed idempotent: duplicate clicks or repeated notification intents with the same `occurrence_id` cannot insert duplicate rows or double-deduct inventory.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Catch-up dosing is strictly guarded: recording a missed dose never prompts or recommends doubling subsequent doses.
- **Source Code**: [NoveloRepository.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/repository/NoveloRepository.kt), [NotificationActionReceiver.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/notification/NotificationActionReceiver.kt).

---

### 9. AUTO_ASSUME vs CONFIRM Recording Modes
- **Purpose**: Adapts to patient habits by distinguishing between medications that require manual confirmation and maintenance medications recorded automatically.
- **What the user can do**: Toggle recording mode per medication in the wizard:
  - `CONFIRM`: Requires user to explicitly press "Take" or confirm via notification. If unconfirmed after time window, marks as `MISSED`.
  - `AUTO_ASSUME`: System assumes adherence according to schedule unless user explicitly marks as Skipped. Distinguishes assumed doses from confirmed doses in reports.
- **What information is stored**: `recording_mode` column in `medications` table (`"CONFIRM"` or `"AUTO_ASSUME"`). Dose status recorded as `TAKEN` vs `ASSUMED`.
- **Where it is stored**: Tables `medications` and `dose_events`.
- **Processing**: Adherence calculator calculates confirmed vs assumed proportions separately to preserve clinical auditability.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Clinical guidelines recommend `CONFIRM` mode for narrow therapeutic index drugs and critical treatments.
- **Source Code**: [AdherenceCalculator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/adherence/AdherenceCalculator.kt), [DomainModels.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/model/DomainModels.kt).

---

### 10. Medication Reminders & Exact Alarm Scheduling
- **Purpose**: Precise, timely alerts delivered at the exact minute each medication dose is due.
- **What the user can do**: Receive Android system notifications for scheduled doses even when the app is closed or device is in Doze mode.
- **What information is stored**: Alarm request codes mapped to occurrence IDs; scheduled epoch timestamps.
- **Where it is stored**: Handled via Android OS `AlarmManager` exact alarms with `PendingIntent` triggers.
- **Processing**: `AlarmScheduler.kt` sets `setExactAndAllowWhileIdle()` alarms. `AlarmReceiver.kt` processes triggers locally and calls `NotificationHelper.kt`.
- **Transmission**: None. Alarms are entirely local to the Android OS.
- **Permissions**: `android.permission.SCHEDULE_EXACT_ALARM`, `android.permission.POST_NOTIFICATIONS`, `android.permission.RECEIVE_BOOT_COMPLETED`, `android.permission.WAKE_LOCK`.
- **Limitations**: Exact alarms require user to grant exact alarm permission on Android 12+ (API 31+); app guides user if disabled.
- **Source Code**: [AlarmScheduler.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/alarm/AlarmScheduler.kt), [AlarmReceiver.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/alarm/AlarmReceiver.kt), [NotificationHelper.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/notification/NotificationHelper.kt).

---

### 11. Snooze & Skip Functionality
- **Purpose**: Accommodates real-world routine disruptions without breaking schedule tracking or deducting stock prematurely.
- **What the user can do**: Tap "Snooze (15m)" on a notification or in-app to reschedule the reminder by 15 minutes; tap "Skip" and optionally record a reason (e.g. *"Nauseous"*, *"Fasting for blood test"*).
- **What information is stored**: For Snooze: temporary 15-minute alarm. For Skip: `status = "SKIPPED"` in `dose_events` with optional note. **Zero stock deduction occurs on skipped doses.**
- **Where it is stored**: Table `dose_events`.
- **Processing**: Handled by `NotificationActionReceiver.kt` and `NoveloRepository.kt`.
- **Transmission**: None.
- **Permissions**: `SCHEDULE_EXACT_ALARM` for snooze alarms.
- **Limitations**: Maximum snooze cascade is handled locally; snoozing does not shift the next day's regular schedule.
- **Source Code**: [NotificationActionReceiver.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/notification/NotificationActionReceiver.kt#L60-L110), [NotificationHelper.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/notification/NotificationHelper.kt#L170-L176).

---

### 12. Adherence Calendar & Longitudinal Visualizer
- **Purpose**: Monthly calendar view displaying adherence status for every past day with color-coded status rings.
- **What the user can do**: Select any date to see the doses scheduled on that day, overall percentage score (e.g. 100%, 80%), breakdown of taken, skipped, and missed doses, and inspect individual dose details.
- **What information is stored**: Historical dose events aggregated by calendar day.
- **Where it is stored**: Room table `dose_events`.
- **Processing**: `AdherenceCalculator.kt` computes daily and monthly adherence scores dynamically using local device timezone.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Adherence calculation only counts doses whose scheduled time has passed; future occurrences do not count as missed.
- **Source Code**: [AdherenceCalendarScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/AdherenceCalendarScreen.kt), [AdherenceCalculator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/adherence/AdherenceCalculator.kt).

---

### 13. Medication History Log
- **Purpose**: Searchable, chronologically reverse-sorted audit trail of all dose actions across all medications.
- **What the user can do**: Filter history by medication name or status (`All`, `Taken`, `Skipped`, `Missed`), review historical notes, inspect exact confirmation timestamps, and initiate corrections.
- **What information is stored**: Full historical timeline of `dose_events`.
- **Where it is stored**: Room table `dose_events`.
- **Processing**: Reactive Flow query via `DoseEventDao.kt`.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Screen supports search queries; soft keyboard auto-dismisses to prevent UI obscuration.
- **Source Code**: [MedicationDoseHistoryScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationDoseHistoryScreen.kt), [Daos.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/local/dao/Daos.kt).

---

### 14. Historical Dose Corrections & Ledger Reconciliation
- **Purpose**: Enables users to fix past mistakes (e.g. accidentally marked a dose as taken when skipped, or vice versa) with clinical integrity.
- **What the user can do**: Open any historical dose event, change status (e.g. from `MISSED` to `TAKEN`, or `TAKEN` to `SKIPPED`), enter a clinical correction note, and save.
- **What information is stored**: `status`, `notes`, `correction_notes`, and `corrected_at_epoch_ms` in `dose_events`. Reverses or inserts corresponding compensating entries in `stock_ledger_entries`.
- **Where it is stored**: Tables `dose_events` and `stock_ledger_entries`.
- **Processing**: Atomic database transaction in `NoveloRepository.kt` reconciling stock ledger to prevent inventory drift.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Original occurrence ID is preserved; historical audit timestamp is recorded.
- **Source Code**: [MedicationDoseHistoryScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationDoseHistoryScreen.kt#L180-L240), [StockLedgerCalculator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/stock/StockLedgerCalculator.kt).

---

### 15. Prescription Management & Reference Tracking
- **Purpose**: Tracks formal medical prescriptions, authorizing doctors, issue/expiry dates, and repeat allowances.
- **What the user can do**: Record prescription reference numbers, issuing doctor/clinic, nominated dispensing pharmacy, prescribed quantity, total repeats authorized, repeats remaining, issue date, and expiry date.
- **What information is stored**: Prescription record and associated audit events.
- **Where it is stored**: Room table `prescriptions` (`id`, `medication_id`, `reference_number`, `issue_date_epoch_day`, `expiry_date_epoch_day`, `quantity_prescribed_raw`, `unit`, `repeats_authorised`, `repeats_remaining`, `has_pending_repeat_request`, `pharmacy_name`, `notes`, `status`).
- **Processing**: Local validation inside `PrescriptionManager.kt`.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Nuvilo does not connect to NHS spine or pharmacy APIs; all tracking is personal and patient-managed.
- **Source Code**: [PrescriptionManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/prescription/PrescriptionManager.kt), [Entities.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/local/entity/Entities.kt#L246-L308).

---

### 16. Repeat Requests & Collection Tracking
- **Purpose**: Manages the repeat prescription lifecycle (Requested → Approved/Dispensed → Collected).
- **What the user can do**: Log when a repeat prescription has been requested from a GP/physician (sets `has_pending_repeat_request = true`), record when the repeat is collected from the pharmacy (automatically decrements `repeats_remaining` by 1 and prompts for a stock restock lot), and view chronological event history.
- **What information is stored**: Event logs recording type (`REPEAT_REQUESTED`, `REPEAT_COLLECTED`, `REPEATS_AMENDED`), timestamp, remaining repeats count, and optional idempotency key.
- **Where it is stored**: Room table `prescription_events` (`id`, `prescription_id`, `medication_id`, `event_type`, `timestamp_epoch_ms`, `repeats_remaining_after`, `action_idempotency_key`, `notes`).
- **Processing**: Pure business logic in `PrescriptionManager.kt` executed in atomic database transaction.
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Prevents collection if `repeats_remaining <= 0` or prescription is expired.
- **Source Code**: [PrescriptionManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/domain/prescription/PrescriptionManager.kt#L45-L120), [MedicationDetailScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationDetailScreen.kt#L480-L580).

---

### 17. Low-Stock Notifications & Deferrable Worker
- **Purpose**: Timely background warning before medication supply runs out, prompting the patient to request a refill or collect repeats.
- **What the user can do**: Set custom low-stock thresholds (e.g. alert when $\le 7$ days supply or $\le 10$ tablets remain); receive local notification when balance dips below threshold.
- **What information is stored**: Set of notified medication IDs stored in DataStore to prevent notification spamming until restock occurs.
- **Where it is stored**: Jetpack DataStore key `low_stock_notified_med_ids`.
- **Processing**: Evaluated after dose deductions and periodically via `LowStockCheckWorker.kt` (AndroidX `WorkManager`).
- **Transmission**: None.
- **Permissions**: `POST_NOTIFICATIONS`. Note: WorkManager's transitive `ACCESS_NETWORK_STATE` is explicitly stripped via `tools:node="remove"`.
- **Limitations**: Only fires when supply balance drops below user threshold; suppressed if an alert was already posted and stock has not changed.
- **Source Code**: [LowStockCheckWorker.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/worker/LowStockCheckWorker.kt), [NotificationHelper.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/notification/NotificationHelper.kt#L210-L260).

---

### 18. Biometric & Device Credential App Lock
- **Purpose**: High-security physical privacy barrier protecting medical records from unauthorized inspection when phone is unlocked or shared.
- **What the user can do**: Enable App Lock in Settings; choose grace period (`Immediately`, `30 seconds`, `1 minute`, `5 minutes`, `15 minutes`); authenticate via fingerprint, face unlock, or system PIN/pattern to access Nuvilo; enable Recent Apps thumbnail masking.
- **What information is stored**: Preference flag `biometric_lock_enabled: Boolean`, `lock_grace_period: String`, `recents_preview_masked: Boolean` in DataStore. **Zero biometric templates or credentials are stored or accessible by Nuvilo.**
- **Where it is stored**: DataStore `user_preferences`.
- **Processing**: Evaluated via `AppLockManager.kt` using monotonic real-time clock (`SystemClock.elapsedRealtime()`). UI gating removes underlying Composable tree from composition when locked; window `FLAG_SECURE` applied. Authentication handled by Android OS `BiometricPrompt`.
- **Transmission**: None. Biometric verification occurs entirely within Android's hardware-isolated TEE/StrongBox.
- **Permissions**: `android.permission.USE_BIOMETRIC`, `android.permission.USE_FINGERPRINT`.
- **Limitations**: Protects the UI from local physical viewing; does not replace full-disk hardware encryption provided by Android OS.
- **Source Code**: [AppLockManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/security/AppLockManager.kt), [BiometricAuthManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/security/BiometricAuthManager.kt), [LockScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/LockScreen.kt).

---

### 19. Lock-Screen Notification Privacy Mode
- **Purpose**: Protects medical confidentiality on public lock screens and shoulder-surfing environments.
- **What the user can do**: Toggle Notification Privacy Mode in Settings.
- **What information is stored**: Preference flag `notification_privacy_masked: Boolean`.
- **Where it is stored**: DataStore `user_preferences`.
- **Processing**: When enabled, `NotificationHelper.kt` substitutes drug names, dosages, and stock levels with generic text:
  - Masked Title: `"Medication Reminder"`
  - Masked Body: `"Time for your scheduled medication dose"`
  - Masked Visibility: `NotificationCompat.VISIBILITY_SECRET`
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Replaces drug-specific text for all active reminders while active.
- **Source Code**: [NotificationHelper.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/system/notification/NotificationHelper.kt#L149-L175), [SettingsScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/SettingsScreen.kt#L180-L220).

---

### 20. Local Encrypted Backup & Restoration (`.nuvilo.json`)
- **Purpose**: Zero-knowledge, military-grade encrypted backup allowing users to securely preserve or transfer their complete medical database.
- **What the user can do**: Create an encrypted backup file (`.nuvilo.json`) protected by a user-chosen passphrase; save via Android Storage Access Framework (SAF) to any destination; restore from a backup file with passphrase verification and schema validation.
- **What information is stored**: Complete JSON snapshot of all 7 database tables, encrypted inside a cryptographic envelope containing format metadata, salt, nonce, and ciphertext.
- **Where it is stored**: Destination chosen by user via SAF (local internal storage, SD card, or user's cloud drive provider).
- **Processing**: 
  - Key Derivation: `PBKDF2WithHmacSHA256` with 100,000 iterations and a 16-byte random salt (`SecureRandom`).
  - Cipher: `AES-256-GCM` with 12-byte random nonce and 128-bit authentication tag.
  - Zero-Knowledge: Passphrase is never saved or logged; `keySpec.clearPassword()` and `Arrays.fill(keyBytes, 0)` wipe secrets from memory immediately.
  - Restoration Validation: Checks schema versions, primary key uniqueness, foreign key validity, and runs within `NoveloDatabase.withTransaction` with 100% rollback guarantee.
- **Transmission**: None by Nuvilo. If the user saves to Google Drive/Dropbox via Android SAF, that transmission is handled by the user's selected SAF document provider, not Nuvilo.
- **Permissions**: None (uses Android Storage Access Framework `ActivityResultContracts.CreateDocument` / `OpenDocument`). Zero external storage permissions requested.
- **Limitations**: If user forgets their passphrase, data cannot be recovered by anyone (zero backdoor).
- **Source Code**: [BackupCrypto.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/backup/BackupCrypto.kt), [JsonBackupManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/backup/JsonBackupManager.kt), [BackupValidator.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/backup/BackupValidator.kt).

---

### 21. Plaintext CSV ZIP Data Export
- **Purpose**: Data portability and user sovereignty, allowing patients to export their data for spreadsheets, doctor appointments, or personal archiving.
- **What the user can do**: Export an unencrypted ZIP bundle (`nuvilo_export_<timestamp>.zip`) containing `manifest.json` and 7 RFC 4180 compliant CSV tables (`medications.csv`, `schedules.csv`, `dose_events.csv`, `stock_lots.csv`, `stock_ledger_entries.csv`, `prescriptions.csv`, `prescription_events.csv`).
- **What information is stored**: Plaintext CSV records.
- **Where it is stored**: User-selected SAF destination.
- **Processing**: Generated in-memory via `CsvZipBackupManager.kt`. App displays explicit security dialog warning the user that CSV files are unencrypted before writing.
- **Transmission**: None.
- **Permissions**: None (SAF picker).
- **Limitations**: Intended for human inspection and external data portability; encrypted `.nuvilo.json` is recommended for full backups and restoration.
- **Source Code**: [CsvZipBackupManager.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/backup/CsvZipBackupManager.kt), [SettingsScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/SettingsScreen.kt#L450-L520).

---

### 22. Appearance, Theming & Diagnostic Controls
- **Purpose**: Accessible visual personalization conforming to WCAG AA clinical contrast guidelines.
- **What the user can do**: Select theme mode (`System Default`, `Light Mode`, `Dark Mode`); view installed app version (`1.0.0`); trigger a test notification to verify notification permissions; open system notification and exact alarm settings.
- **What information is stored**: `theme_mode: String` in DataStore.
- **Where it is stored**: DataStore `user_preferences`.
- **Processing**: Jetpack Compose reactive state collection (`collectAsStateWithLifecycle()`).
- **Transmission**: None.
- **Permissions**: None.
- **Limitations**: Dark mode uses OLED-friendly deep slate `#0F172A` / `#1E293B` to minimize battery consumption.
- **Source Code**: [SettingsScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/SettingsScreen.kt), [Theme.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/theme/Theme.kt), [UserPreferencesRepository.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/data/preferences/UserPreferencesRepository.kt).

---

## 3. Functionality Matrix Summary

| Feature Category | Storage Mechanism | Processing Location | Network Required | Permissions Required | User Deletion Control |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Medication Profiles** | Room SQLite (`medications`) | On-Device | None | None | Full (Delete / Archive) |
| **Schedules** | Room SQLite (`schedules`) | On-Device | None | None | Full (CASCADE) |
| **Dose Events** | Room SQLite (`dose_events`) | On-Device | None | None | Full (CASCADE / Edit) |
| **Stock Ledger & Lots** | Room SQLite (`stock_*`) | On-Device | None | None | Full (CASCADE) |
| **Prescriptions & Events** | Room SQLite (`prescription_*`) | On-Device | None | None | Full (CASCADE) |
| **Alarms & Reminders** | OS AlarmManager | On-Device | None | `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS` | Dismiss / Snooze / Disable |
| **Biometric App Lock** | OS BiometricPrompt + DataStore | On-Device (TEE/StrongBox) | None | `USE_BIOMETRIC` | Toggle on/off anytime |
| **Encrypted Backups** | SAF User Destination | On-Device (AES-256-GCM) | None | None (SAF) | User file deletion |
| **CSV Exports** | SAF User Destination | On-Device (ZIP/CSV) | None | None (SAF) | User file deletion |
| **User Preferences** | Jetpack DataStore | On-Device | None | None | Reset / Clear data |
