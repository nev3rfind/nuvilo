# NHS Prescription Prepayment Certificate (PPC) & Exemption Architecture

## Overview
Nuvilo provides strictly offline-first tracking for NHS **Prescription Prepayment Certificates (PPC)** and **Free NHS Prescription Exemptions**. All certificate dates, coverage types, reminders, and historical records are stored locally in the encrypted-at-rest Room database (`novelo.db`, schema v5) and included in local JSON/CSV backups.

> **Important Scope & Legal Notice**: Nuvilo never connects to NHSBSA or any external network service (`android.permission.INTERNET` is forbidden). Every status badge, countdown, and reminder is explicitly labelled **"Based on your recorded certificate dates"**.

---

## Layer Breakdown

### 1. Domain Layer (`domain/ppc/`)
- **`PpcModels.kt`**:
  - `PpcCoverageType`: `PPC_3_MONTH`, `PPC_12_MONTH`, `FREE_EXEMPTION`
  - `ExemptionCategory`: `MEDICAL`, `MATERNITY`, `HC2`, `AGE_RELATED`, `QUALIFYING_BENEFITS`, `OTHER`
  - `PpcStatus`: `ACTIVE`, `EXPIRING_SOON`, `EXPIRED`, `NOT_YET_VALID`, `NO_RECORD`
  - `PpcReminderConfig`: Configurable offsets (30 days, 14 days, 7 days before expiry, and on expiry day) at local time (default `09:00`).
  - `PpcCertificate`: Immutable domain model with `startDate`, `expiryDate`, `isOngoing`, `isManualExpiryOverride`, `referenceNumber`, `notes`, `isArchived`, and `renewedFromId`.
  - `PpcEvaluation`: Evaluated status, `remainingDays`, `daysUntilStart`, `effectiveExpiryDate`, and `statusBasisNotice` (`"Based on your recorded certificate dates"`).
- **`PpcCalculator.kt`**:
  - Pure Kotlin `java.time.LocalDate` calendar-month arithmetic (`startDate.plusMonths(N).minusDays(1)`).
  - Handles leap years, month-end clamping, year transitions, future start dates, ongoing exemptions without expiry dates, manual expiry overrides, overlap detection (`findOverlaps`), primary certificate selection (`selectPrimaryEvaluation`), and renewal draft generation (`buildRenewalDraft`).

### 2. Data Layer (`data/local/`, `data/repository/`, `data/backup/`)
- **`PpcCertificateEntity`** (`ppc_certificates` table in Room v5): Stores epoch-day `Long` dates (`start_date_epoch_day`, `expiry_date_epoch_day`), reminder flags, archival status, and `renewed_from_id` lineage.
- **`PpcDao`**: Reactive `Flow` queries for active and all certificates, plus CRUD and archival operations.
- **`NoveloRepositoryImpl`**: Maps between `PpcCertificateEntity` and `PpcCertificate` and coordinates transactional renewal (`renewPpcCertificate`).
- **Backup & Restore (`data/backup/`)**:
  - `NoveloDataBackup` includes `ppcCertificates: List<PpcCertificateEntity>`.
  - `JsonBackupManager` and `CsvZipBackupManager` (`ppc_certificates.csv`) serialize and deserialize PPC records while maintaining full backward compatibility with pre-v5 backups.

### 3. System & Alarm Layer (`system/alarm/PpcAlarmScheduler.kt`)
- `PpcAlarmSchedulerImpl` uses `AlarmManager` (`setExactAndAllowWhileIdle` with graceful `setAndAllowWhileIdle` fallback) and deterministic request codes (`"ppc_alarm_${certificateId}_$offsetDays".hashCode()`) to prevent duplicate alarms.
- Cancels stale alarms on edit, renewal, archive, or delete, and reconciles all alarms on boot, app update, timezone change, and manual time change.

### 4. UI Layer (`ui/screens/PpcManagementScreen.kt`)
- Dedicated `PpcManagementScreen` with:
  - Current status hero card & remaining days countdown
  - 4-step progressive creation/edit wizard (Coverage Type -> Dates & Duration -> Reference, Notes & Reminders -> Review & Overlap Check)
  - Historical & archived certificate management (Edit, Mark Renewed, Archive/Restore, Delete with confirmation)
  - Dashboard integration via `DashboardUiState.ppcEvaluation`
