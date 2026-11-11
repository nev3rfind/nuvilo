# Nuvilo Official Website & Privacy Policy Lovable Handoff

**Product:** Nuvilo — *"Medication, made manageable."*  
**Repository:** `https://github.com/nev3rfind/nuvilo`  
**Application ID:** `com.nuvilomed.app` (internal Kotlin namespace: `com.example.nuvilo`)  
**Package Folder:** `website-handoff/`  
**Handoff Date:** 2026-11-11  

---

## 1. Executive Summary & Package Contents

This folder contains a complete, verified, implementation-ready package for building the official **Nuvilo** website on **Lovable** (or any modern web builder). 

The platform is designed around **privacy transparency, clinical authority, and brand trust**, centered on the verified **Privacy Policy** (`/privacy-policy`), an informative **About Nuvilo** page (`/about`), and a dedicated **Support & Contact** page (`/contact`).

### Deliverables Checklist in `website-handoff/`:

1. **[LOVABLE_MASTER_PROMPT.md](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/website-handoff/LOVABLE_MASTER_PROMPT.md)** — **The Primary Deliverable**. A comprehensive, self-contained, copy-paste prompt for Lovable containing all product facts, verified privacy text, CSS custom properties, Tailwind tokens, 18-section policy specifications, page layouts, and asset placement rules.
2. **[PRODUCT_FUNCTIONALITY_AUDIT.md](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/website-handoff/PRODUCT_FUNCTIONALITY_AUDIT.md)** — Exhaustive audit of all 22 genuine implemented features with storage locations, permissions, local processing guarantees, and source code links.
3. **[PRIVACY_DATA_AUDIT.md](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/website-handoff/PRIVACY_DATA_AUDIT.md)** — Deep technical security audit verifying the offline-first architecture (`android.permission.INTERNET` absent), zero-network invariants, PBKDF2 + AES-256-GCM cryptography, Room SQLite storage, DataStore preferences, and data deletion cascades.
4. **[BRAND_DESIGN_SYSTEM.md](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/website-handoff/BRAND_DESIGN_SYSTEM.md)** — Complete visual design system including NHS Clinical Blue (`#005EB8`), interaction blue (`#0284C7`), cyan accent (`#0891B2`), slate neutrals, typography scale with tabular numbers (`tnum`), tactile radii, and CSS variables.
5. **[WEBSITE_STRUCTURE.md](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/website-handoff/WEBSITE_STRUCTURE.md)** — Page-by-page layout architecture, sticky navigation, accessible footer, and responsive breakdown across mobile, tablet, and desktop.
6. **[LOVABLE_ASSET_MANIFEST.md](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/website-handoff/LOVABLE_ASSET_MANIFEST.md)** — Visual asset manifest detailing filenames, resolutions, formats, recommended page locations, and accessibility `alt` descriptions.
7. **`assets/` Directory** — 12 verified high-resolution production assets:
   - `assets/logo/nuvilo-logo-transparent.png` (512 × 512, transparent background)
   - `assets/logo/nuvilo-icon.png` (512 × 512, app icon)
   - `assets/logo/nuvilo-logo.png` (512 × 512, monogram mark)
   - 8 native Android application captures (`dashboard.png`, `medications.png`, `setup.png`, `stock.png`, `calendar.png`, `history.png`, `prescriptions.png`, `privacy.png`, each 1280 × 2856)
   - `assets/backgrounds/nuvilo-feature-graphic.png` (1024 × 500 feature graphic)

---

## 2. Quick-Start Guide for Lovable

Follow these simple steps to build the website with Lovable:

1. **Open Lovable** (or start a new Lovable web project).
2. **Upload Assets**:
   - Drag and drop the contents of `website-handoff/assets/` into Lovable's asset manager (or into the `public/assets/` directory of the project).
3. **Paste Master Prompt**:
   - Open **`website-handoff/LOVABLE_MASTER_PROMPT.md`**.
   - Copy the entire markdown content.
   - Paste it into the Lovable project prompt window.
4. **Review & Iterate**:
   - Lovable will generate the Vite/React application with Tailwind CSS, light/dark themes, the sticky header, the 18-section Privacy Policy, the About showcase, and the Contact page.
   - Verify that the transparent logo appears crisply in the navigation bar and that the genuine application captures are embedded inside the phone mockup cards.

---

## 3. Outstanding Decisions & User Placeholders

The generated website and Privacy Policy intentionally use prominent, standardized placeholders for legal and business identifiers that must be provided by the project owner:

| Variable Placeholder | Description | Example / Recommended Value |
| :--- | :--- | :--- |
| `[LEGAL ENTITY NAME]` | The registered company or individual operating Nuvilo | e.g. `Nuvilo Health Ltd.` or `[Your Legal Name]` |
| `[CONTACT EMAIL]` | Official user support and privacy inquiries mailbox | e.g. `support@nuvilomed.com` or `privacy@nuvilomed.com` |
| `[EFFECTIVE DATE]` | The formal legal publication date of the privacy policy | e.g. `November 11, 2026` |
| `[COUNTRY / JURISDICTION]` | The governing legal jurisdiction for data protection | e.g. `United Kingdom` or `European Union / Ireland` |
| `[INTENDED DOMAIN]` | Official website custom domain | e.g. `nuvilomed.com` or `nuvilo.app` |
| `[REGISTERED ADDRESS]` | Physical corporate address (if required by local company law) | e.g. `London, United Kingdom` or omit if sole developer |

---

## 4. Key Verification Findings Summary

### Offline-First Architecture
- **Verified Source Manifest**: `android.permission.INTERNET` is not declared.
- **Verified Merged Release Manifest**: `android.permission.INTERNET` and `ACCESS_NETWORK_STATE` are strictly absent. Socket creation is blocked by the Android Linux kernel.
- **Verified Dependencies**: Zero network clients, zero analytics, zero ad SDKs, zero crash trackers.

### Storage & Cryptography
- **Database**: Room SQLite at `/data/data/com.nuvilomed.app/databases/novelo.db`.
- **Encrypted Export**: `.nuvilo.json` using PBKDF2-HMAC-SHA256 (100,000 iterations) + AES-256-GCM (12-byte nonce, 128-bit tag). Zero-knowledge memory clearing.
- **Plaintext Export**: `.zip` bundle with 7 RFC 4180 CSV tables. Explicit plaintext warning displayed before writing.
- **Biometrics**: AndroidX `BiometricPrompt` operates via hardware TEE/StrongBox. Zero biometric templates captured or stored.
- **Cloud Backup Exclusion**: `android:allowBackup="false"` and XML rules exclude SQLite database and DataStore preferences from Google Drive backups.
