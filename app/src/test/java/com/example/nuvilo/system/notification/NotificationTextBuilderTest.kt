package com.example.nuvilo.system.notification

import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
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
}
