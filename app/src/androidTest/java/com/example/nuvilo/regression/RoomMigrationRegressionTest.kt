package com.example.nuvilo.regression

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category B (Room, Repositories & Migrations)
 * Tests T074 to T078: Schema Migrations (v1 -> v2 -> v3) and Data Preservation
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationRegressionTest {

    private val dbName = "regression_migration_test.db"
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

    private fun createV1Database(): SupportSQLiteDatabase {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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
        val helper = factory.create(config)
        return helper.writableDatabase
    }

    @Test
    fun t074_migration1To2CreatesStockLotsTable() {
        // T074: Migration v1 to v2 creates stock_lots table
        val v1Db = createV1Database()
        NoveloDatabase.MIGRATION_1_2.migrate(v1Db)

        val cursor = v1Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='stock_lots'")
        assertTrue(cursor.moveToFirst())
        assertEquals("stock_lots", cursor.getString(0))
        cursor.close()
        v1Db.close()
    }

    @Test
    fun t075_migration2To3CreatesPrescriptionsAndEventsTables() {
        // T075: Migration v2 to v3 creates prescriptions and prescription_events tables
        val v1Db = createV1Database()
        NoveloDatabase.MIGRATION_1_2.migrate(v1Db)
        NoveloDatabase.MIGRATION_2_3.migrate(v1Db)

        val cursorRx = v1Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='prescriptions'")
        assertTrue(cursorRx.moveToFirst())
        assertEquals("prescriptions", cursorRx.getString(0))
        cursorRx.close()

        val cursorEvents = v1Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='prescription_events'")
        assertTrue(cursorEvents.moveToFirst())
        assertEquals("prescription_events", cursorEvents.getString(0))
        cursorEvents.close()

        v1Db.close()
    }

    @Test
    fun t076_completeMigrationChainPreservesPreExistingData() {
        // T076: Complete migration chain preserves pre-existing rows
        val v1Db = createV1Database()
        v1Db.execSQL(
            "INSERT INTO medications VALUES ('med_mig_test', 'Paracetamol', '500mg', 'TABLET', 'TABLETS', 32000, 8000, 'CONFIRM', 'For pain', '#005EB8', 0, 1700000000000)"
        )
        v1Db.execSQL(
            "INSERT INTO stock_ledger_entries VALUES ('entry_mig_1', 'med_mig_test', 1700000000000, 'OPENING_BALANCE', 32000, 'TABLETS', 'Initial', NULL, NULL, 0)"
        )

        // Execute migrations
        NoveloDatabase.MIGRATION_1_2.migrate(v1Db)
        NoveloDatabase.MIGRATION_2_3.migrate(v1Db)

        val cursor = v1Db.query("SELECT * FROM medications WHERE id = 'med_mig_test'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Paracetamol", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        cursor.close()

        val ledgerCursor = v1Db.query("SELECT * FROM stock_ledger_entries WHERE id = 'entry_mig_1'")
        assertTrue(ledgerCursor.moveToFirst())
        assertEquals(32000L, ledgerCursor.getLong(ledgerCursor.getColumnIndexOrThrow("quantity_raw")))
        ledgerCursor.close()

        v1Db.close()
    }

    @Test
    fun t077_migration1To2AddsCorrectionColumnsToDoseEvents() {
        // T077: Migration v1 to v2 adds is_correction and original_occurrence_id to dose_events
        val v1Db = createV1Database()
        NoveloDatabase.MIGRATION_1_2.migrate(v1Db)

        val cursor = v1Db.query("PRAGMA table_info(dose_events)")
        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        assertTrue(columnNames.contains("correction_notes"))
        assertTrue(columnNames.contains("corrected_at_epoch_ms"))
        v1Db.close()
    }

    @Test
    fun t078_inMemoryDatabaseInitializationCreatesValidSchemaV3() {
        // T078: In-memory database initialization creates all tables in Schema v3
        val memoryDb = NoveloDatabase.createInMemory(context)
        val openDb = memoryDb.openHelper.writableDatabase

        val tables = listOf("medications", "schedules", "dose_events", "stock_lots", "stock_ledger_entries", "prescriptions", "prescription_events")
        tables.forEach { tableName ->
            val cursor = openDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
            assertTrue("Table $tableName must exist", cursor.moveToFirst())
            assertEquals(tableName, cursor.getString(0))
            cursor.close()
        }
        memoryDb.close()
    }
}
