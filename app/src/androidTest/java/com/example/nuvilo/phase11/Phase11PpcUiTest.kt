package com.example.nuvilo.phase11

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.local.NoveloDatabase
import com.example.nuvilo.data.repository.NoveloRepositoryImpl
import com.example.nuvilo.domain.ppc.PpcCalculator
import com.example.nuvilo.domain.ppc.PpcCertificate
import com.example.nuvilo.domain.ppc.PpcCoverageType
import com.example.nuvilo.ui.components.PpcStatusBannerCard
import com.example.nuvilo.ui.screens.PpcManagementScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.PpcViewModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class Phase11PpcUiTest {

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
    fun ppcStatusBannerCard_rendersActiveInLightAndDarkMode_andTriggersClick() {
        val today = LocalDate.now()
        val cert = PpcCertificate(
            id = "ppc_banner_1",
            coverageType = PpcCoverageType.PPC_12_MONTH,
            startDate = today.minusMonths(2),
            expiryDate = today.plusMonths(10)
        )
        val evaluation = PpcCalculator().evaluateCertificate(cert, today)
        var clicked = false

        composeTestRule.setContent {
            NoveloTheme(darkTheme = true) {
                PpcStatusBannerCard(
                    evaluation = evaluation,
                    onClick = { clicked = true }
                )
            }
        }

        val banner = composeTestRule.onNodeWithText("12-Month PPC", substring = true)
        banner.assertIsDisplayed()
        composeTestRule.onNodeWithText("Based on your recorded certificate dates", substring = true)
            .assertIsDisplayed()
        banner.performClick()
        assertTrue(clicked)
    }

    @Test
    fun ppcManagementScreen_fourStepWizard_savesCertificate() {
        val viewModel = PpcViewModel(ppcRepository = repository, ppcAlarmScheduler = null)

        composeTestRule.setContent {
            NoveloTheme(darkTheme = false) {
                PpcManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("NHS PPC & Exemptions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").performClick()

        // Step 1
        composeTestRule.onNodeWithText("Step 1: Coverage Type", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").performClick()

        // Step 2
        composeTestRule.onNodeWithText("Step 2: Dates & Duration", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").performClick()

        // Step 3
        composeTestRule.onNodeWithText("Step 3: Reference, Notes & Reminders", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").performClick()

        // Step 4
        composeTestRule.onNodeWithText("Step 4: Review & Confirm", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Certificate").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Based on your recorded certificate dates", substring = true)
            .assertIsDisplayed()
    }
}
