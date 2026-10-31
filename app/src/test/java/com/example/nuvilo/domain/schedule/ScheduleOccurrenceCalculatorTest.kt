package com.example.nuvilo.domain.schedule

import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScheduleOccurrenceCalculatorTest {

    private val calculator = ScheduleOccurrenceCalculator()
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun dailyScheduleWithMultipleTimes_generatesAllOccurrences() {
        val schedule = ScheduleDefinition(
            id = "sch_1",
            medicationId = "med_1",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60, 20 * 60), // 08:00 and 20:00
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            endDateEpochDay = null,
            instructions = "Take with water"
        )

        // Evaluate for single day: 2026-10-02
        val date = LocalDate.of(2026, 10, 2)
        val occurrences = calculator.generateOccurrencesForDate(
            schedule = schedule,
            date = date,
            zoneId = zoneId,
            unit = StockUnit.TABLETS
        )

        assertEquals(2, occurrences.size)
        assertEquals("med_1", occurrences[0].medicationId)
        assertEquals(DoseStatus.PENDING, occurrences[0].status)

        // Deterministic occurrence IDs
        val expectedOcc0 = ScheduleOccurrenceCalculator.generateOccurrenceId("med_1", occurrences[0].scheduledEpochMs)
        val expectedOcc1 = ScheduleOccurrenceCalculator.generateOccurrenceId("med_1", occurrences[1].scheduledEpochMs)
        assertEquals(expectedOcc0, occurrences[0].occurrenceId)
        assertEquals(expectedOcc1, occurrences[1].occurrenceId)
    }

    @Test
    fun specificWeekdaysSchedule_onlyGeneratesOnMatchingDays() {
        val schedule = ScheduleDefinition(
            id = "sch_2",
            medicationId = "med_2",
            scheduleType = ScheduleType.SPECIFIC_WEEKDAYS,
            targetTimesMinuteOfDay = listOf(9 * 60), // 09:00
            daysOfWeek = setOf(1, 3, 5), // Mon, Wed, Fri
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            endDateEpochDay = null,
            instructions = ""
        )

        // 2026-10-02 is a Friday (DayOfWeek 5) -> should generate
        val friday = LocalDate.of(2026, 10, 2)
        val friOccs = calculator.generateOccurrencesForDate(schedule, friday, zoneId, StockUnit.TABLETS)
        assertEquals(1, friOccs.size)

        // 2026-10-03 is a Saturday (DayOfWeek 6) -> should NOT generate
        val saturday = LocalDate.of(2026, 10, 3)
        val satOccs = calculator.generateOccurrencesForDate(schedule, saturday, zoneId, StockUnit.TABLETS)
        assertTrue(satOccs.isEmpty())
    }

    @Test
    fun reconciliation_preservesRecordedDoses() {
        val schedule = ScheduleDefinition(
            id = "sch_1",
            medicationId = "med_1",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            endDateEpochDay = null,
            instructions = ""
        )

        val date = LocalDate.of(2026, 10, 1)
        val generated = calculator.generateOccurrencesForDate(schedule, date, zoneId, StockUnit.TABLETS)
        val scheduledMs = generated[0].scheduledEpochMs
        val occId = generated[0].occurrenceId

        // User already confirmed this dose earlier today
        val existingRecorded = listOf(
            DoseOccurrence(
                occurrenceId = occId,
                medicationId = "med_1",
                scheduledEpochMs = scheduledMs,
                doseQuantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = scheduledMs + 300_000L
            )
        )

        val reconciled = calculator.reconcileWithRecordedEvents(
            plannedOccurrences = generated,
            recordedEvents = existingRecorded
        )

        assertEquals(1, reconciled.size)
        assertEquals(DoseStatus.TAKEN, reconciled[0].status)
        assertEquals(scheduledMs + 300_000L, reconciled[0].recordedEpochMs)
    }
}
