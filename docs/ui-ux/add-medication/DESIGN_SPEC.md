# Design Specification: Add & Edit Medication Experience (Phase 12)

**Product**: Nuvilo (*"Medication, made manageable."*)  
**Date**: 2026-11-17

---

## 1. Presentation Architecture: Full-Screen Responsive Wizard vs. AlertDialog
- **Decision**: Upgrade `AddMedicationDialog` and `EditMedicationDialog` from constrained `AlertDialog` modals to dedicated **full-screen responsive Setup Surfaces** using `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false))`.
- **Rationale**:
  1. **Zero clipping & breathing room**: Full viewport width eliminates cramped label wrapping (`"Min (0-59)"`, `"Expiry (days, opt)"`) and multi-row dialog button overflow.
  2. **Sticky Navigation & Action Bar**: Gives users a consistent top progress header (`01 Identity` ━━ `02 Schedule` ━━ `03 Stock`) and a thumb-friendly, keyboard-aware sticky footer (`navigationBarsPadding()` + `imePadding()`).
  3. **Tablet & Small-Phone Adaptation**: Centers content in `Modifier.widthIn(max = 640.dp)` with a clean surface card backdrop on tablets (`>= 600.dp`), while filling edge-to-edge cleanly on phones.
  4. **API & Caller Compatibility**: Keeping the composable wrapped in `Dialog(...)` lets existing call sites (`MedicationListScreen`, `MedicationDetailScreen`, and standalone UI tests) render identically without requiring navigation-graph rewrites.

---

## 2. Colour & Typography System
- Retain Nuvilo's clinical NHS-inspired tokens:
  - Primary NHS Blue `#005EB8` (primary CTAs, active step nodes, focus rings)
  - Interaction Blue `#0284C7` (primary action highlight, active accents)
  - Cyan Accent `#0891B2` (secondary highlights, precision metadata)
  - Light Selected Container `#E0F2FE` / Dark Selected Container `#1E293B`
  - Dark Surface `#121820` / Dark Background `#0C1015`
  - Semantic feedback: `taken` Green (completed steps, valid state), `warning` Amber (incomplete step hints, discard rules), `missed` Red (inline validation errors).
- **Typography & Tabular Numerals**:
  - `.tabular` applied to step counters (`01`, `02`, `03`), time presets (`08:00`), capacities (`300`), package totals (`600 drops`), daily rates (`10 drops/day`), and estimated days remaining (`~60 days`).

---

## 3. Step-by-Step UX Architecture
1. **Header**:
   - Top Bar: `[Close]` (48dp), `"Add Medication"` (`titleLarge`), subtitle `"Add the medication you want to track."`, pill `"Step X of 3"`.
   - Compact Horizontal Stepper (`01 Identity` — `02 Schedule` — `03 Stock`): circular badge (`01`/`Check`), concise label, animated progress track (`animateFloatAsState`).
2. **Step 1 – Identity & Packaging**:
   - Required fields (`Medication Name *`, `Strength / Concentration *`) with clinical icons only where helpful, supporting examples (`Hydro Forte`, `20 mg tablets`, `0.15% eye drops`), inline `isError` feedback, and `ImeAction.Next`.
   - **Dosage Form Grid**: 2-column responsive cards across all 9 `MedicationForm` values (`TABLET`, `CAPSULE`, `LIQUID_ML`, `DROPS`, `INHALER_PUFFS`, `TOPICAL_CREAM`, `INJECTION`, `PATCH`, `OTHER`) with clinical icons and short descriptors.
   - **Form-Change Protection**: Switching forms after manual package customization prompts `FormChangeConfirmationDialog` before replacing package defaults.
   - **Contextual Package Selection**: Responsive cards filtered strictly by selected form (`BLISTER_PACK`, `BOTTLE`, `BOX_SINGLE_DOSE`, `SINGLE_DOSE_UNIT`, `TUBE`, `JAR`, `INHALER`, `VIAL`, `PATCH_BOX`, `CUSTOM`), plus Gel/Cream tracking modes and usable contents/discard period.
3. **Step 2 – Administration, Schedule & Recording**:
   - **Section A (Administration)**: Route selector (`AdministrationRoute`), contextual anatomical site cards for drops (`Left eye`, `Right eye`, `Both eyes`, `Custom`), explicit drop consumption math callout (`"1 drop in both eyes = 2 drops consumed per administration"` vs single-use container decoupling), and quantity `-` / `+` 48dp steppers.
   - **Section B (Schedule)**: `Every Day` vs `Specific Weekdays`, time presets (`08:00 Morning`..`22:00 Bedtime`) + 24h custom time adder, `Start Date` + `Ongoing schedule` toggle + optional `End Date` via Material 3 `DatePickerDialog` (`endDate >= startDate` validated).
   - **Section C (Dose Recording)**: Concise `RecordingModeSelectorCard` explaining `CONFIRM` vs `AUTO_ASSUME`.
4. **Step 3 – Stock & Package Configuration**:
   - Package-aware initial count with `-` / `+` steppers, usable capacity summary, post-opening discard presets (`28`, `30`, `90 days`, `None`), Material 3 `DatePickerDialog` for optional package expiry (`+3m`, `+6m`, `+12m`, `None`), optional batch/lot, low-stock thresholds, and **`LiveInventoryPreviewCard`** showing physical packages -> total internal quantity -> daily consumption -> estimated days runway (omitting days when schedule/capacity is incomplete).
5. **Bottom Action Bar & Validation**:
   - Inline warning banner explaining exact missing requirements when the primary button cannot advance/submit.
   - Auto-scrolls to top / first invalid field on validation failure.
   - `[Cancel]` + `[Continue →]` on Steps 1 & 2; `[Back]` + `[Cancel]` + `[Save medication]` on Step 3 with loading state and duplicate-tap lock.

---

## 4. Motion & Accessibility
- `AnimatedContent` horizontal step slide + fade (`NoveloMotion.directionalHorizontalTransition`), `animateColorAsState` / `animateFloatAsState` selection & progress highlights, and numeric live summary updates, all gated on `LocalMotionEnabled.current`.
- Every interactive control (`Close`, steps, cards, chips, `-`/`+` steppers, swatches, footer CTAs) enforces `heightIn(min = 48.dp)` and semantic roles (`Role.Tab`, `Role.RadioButton`, `Role.Button`, `heading()`).
