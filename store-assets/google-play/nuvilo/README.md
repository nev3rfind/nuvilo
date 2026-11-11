# Nuvilo — Google Play Store Screenshot Campaign & Editor Project

**Official Application Name:** Nuvilo  
**Tagline:** Medication, made manageable.  
**Package Name:** `com.nuvilomed.app`  
**Screenshot Editor Tool:** [`ParthJadhav/app-store-screenshots`](https://github.com/ParthJadhav/app-store-screenshots) (MIT License)

---

## 1. Directory Structure

```text
store-assets/google-play/nuvilo/
├── editor/                             # Reproducible Next.js screenshot editor project (appName: "Nuvilo")
│   ├── app-store-screenshots.json      # Source-of-truth project JSON (Phone + 10" Tablet + Feature Graphic)
│   ├── package.json                    # Next.js 16 + React 19 + html-to-image + JSZip
│   ├── public/
│   │   ├── app-icon.png                # Official Nuvilo launcher icon
│   │   └── screenshots/                # Synced phone & tablet source captures
│   └── src/                            # Editor UI, device presets (1080×1920 & 1920×1080), & theme tokens
├── source-captures/                    # Raw, unedited Jetpack Compose screen captures from emulator-5554
│   ├── phone/                          # 8 distinct phone captures (1080 × 2400 @ 420 dpi)
│   │   ├── dashboard.png
│   │   ├── medications.png
│   │   ├── setup.png
│   │   ├── stock.png
│   │   ├── calendar.png
│   │   ├── history.png
│   │   ├── prescriptions.png
│   │   └── privacy.png
│   └── tablet/                         # 8 distinct large-screen tablet captures (1200 × 1920 @ 240 dpi)
│       ├── dashboard.png
│       ├── medications.png
│       ├── setup.png
│       ├── stock.png
│       ├── calendar.png
│       ├── history.png
│       ├── prescriptions.png
│       └── privacy.png
├── exports/                            # Final Google Play Store submission-ready PNGs (24-bit RGB, no alpha)
│   ├── phone/
│   │   └── en-GB/                      # 8 × 1080 × 1920 (9:16 portrait)
│   │       ├── 01-dashboard.png
│   │       ├── 02-medications.png
│   │       ├── 03-setup.png
│   │       ├── 04-stock.png
│   │       ├── 05-calendar.png
│   │       ├── 06-history.png
│   │       ├── 07-prescriptions.png
│   │       └── 08-privacy.png
│   ├── tablet/
│   │   └── en-GB/                      # 8 × 1920 × 1080 (16:9 landscape)
│   │       ├── 01-dashboard.png
│   │       ├── 02-medications.png
│   │       ├── 03-setup.png
│   │       ├── 04-stock.png
│   │       ├── 05-calendar.png
│   │       ├── 06-history.png
│   │       ├── 07-prescriptions.png
│   │       └── 08-privacy.png
│   └── feature-graphic/
│       └── nuvilo-feature-graphic.png  # 1 × 1024 × 500 (24-bit RGB, no alpha)
├── preview/                            # Visual contact sheets & 160px thumbnail legibility strip
│   ├── phone-contact-sheet.png
│   ├── tablet-contact-sheet.png
│   ├── full-campaign.png
│   └── thumbnail-160px-check.png
├── ALT_TEXT.md                         # Google Play accessibility alt text (<= 140 chars per asset)
├── BRAND_AUDIT.md                      # Complete Novelo -> Nuvilo rebrand audit
├── EXPORT_VALIDATION.md                # Automated technical validation report (17/17 PASS)
├── PREVIOUS_CAMPAIGN_REVIEW.md         # Root-cause review of the archived flawed campaign
├── SCREENSHOT_CONTENT.md               # Slide-by-slide copy, layout, and screen mapping
├── render_campaign.py                  # Deterministic rendering & editor sync script
└── validate_exports.py                 # Automated PNG dimension, RGB mode, and size validator
```

---

## 2. Running the Interactive Next.js Screenshot Editor

To open and edit the Nuvilo screenshot campaign interactively in your browser:

```powershell
cd store-assets/google-play/editor
npm install
npm run dev
```

Then open `http://localhost:3000` in your browser. The editor automatically loads `app-store-screenshots.json` (`appName: "Nuvilo"`, `themeId: "nuvilo-clinical"`).

---

## 3. Reproducing the Exports & Running Technical Validation

To deterministically regenerate all 8 Phone screenshots (`1080 × 1920`), all 8 Tablet screenshots (`1920 × 1080`), the Feature Graphic (`1024 × 500`), and the preview contact sheets from the source captures:

```powershell
python store-assets/google-play/nuvilo/render_campaign.py
python store-assets/google-play/nuvilo/validate_exports.py
```
