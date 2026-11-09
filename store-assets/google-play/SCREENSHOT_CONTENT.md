# Novelo — Google Play Screenshot Marketing Content & Policy Audit

**Date:** 2026-11-09  
**Product:** Novelo – *Medication, made manageable.*  
**Package:** `com.example.nuvilo`  
**Target Locale:** `en-GB`  
**Compliance Standard:** [Google Play Developer Policy – Metadata and Store Listing](https://support.google.com/googleplay/android-developer/answer/9866151)

---

## 1. Content Catalog & Feature Mapping

| Slide | Category Kicker | Headline | Supporting Benefit Copy | Source Screen | Verified Feature Represented | Prohibited Claims Check |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: |
| **01** | `01 · DAILY DASHBOARD` | **Your medication day, at a glance.** | "See what's next, what you've taken and what's still ahead." | `DashboardScreen.kt` | Hero next-dose card, scheduled time countdown, one-tap Take/Snooze/Skip pill actions, 80% clinical adherence benchmark meter. | **COMPLIANT** (No guarantee of adherence) |
| **02** | `02 · MEDICATION ORGANISATION` | **Every medication, clearly organised.** | "Keep medication details, schedules and supplies together." | `MedicationListScreen.kt` | Search bar, Active/Archived segmented tabs, medication cards with strength, form, schedule chips, and "+ Stock" shortcuts. | **COMPLIANT** (Truthful representation) |
| **03** | `03 · GUIDED ROUTINE SETUP` | **Create your routine, step by step.** | "A guided way to add medication, schedules and stock." | `AddMedicationDialog.kt` | 3-step setup wizard (1. Identity, 2. Schedule, 3. Stock), form selector, intake frequency, and recording preference mode. | **COMPLIANT** (Exact 3-step wizard) |
| **04** | `04 · STOCK & FEFO LOTS` | **Know what's in your supply.** | "Keep quantities, medication lots and expiry dates in view." | `MedicationDetailScreen.kt` | Remaining supply indicator, batch lot tracking with FEFO (First-Expired, First-Out) badge, expiration dates, and 3-tier mobile stock movements ledger. | **COMPLIANT** (Reflects immutable ledger) |
| **05** | `05 · ADHERENCE CALENDAR` | **See your routine over time.** | "Review daily outcomes and your medication history." | `AdherenceCalendarScreen.kt` | Monthly calendar grid with color-coded day status dots, daily dose breakdown, and 80% clinical target compliance comparison. | **COMPLIANT** (No medical guarantees) |
| **06** | `06 · AUDITABLE DOSE HISTORY` | **A clear record of every dose.** | "See scheduled times, recorded outcomes and history." | `MedicationDoseHistoryScreen.kt` | Chronological date grouping (Today, Yesterday), verified vs assumed outcome badges, scheduled vs actual recorded timestamps, compensating reversals. | **COMPLIANT** (Historical audit only) |
| **07** | `07 · PRESCRIPTION TRACKING` | **Keep prescriptions organised.** | "Track expiry dates, repeat requests and collections." | `PrescriptionDialog.kt` / Detail | Doctor repeat authorization status, remaining repeats count (e.g. 3 of 4 remaining), collection dates, and expiry tracking. | **COMPLIANT** (Does not claim pharmacy ordering) |
| **08** | `08 · PRIVACY & PROTECTION` | **Privacy built into your routine.** | "Offline-first tracking, app lock and encrypted backups." | `SettingsScreen.kt` | 100% offline zero-network boundary statement, AndroidX BiometricPrompt app lock, PBKDF2/AES-256-GCM encrypted local SAF backups. | **COMPLIANT** (Truthful security claims) |

---

## 2. Policy & Marketing Integrity Audit

### Prohibited Content Verification:
- [x] **No Fake Ratings or Reviews:** Zero star ratings, quote testimonials, or user review badges are included in any visual asset.
- [x] **No Ranking or Superlative Claims:** No claims of "#1 Medication Tracker", "Best App", or "Top Rated".
- [x] **No Promotional or Price Messaging:** No discount banners, free trial claims, or price promotion copy.
- [x] **No Medical Efficacy Guarantees:** No claims that Novelo "prevents all missed doses", "cures non-adherence", or provides certified medical diagnosis.
- [x] **No Google Play Branding:** Zero Google Play badges, Android robot logos, or Play Store UI chrome in the marketing artwork.
- [x] **Truthful Feature Representation:** Every displayed UI element corresponds directly to verified, shipping Jetpack Compose code and Room database entities.

---

## 3. Google Play Feature Graphic Specifications

- **Dimensions:** 1024 × 500 px
- **Visual Composition:** Minimal clinical blue (`#005EB8`) to deep navy (`#0C1015`) gradient background with official Novelo 3D brand logo (220×220) and central focal point.
- **Copy:** "Novelo" headline with official product tagline "Medication, made manageable." and security pill badge `● 100% OFFLINE · NO NETWORK TRACKING · BIOMETRIC SECURE`.
- **Safe Zone:** All essential branding and copy remain centered within the inner 80% safe area ($102\text{px}$ side margins, $50\text{px}$ top/bottom margins).
