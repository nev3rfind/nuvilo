# NHS PPC Local Notification & Alarm Specification

## 1. Scheduling Architecture (`PpcAlarmScheduler`)
- **Scheduler**: Android `AlarmManager` (`setExactAndAllowWhileIdle` when `canScheduleExactAlarms()` is true; graceful fallback to `setAndAllowWhileIdle`).
- **Notification Channel**: Uses `NotificationChannelIds.PRESCRIPTION_REMINDERS` (`novelo_prescription_reminders`).
- **Default Offsets**:
  - `30` days before expiry
  - `14` days before expiry
  - `7` days before expiry
  - `0` days (on expiry day)
  - Default local trigger time: `09:00` (`reminderHour = 9, reminderMinute = 0`).
- **Configurability**: Each offset can be toggled independently in Step 3 of the PPC wizard or when editing a certificate.

---

## 2. Duplicate Prevention & Cancellation
- **Deterministic Request Codes**:
  `requestCode = "ppc_alarm_${certificateId}_$offsetDays".hashCode()`
- **Pre-Schedule Cleanup**: `scheduleRemindersForCertificate` always calls `cancelAlarmsForCertificate(certificate.id)` across all standard offsets `[30, 14, 7, 0]` before scheduling active future triggers (`triggerEpochMs > nowEpochMs`).
- **Lifecycle Cancellation**: Alarms are cancelled immediately on:
  - Certificate edit
  - Certificate renewal
  - Certificate archival
  - Certificate deletion
- **System Reconciliation**: `reconcileAllPpcAlarms()` runs on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, and `TIME_SET`.

---

## 3. Privacy Mode Compliance
`PpcAlarmReceiver` reads `UserPreferences.isNotificationPrivacyMasked` before posting:
- **Standard Mode**:
  - Title: `"3-Month Prescription Prepayment Certificate (PPC) expires in 14 days"` (or 12-Month PPC / NHS Prescription Exemption)
  - Body: `"Based on your recorded certificate dates, your certificate expires on 31 Mar 2027."`
- **Privacy Mode Enabled**:
  - Title: `"Certificate Reminder"`
  - Body: `"Open Nuvilo to review your recorded certificate dates."`
  - Zero certificate type, exemption category, or expiry date is exposed on the lock screen.

## 4. Deep Linking
Tapping the notification opens `MainActivity` with `PpcAlarmReceiver.EXTRA_OPEN_PPC_MANAGEMENT = true` to navigate directly to `PpcManagementScreen`.
