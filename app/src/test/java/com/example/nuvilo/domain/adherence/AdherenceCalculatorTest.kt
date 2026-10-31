package com.example.nuvilo.domain.adherence

import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class AdherenceCalculatorTest {

    private val calculator = AdherenceCalculator()
    private val zoneId = ZoneId.of("Europe/London")

    private fun sampleMedication(
        id: String = "med_1",
        name: String = "Atorvastatin",
        strength: String = "20mg",
        recordingMode: RecordingMode = RecordingMode.CONFIRM
    ) = Medication(
        id = id,
        name = name,
        strength = strength,
        form = MedicationForm.TABLET,
        stockUnit = StockUnit.TABLETS,
        openingStock = ScaledQuantity.ofWhole(30),
        lowStockThreshold = ScaledQuantity.ofWhole(7),
        recordingMode = recordingMode
    )

    private fun epochMsAt(date: LocalDate, hour: Int, minute: Int, zone: ZoneId = zoneId): Long =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun calculateSummary_distinguishesConfirmedAssumedSkippedAndMissed_andExcludesFuture() {
        val med = sampleMedication()
        val startDate = LocalDate.of(2026, 10, 1)
        val endDate = LocalDate.of(2026, 10, 3)

        // 2 doses per day: 08:00 (480) and 20:00 (1200) -> 6 total occurrences over 3 days
        val schedule = ScheduleDefinition(
            id = "sch_1",
            medicationId = med.id,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(480, 1200),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ONE,
            startDateEpochDay = startDate.toEpochDay()
        )

        // Evaluation time is Oct 3 at 12:00 -> Oct 3 20:00 is in the future (1 future occurrence, 5 eligible past)
        val evaluationEpochMs = epochMsAt(LocalDate.of(2026, 10, 3), 12, 0)

        val day1Morning = epochMsAt(LocalDate.of(2026, 10, 1), 8, 0)
        val day1Evening = epochMsAt(LocalDate.of(2026, 10, 1), 20, 0)
        val day2Morning = epochMsAt(LocalDate.of(2026, 10, 2), 8, 0)
        val day2Evening = epochMsAt(LocalDate.of(2026, 10, 2), 20, 0)
        // day3Morning (Oct 3 08:00) is unrecorded in the past -> resolved as MISSED

        val recorded = listOf(
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, day1Morning),
                medicationId = med.id,
                scheduledEpochMs = day1Morning,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = day1Morning + 60_000L
            ),
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, day1Evening),
                medicationId = med.id,
                scheduledEpochMs = day1Evening,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.ASSUMED,
                recordedEpochMs = day1Evening
            ),
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, day2Morning),
                medicationId = med.id,
                scheduledEpochMs = day2Morning,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.SKIPPED,
                recordedEpochMs = day2Morning + 120_000L
            ),
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, day2Evening),
                medicationId = med.id,
                scheduledEpochMs = day2Evening,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = day2Evening + 30_000L
            )
        )

        val summary = calculator.calculateSummary(
            periodLabel = "Test Period",
            medications = listOf(med),
            schedules = listOf(schedule),
            recordedEvents = recorded,
            startDate = startDate,
            endDate = endDate,
            zoneId = zoneId,
            evaluationEpochMs = evaluationEpochMs
        )

        assertEquals(2, summary.confirmedCount)
        assertEquals(1, summary.assumedCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(1, summary.missedCount) // Oct 3 08:00 unrecorded past occurrence
        assertEquals(1, summary.pendingFutureCount) // Oct 3 20:00 future occurrence
        assertEquals(5, summary.eligibleOccurrencesCount)
        assertEquals(3, summary.adherentCount) // 2 confirmed + 1 assumed (skipped is NEVER counted as taken)
        assertEquals(60, summary.adherencePercentage) // 3 / 5 = 60%
        assertEquals(40, summary.confirmedOnlyPercentage) // 2 / 5 = 40%
        assertTrue(summary.explanation.contains("5 eligible past scheduled doses"))
    }

    @Test
    fun calculateSummary_respectsStartDateEndDateAndSelectedWeekdays_andDoesNotInventEarlierHistory() {
        val med = sampleMedication()
        // Schedule only runs Oct 5 (Mon) to Oct 9 (Fri) on Mon(1), Wed(3), Fri(5)
        val schedule = ScheduleDefinition(
            id = "sch_weekday",
            medicationId = med.id,
            scheduleType = ScheduleType.SPECIFIC_WEEKDAYS,
            targetTimesMinuteOfDay = listOf(540), // 09:00
            daysOfWeek = setOf(
                DayOfWeek.MONDAY.value,
                DayOfWeek.WEDNESDAY.value,
                DayOfWeek.FRIDAY.value
            ),
            doseQuantityPerOccurrence = ScaledQuantity.ONE,
            startDateEpochDay = LocalDate.of(2026, 10, 5).toEpochDay(),
            endDateEpochDay = LocalDate.of(2026, 10, 9).toEpochDay()
        )

        // Query the entire month of October 2026 evaluated at Oct 31 23:00
        val evaluationEpochMs = epochMsAt(LocalDate.of(2026, 10, 31), 23, 0)
        val summary = calculator.calculateSummary(
            periodLabel = "October 2026",
            medications = listOf(med),
            schedules = listOf(schedule),
            recordedEvents = emptyList(),
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 31),
            zoneId = zoneId,
            evaluationEpochMs = evaluationEpochMs
        )

        // Only Oct 5 (Mon), Oct 7 (Wed), Oct 9 (Fri) are eligible -> 3 occurrences total
        // Dates Oct 1..4 (before start) and Oct 10..31 (after end) have 0 invented occurrences
        assertEquals(3, summary.eligibleOccurrencesCount)
        assertEquals(3, summary.missedCount)
        assertEquals(0, summary.adherencePercentage)
    }

    @Test
    fun correctedOrReversedDose_doesNotDoubleCountOrInflateAdherence() {
        val med = sampleMedication()
        val date = LocalDate.of(2026, 10, 10)
        val schedule = ScheduleDefinition(
            id = "sch_1",
            medicationId = med.id,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(480), // 08:00
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ONE,
            startDateEpochDay = date.toEpochDay()
        )

        val scheduledMs = epochMsAt(date, 8, 0)
        val occId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, scheduledMs)

        // Simulate original TAKEN record and subsequent corrected SKIPPED record for the same occurrenceId
        val initialTaken = DoseOccurrence(
            occurrenceId = occId,
            medicationId = med.id,
            scheduledEpochMs = scheduledMs,
            doseQuantity = ScaledQuantity.ONE,
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN,
            recordedEpochMs = scheduledMs + 10_000L
        )
        val correctedToSkipped = initialTaken.copy(
            status = DoseStatus.SKIPPED,
            correctionNotes = "Accidentally tapped take",
            correctedAtEpochMs = scheduledMs + 60_000L
        )

        val evaluationEpochMs = epochMsAt(date, 18, 0)
        val summary = calculator.calculateSummary(
            periodLabel = "Single Day",
            medications = listOf(med),
            schedules = listOf(schedule),
            recordedEvents = listOf(initialTaken, correctedToSkipped),
            startDate = date,
            endDate = date,
            zoneId = zoneId,
            evaluationEpochMs = evaluationEpochMs
        )

        assertEquals(1, summary.eligibleOccurrencesCount)
        assertEquals(0, summary.confirmedCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(1, summary.correctedCount)
        assertEquals(0, summary.adherencePercentage)
    }

    @Test
    fun scheduleChanges_preserveRecordedEventsFromPreviousScheduleTimes() {
        val med = sampleMedication()
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)

        // Current schedule was changed to 10:00 (600), starting Oct 1
        val updatedSchedule = ScheduleDefinition(
            id = "sch_updated",
            medicationId = med.id,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(600), // 10:00
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ONE,
            startDateEpochDay = day1.toEpochDay()
        )

        // On day 1 the user had taken an 08:00 dose under the old schedule
        val oldScheduledMs = epochMsAt(day1, 8, 0)
        val newScheduledDay1Ms = epochMsAt(day1, 10, 0)
        val newScheduledDay2Ms = epochMsAt(day2, 10, 0)

        val recorded = listOf(
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, oldScheduledMs),
                medicationId = med.id,
                scheduledEpochMs = oldScheduledMs,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = oldScheduledMs + 1000L
            ),
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, newScheduledDay2Ms),
                medicationId = med.id,
                scheduledEpochMs = newScheduledDay2Ms,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = newScheduledDay2Ms + 1000L
            )
        )

        val resolved = calculator.resolveOccurrencesForRange(
            medications = listOf(med),
            schedules = listOf(updatedSchedule),
            recordedEvents = recorded,
            startDate = day1,
            endDate = day2,
            zoneId = zoneId,
            evaluationEpochMs = epochMsAt(day2, 18, 0)
        )

        assertTrue(resolved.any { it.occurrence.scheduledEpochMs == oldScheduledMs && it.occurrence.status == DoseStatus.TAKEN })
        assertTrue(resolved.any { it.occurrence.scheduledEpochMs == newScheduledDay1Ms })
        assertTrue(resolved.any { it.occurrence.scheduledEpochMs == newScheduledDay2Ms && it.occurrence.status == DoseStatus.TAKEN })
    }

    @Test
    fun monthlyCalendar_handlesMultipleMedicationsAndMixedDailyOutcomes_withoutMarkingEntireDayMissed() {
        val medA = sampleMedication(id = "med_a", name = "Lisinopril")
        val medB = sampleMedication(id = "med_b", name = "Metformin")
        val ym = YearMonth.of(2026, 10)
        val oct15 = LocalDate.of(2026, 10, 15)

        val schA = ScheduleDefinition(
            id = "sch_a",
            medicationId = medA.id,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(480), // 08:00
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ONE,
            startDateEpochDay = oct15.toEpochDay(),
            endDateEpochDay = oct15.toEpochDay()
        )
        val schB = ScheduleDefinition(
            id = "sch_b",
            medicationId = medB.id,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(720, 1200), // 12:00 and 20:00
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ONE,
            startDateEpochDay = oct15.toEpochDay(),
            endDateEpochDay = oct15.toEpochDay()
        )

        // On Oct 15: medA 08:00 TAKEN, medB 12:00 ASSUMED, medB 20:00 unrecorded (MISSED)
        val ms8 = epochMsAt(oct15, 8, 0)
        val ms12 = epochMsAt(oct15, 12, 0)
        val recorded = listOf(
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medA.id, ms8),
                medicationId = medA.id,
                scheduledEpochMs = ms8,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN,
                recordedEpochMs = ms8
            ),
            DoseOccurrence(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medB.id, ms12),
                medicationId = medB.id,
                scheduledEpochMs = ms12,
                doseQuantity = ScaledQuantity.ONE,
                unit = StockUnit.TABLETS,
                status = DoseStatus.ASSUMED,
                recordedEpochMs = ms12
            )
        )

        val calendar = calculator.buildMonthlyCalendar(
            yearMonth = ym,
            medications = listOf(medA, medB),
            schedules = listOf(schA, schB),
            recordedEvents = recorded,
            zoneId = zoneId,
            evaluationEpochMs = epochMsAt(oct15, 22, 0)
        )

        val oct15Record = calendar.daysByDate[oct15]!!
        assertEquals(3, oct15Record.totalScheduledCount)
        assertEquals(1, oct15Record.confirmedCount)
        assertEquals(1, oct15Record.assumedCount)
        assertEquals(0, oct15Record.skippedCount)
        assertEquals(1, oct15Record.missedCount)

        // Days without schedules have no doses
        val oct1Record = calendar.daysByDate[LocalDate.of(2026, 10, 1)]!!
        assertFalse(oct1Record.hasAnyDoses)
    }

    @Test
    fun daylightSavingTransition_andMonthBoundaries_handledDeterministically() {
        // Europe/London DST ends on Sunday 25 October 2026
        val med = sampleMedication()
        val oct24 = LocalDate.of(2026, 10, 24)
        val oct25 = LocalDate.of(2026, 10, 25)
        val oct26 = LocalDate.of(2026, 10, 26)

        val schedule = ScheduleDefinition(
            id = "sch_dst",
            medicationId = med.id,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(540), // 09:00 local time every day
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ONE,
            startDateEpochDay = oct24.toEpochDay(),
            endDateEpochDay = oct26.toEpochDay()
        )

        val resolved = calculator.resolveOccurrencesForRange(
            medications = listOf(med),
            schedules = listOf(schedule),
            recordedEvents = emptyList(),
            startDate = oct24,
            endDate = oct26,
            zoneId = zoneId,
            evaluationEpochMs = epochMsAt(oct26, 20, 0)
        )

        assertEquals(3, resolved.size)
        // Verify each occurrence maps back to 09:00 local time on its respective date across the DST boundary
        val dates = resolved.map {
            java.time.Instant.ofEpochMilli(it.occurrence.scheduledEpochMs).atZone(zoneId).toLocalDateTime()
        }
        assertEquals(LocalTime.of(9, 0), dates[0].toLocalTime())
        assertEquals(LocalTime.of(9, 0), dates[1].toLocalTime())
        assertEquals(LocalTime.of(9, 0), dates[2].toLocalTime())
    }

    @Test
    fun emptyPeriod_returnsNullPercentageAndClearExplanation() {
        val summary = calculator.calculateSummary(
            periodLabel = "Empty",
            medications = emptyList(),
            schedules = emptyList(),
            recordedEvents = emptyList(),
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 31),
            zoneId = zoneId,
            evaluationEpochMs = epochMsAt(LocalDate.of(2026, 10, 15), 12, 0)
        )

        assertEquals(0, summary.eligibleOccurrencesCount)
        assertNull(summary.adherencePercentage)
        assertNull(summary.confirmedOnlyPercentage)
    }
}
