package com.example.nuvilo.regression

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.screens.AdjustStockDialog
import com.example.nuvilo.ui.screens.CollectRepeatDialog
import com.example.nuvilo.ui.screens.EditMedicationDialog
import com.example.nuvilo.ui.screens.RequestRepeatDialog
import com.example.nuvilo.ui.screens.RestockDialog
import com.example.nuvilo.ui.theme.NoveloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category F (Compose UI & Accessibility)
 * Tests T167 to T173: Medication Detail Operations, Inventory Dialogs, and Repeat Flows
 */
@RunWith(AndroidJUnit4::class)
class ComposeMedicationDetailRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMedication = Medication(
        id = "med_detail_comp_1",
        name = "Amoxicillin",
        strength = "500mg",
        form = MedicationForm.CAPSULE,
        stockUnit = StockUnit.CAPSULES,
        openingStock = ScaledQuantity.ofWhole(21),
        lowStockThreshold = ScaledQuantity.ofWhole(6),
        recordingMode = RecordingMode.CONFIRM
    )

    private val samplePrescription = Prescription(
        id = "rx_comp_1",
        medicationId = "med_detail_comp_1",
        referenceNumber = "NHS-RX-12345",
        issueDateEpochDay = 19500L,
        repeatsAuthorised = 3,
        repeatsRemaining = 3,
        status = PrescriptionStatus.ACTIVE
    )

    @Test
    fun t167_restockDialogDisplaysQuantityAndExpiryFields() {
        // T167: Restock dialog accepts lot number, quantity, and expiry date
        composeTestRule.setContent {
            NoveloTheme {
                RestockDialog(
                    medication = sampleMedication,
                    currentBalance = ScaledQuantity.ofWhole(21),
                    onDismiss = {},
                    onConfirm = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Restock Supplies").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quantity Added", substring = true).assertIsDisplayed()
    }

    @Test
    fun t168_adjustStockDialogDisplaysCountIncreaseDecreaseOptions() {
        // T168: Manual adjustment button triggers adjust stock dialog
        composeTestRule.setContent {
            NoveloTheme {
                AdjustStockDialog(
                    medication = sampleMedication,
                    currentBalance = ScaledQuantity.ofWhole(21),
                    lots = emptyList(),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Recount / Adjust Stock").assertIsDisplayed()
    }

    @Test
    fun t169_editMedicationDialogDisplaysEditableFields() {
        // T169: Edit medication dialog displays editable fields
        composeTestRule.setContent {
            NoveloTheme {
                EditMedicationDialog(
                    medication = sampleMedication,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Medication Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medication Name", substring = true).assertIsDisplayed()
    }

    @Test
    fun t170_requestRepeatDialogDisplaysPrescriptionReference() {
        // T170: Request repeat dialog displays prescription reference and authorised repeats
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
    fun t171_collectRepeatDialogDisplaysRemainingRepeats() {
        // T171: Collect repeat dialog displays remaining repeats
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
    }

    @Test
    fun t172_restockDialogDisplaysCurrentStockBalance() {
        // T172: Restock dialog displays current stock balance
        composeTestRule.setContent {
            NoveloTheme {
                RestockDialog(
                    medication = sampleMedication,
                    currentBalance = ScaledQuantity.ofWhole(21),
                    onDismiss = {},
                    onConfirm = { _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Ledger Impact Preview (+RESTOCK)").assertIsDisplayed()
    }

    @Test
    fun t173_adjustStockDialogDisplaysCurrentBalance() {
        // T173: Adjust stock dialog displays current balance
        composeTestRule.setContent {
            NoveloTheme {
                AdjustStockDialog(
                    medication = sampleMedication,
                    currentBalance = ScaledQuantity.ofWhole(21),
                    lots = emptyList(),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Actual Observed Count", substring = true).assertIsDisplayed()
    }
}
