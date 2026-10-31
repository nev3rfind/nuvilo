package com.example.nuvilo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nuvilo.data.local.dao.DoseDao
import com.example.nuvilo.data.local.dao.MedicationDao
import com.example.nuvilo.data.local.dao.PrescriptionDao
import com.example.nuvilo.data.local.dao.ScheduleDao
import com.example.nuvilo.data.local.dao.StockLedgerDao
import com.example.nuvilo.data.local.dao.StockLotDao
import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity

@Database(
    entities = [
        MedicationEntity::class,
        ScheduleEntity::class,
        DoseEventEntity::class,
        StockLotEntity::class,
        StockLedgerEntryEntity::class,
        PrescriptionEntity::class,
        PrescriptionEventEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class NoveloDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseDao(): DoseDao
    abstract fun stockLotDao(): StockLotDao
    abstract fun stockLedgerDao(): StockLedgerDao
    abstract fun prescriptionDao(): PrescriptionDao

    companion object {
        const val DATABASE_NAME = "novelo.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_lots` (
                        `id` TEXT NOT NULL,
                        `medication_id` TEXT NOT NULL,
                        `lot_number` TEXT,
                        `initial_quantity_raw` INTEGER NOT NULL,
                        `remaining_quantity_raw` INTEGER NOT NULL,
                        `unit` TEXT NOT NULL,
                        `expiry_date_epoch_ms` INTEGER,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`medication_id`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_lots_medication_id` ON `stock_lots` (`medication_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_lots_expiry_date_epoch_ms` ON `stock_lots` (`expiry_date_epoch_ms`)")

                db.execSQL("ALTER TABLE `stock_ledger_entries` ADD COLUMN `lot_id` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_entries_lot_id` ON `stock_ledger_entries` (`lot_id`)")

                db.execSQL("ALTER TABLE `dose_events` ADD COLUMN `correction_notes` TEXT")
                db.execSQL("ALTER TABLE `dose_events` ADD COLUMN `corrected_at_epoch_ms` INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prescriptions` (
                        `id` TEXT NOT NULL,
                        `medication_id` TEXT NOT NULL,
                        `reference_number` TEXT,
                        `issue_date_epoch_day` INTEGER NOT NULL,
                        `expiry_date_epoch_day` INTEGER,
                        `quantity_prescribed_raw` INTEGER,
                        `unit` TEXT,
                        `repeats_authorised` INTEGER NOT NULL,
                        `repeats_remaining` INTEGER NOT NULL,
                        `has_pending_repeat_request` INTEGER NOT NULL,
                        `pharmacy_name` TEXT,
                        `notes` TEXT,
                        `status` TEXT NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`medication_id`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prescriptions_medication_id` ON `prescriptions` (`medication_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prescriptions_status` ON `prescriptions` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prescriptions_expiry_date_epoch_day` ON `prescriptions` (`expiry_date_epoch_day`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prescription_events` (
                        `id` TEXT NOT NULL,
                        `prescription_id` TEXT NOT NULL,
                        `medication_id` TEXT NOT NULL,
                        `event_type` TEXT NOT NULL,
                        `timestamp_epoch_ms` INTEGER NOT NULL,
                        `repeats_remaining_after` INTEGER NOT NULL,
                        `action_idempotency_key` TEXT,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`prescription_id`) REFERENCES `prescriptions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prescription_events_prescription_id` ON `prescription_events` (`prescription_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prescription_events_medication_id` ON `prescription_events` (`medication_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prescription_events_timestamp_epoch_ms` ON `prescription_events` (`timestamp_epoch_ms`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prescription_events_action_idempotency_key` ON `prescription_events` (`action_idempotency_key`)")
            }
        }

        fun create(context: Context): NoveloDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NoveloDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        }

        fun createInMemory(context: Context): NoveloDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                NoveloDatabase::class.java
            )
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        }
    }
}
