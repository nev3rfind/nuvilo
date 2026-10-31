# Novelo Agent Guidelines & Project Instructions

Novelo is a premium, offline-first personal medication tracking and prescription management application for native Android.
Tagline: *"Medication, made manageable."*

---

## 1. Core Architecture & Tech Stack
- **Platform**: Native Android (API min 24, compile/target 37, Java 25 / JVM 11 compatibility).
- **Language**: Kotlin 2.2+.
- **UI Framework**: Jetpack Compose with Material 3 (NHS-inspired blue/white, accessible dark mode, full Edge-to-Edge).
- **Data & Storage**: Room database (source of truth) + Jetpack DataStore (preferences).
- **Architecture**: Clean Architecture / MVVM with Unidirectional Data Flow:
  - `data`: Room entities, DAOs, repositories, database migrations.
  - `domain`: Pure Kotlin business logic, models, use cases, stock ledger calculator.
  - `ui`: Composables, ViewModels, navigation, theme, design system components.
  - `system`: AlarmManager receiver, WorkManager workers, notification managers, broadcast receivers.
- **Reminders**: `AlarmManager` with exact alarms for dose times; `WorkManager` strictly for deferrable maintenance (e.g. low-stock checks).
- **Authentication**: BiometricPrompt & Device Credential for optional app lock.

---

## 2. Mandatory Domain & Safety Invariants
Consult `.agents/skills/novelo-medication-safety/SKILL.md` before writing or modifying any medication logic.
- **Dose Recording**:
  - `CONFIRM`: Requires explicit user confirmation before recording a dose as taken.
  - `AUTO_ASSUME`: Records assumed adherence according to schedule; clearly distinguished from verified doses.
  - **Occurrence Identity**: Every scheduled dose has a unique, deterministic `occurrenceId`.
  - **Idempotency**: Repeated notification actions or button clicks must never duplicate dose records or stock deductions.
  - **Stock Deductions**: Occur only on taken/assumed doses. Skipped, snoozed, or dismissed notifications must not deduct stock.
  - **Medical Guardrail**: Never recommend catch-up or double doses after a missed dose.
- **Stock Ledger**:
  - All stock balances are derived from an immutable, append-only ledger of stock movements (opening balance, restocks, deductions, adjustments).
  - Exact quantities only: use scaled integers or exact representations. **Never use floating-point types (`Float` or `Double`) for stock balances or dose quantities.**
  - Label estimated consumption (inhalers, ointments, eye drops) explicitly as `ESTIMATED`.

---

## 3. Privacy & Offline-First Rules
- **Zero Network Access**: `android.permission.INTERNET` must never be declared or merged.
- **No Remote SDKs**: No Firebase, analytics, crash trackers, ads, or third-party AI APIs.
- **Sensitive Data**: Exclude database files from automatic unencrypted cloud backups (`backup_rules.xml`).
- **Privacy Mode**: Support masking notification text on lock screens (generic reminder vs drug name/dose).

---

## 4. UI/UX Quality & Accessibility
- Follow design system tokens in `docs/design/design-system.md` (NHS-inspired blue `#005EB8`, calm neutrals, high contrast).
- WCAG AA compliance: Minimum 48x48dp touch targets, minimum 4.5:1 text contrast.
- Never communicate status solely through color (pair with clear icons and text labels).
- Test on both light and dark themes.

---

## 5. Development, Verification & Git Discipline
- **TDD for Core Domain**: Write unit tests for stock calculations, dose occurrence identity, and schedule logic before writing implementation code.
- **Database Migrations**: Every schema change in Room requires an explicit migration test and schema export.
- **Build Verification**: Before claiming any task is complete, run:
  ```bash
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  .\gradlew.bat test
  .\gradlew.bat assembleDebug
  ```
  Check the actual console output to verify success.
- **Git Operations**: Never run force-push, do not discard unstaged user work, and never commit secrets or credentials.
