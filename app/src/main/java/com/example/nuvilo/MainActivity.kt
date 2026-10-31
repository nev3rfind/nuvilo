package com.example.nuvilo

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.nuvilo.data.preferences.UserPreferencesRepository
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
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.viewmodel.AdherenceCalendarViewModel
import com.example.nuvilo.ui.viewmodel.DashboardViewModel
import com.example.nuvilo.ui.viewmodel.MedicationViewModel
import com.example.nuvilo.ui.viewmodel.SettingsViewModel
import com.example.nuvilo.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.Today),
    HISTORY("Calendar", Icons.Default.CalendarToday),
    MEDICATIONS("Medications", Icons.Default.Medication),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : FragmentActivity() {

    private val selectedMedicationIdState = mutableStateOf<String?>(null)
    private lateinit var appLockManager: AppLockManager
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as NoveloApplication).container
        appLockManager = appContainer.appLockManager
        biometricAuthManager = appContainer.biometricAuthManager
        userPreferencesRepository = appContainer.userPreferencesRepository

        lifecycleScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            appLockManager.initialize(prefs.isBiometricLockEnabled)
        }

        val initialMedId = intent.getStringExtra(NotificationHelper.EXTRA_SELECTED_MEDICATION_ID)
        selectedMedicationIdState.value = initialMedId

        val factory = ViewModelFactory(appContainer)

        val dashboardViewModel: DashboardViewModel by viewModels { factory }
        val calendarViewModel: AdherenceCalendarViewModel by viewModels { factory }
        val medicationViewModel: MedicationViewModel by viewModels { factory }
        val settingsViewModel: SettingsViewModel by viewModels { factory }

        setContent {
            val userPrefs by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

            // Window FLAG_SECURE management for recents preview masking and active lock
            LaunchedEffect(userPrefs.isRecentsPreviewMasked, isLocked) {
                if (userPrefs.isRecentsPreviewMasked || isLocked) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Auto-trigger biometric prompt when locked
            LaunchedEffect(isLocked) {
                val status = biometricAuthManager.checkBiometricStatus()
                if (isLocked && (status == BiometricStatus.AVAILABLE || status == BiometricStatus.DEVICE_CREDENTIAL_ONLY)) {
                    promptUnlock()
                }
            }

            NoveloTheme(themeMode = userPrefs.themeMode) {
                if (isLocked) {
                    BackHandler {
                        finish()
                    }
                    LockScreen(
                        biometricStatus = biometricAuthManager.checkBiometricStatus(),
                        errorMessage = null,
                        onUnlockClick = { promptUnlock() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    var currentScreen by remember { mutableStateOf(Screen.TODAY) }
                    val selectedMedicationId by selectedMedicationIdState
                    var showMedicationDoseHistory by remember(selectedMedicationId) { mutableStateOf(false) }

                    BackHandler(enabled = selectedMedicationId != null) {
                        if (showMedicationDoseHistory) {
                            showMedicationDoseHistory = false
                        } else {
                            selectedMedicationIdState.value = null
                        }
                    }

                    if (selectedMedicationId != null) {
                        val detailViewModel = remember(selectedMedicationId) {
                            factory.createMedicationDetailViewModel(selectedMedicationId!!)
                        }
                        if (showMedicationDoseHistory) {
                            MedicationDoseHistoryScreen(
                                viewModel = detailViewModel,
                                onNavigateBack = { showMedicationDoseHistory = false },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            MedicationDetailScreen(
                                viewModel = detailViewModel,
                                onNavigateBack = { selectedMedicationIdState.value = null },
                                onNavigateToDoseHistory = { showMedicationDoseHistory = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    tonalElevation = 3.dp
                                ) {
                                    Screen.values().forEach { screen ->
                                        val selected = currentScreen == screen
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { currentScreen = screen },
                                            icon = {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = screen.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
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
                        ) { innerPadding ->
                            when (currentScreen) {
                                Screen.TODAY -> DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateToMedications = { currentScreen = Screen.MEDICATIONS },
                                    onNavigateToCalendar = { currentScreen = Screen.HISTORY },
                                    onMedicationClick = { medId -> selectedMedicationIdState.value = medId },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                Screen.HISTORY -> AdherenceCalendarScreen(
                                    viewModel = calendarViewModel,
                                    onMedicationClick = { medId -> selectedMedicationIdState.value = medId },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                Screen.MEDICATIONS -> MedicationListScreen(
                                    viewModel = medicationViewModel,
                                    onMedicationClick = { medId -> selectedMedicationIdState.value = medId },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                Screen.SETTINGS -> SettingsScreen(
                                    viewModel = settingsViewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun promptUnlock() {
        appLockManager.setSystemDialogActive(true)
        biometricAuthManager.authenticate(
            activity = this,
            onResult = { result ->
                appLockManager.setSystemDialogActive(false)
                if (result is BiometricAuthResult.Success) {
                    appLockManager.unlock()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            appLockManager.onAppForegrounded(prefs.isBiometricLockEnabled, prefs.lockGracePeriod)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            appLockManager.onAppBackgrounded()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_SELECTED_MEDICATION_ID)?.let { medId ->
            selectedMedicationIdState.value = medId
        }
    }
}
