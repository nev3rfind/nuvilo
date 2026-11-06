package com.example.nuvilo.regression

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.system.work.LowStockCheckWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category D (Scheduling, Notifications & Lifecycle)
 * Tests T131 to T135: WorkManager Periodic Low-Stock Check Worker
 */
@RunWith(AndroidJUnit4::class)
class BackgroundWorkerRegressionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun t131_workerExecutionReturnsSuccessOnCompletedRun() {
        runBlocking {
            // T131: LowStockCheckWorker execution with empty database returns Result.success()
            val worker = TestListenableWorkerBuilder<LowStockCheckWorker>(context).build()
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun t132_workerIdentifiesLowStockMedicationAndUpdatesNotifiedSet() {
        runBlocking {
            // T132: Worker identifies medication with low stock and triggers low stock alert
            val app = context.applicationContext as NoveloApplication
            val container = app.container

            val lowStockMed = Medication(
                id = "med_worker_low_132",
                name = "Salbutamol Inhaler",
                strength = "100mcg",
                form = MedicationForm.INHALER_PUFFS,
                stockUnit = StockUnit.PUFFS,
                openingStock = ScaledQuantity.ofWhole(10), // below threshold of 20
                lowStockThreshold = ScaledQuantity.ofWhole(20),
                recordingMode = RecordingMode.CONFIRM
            )
            val schedule = ScheduleDefinition(
                id = "sch_worker_low_132",
                medicationId = lowStockMed.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2),
                startDateEpochDay = 19500L
            )
            container.repository.createMedication(lowStockMed, schedule)

            val worker = TestListenableWorkerBuilder<LowStockCheckWorker>(context).build()
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)

            val prefs = container.userPreferencesRepository.userPreferencesFlow.first()
            assertTrue(prefs.lowStockNotifiedMedIds.contains("med_worker_low_132"))
        }
    }

    @Test
    fun t133_workerDeduplicatesAlertsWhenAlreadyNotified() {
        runBlocking {
            // T133: Worker does not re-alert if medication ID is already in lowStockNotifiedMedIds
            val app = context.applicationContext as NoveloApplication
            val container = app.container

            val lowStockMed = Medication(
                id = "med_worker_low_133",
                name = "Insulin Glargine",
                strength = "100 units/ml",
                form = MedicationForm.INJECTION,
                stockUnit = StockUnit.UNITS,
                openingStock = ScaledQuantity.ofWhole(5),
                lowStockThreshold = ScaledQuantity.ofWhole(15),
                recordingMode = RecordingMode.CONFIRM
            )
            val schedule = ScheduleDefinition(
                id = "sch_worker_low_133",
                medicationId = lowStockMed.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(20 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(10),
                startDateEpochDay = 19500L
            )
            container.repository.createMedication(lowStockMed, schedule)

            // Mark as already notified
            container.userPreferencesRepository.markLowStockNotified("med_worker_low_133")

            val worker = TestListenableWorkerBuilder<LowStockCheckWorker>(context).build()
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)

            val prefs = container.userPreferencesRepository.userPreferencesFlow.first()
            assertTrue(prefs.lowStockNotifiedMedIds.contains("med_worker_low_133"))
        }
    }

    @Test
    fun t134_restockingAboveThresholdResetsNotifiedState() {
        runBlocking {
            // T134: Restocking medication above threshold clears notified state so future alerts can fire
            val app = context.applicationContext as NoveloApplication
            val container = app.container

            val med = Medication(
                id = "med_worker_low_134",
                name = "Atorvastatin Calcium",
                strength = "40mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ofWhole(5),
                lowStockThreshold = ScaledQuantity.ofWhole(10),
                recordingMode = RecordingMode.CONFIRM
            )
            container.repository.createMedication(med, ScheduleDefinition(
                id = "sch_worker_low_134",
                medicationId = med.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = 19500L
            ))
            container.userPreferencesRepository.markLowStockNotified(med.id)

            // Restock by 30 tablets (new balance: 35 > threshold 10)
            container.repository.adjustStock(
                medicationId = med.id,
                quantity = ScaledQuantity.ofWhole(30),
                unit = StockUnit.TABLETS,
                reason = "Restocked tablets",
                isIncrease = true
            )

            val worker = TestListenableWorkerBuilder<LowStockCheckWorker>(context).build()
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)

            val prefs = container.userPreferencesRepository.userPreferencesFlow.first()
            assertFalse(prefs.lowStockNotifiedMedIds.contains(med.id))
        }
    }

    @Test
    fun t135_workerExecutionWithHealthyStockCompletesWithoutNotification() {
        runBlocking {
            // T135: Worker execution with healthy stock completes without adding to lowStockNotifiedMedIds
            val app = context.applicationContext as NoveloApplication
            val container = app.container

            val healthyMed = Medication(
                id = "med_worker_healthy_135",
                name = "Cetirizine",
                strength = "10mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ofWhole(60),
                lowStockThreshold = ScaledQuantity.ofWhole(7),
                recordingMode = RecordingMode.CONFIRM
            )
            val schedule = ScheduleDefinition(
                id = "sch_worker_healthy_135",
                medicationId = healthyMed.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = 19500L
            )
            container.repository.createMedication(healthyMed, schedule)

            val worker = TestListenableWorkerBuilder<LowStockCheckWorker>(context).build()
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)

            val prefs = container.userPreferencesRepository.userPreferencesFlow.first()
            assertFalse(prefs.lowStockNotifiedMedIds.contains("med_worker_healthy_135"))
        }
    }
}
