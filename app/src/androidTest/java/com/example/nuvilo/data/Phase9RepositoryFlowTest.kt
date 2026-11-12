package com.example.nuvilo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase9RepositoryFlowTest {

    private lateinit var database: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun eyeDropPackageLifecycle_openOnFirstDose_recountOnlyOpened_discardAndReversalAudit() = runBlocking {
        val medId = "med-hydro-repo"
        val med = Medication(
            id = medId,
            name = "Hydro Forte",
            strength = "0.3%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ofWhole(600),
            lowStockThreshold = ScaledQuantity.ofWhole(30),
            recordingMode = RecordingMode.CONFIRM,
            packagingConfig = PackagingConfig(
                packageType = PackageType.BOTTLE,
                consumptionUnit = StockUnit.DROPS,
                contentsPerPackage = ScaledQuantity.ofWhole(300),
                netContentLabel = "10 ml",
                discardAfterOpeningDays = 28,
                trackingPrecision = TrackingPrecision.ESTIMATED
            ),
            administrationProfile = AdministrationProfile(
                site = AdministrationSite.BOTH_EYES,
                leftEyeQuantity = ScaledQuantity.ofWhole(1),
                rightEyeQuantity = ScaledQuantity.ofWhole(1)
            )
        )
        val schedule = ScheduleDefinition(
            id = "sch-hydro",
            medicationId = medId,
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(480),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2),
            startDateEpochDay = 20_000L
        )

        val nowMs = 1_760_000_000_000L
        val createResult = repository.createMedicationWithPackages(
            medication = med,
            initialSchedule = schedule,
            initialPackagesCount = 2,
            expiryDateEpochMs = nowMs + 365L * 86_400_000L,
            lotNumber = "HF-BATCH-1"
        )
        assertTrue(createResult.isSuccess)

        val initialLots = repository.observeLotsForMedication(medId).first()
        assertEquals(2, initialLots.size)
        assertTrue(initialLots.all { it.packageStatus == PackageStatus.UNOPENED })
        assertTrue(initialLots.all { it.remainingQuantity == ScaledQuantity.ofWhole(300) })

        // 1. First dose taken (2 drops across both eyes) -> auto-opens Bottle #1, stamps 28-day discard, leaves Bottle #2 unopened
        val occ1 = "occ_${medId}_$nowMs"
        val doseRes = repository.recordDose(
            occurrenceId = occ1,
            medicationId = medId,
            scheduledEpochMs = nowMs,
            quantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.DROPS,
            status = DoseStatus.TAKEN
        )
        assertTrue(doseRes.isSuccess)

        val lotsAfterDose = repository.observeLotsForMedication(medId).first().sortedBy { it.packageLabel }
        val bottle1 = lotsAfterDose.first { it.packageStatus == PackageStatus.OPENED }
        val bottle2 = lotsAfterDose.first { it.packageStatus == PackageStatus.UNOPENED }
        assertEquals(ScaledQuantity.ofWhole(298), bottle1.remainingQuantity)
        assertNotNull(bottle1.openedAtEpochMs)
        assertNotNull(bottle1.discardAfterOpeningEpochMs)
        assertEquals(ScaledQuantity.ofWhole(300), bottle2.remainingQuantity)
        assertNull(bottle2.openedAtEpochMs)

        // Idempotency check: repeat recordDose must not double deduct
        repository.recordDose(
            occurrenceId = occ1,
            medicationId = medId,
            scheduledEpochMs = nowMs,
            quantity = ScaledQuantity.ofWhole(2),
            unit = StockUnit.DROPS,
            status = DoseStatus.TAKEN
        )
        val balanceAfterRepeat = repository.observeStockBalance(med).first()
        assertEquals(ScaledQuantity.ofWhole(598), balanceAfterRepeat.currentBalance)

        // 2. Scoped recount of opened bottle #1 ONLY (298 -> 250 drops observed)
        val recountRes = repository.recountOpenedPackage(
            lotId = bottle1.id,
            observedRemaining = ScaledQuantity.ofWhole(250),
            reason = "Bottle appears ~50 drops lighter"
        )
        assertTrue(recountRes.isSuccess)
        val lotsAfterRecount = repository.observeLotsForMedication(medId).first()
        assertEquals(ScaledQuantity.ofWhole(250), lotsAfterRecount.first { it.id == bottle1.id }.remainingQuantity)
        assertEquals(ScaledQuantity.ofWhole(300), lotsAfterRecount.first { it.id == bottle2.id }.remainingQuantity)
        assertEquals(PackageStatus.UNOPENED, lotsAfterRecount.first { it.id == bottle2.id }.packageStatus)
        assertEquals(ScaledQuantity.ofWhole(550), repository.observeStockBalance(med).first().currentBalance)

        // 3. Correct dose back to SKIPPED -> restores 2 drops to bottle1 ONLY (250 -> 252)
        val correctRes = repository.correctDose(
            occurrenceId = occ1,
            targetStatus = DoseStatus.SKIPPED,
            reason = "Logged wrong dose"
        )
        assertTrue(correctRes.isSuccess)
        assertEquals(ScaledQuantity.ofWhole(252), repository.observeLotsForMedication(medId).first().first { it.id == bottle1.id }.remainingQuantity)
        assertEquals(ScaledQuantity.ofWhole(552), repository.observeStockBalance(med).first().currentBalance)

        // 4. Discard opened bottle #1 -> writes WASTAGE ledger entry for 252 drops, remaining total = 300 drops (Bottle #2)
        val discardRes = repository.discardPackage(bottle1.id, "Opened bottle expired after 28 days")
        assertTrue(discardRes.isSuccess)
        val lotsAfterDiscard = repository.observeLotsForMedication(medId).first()
        assertEquals(PackageStatus.DISCARDED, lotsAfterDiscard.first { it.id == bottle1.id }.packageStatus)
        assertEquals(ScaledQuantity.ZERO, lotsAfterDiscard.first { it.id == bottle1.id }.remainingQuantity)
        assertEquals(ScaledQuantity.ofWhole(300), repository.observeStockBalance(med).first().currentBalance)

        val movements = repository.observeMovementsForMedication(medId).first()
        assertTrue(movements.any { it.movementType == MovementType.WASTAGE && it.quantity == ScaledQuantity.ofWhole(252) })
    }
}
