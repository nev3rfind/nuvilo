# Novelo — Google Play Store Screenshot Campaign Assets

This directory contains the complete marketing screenshot collection, editable Next.js screenshot editor project, authentic source application captures, contact sheet previews, and technical export validation reports for **Novelo** on Google Play.

Tagline: *"Medication, made manageable."*  
Package: `com.example.nuvilo`  
Target Locale: `en-GB`

---

## 1. Directory Structure

```text
store-assets/google-play/
├── editor/                     # Next.js 16 + React 19 editable screenshot editor
│   ├── app-store-screenshots.json  # Canonical project source-of-truth deck configuration
│   ├── package.json
│   ├── public/                 # Served assets, frames, app icons, and screenshots
│   └── src/                    # Editor UI, canvas, typography, themes, device frames
│
├── source-captures/            # High-resolution empirical captures from live Android emulators
│   ├── CAPTURE_LOG.md          # Technical audit of devices, window sizes, and DPI
│   ├── phone/                  # 8 source screens captured at 1080 × 2400 (420 dpi)
│   └── tablet/                 # 8 source screens captured at 1080 × 1920 (240 dpi)
│
├── exports/                    # Production-ready marketing deliverables
│   ├── phone/en-GB/            # 8 Phone screenshots (1080 × 1920, 9:16 portrait, RGB opaque)
│   ├── tablet/en-GB/           # 8 Tablet screenshots (1080 × 1920, 9:16 portrait, RGB opaque)
│   ├── feature-graphic/        # Google Play Feature Graphic (1024 × 500, RGB opaque)
│   └── preview/                # Contact sheets and campaign composite overview
│
├── validate_exports.py         # Automated Python/Pillow technical verification suite
├── EXPORT_VALIDATION.md        # Technical validation audit report (17/17 checks passed)
├── SCREENSHOT_CONTENT.md       # Content catalog, feature mapping, and policy integrity audit
├── ALT_TEXT.md                 # Screen-reader alt-text descriptions (all <= 140 characters)
└── README.md                   # This documentation
```

---

## 2. Technical Specifications & Validation Summary

| Asset Group | Count | Target Dimensions | Aspect Ratio | Color Depth | Alpha Channel | Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Phone Screenshots** | 8 | 1080 × 1920 px | 9:16 portrait | 24-bit RGB | **None (Opaque)** | **VERIFIED** |
| **Tablet Screenshots** | 8 | 1080 × 1920 px | 9:16 portrait | 24-bit RGB | **None (Opaque)** | **VERIFIED** |
| **Feature Graphic** | 1 | 1024 × 500 px | 2.048:1 landscape | 24-bit RGB | **None (Opaque)** | **VERIFIED** |
| **Contact Sheets** | 3 | Varied | Composite | 24-bit RGB | **None (Opaque)** | **GENERATED** |

---

## 3. Marketing Story Sequence

1. **`01-dashboard.png`** — *Your medication day, at a glance.*  
   Hero next-dose card, scheduled time countdown, one-tap Take/Snooze/Skip pill actions, 80% clinical adherence benchmark meter.
2. **`02-medications.png`** — *Every medication, clearly organised.*  
   Pill search bar, Active/Archived segmented tabs, medication cards with strength, form, schedule chips, and "+ Stock" shortcuts.
3. **`03-setup.png`** — *Create your routine, step by step.*  
   Guided 3-step setup wizard (1. Identity, 2. Schedule, 3. Stock), form selector, intake frequency, and recording preference mode.
4. **`04-stock.png`** — *Know what's in your supply.*  
   Remaining supply indicator, batch lot tracking with FEFO (First-Expired, First-Out) badge, expiration dates, and 3-tier mobile stock movements ledger.
5. **`05-calendar.png`** — *See your routine over time.*  
   Monthly calendar grid with color-coded day status dots, daily dose breakdown, and 80% clinical target compliance comparison.
6. **`06-history.png`** — *A clear record of every dose.*  
   Chronological date grouping (Today, Yesterday), verified vs assumed outcome badges, scheduled vs actual recorded timestamps, compensating reversals.
7. **`07-prescriptions.png`** — *Keep prescriptions organised.*  
   Doctor repeat authorization status, remaining repeats count (e.g. 3 of 4 remaining), collection dates, and expiry tracking.
8. **`08-privacy.png`** — *Privacy built into your routine.*  
   100% offline zero-network boundary statement, AndroidX BiometricPrompt app lock, PBKDF2/AES-256-GCM encrypted local SAF backups.

---

## 4. How to Run the Next.js Screenshot Editor

The interactive editor allows visual adjustments to copy, layouts, transforms, and device frames:

```powershell
# Navigate to editor directory
cd store-assets/google-play/editor

# Start Next.js local development server
npm run dev
```

Open `http://localhost:3000` in any web browser. Edits automatically persist to `app-store-screenshots.json`.

---

## 5. How to Re-Run the Export & Validation Suite

```powershell
# From repository root
# 1. Render all 17 assets and contact sheets
python scratch/render_assets.py

# 2. Run automated validation checks
python store-assets/google-play/validate_exports.py
```
