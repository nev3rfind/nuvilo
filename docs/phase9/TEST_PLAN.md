# Phase 9 Test Plan (TDD First)

**Date:** 2026-11-12  
**Status:** Test Strategy & Acceptance Matrix (Stage A)

---

## 1. TDD Execution Discipline

Every domain calculator, validator, migration path, notification builder, and haptic controller in Phase 9 is driven **RED -> GREEN -> REFACTOR**:
1. Write failing JVM unit tests or Android instrumentation tests asserting the exact behavior.
2. Observe expected failure.
3. Implement clean Kotlin logic using `ScaledQuantity` (`Long` thousandths only).
4. Re-run suite and verify zero failures and zero skipped tests.

---

## 2. Test Coverage Matrix

### 2.1 JVM Unit Tests (`app/src/test/java/com/example/nuvilo/`)

| Test Suite | Target Logic | Key Test Cases |
|---|---|---|
| `ConsumptionCalculatorTest` | Form/site/package-aware consumption | - Multi-dose eye drops: Left only (`1`), Right only (`1`), Both eyes (`1+1=2`), Asymmetric (`2 left + 1 right = 3`)<br>- Single-dose container med: `1` container treating BOTH_EYES stays `1` (never auto-multiplied to `2`); `2` containers stays `2`<br>- Eye gel/ointment: `ESTIMATED_APPLICATIONS` vs `GRAMS` vs `TUBE_COUNT_ONLY`<br>- Tablets, capsules, oral liquid, inhaler, patch, injection unchanged<br>- `UnitIntegrityValidator` rejects `BOTTLES`/`ML` -> `DROPS` or `TUBES` -> `APPLICATIONS` arithmetic without explicit package capacity |
| `PackageLifecycleAndFefoTest` | Package state machine & FEFO transitions | - `UNOPENED -> OPENED` stamps `openedAtEpochMs` and `discardAfterOpeningEpochMs`<br>- `OPENED` package consumed before `UNOPENED` even if a sealed lot has slightly different expiry rules, respecting eligible FEFO<br>- Multi-bottle split dose (`1 drop` left in Bottle #1 + `2 drops` needed -> depletes Bottle #1 to `EMPTY`, opens Bottle #2 and deducts `1`)<br>- Excludes `EXPIRED`, `DISCARDED`, and `OPENED` lots past `discardAfterOpeningEpochMs`<br>- Idempotent repeated `occurrenceId` never double-opens or double-deducts<br>- `recountOpenedPackage` mutates ONLY selected `OPENED` lot and leaves `UNOPENED` sealed lots intact<br>- `discardPackage` marks `DISCARDED` and creates exact `WASTAGE` ledger delta |
| `SupplyForecastCalculatorTest` | Shelf-life-aware runway & precision | - Realistic bottle runway: 300 drops/bottle at 2 drops/day with **no** discard limit = 150 days; with **28-day** discard-after-opening limit = 28 days for Bottle #1 + 28 days for Bottle #2 = 56 days total, reporting `244 drops/bottle` expiring unused!<br>- Single-dose box of 200 containers @ 1/day = 200 days `EXACT`<br>- Specific weekday schedules (e.g., 3x/week)<br>- Missing capacity/unconfigured legacy med -> `TrackingPrecision.UNKNOWN`, `daysRemaining = null` when needed<br>- Reconciles ledger sum == total active lot balances |
| `Phase9BackupCompatibilityTest` | JSON & CSV ZIP v3 <-> v4 backups | - Deserialize legacy v3 JSON into v4 entities cleanly<br>- Deserialize legacy v3 CSV ZIP into v4 entities cleanly<br>- Round-trip v4 encrypted JSON + CSV ZIP with packaging config, administration sites, opened timestamps, discard-after-opening, and discarded lot records |
| `NotificationAndWorkerLogicTest` | Channels, privacy masking, deduplication | - Default medication reminder channel uses `sound == null`, vibration enabled<br>- Privacy mode strips drug name, eye site, and package info from reminder, low-stock, expiry, and discard notifications<br>- Duplicate check suppression & restock reset |
| `NuviloHapticsControllerTest` | Central haptic policy | - Emits expected `NuviloHapticEvent` when enabled<br>- Suppressed when `hapticsEnabled == false` or system haptics disabled<br>- Never triggered on passive state emissions |

### 2.2 Android Instrumentation Tests (`app/src/androidTest/java/com/example/nuvilo/`)

| Test Suite | Target | Key Verification Cases |
|---|---|---|
| `NoveloDatabaseMigrationTest` | Room `3 -> 4` + full `1 -> 4` | - Migrate v3 database containing tablets + drops + partial lot + empty lot + expired lot + doses + prescriptions<br>- Verify zero lost rows, inferred `packageStatus` (`OPENED`, `EMPTY`, `EXPIRED`, `UNOPENED`), schema validation |
| `Phase9RepositoryFlowTest` | `NoveloRepositoryImpl` transactional integrity | - Create eye drop medication with 2 bottles x 300 drops + 28-day discard rule -> verifies 2 `UNOPENED` lots + `600 drops` ledger<br>- Record dose -> Bottle #1 transitions `UNOPENED -> OPENED` with `discardAfterOpeningEpochMs`<br>- Recount Bottle #1 -> only Bottle #1 changes, Bottle #2 remains `UNOPENED (300 drops)`<br>- Discard Bottle #1 -> `DISCARDED` + `WASTAGE` ledger movement + remaining = Bottle #2 (`300 drops`)<br>- Correct dose -> `DOSE_REVERSAL` restores exact debited lot |
| `Phase9UiAndAccessibilityTest` | Compose UI, Wizard & Accessibility | - Add Eye Drop workflow (Left/Right/Both eyes, drops per bottle, discard days, preview)<br>- Add Single-Dose container box (Both eyes treated, 1 container per dose, preview)<br>- Stock detail contextual dialogs (`Add bottles`, `Recount current bottle`, `Discard opened bottle`)<br>- Settings Haptics & Notification category cards + system settings action<br>- WCAG AA bounds ($\ge 48\text{dp}$), semantic descriptions, light/dark rendering |

---

## 3. Quality Gates (Mandatory Before Any Completion Claim)
1. `.\gradlew.bat testDebugUnitTest` — 100% PASS, 0 failures, 0 skipped
2. `.\gradlew.bat connectedDebugAndroidTest` on `emulator-5554` (API 37) — 100% PASS, 0 failures, 0 skipped
3. `.\gradlew.bat lintDebug` — 0 errors
4. `.\gradlew.bat assembleDebug` & `.\gradlew.bat assembleRelease` — clean build
5. Merged release manifest audit confirming `android.permission.INTERNET` is 100% absent
6. Visual UI inspection & before/after screenshots under `docs/screenshots/phase9/`
