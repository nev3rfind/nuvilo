package com.example.nuvilo.domain.adherence

import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class ResolvedDoseOccurrence(
    val occurrence: DoseOccurrence,
    val medicationName: String,
    val medicationStrength: String,
    val isFuture: Boolean,
    val isCorrected: Boolean
)

data class AdherenceSummary(
    val periodLabel: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val confirmedCount: Int,
    val assumedCount: Int,
    val skippedCount: Int,
    val missedCount: Int,
    val pendingFutureCount: Int,
    val correctedCount: Int
) {
    val eligibleOccurrencesCount: Int
        get() = confirmedCount + assumedCount + skippedCount + missedCount

    val adherentCount: Int
        get() = confirmedCount + assumedCount

    /**
     * Overall adherence percentage (Confirmed + Assumed) / Eligible past occurrences.
     * Null when there are no eligible past occurrences in the period.
     */
    val adherencePercentage: Int?
        get() = if (eligibleOccurrencesCount == 0) {
            null
        } else {
            ((adherentCount * 100) + (eligibleOccurrencesCount / 2)) / eligibleOccurrencesCount
        }

    /**
     * Strictly verified adherence percentage (Confirmed only) / Eligible past occurrences.
     */
    val confirmedOnlyPercentage: Int?
        get() = if (eligibleOccurrencesCount == 0) {
            null
        } else {
            ((confirmedCount * 100) + (eligibleOccurrencesCount / 2)) / eligibleOccurrencesCount
        }

    val explanation: String
        get() = if (eligibleOccurrencesCount == 0) {
            "No eligible past scheduled doses in this period. Future scheduled occurrences are excluded."
        } else {
            val pct = adherencePercentage ?: 0
            val confPct = confirmedOnlyPercentage ?: 0
            "Calculated from $eligibleOccurrencesCount eligible past scheduled " +
                "${if (eligibleOccurrencesCount == 1) "dose" else "doses"}: " +
                "$confirmedCount confirmed + $assumedCount assumed ($pct% overall, $confPct% verified confirmed). " +
                "Skipped ($skippedCount) and missed ($missedCount) count in the denominator; future due doses ($pendingFutureCount) are excluded."
        }
}

data class DailyAdherenceRecord(
    val date: LocalDate,
    val occurrences: List<ResolvedDoseOccurrence>,
    val confirmedCount: Int,
    val assumedCount: Int,
    val skippedCount: Int,
    val missedCount: Int,
    val pendingFutureCount: Int,
    val correctedCount: Int
) {
    val totalScheduledCount: Int
        get() = occurrences.size

    val eligiblePastCount: Int
        get() = confirmedCount + assumedCount + skippedCount + missedCount

    val hasAnyDoses: Boolean
        get() = occurrences.isNotEmpty()
}

data class MonthlyCalendarData(
    val yearMonth: YearMonth,
    val daysByDate: Map<LocalDate, DailyAdherenceRecord>,
    val monthSummary: AdherenceSummary
)

enum class DoseOutcomeFilter(val displayName: String) {
    ALL("All"),
    CONFIRMED("Confirmed"),
    ASSUMED("Assumed"),
    SKIPPED("Skipped"),
    MISSED("Missed"),
    CORRECTED("Corrected")
}

enum class HistoryDateFilter(val displayName: String, val daysBack: Long?) {
    ALL_TIME("All Time", null),
    LAST_7_DAYS("Last 7 Days", 7L),
    LAST_30_DAYS("Last 30 Days", 30L),
    LAST_90_DAYS("Last 90 Days", 90L)
}

/**
 * Pure Kotlin adherence calculation engine.
 * Independent of Android UI / Compose so all adherence rules can be deterministically unit-tested.
 */
class AdherenceCalculator(
    private val scheduleCalculator: ScheduleOccurrenceCalculator = ScheduleOccurrenceCalculator()
) {

    /**
     * Resolves all scheduled and recorded occurrences within [startDate]..[endDate] (inclusive).
     *
     * Rules enforced:
     * 1. Uses eligible scheduled occurrences as the denominator, never notification deliveries.
     * 2. Future occurrences (`scheduledEpochMs > evaluationEpochMs`) remain `PENDING` and are excluded from adherence denominator.
     * 3. Past unrecorded scheduled occurrences (`scheduledEpochMs <= evaluationEpochMs`) are resolved as `MISSED`.
     * 4. Excludes dates before a medication schedule's `startDateEpochDay` and after its `endDateEpochDay`.
     * 5. Deduplicates recorded events by `occurrenceId` so corrections/reversals never double-count.
     * 6. Keeps `TAKEN` (Confirmed) and `ASSUMED` distinguishable.
     */
    fun resolveOccurrencesForRange(
        medications: List<Medication>,
        schedules: List<ScheduleDefinition>,
        recordedEvents: List<DoseOccurrence>,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId,
        evaluationEpochMs: Long
    ): List<ResolvedDoseOccurrence> {
        if (endDate.isBefore(startDate)) return emptyList()

        val medById = medications.associateBy { it.id }
        val schedulesByMed = schedules.groupBy { it.medicationId }

        // Deduplicate recorded events by occurrenceId, keeping the most recently corrected/recorded version
        val authoritativeRecordedById = LinkedHashMap<String, DoseOccurrence>()
        for (event in recordedEvents) {
            val existing = authoritativeRecordedById[event.occurrenceId]
            if (existing == null) {
                authoritativeRecordedById[event.occurrenceId] = event
            } else {
                val existingTs = existing.correctedAtEpochMs ?: existing.recordedEpochMs ?: 0L
                val candidateTs = event.correctedAtEpochMs ?: event.recordedEpochMs ?: 0L
                if (candidateTs >= existingTs) {
                    authoritativeRecordedById[event.occurrenceId] = event
                }
            }
        }

        val rangeStartEpochMs = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val rangeEndExclusiveEpochMs = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Generate planned occurrences day by day across [startDate..endDate]
        val plannedByOccurrenceId = LinkedHashMap<String, DoseOccurrence>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            for (med in medications) {
                val medSchedules = schedulesByMed[med.id].orEmpty()
                for (schedule in medSchedules) {
                    val dailyPlanned = scheduleCalculator.generateOccurrencesForDate(
                        schedule = schedule,
                        date = current,
                        zoneId = zoneId,
                        unit = med.stockUnit
                    )
                    for (occ in dailyPlanned) {
                        plannedByOccurrenceId[occ.occurrenceId] = occ
                    }
                }
            }
            current = current.plusDays(1)
        }

        // Merge planned occurrences with recorded events in [rangeStartEpochMs, rangeEndExclusiveEpochMs)
        val mergedOccurrences = mutableListOf<DoseOccurrence>()
        val includedIds = HashSet<String>()

        for ((occId, planned) in plannedByOccurrenceId) {
            val recorded = authoritativeRecordedById[occId]
            if (recorded != null) {
                mergedOccurrences.add(recorded)
            } else {
                mergedOccurrences.add(planned)
            }
            includedIds.add(occId)
        }

        // Include historical recorded events in the date window that came from an edited/previous schedule
        for ((occId, recorded) in authoritativeRecordedById) {
            if (!includedIds.contains(occId) &&
                recorded.scheduledEpochMs in rangeStartEpochMs until rangeEndExclusiveEpochMs &&
                medById.containsKey(recorded.medicationId)
            ) {
                val med = medById[recorded.medicationId]!!
                val medSchedules = schedulesByMed[med.id].orEmpty()
                val eventDateEpochDay = Instant.ofEpochMilli(recorded.scheduledEpochMs)
                    .atZone(zoneId)
                    .toLocalDate()
                    .toEpochDay()
                val earliestTrackingStart = medSchedules.minOfOrNull { it.startDateEpochDay }
                // Respect tracking start date if schedules exist
                if (earliestTrackingStart == null || eventDateEpochDay >= earliestTrackingStart) {
                    mergedOccurrences.add(recorded)
                    includedIds.add(occId)
                }
            }
        }

        return mergedOccurrences
            .sortedBy { it.scheduledEpochMs }
            .mapNotNull { occ ->
                val med = medById[occ.medicationId] ?: return@mapNotNull null
                val isFuture = occ.scheduledEpochMs > evaluationEpochMs
                val resolvedStatus = when {
                    isFuture -> DoseStatus.PENDING
                    occ.status == DoseStatus.PENDING -> DoseStatus.MISSED
                    else -> occ.status
                }
                val isCorrected = occ.correctedAtEpochMs != null || !occ.correctionNotes.isNullOrBlank()
                ResolvedDoseOccurrence(
                    occurrence = occ.copy(status = resolvedStatus),
                    medicationName = med.name,
                    medicationStrength = med.strength,
                    isFuture = isFuture,
                    isCorrected = isCorrected
                )
            }
    }

    /**
     * Computes adherence statistics for [startDate]..[endDate] (inclusive).
     */
    fun calculateSummary(
        periodLabel: String,
        medications: List<Medication>,
        schedules: List<ScheduleDefinition>,
        recordedEvents: List<DoseOccurrence>,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId,
        evaluationEpochMs: Long
    ): AdherenceSummary {
        val resolved = resolveOccurrencesForRange(
            medications = medications,
            schedules = schedules,
            recordedEvents = recordedEvents,
            startDate = startDate,
            endDate = endDate,
            zoneId = zoneId,
            evaluationEpochMs = evaluationEpochMs
        )
        return summarizeResolvedOccurrences(
            periodLabel = periodLabel,
            startDate = startDate,
            endDate = endDate,
            resolvedOccurrences = resolved
        )
    }

    fun summarizeResolvedOccurrences(
        periodLabel: String,
        startDate: LocalDate,
        endDate: LocalDate,
        resolvedOccurrences: List<ResolvedDoseOccurrence>
    ): AdherenceSummary {
        var confirmed = 0
        var assumed = 0
        var skipped = 0
        var missed = 0
        var pendingFuture = 0
        var corrected = 0

        for (item in resolvedOccurrences) {
            if (item.isFuture) {
                pendingFuture++
                continue
            }
            if (item.isCorrected) {
                corrected++
            }
            when (item.occurrence.status) {
                DoseStatus.TAKEN -> confirmed++
                DoseStatus.ASSUMED -> assumed++
                DoseStatus.SKIPPED -> skipped++
                DoseStatus.MISSED -> missed++
                DoseStatus.PENDING -> missed++
            }
        }

        return AdherenceSummary(
            periodLabel = periodLabel,
            startDate = startDate,
            endDate = endDate,
            confirmedCount = confirmed,
            assumedCount = assumed,
            skippedCount = skipped,
            missedCount = missed,
            pendingFutureCount = pendingFuture,
            correctedCount = corrected
        )
    }

    /**
     * Builds the monthly calendar data structure for [yearMonth], grouping occurrences by local date
     * without conflating a single missed dose with an entirely missed day.
     */
    fun buildMonthlyCalendar(
        yearMonth: YearMonth,
        medications: List<Medication>,
        schedules: List<ScheduleDefinition>,
        recordedEvents: List<DoseOccurrence>,
        zoneId: ZoneId,
        evaluationEpochMs: Long
    ): MonthlyCalendarData {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val allResolved = resolveOccurrencesForRange(
            medications = medications,
            schedules = schedules,
            recordedEvents = recordedEvents,
            startDate = startDate,
            endDate = endDate,
            zoneId = zoneId,
            evaluationEpochMs = evaluationEpochMs
        )

        val groupedByDate = allResolved.groupBy { item ->
            Instant.ofEpochMilli(item.occurrence.scheduledEpochMs)
                .atZone(zoneId)
                .toLocalDate()
        }

        val daysMap = LinkedHashMap<LocalDate, DailyAdherenceRecord>()
        var date = startDate
        while (!date.isAfter(endDate)) {
            val dayItems = groupedByDate[date].orEmpty()
            var confirmed = 0
            var assumed = 0
            var skipped = 0
            var missed = 0
            var pendingFuture = 0
            var corrected = 0

            for (item in dayItems) {
                if (item.isFuture) {
                    pendingFuture++
                } else {
                    if (item.isCorrected) corrected++
                    when (item.occurrence.status) {
                        DoseStatus.TAKEN -> confirmed++
                        DoseStatus.ASSUMED -> assumed++
                        DoseStatus.SKIPPED -> skipped++
                        DoseStatus.MISSED -> missed++
                        DoseStatus.PENDING -> missed++
                    }
                }
            }

            daysMap[date] = DailyAdherenceRecord(
                date = date,
                occurrences = dayItems,
                confirmedCount = confirmed,
                assumedCount = assumed,
                skippedCount = skipped,
                missedCount = missed,
                pendingFutureCount = pendingFuture,
                correctedCount = corrected
            )
            date = date.plusDays(1)
        }

        val monthSummary = summarizeResolvedOccurrences(
            periodLabel = "${yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${yearMonth.year}",
            startDate = startDate,
            endDate = endDate,
            resolvedOccurrences = allResolved
        )

        return MonthlyCalendarData(
            yearMonth = yearMonth,
            daysByDate = daysMap,
            monthSummary = monthSummary
        )
    }

    /**
     * Builds a chronological (newest-first) dose history for a specific medication,
     * applying optional date-range and outcome filters.
     */
    fun buildMedicationDoseHistory(
        medication: Medication,
        schedules: List<ScheduleDefinition>,
        recordedEvents: List<DoseOccurrence>,
        dateFilter: HistoryDateFilter,
        outcomeFilter: DoseOutcomeFilter,
        zoneId: ZoneId,
        evaluationEpochMs: Long
    ): List<ResolvedDoseOccurrence> {
        val currentLocalDate = Instant.ofEpochMilli(evaluationEpochMs).atZone(zoneId).toLocalDate()

        val earliestScheduleDay = schedules
            .filter { it.medicationId == medication.id }
            .minOfOrNull { it.startDateEpochDay }
        val earliestRecordedDay = recordedEvents
            .filter { it.medicationId == medication.id }
            .minOfOrNull {
                Instant.ofEpochMilli(it.scheduledEpochMs).atZone(zoneId).toLocalDate().toEpochDay()
            }

        val earliestTrackingDay = listOfNotNull(earliestScheduleDay, earliestRecordedDay).minOrNull()
            ?: return emptyList()

        val earliestDate = LocalDate.ofEpochDay(earliestTrackingDay)
        val filterStartDate = if (dateFilter.daysBack != null) {
            val candidate = currentLocalDate.minusDays(dateFilter.daysBack - 1)
            if (candidate.isAfter(earliestDate)) candidate else earliestDate
        } else {
            earliestDate
        }

        if (filterStartDate.isAfter(currentLocalDate)) return emptyList()

        val resolved = resolveOccurrencesForRange(
            medications = listOf(medication),
            schedules = schedules.filter { it.medicationId == medication.id },
            recordedEvents = recordedEvents.filter { it.medicationId == medication.id },
            startDate = filterStartDate,
            endDate = currentLocalDate,
            zoneId = zoneId,
            evaluationEpochMs = evaluationEpochMs
        )

        return resolved
            .filter { !it.isFuture }
            .filter { item ->
                when (outcomeFilter) {
                    DoseOutcomeFilter.ALL -> true
                    DoseOutcomeFilter.CONFIRMED -> item.occurrence.status == DoseStatus.TAKEN
                    DoseOutcomeFilter.ASSUMED -> item.occurrence.status == DoseStatus.ASSUMED
                    DoseOutcomeFilter.SKIPPED -> item.occurrence.status == DoseStatus.SKIPPED
                    DoseOutcomeFilter.MISSED -> item.occurrence.status == DoseStatus.MISSED
                    DoseOutcomeFilter.CORRECTED -> item.isCorrected
                }
            }
            .sortedByDescending { it.occurrence.scheduledEpochMs }
    }
}
