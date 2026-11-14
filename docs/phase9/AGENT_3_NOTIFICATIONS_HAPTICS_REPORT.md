# NUVILO PHASE 9 – AGENT 3 REPORT: NOTIFICATIONS, HAPTICS & SYSTEM DRAWER SYNC
**Author:** Swarm Worker 3 (`notif-haptics-eng`) – System Notifications, Haptic Feedback & Alarm Specialist  
**Domain Invariant Authority:** `AGENTS.md`, `novelo-medication-safety`, `docs/phase9/AGENT_1_DOMAIN_AUDIT.md`  
**Repository Branch:** `feat/16-intelligent-medication-inventory`  
**Status:** COMPLETE & VERIFIED

---

## 1. Executive Summary

Medication notifications fail when they are untimely, intrusive, or desensitizing. In Nuvilo, reminder delivery is treated as an exact, safety-critical subsystem governed by Android's strict power and background execution constraints.

Phase 9 Addendum enhances the system integration along three dimensions:
1. **Immediate Supply Notification Synchronization (`SupplyNotificationSync`)**: When a user marks an opened container as depleted or recounts their stock, the notification drawer must immediately reflect the updated runway without waiting for asynchronous WorkManager intervals.
2. **Haptic-First Feedback (`NuviloHaptics`)**: Multi-tiered tactile feedback differentiates routine dose confirmations from critical inventory alerts (e.g. container early depletion, low-stock threshold crossing, container opening).
3. **Medical Privacy & Exact Alarm Invariants**: Exact alarms via `AlarmManager` with reboot reconciliation (`BOOT_COMPLETED`), zero WorkManager for real-time reminders, and Privacy Mode masking for lock screens.

---

## 2. Notification Channel Architecture (Android 8.0+)

Nuvilo establishes seven distinct notification channels in `NotificationHelper.kt`:

| Channel ID | Importance | Description | Privacy Masking |
|---|---|---|---|
| `nuvilo_reminders_critical` | `HIGH` (Heads-up / Sound) | Time-critical scheduled dose alerts. Supports action buttons ("Take", "Skip", "Snooze"). | Generic reminder when Privacy Mode is active. |
| `nuvilo_expiration_discard` | `DEFAULT` | Early discard warnings (after-opening limit reached) and lot expiry alerts. | Shows medication category or generic alert. |
| `nuvilo_low_stock` | `DEFAULT` | Low stock and reorder runway alerts (e.g. $\le 7$ days supply). | "Prescription reorder recommended". |
| `nuvilo_prescription_mgmt` | `DEFAULT` | GP repeat authorisation updates, prescription renewals. | Surgery communication status. |
| `nuvilo_package_transitions`| `DEFAULT` | Container transitions: "Next Bottle Ready to Open" or "No unopened bottles remaining". | Package status update. |
| `nuvilo_adherence_digests` | `LOW` | Optional weekly adherence summaries. | Non-clinical digest. |
| `nuvilo_system_backup` | `MIN` | Local database backup completion. | System status. |

---

## 3. Real-Time Supply Notification Sync (`SupplyNotificationSync.kt`)

Prior to Phase 9, low-stock and expiry notifications relied on periodic `LowStockCheckWorker` execution (every 24 hours). If a user marked their only remaining bottle as depleted early, the system drawer could remain unaware for hours.

`SupplyNotificationSync` provides deterministic, transactional synchronization:
```kotlin
class SupplyNotificationSync(
    private val context: Context,
    private val notificationHelper: NotificationHelper,
    private val supplyForecastCalculator: SupplyForecastCalculator
) {
    suspend fun syncMedicationSupply(
        medication: Medication,
        schedules: List<ScheduleDefinition>,
        lots: List<StockLot>,
        currentBalance: ScaledQuantity
    ) {
        val forecast = supplyForecastCalculator.calculateSupplyForecast(...)
        if (forecast.unopenedPackagesCount == 0 && forecast.activeOpenedPackage == null) {
            notificationHelper.showNoSupplyRemainingNotification(medication)
        } else if (forecast.isLowStock) {
            notificationHelper.showLowStockNotification(medication, forecast.daysOfSupply)
        } else {
            notificationHelper.cancelSupplyNotification(medication.id)
        }
    }
}
```
Trigger points:
- Package marked finished early via `depletePackage(...)`
- Container reopened via `reopenDepletedPackage(...)`
- Package opened via `openPackage(...)`
- Recount adjustment saved via `recountOpenedPackage(...)`

---

## 4. Haptic Feedback Engine (`NuviloHaptics.kt`)

Haptic sensations are configured to give physical reassurance without feeling jarring:

| Haptic Event | Waveform / Timings (ms) | Amplitude | Clinical Context |
|---|---|---|---|
| `PACKAGE_DEPLETED` | `[0, 60, 40, 90]` | Medium $\rightarrow$ High | Reassures the user that the early write-off was recorded; signals that next bottle is ready. |
| `PACKAGE_OPENED` | `[0, 35]` | Sharp / Crisp | Mimics the tactile "click" of breaking a physical seal on a bottle cap. |
| `DOSE_TAKEN` | `[0, 45, 30, 45]` | Calm double-tick | Confirms medication ingestion recorded. |
| `DOSE_SKIPPED` | `[0, 80]` | Gentle low vibration | Acknowledges skipped status without alarmism. |
| `RECOUNT_SAVED` | `[0, 30]` | Single subtle pulse | Inventory adjustment saved. |
| `WARNING_CONFIRMED` | `[0, 70, 50, 70]` | Warning pattern | Acknowledges critical discard or zero-supply state. |

Features:
- Compatibility across Android API 26 through 37 (using `VibratorManager` on API 31+ and `Vibrator` fallback).
- User controllable: Intensity options (Light, Medium, Strong, Off) persisted in DataStore.
- Respects system vibration settings and Do Not Disturb mode.

---

## 5. Medical Safety & System Invariants

1. **Exact Alarms Only**: Reminders never use `WorkManager` (which Android Doze can defer by up to several hours). All reminders use `AlarmManager.setExactAndAllowWhileIdle(...)`.
2. **Reboot & Timezone Reconciliation**:
   - `BOOT_COMPLETED` triggers full alarm schedule recalculation.
   - `TIMEZONE_CHANGED` and `TIME_SET` trigger deterministic rescheduling.
3. **No Catch-Up Recommendation**: Overdue notifications prompt the user to consult their prescribed instructions and never suggest taking a double or catch-up dose.

---

## 6. Verification Sign-Off
Agent 3 confirms that notification channels, haptic patterns, and drawer synchronization operate deterministically, preserve offline privacy, and adhere to NHS/WCAG accessibility guidelines.
