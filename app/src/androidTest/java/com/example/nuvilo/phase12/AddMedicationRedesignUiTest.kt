package com.example.nuvilo.phase12

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationRoute
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import com.example.nuvilo.ui.screens.AddMedicationDialog
import com.example.nuvilo.ui.screens.EditMedicationDialog
import com.example.nuvilo.ui.theme.NoveloTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddMedicationRedesignUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchWizard(
        darkTheme: Boolean = false,
        fontScale: Float = 1.0f,
        onDismiss: () -> Unit = {},
        onConfirmFull: ((
            name: String,
            strength: String,
            form: MedicationForm,
            unit: StockUnit,
            openingStock: String,
            lowStockThreshold: String,
            recordingMode: RecordingMode,
            targetTimes: List<Int>,
            daysOfWeek: Set<Int>,
            doseQuantity: String,
            notes: String,
            colorTagHex: String,
            instructions: String,
            packagingConfig: PackagingConfig?,
            administrationProfile: AdministrationProfile,
            initialPackagesCount: Int,
            lowStockDaysThreshold: Int?,
            expiryDateEpochMs: Long?,
            lotNumber: String?,
            administrationRoute: AdministrationRoute,
            startDateEpochDay: Long,
            endDateEpochDay: Long?
        ) -> Unit)? = null
    ) {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = fontScale)
            ) {
                NoveloTheme(darkTheme = darkTheme) {
                    AddMedicationDialog(
                        onDismiss = onDismiss,
                        onConfirmFull = onConfirmFull
                    )
                }
            }
        }
    }

    private fun fillValidStep1(name: String = "Hydro Forte", strength: String = "0.15%") {
        composeTestRule.onNodeWithText("Medication Name *").performScrollTo().performTextReplacement(name)
        composeTestRule.onNodeWithText("Strength / Concentration *").performScrollTo().performTextReplacement(strength)
    }

    @Test
    fun t01_openWizard_displaysHeaderAndCloseAction() {
        val visible = mutableStateOf(true)
        composeTestRule.setContent {
            NoveloTheme {
                if (visible.value) {
                    AddMedicationDialog(onDismiss = { visible.value = false })
                }
            }
        }
        composeTestRule.onNodeWithText("Add Medication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add the medication you want to track.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_close_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add Medication").assertDoesNotExist()
    }

    @Test
    fun t02_step1_rendersIdentityDosageFormAndPackageSections() {
        launchWizard()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medication Identity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dosage Form").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("How You Buy / Store It (Package)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t03_step2_rendersAdministrationScheduleAndRecordingSections() {
        launchWizard()
        fillValidStep1()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Section A · Administration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Section B · Schedule").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Section C · Dose Recording").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t04_step3_rendersOpeningInventoryExpiryAndRegimenSummary() {
        launchWizard()
        fillValidStep1()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Opening Physical Inventory").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expiry & Batch Tracking (Optional)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Regimen Summary").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t05_progressIndicatorNavigation_switchesBetweenSteps() {
        launchWizard()
        composeTestRule.onNodeWithContentDescription("02 Schedule").performClick()
        composeTestRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("03 Stock").performClick()
        composeTestRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("01 Identity").performClick()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
    }

    @Test
    fun t06_backNavigation_returnsToPreviousStep() {
        launchWizard()
        fillValidStep1()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
    }

    @Test
    fun t07_preservesEnteredStateAcrossStepNavigation() {
        launchWizard()
        fillValidStep1(name = "Bisoprolol", strength = "2.5 mg")
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("18:00 Evening").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Back").performClick()
        composeTestRule.onNodeWithText("Bisoprolol").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.5 mg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Bisoprolol · 2.5 mg", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t08_validation_missingNameAndStrength_blocksAdvanceWithInlineErrors() {
        launchWizard()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter the medication name to continue.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Required: Enter medication name and strength to continue.").assertIsDisplayed()
    }

    @Test
    fun t09_dosageFormSelection_coversAllFormsIncludingDropsAndOther() {
        launchWizard()
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Estimated drops per bottle *", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Other").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Confirm").assertDoesNotExist()
    }

    @Test
    fun t10_dosageFormSwitchAfterCustomPackage_promptsConfirmation() {
        launchWizard()
        composeTestRule.onNodeWithText("Usable tab per blister pack *").performScrollTo().performTextReplacement("56")
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Change dosage form?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Switch Form").performClick()
        composeTestRule.onNodeWithText("Estimated drops per bottle *").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t11_contextualPackageOptions_hideIrrelevantContainers() {
        launchWizard()
        // Drops -> Bottle, Box of single-dose containers, Single-dose container; not Blister pack
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("How You Buy / Store It (Package)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bottle").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Box of single-dose containers").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Blister pack").assertDoesNotExist()
    }

    @Test
    fun t12_eyeDrops_administrationSiteShowsLeftRightBothMath() {
        launchWizard()
        fillValidStep1(name = "Hydro Forte", strength = "0.15%")
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Left eye").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Right eye").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Both eyes").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1 drop in each eye = 2 drops consumed per administration.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t13_scheduleCreation_weekdayAndOngoingControlsWork() {
        launchWizard()
        fillValidStep1()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Specific Weekdays").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Mon").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Ongoing schedule").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Ends automatically after the selected end date").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t14_timeSelection_presetAndCustom24hTimeWork() {
        launchWizard()
        fillValidStep1()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("12:00 Noon").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Hour").performScrollTo().performTextReplacement("15")
        composeTestRule.onNodeWithText("Minute").performScrollTo().performTextReplacement("30")
        composeTestRule.onNodeWithText("Add").performScrollTo().performClick()
        composeTestRule.onNodeWithText("15:30 Custom").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t15_packageQuantityCalculation_twoBottlesTimes300Equals600Drops() {
        launchWizard()
        fillValidStep1(name = "Hydro Forte", strength = "0.15%")
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithContentDescription("Increase Number of Bottles").performScrollTo().performClick()
        composeTestRule.onNodeWithText("2 bottles × 300 = 600 drops").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t16_estimatedSupplyCalculation_calculatesAccurateDaysRunway() {
        launchWizard()
        fillValidStep1(name = "Hydro Forte", strength = "0.15%")
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        // 2 drops in BOTH eyes, 1x daily = 2 drops/day; 1 bottle = 300 drops -> ~150 days
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Approximately 150 day(s)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t17_invalidPackageCapacity_zeroOrNegative_showsInlineErrorAndBlocksNext() {
        launchWizard()
        fillValidStep1()
        composeTestRule.onNodeWithText("Usable tab per blister pack *").performScrollTo().performTextReplacement("0")
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter a positive quantity greater than 0.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t18_missingRequiredFields_onStep3ShowsDisabledSaveReason() {
        launchWizard()
        composeTestRule.onNodeWithContentDescription("03 Stock").performClick()
        composeTestRule.onNodeWithTag("wizard_save_button").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Step 1 incomplete", substring = true).assertIsDisplayed()
    }

    @Test
    fun t19_disabledActionExplanation_clearsWhenRequiredFieldsAreFilled() {
        launchWizard()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Required: Enter medication name and strength to continue.").assertIsDisplayed()
        fillValidStep1()
        composeTestRule.onNodeWithText("Required: Enter medication name and strength to continue.").assertDoesNotExist()
    }

    @Test
    fun t20_successfulMedicationCreation_invokesCallbackWithCompletePayload() {
        var savedName: String? = null
        var savedForm: MedicationForm? = null
        var savedPkg: PackagingConfig? = null
        var savedAdmin: AdministrationProfile? = null
        var savedPkgCount = 0
        var savedRoute: AdministrationRoute? = null

        launchWizard(
            onConfirmFull = { name, _, form, _, _, _, _, _, _, _, _, _, _, pkg, admin, pkgCount, _, _, _, route, _, _ ->
                savedName = name
                savedForm = form
                savedPkg = pkg
                savedAdmin = admin
                savedPkgCount = pkgCount
                savedRoute = route
            }
        )

        fillValidStep1(name = "Hydro Forte", strength = "0.15%")
        composeTestRule.onNodeWithText("Eye / Ear Drops").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithContentDescription("Increase Number of Bottles").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("wizard_save_button").assertIsEnabled().performClick()
        composeTestRule.waitForIdle()

        assertEquals("Hydro Forte", savedName)
        assertEquals(MedicationForm.DROPS, savedForm)
        assertEquals(PackageType.BOTTLE, savedPkg?.packageType)
        assertEquals(StockUnit.DROPS, savedPkg?.consumptionUnit)
        assertEquals(AdministrationSite.BOTH_EYES, savedAdmin?.site)
        assertEquals(2, savedPkgCount)
        assertEquals(AdministrationRoute.OPHTHALMIC, savedRoute)
    }

    @Test
    fun t21_duplicateSubmissionPrevention_invokesSaveOnce() {
        var submitCount = 0
        launchWizard(
            onConfirmFull = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ ->
                submitCount += 1
            }
        )
        fillValidStep1()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithTag("wizard_save_button").performClick()
        composeTestRule.onNodeWithTag("wizard_save_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, submitCount)
    }

    @Test
    fun t22_keyboardAndImeNext_workSmoothlyOnIdentityFields() {
        launchWizard()
        composeTestRule.onNodeWithText("Medication Name *").performTextReplacement("Metformin")
        composeTestRule.onNodeWithText("Medication Name *").performImeAction()
        composeTestRule.onNodeWithText("Strength / Concentration *").performTextReplacement("500 mg")
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
    }

    @Test
    fun t23_scrollingAcrossLongStepContent_keepsFooterAccessible() {
        launchWizard()
        fillValidStep1()
        composeTestRule.onNodeWithText("Clinical Notes (Optional)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_continue_button").assertIsDisplayed()
    }

    @Test
    fun t24_lightMode_rendersStep1To3Cleanly() {
        launchWizard(darkTheme = false)
        fillValidStep1()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
    }

    @Test
    fun t25_darkMode_rendersStep1To3Cleanly() {
        launchWizard(darkTheme = true)
        fillValidStep1()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
    }

    @Test
    fun t26_largeFontScaling_noCrashAndButtonsAccessible() {
        launchWizard(fontScale = 1.35f)
        composeTestRule.onNodeWithText("Add Medication").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_continue_button").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("wizard_close_button").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun t27_smallScreenLayout_remainsScrollableWithoutClippedActions() {
        composeTestRule.setContent {
            NoveloTheme {
                Box(modifier = Modifier.size(320.dp, 568.dp)) {
                    AddMedicationDialog(onDismiss = {})
                }
            }
        }
        composeTestRule.onNodeWithText("Add Medication").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_continue_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun t28_tabletLayout_rendersCenteredResponsiveContainer() {
        composeTestRule.setContent {
            NoveloTheme {
                Box(modifier = Modifier.size(900.dp, 1000.dp)) {
                    AddMedicationDialog(onDismiss = {})
                }
            }
        }
        composeTestRule.onNodeWithText("Add Medication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_continue_button").assertIsDisplayed()
    }

    @Test
    fun t29_talkBackSemantics_exposeHeadingsTabsAnd48dpTargets() {
        launchWizard()
        composeTestRule.onNodeWithText("Add Medication")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeTestRule.onNodeWithContentDescription("01 Identity")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("wizard_close_button")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("wizard_continue_button")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun t30_editMedicationDialog_displaysHistoryProtectionAndFormConfirmation() {
        val sampleMed = Medication(
            id = "med-edit-1",
            name = "Hydro Forte",
            strength = "0.15%",
            form = MedicationForm.DROPS,
            stockUnit = StockUnit.DROPS,
            openingStock = ScaledQuantity.ofWhole(300),
            lowStockThreshold = ScaledQuantity.ofWhole(40),
            packagingConfig = PackagingConfig(
                packageType = PackageType.BOTTLE,
                consumptionUnit = StockUnit.DROPS,
                contentsPerPackage = ScaledQuantity.ofWhole(300),
                discardAfterOpeningDays = 28,
                trackingPrecision = TrackingPrecision.ESTIMATED
            )
        )
        var updatedName: String? = null
        composeTestRule.setContent {
            NoveloTheme {
                EditMedicationDialog(
                    medication = sampleMed,
                    onDismiss = {},
                    onConfirmFull = { name, _, _, _, _, _, _, _, _, _, _, _, _ ->
                        updatedName = name
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Medication Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("History Protection & Supply Impact").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Medication Name *")[0].performScrollTo().performTextReplacement("Hydro Forte Gel")
        composeTestRule.onNodeWithText("Save Changes").performClick()
        composeTestRule.waitForIdle()
        assertNotNull(updatedName)
        assertEquals("Hydro Forte Gel", updatedName)
    }
}
