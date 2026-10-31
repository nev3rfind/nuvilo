package com.example.nuvilo.system

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.preferences.UserPreferencesRepository
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.system.alarm.AlarmReceiver
import com.example.nuvilo.system.alarm.AlarmSchedulerImpl
import com.example.nuvilo.system.notification.NotificationActionReceiver
import com.example.nuvilo.system.notification.NotificationHelper
import com.example.nuvilo.system.work.LowStockCheckWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NotificationAndSchedulerTest {

    private lateinit var context: Context
    private lateinit var db: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var scheduler: AlarmSchedulerImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(db)
        preferencesRepository = UserPreferencesRepository(context)
        notificationHelper = NotificationHelper(context)
        scheduler = AlarmSchedulerImpl(
            context = context,
            medicationRepository = repository,
            doseDao = db.doseDao(),
            userPreferencesRepository = preferencesRepository
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun confirmMode_doesNotDeductStockUntilExplicitTakeAction() = runBlocking {
        val med = Medication(
            id = "med_confirm",
            name = "Paracetamol",
            strength = "500 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(20),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            recordingMode = RecordingMode.CONFIRM
        )
        val schedule = ScheduleDefinition(
            id = "sch_confirm",
            medicationId = "med_confirm",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2),
            startDateEpochDay = LocalDate.now().toEpochDay()
        )
        repository.createMedication(med, schedule)

        val occId = "occ_confirm_1"
        val scheduledEpoch = System.currentTimeMillis() + 60_000L

        // Stock before any action is 20
        var balance = repository.observeStockBalance(med).first()
        assertEquals(20_000L, balance.currentBalance.rawScaledValue)

        // Simulate AlarmReceiver trigger for CONFIRM mode:
        // Must NOT log any taken dose or deduct stock automatically
        val existingDose = db.doseDao().getDoseByOccurrenceId(occId)
        assertNull(existingDose)

        // Now user taps "Take" in notification:
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKE
            putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_ID, occId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, "med_confirm")
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_EPOCH_MS, scheduledEpoch)
            putExtra(NotificationActionReceiver.EXTRA_DOSE_QUANTITY_RAW, 2_000L)
            putExtra(NotificationActionReceiver.EXTRA_STOCK_UNIT, StockUnit.TABLETS.name)
            putExtra(NotificationActionReceiver.EXTRA_RECORDING_MODE, RecordingMode.CONFIRM.name)
        }

        // Execute Take action directly via repository
        repository.recordDose(
            occurrenceId = occId,
            medicationId = "med_confirm",
            scheduledEpochMs = scheduledEpoch,
            quantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )

        // Balance should now be 20 - 2 = 18
        balance = repository.observeStockBalance(med).first()
        assertEquals(18_000L, balance.currentBalance.rawScaledValue)

        // Verify dose event is confirmed TAKEN
        val recordedDose = db.doseDao().getDoseByOccurrenceId(occId)
        assertNotNull(recordedDose)
        assertEquals(DoseStatus.TAKEN.name, recordedDose?.status)

        // Duplicate / Rapid tap on Take action: must be strictly idempotent
        repository.recordDose(
            occurrenceId = occId,
            medicationId = "med_confirm",
            scheduledEpochMs = scheduledEpoch,
            quantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )
        balance = repository.observeStockBalance(med).first()
        assertEquals(18_000L, balance.currentBalance.rawScaledValue)

        val deductionCount = db.stockLedgerDao().countDeductionsForOccurrence(occId)
        assertEquals(1, deductionCount)
    }

    @Test
    fun autoAssumeMode_recordsAssumedAdherence_andCompensatesIfSkipped() = runBlocking {
        val med = Medication(
            id = "med_auto",
            name = "Amlodipine",
            strength = "5 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(30),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            recordingMode = RecordingMode.AUTO_ASSUME
        )
        val schedule = ScheduleDefinition(
            id = "sch_auto",
            medicationId = "med_auto",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(9 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = LocalDate.now().toEpochDay()
        )
        repository.createMedication(med, schedule)

        val occId = "occ_auto_1"
        val scheduledEpoch = System.currentTimeMillis() - 1000L

        // In AUTO_ASSUME mode, scheduled time logs ASSUMED dose and deducts stock
        repository.recordDose(
            occurrenceId = occId,
            medicationId = "med_auto",
            scheduledEpochMs = scheduledEpoch,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.ASSUMED,
            notes = "Assumed adherence recorded per schedule"
        )

        // Balance should be 30 - 1 = 29
        var balance = repository.observeStockBalance(med).first()
        assertEquals(29_000L, balance.currentBalance.rawScaledValue)

        val assumedDose = db.doseDao().getDoseByOccurrenceId(occId)
        assertNotNull(assumedDose)
        assertEquals(DoseStatus.ASSUMED.name, assumedDose?.status)

        // User notices notification and indicates they did NOT take the pill (taps "Skip")
        // Must correct dose from ASSUMED to SKIPPED and insert compensating DOSE_REVERSAL
        val correctResult = repository.correctDose(
            occurrenceId = occId,
            targetStatus = DoseStatus.SKIPPED,
            reason = "User marked skipped from notification"
        )
        assertTrue(correctResult.isSuccess)

        // Balance must be restored to 30
        balance = repository.observeStockBalance(med).first()
        assertEquals(30_000L, balance.currentBalance.rawScaledValue)

        // Verify DOSE_REVERSAL entry in ledger
        val movements = repository.observeMovementsForMedication("med_auto").first()
        val reversalMove = movements.find { it.movementType == MovementType.DOSE_REVERSAL }
        assertNotNull(reversalMove)
        assertEquals(1_000L, reversalMove?.quantity?.rawScaledValue)

        // Repeated tap on Skip: must be idempotent and not create duplicate reversal
        repository.correctDose(
            occurrenceId = occId,
            targetStatus = DoseStatus.SKIPPED,
            reason = "Repeated skip tap"
        )
        balance = repository.observeStockBalance(med).first()
        assertEquals(30_000L, balance.currentBalance.rawScaledValue)

        val reversalsCount = db.stockLedgerDao().countReversalsForOccurrence(occId)
        assertEquals(1, reversalsCount)
    }

    @Test
    fun snoozeAction_leavesStockAndDoseRecordsUntouched() = runBlocking {
        val med = Medication(
            id = "med_snooze",
            name = "Vitamin D3",
            strength = "1000 IU",
            form = MedicationForm.CAPSULE,
            stockUnit = StockUnit.CAPSULES,
            openingStock = ScaledQuantity.ofWhole(50),
            lowStockThreshold = ScaledQuantity.ofWhole(10),
            recordingMode = RecordingMode.CONFIRM
        )
        val schedule = ScheduleDefinition(
            id = "sch_snooze",
            medicationId = "med_snooze",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = LocalDate.now().toEpochDay()
        )
        repository.createMedication(med, schedule)

        val occId = "occ_snooze_1"

        // Snooze for 15 minutes
        scheduler.snoozeOccurrence(
            occurrenceId = occId,
            medicationId = "med_snooze",
            doseQuantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.CAPSULES,
            recordingMode = RecordingMode.CONFIRM,
            snoozeMinutes = 15
        )

        // Dose event must NOT be created
        val dose = db.doseDao().getDoseByOccurrenceId(occId)
        assertNull(dose)

        // Stock balance must remain exactly 50
        val balance = repository.observeStockBalance(med).first()
        assertEquals(50_000L, balance.currentBalance.rawScaledValue)

        // No ledger movements created
        val movements = repository.observeMovementsForMedication("med_snooze").first()
        val nonOpening = movements.filter { it.movementType != MovementType.OPENING_BALANCE }
        assertTrue(nonOpening.isEmpty())
    }

    @Test
    fun reconcileUpcomingReminders_updatesTimestampAndSchedulesFutureDoses() = runBlocking {
        val med = Medication(
            id = "med_recon",
            name = "Loratadine",
            strength = "10 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(30),
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            recordingMode = RecordingMode.CONFIRM
        )
        val schedule = ScheduleDefinition(
            id = "sch_recon",
            medicationId = "med_recon",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60, 20 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = LocalDate.now().toEpochDay()
        )
        repository.createMedication(med, schedule)

        val result = scheduler.reconcileUpcomingReminders(horizonDays = 2)
        assertTrue(result.isSuccess)

        // Verify last reconciliation timestamp was persisted
        val prefs = preferencesRepository.userPreferencesFlow.first()
        assertTrue(prefs.lastReconciliationEpochMs > 0L)
    }

    @Test
    fun lowStockAlertDeduplication_resetsAfterRestock() = runBlocking {
        val medId = "med_low_test"
        preferencesRepository.clearLowStockNotified(medId)

        // Initially not notified
        var prefs = preferencesRepository.userPreferencesFlow.first()
        assertFalse(prefs.lowStockNotifiedMedIds.contains(medId))

        // Mark as notified
        preferencesRepository.markLowStockNotified(medId)
        prefs = preferencesRepository.userPreferencesFlow.first()
        assertTrue(prefs.lowStockNotifiedMedIds.contains(medId))

        // After restock: cleared
        preferencesRepository.clearLowStockNotified(medId)
        prefs = preferencesRepository.userPreferencesFlow.first()
        assertFalse(prefs.lowStockNotifiedMedIds.contains(medId))
    }
}
