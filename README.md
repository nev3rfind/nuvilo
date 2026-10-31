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

**Current Phase: Phase 0 (Environment, Skills & Product Preparation)**
- [x] Android Studio environment inspected and verified with JDK 25 and AGP 9.4.1.
- [x] Baseline build and unit tests passing (`BUILD SUCCESSFUL in 2m 22s`).
- [x] Curated suite of 21 agent skills and custom medication safety skill configured in `.agents/skills/`.
- [x] Architecture Decision Records (ADRs 0001–0006) established in `docs/decisions/`.
- [x] Design system specifications and dashboard layout comparisons created in `docs/design/`.
- [x] Implementation plan and product requirements established.
- [ ] *Phase 1 (Data Architecture & Stock Ledger Engine) pending user review.*

---

## 🏗 Building and Testing

Novelo requires Java 25 or a compatible modern JDK (such as the bundled Android Studio JBR).

### Set Java Home (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### Run Unit Tests
```powershell
.\gradlew.bat test
```

### Assemble Debug APK
```powershell
.\gradlew.bat assembleDebug
```

### Static Analysis / Lint
```powershell
.\gradlew.bat lintDebug
```

For detailed testing instructions, device connections, and instrumentation configurations, see [TESTING.md](TESTING.md).

---

## 📚 Project Documentation

- [Product Requirements](docs/product-requirements.md)
- [System Architecture](ARCHITECTURE.md)
- [Domain Glossary](GLOSSARY.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)
- [Security & Threat Model](SECURITY.md)
- [Skills Inventory](SKILLS_INVENTORY.md)
- [UI/UX Design System](docs/design/design-system.md)
- [Screen Specifications](docs/design/screen-specifications.md)
- [Architecture Decisions (ADRs)](docs/decisions/)
