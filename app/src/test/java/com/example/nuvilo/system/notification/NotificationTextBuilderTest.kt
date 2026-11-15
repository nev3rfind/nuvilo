package com.example.nuvilo.system.notification

import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTextBuilderTest {

    @Test
    fun buildBigText_withHealthyStock_showsExactRemainingAndRunwayDays() {
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(28),
            unit = StockUnit.TABLETS,
            isNegative = false,
            isLowStock = false,
            isEstimate = false,
            daysRemaining = 14
        )

        val bigText = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            medicationStrength = "500mg",
            scheduledTimeStr = "08:00",
            stockBalance = balance,
            hasPendingOrder = false
        )

        assertTrue(bigText.contains("Take 1 tab (500mg) · Scheduled for 08:00"))
        assertTrue(bigText.contains("Remaining Supply: 28 tab (approx. 14 days runway)"))
        assertTrue(bigText.contains("Supply Healthy: ~14 days runway remaining"))

        val subText = NotificationTextBuilder.buildSubText(
            stockBalance = balance,
            unit = StockUnit.TABLETS,
            hasPendingOrder = false
        )
        assertEquals("28 tab left", subText)
    }

    @Test
    fun buildBigText_withLowStockUnder7Days_showsGPRepeatReorderRecommendation() {
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(12),
            unit = StockUnit.DROPS,
            isNegative = false,
            isLowStock = true,
            isEstimate = true,
            daysRemaining = 3
        )

        val bigText = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.DROPS,
            medicationStrength = "0.5%",
            scheduledTimeStr = "12:00",
            stockBalance = balance,
            hasPendingOrder = false
        )

        assertTrue(bigText.contains("Take 2 drops (0.5%) · Scheduled for 12:00"))
        assertTrue(bigText.contains("Remaining Supply: 12 drops (approx. 3 days runway)"))
        assertTrue(bigText.contains("⚠️ Urgent: GP Recommendation: Approx. 3 days supply left. Order repeat prescription now (takes 2-3 working days)."))
    }

    @Test
    fun buildBigText_withPendingGPOrder_showsPendingRepeatStatusAndNotes() {
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(10),
            unit = StockUnit.PUFFS,
            isNegative = false,
            isLowStock = true,
            isEstimate = true,
            daysRemaining = 2
        )

        val bigText = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.PUFFS,
            medicationStrength = "100mcg",
            scheduledTimeStr = "09:00",
            stockBalance = balance,
            hasPendingOrder = true,
            pendingOrderQuantity = ScaledQuantity.ofWhole(120),
            pendingOrderNotes = "NHS App order"
        )

        assertTrue(bigText.contains("Take 2 puffs (100mcg) · Scheduled for 09:00"))
        assertTrue(bigText.contains("Remaining Supply: 10 puffs (approx. 2 days runway)"))
        assertTrue(bigText.contains("📋 GP Order Pending: 120 puffs requested (NHS App order) — in progress with surgery/pharmacy"))

        val subText = NotificationTextBuilder.buildSubText(
            stockBalance = balance,
            unit = StockUnit.PUFFS,
            hasPendingOrder = true
        )
        assertEquals("GP Order Pending · 10 left", subText)
    }

    @Test
    fun buildBigText_withActiveBottleAndReserves_rendersPhase10PackageInventoryFormat() {
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(1980),
            unit = StockUnit.DROPS,
            isNegative = false,
            isLowStock = false,
            isEstimate = true,
            daysRemaining = 198
        )

        val bigText = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.DROPS,
            medicationStrength = "0.3%",
            scheduledTimeStr = "08:00",
            stockBalance = balance,
            activePackageLabel = "Bottle #1",
            activePackageRemaining = ScaledQuantity.ofWhole(180),
            unopenedPackagesCount = 6
        )

        assertTrue(bigText.contains("Take 2 drops (0.3%) · Scheduled for 08:00"))
        assertTrue(bigText.contains("📦 Current bottle: 180 drops remaining."))
        assertTrue(bigText.contains("📦 Unopened bottles: 6."))
        assertTrue(bigText.contains("⏳ Estimated total supply: 198 days."))

        val subText = NotificationTextBuilder.buildSubText(
            stockBalance = balance,
            unit = StockUnit.DROPS,
            hasPendingOrder = false,
            activePackageLabel = "Bottle #1",
            activePackageRemaining = ScaledQuantity.ofWhole(180),
            unopenedPackagesCount = 6
        )
        assertEquals("Bottle #1 · 180 left · 6 sealed reserve", subText)
    }

    @Test
    fun buildBigText_withStagedNextPackage_rendersStagedPackageAlert() {
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(1800),
            unit = StockUnit.DROPS,
            isNegative = false,
            isLowStock = false,
            isEstimate = true,
            daysRemaining = 180
        )

        val bigText = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.DROPS,
            medicationStrength = "0.3%",
            scheduledTimeStr = "08:00",
            stockBalance = balance,
            unopenedPackagesCount = 6,
            isNextPackageStaged = true,
            stagedPackageLabel = "Bottle #2"
        )

        assertTrue(bigText.contains("📦 Package Status: Bottle #2 staged & ready to open (5 further in reserve)"))
        assertTrue(bigText.contains("📦 Current bottle: Finished."))
        assertTrue(bigText.contains("📦 Unopened bottles: 6."))
        assertTrue(bigText.contains("⏳ Estimated total supply: 180 days."))

        val subText = NotificationTextBuilder.buildSubText(
            stockBalance = balance,
            unit = StockUnit.DROPS,
            hasPendingOrder = false,
            isNextPackageStaged = true,
            unopenedPackagesCount = 6
        )
        assertEquals("Next pack ready · 1800 left · 6 sealed reserve", subText)
    }

    @Test
    fun buildFinishedPackageAlert_unmasked_rendersSpecExactNotice() {
        val hydro = Medication(
            id = "med-hf",
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ofWhole(2100),
            lowStockThreshold = ScaledQuantity.ofWhole(100)
        )

        val alert = NotificationTextBuilder.buildFinishedPackageAlert(
            medication = hydro,
            packageLabel = "Bottle #1",
            packageType = PackageType.BOTTLE,
            unopenedPackagesCount = 6,
            isPrivacyMasked = false
        )

        assertEquals("Hydro Forte – Current bottle finished.", alert.title)
        assertEquals("6 unopened bottles available.\nOpen the next bottle when ready.", alert.body)
    }

    @Test
    fun buildFinishedPackageAlert_privacyMasked_doesNotLeakMedicationName() {
        val hydro = Medication(
            id = "med-hf",
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ofWhole(2100),
            lowStockThreshold = ScaledQuantity.ofWhole(100)
        )

        val alert = NotificationTextBuilder.buildFinishedPackageAlert(
            medication = hydro,
            packageLabel = "Bottle #1",
            packageType = PackageType.BOTTLE,
            unopenedPackagesCount = 6,
            isPrivacyMasked = true
        )

        assertEquals("Medication container finished", alert.title)
        assertFalse(alert.body.contains("Hydro Forte"))
        assertTrue(alert.body.contains("6 unopened container(s) available"))
    }

    @Test
    fun buildLowStockAlert_whenDepletedEarlyWithNoReserves_rendersUrgentExhaustedAlert() {
        val alert = NotificationTextBuilder.buildLowStockAlert(
            medicationName = "Hydro Drops",
            currentBalance = ScaledQuantity.ZERO,
            unit = StockUnit.DROPS,
            daysRemaining = 0,
            hasPendingOrder = false,
            isPrivacyMasked = false,
            isDepletedEarly = true,
            depletedPackageLabel = "Bottle #1",
            unopenedPackagesCount = 0
        )

        assertEquals("Urgent Supply Exhausted: Hydro Drops", alert.title)
        assertTrue(alert.body.contains("Bottle #1 finished earlier than estimated"))
        assertTrue(alert.body.contains("Zero sealed packages remain in reserve (0 drops left)"))
        assertTrue(alert.body.contains("Request an urgent repeat prescription immediately"))
    }

    @Test
    fun buildLowStockAlert_whenDepletedEarlyWithReserves_rendersStagedNextPackageReady() {
        val alert = NotificationTextBuilder.buildLowStockAlert(
            medicationName = "Hydro Drops",
            currentBalance = ScaledQuantity.ofWhole(300),
            unit = StockUnit.DROPS,
            daysRemaining = 30,
            hasPendingOrder = false,
            isPrivacyMasked = false,
            isDepletedEarly = true,
            depletedPackageLabel = "Bottle #1",
            unopenedPackagesCount = 1,
            nextPackageLabel = "Bottle #2"
        )

        assertEquals("Package Depleted Early: Hydro Drops", alert.title)
        assertTrue(alert.body.contains("Bottle #1 ran out earlier than estimated"))
        assertTrue(alert.body.contains("Bottle #2 is staged and ready to open"))
        assertTrue(alert.body.contains("1 sealed pack(s) in reserve (~30 days runway remaining)"))
    }

    @Test
    fun buildLowStockAlert_whenDepletedEarlyWithPrivacyMasking_masksDrugAndPackageNames() {
        val alert = NotificationTextBuilder.buildLowStockAlert(
            medicationName = "Hydro Drops",
            currentBalance = ScaledQuantity.ZERO,
            unit = StockUnit.DROPS,
            daysRemaining = 0,
            hasPendingOrder = false,
            isPrivacyMasked = true,
            isDepletedEarly = true,
            depletedPackageLabel = "Bottle #1",
            unopenedPackagesCount = 0
        )

        assertEquals("Medication Supply Notice", alert.title)
        assertFalse(alert.body.contains("Hydro Drops"))
        assertFalse(alert.body.contains("Bottle #1"))
        assertTrue(alert.body.contains("A tracked medication package finished earlier than expected"))
    }
}
