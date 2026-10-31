# Novelo 💊
> *"Medication, made manageable."*

Novelo is a premium, offline-first personal medication tracking and prescription management application built natively for Android.

Designed with healthcare-grade rigor, Novelo focuses on mathematical stock integrity, idempotent dose recording, reliable alarm scheduling, and strict personal health privacy—without cloud dependencies, advertising, or external telemetry.

---

## 🎯 Core Capabilities (Planned Scope)

- **Medication Management**: Custom medication profiles with flexible forms (tablets, capsules, liquids, inhalers, creams), strengths, and stock units.
- **Precision Stock Ledger**: Auditable, append-only double-entry stock ledger. Exact integer arithmetic with zero floating-point drift, tracking restocks, deductions, and manual audit corrections.
- **Flexible Scheduling**: Daily schedules, multiple daily times, specific weekdays, cyclical intervals, and PRN (as-needed) routines.
- **Dose Recording Preferences**: Per-medication recording mode:
  - `CONFIRM`: Requires explicit confirmation before a dose is recorded as taken.
  - `AUTO_ASSUME`: Records assumed adherence according to schedule while clearly differentiating from confirmed ingestion.
- **Reliable, Privacy-Aware Notifications**: Direct `AlarmManager` exact alarms with actionable buttons ("Take", "Snooze", "Skip"). Privacy mode option to mask sensitive drug names on lock screens.
- **Prescription & Refill Tracking**: Tracks authorized repeats, expiry dates, and estimated days of medication remaining with advance refill lead-time warnings.
- **Adherence History & Calendar**: Detailed chronological timeline and month-by-month calendar view of confirmed, assumed, skipped, and missed doses.
- **Zero-Network Privacy & Local App Lock**: No internet permission requested. Optional biometric or device PIN/pattern lock. Safe local JSON/CSV export via Android Storage Access Framework.

---

## 🛠 Technology Stack

- **Platform**: Native Android (Min SDK 24, Compile SDK 37, Target SDK 37).
- **Language**: Kotlin 2.2+.
- **UI Framework**: Jetpack Compose with Material 3 (NHS-inspired blue `#005EB8`, accessible dark mode).
- **Persistence**: Room SQLite database (source of truth) + Jetpack DataStore (preferences).
- **Architecture**: Clean Architecture / MVVM with Unidirectional Data Flow (UDF).
- **Reminders**: `AlarmManager` for time-critical exact dose reminders; `WorkManager` for non-critical maintenance.
- **Security**: BiometricPrompt authentication, Credential Encrypted Storage, explicit cloud backup exclusions.

---

## 🚦 Project Status

**Current Phase: Phase 7 Complete (Release Candidate Hardening & Final Quality Assurance)**
- [x] **Phase 0**: Environment, architecture standards, design specifications, and agent skills configured.
- [x] **Phase 1**: Database schema, append-only stock ledger engine (`ScaledQuantity`), deterministic occurrence calculations.
- [x] **Phase 2**: Medication details, FEFO stock lot tracking, auditable adjustments, historical dose corrections (`DOSE_REVERSAL`).
- [x] **Phase 3**: Bounded exact alarm scheduling (`AlarmManager`), system broadcasts, actionable notifications, deferrable low-stock worker (`WorkManager`).
- [x] **Phase 4**: Adherence calculation engine, monthly calendar with accessible indicators (`✓`, `~`, `×`, `!`), prescription repeat tracking, database v3 migration.
- [x] **Phase 5**: NHS-inspired healthcare aesthetic (`#005EB8`), `#121820` slate dark theme, Hero Dose + Timeline hybrid layout, reusable components.
- [x] **Phase 6**: Biometric app lock (`BiometricPrompt` + device credentials), window `FLAG_SECURE` recents masking, PBKDF2/AES-256-GCM encrypted local SAF backup, RFC 4180 CSV ZIP export, atomic transactional restore.
- [x] **Phase 7**: Release hardening, R8 minification (3.09 MB release APK, 86.6% reduction), keep rules, WCAG AA compliance audit ($\ge 48\text{dp}$ touch targets), lifecycle flow collections (`collectAsStateWithLifecycle`), automated accessibility tests (23/23 passing on-device tests, 49/49 JVM unit tests).

---

## 🏗 Building and Testing

Novelo requires Java 25 or a compatible modern JDK (such as the bundled Android Studio JBR).

### Set Java Home (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### Run Unit Tests (49 tests across 9 test classes)
```powershell
.\gradlew.bat testDebugUnitTest
```

### Run On-Device Instrumented & Accessibility Tests (23 tests on connected device/emulator)
```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Static Analysis / Lint (0 errors)
```powershell
.\gradlew.bat lintDebug
```

### Assemble Debug APK
```powershell
.\gradlew.bat assembleDebug
```

### Assemble Hardened Release APK (R8 Minified)
```powershell
.\gradlew.bat assembleRelease
```

For detailed testing instructions, device connections, and instrumentation configurations, see [TESTING.md](TESTING.md).

---

## 📚 Project Documentation

- [Release Checklist & Build Verification](docs/RELEASE_CHECKLIST.md)
- [Accessibility Audit (WCAG AA)](docs/ACCESSIBILITY_AUDIT.md)
- [Performance & Memory Leak Audit](docs/PERFORMANCE_AUDIT.md)
- [Development Workflow](docs/DEVELOPMENT_WORKFLOW.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Product Requirements](docs/product-requirements.md)
- [System Architecture](ARCHITECTURE.md)
- [Domain Glossary](GLOSSARY.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)
- [Security & Threat Model](SECURITY.md)
- [Skills Inventory](SKILLS_INVENTORY.md)
- [UI/UX Design System](docs/design/design-system.md)
- [Screen Specifications](docs/design/screen-specifications.md)
- [Architecture Decisions (ADRs)](docs/decisions/)
