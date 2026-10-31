package com.example.nuvilo.data.backup

import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import org.json.JSONArray
import org.json.JSONObject

object JsonBackupManager {

    fun serializeBackup(backup: NoveloDataBackup): String = serialize(backup)
    fun deserializeBackup(jsonString: String): NoveloDataBackup = deserialize(jsonString)

    fun serialize(backup: NoveloDataBackup): String {
        val root = JSONObject().apply {
            put("formatVersion", backup.formatVersion)
            put("appVersion", backup.appVersion)
            put("schemaVersion", backup.schemaVersion)
            put("exportTimestampEpochMs", backup.exportTimestampEpochMs)

            // 1. Medications
            val medsArray = JSONArray()
            backup.medications.forEach { med ->
                medsArray.put(JSONObject().apply {
                    put("id", med.id)
                    put("name", med.name)
                    put("strength", med.strength)
                    put("form", med.form)
                    put("stockUnit", med.stockUnit)
                    put("openingStockRaw", med.openingStockRaw)
                    put("lowStockThresholdRaw", med.lowStockThresholdRaw)
                    put("recordingMode", med.recordingMode)
                    put("notes", med.notes)
                    put("colorTagHex", med.colorTagHex)
                    put("isArchived", med.isArchived)
                    put("createdAtEpochMs", med.createdAtEpochMs)
                })
            }
            put("medications", medsArray)

            // 2. Schedules
            val schedulesArray = JSONArray()
            backup.schedules.forEach { s ->
                schedulesArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("medicationId", s.medicationId)
                    put("scheduleType", s.scheduleType)
                    put("targetTimesCsv", s.targetTimesCsv)
                    put("daysOfWeekCsv", s.daysOfWeekCsv)
                    put("doseQuantityRaw", s.doseQuantityRaw)
                    put("startDateEpochDay", s.startDateEpochDay)
                    if (s.endDateEpochDay != null) put("endDateEpochDay", s.endDateEpochDay)
                    put("instructions", s.instructions)
                })
            }
            put("schedules", schedulesArray)

            // 3. Dose Events
            val dosesArray = JSONArray()
            backup.doseEvents.forEach { d ->
                dosesArray.put(JSONObject().apply {
                    put("occurrenceId", d.occurrenceId)
                    put("medicationId", d.medicationId)
                    put("scheduledEpochMs", d.scheduledEpochMs)
                    put("doseQuantityRaw", d.doseQuantityRaw)
                    put("unit", d.unit)
                    put("status", d.status)
                    if (d.recordedEpochMs != null) put("recordedEpochMs", d.recordedEpochMs)
                    if (d.notes != null) put("notes", d.notes)
                    if (d.correctionNotes != null) put("correctionNotes", d.correctionNotes)
                    if (d.correctedAtEpochMs != null) put("correctedAtEpochMs", d.correctedAtEpochMs)
                })
            }
            put("doseEvents", dosesArray)

            // 4. Stock Lots
            val lotsArray = JSONArray()
            backup.stockLots.forEach { lot ->
                lotsArray.put(JSONObject().apply {
                    put("id", lot.id)
                    put("medicationId", lot.medicationId)
                    if (lot.lotNumber != null) put("lotNumber", lot.lotNumber)
                    put("initialQuantityRaw", lot.initialQuantityRaw)
                    put("remainingQuantityRaw", lot.remainingQuantityRaw)
                    put("unit", lot.unit)
                    if (lot.expiryDateEpochMs != null) put("expiryDateEpochMs", lot.expiryDateEpochMs)
                    put("createdAtEpochMs", lot.createdAtEpochMs)
                    if (lot.notes != null) put("notes", lot.notes)
                })
            }
            put("stockLots", lotsArray)

            // 5. Stock Ledger Entries
            val ledgerArray = JSONArray()
            backup.stockLedgerEntries.forEach { entry ->
                ledgerArray.put(JSONObject().apply {
                    put("id", entry.id)
                    put("medicationId", entry.medicationId)
                    put("timestampEpochMs", entry.timestampEpochMs)
                    put("movementType", entry.movementType)
                    put("quantityRaw", entry.quantityRaw)
                    put("unit", entry.unit)
                    if (entry.reason != null) put("reason", entry.reason)
                    if (entry.doseOccurrenceId != null) put("doseOccurrenceId", entry.doseOccurrenceId)
                    if (entry.restockLotNumber != null) put("restockLotNumber", entry.restockLotNumber)
                    if (entry.lotId != null) put("lotId", entry.lotId)
                    put("isEstimate", entry.isEstimate)
                })
            }
            put("stockLedgerEntries", ledgerArray)

            // 6. Prescriptions
            val prescriptionsArray = JSONArray()
            backup.prescriptions.forEach { p ->
                prescriptionsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("medicationId", p.medicationId)
                    if (p.referenceNumber != null) put("referenceNumber", p.referenceNumber)
                    put("issueDateEpochDay", p.issueDateEpochDay)
                    if (p.expiryDateEpochDay != null) put("expiryDateEpochDay", p.expiryDateEpochDay)
                    if (p.quantityPrescribedRaw != null) put("quantityPrescribedRaw", p.quantityPrescribedRaw)
                    if (p.unit != null) put("unit", p.unit)
                    put("repeatsAuthorised", p.repeatsAuthorised)
                    put("repeatsRemaining", p.repeatsRemaining)
                    put("hasPendingRepeatRequest", p.hasPendingRepeatRequest)
                    if (p.pharmacyName != null) put("pharmacyName", p.pharmacyName)
                    if (p.notes != null) put("notes", p.notes)
                    put("status", p.status)
                    put("createdAtEpochMs", p.createdAtEpochMs)
                    put("updatedAtEpochMs", p.updatedAtEpochMs)
                })
            }
            put("prescriptions", prescriptionsArray)

            // 7. Prescription Events
            val eventsArray = JSONArray()
            backup.prescriptionEvents.forEach { ev ->
                eventsArray.put(JSONObject().apply {
                    put("id", ev.id)
                    put("prescriptionId", ev.prescriptionId)
                    put("medicationId", ev.medicationId)
                    put("eventType", ev.eventType)
                    put("timestampEpochMs", ev.timestampEpochMs)
                    put("repeatsRemainingAfter", ev.repeatsRemainingAfter)
                    if (ev.actionIdempotencyKey != null) put("actionIdempotencyKey", ev.actionIdempotencyKey)
                    if (ev.notes != null) put("notes", ev.notes)
                })
            }
            put("prescriptionEvents", eventsArray)
        }

        return root.toString(2)
    }

    fun deserialize(jsonString: String): NoveloDataBackup {
        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            throw IllegalArgumentException("Malformed JSON backup document", e)
        }

        val formatVersion = root.optInt("formatVersion", 1)
        if (formatVersion > NoveloDataBackup.CURRENT_FORMAT_VERSION) {
            throw UnsupportedBackupFormatException(
                "Backup format version $formatVersion is newer than current supported version ${NoveloDataBackup.CURRENT_FORMAT_VERSION}"
            )
        }

        val schemaVersion = root.optInt("schemaVersion", 1)
        if (schemaVersion > NoveloDataBackup.CURRENT_SCHEMA_VERSION) {
            throw UnsupportedBackupFormatException(
                "Database schema version $schemaVersion is newer than current supported version ${NoveloDataBackup.CURRENT_SCHEMA_VERSION}"
            )
        }

        val appVersion = root.optString("appVersion", "1.0")
        val exportTimestampEpochMs = root.optLong("exportTimestampEpochMs", System.currentTimeMillis())

        // 1. Parse Medications
        val medications = mutableListOf<MedicationEntity>()
        val medsArray = root.optJSONArray("medications") ?: JSONArray()
        for (i in 0 until medsArray.length()) {
            val obj = medsArray.getJSONObject(i)
            medications.add(
                MedicationEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    strength = obj.optString("strength", ""),
                    form = obj.optString("form", "TABLET"),
                    stockUnit = obj.optString("stockUnit", "TABLETS"),
                    openingStockRaw = obj.optLong("openingStockRaw", 0L),
                    lowStockThresholdRaw = obj.optLong("lowStockThresholdRaw", 0L),
                    recordingMode = obj.optString("recordingMode", "CONFIRM"),
                    notes = obj.optString("notes", ""),
                    colorTagHex = obj.optString("colorTagHex", "#005EB8"),
                    isArchived = obj.optBoolean("isArchived", false),
                    createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                )
            )
        }

        // 2. Parse Schedules
        val schedules = mutableListOf<ScheduleEntity>()
        val schedulesArray = root.optJSONArray("schedules") ?: JSONArray()
        for (i in 0 until schedulesArray.length()) {
            val obj = schedulesArray.getJSONObject(i)
            schedules.add(
                ScheduleEntity(
                    id = obj.getString("id"),
                    medicationId = obj.getString("medicationId"),
                    scheduleType = obj.optString("scheduleType", "DAILY"),
                    targetTimesCsv = obj.optString("targetTimesCsv", ""),
                    daysOfWeekCsv = obj.optString("daysOfWeekCsv", ""),
                    doseQuantityRaw = obj.optLong("doseQuantityRaw", 1000L),
                    startDateEpochDay = obj.optLong("startDateEpochDay", 0L),
                    endDateEpochDay = if (obj.has("endDateEpochDay") && !obj.isNull("endDateEpochDay")) obj.getLong("endDateEpochDay") else null,
                    instructions = obj.optString("instructions", "")
                )
            )
        }

        // 3. Parse Dose Events
        val doseEvents = mutableListOf<DoseEventEntity>()
        val dosesArray = root.optJSONArray("doseEvents") ?: JSONArray()
        for (i in 0 until dosesArray.length()) {
            val obj = dosesArray.getJSONObject(i)
            doseEvents.add(
                DoseEventEntity(
                    occurrenceId = obj.getString("occurrenceId"),
                    medicationId = obj.getString("medicationId"),
                    scheduledEpochMs = obj.getLong("scheduledEpochMs"),
                    doseQuantityRaw = obj.optLong("doseQuantityRaw", 1000L),
                    unit = obj.optString("unit", "TABLETS"),
                    status = obj.getString("status"),
                    recordedEpochMs = if (obj.has("recordedEpochMs") && !obj.isNull("recordedEpochMs")) obj.getLong("recordedEpochMs") else null,
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                    correctionNotes = if (obj.has("correctionNotes") && !obj.isNull("correctionNotes")) obj.getString("correctionNotes") else null,
                    correctedAtEpochMs = if (obj.has("correctedAtEpochMs") && !obj.isNull("correctedAtEpochMs")) obj.getLong("correctedAtEpochMs") else null
                )
            )
        }

        // 4. Parse Stock Lots
        val stockLots = mutableListOf<StockLotEntity>()
        val lotsArray = root.optJSONArray("stockLots") ?: JSONArray()
        for (i in 0 until lotsArray.length()) {
            val obj = lotsArray.getJSONObject(i)
            stockLots.add(
                StockLotEntity(
                    id = obj.getString("id"),
                    medicationId = obj.getString("medicationId"),
                    lotNumber = if (obj.has("lotNumber") && !obj.isNull("lotNumber")) obj.getString("lotNumber") else null,
                    initialQuantityRaw = obj.getLong("initialQuantityRaw"),
                    remainingQuantityRaw = obj.getLong("remainingQuantityRaw"),
                    unit = obj.optString("unit", "TABLETS"),
                    expiryDateEpochMs = if (obj.has("expiryDateEpochMs") && !obj.isNull("expiryDateEpochMs")) obj.getLong("expiryDateEpochMs") else null,
                    createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null
                )
            )
        }

        // 5. Parse Stock Ledger Entries
        val stockLedgerEntries = mutableListOf<StockLedgerEntryEntity>()
        val ledgerArray = root.optJSONArray("stockLedgerEntries") ?: JSONArray()
        for (i in 0 until ledgerArray.length()) {
            val obj = ledgerArray.getJSONObject(i)
            stockLedgerEntries.add(
                StockLedgerEntryEntity(
                    id = obj.getString("id"),
                    medicationId = obj.getString("medicationId"),
                    timestampEpochMs = obj.getLong("timestampEpochMs"),
                    movementType = obj.getString("movementType"),
                    quantityRaw = obj.getLong("quantityRaw"),
                    unit = obj.optString("unit", "TABLETS"),
                    reason = if (obj.has("reason") && !obj.isNull("reason")) obj.getString("reason") else null,
                    doseOccurrenceId = if (obj.has("doseOccurrenceId") && !obj.isNull("doseOccurrenceId")) obj.getString("doseOccurrenceId") else null,
                    restockLotNumber = if (obj.has("restockLotNumber") && !obj.isNull("restockLotNumber")) obj.getString("restockLotNumber") else null,
                    lotId = if (obj.has("lotId") && !obj.isNull("lotId")) obj.getString("lotId") else null,
                    isEstimate = obj.optBoolean("isEstimate", false)
                )
            )
        }

        // 6. Parse Prescriptions
        val prescriptions = mutableListOf<PrescriptionEntity>()
        val prescriptionsArray = root.optJSONArray("prescriptions") ?: JSONArray()
        for (i in 0 until prescriptionsArray.length()) {
            val obj = prescriptionsArray.getJSONObject(i)
            prescriptions.add(
                PrescriptionEntity(
                    id = obj.getString("id"),
                    medicationId = obj.getString("medicationId"),
                    referenceNumber = if (obj.has("referenceNumber") && !obj.isNull("referenceNumber")) obj.getString("referenceNumber") else null,
                    issueDateEpochDay = obj.getLong("issueDateEpochDay"),
                    expiryDateEpochDay = if (obj.has("expiryDateEpochDay") && !obj.isNull("expiryDateEpochDay")) obj.getLong("expiryDateEpochDay") else null,
                    quantityPrescribedRaw = if (obj.has("quantityPrescribedRaw") && !obj.isNull("quantityPrescribedRaw")) obj.getLong("quantityPrescribedRaw") else null,
                    unit = if (obj.has("unit") && !obj.isNull("unit")) obj.getString("unit") else null,
                    repeatsAuthorised = obj.optInt("repeatsAuthorised", 0),
                    repeatsRemaining = obj.optInt("repeatsRemaining", 0),
                    hasPendingRepeatRequest = obj.optBoolean("hasPendingRepeatRequest", false),
                    pharmacyName = if (obj.has("pharmacyName") && !obj.isNull("pharmacyName")) obj.getString("pharmacyName") else null,
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                    status = obj.optString("status", "ACTIVE"),
                    createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                )
            )
        }

        // 7. Parse Prescription Events
        val prescriptionEvents = mutableListOf<PrescriptionEventEntity>()
        val eventsArray = root.optJSONArray("prescriptionEvents") ?: JSONArray()
        for (i in 0 until eventsArray.length()) {
            val obj = eventsArray.getJSONObject(i)
            prescriptionEvents.add(
                PrescriptionEventEntity(
                    id = obj.getString("id"),
                    prescriptionId = obj.getString("prescriptionId"),
                    medicationId = obj.getString("medicationId"),
                    eventType = obj.getString("eventType"),
                    timestampEpochMs = obj.getLong("timestampEpochMs"),
                    repeatsRemainingAfter = obj.getInt("repeatsRemainingAfter"),
                    actionIdempotencyKey = if (obj.has("actionIdempotencyKey") && !obj.isNull("actionIdempotencyKey")) obj.getString("actionIdempotencyKey") else null,
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null
                )
            )
        }

        return NoveloDataBackup(
            formatVersion = formatVersion,
            appVersion = appVersion,
            schemaVersion = schemaVersion,
            exportTimestampEpochMs = exportTimestampEpochMs,
            medications = medications,
            schedules = schedules,
            doseEvents = doseEvents,
            stockLots = stockLots,
            stockLedgerEntries = stockLedgerEntries,
            prescriptions = prescriptions,
            prescriptionEvents = prescriptionEvents
        )
    }
}
