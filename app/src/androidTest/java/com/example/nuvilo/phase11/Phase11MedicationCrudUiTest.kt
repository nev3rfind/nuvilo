package com.example.nuvilo.phase11

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.screens.MedicationDetailScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.MedicationDetailViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11MedicationCrudUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun medicationDetail_editOpensDialogAndSafeDeleteWithHistoryOffersArchiveInstead() = runBlocking {
        val med = Medication(
            id = "med_ui_crud",
            name = "Metformin",
            strength = "500 mg",
            form = MedicationForm.TABLET,
            stockUnit = StockUnit.TABLETS,
            openingStock = ScaledQuantity.ofWhole(28),
            lowStockThreshold = ScaledQuantity.ofWhole(7)
        )
        val schedule = ScheduleDefinition(
            id = "sch_ui_crud",
            medicationId = "med_ui_crud",
            scheduleType = ScheduleType.DAILY,
            targetTimesMinuteOfDay = listOf(8 * 60),
            daysOfWeek = emptySet(),
            doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
            startDateEpochDay = 20100L
        )
        repository.createMedication(med, schedule)
        repository.recordDose(
            occurrenceId = "occ_ui_crud_1",
            medicationId = "med_ui_crud",
            scheduledEpochMs = 1700000000000L,
            quantity = ScaledQuantity.ofWhole(1),
            unit = StockUnit.TABLETS,
            status = DoseStatus.TAKEN
        )

        val viewModel = MedicationDetailViewModel(
            medicationId = "med_ui_crud",
            medicationRepository = repository,
            doseRepository = repository,
            stockRepository = repository,
            prescriptionRepository = repository
        )

        composeTestRule.setContent {
            NoveloTheme(darkTheme = false) {
                MedicationDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isLoading && viewModel.uiState.value.medication != null
        }
        composeTestRule.waitForIdle()

        // 1. Open Edit Medication Dialog and close
        composeTestRule.onNodeWithContentDescription("Edit medication details").performClick()
        composeTestRule.onNodeWithText("Edit Medication Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()

        // 2. Open Safe Delete Consent Dialog (because medication has dose + stock history)
        composeTestRule.onNodeWithContentDescription("Delete medication").performClick()
        composeTestRule.onNodeWithText("Delete Metformin?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Archive Instead (Preserve History)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Permanently").assertIsDisplayed()

        // 3. Choose Archive Instead -> preserves medication in archived state
        composeTestRule.onNodeWithText("Archive Instead (Preserve History)").performClick()
        composeTestRule.waitUntil(5_000) {
            runBlocking { repository.getMedication("med_ui_crud")?.isArchived == true }
        }

        val archivedMed = repository.getMedication("med_ui_crud")
        assertTrue(archivedMed?.isArchived == true)
    }
}
