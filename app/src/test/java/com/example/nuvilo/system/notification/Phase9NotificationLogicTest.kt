package com.example.nuvilo.system.notification

import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9NotificationLogicTest {

    @Test
    fun medicationReminderChannelSpec_defaultsToSilentWithVibrationEnabled() {
        val specs = NotificationChannelRegistry.allChannelSpecs()
        assertEquals(7, specs.size)

        val reminderSpec = specs.first { it.id == NotificationChannelIds.MEDICATION_REMINDERS }
        assertTrue(reminderSpec.defaultSilent)
        assertTrue(reminderSpec.defaultVibrationEnabled)

        val expectedIds = setOf(
            NotificationChannelIds.MEDICATION_REMINDERS,
            NotificationChannelIds.UPCOMING_REMINDERS,
            NotificationChannelIds.LOW_STOCK_ALERTS,
            NotificationChannelIds.EXPIRY_WARNINGS,
            NotificationChannelIds.OPENED_DISCARD_WARNINGS,
            NotificationChannelIds.PRESCRIPTION_REMINDERS,
            NotificationChannelIds.SECURITY_EVENTS
        )
        assertEquals(expectedIds, specs.map { it.id }.toSet())
    }

    @Test
    fun privacyMasking_stripsMedicationNameAndEyeSiteAndPackageDetailsFromAllAlerts() {
        val hydro = Medication(
            id = "med-eye",
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ofWhole(300),
            lowStockThreshold = ScaledQuantity.ofWhole(30),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.BOTTLE,
                consumptionUnit = StockUnit.DROPS,
                contentsPerPackage = ScaledQuantity.ofWhole(300),
                discardAfterOpeningDays = 28,
                trackingPrecision = TrackingPrecision.ESTIMATED
            ),
            administrationProfile = AdministrationProfile(
                site = AdministrationSite.BOTH_EYES,
                leftEyeQuantity = ScaledQuantity.ofWhole(1),
                rightEyeQuantity = ScaledQuantity.ofWhole(1)
            )
        )
        val openedBottle = StockLot(
            id = "lot-1",
            medicationId = hydro.id,
            lotNumber = "HF-9",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(42),
            unit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            packageLabel = "Bottle #1",
            packageStatus = PackageStatus.OPENED
        )

        val expiryMasked = NotificationTextBuilder.buildExpiryAlert(
            medication = hydro,
            lot = openedBottle,
            daysUntilExpiry = 2,
            isPrivacyMasked = true
        )
        assertFalse(expiryMasked.title.contains("Hydro Forte"))
        assertFalse(expiryMasked.body.contains("Hydro Forte"))
        assertFalse(expiryMasked.body.contains("drops"))

        val discardMasked = NotificationTextBuilder.buildOpenedDiscardAlert(
            medication = hydro,
            lot = openedBottle,
            daysUntilDiscard = 0,
            isPrivacyMasked = true
        )
        assertFalse(discardMasked.title.contains("Hydro Forte"))
        assertFalse(discardMasked.body.contains("Hydro Forte"))
        assertFalse(discardMasked.body.contains("eye"))
    }

    @Test
    fun unmaskedEyeDropAlerts_includeSiteAndPackageContext() {
        val hydro = Medication(
            id = "med-eye",
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ofWhole(300),
            lowStockThreshold = ScaledQuantity.ofWhole(30),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.BOTTLE,
                consumptionUnit = StockUnit.DROPS,
                contentsPerPackage = ScaledQuantity.ofWhole(300),
                discardAfterOpeningDays = 28,
                trackingPrecision = TrackingPrecision.ESTIMATED
            ),
            administrationProfile = AdministrationProfile(
                site = AdministrationSite.BOTH_EYES,
                leftEyeQuantity = ScaledQuantity.ofWhole(1),
                rightEyeQuantity = ScaledQuantity.ofWhole(1)
            )
        )
        val bottle = StockLot(
            id = "lot-1",
            medicationId = hydro.id,
            lotNumber = "HF-9",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(42),
            unit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            packageLabel = "Bottle #1",
            packageStatus = PackageStatus.OPENED
        )

        val line = NotificationTextBuilder.buildDoseInstructionLine(hydro, ScaledQuantity.ofWhole(2))
        assertTrue(line.contains("Both eyes"))
        assertTrue(line.contains("1 left"))
        assertTrue(line.contains("1 right"))

        val discardMsg = NotificationTextBuilder.buildOpenedDiscardAlert(
            medication = hydro,
            lot = bottle,
            daysUntilDiscard = 1,
            isPrivacyMasked = false
        )
        assertTrue(discardMsg.title.contains("Hydro Forte"))
        assertTrue(discardMsg.body.contains("Bottle #1"))
        assertTrue(discardMsg.body.contains("1 day"))
        assertNull(NotificationChannelRegistry.allChannelSpecs().first { it.id == NotificationChannelIds.MEDICATION_REMINDERS }.customSoundUri)

        val lowStockMasked = NotificationTextBuilder.buildLowStockAlert(
            medicationName = "Hydro Forte",
            currentBalance = ScaledQuantity.ofWhole(12),
            unit = StockUnit.DROPS,
            daysRemaining = 3,
            isPrivacyMasked = true,
            precision = TrackingPrecision.ESTIMATED
        )
        assertFalse(lowStockMasked.title.contains("Hydro Forte"))
        assertFalse(lowStockMasked.body.contains("Hydro Forte"))
        assertFalse(lowStockMasked.body.contains("12 drops"))
    }
}
