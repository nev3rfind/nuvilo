# Phase 9 Database & Backup Migration Plan (v3 -> v4)

**Date:** 2026-11-12  
**Status:** Schema & Compatibility Specification (Stage A)

---

## 1. Room Schema Changes (`NoveloDatabase` v3 -> v4)

All schema changes are strictly **additive** (`ALTER TABLE ... ADD COLUMN ...`) to guarantee zero data loss for existing users.

### 1.1 `medications` Table Additions
| Column | SQL Type | Default | Purpose |
|---|---|---|---|
| `package_type` | `TEXT` (nullable) | `NULL` | Purchase container type (`PackageType.name`) |
| `contents_per_package_raw` | `INTEGER` (nullable) | `NULL` | Configured usable consumption units per sealed package |
| `net_content_label` | `TEXT` (nullable) | `NULL` | Optional label text e.g. `"10 ml"`, `"5 g"` |
| `discard_after_opening_days` | `INTEGER` (nullable) | `NULL` | Post-opening shelf life in days (e.g. `28`) |
| `tracking_precision` | `TEXT NOT NULL` | `'UNKNOWN'` | `EXACT`, `ESTIMATED`, `APPROXIMATE`, `UNKNOWN` (backfilled conservatively) |
| `gel_tracking_mode` | `TEXT` (nullable) | `NULL` | `GRAMS`, `ESTIMATED_APPLICATIONS`, `TUBE_COUNT_ONLY` |
| `administration_site` | `TEXT NOT NULL` | `'NONE'` | `NONE`, `LEFT_EYE`, `RIGHT_EYE`, `BOTH_EYES`, `CUSTOM` |
| `left_eye_quantity_raw` | `INTEGER` (nullable) | `NULL` | Left eye dose in consumption units |
| `right_eye_quantity_raw` | `INTEGER` (nullable) | `NULL` | Right eye dose in consumption units |
| `containers_per_admin_raw` | `INTEGER` (nullable) | `NULL` | Single-dose containers consumed per admin regardless of eyes treated |
| `low_stock_days_threshold` | `INTEGER` (nullable) | `NULL` | Optional days-remaining warning threshold |

### 1.2 `stock_lots` Table Additions
| Column | SQL Type | Default | Purpose |
|---|---|---|---|
| `package_type` | `TEXT` (nullable) | `NULL` | Package container type for this lot |
| `package_label` | `TEXT` (nullable) | `NULL` | Human-readable package label (`"Bottle #1"`) |
| `package_status` | `TEXT NOT NULL` | `'UNOPENED'` | `UNOPENED`, `OPENED`, `EMPTY`, `EXPIRED`, `DISCARDED` |
| `opened_at_epoch_ms` | `INTEGER` (nullable) | `NULL` | Timestamp when opened (`OPENED`) |
| `discard_after_opening_days` | `INTEGER` (nullable) | `NULL` | Lot-level override / snapshot from medication config |
| `discard_after_opening_epoch_ms` | `INTEGER` (nullable) | `NULL` | Computed discard cutoff timestamp once opened |
| `discarded_at_epoch_ms` | `INTEGER` (nullable) | `NULL` | Timestamp when marked `DISCARDED` |
| `discard_reason` | `TEXT` (nullable) | `NULL` | Audit note explaining discard |

---

## 2. Deterministic Backfill During `MIGRATION_3_4`

Inside `MIGRATION_3_4 = object : Migration(3, 4)`:
1. Add the 11 nullable/defaulted columns to `medications`.
2. Update `tracking_precision` on migrated medications:
   - If `form IN ('DROPS', 'INHALER_PUFFS', 'TOPICAL_CREAM')` -> `'ESTIMATED'`
   - Else -> `'EXACT'`
   - Note: Never fabricate fake package capacities (`contents_per_package_raw` stays `NULL` until configured by the user).
3. Add the 8 columns to `stock_lots`.
4. Infer safe `package_status` for existing pre-v4 lots:
   - If `remaining_quantity_raw <= 0` -> `'EMPTY'`
   - Else if `expiry_date_epoch_ms IS NOT NULL AND expiry_date_epoch_ms <= <migrationNowMs>` -> `'EXPIRED'`
   - Else if `remaining_quantity_raw < initial_quantity_raw` -> `'OPENED'` (with `opened_at_epoch_ms = created_at_epoch_ms`) — because partial consumption proves the package was already opened!
   - Else -> `'UNOPENED'` (full untouched lot).

This guarantees:
- Existing medications, schedules, dose events, stock lots, ledger entries, and prescriptions migrate cleanly with zero dropped rows.
- Existing Hydro Forte / tablet / inhaler / prescription records remain readable and actionable immediately, with gentle in-UI prompts to configure packaging details when beneficial.

---

## 3. Encrypted JSON & CSV ZIP Backup Compatibility

1. Bump `NoveloDataBackup.CURRENT_SCHEMA_VERSION = 4` (while keeping `CURRENT_FORMAT_VERSION = 1`).
2. `BackupValidator`: Accepts `schemaVersion in 1..CURRENT_SCHEMA_VERSION`.
3. `JsonBackupManager`:
   - **Serialize**: Writes all new Phase 9 fields onto medication and stock lot JSON objects when present (and always includes defaults for status/precision/site).
   - **Deserialize**: Uses `optString` / `has && !isNull` so **v3 encrypted/plaintext JSON backups** deserialize cleanly into v4 `MedicationEntity` and `StockLotEntity` records without throwing `JSONException`.
4. `CsvZipBackupManager`:
   - **Export**: Appends new columns at the end of `medications.csv` and `stock_lots.csv`.
   - **Import**: Reads header columns by name with graceful fallback (`null` / safe defaults) when restoring older v3 CSV ZIP exports lacking the new columns.
5. `BackupRestoreRepositoryImpl.executeRestore()`:
   - Still executes in an atomic `database.withTransaction { ... }` block.
   - Schema export: Commit `app/schemas/4.json` alongside `1.json`, `2.json`, `3.json`.

---

## 4. Verification Plan for Migration & Backups
- Automated Room `MigrationTestHelper` tests in `NoveloDatabaseMigrationTest.kt`:
  - `migrate_3_to_4_preservesAllDataAndInfersPackageStates`: Inserts active, partial, empty, and expired lots + drop/tablet medications + ledger + doses + prescriptions in v3; runs `MIGRATION_3_4`; verifies `validateMigrations`, exact row preservation, inferred statuses (`EMPTY`, `EXPIRED`, `OPENED`, `UNOPENED`), and `OPENING_BALANCE`/ledger totals.
  - Full chain test `migrate_1_to_4_succeeds`.
- Backup unit tests:
  - Restore a legacy **v3 JSON** and **v3 CSV ZIP** backup into a **v4** repository/database cleanly.
  - Round-trip a **v4 encrypted JSON** and **v4 CSV ZIP** backup containing eye-drop packaging, opened lot timestamps, discard-after-opening cutoffs, and discarded lot audit fields.
