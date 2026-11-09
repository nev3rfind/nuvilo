# Previous Google Play Screenshot Campaign — Failure Analysis & Remediation Report

**Product:** **Nuvilo** (*"Medication, made manageable."*)  
**Date:** 2026-11-09  
**Archived Previous Campaign Location:** `store-assets/google-play/previous-campaign-archive/`  
**Redesigned Campaign Location:** `store-assets/google-play/nuvilo/`  

---

## 1. Executive Summary

Before designing the new **Nuvilo** Google Play Store campaign, every single exported image from the previous campaign (`01-dashboard.png` through `08-privacy.png` for both Phone and Tablet, plus the feature graphic) was individually inspected.

The previous campaign failed both art-direction quality standards and fundamental content authenticity requirements. All 16 screenshots and the feature graphic have been completely discarded and regenerated from scratch using authentic, seeded **Nuvilo** application captures and the official `ParthJadhav/app-store-screenshots` Next.js editor.

---

## 2. Detailed Defect Audit (Problems A–G)

### Problem A: Device Screens Were Too Small & Canvas Was Empty
- **Observed in Previous Campaign**: In `scratch/render_assets.py`, the phone frame was rendered at a small fixed scale (`y = 500`, `width = 658px` on a `1080px` canvas, ~45% width area) floating inside a pale `#F8FAFC` background with excessive dead space around the sides.
- **Remediation in New Campaign**: Redesigned using the official Next.js screenshot editor with large, high-impact device framing occupying **62%–74% of the canvas area** (`78%–86%` of phone width), cropped/anchored dynamically so the actual **Nuvilo** interface is the unmistakable hero subject on every slide.

### Problem B: Headlines Were Weak, Cramped & Lacked Hierarchy
- **Observed in Previous Campaign**: Headlines were small, left-aligned with low visual weight, lacked accent word highlighting, and were accompanied by tiny pill tags (`19px` font) that became completely illegible at Google Play search-result thumbnail scale (`160px–220px` width).
- **Remediation in New Campaign**: Replaced with bold **Figtree** display headlines (3–5 words per slide), generous top breathing room, high-contrast color pairings ($\ge 7.5:1$ contrast), and a single accent-highlighted keyword per headline (`#005EB8` / `#38BDF8` / `#0284C7`) verified via a dedicated `160px` thumbnail legibility test.

### Problem C: Repeated Medication-List Screens Across Slides 02, 03, and 04
- **Observed in Previous Campaign**:
  - `phone/en-GB/02-medications.png` showed the Medications list screen (`Active (2)`).
  - `phone/en-GB/03-setup.png` ("Create your routine, step by step") showed the **exact same Medications list screen** instead of the 3-step Add Medication wizard (`02_medications_list_light.png` and `10_add_medication_wizard_light.png` in `docs/screenshots/stage2/after/` were byte-for-byte identical `187,584`-byte files!).
  - `phone/en-GB/04-stock.png` ("Know what's in your supply") also showed the **Medications list screen** with the search bar focused instead of the Medication Detail FEFO batch lot & stock ledger screen.
  - On Tablet (`tablet/en-GB/01` through `07`), slides 02, 03, and 04 all showed an empty `"No Active Medications"` screen, and slides 05, 06, and 07 all showed an empty `"No Adherence Data Yet"` screen!
- **Remediation in New Campaign**: Every slide (`01` through `08`) on both Phone and Tablet now uses a **100% distinct, freshly captured Nuvilo screen** backed by a rich synthetic clinical dataset:
  - **Slide 02 (`medications.png`)**: Medications list with 5 realistic medications, dosage forms, schedule pills, and stock badges.
  - **Slide 03 (`setup.png`)**: Genuine 3-step **Add Medication** wizard (`Step 2 of 3 · Schedule` / `Step 1 · Identity` / `Step 3 · Stock`) showing step capsules, frequency chips, and dose time pickers.
  - **Slide 04 (`stock.png`)**: Genuine **Medication Detail** Stock & Batch Lots view showing current supply, FEFO (`First-Expiry, First-Out`) batch lots with expiry dates, and the append-only stock ledger.

### Problem D: Android Soft Keyboard Left Visible Obscuring Content (Slide 06)
- **Observed in Previous Campaign**: `phone/en-GB/06-history.png` ("A clear record of every dose") displayed the Medications list with the search field focused and the **Android QWERTY soft keyboard covering the bottom 40% of the screen** — and didn't even show the Dose History screen!
- **Remediation in New Campaign**: Captured the dedicated **Medication Dose History** screen (`MedicationDoseHistoryScreen`) showing the 30-Day Adherence Summary banner, date/outcome filter chips, and chronological dose cards (`Confirmed`, `Assumed`, `Skipped`, scheduled vs recorded timestamps, and audit revision notes) with **zero soft keyboard visible**.

### Problem E: Suspicious Google-Branded Cookie Consent Screen (Slide 07)
- **Observed in Previous Campaign**: `phone/en-GB/07-prescriptions.png` ("Keep prescriptions organised") displayed a **"Google — Before you continue to Google"** web cookie consent screen instead of Nuvilo! This occurred because an earlier capture script copied `11_restock_dialog_light.png` which had been captured while Chrome/Google was in the foreground on the emulator.
- **Remediation in New Campaign**: Verified foreground window focus (`com.example.nuvilo/.MainActivity`) before every capture and navigated directly to Nuvilo's **Prescription & Repeat Tracking** section on `MedicationDetailScreen`, showcasing active prescription reference numbers, repeats remaining (`4 of 6 repeats left`), issue/expiry dates, pharmacy details, and repeat request/collection history.

### Problem F: Monotonous Identical Composition Across All 8 Slides
- **Observed in Previous Campaign**: Every single slide used the exact same top-left text + centered bottom phone frame layout, with only one dark background on Slide 06.
- **Remediation in New Campaign**: Applied the **Bento Keynote Grid + Clinical Editorial** art direction across the 8-slide narrative arc, varying layouts so consecutive slides never repeat the same composition (Hero Center Stage, Editorial Split Left, Elevated Bento Callout, Dark Slate Contrast Spotlight, Top-Anchored Device Peek, Dual Badge Focus, Clinical Card Grid, and Dark Security Vault Finale).

### Problem G: Incorrect Product Name (`Novelo` Instead of `Nuvilo`) & Weak Feature Graphic
- **Observed in Previous Campaign**: The feature graphic (`novelo-feature-graphic.png`) and app UI used the incorrect name `"Novelo"`, and the feature graphic was just a lonely app icon over a plain blue gradient with no visual connection to the product UI.
- **Remediation in New Campaign**: Completed a full codebase and documentation rebrand to **`Nuvilo`**, updated `appName: "Nuvilo"` in the screenshot editor, and designed a rich `1024 × 500` feature graphic (`nuvilo-feature-graphic.png`) featuring the **Nuvilo** wordmark, tagline *"Medication, made manageable."*, key clinical trust pills, and an angled architectural preview of the real Nuvilo Dashboard UI.
