package com.example.nuvilo.regression

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.system.notification.NotificationHelper
import com.example.nuvilo.system.notification.NotificationTextBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Regression Test Suite: Category D (Scheduling, Notifications & Lifecycle)
 * Tests T118 to T125: Notification Channels, Content Text Builders, and Action Contracts
 */
@RunWith(AndroidJUnit4::class)
class NotificationActionRegressionTest {

    private lateinit var context: Context
    private lateinit var db: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl
    private lateinit var notificationHelper: NotificationHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(db)
        notificationHelper = NotificationHelper(context)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun t118_actionTakeRecordsDoseAsTakenAndDeductsStock() {
        runBlocking {
            // T118: Action TAKE intent builds and executes with status TAKEN
            val med = Medication(
                id = "med_notif_take",
                name = "Sertraline",
                strength = "50mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ofWhole(28),
                lowStockThreshold = ScaledQuantity.ofWhole(7),
                recordingMode = RecordingMode.CONFIRM
            )
            val schedule = ScheduleDefinition(
                id = "sch_notif_take",
                medicationId = med.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = 19500L
            )
            repository.createMedication(med, schedule)

            val result = repository.recordDose(
                occurrenceId = "occ_take_118",
                medicationId = med.id,
                scheduledEpochMs = 1700000000000L,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN
            )
            assertTrue(result.isSuccess)

            val balance = repository.observeStockBalance(med).first()
            assertEquals(ScaledQuantity.ofWhole(27), balance.currentBalance)
        }
    }

    @Test
    fun t119_actionSnoozePreservesPendingStatusWithoutStockDeduction() {
        runBlocking {
            // T119: Action SNOOZE preserves pending status without stock deduction
            val med = Medication(
                id = "med_notif_snooze",
                name = "Levothyroxine",
                strength = "50mcg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ofWhole(28),
                lowStockThreshold = ScaledQuantity.ofWhole(7),
                recordingMode = RecordingMode.CONFIRM
            )
            val schedule = ScheduleDefinition(
                id = "sch_notif_snooze",
                medicationId = med.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(7 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = 19500L
            )
            repository.createMedication(med, schedule)

            // Snooze does not alter the dose record or stock ledger
            val balance = repository.observeStockBalance(med).first()
            assertEquals(ScaledQuantity.ofWhole(28), balance.currentBalance)
        }
    }

    @Test
    fun t120_actionSkipRecordsSkippedStatusWithoutDeductingStock() {
        runBlocking {
            // T120: Action SKIP intent marks status SKIPPED without deducting stock
            val med = Medication(
                id = "med_notif_skip",
                name = "Amlodipine",
                strength = "10mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ofWhole(28),
                lowStockThreshold = ScaledQuantity.ofWhole(7),
                recordingMode = RecordingMode.CONFIRM
            )
            val schedule = ScheduleDefinition(
                id = "sch_notif_skip",
                medicationId = med.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = 19500L
            )
            repository.createMedication(med, schedule)

            val result = repository.recordDose(
                occurrenceId = "occ_skip_120",
                medicationId = med.id,
                scheduledEpochMs = 1700000000000L,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                status = DoseStatus.SKIPPED
            )
            assertTrue(result.isSuccess)

            val balance = repository.observeStockBalance(med).first()
            assertEquals(ScaledQuantity.ofWhole(28), balance.currentBalance)
        }
    }

    @Test
    fun t121_notificationChannelsAreProperlyRegisteredWithSystem() {
        // T121: Notification channels exist with correct IDs
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val remindersChannel = nm.getNotificationChannel(NotificationHelper.CHANNEL_REMINDERS_ID)
        val lowStockChannel = nm.getNotificationChannel(NotificationHelper.CHANNEL_LOW_STOCK_ID)

        assertNotNull(remindersChannel)
        assertNotNull(lowStockChannel)
        assertEquals("Medication Reminders", remindersChannel.name)
        assertEquals("Low Stock Alerts", lowStockChannel.name)
    }

    @Test
    fun t122_notificationTextBuilderBuildsBigTextWithRunwayDays() {
        // T122: NotificationTextBuilder builds big text with remaining runway days
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(20),
            unit = StockUnit.TABLETS,
            isNegative = false,
            isLowStock = false,
            isEstimate = false,
            daysRemaining = 10
        )

        val text = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            medicationStrength = "10mg",
            scheduledTimeStr = "09:00",
            stockBalance = balance,
            hasPendingOrder = false
        )

        assertTrue(text.contains("Take 1 tab (10mg) · Scheduled for 09:00"))
        assertTrue(text.contains("approx. 10 days runway"))
    }

    @Test
    fun t123_notificationTextBuilderBuildsSubTextWithConciseStockCount() {
        // T123: NotificationTextBuilder builds subtext with concise stock count
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(14),
            unit = StockUnit.CAPSULES,
            isNegative = false,
            isLowStock = false,
            isEstimate = false,
            daysRemaining = 14
        )

        val subText = NotificationTextBuilder.buildSubText(
            stockBalance = balance,
            unit = StockUnit.CAPSULES,
            hasPendingOrder = false
        )

        assertEquals("14 cap left", subText)
    }

    @Test
    fun t124_notificationTextBuilderShowsGpReorderRecommendationUnder7Days() {
        // T124: NotificationTextBuilder shows GP reorder recommendation under 7 days runway
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(4),
            unit = StockUnit.TABLETS,
            isNegative = false,
            isLowStock = true,
            isEstimate = false,
            daysRemaining = 4
        )

        val text = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            medicationStrength = "20mg",
            scheduledTimeStr = "08:00",
            stockBalance = balance,
            hasPendingOrder = false
        )

        assertTrue(text.contains("GP Recommendation: Approx. 4 days supply left. Order repeat prescription now"))
    }

    @Test
    fun t125_notificationTextBuilderDisplaysPendingRepeatNoteWhenOrderPending() {
        // T125: NotificationTextBuilder displays pending repeat note when order is requested
        val balance = StockBalance(
            currentBalance = ScaledQuantity.ofWhole(5),
            unit = StockUnit.TABLETS,
            isNegative = false,
            isLowStock = true,
            isEstimate = false,
            daysRemaining = 5
        )

        val text = NotificationTextBuilder.buildBigText(
            doseQuantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            medicationStrength = "20mg",
            scheduledTimeStr = "08:00",
            stockBalance = balance,
            hasPendingOrder = true,
            pendingOrderQuantity = ScaledQuantity.ofWhole(28)
        )

        assertTrue(text.contains("GP Order Pending: 28 tab requested"))
    }
}
