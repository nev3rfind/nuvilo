# Nuvilo — Google Play Store Export Technical Validation Report

**Application Name:** Nuvilo  
**Tagline:** Medication, made manageable.  
**Authority:** [Google Play Official Store Listing Requirements](https://support.google.com/googleplay/android-developer/answer/9866151)  
**Package:** `com.nuvilomed.app`  
**Status:** `VERIFIED PASS`

---

## 1. Phone Screenshots (8 Required, 1080 × 1920, 9:16 Portrait)

| Asset Filename | Dimensions | Image Mode | File Size | Aspect Ratio | Verification Result |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `01-dashboard.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 389.0 KB | 0.562 | **PASSED** |
| `02-medications.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 276.0 KB | 0.562 | **PASSED** |
| `03-setup.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 488.9 KB | 0.562 | **PASSED** |
| `04-stock.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 346.3 KB | 0.562 | **PASSED** |
| `05-calendar.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 331.3 KB | 0.562 | **PASSED** |
| `06-history.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 496.3 KB | 0.562 | **PASSED** |
| `07-prescriptions.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 464.0 KB | 0.562 | **PASSED** |
| `08-privacy.png` | 1080 × 1920 | RGB (24-bit Opaque, No Alpha) | 400.2 KB | 0.562 | **PASSED** |

---

## 2. Tablet Screenshots (8 Required, 1920 × 1080, 16:9 Landscape)

| Asset Filename | Dimensions | Image Mode | File Size | Aspect Ratio | Verification Result |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `01-dashboard.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 300.2 KB | 1.778 | **PASSED** |
| `02-medications.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 340.4 KB | 1.778 | **PASSED** |
| `03-setup.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 281.9 KB | 1.778 | **PASSED** |
| `04-stock.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 287.2 KB | 1.778 | **PASSED** |
| `05-calendar.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 238.2 KB | 1.778 | **PASSED** |
| `06-history.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 408.2 KB | 1.778 | **PASSED** |
| `07-prescriptions.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 417.8 KB | 1.778 | **PASSED** |
| `08-privacy.png` | 1920 × 1080 | RGB (24-bit Opaque, No Alpha) | 313.7 KB | 1.778 | **PASSED** |

---

## 3. Google Play Feature Graphic (1024 × 500)

| Asset Filename | Dimensions | Image Mode | File Size | Aspect Ratio | Verification Result |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `nuvilo-feature-graphic.png` | 1024 × 500 | RGB (24-bit Opaque, No Alpha) | 257.6 KB | 2.048 | **PASSED** |

---

## 4. Contact Sheets, Full Campaign & 160px Thumbnail Check

| Preview Asset | Dimensions | File Size | Mode | Status |
| :--- | :---: | :---: | :---: | :---: |
| `phone-contact-sheet.png` | 1560 × 1442 | 932.6 KB | RGB | **GENERATED** |
| `tablet-contact-sheet.png` | 1352 × 1650 | 815.9 KB | RGB | **GENERATED** |
| `full-campaign.png` | 1840 × 1460 | 1139.7 KB | RGB | **GENERATED** |
| `thumbnail-160px-check.png` | 1424 × 360 | 279.8 KB | RGB | **GENERATED** |

---

## 5. Compliance Matrix Summary

- **Total Technical Assertions:** 17
- **Passed Checks:** 17
- **Failed Checks:** 0
- **Compliance Pass Rate:** 100.0%

### Key Audit Assertions Confirmed:

1. **Brand Identity:** All 17 store assets, headlines, badges, and editor configurations display **Nuvilo** (`appName: "Nuvilo"`, zero `Novelo` references).
2. **Alpha Channel Absence:** Every single export is 100% 24-bit `RGB` PNG with zero alpha channel (`RGBA` or transparency index), strictly complying with Google Play Console requirements.
3. **Exact Google Play Resolutions:** All 8 phone screenshots strictly match `1080 × 1920` (9:16 portrait); all 8 tablet screenshots strictly match `1920 × 1080` (16:9 landscape); the feature graphic strictly matches `1024 × 500`.
4. **Genuine Screen Uniqueness:** All 8 slides use distinct real Jetpack Compose captures (`dashboard.png`, `medications.png`, `setup.png`, `stock.png`, `calendar.png`, `history.png`, `prescriptions.png`, `privacy.png`) with zero soft keyboards and zero third-party web pages.
