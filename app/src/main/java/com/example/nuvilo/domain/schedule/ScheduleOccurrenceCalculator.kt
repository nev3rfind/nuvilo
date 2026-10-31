package com.example.nuvilo.domain.schedule

import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure Kotlin calculator for generating deterministic scheduled dose occurrences
 * and reconciling them with user-recorded adherence history.
 */
class ScheduleOccurrenceCalculator {

    fun generateOccurrencesForDate(
        schedule: ScheduleDefinition,
        date: LocalDate,
        zoneId: ZoneId,
        unit: StockUnit
    ): List<DoseOccurrence> {
        val epochDay = date.toEpochDay()
        if (epochDay < schedule.startDateEpochDay) return emptyList()
        if (schedule.endDateEpochDay != null && epochDay > schedule.endDateEpochDay) return emptyList()

        val shouldGenerate = when (schedule.scheduleType) {
            ScheduleType.DAILY -> true
            ScheduleType.SPECIFIC_WEEKDAYS -> schedule.daysOfWeek.contains(date.dayOfWeek.value)
            ScheduleType.CYCLICAL -> true // Future extension
            ScheduleType.PRN -> false // PRN doses are un-scheduled, logged as-needed
        }

        if (!shouldGenerate) return emptyList()

        return schedule.targetTimesMinuteOfDay.sorted().map { minuteOfDay ->
            val hour = minuteOfDay / 60
            val minute = minuteOfDay % 60
            val localDateTime = date.atTime(LocalTime.of(hour, minute))
            val scheduledEpochMs = localDateTime.atZone(zoneId).toInstant().toEpochMilli()
            val occId = generateOccurrenceId(schedule.medicationId, scheduledEpochMs)

            DoseOccurrence(
                occurrenceId = occId,
                medicationId = schedule.medicationId,
                scheduledEpochMs = scheduledEpochMs,
                doseQuantity = schedule.doseQuantityPerOccurrence,
                unit = unit,
                status = DoseStatus.PENDING,
                recordedEpochMs = null,
                notes = null
            )
        }
    }

    /**
     * Merges planned occurrences with historical recorded events, ensuring that
     * user-taken, assumed, or skipped doses preserve their historical timestamps and statuses.
     */
    fun reconcileWithRecordedEvents(
        plannedOccurrences: List<DoseOccurrence>,
        recordedEvents: List<DoseOccurrence>
    ): List<DoseOccurrence> {
        val recordedMap = recordedEvents.associateBy { it.occurrenceId }
        val result = mutableListOf<DoseOccurrence>()

        for (planned in plannedOccurrences) {
            val recorded = recordedMap[planned.occurrenceId]
            if (recorded != null) {
                result.add(recorded)
            } else {
                result.add(planned)
            }
        }

        // Also include any PRN or ad-hoc doses recorded on this day that were not in the planned schedule
        val plannedIds = plannedOccurrences.map { it.occurrenceId }.toSet()
        val extraDoses = recordedEvents.filter { !plannedIds.contains(it.occurrenceId) }
        result.addAll(extraDoses)

        return result.sortedBy { it.scheduledEpochMs }
    }

    companion object {
        fun generateOccurrenceId(medicationId: String, scheduledEpochMs: Long): String =
            "occ_${medicationId}_$scheduledEpochMs"
    }
}
