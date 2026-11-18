package com.example.nuvilo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationRoute
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.GelTrackingMode
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.model.TrackingPrecision
import com.example.nuvilo.domain.stock.SupplyForecastCalculator
import com.example.nuvilo.ui.components.AddMedicationTopBarAndProgressHeader
import com.example.nuvilo.ui.components.DialogSystemBarsEffect
import com.example.nuvilo.ui.components.DosageFormSelectionGrid
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.FormChangeConfirmationDialog
import com.example.nuvilo.ui.components.LiveInventoryPreviewCard
import com.example.nuvilo.ui.components.MedicationItemCard
import com.example.nuvilo.ui.components.PackageQuantityStepper
import com.example.nuvilo.ui.components.PackageTypeSelectionGrid
import com.example.nuvilo.ui.components.ScheduleConfigurationContent
import com.example.nuvilo.ui.components.TrackingPrecisionBadge
import com.example.nuvilo.ui.components.WizardBottomActionBar
import com.example.nuvilo.ui.components.WizardSectionCard
import com.example.nuvilo.ui.components.availablePackageTypesForForm
import com.example.nuvilo.ui.components.availableRoutesForForm
import com.example.nuvilo.ui.components.defaultContentsPerPackageStr
import com.example.nuvilo.ui.components.defaultPackageTypeForForm
import com.example.nuvilo.ui.components.defaultStockUnitForForm
import com.example.nuvilo.ui.components.formatMinuteOfDay
import com.example.nuvilo.ui.components.noveloWizardFilterChipColors
import com.example.nuvilo.ui.components.parseAccentColor
import com.example.nuvilo.ui.haptics.LocalNuviloHaptics
import com.example.nuvilo.ui.haptics.NuviloHapticEvent
import com.example.nuvilo.ui.theme.LocalMotionEnabled
import com.example.nuvilo.ui.theme.MedicationAccentSwatches
import com.example.nuvilo.ui.theme.NoveloBannerShape
import com.example.nuvilo.ui.theme.NoveloCardShape
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.NoveloPillShape
import com.example.nuvilo.ui.theme.NoveloSheetShape
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.theme.tabular
import com.example.nuvilo.ui.viewmodel.MedicationViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MedicationListScreen(
    viewModel: MedicationViewModel,
    onSelectMedication: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterLowStockOnly by rememberSaveable { mutableStateOf(false) }
    var medicationToRestock by remember { mutableStateOf<Pair<Medication, ScaledQuantity>?>(null) }

    val rawList = if (selectedTabIndex == 0) uiState.activeMedications else uiState.archivedMedications
    val filteredList = rawList.filter { item ->
        val matchesQuery = searchQuery.isBlank() ||
            item.medication.name.contains(searchQuery.trim(), ignoreCase = true) ||
            item.medication.strength.contains(searchQuery.trim(), ignoreCase = true) ||
            item.medication.form.displayName.contains(searchQuery.trim(), ignoreCase = true)
        val matchesLowStock = !filterLowStockOnly || (item.stockBalance?.isLowStock == true)
        matchesQuery && matchesLowStock
    }

    val lowStockActiveCount = uiState.activeMedications.count { it.stockBalance?.isLowStock == true }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "Add Medication"
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = NoveloPillShape,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = "Add Medication",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Editorial Header
            Text(
                text = "Medications",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Manage prescriptions, regimens, and append-only stock balances",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Pill Switcher: Active vs Archived
            SegmentedMedicationTabs(
                selectedTabIndex = selectedTabIndex,
                activeCount = uiState.activeMedications.size,
                archivedCount = uiState.archivedMedications.size,
                onSelectTab = { selectedTabIndex = it }
            )

            // Search & Quick Filter Bar (shown when there are medications in the tab or active search)
            if (rawList.isNotEmpty() || searchQuery.isNotEmpty() || filterLowStockOnly) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, strength, form...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = NoveloPillShape,
                        modifier = Modifier.weight(1f)
                    )

                    if (selectedTabIndex == 0 && lowStockActiveCount > 0) {
                        FilterChip(
                            selected = filterLowStockOnly,
                            onClick = { filterLowStockOnly = !filterLowStockOnly },
                            label = { Text("Low Stock ($lowStockActiveCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                rawList.isEmpty() -> {
                    EmptyStateCard(
                        icon = Icons.Default.Medication,
                        title = if (selectedTabIndex == 0) "No Active Medications" else "No Archived Medications",
                        description = if (selectedTabIndex == 0) {
                            "Add your first medication to configure dose times, weekday regimens, and exact stock tracking."
                        } else {
                            "Medications you archive when a course finishes will appear here with their full audit history intact."
                        },
                        actionLabel = if (selectedTabIndex == 0) "Add Medication" else null,
                        onActionClick = if (selectedTabIndex == 0) { { showAddDialog = true } } else null
                    )
                }
                filteredList.isEmpty() -> {
                    EmptyStateCard(
                        icon = Icons.Default.Search,
                        title = "No Matching Medications",
                        description = "No medications matched your current search or low-stock filter.",
                        actionLabel = "Reset Filters",
                        onActionClick = {
                            searchQuery = ""
                            filterLowStockOnly = false
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(
                            items = filteredList,
                            key = { it.medication.id },
                            contentType = { "medication_directory_card" }
                        ) { item ->
                            MedicationItemCard(
                                medication = item.medication,
                                stockBalance = item.stockBalance,
                                schedules = item.schedules,
                                pendingPrescription = item.pendingPrescription,
                                supplyForecast = item.supplyForecast,
                                onArchiveToggle = {
                                    val newArchived = !item.medication.isArchived
                                    viewModel.setArchived(item.medication.id, newArchived)
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val actionLabel = if (newArchived) "Archived" else "Restored"
                                        val result = snackbarHostState.showSnackbar(
                                            message = "$actionLabel ${item.medication.name}",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.setArchived(item.medication.id, !newArchived)
                                        }
                                    }
                                },
                                onCardClick = {
                                    onSelectMedication(item.medication.id)
                                },
                                onRestockClick = {
                                    medicationToRestock = item.medication to (item.stockBalance?.currentBalance ?: ScaledQuantity.ZERO)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onConfirmFull = { name, strength, form, unit, opening, threshold, mode, times, daysOfWeek, doseQty, notes, colorHex, instructions, packagingConfig, administrationProfile, initialPackagesCount, lowStockDaysThreshold, expiryEpochMs, lotNumber, administrationRoute, startDateEpochDay, endDateEpochDay ->
                viewModel.createMedicationWithPackages(
                    name = name,
                    strength = strength,
                    form = form,
                    unit = unit,
                    openingStockStr = opening,
                    lowStockThresholdStr = threshold,
                    recordingMode = mode,
                    targetTimesMinuteOfDay = times,
                    daysOfWeek = daysOfWeek,
                    doseQuantityStr = doseQty,
                    notes = notes,
                    colorTagHex = colorHex,
                    instructions = instructions,
                    packagingConfig = packagingConfig,
                    administrationProfile = administrationProfile,
                    initialPackagesCount = initialPackagesCount,
                    lowStockDaysThreshold = lowStockDaysThreshold,
                    expiryDateEpochMs = expiryEpochMs,
                    lotNumber = lotNumber,
                    administrationRoute = administrationRoute,
                    startDateEpochDay = startDateEpochDay,
                    endDateEpochDay = endDateEpochDay
                )
                showAddDialog = false
            }
        )
    }

    medicationToRestock?.let { (med, curBal) ->
        RestockDialog(
            medication = med,
            currentBalance = curBal,
            onDismiss = { medicationToRestock = null },
            onConfirmPackages = { packageCount, contentsPerPackage, consumptionUnit, packageType, discardDays, lotNumber, expiryEpochMs, notes, openFirst ->
                viewModel.restockPackages(
                    medicationId = med.id,
                    packageCount = packageCount,
                    contentsPerPackage = contentsPerPackage,
                    consumptionUnit = consumptionUnit,
                    packageType = packageType,
                    discardAfterOpeningDays = discardDays,
                    lotNumber = lotNumber,
                    expiryDateEpochMs = expiryEpochMs,
                    notes = notes,
                    openFirstPackageImmediately = openFirst
                )
                medicationToRestock = null
                val addedTotal = ScaledQuantity(contentsPerPackage.rawScaledValue * packageCount.toLong())
                scope.launch {
                    snackbarHostState.showSnackbar("Added $packageCount ${packageType.pluralName.lowercase()} (${addedTotal.toDisplayString()} ${consumptionUnit.abbreviation}) for ${med.name}")
                }
            },
            onConfirm = { qty, lotNumber, expiryEpochMs, notes ->
                viewModel.restockMedication(
                    medicationId = med.id,
                    quantity = qty,
                    unit = med.effectiveConsumptionUnit,
                    lotNumber = lotNumber,
                    expiryDateEpochMs = expiryEpochMs,
                    notes = notes
                )
                medicationToRestock = null
                scope.launch {
                    snackbarHostState.showSnackbar("Restocked ${qty.toDisplayString()} ${med.effectiveConsumptionUnit.abbreviation} for ${med.name}")
                }
            }
        )
    }
}

@Composable
private fun SegmentedMedicationTabs(
    selectedTabIndex: Int,
    activeCount: Int,
    archivedCount: Int,
    onSelectTab: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = NoveloPillShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                0 to "Active ($activeCount)",
                1 to "Archived ($archivedCount)"
            )
            tabs.forEach { (index, label) ->
                val isSelected = selectedTabIndex == index
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(NoveloPillShape)
                        .clickable(
                            onClick = { onSelectTab(index) },
                            role = Role.Tab
                        ),
                    shape = NoveloPillShape,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.tabular,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private val WIZARD_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

/**
 * Full-screen responsive 3-step Add Medication Wizard separating:
 * Identity & Form -> Packaging Format -> Administration & Schedule -> Physical Stock & Runway.
 *
 * Adapts to phones (edge-to-edge full-screen with keyboard/IME awareness) and tablets
 * (centered 640dp editorial column), while preserving compatibility with legacy [onConfirm]
 * and [onConfirmWithPackaging] callbacks.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onConfirm: (
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
        instructions: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onConfirmWithPackaging: ((
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
        lotNumber: String?
    ) -> Unit)? = null,
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
    val nuviloHaptics = LocalNuviloHaptics.current
    val motionEnabled = LocalMotionEnabled.current
    val semantic = MaterialTheme.noveloColors
    val scope = rememberCoroutineScope()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var maxVisitedStep by rememberSaveable { mutableIntStateOf(0) }
    var attemptedStepsMask by rememberSaveable { mutableIntStateOf(0) }

    // Step 1 State: Identity, Form & Package Definition
    var name by rememberSaveable { mutableStateOf("") }
    var strength by rememberSaveable { mutableStateOf("") }
    var selectedFormName by rememberSaveable { mutableStateOf(MedicationForm.TABLET.name) }
    var selectedRouteName by rememberSaveable { mutableStateOf(AdministrationRoute.ORAL.name) }
    var selectedUnitName by rememberSaveable { mutableStateOf(StockUnit.TABLETS.name) }
    var selectedPackageTypeName by rememberSaveable { mutableStateOf(PackageType.BLISTER_PACK.name) }
    var contentsPerPackageStr by rememberSaveable { mutableStateOf("28") }
    var discardAfterOpeningDaysStr by rememberSaveable { mutableStateOf("") }
    var gelModeName by rememberSaveable { mutableStateOf(GelTrackingMode.ESTIMATED_APPLICATIONS.name) }
    var hasUserCustomizedPackage by rememberSaveable { mutableStateOf(false) }
    var pendingFormSwitchName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedColorHex by rememberSaveable { mutableStateOf("#005EB8") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Step 2 State: Administration, Schedule & Recording
    val todayEpochDay = remember { LocalDate.now().toEpochDay() }
    var isSpecificWeekdays by rememberSaveable { mutableStateOf(false) }
    var selectedDaysList by rememberSaveable { mutableStateOf(listOf(1, 3, 5)) }
    var selectedTimesList by rememberSaveable { mutableStateOf(listOf(480)) } // Default 08:00
    var startDateEpochDay by rememberSaveable { mutableLongStateOf(todayEpochDay) }
    var endDateEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var doseQuantity by rememberSaveable { mutableStateOf("1") }
    var eyeSiteName by rememberSaveable { mutableStateOf(AdministrationSite.BOTH_EYES.name) }
    var leftEyeDropsStr by rememberSaveable { mutableStateOf("1") }
    var rightEyeDropsStr by rememberSaveable { mutableStateOf("1") }
    var containersPerDoseStr by rememberSaveable { mutableStateOf("1") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var selectedModeName by rememberSaveable { mutableStateOf(RecordingMode.CONFIRM.name) }

    // Step 3 State: Opening Inventory, Expiry & Thresholds
    var initialPackagesCountStr by rememberSaveable { mutableStateOf("1") }
    var openingStock by rememberSaveable { mutableStateOf("28") }
    var lowStockThreshold by rememberSaveable { mutableStateOf("7") }
    var lowStockDaysThresholdStr by rememberSaveable { mutableStateOf("7") }
    var lotNumberStr by rememberSaveable { mutableStateOf("") }
    var expiryDateEpochMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var showExpiryDatePicker by rememberSaveable { mutableStateOf(false) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val selectedForm = runCatching { MedicationForm.valueOf(selectedFormName) }.getOrDefault(MedicationForm.TABLET)
    val selectedRoute = runCatching { AdministrationRoute.valueOf(selectedRouteName) }
        .getOrDefault(AdministrationRoute.defaultForForm(selectedForm))
    val selectedPackageType = runCatching { PackageType.valueOf(selectedPackageTypeName) }.getOrDefault(defaultPackageTypeForForm(selectedForm))
    val gelMode = runCatching { GelTrackingMode.valueOf(gelModeName) }.getOrDefault(GelTrackingMode.ESTIMATED_APPLICATIONS)
    val selectedUnit = when {
        selectedPackageType == PackageType.BOX_SINGLE_DOSE || selectedPackageType == PackageType.SINGLE_DOSE_UNIT -> StockUnit.CONTAINERS
        selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.GRAMS -> StockUnit.GRAMS
        selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.ESTIMATED_APPLICATIONS -> StockUnit.APPLICATIONS
        else -> runCatching { StockUnit.valueOf(selectedUnitName) }.getOrDefault(StockUnit.TABLETS)
    }
    val selectedMode = runCatching { RecordingMode.valueOf(selectedModeName) }.getOrDefault(RecordingMode.CONFIRM)
    val eyeSite = runCatching { AdministrationSite.valueOf(eyeSiteName) }.getOrDefault(AdministrationSite.BOTH_EYES)

    val applyFormDefaults = { form: MedicationForm ->
        selectedFormName = form.name
        selectedRouteName = AdministrationRoute.defaultForForm(form).name
        val defaultPkg = defaultPackageTypeForForm(form)
        val defaultUnit = defaultStockUnitForForm(form)
        selectedPackageTypeName = defaultPkg.name
        selectedUnitName = defaultUnit.name
        contentsPerPackageStr = defaultContentsPerPackageStr(form, defaultPkg, defaultUnit)
        discardAfterOpeningDaysStr = if (form == MedicationForm.DROPS && defaultPkg == PackageType.BOTTLE) "28" else ""
        hasUserCustomizedPackage = false
    }

    val isSingleDoseEyeOrLiquid = selectedPackageType == PackageType.BOX_SINGLE_DOSE ||
        selectedPackageType == PackageType.SINGLE_DOSE_UNIT ||
        selectedUnit == StockUnit.CONTAINERS

    val isMultiDoseDrops = selectedForm == MedicationForm.DROPS && !isSingleDoseEyeOrLiquid && selectedUnit == StockUnit.DROPS

    val computedDoseQtyStr = when {
        isSingleDoseEyeOrLiquid -> containersPerDoseStr.ifBlank { "1" }
        isMultiDoseDrops -> when (eyeSite) {
            AdministrationSite.LEFT_EYE -> leftEyeDropsStr.ifBlank { "1" }
            AdministrationSite.RIGHT_EYE -> rightEyeDropsStr.ifBlank { "1" }
            AdministrationSite.BOTH_EYES -> {
                val l = runCatching { ScaledQuantity.fromDisplayString(leftEyeDropsStr) }.getOrDefault(ScaledQuantity.ofWhole(1))
                val r = runCatching { ScaledQuantity.fromDisplayString(rightEyeDropsStr) }.getOrDefault(ScaledQuantity.ofWhole(1))
                (l + r).toDisplayString()
            }
            else -> doseQuantity.ifBlank { "1" }
        }
        else -> doseQuantity.ifBlank { "1" }
    }

    val parsedContentsPerPkg = runCatching { ScaledQuantity.fromDisplayString(contentsPerPackageStr) }.getOrNull()
    val pkgCountRaw = initialPackagesCountStr.toIntOrNull()
    val pkgCountValid = pkgCountRaw != null && pkgCountRaw in 1..99
    val pkgCount = (pkgCountRaw ?: 1).coerceIn(1, 99)
    val parsedDiscardDays = discardAfterOpeningDaysStr.toIntOrNull()?.takeIf { it > 0 }
    val parsedDoseQty = runCatching { ScaledQuantity.fromDisplayString(computedDoseQtyStr) }.getOrNull()
    val parsedThreshold = runCatching { ScaledQuantity.fromDisplayString(lowStockThreshold) }.getOrNull()
    val isTubeCountOnly = selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.TUBE_COUNT_ONLY

    val derivedPrecision = when {
        isTubeCountOnly -> TrackingPrecision.UNKNOWN
        selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.GRAMS -> TrackingPrecision.APPROXIMATE
        selectedForm == MedicationForm.DROPS && isSingleDoseEyeOrLiquid -> TrackingPrecision.EXACT
        selectedForm.isEstimatedConsumption || selectedForm == MedicationForm.DROPS || (selectedPackageType == PackageType.BOTTLE && selectedUnit == StockUnit.DROPS) -> TrackingPrecision.ESTIMATED
        else -> TrackingPrecision.EXACT
    }

    val packagingConfig = PackagingConfig(
        packageType = selectedPackageType,
        consumptionUnit = selectedUnit,
        contentsPerPackage = parsedContentsPerPkg?.takeIf { it.isPositive() },
        discardAfterOpeningDays = parsedDiscardDays,
        gelTrackingMode = if (selectedForm == MedicationForm.TOPICAL_CREAM) gelMode else null,
        trackingPrecision = derivedPrecision
    )

    val administrationProfile = if (selectedForm == MedicationForm.DROPS) {
        when {
            isSingleDoseEyeOrLiquid -> AdministrationProfile(
                site = eyeSite,
                containersPerAdministration = runCatching { ScaledQuantity.fromDisplayString(containersPerDoseStr) }.getOrDefault(ScaledQuantity.ofWhole(1))
            )
            eyeSite == AdministrationSite.BOTH_EYES -> AdministrationProfile(
                site = AdministrationSite.BOTH_EYES,
                leftEyeQuantity = runCatching { ScaledQuantity.fromDisplayString(leftEyeDropsStr) }.getOrDefault(ScaledQuantity.ofWhole(1)),
                rightEyeQuantity = runCatching { ScaledQuantity.fromDisplayString(rightEyeDropsStr) }.getOrDefault(ScaledQuantity.ofWhole(1))
            )
            eyeSite == AdministrationSite.LEFT_EYE -> AdministrationProfile(
                site = AdministrationSite.LEFT_EYE,
                leftEyeQuantity = runCatching { ScaledQuantity.fromDisplayString(leftEyeDropsStr) }.getOrDefault(ScaledQuantity.ofWhole(1))
            )
            eyeSite == AdministrationSite.RIGHT_EYE -> AdministrationProfile(
                site = AdministrationSite.RIGHT_EYE,
                rightEyeQuantity = runCatching { ScaledQuantity.fromDisplayString(rightEyeDropsStr) }.getOrDefault(ScaledQuantity.ofWhole(1))
            )
            else -> AdministrationProfile(site = eyeSite)
        }
    } else {
        AdministrationProfile()
    }

    val computedTotalOpening = if (parsedContentsPerPkg != null && parsedContentsPerPkg.isPositive()) {
        ScaledQuantity(parsedContentsPerPkg.rawScaledValue * pkgCount.toLong())
    } else {
        runCatching { ScaledQuantity.fromDisplayString(openingStock) }.getOrDefault(ScaledQuantity.ZERO)
    }

    val effectiveDays = if (isSpecificWeekdays) selectedDaysList.toSet() else emptySet()
    val hasValidName = name.isNotBlank()
    val hasValidStrength = strength.isNotBlank()
    val hasValidPackageContents = isTubeCountOnly || (parsedContentsPerPkg != null && parsedContentsPerPkg.isPositive())
    val step0Valid = hasValidName && hasValidStrength && hasValidPackageContents

    val hasValidDateRange = endDateEpochDay == null || endDateEpochDay!! >= startDateEpochDay
    val hasValidDoseQty = parsedDoseQty != null && parsedDoseQty.isPositive()
    val hasValidScheduleTimes = selectedTimesList.isNotEmpty() && (!isSpecificWeekdays || effectiveDays.isNotEmpty())
    val step1Valid = hasValidScheduleTimes && hasValidDoseQty && hasValidDateRange

    val hasValidThreshold = parsedThreshold != null && !parsedThreshold.isNegative()
    val step2Valid = pkgCountValid && hasValidPackageContents && hasValidThreshold
    val canSubmit = step0Valid && step1Valid && step2Valid && !isSubmitting

    val completedSteps = buildSet {
        if (step0Valid && (currentStep > 0 || maxVisitedStep > 0)) add(0)
        if (step1Valid && maxVisitedStep >= 1 && currentStep != 1) add(1)
        if (step2Valid && maxVisitedStep >= 2 && currentStep != 2) add(2)
    }

    val showStep0Errors = (attemptedStepsMask and 1) != 0
    val showStep1Errors = (attemptedStepsMask and 2) != 0
    val showStep2Errors = (attemptedStepsMask and 4) != 0

    val missingRequirementHint = when (currentStep) {
        0 -> when {
            !hasValidName && !hasValidStrength -> "Required: Enter medication name and strength to continue."
            !hasValidName -> "Required: Enter the medication name."
            !hasValidStrength -> "Required: Enter the strength or concentration."
            !hasValidPackageContents -> "Required: Enter a positive usable capacity per package."
            else -> null
        }
        1 -> when {
            !hasValidScheduleTimes -> "Required: Select at least one scheduled dose time and active day."
            !hasValidDoseQty -> "Required: Dose quantity per administration must be greater than 0."
            !hasValidDateRange -> "Fix schedule dates: End date must be on or after start date."
            else -> null
        }
        else -> when {
            !step0Valid -> "Step 1 incomplete: Return to Identity to enter name, strength, and package capacity."
            !step1Valid -> "Step 2 incomplete: Return to Schedule to confirm times, dates, and dose quantity."
            !pkgCountValid -> "Required: Initial package count must be between 1 and 99."
            !hasValidThreshold -> "Required: Enter a non-negative low-stock threshold."
            else -> null
        }
    }

    val step0Scroll = rememberScrollState()
    val step1Scroll = rememberScrollState()
    val step2Scroll = rememberScrollState()

    val submitMedication = {
        if (canSubmit) {
            isSubmitting = true
            nuviloHaptics.perform(NuviloHapticEvent.RESTOCK_SAVED)
            val notesCombined = buildString {
                if (notes.isNotBlank()) append(notes.trim())
                if (instructions.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(instructions.trim())
                }
            }
            val openingStr = if (computedTotalOpening.isPositive()) {
                computedTotalOpening.toDisplayString()
            } else {
                openingStock.ifBlank { "0" }
            }
            val thresholdDays = lowStockDaysThresholdStr.toIntOrNull()?.takeIf { it > 0 }
            val cleanedLot = lotNumberStr.trim().ifBlank { null }

            when {
                onConfirmFull != null -> onConfirmFull(
                    name.trim(),
                    strength.trim(),
                    selectedForm,
                    selectedUnit,
                    openingStr,
                    lowStockThreshold.ifBlank { "7" },
                    selectedMode,
                    selectedTimesList.sorted(),
                    effectiveDays,
                    computedDoseQtyStr,
                    notesCombined,
                    selectedColorHex,
                    instructions.trim(),
                    packagingConfig,
                    administrationProfile,
                    pkgCount,
                    thresholdDays,
                    expiryDateEpochMs,
                    cleanedLot,
                    selectedRoute,
                    startDateEpochDay,
                    endDateEpochDay
                )
                onConfirmWithPackaging != null -> onConfirmWithPackaging(
                    name.trim(),
                    strength.trim(),
                    selectedForm,
                    selectedUnit,
                    openingStr,
                    lowStockThreshold.ifBlank { "7" },
                    selectedMode,
                    selectedTimesList.sorted(),
                    effectiveDays,
                    computedDoseQtyStr,
                    notesCombined,
                    selectedColorHex,
                    instructions.trim(),
                    packagingConfig,
                    administrationProfile,
                    pkgCount,
                    thresholdDays,
                    expiryDateEpochMs,
                    cleanedLot
                )
                else -> onConfirm(
                    name.trim(),
                    strength.trim(),
                    selectedForm,
                    selectedUnit,
                    openingStr,
                    lowStockThreshold.ifBlank { "7" },
                    selectedMode,
                    selectedTimesList.sorted(),
                    effectiveDays,
                    computedDoseQtyStr,
                    notesCombined,
                    selectedColorHex,
                    instructions.trim()
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogSystemBarsEffect()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AddMedicationTopBarAndProgressHeader(
                        title = "Add Medication",
                        subtitle = "Add the medication you want to track.",
                        currentStep = currentStep,
                        completedSteps = completedSteps,
                        onClose = onDismiss,
                        onStepSelected = { targetStep ->
                            nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                            currentStep = targetStep
                            if (targetStep > maxVisitedStep) maxVisitedStep = targetStep
                        }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                NoveloMotion.directionalHorizontalTransition(
                                    forward = targetState > initialState,
                                    motionEnabled = motionEnabled
                                )
                            },
                            label = "addMedicationWizardStep"
                        ) { step ->
                            when (step) {
                                0 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(step0Scroll)
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        WizardSectionCard(
                                            title = "Medication Identity",
                                            subtitle = "Enter the name and strength exactly as printed on your dispensing label."
                                        ) {
                                            OutlinedTextField(
                                                value = name,
                                                onValueChange = { name = it },
                                                label = { Text("Medication Name *") },
                                                placeholder = { Text("e.g. Hydro Forte, Atorvastatin") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Medication, contentDescription = null)
                                                },
                                                isError = showStep0Errors && !hasValidName,
                                                supportingText = {
                                                    if (showStep0Errors && !hasValidName) {
                                                        Text("Enter the medication name to continue.")
                                                    } else {
                                                        Text("Brand or generic name shown on your reminders.")
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(
                                                    capitalization = KeyboardCapitalization.Words,
                                                    imeAction = ImeAction.Next
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            OutlinedTextField(
                                                value = strength,
                                                onValueChange = { strength = it },
                                                label = { Text("Strength / Concentration *") },
                                                placeholder = { Text("e.g. 20 mg, 0.15% eye drops") },
                                                isError = showStep0Errors && !hasValidStrength,
                                                supportingText = {
                                                    if (showStep0Errors && !hasValidStrength) {
                                                        Text("Enter strength or concentration (e.g. 20 mg or 0.15%).")
                                                    } else {
                                                        Text("Examples: 20 mg tablets · 0.15% eye drops · 100 mcg/puff")
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }

                                        WizardSectionCard(
                                            title = "Dosage Form",
                                            subtitle = "Determines administration route and available packaging."
                                        ) {
                                            DosageFormSelectionGrid(
                                                selectedForm = selectedForm,
                                                onSelectForm = { form ->
                                                    if (form != selectedForm) {
                                                        nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                        if (hasUserCustomizedPackage) {
                                                            pendingFormSwitchName = form.name
                                                        } else {
                                                            applyFormDefaults(form)
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        WizardSectionCard(
                                            title = "How You Buy / Store It (Package)",
                                            subtitle = "Separates the physical container you buy from the dose units you take."
                                        ) {
                                            PackageTypeSelectionGrid(
                                                form = selectedForm,
                                                selectedPackageType = selectedPackageType,
                                                onSelectPackageType = { pkgType ->
                                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                    selectedPackageTypeName = pkgType.name
                                                    val nextUnit = when (pkgType) {
                                                        PackageType.BOX_SINGLE_DOSE, PackageType.SINGLE_DOSE_UNIT -> StockUnit.CONTAINERS
                                                        PackageType.BOTTLE -> if (selectedForm == MedicationForm.DROPS) StockUnit.DROPS else defaultStockUnitForForm(selectedForm)
                                                        else -> defaultStockUnitForForm(selectedForm)
                                                    }
                                                    selectedUnitName = nextUnit.name
                                                    contentsPerPackageStr = defaultContentsPerPackageStr(selectedForm, pkgType, nextUnit)
                                                    if (pkgType == PackageType.BOX_SINGLE_DOSE || pkgType == PackageType.SINGLE_DOSE_UNIT) {
                                                        discardAfterOpeningDaysStr = ""
                                                    } else if (selectedForm == MedicationForm.DROPS && pkgType == PackageType.BOTTLE && discardAfterOpeningDaysStr.isBlank()) {
                                                        discardAfterOpeningDaysStr = "28"
                                                    }
                                                    hasUserCustomizedPackage = true
                                                }
                                            )

                                            if (selectedForm == MedicationForm.TOPICAL_CREAM) {
                                                Text(
                                                    text = "Gel / Cream Tracking Mode",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    GelTrackingMode.entries.forEach { modeOpt ->
                                                        FilterChip(
                                                            selected = gelMode == modeOpt,
                                                            onClick = {
                                                                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                                gelModeName = modeOpt.name
                                                                val unitForMode = when (modeOpt) {
                                                                    GelTrackingMode.GRAMS -> StockUnit.GRAMS
                                                                    GelTrackingMode.ESTIMATED_APPLICATIONS, GelTrackingMode.TUBE_COUNT_ONLY -> StockUnit.APPLICATIONS
                                                                }
                                                                selectedUnitName = unitForMode.name
                                                                contentsPerPackageStr = defaultContentsPerPackageStr(selectedForm, selectedPackageType, unitForMode)
                                                                hasUserCustomizedPackage = true
                                                            },
                                                            label = { Text(modeOpt.displayName) },
                                                            colors = noveloWizardFilterChipColors(),
                                                            modifier = Modifier.heightIn(min = 48.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (!isTubeCountOnly) {
                                                val pkgSingular = selectedPackageType.displayName.lowercase()
                                                val contentsLabel = when {
                                                    selectedPackageType == PackageType.BOX_SINGLE_DOSE -> "Containers per box *"
                                                    selectedForm == MedicationForm.DROPS && selectedPackageType == PackageType.BOTTLE -> "Estimated drops per bottle *"
                                                    selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.GRAMS -> "Grams per $pkgSingular *"
                                                    selectedForm == MedicationForm.TOPICAL_CREAM -> "Estimated applications per $pkgSingular *"
                                                    else -> "Usable ${selectedUnit.abbreviation} per $pkgSingular *"
                                                }
                                                OutlinedTextField(
                                                    value = contentsPerPackageStr,
                                                    onValueChange = {
                                                        contentsPerPackageStr = it
                                                        hasUserCustomizedPackage = true
                                                    },
                                                    label = { Text(contentsLabel) },
                                                    isError = (showStep0Errors || contentsPerPackageStr.isNotBlank()) && !hasValidPackageContents,
                                                    supportingText = {
                                                        when {
                                                            !hasValidPackageContents -> Text("Enter a positive quantity greater than 0.")
                                                            selectedForm == MedicationForm.DROPS && selectedPackageType == PackageType.BOTTLE ->
                                                                Text("Nuvilo never guesses drops from ml because droppers vary.")
                                                            else -> Text("Total usable ${selectedUnit.displayName.lowercase()} inside one sealed $pkgSingular.")
                                                        }
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            if (selectedPackageType == PackageType.BOTTLE || selectedPackageType == PackageType.TUBE || selectedPackageType == PackageType.JAR) {
                                                Text(
                                                    text = "Opened-Package Discard Limit (Optional)",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf("28" to "28 days", "30" to "30 days", "90" to "90 days", "" to "None").forEach { (daysVal, label) ->
                                                        FilterChip(
                                                            selected = discardAfterOpeningDaysStr == daysVal,
                                                            onClick = {
                                                                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                                discardAfterOpeningDaysStr = daysVal
                                                                hasUserCustomizedPackage = true
                                                            },
                                                            label = { Text(label) },
                                                            colors = noveloWizardFilterChipColors(),
                                                            modifier = Modifier.heightIn(min = 48.dp)
                                                        )
                                                    }
                                                }
                                                OutlinedTextField(
                                                    value = discardAfterOpeningDaysStr,
                                                    onValueChange = {
                                                        discardAfterOpeningDaysStr = it.filter { ch -> ch.isDigit() }
                                                        hasUserCustomizedPackage = true
                                                    },
                                                    label = { Text("Discard After Opening (days, optional)") },
                                                    placeholder = { Text("e.g. 28 for preservative eye drops") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        WizardSectionCard(
                                            title = "Tag & Clinical Notes",
                                            subtitle = "Optional colour coding and safety notes."
                                        ) {
                                            Text(
                                                text = "Visual Accent Tag",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                MedicationAccentSwatches.forEach { (hex, label) ->
                                                    val swatchColor = parseAccentColor(hex, MaterialTheme.colorScheme.primary)
                                                    val isChosen = selectedColorHex.equals(hex, ignoreCase = true)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .clickable(
                                                                role = Role.RadioButton,
                                                                onClickLabel = "Select $label color tag"
                                                            ) {
                                                                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                                selectedColorHex = hex
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(34.dp)
                                                                .clip(CircleShape)
                                                                .background(swatchColor)
                                                                .border(
                                                                    width = if (isChosen) 2.5.dp else 1.dp,
                                                                    color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                                    shape = CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isChosen) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "$label selected",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = notes,
                                                onValueChange = { notes = it },
                                                label = { Text("Clinical Notes (Optional)") },
                                                placeholder = { Text("e.g. Wait 5 mins before other drops") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }

                                1 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(step1Scroll)
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        WizardSectionCard(
                                            title = "Section A · Administration",
                                            subtitle = "Configure route, site, and exact consumption per dose."
                                        ) {
                                            Text(
                                                text = "Administration Route",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                availableRoutesForForm(selectedForm).forEach { routeOpt ->
                                                    FilterChip(
                                                        selected = selectedRoute == routeOpt,
                                                        onClick = {
                                                            nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                            selectedRouteName = routeOpt.name
                                                        },
                                                        label = { Text(routeOpt.displayName) },
                                                        leadingIcon = if (selectedRoute == routeOpt) {
                                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                                        } else null,
                                                        colors = noveloWizardFilterChipColors(),
                                                        modifier = Modifier.heightIn(min = 48.dp)
                                                    )
                                                }
                                            }

                                            if (selectedForm == MedicationForm.DROPS) {
                                                Text(
                                                    text = "Treated Eye(s)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf(
                                                        AdministrationSite.LEFT_EYE,
                                                        AdministrationSite.RIGHT_EYE,
                                                        AdministrationSite.BOTH_EYES,
                                                        AdministrationSite.CUSTOM
                                                    ).forEach { siteOpt ->
                                                        FilterChip(
                                                            selected = eyeSite == siteOpt,
                                                            onClick = {
                                                                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                                eyeSiteName = siteOpt.name
                                                            },
                                                            label = { Text(siteOpt.displayName) },
                                                            colors = noveloWizardFilterChipColors(),
                                                            leadingIcon = {
                                                                Icon(
                                                                    imageVector = if (eyeSite == siteOpt) Icons.Default.Check else Icons.Default.Visibility,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            },
                                                            modifier = Modifier.heightIn(min = 48.dp)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = NoveloBannerShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        val eyeCallout = when {
                                                            isSingleDoseEyeOrLiquid ->
                                                                "Single-dose container mode: $computedDoseQtyStr container(s) consumed per administration for ${eyeSite.displayName}."
                                                            eyeSite == AdministrationSite.BOTH_EYES ->
                                                                "1 drop in each eye = $computedDoseQtyStr drops consumed per administration."
                                                            else ->
                                                                "${eyeSite.displayName} selected: $computedDoseQtyStr ${selectedUnit.abbreviation} consumed per administration."
                                                        }
                                                        Text(
                                                            text = eyeCallout,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }

                                                if (isSingleDoseEyeOrLiquid) {
                                                    OutlinedTextField(
                                                        value = containersPerDoseStr,
                                                        onValueChange = { containersPerDoseStr = it },
                                                        label = { Text("Single-Dose Containers Used Per Administration *") },
                                                        supportingText = {
                                                            Text("Treated eyes (${eyeSite.displayName}) are stored separately from vial consumption.")
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else if (eyeSite == AdministrationSite.BOTH_EYES) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedTextField(
                                                            value = leftEyeDropsStr,
                                                            onValueChange = { leftEyeDropsStr = it },
                                                            label = { Text("Left Eye Drops") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            singleLine = true,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        OutlinedTextField(
                                                            value = rightEyeDropsStr,
                                                            onValueChange = { rightEyeDropsStr = it },
                                                            label = { Text("Right Eye Drops") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            singleLine = true,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                } else if (eyeSite == AdministrationSite.LEFT_EYE) {
                                                    OutlinedTextField(
                                                        value = leftEyeDropsStr,
                                                        onValueChange = { leftEyeDropsStr = it },
                                                        label = { Text("Drops in Left Eye") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else if (eyeSite == AdministrationSite.RIGHT_EYE) {
                                                    OutlinedTextField(
                                                        value = rightEyeDropsStr,
                                                        onValueChange = { rightEyeDropsStr = it },
                                                        label = { Text("Drops in Right Eye") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }

                                        WizardSectionCard(
                                            title = "Section B · Schedule",
                                            subtitle = "Pick days, daily dose times, start date, and ongoing status."
                                        ) {
                                            ScheduleConfigurationContent(
                                                isSpecificWeekdays = isSpecificWeekdays,
                                                onScheduleModeChange = { isSpecificWeekdays = it },
                                                selectedDaysOfWeek = selectedDaysList.toSet(),
                                                onToggleDayOfWeek = { day ->
                                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                    selectedDaysList = if (selectedDaysList.contains(day)) {
                                                        if (selectedDaysList.size > 1) selectedDaysList - day else selectedDaysList
                                                    } else {
                                                        (selectedDaysList + day).sorted()
                                                    }
                                                },
                                                selectedTimesMinuteOfDay = selectedTimesList,
                                                onToggleTimeMinuteOfDay = { timeMin ->
                                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                    selectedTimesList = if (selectedTimesList.contains(timeMin)) {
                                                        if (selectedTimesList.size > 1) selectedTimesList - timeMin else selectedTimesList
                                                    } else {
                                                        (selectedTimesList + timeMin).sorted()
                                                    }
                                                },
                                                doseQuantityStr = computedDoseQtyStr,
                                                onDoseQuantityChange = { newQty ->
                                                    doseQuantity = newQty
                                                    if (isSingleDoseEyeOrLiquid) containersPerDoseStr = newQty
                                                },
                                                stockUnit = selectedUnit,
                                                instructions = instructions,
                                                onInstructionsChange = { instructions = it },
                                                startDateEpochDay = startDateEpochDay,
                                                onStartDateEpochDayChange = { newStart ->
                                                    startDateEpochDay = newStart
                                                },
                                                endDateEpochDay = endDateEpochDay,
                                                onEndDateEpochDayChange = { newEnd ->
                                                    endDateEpochDay = newEnd
                                                },
                                                showValidationErrors = showStep1Errors
                                            )
                                        }

                                        WizardSectionCard(
                                            title = "Section C · Dose Recording",
                                            subtitle = "Choose whether doses require explicit tap confirmation or auto-assumed tracking."
                                        ) {
                                            RecordingModeSelectorCard(
                                                selectedMode = selectedMode,
                                                onSelectMode = {
                                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                    selectedModeName = it.name
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }

                                else -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(step2Scroll)
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        WizardSectionCard(
                                            title = "Opening Physical Inventory",
                                            subtitle = "Enter the physical ${selectedPackageType.pluralName.lowercase()} you currently have on hand."
                                        ) {
                                            PackageQuantityStepper(
                                                label = "Number of ${selectedPackageType.pluralName}",
                                                value = pkgCount,
                                                unitLabel = if (pkgCount == 1) selectedPackageType.displayName.lowercase() else selectedPackageType.pluralName.lowercase(),
                                                onValueChange = { nextCount ->
                                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                    initialPackagesCountStr = nextCount.toString()
                                                }
                                            )

                                            OutlinedTextField(
                                                value = initialPackagesCountStr,
                                                onValueChange = { initialPackagesCountStr = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("Initial ${selectedPackageType.pluralName} *") },
                                                isError = (showStep2Errors || initialPackagesCountStr.isNotBlank()) && !pkgCountValid,
                                                supportingText = {
                                                    if (!pkgCountValid) {
                                                        Text("Enter between 1 and 99 physical packages.")
                                                    } else if (parsedContentsPerPkg != null && parsedContentsPerPkg.isPositive()) {
                                                        Text("Total opening stock: ${computedTotalOpening.toDisplayString()} ${selectedUnit.abbreviation}")
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = lowStockThreshold,
                                                    onValueChange = { lowStockThreshold = it },
                                                    label = { Text("Low Stock (${selectedUnit.abbreviation})") },
                                                    isError = !hasValidThreshold,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = lowStockDaysThresholdStr,
                                                    onValueChange = { lowStockDaysThresholdStr = it.filter { ch -> ch.isDigit() } },
                                                    label = { Text("Runway Alert (Days)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                            }
                                        }

                                        WizardSectionCard(
                                            title = "Expiry & Batch Tracking (Optional)",
                                            subtitle = "Set package expiry date for FEFO reminders."
                                        ) {
                                            val formattedExpiry = expiryDateEpochMs?.let { ms ->
                                                Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(WIZARD_DATE_FORMATTER)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { showExpiryDatePicker = true },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = formattedExpiry?.let { "Expiry: $it" } ?: "Select Expiry Date",
                                                        style = MaterialTheme.typography.labelLarge.tabular
                                                    )
                                                }
                                                if (expiryDateEpochMs != null) {
                                                    TextButton(
                                                        onClick = { expiryDateEpochMs = null },
                                                        modifier = Modifier.heightIn(min = 48.dp)
                                                    ) {
                                                        Text("Clear")
                                                    }
                                                }
                                            }

                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(3L to "+3m", 6L to "+6m", 12L to "+12m", 0L to "No expiry").forEach { (months, label) ->
                                                    val presetMs = if (months == 0L) null else LocalDate.now().plusMonths(months).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                                    FilterChip(
                                                        selected = (months == 0L && expiryDateEpochMs == null) ||
                                                            (presetMs != null && expiryDateEpochMs == presetMs),
                                                        onClick = {
                                                            nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                            expiryDateEpochMs = presetMs
                                                        },
                                                        label = { Text(label) },
                                                        colors = noveloWizardFilterChipColors(),
                                                        modifier = Modifier.heightIn(min = 48.dp)
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = lotNumberStr,
                                                onValueChange = { lotNumberStr = it },
                                                label = { Text("Batch / Lot Number (Optional)") },
                                                placeholder = { Text("e.g. BATCH-4092") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }

                                        val previewMed = Medication(
                                            id = "preview",
                                            name = name.ifBlank { "Medication" },
                                            strength = strength,
                                            form = selectedForm,
                                            stockUnit = selectedUnit,
                                            openingStock = computedTotalOpening,
                                            lowStockThreshold = parsedThreshold ?: ScaledQuantity.ofWhole(7),
                                            lowStockDaysThreshold = lowStockDaysThresholdStr.toIntOrNull(),
                                            packagingConfig = packagingConfig,
                                            administrationProfile = administrationProfile,
                                            administrationRoute = selectedRoute
                                        )
                                        val previewDoseQty = parsedDoseQty ?: ScaledQuantity.ofWhole(1)
                                        val previewSchedule = ScheduleDefinition(
                                            id = "sch-preview",
                                            medicationId = "preview",
                                            scheduleType = if (effectiveDays.isEmpty()) ScheduleType.DAILY else ScheduleType.SPECIFIC_WEEKDAYS,
                                            targetTimesMinuteOfDay = selectedTimesList,
                                            daysOfWeek = effectiveDays,
                                            doseQuantityPerOccurrence = previewDoseQty,
                                            startDateEpochDay = startDateEpochDay,
                                            endDateEpochDay = endDateEpochDay
                                        )
                                        val perPkgQty = parsedContentsPerPkg ?: ScaledQuantity.ZERO
                                        val previewLots = List(pkgCount) { index ->
                                            StockLot(
                                                id = "lot-prev-$index",
                                                medicationId = "preview",
                                                lotNumber = lotNumberStr.trim().ifBlank { null },
                                                expiryDateEpochMs = expiryDateEpochMs,
                                                initialQuantity = perPkgQty,
                                                remainingQuantity = perPkgQty,
                                                unit = selectedUnit,
                                                packageType = selectedPackageType,
                                                packageLabel = "${selectedPackageType.displayName} #${index + 1}",
                                                packageStatus = if (index == 0) PackageStatus.OPENED else PackageStatus.UNOPENED,
                                                openedAtEpochMs = if (index == 0) System.currentTimeMillis() else null,
                                                discardAfterOpeningDays = parsedDiscardDays
                                            )
                                        }
                                        val forecast = if (hasValidScheduleTimes && hasValidPackageContents) {
                                            SupplyForecastCalculator().calculateSupplyForecast(
                                                medication = previewMed,
                                                schedules = listOf(previewSchedule),
                                                lots = previewLots,
                                                ledgerTotalUsable = computedTotalOpening
                                            )
                                        } else {
                                            SupplyForecastCalculator().calculateSupplyForecast(
                                                medication = previewMed,
                                                schedules = emptyList(),
                                                lots = previewLots,
                                                ledgerTotalUsable = computedTotalOpening
                                            )
                                        }
                                        val dailyRateQty = if (selectedTimesList.isNotEmpty() && hasValidDoseQty) {
                                            ScaledQuantity(previewDoseQty.rawScaledValue * selectedTimesList.size.toLong())
                                        } else {
                                            null
                                        }
                                        val formattedExpiry = expiryDateEpochMs?.let { ms ->
                                            Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(WIZARD_DATE_FORMATTER)
                                        }

                                        LiveInventoryPreviewCard(
                                            medicationName = name,
                                            strength = strength,
                                            packageType = selectedPackageType,
                                            packageCount = pkgCount,
                                            perPackageQty = perPkgQty,
                                            totalOpeningQty = computedTotalOpening,
                                            consumptionUnit = selectedUnit,
                                            dosePerAdminQty = previewDoseQty,
                                            administrationsPerDay = selectedTimesList.size,
                                            dailyRateQty = dailyRateQty,
                                            forecast = forecast,
                                            discardAfterOpeningDays = parsedDiscardDays,
                                            expiryDateFormatted = formattedExpiry
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    WizardBottomActionBar(
                        currentStep = currentStep,
                        primaryLabel = if (currentStep < 2) "Continue" else "Save medication",
                        primaryEnabled = when (currentStep) {
                            2 -> canSubmit
                            else -> true
                        },
                        missingRequirementHint = when (currentStep) {
                            0 -> if (showStep0Errors) missingRequirementHint else null
                            1 -> if (showStep1Errors) missingRequirementHint else null
                            else -> missingRequirementHint
                        },
                        isSubmitting = isSubmitting,
                        onBack = {
                            if (currentStep > 0) {
                                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                currentStep -= 1
                            }
                        },
                        onCancel = onDismiss,
                        onPrimaryClick = {
                            when (currentStep) {
                                0 -> {
                                    attemptedStepsMask = attemptedStepsMask or 1
                                    if (step0Valid) {
                                        nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                        currentStep = 1
                                        if (1 > maxVisitedStep) maxVisitedStep = 1
                                    } else {
                                        nuviloHaptics.perform(NuviloHapticEvent.WARNING_CONFIRMED)
                                        scope.launch { step0Scroll.animateScrollTo(0) }
                                    }
                                }
                                1 -> {
                                    attemptedStepsMask = attemptedStepsMask or 2
                                    if (step1Valid) {
                                        nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                        currentStep = 2
                                        if (2 > maxVisitedStep) maxVisitedStep = 2
                                    } else {
                                        nuviloHaptics.perform(NuviloHapticEvent.WARNING_CONFIRMED)
                                        scope.launch { step1Scroll.animateScrollTo(0) }
                                    }
                                }
                                else -> {
                                    attemptedStepsMask = attemptedStepsMask or 4
                                    if (canSubmit) {
                                        submitMedication()
                                    } else {
                                        nuviloHaptics.perform(NuviloHapticEvent.WARNING_CONFIRMED)
                                        scope.launch { step2Scroll.animateScrollTo(0) }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    pendingFormSwitchName?.let { nextFormName ->
        val nextForm = runCatching { MedicationForm.valueOf(nextFormName) }.getOrNull()
        if (nextForm != null) {
            FormChangeConfirmationDialog(
                currentForm = selectedForm,
                pendingForm = nextForm,
                onDismissKeepCurrent = { pendingFormSwitchName = null },
                onConfirmSwitch = {
                    pendingFormSwitchName = null
                    applyFormDefaults(nextForm)
                }
            )
        }
    }

    if (showExpiryDatePicker) {
        val initialMillis = expiryDateEpochMs ?: LocalDate.now().plusMonths(6)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            expiryDateEpochMs = pickedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        showExpiryDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun RecordingModeSelectorCard(
    selectedMode: RecordingMode,
    onSelectMode: (RecordingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf(
            Triple(
                RecordingMode.CONFIRM,
                "Manual Confirmation (Recommended)",
                "Requires your explicit tap to record a dose as taken and deduct stock."
            ),
            Triple(
                RecordingMode.AUTO_ASSUME,
                "Auto-Assume Adherence",
                "Assumes doses are taken according to schedule; distinctly marked as Assumed."
            )
        )
        options.forEach { (mode, title, desc) ->
            val isSelected = selectedMode == mode
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NoveloCardShape)
                    .clickable(
                        onClick = { onSelectMode(mode) },
                        role = Role.RadioButton
                    ),
                shape = NoveloCardShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (mode == RecordingMode.CONFIRM) Icons.Default.VerifiedUser else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
