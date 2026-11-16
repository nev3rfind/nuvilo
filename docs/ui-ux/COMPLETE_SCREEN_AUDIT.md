# Nuvilo Phase 11 — Complete Screen, Dialog, Notification & UX Audit

**Author:** Agent 1 (UI/UX Design Director)  
**Device Baseline:** Samsung Galaxy S24 Ultra (`SM-S948B`, `R3GL5059FTZ`, `1080×2340`)  
**BEFORE Screenshots:**
- `docs/ui-ux/screenshots/before/01_dashboard.png` (`DashboardScreen`)
- `docs/ui-ux/screenshots/before/02_medications.png` (`MedicationListScreen`)
- `docs/ui-ux/screenshots/before/03_medication_detail.png` (`MedicationDetailScreen`)
- `docs/ui-ux/screenshots/before/04_adherence.png` (`AdherenceCalendarScreen`)
- `docs/ui-ux/screenshots/before/05_settings.png` (`SettingsScreen`)

---

## 1. Full UI Surface Inventory (Codebase-Verified)

### 1.1 Top-Level Navigation & Shell (`MainActivity.kt`)
- **Screens**: `SplashScreen`, `LockScreen`, `NoveloMainContent` (4-tab `NavigationBar`: `TODAY` -> `DashboardScreen`, `MEDICATIONS` -> `MedicationListScreen`, `CALENDAR` -> `AdherenceCalendarScreen`, `SETTINGS` -> `SettingsScreen`), plus detail stack (`MedicationDetailScreen`, `MedicationDoseHistoryScreen`) and Phase 11 `PpcManagementScreen`.
- **Issues Found**:
  1. **Double Window Insets Padding**: `MainActivity` `Scaffold` passed `innerPadding` into `AnimatedContent`, while inner screens (`DashboardScreen`, `MedicationListScreen`, `AdherenceCalendarScreen`) wrapped their own `Scaffold` without `consumeWindowInsets(innerPadding)` / `WindowInsets(0,0,0,0)` consistently, creating excessive top status-bar whitespace (visible in `01_dashboard.png`).
  2. **Duplicate Navigation Iconography**: `Screen.TODAY` (`Icons.Filled/Outlined.Today`) and `Screen.CALENDAR` (`Icons.Filled/Outlined.CalendarToday`) share nearly identical calendar-box silhouettes in the bottom bar (`01_dashboard.png`).
  3. **TalkBack Duplicate Announcements**: `NavigationBarItem` `Icon` sets `contentDescription = screen.title` alongside `label = { Text(screen.title) }`, causing TalkBack to read `"Today, Today"` / `"Settings, Settings"`.
  4. **Missing PPC Route**: No route or deep-link extra (`extra_open_ppc_management`) exists yet for `PpcManagementScreen`.

### 1.2 `SplashScreen` (`SplashScreen.kt`)
- **Elements**: Radial aura, 3D `ic_novelo_logo`, `"Nuvilo"` wordmark, tagline `"Medication, made manageable."`, gradient progress bar, privacy footnote pill (`"100% Offline · Private On-Device Vault"`), tap-to-skip.
- **Findings**:
  - Already checks `ValueAnimator.areAnimatorsEnabled()` for reduced motion.
  - **Typography**: Progress percentage (`"${(progress * 100).toInt()}%"`) lacks `.tabular`, causing slight horizontal jitter during 0%→100% animation.
  - **Accessibility**: `Image(R.drawable.ic_novelo_logo)` has `contentDescription = "Nuvilo Logo"` while parent `Box` already provides a merged `contentDescription`.

### 1.3 `LockScreen` (`LockScreen.kt`)
- **Elements**: Privacy shield card, `ic_novelo_logo`, `"App Locked · Health Records Protected"` badge, `"Unlock Nuvilo"` primary CTA, fallback `"Open Device Security Settings"` when no credentials are enrolled.
- **Findings**:
  - Zero health data / medication names leaked (compliant).
  - **Geometry**: Primary CTA uses `MaterialTheme.shapes.small` (12dp) instead of `NoveloPillShape` used on `HeroDoseCard` primary CTAs.

### 1.4 `DashboardScreen` (`DashboardScreen.kt` + `NoveloComponents.kt`)
- **Elements**: `DashboardHeaderSection` (logo, date pill, `"Offline Protected"` badge, greeting, `"Today's Adherence Progress"` strip), `AlertBanner` (low stock / expiring Rx / discard warnings), `HeroDoseCard` (next pending dose with Take / Snooze 15m / Skip), `CompletedDayCard`, `SectionHeader`, `TimelineDoseItem` list.
- **Issues Found (Verified in `01_dashboard.png`)**:
  1. **Pluralization Defects**:
     - `HeroDoseCard` renders `"Take 1 drops"` and `"Confirm Dose Taken (1 drops)"` when quantity is `1` (`StockUnit.DROPS`).
     - `HeroDoseCard` renders `"1 Bottles total"` instead of `"1 Bottle total"` when `totalPhysical == 1`.
  2. **Missing NHS PPC Status Card**: No compact NHS Prescription Prepayment Certificate (PPC) / Free Exemption status card on Dashboard.
  3. **Scrollable Header**: `DashboardHeaderSection` is pinned above `LazyColumn`, reducing vertical viewport on smaller screens or large font scales.
  4. **Accessibility**: `DashboardHeaderSection` logo + title + date pill lack a unified semantic group; `TimelineDoseItem` secondary actions should guarantee `>= 48.dp` height.

### 1.5 `MedicationListScreen` (`MedicationListScreen.kt` + `NoveloComponents.kt`)
- **Elements**: Header, `SegmentedMedicationTabs` (`Active (n)` / `Archived (n)`), Search `OutlinedTextField`, `"Low Stock (n)"` `FilterChip`, `MedicationItemCard` list, `ExtendedFloatingActionButton` (`"Add Medication"`), `AddMedicationDialog` (multi-step wizard), `RestockDialog`.
- **Issues Found (Verified in `02_medications.png`)**:
  1. **Double Plus Sign**: `MedicationItemCard` restock `OutlinedButton` has `Icons.Default.Add` AND `text = "+ Stock"`, rendering `+ + Stock`.
  2. **Touch Target Below 48dp**: `MedicationItemCard` restock `OutlinedButton` uses `heightIn(min = 36.dp)` (violates WCAG AA 48dp minimum).
  3. **Schedule Chip Truncation**: `Daily · ...` truncates aggressively on narrow rows (`02_medications.png`).
  4. **Missing Undo on Archive/Restore**: Archiving/restoring a medication happens immediately without an Undo `Snackbar`.
  5. **Missing Safe Delete Entry Point**: No safe delete action from list/detail with historical record check.
  6. **Wizard Step Chips**: Step indicator chips in `AddMedicationDialog` need `>= 48.dp` touch target and clearer step labels.

### 1.6 `MedicationDetailScreen` (`MedicationDetailScreen.kt`)
- **Elements**:
  - `TopAppBar` (Back, Edit `Icons.Default.Edit`, Archive/Unarchive `Icons.Default.Archive`).
  - `MedicationHeroAndStockCard` (active opened bottle, next staged unopened bottle, `"Mark bottle as finished"`, `"Discard Open Bottle"`, `"Recount"`, `"Restock"`, `"Adjust Stock"`).
  - `ScheduleSummaryCard` (`"Edit Schedule"`).
  - `PrescriptionOverviewSection` (`"Audit Log"`, `"+ Add Rx"`, `"1. Request Repeat"`, `"2. Collect Repeat"`, edit/archive Rx icon buttons).
  - `RecentDosesPreviewCard` (`"Full History (n)"`, `"Correct"` button per dose).
  - `CollapsibleBatchLotsSection` (`StockLotRow` list with Recount / Discard / Deplete / Reopen / Edit Lot).
  - `CollapsibleStockLedgerSection` (`StockMovementRow` append-only audit list).
- **Dialogs Inventoried (13 total)**:
  1. `EditMedicationDialog` (L2496) — has `imePadding()` + `verticalScroll`. Missing: administration route, notification toggle, and explicit future-impact note when changing dose/packaging config.
  2. `EditScheduleDialog` (L2986) — has `imePadding()` + `verticalScroll`.
  3. `RestockDialog` (L3088) — has `imePadding()` + `verticalScroll`.
  4. `AdjustStockDialog` (L3391) — has `imePadding()` + `verticalScroll`.
  5. `RecountOpenedPackageDialog` (L3645) — has `imePadding()` + `verticalScroll`.
  6. `DiscardPackageDialog` (L3815) — has `imePadding()` + `verticalScroll`.
  7. `EarlyPackageDepletionDialog` (L3917) — has `imePadding()` + `verticalScroll`.
  8. `ReopenPackageDialog` (L4090) — has `imePadding()` + `verticalScroll`.
  9. `EditPackageDetailsDialog` (L4159) — has `imePadding()` + `verticalScroll`.
  10. `CorrectDoseDialog` (L4280) — has `imePadding()` + `verticalScroll`.
  11. `AddEditPrescriptionDialog` (L4789) — has `imePadding()` + `verticalScroll`, but raw text date entry lacks Material 3 `DatePickerDialog` and quick expiry presets, and lacks `repeatRequestedDate` / `dispensedDate` fields.
  12. `PrescriptionHistoryDialog` (L4982) — has `verticalScroll`.
  13. `RequestRepeatDialog` (L5108) & `CollectRepeatDialog` (L5238) — both have `imePadding()` + `verticalScroll`.
- **Missing Actions**:
  - **Safe Delete Medication** workflow (checking dose history, stock ledger movements, prescription history; preferring Archive when history exists; requiring explicit confirmation if hard-deleting).
  - **Delete Prescription** (or clear archive/delete distinction) with confirmation.

### 1.7 `MedicationDoseHistoryScreen` (`MedicationDoseHistoryScreen.kt`)
- **Elements**: `TopAppBar`, `MedicationAdherenceSummaryBanner`, Date & Outcome `FilterChip` card, grouped chronological `DoseHistoryRow` list, `CorrectDoseDialog`.
- **Findings**:
  - Well-structured `LazyColumn`; ensure `FilterChip` rows and `"Correct"` buttons meet `>= 48.dp` height and use `.tabular` for timestamps/counts.

### 1.8 `AdherenceCalendarScreen` (`AdherenceCalendarScreen.kt`)
- **Elements**: Header + `"Today"` button, `AdherenceOverviewStrip` (KPI card + 3 mini metric cards + `7D/14D/30D/90D` window chips), `MonthlyCalendarCard` (month nav + 7-col grid of `CalendarDayCell` + `CalendarLegendRow`), selected-day `CalendarDayOccurrenceCard` list, `CollapsibleMethodologyCard`.
- **Issues Found (Verified in `04_adherence.png`)**:
  1. **Redundant Punctuation**: `"! Below Clinical Target (80%)"` renders a warning icon followed by a literal `"!"` character.
  2. **Calendar Cell Sizing**: `CalendarDayCell` has `heightIn(min = 48.dp)` and `aspectRatio(0.95f)` which looks crisp, but month transition `AnimatedContent` should respect `LocalMotionEnabled`.

### 1.9 `SettingsScreen` (`SettingsScreen.kt`)
- **Elements**:
  - `Appearance & Theme` (`ThemeOptionCard`: System / Light / Dark).
  - `Privacy & App Lock` (`Biometric / Device Lock`, `Mask Lock-Screen Notifications`, grace period chips, recents preview masking).
  - `Notifications & Haptics` (channel diagnostics, test notification, sound/vibration toggles, haptic intensity).
  - `Local Data Backup & Restore` (Encrypted `.nuvilo.json` & Plaintext `.csv`, `BackupFormatCard`, 4 `AlertDialog`s: Encrypted Export, Plaintext Export Warning, Encrypted Restore, Diagnostics/Import).
- **Issues Found**:
  - Backup/restore `AlertDialog`s need consistent `imePadding()` + `verticalScroll` for passphrase entry on smaller screens.
  - Add quick link / section to **NHS PPC & Prescription Exemptions** (`PpcManagementScreen`) if helpful.

### 1.10 System Notifications (`NotificationHelper.kt`, `NotificationChannelRegistry.kt`, `NotificationActionReceiver.kt`)
- **Channels**: `MEDICATION_REMINDERS` (`VISIBILITY_PRIVATE`), `LOW_STOCK_ALERTS` (`VISIBILITY_PUBLIC`), `SECURITY_EVENTS` (`VISIBILITY_PRIVATE`), plus Phase 11 `PPC_EXPIRY_REMINDERS`.
- **Findings**:
  - Dose reminders support Privacy Mode masking (`"You have a scheduled medication reminder"`), and include `Take`, `Snooze 15m`, `Skip` actions with idempotent `occurrenceId`.
  - Phase 11 adds `PpcAlarmScheduler` for 30d/14d/7d/0d local PPC expiry reminders with deep-link `extra_open_ppc_management`.

---

## 2. Verification Test Matrix Summary
- **Unit Tests**: `PpcCalculatorTest` (3m/12m calendar math, leap years, month-end, future start, expired, ongoing), `MedicationCrudAndSafeDeleteTest`, `NoveloDatabaseMigration4To5Test`, `BackupRestorePpcTest`.
- **UI Instrumentation Tests**: Dashboard PPC banner states, `PpcManagementScreen` 4-step wizard, Medication Edit / Archive + Undo / Safe Delete confirmation, Light & Dark mode contrast/layout checks, Reduced-motion (`LocalMotionEnabled = false`) verification.
- **Offline & Privacy Gate**: Verify merged release `AndroidManifest.xml` has zero `android.permission.INTERNET` and zero remote SDKs.
