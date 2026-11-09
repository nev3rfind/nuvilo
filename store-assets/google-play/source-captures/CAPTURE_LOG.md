# Novelo — Source Capture Audit & Device Profile Log

**Date:** 2026-11-09  
**Application:** Novelo – *Medication, made manageable.*  
**Package:** `com.example.nuvilo`  
**Target Platform:** Google Play Store (Phone & Tablet)

---

## 1. Device Profiles & Capture Environment

| Environment | Device Target | Resolution | Density | Window Insets | OS / API |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Phone Profile** | Google Pixel 10 Pro / Emulator-5554 | 1080 × 2400 (Native 9:20) | 420 dpi | Edge-to-edge system bars | Android 17 (API 37) |
| **Tablet Profile (Portrait)** | Large-Screen Tablet Profile (`sw720dp`) | 1080 × 1920 (Native 9:16) | 240 dpi | Safe drawing insets | Android 17 (API 37) |
| **Tablet Profile (Landscape)** | Large-Screen Tablet Profile (`sw600dp+`) | 1920 × 1080 (Native 16:9) | 240 dpi | Safe drawing insets | Android 17 (API 37) |

---

## 2. Responsive UI Evaluation & Findings

1. **Phone Responsive Performance**:
   - The phone interface demonstrates exceptional information density and visual rhythm.
   - The hybrid Hero Dose card and Timeline list provide immediate orientation for next dose actions.
   - All interactive touch targets strictly meet or exceed the $48 \times 48\text{dp}$ WCAG AA threshold.
   - Tabular numerals (`fontFeatureSettings = "tnum"`) eliminate numeral width jitter across timers and stock counts.

2. **Tablet Responsive Evaluation**:
   - **Landscape (1920 × 1080)**: Novelo currently uses a single-pane vertical column layout with bottom navigation. In 16:9 landscape, bottom navigation tab items and full-width card containers span the entire 1920px horizontal width, producing noticeable negative horizontal whitespace.
   - **Portrait (1080 × 1920)**: In tablet portrait at 240 dpi (`sw720dp`), Novelo presents balanced card margins, natural typographic scale, and cohesive touch targets without artificial stretching.
   - **Play Console Recommendation**: Tablet portrait (1080 × 1920) is selected as the primary presentation mode as it most faithfully represents the application's actual UI and complies with Google Play large-screen guidelines.

---

## 3. Demonstration Data Integrity

All source captures utilize reproducible synthetic clinical data:
- **Medications**: Amoxicillin (500mg capsule, 3x daily), Lisinopril (10mg tablet, once daily morning), Metformin (500mg tablet, twice daily with meals).
- **Stock Accounting**: Scaled integer arithmetic (`ScaledQuantity`); FEFO lot allocation with batch lot numbers and expiration dates.
- **Adherence Metrics**: 80% clinical adherence benchmark comparison with non-color indicator badges (`NoveloBulletKpiCard`).
- **Security & Privacy**: Zero-network offline architecture statement, biometric app lock preference, and AES-256-GCM encrypted SAF backup export.
