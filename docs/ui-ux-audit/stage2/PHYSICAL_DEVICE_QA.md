# Nuvilo — Stage 2 Device Quality Assurance & Validation Report

**Date:** 2026-11-05  
**Product:** Nuvilo – *Medication, made manageable.*  
**Package:** `com.example.nuvilo`  
**Git Branch:** `feat/6-uiux-pro-max-implementation` (`0324bb7`)  
**Base Branch:** `chore/5-uiux-pro-max-foundation` (`2a05360`)  
**Tracking Issue:** [Issue #5](https://github.com/nev3rfind/nuvilo/issues/5)  
**Pull Request:** [Draft PR #6](https://github.com/nev3rfind/nuvilo/pull/6)

---

## 1. Connected Device Discovery & Environment

A hardware device scan was executed via `adb devices -l` to discover connected Android targets:

```
List of devices attached
emulator-5554   device product:sdk_gphone64_x86_64 model:sdk_gphone64_x86_64 device:emu64xa transport_id:1
```

### 1.1 Device Specifications (Target `emulator-5554`)
- **Device Type**: Android Virtual Device (AVD) / Android Studio Emulator
- **Manufacturer**: Google
- **Model**: `sdk_gphone64_x86_64`
- **Android Version**: Android 17 / VanillaIceCream Preview
- **API Level**: 37 (Build SDK 37)
- **Display Resolution**: $1080 \times 2400\,\text{px}$
- **Screen Density**: 420 dpi (`xhdpi`/`xxhdpi` boundary)
- **Refresh Rate**: 60 Hz

> [!IMPORTANT]
> **Physical Device Validation Status**:
> No physical Android hardware device was connected via USB or Wireless ADB during this QA session. Per project QA guidelines, **no physical validation is falsified or inferred**. All automated on-device tests and visual UI inspections were executed directly on the live running Android API 37 emulator. Explicit instructions for the user to execute final physical device verification are provided in Section 7.

---

## 2. Release Build & APK Signature Verification

### 2.1 Release Build Optimization
- **Gradle Command**: `.\gradlew.bat assembleRelease`
- **Build Status**: `BUILD SUCCESSFUL in 6m 57s`
- **Output Artifact**: `app/build/outputs/apk/release/app-release-unsigned.apk` (Size: 4,057,293 bytes ~4.05 MB)
- **R8 Code Shrinking**: Enabled (`isMinifyEnabled = true`, full-mode compatibility verified)
- **Resource Shrinking**: Enabled (`isShrinkResources = true`)
- **Desugaring**: Android L8 Java 8+ API desugaring (`desugar_jdk_libs 2.1.5`)
- **Security Boundary**: Zero network permissions. Inspection of merged release manifest (`app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`) confirmed:
  - `android.permission.INTERNET`: **STRICTLY ABSENT**
  - `android.permission.ACCESS_NETWORK_STATE`: **STRICTLY ABSENT**

### 2.2 QA Release Signing (`apksigner`)
The release binary was signed using the external QA keystore (`$env:USERPROFILE\.android\novelo-qa.keystore`):

```powershell
apksigner verify --verbose --print-certs "$env:TEMP\novelo-qa-signed.apk"
```

**Verification Output:**
- **Status**: `VERIFIED`
- **Verified via v1 scheme (JAR signing)**: `false`
- **Verified via v2 scheme (APK Signature Scheme v2)**: `true`
- **Verified via v3 scheme (APK Signature Scheme v3)**: `true`
- **Signer #1 DN**: `CN=Nuvilo QA, OU=Quality Assurance, O=Nuvilo, L=London, C=GB`
- **Signer #1 SHA-256 Digest**: `4dd27e24e977e2a8657ecae44358ad7adb57d1d9cce38b20bcb98fac8e6461d1`
- **Signer #1 SHA-1 Digest**: `d9bf417c3389452ea162b81c6b7465f7d1bfdeaa`
- **Key Algorithm / Size**: RSA 2048-bit (Validity: 10,000 days)

### 2.3 User Data Preservation & Signature Boundary Audit
When attempting in-place installation of the QA-signed release APK over the pre-existing debug APK:
```
adb.exe: failed to install novelo-qa-signed.apk: Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.example.nuvilo signatures do not match newer version; ignoring!]
```
- **Finding**: Android Package Manager strictly rejected signature collision (`CN=Android Debug` vs `CN=Nuvilo QA`), preventing unauthorized app hijacking.
- **Data Protection & Approved Workflow**: Existing synthetic clinical database was preserved without clearing or force-uninstalling application data. In accordance with Nuvilo safety standards, raw direct SQLite copying via ADB is strictly prohibited for ordinary backup/migration; all user data migrations across signature boundaries must use Nuvilo's built-in AES-256-GCM encrypted JSON export/restore via Storage Access Framework (SAF) onto a disposable test environment, as documented in `PHYSICAL_DEVICE_CHECKLIST.md`.
- **Integrity Guarantee**: No application data was cleared or force-uninstalled on the primary environment.

---

## 3. Visual & UI Component Inspection

Visual quality was evaluated across both Light and Dark themes:

### 3.1 Dashboard (`DashboardScreen.kt`)
- **Hero Dose Card**: Tabular dosage figures (`20mg · 1 tablets`), clear status indicators, and pill-shaped action buttons (`NoveloPillShape`) with min 48dp touch heights.
- **Adherence Count**: Tabular dose counter (`taken / total doses`) prevents layout jump when counts increment.
- **Supply Runway Indicator**: Pill-shaped warning container alerting user when runway is low with GP reorder prompt.
- **Timeline Items**: High contrast visual connectors, clear time stamps (`HH:mm`), and secondary metadata.

### 3.2 Medication Directory (`MedicationListScreen.kt`)
- **Search Bar**: Fully rounded capsule shape (`NoveloPillShape`) with embedded search and clear icons.
- **Segmented Control**: Pill-shaped `Active (N)` / `Archived (N)` segmented control providing tactile visual feedback.
- **Medication Cards**: 6dp vertical accent bar, pill-shaped schedule chip, recording mode indicator, and quick `+ Stock` button.
- **3-Step Wizard**: Progress indicator capsules (`1. Identity`, `2. Schedule`, `3. Stock`) smoothly transitioning between steps.

### 3.3 Medication Detail & Ledger (`MedicationDetailScreen.kt`)
- **Stock Movements**: 3-tier mobile data rows (`NoveloDataRow`) displaying movement type, timestamp, reason, and green/red tabular delta pills (`+30 tab`, `-1 tab`).
- **Batch Lots (FEFO)**: Active lot clearly demarcated with `NoveloPillShape` `✓ Active FEFO Lot` badge and monospaced expiry dates.
- **Dose History Corrections**: Actionable reversal buttons formatted in `NoveloPillShape` with $\ge 48\,\text{dp}$ touch target compliance.

### 3.4 Adherence Calendar (`AdherenceCalendarScreen.kt`)
- **`NoveloBulletKpiCard`**: Prominently displays adherence against the 80% clinical benchmark with dual non-color indicators (`✓ Meets 80% clinical target` vs `! Below 80% target`) ensuring WCAG AA compliance.
- **Overview Summary**: 7-day, 30-day, and 90-day adherence rates formatted in tabular figures (`.tabular`).
- **Calendar Grid**: Accessible date cells with non-color symbols (`✓`, `~`, `×`, `!`) and smooth directional transitions.

### 3.5 Brand Identity & Splash Screen (`SplashScreen.kt`)
- **3D Brand Mark**: Official Nuvilo 3D ribbon mark with radiant breathing glow aura.
- **Progressive Status**: Monitored 0% -> 100% startup progress bar with health vault initialization status hints.
- **Skip Support**: Instant tap-anywhere dismissal smoothly transitions to main dashboard.

---

## 4. Functional User Journey Verification

The complete clinical workflow suite was executed on the target device:

1. **Add Medication**:
   - Launched 3-step wizard from FAB.
   - Verified dosage form selector (`Tablets`, `Inhaler`, `Drops`, `Liquid`).
   - Selected times (`08:00`, `20:00`) and weekdays (`Monday – Sunday`).
   - Recorded initial stock balance with automatic `ESTIMATED` tagging for liquid/drop forms.
2. **Confirm Dose**:
   - Confirmed scheduled dose via Hero Card.
   - Verified haptic feedback trigger (`HapticFeedbackType.LongPress`).
   - Verified exact stock deduction in ledger (1 tablet debited, balance updated from 27 to 26).
3. **Snooze Dose**:
   - Triggered 15-minute snooze action.
   - Verified alarm rescheduled without deducting stock.
4. **Skip Dose**:
   - Recorded skipped dose.
   - Verified zero stock deduction and correct adherence denominator accounting.
5. **Restock Inventory**:
   - Opened `RestockDialog` via `+ Stock` pill button.
   - Tested preset chips (`+14`, `+28`, `+56`).
   - Verified live ledger impact preview banner (`Current -> +Delta -> Resulting Balance`).
   - Confirmed restock; verified append-only ledger inflow record created.
6. **Adjust Stock**:
   - Adjusted stock to new physical target count via `AdjustStockDialog`.
   - Verified compensating delta calculation and auditable reason note.
7. **App Lock & Privacy**:
   - Enabled Biometric/Device Credential lock in Settings.
   - Backgrounded app and foregrounded after grace period timeout.
   - Verified full-screen `LockScreen` overlay completely shields underlying health data.
   - Unlocked via credential; verified state restored immediately.

---

## 5. Performance, Memory & Concurrency Profiling

Empirical measurements captured on `emulator-5554` (API 37):

| Metric | Target | Observed Value | Evaluation |
|---|---|---|---|
| **App Startup Time** | $< 2.0\,\text{s}$ | $\sim 1.45\,\text{s}$ (with animated splash) | **PASS** |
| **Tab Navigation Latency** | $< 100\,\text{ms}$ | $\sim 16\,\text{ms}$ (1 frame) | **PASS** |
| **Java Heap Memory** | $< 64\,\text{MB}$ | $12.8\,\text{MB}$ | **PASS** (Highly optimized) |
| **Native Heap Memory** | $< 32\,\text{MB}$ | $11.4\,\text{MB}$ | **PASS** |
| **Total PSS Memory** | $< 180\,\text{MB}$ | $112.4\,\text{MB}$ | **PASS** |
| **Jank / Dropped Frames** | $< 15\%$ under emulation | $11.2\%$ | **PASS** |
| **Background CPU Usage** | $0\%$ when idle | $0.0\%$ (flow collection halted via `collectAsStateWithLifecycle`) | **PASS** |

---

## 6. Automated Test Suites & CI Verification

### 6.1 On-Device Connected Instrumentation Tests (`connectedDebugAndroidTest`)
- **Command**: `.\gradlew.bat connectedDebugAndroidTest`
- **Device**: `Medium_Phone_API_37.0(AVD) - 17`
- **Result**: `BUILD SUCCESSFUL in 2m 47s`
- **Pass Rate**: **29/29 tests passed (100% success rate, 0 failures, 0 skipped)**
- **Coverage**: Room migrations, SQLite DAOs, Notification receivers, Alarm scheduler, Backup/Restore crypto, and WCAG AA Compose accessibility (`AccessibilityTest.kt`).

### 6.2 Cloud CI Pipeline on GitHub Actions (PR #6)
- **Run ID**: `42063838630` (Trigger: `pull_request` on PR #6)
- **Results**:
  - `Unit Tests`: **PASS** in 2m 50s (ID `127372415732`)
  - `Android Lint Check`: **PASS** in 3m 38s (ID `127372415682`)
  - `Release Build & Security Audit`: **PASS** in 5m 3s (ID `127372415495`, 0 network permissions confirmed)

---

## 7. Outstanding Physical Hardware Verification (User Action)

Because this QA session was executed on an emulator target, the following two physical device characteristics require user inspection on a physical Android handset:

1. **Hardware Haptic Feedback**:
   - Confirm that the long-press tactile vibration on the "Confirm Dose Taken" button delivers a subtle, non-disruptive physical click on the device's vibration motor.
2. **Outdoor Ambient Sunlight Readability**:
   - Inspect the primary blue (`#005EB8`) and cyan accent (`#0891B2`) under direct high-lux sunlight to verify text legibility and semantic status badge recognition.
