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
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.MedicationItemCard
import com.example.nuvilo.ui.components.ScheduleConfigurationContent
import com.example.nuvilo.ui.components.defaultStockUnitForForm
import com.example.nuvilo.ui.components.formatMinuteOfDay
import com.example.nuvilo.ui.components.parseAccentColor
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
            onConfirm = { name, strength, form, unit, opening, threshold, mode, times, daysOfWeek, doseQty, notes, colorHex, instructions ->
                viewModel.createMedication(
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
                    instructions = instructions
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
            onConfirm = { qty, lotNumber, expiryEpochMs, notes ->
                viewModel.restockMedication(
                    medicationId = med.id,
                    quantity = qty,
                    unit = med.stockUnit,
                    lotNumber = lotNumber,
                    expiryDateEpochMs = expiryEpochMs,
                    notes = notes
                )
                medicationToRestock = null
                scope.launch {
                    snackbarHostState.showSnackbar("Restocked ${qty.toDisplayString()} ${med.stockUnit.abbreviation} for ${med.name}")
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

/**
 * Progressive-disclosure 3-Step Add Medication Wizard.
 * - Step 1: Medication Identity, Dosage Form (with smart StockUnit defaulting), Accent Color, & Notes
 * - Step 2: Schedule Frequency (Daily vs Specific Weekdays), Custom/Preset Dose Times, Dose Quantity, & Recording Mode
 * - Step 3: Opening Stock, Low-Stock Threshold, Estimated Consumption Notice, & Regimen Summary
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
    ) -> Unit
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    // Step 1 State
    var name by rememberSaveable { mutableStateOf("") }
    var strength by rememberSaveable { mutableStateOf("") }
    var selectedFormName by rememberSaveable { mutableStateOf(MedicationForm.TABLET.name) }
    var selectedUnitName by rememberSaveable { mutableStateOf(StockUnit.TABLETS.name) }
    var selectedColorHex by rememberSaveable { mutableStateOf("#005EB8") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Step 2 State
    var isSpecificWeekdays by rememberSaveable { mutableStateOf(false) }
    var selectedDaysList by rememberSaveable { mutableStateOf(listOf(1, 3, 5)) }
    var selectedTimesList by rememberSaveable { mutableStateOf(listOf(480)) } // Default 08:00
    var doseQuantity by rememberSaveable { mutableStateOf("1") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var selectedModeName by rememberSaveable { mutableStateOf(RecordingMode.CONFIRM.name) }

    // Step 3 State
    var openingStock by rememberSaveable { mutableStateOf("28") }
    var lowStockThreshold by rememberSaveable { mutableStateOf("7") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val selectedForm = runCatching { MedicationForm.valueOf(selectedFormName) }.getOrDefault(MedicationForm.TABLET)
    val selectedUnit = runCatching { StockUnit.valueOf(selectedUnitName) }.getOrDefault(StockUnit.TABLETS)
    val selectedMode = runCatching { RecordingMode.valueOf(selectedModeName) }.getOrDefault(RecordingMode.CONFIRM)

    val effectiveDays = if (isSpecificWeekdays) selectedDaysList.toSet() else emptySet()
    val hasValidIdentity = name.isNotBlank() && strength.isNotBlank()
    val hasValidSchedule = selectedTimesList.isNotEmpty() && (!isSpecificWeekdays || effectiveDays.isNotEmpty())
    val canSubmit = hasValidIdentity && hasValidSchedule && !isSubmitting

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
                                .clip(NoveloPillShape)
                                .clickable { currentStep = idx },
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (step) {
                        0 -> {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Medication Name *") },
                                placeholder = { Text("e.g. Atorvastatin, Lisinopril") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = strength,
                                onValueChange = { strength = it },
                                label = { Text("Strength / Concentration *") },
                                placeholder = { Text("e.g. 20mg, 500mg, 100mcg/puff") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(
                                text = "Dosage Form (auto-matches stock unit)",
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
                                            selectedFormName = form.name
                                            selectedUnitName = defaultStockUnitForForm(form).name
                                        },
                                        label = { Text(form.displayName) },
                                        leadingIcon = if (selectedForm == form) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                        } else null
                                    )
                                }
                            }

                            Text(
                                text = "Stock Counting Unit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StockUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = selectedUnit == unit,
                                        onClick = { selectedUnitName = unit.name },
                                        label = { Text("${unit.displayName} (${unit.abbreviation})") }
                                    )
                                }
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
                                                onClick = { selectedColorHex = hex },
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
                                placeholder = { Text("e.g. Prescribed for blood pressure") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        1 -> {
                            ScheduleConfigurationContent(
                                isSpecificWeekdays = isSpecificWeekdays,
                                onScheduleModeChange = { isSpecificWeekdays = it },
                                selectedDaysOfWeek = selectedDaysList.toSet(),
                                onToggleDayOfWeek = { day ->
                                    selectedDaysList = if (selectedDaysList.contains(day)) {
                                        if (selectedDaysList.size > 1) selectedDaysList - day else selectedDaysList
                                    } else {
                                        (selectedDaysList + day).sorted()
                                    }
                                },
                                selectedTimesMinuteOfDay = selectedTimesList,
                                onToggleTimeMinuteOfDay = { timeMin ->
                                    selectedTimesList = if (selectedTimesList.contains(timeMin)) {
                                        if (selectedTimesList.size > 1) selectedTimesList - timeMin else selectedTimesList
                                    } else {
                                        (selectedTimesList + timeMin).sorted()
                                    }
                                },
                                doseQuantityStr = doseQuantity,
                                onDoseQuantityChange = { doseQuantity = it },
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
                                onSelectMode = { selectedModeName = it.name }
                            )
                        }

                        else -> {
                            if (selectedForm.isEstimatedConsumption) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = semantic.warningContainer),
                                    border = BorderStroke(1.dp, semantic.warningBorder),
                                    shape = NoveloCardShape
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = semantic.warning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${selectedForm.displayName} supplies are tracked as ESTIMATED balances because physical actuation/application volume can vary.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = semantic.warning
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = openingStock,
                                    onValueChange = { openingStock = it },
                                    label = { Text("Opening Stock (${selectedUnit.abbreviation})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

                            // Regimen Review Summary Card
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
                                    Text(
                                        text = "Regimen Summary",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${name.ifBlank { "Unnamed Medication" }} · ${strength.ifBlank { "No strength" }} (${selectedForm.displayName})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val timesSummary = selectedTimesList.sorted().joinToString(", ") { formatMinuteOfDay(it) }
                                    val freqLabel = if (isSpecificWeekdays) "${effectiveDays.size} weekday(s)/week" else "Every day"
                                    Text(
                                        text = "Schedule: $freqLabel at $timesSummary ($doseQuantity ${selectedUnit.abbreviation} per dose)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Opening Ledger Balance: $openingStock ${selectedUnit.abbreviation} (Low-stock alert at $lowStockThreshold ${selectedUnit.abbreviation})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                        onClick = { currentStep += 1 }
                    ) {
                        Text("Next Step")
                    }
                }
                Button(
                    onClick = {
                        if (canSubmit) {
                            isSubmitting = true
                            val notesCombined = buildString {
                                if (notes.isNotBlank()) append(notes.trim())
                                if (instructions.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(instructions.trim())
                                }
                            }
                            onConfirm(
                                name.trim(),
                                strength.trim(),
                                selectedForm,
                                selectedUnit,
                                openingStock.ifBlank { "0" },
                                lowStockThreshold.ifBlank { "7" },
                                selectedMode,
                                selectedTimesList.sorted(),
                                effectiveDays,
                                doseQuantity.ifBlank { "1" },
                                notesCombined,
                                selectedColorHex,
                                instructions.trim()
                            )
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
                    OutlinedButton(onClick = { currentStep -= 1 }) {
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
