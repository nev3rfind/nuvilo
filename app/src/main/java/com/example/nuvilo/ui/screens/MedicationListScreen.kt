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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.domain.model.AdministrationProfile
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
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.MedicationItemCard
import com.example.nuvilo.ui.components.ScheduleConfigurationContent
import com.example.nuvilo.ui.components.TrackingPrecisionBadge
import com.example.nuvilo.ui.components.defaultStockUnitForForm
import com.example.nuvilo.ui.components.formatMinuteOfDay
import com.example.nuvilo.ui.components.parseAccentColor
import com.example.nuvilo.ui.haptics.LocalNuviloHaptics
import com.example.nuvilo.ui.haptics.NuviloHapticEvent
import com.example.nuvilo.ui.theme.MedicationAccentSwatches
import com.example.nuvilo.ui.theme.NoveloCardShape
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.NoveloPillShape
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.theme.tabular
import com.example.nuvilo.ui.viewmodel.MedicationViewModel
import kotlinx.coroutines.launch

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
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
                                    viewModel.setArchived(item.medication.id, !item.medication.isArchived)
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
            onConfirmWithPackaging = { name, strength, form, unit, opening, threshold, mode, times, daysOfWeek, doseQty, notes, colorHex, instructions, packagingConfig, administrationProfile, initialPackagesCount, lowStockDaysThreshold, expiryEpochMs, lotNumber ->
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
                    lotNumber = lotNumber
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
            onConfirmPackages = { packageCount, contentsPerPackage, consumptionUnit, packageType, discardDays, lotNumber, expiryEpochMs, notes ->
                viewModel.restockPackages(
                    medicationId = med.id,
                    packageCount = packageCount,
                    contentsPerPackage = contentsPerPackage,
                    consumptionUnit = consumptionUnit,
                    packageType = packageType,
                    discardAfterOpeningDays = discardDays,
                    lotNumber = lotNumber,
                    expiryDateEpochMs = expiryEpochMs,
                    notes = notes
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
                        .heightIn(min = 44.dp)
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
                            style = MaterialTheme.typography.labelLarge,
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

private fun defaultPackageTypeForForm(form: MedicationForm): PackageType = when (form) {
    MedicationForm.TABLET, MedicationForm.CAPSULE -> PackageType.BLISTER_PACK
    MedicationForm.LIQUID_ML, MedicationForm.DROPS -> PackageType.BOTTLE
    MedicationForm.INHALER_PUFFS -> PackageType.INHALER
    MedicationForm.TOPICAL_CREAM -> PackageType.TUBE
    MedicationForm.INJECTION -> PackageType.VIAL
    MedicationForm.PATCH -> PackageType.PATCH_BOX
    MedicationForm.OTHER -> PackageType.CUSTOM
}

private fun availablePackageTypesForForm(form: MedicationForm): List<PackageType> = when (form) {
    MedicationForm.DROPS -> listOf(PackageType.BOTTLE, PackageType.BOX_SINGLE_DOSE, PackageType.SINGLE_DOSE_UNIT)
    MedicationForm.TOPICAL_CREAM -> listOf(PackageType.TUBE, PackageType.JAR)
    MedicationForm.TABLET, MedicationForm.CAPSULE -> listOf(PackageType.BLISTER_PACK, PackageType.BOTTLE)
    MedicationForm.LIQUID_ML -> listOf(PackageType.BOTTLE, PackageType.SINGLE_DOSE_UNIT)
    MedicationForm.INHALER_PUFFS -> listOf(PackageType.INHALER)
    MedicationForm.INJECTION -> listOf(PackageType.VIAL, PackageType.CUSTOM)
    MedicationForm.PATCH -> listOf(PackageType.PATCH_BOX)
    MedicationForm.OTHER -> listOf(PackageType.CUSTOM, PackageType.BOTTLE, PackageType.BLISTER_PACK)
}

private fun defaultContentsPerPackageStr(form: MedicationForm, packageType: PackageType, unit: StockUnit): String = when {
    form == MedicationForm.DROPS && packageType == PackageType.BOTTLE -> "300"
    packageType == PackageType.BOX_SINGLE_DOSE -> "30"
    packageType == PackageType.SINGLE_DOSE_UNIT -> "1"
    form == MedicationForm.TOPICAL_CREAM && unit == StockUnit.APPLICATIONS -> "30"
    form == MedicationForm.TOPICAL_CREAM && unit == StockUnit.GRAMS -> "15"
    form == MedicationForm.LIQUID_ML -> "100"
    form == MedicationForm.INHALER_PUFFS -> "120"
    form == MedicationForm.PATCH -> "8"
    else -> "28"
}

/**
 * Progressive-disclosure 3-Step Add Medication Wizard separating:
 * Purchase Package -> Package Contents -> Administration Consumption -> Remaining Supply.
 *
 * Keeps legacy 13-arg [onConfirm] signature default-compatible while supporting rich
 * packaging & administration output via [onConfirmWithPackaging].
 */
@OptIn(ExperimentalLayoutApi::class)
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
    ) -> Unit)? = null
) {
    val nuviloHaptics = LocalNuviloHaptics.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    // Step 1 State: Identity, Form & Package Definition
    var name by rememberSaveable { mutableStateOf("") }
    var strength by rememberSaveable { mutableStateOf("") }
    var selectedFormName by rememberSaveable { mutableStateOf(MedicationForm.TABLET.name) }
    var selectedUnitName by rememberSaveable { mutableStateOf(StockUnit.TABLETS.name) }
    var selectedPackageTypeName by rememberSaveable { mutableStateOf(PackageType.BLISTER_PACK.name) }
    var contentsPerPackageStr by rememberSaveable { mutableStateOf("28") }
    var discardAfterOpeningDaysStr by rememberSaveable { mutableStateOf("") }
    var gelModeName by rememberSaveable { mutableStateOf(GelTrackingMode.ESTIMATED_APPLICATIONS.name) }
    var selectedColorHex by rememberSaveable { mutableStateOf("#005EB8") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Step 2 State: Schedule & Administration Site
    var isSpecificWeekdays by rememberSaveable { mutableStateOf(false) }
    var selectedDaysList by rememberSaveable { mutableStateOf(listOf(1, 3, 5)) }
    var selectedTimesList by rememberSaveable { mutableStateOf(listOf(480)) } // Default 08:00
    var doseQuantity by rememberSaveable { mutableStateOf("1") }
    var eyeSiteName by rememberSaveable { mutableStateOf(AdministrationSite.BOTH_EYES.name) }
    var leftEyeDropsStr by rememberSaveable { mutableStateOf("1") }
    var rightEyeDropsStr by rememberSaveable { mutableStateOf("1") }
    var containersPerDoseStr by rememberSaveable { mutableStateOf("1") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var selectedModeName by rememberSaveable { mutableStateOf(RecordingMode.CONFIRM.name) }

    // Step 3 State: Opening Inventory & Thresholds
    var initialPackagesCountStr by rememberSaveable { mutableStateOf("1") }
    var openingStock by rememberSaveable { mutableStateOf("28") }
    var lowStockThreshold by rememberSaveable { mutableStateOf("7") }
    var lowStockDaysThresholdStr by rememberSaveable { mutableStateOf("7") }
    var lotNumberStr by rememberSaveable { mutableStateOf("") }
    var expiryDaysAheadStr by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val selectedForm = runCatching { MedicationForm.valueOf(selectedFormName) }.getOrDefault(MedicationForm.TABLET)
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
    val pkgCount = (initialPackagesCountStr.toIntOrNull() ?: 1).coerceAtLeast(1)
    val parsedDiscardDays = discardAfterOpeningDaysStr.toIntOrNull()?.takeIf { it > 0 }
    val derivedPrecision = when {
        selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.TUBE_COUNT_ONLY -> TrackingPrecision.UNKNOWN
        selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.GRAMS -> TrackingPrecision.APPROXIMATE
        selectedForm == MedicationForm.DROPS && isSingleDoseEyeOrLiquid -> TrackingPrecision.EXACT
        selectedForm.isEstimatedConsumption || selectedForm == MedicationForm.DROPS || selectedPackageType == PackageType.BOTTLE && selectedUnit == StockUnit.DROPS -> TrackingPrecision.ESTIMATED
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
    val hasValidIdentity = name.isNotBlank() && strength.isNotBlank()
    val hasValidPackageContents = when {
        selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.TUBE_COUNT_ONLY -> true
        else -> parsedContentsPerPkg != null && parsedContentsPerPkg.isPositive()
    }
    val hasValidSchedule = selectedTimesList.isNotEmpty() && (!isSpecificWeekdays || effectiveDays.isNotEmpty())
    val canSubmit = hasValidIdentity && hasValidPackageContents && hasValidSchedule && !isSubmitting

    val semantic = MaterialTheme.noveloColors

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Medication",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Step ${currentStep + 1} of 3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Interactive 3-Step Indicator Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val steps = listOf(
                        0 to "1. Identity",
                        1 to "2. Schedule",
                        2 to "3. Stock"
                    )
                    steps.forEach { (idx, stepTitle) ->
                        val isActive = currentStep == idx
                        val isDone = idx < currentStep
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 36.dp)
                                .clip(NoveloPillShape)
                                .clickable {
                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                    currentStep = idx
                                },
                            shape = NoveloPillShape,
                            color = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isDone -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = stepTitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isActive -> MaterialTheme.colorScheme.onPrimary
                                    isDone -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { NoveloMotion.directionalHorizontalTransition(targetState > initialState) },
                label = "addMedicationWizardStep"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (step) {
                        0 -> {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Medication Name *") },
                                placeholder = { Text("e.g. Hydro Forte, Atorvastatin") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = strength,
                                onValueChange = { strength = it },
                                label = { Text("Strength / Concentration *") },
                                placeholder = { Text("e.g. 0.3%, 20mg, 100mcg/puff") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(
                                text = "Dosage Form",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    MedicationForm.TABLET,
                                    MedicationForm.CAPSULE,
                                    MedicationForm.LIQUID_ML,
                                    MedicationForm.INHALER_PUFFS,
                                    MedicationForm.DROPS,
                                    MedicationForm.TOPICAL_CREAM,
                                    MedicationForm.INJECTION,
                                    MedicationForm.PATCH
                                ).forEach { form ->
                                    FilterChip(
                                        selected = selectedForm == form,
                                        onClick = {
                                            nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                            selectedFormName = form.name
                                            val defaultPkg = defaultPackageTypeForForm(form)
                                            val defaultUnit = defaultStockUnitForForm(form)
                                            selectedPackageTypeName = defaultPkg.name
                                            selectedUnitName = defaultUnit.name
                                            contentsPerPackageStr = defaultContentsPerPackageStr(form, defaultPkg, defaultUnit)
                                            discardAfterOpeningDaysStr = if (form == MedicationForm.DROPS && defaultPkg == PackageType.BOTTLE) "28" else ""
                                        },
                                        label = { Text(form.displayName) },
                                        leadingIcon = if (selectedForm == form) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                        } else null
                                    )
                                }
                            }

                            // Contextual Package Selection
                            val pkgOptions = availablePackageTypesForForm(selectedForm)
                            Text(
                                text = "How You Buy / Store It (Package)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pkgOptions.forEach { pkgType ->
                                    FilterChip(
                                        selected = selectedPackageType == pkgType,
                                        onClick = {
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
                                        },
                                        label = { Text(pkgType.displayName) }
                                    )
                                }
                            }

                            // Gel / Cream Tracking Mode Selector
                            if (selectedForm == MedicationForm.TOPICAL_CREAM) {
                                Text(
                                    text = "Gel / Cream Tracking Mode",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                            },
                                            label = { Text(modeOpt.displayName) }
                                        )
                                    }
                                }
                            }

                            // Usable Contents per Package (Never guessed from ml!)
                            if (!(selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.TUBE_COUNT_ONLY)) {
                                val pkgSingular = selectedPackageType.displayName.lowercase()
                                val contentsLabel = when {
                                    selectedPackageType == PackageType.BOX_SINGLE_DOSE -> "Containers per box *"
                                    selectedForm == MedicationForm.DROPS && selectedPackageType == PackageType.BOTTLE -> "Usable drops per bottle (leaflet/estimate) *"
                                    selectedForm == MedicationForm.TOPICAL_CREAM && gelMode == GelTrackingMode.GRAMS -> "Grams per $pkgSingular *"
                                    selectedForm == MedicationForm.TOPICAL_CREAM -> "Estimated applications per $pkgSingular *"
                                    else -> "Usable ${selectedUnit.abbreviation} per $pkgSingular *"
                                }
                                OutlinedTextField(
                                    value = contentsPerPackageStr,
                                    onValueChange = { contentsPerPackageStr = it },
                                    label = { Text(contentsLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (selectedForm == MedicationForm.DROPS && selectedPackageType == PackageType.BOTTLE) {
                                    Text(
                                        text = "Nuvilo never converts ml to drops automatically because bottle droppers vary.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (selectedPackageType == PackageType.BOTTLE || selectedPackageType == PackageType.TUBE || selectedPackageType == PackageType.JAR) {
                                OutlinedTextField(
                                    value = discardAfterOpeningDaysStr,
                                    onValueChange = { discardAfterOpeningDaysStr = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Discard After Opening (days, optional)") },
                                    placeholder = { Text("e.g. 28 for eye drops") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text(
                                text = "Visual Accent Tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MedicationAccentSwatches.forEach { (hex, label) ->
                                    val swatchColor = parseAccentColor(hex, MaterialTheme.colorScheme.primary)
                                    val isChosen = selectedColorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (isChosen) 2.5.dp else 1.dp,
                                                color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                onClick = {
                                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                    selectedColorHex = hex
                                                },
                                                onClickLabel = "Select $label color tag"
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChosen) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = label,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
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

                        1 -> {
                            // Ophthalmic Administration Controls when form == DROPS
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
                                            label = { Text(siteOpt.displayName) }
                                        )
                                    }
                                }

                                if (isSingleDoseEyeOrLiquid) {
                                    OutlinedTextField(
                                        value = containersPerDoseStr,
                                        onValueChange = { containersPerDoseStr = it },
                                        label = { Text("Single-Dose Containers Used Per Administration *") },
                                        supportingText = {
                                            Text("Treated eyes (${eyeSite.displayName}) are recorded separately from container count (e.g. 1 vial used for both eyes).")
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
                                            label = { Text("Left Eye (drops)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = rightEyeDropsStr,
                                            onValueChange = { rightEyeDropsStr = it },
                                            label = { Text("Right Eye (drops)") },
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
                                onInstructionsChange = { instructions = it }
                            )

                            Text(
                                text = "Adherence Recording Mode",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            RecordingModeSelectorCard(
                                selectedMode = selectedMode,
                                onSelectMode = {
                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                    selectedModeName = it.name
                                }
                            )
                        }

                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = initialPackagesCountStr,
                                    onValueChange = { initialPackagesCountStr = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Initial ${selectedPackageType.pluralName}") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = lowStockThreshold,
                                    onValueChange = { lowStockThreshold = it },
                                    label = { Text("Low Alert (${selectedUnit.abbreviation})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = lowStockDaysThresholdStr,
                                    onValueChange = { lowStockDaysThresholdStr = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Alert Days Runway") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = expiryDaysAheadStr,
                                    onValueChange = { expiryDaysAheadStr = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Expiry (days, opt)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = lotNumberStr,
                                onValueChange = { lotNumberStr = it },
                                label = { Text("Batch / Lot Number (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Live Runway Preview Card
                            val previewMed = Medication(
                                id = "preview",
                                name = name.ifBlank { "Medication" },
                                strength = strength,
                                form = selectedForm,
                                stockUnit = selectedUnit,
                                openingStock = computedTotalOpening,
                                lowStockThreshold = runCatching { ScaledQuantity.fromDisplayString(lowStockThreshold) }.getOrDefault(ScaledQuantity.ofWhole(7)),
                                lowStockDaysThreshold = lowStockDaysThresholdStr.toIntOrNull(),
                                packagingConfig = packagingConfig,
                                administrationProfile = administrationProfile
                            )
                            val previewDoseQty = runCatching { ScaledQuantity.fromDisplayString(computedDoseQtyStr) }.getOrDefault(ScaledQuantity.ofWhole(1))
                            val previewSchedule = ScheduleDefinition(
                                id = "sch-preview",
                                medicationId = "preview",
                                scheduleType = if (effectiveDays.isEmpty()) ScheduleType.DAILY else ScheduleType.SPECIFIC_WEEKDAYS,
                                targetTimesMinuteOfDay = selectedTimesList,
                                daysOfWeek = effectiveDays,
                                doseQuantityPerOccurrence = previewDoseQty,
                                startDateEpochDay = java.time.LocalDate.now().toEpochDay()
                            )
                            val perPkgQty = parsedContentsPerPkg ?: ScaledQuantity.ZERO
                            val previewExpiryMs = expiryDaysAheadStr.toLongOrNull()?.takeIf { it > 0L }?.let {
                                System.currentTimeMillis() + it * 86_400_000L
                            }
                            val previewLots = List(pkgCount) { index ->
                                StockLot(
                                    id = "lot-prev-$index",
                                    medicationId = "preview",
                                    lotNumber = lotNumberStr.trim().ifBlank { null },
                                    expiryDateEpochMs = previewExpiryMs,
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
                            val forecast = SupplyForecastCalculator().calculateSupplyForecast(
                                medication = previewMed,
                                schedules = listOf(previewSchedule),
                                lots = previewLots,
                                ledgerTotalUsable = computedTotalOpening
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = NoveloCardShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Regimen Summary",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TrackingPrecisionBadge(precision = forecast.precision)
                                    }
                                    Text(
                                        text = "${name.ifBlank { "Unnamed Medication" }} · ${strength.ifBlank { "No strength" }} (${selectedPackageType.displayName})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val pkgBreakdown = if (perPkgQty.isPositive()) {
                                        "$pkgCount ${if (pkgCount == 1) selectedPackageType.displayName.lowercase() else selectedPackageType.pluralName.lowercase()} × ${perPkgQty.toDisplayString()} = ${computedTotalOpening.toDisplayString()} ${selectedUnit.abbreviation}"
                                    } else {
                                        "$pkgCount ${selectedPackageType.pluralName.lowercase()} tracked"
                                    }
                                    Text(
                                        text = pkgBreakdown,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val runwayStr = forecast.daysOfSupply?.let { "Expected supply runway: ~$it day(s)" }
                                        ?: "Expected supply runway: Add a schedule & capacity to estimate"
                                    Text(
                                        text = runwayStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (parsedDiscardDays != null) {
                                        Text(
                                            text = "Opened pack safety rule: discard $parsedDiscardDays days after opening (Bottle #1 starts opened).",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semantic.assumed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentStep < 2) {
                    FilledTonalButton(
                        onClick = {
                            nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                            currentStep += 1
                        }
                    ) {
                        Text("Next Step")
                    }
                }
                Button(
                    onClick = {
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
                            val expiryEpochMs = expiryDaysAheadStr.toLongOrNull()?.takeIf { it > 0L }?.let {
                                System.currentTimeMillis() + it * 86_400_000L
                            }
                            val thresholdDays = lowStockDaysThresholdStr.toIntOrNull()?.takeIf { it > 0 }
                            val cleanedLot = lotNumberStr.trim().ifBlank { null }

                            if (onConfirmWithPackaging != null) {
                                onConfirmWithPackaging(
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
                                    expiryEpochMs,
                                    cleanedLot
                                )
                            } else {
                                onConfirm(
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
                    },
                    enabled = canSubmit
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = {
                        nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                        currentStep -= 1
                    }) {
                        Text("Back")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
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
