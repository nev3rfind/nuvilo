# Baseline Audit: Add & Edit Medication Experience

**Document**: Phase 12 Baseline UI/UX & Functional Audit  
**Product**: Nuvilo (*"Medication, made manageable."*)  
**Package**: `com.example.nuvilo`  
**Audited Source Files**:
- [MedicationListScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationListScreen.kt) (`AddMedicationDialog`, lines 514–1382)
- [MedicationDetailScreen.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/screens/MedicationDetailScreen.kt) (`EditMedicationDialog`, lines 2683–3251; `EditScheduleDialog`, lines 3254–3350)
- [NoveloComponents.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/components/NoveloComponents.kt) (`ScheduleConfigurationContent`, lines 1860–2086)
- [ViewModels.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/viewmodel/ViewModels.kt) (`MedicationViewModel.createMedicationWithPackages`, `MedicationDetailViewModel.updateMedication`)
- Baseline screenshots captured on `SM-S948B` (`docs/screenshots/add-medication-redesign/before/01_before_step1.png`, `02_before_step2.png`, `03_before_step3.png`).

---

## 1. Visual & Layout Defects Observed in Baseline Screenshots

| Category | Observed Problem | Impact on Usability |
|---|---|---|
| **Modal Container Constraint** | `AddMedicationDialog` and `EditMedicationDialog` render inside standard Material 3 `AlertDialog` with default platform dialog width (`~312dp–360dp`), outer margins, and fixed vertical viewport height. | Forms feel claustrophobic; multi-column fields wrap unpredictably; Step 1 clips `"Usable tab per blister pack *"` at the scroll fold; Edit mode becomes an overly long scrolling modal. |
| **Step Progress Header Misalignment** | Three equal-weight `Surface` ovals (`NoveloPillShape`) have top-left aligned `Text(..., padding(8.dp, 6.dp))` with no `Box(contentAlignment = Center)` and no step numbers/checkmarks/connector bars. | Step labels (`1. Identity`, `2. Schedule`, `3. Stock`) float awkwardly in the top-left corner of huge teal/blue pills (`48dp+` tall), creating poor visual hierarchy. |
| **Confusing & Inconsistent Footer** | `AlertDialog`'s `confirmButton` renders **both** `Next Step` and `Save` simultaneously on Steps 1 and 2, while `dismissButton` wraps onto a second row (`Back`, `Cancel`) on Step 2 (`02_before_step2.png`). | Four actions appear at once with unclear hierarchy. When mandatory Step 1 fields are empty, `Save` is silently disabled without explaining what is missing. |
| **Uneven Two-Column TextField Pairs** | `"Hour (0-23)"` (1 line) vs `"Min (0-59)"` (wraps onto 2 lines), and `"Alert Days Runway"` (1 line) vs `"Expiry (days, opt)"` (wraps onto 2 lines) have mismatched heights (`02_before_step2.png`, `03_before_step3.png`). | Creates ragged baseline alignment in side-by-side form inputs. |
| **Weak Selection Controls** | Dosage form and package type options use flat `FilterChip` pills with no icons, no descriptive subtitles, and muddy dark-teal active fills. `MedicationForm.OTHER` is omitted from Step 1. | Forms vs packaging formats look visually identical, reinforcing confusion between dosage form and purchasing package type. |
| **Silent Reset on Form Change** | Tapping a different `MedicationForm` in Step 1 or Edit immediately overwrites `selectedPackageTypeName`, `contentsPerPackageStr`, and `discardAfterOpeningDaysStr`. | Users who customized package capacity or discard rules lose their edits silently without confirmation. |
| **Missing Domain Inputs in Wizard** | `AdministrationRoute`, schedule `startDateEpochDay` / optional `endDateEpochDay`, and explicit eye-drop math callouts (`1 drop per eye = 2 drops consumed`) are absent in Step 2; package expiry uses raw integer days (`"Expiry (days, opt)"`) rather than a Date Picker. | Forces users to mentally calculate calendar days and administration consumption rules. |
| **Lack of Inline Validation & Auto-Scroll** | `name`, `strength`, `contentsPerPackageStr`, `doseQuantity`, and package count have no `isError` borders, inline error messages, or scroll-to-error behavior; users can freely jump to Step 3 via the top header without warning. | Users reach Step 3 (`03_before_step3.png`), see a disabled `Save` button, and receive zero guidance on why saving is blocked. |
| **No Tablet Adaptive Frame** | Because `AlertDialog` only adapts marginally, full-width layouts would stretch across tablets without a centered editorial panel, while phones suffer inside narrow modal margins. | Needs a dedicated responsive full-screen dialog (`usePlatformDefaultWidth = false`) with `widthIn(max = 640.dp)` centered on larger displays. |

---

## 2. State Persistence & Functional Audit
- `rememberSaveable` already preserves `name`, `strength`, `selectedFormName`, `selectedUnitName`, `selectedPackageTypeName`, schedule arrays, and Step 3 numbers across step switches and configuration changes — this behavior **must** be preserved and extended to new fields (`selectedRouteName`, `startDateEpochDay`, `endDateEpochDay`, `isOngoingSchedule`, `expiryDateEpochMs`).
- Stock calculations use `ScaledQuantity` (`Long` scaled by 1,000) and `SupplyForecastCalculator` — all accounting invariants remain strictly integer-based (`Float`/`Double` prohibited).
- `isSubmitting` prevents double-taps, but needs an explicit visible loading indicator and lock across all submit paths.
