# Novelo Phased Implementation Plan

This plan organizes the implementation of Novelo into sequential, verifiable work packages. Each phase lists clear prerequisites, implementation tasks, acceptance criteria, test requirements, and a verification checklist.

---

## Phase 0: Environment, Skills & Documentation Foundation (Current Phase)
- **Status**: Completed / Ready for Review
- **Deliverables**:
  - [x] Environment inspection (Gradle 9.6, AGP 9.4.1, JDK 25, compile/target SDK 37, min SDK 24).
  - [x] Baseline build & unit test pass confirmed (`BUILD SUCCESSFUL in 2m 22s`).
  - [x] Agent skills inventory and installation (`.agents/skills/` with 21 curated skills + custom skill).
  - [x] Persistent project instructions (`AGENTS.md`, `.agents/rules/`).
  - [x] Custom domain safety skill (`.agents/skills/novelo-medication-safety/SKILL.md`).
  - [x] Technical documentation (`ARCHITECTURE.md`, `GLOSSARY.md`, `TESTING.md`, `SECURITY.md`, `SKILLS_INVENTORY.md`).
  - [x] Initial Architecture Decision Records (ADRs 0001–0006).
  - [x] UI/UX design foundation and dashboard layout alternatives.
  - [x] Formal Product Requirements Document (`docs/product-requirements.md`).

---

## Phase 1: Data Architecture, Stock Ledger & Domain Models
- **Goal**: Implement the immutable Room SQLite database layer, DataStore preferences, domain models, and core mathematical ledger engine.
- **Dependencies**: Phase 0.
- **Work Packages**:
  1. **Dependencies & Version Catalog**: Add Room (`androidx.room`), Room KSP, DataStore Preferences, and Kotlinx Serialization to `libs.versions.toml` and `app/build.gradle.kts`.
  2. **Domain Entities & Models**:
     - `Medication`, `MedicationForm`, `StockUnit`, `ScheduleDefinition`.
     - `DoseOccurrence`, `DoseEvent`, `DoseStatus` (`CONFIRM` vs `AUTO_ASSUME`).
     - `StockLedgerEntry`, `MovementType` (`OPENING_BALANCE`, `RESTOCK`, `DOSE_TAKEN`, `ADJUSTMENT`).
  3. **Room Database & DAOs**:
     - `MedicationEntity`, `ScheduleEntity`, `DoseEventEntity`, `StockLedgerEntryEntity`, `PrescriptionEntity`.
     - Schema configuration with Room foreign keys, indices, and room-schema export.
     - `NoveloDatabase` with TypeConverters for Instant, Enums, and ScaledInteger.
  4. **Stock Ledger Engine (TDD)**:
     - Pure Kotlin `StockLedgerCalculator` with exact scaled integer arithmetic.
     - Invariant enforcement: no negative unconfirmed balances, no floating-point math, exact days remaining calculations.
  5. **DataStore User Preferences**:
     - Repository for theme mode, biometric lock preference, quiet hours, and notification privacy mode.
- **Acceptance Criteria**:
  - All Room DAOs tested with in-memory SQLite database.
  - Unit tests verify stock calculations across hundreds of simulated ledger movements.
  - Build succeeds without warnings; schemas exported to `app/schemas/`.

---

## Phase 2: Medication Management & Idempotent Dose Engine
- **Goal**: Implement Medication CRUD, flexible units, and the transactional dose recording use cases.
- **Dependencies**: Phase 1.
- **Work Packages**:
  1. **Medication Repository & Use Cases**:
     - `CreateMedicationUseCase`, `UpdateMedicationUseCase`, `ArchiveMedicationUseCase`.
     - Validations: non-empty name, valid unit, non-negative opening stock.
  2. **Idempotent Dose Recording Engine**:
     - `RecordDoseUseCase`: Enforces unique `occurrenceId`, transactional stock deduction, handling `TAKEN`, `ASSUMED`, `SKIPPED`.
     - `ReconcileHistoricalDoseUseCase`: Reverts stock deductions via auditable compensating adjustments.
  3. **Stock Management Use Cases**:
     - `RestockMedicationUseCase`: Adds new stock lot with optional expiry date.
     - `AdjustStockUseCase`: Adds manual audit correction with user reason.
- **Acceptance Criteria**:
  - Calling `RecordDoseUseCase` repeatedly with the same `occurrenceId` executes exactly once and does not duplicate deductions.
  - Skipping a dose records adherence but causes zero stock deduction.
  - Historical edits generate clear adjustment movements in the ledger.

---

## Phase 3: Scheduling, AlarmManager & Reliable Notifications
- **Goal**: Implement flexible schedule calculations, exact AlarmManager notifications, wakeful broadcast receivers, and privacy masking.
- **Dependencies**: Phase 2.
- **Work Packages**:
  1. **Schedule Occurrence Generator**:
     - Generates deterministic dose occurrences for daily, multiple-times-per-day, and specific weekday patterns.
     - Deterministic `occurrenceId` generation based on medication ID and scheduled epoch.
  2. **Alarm Scheduling Engine**:
     - `AlarmScheduler`: Registers exact alarms via `AlarmManager.setExactAndAllowWhileIdle()`.
     - Android 12+ permission handling (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`).
  3. **Notification Shade & Actions**:
     - Foreground notifications with "Take", "Snooze", "Skip" actions.
     - `NotificationActionReceiver`: Executes dose use case directly from pending intent without opening activity.
     - Privacy Mode: Masking medication name/dosage when lockscreen privacy is toggled.
  4. **System Event Reconciliation**:
     - `BootAndChangeReceiver`: Listens to `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET`, `MY_PACKAGE_REPLACED`, re-registering all upcoming alarms.
- **Acceptance Criteria**:
  - Alarms fire accurately in test environments.
  - Notification action "Take" deducts stock and dismisses notification.
  - Device reboot or timezone change triggers full alarm reconciliation without data loss.

---

## Phase 4: History, Calendar & Prescription Tracking
- **Status**: Completed / Verified
- **Dependencies**: Phase 3.
- **Work Packages**:
  1. [x] **History & Adherence Engine (`AdherenceCalculator.kt`)**:
     - Query dose events by day, week, month with computed adherence rates.
     - Accurate denominator: evaluates eligible past scheduled occurrences (`scheduledEpochMs <= evaluationEpochMs`), excluding future occurrences.
     - Never invents history prior to medication start date or adherence tracking start.
     - Evaluates unrecorded past occurrences as `MISSED` without recommending catch-up or double doses.
     - Clear distinction between confirmed (`TAKEN`) and assumed (`ASSUMED`) doses.
     - Corrected or reversed doses (`DOSE_REVERSAL`) accounted for without double counting.
     - Unit tests in `AdherenceCalculatorTest.kt` (7 tests covering standard, DST transitions, corrections, and mixed schedules).
  2. [x] **Monthly Adherence Calendar Screen (`AdherenceCalendarScreen.kt`)**:
     - Interactive month grid with previous, next, and today month navigation.
     - Accessible indicators pairing color with high-contrast text symbols (`✓`, `~`, `×`, `!`) and descriptive TalkBack labels.
     - Handles mixed-outcome days (e.g. morning taken, evening missed) without marking an entire day as missed.
     - Detailed selected-date dose breakdown showing schedule time, medication name, dose quantity, status, and recording timestamps.
     - Period analytics summary cards: Today, Last 7 Days, and selectable 14d / 30d / 90d periods with adherence percentage, taken/assumed counts, and calculation explanations.
  3. [x] **Dedicated Medication Dose History Screen (`MedicationDoseHistoryScreen.kt`)**:
     - Chronological dose log accessible from `MedicationDetailScreen` ("View Full History").
     - Filter chips for outcome (`ALL`, `TAKEN`, `ASSUMED`, `SKIPPED`, `MISSED`) and date presets (`7 Days`, `30 Days`, `90 Days`, `All Time`).
     - Distinct display of scheduled dose time vs actual user recording / correction time.
     - One-click dose correction dialog with reason tracking and compensating ledger movements.
  4. [x] **Prescription & Repeat Tracking System (`PrescriptionManager.kt`, `PrescriptionRepository`)**:
     - Room v3 schema with `prescriptions` and `prescription_events` tables and non-destructive `MIGRATION_2_3`.
     - Data models capturing authorised repeats, remaining repeats, issue date, expiry date, doctor, pharmacy, notes, and status (`ACTIVE`, `EXPIRED`, `COMPLETED`, `ARCHIVED`).
     - Repeat request neutrality: requesting a repeat records an audit event but does NOT decrement remaining repeats or alter stock.
     - Repeat collection decrement: collecting a repeat decrements remaining repeats by exactly 1 (never below 0) and creates an immutable audit event.
     - Expiry enforcement: expired prescriptions cannot be presented as active or allow repeat requests/collections.
     - Idempotency guard: duplicate action keys prevent repeated network-free clicks from multiple decrements.
     - UI integration: Prescription overview card in `MedicationDetailScreen`, Add/Edit prescription dialog, and immutable audit event history dialog.
- **Acceptance Criteria**:
  - [x] Calendar shows accurate status symbols for taken, assumed, skipped, and missed doses.
  - [x] All 32 unit tests pass (`testDebugUnitTest`).
  - [x] All 15 connected instrumentation tests pass (`connectedDebugAndroidTest`).
  - [x] Room schema export `3.json` validated via automated migration test.
  - [x] Android Lint reports zero errors.

---

## Phase 5: Premium Healthcare UI/UX & Design System Implementation
- **Status**: Completed / Verified
- **Dependencies**: Phases 1–4.
- **Work Packages**:
  1. [x] **Design System & Components (`Color.kt`, `Theme.kt`, `NoveloComponents.kt`)**:
     - Color tokens: NHS-inspired blue (`#005EB8`), Dark Mode Slate Navy (`#121820`), semantic status colors with high-contrast text/container pairs (`StatusTaken`, `StatusAssumed`, `StatusSkipped`, `StatusMissed`, `StatusWarning`).
     - Typography & elevation: M3 rounded shapes (16dp cards, 12dp tiles), subtle 1dp border strokes (`outlineVariant.copy(alpha = 0.5f)`).
     - Central reusable components: `HeroDoseCard`, `TimelineDoseItem`, `CompletedDayCard`, `AlertBanner`, `DoseStatusBadge` (with `✓`, `~`, `×`, `!`), `StockBadge`, `MedicationItemCard`, `EmptyStateCard`, `SectionHeader`.
  2. [x] **Screen Implementations & Refinements**:
     - **Today Dashboard (`DashboardScreen.kt`)**: Implemented hybrid Hero Dose + Chronological Timeline layout. Displays personalized time-of-day greeting + formatted date, low-stock / prescription renewal alert banner, prominent Hero Dose card with Take/Skip/Snooze (+15m) actions, reassuring Completed Day state, compact Daily Adherence progress summary, and chronological today's timeline with vertical track connectors.
     - **Medication List & Detail Screens (`MedicationListScreen.kt`, `MedicationDetailScreen.kt`)**: Polished typography, active/archived treatment tabs, FEFO lot badges, prescription tracking, and soft-keyboard-aware dialogs with `Modifier.imePadding()` to prevent button obscuration.
     - **Adherence & Calendar Screen (`AdherenceCalendarScreen.kt`)**: Refined month calendar grid with accessible status symbols, selected date breakdown, period analytics (Today, 7-day, 14d, 30d, 90d), and calculation explanations.
     - **Medication Dose History Screen (`MedicationDoseHistoryScreen.kt`)**: Filter chips for date and outcome, scheduled vs actual recording timestamp separation, and click-to-correct dialog.
     - **Settings Screen (`SettingsScreen.kt`)**: Appearance selection (System, Light NHS Blue, Dark Slate Navy), privacy guarantee banner, real-time exact alarm diagnostics, alarm reconciliation, and test alert trigger.
  3. [x] **Edge-to-Edge & Accessibility Audit**:
     - Clean window insets without double status-bar padding.
     - WCAG AA touch targets (`>= 48x48dp`) and contrast ratios verified.
     - Semantic headings (`semantics { heading() }`) for screen readers.
     - Before and after screenshots captured in `docs/screenshots/phase5/`.
- **Acceptance Criteria**:
  - [x] Edge-to-edge implemented cleanly on Android 15/16/37 without double padding or navigation bar overlap.
  - [x] TalkBack accessibility semantics verified with localized descriptions and heading tags.
  - [x] Light and Dark modes render with full contrast and visual polish (verified with `#121820` slate dark mode screenshots).
  - [x] All 32 unit tests pass (`testDebugUnitTest`).
  - [x] All 15 connected instrumentation tests pass (`connectedDebugAndroidTest`).
  - [x] Android Lint reports zero errors (`lintDebug`).
  - [x] Debug APK builds and installs cleanly (`assembleDebug`).

---

## Phase 6: Privacy, App Lock & Local SAF Export [COMPLETED]
- **Goal**: Implement biometric app lock, local encrypted JSON and CSV ZIP export, and strict offline backup rules.
- **Dependencies**: Phase 5.
- **Work Packages**:
  1. **Biometric & Credential App Lock**:
     - [x] AndroidX `BiometricPrompt` integration with `BIOMETRIC_STRONG` and `DEVICE_CREDENTIAL` PIN/pattern/password fallback.
     - [x] `FragmentActivity` architecture supporting biometric callbacks across all Android API levels (24+).
     - [x] Dedicated, accessible `LockScreen` overlay completely masking sensitive medication UI in composition.
     - [x] Central testable `AppLockManager` with monotonic clock (`elapsedRealtime()`) avoiding clock-tampering vulnerabilities.
     - [x] Configurable grace periods: `Immediately`, `30 seconds`, `1 minute`, `5 minutes`, `15 minutes`.
     - [x] Dynamic window `FLAG_SECURE` management for recent apps switcher masking and active lock screen protection.
  2. **Data Extraction & Backup Privacy Rules**:
     - [x] Configured `android:allowBackup="false"` in `AndroidManifest.xml`.
     - [x] Configured `data_extraction_rules.xml` and `backup_rules.xml` strictly excluding `database`, `sharedpref`, `root`, and `file` from cloud backups and device transfers.
     - [x] Maintained strict zero-network policy (`android.permission.INTERNET` forbidden).
  3. **Local Export & Import (Storage Access Framework)**:
     - [x] Full-fidelity JSON export encrypted with PBKDF2-HMAC-SHA256 (100,000 iterations, 16-byte salt) and AES-256-GCM (12-byte nonce, 128-bit authentication tag).
     - [x] Full-fidelity CSV ZIP bundle export (`manifest.json` + 7 RFC 4180 CSV tables with null token `\N`).
     - [x] SAF `CreateDocument` and `OpenDocument` launchers (zero broad storage permissions requested).
     - [x] Pre-restore structural and referential integrity validator (`BackupValidator`).
     - [x] Atomic transactional restore inside `NoveloDatabase.withTransaction` with 100% rollback guarantee on failure.
     - [x] Automatic reminder rescheduling reconciliation upon successful restore.
- **Acceptance Criteria**:
  - [x] App locks securely upon backgrounding when enabled and starts locked on fresh process launch.
  - [x] Encrypted JSON and unencrypted CSV ZIP export safely via SAF to external storage/Drive.
  - [x] Zero network permissions verified in merged manifest.
  - [x] All 49 JVM unit tests pass (`testDebugUnitTest`).
  - [x] All 18 connected Android instrumentation tests pass (`connectedDebugAndroidTest`).
  - [x] Android Lint reports zero errors (`lintDebug`).
  - [x] Debug APK builds and installs cleanly (`assembleDebug`).

---

## Phase 7: Verification, Hardening & Final Release Polish
- **Goal**: Run static analysis, comprehensive automated test suites, performance benchmarks, and release readiness.
- **Dependencies**: All prior phases.
- **Work Packages**:
  1. **Static Analysis & Lint**: Android Lint, detekt, dependency check.
  2. **Automated Test Suite**:
     - 100% unit test coverage on domain use cases and ledger calculations.
     - Compose UI tests for core flows (adding medication, taking dose).
  3. **Performance Profiling**:
     - Verify zero frame drops on dashboard scrolling.
     - Memory leak checks on notification receivers and database observers.
- **Acceptance Criteria**:
  - `.\gradlew.bat test` passes with zero failures.
  - `.\gradlew.bat lint` reports zero errors.
  - App runs smoothly on emulator and physical devices.
