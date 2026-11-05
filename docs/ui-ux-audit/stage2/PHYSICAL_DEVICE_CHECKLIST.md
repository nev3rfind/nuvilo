# Novelo — Physical Device QA Verification Checklist

**Document Version:** 1.0.0  
**Date:** 2026-11-05  
**Product:** Novelo – *Medication, made manageable.*  
**Package:** `com.example.nuvilo`  
**Target Build:** QA-Signed Release Build (`app-release-unsigned.apk` signed with `novelo-qa.keystore`)  
**Current Automated Environment:** Android 17 / API 37 Emulator (`emulator-5554`)  
**Purpose:** Comprehensive on-device verification checklist for manual physical hardware validation.

> [!IMPORTANT]
> **Physical Device Validation Integrity Notice:**  
> During automated CI and preliminary verification sessions, automated tests and UI inspection were conducted on an **Android Virtual Device (AVD API 37 / Android 17)**. Per project standards, **physical hardware verification must never be falsified or assumed**. This protocol outlines the exact manual validation procedure for human verification on a physical Android handset.

---

## 1. Prerequisites & Release APK Installation

### 1.1 Target Binary & Signature Information
- **Source Artifact:** `app/build/outputs/apk/release/app-release-unsigned.apk` (Size: ~4.05 MB)
- **Signed Binary Location:** `$env:TEMP\novelo-qa-signed.apk`
- **Signing Keystore:** `$env:USERPROFILE\.android\novelo-qa.keystore`
- **Signer DN:** `CN=Novelo QA, OU=Quality Assurance, O=Novelo, L=London, C=GB`
- **Signer SHA-256 Digest:** `4dd27e24e977e2a8657ecae44358ad7adb57d1d9cce38b20bcb98fac8e6461d1`

To sign the latest release APK manually if needed:
```powershell
$signer = "C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe"
# Or using apksigner from Android SDK Build-Tools:
apksigner sign --ks "$env:USERPROFILE\.android\novelo-qa.keystore" `
  --ks-pass pass:novelo123 `
  --key-pass pass:novelo123 `
  --out "$env:TEMP\novelo-qa-signed.apk" `
  app\build\outputs\apk\release\app-release-unsigned.apk
```

### 1.2 User Data Preservation Across Signing Certificate Boundaries
When upgrading from a development debug build (`CN=Android Debug`) to a QA release build (`CN=Novelo QA`), Android Package Manager will reject in-place updates with:
```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.example.nuvilo signatures do not match newer version]
```

**Safe Data Preservation Protocol (Do Not Wipe Data):**
1. **Back up existing clinical database from device:**
   ```powershell
   adb exec-out run-as com.example.nuvilo cat databases/novelo.db > scratch/novelo_backup.db
   adb exec-out run-as com.example.nuvilo cat databases/novelo.db-wal > scratch/novelo_backup.db-wal
   ```
2. **If uninstalling debug build is approved by the tester:**
   ```powershell
   adb uninstall com.example.nuvilo
   adb install -r "$env:TEMP\novelo-qa-signed.apk"
   ```
3. **Restore preserved database to newly installed app:**
   ```powershell
   adb push scratch/novelo_backup.db /data/local/tmp/novelo.db
   adb shell "run-as com.example.nuvilo cp /data/local/tmp/novelo.db databases/novelo.db"
   adb shell "run-as com.example.nuvilo chmod 660 databases/novelo.db"
   ```

---

## 2. On-Device Verification Checklist

### 2.1 Startup & 3D Brand Presentation
- [ ] **Splash Screen Animation:** Launch app from launcher icon. Confirm smooth spring entrance of the official 3D Novelo brand logo (`R.drawable.ic_novelo_logo`).
- [ ] **Ambient Radiant Aura:** Verify subtle breathing glow around the 3D logo with radial gradient blending cyan and clinical blue.
- [ ] **Progress Bar & Handoff:** Confirm horizontal determinate progress bar loads from 0% to 100% with progressive status messages ("Securing private health vault...", "Loading medication schedule...", "Verifying offline stock ledger...").
- [ ] **Tap to Skip:** Tap anywhere during splash animation; verify immediate transition to Dashboard without lag.
- [ ] **App Launcher Icon:** Check phone home screen and app drawer. Confirm high-resolution circular/adaptive icon renders crisp without artifacting or distortion.

### 2.2 Dashboard & Scrolling Dynamics
- [ ] **Hero Dose Card:** Verify "Next Dose" hero banner renders prominent tabular dosage figures (`20mg · 1 tablets`), time pill, and high-contrast clinical blue CTA.
- [ ] **Smooth Scrolling:** Scroll up and down on the Today dashboard. Verify 60Hz/120Hz frame rate stability without frame drops or jitter.
- [ ] **Empty State:** When no doses remain, verify the `CompletedDayCard` transitions in smoothly with checkmark illustration and positive reassurance text.
- [ ] **Stock Runway Banner:** Verify low stock warning pill appears when medication supply drops below threshold, showing accurate days remaining.

### 2.3 Light & Dark Theme Readability
- [ ] **System Theme Toggle:** Toggle system Dark Mode from Android Quick Settings while app is open.
- [ ] **Light Mode Contrast:** Verify pure white cards on cool slate canvas (`#F8FAFC`). Confirm body text (`#0F172A`) meets WCAG AA $\ge 4.5:1$ contrast.
- [ ] **Dark Mode Legibility:** Verify elevated slate cards (`#1E293B`) on deep slate background (`#0F172A`). Confirm text (`#F8FAFC`) has zero glare or OLED smear.
- [ ] **Status Badge Contrast in Dark Theme:** Check Taken (emerald `#6EE7B7`), Assumed (sky `#7DD3FC`), Skipped (slate `#CBD5E1`), and Missed (rose `#FCA5A5`) badges for crisp contrast.

### 2.4 Edge-to-Edge, Screen Cutouts & System Insets
- [ ] **Status Bar Inset:** Check top app bar on devices with punch-hole cameras or display notches. Confirm icons and title text are never obscured by the camera cutout.
- [ ] **Gesture Navigation Bar Inset:** Verify bottom navigation bar sits cleanly above the Android gesture pill (`navigationBarsPadding()`), with no overlapping touch targets.
- [ ] **Landscape Mode / Rotation:** Rotate device to landscape (if enabled) or unfold on foldable devices. Confirm content does not clip beneath system bars.

### 2.5 Touch Target Accessibility
- [ ] **Minimum 48x48dp Bounds:** Test all interactive components:
  - Bottom navigation bar icons.
  - "Take Dose" and "Skip" action buttons.
  - Medication card chevron buttons.
  - History reversal and correction icon buttons.
  - Quick "+ Stock" action chips.
- [ ] **No Overlapping Click Targets:** Verify ample tap padding between adjacent buttons.

### 2.6 Physical Haptics & Feedback
- [ ] **Dose Confirmation Haptic:** Tap "Take Dose" on Hero Dose Card or timeline item. Verify tactile physical vibration occurs (`HapticFeedbackType.LongPress`).
- [ ] **Restock Confirmation Haptic:** Record a restock event in Medication Detail. Confirm tactile feedback fires upon saving.
- [ ] **Settings Toggle:** Verify subtle tick when enabling/disabling Privacy Mode or Biometric Lock.

### 2.7 Dialogs & Virtual Keyboard (IME)
- [ ] **Restock Dialog:** Open "Add Stock" dialog. Focus the quantity input field. Verify virtual keyboard appears without pushing dialog buttons offscreen.
- [ ] **Identity / Add Medication Wizard:** Navigate through 3-step wizard. Test keyboard "Next" and "Done" actions.
- [ ] **Dismissal:** Tap outside dialog or press back button. Verify smooth dismiss transition.

### 2.8 Biometric Hardware & Device Credential Lock
- [ ] **Biometric Prompt:** In Settings, enable App Lock. Close app and reopen. Verify Android BiometricPrompt appears asking for Fingerprint / Face Unlock / PIN.
- [ ] **Successful Authentication:** Authenticate with registered fingerprint. Verify app unlocks immediately to previous state.
- [ ] **Fallback Authentication:** Cancel biometric prompt or test device credential fallback (PIN/pattern). Verify secure unlock.
- [ ] **Privacy Mode Masking:** With App Lock enabled, confirm recent tasks thumbnail (Android App Switcher) does not expose sensitive health information.

### 2.9 Offline Exact Alarms & Notification Actions
- [ ] **Exact Alarm Firing:** Schedule a dose 2 minutes in the future. Lock device. Confirm notification fires at exact minute with sound/vibration.
- [ ] **Interactive Notification Actions:** Expand notification. Test inline "Take" button directly from lock screen.
- [ ] **Privacy Mode Notification:** Enable Notification Privacy in Settings. Schedule dose. Confirm notification displays generic reminder ("Scheduled dose reminder") without revealing drug name or dosage.
- [ ] **Offline Stock Ledger Deduction:** After tapping "Take" on notification, open app. Confirm stock count decremented by exactly the scheduled dose, with an immutable movement recorded in the ledger.

---

## 3. Evidence Collection & Reporting
Upon completing physical testing on actual hardware:
1. Record device manufacturer, exact model name, and Android OS version.
2. Note any layout clipping, touch target issues, or haptic anomalies.
3. Attach device screenshots to `docs/ui-ux-audit/stage2/` if desired.
4. Update `PROGRESS.md` to reflect physical device validation completion.
