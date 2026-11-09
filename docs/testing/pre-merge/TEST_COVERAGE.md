# Nuvilo: Pre-Merge Test Coverage Analysis (TEST_COVERAGE.md)

**Product:** Nuvilo – Medication, made manageable.  
**Repository:** https://github.com/nev3rfind/nuvilo  
**Target Branch:** `test/8-pre-merge-regression-200`  
**Total Automated Tests:** 285 tests (56 baseline JVM + 29 baseline AVD + 80 regression JVM + 120 regression AVD)

---

## 1. Functional Domain Coverage

### 1.1 Medication Domain & Ledger Mathematics (Category A)
- **ScaledQuantity Exact Integer Arithmetic**: 100% covered.
  - Zero floating-point representation invariant verified across fractional amounts (`0.25`, `0.5`, `1.5`, `2.75`).
  - Unit scaling factor of 1,000 tested for whole, fractional, and boundary limits.
  - Addition, subtraction, multiplication, string formatting, and parsing strictly verified against truncation or rounding errors.
- **Stock Ledger Accounting**: 100% covered.
  - Append-only immutable movement log verified across all 8 movement types (`OPENING_BALANCE`, `RESTOCK`, `DOSE_TAKEN`, `DOSE_ASSUMED`, `DOSE_REVERSAL`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `WASTAGE`).
  - Negative balance handling and detection without overflow.
  - Stock deduction prevention for skipped, snoozed, and dismissed dose events.
- **FEFO (First-Expired, First-Out) Lot Allocation**: 100% covered.
  - Exhaustion of earlier expiry lots before newer batches.
  - Handling of non-dated legacy lots and fallback to FIFO.
  - Lot quantity preservation upon dose reversal compensations.
- **Schedule Horizon & Occurrence Identity**: 100% covered.
  - Deterministic `occurrenceId` generation (`{medId}_{date}_{minuteOfDay}`).
  - Schedule types verified: Daily, Specific Weekdays, Interval Days, and Cyclic Regimens.
- **Adherence Calculation Engine**: 100% covered.
  - Mathematical adherence formula: `(Confirmed + Assumed) / Eligible * 100`.
  - Exclusion of future scheduled doses from eligible denominator.
  - Accurate categorization of `confirmedCount`, `assumedCount`, `skippedCount`, and `missedCount`.

---

## 2. Persistence, Database & Security Coverage

### 2.1 Room SQLite DAOs & Migrations (Category B)
- **Room DAOs**: 100% covered.
  - CRUD operations across `MedicationDao`, `ScheduleDao`, `DoseDao`, `StockLotDao`, `StockLedgerDao`, and `PrescriptionDao`.
  - Transactional atomicity: Verified that exceptions during multi-table insertions cleanly trigger database rollback.
  - Foreign key constraints: CASCADE deletion and parent-child integrity validated across all tables.
- **Database Migrations (v1 -> v2 -> v3)**: 100% covered.
  - Schema v1 to v2: Creation of `stock_lots` table, addition of `lot_id` to ledger, and dose correction audit columns.
  - Schema v2 to v3: Creation of `prescriptions` and `prescription_events` tables with index validation.
  - Zero data loss: Row count and column data preservation confirmed across full migration chain.

### 2.2 Security, Cryptography & Privacy (Category C)
- **Encrypted Local Backups**: 100% covered.
  - AES-256-GCM authenticated encryption with PBKDF2 key derivation (600,000 iterations).
  - Tamper detection: Authenticated tag rejection on modified ciphertext payloads.
  - Passphrase verification: Rejection of incorrect passphrases with clear cryptographic error signals.
  - SAF (Storage Access Framework) JSON export and import deserialization integrity.
- **Manifest & Offline Privacy Invariants**: 100% covered.
  - Strict absence of `android.permission.INTERNET` in merged manifest.
  - Auto-backup exclusion rules: Exclusion of `novelo.db` from unencrypted cloud backups (`backup_rules.xml`).
  - Zero remote tracking SDKs (no Firebase, no Crashlytics, no network analytics).

---

## 3. System, Reminders & Lifecycle Coverage

### 3.1 Scheduling, Alarms & Notifications (Category D)
- **AlarmScheduler**: 100% covered.
  - Exact alarm registration with Android `AlarmManager`.
  - Horizon scanning: Accurate reconciliation of upcoming dose reminders across a 24-hour lookahead window.
  - Snooze handling: Exact 15-minute reminder rescheduling without state distortion.
- **Notification Helpers & System Broadcasts**: 100% covered.
  - Dedicated notification channels: `novelo_medication_reminders` (High Importance) and `novelo_low_stock_alerts` (Default Importance).
  - Notification text builder: BigText and SubText generation formatting stock remaining and GP reorder notices.
  - Manifest broadcast receiver declarations for system reboot (`BOOT_COMPLETED`), package replacement, and time/timezone changes.
- **Periodic Maintenance Worker**: 100% covered.
  - WorkManager `LowStockCheckWorker` execution and deduplication logic.
  - Notification state reset upon restocking above configured threshold.

---

## 4. UI Layer, Accessibility & End-to-End Journeys

### 4.1 ViewModels & Application State (Category E)
- StateFlow unidirectional data flow verified for `DashboardViewModel`, `MedicationViewModel`, `MedicationDetailViewModel`, `AdherenceCalendarViewModel`, and `SettingsViewModel`.
- Reactive state updates on dose confirmation, skipping, inventory restock, recount adjustment, and repeat prescription orders.

### 4.2 Jetpack Compose UI & Accessibility (Category F)
- All main screens tested with Android Compose Testing Rule on real AVD:
  - `HeroDoseCard`: Action buttons (Confirm Take, Skip, Snooze), dose information, and visual styling.
  - `MedicationListScreen`: Medication item cards, stock badges, low-stock warnings, and estimated badges.
  - `MedicationDetailScreen`: Restock dialog, adjust count dialog, edit dialog, and prescription actions.
  - `AdherenceCalendarScreen`: Monthly calendar card, rolling window chips (`7D`, `14D`, `30D`, `90D`), and metric cards.
  - `SettingsScreen`: Theme selection (`System`, `Light`, `Dark`), Privacy & App Lock options, and local backup format cards.
  - Accessibility (WCAG AA): 48x48dp minimum touch targets verified on all interactive components; localized content descriptions verified.

### 4.3 End-to-End User Journeys (Category G)
- 10 comprehensive user journeys (T191–T200) verified:
  - Journey 1: Cold launch to Dashboard schedule summary.
  - Journey 2: Navigation to Medication inventory.
  - Journey 3: Multi-step Add Medication wizard interaction.
  - Journey 4: Navigation to Calendar adherence breakdown.
  - Journey 5: Navigation to Settings configuration.
  - Journey 6: Theme switching workflow.
  - Journey 7: Privacy mode preference persistence.
  - Journey 8: Medication Detail runway inspection.
  - Journey 9: Stock restocking and lot registration end-to-end.
  - Journey 10: Dose recording, ledger debiting, and history auditing.
