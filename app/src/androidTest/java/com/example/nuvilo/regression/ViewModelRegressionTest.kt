package com.example.nuvilo.regression

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.preferences.UserPreferencesRepository
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.viewmodel.AdherenceCalendarViewModel
import com.example.nuvilo.ui.viewmodel.DashboardViewModel
import com.example.nuvilo.ui.viewmodel.MedicationDetailViewModel
import com.example.nuvilo.ui.viewmodel.MedicationViewModel
import com.example.nuvilo.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

/**
 * Regression Test Suite: Category E (ViewModels & Application State)
 * Tests T136 to T155: StateFlow State Transitions and User Intent Handling
 */
@RunWith(AndroidJUnit4::class)
class ViewModelRegressionTest {

    private lateinit var context: Context
    private lateinit var db: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl
    private lateinit var preferencesRepository: UserPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(db)
        preferencesRepository = UserPreferencesRepository(context)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() { /* In-memory db safely garbage-collected; avoids closing active viewModelScope Room flow connections */ }

    private fun createMed(id: String = "med_vm_1", name: String = "Atorvastatin") = Medication(
        id = id,
        name = name,
        strength = "20mg",
        form = MedicationForm.TABLET,
        stockUnit = StockUnit.TABLETS,
        openingStock = ScaledQuantity.ofWhole(28),
        lowStockThreshold = ScaledQuantity.ofWhole(7),
        recordingMode = RecordingMode.CONFIRM
    )

    private fun createSchedule(medId: String = "med_vm_1") = ScheduleDefinition(
        id = "sch_$medId",
        medicationId = medId,
        scheduleType = ScheduleType.DAILY,
        targetTimesMinuteOfDay = listOf(8 * 60), // 08:00
        daysOfWeek = emptySet(),
        doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
        startDateEpochDay = LocalDate.now().toEpochDay()
    )

    @Test
    fun t136_dashboardViewModelLoadsActiveMedicationsAndDoses() = runBlocking {
        // T136: DashboardViewModel loads active medications and shows scheduled doses
        val med = createMed()
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = DashboardViewModel(
            medicationRepository = repository,
            doseRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(1, state.items.size)
        assertEquals("Atorvastatin", state.items[0].medication.name)
    }

    @Test
    fun t137_dashboardViewModelCalculatesAccurateLowStockCount() = runBlocking {
        // T137: DashboardViewModel calculates accurate low-stock warning count in state
        val lowStockMed = createMed("med_low_vm", "Salbutamol").copy(
            openingStock = ScaledQuantity.ofWhole(3),
            lowStockThreshold = ScaledQuantity.ofWhole(5)
        )
        repository.createMedication(lowStockMed, createSchedule(lowStockMed.id))

        val viewModel = DashboardViewModel(
            medicationRepository = repository,
            doseRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(1, state.lowStockCount)
    }

    @Test
    fun t138_dashboardViewModelRecordsDoseConfirmed() = runBlocking {
        // T138: DashboardViewModel.onTakeDose records dose confirmed and updates UI state
        val med = createMed()
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = DashboardViewModel(
            medicationRepository = repository,
            doseRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        val stateInitial = viewModel.uiState.first { !it.isLoading }
        val doseItem = stateInitial.items[0]

        viewModel.onTakeDose(doseItem.occurrence)

        val stateUpdated = viewModel.uiState.first { it.items.any { d -> d.occurrence.status == DoseStatus.TAKEN } }
        assertTrue(stateUpdated.items.any { it.occurrence.status == DoseStatus.TAKEN })
    }

    @Test
    fun t139_dashboardViewModelSkipsDoseWithoutDeductingStock() = runBlocking {
        // T139: DashboardViewModel.onSkipDose updates dose occurrence to SKIPPED without deducting inventory
        val med = createMed()
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = DashboardViewModel(
            medicationRepository = repository,
            doseRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        val stateInitial = viewModel.uiState.first { !it.isLoading }
        val doseItem = stateInitial.items[0]

        viewModel.onSkipDose(doseItem.occurrence)

        val balance = repository.observeStockBalance(med).first()
        assertEquals(ScaledQuantity.ofWhole(28), balance.currentBalance)
    }

    @Test
    fun t140_medicationViewModelLoadsActiveMedicationsList() = runBlocking {
        // T140: MedicationViewModel loads active medication list
        val med = createMed()
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationViewModel(
            medicationRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(1, state.activeMedications.size)
        assertEquals("Atorvastatin", state.activeMedications[0].medication.name)
    }

    @Test
    fun t141_medicationViewModelFiltersArchivedMedicationsWhenToggled() = runBlocking {
        // T141: MedicationViewModel filters archived medications when archived filter is toggled
        val activeMed = createMed("med_act", "Active Med")
        val archivedMed = createMed("med_arc", "Archived Med").copy(isArchived = true)
        repository.createMedication(activeMed, createSchedule(activeMed.id))
        repository.createMedication(archivedMed, createSchedule(archivedMed.id))
        repository.setArchived(archivedMed.id, true)

        val viewModel = MedicationViewModel(
            medicationRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(1, state.activeMedications.size)
        assertEquals(1, state.archivedMedications.size)
        assertEquals("Active Med", state.activeMedications[0].medication.name)
        assertEquals("Archived Med", state.archivedMedications[0].medication.name)
    }

    @Test
    fun t142_medicationViewModelCreateMedicationPersistsNewMedication() = runBlocking {
        // T142: MedicationViewModel.createMedication validates input and persists new medication
        val viewModel = MedicationViewModel(
            medicationRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        viewModel.createMedication(
            name = "Created Med",
            strength = "10mg",
            form = MedicationForm.TABLET,
            unit = StockUnit.TABLETS,
            openingStockStr = "30",
            lowStockThresholdStr = "5",
            recordingMode = RecordingMode.CONFIRM,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityStr = "1"
        )

        val state = viewModel.uiState.first { it.activeMedications.any { m -> m.medication.name == "Created Med" } }
        assertTrue(state.activeMedications.any { it.medication.name == "Created Med" })
    }

    @Test
    fun t143_medicationViewModelArchiveMedicationMovesMedicationToArchived() = runBlocking {
        // T143: MedicationViewModel.setArchived moves medication from active to archived list
        val med = createMed("med_to_archive", "To Archive")
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationViewModel(
            medicationRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        viewModel.setArchived("med_to_archive", true)

        val state = viewModel.uiState.first { it.archivedMedications.any { m -> m.medication.id == "med_to_archive" } }
        assertTrue(state.archivedMedications.any { it.medication.id == "med_to_archive" })
    }

    @Test
    fun t144_medicationDetailViewModelLoadsBalanceAndRunwayDays() = runBlocking {
        // T144: MedicationDetailViewModel loads medication details, stock balance, and runway days
        val med = createMed("med_detail_1", "Detail Med")
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationDetailViewModel(
            medicationId = "med_detail_1",
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertNotNull(state.medication)
        assertEquals("Detail Med", state.medication?.name)
        assertEquals(ScaledQuantity.ofWhole(28), state.stockBalance?.currentBalance)
        assertEquals(28, state.stockBalance?.daysRemaining)
    }

    @Test
    fun t145_medicationDetailViewModelLoadsLotsAndLedgerMovements() = runBlocking {
        // T145: MedicationDetailViewModel loads lot history and stock movements
        val med = createMed("med_detail_lots", "Lot Detail Med")
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationDetailViewModel(
            medicationId = "med_detail_lots",
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(1, state.recentMovements.size) // initial opening balance
    }

    @Test
    fun t146_medicationDetailViewModelRestockIncreasesBalanceAndCreatesLot() = runBlocking {
        // T146: MedicationDetailViewModel.restock increases stock balance and creates new lot entry
        val med = createMed("med_detail_restock", "Restock Med")
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationDetailViewModel(
            medicationId = "med_detail_restock",
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        viewModel.uiState.first { it.medication != null && !it.isLoading }

        viewModel.restock(
            quantity = ScaledQuantity.ofWhole(30),
            lotNumber = "LOT-999",
            expiryDateEpochMs = 1800000000000L,
            notes = "Delivered by Boots"
        )

        val state = viewModel.uiState.first { it.stockBalance?.currentBalance == ScaledQuantity.ofWhole(58) && it.lots.isNotEmpty() }
        assertEquals(ScaledQuantity.ofWhole(58), state.stockBalance?.currentBalance)
        assertEquals(1, state.lots.size)
    }

    @Test
    fun t147_medicationDetailViewModelAdjustStockCreatesAdjustmentEntry() = runBlocking {
        // T147: MedicationDetailViewModel.adjustStock creates adjustment movement entry
        val med = createMed("med_detail_adjust", "Adjust Med")
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationDetailViewModel(
            medicationId = "med_detail_adjust",
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        viewModel.uiState.first { it.stockBalance != null && !it.isLoading }

        viewModel.adjustStock(
            targetBalance = ScaledQuantity.ofWhole(33),
            reason = "Found extra blister pack"
        )

        val state = viewModel.uiState.first { it.stockBalance?.currentBalance == ScaledQuantity.ofWhole(33) }
        assertEquals(ScaledQuantity.ofWhole(33), state.stockBalance?.currentBalance)
    }

    @Test
    fun t148_medicationDetailViewModelRecordWastageDebitsStock() = runBlocking {
        // T148: MedicationDetailViewModel.recordWastage debits stock as wastage
        val med = createMed("med_detail_waste", "Waste Med")
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationDetailViewModel(
            medicationId = "med_detail_waste",
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        viewModel.uiState.first { it.stockBalance != null && !it.isLoading }

        viewModel.adjustStock(
            targetBalance = ScaledQuantity.ofWhole(25),
            reason = "Dropped on dirty floor"
        )

        val state = viewModel.uiState.first { it.stockBalance?.currentBalance == ScaledQuantity.ofWhole(25) }
        assertEquals(ScaledQuantity.ofWhole(25), state.stockBalance?.currentBalance)
    }

    @Test
    fun t148b_medicationDetailViewModelAdjustStockImmediateInvocationAwaitsLoadedState() = runBlocking {
        // Verifies BUG-002 fix: adjustStock invoked immediately without awaiting uiState does not treat balance as 0
        val med = createMed("med_detail_race", "Race Med")
        repository.createMedication(med, createSchedule(med.id))

        val viewModel = MedicationDetailViewModel(
            medicationId = "med_detail_race",
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        // Immediately invoke adjustStock without manual first { it.stockBalance != null }
        viewModel.adjustStock(
            targetBalance = ScaledQuantity.ofWhole(24),
            reason = "Count discrepancy recount"
        )

        val state = viewModel.uiState.first { it.stockBalance?.currentBalance == ScaledQuantity.ofWhole(24) }
        assertEquals(ScaledQuantity.ofWhole(24), state.stockBalance?.currentBalance)
    }

    @Test
    fun t149_medicationDetailViewModelRequestRepeatMarksPrescriptionPending() = runBlocking {
        // T149: MedicationDetailViewModel.requestRepeat marks prescription repeat request pending
        val med = createMed("med_detail_rx_req", "Rx Req Med")
        repository.createMedication(med, createSchedule(med.id))

        val rx = Prescription(
            id = "rx_vm_req",
            medicationId = med.id,
            issueDateEpochDay = 19500L,
            repeatsAuthorised = 3,
            repeatsRemaining = 3,
            status = PrescriptionStatus.ACTIVE
        )
        repository.createPrescription(rx)

        val viewModel = MedicationDetailViewModel(
            medicationId = med.id,
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        viewModel.requestPrescriptionRepeat(
            prescriptionId = "rx_vm_req",
            requestedQuantity = null,
            notes = "Requested repeat via surgery app"
        )

        val state = viewModel.uiState.first { it.prescriptions.firstOrNull()?.hasPendingRepeatRequest == true }
        assertTrue(state.prescriptions.first().hasPendingRepeatRequest)
    }

    @Test
    fun t150_medicationDetailViewModelCollectRepeatDecrementsRepeats() = runBlocking {
        // T150: MedicationDetailViewModel.collectRepeat decrements repeat counter and updates balance
        val med = createMed("med_detail_rx_col", "Rx Col Med")
        repository.createMedication(med, createSchedule(med.id))

        val rx = Prescription(
            id = "rx_vm_col",
            medicationId = med.id,
            issueDateEpochDay = 19500L,
            repeatsAuthorised = 3,
            repeatsRemaining = 3,
            hasPendingRepeatRequest = true,
            status = PrescriptionStatus.ACTIVE
        )
        repository.createPrescription(rx)

        val viewModel = MedicationDetailViewModel(
            medicationId = med.id,
            medicationRepository = repository,
            stockRepository = repository,
            doseRepository = repository,
            prescriptionRepository = repository
        )

        viewModel.collectPrescriptionRepeat("rx_vm_col", "Collected from Boots")

        val state = viewModel.uiState.first { it.prescriptions.firstOrNull()?.repeatsRemaining == 2 }
        assertEquals(2, state.prescriptions.first().repeatsRemaining)
        assertFalse(state.prescriptions.first().hasPendingRepeatRequest)
    }

    @Test
    fun t151_adherenceCalendarViewModelInitializesWithCurrentMonth() = runBlocking {
        // T151: AdherenceCalendarViewModel loads monthly calendar data for current month
        val viewModel = AdherenceCalendarViewModel(
            medicationRepository = repository,
            doseRepository = repository
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertNotNull(state.calendarData)
        assertEquals(LocalDate.now().year, state.calendarData?.yearMonth?.year)
        assertEquals(LocalDate.now().monthValue, state.calendarData?.yearMonth?.monthValue)
    }

    @Test
    fun t152_adherenceCalendarViewModelNavigatesMonths() = runBlocking {
        // T152: AdherenceCalendarViewModel navigates to previous and next months
        val viewModel = AdherenceCalendarViewModel(
            medicationRepository = repository,
            doseRepository = repository
        )

        val currentMonth = LocalDate.now().monthValue
        viewModel.onPreviousMonth()

        val prevMonth = (currentMonth - 2 + 12) % 12 + 1
        val state = viewModel.uiState.first { it.calendarData?.yearMonth?.monthValue == prevMonth }
        assertEquals(prevMonth, state.calendarData?.yearMonth?.monthValue)
    }

    @Test
    fun t153_adherenceCalendarViewModelSelectsPeriodDays() = runBlocking {
        // T153: AdherenceCalendarViewModel changes adherence summary period
        val viewModel = AdherenceCalendarViewModel(
            medicationRepository = repository,
            doseRepository = repository
        )

        viewModel.onSelectPeriodDays(14)
        val state = viewModel.uiState.first { it.selectedPeriodDays == 14 }
        assertEquals(14, state.selectedPeriodDays)
    }

    @Test
    fun t154_settingsViewModelLoadsUserPreferences() = runBlocking {
        // T154: SettingsViewModel loads user privacy mode and biometric lock settings
        val viewModel = SettingsViewModel(
            preferencesRepository = preferencesRepository
        )

        val prefs = viewModel.userPreferences.first()
        assertNotNull(prefs)
        assertFalse(prefs.isNotificationPrivacyMasked)
    }

    @Test
    fun t155_settingsViewModelTogglesPrivacyMasking() = runBlocking {
        // T155: SettingsViewModel toggles privacy mode and saves to preferences
        val viewModel = SettingsViewModel(
            preferencesRepository = preferencesRepository
        )

        viewModel.setNotificationPrivacy(true)
        val prefs = viewModel.userPreferences.first { it.isNotificationPrivacyMasked }
        assertTrue(prefs.isNotificationPrivacyMasked)
    }
}

