# Nuvilo — Google Play Store Screenshot Campaign & Editor Project

**Official Application Name:** Nuvilo  
**Tagline:** Medication, made manageable.  
**Package Name:** `com.nuvilomed.app`  
**Screenshot Editor Tool:** [`ParthJadhav/app-store-screenshots`](https://github.com/ParthJadhav/app-store-screenshots) (MIT License)

---

## 1. Directory Structure

```text
store-assets/google-play/
├── nuvilo/                             # Canonical Nuvilo store-asset campaign package
│   ├── editor/                         # Reproducible Next.js screenshot editor (appName: "Nuvilo")
│   ├── source-captures/                # Raw, unedited Jetpack Compose captures (phone + tablet)
│   ├── exports/                        # Final 24-bit RGB PNGs (8 phone, 8 tablet, 1 feature graphic)
│   ├── preview/                        # Contact sheets, full campaign overview, & 160px thumbnail strip
│   ├── ALT_TEXT.md                     # Google Play accessibility alt text (<= 140 chars per asset)
│   ├── BRAND_AUDIT.md                  # Complete Novelo -> Nuvilo rebrand audit
│   ├── EXPORT_VALIDATION.md            # Automated technical validation report (17/17 PASS)
│   ├── PREVIOUS_CAMPAIGN_REVIEW.md     # Root-cause review of the archived flawed campaign
│   ├── SCREENSHOT_CONTENT.md           # Slide-by-slide copy, layout, and screen mapping
│   ├── render_campaign.py              # Deterministic rendering & editor sync script
│   └── validate_exports.py             # Automated PNG dimension, RGB mode, and size validator
│
├── editor/                             # Active Next.js 16 + React 19 screenshot editor workspace
├── source-captures/                    # Synced phone (1080×2400) & tablet (1200×1920) source captures
├── exports/                            # Synced phone (1080×1920), tablet (1920×1080), & feature graphic (1024×500)
├── preview/                            # Synced contact sheets & 160px thumbnail legibility strip
└── previous-campaign-archive/          # Archived flawed campaign for audit comparison
```

---

## 2. Technical Specifications & Validation Summary

| Asset Group | Count | Target Dimensions | Aspect Ratio | Color Depth | Alpha Channel | Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Phone Screenshots** | 8 | `1080 × 1920` px | `9:16` portrait | 24-bit RGB | **None (Opaque)** | **VERIFIED PASS** |
| **Tablet Screenshots** | 8 | `1920 × 1080` px | `16:9` landscape | 24-bit RGB | **None (Opaque)** | **VERIFIED PASS** |
| **Feature Graphic** | 1 | `1024 × 500` px | `2.048:1` landscape | 24-bit RGB | **None (Opaque)** | **VERIFIED PASS** |
| **Contact Sheets & Strip** | 4 | Composite | Composite | 24-bit RGB | **None (Opaque)** | **GENERATED** |

---

## 3. Running the Interactive Next.js Screenshot Editor

```powershell
cd store-assets/google-play/editor
npm install
npm run dev
```

Open `http://localhost:3000` in your browser. Edits automatically persist to `app-store-screenshots.json` (`appName: "Nuvilo"`, `themeId: "nuvilo-clinical"`).

---

## 4. Reproducing the Exports & Running Technical Validation

```powershell
python store-assets/google-play/nuvilo/render_campaign.py
python store-assets/google-play/nuvilo/validate_exports.py
```
