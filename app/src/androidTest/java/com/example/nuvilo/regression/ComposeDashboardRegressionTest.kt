package com.example.nuvilo.regression

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.components.CompletedDayCard
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.HeroDoseCard
import com.example.nuvilo.ui.theme.NoveloTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category F (Compose UI & Accessibility)
 * Tests T156 to T160: Dashboard Compose Components and User Interactions
 */
@RunWith(AndroidJUnit4::class)
class ComposeDashboardRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMedication = Medication(
        id = "med_dash_1",
        name = "Atorvastatin",
        strength = "20mg",
        form = MedicationForm.TABLET,
        stockUnit = StockUnit.TABLETS,
        openingStock = ScaledQuantity.ofWhole(28),
        lowStockThreshold = ScaledQuantity.ofWhole(7),
        recordingMode = RecordingMode.CONFIRM
    )

    private val sampleOccurrence = DoseOccurrence(
        occurrenceId = "occ_dash_1",
        medicationId = "med_dash_1",
        scheduledEpochMs = 1700000000000L,
        doseQuantity = ScaledQuantity.ofWhole(1),
        unit = StockUnit.TABLETS,
        status = DoseStatus.PENDING
    )

    private val sampleBalance = StockBalance(
        currentBalance = ScaledQuantity.ofWhole(28),
        unit = StockUnit.TABLETS,
        isNegative = false,
        isLowStock = false,
        isEstimate = false,
        daysRemaining = 28
    )

    @Test
    fun t156_heroDoseCardDisplaysMedicationDetailsAndDosage() {
        // T156: HeroDoseCard displays medication name, dosage, and scheduled time
        composeTestRule.setContent {
            NoveloTheme {
                HeroDoseCard(
                    occurrence = sampleOccurrence,
                    medication = sampleMedication,
                    stockBalance = sampleBalance
                )
            }
        }

        composeTestRule.onNodeWithText("Atorvastatin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take 1 tablet", substring = true).assertIsDisplayed()
    }

    @Test
    fun t157_heroDoseCardTakeButtonTriggersCallback() {
        // T157: HeroDoseCard "Take Dose" button triggers onTake callback
        var takeClicked = false
        composeTestRule.setContent {
            NoveloTheme {
                HeroDoseCard(
                    occurrence = sampleOccurrence,
                    medication = sampleMedication,
                    stockBalance = sampleBalance,
                    onTake = { takeClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Confirm Dose Taken", substring = true).performClick()
        assertTrue(takeClicked)
    }

    @Test
    fun t158_heroDoseCardSkipButtonTriggersCallback() {
        // T158: HeroDoseCard "Skip" button triggers onSkip callback
        var skipClicked = false
        composeTestRule.setContent {
            NoveloTheme {
                HeroDoseCard(
                    occurrence = sampleOccurrence,
                    medication = sampleMedication,
                    stockBalance = sampleBalance,
                    onSkip = { skipClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Skip Dose").performClick()
        assertTrue(skipClicked)
    }

    @Test
    fun t159_completedDayCardDisplaysAllDosesTakenConfirmation() {
        // T159: CompletedDayCard displays all doses taken confirmation
        composeTestRule.setContent {
            NoveloTheme {
                CompletedDayCard(
                    takenDoses = 3,
                    totalDoses = 3
                )
            }
        }

        composeTestRule.onNodeWithText("All Caught Up for Today").assertIsDisplayed()
    }

    @Test
    fun t160_emptyStateCardDisplaysWhenNoDosesAreScheduled() {
        // T160: EmptyStateCard displays when no doses are scheduled
        composeTestRule.setContent {
            NoveloTheme {
                EmptyStateCard(
                    title = "No doses scheduled",
                    description = "You have no upcoming doses for today"
                )
            }
        }

        composeTestRule.onNodeWithText("No doses scheduled").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have no upcoming doses for today").assertIsDisplayed()
    }
}
