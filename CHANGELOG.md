# Changelog

All notable changes to **Novelo** are documented in this file.
Format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### Added
- **Phase 8: Premium Product Experience & Complete UI/UX Overhaul (Refs #3)**:
  - Added theme-aware semantic status color system (`NoveloSemanticColors`, `LocalNoveloColors`, `MaterialTheme.noveloColors`) in `Color.kt` and `Theme.kt` so all status badges, alert banners, stock pills, and calendar cells automatically resolve dark-mode or light-mode palettes with $\ge 5.2:1$ WCAG AA contrast.
  - Added centralized shape (`Shape.kt`) and motion (`Motion.kt` / `NoveloMotion`) systems providing spring-based progress animations, screen transitions, Hero Dose -> Completed Day transitions, and collapsible section animations.
  - Replaced single-scroll Add Medication dialog with a progressive-disclosure **3-Step Medication Wizard** (`1. Identity`, `2. Schedule`, `3. Stock`) featuring smart `MedicationForm` -> `StockUnit` defaulting, visual accent color swatches, weekday selector (`Mon–Sun`), preset + custom 24-hour (`HH:MM`) time picker, and regimen summary preview.
  - Added in-place **Edit Medication** (`EditMedicationDialog`) and **Edit Schedule** (`EditScheduleDialog`) flows on `MedicationDetailScreen`, backed by `MedicationRepository.updateSchedule()` and automatic `AlarmScheduler` reconciliation.
  - Added instant search and low-stock quick filter bar + segmented `Active / Archived` pill switcher on `MedicationListScreen`.
  - Added live **Ledger Impact Preview** banners inside `RestockDialog`, `AdjustStockDialog`, and `CorrectDoseDialog` showing `Current Balance -> New Balance` and exact ledger movement deltas before confirmation.
  - Added visual repeat progress bar (`LinearProgressIndicator`) in `PrescriptionOverviewSection` and vertical event timeline in `PrescriptionHistoryDialog`.
  - Added chronological date section grouping (`Today`, `Yesterday`, `EEEE, d MMMM yyyy`) in `MedicationDoseHistoryScreen`.
  - Added 3-column visual theme selector cards (`System`, `Light`, `Dark`) and collapsible `Reminder & Alarm Diagnostics` in `SettingsScreen`.
  - Added complete UI/UX audit and design documentation in `docs/ui-ux-audit/` (`SCREEN_INVENTORY.md`, `ISSUES.md`, `DESIGN_SYSTEM.md`, `UX_DECISIONS.md`, `USER_JOURNEYS.md`, `FINAL_AUDIT.md`) and before/after screenshot evidence in `docs/screenshots/phase8/`.
- **Phase 7.1: CI, Evidence & Signed Release Validation (Refs #1)**:
  - Added GitHub Actions CI workflow in `.github/workflows/ci.yml` supporting triggers on `main`, `release/**`, and `chore/1-phase-07-release-hardening` with jobs for unit tests, lint, release build, and zero-network manifest verification.
  - Formulated and published exact 24-point WCAG 2.1 Level AA Contrast Calculation Matrix in `docs/ACCESSIBILITY_AUDIT.md`. Remediated light mode status color pairs (`StatusTaken`, `StatusAssumed`, `StatusSkipped`, `StatusWarning`) in `Color.kt` to guarantee all pairs exceed the 4.5:1 threshold (5.02:1 to 7.58:1 in light mode, 5.23:1 to 8.25:1 in dark mode).
  - Expanded automated Compose semantics test suite in `AccessibilityTest.kt` to 8 tests covering `AlertBanner`, `CompletedDayCard`, and `MedicationItemCard` touch targets ($\ge 48\times 48\text{dp}$) and TalkBack labels (all 8 tests passing; 26/26 on-device tests passing).
  - Configured project Proguard keep rules in `app/proguard-rules.pro` and added `android.enableR8.fullMode=false` to `gradle.properties`, eliminating R8 synthetic class visibility errors (`IllegalAccessError: Class kotlin.sequences.d extended by class lx1 is inaccessible`).
  - Generated external QA keystore outside the repository, signed release APK, and verified APK Signature Scheme v2 & v3 via `apksigner`.
  - Conducted empirical performance profiling on connected Android 16 device: cold startup times via `am start -W`, frame statistics via `dumpsys gfxinfo`, and memory allocations via `dumpsys meminfo` documented in `docs/PERFORMANCE_AUDIT.md`.
- **Phase 7: Release Hardening, Accessibility & Final Quality Assurance (Refs #1)**:
  - Added automated Jetpack Compose Semantics and Accessibility test suite in `app/src/androidTest/java/com/example/nuvilo/ui/AccessibilityTest.kt` verifying WCAG AA touch targets ($\ge 48\times 48\text{dp}$), TalkBack labels, heading navigation, and lock-screen content isolation.
  - Formulated and documented comprehensive audit artifacts: `docs/ACCESSIBILITY_AUDIT.md`, `docs/PERFORMANCE_AUDIT.md`, `docs/RELEASE_CHECKLIST.md`, `docs/DEVELOPMENT_WORKFLOW.md`, and `CONTRIBUTING.md`.
  - Configured project-specific R8 keep rules in `app/src/main/keepRules/rules.keep` for Room SQLite DAOs/entities/migrations, WorkManager workers, SAF cryptographic data models, and broadcast receivers.
  - Achieved an 86.6% release binary size reduction with release APK measuring `3,097,805 bytes (~3.09 MB)`.

### Changed
- Migrated StateFlow UI consumption across all screens and `MainActivity` from `collectAsState()` to lifecycle-aware `collectAsStateWithLifecycle()` to eliminate background flow emissions and conserve battery.
- Upgraded `androidx.test.espresso:espresso-core` to `3.7.0` and `androidx.test.ext:junit` to `1.3.0` for full compatibility with Android 16 (API 37) input manager architectures.
- Updated deprecated dropdown menu anchors in `MedicationListScreen.kt` to `ExposedDropdownMenuAnchorType.PrimaryNotEditable`.
- Enforced Compose modifier parameter convention (`modifier: Modifier = Modifier` as first optional parameter) across all UI screens and components.
- Enforced strict $\ge 48\text{dp}$ touch target bounds (`heightIn(min = 48.dp)`) across action buttons in `HeroDoseCard`, `TimelineDoseItem`, and `EmptyStateCard`.
- Disentangled composite card semantics in `HeroDoseCard` and `TimelineDoseItem` to allow TalkBack users independent access to "Take", "Skip", and "Snooze" buttons.
- Explicitly stripped `android.permission.ACCESS_NETWORK_STATE` from merged manifests using `tools:node="remove"`, ensuring 100% zero-network boundary.
- Removed redundant activity label and resolved all `DefaultLocale`, `UnusedResources`, and version catalog lint warnings.
- **Phase 6: Privacy, Biometric Lock & Local SAF Export/Restore**:
  - Implemented AndroidX `BiometricPrompt` on `FragmentActivity` architecture supporting biometric credentials across API 24–37.
  - Implemented `BiometricAuthManager` supporting `BIOMETRIC_STRONG` (fingerprint, face) and `DEVICE_CREDENTIAL` (PIN/pattern/password fallback) without negative button conflict.
  - Implemented `AppLockManager` with monotonic clock (`elapsedRealtime()`) tracking grace periods (`Immediately`, `30s`, `1m`, `5m`, `15m`), cold-start lock enforcement, and system dialog suppression.
  - Implemented accessible full-screen `LockScreen` overlay completely omitting underlying sensitive medication UI from the Compose composition tree.
  - Implemented dynamic window `FLAG_SECURE` management for recent apps switcher masking and active lock protection.
  - Implemented local JSON export encrypted with PBKDF2-HMAC-SHA256 (100,000 iterations, 16-byte random salt) and AES-256-GCM (12-byte random nonce, 128-bit authentication tag) via `BackupCrypto`.
  - Implemented full-fidelity RFC 4180 CSV ZIP bundle export (`manifest.json` + 7 CSV tables with null token `\N`) via `CsvZipBackupManager`.
  - Implemented Storage Access Framework (SAF) `CreateDocument` and `OpenDocument` launchers (zero broad storage permissions requested).
  - Implemented `BackupValidator` verifying format/schema versions, primary key uniqueness, and foreign key referential integrity before database mutation.
  - Implemented atomic transactional restore inside `NoveloDatabase.withTransaction` with 100% rollback guarantee on failure.
  - Implemented post-restore reminder reconciliation sync via `AlarmScheduler`.
  - Configured `android:allowBackup="false"` in `AndroidManifest.xml` and excluded `database`, `sharedpref`, `root`, and `file` from cloud backups and device transfers in `backup_rules.xml` and `data_extraction_rules.xml`.
  - Added unit test suites (`AppLockManagerTest`, `BackupCryptoTest`, `BackupSerializationTest`) and connected integration tests (`BackupRestoreDatabaseTest`).
- **Phase 5: Premium Healthcare UI/UX & Design System Implementation**:
  - Implemented cohesive central design tokens in `Color.kt` and `Theme.kt`: NHS-inspired blue primary (`#005EB8`), Dark Mode Slate Navy (`#121820` surface, `#0C1015` background), and semantic status colors with high-contrast container pairs.
  - Developed reusable component library in `NoveloComponents.kt`: `HeroDoseCard`, `TimelineDoseItem`, `CompletedDayCard`, `AlertBanner`, `DoseStatusBadge` (with `✓`, `~`, `×`, `!`), `StockBadge`, `MedicationItemCard`, `EmptyStateCard`, and `SectionHeader`.
  - Implemented hybrid Hero Dose + Chronological Timeline layout in `DashboardScreen.kt`: time-of-day greeting, formatted date, prominent next dose card with Take/Skip/Snooze (+15m) actions, daily adherence progress summary, and chronological today's timeline with vertical track connectors.
  - Refined `MedicationListScreen.kt` and `MedicationDetailScreen.kt`: subtle 1dp border outlines (`outlineVariant`), FEFO lot badges, prescription management, and keyboard-aware dialogs with `Modifier.imePadding()` to prevent virtual keyboard obscuration of Save buttons.
  - Refined `AdherenceCalendarScreen.kt`, `MedicationDoseHistoryScreen.kt`, and `SettingsScreen.kt` with card border strokes, semantic headings (`semantics { heading() }`), TalkBack content descriptions, and appearance switching (System, Light, Dark).
  - Cleaned edge-to-edge window insets in `MainActivity.kt` avoiding double status bar padding.
  - Captured complete before and after visual evidence across all 5 screens in light and dark mode (`docs/screenshots/phase5/`).
- **Phase 4: Adherence History, Calendar & Prescription Tracking**:
  - Implemented pure Kotlin `AdherenceCalculator` with deterministic occurrence resolution, strict denominator validation (eligible scheduled occurrences, excluding future occurrences), distinct confirmed (`TAKEN`) vs assumed (`ASSUMED`) calculations, and reversal awareness.
  - Implemented `AdherenceCalendarScreen` with interactive monthly grid, accessible indicators (`✓`, `~`, `×`, `!`), mixed-outcome day support, daily schedule breakdowns, and period analytics cards.
  - Implemented `MedicationDoseHistoryScreen` with status and date preset filtering, distinct display of scheduled vs recorded timestamps, and one-click dose corrections.
  - Implemented `PrescriptionManager` and `PrescriptionRepository` for tracking prescriptions, authorized repeats, refill requests, and collections with duplicate action key idempotency and expiry guards.
  - Upgraded `NoveloDatabase` to version 3 with `prescriptions` and `prescription_events` tables and non-destructive `MIGRATION_2_3`.
  - Added "Calendar" tab (`Screen.HISTORY`) to bottom navigation in `MainActivity.kt`.
  - Added unit test suites (`AdherenceCalculatorTest`, `PrescriptionDomainTest`) and connected integration tests (`NoveloDatabaseMigrationTest` 2->3, `prescriptionRepository_supportsCreateUpdateRequestCollectAndAuditHistory`).
- **Phase 3: Reliable Android Medication Notifications**:
  - Implemented `AlarmSchedulerImpl` using `AlarmManager.setExactAndAllowWhileIdle()` with fallback and bounded 3-day scheduling horizon.
  - Implemented `NotificationHelper` with dedicated channels (`novelo_medication_reminders` and `novelo_low_stock_alerts`), lock screen privacy masking, and interactive actions (Take, Skip, Snooze).
  - Implemented `NotificationActionReceiver` (`android:exported="false"`) executing idempotent dose confirmations and compensating reversals.
  - Implemented `BootAndChangeReceiver` (`android:exported="true"`) for alarm recovery across reboots and clock adjustments.
  - Implemented `LowStockCheckWorker` for periodic background low-stock evaluation with alert deduplication.
  - Added system diagnostics and manual reconciliation triggers in `SettingsScreen`.
- **Phase 2: Medication Details & Inventory Management**:
  - Implemented `MedicationDetailScreen` showing real-time stock balances, FEFO lot breakdown, supply expiry countdowns, and recent dose history.
  - Implemented lot tracking with `StockLotEntity`, `StockLotDao`, and FEFO lot depletion in `StockLedgerCalculator`.
  - Added `RestockDialog`, `AdjustStockDialog`, and `CorrectDoseDialog` with auditable compensating ledger entries.
  - Upgraded `NoveloDatabase` to version 2 with `MIGRATION_1_2` and automated migration verification.
- **Phase 1: Application Foundation & Core Domain**:
  - Pure Kotlin domain models (`Medication`, `Schedule`, `DoseEvent`, `StockLedgerEntry`, `ScaledQuantity`).
  - Implemented mathematical `StockLedgerCalculator` with scaled integer arithmetic (zero floating-point math).
  - Implemented deterministic `ScheduleOccurrenceCalculator` for recurring schedules.
  - Implemented Room database v1 (`NoveloDatabase`) with DAOs and foreign key referential integrity.
  - Implemented DataStore user preferences repository (`UserPreferencesRepository`).
  - Implemented NHS-inspired Material 3 design system and core screens (`DashboardScreen`, `MedicationListScreen`, `SettingsScreen`).
- **Phase 0: Environment, Agent Skills & Architectural Foundation**:
  - Environment inspection and baseline build pass verified (AGP 9.4.1, Kotlin 2.2, JDK 25).
  - Configured 21 curated agent skills spanning official Android skills, Matt Pocock's engineering skills, Superpowers framework, and supplementary community skills in `.agents/skills/`.
  - Created custom domain safety skill `.agents/skills/novelo-medication-safety/SKILL.md` enforcing dose idempotency and stock ledger invariants.
  - Created persistent project rules: `AGENTS.md`, `.agents/rules/android-quality.md`, and `.agents/rules/offline-privacy.md`.
  - Established project documentation: `ARCHITECTURE.md`, `GLOSSARY.md`, `IMPLEMENTATION_PLAN.md`, `TESTING.md`, `SECURITY.md`, and `SKILLS_INVENTORY.md`.
  - Added Architecture Decision Records (ADRs 0001–0006) in `docs/decisions/`.
  - Formulated UI/UX design specifications, NHS-inspired design system tokens, and dashboard layout comparisons in `docs/design/`.
  - Authored comprehensive Product Requirements Document in `docs/product-requirements.md`.
