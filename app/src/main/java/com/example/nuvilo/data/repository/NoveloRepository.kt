package com.example.nuvilo.data.repository

import androidx.room.withTransaction
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import com.example.nuvilo.data.mapper.toDomain
import com.example.nuvilo.data.mapper.toEntity
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEvent
import com.example.nuvilo.domain.model.PrescriptionEventType
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.prescription.PrescriptionManager
import com.example.nuvilo.domain.stock.StockLedgerCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface MedicationRepository {
    fun observeActiveMedications(): Flow<List<Medication>>
    fun observeAllMedications(): Flow<List<Medication>>
    fun observeMedication(id: String): Flow<Medication?>
    fun observeAllSchedules(): Flow<List<ScheduleDefinition>>
    suspend fun getMedication(id: String): Medication?
    suspend fun getSchedulesForMedication(medicationId: String): List<ScheduleDefinition>
    suspend fun createMedication(medication: Medication, schedule: ScheduleDefinition): Result<Unit>
    suspend fun updateMedication(medication: Medication): Result<Unit>
    suspend fun setArchived(id: String, archived: Boolean): Result<Unit>
}

interface DoseRepository {
    fun observeDosesInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<DoseOccurrence>>
    fun observeAllDoses(): Flow<List<DoseOccurrence>>
    fun observeDosesForMedication(medicationId: String): Flow<List<DoseOccurrence>>
    suspend fun recordDose(
        occurrenceId: String,
        medicationId: String,
        scheduledEpochMs: Long,
        quantity: ScaledQuantity,
        unit: StockUnit,
        status: DoseStatus,
        recordedEpochMs: Long = System.currentTimeMillis(),
        notes: String? = null
    ): Result<Unit>
    suspend fun correctDose(
        occurrenceId: String,
        targetStatus: DoseStatus,
        reason: String
    ): Result<Unit>
}

interface PrescriptionRepository {
    fun observePrescriptionsForMedication(medicationId: String): Flow<List<Prescription>>
    fun observeAllPrescriptions(): Flow<List<Prescription>>
    fun observeEventsForPrescription(prescriptionId: String): Flow<List<PrescriptionEvent>>
    fun observePrescriptionEventsForMedication(medicationId: String): Flow<List<PrescriptionEvent>>
    suspend fun createPrescription(prescription: Prescription, notes: String? = null): Result<Unit>
    suspend fun updatePrescription(prescription: Prescription, notes: String? = null): Result<Unit>
    suspend fun requestPrescriptionRepeat(
        prescriptionId: String,
        currentDateEpochDay: Long,
        actionIdempotencyKey: String? = null,
        notes: String? = null
    ): Result<Unit>
    suspend fun collectPrescriptionRepeat(
        prescriptionId: String,
        currentDateEpochDay: Long,
        actionIdempotencyKey: String? = null,
        notes: String? = null
    ): Result<Unit>
    suspend fun archivePrescription(prescriptionId: String, notes: String? = null): Result<Unit>
}

interface StockRepository {
    fun observeStockBalance(medication: Medication): Flow<StockBalance>
    fun observeLotsForMedication(medicationId: String): Flow<List<StockLot>>
    fun observeMovementsForMedication(medicationId: String): Flow<List<StockLedgerEntry>>
    fun observeAllMovements(): Flow<List<StockLedgerEntry>>
    suspend fun restock(
        medicationId: String,
        quantity: ScaledQuantity,
        unit: StockUnit,
        lotNumber: String? = null
    ): Result<Unit>
    suspend fun restockWithLot(
        medicationId: String,
        quantity: ScaledQuantity,
        unit: StockUnit,
        lotNumber: String? = null,
        expiryDateEpochMs: Long? = null,
        notes: String? = null
    ): Result<Unit>
    suspend fun adjustStock(
        medicationId: String,
        quantity: ScaledQuantity,
        unit: StockUnit,
        reason: String,
        isIncrease: Boolean
    ): Result<Unit>
    suspend fun adjustStockToTarget(
        medicationId: String,
        currentBalance: ScaledQuantity,
        targetBalance: ScaledQuantity,
        unit: StockUnit,
        reason: String,
        targetLotId: String? = null
    ): Result<Unit>
}

class NoveloRepositoryImpl(
    private val database: NoveloDatabase,
    private val stockCalculator: StockLedgerCalculator = StockLedgerCalculator(),
    private val prescriptionManager: PrescriptionManager = PrescriptionManager()
) : MedicationRepository, DoseRepository, StockRepository, PrescriptionRepository {

    private val medicationDao = database.medicationDao()
    private val scheduleDao = database.scheduleDao()
    private val doseDao = database.doseDao()
    private val stockLotDao = database.stockLotDao()
    private val stockLedgerDao = database.stockLedgerDao()
    private val prescriptionDao = database.prescriptionDao()

    override fun observeActiveMedications(): Flow<List<Medication>> =
        medicationDao.observeActiveMedications().map { list -> list.map { it.toDomain() } }

    override fun observeAllMedications(): Flow<List<Medication>> =
        medicationDao.observeAllMedications().map { list -> list.map { it.toDomain() } }

    override fun observeMedication(id: String): Flow<Medication?> =
        medicationDao.observeMedicationById(id).map { it?.toDomain() }

    override fun observeAllSchedules(): Flow<List<ScheduleDefinition>> =
        scheduleDao.observeAllSchedules().map { list -> list.map { it.toDomain() } }

    override suspend fun getSchedulesForMedication(medicationId: String): List<ScheduleDefinition> =
        scheduleDao.getSchedulesForMedication(medicationId).map { it.toDomain() }

    override suspend fun getMedication(id: String): Medication? =
        medicationDao.getMedicationById(id)?.toDomain()

    override suspend fun createMedication(
        medication: Medication,
        schedule: ScheduleDefinition
    ): Result<Unit> = runCatching {
        database.withTransaction {
            medicationDao.insertMedication(medication.toEntity())
            scheduleDao.insertSchedule(schedule.toEntity())

            // If an initial opening stock was provided, create the opening ledger entry
            if (medication.openingStock.isPositive()) {
                val openingEntry = StockLedgerEntryEntity(
                    id = UUID.randomUUID().toString(),
                    medicationId = medication.id,
                    timestampEpochMs = medication.createdAtEpochMs,
                    movementType = MovementType.OPENING_BALANCE.name,
                    quantityRaw = medication.openingStock.rawScaledValue,
                    unit = medication.stockUnit.name,
                    reason = "Initial inventory registered",
                    isEstimate = medication.form.isEstimatedConsumption
                )
                stockLedgerDao.insertMovement(openingEntry)
            }
        }
    }

    override suspend fun updateMedication(medication: Medication): Result<Unit> = runCatching {
        medicationDao.updateMedication(medication.toEntity())
    }

    override suspend fun setArchived(id: String, archived: Boolean): Result<Unit> = runCatching {
        medicationDao.setArchived(id, archived)
    }

    override fun observeDosesInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<DoseOccurrence>> =
        doseDao.observeDosesInRange(startEpochMs, endEpochMs).map { list -> list.map { it.toDomain() } }

    override fun observeAllDoses(): Flow<List<DoseOccurrence>> =
        doseDao.observeAllDoses().map { list -> list.map { it.toDomain() } }

    override fun observeDosesForMedication(medicationId: String): Flow<List<DoseOccurrence>> =
        doseDao.observeDosesForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    /**
     * Atomically and idempotently records a dose event and its corresponding ledger movement.
     * Guaranteed safe against duplicate clicks and concurrent workers.
     */
    override suspend fun recordDose(
        occurrenceId: String,
        medicationId: String,
        scheduledEpochMs: Long,
        quantity: ScaledQuantity,
        unit: StockUnit,
        status: DoseStatus,
        recordedEpochMs: Long,
        notes: String?
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val existingDose = doseDao.getDoseByOccurrenceId(occurrenceId)

            // If already confirmed or assumed, reject duplicate execution idempotently
            if (existingDose != null && (existingDose.status == DoseStatus.TAKEN.name || existingDose.status == DoseStatus.ASSUMED.name)) {
                return@withTransaction
            }

            // Deduct stock strictly on confirmed (TAKEN) or AUTO_ASSUME doses
            val shouldDeduct = (status == DoseStatus.TAKEN || status == DoseStatus.ASSUMED)
            if (shouldDeduct) {
                val existingDeductions = stockLedgerDao.countDeductionsForOccurrence(occurrenceId)
                val existingReversals = stockLedgerDao.countReversalsForOccurrence(occurrenceId)
                val netDeductions = existingDeductions - existingReversals

                if (netDeductions == 0) {
                    // Debit available lots using FEFO allocation
                    val lots = stockLotDao.getLotsForMedication(medicationId).map { it.toDomain() }
                    val allocations = stockCalculator.allocateDoseAgainstLots(quantity, lots)
                    for ((lot, debited) in allocations) {
                        val newRemaining = lot.remainingQuantity.rawScaledValue - debited.rawScaledValue
                        stockLotDao.updateRemainingQuantity(lot.id, kotlin.math.max(0L, newRemaining))
                    }
                    val primaryLotId = allocations.firstOrNull()?.first?.id

                    val movement = StockLedgerEntryEntity(
                        id = UUID.randomUUID().toString(),
                        medicationId = medicationId,
                        timestampEpochMs = recordedEpochMs,
                        movementType = if (status == DoseStatus.TAKEN) MovementType.DOSE_TAKEN.name else MovementType.DOSE_ASSUMED.name,
                        quantityRaw = quantity.rawScaledValue,
                        unit = unit.name,
                        reason = if (status == DoseStatus.TAKEN) "Confirmed dose taken" else "Assumed dose per schedule",
                        doseOccurrenceId = occurrenceId,
                        lotId = primaryLotId,
                        isEstimate = false
                    )
                    stockLedgerDao.insertMovement(movement)
                }
            }

            // Upsert the dose event record
            val occurrence = DoseOccurrence(
                occurrenceId = occurrenceId,
                medicationId = medicationId,
                scheduledEpochMs = scheduledEpochMs,
                doseQuantity = quantity,
                unit = unit,
                status = status,
                recordedEpochMs = recordedEpochMs,
                notes = notes
            )
            if (existingDose == null) {
                doseDao.insertDoseEvent(occurrence.toEntity())
            } else {
                doseDao.updateDoseEvent(occurrence.toEntity())
            }
        }
    }

    /**
     * Corrects a historical dose status. Reverses ledger deductions via an auditable DOSE_REVERSAL
     * if changing from TAKEN/ASSUMED to SKIPPED/MISSED. Prevents duplicate reversals idempotently.
     */
    override suspend fun correctDose(
        occurrenceId: String,
        targetStatus: DoseStatus,
        reason: String
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val existingDose = doseDao.getDoseByOccurrenceId(occurrenceId) ?: return@withTransaction
            val prevStatus = runCatching { DoseStatus.valueOf(existingDose.status) }.getOrDefault(DoseStatus.PENDING)

            if (prevStatus == targetStatus) return@withTransaction

            val deductions = stockLedgerDao.countDeductionsForOccurrence(occurrenceId)
            val reversals = stockLedgerDao.countReversalsForOccurrence(occurrenceId)
            val netDeductions = deductions - reversals

            // TAKEN/ASSUMED -> SKIPPED/MISSED: Compensate deduction via DOSE_REVERSAL
            if ((prevStatus == DoseStatus.TAKEN || prevStatus == DoseStatus.ASSUMED) &&
                (targetStatus == DoseStatus.SKIPPED || targetStatus == DoseStatus.MISSED)
            ) {
                if (netDeductions > 0) {
                    val reversal = StockLedgerEntryEntity(
                        id = UUID.randomUUID().toString(),
                        medicationId = existingDose.medicationId,
                        timestampEpochMs = System.currentTimeMillis(),
                        movementType = MovementType.DOSE_REVERSAL.name,
                        quantityRaw = existingDose.doseQuantityRaw,
                        unit = existingDose.unit,
                        reason = "Dose correction (${prevStatus.displayName} -> ${targetStatus.displayName}): $reason",
                        doseOccurrenceId = occurrenceId
                    )
                    stockLedgerDao.insertMovement(reversal)

                    // Restore quantity to lots where remaining < initial
                    val lots = stockLotDao.getLotsForMedication(existingDose.medicationId).map { it.toDomain() }
                    val lotToRestore = lots.firstOrNull { it.remainingQuantity < it.initialQuantity } ?: lots.firstOrNull()
                    if (lotToRestore != null) {
                        val restored = kotlin.math.min(
                            lotToRestore.initialQuantity.rawScaledValue,
                            lotToRestore.remainingQuantity.rawScaledValue + existingDose.doseQuantityRaw
                        )
                        stockLotDao.updateRemainingQuantity(lotToRestore.id, restored)
                    }
                }
            }

            // SKIPPED/MISSED/PENDING -> TAKEN/ASSUMED: Record deduction
            if ((prevStatus == DoseStatus.SKIPPED || prevStatus == DoseStatus.MISSED || prevStatus == DoseStatus.PENDING) &&
                (targetStatus == DoseStatus.TAKEN || targetStatus == DoseStatus.ASSUMED)
            ) {
                if (netDeductions == 0) {
                    val lots = stockLotDao.getLotsForMedication(existingDose.medicationId).map { it.toDomain() }
                    val allocations = stockCalculator.allocateDoseAgainstLots(ScaledQuantity(existingDose.doseQuantityRaw), lots)
                    for ((lot, debited) in allocations) {
                        val newRemaining = lot.remainingQuantity.rawScaledValue - debited.rawScaledValue
                        stockLotDao.updateRemainingQuantity(lot.id, kotlin.math.max(0L, newRemaining))
                    }

                    val deduction = StockLedgerEntryEntity(
                        id = UUID.randomUUID().toString(),
                        medicationId = existingDose.medicationId,
                        timestampEpochMs = System.currentTimeMillis(),
                        movementType = if (targetStatus == DoseStatus.TAKEN) MovementType.DOSE_TAKEN.name else MovementType.DOSE_ASSUMED.name,
                        quantityRaw = existingDose.doseQuantityRaw,
                        unit = existingDose.unit,
                        reason = "Dose correction (${prevStatus.displayName} -> ${targetStatus.displayName}): $reason",
                        doseOccurrenceId = occurrenceId,
                        lotId = allocations.firstOrNull()?.first?.id
                    )
                    stockLedgerDao.insertMovement(deduction)
                }
            }

            // Update dose event with correction audit metadata
            val updatedDose = existingDose.copy(
                status = targetStatus.name,
                correctionNotes = reason,
                correctedAtEpochMs = System.currentTimeMillis()
            )
            doseDao.updateDoseEvent(updatedDose)
        }
    }

    override fun observeStockBalance(medication: Medication): Flow<StockBalance> {
        return stockLedgerDao.observeMovementsForMedication(medication.id).map { movementEntities ->
            val movements = movementEntities.map { it.toDomain() }
            val hasOpeningEntry = movements.any { it.movementType == MovementType.OPENING_BALANCE }
            val baseOpening = if (hasOpeningEntry) ScaledQuantity.ZERO else medication.openingStock

            stockCalculator.calculateBalance(
                openingStock = baseOpening,
                openingUnit = medication.stockUnit,
                movements = movements,
                lowStockThreshold = medication.lowStockThreshold,
                isEstimatedConsumption = medication.form.isEstimatedConsumption
            )
        }
    }

    override fun observeLotsForMedication(medicationId: String): Flow<List<StockLot>> =
        stockLotDao.observeLotsForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override fun observeMovementsForMedication(medicationId: String): Flow<List<StockLedgerEntry>> =
        stockLedgerDao.observeMovementsForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override fun observeAllMovements(): Flow<List<StockLedgerEntry>> =
        stockLedgerDao.observeAllMovements().map { list -> list.map { it.toDomain() } }

    override suspend fun restock(
        medicationId: String,
        quantity: ScaledQuantity,
        unit: StockUnit,
        lotNumber: String?
    ): Result<Unit> = restockWithLot(
        medicationId = medicationId,
        quantity = quantity,
        unit = unit,
        lotNumber = lotNumber,
        expiryDateEpochMs = null,
        notes = null
    )

    override suspend fun restockWithLot(
        medicationId: String,
        quantity: ScaledQuantity,
        unit: StockUnit,
        lotNumber: String?,
        expiryDateEpochMs: Long?,
        notes: String?
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val lotId = UUID.randomUUID().toString()
            val lotEntity = StockLotEntity(
                id = lotId,
                medicationId = medicationId,
                lotNumber = lotNumber?.takeIf { it.isNotBlank() },
                initialQuantityRaw = quantity.rawScaledValue,
                remainingQuantityRaw = quantity.rawScaledValue,
                unit = unit.name,
                expiryDateEpochMs = expiryDateEpochMs,
                createdAtEpochMs = System.currentTimeMillis(),
                notes = notes
            )
            stockLotDao.insertLot(lotEntity)

            val entry = StockLedgerEntryEntity(
                id = UUID.randomUUID().toString(),
                medicationId = medicationId,
                timestampEpochMs = System.currentTimeMillis(),
                movementType = MovementType.RESTOCK.name,
                quantityRaw = quantity.rawScaledValue,
                unit = unit.name,
                restockLotNumber = lotNumber?.takeIf { it.isNotBlank() },
                lotId = lotId,
                reason = notes?.takeIf { it.isNotBlank() } ?: "Prescription refill restock"
            )
            stockLedgerDao.insertMovement(entry)
        }
    }

    override suspend fun adjustStock(
        medicationId: String,
        quantity: ScaledQuantity,
        unit: StockUnit,
        reason: String,
        isIncrease: Boolean
    ): Result<Unit> = runCatching {
        val entry = StockLedgerEntryEntity(
            id = UUID.randomUUID().toString(),
            medicationId = medicationId,
            timestampEpochMs = System.currentTimeMillis(),
            movementType = if (isIncrease) MovementType.ADJUSTMENT_IN.name else MovementType.ADJUSTMENT_OUT.name,
            quantityRaw = quantity.rawScaledValue,
            unit = unit.name,
            reason = reason
        )
        stockLedgerDao.insertMovement(entry)
    }

    override suspend fun adjustStockToTarget(
        medicationId: String,
        currentBalance: ScaledQuantity,
        targetBalance: ScaledQuantity,
        unit: StockUnit,
        reason: String,
        targetLotId: String?
    ): Result<Unit> = runCatching {
        if (targetBalance == currentBalance) return@runCatching

        database.withTransaction {
            if (targetBalance > currentBalance) {
                val delta = targetBalance - currentBalance
                val entry = StockLedgerEntryEntity(
                    id = UUID.randomUUID().toString(),
                    medicationId = medicationId,
                    timestampEpochMs = System.currentTimeMillis(),
                    movementType = MovementType.ADJUSTMENT_IN.name,
                    quantityRaw = delta.rawScaledValue,
                    unit = unit.name,
                    reason = reason,
                    lotId = targetLotId
                )
                stockLedgerDao.insertMovement(entry)

                if (targetLotId != null) {
                    val lot = stockLotDao.getLotById(targetLotId)
                    if (lot != null) {
                        stockLotDao.updateRemainingQuantity(targetLotId, lot.remainingQuantityRaw + delta.rawScaledValue)
                    }
                }
            } else {
                val delta = currentBalance - targetBalance
                val entry = StockLedgerEntryEntity(
                    id = UUID.randomUUID().toString(),
                    medicationId = medicationId,
                    timestampEpochMs = System.currentTimeMillis(),
                    movementType = MovementType.ADJUSTMENT_OUT.name,
                    quantityRaw = delta.rawScaledValue,
                    unit = unit.name,
                    reason = reason,
                    lotId = targetLotId
                )
                stockLedgerDao.insertMovement(entry)

                if (targetLotId != null) {
                    val lot = stockLotDao.getLotById(targetLotId)
                    if (lot != null) {
                        val newRem = kotlin.math.max(0L, lot.remainingQuantityRaw - delta.rawScaledValue)
                        stockLotDao.updateRemainingQuantity(targetLotId, newRem)
                    }
                } else {
                    val lots = stockLotDao.getLotsForMedication(medicationId).map { it.toDomain() }
                    val allocations = stockCalculator.allocateDoseAgainstLots(delta, lots)
                    for ((lot, debited) in allocations) {
                        val newRem = kotlin.math.max(0L, lot.remainingQuantity.rawScaledValue - debited.rawScaledValue)
                        stockLotDao.updateRemainingQuantity(lot.id, newRem)
                    }
                }
            }
        }
    }

    override fun observePrescriptionsForMedication(medicationId: String): Flow<List<Prescription>> =
        prescriptionDao.observePrescriptionsForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override fun observeAllPrescriptions(): Flow<List<Prescription>> =
        prescriptionDao.observeAllPrescriptions().map { list -> list.map { it.toDomain() } }

    override fun observeEventsForPrescription(prescriptionId: String): Flow<List<PrescriptionEvent>> =
        prescriptionDao.observeEventsForPrescription(prescriptionId).map { list -> list.map { it.toDomain() } }

    override fun observePrescriptionEventsForMedication(medicationId: String): Flow<List<PrescriptionEvent>> =
        prescriptionDao.observeEventsForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override suspend fun createPrescription(
        prescription: Prescription,
        notes: String?
    ): Result<Unit> = runCatching {
        require(prescription.repeatsAuthorised >= 0) { "Repeats authorised cannot be negative." }
        require(prescription.repeatsRemaining >= 0) { "Repeats remaining cannot be negative." }
        require(prescription.repeatsRemaining <= prescription.repeatsAuthorised) {
            "Repeats remaining cannot exceed repeats authorised."
        }

        database.withTransaction {
            val sanitized = prescription.copy(
                repeatsAuthorised = prescription.repeatsAuthorised.coerceAtLeast(0),
                repeatsRemaining = prescription.repeatsRemaining.coerceIn(0, prescription.repeatsAuthorised.coerceAtLeast(0))
            )
            prescriptionDao.insertPrescription(sanitized.toEntity())

            val event = PrescriptionEventEntity(
                id = UUID.randomUUID().toString(),
                prescriptionId = sanitized.id,
                medicationId = sanitized.medicationId,
                eventType = PrescriptionEventType.CREATED.name,
                timestampEpochMs = sanitized.createdAtEpochMs,
                repeatsRemainingAfter = sanitized.repeatsRemaining,
                notes = notes ?: "Prescription added (${sanitized.repeatsRemaining} of ${sanitized.repeatsAuthorised} repeats)"
            )
            prescriptionDao.insertPrescriptionEvent(event)
        }
    }

    override suspend fun updatePrescription(
        prescription: Prescription,
        notes: String?
    ): Result<Unit> = runCatching {
        require(prescription.repeatsAuthorised >= 0) { "Repeats authorised cannot be negative." }
        require(prescription.repeatsRemaining >= 0) { "Repeats remaining cannot be negative." }
        require(prescription.repeatsRemaining <= prescription.repeatsAuthorised) {
            "Repeats remaining cannot exceed repeats authorised."
        }

        database.withTransaction {
            val now = System.currentTimeMillis()
            val updated = prescription.copy(updatedAtEpochMs = now)
            prescriptionDao.updatePrescription(updated.toEntity())

            val event = PrescriptionEventEntity(
                id = UUID.randomUUID().toString(),
                prescriptionId = updated.id,
                medicationId = updated.medicationId,
                eventType = PrescriptionEventType.UPDATED.name,
                timestampEpochMs = now,
                repeatsRemainingAfter = updated.repeatsRemaining,
                notes = notes ?: "Prescription details updated (${updated.repeatsRemaining} repeats remaining)"
            )
            prescriptionDao.insertPrescriptionEvent(event)
        }
    }

    override suspend fun requestPrescriptionRepeat(
        prescriptionId: String,
        currentDateEpochDay: Long,
        actionIdempotencyKey: String?,
        notes: String?
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val existingEntity = prescriptionDao.getPrescriptionById(prescriptionId)
                ?: throw IllegalArgumentException("Prescription not found")
            val existingEvents = prescriptionDao.getEventsForPrescription(prescriptionId).map { it.toDomain() }

            val transition = prescriptionManager.requestRepeat(
                prescription = existingEntity.toDomain(),
                existingEvents = existingEvents,
                currentDateEpochDay = currentDateEpochDay,
                actionIdempotencyKey = actionIdempotencyKey,
                notes = notes
            )

            if (transition.auditEvent != null) {
                prescriptionDao.updatePrescription(transition.updatedPrescription.toEntity())
                prescriptionDao.insertPrescriptionEvent(transition.auditEvent.toEntity())
            }
        }
    }

    override suspend fun collectPrescriptionRepeat(
        prescriptionId: String,
        currentDateEpochDay: Long,
        actionIdempotencyKey: String?,
        notes: String?
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val existingEntity = prescriptionDao.getPrescriptionById(prescriptionId)
                ?: throw IllegalArgumentException("Prescription not found")
            val existingEvents = prescriptionDao.getEventsForPrescription(prescriptionId).map { it.toDomain() }

            val transition = prescriptionManager.collectRepeat(
                prescription = existingEntity.toDomain(),
                existingEvents = existingEvents,
                currentDateEpochDay = currentDateEpochDay,
                actionIdempotencyKey = actionIdempotencyKey,
                notes = notes
            )

            if (transition.auditEvent != null) {
                prescriptionDao.updatePrescription(transition.updatedPrescription.toEntity())
                prescriptionDao.insertPrescriptionEvent(transition.auditEvent.toEntity())
            }
        }
    }

    override suspend fun archivePrescription(
        prescriptionId: String,
        notes: String?
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val existingEntity = prescriptionDao.getPrescriptionById(prescriptionId)
                ?: throw IllegalArgumentException("Prescription not found")
            val now = System.currentTimeMillis()
            val updated = existingEntity.toDomain().copy(
                status = PrescriptionStatus.ARCHIVED,
                hasPendingRepeatRequest = false,
                updatedAtEpochMs = now
            )
            prescriptionDao.updatePrescription(updated.toEntity())

            val event = PrescriptionEventEntity(
                id = UUID.randomUUID().toString(),
                prescriptionId = updated.id,
                medicationId = updated.medicationId,
                eventType = PrescriptionEventType.ARCHIVED.name,
                timestampEpochMs = now,
                repeatsRemainingAfter = updated.repeatsRemaining,
                notes = notes ?: "Prescription archived"
            )
            prescriptionDao.insertPrescriptionEvent(event)
        }
    }
}
