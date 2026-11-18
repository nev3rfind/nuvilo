# Visual Verification: Add & Edit Medication Experience Redesign (Phase 12)

**Product**: Nuvilo (*"Medication, made manageable."*)  
**Package**: `com.example.nuvilo`  
**GitHub Issue**: `#22` | **Branch**: `feat/22-add-medication-redesign`

> [!NOTE]
> **Device Provenance**:
> - **Baseline ("Before") screenshots** (`before/01_before_step1.png`, `before/02_before_step2.png`, `before/03_before_step3.png`) were captured on physical device `SM-S948B` (`R3GL5059FTZ`).
> - **Redesigned ("After") screenshots** (`01_step1_light.png` through `13_tablet_layout.png`) were captured on Android Emulator `Pixel_10_Pro` (`emulator-5554`, API 37).

---

## 1. Before vs. After Side-by-Side Matrix

| Stage / State | Baseline (`before/`) | Redesigned (`add-medication-redesign/`) | Verified Visual Improvements |
| :--- | :--- | :--- | :--- |
| **Step 1 (Light / Dark)** | [`01_before_step1.png`](../../screenshots/add-medication-redesign/before/01_before_step1.png) | [`01_step1_light.png`](../../screenshots/add-medication-redesign/01_step1_light.png), [`02_step1_dark.png`](../../screenshots/add-medication-redesign/02_step1_dark.png) | Replaced constrained `AlertDialog` with full-screen edge-to-edge layout (`DialogSystemBarsEffect` ensuring crisp dark status icons in Light mode and crisp light icons in Dark mode). Upgraded giant top-left-aligned step pills (`1. Identity`, `2. Schedule`, `3. Stock`) to compact `01 Identity` — `02 Schedule` — `03 Stock` step badges + animated progress bar. Eliminated bottom scroll clipping of `Usable tab per blister pack *`. |
| **Step 2 (Light / Dark)** | [`02_before_step2.png`](../../screenshots/add-medication-redesign/before/02_before_step2.png) | [`03_step2_light.png`](../../screenshots/add-medication-redesign/03_step2_light.png), [`04_step2_dark.png`](../../screenshots/add-medication-redesign/04_step2_dark.png) | Removed wrapped 4-button AlertDialog footer (`Next Step`, `Save`, `Back`, `Cancel` across two rows). Added `Section A · Administration` (`AdministrationRoute`, `Treated Eye(s)`, explicit drop math callout: `"1 drop in each eye = 2 drops consumed per administration"`, equal-width `Left Eye Drops` / `Right Eye Drops`) and `Section B · Schedule` (equal-height `Hour` / `Minute` / `+ Add`, `Start` date & `Ongoing` toggle). Completed step badge turns accessible green check (`✓ Identity`). |
| **Step 3 (Light / Dark)** | [`03_before_step3.png`](../../screenshots/add-medication-redesign/before/03_step3_light.png) | [`05_step3_light.png`](../../screenshots/add-medication-redesign/05_step3_light.png), [`06_step3_dark.png`](../../screenshots/add-medication-redesign/06_step3_dark.png) | Replaced jagged two-line `"Expiry (days, opt)"` input with physical package `-` / `+` stepper (`Number of Bottles` / `Number of Packs`), aligned `Low Stock` / `Runway Alert (Days)` fields, Material 3 `Select Expiry Date` + `+3m` / `+6m` / `+12m` / `No expiry` chips, and dedicated `Regimen Summary` card with `Estimated` vs `Exact count` pill. |

*(Correction note: `03_before_step3.png` resides at [`03_before_step3.png`](../../screenshots/add-medication-redesign/before/03_before_step3.png).)*

---

## 2. Targeted Interaction & Responsive Verification

| Screenshot | Verified Scenario | Observed Result |
| :--- | :--- | :--- |
| [`07_selection_states.png`](../../screenshots/add-medication-redesign/07_selection_states.png) | Contextual package cards & discard presets | Selecting `Eye / Ear Drops` filters packages to `Bottle (Dropper bottle)`, `Box of single-dose containers`, `Single-dose container`, and `Package`. Shows `"Nuvilo never guesses drops from ml because droppers vary."` and `28 days` / `30 days` / `90 days` / `None` post-opening discard presets. |
| [`08_validation_errors.png`](../../screenshots/add-medication-redesign/08_validation_errors.png) | Empty-field validation & footer feedback | Clicking `Continue ->` with blank Step 1 inputs marks `Medication Name *` and `Strength / Concentration *` with red error borders, inline hints (`"Enter the medication name to continue."`, `"Enter strength or concentration (e.g. 20 mg or 0.15%)."`), and an amber footer explanation banner (`"Required: Enter medication name and strength to continue."`). |
| [`09_keyboard_open.png`](../../screenshots/add-medication-redesign/09_keyboard_open.png) | Soft keyboard (IME) inset handling | `WizardBottomActionBar` (including amber hint banner, `Cancel`, and `Continue ->`) docks directly above the Gboard soft keyboard via `imePadding()` without overlapping or obscuring active form inputs (`Hydro Forte`, `Strength / Concentration *`). |
| [`10_inventory_preview.png`](../../screenshots/add-medication-redesign/10_inventory_preview.png) | Live `Regimen Summary` forecast | Displays `Hydro Forte · 0.15% (Bottle)` with `Estimated` pill, tabular metrics (`Initial supply: 2 bottles x 300 = 600 drops`, `Current dosage: 2 drops x 1/day`, `Daily consumption: 2 drops/day`, `Estimated supply: Approximately 178 day(s)`, `After-opening limit: Discard after 28 days`), and active `Save medication` CTA. |
| [`11_edit_medication.png`](../../screenshots/add-medication-redesign/11_edit_medication.png) | Edit Medication modal parity | Full-screen `Edit Medication Details` renders pre-populated `"Hydro Forte"` / `"0.15%"`, interactive 48dp Visual Accent Tag swatches with active blue ring + checkmark, `Dosage Form & Route` card selection (`Eye / Ear Drops`), and sticky `Cancel` / `Save Changes` footer. |
| [`12_small_screen.png`](../../screenshots/add-medication-redesign/12_small_screen.png) | Compact phone (`720x1280` @ `320dpi` = `360x640dp`) | Step 1 renders cleanly without horizontal overflow, text truncation inside cards, or obscured primary `Continue ->` / `Cancel` footer buttons. |
| [`13_tablet_layout.png`](../../screenshots/add-medication-redesign/13_tablet_layout.png) | Tablet layout (`1600x2560` @ `320dpi` = `800x1280dp`) | Centers the wizard panel cleanly via `widthIn(max = 640.dp)` against the elevated `#F4F7FA` backdrop, showing `Medication Identity`, `Dosage Form` grid, and `How You Buy / Store It (Package)` cards without extreme horizontal stretching. |
