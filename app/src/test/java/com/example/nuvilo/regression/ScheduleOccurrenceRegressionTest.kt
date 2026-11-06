package com.example.nuvilo.regression

import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Regression Test Suite: Category A (Domain Models & Accounting)
 * Tests T031 to T038: Schedule Occurrence Generation and Reconciliation Invariants
 */
class ScheduleOccurrenceRegressionTest {

    private lateinit var calculator: ScheduleOccurrenceCalculator
    private val zoneId = ZoneId.of("UTC")
    private val testDate = LocalDate.of(2026, 11, 6) // Friday (Day of week 5)

    @Before
    fun setUp() {
        calculator = ScheduleOccurrenceCalculator()
    }

    private fun createSchedule(
        scheduleType: ScheduleType = ScheduleType.DAILY,
        times: List<Int> = listOf(8 * 60, 20 * 60), // 08:00 and 20:00
        daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = null
    ): ScheduleDefinition = ScheduleDefinition(
        id = "sch_1",
        medicationId = "med_amoxicillin",
        scheduleType = scheduleType,
        targetTimesMinuteOfDay = times,
        daysOfWeek = daysOfWeek,
        doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
        startDateEpochDay = startDate.toEpochDay(),
        endDateEpochDay = endDate?.toEpochDay(),
        instructions = "Take with water"
    )

    @Test
    fun t031_dailyScheduleGeneratesOccurrencesForAllTargetMinutesOnActiveDate() {
        // T031: Daily schedule generates occurrences for all target minutes on active date
        val schedule = createSchedule()
        val occurrences = calculator.generateOccurrencesForDate(schedule, testDate, zoneId, StockUnit.TABLETS)

        assertEquals(2, occurrences.size)
        assertEquals(DoseStatus.PENDING, occurrences[0].status)
        assertEquals(DoseStatus.PENDING, occurrences[1].status)
        assertEquals(StockUnit.TABLETS, occurrences[0].unit)
        assertEquals("med_amoxicillin", occurrences[0].medicationId)
    }

    @Test
    fun t032_dateBeforeStartDateReturnsEmptyOccurrences() {
        // T032: Date before startDateEpochDay returns empty occurrences
        val futureStart = testDate.plusDays(10)
        val schedule = createSchedule(startDate = futureStart)
        val occurrences = calculator.generateOccurrencesForDate(schedule, testDate, zoneId, StockUnit.TABLETS)

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun t033_dateAfterEndDateReturnsEmptyOccurrences() {
        // T033: Date after endDateEpochDay returns empty occurrences
        val pastEnd = testDate.minusDays(5)
        val schedule = createSchedule(endDate = pastEnd)
        val occurrences = calculator.generateOccurrencesForDate(schedule, testDate, zoneId, StockUnit.TABLETS)

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun t034_specificWeekdaysScheduleGeneratesOccurrencesOnlyOnMatchingDay() {
        // T034: SPECIFIC_WEEKDAYS schedule generates occurrences only on matching day of week
        // testDate (2026-11-06) is Friday (dayOfWeek.value == 5)
        val fridayOnlySchedule = createSchedule(
            scheduleType = ScheduleType.SPECIFIC_WEEKDAYS,
            daysOfWeek = setOf(5)
        )
        val occurrences = calculator.generateOccurrencesForDate(fridayOnlySchedule, testDate, zoneId, StockUnit.TABLETS)

        assertEquals(2, occurrences.size)
    }

    @Test
    fun t035_specificWeekdaysScheduleReturnsEmptyOnNonMatchingDay() {
        // T035: SPECIFIC_WEEKDAYS schedule returns empty on non-matching days
        // testDate is Friday (5); schedule is for Monday and Wednesday (1, 3)
        val monWedSchedule = createSchedule(
            scheduleType = ScheduleType.SPECIFIC_WEEKDAYS,
            daysOfWeek = setOf(1, 3)
        )
        val occurrences = calculator.generateOccurrencesForDate(monWedSchedule, testDate, zoneId, StockUnit.TABLETS)

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun t036_prnScheduleReturnsEmptyScheduledOccurrences() {
        // T036: PRN schedule returns empty scheduled occurrences (as-needed only)
        val prnSchedule = createSchedule(scheduleType = ScheduleType.PRN)
        val occurrences = calculator.generateOccurrencesForDate(prnSchedule, testDate, zoneId, StockUnit.TABLETS)

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun t037_occurrenceIdGenerationIsDeterministicAndUniquePerMedAndTimestamp() {
        // T037: Occurrence ID generation is deterministic and unique per medication and epoch ms
        val id1 = ScheduleOccurrenceCalculator.generateOccurrenceId("med_1", 1700000000000L)
        val id2 = ScheduleOccurrenceCalculator.generateOccurrenceId("med_1", 1700000000000L)
        val idDifferentTime = ScheduleOccurrenceCalculator.generateOccurrenceId("med_1", 1700000060000L)
        val idDifferentMed = ScheduleOccurrenceCalculator.generateOccurrenceId("med_2", 1700000000000L)

        assertEquals(id1, id2)
        assertEquals("occ_med_1_1700000000000", id1)
        assertNotEquals(id1, idDifferentTime)
        assertNotEquals(id1, idDifferentMed)
    }

    @Test
    fun t038_reconcilePlannedOccurrencesPreservesHistoricalRecordedEvents() {
        // T038: Reconcile planned occurrences preserves historical recorded events (TAKEN, SKIPPED, notes)
        val schedule = createSchedule()
        val planned = calculator.generateOccurrencesForDate(schedule, testDate, zoneId, StockUnit.TABLETS)
        assertEquals(2, planned.size)

        // User took the morning dose (index 0)
        val recordedMorning = planned[0].copy(
            status = DoseStatus.TAKEN,
            recordedEpochMs = 1700000010000L,
            notes = "Taken with breakfast"
        )

        val reconciled = calculator.reconcileWithRecordedEvents(
            plannedOccurrences = planned,
            recordedEvents = listOf(recordedMorning)
        )

        assertEquals(2, reconciled.size)
        val morning = reconciled.first { it.occurrenceId == planned[0].occurrenceId }
        val evening = reconciled.first { it.occurrenceId == planned[1].occurrenceId }

        assertEquals(DoseStatus.TAKEN, morning.status)
        assertEquals("Taken with breakfast", morning.notes)
        assertEquals(1700000010000L, morning.recordedEpochMs)

        assertEquals(DoseStatus.PENDING, evening.status)
    }
}
