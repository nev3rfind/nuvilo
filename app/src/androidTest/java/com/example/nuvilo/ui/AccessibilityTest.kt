package com.example.nuvilo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
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
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.HeroDoseCard
import com.example.nuvilo.ui.components.MedicationItemCard
import com.example.nuvilo.ui.components.SectionHeader
import com.example.nuvilo.ui.components.TimelineDoseItem
import com.example.nuvilo.ui.screens.LockScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated Jetpack Compose Semantics and Accessibility Test Suite.
 *
 * Verifies WCAG AA accessibility compliance across core components:
 * 1. Minimum 48x48dp interactive touch target bounds.
 * 2. Explicit TalkBack content descriptions on action buttons and status badges.
 * 3. Heading semantics for screen-reader structure navigation.
 * 4. Screen-reader content isolation on [LockScreen] to guarantee health privacy.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMedication = Medication(
        id = "med-test-01",
        name = "Amoxicillin",
        strength = "500 mg",
        form = MedicationForm.CAPSULE,
        stockUnit = StockUnit.CAPSULES,
        openingStock = ScaledQuantity.ofWhole(28),
        lowStockThreshold = ScaledQuantity.ofWhole(7),
        recordingMode = RecordingMode.CONFIRM,
        isArchived = false
    )

    private val sampleOccurrence = DoseOccurrence(
        occurrenceId = "occ-amox-001",
        medicationId = "med-test-01",
        scheduledEpochMs = 1700003600000L, // 01:00 UTC
        doseQuantity = ScaledQuantity.ofWhole(1),
        unit = StockUnit.CAPSULES,
        status = DoseStatus.PENDING
    )

    private val sampleStock = StockBalance(
        currentBalance = ScaledQuantity.ofWhole(28),
        unit = StockUnit.CAPSULES,
        isEstimate = false,
        isNegative = false,
        daysRemaining = 14,
        isLowStock = false
    )

    @Test
    fun heroDoseCard_actionButtons_meetMinimumTouchTargetSize_andProvideTalkBackLabels() {
        var taken = false
        var skipped = false
        var snoozed = false

        composeTestRule.setContent {
            NoveloTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    HeroDoseCard(
                        occurrence = sampleOccurrence,
                        medication = sampleMedication,
                        stockBalance = sampleStock,
                        onTake = { taken = true },
                        onSkip = { skipped = true },
                        onSnooze = { snoozed = true },
                        onClick = {}
                    )
                }
            }
        }

        // 1. Verify "Take Dose" button accessibility
        composeTestRule
            .onNodeWithContentDescription("Record Amoxicillin as taken")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        // 2. Verify "Skip" button accessibility
        composeTestRule
            .onNodeWithContentDescription("Skip Amoxicillin dose")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        // 3. Verify "Snooze" button accessibility
        composeTestRule
            .onNodeWithContentDescription("Snooze Amoxicillin reminder")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun timelineDoseItem_actionButtons_meetMinimumTouchTargetSize_andAreIndependentlyAccessible() {
        composeTestRule.setContent {
            NoveloTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    TimelineDoseItem(
                        occurrence = sampleOccurrence,
                        medicationName = sampleMedication.name,
                        medicationStrength = sampleMedication.strength,
                        stockBalance = sampleStock,
                        isFirst = true,
                        isLast = true,
                        onTake = {},
                        onSkip = {},
                        onClick = {}
                    )
                }
            }
        }

        // Ensure "Take" and "Skip" action buttons are independently exposed to TalkBack and meet 48dp target
        composeTestRule
            .onNodeWithContentDescription("Take Amoxicillin dose")
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
    fun sectionHeader_hasHeadingSemantics_forScreenReaderNavigation() {
        composeTestRule.setContent {
            NoveloTheme {
                SectionHeader(
                    title = "Today's Schedule",
                    actionText = "View All",
                    onActionClick = {}
                )
            }
        }

        // Verify heading semantics property is active on section header
        composeTestRule
            .onNodeWithText("Today's Schedule")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun emptyStateCard_meetsAccessibilityTarget_andPresentsClearLabels() {
        var actionClicked = false
        composeTestRule.setContent {
            NoveloTheme {
                EmptyStateCard(
                    title = "No Medications Added",
                    description = "Add your prescribed medications to get started.",
                    icon = Icons.Default.Medication,
                    actionButtonText = "Add Medication",
                    onActionClick = { actionClicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithText("No Medications Added")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Add Medication")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun lockScreen_isolatesSensitiveHealthData_andProvidesAccessibleUnlockTarget() {
        var unlockClicked = false

        composeTestRule.setContent {
            NoveloTheme {
                LockScreen(
                    biometricStatus = BiometricStatus.AVAILABLE,
                    errorMessage = null,
                    onUnlockClick = { unlockClicked = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 1. Verify app title is a semantic heading
        composeTestRule
            .onNodeWithText("Novelo")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        // 2. Verify unlock button satisfies touch target $\ge 48$dp
        composeTestRule
            .onNodeWithText("Unlock Novelo")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        // 3. Ensure NO sensitive medication or adherence text exists on screen
        composeTestRule
            .onNode(hasText("Amoxicillin"))
            .assertDoesNotExist()
        composeTestRule
            .onNode(hasText("Take"))
            .assertDoesNotExist()
        composeTestRule
            .onNode(hasContentDescription("Next dose"))
            .assertDoesNotExist()
    }
}
