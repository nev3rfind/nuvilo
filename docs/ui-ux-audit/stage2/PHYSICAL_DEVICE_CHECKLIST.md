# Novelo — Physical Device QA Verification Checklist & Safe Data Protocol

**Document Version:** 2.0.0
**Date:** 2026-11-06
**Product:** Novelo – *Medication, made manageable.*
**Package:** `com.example.nuvilo`
**Target Build:** QA-Signed Release Build (`app-release-unsigned.apk` signed with `novelo-qa.keystore`)
**Current Automated / AVD Environment:** Android 17 / API 37 Emulator (`emulator-5554`)
**Purpose:** Comprehensive on-device verification checklist and data integrity protocol for human manual validation on physical Android handsets.

> [!IMPORTANT]
> **Verification Tier Distinctions & Integrity Notice:**
> This checklist distinguishes between three distinct verification tiers:
> - `[AUTOMATED]`: Automated CI checks and unit/instrumentation tests.
> - `[EMULATOR]`: Executed and visually confirmed on Android Virtual Devices (`emulator-5554`, API 37).
> - `[PHYSICAL]`: Reserved **strictly** for manual human validation on physical Android hardware.
>
> Per Novelo engineering standards, **physical hardware verification must never be falsified, assumed, or pre-checked**. All `[PHYSICAL]` checkboxes remain unchecked until executed by a human tester on physical hardware.

---

## 1. Device and OS Information

- [x] `[AUTOMATED]` Verify target SDK compatibility: compile/target SDK 37, minimum SDK 24.
- [x] `[EMULATOR]` Record AVD baseline specifications:
  - Model: Google `sdk_gphone64_x86_64` (Medium Phone AVD)
  - Android Version: Android 17 / VanillaIceCream Preview (API Level 37)
  - Resolution & Density: $1080 \times 2400\,\text{px}$, 420 dpi, 60 Hz
- [ ] `[PHYSICAL]` Record physical test device specifications:
  - Device Manufacturer & Model: _______________________
  - Android OS Version & Security Patch Level: _______________________
  - Display Cutout Type (Punch-hole, Notch, Dynamic Island, None): _______________________
  - Screen Refresh Rate (60Hz / 90Hz / 120Hz): _______________________
  - Biometric Hardware Type (Optical / Ultrasonic Fingerprint, Face Unlock): _______________________

---

## 2. QA Signing Verification

- [x] `[AUTOMATED]` R8 code shrinking and resource shrinking enabled without reflection errors.
- [x] `[AUTOMATED]` Merged release manifest verified 100% free of network permissions (`INTERNET` and `ACCESS_NETWORK_STATE` strictly absent).
- [x] `[AUTOMATED]` Release APK signed with QA keystore (`$env:USERPROFILE\.android\novelo-qa.keystore`) via `apksigner`.
- [x] `[EMULATOR]` Verify APK signature schemes and digest via `apksigner`:
  ```powershell
  apksigner verify --verbose --print-certs "$env:TEMP\novelo-qa-signed.apk"
  ```
  - Status: `VERIFIED`
  - APK Signature Scheme v2: `true`
  - APK Signature Scheme v3: `true`
  - Signer DN: `CN=Novelo QA, OU=Quality Assurance, O=Novelo, L=London, C=GB`
  - Signer SHA-256 Digest: `4dd27e24e977e2a8657ecae44358ad7adb57d1d9cce38b20bcb98fac8e6461d1`
- [ ] `[PHYSICAL]` Verify release package digest matches before physical installation.

---

## 3. Safe Backup Preparation (Encrypted SAF Workflow)

> [!CAUTION]
> **Prohibition of Direct SQLite / WAL Copying:**
> Never use `adb exec-out run-as ... cat databases/novelo.db` or manually copy SQLite database/WAL files as an ordinary backup or migration method. Copying active SQLite files while Room is running can lead to database corruption, incomplete WAL flush states, and orphaned referential constraints. Always use Novelo's application-level encrypted backup mechanism.

### Approved Application-Level Backup Export Protocol:
1. Open the existing Novelo application on the device.
2. Navigate to **Settings → Local Data & Backup**.
3. Select **Export Encrypted Backup (JSON)**.
4. Enter a strong, memorable backup passphrase.
   *(Note: The passphrase is never stored or transmitted; Novelo cannot recover lost passphrases).*
5. When the Android Storage Access Framework (SAF) document picker appears, choose a **trusted local destination** (e.g. `Downloads` or device local storage).
   *(Avoid cloud-synced folders to uphold strict offline privacy).*
6. Confirm the encrypted `.json` backup file exists outside the application sandbox.

- [ ] `[PHYSICAL]` Export verified encrypted JSON backup via Storage Access Framework.
- [ ] `[PHYSICAL]` Securely record or memorize the backup passphrase.
- [ ] `[PHYSICAL]` Confirm backup file is safely stored on local physical storage.

---

## 4. Installation on a Disposable Test Environment

### QA Signing Certificate Mismatch Policy
A QA-signed release APK (`CN=Novelo QA`) cannot overwrite an existing development build signed with Android Debug (`CN=Android Debug`), resulting in:
```text
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.example.nuvilo signatures do not match newer version]
```

**Safety Rules for QA Testing:**
- **Never force an installation** using `-d` downgrade flags that risk corrupting data.
- **Never uninstall the user's primary installation** without a verified, tested backup.
- **Never clear user application data** via `pm clear` during QA testing.
- **Always install the QA build on a disposable test environment**, such as a secondary test device, dedicated work profile, guest profile, or clean emulator.

- [x] `[EMULATOR]` Clean install executed on disposable AVD `emulator-5554`:
  ```powershell
  adb -s emulator-5554 install -r "$env:TEMP\novelo-qa-signed.apk"
  ```
- [ ] `[PHYSICAL]` Install QA release APK on dedicated physical test device or secondary profile.
- [ ] `[PHYSICAL]` Confirm clean installation succeeds without signature rejection.

---

## 5. Dashboard and Visual Checks

- [x] `[EMULATOR]` Startup splash screen displays official 3D brand logo (`R.drawable.ic_novelo_logo`) with entrance spring animation and breathing ambient glow.
- [x] `[EMULATOR]` Determinate progress bar animates from 0% to 100% with progressive status text; tap-to-skip transitions immediately.
- [x] `[EMULATOR]` Hero Dose Card renders tabular dosage numbers (`20mg · 1 tablets`) and high-contrast clinical blue CTA (`#005EB8`).
- [x] `[EMULATOR]` Tabular numerals (`fontFeatureSettings = "tnum"`) applied to all dosage quantities, adherence counts, and timestamps.
- [ ] `[PHYSICAL]` Launch application on physical device; confirm splash animation fluidity and ambient aura rendering.
- [ ] `[PHYSICAL]` Verify home screen launcher adaptive icon renders sharp with clinical blue gradient background.
- [ ] `[PHYSICAL]` Verify 60Hz / 120Hz smooth vertical scrolling on the Today dashboard list without frame stutter.
- [ ] `[PHYSICAL]` Verify `CompletedDayCard` empty state transitions in cleanly when all doses for the day are taken.

---

## 6. Medication Interactions

- [x] `[EMULATOR]` 3-Step Medication Wizard (`1. Identity`, `2. Schedule`, `3. Stock`) progresses smoothly.
- [x] `[EMULATOR]` Dosage form selector automatically updates counting units (tablets, puffs, ml, drops).
- [x] `[EMULATOR]` In-place Edit Medication and Edit Schedule dialogs update Room database and reconcile alarms.
- [ ] `[PHYSICAL]` Create a new medication using the 3-step wizard on physical device.
- [ ] `[PHYSICAL]` Edit existing medication schedule and verify updated times appear on the dashboard timeline.
- [ ] `[PHYSICAL]` Test "Take Dose" interaction from the Hero card; confirm visual state updates to "Taken".
- [ ] `[PHYSICAL]` Test "Skip Dose" and "Snooze" actions; verify adherence calculations reflect skip without stock debit.

---

## 7. Stock and FEFO Verification

- [x] `[EMULATOR]` 3-tier mobile data rows (`NoveloDataRow`) in Medication Detail show delta pills (`+30 tab`, `-1 tab`).
- [x] `[EMULATOR]` Active FEFO lot marked with `✓ Active FEFO Lot` pill badge and monospaced expiry date.
- [x] `[EMULATOR]` Low-stock runway indicator alerts user when remaining supply drops below threshold.
- [ ] `[PHYSICAL]` Record a restock event with a batch number and expiry date; confirm FEFO ordering prioritizes earliest expiry.
- [ ] `[PHYSICAL]` Verify quick "+ Stock" action chips (`+7`, `+14`, `+28`) on medication list cards.
- [ ] `[PHYSICAL]` Verify Repeat Prescription flow: open `RequestRepeatDialog`, submit request, verify pending badge on dashboard and notification preview.

---

## 8. Notifications and Exact Alarms

- [x] `[EMULATOR]` Reminders scheduled with exact alarm manager (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`).
- [x] `[EMULATOR]` Bounded 3-day alarm scheduling horizon prevents system alarm queue exhaustion.
- [ ] `[PHYSICAL]` Schedule a dose 2 minutes in the future; lock phone screen.
- [ ] `[PHYSICAL]` Verify notification fires at the exact minute with heads-up display, sound, and vibration.
- [ ] `[PHYSICAL]` Expand notification: confirm notification displays remaining stock quantity and runway advisory.
- [ ] `[PHYSICAL]` Tap inline "Take" action from the notification: confirm stock decrements and notification dismisses.
- [ ] `[PHYSICAL]` Enable Notification Privacy Mode in Settings; trigger alarm; confirm notification masks sensitive medication name and dosage with a generic reminder.

---

## 9. Biometrics and App Lock

- [x] `[AUTOMATED]` `AppLockManagerTest` unit tests passing across all grace periods (`Immediately`, `30s`, `1m`, `5m`, `15m`).
- [x] `[EMULATOR]` Lock screen overlay renders accessible lock icon and gates underlying UI hierarchy.
- [x] `[EMULATOR]` Dynamic `FLAG_SECURE` prevents screen capture when app lock is active.
- [ ] `[PHYSICAL]` Enable App Lock in Settings on physical hardware.
- [ ] `[PHYSICAL]` Close application and reopen; verify Android `BiometricPrompt` prompts for fingerprint / face unlock.
- [ ] `[PHYSICAL]` Authenticate with enrolled fingerprint; verify instant unlock to previous state.
- [ ] `[PHYSICAL]` Test device credential fallback (PIN / pattern); verify successful authentication.
- [ ] `[PHYSICAL]` Switch to Android Recent Apps overview; verify application thumbnail is masked (black screen).

---

## 10. Haptic Feedback

- [x] `[EMULATOR]` Haptic feedback invocations (`HapticFeedbackType.LongPress`) integrated on dose and restock confirmations.
- [ ] `[PHYSICAL]` Confirm physical vibration fires upon tapping "Take Dose" on Hero card.
- [ ] `[PHYSICAL]` Confirm physical vibration fires upon confirming restock in `RestockDialog`.
- [ ] `[PHYSICAL]` Confirm subtle tactile tick when toggling switches in Settings.

---

## 11. Display Cut-outs and System Insets

- [x] `[EMULATOR]` Edge-to-edge layout enabled with `WindowInsets.safeDrawing`, `statusBarsPadding()`, and `navigationBarsPadding()`.
- [ ] `[PHYSICAL]` Check top app bar on physical handset with camera cutout (hole-punch or notch); confirm title text and action icons do not intersect the camera cutout.
- [ ] `[PHYSICAL]` Check bottom navigation bar with gesture navigation active; confirm tabs sit comfortably above the Android gesture bar.
- [ ] `[PHYSICAL]` Open virtual keyboard in `AddMedicationDialog` and `RestockDialog`; verify `imePadding()` prevents input fields and action buttons from being obscured by keyboard.

---

## 12. Theme and Outdoor Readability

- [x] `[EMULATOR]` Light theme: cool slate canvas (`#F8FAFC`), pure white cards (`#FFFFFF`), high-contrast ink text (`#0F172A`).
- [x] `[EMULATOR]` Dark theme: deep slate canvas (`#0F172A`), elevated slate cards (`#1E293B`), crisp light text (`#F8FAFC`).
- [ ] `[PHYSICAL]` Toggle system Dark Theme from Quick Settings while app is open; verify instant, flicker-free re-theming.
- [ ] `[PHYSICAL]` Test application outdoors under bright direct sunlight; verify primary buttons, dosage text, and status chips remain easily readable ($\ge 4.5:1$ contrast).
- [ ] `[PHYSICAL]` Verify OLED dark mode comfort in dark room with zero eye strain or harsh pure-black smear.

---

## 13. Backup Restoration (Application-Level SAF Restore)

### Approved Application-Level Restore Protocol:
1. On the disposable test device/profile, open Novelo.
2. Complete initial welcome flow.
3. Navigate to **Settings → Local Data & Backup**.
4. Select **Restore Encrypted Backup (JSON)**.
5. Select the encrypted `.json` backup file exported in Step 3 via the system SAF picker.
6. Enter the backup passphrase.
7. Inspect the **Restore Preview dialog**:
   - Confirm medication count matches original.
   - Confirm schedule count matches original.
   - Confirm stock lots and ledger movement counts match original.
   - Confirm prescription and history counts match original.
8. Tap **Restore Data** to execute transactional restoration within `NoveloDatabase.withTransaction`.

- [ ] `[PHYSICAL]` Open encrypted JSON file via system document picker.
- [ ] `[PHYSICAL]` Enter correct passphrase; confirm decryption succeeds.
- [ ] `[PHYSICAL]` Inspect restore preview and verify entity counts.
- [ ] `[PHYSICAL]` Execute restore and confirm atomic completion.

### Restoration Caveats & Technical Boundary Notice:
- **Authentication Preferences:** Biometric app lock and passphrase preferences are deliberately omitted from backup files to prevent lockouts on new hardware; configure App Lock manually after restore.
- **Runtime Permissions:** Android platform permissions (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`) must be granted on the new installation.
- **Alarm Reconciliation:** Upon successful restore, Novelo automatically triggers `AlarmScheduler.reconcileUpcomingReminders()` to reschedule all active doses for the next 3 days.

---

## 14. Final Data-Integrity Checks

Following restoration in the disposable test environment, verify:
- [ ] `[PHYSICAL]` All active medications appear in the directory with correct names, forms, and accent swatches.
- [ ] `[PHYSICAL]` Schedules remain accurate (dosage amount, dose times, active weekdays).
- [ ] `[PHYSICAL]` Stock balances remain exact (no float drift; integer units match pre-backup values).
- [ ] `[PHYSICAL]` FEFO lots and expiry dates are fully preserved.
- [ ] `[PHYSICAL]` Adherence history and calendar logs display historical dose outcomes (`✓`, `~`, `×`, `!`).
- [ ] `[PHYSICAL]` Active repeat prescriptions and audit events remain intact.
- [ ] `[PHYSICAL]` Upcoming doses appear on the Today dashboard with accurate countdowns.
- [ ] `[PHYSICAL]` System Diagnostics in Settings confirms exact alarms are registered and reconciled.

---

## 15. Manual QA Sign-Off

| Role | Name | Signature / Identifier | Date | Result (Pass / Fail) |
|---|---|---|---|---|
| **QA Engineer / Tester** | _______________________ | _______________________ | ____________ | ___________________ |
| **Lead Developer** | _______________________ | _______________________ | ____________ | ___________________ |
