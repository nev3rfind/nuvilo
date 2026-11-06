package com.example.nuvilo.regression

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.screens.AddMedicationDialog
import com.example.nuvilo.ui.screens.AdherenceCalendarScreen
import com.example.nuvilo.ui.screens.DashboardScreen
import com.example.nuvilo.ui.screens.MedicationDetailScreen
import com.example.nuvilo.ui.screens.MedicationListScreen
import com.example.nuvilo.ui.screens.SettingsScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.AdherenceCalendarViewModel
import com.example.nuvilo.ui.viewmodel.DashboardViewModel
import com.example.nuvilo.ui.viewmodel.MedicationDetailViewModel
import com.example.nuvilo.ui.viewmodel.MedicationViewModel
import com.example.nuvilo.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Regression Test Suite: Category G (Full Application / End-to-End User Journeys)
 * Tests T191 to T200: Complete Cross-Screen User Journeys
 */
@RunWith(AndroidJUnit4::class)
class FullApplicationJourneyRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var app: NoveloApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        app = context.applicationContext as NoveloApplication
    }

    private fun sampleMedication(id: String = "journey_med_1") = Medication(
        id = id,
        name = "Metformin",
        strength = "500mg",
        form = MedicationForm.TABLET,
        stockUnit = StockUnit.TABLETS,
        openingStock = ScaledQuantity.ofWhole(60),
        lowStockThreshold = ScaledQuantity.ofWhole(14),
        recordingMode = RecordingMode.CONFIRM
    )

    private fun sampleSchedule(medId: String = "journey_med_1") = ScheduleDefinition(
        id = "sch_$medId",
        medicationId = medId,
        scheduleType = ScheduleType.DAILY,
        targetTimesMinuteOfDay = listOf(8 * 60, 20 * 60),
        daysOfWeek = emptySet(),
        doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
        startDateEpochDay = LocalDate.now().toEpochDay()
    )

    @Test
    fun t191_journeyDashboardDisplaysActiveScheduleSummary() {
        runBlocking {
            // T191: Complete User Journey: Launch app and display Dashboard
            val container = app.container
            val med = sampleMedication("med_j191")
            container.repository.createMedication(med, sampleSchedule(med.id))

            val dashboardVm = DashboardViewModel(
                medicationRepository = container.repository,
                doseRepository = container.repository,
                stockRepository = container.repository,
                prescriptionRepository = container.repository
            )

            composeTestRule.setContent {
                NoveloTheme {
                    DashboardScreen(
                        viewModel = dashboardVm,
                        onSelectMedication = {},
                        onNavigateToMedications = {}
                    )
                }
            }

            composeTestRule.onNodeWithText("Offline Protected").assertIsDisplayed()
            composeTestRule.onNodeWithText("Today's Adherence Progress").assertIsDisplayed()
        }
    }

    @Test
    fun t192_journeyNavigateToMedicationListShowsInventory() {
        runBlocking {
            // T192: Journey: Navigate from Dashboard to Medication List
            val container = app.container
            val med = sampleMedication("med_j192")
            container.repository.createMedication(med, sampleSchedule(med.id))

            val medicationVm = MedicationViewModel(
                medicationRepository = container.repository,
                stockRepository = container.repository,
                prescriptionRepository = container.repository
            )

            composeTestRule.setContent {
                NoveloTheme {
                    MedicationListScreen(
                        viewModel = medicationVm,
                        onSelectMedication = {}
                    )
                }
            }

            composeTestRule.onNodeWithText("Medications").assertIsDisplayed()
            composeTestRule.onNodeWithText("Metformin", substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun t193_journeyOpenAddMedicationDialogAndDismiss() {
        // T193: Journey: Open Add Medication Dialog and cancel
        var dismissed = false
        composeTestRule.setContent {
            NoveloTheme {
                AddMedicationDialog(
                    onDismiss = { dismissed = true },
                    onConfirm = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Add Medication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun t194_journeyNavigateToCalendarDisplaysMonthlyAdherence() {
        // T194: Journey: Navigate from Dashboard to Calendar Screen
        val container = app.container
        val calendarVm = AdherenceCalendarViewModel(
            medicationRepository = container.repository,
            doseRepository = container.repository
        )

        composeTestRule.setContent {
            NoveloTheme {
                AdherenceCalendarScreen(
                    viewModel = calendarVm,
                    onSelectMedication = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Adherence & History").assertIsDisplayed()
    }

    @Test
    fun t195_journeyNavigateToSettingsDisplaysConfiguration() {
        // T195: Journey: Navigate from Dashboard to Settings Screen
        val container = app.container
        val settingsVm = SettingsViewModel(container.userPreferencesRepository)

        composeTestRule.setContent {
            NoveloTheme {
                SettingsScreen(viewModel = settingsVm)
            }
        }

        composeTestRule.onNodeWithText("Settings & Privacy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appearance & Theme").assertIsDisplayed()
    }

    @Test
    fun t196_journeySettingsThemeSelectionExecutesCleanly() {
        // T196: Journey: Settings screen toggles theme without crash
        val container = app.container
        val settingsVm = SettingsViewModel(container.userPreferencesRepository)

        composeTestRule.setContent {
            NoveloTheme {
                SettingsScreen(viewModel = settingsVm)
            }
        }

        composeTestRule.onNodeWithText("Light").performClick()
    }

    @Test
    fun t197_journeySettingsPrivacyMaskingToggleUpdatesPreferences() {
        runBlocking {
            // T197: Journey: Settings screen toggles notification privacy mask
            val container = app.container
            container.userPreferencesRepository.setNotificationPrivacy(true)

            val prefs = container.userPreferencesRepository.userPreferencesFlow.first { it.isNotificationPrivacyMasked }
            assertTrue(prefs.isNotificationPrivacyMasked)
        }
    }

    @Test
    fun t198_journeyMedicationDetailDisplaysRunwayAndHistory() {
        runBlocking {
            // T198: Journey: Complete Medication Creation and Detail Inspection
            val container = app.container
            val med = sampleMedication("med_j198")
            container.repository.createMedication(med, sampleSchedule(med.id))

            val detailVm = MedicationDetailViewModel(
                medicationId = med.id,
                medicationRepository = container.repository,
                stockRepository = container.repository,
                doseRepository = container.repository,
                prescriptionRepository = container.repository
            )

            composeTestRule.setContent {
                NoveloTheme {
                    MedicationDetailScreen(
                        viewModel = detailVm,
                        onNavigateBack = {}
                    )
                }
            }

            composeTestRule.onAllNodesWithText("Metformin", substring = true)[0].assertIsDisplayed()
            composeTestRule.onNodeWithText("Restock Supply").assertIsDisplayed()
        }
    }

    @Test
    fun t199_journeyStockRestockFlowIncreasesInventory() {
        runBlocking {
            // T199: Journey: Stock Restock Flow on Medication Detail Screen
            val container = app.container
            val med = sampleMedication("med_j199")
            container.repository.createMedication(med, sampleSchedule(med.id))

            container.repository.restockWithLot(
                medicationId = med.id,
                quantity = ScaledQuantity.ofWhole(30),
                unit = StockUnit.TABLETS,
                lotNumber = "LOT-REFILL-2026",
                expiryDateEpochMs = 1800000000000L,
                notes = "NHS prescription dispense"
            )

            val balance = container.repository.observeStockBalance(med).first { it.currentBalance == ScaledQuantity.ofWhole(90) }
            assertEquals(ScaledQuantity.ofWhole(90), balance.currentBalance)
        }
    }

    @Test
    fun t200_journeyDoseRecordingAndStockLedgerVerificationEndToEnd() {
        runBlocking {
            // T200: Journey: Dose Recording and Stock Ledger Verification End-to-End
            val container = app.container
            val med = sampleMedication("med_j200")
            container.repository.createMedication(med, sampleSchedule(med.id))

            // Record dose taken
            container.repository.recordDose(
                occurrenceId = "occ_j200_taken",
                medicationId = med.id,
                scheduledEpochMs = System.currentTimeMillis(),
                quantity = ScaledQuantity.ofWhole(2),
                unit = StockUnit.TABLETS,
                status = DoseStatus.TAKEN
            )

            // Verify stock deducted
            val balance = container.repository.observeStockBalance(med).first()
            assertEquals(ScaledQuantity.ofWhole(58), balance.currentBalance)

            // Verify ledger movement
            val movements = container.repository.observeMovementsForMedication(med.id).first()
            assertTrue(movements.any { it.movementType == MovementType.DOSE_TAKEN && it.quantity == ScaledQuantity.ofWhole(2) })
        }
    }
}
