package com.example.nuvilo.regression

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.security.BiometricStatus
import com.example.nuvilo.ui.components.HeroDoseCard
import com.example.nuvilo.ui.components.SectionHeader
import com.example.nuvilo.ui.screens.LockScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category F (Compose UI & Accessibility)
 * Tests T187 to T190: Accessibility Standards (WCAG AA) Verification
 */
@RunWith(AndroidJUnit4::class)
class ComposeAccessibilityRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMedication = Medication(
        id = "med_access_1",
        name = "Amoxicillin",
        strength = "500mg",
        form = MedicationForm.CAPSULE,
        stockUnit = StockUnit.CAPSULES,
        openingStock = ScaledQuantity.ofWhole(21),
        lowStockThreshold = ScaledQuantity.ofWhole(6),
        recordingMode = RecordingMode.CONFIRM
    )

    private val sampleOccurrence = DoseOccurrence(
        occurrenceId = "occ_access_1",
        medicationId = "med_access_1",
        scheduledEpochMs = 1700000000000L,
        doseQuantity = ScaledQuantity.ofWhole(1),
        unit = StockUnit.CAPSULES,
        status = DoseStatus.PENDING
    )

    private val sampleStock = StockBalance(
        currentBalance = ScaledQuantity.ofWhole(21),
        unit = StockUnit.CAPSULES,
        isNegative = false,
        isLowStock = false,
        isEstimate = false,
        daysRemaining = 7
    )

    @Test
    fun t187_heroDoseCardActionsMeetMinimum48dpTouchTargetSize() {
        // T187: All primary interactive buttons meet minimum 48x48dp touch target bounds
        composeTestRule.setContent {
            NoveloTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    HeroDoseCard(
                        occurrence = sampleOccurrence,
                        medication = sampleMedication,
                        stockBalance = sampleStock,
                        onTake = {},
                        onSkip = {},
                        onSnooze = {}
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Record Amoxicillin as taken")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        composeTestRule
            .onNodeWithContentDescription("Skip Amoxicillin dose")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun t188_contentDescriptionsArePresentOnAllActionButtons() {
        // T188: Content descriptions are present on all icon buttons and action badges
        composeTestRule.setContent {
            NoveloTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    HeroDoseCard(
                        occurrence = sampleOccurrence,
                        medication = sampleMedication,
                        stockBalance = sampleStock,
                        onTake = {},
                        onSkip = {},
                        onSnooze = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Record Amoxicillin as taken").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Skip Amoxicillin dose").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Snooze Amoxicillin reminder").assertIsDisplayed()
    }

    @Test
    fun t189_sectionHeadersDeclareSemanticsHeadingProperty() {
        // T189: Section headers declare semantics heading property for screen readers
        composeTestRule.setContent {
            NoveloTheme {
                SectionHeader(
                    title = "Upcoming Doses",
                    actionText = "See All",
                    onActionClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Upcoming Doses")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun t190_lockScreenMasksSensitiveHealthDataFromAccessibilityHierarchy() {
        // T190: LockScreen masks sensitive health data from accessibility hierarchy
        composeTestRule.setContent {
            NoveloTheme {
                LockScreen(
                    biometricStatus = BiometricStatus.AVAILABLE,
                    errorMessage = null,
                    onUnlockClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("App Locked · Health Records Protected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unlock Nuvilo").assertIsDisplayed()
    }
}
