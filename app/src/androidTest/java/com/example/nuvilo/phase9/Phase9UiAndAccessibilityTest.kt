package com.example.nuvilo.phase9

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import com.example.nuvilo.ui.haptics.LocalNuviloHaptics
import com.example.nuvilo.ui.haptics.NuviloHapticEvent
import com.example.nuvilo.ui.haptics.RecordingNuviloHapticController
import com.example.nuvilo.ui.screens.AddMedicationDialog
import com.example.nuvilo.ui.screens.DiscardPackageDialog
import com.example.nuvilo.ui.screens.RecountOpenedPackageDialog
import com.example.nuvilo.ui.screens.RestockDialog
import com.example.nuvilo.ui.screens.SettingsScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase9UiAndAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleHydroForteMed(): Medication = Medication(
        id = "med-hydro-ui-1",
        name = "Hydro Forte",
        strength = "0.2%",
        form = MedicationForm.DROPS,
        stockUnit = StockUnit.DROPS,
        openingStock = ScaledQuantity.ofWhole(300),
        lowStockThreshold = ScaledQuantity.ofWhole(40),
        lowStockDaysThreshold = 7,
        recordingMode = RecordingMode.CONFIRM,
        packagingConfig = PackagingConfig(
            packageType = PackageType.BOTTLE,
            consumptionUnit = StockUnit.DROPS,
            contentsPerPackage = ScaledQuantity.ofWhole(300),
            discardAfterOpeningDays = 28,
            trackingPrecision = TrackingPrecision.ESTIMATED
        ),
        administrationProfile = AdministrationProfile(
            site = AdministrationSite.BOTH_EYES,
            leftEyeQuantity = ScaledQuantity.ofWhole(1),
            rightEyeQuantity = ScaledQuantity.ofWhole(1)
        )
    )

    @Test
    fun addMedicationWizard_singleDoseEyeDrops_decouplesTreatedEyesFromContainerConsumption() {
        var capturedPackaging: PackagingConfig? = null
        var capturedAdmin: AdministrationProfile? = null
        var capturedPackageCount = 0

        composeTestRule.setContent {
            NoveloTheme {
                AddMedicationDialog(
                    onDismiss = {},
                    onConfirmWithPackaging = { _, _, _, _, _, _, _, _, _, _, _, _, _, pkg, admin, pkgCount, _, _, _ ->
                        capturedPackaging = pkg
                        capturedAdmin = admin
                        capturedPackageCount = pkgCount
                    }
                )
            }
        }

        // Step 1: Select Eye Drops + Single-dose box first, then fill text without opening soft keyboard
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Box of single-dose containers").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Medication Name *").performScrollTo().performTextReplacement("Tear Guard PF")
        composeTestRule.onNodeWithText("Strength / Concentration *").performScrollTo().performTextReplacement("0.15%")

        composeTestRule.onNodeWithText("Next Step").performClick()

        // Step 2: Both eyes treated, single-dose container decoupling visible
        composeTestRule.onNodeWithText("Treated Eye(s)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Single-Dose Containers Used Per Administration *")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Next Step").performClick()

        // Step 3: Save & assert rich Phase 9 payload
        composeTestRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()
        assertNotNull(capturedPackaging)
        assertEquals(PackageType.BOX_SINGLE_DOSE, capturedPackaging?.packageType)
        assertEquals(StockUnit.CONTAINERS, capturedPackaging?.consumptionUnit)
        assertEquals(TrackingPrecision.EXACT, capturedPackaging?.trackingPrecision)
        assertEquals(AdministrationSite.BOTH_EYES, capturedAdmin?.site)
        assertEquals(ScaledQuantity.ofWhole(1), capturedAdmin?.containersPerAdministration)
        assertEquals(1, capturedPackageCount)
    }

    @Test
    fun restockDialog_packageMode_addsBottlesInDrops_andShowsCombinedImpact() {
        val med = sampleHydroForteMed()
        var restockedPackages = 0
        var addedPerPkg: ScaledQuantity? = null

        composeTestRule.setContent {
            NoveloTheme {
                RestockDialog(
                    medication = med,
                    currentBalance = ScaledQuantity.ofWhole(50),
                    onDismiss = {},
                    onConfirm = { _, _, _, _ -> },
                    onConfirmPackages = { packageCount, contentsPerPackage, _, _, _, _, _, _ ->
                        restockedPackages = packageCount
                        addedPerPkg = contentsPerPackage
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("+2 Bottles").performScrollTo().performClick()
        composeTestRule.onNodeWithText("50 → 650 drops (+600)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Restock").performClick()

        assertEquals(2, restockedPackages)
        assertEquals(ScaledQuantity.ofWhole(300), addedPerPkg)
    }

    @Test
    fun recountOpenedPackageDialog_scopesToOpenedBottle() {
        val med = sampleHydroForteMed()
        val openedBottle = StockLot(
            id = "lot-open-bottle-1",
            medicationId = med.id,
            lotNumber = "HYD-99",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(120),
            unit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            packageLabel = "Bottle #1 (Opened)",
            packageStatus = PackageStatus.OPENED,
            openedAtEpochMs = System.currentTimeMillis() - 86_400_000L,
            discardAfterOpeningDays = 28
        )

        var recountedValue: ScaledQuantity? = null
        var recountReason: String? = null

        composeTestRule.setContent {
            NoveloTheme {
                RecountOpenedPackageDialog(
                    medication = med,
                    openedLot = openedBottle,
                    onDismiss = {},
                    onConfirm = { observed, reason ->
                        recountedValue = observed
                        recountReason = reason
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Recount Opened Bottle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Observed Remaining in Opened Bottle (Drops) *")
            .performTextReplacement("95")
        composeTestRule.onNodeWithText("Physical recount").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Update Opened Bottle").performClick()

        assertEquals(ScaledQuantity.ofWhole(95), recountedValue)
        assertEquals("Physical recount", recountReason)
    }

    @Test
    fun discardPackageDialog_recordsWastageReason() {
        val med = sampleHydroForteMed()
        val openedBottle = StockLot(
            id = "lot-open-bottle-2",
            medicationId = med.id,
            lotNumber = "HYD-99",
            initialQuantity = ScaledQuantity.ofWhole(300),
            remainingQuantity = ScaledQuantity.ofWhole(120),
            unit = StockUnit.DROPS,
            packageType = PackageType.BOTTLE,
            packageLabel = "Bottle #1 (Opened)",
            packageStatus = PackageStatus.OPENED,
            openedAtEpochMs = System.currentTimeMillis() - 86_400_000L,
            discardAfterOpeningDays = 28
        )

        var discardedReason: String? = null
        composeTestRule.setContent {
            NoveloTheme(darkTheme = true) {
                DiscardPackageDialog(
                    medication = med,
                    lot = openedBottle,
                    onDismiss = {},
                    onConfirm = { discardedReason = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Wastage Ledger Deduction (-WASTAGE)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Discard").performClick()
        assertEquals("Opened past discard window", discardedReason)
    }

    @Test
    fun settingsScreen_hapticsAnd7NotificationCategories_meetTouchTargetAndTriggerHaptics() {
        val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as NoveloApplication
        val viewModel = SettingsViewModel(app.container.userPreferencesRepository)
        val recorder = RecordingNuviloHapticController()

        composeTestRule.setContent {
            NoveloTheme {
                CompositionLocalProvider(LocalNuviloHaptics provides recorder) {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }

        composeTestRule.onNodeWithText("Haptic & Tactile Feedback").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Balanced")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Test Haptic Feel")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(recorder.recordedEvents.contains(NuviloHapticEvent.DOSE_TAKEN))

        composeTestRule.onNodeWithText("Notification Categories (7)")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Medication Reminders", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Opened-Package Discard", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
