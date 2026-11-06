package com.example.nuvilo.regression

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.domain.model.MovementType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Regression Test Suite: Category B (Room, Repositories & Migrations)
 * Tests T066 to T073: Database Integrity Constraints and Invariants
 */
@RunWith(AndroidJUnit4::class)
class DatabaseInvariantsRegressionTest {

    private lateinit var db: NoveloDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = NoveloDatabase.createInMemory(context)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun sampleMed(id: String = "med_base") = MedicationEntity(
        id = id,
        name = "Metformin",
        strength = "500mg",
        form = "TABLET",
        stockUnit = "TABLETS",
        openingStockRaw = 60000L,
        lowStockThresholdRaw = 14000L,
        recordingMode = "CONFIRM",
        notes = "",
        colorTagHex = "#005EB8",
        isArchived = false,
        createdAtEpochMs = 1700000000000L
    )

    @Test
    fun t066_primaryKeyCollisionRejection() {
        runBlocking {
            // T066: Nonce / primary key collision rejection in SQLite schema
            val med = sampleMed("med_dup")
            db.medicationDao().insertMedication(med)

            // Direct SQL insert without REPLACE triggers primary key SQLiteConstraintException
            assertThrows(SQLiteConstraintException::class.java) {
                db.openHelper.writableDatabase.execSQL(
                    "INSERT INTO medications (id, name, strength, form, stock_unit, opening_stock_raw, low_stock_threshold_raw, recording_mode, notes, color_tag_hex, is_archived, created_at_epoch_ms) " +
                    "VALUES ('med_dup', 'Duplicate', '10mg', 'TABLET', 'TABLETS', 1000, 100, 'CONFIRM', '', '#005EB8', 0, 1000)"
                )
            }
        }
    }

    @Test
    fun t067_foreignKeyConstraintEnforcementBetweenScheduleAndMedication() {
        runBlocking {
            // T067: Foreign key constraint enforcement between schedule and medication
            val orphanSchedule = ScheduleEntity(
                id = "sch_orphan",
                medicationId = "non_existent_med_id",
                scheduleType = "DAILY",
                targetTimesCsv = "480",
                daysOfWeekCsv = "1,2,3,4,5,6,7",
                doseQuantityRaw = 1000L,
                startDateEpochDay = 19500L,
                endDateEpochDay = null,
                instructions = ""
            )

            assertThrows(SQLiteConstraintException::class.java) {
                runBlocking {
                    db.scheduleDao().insertSchedule(orphanSchedule)
                }
            }
        }
    }

    @Test
    fun t068_doseEventUniqueOccurrenceIdConstraintPreventsDuplicateDoses() {
        runBlocking {
            // T068: Dose event unique occurrence ID constraint prevents duplicate entries
            db.medicationDao().insertMedication(sampleMed("med_dose_unique"))

            val dose1 = DoseEventEntity(
                occurrenceId = "occ_fixed_123",
                medicationId = "med_dose_unique",
                scheduledEpochMs = 1700000000000L,
                doseQuantityRaw = 1000L,
                unit = "TABLETS",
                status = "TAKEN"
            )
            db.doseDao().insertDoseEvent(dose1)

            val duplicateDose = dose1.copy(notes = "Different note")
            assertThrows(SQLiteConstraintException::class.java) {
                runBlocking {
                    db.doseDao().insertDoseEvent(duplicateDose)
                }
            }
        }
    }

    @Test
    fun t069_stockLedgerMovementTypesEnumSerialization() {
        runBlocking {
            // T069: Stock ledger movement types enum serialization in SQLite
            db.medicationDao().insertMedication(sampleMed("med_movements"))

            val types = listOf(
                MovementType.OPENING_BALANCE,
                MovementType.RESTOCK,
                MovementType.DOSE_TAKEN,
                MovementType.DOSE_ASSUMED,
                MovementType.DOSE_REVERSAL,
                MovementType.ADJUSTMENT_IN,
                MovementType.ADJUSTMENT_OUT,
                MovementType.WASTAGE
            )

            types.forEachIndexed { index, mType ->
                val entry = StockLedgerEntryEntity(
                    id = "entry_$index",
                    medicationId = "med_movements",
                    timestampEpochMs = 1700000000000L + index,
                    movementType = mType.name,
                    quantityRaw = 1000L,
                    unit = "TABLETS"
                )
                db.stockLedgerDao().insertMovement(entry)
            }

            val entries = db.stockLedgerDao().observeMovementsForMedication("med_movements").first()
            assertEquals(8, entries.size)
            types.forEach { expectedType ->
                assertTrue(entries.any { it.movementType == expectedType.name })
            }
        }
    }

    @Test
    fun t070_scaledQuantityRawLongStorageFidelityWithoutBitLoss() {
        runBlocking {
            // T070: ScaledQuantity raw Long storage fidelity in Room
            val largeRaw = 9_000_000_000_000L // 9 billion raw = 9 million units
            val med = sampleMed("med_large_raw").copy(openingStockRaw = largeRaw)
            db.medicationDao().insertMedication(med)

            val retrieved = db.medicationDao().getMedicationById("med_large_raw")
            assertNotNull(retrieved)
            assertEquals(largeRaw, retrieved?.openingStockRaw)
        }
    }

    @Test
    fun t071_prescriptionStatusStringConversion() {
        runBlocking {
            // T071: Prescription status string conversion in Room
            db.medicationDao().insertMedication(sampleMed("med_rx_status"))

            val rx = PrescriptionEntity(
                id = "rx_status_test",
                medicationId = "med_rx_status",
                issueDateEpochDay = 19500L,
                repeatsAuthorised = 1,
                repeatsRemaining = 0,
                status = "COMPLETED",
                createdAtEpochMs = 1700000000000L,
                updatedAtEpochMs = 1700000000000L
            )
            db.prescriptionDao().insertPrescription(rx)

            val retrieved = db.prescriptionDao().observePrescriptionsForMedication("med_rx_status").first()
            assertEquals(1, retrieved.size)
            assertEquals("COMPLETED", retrieved[0].status)
        }
    }

    @Test
    fun t072_foreignKeyRestrictBehaviorPreventsOrphanChildren() {
        runBlocking {
            // T072: Foreign key CASCADE behavior deletes child schedules when parent medication is deleted
            db.medicationDao().insertMedication(sampleMed("med_with_child"))
            val schedule = ScheduleEntity(
                id = "sch_child",
                medicationId = "med_with_child",
                scheduleType = "DAILY",
                targetTimesCsv = "480",
                daysOfWeekCsv = "1,2,3,4,5,6,7",
                doseQuantityRaw = 1000L,
                startDateEpochDay = 19500L,
                instructions = ""
            )
            db.scheduleDao().insertSchedule(schedule)

            val before = db.scheduleDao().getSchedulesForMedication("med_with_child")
            assertEquals(1, before.size)

            // Deleting parent cascades and removes all dependent schedules without orphan rows
            db.openHelper.writableDatabase.execSQL("DELETE FROM medications WHERE id = 'med_with_child'")

            val after = db.scheduleDao().getSchedulesForMedication("med_with_child")
            assertEquals(0, after.size)
        }
    }

    @Test
    fun t073_transactionalAtomicCommitAcrossMultipleDaos() {
        runBlocking {
            // T073: Transactional atomic commit across multiple DAOs rolls back on exception
            val med = sampleMed("med_atomic")

            try {
                db.withTransaction {
                    db.medicationDao().insertMedication(med)
                    throw IllegalStateException("Simulated rollback")
                }
            } catch (e: IllegalStateException) {
                // Expected
            }

            val retrieved = db.medicationDao().getMedicationById("med_atomic")
            assertNull(retrieved)
        }
    }
}
