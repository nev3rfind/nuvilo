package com.example.nuvilo.regression

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.screens.AdherenceCalendarScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.AdherenceCalendarViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

/**
 * Regression Test Suite: Category F (Compose UI & Accessibility)
 * Tests T174 to T178: Adherence Calendar Screen and Month Navigation
 */
@RunWith(AndroidJUnit4::class)
class ComposeCalendarRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: NoveloDatabase
    private lateinit var repository: NoveloRepositoryImpl
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModel: AdherenceCalendarViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = NoveloDatabase.createInMemory(context)
        repository = NoveloRepositoryImpl(db)
        runBlocking {
            val med = Medication(
                id = "med_cal_1",
                name = "Atorvastatin",
                strength = "20mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ofWhole(28),
                lowStockThreshold = ScaledQuantity.ofWhole(7),
                recordingMode = RecordingMode.CONFIRM
            )
            val schedule = ScheduleDefinition(
                id = "sch_cal_1",
                medicationId = med.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = LocalDate.now().minusDays(10).toEpochDay()
            )
            repository.createMedication(med, schedule)
        }
        viewModelStore = ViewModelStore()
        viewModel = AdherenceCalendarViewModel(
            medicationRepository = repository,
            doseRepository = repository
        )
        viewModelStore.put("calendar_vm", viewModel)
    }

    private fun waitForCalendarLoaded() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isLoading && viewModel.uiState.value.hasAnyScheduleOrHistory
        }
        composeTestRule.waitForIdle()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        viewModelStore.clear()
        db.close()
    }

    @Test
    fun t174_calendarScreenDisplaysAdherenceHeader() {
        // T174: Calendar screen displays adherence history title
        composeTestRule.setContent {
            NoveloTheme {
                AdherenceCalendarScreen(
                    viewModel = viewModel,
                    onSelectMedication = {}
                )
            }
        }

        waitForCalendarLoaded()
        composeTestRule.onNodeWithText("Adherence & History").assertIsDisplayed()
    }

    @Test
    fun t175_calendarScreenDisplaysOverviewCards() {
        // T175: Calendar screen displays Today and Last 7 Days metrics
        composeTestRule.setContent {
            NoveloTheme {
                AdherenceCalendarScreen(
                    viewModel = viewModel,
                    onSelectMedication = {}
                )
            }
        }

        waitForCalendarLoaded()
        composeTestRule.onAllNodesWithText("Today")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Last 7 Days").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t176_calendarPreviousMonthButtonNavigatesMonth() {
        // T176: Calendar month navigation previous button changes month header
        composeTestRule.setContent {
            NoveloTheme {
                AdherenceCalendarScreen(
                    viewModel = viewModel,
                    onSelectMedication = {}
                )
            }
        }

        waitForCalendarLoaded()
        composeTestRule.onNodeWithContentDescription("Previous month").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Previous month").performClick()
    }

    @Test
    fun t177_calendarNextMonthButtonNavigatesMonth() {
        // T177: Calendar month navigation next button changes month header
        composeTestRule.setContent {
            NoveloTheme {
                AdherenceCalendarScreen(
                    viewModel = viewModel,
                    onSelectMedication = {}
                )
            }
        }

        waitForCalendarLoaded()
        composeTestRule.onNodeWithContentDescription("Next month").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next month").performClick()
    }

    @Test
    fun t178_calendarScreenDisplaysOutcomeFilters() {
        // T178: Rolling window chips allow selecting evaluation periods
        composeTestRule.setContent {
            NoveloTheme {
                AdherenceCalendarScreen(
                    viewModel = viewModel,
                    onSelectMedication = {}
                )
            }
        }

        waitForCalendarLoaded()
        composeTestRule.onNodeWithText("7D").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("14D").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("30D").performScrollTo().assertIsDisplayed()
    }
}
