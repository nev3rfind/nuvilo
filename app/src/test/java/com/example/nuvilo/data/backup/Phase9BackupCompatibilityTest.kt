package com.example.nuvilo.data.backup

import com.example.nuvilo.data.local.entity.MedicationEntity
import com.example.nuvilo.data.local.entity.StockLotEntity
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9BackupCompatibilityTest {

    @Test
    fun legacySchemaV3JsonBackup_deserializesIntoV4EntitiesSafely() {
        val v3Json = """
            {
              "formatVersion": 1,
              "appVersion": "1.0.0",
              "schemaVersion": 3,
              "exportTimestampEpochMs": 1760000000000,
              "medications": [
                {
                  "id": "med-hydro-v3",
                  "name": "Hydro Forte",
                  "strength": "0.3%",
                  "form": "DROPS",
                  "stockUnit": "DROPS",
                  "openingStockRaw": 300000,
                  "lowStockThresholdRaw": 30000,
                  "recordingMode": "CONFIRM",
                  "notes": "",
                  "colorTagHex": "#005EB8",
                  "isArchived": false,
                  "createdAtEpochMs": 1759000000000
                }
              ],
              "schedules": [],
              "doseEvents": [],
              "stockLots": [
                {
                  "id": "lot-v3",
                  "medicationId": "med-hydro-v3",
                  "lotNumber": "OLD-LOT",
                  "initialQuantityRaw": 300000,
                  "remainingQuantityRaw": 120000,
                  "unit": "DROPS",
                  "expiryDateEpochMs": 1790000000000,
                  "createdAtEpochMs": 1759000000000,
                  "notes": "V3 bottle"
                }
              ],
              "stockLedgerEntries": [],
              "prescriptions": [],
              "prescriptionEvents": []
            }
        """.trimIndent()

        val parsed = JsonBackupManager.deserializeBackup(v3Json)
        assertEquals(3, parsed.schemaVersion)
        assertEquals(1, parsed.medications.size)
        val med = parsed.medications.single()
        assertEquals("Hydro Forte", med.name)
        assertNull(med.packageType)
        assertNull(med.contentsPerPackageRaw)
        assertEquals("NONE", med.administrationSite)

        val lot = parsed.stockLots.single()
        // Partial legacy lot infers OPENED safely or defaults cleanly
        assertEquals("OLD-LOT", lot.lotNumber)
        assertEquals(120000L, lot.remainingQuantityRaw)
    }

    @Test
    fun schemaV4JsonAndCsvZip_roundTripPreservesAllPackagingAndPackageLifecycleFields() {
        val med = MedicationEntity(
            id = "med-eye-v4",
            name = "Hydro Forte",
            strength = "0.3%",
            form = "DROPS",
            stockUnit = "DROPS",
            openingStockRaw = 600_000L,
            lowStockThresholdRaw = 30_000L,
            recordingMode = "CONFIRM",
            notes = "Keep refrigerated",
            colorTagHex = "#005EB8",
            isArchived = false,
            createdAtEpochMs = 1760000000000L,
            packageType = "BOTTLE",
            contentsPerPackageRaw = 300_000L,
            netContentLabel = "10 ml",
            discardAfterOpeningDays = 28,
            trackingPrecision = "ESTIMATED",
            gelTrackingMode = null,
            administrationSite = "BOTH_EYES",
            leftEyeQuantityRaw = 1_000L,
            rightEyeQuantityRaw = 1_000L,
            containersPerAdminRaw = null,
            lowStockDaysThreshold = 7
        )

        val lot = StockLotEntity(
            id = "lot-eye-1",
            medicationId = "med-eye-v4",
            lotNumber = "BTL-2026",
            initialQuantityRaw = 300_000L,
            remainingQuantityRaw = 240_000L,
            unit = "DROPS",
            expiryDateEpochMs = 1800000000000L,
            createdAtEpochMs = 1760000000000L,
            notes = "Opened bottle #1",
            packageType = "BOTTLE",
            packageLabel = "Bottle #1",
            packageStatus = "OPENED",
            openedAtEpochMs = 1760100000000L,
            discardAfterOpeningDays = 28,
            discardAfterOpeningEpochMs = 1762519200000L,
            discardedAtEpochMs = null,
            discardReason = null
        )

        val backup = NoveloDataBackup(
            schemaVersion = 4,
            medications = listOf(med),
            stockLots = listOf(lot)
        )

        val jsonOut = JsonBackupManager.serializeBackup(backup)
        val fromJson = JsonBackupManager.deserializeBackup(jsonOut)
        assertEquals(med, fromJson.medications.single())
        assertEquals(lot, fromJson.stockLots.single())

        val zipStream = ByteArrayOutputStream()
        CsvZipBackupManager.exportToZip(backup, zipStream)
        val fromZip = CsvZipBackupManager.importFromZip(zipStream.toByteArray())
        assertEquals(med, fromZip.medications.single())
        assertEquals(lot, fromZip.stockLots.single())
        assertTrue(BackupValidator.validate(fromZip) is BackupValidationResult.Valid)
    }
}
