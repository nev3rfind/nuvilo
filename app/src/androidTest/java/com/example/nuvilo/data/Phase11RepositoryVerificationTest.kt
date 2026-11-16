package com.example.nuvilo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.AdministrationRoute
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationDeleteOutcome
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.ppc.PpcCertificate
import com.example.nuvilo.domain.ppc.PpcCoverageType
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
import java.io.IOException
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class Phase11RepositoryVerificationTest {

    private lateinit var db: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(db)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun deleteMedicationSafely_withoutHistory_deletesAndSupportsUndo() = runBlocking {
        val med = Medication(
            id = "med_no_hist",
            name = "Vitamin D3",
            strength = "1000 IU",
            form = MedicationForm.CAPSULE,
            administrationRoute = AdministrationRoute.ORAL,
            stockUnit = StockUnit.CAPSULES,
            openingStock = ScaledQuantity.ZERO,
            lowStockThreshold = ScaledQuantity.ofWhole(5),
            notificationsEnabled = true
        )
        val schedule = ScheduleDefinition(
            id = "sch_no_hist",
            medicationId = "med_no_hist",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(9 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20100L
        )
        assertTrue(repository.createMedication(med, schedule).isSuccess)

        val outcome = repository.deleteMedicationSafely("med_no_hist", confirmCascadeWithHistory = false).getOrThrow()
        assertTrue(outcome is MedicationDeleteOutcome.Deleted)
        val deleted = outcome as MedicationDeleteOutcome.Deleted
        assertTrue(deleted.canUndo)
        assertNotNull(deleted.snapshot)
        assertNull(repository.getMedication("med_no_hist"))

        // Undo restore
        assertTrue(repository.undoDeleteMedication(deleted.snapshot!!).isSuccess)
        val restored = repository.getMedication("med_no_hist")
        assertNotNull(restored)
        assertEquals("Vitamin D3", restored?.name)
        assertEquals(AdministrationRoute.ORAL, restored?.administrationRoute)
    }

    @Test
    fun deleteMedicationSafely_withHistory_requiresExplicitConsent_thenCascadesOnlyWhenConfirmed() = runBlocking {
        val med = Medication(
            id = "med_with_hist",
            name = "Metformin",
            strength = "500 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(28),
            lowStockThreshold = ScaledQuantity.ofWhole(7)
        )
        val schedule = ScheduleDefinition(
            id = "sch_with_hist",
            medicationId = "med_with_hist",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20100L
        )
        assertTrue(repository.createMedication(med, schedule).isSuccess)
        assertTrue(
            repository.recordDose(
                occurrenceId = "occ_hist_1",
                medicationId = "med_with_hist",
                scheduledEpochMs = 1700000000000L,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN
            ).isSuccess
        )
        assertTrue(
            repository.createPrescription(
                Prescription(
                    id = "rx_hist_1",
                    medicationId = "med_with_hist",
                    referenceNumber = "RX-900",
                    issueDateEpochDay = 20100L,
                    expiryDateEpochDay = 20280L,
                    repeatsAuthorised = 6,
                    repeatsRemaining = 5,
                    status = PrescriptionStatus.ACTIVE
                )
            ).isSuccess
        )

        // First attempt without consent must NOT delete
        val consentOutcome = repository.deleteMedicationSafely("med_with_hist", confirmCascadeWithHistory = false).getOrThrow()
        assertTrue(consentOutcome is MedicationDeleteOutcome.RequiresExplicitConsent)
        val consent = consentOutcome as MedicationDeleteOutcome.RequiresExplicitConsent
        assertTrue(consent.counts.hasAnyHistory)
        assertEquals(1, consent.counts.doseEventCount)
        assertTrue(consent.counts.stockMovementCount >= 2)
        assertEquals(1, consent.counts.prescriptionCount)
        assertNotNull(repository.getMedication("med_with_hist"))

        // Confirmed cascade delete
        val deletedOutcome = repository.deleteMedicationSafely("med_with_hist", confirmCascadeWithHistory = true).getOrThrow()
        assertTrue(deletedOutcome is MedicationDeleteOutcome.Deleted)
        assertFalse((deletedOutcome as MedicationDeleteOutcome.Deleted).canUndo)
        assertNull(repository.getMedication("med_with_hist"))
    }

    @Test
    fun archiveAndRestoreMedication_preservesHistory() = runBlocking {
        val med = Medication(
            id = "med_arch",
            name = "Ramipril",
            strength = "5 mg",
            form = MedicationForm.CAPSULE,
            stockUnit = StockUnit.CAPSULES,
            openingStock = ScaledQuantity.ofWhole(28),
            lowStockThreshold = ScaledQuantity.ofWhole(7)
        )
        val schedule = ScheduleDefinition(
            id = "sch_arch",
            medicationId = "med_arch",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20100L
        )
        repository.createMedication(med, schedule)

        repository.setArchived("med_arch", true)
        val activeIds = repository.observeActiveMedications().first().map { it.id }
        val allIds = repository.observeAllMedications().first().map { it.id }
        assertFalse(activeIds.contains("med_arch"))
        assertTrue(allIds.contains("med_arch"))

        repository.setArchived("med_arch", false)
        val restoredActiveIds = repository.observeActiveMedications().first().map { it.id }
        assertTrue(restoredActiveIds.contains("med_arch"))
    }

    @Test
    fun ppcCertificateLifecycle_saveRenewArchiveAndDelete() = runBlocking {
        val initial = PpcCertificate(
            id = "ppc_old",
            coverageType = PpcCoverageType.PPC_3_MONTH,
            startDate = LocalDate.of(2026, 1, 1),
            expiryDate = LocalDate.of(2026, 3, 31),
            referenceNumber = "PPC-111"
        )
        assertTrue(repository.savePpcCertificate(initial).isSuccess)

        val renewed = PpcCertificate(
            id = "ppc_new",
            coverageType = PpcCoverageType.PPC_12_MONTH,
            startDate = LocalDate.of(2026, 4, 1),
            expiryDate = LocalDate.of(2027, 3, 31),
            referenceNumber = "PPC-222",
            renewedFromId = "ppc_old"
        )
        assertTrue(repository.renewPpcCertificate("ppc_old", renewed, archivePrevious = true).isSuccess)

        val active = repository.observeActivePpcCertificates().first()
        assertEquals(1, active.size)
        assertEquals("ppc_new", active.first().id)

        val old = repository.getPpcCertificateById("ppc_old")
        assertTrue(old?.isArchived == true)

        assertTrue(repository.deletePpcCertificate("ppc_old").isSuccess)
        assertNull(repository.getPpcCertificateById("ppc_old"))
    }

    @Test
    fun prescriptionDates_persistRepeatRequestedAndDispensedDates() = runBlocking {
        val med = Medication(
            id = "med_rx_dates",
            name = "Sertraline",
            strength = "50 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(28),
            lowStockThreshold = ScaledQuantity.ofWhole(7)
        )
        val schedule = ScheduleDefinition(
            id = "sch_rx_dates",
            medicationId = "med_rx_dates",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(9 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20100L
        )
        repository.createMedication(med, schedule)

        val rx = Prescription(
            id = "rx_dates_1",
            medicationId = "med_rx_dates",
            referenceNumber = "NHS-2026-42",
            pharmacyName = "Lloyds",
            issueDateEpochDay = 20500L,
            expiryDateEpochDay = 20680L,
            repeatRequestedDateEpochDay = 20525L,
            dispensedDateEpochDay = 20527L,
            repeatsAuthorised = 6,
            repeatsRemaining = 5,
            status = PrescriptionStatus.ACTIVE
        )
        assertTrue(repository.createPrescription(rx).isSuccess)

        val loaded = repository.observePrescriptionsForMedication("med_rx_dates").first().firstOrNull()
        assertNotNull(loaded)
        assertEquals(20525L, loaded?.repeatRequestedDateEpochDay)
        assertEquals(20527L, loaded?.dispensedDateEpochDay)
    }
}
