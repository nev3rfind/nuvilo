# Phase 9 Notification & Haptic-First Architecture

**Date:** 2026-11-12  
**Status:** System Behavior Specification (Stage A)

---

## 1. Purpose-Specific Notification Channels

| Channel ID | User-Visible Name | Android Importance (API 26+) | Default Sound / Vibration | Purpose & Trigger |
|---|---|---|---|---|
| `novelo_medication_reminders` | Medication Reminders | `IMPORTANCE_HIGH` | **Silent (`sound = null`), Vibration ON** | Exact `AlarmManager` dose reminders & snoozed reminders |
| `novelo_upcoming_reminders` | Upcoming Reminders | `IMPORTANCE_DEFAULT` | Silent, gentle vibration | Optional upcoming-window alerts |
| `novelo_low_stock_alerts` | Low-Stock Warnings | `IMPORTANCE_DEFAULT` | System default (alertable) | WorkManager restock threshold & runway warnings |
| `novelo_expiry_warnings` | Expiry Warnings | `IMPORTANCE_DEFAULT` | Silent, gentle vibration | WorkManager alerts for packs nearing or past manufacturer expiry |
| `novelo_opened_discard_warnings` | Opened-Package Discard Warnings | `IMPORTANCE_HIGH` | Silent, Vibration ON | WorkManager alerts when opened eye drops/gels reach or near discard-after-opening cutoff |
| `novelo_prescription_reminders` | Prescription Reminders | `IMPORTANCE_DEFAULT` | System default | Prescription expiry & low-repeats alerts |
| `novelo_security_events` | Security Alerts | `IMPORTANCE_HIGH` | System default | Local security & backup/lock notices |

### 1.1 Respecting Android Channel Ownership (API 26+)
- On Android 8.0+, once a `NotificationChannel` is registered with the system, Android persists user-chosen sound, vibration, and importance overrides in system settings.
- **Invariant:** `NotificationHelper.ensureNotificationChannels()` MUST create missing channels idempotently with `createNotificationChannel(...)` and **NEVER delete/recreate existing channels**, so user customization in Android Settings is always respected.
- `NotificationHelper.getChannelDiagnostics()` inspects live `NotificationChannel` objects (`importance != IMPORTANCE_NONE`, `sound != null`, `shouldVibrate()`) so the Settings screen accurately reflects live OS state and links directly to `Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS` for per-category tuning.
- On API < 26 (minSdk 24–25) and in DataStore preferences (`NotificationCategoryConfig`), Nuvilo reads per-category preferences for vibration/sound where applicable.

---

## 2. Package-Aware & Privacy-Safe Notification Content

- **Reminder Notifications**:
  - When `isNotificationPrivacyMasked == true`: displays generic non-identifying text (`"Scheduled medication reminder"`, `"Tap to review or log your scheduled dose"`) with **zero** drug names, eye sites, or package info.
  - When `isNotificationPrivacyMasked == false`: includes form & administration site + package progress when useful (e.g., `"Hydro Forte — 2 drops (1 left eye, 1 right eye)"` or `"Single-dose eye drops — 1 container across both eyes"`), while keeping actions `Taken`, `Snooze`, `Skip` fast and unambiguous.
- **Low-Stock, Expiry & Discard Warnings (`LowStockCheckWorker`)**:
  - **Low-stock**: Computes package-aware supply runway (`ScheduleOccurrenceCalculator` + `SupplyForecastCalculator`), reporting remaining packages/units and realistic days left (`~5 days · 1 bottle unopened`). Deduplicated via `lowStockNotifiedMedIds` (reset when restocked above threshold).
  - **Expiry warnings**: Alerts once per `(lotId, warningStage)` (`notifiedExpiringLotKeys`) when an active lot expires within 14 days or has expired.
  - **Opened-package discard warnings**: Alerts once per `(lotId, discardStage)` (`notifiedDiscardLotKeys`) when an `OPENED` package is within 3 days of `discardAfterOpeningEpochMs` or has passed its discard window (and recommends discarding + opening next sealed package without touching stock until user confirms or opens).
  - **All WorkManager alerts respect `isNotificationPrivacyMasked`**: masking medication names when Privacy Mode is ON!

---

## 3. Central Haptic-First Architecture (`ui/haptics/NuviloHaptics.kt`)

### 3.1 Abstraction & Preferences
- `UserPreferences` additions:
  - `hapticsEnabled: Boolean = true`
  - `hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM` (`LOW`, `MEDIUM`, `HIGH`)
  - Plus per-category sound/vibration toggles & notification deduplication sets.
- `NuviloHapticEvent`:
  - `DOSE_TAKEN` — crisp confirmation pulse
  - `DOSE_SKIPPED` — distinct subtle tick
  - `DOSE_SNOOZED` — soft nudge
  - `PACKAGE_OPENED` — double-step tactile click
  - `RESTOCK_SAVED` — firm confirm pulse
  - `RECOUNT_SAVED` — firm confirm pulse
  - `PACKAGE_DISCARDED` — cautionary heavy pulse
  - `WARNING_CONFIRMED` — emphatic caution tick
  - `SELECTION_TICK` — light selection feedback (wizard steps, eye selection)
  - `TEST_HAPTIC` — user-triggered verification pulse in Settings

### 3.2 Safety & Respect for System Settings
- Checks both `UserPreferences.hapticsEnabled` and Android's `Settings.System.HAPTIC_FEEDBACK_ENABLED` (plus `Vibrator.hasVibrator()`).
- Provided via `LocalNuviloHaptics` in `NoveloTheme` so unit/UI tests can pass a recording `FakeHapticController`.
- **Never** invoked inside `LaunchedEffect` recompositions or duplicate/blocked clicks — strictly tied to confirmed user action callbacks and successful mutation handlers.
- Adding `<uses-permission android:name="android.permission.VIBRATE" />` (normal permission, zero network/privacy risk) enables `VibrationEffect.createOneShot(...)` scaled by `HapticIntensity` on API 26+, falling back gracefully to Compose `LocalHapticFeedback` on older APIs.
