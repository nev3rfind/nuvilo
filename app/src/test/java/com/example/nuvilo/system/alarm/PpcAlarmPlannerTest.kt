package com.example.nuvilo.system.alarm

import com.example.nuvilo.domain.ppc.ExemptionCategory
import com.example.nuvilo.domain.ppc.PpcCertificate
import com.example.nuvilo.domain.ppc.PpcCoverageType
import com.example.nuvilo.domain.ppc.PpcReminderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PpcAlarmPlannerTest {

    private val zone = ZoneId.of("Europe/London")

    @Test
    fun computeTriggers_schedulesConfiguredFutureOffsetsOnly() {
        val expiry = LocalDate.of(2026, 8, 1)
        val cert = PpcCertificate(
            id = "ppc-1",
            coverageType = PpcCoverageType.PPC_12_MONTH,
            startDate = LocalDate.of(2025, 8, 2),
            expiryDate = expiry,
            reminderConfig = PpcReminderConfig(
                remind30DaysBefore = true,
                remind14DaysBefore = false,
                remind7DaysBefore = true,
                remindOnExpiryDay = true,
                reminderHour = 9,
                reminderMinute = 0
            )
        )

        // Now is 20 days before expiry -> 30-day offset is in the past, 7-day and 0-day are in the future
        val nowMs = ZonedDateTime.of(expiry.minusDays(20), LocalTime.of(10, 0), zone)
            .toInstant().toEpochMilli()

        val triggers = PpcAlarmPlanner.computeTriggers(cert, nowEpochMs = nowMs, zoneId = zone)
        assertEquals(listOf(7, 0), triggers.map { it.offsetDays })
        assertEquals(2, triggers.map { it.requestCode }.distinct().size)
    }

    @Test
    fun computeTriggers_archivedOrOngoing_returnsEmpty() {
        val archived = PpcCertificate(
            id = "ppc-arch",
            coverageType = PpcCoverageType.PPC_3_MONTH,
            startDate = LocalDate.of(2026, 1, 1),
            expiryDate = LocalDate.of(2026, 3, 31),
            isArchived = true
        )
        val ongoing = PpcCertificate(
            id = "ppc-ongoing",
            coverageType = PpcCoverageType.FREE_EXEMPTION,
            exemptionCategory = ExemptionCategory.MEDICAL,
            startDate = LocalDate.of(2025, 1, 1),
            expiryDate = null,
            isOngoing = true
        )

        val nowMs = ZonedDateTime.of(LocalDate.of(2026, 1, 15), LocalTime.of(9, 0), zone)
            .toInstant().toEpochMilli()

        assertTrue(PpcAlarmPlanner.computeTriggers(archived, nowEpochMs = nowMs, zoneId = zone).isEmpty())
        assertTrue(PpcAlarmPlanner.computeTriggers(ongoing, nowEpochMs = nowMs, zoneId = zone).isEmpty())
    }

    @Test
    fun requestCodeFor_isDeterministicAndUniquePerCertificateAndOffset() {
        val c1d30 = PpcAlarmPlanner.requestCodeFor("cert-a", 30)
        val c1d14 = PpcAlarmPlanner.requestCodeFor("cert-a", 14)
        val c2d30 = PpcAlarmPlanner.requestCodeFor("cert-b", 30)

        assertEquals(c1d30, PpcAlarmPlanner.requestCodeFor("cert-a", 30))
        assertNotEquals(c1d30, c1d14)
        assertNotEquals(c1d30, c2d30)
    }

    @Test
    fun buildNotificationContent_respectsPrivacyModeAndOfficialTerminology() {
        val standard = PpcAlarmPlanner.buildNotificationContent(
            coverageType = PpcCoverageType.PPC_12_MONTH,
            expiryDate = LocalDate.of(2026, 12, 31),
            offsetDays = 14,
            isPrivacyMasked = false
        )
        assertTrue(standard.title.contains("Prescription Prepayment Certificate (PPC)"))
        assertTrue(standard.body.contains("Based on your recorded certificate dates"))
        assertFalse(standard.title.contains("BPC"))

        val masked = PpcAlarmPlanner.buildNotificationContent(
            coverageType = PpcCoverageType.PPC_12_MONTH,
            expiryDate = LocalDate.of(2026, 12, 31),
            offsetDays = 14,
            isPrivacyMasked = true
        )
        assertFalse(masked.title.contains("PPC"))
        assertFalse(masked.body.contains("2026"))
    }
}
