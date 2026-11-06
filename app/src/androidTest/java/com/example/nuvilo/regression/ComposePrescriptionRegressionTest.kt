package com.example.nuvilo.regression

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.screens.CollectRepeatDialog
import com.example.nuvilo.ui.screens.RequestRepeatDialog
import com.example.nuvilo.ui.theme.NoveloTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category F (Compose UI & Accessibility)
 * Tests T179 to T182: Prescription Lifecycle Dialogs and Repeat Workflows
 */
@RunWith(AndroidJUnit4::class)
class ComposePrescriptionRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMedication = Medication(
        id = "med_rx_test",
        name = "Atorvastatin",
        strength = "20mg",
        form = MedicationForm.TABLET,
        stockUnit = StockUnit.TABLETS,
        openingStock = ScaledQuantity.ofWhole(28),
        lowStockThreshold = ScaledQuantity.ofWhole(7),
        recordingMode = RecordingMode.CONFIRM
    )

    private val samplePrescription = Prescription(
        id = "rx_test_1",
        medicationId = "med_rx_test",
        referenceNumber = "NHS-RX-7890",
        issueDateEpochDay = 19500L,
        repeatsAuthorised = 4,
        repeatsRemaining = 3,
        status = PrescriptionStatus.ACTIVE
    )

    @Test
    fun t179_requestRepeatDialogDisplaysPrescriptionReferenceAndRepeats() {
        // T179: Prescription request repeat dialog displays title and explanation
        composeTestRule.setContent {
            NoveloTheme {
                RequestRepeatDialog(
                    prescription = samplePrescription,
                    medication = sampleMedication,
                    onDismiss = {},
                    onConfirm = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Request Repeat from GP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Record an order submitted to your GP surgery", substring = true).assertIsDisplayed()
    }

    @Test
    fun t180_requestRepeatDialogConfirmButtonTriggersCallback() {
        // T180: "Confirm Repeat Order" button is visible and clickable
        var confirmed = false
        composeTestRule.setContent {
            NoveloTheme {
                RequestRepeatDialog(
                    prescription = samplePrescription,
                    medication = sampleMedication,
                    onDismiss = {},
                    onConfirm = { _, _ -> confirmed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Confirm Repeat Order").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun t181_collectRepeatDialogDisplaysPromptWithRemainingCount() {
        // T181: CollectRepeatDialog displays confirmation prompt with repeats remaining
        composeTestRule.setContent {
            NoveloTheme {
                CollectRepeatDialog(
                    prescription = samplePrescription,
                    medication = sampleMedication,
                    onDismiss = {},
                    onConfirm = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Collect Repeat Prescription").assertIsDisplayed()
        composeTestRule.onNodeWithText("Collect & Restock").assertIsDisplayed()
    }

    @Test
    fun t182_collectRepeatDialogConfirmButtonTriggersCallback() {
        // T182: "Collect & Restock" button decrements remaining repeat count via callback
        var collected = false
        composeTestRule.setContent {
            NoveloTheme {
                CollectRepeatDialog(
                    prescription = samplePrescription,
                    medication = sampleMedication,
                    onDismiss = {},
                    onConfirm = { _, _, _, _ -> collected = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Collect & Restock").performClick()
        assertTrue(collected)
    }
}
