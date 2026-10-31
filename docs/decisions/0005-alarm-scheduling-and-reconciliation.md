# ADR 0005: Alarm Scheduling and Reconciliation Architecture

- **Status**: Accepted
- **Date**: 2026-10-29
- **Deciders**: Senior Android Lead, Systems Architect

## Context
Medication reminders must be delivered at exact times to ensure patient safety (e.g. antibiotics, insulin, immunosuppressants). Android aggressively optimizes battery consumption via Doze mode, App Standby buckets, and manufacturer-specific task killers. Furthermore, system reboots, manual time shifts, daylight saving time (DST) transitions, and crossing timezones can invalidate registered operating system alarms.

## Decision
1. **Tiered Scheduling**:
   - **`AlarmManager` with Exact Alarms (`setExactAndAllowWhileIdle()`)**: Exclusively used for time-critical dose reminders.
   - **`WorkManager`**: Exclusively used for non-time-critical deferrable background tasks (e.g. daily low-stock evaluations, database maintenance).
2. **Permission Model**:
   - Request `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`.
   - On Android 12+ (API 31+), check `AlarmManager.canScheduleExactAlarms()` before scheduling. If revoked, alert the user via an in-app banner directing them to system settings, and fall back gracefully to inexact alarms.
3. **Comprehensive Reconciliation**:
   - A dedicated `BootAndChangeReceiver` listens for:
     - `android.intent.action.BOOT_COMPLETED`
     - `android.intent.action.TIMEZONE_CHANGED`
     - `android.intent.action.TIME_SET`
     - `android.intent.action.MY_PACKAGE_REPLACED`
   - Upon receiving any trigger, it queries active schedules from Room and reschedules all alarm occurrences for the next 7-day rolling window.
4. **Timezone Policy**:
   - Wall-clock schedules (e.g. "Take at 08:00 AM every day") adapt to local time after timezone change.
   - Interval-based schedules (e.g. "Every 8 hours") maintain absolute elapsed time across timezones.

## Alternatives Considered
1. **WorkManager for Reminders**: PeriodicWorkRequest cannot guarantee exact minute-level execution (minimum periodic interval is 15 minutes, with arbitrary OS deferrals). Unsuitable for clinical medication reminders.
2. **Foreground Service with Persistent Notification**: High battery drain, intrusive notification persistent in the status bar, and easily terminated by OEM battery monitors.

## Consequences
- **Positive**: Guaranteed timely alerts even in Doze mode; automatic recovery after reboots, time changes, or timezone shifts; transparent battery health compliance.
- **Negative**: Requires handling Android 12+ exact alarm permission revocation; requires maintaining alarm receiver broadcast registrations.
