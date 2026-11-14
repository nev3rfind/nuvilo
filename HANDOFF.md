# Nuvilo Engineering Handover Document (HANDOFF.md)

> **Product**: Nuvilo — *"Medication, made manageable."*  
> **Package / Application ID**: `com.nuvilomed.app` (internal namespace: `com.example.nuvilo`)  
> **Repository**: [https://github.com/nev3rfind/nuvilo](https://github.com/nev3rfind/nuvilo)  
> **Active Branch**: `main` (Single source of truth; clean and in sync with `origin/main`)  
> **Latest Verified Commit**: `5a1ef35` (Pre-merge main) / `feat/16-intelligent-medication-inventory` (Phase 9)  
> **Date**: 2026-11-12  

---

## 1. Executive Summary & Repository State

Nuvilo is a native Android, offline-first personal medication tracking and stock management app built with healthcare-grade rigor. Development through **Phases 0 through 9** (including **Room Schema v4**, **Intelligent Real-World Packaging**, **Decoupled Consumption**, **Discrete FEFO Shelf-Life Simulation**, **Haptic-First Feedback**, **7 Notification Channels**, and the **Pre-Merge 200+ Regression Suite**) has been completed, verified on physical/emulated Android 17 (API 37).

### Active Branch & Git Cleanliness
- **Branch**: `feat/16-intelligent-medication-inventory` (Feature branch for Phase 9, Issue #16)
- **Base Branch**: `main`
- **Room Database Schema**: Version 4 (`MIGRATION_3_4`) with packaging and package lifecycle tracking.
- **Backup Schema**: Version 4 (`BackupDataV4`) with bidirectional compatibility (v1, v2, v3, v4).
- **Phase 9 Addendum Architecture**:
  - *Early Package Depletion*: Allows marking an opened container finished ahead of theoretical zero capacity (`depletePackageEarly`), writing off remaining drops strictly as `MovementType.WASTAGE` with zero synthetic dose fabrication.
  - *Option B Persistence Mapping*: Preserves Room Schema v4 by encoding structured depletion reasons and notes in `discard_reason` (`DEPLETED[code]: note`) and `discarded_at_epoch_ms`.
  - *Intelligent Container Staging*: Next eligible unopened package selected via FEFO (`findStagedNextPackage`) and staged as "Ready to Open" without premature auto-opening.
  - *Accidental Depletion Reversal*: `reopenDepletedPackage` applies compensating `MovementType.ADJUSTMENT_IN` and restores container balance and state.
  - *Five-Agent Swarm Audit Suite*: Conducted parallel evaluations across Domain Invariants (`AGENT_1_DOMAIN_AUDIT.md`), Inventory Accounting (`AGENT_2_INVENTORY_REPORT.md`), Notifications & Haptics (`AGENT_3_NOTIFICATIONS_HAPTICS_REPORT.md`), UI/UX Accessibility (`AGENT_4_UI_UX_AUDIT.md`), and Quality Assurance (`AGENT_5_QA_REPORT.md`).
  - *Acceptance Test*: Created `Phase9EarlyDepletionAcceptanceTest.kt` verifying the full Hydro Forte 300+300 drop scenario, early depletion write-off, and accidental reversal.

---

## 2. Test Suite & Quality Status (287 Automated Tests, 100% Passing)

Before any PR, branch, or release claim, all quality gates must pass. As of commit `5a1ef35`, the suite is 100% green:

| Quality Gate | Gradle Task | Target / Environment | Count / Result | Pass Rate | Status |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **JVM Unit Tests** | `.\gradlew.bat testDebugUnitTest` | OpenJDK 21 (Android Studio JBR) | **136 / 136** | 100.0% | **PASSED** |
| **Connected Android Tests** | `.\gradlew.bat connectedDebugAndroidTest` | `emulator-5554` (Android 17 / API 37) | **159 / 159** | 100.0% | **PASSED** |
| **Grand Total Automated Tests** | — | **JVM + Android Instrumentation** | **295 / 295** | **100.0%** | **PASSED** |
| **Static Analysis / Lint** | `.\gradlew.bat lintDebug` | 119 active checks | **0 Errors** | 100.0% | **PASSED** |
| **Release Build & Shrinking** | `.\gradlew.bat assembleRelease` | R8 Minification + ProGuard | APK Generated | Zero Issues | **PASSED** |

---

## 3. Mandatory Safety & Domain Invariants

Any agent modifying medication tracking, stock management, or database logic **MUST** uphold these non-negotiable invariants (consult `.agents/skills/novelo-medication-safety/SKILL.md`):

1. **Zero Network Access**:
   * `android.permission.INTERNET` must **never** be declared or merged.
   * `android.permission.ACCESS_NETWORK_STATE` is explicitly removed via `tools:node="remove"`.
   * Absolutely zero third-party remote SDKs (no Firebase, Sentry, Mixpanel, Crashlytics, Google Analytics, or external AI APIs).
2. **Exact Scaled Integer Arithmetic (No Floating-Point Math)**:
   * **Never use `Float` or `Double`** for medication inventory balances, doses, or ledger transactions.
   * All quantities use `ScaledQuantity` with integer micro-units (`rawScaledValue`) or exact decimal scaling.
3. **Immutable Append-Only Stock Ledger**:
   * Balances are **never** stored as unbacked mutable integers that can drift.
   * Stock is deterministically computed from:  
     $$\text{Current Stock} = \text{Opening Balance} + \sum(\text{Restocks}) - \sum(\text{Deductions}) \pm \sum(\text{Adjustments})$$
   * Manual recounts insert an `ADJUSTMENT_IN` or `ADJUSTMENT_OUT` ledger entry with the exact delta.
   * Retrospective dose corrections log compensating `DOSE_REVERSAL` movements.
4. **Dose Recording Preferences**:
   * `CONFIRM`: Requires explicit confirmation before recording a dose as taken.
   * `AUTO_ASSUME`: Records assumed adherence according to schedule; clearly distinguished from verified ingestion in adherence reports.
   * **Occurrence Identity**: Deterministic `occurrenceId` ensures repeated clicks or alarms never duplicate dose events or stock deductions.
   * **Medical Safety Guardrail**: Never recommend double doses or catch-up doses after a missed dose.
5. **Scheduler Architecture**:
   * Time-critical dose notifications use `AlarmManager` with exact alarms (`setExactAndAllowWhileIdle()`).
   * `WorkManager` is strictly reserved for deferrable maintenance (e.g., periodic low-stock evaluation via `LowStockCheckWorker`).

---

## 4. Key Architectural Map

The codebase strictly follows Clean Architecture / MVVM with Unidirectional Data Flow (UDF):

```
app/src/main/java/com/example/nuvilo/
├── data/
│   ├── backup/            # PBKDF2/AES-256-GCM encrypted SAF JSON & RFC 4180 CSV ZIP export/restore
│   ├── local/             # Room Database (NoveloDatabase), DAOs, Entities, Migrations (1->2, 2->3)
│   ├── mapper/            # Entity <-> Domain Model mapping extensions
│   ├── preferences/       # Jetpack DataStore (UserPreferencesRepository, ThemeMode, PrivacyMode)
│   └── repository/        # Repository implementations (NoveloRepositoryImpl, MedicationRepository, etc.)
├── domain/
│   ├── adherence/         # AdherenceCalculator, MonthlyCalendarData, DoseOutcomeFilter
│   ├── model/             # Domain entities (Medication, ScaledQuantity, DoseOccurrence, StockLot, etc.)
│   ├── prescription/      # PrescriptionManager, PrescriptionStatus, repeat authorisation logic
│   ├── schedule/          # ScheduleOccurrenceCalculator, schedule horizons, deterministic occurrenceId
│   └── stock/             # StockLedgerCalculator, FEFO multi-lot allocation engine
├── security/              # BiometricAuthManager (BiometricPrompt), AppLockManager (grace periods, FLAG_SECURE)
├── system/
│   ├── alarm/             # AlarmSchedulerImpl, AlarmReceiver (exact dose alarms)
│   ├── notification/      # NotificationHelper, NotificationActionReceiver (Take, Skip, Snooze)
│   ├── receiver/          # BootAndChangeReceiver (BOOT_COMPLETED, TIMEZONE_CHANGED reconciliation)
│   └── work/              # LowStockCheckWorker (periodic low-stock evaluation)
└── ui/
    ├── components/        # Reusable Compose design components (NoveloDataRow, NoveloBulletKpiCard, badges, pills)
    ├── screens/           # DashboardScreen, MedicationListScreen, MedicationDetailScreen,
    │                      # AdherenceCalendarScreen, SettingsScreen, SplashScreen, LockScreen
    ├── theme/             # Material 3 Theme.kt, Color.kt (NHS #005EB8), Shape.kt (Pill, Card), Type.kt (tnum)
    └── viewmodel/         # ViewModels.kt (Dashboard, Medication, MedicationDetail, Calendar, Settings)
```

---

## 5. Critical Code Patches & Caveats

When working on ViewModels, Receivers, or Compose UI, be aware of recent architectural fixes:

1. **Receiver Null Safety (`BUG-001`)**:
   * In `BootAndChangeReceiver.kt`, `AlarmReceiver.kt`, and `NotificationActionReceiver.kt`, always call `pendingResult?.finish()` with null safety. Outside active system broadcast dispatch (such as unit/instrumentation test invocation), `goAsync()` returns `null`.
2. **ViewModel State Loading (`BUG-002`)**:
   * In `MedicationDetailViewModel`, always use `awaitLoadedState()` before calculating inventory adjustments or restocks. Never read `uiState.value.stockBalance?.currentBalance ?: ScaledQuantity.ZERO` without checking `isLoading`, otherwise balance adjusts against zero and compounds.
3. **Tabular Numerics for Healthcare Data**:
   * When displaying numbers, stock balances, counters, or timestamps, always apply `.tabular` or `NoveloNumericTypography` in `Type.kt` (`fontFeatureSettings = "tnum"`) to eliminate horizontal layout jitter.
4. **Touch Targets & Accessibility**:
   * Interactive UI elements must meet the $\ge 48\times 48\text{dp}$ touch target requirement (`heightIn(min = 48.dp)` or `minimumInteractiveComponentSize`).
   * Color contrast must meet WCAG AA ($\ge 4.5:1$). Status must never be conveyed solely by color (pair with clear text and icons).

---

## 6. How the Next Agent Should Work

### 1. Environment Configuration (PowerShell)
Nuvilo compiles against Android SDK 37 using Java 21/25 (bundled Android Studio JBR):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### 2. Standard Quality Verification Commands
Run before claiming any task is complete or creating commits:
```powershell
# 1. Run JVM Unit Tests (136 tests)
.\gradlew.bat testDebugUnitTest

# 2. Run Connected Tests (151 tests on emulator-5554 or connected device)
.\gradlew.bat connectedDebugAndroidTest

# 3. Run Android Lint (Must have 0 errors)
.\gradlew.bat lintDebug

# 4. Assemble Release APK (Verifies R8 shrinking and ProGuard rules)
.\gradlew.bat assembleRelease
```

### 3. Git & Branching Workflow
- Always branch off `main`:
  ```powershell
  git checkout main
  git pull origin main
  git checkout -b <type>/<issue-number>-<short-description>
  ```
- Follow conventional commits (`feat:`, `fix:`, `chore:`, `test:`, `docs:`).
- Never run force-push (`git push --force`).
- Never discard unstaged user modifications without explicit authorization.

---

## 7. Open Backlog & Suggested Next Tasks

If tasked with continuing work, the following areas represent natural high-value initiatives:

1. **Physical Device QA Walkthrough**:
   * Complete the human-tester checklist in [`docs/ui-ux-audit/stage2/PHYSICAL_DEVICE_CHECKLIST.md`](docs/ui-ux-audit/stage2/PHYSICAL_DEVICE_CHECKLIST.md) on physical Android hardware (testing punch-hole/notch insets, 60/120Hz refresh rates, physical haptic vibrations, and hardware fingerprint/face authentication).
2. **Production Keystore Configuration (Optional for Google Play Distribution)**:
   * Current release builds compile cleanly under R8 with an unsigned/QA certificate. When ready for Google Play distribution, configure production signing credentials via environment variables in `app/build.gradle.kts`.
3. **Accessibility & Multi-Language Localization**:
   * Extract hardcoded English strings into `res/values/strings.xml` for internationalization.
   * Add dynamic font scaling tests ($\ge 200\%$ system font scale) to verify non-clipping text layouts.

---

## 8. Index of Source-of-Truth Documents

| Document | Description |
| :--- | :--- |
| [`AGENTS.md`](AGENTS.md) | Agent behavioral rules, safety invariants, and quality guidelines. |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Comprehensive system architecture, data models, and database schema. |
| [`PROGRESS.md`](PROGRESS.md) | Full historical log of all completed phases and verification evidence. |
| [`CHANGELOG.md`](CHANGELOG.md) | Structured changelog following Keep a Changelog format. |
| [`SECURITY.md`](SECURITY.md) | Security model, zero-network boundary audit, and encryption specs. |
| [`TESTING.md`](TESTING.md) | Testing strategy, instrumentation guides, and test execution commands. |
| [`docs/testing/pre-merge/`](docs/testing/pre-merge/) | Pre-merge QA deliverables (`TEST_RESULTS.md`, `TEST_CASE_CATALOG.md`, `BUGS_FOUND.md`, `TEST_COVERAGE.md`). |
| [`docs/ui-ux-audit/`](docs/ui-ux-audit/) | Complete Phase 8 & Stage 2 UI/UX audit, design system specs, and before/after screenshots. |
| [`docs/phase9/`](docs/phase9/) | Phase 9 specifications and reports (`IMPLEMENTATION_REPORT.md`, `UX_VERIFICATION.md`, `KNOWN_LIMITATIONS.md`, `DOMAIN_MODEL_DESIGN.md`). |
| [`docs/screenshots/phase9/`](docs/screenshots/phase9/) | 12 verified Phase 9 Light and Dark screenshots on Google Pixel 10 Pro (API 37). |
| [`design-system/novelo/MASTER.md`](design-system/novelo/MASTER.md) | Master design system tokens, typography scales, and component specifications. |
| [`website-handoff/`](website-handoff/) | Official website, Lovable master prompt, verified privacy policy specification, and visual asset suite. |
| [`store-assets/google-play/`](store-assets/google-play/) | Google Play Store marketing campaign (phone/tablet screenshots, feature graphic, and Next.js editor project). |
