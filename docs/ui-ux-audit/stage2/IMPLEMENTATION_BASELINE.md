# Nuvilo — Stage 2 UI/UX Baseline Inspection Report

**Date:** 2026-11-05  
**Product:** Nuvilo – *Medication, made manageable.*  
**Environment:** Android Emulator (`emulator-5554`), API 37, Native Jetpack Compose, Material 3.  
**Baseline Build:** Commit `2a05360` (`feat/6-uiux-pro-max-implementation`).

---

## 1. Executive Summary

Prior to implementing Stage 2 of the UI/UX Pro Max design system rollout, the live running application was inspected across all primary and secondary routes in both Light and Dark themes. Synthetic clinical data (Atorvastatin 20mg, daily schedule, lot allocations, 100% adherence, and repeat prescription metadata) was verified.

A total of 11 baseline screenshots were captured and archived in `docs/screenshots/stage2/before/`:
- `01_dashboard_light.png`
- `02_medications_list_light.png`
- `03_medication_detail_light.png`
- `04_adherence_calendar_light.png`
- `05_settings_light.png`
- `06_settings_dark.png`
- `07_dashboard_dark.png`
- `08_medications_list_dark.png`
- `09_medication_detail_dark.png`
- `10_add_medication_wizard_light.png`
- `11_restock_dialog_light.png`

---

## 2. Identified Weaknesses & Enhancement Opportunities

### 2.1 Typography & Numerics
1. **Lack of Tabular Numerics (`tnum`)**: Stock counts (`26 tab left`), timestamps (`08:00`), percentages (`100%`), and dosages (`20mg · 1 tablets`) use proportional figures without `fontFeatureSettings = "tnum"`. In lists and comparison cards, changing numbers cause minor horizontal jitter and uneven alignment.
2. **Typography Hierarchy Flatness**: Section titles, hero metrics, and supporting metadata share similar font weights and tracking, lacking the intentional editorial hierarchy recommended by UI/UX Pro Max.

### 2.2 Brand Colours & Tonal Surfaces
1. **Primary Blue Monolith**: Nuvilo NHS Blue (`#005EB8`) is applied uniformly across navigation indicators, buttons, chips, and borders. The app lacks the layered tonal hierarchy (`#0284C7` clinical interaction, `#0891B2` cyan/teal accent, `#E0F2FE` light container) generated in `MASTER.md`.
2. **Dark Theme Elevation**: While dark mode achieves compliant contrast, elevated card surfaces (`#131C27`) blend slightly into root background (`#0B111A`) in low-light environments without crisp tonal container borders.

### 2.3 Mobile Data Presentation & 3-Tier Layouts
1. **Dose History Rows**: Chronological entries currently display status, time, and medication across a single flat row without clear separation between Tier 1 (primary identity/dose), Tier 2 (scheduled vs recorded time), and Tier 3 (compensating reversal context / audit notes).
2. **Stock Ledger Presentation**: The append-only ledger entries present quantity and balance changes in plain text, making it harder to scan inflow versus outflow deltas at a glance.
3. **FEFO Lot Allocation**: Batch lots lack an explicit, high-contrast visual badge differentiating the currently active lot from reserve lots.

### 2.4 Component Silhouettes & Spacing
1. **Corner Radii Consistency**: Chips and badges currently use `8.dp` or `12.dp` rounded rectangles rather than the modern M3 Expressive pill silhouette (`RoundedCornerShape(999.dp)`).
2. **Empty States**: Empty state cards provide text and an action button, but lack high-contrast focal iconography and structured guidance.

### 2.5 Motion & Tactile Feedback
1. **Dose Action Transition**: Marking a dose as taken immediately switches states; adding smooth spring-scale confirmation enhances clinical confidence and delight.
2. **Tab Navigation Transition**: Screen switches use vertical slide-ins; adding horizontal shared-axis navigation across bottom tabs improves spatial orientation.

---

## 3. Stage 2 Scope & Priority Matrix

| Step | Focus Area | Deliverables | Priority |
| :--- | :--- | :--- | :--- |
| **Step 1** | **Typography Redesign** | `Type.kt` tabular tokens (`tnum`), display/title/body/label numeric extensions | <badge color="success">Highest</badge> |
| **Step 2** | **Brand Colour System** | `Color.kt` & `Theme.kt`: Pro Max tonal containers (`#0284C7`, `#0891B2`, `#E0F2FE`), contrast verification | <badge color="success">High</badge> |
| **Step 3** | **Premium Components** | `NoveloComponents.kt`: Pill badges, 3-tier data rows, metric cards, enhanced `HeroDoseCard` | <badge color="success">High</badge> |
| **Step 4** | **Data Presentation & Tables** | Structured ledger rows, FEFO lot badges, bullet adherence KPI card, compact dose rows | <badge color="success">High</badge> |
| **Step 5** | **Application Screens Rollout** | Dashboard, Medications, Detail, Wizard, Calendar, History, Settings, LockScreen | <badge color="success">High</badge> |
| **Step 6** | **Micro-interactions & Animation**| Motion 3/10 spring transitions, animated dose confirmation, pill state changes | <badge color="success">Medium</badge> |
| **Step 7** | **Bug Fixing & QA** | Touch targets $\ge 48\text{dp}$, large font scaling, dark mode parity, SAF backups | <badge color="success">Critical</badge> |
