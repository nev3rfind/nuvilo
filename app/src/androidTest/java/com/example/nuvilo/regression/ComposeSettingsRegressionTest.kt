package com.example.nuvilo.regression

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nuvilo.data.preferences.UserPreferencesRepository
import com.example.nuvilo.ui.screens.SettingsScreen
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.SettingsViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression Test Suite: Category F (Compose UI & Accessibility)
 * Tests T183 to T186: Settings Screen, Preferences, and Diagnostics
 */
@RunWith(AndroidJUnit4::class)
class ComposeSettingsRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferencesRepository = UserPreferencesRepository(context)
        viewModel = SettingsViewModel(preferencesRepository)
    }

    @Test
    fun t183_settingsScreenDisplaysThemeSelectionOptions() {
        // T183: Settings screen displays theme mode selection options
        composeTestRule.setContent {
            NoveloTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Appearance & Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("System").assertIsDisplayed()
    }

    @Test
    fun t184_settingsScreenDisplaysNotificationPrivacyMaskingOption() {
        // T184: Settings screen displays privacy & app lock options
        composeTestRule.setContent {
            NoveloTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Privacy & App Lock").assertExists()
    }

    @Test
    fun t185_settingsScreenDisplaysAppLockOptions() {
        // T185: Settings screen displays biometric / device credential lock section
        composeTestRule.setContent {
            NoveloTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Biometric / Device Lock", substring = true).assertExists()
    }

    @Test
    fun t186_settingsScreenDisplaysBackupAndRestoreOptions() {
        // T186: Settings screen displays local encrypted backup & restore cards
        composeTestRule.setContent {
            NoveloTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Local Data Backup & Restore", substring = true).assertExists()
    }
}
