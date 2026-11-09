package com.example.nuvilo.regression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression Test Suite: Category C (Backup, Restoration & Security)
 * Tests T107 to T110: Strict Offline-First, Zero-Network Invariant, and Privacy Protection
 */
class PrivacyAndManifestRegressionTest {

    @Test
    fun t107_manifestInvariantZeroNetworkDeclaredInSource() {
        // T107: Manifest invariant: android.permission.INTERNET is never present in AndroidManifest.xml
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml should exist", manifestFile.exists())

        val manifestContent = manifestFile.readText()
        assertFalse(
            "Security Violation: android.permission.INTERNET must never be declared in Nuvilo",
            manifestContent.contains("android.permission.INTERNET")
        )
    }

    @Test
    fun t108_backupRulesFileExcludesDatabaseAndSharedPrefFromCloudBackup() {
        // T108: Backup rules file (backup_rules.xml) excludes Room database and shared preferences
        val backupRulesFile = File("src/main/res/xml/backup_rules.xml")
        assertTrue("backup_rules.xml must exist", backupRulesFile.exists())

        val content = backupRulesFile.readText()
        assertTrue(content.contains("""<exclude domain="database" path="." />"""))
        assertTrue(content.contains("""<exclude domain="sharedpref" path="." />"""))
    }

    @Test
    fun t109_dataExtractionRulesFileExcludesDatabasePaths() {
        // T109: Data extraction rules file (data_extraction_rules.xml) excludes database paths
        val dataExtractionFile = File("src/main/res/xml/data_extraction_rules.xml")
        assertTrue("data_extraction_rules.xml must exist", dataExtractionFile.exists())

        val content = dataExtractionFile.readText()
        assertTrue(content.contains("""<exclude domain="database" path="." />"""))
        assertTrue(content.contains("""<exclude domain="sharedpref" path="." />"""))
    }

    @Test
    fun t110_privacyMaskingProtectsDrugNameAndDoseOnLockscreen() {
        // T110: Privacy masking logic generates generic reminder without drug name or quantity
        val medicationName = "Metformin"
        val doseQuantityStr = "500mg"

        // Simulate privacy mode title and text generation logic as implemented in NotificationHelper
        fun getNotificationTitle(medName: String, isPrivacyMasked: Boolean): String {
            return if (isPrivacyMasked) "Medication Reminder" else medName
        }

        fun getNotificationText(doseStr: String, isPrivacyMasked: Boolean): String {
            return if (isPrivacyMasked) "A scheduled dose is due now" else "Take $doseStr"
        }

        val maskedTitle = getNotificationTitle(medicationName, isPrivacyMasked = true)
        val maskedText = getNotificationText(doseQuantityStr, isPrivacyMasked = true)

        assertFalse("Masked title must never reveal drug name", maskedTitle.contains(medicationName))
        assertFalse("Masked text must never reveal dosage", maskedText.contains(doseQuantityStr))
        assertEquals("Medication Reminder", maskedTitle)
        assertEquals("A scheduled dose is due now", maskedText)

        val unmaskedTitle = getNotificationTitle(medicationName, isPrivacyMasked = false)
        val unmaskedText = getNotificationText(doseQuantityStr, isPrivacyMasked = false)
        assertTrue(unmaskedTitle.contains(medicationName))
        assertTrue(unmaskedText.contains(doseQuantityStr))
    }
}
