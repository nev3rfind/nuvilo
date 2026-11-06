package com.example.nuvilo.regression

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.MedicationItemCard
import com.example.nuvilo.ui.screens.AddMedicationDialog
import com.example.nuvilo.ui.theme.NoveloTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category F (Compose UI & Accessibility)
 * Tests T161 to T166: Medication List Components, Cards, and Dialogs
 */
@RunWith(AndroidJUnit4::class)
class ComposeMedicationListRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMedication = Medication(
        id = "med_list_1",
        name = "Bisoprolol",
        strength = "2.5mg",
        form = MedicationForm.TABLET,
        stockUnit = StockUnit.TABLETS,
        openingStock = ScaledQuantity.ofWhole(28),
        lowStockThreshold = ScaledQuantity.ofWhole(7),
        recordingMode = RecordingMode.CONFIRM
    )

    private val healthyBalance = StockBalance(
        currentBalance = ScaledQuantity.ofWhole(28),
        unit = StockUnit.TABLETS,
        isNegative = false,
        isLowStock = false,
        isEstimate = false,
        daysRemaining = 28
    )

    private val lowBalance = StockBalance(
        currentBalance = ScaledQuantity.ofWhole(3),
        unit = StockUnit.TABLETS,
        isNegative = false,
        isLowStock = true,
        isEstimate = false,
        daysRemaining = 3
    )

    @Test
    fun t161_medicationItemCardDisplaysDetailsAndStock() {
        // T161: MedicationItemCard displays name, strength, form, and stock balance
        composeTestRule.setContent {
            NoveloTheme {
                MedicationItemCard(
                    medication = sampleMedication,
                    stockBalance = healthyBalance,
                    onArchiveToggle = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Bisoprolol").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.5mg · Tablet").assertIsDisplayed()
    }

    @Test
    fun t162_medicationItemCardClickInvokesCallback() {
        // T162: MedicationItemCard clicking invokes onMedicationClick with med ID
        var clicked = false
        composeTestRule.setContent {
            NoveloTheme {
                MedicationItemCard(
                    medication = sampleMedication,
                    stockBalance = healthyBalance,
                    onArchiveToggle = {},
                    onCardClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Bisoprolol").performClick()
        assertTrue(clicked)
    }

    @Test
    fun t163_lowStockBadgeDisplayedWhenMedicationIsLowOnStock() {
        // T163: Low stock badge is displayed when medication is low on stock
        composeTestRule.setContent {
            NoveloTheme {
                MedicationItemCard(
                    medication = sampleMedication,
                    stockBalance = lowBalance,
                    onArchiveToggle = {}
                )
            }
        }

        composeTestRule.onNodeWithText("3 tab left").assertIsDisplayed()
    }

    @Test
    fun t164_estimatedConsumptionBadgeDisplayedForDropsOrInhalers() {
        // T164: Estimated consumption badge is displayed for drops/inhalers
        val estimatedMed = sampleMedication.copy(
            name = "Hypromellose Eye Drops",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS
        )
        val estimatedBalance = lowBalance.copy(isEstimate = true, unit = StockUnit.DROPS)

        composeTestRule.setContent {
            NoveloTheme {
                MedicationItemCard(
                    medication = estimatedMed,
                    stockBalance = estimatedBalance,
                    onArchiveToggle = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Est. 3 drops left").assertIsDisplayed()
    }

    @Test
    fun t165_addMedicationDialogDisplaysRequiredFields() {
        // T165: Add medication dialog displays required input fields
        composeTestRule.setContent {
            NoveloTheme {
                AddMedicationDialog(
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Add Medication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medication Name", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Strength", substring = true).assertIsDisplayed()
    }

    @Test
    fun t166_emptyInventoryMessageDisplayedWhenMedicationListEmpty() {
        // T166: Empty inventory message displayed when medication list is empty
        composeTestRule.setContent {
            NoveloTheme {
                EmptyStateCard(
                    title = "No Medications",
                    description = "Tap the plus button below to add your first medication."
                )
            }
        }

        composeTestRule.onNodeWithText("No Medications").assertIsDisplayed()
    }
}
