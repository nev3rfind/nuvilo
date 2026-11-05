package com.example.nuvilo

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.nuvilo.data.preferences.ThemeMode
import com.example.nuvilo.security.AppLockManager
import com.example.nuvilo.security.BiometricAuthManager
import com.example.nuvilo.security.BiometricAuthResult
import com.example.nuvilo.security.BiometricStatus
import com.example.nuvilo.system.notification.NotificationHelper
import com.example.nuvilo.ui.screens.AdherenceCalendarScreen
import com.example.nuvilo.ui.screens.DashboardScreen
import com.example.nuvilo.ui.screens.LockScreen
import com.example.nuvilo.ui.screens.MedicationDetailScreen
import com.example.nuvilo.ui.screens.MedicationDoseHistoryScreen
import com.example.nuvilo.ui.screens.MedicationListScreen
import com.example.nuvilo.ui.screens.SettingsScreen
import com.example.nuvilo.ui.screens.SplashScreen
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.AdherenceCalendarViewModel
import com.example.nuvilo.ui.viewmodel.DashboardViewModel
import com.example.nuvilo.ui.viewmodel.MedicationViewModel
import com.example.nuvilo.ui.viewmodel.SettingsViewModel
import com.example.nuvilo.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class Screen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("Today", Icons.Filled.Today, Icons.Outlined.Today),
    MEDICATIONS("Medications", Icons.Filled.Medication, Icons.Outlined.Medication),
    CALENDAR("Adherence", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : FragmentActivity() {

    private val appContainer by lazy { (application as NoveloApplication).container }
    private val viewModelFactory by lazy { ViewModelFactory(appContainer) }

    private val dashboardViewModel: DashboardViewModel by viewModels { viewModelFactory }
    private val medicationViewModel: MedicationViewModel by viewModels { viewModelFactory }
    private val adherenceCalendarViewModel: AdherenceCalendarViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }

    private val appLockManager: AppLockManager by lazy { appContainer.appLockManager }
    private val biometricAuthManager: BiometricAuthManager by lazy { appContainer.biometricAuthManager }

    private val intentMedicationIdState = mutableStateOf<String?>(null)
    private val authErrorMessageState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        handleIncomingIntent(intent)

        lifecycleScope.launch {
            val prefs = appContainer.userPreferencesRepository.userPreferencesFlow.first()
            appLockManager.initialize(prefs.isBiometricLockEnabled)
        }

        setContent {
            val prefs by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

            var showSplashScreen by rememberSaveable {
                mutableStateOf(intentMedicationIdState.value == null)
            }

            val darkTheme = when (prefs.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            LaunchedEffect(prefs.isBiometricLockEnabled, prefs.isRecentsPreviewMasked, isLocked, showSplashScreen) {
                val shouldSecure = (!showSplashScreen && isLocked) || (prefs.isBiometricLockEnabled && prefs.isRecentsPreviewMasked)
                if (shouldSecure) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(isLocked, showSplashScreen) {
                if (!showSplashScreen && isLocked && prefs.isBiometricLockEnabled) {
                    triggerBiometricPrompt()
                }
            }

            NoveloTheme(darkTheme = darkTheme) {
                AnimatedContent(
                    targetState = showSplashScreen,
                    transitionSpec = { NoveloMotion.screenTransition() },
                    label = "splashAppTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onSplashComplete = { showSplashScreen = false }
                        )
                    } else if (prefs.isBiometricLockEnabled && isLocked) {
                        val bioStatus = remember(isLocked) { biometricAuthManager.checkBiometricStatus() }
                        LockScreen(
                            biometricStatus = bioStatus,
                            errorMessage = authErrorMessageState.value,
                            onUnlockClick = { triggerBiometricPrompt() }
                        )
                    } else {
                        NoveloMainContent(
                            dashboardViewModel = dashboardViewModel,
                            medicationViewModel = medicationViewModel,
                            adherenceCalendarViewModel = adherenceCalendarViewModel,
                            settingsViewModel = settingsViewModel,
                            viewModelFactory = viewModelFactory,
                            intentMedicationId = intentMedicationIdState.value,
                            onConsumeIntentMedicationId = { intentMedicationIdState.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val prefs = appContainer.userPreferencesRepository.userPreferencesFlow.first()
            appLockManager.onAppForegrounded(
                lockEnabled = prefs.isBiometricLockEnabled,
                gracePeriod = prefs.lockGracePeriod
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            appLockManager.onAppBackgrounded()
        }
    }

    private fun triggerBiometricPrompt() {
        val status = biometricAuthManager.checkBiometricStatus()
        if (status != BiometricStatus.AVAILABLE && status != BiometricStatus.DEVICE_CREDENTIAL_ONLY) {
            return
        }
        authErrorMessageState.value = null
        biometricAuthManager.authenticate(
            activity = this,
            onResult = { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        authErrorMessageState.value = null
                        appLockManager.unlock()
                    }
                    is BiometricAuthResult.Cancelled -> {
                        authErrorMessageState.value = "Authentication cancelled. Tap Unlock to try again."
                    }
                    is BiometricAuthResult.Failed -> {
                        authErrorMessageState.value = result.message
                    }
                    is BiometricAuthResult.Error -> {
                        authErrorMessageState.value = result.errString.toString()
                    }
                }
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val medId = intent?.getStringExtra(NotificationHelper.EXTRA_SELECTED_MEDICATION_ID)
        if (!medId.isNullOrBlank()) {
            intentMedicationIdState.value = medId
        }
    }
}

@Composable
private fun NoveloMainContent(
    dashboardViewModel: DashboardViewModel,
    medicationViewModel: MedicationViewModel,
    adherenceCalendarViewModel: AdherenceCalendarViewModel,
    settingsViewModel: SettingsViewModel,
    viewModelFactory: ViewModelFactory,
    intentMedicationId: String?,
    onConsumeIntentMedicationId: () -> Unit
) {
    var currentScreenName by rememberSaveable { mutableStateOf(Screen.TODAY.name) }
    var selectedMedicationId by rememberSaveable { mutableStateOf<String?>(null) }
    var showMedicationDoseHistory by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(intentMedicationId) {
        if (!intentMedicationId.isNullOrBlank()) {
            selectedMedicationId = intentMedicationId
            showMedicationDoseHistory = false
            onConsumeIntentMedicationId()
        }
    }

    val currentScreen = runCatching { Screen.valueOf(currentScreenName) }.getOrDefault(Screen.TODAY)

    if (selectedMedicationId != null) {
        val medId = selectedMedicationId!!
        val detailViewModel = remember(medId) {
            viewModelFactory.createMedicationDetailViewModel(medId)
        }

        if (showMedicationDoseHistory) {
            BackHandler { showMedicationDoseHistory = false }
            MedicationDoseHistoryScreen(
                viewModel = detailViewModel,
                onNavigateBack = { showMedicationDoseHistory = false }
            )
        } else {
            BackHandler { selectedMedicationId = null }
            MedicationDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { selectedMedicationId = null },
                onNavigateToDoseHistory = { showMedicationDoseHistory = true }
            )
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        Screen.entries.forEach { screen ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selected = isSelected,
                                onClick = { currentScreenName = screen.name },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    NoveloMotion.directionalHorizontalTransition(targetState.ordinal > initialState.ordinal)
                },
                label = "mainTabTransition",
                modifier = Modifier.padding(innerPadding)
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.TODAY -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        onSelectMedication = {
                            selectedMedicationId = it
                            showMedicationDoseHistory = false
                        },
                        onNavigateToMedications = {
                            currentScreenName = Screen.MEDICATIONS.name
                        }
                    )
                    Screen.MEDICATIONS -> MedicationListScreen(
                        viewModel = medicationViewModel,
                        onSelectMedication = {
                            selectedMedicationId = it
                            showMedicationDoseHistory = false
                        }
                    )
                    Screen.CALENDAR -> AdherenceCalendarScreen(
                        viewModel = adherenceCalendarViewModel,
                        onSelectMedication = {
                            selectedMedicationId = it
                            showMedicationDoseHistory = false
                        }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
