# Contributing to Novelo

Thank you for contributing to **Novelo**, a premium, offline-first personal medication tracking application for native Android (*"Medication, made manageable."*).

To maintain the architectural integrity, patient safety invariants, and strict privacy guarantees of the application, all contributions must adhere to the standards outlined in this guide.

---

## 1. Core Principles & Non-Negotiable Invariants

Before writing or modifying any code, review:
- `AGENTS.md` - Core project guidelines and rules.
- `.agents/skills/novelo-medication-safety/SKILL.md` - Mandatory medication safety invariants.
- `.agents/rules/android-quality.md` - Architectural and Compose UI guidelines.
- `.agents/rules/offline-privacy.md` - Zero-network and health data privacy rules.

### Key Invariants
1. **Zero Network Access**: `android.permission.INTERNET` is strictly forbidden. Novelo contains no remote telemetry, cloud SDKs, third-party analytics, or crash trackers.
2. **Immutable Stock Ledger**: All stock quantities are calculated from append-only ledger entries. Scaled integers (`ScaledQuantity`) are mandatory; floating-point types (`Float`, `Double`) are prohibited for quantities and dose counts.
3. **Deterministic Dose Occurrence**: Every scheduled dose has a unique, deterministic `occurrenceId`. Reversals (`DOSE_REVERSAL`) compensate for changes idempotently.
4. **Offline Storage Access Framework (SAF)**: Backup and restore must use system SAF pickers (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`). No broad storage permissions (`MANAGE_EXTERNAL_STORAGE`, `READ/WRITE_EXTERNAL_STORAGE`).
5. **Accessibility (WCAG AA)**: Minimum 48x48dp touch targets, minimum 4.5:1 text contrast ratio, TalkBack semantics on all interactive elements, and zero color-alone status indications.

---

## 2. Git & GitHub Branching Workflow

We follow a structured issue-driven branch and pull request workflow.

### Step 1: Issue First
- Every feature, bugfix, or maintenance task must be tracked in a GitHub Issue.
- Inspect existing issues to avoid duplicates.
- Record the issue number and link in `PROGRESS.md`.

### Step 2: Feature Branches
- **Never work directly on the default branch (`main`)**.
- Branch naming convention: `<type>/<issue-number>-<short-description>`:
  - `feat/42-medication-reminders`
  - `fix/55-stock-deduction-rounding`
  - `chore/1-phase-07-release-hardening`
  - `perf/63-calendar-month-scroll`
  - `docs/12-update-architecture-decisions`

### Step 3: Conventional Commits
Use concise, atomic commits following the Conventional Commits specification:
```text
<type>(<scope>): <short description> (Refs #<issue-number>)

[optional body explaining why and what changed]
```
Common types:
- `feat`: New product capabilities.
- `fix`: Bug fixes.
- `perf`: Performance improvements.
- `test`: Unit, integration, or screenshot tests.
- `refactor`: Structural changes without altering behaviour.
- `build`: Gradle dependencies, R8/ProGuard, compiler flags.
- `docs`: Documentation updates.
- `chore`: Maintenance tasks.

### Step 4: Quality Verification Before Push
Before pushing any commit or opening a pull request, run all quality gates locally:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```
All suites must pass with 0 errors.

### Step 5: Draft Pull Requests
- Push your topic branch: `git push origin <branch-name>`.
- Open a **Draft Pull Request** targeting `main`.
- Reference the issue in the description (`Refs #<issue-number>`).
- Include test evidence, verification output, and screenshots where applicable.
- Do not merge without peer review and explicit approval.

---

## 3. Code Style & Standards

- **Kotlin 2.2+**: Adhere to official Kotlin style guidelines.
- **Compose**: All public composables should accept an optional `modifier: Modifier = Modifier` chained to the root element.
- **StateFlow**: ViewModels expose immutable `StateFlow<UiState>`. Mutable flows must remain private.
- **Edge-to-Edge**: Utilize `WindowInsets.safeDrawing`, `statusBarsPadding()`, and `navigationBarsPadding()` appropriately.
- **Room Migrations**: Any entity change requires an explicit schema export and a migration test (`NoveloDatabaseMigrationTest`).
