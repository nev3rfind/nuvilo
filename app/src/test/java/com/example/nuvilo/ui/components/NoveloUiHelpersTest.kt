package com.example.nuvilo.ui.components

import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class NoveloUiHelpersTest {

    @Test
    fun defaultStockUnitForForm_mapsEveryDosageFormToExpectedUnit() {
        assertEquals(StockUnit.TABLETS, defaultStockUnitForForm(MedicationForm.TABLET))
        assertEquals(StockUnit.CAPSULES, defaultStockUnitForForm(MedicationForm.CAPSULE))
        assertEquals(StockUnit.ML, defaultStockUnitForForm(MedicationForm.LIQUID_ML))
        assertEquals(StockUnit.PUFFS, defaultStockUnitForForm(MedicationForm.INHALER_PUFFS))
        assertEquals(StockUnit.APPLICATIONS, defaultStockUnitForForm(MedicationForm.TOPICAL_CREAM))
        assertEquals(StockUnit.DROPS, defaultStockUnitForForm(MedicationForm.DROPS))
        assertEquals(StockUnit.UNITS, defaultStockUnitForForm(MedicationForm.INJECTION))
        assertEquals(StockUnit.PATCHES, defaultStockUnitForForm(MedicationForm.PATCH))
        assertEquals(StockUnit.TABLETS, defaultStockUnitForForm(MedicationForm.OTHER))
    }

    @Test
    fun formatMinuteOfDay_formatsZeroTo2359Accurately() {
        assertEquals("00:00", formatMinuteOfDay(0))
        assertEquals("08:00", formatMinuteOfDay(480))
        assertEquals("09:30", formatMinuteOfDay(570))
        assertEquals("14:05", formatMinuteOfDay(845))
        assertEquals("20:00", formatMinuteOfDay(1200))
        assertEquals("23:59", formatMinuteOfDay(1439))
    }

    @Test
    fun formatScheduleSummaryPill_formatsDailyAndWeekdaySchedulesCleanly() {
        assertEquals("No active schedule", formatScheduleSummaryPill(emptyList()))

        val dailySchedule = ScheduleDefinition(
            id = "sch-1",
            medicationId = "med-1",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(1200, 480),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20000L
        )
        assertEquals("Daily · 08:00, 20:00", formatScheduleSummaryPill(listOf(dailySchedule)))

        val weekdaySchedule = ScheduleDefinition(
            id = "sch-2",
            medicationId = "med-1",
            scheduleType = ScheduleType.SPECIFIC_WEEKDAYS,
            targetTimesMinuteOfDay = listOf(540),
            daysOfWeek = setOf(5, 1, 3),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2),
            startDateEpochDay = 20000L
        )
        assertEquals("Mon, Wed, Fri · 09:00", formatScheduleSummaryPill(listOf(weekdaySchedule)))
    }
}
