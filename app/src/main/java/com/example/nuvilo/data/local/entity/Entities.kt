package com.example.nuvilo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medications",
    indices = [
        Index(value = ["is_archived"]),
        Index(value = ["name"])
    ]
)
data class MedicationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "strength")
    val strength: String,

    @ColumnInfo(name = "form")
    val form: String,

    @ColumnInfo(name = "stock_unit")
    val stockUnit: String,

    @ColumnInfo(name = "opening_stock_raw")
    val openingStockRaw: Long,

    @ColumnInfo(name = "low_stock_threshold_raw")
    val lowStockThresholdRaw: Long,

    @ColumnInfo(name = "recording_mode")
    val recordingMode: String,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "color_tag_hex")
    val colorTagHex: String = "#005EB8",

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medication_id"])
    ]
)
data class ScheduleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "medication_id")
    val medicationId: String,

    @ColumnInfo(name = "schedule_type")
    val scheduleType: String,

    @ColumnInfo(name = "target_times_csv")
    val targetTimesCsv: String,

    @ColumnInfo(name = "days_of_week_csv")
    val daysOfWeekCsv: String,

    @ColumnInfo(name = "dose_quantity_raw")
    val doseQuantityRaw: Long,

    @ColumnInfo(name = "start_date_epoch_day")
    val startDateEpochDay: Long,

    @ColumnInfo(name = "end_date_epoch_day")
    val endDateEpochDay: Long? = null,

    @ColumnInfo(name = "instructions")
    val instructions: String = ""
)

@Entity(
    tableName = "dose_events",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["occurrence_id"], unique = true),
        Index(value = ["medication_id"]),
        Index(value = ["scheduled_epoch_ms"])
    ]
)
data class DoseEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: String,

    @ColumnInfo(name = "medication_id")
    val medicationId: String,

    @ColumnInfo(name = "scheduled_epoch_ms")
    val scheduledEpochMs: Long,

    @ColumnInfo(name = "dose_quantity_raw")
    val doseQuantityRaw: Long,

    @ColumnInfo(name = "unit")
    val unit: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "recorded_epoch_ms")
    val recordedEpochMs: Long? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "correction_notes")
    val correctionNotes: String? = null,

    @ColumnInfo(name = "corrected_at_epoch_ms")
    val correctedAtEpochMs: Long? = null
)

@Entity(
    tableName = "stock_lots",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medication_id"]),
        Index(value = ["expiry_date_epoch_ms"])
    ]
)
data class StockLotEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "medication_id")
    val medicationId: String,

    @ColumnInfo(name = "lot_number")
    val lotNumber: String? = null,

    @ColumnInfo(name = "initial_quantity_raw")
    val initialQuantityRaw: Long,

    @ColumnInfo(name = "remaining_quantity_raw")
    val remainingQuantityRaw: Long,

    @ColumnInfo(name = "unit")
    val unit: String,

    @ColumnInfo(name = "expiry_date_epoch_ms")
    val expiryDateEpochMs: Long? = null,

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "notes")
    val notes: String? = null
)

@Entity(
    tableName = "stock_ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medication_id"]),
        Index(value = ["timestamp_epoch_ms"]),
        Index(value = ["dose_occurrence_id"]),
        Index(value = ["lot_id"])
    ]
)
data class StockLedgerEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "medication_id")
    val medicationId: String,

    @ColumnInfo(name = "timestamp_epoch_ms")
    val timestampEpochMs: Long,

    @ColumnInfo(name = "movement_type")
    val movementType: String,

    @ColumnInfo(name = "quantity_raw")
    val quantityRaw: Long,

    @ColumnInfo(name = "unit")
    val unit: String,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "dose_occurrence_id")
    val doseOccurrenceId: String? = null,

    @ColumnInfo(name = "restock_lot_number")
    val restockLotNumber: String? = null,

    @ColumnInfo(name = "lot_id")
    val lotId: String? = null,

    @ColumnInfo(name = "is_estimate")
    val isEstimate: Boolean = false
)

@Entity(
    tableName = "prescriptions",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medication_id"]),
        Index(value = ["status"]),
        Index(value = ["expiry_date_epoch_day"])
    ]
)
data class PrescriptionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "medication_id")
    val medicationId: String,

    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,

    @ColumnInfo(name = "issue_date_epoch_day")
    val issueDateEpochDay: Long,

    @ColumnInfo(name = "expiry_date_epoch_day")
    val expiryDateEpochDay: Long? = null,

    @ColumnInfo(name = "quantity_prescribed_raw")
    val quantityPrescribedRaw: Long? = null,

    @ColumnInfo(name = "unit")
    val unit: String? = null,

    @ColumnInfo(name = "repeats_authorised")
    val repeatsAuthorised: Int = 0,

    @ColumnInfo(name = "repeats_remaining")
    val repeatsRemaining: Int = 0,

    @ColumnInfo(name = "has_pending_repeat_request")
    val hasPendingRepeatRequest: Boolean = false,

    @ColumnInfo(name = "pharmacy_name")
    val pharmacyName: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE",

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "prescription_events",
    foreignKeys = [
        ForeignKey(
            entity = PrescriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["prescription_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["prescription_id"]),
        Index(value = ["medication_id"]),
        Index(value = ["timestamp_epoch_ms"]),
        Index(value = ["action_idempotency_key"])
    ]
)
data class PrescriptionEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "prescription_id")
    val prescriptionId: String,

    @ColumnInfo(name = "medication_id")
    val medicationId: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "timestamp_epoch_ms")
    val timestampEpochMs: Long,

    @ColumnInfo(name = "repeats_remaining_after")
    val repeatsRemainingAfter: Int,

    @ColumnInfo(name = "action_idempotency_key")
    val actionIdempotencyKey: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
