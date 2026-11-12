package com.example.nuvilo.data.mapper

import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.GelTrackingMode
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEvent
import com.example.nuvilo.domain.model.PrescriptionEventType
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision

fun MedicationEntity.toDomain(): Medication {
    val parsedUnit = runCatching { StockUnit.valueOf(stockUnit) }.getOrDefault(StockUnit.OTHER)
    val parsedPkgType = packageType?.let { runCatching { PackageType.valueOf(it) }.getOrNull() }
    val parsedPrecision = runCatching { TrackingPrecision.valueOf(trackingPrecision) }.getOrDefault(TrackingPrecision.UNKNOWN)
    val parsedGelMode = gelTrackingMode?.let { runCatching { GelTrackingMode.valueOf(it) }.getOrNull() }
    val parsedSite = runCatching { AdministrationSite.valueOf(administrationSite) }.getOrDefault(AdministrationSite.NONE)

    val pkgConfig = if (parsedPkgType != null || contentsPerPackageRaw != null || netContentLabel != null || discardAfterOpeningDays != null || parsedGelMode != null || parsedPrecision != TrackingPrecision.UNKNOWN) {
        PackagingConfig(
            packageType = parsedPkgType ?: PackageType.CUSTOM,
            consumptionUnit = parsedUnit,
            contentsPerPackage = contentsPerPackageRaw?.let { ScaledQuantity(it) },
            netContentLabel = netContentLabel,
            discardAfterOpeningDays = discardAfterOpeningDays,
            trackingPrecision = parsedPrecision,
            gelTrackingMode = parsedGelMode
        )
    } else null

    return Medication(
        id = id,
        name = name,
        strength = strength,
        form = runCatching { MedicationForm.valueOf(form) }.getOrDefault(MedicationForm.OTHER),
        stockUnit = parsedUnit,
        openingStock = ScaledQuantity(openingStockRaw),
        lowStockThreshold = ScaledQuantity(lowStockThresholdRaw),
        recordingMode = runCatching { RecordingMode.valueOf(recordingMode) }.getOrDefault(RecordingMode.CONFIRM),
        notes = notes,
        colorTagHex = colorTagHex,
        isArchived = isArchived,
        createdAtEpochMs = createdAtEpochMs,
        packagingConfig = pkgConfig,
        administrationProfile = AdministrationProfile(
            site = parsedSite,
            leftEyeQuantity = leftEyeQuantityRaw?.let { ScaledQuantity(it) },
            rightEyeQuantity = rightEyeQuantityRaw?.let { ScaledQuantity(it) },
            containersPerAdministration = containersPerAdminRaw?.let { ScaledQuantity(it) }
        ),
        lowStockDaysThreshold = lowStockDaysThreshold
    )
}

fun Medication.toEntity(): MedicationEntity {
    val pkg = packagingConfig
    val effectivePrecision = pkg?.trackingPrecision?.name ?: if (form.isEstimatedConsumption) TrackingPrecision.ESTIMATED.name else TrackingPrecision.EXACT.name
    return MedicationEntity(
        id = id,
        name = name,
        strength = strength,
        form = form.name,
        stockUnit = effectiveConsumptionUnit.name,
        openingStockRaw = openingStock.rawScaledValue,
        lowStockThresholdRaw = lowStockThreshold.rawScaledValue,
        recordingMode = recordingMode.name,
        notes = notes,
        colorTagHex = colorTagHex,
        isArchived = isArchived,
        createdAtEpochMs = createdAtEpochMs,
        packageType = pkg?.packageType?.name,
        contentsPerPackageRaw = pkg?.contentsPerPackage?.rawScaledValue,
        netContentLabel = pkg?.netContentLabel,
        discardAfterOpeningDays = pkg?.discardAfterOpeningDays,
        trackingPrecision = effectivePrecision,
        gelTrackingMode = pkg?.gelTrackingMode?.name,
        administrationSite = administrationProfile.site.name,
        leftEyeQuantityRaw = administrationProfile.leftEyeQuantity?.rawScaledValue,
        rightEyeQuantityRaw = administrationProfile.rightEyeQuantity?.rawScaledValue,
        containersPerAdminRaw = administrationProfile.containersPerAdministration?.rawScaledValue,
        lowStockDaysThreshold = lowStockDaysThreshold
    )
}

fun ScheduleEntity.toDomain(): ScheduleDefinition = ScheduleDefinition(
    id = id,
    medicationId = medicationId,
    scheduleType = runCatching { ScheduleType.valueOf(scheduleType) }.getOrDefault(ScheduleType.DAILY),
    targetTimesMinuteOfDay = targetTimesCsv.split(",").mapNotNull { it.trim().toIntOrNull() },
    daysOfWeek = daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
    doseQuantityPerOccurrence = ScaledQuantity(doseQuantityRaw),
    startDateEpochDay = startDateEpochDay,
    endDateEpochDay = endDateEpochDay,
    instructions = instructions
)

fun ScheduleDefinition.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    medicationId = medicationId,
    scheduleType = scheduleType.name,
    targetTimesCsv = targetTimesMinuteOfDay.joinToString(","),
    daysOfWeekCsv = daysOfWeek.joinToString(","),
    doseQuantityRaw = doseQuantityPerOccurrence.rawScaledValue,
    startDateEpochDay = startDateEpochDay,
    endDateEpochDay = endDateEpochDay,
    instructions = instructions
)

fun DoseEventEntity.toDomain(): DoseOccurrence = DoseOccurrence(
    occurrenceId = occurrenceId,
    medicationId = medicationId,
    scheduledEpochMs = scheduledEpochMs,
    doseQuantity = ScaledQuantity(doseQuantityRaw),
    unit = runCatching { StockUnit.valueOf(unit) }.getOrDefault(StockUnit.OTHER),
    status = runCatching { DoseStatus.valueOf(status) }.getOrDefault(DoseStatus.PENDING),
    recordedEpochMs = recordedEpochMs,
    notes = notes,
    correctionNotes = correctionNotes,
    correctedAtEpochMs = correctedAtEpochMs
)

fun DoseOccurrence.toEntity(): DoseEventEntity = DoseEventEntity(
    occurrenceId = occurrenceId,
    medicationId = medicationId,
    scheduledEpochMs = scheduledEpochMs,
    doseQuantityRaw = doseQuantity.rawScaledValue,
    unit = unit.name,
    status = status.name,
    recordedEpochMs = recordedEpochMs,
    notes = notes,
    correctionNotes = correctionNotes,
    correctedAtEpochMs = correctedAtEpochMs
)

fun StockLotEntity.toDomain(): StockLot = StockLot(
    id = id,
    medicationId = medicationId,
    lotNumber = lotNumber,
    initialQuantity = ScaledQuantity(initialQuantityRaw),
    remainingQuantity = ScaledQuantity(remainingQuantityRaw),
    unit = runCatching { StockUnit.valueOf(unit) }.getOrDefault(StockUnit.OTHER),
    expiryDateEpochMs = expiryDateEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    notes = notes,
    packageType = packageType?.let { runCatching { PackageType.valueOf(it) }.getOrNull() },
    packageLabel = packageLabel,
    packageStatus = runCatching { PackageStatus.valueOf(packageStatus) }.getOrDefault(PackageStatus.UNOPENED),
    openedAtEpochMs = openedAtEpochMs,
    discardAfterOpeningDays = discardAfterOpeningDays,
    discardAfterOpeningEpochMs = discardAfterOpeningEpochMs,
    discardedAtEpochMs = discardedAtEpochMs,
    discardReason = discardReason
)

fun StockLot.toEntity(): StockLotEntity = StockLotEntity(
    id = id,
    medicationId = medicationId,
    lotNumber = lotNumber,
    initialQuantityRaw = initialQuantity.rawScaledValue,
    remainingQuantityRaw = remainingQuantity.rawScaledValue,
    unit = unit.name,
    expiryDateEpochMs = expiryDateEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    notes = notes,
    packageType = packageType?.name,
    packageLabel = packageLabel,
    packageStatus = packageStatus.name,
    openedAtEpochMs = openedAtEpochMs,
    discardAfterOpeningDays = discardAfterOpeningDays,
    discardAfterOpeningEpochMs = discardAfterOpeningEpochMs,
    discardedAtEpochMs = discardedAtEpochMs,
    discardReason = discardReason
)

fun StockLedgerEntryEntity.toDomain(): StockLedgerEntry = StockLedgerEntry(
    id = id,
    medicationId = medicationId,
    timestampEpochMs = timestampEpochMs,
    movementType = runCatching { MovementType.valueOf(movementType) }.getOrDefault(MovementType.ADJUSTMENT_OUT),
    quantity = ScaledQuantity(quantityRaw),
    unit = runCatching { StockUnit.valueOf(unit) }.getOrDefault(StockUnit.OTHER),
    reason = reason,
    doseOccurrenceId = doseOccurrenceId,
    restockLotNumber = restockLotNumber,
    lotId = lotId,
    isEstimate = isEstimate
)

fun StockLedgerEntry.toEntity(): StockLedgerEntryEntity = StockLedgerEntryEntity(
    id = id,
    medicationId = medicationId,
    timestampEpochMs = timestampEpochMs,
    movementType = movementType.name,
    quantityRaw = quantity.rawScaledValue,
    unit = unit.name,
    reason = reason,
    doseOccurrenceId = doseOccurrenceId,
    restockLotNumber = restockLotNumber,
    lotId = lotId,
    isEstimate = isEstimate
)

fun PrescriptionEntity.toDomain(): Prescription = Prescription(
    id = id,
    medicationId = medicationId,
    referenceNumber = referenceNumber,
    issueDateEpochDay = issueDateEpochDay,
    expiryDateEpochDay = expiryDateEpochDay,
    quantityPrescribed = quantityPrescribedRaw?.let { ScaledQuantity(it) },
    unit = unit?.let { runCatching { StockUnit.valueOf(it) }.getOrNull() },
    repeatsAuthorised = repeatsAuthorised,
    repeatsRemaining = repeatsRemaining,
    hasPendingRepeatRequest = hasPendingRepeatRequest,
    pharmacyName = pharmacyName,
    notes = notes,
    status = runCatching { PrescriptionStatus.valueOf(status) }.getOrDefault(PrescriptionStatus.ACTIVE),
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Prescription.toEntity(): PrescriptionEntity = PrescriptionEntity(
    id = id,
    medicationId = medicationId,
    referenceNumber = referenceNumber,
    issueDateEpochDay = issueDateEpochDay,
    expiryDateEpochDay = expiryDateEpochDay,
    quantityPrescribedRaw = quantityPrescribed?.rawScaledValue,
    unit = unit?.name,
    repeatsAuthorised = repeatsAuthorised,
    repeatsRemaining = repeatsRemaining,
    hasPendingRepeatRequest = hasPendingRepeatRequest,
    pharmacyName = pharmacyName,
    notes = notes,
    status = status.name,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun PrescriptionEventEntity.toDomain(): PrescriptionEvent = PrescriptionEvent(
    id = id,
    prescriptionId = prescriptionId,
    medicationId = medicationId,
    eventType = runCatching { PrescriptionEventType.valueOf(eventType) }.getOrDefault(PrescriptionEventType.UPDATED),
    timestampEpochMs = timestampEpochMs,
    repeatsRemainingAfter = repeatsRemainingAfter,
    actionIdempotencyKey = actionIdempotencyKey,
    notes = notes
)

fun PrescriptionEvent.toEntity(): PrescriptionEventEntity = PrescriptionEventEntity(
    id = id,
    prescriptionId = prescriptionId,
    medicationId = medicationId,
    eventType = eventType.name,
    timestampEpochMs = timestampEpochMs,
    repeatsRemainingAfter = repeatsRemainingAfter,
    actionIdempotencyKey = actionIdempotencyKey,
    notes = notes
)
