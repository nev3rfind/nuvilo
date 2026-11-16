package com.example.nuvilo.data.backup

import com.example.nuvilo.data.local.entity.PpcCertificateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class PpcBackupCompatibilityTest {

    private val samplePpcs = listOf(
        PpcCertificateEntity(
            id = "ppc-1",
            coverageType = "PPC_12_MONTH",
            exemptionCategory = null,
            startDateEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            expiryDateEpochDay = LocalDate.of(2026, 12, 31).toEpochDay(),
            isOngoing = false,
            isManualExpiryOverride = false,
            referenceNumber = "PPC-123456",
            notes = "Annual renewal",
            remind30Days = true,
            remind14Days = true,
            remind7Days = true,
            remindOnExpiry = true,
            reminderHour = 9,
            reminderMinute = 0,
            isArchived = false,
            renewedFromId = null,
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        ),
        PpcCertificateEntity(
            id = "ppc-2",
            coverageType = "FREE_EXEMPTION",
            exemptionCategory = "MEDICAL",
            startDateEpochDay = LocalDate.of(2025, 5, 1).toEpochDay(),
            expiryDateEpochDay = null,
            isOngoing = true,
            isManualExpiryOverride = false,
            referenceNumber = null,
            notes = null,
            remind30Days = false,
            remind14Days = false,
            remind7Days = false,
            remindOnExpiry = false,
            reminderHour = 9,
            reminderMinute = 0,
            isArchived = true,
            renewedFromId = "ppc-0",
            createdAtEpochMs = 1700000001000L,
            updatedAtEpochMs = 1700000002000L
        )
    )

    @Test
    fun jsonBackup_roundTripsPpcCertificates() {
        val backup = NoveloDataBackup(
            schemaVersion = 5,
            ppcCertificates = samplePpcs
        )
        val json = JsonBackupManager.serialize(backup)
        val restored = JsonBackupManager.deserialize(json)

        assertEquals(2, restored.ppcCertificates.size)
        assertEquals(samplePpcs[0], restored.ppcCertificates[0])
        assertEquals(samplePpcs[1], restored.ppcCertificates[1])
    }

    @Test
    fun csvZipBackup_roundTripsPpcCertificates() {
        val backup = NoveloDataBackup(
            schemaVersion = 5,
            ppcCertificates = samplePpcs
        )
        val out = ByteArrayOutputStream()
        CsvZipBackupManager.exportToZip(backup, out)
        val restored = CsvZipBackupManager.importFromZip(out.toByteArray())

        assertEquals(2, restored.ppcCertificates.size)
        assertEquals(samplePpcs[0], restored.ppcCertificates[0])
        assertEquals(samplePpcs[1], restored.ppcCertificates[1])
    }

    @Test
    fun jsonBackup_backwardCompatibleWithSchemaV4WithoutPpcArray() {
        val v4Json = """
            {
              "formatVersion": 1,
              "appVersion": "1.0",
              "schemaVersion": 4,
              "exportTimestampEpochMs": 1700000000000,
              "medications": [],
              "schedules": [],
              "doseEvents": [],
              "stockLots": [],
              "stockLedgerEntries": [],
              "prescriptions": [],
              "prescriptionEvents": []
            }
        """.trimIndent()

        val restored = JsonBackupManager.deserialize(v4Json)
        assertEquals(4, restored.schemaVersion)
        assertTrue(restored.ppcCertificates.isEmpty())
        val validation = BackupValidator.validate(restored)
        assertTrue(validation is BackupValidationResult.Valid)
    }

    @Test
    fun backupValidator_rejectsDuplicatePpcIdAndCountsPreview() {
        val valid = BackupValidator.validate(NoveloDataBackup(ppcCertificates = samplePpcs))
        assertTrue(valid is BackupValidationResult.Valid)
        assertEquals(2, (valid as BackupValidationResult.Valid).preview.ppcCertificateCount)

        val duplicate = BackupValidator.validate(
            NoveloDataBackup(ppcCertificates = listOf(samplePpcs[0], samplePpcs[0]))
        )
        assertTrue(duplicate is BackupValidationResult.Invalid)
    }
}
