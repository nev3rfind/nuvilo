# Nuvilo Official Website Structure & Navigation Architecture

**Primary Mission:** Build an official, premium, trust-centered website for Nuvilo focused on privacy transparency, offline-first philosophy, and comprehensive product information.  
**Target Routing Framework:** React Router / Next.js / Static SPA supported by Lovable.  
**Primary Page:** `/privacy-policy`  
**Secondary Pages:** `/about`, `/contact`  

---

## 1. Global Navigation & Layout Architecture

### 1.1 Sticky Header (`<Header />`)
- **Visual Style**: Clean, accessible navigation bar with backdrop blur (`backdrop-blur-md bg-white/90 dark:bg-slate-900/90 border-b border-slate-200 dark:border-slate-800`).
- **Left**: Nuvilo brand lockup:
  - Logo icon (`assets/logo/nuvilo-logo-transparent.png`, 36 × 36px).
  - Wordmark: **Nuvilo** in bold sans-serif with clinical blue dot.
  - Subtitle on desktop: *"Medication, made manageable."* (hidden on mobile).
- **Center**: Navigation Links:
  - **Privacy Policy** (`/privacy-policy`) — *Primary landing page*
  - **About Nuvilo** (`/about`)
  - **Support & Contact** (`/contact`)
- **Right**:
  - Theme toggle (Light / Dark mode).
  - Call-to-action button: *"Get Nuvilo for Android"* (links to Google Play Store listing placeholder).

### 1.2 Global Footer (`<Footer />`)
- **Top Row**: Nuvilo logo, tagline, and summary badge: *"100% Offline-First · Zero Cloud Telemetry · On-Device Health Privacy"*.
- **Navigation Columns**:
  - **Product**: About Nuvilo, Core Features, Offline Architecture, Release Notes.
  - **Privacy & Legal**: Privacy Policy, Data Portability, Encryption Standards, Backup Rules.
  - **Support**: Contact Support, Security FAQs, Restore Guidance.
- **Bottom Row**:
  - Copyright: `© 2026 [LEGAL ENTITY NAME]. All rights reserved.`
  - Disclaimer: *"Nuvilo is a personal medication management tool and does not provide clinical diagnosis or direct medical advice."*
  - Jurisdiction marker: `Registered in [COUNTRY / JURISDICTION]`.

---

## 2. Page 1: Privacy Policy (`/privacy-policy`) — Primary Page

The Privacy Policy is designed to be **scannable, authoritative, and readable**. It avoids walls of fine print by pairing an interactive quick-summary card index with the full, numbered legal text.

### 2.1 Hero & Transparency Summary Box
- **Badge**: `OFFICIAL PRIVACY POLICY · VERIFIED OFFLINE ARCHITECTURE`
- **Headline**: *"Your Health Records Belong to You. Strictly on Your Device."*
- **Effective Date**: `Last Updated: [EFFECTIVE DATE]`
- **Quick-Summary Grid (4 High-Trust Cards)**:
  1. **Zero Cloud Transmission**: Nuvilo contains no network permissions (`android.permission.INTERNET` is absent). Your medication records never leave your phone.
  2. **No Tracking or Third Parties**: Zero analytics SDKs, zero advertising networks, zero third-party telemetry, and zero remote databases.
  3. **Biometric Privacy**: Fingerprint and face authentication are processed entirely by Android's hardware keystore. Nuvilo never touches biometric data.
  4. **Military-Grade Backups**: Offline backups are encrypted on-device with AES-256-GCM and PBKDF2 using your private passphrase.

### 2.2 Table of Contents (Sticky on Desktop, Accordion on Mobile)
Quick jump navigation to all 18 numbered policy sections:
1. Introduction & Core Philosophy
2. Scope & Technical Application ID
3. Complete Personal Information Inventory
4. Purely Local Data Processing
5. Sensitive Health Records & Medical Invariants
6. Notification & Lock-Screen Privacy Masking
7. Biometric Authentication vs Data Collection
8. Local Encrypted Backups (AES-256-GCM)
9. Plaintext CSV Data Portability
10. Android Storage Access Framework (SAF)
11. Absolute Prohibition of Third-Party Data Sharing
12. Android OS Cloud Backup Exclusions
13. Data Retention & Permanent Deletion Controls
14. Device-Level Security & Physical Protection
15. Children's Privacy Protection
16. Website Visitor Data vs Mobile Application
17. Changes to this Policy
18. Legal Controller, Jurisdiction & Contact

### 2.3 Detailed Section Specifications

#### Section 1: Introduction & Core Philosophy
Explains Nuvilo's founding principle: health data privacy through architectural impossibility of surveillance. Because the app has no internet access, data breaches, remote leaks, and unauthorized profiling are prevented at the Linux kernel level.

#### Section 2: Scope & Technical Identifiers
Identifies the exact application covered:
- **Application Display Name**: Nuvilo
- **Google Play Application ID**: `com.nuvilomed.app`
- **Internal Namespace**: `com.example.nuvilo`
- **Platforms**: Android 7.0+ (API level 24 through 37).

#### Section 3: Information Stored by Nuvilo
Explicitly inventories the data fields stored locally:
- Medication profiles (names, forms, dosages, clinical notes, color tags).
- Schedule definitions (times, days of week, target intervals).
- Dose events (occurrence IDs, taken/skipped/missed timestamps, correction notes).
- Stock ledger entries & lot records (FEFO batch numbers, expiry dates, balance adjustments).
- Prescription records (reference numbers, repeats authorized/remaining, pharmacy details).
- User preferences (theme, privacy mode toggle, lock grace periods).

#### Section 4: Purely Local Data Processing
Discloses that all adherence scoring, stock deduction, and notification scheduling calculations are computed by native Kotlin code executing locally in device memory.

#### Section 5: Sensitive Health Records
Categorizes medication data as special-category health data under GDPR (Article 9) and international standards. Affirms that Nuvilo acts as an offline personal tool where the user remains the sole possessor of their records.

#### Section 6: Notifications & Lock-Screen Privacy
Explains how reminder notifications operate locally via Android `NotificationManager`. Discloses Notification Privacy Mode, which replaces medication names and doses with generic reminders on public lock screens.

#### Section 7: Biometric Authentication vs Data Collection
Verifies that Nuvilo uses AndroidX `BiometricPrompt` and **does not capture, store, or transmit fingerprints, facial scans, or PINs**. The OS handles verification in hardware TEE/StrongBox and returns only a pass/fail result.

#### Section 8: Local Encrypted Backups (`.nuvilo.json`)
Details the cryptographic parameters verified in `BackupCrypto.kt`:
- PBKDF2-HMAC-SHA256 with 100,000 iterations.
- 16-byte cryptographically secure random salt.
- AES-256-GCM encryption with 12-byte random nonce and 128-bit authentication tag.
- In-memory zeroization of passphrase characters and derived keys.
- No backdoors: forgotten passphrases cannot be recovered.

#### Section 9: Plaintext CSV Data Portability
Discloses the CSV ZIP export option for spreadsheets. Explicitly warns users that CSV exports are unencrypted plain text files and should be stored securely.

#### Section 10: Storage Access Framework (SAF)
Explains that file saving is user-initiated. If the user saves a backup to Google Drive or Dropbox via the Android document picker, that transfer is governed by the user's agreement with that cloud provider.

#### Section 11: Zero Third-Party Sharing
Affirms that Nuvilo does not sell, lease, disclose, or transmit any health or usage data to any third party, advertising network, analytics vendor, or insurance provider.

#### Section 12: Android Cloud Backup Exclusions
Discloses that `android:allowBackup="false"` and Android XML extraction rules actively exclude Nuvilo's SQLite database and DataStore preferences from automated Google Drive cloud backups.

#### Section 13: Data Retention & Permanent Deletion
- Deleting a medication triggers `CASCADE` deletion of all associated schedules, doses, stock entries, and prescriptions.
- Uninstalling the app permanently purges all local app data from the device.
- Users retain 100% control over exported files.

#### Section 14: Device-Level Security & Physical Protection
Recommends keeping devices updated with system screen locks. Discloses `FLAG_SECURE` window protection when App Lock is active.

#### Section 15: Children's Privacy
Nuvilo does not knowingly collect information from children or anyone else remotely, as all operations remain local to the device.

#### Section 16: Website Visitor Data vs Mobile Application
**Crucial Distinction**: Clearly differentiates between:
- The **Android Application** (100% offline, zero network access).
- The **Marketing Website** (hosted on standard web infrastructure like Cloudflare/Vercel, which processes standard HTTP server access logs such as IP address and user-agent string solely for web delivery, security, and DDoS protection).

#### Section 17: Changes to this Privacy Policy
Explains that updates will be posted to this URL with an updated `[EFFECTIVE DATE]`.

#### Section 18: Legal Controller & Contact
Contains verified placeholders for the application publisher:
- **Legal Entity Name**: `[LEGAL ENTITY NAME]`
- **Official Contact Email**: `[CONTACT EMAIL]`
- **Jurisdiction**: `[COUNTRY / JURISDICTION]`
- **Physical Address**: `[REGISTERED ADDRESS OR PLACEHOLDER]`

---

## 3. Page 2: About Nuvilo (`/about`)

The About page communicates Nuvilo's product philosophy, craftsmanship, and feature depth.

### 3.1 Hero Section
- **Headline**: *"Medication, made manageable."*
- **Subheadline**: *"A clinical-grade, offline-first personal medication and prescription tracking application for Android. Designed for calm, privacy, and absolute reliability."*
- **Primary CTA**: *"View Privacy Policy"* & *"Get for Android"* (Google Play badge).
- **Hero Image**: Device frame embedding `assets/screenshots/dashboard.png` alongside floating metric badges (100% adherence, zero network badges).

### 3.2 Product Differentiators (3 Pillars)
1. **Offline-First by Design**: Built from day one without network dependencies. Your health routine works anywhere—in hospitals, underground transit, flights, or rural areas.
2. **Double-Entry Stock Ledger & FEFO**: Clinical inventory accounting prevents unexpected stock-outs. Tracks batch numbers and prioritizes consuming earliest-expiring medications first.
3. **Uncompromising Privacy**: Native biometric app lock, lock-screen notification masking, and military-grade AES-256-GCM encrypted backups.

### 3.3 Genuine Feature Walkthrough (Embedded Captures)
A structured showcase using the verified application screenshots:
1. **Daily Timeline & Adherence**: Embedding `assets/screenshots/dashboard.png` and `assets/screenshots/calendar.png`.
2. **Clinical Medication Registry & Wizard**: Embedding `assets/screenshots/medications.png` and `assets/screenshots/setup.png`.
3. **FEFO Inventory & Stock Management**: Embedding `assets/screenshots/stock.png`.
4. **Prescription Lifecycle & Repeat Tracking**: Embedding `assets/screenshots/prescriptions.png`.
5. **Dose Audit Trail & Ledger Reconciliation**: Embedding `assets/screenshots/history.png`.
6. **Hardware App Lock & Encrypted Backup**: Embedding `assets/screenshots/privacy.png`.

### 3.4 Architecture & Open Technical Standards
- Native Android with Jetpack Compose & Material 3.
- SQLite Database via Room (ACID compliant).
- AES-256-GCM / PBKDF2-HMAC-SHA256 encryption.
- Zero tracking SDKs or ad libraries.

---

## 4. Page 3: Support & Contact (`/contact`)

### 4.1 Contact Form & Direct Support
- Clear, privacy-respecting contact card.
- **Support Email**: `[CONTACT EMAIL]` (e.g. `support@nuvilomed.com`).
- Response expectation notice: *"We aim to reply within 1–2 business days."*

### 4.2 Essential Technical FAQs
- **"Does Nuvilo need an internet connection?"**  
  *No. Nuvilo works 100% offline. It does not even request the internet permission.*
- **"How do I transfer my data to a new phone?"**  
  *Open Settings > Encrypted JSON Backup. Choose a passphrase and save your `.nuvilo.json` file. On your new device, open Nuvilo and tap Restore.*
- **"What happens if I forget my backup passphrase?"**  
  *Because Nuvilo uses zero-knowledge encryption, no one—not even the developers—can recover an encrypted backup without the passphrase. Please store your passphrase safely.*
- **"Does Nuvilo share data with my doctor or pharmacy?"**  
  *No. Nuvilo is a private tool for you. You can export a readable CSV ZIP bundle to share with your clinician if you choose.*

---

## 5. Technical Requirements for Lovable Implementation

- **Framework**: React / Vite / Tailwind CSS.
- **UI Components**: shadcn-ui (Button, Card, Accordion, Tooltip, DropdownMenu).
- **SEO & Social Meta**:
  - `<title>`: `Nuvilo: Medication Reminder — Official Privacy Policy & Offline Health Tracking`
  - Meta description: `Discover Nuvilo's verified offline-first medication reminder and prescription tracking application. Zero network access, zero tracking, and on-device privacy.`
  - Canonical URL: `https://[INTENDED DOMAIN]/privacy-policy`
  - Open Graph tags with `assets/backgrounds/nuvilo-feature-graphic.png`.
- **Responsive Layout**: Full support for mobile (<640px), tablet (640–1024px), and desktop (>1024px).
- **Accessibility**: 100% WCAG AA compliant. Minimum 4.5:1 text contrast, visible focus rings, landmark navigation (`<header>`, `<main>`, `<nav>`, `<footer>`, `<article>`).
