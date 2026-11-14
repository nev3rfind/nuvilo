package com.example.nuvilo.domain.stock

import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class SupplyForecast(
    val totalUsableUnits: ScaledQuantity,
    val unit: StockUnit,
    val daysOfSupply: Int?,
    val expectedRunoutEpochDay: Long?,
    val unitsLikelyLostToExpiryOrDiscard: ScaledQuantity,
    val precision: TrackingPrecision,
    val openedPackagesCount: Int,
    val unopenedPackagesCount: Int,
    val activeOpenedPackage: StockLot?,
    val daysUntilActivePackageDiscard: Int?,
    val daysUntilSoonestExpiry: Int?,
    val packagesNeedingDiscardCount: Int,
    val expiredLotsCount: Int,
    val needsAttentionForExpiredOrOpenLimit: Boolean,
    val isLowStock: Boolean
)

/**
 * Pure Kotlin deterministic supply forecaster using discrete day-by-day FEFO package simulation.
 * Accounts for manufacturer expiry dates AND discard-after-opening countdowns (e.g., 28-day eye drops)
 * using scaled-integer arithmetic only.
 */
class SupplyForecastCalculator(
    private val consumptionCalculator: ConsumptionCalculator = ConsumptionCalculator()
) {

    fun calculateSupplyForecast(
        medication: Medication,
        schedules: List<ScheduleDefinition>,
        lots: List<StockLot>,
        ledgerTotalUsable: ScaledQuantity,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SupplyForecast {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val unit = medication.effectiveConsumptionUnit

        var expiredCount = 0
        var pastOpenDiscardCount = 0

        val eligibleOpened = mutableListOf<StockLot>()
        val eligibleUnopened = mutableListOf<StockLot>()

        for (lot in lots) {
            if (!lot.remainingQuantity.isPositive()) continue
            when (lot.packageStatus) {
                PackageStatus.EMPTY, PackageStatus.DISCARDED -> continue
                PackageStatus.EXPIRED -> {
                    expiredCount++
                    continue
                }
                PackageStatus.OPENED -> {
                    val mfgExpired = lot.expiryDateEpochMs != null && lot.expiryDateEpochMs <= nowEpochMs
                    val openDiscardExpired = lot.discardAfterOpeningEpochMs != null &&
                        Instant.ofEpochMilli(lot.discardAfterOpeningEpochMs).atZone(zoneId).toLocalDate() <= today &&
                        lot.discardAfterOpeningEpochMs <= nowEpochMs
                    if (mfgExpired) {
                        expiredCount++
                        continue
                    }
                    if (openDiscardExpired) {
                        pastOpenDiscardCount++
                        continue
                    }
                    eligibleOpened.add(lot)
                }
                PackageStatus.UNOPENED -> {
                    val mfgExpired = lot.expiryDateEpochMs != null && lot.expiryDateEpochMs <= nowEpochMs
                    if (mfgExpired) {
                        expiredCount++
                        continue
                    }
                    eligibleUnopened.add(lot)
                }
            }
        }

        val sortedOpened = eligibleOpened.sortedWith(
            compareBy<StockLot>(
                { it.discardAfterOpeningEpochMs ?: Long.MAX_VALUE },
                { it.expiryDateEpochMs ?: Long.MAX_VALUE },
                { it.createdAtEpochMs }
            )
        )
        val sortedUnopened = eligibleUnopened.sortedWith(
            compareBy<StockLot>(
                { it.expiryDateEpochMs ?: Long.MAX_VALUE },
                { it.createdAtEpochMs }
            )
        )

        val totalUsableFromLots = ScaledQuantity(
            (sortedOpened.sumOf { it.remainingQuantity.rawScaledValue }) +
                (sortedUnopened.sumOf { it.remainingQuantity.rawScaledValue })
        )
        val hasTrackedPackages = lots.isNotEmpty()
        val effectiveUsableUnits = if (hasTrackedPackages) totalUsableFromLots else ledgerTotalUsable

        val activeOpen = sortedOpened.firstOrNull()
        val daysUntilDiscard = activeOpen?.discardAfterOpeningEpochMs?.let { cutoffMs ->
            val cutoffDate = Instant.ofEpochMilli(cutoffMs).atZone(zoneId).toLocalDate()
            ChronoUnit.DAYS.between(today, cutoffDate).toInt().coerceAtLeast(0)
        }

        val allEligible = sortedOpened + sortedUnopened
        val daysUntilExpiry = allEligible.mapNotNull { lot ->
            lot.expiryDateEpochMs?.let { expMs ->
                val expDate = Instant.ofEpochMilli(expMs).atZone(zoneId).toLocalDate()
                ChronoUnit.DAYS.between(today, expDate).toInt().coerceAtLeast(0)
            }
        }.minOrNull()

        val scheduledDailyNeed = schedules.any { schedule ->
            schedule.scheduleType != ScheduleType.PRN && schedule.targetTimesMinuteOfDay.isNotEmpty()
        }

        val precision = if (!hasTrackedPackages && medication.packagingConfig == null) {
            if (medication.form.isEstimatedConsumption) TrackingPrecision.ESTIMATED else TrackingPrecision.UNKNOWN
        } else {
            val schConsumption = consumptionCalculator.calculateOccurrenceConsumption(medication, schedules.firstOrNull())
            schConsumption.precision
        }

        // If no regular daily/weekday schedule or tube-count-only (0 per dose consumption), cannot compute days
        val sampleNeedOnActiveDay = schedules
            .filter { it.scheduleType != ScheduleType.PRN && it.targetTimesMinuteOfDay.isNotEmpty() }
            .sumOf { sch ->
                val perOcc = consumptionCalculator.calculateOccurrenceConsumption(medication, sch)
                perOcc.quantity.rawScaledValue * sch.targetTimesMinuteOfDay.size
            }

        if (!scheduledDailyNeed || sampleNeedOnActiveDay <= 0L) {
            val isLow = (effectiveUsableUnits <= medication.lowStockThreshold)
            return SupplyForecast(
                totalUsableUnits = effectiveUsableUnits,
                unit = unit,
                daysOfSupply = null,
                expectedRunoutEpochDay = null,
                unitsLikelyLostToExpiryOrDiscard = ScaledQuantity.ZERO,
                precision = if (sampleNeedOnActiveDay <= 0L) TrackingPrecision.UNKNOWN else precision,
                openedPackagesCount = sortedOpened.size,
                unopenedPackagesCount = sortedUnopened.size,
                activeOpenedPackage = activeOpen,
                daysUntilActivePackageDiscard = daysUntilDiscard,
                daysUntilSoonestExpiry = daysUntilExpiry,
                packagesNeedingDiscardCount = expiredCount + pastOpenDiscardCount,
                expiredLotsCount = expiredCount,
                needsAttentionForExpiredOrOpenLimit = (expiredCount + pastOpenDiscardCount) > 0,
                isLowStock = isLow
            )
        }

        // Simulate day by day cleanly with mutable local slots
        data class SimPkg(
            var remainingRaw: Long,
            var isOpen: Boolean,
            val mfgExpiryDate: LocalDate?,
            val discardDays: Int?,
            var openDiscardDate: LocalDate?
        )

        val simQueue: MutableList<SimPkg> = if (hasTrackedPackages) {
            (sortedOpened + sortedUnopened).map { lot ->
                SimPkg(
                    remainingRaw = lot.remainingQuantity.rawScaledValue,
                    isOpen = lot.packageStatus == PackageStatus.OPENED,
                    mfgExpiryDate = lot.expiryDateEpochMs?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() },
                    discardDays = lot.discardAfterOpeningDays ?: medication.packagingConfig?.discardAfterOpeningDays,
                    openDiscardDate = lot.discardAfterOpeningEpochMs?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
                )
            }.toMutableList()
        } else if (effectiveUsableUnits.isPositive()) {
            mutableListOf(
                SimPkg(
                    remainingRaw = effectiveUsableUnits.rawScaledValue,
                    isOpen = true,
                    mfgExpiryDate = null,
                    discardDays = null,
                    openDiscardDate = null
                )
            )
        } else {
            mutableListOf()
        }

        var lostRaw = 0L
        var coveredDays = 0
        val maxSimulationDays = 3650

        for (dayOffset in 0 until maxSimulationDays) {
            val simDate = today.plusDays(dayOffset.toLong())
            val simEpochDay = simDate.toEpochDay()

            // Remove packages that have expired on or before simDate
            val iterator = simQueue.iterator()
            while (iterator.hasNext()) {
                val pkg = iterator.next()
                val reachedMfgExpiry = pkg.mfgExpiryDate != null && !simDate.isBefore(pkg.mfgExpiryDate)
                val reachedOpenDiscard = pkg.isOpen && pkg.openDiscardDate != null && !simDate.isBefore(pkg.openDiscardDate)
                if (reachedMfgExpiry || reachedOpenDiscard || pkg.remainingRaw <= 0L) {
                    if (pkg.remainingRaw > 0L) {
                        lostRaw += pkg.remainingRaw
                    }
                    iterator.remove()
                }
            }

            if (simQueue.isEmpty()) break

            // Daily demand on simDate across active schedules
            var demandRaw = 0L
            for (sch in schedules) {
                if (simEpochDay < sch.startDateEpochDay) continue
                if (sch.endDateEpochDay != null && simEpochDay > sch.endDateEpochDay) continue
                val applies = when (sch.scheduleType) {
                    ScheduleType.DAILY, ScheduleType.CYCLICAL -> true
                    ScheduleType.SPECIFIC_WEEKDAYS -> sch.daysOfWeek.contains(simDate.dayOfWeek.value)
                    ScheduleType.PRN -> false
                }
                if (applies && sch.targetTimesMinuteOfDay.isNotEmpty()) {
                    val perOcc = consumptionCalculator.calculateOccurrenceConsumption(medication, sch)
                    demandRaw += perOcc.quantity.rawScaledValue * sch.targetTimesMinuteOfDay.size
                }
            }

            if (demandRaw == 0L) {
                coveredDays++
                continue
            }

            // Can today's full demand be satisfied from simQueue?
            val availableRaw = simQueue.sumOf { it.remainingRaw }
            if (availableRaw < demandRaw) {
                break
            }

            var remainingToDeduct = demandRaw
            while (remainingToDeduct > 0L && simQueue.isNotEmpty()) {
                val head = simQueue.first()
                if (!head.isOpen) {
                    head.isOpen = true
                    if (head.discardDays != null && head.discardDays > 0) {
                        head.openDiscardDate = simDate.plusDays(head.discardDays.toLong())
                    }
                }
                val take = minOf(remainingToDeduct, head.remainingRaw)
                head.remainingRaw -= take
                remainingToDeduct -= take
                if (head.remainingRaw <= 0L) {
                    simQueue.removeAt(0)
                }
            }

            coveredDays++
        }

        val runoutEpochDay = if (simQueue.isEmpty() || coveredDays < maxSimulationDays) {
            today.plusDays(coveredDays.toLong()).toEpochDay()
        } else {
            null
        }

        val isLowByUnits = effectiveUsableUnits <= medication.lowStockThreshold
        val isLowByDays = medication.lowStockDaysThreshold != null && coveredDays <= medication.lowStockDaysThreshold

        return SupplyForecast(
            totalUsableUnits = effectiveUsableUnits,
            unit = unit,
            daysOfSupply = coveredDays,
            expectedRunoutEpochDay = runoutEpochDay,
            unitsLikelyLostToExpiryOrDiscard = ScaledQuantity(lostRaw),
            precision = precision,
            openedPackagesCount = sortedOpened.size,
            unopenedPackagesCount = sortedUnopened.size,
            activeOpenedPackage = activeOpen,
            daysUntilActivePackageDiscard = daysUntilDiscard,
            daysUntilSoonestExpiry = daysUntilExpiry,
            packagesNeedingDiscardCount = expiredCount + pastOpenDiscardCount,
            expiredLotsCount = expiredCount,
            needsAttentionForExpiredOrOpenLimit = (expiredCount + pastOpenDiscardCount) > 0,
            isLowStock = isLowByUnits || isLowByDays
        )
    }
}
