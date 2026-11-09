# Nuvilo — Stage 2 UI/UX Issues & Remediation Log

**Date:** 2026-11-05  
**Product:** Nuvilo – *Medication, made manageable.*  
**Environment:** Android API 37, Native Jetpack Compose.  
**Git Branch:** `feat/6-uiux-pro-max-implementation`.

---

## 1. Summary of Identified Defects & Remediations

During the Stage 2 rollout and emulator inspection, several visual and interaction defects were discovered and remediated:

### Issue 1: Character Width Jitter on Stock Balances and Adherence Percentages
- **Severity**: Low (Visual polish).
- **Description**: Proportional numeric fonts caused the width of numbers like `1` vs `8` to vary, causing horizontal jitter during real-time adherence updates or ledger recalculations.
- **Root Cause**: Default Roboto Sans font using proportional numeric figures.
- **Remediation**: Implemented `fontFeatureSettings = "tnum"` via `NoveloNumericTypography` and extension property `.tabular` across `Type.kt`. Applied to all numeric cards, timestamps, lot quantities, and badges.

### Issue 2: Inconsistent Touch Target Heights in Hero and Standard Dose Cards
- **Severity**: Medium (WCAG AA Accessibility).
- **Description**: Small secondary buttons ("Take", "Snooze", "Skip") had implicit heights below 48dp on high-density viewports.
- **Root Cause**: Explicit vertical padding without `heightIn(min = 48.dp)`.
- **Remediation**: Added `heightIn(min = 48.dp)` and wrapped in `NoveloPillShape`, guaranteeing compliance with Android accessibility touch target standards.

### Issue 3: Low Contrast on Dark Mode Outline Borders
- **Severity**: Low (Visual hierarchy).
- **Description**: In Dark Mode, card borders using `outlineVariant` without alpha compensation were slightly indistinct against `#0F172A` background.
- **Root Cause**: Generic Material 3 dark container default styling.
- **Remediation**: Layered `NoveloDarkContainer` (`#1E293B`) with explicit border strokes (`1.dp` with `outlineVariant.copy(alpha = 0.5f)`), yielding clear edge definition without harsh white outlines.

### Issue 4: Directionless Tab Navigation Transitions
- **Severity**: Low (Motion UX).
- **Description**: Switching between bottom navigation tabs (Today -> Medications -> Adherence -> Settings) played a uniform vertical fade/slide, failing to communicate horizontal spatial relationship.
- **Root Cause**: Single `AnimatedContent` transition spec regardless of source/target tab index.
- **Remediation**: Implemented `NoveloMotion.directionalHorizontalTransition(targetState.ordinal > initialState.ordinal)` in `MainActivity.kt`.

---

## 2. Unresolved / Upstream Observations
- None. All identified Stage 2 UI/UX enhancement targets and safety invariants were fully verified and resolved.
