# Nuvilo Lovable Asset Manifest & Visual Asset Directory

**Target Platform:** Lovable Web Application Builder  
**Asset Folder Path:** `website-handoff/assets/`  
**Generated Date:** 2026-11-11  

---

## 1. Directory Structure

```text
website-handoff/assets/
├── logo/
│   ├── nuvilo-logo-transparent.png   (512 × 512, RGBA, Transparent background)
│   ├── nuvilo-icon.png               (512 × 512, RGBA, Rounded app icon)
│   └── nuvilo-logo.png               (512 × 512, RGBA, High-res logo mark)
│
├── screenshots/
│   ├── dashboard.png                 (1280 × 2856, RGBA, Native high-res Android capture)
│   ├── medications.png               (1280 × 2856, RGB,  Native high-res Android capture)
│   ├── setup.png                     (1280 × 2856, RGBA, Native high-res Android capture)
│   ├── stock.png                     (1280 × 2856, RGBA, Native high-res Android capture)
│   ├── calendar.png                  (1280 × 2856, RGBA, Native high-res Android capture)
│   ├── history.png                   (1280 × 2856, RGBA, Native high-res Android capture)
│   ├── prescriptions.png             (1280 × 2856, RGBA, Native high-res Android capture)
│   └── privacy.png                   (1280 × 2856, RGBA, Native high-res Android capture)
│
└── backgrounds/
    └── nuvilo-feature-graphic.png    (1024 × 500,  RGB,  Google Play clinical feature banner)
```

---

## 2. Complete Asset Inventory & Placement Guide

| Asset Path | Resolution | Format | Purpose | Recommended Website Placement | Recommended `alt` Text |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `assets/logo/nuvilo-logo-transparent.png` | 512 × 512 | RGBA (Alpha) | Primary brand logo with transparent backdrop | **Header**: Top-left navigation bar.<br>**Hero**: App title lockup.<br>**Footer**: Bottom branding. | `"Nuvilo Logo — Medication, made manageable."` |
| `assets/logo/nuvilo-icon.png` | 512 × 512 | RGBA | Official square/rounded Android app icon | **Hero**: Floating app badge.<br>**Meta**: Favicon & Open Graph thumbnail (`og:image`). | `"Nuvilo Application Icon"` |
| `assets/logo/nuvilo-logo.png` | 512 × 512 | RGBA | High-res monogram mark | Used for branding accents, About page header, or dark sections. | `"Nuvilo Monogram"` |
| `assets/screenshots/dashboard.png` | 1280 × 2856 | RGBA | Today's timeline, 100% adherence KPI, active doses | **About page hero / Feature showcase**: Primary phone mockup demonstrating daily medication routines. | `"Nuvilo Dashboard showing today's medication timeline and 100% adherence score"` |
| `assets/screenshots/medications.png` | 1280 × 2856 | RGB | Medication registry, stock balances, active cards | **About page**: Regimen management section, highlighting days of supply and color tags. | `"Nuvilo Medication List showing active medications and remaining days of supply"` |
| `assets/screenshots/setup.png` | 1280 × 2856 | RGBA | Clinical add medication wizard with forms & units | **About page**: Step-by-step setup guide showcasing pharmaceutical form selection. | `"Nuvilo Add Medication Wizard showing dosage form selection and schedule options"` |
| `assets/screenshots/stock.png` | 1280 × 2856 | RGBA | Stock ledger, FEFO batch lots, expiry tracking | **About page & Privacy Policy**: Demonstrating local inventory ledger and lot management. | `"Nuvilo Stock Management interface showing FEFO batch lots and expiry tracking"` |
| `assets/screenshots/calendar.png` | 1280 × 2856 | RGBA | Monthly adherence calendar with color-coded status | **About page**: Adherence tracking section, illustrating monthly consistency rings. | `"Nuvilo Adherence Calendar showing monthly dose completion scores"` |
| `assets/screenshots/history.png` | 1280 × 2856 | RGBA | Reverse chronological dose log & audit trail | **About page & Privacy Policy**: Demonstrating local historical record-keeping and auditability. | `"Nuvilo Dose History log showing chronological confirmation records"` |
| `assets/screenshots/prescriptions.png` | 1280 × 2856 | RGBA | Repeat prescription tracker with remaining repeats | **About page**: Prescription lifecycle management section showing doctor & pharmacy details. | `"Nuvilo Prescription Management interface showing repeat allowances and pharmacy notes"` |
| `assets/screenshots/privacy.png` | 1280 × 2856 | RGBA | Settings, App Lock, Encrypted Backup, Privacy Mode | **Privacy Policy Hero & About page**: Hero visual demonstrating physical security and offline backups. | `"Nuvilo Privacy & Security Settings showing App Lock, Notification Masking, and Encrypted Backups"` |
| `assets/backgrounds/nuvilo-feature-graphic.png` | 1024 × 500 | RGB | Official Google Play marketing feature graphic | **Open Graph / Social preview**: Website `og:image` and Twitter card banner. | `"Nuvilo Medication Management Feature Graphic"` |

---

## 3. Upload & Integration Instructions for Lovable

1. **Upload Assets**: When initializing the Lovable project, upload all 12 files from `website-handoff/assets/` into Lovable's asset manager or place them in `public/assets/`.
2. **Transparent Logo Rule**: Always use `assets/logo/nuvilo-logo-transparent.png` for header navigation, hero lockups, and footer branding. Never place a white-bordered logo onto dark or light blue containers.
3. **Mockup Framing**: When displaying screenshots (`dashboard.png`, `privacy.png`, etc.), wrap them in modern CSS device frames (`rounded-[36px] border-[6px] border-slate-800 shadow-2xl`) or use responsive floating perspective cards.
4. **Synthetic Data Integrity**: All 8 screenshots feature synthetic demo regimens (Atorvastatin, Metformin, Lisinopril, Ventolin, Amoxicillin). They contain zero private health information and are completely safe for public web publication.
