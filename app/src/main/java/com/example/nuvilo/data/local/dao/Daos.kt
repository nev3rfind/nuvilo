package com.example.nuvilo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE is_archived = 0 ORDER BY name ASC")
    fun observeActiveMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun observeAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: String): MedicationEntity?

    @Query("SELECT * FROM medications WHERE id = :id")
    fun observeMedicationById(id: String): Flow<MedicationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("SELECT * FROM medications")
    suspend fun getAllMedications(): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<MedicationEntity>)

    @Query("DELETE FROM medications")
    suspend fun deleteAllMedications()
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE medication_id = :medicationId")
    suspend fun getSchedulesForMedication(medicationId: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE medication_id = :medicationId")
    fun observeSchedulesForMedication(medicationId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    fun observeAllSchedules(): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE medication_id = :medicationId")
    suspend fun deleteSchedulesForMedication(medicationId: String)

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
}

@Dao
interface DoseDao {
    @Query("SELECT * FROM dose_events WHERE occurrence_id = :occurrenceId")
    suspend fun getDoseByOccurrenceId(occurrenceId: String): DoseEventEntity?

    @Query("SELECT * FROM dose_events WHERE scheduled_epoch_ms BETWEEN :startEpochMs AND :endEpochMs")
    fun observeDosesInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events ORDER BY scheduled_epoch_ms DESC")
    fun observeAllDoses(): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE medication_id = :medicationId ORDER BY scheduled_epoch_ms DESC")
    fun observeDosesForMedication(medicationId: String): Flow<List<DoseEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDoseEvent(dose: DoseEventEntity)

    @Update
    suspend fun updateDoseEvent(dose: DoseEventEntity)

    @Query("SELECT * FROM dose_events")
    suspend fun getAllDoses(): List<DoseEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoses(doses: List<DoseEventEntity>)

    @Query("DELETE FROM dose_events")
    suspend fun deleteAllDoses()
}

@Dao
interface StockLotDao {
    @Query("SELECT * FROM stock_lots WHERE medication_id = :medicationId ORDER BY expiry_date_epoch_ms ASC, created_at_epoch_ms ASC")
    fun observeLotsForMedication(medicationId: String): Flow<List<StockLotEntity>>

    @Query("SELECT * FROM stock_lots WHERE medication_id = :medicationId ORDER BY expiry_date_epoch_ms ASC, created_at_epoch_ms ASC")
    suspend fun getLotsForMedication(medicationId: String): List<StockLotEntity>

    @Query("SELECT * FROM stock_lots WHERE id = :id")
    suspend fun getLotById(id: String): StockLotEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLot(lot: StockLotEntity)

    @Update
    suspend fun updateLot(lot: StockLotEntity)

    @Query("UPDATE stock_lots SET remaining_quantity_raw = :remainingRaw WHERE id = :lotId")
    suspend fun updateRemainingQuantity(lotId: String, remainingRaw: Long)

    @Delete
    suspend fun deleteLot(lot: StockLotEntity)

    @Query("SELECT * FROM stock_lots")
    suspend fun getAllLots(): List<StockLotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLots(lots: List<StockLotEntity>)

    @Query("DELETE FROM stock_lots")
    suspend fun deleteAllLots()
}

@Dao
interface StockLedgerDao {
    @Query("SELECT * FROM stock_ledger_entries WHERE medication_id = :medicationId ORDER BY timestamp_epoch_ms ASC")
    fun observeMovementsForMedication(medicationId: String): Flow<List<StockLedgerEntryEntity>>

    @Query("SELECT * FROM stock_ledger_entries WHERE medication_id = :medicationId ORDER BY timestamp_epoch_ms ASC")
    suspend fun getMovementsForMedication(medicationId: String): List<StockLedgerEntryEntity>

    @Query("SELECT * FROM stock_ledger_entries ORDER BY timestamp_epoch_ms DESC")
    fun observeAllMovements(): Flow<List<StockLedgerEntryEntity>>

    @Query("SELECT COUNT(*) FROM stock_ledger_entries WHERE dose_occurrence_id = :occurrenceId AND movement_type IN ('DOSE_TAKEN', 'DOSE_ASSUMED')")
    suspend fun countDeductionsForOccurrence(occurrenceId: String): Int

    @Query("SELECT COUNT(*) FROM stock_ledger_entries WHERE dose_occurrence_id = :occurrenceId AND movement_type = 'DOSE_REVERSAL'")
    suspend fun countReversalsForOccurrence(occurrenceId: String): Int

    @Query("SELECT * FROM stock_ledger_entries WHERE dose_occurrence_id = :occurrenceId")
    suspend fun getMovementsForOccurrence(occurrenceId: String): List<StockLedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(entry: StockLedgerEntryEntity)

    @Query("SELECT * FROM stock_ledger_entries")
    suspend fun getAllMovements(): List<StockLedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(entries: List<StockLedgerEntryEntity>)

    @Query("DELETE FROM stock_ledger_entries")
    suspend fun deleteAllMovements()
}

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions WHERE medication_id = :medicationId ORDER BY issue_date_epoch_day DESC, created_at_epoch_ms DESC")
    fun observePrescriptionsForMedication(medicationId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE medication_id = :medicationId ORDER BY issue_date_epoch_day DESC, created_at_epoch_ms DESC")
    suspend fun getPrescriptionsForMedication(medicationId: String): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions ORDER BY issue_date_epoch_day DESC, created_at_epoch_ms DESC")
    fun observeAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: String): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Query("SELECT * FROM prescription_events WHERE prescription_id = :prescriptionId ORDER BY timestamp_epoch_ms DESC")
    fun observeEventsForPrescription(prescriptionId: String): Flow<List<PrescriptionEventEntity>>

    @Query("SELECT * FROM prescription_events WHERE prescription_id = :prescriptionId ORDER BY timestamp_epoch_ms DESC")
    suspend fun getEventsForPrescription(prescriptionId: String): List<PrescriptionEventEntity>

    @Query("SELECT * FROM prescription_events WHERE medication_id = :medicationId ORDER BY timestamp_epoch_ms DESC")
    fun observeEventsForMedication(medicationId: String): Flow<List<PrescriptionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrescriptionEvent(event: PrescriptionEventEntity)

    @Query("SELECT * FROM prescriptions")
    suspend fun getAllPrescriptions(): List<PrescriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptions(prescriptions: List<PrescriptionEntity>)

    @Query("DELETE FROM prescriptions")
    suspend fun deleteAllPrescriptions()

    @Query("SELECT * FROM prescription_events")
    suspend fun getAllPrescriptionEvents(): List<PrescriptionEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptionEvents(events: List<PrescriptionEventEntity>)

    @Query("DELETE FROM prescription_events")
    suspend fun deleteAllPrescriptionEvents()
}
