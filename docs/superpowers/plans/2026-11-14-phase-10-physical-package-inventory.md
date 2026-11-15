# Phase 10 Implementation Plan: Physical Package Inventory, Bottle Lifecycle & Intelligent Stock Management

**Goal**: Perform a comprehensive audit and redesign of Nuvilo's medication inventory system. Eliminate purchasing in raw drop units, establish physical package inventory tracking (bottles, tubes, single-dose boxes, packs), isolate consumption to the currently opened package, require explicit user confirmation to open the next bottle, provide package-level recount and correction workflows, elevate dashboard and notification stock intelligence, and verify with a full 7-bottle end-to-end acceptance scenario.

**Tracking**: GitHub Issue [#18](https://github.com/nev3rfind/nuvilo/issues/18) | Branch `feat/18-physical-package-inventory`

---

## 1. Architecture & Domain Invariants
- **Core Distinction**:
  - `PurchasingUnit`: What the user physically purchases (`PackageType`: Bottle, Tube, Single-dose Box, Blister Pack, etc.).
  - `PackageConsumptionCapacity`: How much usable medication one package contains (e.g. 1 eye-drop bottle = 300 drops).
  - `AdministrationUnit`: What is consumed per administration (e.g. 1 drop per eye × 2 eyes = 2 drops).
- **Physical Package Identity**:
  - Every package is individually tracked in `stock_lots` with unique ID, capacity, remaining quantity, expiry, batch number, received date, opened date, status (`UNOPENED`, `OPENED`, `EMPTY`, `DISCARDED`, `EXPIRED`, `DAMAGED`, `LOST`).
  - Total physical inventory = `openedCount + unopenedCount`.
  - Opening a bottle reduces `unopenedCount` by 1 and increases `openedCount` by 1; total physical packages remains unchanged.
- **Consumption Isolation**:
  - Doses deduct strictly from the active `OPENED` package. Unopened packages remain completely untouched.
  - When the opened package is exhausted, its status transitions to `EMPTY`.
  - The next eligible unopened package is staged as "Ready to Open" via FEFO. It is **never** auto-opened without explicit user confirmation.
- **Zero Dose Fabrication**:
  - Marking a bottle as finished ahead of zero or discarding a package writes off remaining units strictly as `MovementType.WASTAGE` (`isEstimate = true`, `doseOccurrenceId = null`).
  - Dose events table and adherence scores remain 100% untouched.
- **Strict Offline & Scaled Integer Arithmetic**:
  - Zero `android.permission.INTERNET`.
  - Zero `Float`/`Double` in stock or ledger accounting; all quantities use `ScaledQuantity` integer thousandths.

---

## 2. Multi-Agent Swarm Allocation
- **Lead Coordinator**: Architecture, integration, plan execution, and final handoff.
- **Agent 1 (`domain-auditor`)**: Domain models, `Medication.effectivePackagingConfig`, `PackageStatus` expansion (`DAMAGED`, `LOST`), and `StockLedgerCalculator.allocateDoseAgainstPackages` open-package isolation.
- **Agent 2 (`inventory-eng`)**: Repository & ViewModel package restocking with `openFirstPackageImmediately`, package-level recount, corrections, early depletion, and reversal workflows.
- **Agent 3 (`notif-haptics-eng`)**: Notification text overhaul ("Current bottle: X drops · Unopened: Y bottles · Z days supply"), finished package notification, Dashboard package summary, and tactile haptics.
- **Agent 4 (`uiux-specialist`)**: Restock Supplies dialog redesign (bottles not drops, presets +1, +2, +3, +5 bottles, live summary, open immediately switch), Medication Detail stock overview redesign, and Recount/Adjust package management UI.
- **Agent 5 (`qa-engineer`)**: Connected Hydro Forte 7-bottle acceptance test on `Pixel_10_Pro` (API 37), unit test suite verification, lint vitals, and release build security audit.

---

## 3. Step-by-Step Implementation Tasks

### Task 1: Domain & Calculator Package Isolation (Agent 1)
- [ ] Add `DAMAGED` and `LOST` to `PackageStatus` in `PackagingModels.kt`.
- [ ] Add `Medication.effectivePackagingConfig` extension property in `PackagingModels.kt` or `DomainModels.kt` providing smart defaults based on `MedicationForm` and `StockUnit` so no medication lacks package metadata.
- [ ] Update `StockLedgerCalculator.allocateDoseAgainstPackages`:
  - When multi-dose packages (e.g. bottles, tubes, jars) are in use, deduct strictly from `OPENED` packages.
  - Do not auto-open `UNOPENED` packages when opened packages are exhausted.
  - Transition exhausted package to `EMPTY`.
- [ ] Author unit tests in `StockLedgerCalculatorTest.kt` verifying opened-package isolation and non-auto-opening invariant.

### Task 2: Repository & ViewModel Package Operations (Agent 2)
- [ ] Update `NoveloRepository.restockPackages` to accept `openFirstPackageImmediately: Boolean = false`.
  - If `true` and no currently opened package exists for the medication, create package #1 with `packageStatus = PackageStatus.OPENED` and `openedAtEpochMs = nowMs`.
  - Create remaining packages with `packageStatus = PackageStatus.UNOPENED`.
- [ ] Implement `revertOpenedPackage(lotId: String)` in `NoveloRepository` and `ViewModels.kt` to allow users to undo an accidental package opening.
- [ ] Implement `updatePackageMetadata(lotId: String, ...)` for correcting batch number, expiry date, or capacity.
- [ ] Ensure `MedicationViewModel` and `MedicationDetailViewModel` expose package restock, package recount, package depletion, and package opening methods.
- [ ] Author unit tests in `NoveloRepositoryTest.kt` or dedicated package tests.

### Task 3: Intelligent Notifications & Dashboard Intelligence (Agent 3)
- [ ] Overhaul `NotificationTextBuilder.buildBigText` and `buildSubText`:
  - Format bottle-based reminders:
    - Subtext: `Bottle #1 · 180 left · 6 sealed reserve`
    - BigText: `Take 2 drops · Both eyes (0.3%) · Scheduled for 08:00\n📦 Current bottle: 180 drops remaining.\n📦 Unopened bottles: 6.\n⏳ Estimated total supply: 198 days.`
  - Finished bottle notification:
    - `Hydro Forte – Current bottle finished.\n6 unopened bottles available.\nOpen the next bottle when ready.`
- [ ] Overhaul `DashboardItem` and `HeroDoseCard` / `TimelineDoseItem`:
  - Compute package breakdown: `openedPackage`, `unopenedCount`, `totalPhysicalCount`, `daysOfSupply`.
  - Render compact package inventory row on dashboard cards:
    `Open bottle: 180 / 300 drops · Unopened: 6 bottles · Total: 7 bottles · ~198 days supply`.
  - Show warning chips: "Bottle nearly empty", "No unopened bottles remaining", "Expiring soon", "Past discard limit".
- [ ] Verify haptics `NuviloHapticEvent.RESTOCK_SAVED`, `PACKAGE_OPENED`, `PACKAGE_DEPLETED`.

### Task 4: Premium UI/UX Redesign (Agent 4)
- [ ] Redesign `RestockDialog` in `MedicationDetailScreen.kt`:
  - Remove raw "Quantity Added (Drops)" and +150/+300/+600 presets.
  - Render package purchasing form:
    - Package type name (e.g. Bottle).
    - Number of bottles purchased: `[ 7 ]`.
    - Estimated capacity per bottle: `[ 300 ] drops`.
    - Presets: `+1 bottle`, `+2 bottles`, `+3 bottles`, `+5 bottles` (adapting to package type).
    - Switch: "Open first bottle immediately" (default enabled if 0 opened bottles exist, disabled if 1 opened bottle exists).
    - Optional: Expiry date, Batch number, Purchase date, Notes.
    - Live summary card:
      "7 bottles received. 2,100 estimated drops in total. 1 opened bottle + 6 unopened bottles. Estimated supply: 210 days at 10 drops/day."
- [ ] Redesign Medication Detail Stock Overview:
  - Current opened bottle card with progress bar (`180 / 300 drops remaining`), opened date, discard deadline.
  - Unopened supply card (`6 bottles available`).
  - Total physical inventory (`7 total bottles = 1 opened + 6 unopened`).
  - Total estimated usable supply (`1,980 drops, subject to package usability and expiry`).
  - Estimated days of supply (`~198 days at 10 drops/day`).
- [ ] Redesign Recount / Adjust dialog:
  - Show physical packages.
  - Recount opened bottle with before-and-after preview (`180 -> 165 drops`, delta `-15 drops`).
  - Mark bottle as finished action (even if remaining > 0).
  - Revert opened bottle action.
  - Inspect unopened package details.

### Task 5: QA, Acceptance Scenarios & Quality Gates (Agent 5)
- [ ] Author `Phase10PackageInventoryAcceptanceTest.kt`:
  - Hydro Forte 7-bottle scenario:
    - Initial restock: 7 bottles, 300 drops/bottle, 1 opened + 6 unopened = 7 physical bottles (2,100 drops, 210 days at 10 drops/day).
    - 12 doses (24 drops) taken: Bottle 1 = 276 drops, 6 unopened, 7 physical bottles, 12 adherence records.
    - 138 more doses (total 150 doses = 300 drops): Bottle 1 exhausted (`EMPTY`), Bottle 2 staged ("Ready to Open"), user confirms opening Bottle 2, Bottle 2 = 300 drops, 5 unopened, 6 physical bottles.
    - Second bottle marked finished early with 60 drops remaining: 60 drops written off as `WASTAGE`, 0 dose events created, adherence untouched, Bottle 3 staged, total supply recalculated.
- [ ] Execute `testDebugUnitTest` (100% pass rate).
- [ ] Execute `connectedDebugAndroidTest` on `Pixel_10_Pro` (API 37).
- [ ] Execute `lintDebug` and `assembleRelease`.
- [ ] Verify zero `android.permission.INTERNET` in merged release manifest.

### Task 6: Documentation, Git & PR Deliverables
- [ ] Create documentation in `docs/inventory/`:
  - `PACKAGE_INVENTORY_ARCHITECTURE.md`
  - `PACKAGE_LIFECYCLE_SPEC.md`
  - `STOCK_CALCULATION_AUDIT.md`
  - `NOTIFICATION_STOCK_SPEC.md`
  - `INVENTORY_UI_UX_AUDIT.md`
  - `IMPLEMENTATION_REPORT.md`
  - `TEST_REPORT.md`
- [ ] Update `PROGRESS.md`, `CHANGELOG.md`, `HANDOFF.md`, `ARCHITECTURE.md`.
- [ ] Push branch `feat/18-physical-package-inventory`.
- [ ] Create Draft PR linking Issue [#18](https://github.com/nev3rfind/nuvilo/issues/18).
- [ ] Provide Copyable Handoff for ChatGPT.
