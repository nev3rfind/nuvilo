# Nuvilo Phase 8 — Product & UX Architecture Decisions

This document records the key product design, interaction, and architectural decisions made during the Phase 8 UI/UX overhaul of **Nuvilo**.

---

## Decision 1: Theme-Aware Semantic Color System (`LocalNoveloColors`)
- **Problem:** In Phases 1–7, `Color.kt` defined both light and dark semantic status colors (`StatusTaken` / `StatusTakenDark`, `StatusTakenContainer` / `StatusTakenContainerDark`), but UI composables imported the light-mode constants directly. In Dark Mode, badges and banners rendered glaring pastel backgrounds (`#E6F4EA`, `#FEF3C7`, `#FEE2E2`) with dark text on top of dark slate cards (`#131C27`).
- **Decision:** Create an immutable `NoveloSemanticColors` data class provided through a `staticCompositionLocalOf` (`LocalNoveloColors`) inside `NoveloTheme` (`Theme.kt`) and exposed via `MaterialTheme.noveloColors`.
- **Rationale:**
  - Guarantees automatic, zero-boilerplate adaptation of all 5 clinical status palettes (`taken`, `assumed`, `skipped`, `missed`, `warning`) across Light and Dark modes.
  - Preserves strict WCAG AA contrast ($\ge 4.5:1$) in both themes while keeping Dark Mode calm, cohesive, and restful at night.

---

## Decision 2: Progressive Disclosure via a 3-Step Medication Wizard
- **Problem:** `AddMedicationDialog` crammed 9 input controls into a single scrolling `AlertDialog` and restricted schedule creation to two hardcoded times (`08:00` and `20:00`) with no weekday selection.
- **Decision:** Replace the single scrolling dialog with a structured **3-Step Medication Wizard** (`Step 1: Medication Identity & Form`, `Step 2: Schedule & Recording Mode`, `Step 3: Stock & Alerts`) with saveable state and smart form-to-unit defaulting.
- **Rationale:**
  - Reduces cognitive load during medication setup by grouping related clinical questions together.
  - Unlocks the full power of the existing `ScheduleDefinition` domain model (`DAILY` vs `SPECIFIC_WEEKDAYS`, custom `HH:MM` times, multiple daily doses, and per-dose quantities) without requiring any database schema migration.
  - Smartly maps `MedicationForm` (`TABLET`, `CAPSULE`, `LIQUID`, `INHALER`, `DROPS`, `INJECTION`, `TOPICAL`) to the corresponding `StockUnit` automatically while still allowing manual unit selection.

---

## Decision 3: In-Place Editing for Medications & Schedules with Automatic Alarm Reconciliation
- **Problem:** Users could not edit a medication's name, strength, threshold, recording mode, or schedule after initial creation without archiving and recreating the medication (which would orphan historical stock ledger and dose records).
- **Decision:** Add `updateSchedule(schedule: ScheduleDefinition)` to `MedicationRepository` and expose `updateMedication(...)` and `updateSchedule(...)` in `MedicationDetailViewModel`, paired with dedicated **Edit Medication** and **Edit Schedule** dialogs on `MedicationDetailScreen`.
- **Rationale:**
  - Preserves the medication's unique `id`, historical `DoseOccurrence` audit trail, and append-only `StockLedgerEntry` chain while allowing regimens to evolve as prescribers adjust dosages or times.
  - Automatically triggers `alarmScheduler.reconcileUpcomingReminders()` after schedule edits so upcoming exact alarms remain 100% synchronized.

---

## Decision 4: Surface Hierarchy & Collapsible Secondary Audit Sections on `MedicationDetailScreen`
- **Problem:** `MedicationDetailScreen` displayed 6+ equally weighted cards and up to 40 raw audit rows in one long list, causing visual fatigue and burying primary actions (`Restock`, `Adjust`, `Add Rx`).
- **Decision:** Establish a 3-tier surface hierarchy on `MedicationDetailScreen`:
  1. **Tier 1 (Hero Focal Point):** Unified `MedicationHero & Stock Runway` header surfacing identity, recording mode, current balance, estimated runway (`~X days left`), and primary `Restock` / `Adjust` actions.
  2. **Tier 2 (Active Regimen & Prescriptions):** `ScheduleSummaryCard` (with inline `Edit`) and `PrescriptionOverviewSection` (with visual repeat progress bar).
  3. **Tier 3 (Progressive Disclosure Audit Sections):** Recent Doses preview (with `Full History` CTA), plus collapsible accordion cards for **Batch Lots (`FEFO` allocation)** and **Stock Ledger Audit Trail**.
- **Rationale:**
  - Everyday tasks (checking balance, restocking, reviewing schedule, requesting/collecting a repeat) are immediately accessible above the fold, while full batch-lot and ledger transparency remains one tap away.

---

## Decision 5: Before → After Impact Previews on Inventory & Dose Correction Dialogs
- **Problem:** Inventory (`RestockDialog`, `AdjustStockDialog`) and historical audit (`CorrectDoseDialog`) dialogs modified the append-only stock ledger without showing the user the resulting balance or exact compensating movement before confirmation.
- **Decision:** Embed a live **Ledger Impact Preview** banner in all three dialogs showing `Current Balance → New Balance` and the exact ledger movement (`+RESTOCK`, `+ADJUSTMENT_IN` / `-ADJUSTMENT_OUT`, or `+DOSE_REVERSAL` / `-DOSE_TAKEN`).
- **Rationale:**
  - Reinforces user trust in Nuvilo's append-only ledger math and prevents accidental data entry errors before they are committed.

---

## Decision 6: Clean Calendar Day Indicators & Font-Scale Resilience
- **Problem:** `AdherenceCalendarScreen` rendered multi-character text strings (`"✓/✕"`, `"~✓"`, `"••"`) inside compact calendar day cells, causing text wrapping and clipping when Android system font size was scaled up (`1.5x–2.0x`).
- **Decision:** Replace text strings inside calendar day cells with a clean, high-contrast status indicator bar/dot and distinct container tinting, while providing full descriptive strings via `Modifier.semantics { contentDescription = ... }` and the legend below the calendar.
- **Rationale:**
  - Keeps the 7-column calendar grid crisp, balanced, and overflow-free at all font scales while maintaining complete WCAG AA non-color differentiation (via day cell badge + selected-day breakdown + accessible TalkBack descriptions).
