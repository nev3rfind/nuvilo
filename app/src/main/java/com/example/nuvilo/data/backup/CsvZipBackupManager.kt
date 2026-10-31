package com.example.nuvilo.data.backup

import com.example.nuvilo.data.local.entity.DoseEventEntity
import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.PrescriptionEntity
import com.example.nuvilo.data.local.entity.PrescriptionEventEntity
import com.example.nuvilo.data.local.entity.ScheduleEntity
import com.example.nuvilo.data.local.entity.StockLedgerEntryEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

object CsvZipBackupManager {

    private const val NULL_TOKEN = "\\N"

    fun exportToZip(backup: NoveloDataBackup, outputStream: OutputStream) {
        val zipOut = ZipOutputStream(outputStream)
        val writer = OutputStreamWriter(zipOut, StandardCharsets.UTF_8)

        fun writeEntry(name: String, content: String) {
            zipOut.putNextEntry(ZipEntry(name))
            writer.write(content)
            writer.flush()
            zipOut.closeEntry()
        }

        // 1. manifest.json
        val manifest = JSONObject().apply {
            put("formatVersion", backup.formatVersion)
            put("schemaVersion", backup.schemaVersion)
            put("appVersion", backup.appVersion)
            put("exportTimestampEpochMs", backup.exportTimestampEpochMs)
            put("tables", JSONObject().apply {
                put("medications", backup.medications.size)
                put("schedules", backup.schedules.size)
                put("dose_events", backup.doseEvents.size)
                put("stock_lots", backup.stockLots.size)
                put("stock_ledger_entries", backup.stockLedgerEntries.size)
                put("prescriptions", backup.prescriptions.size)
                put("prescription_events", backup.prescriptionEvents.size)
            })
        }
        writeEntry("manifest.json", manifest.toString(2))

        // 2. medications.csv
        val medRows = mutableListOf<List<String>>()
        medRows.add(listOf("id", "name", "strength", "form", "stock_unit", "opening_stock_raw", "low_stock_threshold_raw", "recording_mode", "notes", "color_tag_hex", "is_archived", "created_at_epoch_ms"))
        backup.medications.forEach { m ->
            medRows.add(listOf(
                m.id, m.name, m.strength, m.form, m.stockUnit,
                m.openingStockRaw.toString(), m.lowStockThresholdRaw.toString(),
                m.recordingMode, m.notes, m.colorTagHex,
                m.isArchived.toString(), m.createdAtEpochMs.toString()
            ))
        }
        writeEntry("medications.csv", buildCsv(medRows))

        // 3. schedules.csv
        val schedRows = mutableListOf<List<String>>()
        schedRows.add(listOf("id", "medication_id", "schedule_type", "target_times_csv", "days_of_week_csv", "dose_quantity_raw", "start_date_epoch_day", "end_date_epoch_day", "instructions"))
        backup.schedules.forEach { s ->
            schedRows.add(listOf(
                s.id, s.medicationId, s.scheduleType, s.targetTimesCsv, s.daysOfWeekCsv,
                s.doseQuantityRaw.toString(), s.startDateEpochDay.toString(),
                s.endDateEpochDay?.toString() ?: NULL_TOKEN, s.instructions
            ))
        }
        writeEntry("schedules.csv", buildCsv(schedRows))

        // 4. dose_events.csv
        val doseRows = mutableListOf<List<String>>()
        doseRows.add(listOf("occurrence_id", "medication_id", "scheduled_epoch_ms", "dose_quantity_raw", "unit", "status", "recorded_epoch_ms", "notes", "correction_notes", "corrected_at_epoch_ms"))
        backup.doseEvents.forEach { d ->
            doseRows.add(listOf(
                d.occurrenceId, d.medicationId, d.scheduledEpochMs.toString(),
                d.doseQuantityRaw.toString(), d.unit, d.status,
                d.recordedEpochMs?.toString() ?: NULL_TOKEN,
                d.notes ?: NULL_TOKEN,
                d.correctionNotes ?: NULL_TOKEN,
                d.correctedAtEpochMs?.toString() ?: NULL_TOKEN
            ))
        }
        writeEntry("dose_events.csv", buildCsv(doseRows))

        // 5. stock_lots.csv
        val lotRows = mutableListOf<List<String>>()
        lotRows.add(listOf("id", "medication_id", "lot_number", "initial_quantity_raw", "remaining_quantity_raw", "unit", "expiry_date_epoch_ms", "created_at_epoch_ms", "notes"))
        backup.stockLots.forEach { l ->
            lotRows.add(listOf(
                l.id, l.medicationId, l.lotNumber ?: NULL_TOKEN,
                l.initialQuantityRaw.toString(), l.remainingQuantityRaw.toString(),
                l.unit, l.expiryDateEpochMs?.toString() ?: NULL_TOKEN,
                l.createdAtEpochMs.toString(), l.notes ?: NULL_TOKEN
            ))
        }
        writeEntry("stock_lots.csv", buildCsv(lotRows))

        // 6. stock_ledger_entries.csv
        val ledgerRows = mutableListOf<List<String>>()
        ledgerRows.add(listOf("id", "medication_id", "timestamp_epoch_ms", "movement_type", "quantity_raw", "unit", "reason", "dose_occurrence_id", "restock_lot_number", "lot_id", "is_estimate"))
        backup.stockLedgerEntries.forEach { e ->
            ledgerRows.add(listOf(
                e.id, e.medicationId, e.timestampEpochMs.toString(),
                e.movementType, e.quantityRaw.toString(), e.unit,
                e.reason ?: NULL_TOKEN, e.doseOccurrenceId ?: NULL_TOKEN,
                e.restockLotNumber ?: NULL_TOKEN, e.lotId ?: NULL_TOKEN,
                e.isEstimate.toString()
            ))
        }
        writeEntry("stock_ledger_entries.csv", buildCsv(ledgerRows))

        // 7. prescriptions.csv
        val rxRows = mutableListOf<List<String>>()
        rxRows.add(listOf("id", "medication_id", "reference_number", "issue_date_epoch_day", "expiry_date_epoch_day", "quantity_prescribed_raw", "unit", "repeats_authorised", "repeats_remaining", "has_pending_repeat_request", "pharmacy_name", "notes", "status", "created_at_epoch_ms", "updated_at_epoch_ms"))
        backup.prescriptions.forEach { p ->
            rxRows.add(listOf(
                p.id, p.medicationId, p.referenceNumber ?: NULL_TOKEN,
                p.issueDateEpochDay.toString(), p.expiryDateEpochDay?.toString() ?: NULL_TOKEN,
                p.quantityPrescribedRaw?.toString() ?: NULL_TOKEN, p.unit ?: NULL_TOKEN,
                p.repeatsAuthorised.toString(), p.repeatsRemaining.toString(),
                p.hasPendingRepeatRequest.toString(), p.pharmacyName ?: NULL_TOKEN,
                p.notes ?: NULL_TOKEN, p.status, p.createdAtEpochMs.toString(),
                p.updatedAtEpochMs.toString()
            ))
        }
        writeEntry("prescriptions.csv", buildCsv(rxRows))

        // 8. prescription_events.csv
        val rxEventRows = mutableListOf<List<String>>()
        rxEventRows.add(listOf("id", "prescription_id", "medication_id", "event_type", "timestamp_epoch_ms", "repeats_remaining_after", "action_idempotency_key", "notes"))
        backup.prescriptionEvents.forEach { ev ->
            rxEventRows.add(listOf(
                ev.id, ev.prescriptionId, ev.medicationId,
                ev.eventType, ev.timestampEpochMs.toString(),
                ev.repeatsRemainingAfter.toString(),
                ev.actionIdempotencyKey ?: NULL_TOKEN,
                ev.notes ?: NULL_TOKEN
            ))
        }
        writeEntry("prescription_events.csv", buildCsv(rxEventRows))

        zipOut.finish()
    }

    fun importFromZip(bytes: ByteArray): NoveloDataBackup {
        return importFromZip(ByteArrayInputStream(bytes))
    }

    fun importFromZip(inputStream: InputStream): NoveloDataBackup {
        val zipIn = ZipInputStream(inputStream)
        val files = mutableMapOf<String, String>()

        var entry = zipIn.nextEntry
        while (entry != null) {
            val content = BufferedReader(InputStreamReader(zipIn, StandardCharsets.UTF_8)).readText()
            files[entry.name] = content
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        val manifestContent = files["manifest.json"]
            ?: throw IllegalArgumentException("Missing manifest.json in backup zip archive")
        val manifest = JSONObject(manifestContent)

        val formatVersion = manifest.optInt("formatVersion", 1)
        if (formatVersion > NoveloDataBackup.CURRENT_FORMAT_VERSION) {
            throw UnsupportedBackupFormatException(
                "Backup format version $formatVersion is newer than current supported version ${NoveloDataBackup.CURRENT_FORMAT_VERSION}"
            )
        }

        val schemaVersion = manifest.optInt("schemaVersion", 1)
        if (schemaVersion > NoveloDataBackup.CURRENT_SCHEMA_VERSION) {
            throw UnsupportedBackupFormatException(
                "Database schema version $schemaVersion is newer than current supported version ${NoveloDataBackup.CURRENT_SCHEMA_VERSION}"
            )
        }

        val appVersion = manifest.optString("appVersion", "1.0")
        val exportTimestampEpochMs = manifest.optLong("exportTimestampEpochMs", System.currentTimeMillis())

        // 1. Medications
        val medications = mutableListOf<MedicationEntity>()
        files["medications.csv"]?.let { csv ->
            val rows = parseCsv(csv)
            for (i in 1 until rows.size) {
                val r = rows[i]
                if (r.isEmpty()) continue
                medications.add(
                    MedicationEntity(
                        id = r[0],
                        name = r[1],
                        strength = r.getOrElse(2) { "" },
                        form = r.getOrElse(3) { "TABLET" },
                        stockUnit = r.getOrElse(4) { "TABLETS" },
                        openingStockRaw = r.getOrElse(5) { "0" }.toLong(),
                        lowStockThresholdRaw = r.getOrElse(6) { "0" }.toLong(),
                        recordingMode = r.getOrElse(7) { "CONFIRM" },
                        notes = r.getOrElse(8) { "" },
                        colorTagHex = r.getOrElse(9) { "#005EB8" },
                        isArchived = r.getOrElse(10) { "false" }.toBoolean(),
                        createdAtEpochMs = r.getOrElse(11) { System.currentTimeMillis().toString() }.toLong()
                    )
                )
            }
        }

        // 2. Schedules
        val schedules = mutableListOf<ScheduleEntity>()
        files["schedules.csv"]?.let { csv ->
            val rows = parseCsv(csv)
            for (i in 1 until rows.size) {
                val r = rows[i]
                if (r.isEmpty()) continue
                schedules.add(
                    ScheduleEntity(
                        id = r[0],
                        medicationId = r[1],
                        scheduleType = r.getOrElse(2) { "DAILY" },
                        targetTimesCsv = r.getOrElse(3) { "" },
                        daysOfWeekCsv = r.getOrElse(4) { "" },
                        doseQuantityRaw = r.getOrElse(5) { "1000" }.toLong(),
                        startDateEpochDay = r.getOrElse(6) { "0" }.toLong(),
                        endDateEpochDay = parseNullableLong(r.getOrNull(7)),
                        instructions = r.getOrElse(8) { "" }
                    )
                )
            }
        }

        // 3. Dose Events
        val doseEvents = mutableListOf<DoseEventEntity>()
        files["dose_events.csv"]?.let { csv ->
            val rows = parseCsv(csv)
            for (i in 1 until rows.size) {
                val r = rows[i]
                if (r.isEmpty()) continue
                doseEvents.add(
                    DoseEventEntity(
                        occurrenceId = r[0],
                        medicationId = r[1],
                        scheduledEpochMs = r[2].toLong(),
                        doseQuantityRaw = r[3].toLong(),
                        unit = r.getOrElse(4) { "TABLETS" },
                        status = r[5],
                        recordedEpochMs = parseNullableLong(r.getOrNull(6)),
                        notes = parseNullableString(r.getOrNull(7)),
                        correctionNotes = parseNullableString(r.getOrNull(8)),
                        correctedAtEpochMs = parseNullableLong(r.getOrNull(9))
                    )
                )
            }
        }

        // 4. Stock Lots
        val stockLots = mutableListOf<StockLotEntity>()
        files["stock_lots.csv"]?.let { csv ->
            val rows = parseCsv(csv)
            for (i in 1 until rows.size) {
                val r = rows[i]
                if (r.isEmpty()) continue
                stockLots.add(
                    StockLotEntity(
                        id = r[0],
                        medicationId = r[1],
                        lotNumber = parseNullableString(r.getOrNull(2)),
                        initialQuantityRaw = r[3].toLong(),
                        remainingQuantityRaw = r[4].toLong(),
                        unit = r.getOrElse(5) { "TABLETS" },
                        expiryDateEpochMs = parseNullableLong(r.getOrNull(6)),
                        createdAtEpochMs = r.getOrElse(7) { System.currentTimeMillis().toString() }.toLong(),
                        notes = parseNullableString(r.getOrNull(8))
                    )
                )
            }
        }

        // 5. Stock Ledger Entries
        val stockLedgerEntries = mutableListOf<StockLedgerEntryEntity>()
        files["stock_ledger_entries.csv"]?.let { csv ->
            val rows = parseCsv(csv)
            for (i in 1 until rows.size) {
                val r = rows[i]
                if (r.isEmpty()) continue
                stockLedgerEntries.add(
                    StockLedgerEntryEntity(
                        id = r[0],
                        medicationId = r[1],
                        timestampEpochMs = r[2].toLong(),
                        movementType = r[3],
                        quantityRaw = r[4].toLong(),
                        unit = r.getOrElse(5) { "TABLETS" },
                        reason = parseNullableString(r.getOrNull(6)),
                        doseOccurrenceId = parseNullableString(r.getOrNull(7)),
                        restockLotNumber = parseNullableString(r.getOrNull(8)),
                        lotId = parseNullableString(r.getOrNull(9)),
                        isEstimate = r.getOrElse(10) { "false" }.toBoolean()
                    )
                )
            }
        }

        // 6. Prescriptions
        val prescriptions = mutableListOf<PrescriptionEntity>()
        files["prescriptions.csv"]?.let { csv ->
            val rows = parseCsv(csv)
            for (i in 1 until rows.size) {
                val r = rows[i]
                if (r.isEmpty()) continue
                prescriptions.add(
                    PrescriptionEntity(
                        id = r[0],
                        medicationId = r[1],
                        referenceNumber = parseNullableString(r.getOrNull(2)),
                        issueDateEpochDay = r[3].toLong(),
                        expiryDateEpochDay = parseNullableLong(r.getOrNull(4)),
                        quantityPrescribedRaw = parseNullableLong(r.getOrNull(5)),
                        unit = parseNullableString(r.getOrNull(6)),
                        repeatsAuthorised = r.getOrElse(7) { "0" }.toInt(),
                        repeatsRemaining = r.getOrElse(8) { "0" }.toInt(),
                        hasPendingRepeatRequest = r.getOrElse(9) { "false" }.toBoolean(),
                        pharmacyName = parseNullableString(r.getOrNull(10)),
                        notes = parseNullableString(r.getOrNull(11)),
                        status = r.getOrElse(12) { "ACTIVE" },
                        createdAtEpochMs = r.getOrElse(13) { System.currentTimeMillis().toString() }.toLong(),
                        updatedAtEpochMs = r.getOrElse(14) { System.currentTimeMillis().toString() }.toLong()
                    )
                )
            }
        }

        // 7. Prescription Events
        val prescriptionEvents = mutableListOf<PrescriptionEventEntity>()
        files["prescription_events.csv"]?.let { csv ->
            val rows = parseCsv(csv)
            for (i in 1 until rows.size) {
                val r = rows[i]
                if (r.isEmpty()) continue
                prescriptionEvents.add(
                    PrescriptionEventEntity(
                        id = r[0],
                        prescriptionId = r[1],
                        medicationId = r[2],
                        eventType = r[3],
                        timestampEpochMs = r[4].toLong(),
                        repeatsRemainingAfter = r[5].toInt(),
                        actionIdempotencyKey = parseNullableString(r.getOrNull(6)),
                        notes = parseNullableString(r.getOrNull(7))
                    )
                )
            }
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

    private fun parseNullableString(value: String?): String? {
        return if (value == null || value == NULL_TOKEN) null else value
    }

    private fun parseNullableLong(value: String?): Long? {
        return if (value == null || value == NULL_TOKEN) null else value.toLongOrNull()
    }

    private fun buildCsv(rows: List<List<String>>): String {
        val sb = StringBuilder()
        rows.forEach { row ->
            val line = row.joinToString(",") { escapeCsv(it) }
            sb.append(line).append("\r\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsv(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < csv.length) {
            val c = csv[i]
            when {
                c == '\"' -> {
                    if (insideQuotes && i + 1 < csv.length && csv[i + 1] == '\"') {
                        currentField.append('\"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                c == ',' && !insideQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                }
                (c == '\r' || c == '\n') && !insideQuotes -> {
                    if (c == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') {
                        i++ // Skip \n in \r\n
                    }
                    currentRow.add(currentField.toString())
                    currentField.clear()
                    if (currentRow.isNotEmpty() && !(currentRow.size == 1 && currentRow[0].isEmpty())) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }

        return rows
    }
}
