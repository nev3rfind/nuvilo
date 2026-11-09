# Google Play Store Screenshot Campaign – Phone & Tablet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a professional Google Play Store screenshot collection (8 Phone, 8 Tablet, 1 Feature Graphic) showcasing Novelo's genuine healthcare features with an editable Next.js screenshot editor project, automated PIL export validation, and comprehensive Play Store marketing documentation.

**Architecture:** Maintain complete separation between the Android application module and the Next.js screenshot editor by placing the editor in `store-assets/google-play/editor/`. Capture authentic application screens from Android emulators with realistic synthetic data, compose them using the "Premium Clinical Blue + Editorial Bento" design direction in the screenshot editor with `app-store-screenshots.json`, render opaque 24-bit PNGs matching exact Google Play dimensions, and validate all assets using an automated Python Pillow verification suite.

**Tech Stack:** Next.js 16 (React 19, Tailwind CSS, ShadCN, html-to-image), Android SDK 37 (Jetpack Compose, Material 3, Room, DataStore), Python 3.12 (Pillow 13.0), PowerShell, GitHub CLI (`gh`).

**Spec:** User prompt requirements and guidelines in `docs/ui-ux-audit/stage2/IMPLEMENTATION_REPORT.md`, `design-system/novelo/MASTER.md`, `docs/ui-ux-audit/PRO_MAX_RESEARCH.md`, and `.agents/skills/app-store-screenshots/SKILL.md`.

## Global Constraints

- **Phone Dimensions:** Exactly 8 images, 1080 × 1920 px (9:16 portrait), opaque 24-bit RGB PNG (no alpha channel).
- **Tablet Dimensions:** Exactly 8 images, 1920 × 1080 px (16:9 landscape) or 1080 × 1920 px (9:16 portrait) based on actual responsive evaluation, opaque 24-bit RGB PNG (no alpha channel).
- **Feature Graphic:** Exactly 1 image, 1024 × 500 px, opaque 24-bit RGB PNG (no alpha channel), centered composition, no device frames or rating badges.
- **Brand Palette:** Novelo Clinical Blue (`#005EB8`), Interaction Blue (`#0284C7`), Cyan Accent (`#0891B2`), Dark Neutral (`#121820` / `#0C1015`), and official Novelo 3D logo / icon.
- **Offline / Privacy Safety:** Zero network claims; do not claim remote sync or clinical guarantees.
- **Prohibited Marketing Claims:** No fake ratings, awards, #1 claims, medical efficacy guarantees, or Google Play badges.
- **Directory Isolation:** The Next.js screenshot editor lives in `store-assets/google-play/editor/`; do not add Node dependencies to Android Gradle or commit `node_modules`/`.next`.

## Review Focus

1. **Alpha Channel Presence:** Google Play strictly rejects PNGs containing an alpha (transparency) channel. Ensure every export is converted to 24-bit RGB without RGBA mode.
2. **Dimension Precision:** Google Play rejects phone/tablet screenshots that deviate from accepted aspect ratios or minimum resolutions. Verify exact 1080×1920, 1920×1080, and 1024×500 dimensions with automated PIL pixel inspection.
3. **Responsive Tablet Authenticity:** Never stretch a phone capture to tablet size. Capture genuine tablet UI and assess portrait vs landscape presentation.
4. **Data Coherence Across Captures:** Ensure dates, stock quantities, adherence metrics, and medication regimens in demonstration screenshots are logically consistent and clearly synthetic.
5. **Git & Build Cleanliness:** Ensure `.gitignore` excludes `node_modules`, `.next`, and cache directories from the git repository before pushing to `origin`.

---

### Task 1: GitHub Issue & Marketing Branch Initialization

**Files:**
- Modify: `.gitignore` (add `store-assets/**/node_modules/`, `store-assets/**/.next/`, `store-assets/**/out/`)

- [ ] **Step 1: Check existing GitHub issues and create campaign issue**
  Run `gh issue list` to verify open issues. Create issue titled `Google Play Store Screenshot Campaign – Phone & Tablet`.
  Command: `gh issue create --title "Google Play Store Screenshot Campaign – Phone & Tablet" --body "..."`
  Record returned issue number `<issue-number>`.

- [ ] **Step 2: Create dedicated marketing branch**
  Checkout new branch from `main`:
  Command: `git checkout -b marketing/<issue-number>-google-play-store-assets`

- [ ] **Step 3: Update `.gitignore` to prevent Node/Next.js artifacts from leaking into git**
  Add exclusion rules for `store-assets/**/node_modules/`, `store-assets/**/.next/`, and `store-assets/**/.turbo/`.

- [ ] **Step 4: Commit `.gitignore` update**
  Command: `git add .gitignore && git commit -m "chore(marketing): configure gitignore for store assets editor"`

---

### Task 2: Scaffold Next.js Screenshot Editor in `store-assets/google-play/editor/`

**Files:**
- Create: `store-assets/google-play/editor/package.json`
- Create: `store-assets/google-play/editor/src/...` (from `.agents/skills/app-store-screenshots/template/`)
- Modify: `store-assets/google-play/editor/src/lib/devices.ts` (add Google Play 16:9 tablet preset `1920x1080` & `1080x1920`)
- Create: `store-assets/google-play/editor/public/app-icon.png` (copy official Novelo 3D launcher icon)

- [ ] **Step 1: Copy template into `store-assets/google-play/editor/`**
  Copy all files from `.agents/skills/app-store-screenshots/template/` into `store-assets/google-play/editor/`.

- [ ] **Step 2: Copy official Novelo app icon into editor public directory**
  Copy `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (or high-res 3D brand logo) to `store-assets/google-play/editor/public/app-icon.png`.

- [ ] **Step 3: Update `src/lib/devices.ts` with Google Play compliant 16:9 and 9:16 tablet presets**
  Ensure `android-tablet` or `android-10` includes exact `1920 x 1080` landscape and `1080 x 1920` portrait export targets.

- [ ] **Step 4: Install dependencies in `store-assets/google-play/editor/`**
  Run `npm install` inside `store-assets/google-play/editor/`.
  Verify zero installation errors.

- [ ] **Step 5: Verify editor build**
  Run `npm run build` inside `store-assets/google-play/editor/`.
  Expected: Successful compilation of Next.js app.

---

### Task 3: Capture Authentic Android Application Screens (Phone & Tablet)

**Files:**
- Create: `store-assets/google-play/source-captures/phone/01-dashboard.png`
- Create: `store-assets/google-play/source-captures/phone/02-medications.png`
- Create: `store-assets/google-play/source-captures/phone/03-setup.png`
- Create: `store-assets/google-play/source-captures/phone/04-stock.png`
- Create: `store-assets/google-play/source-captures/phone/05-calendar.png`
- Create: `store-assets/google-play/source-captures/phone/06-history.png`
- Create: `store-assets/google-play/source-captures/phone/07-prescriptions.png`
- Create: `store-assets/google-play/source-captures/phone/08-privacy.png`
- Create: `store-assets/google-play/source-captures/tablet/01-dashboard.png` ... `08-privacy.png`
- Create: `store-assets/google-play/source-captures/CAPTURE_LOG.md`

- [ ] **Step 1: Install and launch Novelo on Android emulator**
  Deploy `app-debug.apk` to `emulator-5554` with `adb install -r`.
  Populate with realistic synthetic test data (consistent dates, active medications, FEFO lots, adherence record).

- [ ] **Step 2: Capture 8 phone screens**
  Capture the 8 key functional surfaces via `adb exec-out screencap -p` or verified Stage 2 high-resolution captures:
  1. Dashboard (Greeting, next dose card, daily timeline, adherence KPI).
  2. Medications List (Search pill, categorized medications, dosage, stock status).
  3. Add Medication Wizard (Step indicator, routine configuration).
  4. Stock Management & FEFO Lots (Collapsible lots with FEFO badges, inventory ledger).
  5. Adherence Calendar (Monthly grid with confirmed/assumed/skipped dots, KPI metrics).
  6. Dose History (Chronological groupings, outcome badges, recorded vs scheduled timestamps).
  7. Prescription Tracking (Status, repeats remaining, renewal/expiry tracking).
  8. Privacy & Security Settings (App lock, Biometric auth, encrypted SAF backup/restore).

- [ ] **Step 3: Capture 8 tablet screens and evaluate responsive behavior**
  Inspect layout on tablet profile (`Medium_Tablet` or tablet window configuration).
  Evaluate whether landscape (`1920x1080`) or portrait (`1080x1920`) best highlights Novelo's real interface.
  Capture the 8 corresponding tablet screens.

- [ ] **Step 4: Document capture environment in `CAPTURE_LOG.md`**
  Record device models, window sizes, densities, API levels, and capture timestamps.

---

### Task 4: Configure Project JSON & Design System in Screenshot Editor

**Files:**
- Create: `store-assets/google-play/editor/app-store-screenshots.json`
- Modify: `store-assets/google-play/editor/src/lib/constants.ts` (register Novelo clinical theme tokens)

- [ ] **Step 1: Define Novelo theme preset in `constants.ts`**
  Register theme `novelo-clinical`:
  - Background primary: `#F8FAFC` (light), `#121820` (dark)
  - Primary anchor: `#005EB8`
  - Interaction blue: `#0284C7`
  - Accent cyan: `#0891B2`
  - Text primary: `#0F172A` / `#F8FAFC`

- [ ] **Step 2: Author `app-store-screenshots.json`**
  Configure:
  - App Name: "Novelo"
  - Locale: "en-GB"
  - Platform: "android"
  - 8 Phone slides (`android` deck) with headlines, supporting copy, layout ("device-bottom", "split-landscape", "hero", "bento-card"), source captures.
  - 8 Tablet slides (`android-tablet` deck) configured with responsive tablet captures.
  - Feature Graphic (`feature-graphic` deck): 1024x500 banner with centered 3D logo, headline "Novelo", tagline "Medication, made manageable.", no device mockup.

- [ ] **Step 3: Verify preview rendering**
  Start Next.js dev server or run headless screenshot export verification.

---

### Task 5: Export Final Assets & Generate Contact Sheets

**Files:**
- Create: `store-assets/google-play/exports/phone/en-GB/01-dashboard.png` ... `08-privacy.png`
- Create: `store-assets/google-play/exports/tablet/en-GB/01-dashboard.png` ... `08-privacy.png`
- Create: `store-assets/google-play/exports/feature-graphic/novelo-feature-graphic.png`
- Create: `store-assets/google-play/exports/preview/phone-contact-sheet.png`
- Create: `store-assets/google-play/exports/preview/tablet-contact-sheet.png`
- Create: `store-assets/google-play/exports/preview/campaign-overview.png`

- [ ] **Step 1: Render and export 8 phone screenshots**
  Export 8 high-resolution 1080 × 1920 images into `store-assets/google-play/exports/phone/en-GB/`.

- [ ] **Step 2: Render and export 8 tablet screenshots**
  Export 8 high-resolution images (1920 × 1080 or 1080 × 1920) into `store-assets/google-play/exports/tablet/en-GB/`.

- [ ] **Step 3: Render and export feature graphic**
  Export 1024 × 500 banner into `store-assets/google-play/exports/feature-graphic/novelo-feature-graphic.png`.

- [ ] **Step 4: Generate contact sheets**
  Using Python/PIL, compose multi-image contact sheets:
  - `phone-contact-sheet.png` (8 phone slides in a 4x2 or 8x1 grid)
  - `tablet-contact-sheet.png` (8 tablet slides in a 4x2 grid)
  - `campaign-overview.png` (Phone + Tablet + Feature Graphic composite)

---

### Task 6: Automated Export Validation & Technical Compliance Audit

**Files:**
- Create: `store-assets/google-play/validate_exports.py`
- Create: `store-assets/google-play/EXPORT_VALIDATION.md`

- [ ] **Step 1: Write automated Python/Pillow validation script `validate_exports.py`**
  Script checks:
  1. Exact dimensions (Phone: 1080x1920, Tablet: 1920x1080 or 1080x1920, Feature Graphic: 1024x500).
  2. Image format (`PNG`).
  3. Image mode (`RGB` — strictly verify absence of alpha channel `RGBA` / `P`).
  4. Bit depth (24-bit RGB).
  5. Aspect ratio (Phone: 9:16, Tablet: 16:9 / 9:16, Feature Graphic: 1024:500).
  6. Non-zero byte size and non-empty image data.

- [ ] **Step 2: Run validation script and output `EXPORT_VALIDATION.md`**
  Execute: `python store-assets/google-play/validate_exports.py`
  Verify all 17 assets pass 100% of checks.

---

### Task 7: Marketing Content, Alt Text & Store Listing Documentation

**Files:**
- Create: `store-assets/google-play/SCREENSHOT_CONTENT.md`
- Create: `store-assets/google-play/ALT_TEXT.md`
- Create: `store-assets/google-play/README.md`

- [ ] **Step 1: Author `SCREENSHOT_CONTENT.md`**
  Document for each slide:
  - Slide # and filename.
  - Headline and supporting copy.
  - Actual feature demonstrated.
  - Source application screen.
  - Verification status and adherence to Google Play policies (no prohibited claims).

- [ ] **Step 2: Author `ALT_TEXT.md`**
  Document accessible screen reader alt-text descriptions ($\le 140$ characters each) for all 8 phone and 8 tablet screenshots.

- [ ] **Step 3: Author `README.md`**
  Provide instructions on running the Next.js editor locally, modifying copy/assets, reproducing exports, and uploading to Google Play Console.

---

### Task 8: Git Integration, Push & Draft Pull Request

**Files:**
- Stage and commit all store assets, captures, documentation, and editor code (strictly excluding `node_modules` and `.next`).

- [ ] **Step 1: Verify git status and file cleanliness**
  Confirm no `node_modules`, `.next`, APKs, or temporary cache files are staged.

- [ ] **Step 2: Commit assets using Conventional Commits**
  Commit with descriptive commit message:
  `feat(marketing): deliver Google Play Store screenshot campaign for phone and tablet`

- [ ] **Step 3: Push branch to `origin`**
  Command: `git push -u origin marketing/<issue-number>-google-play-store-assets`

- [ ] **Step 4: Create Draft Pull Request**
  Command: `gh pr create --draft --title "feat(marketing): Google Play Store screenshot campaign (Phone & Tablet)" --body "..."`

- [ ] **Step 5: Prepare final copyable handoff**
  Compile the `## COPYABLE HANDOFF FOR CHATGPT` with all required subsections.
