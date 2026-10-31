package com.example.nuvilo.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NoveloDatabaseMigrationTest {

    private val dbName = "migration_test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration1To2_preservesExistingDataAndAddsLotTrackingAndDoseCorrections() {
        // Step 1: Create Version 1 database and insert Schema 1 data
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create Schema 1 tables
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `medications` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `strength` TEXT NOT NULL,
                            `form` TEXT NOT NULL,
                            `stock_unit` TEXT NOT NULL,
                            `opening_stock_raw` INTEGER NOT NULL,
                            `low_stock_threshold_raw` INTEGER NOT NULL,
                            `recording_mode` TEXT NOT NULL,
                            `notes` TEXT NOT NULL,
                            `color_tag_hex` TEXT NOT NULL,
                            `is_archived` INTEGER NOT NULL,
                            `created_at_epoch_ms` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_is_archived` ON `medications` (`is_archived`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_name` ON `medications` (`name`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `schedules` (
                            `id` TEXT NOT NULL,
                            `medication_id` TEXT NOT NULL,
                            `schedule_type` TEXT NOT NULL,
                            `target_times_csv` TEXT NOT NULL,
                            `days_of_week_csv` TEXT NOT NULL,
                            `dose_quantity_raw` INTEGER NOT NULL,
                            `start_date_epoch_day` INTEGER NOT NULL,
                            `end_date_epoch_day` INTEGER,
                            `instructions` TEXT NOT NULL,
                            PRIMARY KEY(`id`),
                            FOREIGN KEY(`medication_id`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedules_medication_id` ON `schedules` (`medication_id`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `dose_events` (
                            `occurrence_id` TEXT NOT NULL,
                            `medication_id` TEXT NOT NULL,
                            `scheduled_epoch_ms` INTEGER NOT NULL,
                            `dose_quantity_raw` INTEGER NOT NULL,
                            `unit` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `recorded_epoch_ms` INTEGER,
                            `notes` TEXT,
                            PRIMARY KEY(`occurrence_id`),
                            FOREIGN KEY(`medication_id`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dose_events_occurrence_id` ON `dose_events` (`occurrence_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_dose_events_medication_id` ON `dose_events` (`medication_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_dose_events_scheduled_epoch_ms` ON `dose_events` (`scheduled_epoch_ms`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `stock_ledger_entries` (
                            `id` TEXT NOT NULL,
                            `medication_id` TEXT NOT NULL,
                            `timestamp_epoch_ms` INTEGER NOT NULL,
                            `movement_type` TEXT NOT NULL,
                            `quantity_raw` INTEGER NOT NULL,
                            `unit` TEXT NOT NULL,
                            `reason` TEXT,
                            `dose_occurrence_id` TEXT,
                            `restock_lot_number` TEXT,
                            `is_estimate` INTEGER NOT NULL,
                            PRIMARY KEY(`id`),
                            FOREIGN KEY(`medication_id`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_entries_medication_id` ON `stock_ledger_entries` (`medication_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_entries_timestamp_epoch_ms` ON `stock_ledger_entries` (`timestamp_epoch_ms`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_entries_dose_occurrence_id` ON `stock_ledger_entries` (`dose_occurrence_id`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v1Db = factory.create(config).writableDatabase

        // Insert test records into V1 tables
        v1Db.execSQL(
            "INSERT INTO medications VALUES ('med_v1', 'Atorvastatin', '20 mg', 'TABLET', 'TABLETS', 30000, 7000, 'CONFIRM', 'Take at bedtime', '#005EB8', 0, 1000000)"
        )
        v1Db.execSQL(
            "INSERT INTO schedules VALUES ('sch_v1', 'med_v1', 'DAILY', '1200', '', 1000, 20000, NULL, 'With water')"
        )
        v1Db.execSQL(
            "INSERT INTO dose_events VALUES ('occ_v1_1', 'med_v1', 1000000, 1000, 'TABLETS', 'TAKEN', 1000050, 'Taken on time')"
        )
        v1Db.execSQL(
            "INSERT INTO stock_ledger_entries VALUES ('entry_v1_1', 'med_v1', 1000000, 'OPENING_BALANCE', 30000, 'TABLETS', 'Initial', NULL, NULL, 0)"
        )

        // Step 2: Run MIGRATION_1_2
        NoveloDatabase.MIGRATION_1_2.migrate(v1Db)

        // Step 3: Validate V1 data preserved
        val medCursor = v1Db.query("SELECT * FROM medications WHERE id = 'med_v1'")
        assertTrue(medCursor.moveToFirst())
        assertEquals("Atorvastatin", medCursor.getString(medCursor.getColumnIndexOrThrow("name")))
        medCursor.close()

        val schCursor = v1Db.query("SELECT * FROM schedules WHERE id = 'sch_v1'")
        assertTrue(schCursor.moveToFirst())
        assertEquals("1200", schCursor.getString(schCursor.getColumnIndexOrThrow("target_times_csv")))
        schCursor.close()

        // Validate dose_events has new nullable columns and old data intact
        val doseCursor = v1Db.query("SELECT * FROM dose_events WHERE occurrence_id = 'occ_v1_1'")
        assertTrue(doseCursor.moveToFirst())
        assertEquals("TAKEN", doseCursor.getString(doseCursor.getColumnIndexOrThrow("status")))
        assertTrue(doseCursor.isNull(doseCursor.getColumnIndexOrThrow("correction_notes")))
        assertTrue(doseCursor.isNull(doseCursor.getColumnIndexOrThrow("corrected_at_epoch_ms")))
        doseCursor.close()

        // Validate stock_ledger_entries has new nullable lot_id column
        val ledgerCursor = v1Db.query("SELECT * FROM stock_ledger_entries WHERE id = 'entry_v1_1'")
        assertTrue(ledgerCursor.moveToFirst())
        assertEquals("OPENING_BALANCE", ledgerCursor.getString(ledgerCursor.getColumnIndexOrThrow("movement_type")))
        assertTrue(ledgerCursor.isNull(ledgerCursor.getColumnIndexOrThrow("lot_id")))
        ledgerCursor.close()

        // Step 4: Validate new Schema 2 operations succeed
        // 4a. Update dose_events with correction columns
        v1Db.execSQL(
            "UPDATE dose_events SET correction_notes = 'Test correction', corrected_at_epoch_ms = 1005000 WHERE occurrence_id = 'occ_v1_1'"
        )
        val updatedDoseCursor = v1Db.query("SELECT * FROM dose_events WHERE occurrence_id = 'occ_v1_1'")
        assertTrue(updatedDoseCursor.moveToFirst())
        assertEquals("Test correction", updatedDoseCursor.getString(updatedDoseCursor.getColumnIndexOrThrow("correction_notes")))
        assertEquals(1005000L, updatedDoseCursor.getLong(updatedDoseCursor.getColumnIndexOrThrow("corrected_at_epoch_ms")))
        updatedDoseCursor.close()

        // 4b. Insert into stock_lots
        v1Db.execSQL(
            "INSERT INTO stock_lots VALUES ('lot_1', 'med_v1', 'BATCH-99', 28000, 28000, 'TABLETS', 1850000000000, 1006000, 'Pharmacy lot')"
        )
        val lotCursor = v1Db.query("SELECT * FROM stock_lots WHERE id = 'lot_1'")
        assertTrue(lotCursor.moveToFirst())
        assertEquals("BATCH-99", lotCursor.getString(lotCursor.getColumnIndexOrThrow("lot_number")))
        assertEquals(28000L, lotCursor.getLong(lotCursor.getColumnIndexOrThrow("initial_quantity_raw")))
        lotCursor.close()

        // 4c. Insert into stock_ledger_entries with lot_id
        v1Db.execSQL(
            "INSERT INTO stock_ledger_entries (id, medication_id, timestamp_epoch_ms, movement_type, quantity_raw, unit, reason, dose_occurrence_id, restock_lot_number, is_estimate, lot_id) VALUES ('entry_v2_1', 'med_v1', 1006000, 'RESTOCK', 28000, 'TABLETS', 'Restock', NULL, 'BATCH-99', 0, 'lot_1')"
        )
        val newLedgerCursor = v1Db.query("SELECT * FROM stock_ledger_entries WHERE id = 'entry_v2_1'")
        assertTrue(newLedgerCursor.moveToFirst())
        assertEquals("lot_1", newLedgerCursor.getString(newLedgerCursor.getColumnIndexOrThrow("lot_id")))
        newLedgerCursor.close()

        // Step 5: Run MIGRATION_2_3 on top of V2 database
        NoveloDatabase.MIGRATION_2_3.migrate(v1Db)

        // Verify V1 & V2 data still preserved after 2 -> 3 migration
        val medAfterV3 = v1Db.query("SELECT * FROM medications WHERE id = 'med_v1'")
        assertTrue(medAfterV3.moveToFirst())
        assertEquals("Atorvastatin", medAfterV3.getString(medAfterV3.getColumnIndexOrThrow("name")))
        medAfterV3.close()

        val lotAfterV3 = v1Db.query("SELECT * FROM stock_lots WHERE id = 'lot_1'")
        assertTrue(lotAfterV3.moveToFirst())
        assertEquals("BATCH-99", lotAfterV3.getString(lotAfterV3.getColumnIndexOrThrow("lot_number")))
        lotAfterV3.close()

        // Verify new V3 tables (prescriptions & prescription_events) work properly
        v1Db.execSQL(
            """
            INSERT INTO prescriptions (
                id, medication_id, reference_number, issue_date_epoch_day, expiry_date_epoch_day,
                quantity_prescribed_raw, unit, repeats_authorised, repeats_remaining,
                has_pending_repeat_request, pharmacy_name, notes, status,
                created_at_epoch_ms, updated_at_epoch_ms
            ) VALUES (
                'rx_v3_1', 'med_v1', 'RX-100', 20000, 20365,
                28000, 'TABLETS', 4, 4,
                0, 'Community Pharmacy', 'Repeat every 28 days', 'ACTIVE',
                1010000, 1010000
            )
            """.trimIndent()
        )
        v1Db.execSQL(
            """
            INSERT INTO prescription_events (
                id, prescription_id, medication_id, event_type, timestamp_epoch_ms,
                repeats_remaining_after, action_idempotency_key, notes
            ) VALUES (
                'rx_ev_1', 'rx_v3_1', 'med_v1', 'CREATED', 1010000,
                4, 'init_key_1', 'Created in V3 migration test'
            )
            """.trimIndent()
        )

        val rxCursor = v1Db.query("SELECT * FROM prescriptions WHERE id = 'rx_v3_1'")
        assertTrue(rxCursor.moveToFirst())
        assertEquals("RX-100", rxCursor.getString(rxCursor.getColumnIndexOrThrow("reference_number")))
        assertEquals(4, rxCursor.getInt(rxCursor.getColumnIndexOrThrow("repeats_remaining")))
        rxCursor.close()

        val rxEvCursor = v1Db.query("SELECT * FROM prescription_events WHERE id = 'rx_ev_1'")
        assertTrue(rxEvCursor.moveToFirst())
        assertEquals("CREATED", rxEvCursor.getString(rxEvCursor.getColumnIndexOrThrow("event_type")))
        assertEquals(4, rxEvCursor.getInt(rxEvCursor.getColumnIndexOrThrow("repeats_remaining_after")))
        rxEvCursor.close()

        v1Db.close()
    }
}
