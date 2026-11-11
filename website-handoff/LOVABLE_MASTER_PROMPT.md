# LOVABLE MASTER BUILD PROMPT: NUVILO OFFICIAL WEBSITE & VERIFIED PRIVACY POLICY

> **INSTRUCTIONS FOR LOVABLE:**  
> Copy and paste the entire prompt below into Lovable. This prompt contains all product context, verified privacy audits, exact design system tokens, CSS variables, complete page-by-page specifications, and asset placement rules required to construct the official, premium website for **Nuvilo** without requiring access to the Android repository.

---

```markdown
# BUILD THE OFFICIAL NUVILO WEBSITE & VERIFIED PRIVACY POLICY PLATFORM

## 1. PROJECT IDENTITY & PRODUCT CONTEXT

You are building the official, premium web platform for **Nuvilo**, a clinical-grade, offline-first personal medication management and prescription tracking application for native Android.

### Key Brand Facts:
- **Application Name**: Nuvilo
- **Google Play Title**: Nuvilo: Medication Reminder
- **Tagline**: *"Medication, made manageable."*
- **Application ID (Package)**: `com.nuvilomed.app`
- **Core Philosophy**: Zero-Network Architecture, Patient Privacy Sovereignty, Clinical Precision.
- **Key Differentiators**:
  - **100% Offline-First**: Operates completely without internet (`android.permission.INTERNET` is strictly absent). Health data never touches cloud servers.
  - **Double-Entry Stock Ledger & FEFO**: Prevents stock-outs via First-Expired, First-Out lot tracking with 64-bit integer precision (zero floating-point errors).
  - **Adherence Calendar & Audit Trail**: Deterministic dose occurrence tracking with historical corrections and adherence scoring.
  - **Prescription Repeat Management**: Tracks authorized vs remaining repeats, GP repeat requests, and pharmacy collections.
  - **Military-Grade Security**: Hardware-isolated Biometric App Lock (`BiometricPrompt`), lock-screen notification masking, and AES-256-GCM + PBKDF2 local encrypted backups.
  - **Zero Telemetry**: Zero tracking libraries, zero analytics SDKs, zero advertising networks, zero remote databases.

---

## 2. EXACT BRAND DESIGN SYSTEM & TOKENS

The website must look unmistakably like Nuvilo—authoritative, calming, clinical, and sophisticated. It must avoid generic healthcare tropes (no stock photos of smiling doctors in stethoscopes) and strictly mirror the native Android Material 3 design system.

### 2.1 CSS Custom Properties (Add to root stylesheet)
```css
:root {
  /* Brand Anchor: NHS Clinical Blue */
  --nuvilo-primary: #005EB8;
  --nuvilo-primary-dark: #003087;
  --nuvilo-primary-bright: #0072CE;
  --nuvilo-on-primary: #FFFFFF;

  /* Interactive Elements & Containers */
  --nuvilo-interaction: #0284C7;
  --nuvilo-interaction-container: #E0F2FE;
  --nuvilo-on-interaction-container: #002B5B;
  --nuvilo-cyan: #0891B2;
  --nuvilo-cyan-container: #DDF5F4;
  --nuvilo-on-cyan-container: #003735;

  /* Clinical Semantic Statuses */
  --nuvilo-status-taken: #0D6832;
  --nuvilo-status-taken-bg: #E6F4EA;
  --nuvilo-status-taken-border: #A7F3D0;
  --nuvilo-status-assumed: #005A8D;
  --nuvilo-status-assumed-bg: #E0F2FE;
  --nuvilo-status-assumed-border: #BAE6FD;
  --nuvilo-status-skipped: #475569;
  --nuvilo-status-skipped-bg: #F1F5F9;
  --nuvilo-status-skipped-border: #CBD5E1;
  --nuvilo-status-missed: #B91C1C;
  --nuvilo-status-missed-bg: #FEE2E2;
  --nuvilo-status-missed-border: #FECACA;
  --nuvilo-status-warning: #92400E;
  --nuvilo-status-warning-bg: #FEF3C7;
  --nuvilo-status-warning-border: #FDE68A;

  /* Surfaces & Neutrals (Light Mode) */
  --nuvilo-bg: #F8FAFC;
  --nuvilo-surface: #FFFFFF;
  --nuvilo-surface-variant: #F1F5F9;
  --nuvilo-text-primary: #0F172A;
  --nuvilo-text-secondary: #475569;
  --nuvilo-text-tertiary: #64748B;
  --nuvilo-outline: #CBD5E1;
  --nuvilo-outline-variant: #E2E8F0;

  /* Geometry Scale */
  --radius-card: 18px;
  --radius-sheet: 24px;
  --radius-pill: 9999px;
  --radius-button: 14px;
}

.dark {
  /* Brand Anchor (Dark) */
  --nuvilo-primary: #6EB4FF;
  --nuvilo-primary-dark: #0C4A6E;
  --nuvilo-on-primary: #002F64;

  /* Interactive Elements (Dark) */
  --nuvilo-interaction: #38BDF8;
  --nuvilo-interaction-container: #0C4A6E;
  --nuvilo-on-interaction-container: #E0F2FE;
  --nuvilo-cyan: #22D3EE;
  --nuvilo-cyan-container: #164E63;
  --nuvilo-on-cyan-container: #CFFAFE;

  /* Clinical Statuses (Dark) */
  --nuvilo-status-taken: #6EE7B7;
  --nuvilo-status-taken-bg: #064E3B;
  --nuvilo-status-taken-border: #047857;
  --nuvilo-status-assumed: #7DD3FC;
  --nuvilo-status-assumed-bg: #0C4A6E;
  --nuvilo-status-assumed-border: #0369A1;
  --nuvilo-status-skipped: #CBD5E1;
  --nuvilo-status-skipped-bg: #1E293B;
  --nuvilo-status-skipped-border: #334155;
  --nuvilo-status-missed: #FCA5A5;
  --nuvilo-status-missed-bg: #7F1D1D;
  --nuvilo-status-missed-border: #991B1B;
  --nuvilo-status-warning: #FCD34D;
  --nuvilo-status-warning-bg: #78350F;
  --nuvilo-status-warning-border: #92400E;

  /* Surfaces & Neutrals (Dark Mode) */
  --nuvilo-bg: #0F172A;
  --nuvilo-surface: #1E293B;
  --nuvilo-surface-variant: #334155;
  --nuvilo-text-primary: #F8FAFC;
  --nuvilo-text-secondary: #94A3B8;
  --nuvilo-text-tertiary: #64748B;
  --nuvilo-outline: #475569;
  --nuvilo-outline-variant: #334155;
}
```

### 2.2 Typography Rules
- **Font Stack**: `Inter, system-ui, -apple-system, sans-serif`.
- **Tabular Figures**: Set `font-feature-settings: "tnum" 1` on all numeric data, adherence percentages, times (`08:00`), and lot counts.
- **Scale**:
  - Hero Display: `40px` (desktop: `48px`), weight `800`, line height `1.2`.
  - H1 Page Title: `32px` (desktop: `40px`), weight `700`, line height `1.25`.
  - H2 Section Header: `24px` (desktop: `28px`), weight `700`, line height `1.3`.
  - H3 Subsection: `20px`, weight `600`, line height `1.4`.
  - Body Copy: `16px`, weight `400` / `500`, line height `1.6`.
  - Caption / Badges: `13px`, weight `500` / `600`, line height `1.4`.

### 2.3 Shapes, Borders & Elevation
- **Cards**: `rounded-2xl` (`18px`), `border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-800/90`.
- **Shadows**: Ultra-subtle clinical ambient light: `box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.05)`. Avoid harsh, dark dropshadows.
- **Pills**: `rounded-full` for all status badges and action tags.
- **Buttons**: `rounded-xl` (`14px`) with smooth hover elevation (`translateY(-2px)`).

---

## 3. UPLOADED ASSETS & EXACT VISUAL PLACEMENT

You have been provided with 12 genuine, verified high-resolution Nuvilo assets located in `assets/`:

| Filename | Resolution | Role | Placement & Rules |
| :--- | :--- | :--- | :--- |
| `assets/logo/nuvilo-logo-transparent.png` | 512 × 512 | Primary Brand Mark (Transparent) | **Always use for**: Header top-left, Footer branding, and Hero brand title. Never use a white-bordered logo. |
| `assets/logo/nuvilo-icon.png` | 512 × 512 | Square App Icon | Floating hero badge, favicon, and Open Graph logo. |
| `assets/screenshots/dashboard.png` | 1280 × 2856 | Daily Timeline & Adherence KPI | **About Page Hero**: Place inside a phone frame mockup (`rounded-[36px] border-4 border-slate-700 shadow-2xl`). |
| `assets/screenshots/medications.png` | 1280 × 2856 | Medication Registry & Stock | **About Page**: Feature section for Regimen & Stock Management. |
| `assets/screenshots/setup.png` | 1280 × 2856 | Clinical Add Medication Wizard | **About Page**: Step-by-step medication onboarding walkthrough. |
| `assets/screenshots/stock.png` | 1280 × 2856 | Stock Ledger & FEFO Batches | **About Page & Privacy Policy**: Demonstrating local inventory ledger and lot expiry tracking. |
| `assets/screenshots/calendar.png` | 1280 × 2856 | Monthly Adherence Calendar | **About Page**: Adherence tracking showcase showing monthly status rings. |
| `assets/screenshots/history.png` | 1280 × 2856 | Chronological Dose Audit Log | **About Page & Privacy Policy**: Demonstrating local audit trails and historical dose corrections. |
| `assets/screenshots/prescriptions.png` | 1280 × 2856 | Repeat Prescription Tracking | **About Page**: Prescription management section showing GP repeat allowances. |
| `assets/screenshots/privacy.png` | 1280 × 2856 | Privacy & Security Settings | **Privacy Policy Hero & About Page**: Showcasing App Lock, Notification Masking, and Encrypted Backups. |
| `assets/backgrounds/nuvilo-feature-graphic.png` | 1024 × 500 | Feature Graphic Banner | Social preview banner (`og:image`) and About page header banner. |

---

## 4. WEBSITE STRUCTURE & PAGE SPECIFICATIONS

Build a clean 3-page application using client-side routing:
1. `/privacy-policy` (**Primary Page**)
2. `/about`
3. `/contact`

### 4.1 Global Header (`<Header />`)
- **Sticky Glass Navbar**: `backdrop-blur-md bg-white/85 dark:bg-slate-900/85 border-b border-slate-200 dark:border-slate-800`.
- **Branding**: Displays `assets/logo/nuvilo-logo-transparent.png` (36px high) with wordmark **Nuvilo** and subtle tag: *"Medication, made manageable."*
- **Navigation Links**:
  - `Privacy Policy` (active state indicator: subtle blue bottom border or pill)
  - `About Nuvilo`
  - `Contact & Support`
- **Actions**:
  - Light/Dark mode toggle (Sun/Moon icons).
  - Primary Button: *"Get Nuvilo for Android"* (links to `#download` or Google Play placeholder).

---

### 4.2 Page 1: Privacy Policy (`/privacy-policy`) — PRIMARY FOCUS

This page must be **deeply authoritative, transparent, and readable**. It presents Nuvilo's verified privacy posture based on the verified application audit.

#### A. Hero & Quick-Summary Cards
- **Tag**: `OFFICIAL PRIVACY POLICY · VERIFIED ON-DEVICE ARCHITECTURE`
- **Title**: *"Your Health Records Belong to You. Strictly on Your Device."*
- **Subtitle**: *"Nuvilo was architected under a zero-network philosophy. We cannot sell, see, or share your medical data because it never leaves your phone."*
- **Last Updated Badge**: `Effective: [EFFECTIVE DATE] · Application Version: 1.0.0`
- **4 Summary Pillar Cards (Grid layout)**:
  1. **Zero Cloud Network**: `android.permission.INTERNET` is strictly absent from the application manifest. The Android operating system blocks Nuvilo from making any network connection.
  2. **Zero Third-Party Telemetry**: Contains no Firebase, Google Analytics, advertising SDKs, crash trackers, or third-party cookies.
  3. **Biometric Privacy**: Face and fingerprint verification use Android's hardware `BiometricPrompt`. Nuvilo never captures, stores, or accesses biometric templates.
  4. **Military-Grade Backups**: On-device backups are encrypted using AES-256-GCM and PBKDF2 (100,000 iterations). Passphrases are zeroized in RAM immediately.

#### B. Sticky Table of Contents & Policy Navigation
Provide an interactive sidebar table of contents on desktop (sticky) and a collapsible sheet on mobile linking smoothly to all 18 numbered sections.

#### C. The 18 Numbered Policy Sections (Render with clear typography):

1. **Introduction & Architectural Privacy**: Nuvilo's privacy is enforced by engineering, not just policy. Because the application operates offline without network permissions, data breaches and unauthorized cloud profiling are prevented at the Linux kernel level.
2. **Scope & Application Identity**: Governs Nuvilo for Android (`com.nuvilomed.app`, internal namespace `com.example.nuvilo`).
3. **Data Inventory Stored On-Device**:
   - Medication records (name, strength, form, unit, clinical notes, color tags).
   - Schedules (times, days of week, target intervals).
   - Dose events (occurrence IDs, confirmed/skipped/missed timestamps, correction notes).
   - Inventory & FEFO lots (batch lot numbers, expiry dates, stock balances).
   - Prescriptions (reference numbers, repeats authorized/remaining, pharmacy notes).
   - Application preferences (theme mode, app lock toggle, privacy mode toggle).
4. **Purely Local Processing**: All calculations (adherence percentages, FEFO stock deductions, exact reminder alarms) are executed by Kotlin code directly on the user's CPU and stored in local SQLite (`/data/data/com.nuvilomed.app/databases/novelo.db`).
5. **Special-Category Health Data**: Medication routines represent sensitive personal health data under GDPR (Article 9) and international regulations. Nuvilo acts as an offline personal tool where the user is the sole custodian.
6. **Alarms & Notification Privacy Mode**: Reminders are delivered locally via Android `AlarmManager`. When Notification Privacy Mode is enabled, notification titles and bodies are masked on public lock screens (e.g. *"Medication Reminder"*, concealing drug names and dosages).
7. **Biometric Authentication vs Data Collection**: Nuvilo uses Android's native `BiometricPrompt` for App Lock. Biometric templates remain isolated in the device's hardware TEE/StrongBox. Nuvilo receives only a boolean pass/fail signal.
8. **Military-Grade Encrypted Backups (`.nuvilo.json`)**: Users can export full backups encrypted with AES-256-GCM and PBKDF2 (100,000 iterations, 16-byte random salt). Passphrase characters and key bytes are wiped from memory immediately after encryption. Forgotten passphrases cannot be recovered.
9. **Plaintext CSV Data Portability (`.zip`)**: For spreadsheets or clinician visits, users can export an unencrypted ZIP bundle containing 7 CSV tables. The app displays an explicit warning before saving.
10. **Storage Access Framework (SAF)**: Export and restore operations use Android's system document picker. If a user saves a file to Google Drive or Dropbox, that transfer is handled by the user's selected cloud document provider, not Nuvilo.
11. **Zero Third-Party Sharing**: Nuvilo does not disclose, sell, license, or transmit any data to advertisers, analytics providers, insurers, or data brokers.
12. **Android Cloud Backup Exclusions**: Nuvilo explicitly sets `android:allowBackup="false"` and includes XML rules excluding its database and DataStore preferences from automated Google Drive cloud backups.
13. **Data Retention & Permanent Deletion**:
    - Deleting a medication executes a `CASCADE` delete, instantly removing all associated schedules, doses, stock lots, and prescriptions.
    - Uninstalling the app permanently purges the local database from the device.
14. **Device Security**: Recommends keeping devices updated. When App Lock is active, `FLAG_SECURE` blocks screen capture and Recent Apps thumbnails.
15. **Children's Privacy**: Nuvilo does not collect data from children or anyone else remotely.
16. **Website Visitor Data vs Mobile App**: Clearly discloses that while the Android app is 100% offline, visiting this marketing website involves standard server access logs (IP address, browser user-agent) collected by the website hosting infrastructure (e.g. Cloudflare/Vercel) solely for site delivery and security.
17. **Changes to this Policy**: Updates will be posted to this page with an updated Effective Date.
18. **Legal Controller & Contact**:
    - Publisher: `[LEGAL ENTITY NAME]`
    - Official Contact Email: `[CONTACT EMAIL]`
    - Jurisdiction: `[COUNTRY / JURISDICTION]`
    - Address: `[REGISTERED ADDRESS OR PLACEHOLDER]`

---

### 4.3 Page 2: About Nuvilo (`/about`)

A product presentation page demonstrating Nuvilo's clinical depth and genuine craftsmanship.

#### A. Hero Section
- **Tagline**: *"Medication, made manageable."*
- **Headline**: *"The Clinical-Grade, Offline-First Medication Companion for Android"*
- **Prose**: *"Nuvilo is built for people who want complete control over their medication routines, accurate stock tracking, and uncompromising privacy. No cloud accounts, no subscription lock-in, and zero health tracking surveillance."*
- **Hero Image**: Device frame showing `assets/screenshots/dashboard.png` with floating badges: `100% Adherence Score`, `Zero Network Required`, `FEFO Stock Accounting`.

#### B. The 3 Core Pillars (Cards)
1. **Architectural Offline-First**: Built from scratch without network dependencies. Operates anywhere—remote travel, flights, underground transit, and hospital rooms.
2. **Double-Entry Stock Ledger & FEFO**: Fixed-point scaled integer math guarantees zero rounding errors. Automatically prioritizes consuming earliest-expiring lots first.
3. **Physical & Digital Confidentiality**: Biometric app lock, notification privacy masking, and user-held cryptographic backups.

#### C. Verified Feature Walkthrough (Embedding Real Captures)
Create dedicated feature blocks displaying the genuine application screenshots:
1. **Today's Timeline & One-Tap Dosing**: Display `assets/screenshots/dashboard.png`. Explain morning/afternoon/evening dose grouping and instant Taken/Skip/Snooze controls.
2. **Complete Regimen Registry**: Display `assets/screenshots/medications.png` and `assets/screenshots/setup.png`. Highlight multi-form support (tablets, drops, inhalers, injections) and custom color tags.
3. **Smart Inventory & Expiry Management**: Display `assets/screenshots/stock.png`. Explain FEFO lot tracking, remaining days of supply, and low-stock alerts.
4. **Adherence Calendar & Longitudinal Trends**: Display `assets/screenshots/calendar.png`. Highlight monthly consistency rings and objective percentage metrics.
5. **Prescription & Repeat Tracking**: Display `assets/screenshots/prescriptions.png`. Showcase repeat allowances, GP request tracking, and pharmacy collection logs.
6. **Audit History & Dose Corrections**: Display `assets/screenshots/history.png`. Demonstrate chronological event logs and ledger-reconciling dose corrections.
7. **Security & Cryptographic Backups**: Display `assets/screenshots/privacy.png`. Showcase biometric lock settings and AES-256-GCM export controls.

#### D. Technical Architecture Spotlight
- **UI**: 100% Jetpack Compose with Material 3.
- **Database**: Room SQLite with Foreign Key Cascade integrity.
- **Background Tasks**: AndroidX WorkManager (low-stock checks) & exact AlarmManager.
- **Cryptography**: `PBKDF2WithHmacSHA256` + `AES-256-GCM`.

---

### 4.4 Page 3: Contact & Support (`/contact`)

#### A. Contact Card
- **Heading**: *"Contact Nuvilo Support"*
- **Email Contact**: `[CONTACT EMAIL]` (with one-click `mailto:` button).
- **Notice**: *"Because Nuvilo operates completely offline on your device, our support team cannot view, inspect, or modify your personal health records. When asking questions, please specify your Android OS version and device model."*

#### B. Interactive Support FAQs (Accordion)
- **"Does Nuvilo require an internet connection or account?"**  
  *No. Nuvilo requires zero registration, zero accounts, and zero internet connectivity.*
- **"How do I transfer my data to a new phone?"**  
  *In Nuvilo, navigate to Settings > Encrypted JSON Backup. Choose a passphrase and save your `.nuvilo.json` file. On your new device, open Nuvilo and tap Restore.*
- **"What should I do if I forget my backup passphrase?"**  
  *Because Nuvilo utilizes zero-knowledge cryptography, passphrases are never stored on any server. If a passphrase is lost, the encrypted file cannot be decrypted by anyone.*
- **"Can my doctor or pharmacy see my medication records?"**  
  *Only if you choose to export an unencrypted CSV ZIP bundle and hand it to them directly. Nuvilo never communicates with healthcare networks automatically.*

---

### 4.5 Global Footer (`<Footer />`)
- **Branding**: Transparent logo `assets/logo/nuvilo-logo-transparent.png`, title **Nuvilo**, and tagline.
- **Trust Badge**: *"100% Offline-First · Zero Cloud Telemetry · On-Device Privacy"*
- **Legal Row**:
  - `© 2026 [LEGAL ENTITY NAME]. All rights reserved.`
  - `Registered in [COUNTRY / JURISDICTION]`
  - Links: Privacy Policy · About Nuvilo · Contact Support
  - Medical Disclaimer: *"Nuvilo is an offline personal medication tracker and does not provide clinical diagnosis, medical prescriptions, or emergency response services."*

---

## 5. TECHNICAL IMPLEMENTATION & ACCESSIBILITY

- **Accessibility (WCAG AA)**:
  - Contrast: Minimum 4.5:1 for body copy; 3:1 for large headers.
  - Keyboard Focus: Explicit `:focus-visible` rings using `--nuvilo-interaction` (`#0284C7`).
  - Semantic HTML: Use `<header>`, `<nav>`, `<main>`, `<article>`, `<section>`, `<footer>`.
- **Responsive Layout**:
  - Mobile (<640px): Drawer/accordion navigation, full-width cards, vertical screenshot showcases.
  - Tablet (640–1024px): 2-column feature grids, floating screenshot cards.
  - Desktop (>1024px): 3-column grids, sticky privacy policy table of contents, high-fidelity phone mockups.
- **SEO & Metadata**:
  - Title: `Nuvilo: Medication Reminder — Official Privacy Policy & Offline Health Tracking`
  - Meta Description: `Explore Nuvilo's verified offline-first medication management app. Zero network access, zero tracking, and on-device health privacy.`
  - Canonical URL: `https://[INTENDED DOMAIN]/privacy-policy`
  - Open Graph: `og:title`, `og:description`, `og:image` pointing to `assets/backgrounds/nuvilo-feature-graphic.png`.

---

## 6. ACCEPTANCE CRITERIA

A completed website build must:
1. Load cleanly in light and dark mode with exact Nuvilo color tokens (`#005EB8`, `#0284C7`, `#0891B2`, `#F8FAFC`, `#0F172A`).
2. Feature the transparent Nuvilo logo (`nuvilo-logo-transparent.png`) in the header and footer with zero visual artifacting.
3. Present the complete, verified 18-section Privacy Policy with clear placeholders (`[LEGAL ENTITY NAME]`, `[CONTACT EMAIL]`, `[EFFECTIVE DATE]`, `[COUNTRY / JURISDICTION]`).
4. Embed the genuine application screenshots (`dashboard.png`, `medications.png`, `stock.png`, etc.) inside elegant phone mockup containers on the About and Privacy pages.
5. Provide a working theme toggle, responsive navigation, and active-link indicators across `/privacy-policy`, `/about`, and `/contact`.
6. Make zero claims of cloud accounts, remote servers, or features not present in the native Android app.
```
