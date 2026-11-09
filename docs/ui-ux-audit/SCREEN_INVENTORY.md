# Nuvilo Phase 8 — Complete Screen & Component Inventory

This inventory catalogues every user-facing screen, sub-screen, modal dialog, and secondary state in **Nuvilo** prior to the Phase 8 UI/UX overhaul, documenting its entry routes, state dependencies, identified visual/functional issues, priority, and target resolution status.

---

## Summary Table

| # | Screen / Component | Source File | Navigation / Trigger Route | State / ViewModel | Priority | Status |
|---|---|---|---|---|---|---|
| 1 | **Main Dashboard (Today)** | `ui/screens/DashboardScreen.kt` | Bottom Nav `Screen.TODAY` (default root) | `DashboardViewModel` (`DashboardUiState`) | **P1** | Completed |
| 2 | **Medication List** | `ui/screens/MedicationListScreen.kt` | Bottom Nav `Screen.MEDICATIONS` | `MedicationViewModel` (`MedicationListUiState`) | **P1** | Completed |
| 3 | **Add Medication Wizard** | `ui/screens/MedicationListScreen.kt` | `FloatingActionButton` on `MedicationListScreen` | `MedicationViewModel.createMedication()` | **P0** | Completed |
| 4 | **Medication Details** | `ui/screens/MedicationDetailScreen.kt` | Tap `MedicationItemCard` on List or Dashboard | `MedicationDetailViewModel` (`MedicationDetailUiState`) | **P0** | Completed |
| 5 | **Schedule Creation & Editing** | `ui/screens/MedicationDetailScreen.kt` & `MedicationListScreen.kt` | Step 2 of Add Wizard / `Edit Schedule` action on Medication Detail | `MedicationViewModel` / `MedicationDetailViewModel` | **P0** | Completed |
| 6 | **Stock & Lot Management** | `ui/screens/MedicationDetailScreen.kt` | `Restock` / `Adjust` buttons & Batch Lots section on Detail | `MedicationDetailViewModel.restock()` / `adjustStock()` | **P1** | Completed |
| 7 | **Adherence Calendar** | `ui/screens/AdherenceCalendarScreen.kt` | Bottom Nav `Screen.CALENDAR` | `AdherenceCalendarViewModel` (`AdherenceCalendarUiState`) | **P1** | Completed |
| 8 | **Medication Dose History** | `ui/screens/MedicationDoseHistoryScreen.kt` | `Full Dose History` button on `MedicationDetailScreen` | `MedicationDetailViewModel` (`fullDoseHistory`, filters) | **P1** | Completed |
| 9 | **Dose Correction Dialog** | `ui/screens/MedicationDetailScreen.kt` (`CorrectDoseDialog`) | `Correct` button on `DoseHistoryRow` or `DetailedHistoryItemCard` | `MedicationDetailViewModel.correctDose()` | **P1** | Completed |
| 10 | **Prescription Overview** | `ui/screens/MedicationDetailScreen.kt` (`PrescriptionOverviewSection`) | Embedded section on `MedicationDetailScreen` | `MedicationDetailViewModel` (`prescriptions`) | **P1** | Completed |
| 11 | **Add / Edit Prescription** | `ui/screens/MedicationDetailScreen.kt` (`AddEditPrescriptionDialog`) | `Add Rx` or `Edit` icon in `PrescriptionOverviewSection` | `MedicationDetailViewModel.savePrescription()` | **P2** | Completed |
| 12 | **Repeat Prescription Actions & Audit History** | `ui/screens/MedicationDetailScreen.kt` (`PrescriptionHistoryDialog`) | `Request Repeat` / `Collect Repeat` / `History` in `PrescriptionOverviewSection` | `MedicationDetailViewModel` (`prescriptionEvents`) | **P2** | Completed |
| 13 | **Settings Overview** | `ui/screens/SettingsScreen.kt` | Bottom Nav `Screen.SETTINGS` | `SettingsViewModel` (`userPreferences`, `exportState`, `restoreState`) | **P1** | Completed |
| 14 | **Appearance & Theme Selection** | `ui/screens/SettingsScreen.kt` | Appearance section in `SettingsScreen` | `SettingsViewModel.setThemeMode()` (`ThemeMode`) | **P2** | Completed |
| 15 | **App Lock & Biometric Auth** | `ui/screens/LockScreen.kt` & `SettingsScreen.kt` | Lifecycle lock gate in `MainActivity` + Privacy & Security in `SettingsScreen` | `AppLockManager`, `BiometricAuthManager`, `SettingsViewModel` | **P1** | Completed |
| 16 | **Reminder Settings & Diagnostics** | `ui/screens/SettingsScreen.kt` | System & Reminder Diagnostics section in `SettingsScreen` | `SettingsViewModel` (`canScheduleExactAlarms`, `areNotificationsEnabled`) | **P2** | Completed |
| 17 | **Backup & Export Flows** | `ui/screens/SettingsScreen.kt` | Local Data Backup & Restore section in `SettingsScreen` | `SettingsViewModel.exportEncryptedJson()` / `exportCsvZip()` | **P1** | Completed |
| 18 | **Import & Restore Preview** | `ui/screens/SettingsScreen.kt` | `Restore from Backup File` in `SettingsScreen` | `SettingsViewModel.inspectImportFile()` / `confirmRestore()` | **P1** | Completed |
| 19 | **Global Navigation & App Shell** | `MainActivity.kt` | Root `NoveloMainContent` `Scaffold` & `NavigationBar` | `currentScreen`, `selectedMedicationId`, `showMedicationDoseHistory` | **P0** | Completed |
| 20 | **Secondary UI States & Shared Components** | `ui/components/NoveloComponents.kt` | Shared across all screens (Hero card, timeline row, badges, empty states) | Stateless / parameter-driven composables | **P1** | Completed |

---

## Detailed Screen-by-Screen Audit

### 1. Main Dashboard – Today (`DashboardScreen.kt`)
- **Purpose:** Daily operational hub showing today's adherence progress, low-stock/prescription alerts, the immediate next due dose (`HeroDoseCard`), and the chronological daily schedule timeline.
- **States:** Loading (`CircularProgressIndicator`), Empty (no active schedules), Active with pending Hero Dose, Completed Day (`CompletedDayCard` when all today's doses are resolved), Alert banner visible/hidden.
- **Identified Problems:**
  1. **Flat Header & Static Progress:** Greeting and date lack editorial hierarchy; adherence progress bar jumps abruptly without smooth animation (`animateFloatAsState`).
  2. **Dark Mode Semantic Badge Defect:** `AlertBanner`, `CompletedDayCard`, `DoseStatusBadge`, and `StockBadge` reference hardcoded light-mode container/text colors (`StatusTakenContainer`, `StatusWarningContainer`), causing glaring bright pastel boxes with dark text in Dark Mode.
  3. **Timeline Visual Competition:** Every timeline row is a full card competing visually with `HeroDoseCard` instead of using a connected chronological track with subtle surface hierarchy.
  4. **Missing `contentType` & Formatter Allocations:** `LazyColumn` items lack `contentType`, and `DateTimeFormatter` is instantiated inside `@Composable` bodies on every recomposition.

### 2. Medication List (`MedicationListScreen.kt`)
- **Purpose:** Master directory of all `Active` and `Archived` medications with quick-scan stock balances, recording modes, and archive/restore controls.
- **States:** Loading, Active tab (populated/empty), Archived tab (populated/empty).
- **Identified Problems:**
  1. **No Search or Quick Filter:** Users with multiple medications cannot filter by name, strength, or low-stock status.
  2. **State Loss on Rotation:** `selectedTabIndex` and `showAddDialog` use `remember` instead of `rememberSaveable`.
  3. **Generic Tab Row:** Uses a default flat `TabRow` instead of a tactile segmented pill switcher with count badges.
  4. **Card Information Density:** `MedicationItemCard` uses a small icon and plain text rows without surfacing low-stock runway or color tag accents clearly.

### 3. Add Medication Wizard (`MedicationListScreen.kt`)
- **Purpose:** Onboarding a new medication, its dosage form, unit, recording mode, schedule times/weekdays, and opening stock.
- **States:** Open/Closed dialog, form validation states.
- **Identified Problems:**
  1. **Cramped Single-Dialog Scroll (P0):** Packs 9 fields into a narrow `AlertDialog` with a long vertical scroll.
  2. **Rigid Hardcoded Schedule Options (P0):** Only provides two hardcoded checkboxes (`Morning (08:00)` and `Evening (20:00)`) and passes `emptySet()` for `daysOfWeek`! Users cannot pick custom times, add 3+ daily doses, or configure specific weekdays (`Mon–Sun`).
  3. **No Smart Unit Defaulting:** Selecting `MedicationForm.CAPSULE`, `INHALER`, or `LIQUID` leaves `StockUnit` stuck on `TABLETS` unless manually changed.
  4. **Form State Lost on Rotation:** All input state variables use `remember` instead of `rememberSaveable`.

### 4. Medication Details (`MedicationDetailScreen.kt`)
- **Purpose:** Comprehensive clinical profile for a single medication: identity, stock runway, prescription status, schedule, batch lots, recent doses, and stock ledger audit trail.
- **States:** Loading, Error, Active/Archived medication, empty/populated lots, prescriptions, doses, and movements.
- **Identified Problems:**
  1. **No Way to Edit Medication Details (P0):** Once created, a medication's name, strength, notes, low-stock threshold, or recording mode cannot be edited from `MedicationDetailScreen`.
  2. **Wall of Cards / Information Overload (P1):** Renders 6+ full-width cards and up to 40 list items (`recentDoses` + `recentMovements`) in one long vertical stream without collapsible secondary sections for Batch Lots and Ledger Audit Trail.
  3. **Weekday Schedule Runway Calculation Bug (P1):** `MedicationDetailViewModel` only calculates `dailyConsumptionRaw` for `ScheduleType.DAILY`, ignoring `ScheduleType.SPECIFIC_WEEKDAYS`, resulting in missing `daysRemaining` estimates for weekday schedules.

### 5. Schedule Creation & Editing (`MedicationDetailScreen.kt` & `MedicationListScreen.kt`)
- **Purpose:** Defining when and how much of a medication is taken (daily vs specific weekdays, target times of day, dose quantity per occurrence, instructions).
- **Identified Problems:**
  1. **No Schedule Editing After Creation (P0):** `ScheduleSummaryCard` is read-only; users cannot adjust dose times, dose quantities, or weekday rules if their prescriber changes their regimen.
  2. **Lack of Custom Time Picker:** Users need an intuitive hour/minute time picker and quick preset chips (`08:00`, `12:00`, `14:00`, `18:00`, `20:00`, `22:00`) plus interactive weekday chips (`M T W T F S S`).

### 6. Stock & Medication Lot Management (`MedicationDetailScreen.kt`)
- **Purpose:** Displaying derived stock balance, estimated runway, FEFO batch lots, restocking (`RestockDialog`), and physical recount adjustments (`AdjustStockDialog`).
- **Identified Problems:**
  1. **No Active FEFO Lot Indicator:** `StockLotRow` lists batches but does not highlight which batch is currently being drawn down first under First-Expired, First-Out (`FEFO`) allocation.
  2. **Basic Dialog Previews:** `RestockDialog` does not preview the new post-restock balance or computed expiry date (`d MMM yyyy`), and `AdjustStockDialog` lacks a visual before → after balance comparison card.

### 7. Adherence Calendar (`AdherenceCalendarScreen.kt`)
- **Purpose:** Monthly visual adherence overview, period adherence metrics (`Today`, `7D`, `14D`, `30D`, `90D`), and selected-day dose breakdown with inline action/status view.
- **Identified Problems:**
  1. **Cluttered Day Cell Symbols:** `CalendarDayCell` renders multi-character text strings (`"✓/✕"`, `"~✓"`, `"••"`) inside small cells that wrap or clip under `2.0x` font scaling.
  2. **Abrupt Month Switching:** Month navigation lacks smooth directional `AnimatedContent` transitions.
  3. **Hardcoded Light-Mode Semantic Colors:** Day cells and summary cards hardcode light-mode status colors in dark mode.

### 8. Medication Dose History (`MedicationDoseHistoryScreen.kt`)
- **Purpose:** Filterable audit log of all past and today's dose occurrences for a specific medication, with date range and outcome filter chips.
- **Identified Problems:**
  1. **No Chronological Date Grouping:** All dose occurrences render in a flat list without sticky/sectioned date headers (`Today`, `Yesterday`, `EEE, d MMM yyyy`), making multi-week scanning harder.
  2. **Timestamp Hierarchy:** Scheduled time and actual recorded timestamp are combined into a single dense subtitle string.

### 9. Dose Correction Dialog (`MedicationDetailScreen.kt`)
- **Purpose:** Auditing and revising a historical dose status (`TAKEN`, `SKIPPED`, `MISSED`) with automatic compensating `DOSE_REVERSAL` or `DOSE_TAKEN` stock ledger adjustment.
- **Identified Problems:**
  1. **Plain Radio List:** Uses basic radio buttons without visual status badges or an explicit "Stock Impact Preview" banner showing the exact `+qty` or `-qty` ledger movement that will be recorded.

### 10–12. Prescription Overview, Add/Edit Prescription & Repeat Audit History (`MedicationDetailScreen.kt`)
- **Purpose:** Personal tracking of prescription issue/expiry dates, repeats authorised vs remaining, pending repeat requests, pharmacy details, and immutable repeat event history.
- **Identified Problems:**
  1. **No Visual Repeat Progress Bar:** Repeats remaining is shown only as text (`"3 of 6 authorised"`) without a visual progress indicator.
  2. **Request vs Collect Clarity:** The two-step workflow (`Request Repeat` → `Awaiting Collection` → `Collect Repeat`) can be made much clearer with a step indicator and optional quick prompt to restock supplies upon collection.
  3. **Audit Log Formatting:** `PrescriptionHistoryDialog` renders audit events as plain text paragraphs instead of a structured vertical timeline.

### 13–18. Settings, Appearance, App Lock, Diagnostics, Backup & Restore (`SettingsScreen.kt` & `LockScreen.kt`)
- **Purpose:** Managing theme mode, notification privacy masking, biometric/credential app lock, grace period, recents masking, encrypted JSON / CSV ZIP backups, transactional restore preview, and system alarm/notification diagnostics.
- **Identified Problems:**
  1. **Appearance Radio List:** Theme selection uses 3 plain radio rows instead of a tactile 3-card visual theme selector (`System`, `Light`, `Dark`).
  2. **Technical Diagnostics Prominence:** Raw diagnostics (`Exact Alarm Status`, `Reconcile Now`, `Test Alert`) sit prominently above Appearance with dense technical copy; should be cleanly organized with a collapsible technical detail drawer.
  3. **Outdated Footer String:** Footer in `SettingsScreen.kt` still says `"Nuvilo 1.0 (Phase 6 Privacy & Security)"`.
  4. **Lock Screen Polish:** `LockScreen.kt` is functional but can be elevated with a refined shield emblem, subtle pulsing/spring entrance, and clearer privacy reassurance badge.

### 19 & 20. Global Navigation Shell (`MainActivity.kt`) & Shared Components (`NoveloComponents.kt`)
- **Purpose:** Top-level navigation bar, screen routing, back-stack handling, lock overlay, and reusable UI building blocks.
- **Identified Problems:**
  1. **Navigation State Reset on Configuration Change (P0):** `currentScreen`, `selectedMedicationIdState`, and `showMedicationDoseHistory` use non-saveable `remember`/`mutableStateOf`, resetting the user to `Today` when rotating the screen or toggling dark mode!
  2. **No Screen Transition Motion:** Switching tabs or opening `MedicationDetailScreen` / `MedicationDoseHistoryScreen` snaps instantaneously without a refined fade/slide transition.
  3. **Navigation Bar Styling:** Uses default Material 3 indicator without subtle top hairline divider or distinct selected/unselected icon states (`Outlined` vs `Filled`).
