package com.example.nuvilo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.domain.adherence.AdherenceSummary
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEvent
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.components.DoseStatusBadge
import com.example.nuvilo.ui.components.ScheduleConfigurationContent
import com.example.nuvilo.ui.components.StockBadge
import com.example.nuvilo.ui.components.defaultStockUnitForForm
import com.example.nuvilo.ui.components.formatMinuteOfDay
import com.example.nuvilo.ui.components.parseAccentColor
import com.example.nuvilo.ui.theme.MedicationAccentSwatches
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.viewmodel.MedicationDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val DOSE_ROW_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm")
private val MOVEMENT_ROW_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm")
private val AUDIT_TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    viewModel: MedicationDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToDoseHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRestockDialog by rememberSaveable { mutableStateOf(false) }
    var showAdjustDialog by rememberSaveable { mutableStateOf(false) }
    var showEditMedicationDialog by rememberSaveable { mutableStateOf(false) }
    var showEditScheduleDialog by rememberSaveable { mutableStateOf(false) }
    var doseToCorrect by remember { mutableStateOf<DoseOccurrence?>(null) }
    var showPrescriptionFormDialog by rememberSaveable { mutableStateOf(false) }
    var prescriptionToEdit by remember { mutableStateOf<Prescription?>(null) }
    var showPrescriptionHistoryDialog by rememberSaveable { mutableStateOf(false) }

    var lotsExpanded by rememberSaveable { mutableStateOf(false) }
    var ledgerExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.actionSuccessMessage, uiState.errorMessage) {
        uiState.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val med = uiState.medication

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = med?.name ?: "Medication Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (med != null) {
                            Text(
                                text = "${med.strength} · ${med.form.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (med != null) {
                        IconButton(onClick = { showEditMedicationDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit medication details"
                            )
                        }
                        IconButton(onClick = { viewModel.setArchived(!med.isArchived) }) {
                            Icon(
                                imageVector = if (med.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (med.isArchived) "Restore Medication" else "Archive Medication"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            med == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Medication not found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                val activeFefoLotId = uiState.lots
                    .filter { it.remainingQuantity.isPositive() }
                    .minWithOrNull(compareBy<StockLot> { it.expiryDateEpochMs ?: Long.MAX_VALUE }.thenBy { it.createdAtEpochMs })
                    ?.id

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp)
                ) {
                    // 1. Unified Hero Medication & Stock Runway Card
                    item(key = "hero_stock_card", contentType = "hero_card") {
                        MedicationHeroAndStockCard(
                            medication = med,
                            balance = uiState.stockBalance,
                            adherenceSummary = uiState.medicationAdherenceSummary,
                            onEditMedicationClick = { showEditMedicationDialog = true },
                            onRestockClick = { showRestockDialog = true },
                            onAdjustClick = { showAdjustDialog = true }
                        )
                    }

                    // 2. Schedule Summary Card with Inline Edit Action
                    item(key = "schedule_card", contentType = "schedule_card") {
                        ScheduleSummaryCard(
                            schedules = uiState.schedules,
                            unit = med.stockUnit,
                            onEditScheduleClick = { showEditScheduleDialog = true }
                        )
                    }

                    // 3. Prescription & Repeat Overview Section
                    item(key = "prescription_section", contentType = "prescription_card") {
                        PrescriptionOverviewSection(
                            prescriptions = uiState.prescriptions,
                            stockUnit = med.stockUnit,
                            onAddPrescriptionClick = {
                                prescriptionToEdit = null
                                showPrescriptionFormDialog = true
                            },
                            onEditPrescriptionClick = { rx ->
                                prescriptionToEdit = rx
                                showPrescriptionFormDialog = true
                            },
                            onRequestRepeatClick = { rx ->
                                viewModel.requestPrescriptionRepeat(rx.id)
                            },
                            onCollectRepeatClick = { rx ->
                                viewModel.collectPrescriptionRepeat(rx.id)
                            },
                            onArchivePrescriptionClick = { rx ->
                                viewModel.archivePrescription(rx.id)
                            },
                            onViewHistoryClick = {
                                showPrescriptionHistoryDialog = true
                            }
                        )
                    }

                    // 4. Recent Dose History & Audit Section (Top 5 preview + Full History button)
                    item(key = "recent_doses_section", contentType = "recent_doses") {
                        RecentDosesPreviewCard(
                            recentDoses = uiState.recentDoses.take(5),
                            totalDoseCount = uiState.recentDoses.size,
                            onNavigateToDoseHistory = onNavigateToDoseHistory,
                            onCorrectDose = { doseToCorrect = it }
                        )
                    }

                    // 5. Collapsible Batch Lots (FEFO Allocation) Section
                    item(key = "batch_lots_section", contentType = "collapsible_lots") {
                        CollapsibleBatchLotsSection(
                            lots = uiState.lots,
                            activeFefoLotId = activeFefoLotId,
                            expanded = lotsExpanded,
                            onToggleExpand = { lotsExpanded = !lotsExpanded },
                            onAddBatchClick = { showRestockDialog = true }
                        )
                    }

                    // 6. Collapsible Stock Ledger Audit Trail Section
                    item(key = "stock_ledger_section", contentType = "collapsible_ledger") {
                        CollapsibleStockLedgerSection(
                            movements = uiState.recentMovements,
                            expanded = ledgerExpanded,
                            onToggleExpand = { ledgerExpanded = !ledgerExpanded }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showEditMedicationDialog && med != null) {
        EditMedicationDialog(
            medication = med,
            onDismiss = { showEditMedicationDialog = false },
            onConfirm = { name, strength, form, unit, threshold, mode, notes, colorHex ->
                viewModel.updateMedication(
                    name = name,
                    strength = strength,
                    form = form,
                    unit = unit,
                    lowStockThresholdStr = threshold,
                    recordingMode = mode,
                    notes = notes,
                    colorTagHex = colorHex
                )
                showEditMedicationDialog = false
            }
        )
    }

    if (showEditScheduleDialog && med != null) {
        EditScheduleDialog(
            existingSchedule = uiState.schedules.firstOrNull(),
            stockUnit = med.stockUnit,
            onDismiss = { showEditScheduleDialog = false },
            onConfirm = { times, daysOfWeek, doseQty, instructions ->
                viewModel.saveSchedule(
                    existingSchedule = uiState.schedules.firstOrNull(),
                    targetTimesMinuteOfDay = times,
                    daysOfWeek = daysOfWeek,
                    doseQuantityStr = doseQty,
                    instructions = instructions
                )
                showEditScheduleDialog = false
            }
        )
    }

    if (showRestockDialog && med != null) {
        RestockDialog(
            medication = med,
            currentBalance = uiState.stockBalance?.currentBalance ?: ScaledQuantity.ZERO,
            onDismiss = { showRestockDialog = false },
            onConfirm = { qty, lotNumber, expiryEpoch, notes ->
                viewModel.restock(qty, lotNumber, expiryEpoch, notes)
                showRestockDialog = false
            }
        )
    }

    if (showAdjustDialog && med != null) {
        val currentBal = uiState.stockBalance?.currentBalance ?: ScaledQuantity.ZERO
        AdjustStockDialog(
            medication = med,
            currentBalance = currentBal,
            lots = uiState.lots,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { targetBal, reason, targetLotId ->
                viewModel.adjustStock(targetBal, reason, targetLotId)
                showAdjustDialog = false
            }
        )
    }

    val currentDoseToCorrect = doseToCorrect
    if (currentDoseToCorrect != null && med != null) {
        CorrectDoseDialog(
            dose = currentDoseToCorrect,
            medicationName = med.name,
            onDismiss = { doseToCorrect = null },
            onConfirm = { newStatus, reason ->
                viewModel.correctDose(currentDoseToCorrect.occurrenceId, newStatus, reason)
                doseToCorrect = null
            }
        )
    }

    if (showPrescriptionFormDialog && med != null) {
        AddEditPrescriptionDialog(
            existingPrescription = prescriptionToEdit,
            stockUnit = med.stockUnit,
            onDismiss = {
                showPrescriptionFormDialog = false
                prescriptionToEdit = null
            },
            onConfirm = { ref, issueDay, expiryDay, qty, repeatsAuth, repeatsRem, pharmacy, notes ->
                viewModel.savePrescription(
                    existingPrescription = prescriptionToEdit,
                    referenceNumber = ref,
                    issueDateEpochDay = issueDay,
                    expiryDateEpochDay = expiryDay,
                    quantityPrescribed = qty,
                    repeatsAuthorised = repeatsAuth,
                    repeatsRemaining = repeatsRem,
                    pharmacyName = pharmacy,
                    notes = notes
                )
                showPrescriptionFormDialog = false
                prescriptionToEdit = null
            }
        )
    }

    if (showPrescriptionHistoryDialog) {
        PrescriptionHistoryDialog(
            prescriptions = uiState.prescriptions,
            events = uiState.prescriptionEvents,
            onDismiss = { showPrescriptionHistoryDialog = false }
        )
    }
}

@Composable
private fun MedicationHeroAndStockCard(
    medication: Medication,
    balance: StockBalance?,
    adherenceSummary: AdherenceSummary?,
    onEditMedicationClick: () -> Unit,
    onRestockClick: () -> Unit,
    onAdjustClick: () -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    val accentColor = parseAccentColor(medication.colorTagHex, MaterialTheme.colorScheme.primary)
    val isWarning = balance?.isLowStock == true || balance?.isNegative == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isWarning) semantic.warningBorder else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Gradient Identity Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(semantic.heroGradientStart, semantic.heroGradientEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = medication.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${medication.strength} · ${medication.form.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onEditMedicationClick,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Pill Row: Recording Mode + 30-Day Adherence + Archive Status
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (medication.recordingMode == RecordingMode.CONFIRM) {
                                        Icons.Default.VerifiedUser
                                    } else {
                                        Icons.Default.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = medication.recordingMode.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (adherenceSummary != null && adherenceSummary.adherencePercentage != null) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = semantic.takenContainer
                            ) {
                                Text(
                                    text = "30d Adherence: ${adherenceSummary.adherencePercentage}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.taken,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (medication.isArchived) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = semantic.skippedContainer
                            ) {
                                Text(
                                    text = "Archived",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.skipped,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (medication.notes.isNotBlank()) {
                        Text(
                            text = medication.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Bottom Stock Runway & Actions Section
            if (balance != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "CURRENT STOCK BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = balance.currentBalance.toDisplayString(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isWarning) semantic.warning else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = balance.unit.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (balance.daysRemaining != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (isWarning) semantic.warningContainer else MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isWarning) semantic.warningBorder else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = "~${balance.daysRemaining} day(s) supply",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWarning) semantic.warning else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            if (balance.isEstimate) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = semantic.assumedContainer
                                ) {
                                    Text(
                                        text = "ESTIMATED BALANCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.assumed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isWarning) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = semantic.warningContainer,
                            border = BorderStroke(1.dp, semantic.warningBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Low stock warning",
                                    tint = semantic.warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (balance.isNegative) {
                                        "Balance is negative (${balance.currentBalance.toDisplayString()} ${balance.unit.abbreviation}). Please record a restock or recount adjustment."
                                    } else {
                                        "Stock is at or below your alert threshold (${medication.lowStockThreshold.toDisplayString()} ${medication.stockUnit.abbreviation})."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = semantic.warning
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRestockClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restock Supply", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = onAdjustClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recount / Adjust", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleSummaryCard(
    schedules: List<ScheduleDefinition>,
    unit: StockUnit,
    onEditScheduleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dosage Regimen & Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onEditScheduleClick,
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Schedule", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }

            if (schedules.isEmpty()) {
                Text(
                    text = "No active schedule configured. Tap 'Edit Schedule' to set daily or weekday dose times.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                for (sch in schedules) {
                    val freqDesc = if (sch.scheduleType == ScheduleType.SPECIFIC_WEEKDAYS && sch.daysOfWeek.isNotEmpty()) {
                        val days = sch.daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
                        "Specific Weekdays ($days)"
                    } else {
                        "Every Day"
                    }

                    Text(
                        text = "$freqDesc · ${sch.doseQuantityPerOccurrence.toDisplayString()} ${unit.displayName.lowercase()} per dose",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sch.targetTimesMinuteOfDay.sorted().forEach { min ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = formatMinuteOfDay(min),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (sch.instructions.isNotBlank()) {
                        Text(
                            text = "Instructions: ${sch.instructions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentDosesPreviewCard(
    recentDoses: List<DoseOccurrence>,
    totalDoseCount: Int,
    onNavigateToDoseHistory: () -> Unit,
    onCorrectDose: (DoseOccurrence) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Recent Dose Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap 'Correct' to audit or revise a recorded outcome",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onNavigateToDoseHistory,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Text(
                        text = if (totalDoseCount > 0) "Full History ($totalDoseCount)" else "Full History",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            if (recentDoses.isEmpty()) {
                Text(
                    text = "No recorded dose events yet. You can also open Full History to inspect or log past scheduled occurrences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentDoses.forEach { dose ->
                        DoseHistoryRow(
                            dose = dose,
                            onCorrectClick = { onCorrectDose(dose) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleBatchLotsSection(
    lots: List<StockLot>,
    activeFefoLotId: String?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddBatchClick: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = NoveloMotion.standardFloatSpring(),
        label = "lotsChevronRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand, role = Role.Button)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Batch Lots & FEFO Allocation (${lots.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Doses automatically deduct from earliest-expiring batch first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse batch lots" else "Expand batch lots",
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = NoveloMotion.sectionExpand(),
                exit = NoveloMotion.sectionCollapse()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (lots.isEmpty()) {
                        Text(
                            text = "No batch lots recorded yet. Use 'Restock Supply' to register a new batch with lot number and expiry date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        lots.forEach { lot ->
                            StockLotRow(
                                lot = lot,
                                isActiveFefoLot = lot.id == activeFefoLotId
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onAddBatchClick,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register New Batch / Restock")
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleStockLedgerSection(
    movements: List<StockLedgerEntry>,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = NoveloMotion.standardFloatSpring(),
        label = "ledgerChevronRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand, role = Role.Button)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Append-Only Stock Ledger (${movements.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Immutable audit trail of all inflows, dose deductions, and adjustments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse stock ledger" else "Expand stock ledger",
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = NoveloMotion.sectionExpand(),
                exit = NoveloMotion.sectionCollapse()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (movements.isEmpty()) {
                        Text(
                            text = "No stock movements recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        movements.forEach { movement ->
                            StockMovementRow(movement = movement)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockLotRow(
    lot: StockLot,
    isActiveFefoLot: Boolean = false
) {
    val semantic = MaterialTheme.noveloColors
    val expiryStr = lot.expiryDateEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
    } ?: "No expiry set"

    val isExpiringSoon = lot.expiryDateEpochMs?.let {
        val daysToExpiry = (it - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        daysToExpiry <= 30
    } ?: false

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActiveFefoLot) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = lot.lotNumber?.let { "Lot #$it" } ?: "Unnumbered Batch",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isActiveFefoLot) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Active FEFO Batch",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Remaining: ${lot.remainingQuantity.toDisplayString()} of ${lot.initialQuantity.toDisplayString()} ${lot.unit.abbreviation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lot.notes != null) {
                    Text(
                        text = lot.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = if (isExpiringSoon) semantic.warningContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isExpiringSoon) semantic.warningBorder else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = "Exp: $expiryStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpiringSoon) semantic.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DoseHistoryRow(
    dose: DoseOccurrence,
    onCorrectClick: () -> Unit
) {
    val timeStr = Instant.ofEpochMilli(dose.scheduledEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(DOSE_ROW_FORMATTER)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${dose.doseQuantity.toDisplayString()} ${dose.unit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DoseStatusBadge(status = dose.status)

                    OutlinedButton(
                        onClick = onCorrectClick,
                        shape = MaterialTheme.shapes.extraSmall,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Correct dose record",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Correct", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (dose.correctionNotes != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Revision note: ${dose.correctionNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StockMovementRow(movement: StockLedgerEntry) {
    val semantic = MaterialTheme.noveloColors
    val timeStr = Instant.ofEpochMilli(movement.timestampEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(MOVEMENT_ROW_FORMATTER)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movement.movementType.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$timeStr · ${movement.reason ?: "No reason noted"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val prefix = if (movement.movementType.isInflow) "+" else "-"
        val textColor = if (movement.movementType.isInflow) semantic.taken else semantic.missed

        Text(
            text = "$prefix${movement.quantity.toDisplayString()} ${movement.unit.abbreviation}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditMedicationDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        strength: String,
        form: MedicationForm,
        unit: StockUnit,
        lowStockThreshold: String,
        recordingMode: RecordingMode,
        notes: String,
        colorTagHex: String
    ) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(medication.name) }
    var strength by rememberSaveable { mutableStateOf(medication.strength) }
    var selectedFormName by rememberSaveable { mutableStateOf(medication.form.name) }
    var selectedUnitName by rememberSaveable { mutableStateOf(medication.stockUnit.name) }
    var lowStockThresholdStr by rememberSaveable { mutableStateOf(medication.lowStockThreshold.toDisplayString()) }
    var selectedModeName by rememberSaveable { mutableStateOf(medication.recordingMode.name) }
    var notes by rememberSaveable { mutableStateOf(medication.notes) }
    var selectedColorHex by rememberSaveable { mutableStateOf(medication.colorTagHex) }

    val selectedForm = runCatching { MedicationForm.valueOf(selectedFormName) }.getOrDefault(medication.form)
    val selectedUnit = runCatching { StockUnit.valueOf(selectedUnitName) }.getOrDefault(medication.stockUnit)
    val selectedMode = runCatching { RecordingMode.valueOf(selectedModeName) }.getOrDefault(medication.recordingMode)
    val isValid = name.isNotBlank() && strength.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Edit Medication Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = strength,
                    onValueChange = { strength = it },
                    label = { Text("Strength / Concentration *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Dosage Form", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MedicationForm.entries.forEach { form ->
                        FilterChip(
                            selected = selectedForm == form,
                            onClick = {
                                selectedFormName = form.name
                                selectedUnitName = defaultStockUnitForForm(form).name
                            },
                            label = { Text(form.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = lowStockThresholdStr,
                    onValueChange = { lowStockThresholdStr = it },
                    label = { Text("Low-Stock Warning Threshold (${selectedUnit.abbreviation})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Visual Accent Tag", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MedicationAccentSwatches.forEach { (hex, label) ->
                        val swatchColor = parseAccentColor(hex, MaterialTheme.colorScheme.primary)
                        val isChosen = selectedColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = label,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Text("Recording Mode", style = MaterialTheme.typography.labelLarge)
                RecordingModeSelectorCard(
                    selectedMode = selectedMode,
                    onSelectMode = { selectedModeName = it.name }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinical Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            name.trim(),
                            strength.trim(),
                            selectedForm,
                            selectedUnit,
                            lowStockThresholdStr,
                            selectedMode,
                            notes.trim(),
                            selectedColorHex
                        )
                    }
                },
                enabled = isValid
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditScheduleDialog(
    existingSchedule: ScheduleDefinition?,
    stockUnit: StockUnit,
    onDismiss: () -> Unit,
    onConfirm: (
        targetTimesMinuteOfDay: List<Int>,
        daysOfWeek: Set<Int>,
        doseQuantityStr: String,
        instructions: String
    ) -> Unit
) {
    var isSpecificWeekdays by rememberSaveable {
        mutableStateOf(existingSchedule?.scheduleType == ScheduleType.SPECIFIC_WEEKDAYS && existingSchedule.daysOfWeek.isNotEmpty())
    }
    var selectedDaysList by rememberSaveable {
        mutableStateOf(
            existingSchedule?.daysOfWeek?.takeIf { it.isNotEmpty() }?.sorted() ?: listOf(1, 3, 5)
        )
    }
    var selectedTimesList by rememberSaveable {
        mutableStateOf(
            existingSchedule?.targetTimesMinuteOfDay?.takeIf { it.isNotEmpty() }?.sorted() ?: listOf(480)
        )
    }
    var doseQuantityStr by rememberSaveable {
        mutableStateOf(existingSchedule?.doseQuantityPerOccurrence?.toDisplayString() ?: "1")
    }
    var instructions by rememberSaveable {
        mutableStateOf(existingSchedule?.instructions ?: "")
    }

    val effectiveDays = if (isSpecificWeekdays) selectedDaysList.toSet() else emptySet()
    val parsedDose = runCatching { ScaledQuantity.fromDisplayString(doseQuantityStr) }.getOrNull()
    val isValid = selectedTimesList.isNotEmpty() &&
        (!isSpecificWeekdays || effectiveDays.isNotEmpty()) &&
        parsedDose != null && parsedDose.isPositive()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Edit Dosage Schedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    doseQuantityStr = doseQuantityStr,
                    onDoseQuantityChange = { doseQuantityStr = it },
                    stockUnit = stockUnit,
                    instructions = instructions,
                    onInstructionsChange = { instructions = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            selectedTimesList.sorted(),
                            effectiveDays,
                            doseQuantityStr,
                            instructions
                        )
                    }
                },
                enabled = isValid
            ) {
                Text("Update Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestockDialog(
    medication: Medication,
    currentBalance: ScaledQuantity = ScaledQuantity.ZERO,
    onDismiss: () -> Unit,
    onConfirm: (
        quantity: ScaledQuantity,
        lotNumber: String?,
        expiryDateEpochMs: Long?,
        notes: String?
    ) -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    var quantityStr by rememberSaveable { mutableStateOf("28") }
    var lotNumber by rememberSaveable { mutableStateOf("") }
    var expiryDaysAheadStr by rememberSaveable { mutableStateOf("365") }
    var notes by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val parsedQty = runCatching { ScaledQuantity.fromDisplayString(quantityStr) }.getOrNull()
    val isValid = parsedQty != null && parsedQty.isPositive()
    val newBalance = if (parsedQty != null) currentBalance + parsedQty else currentBalance
    val daysAhead = expiryDaysAheadStr.toLongOrNull() ?: 365L
    val computedExpiryDateStr = LocalDate.now().plusDays(daysAhead.coerceAtLeast(1L)).format(DATE_FORMATTER)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Restock Supplies", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Recording new supply for ${medication.name} (${medication.strength}). Appends an immutable RESTOCK entry and creates a FEFO batch lot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Live Post-Restock Impact Preview
                if (parsedQty != null && parsedQty.isPositive()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = semantic.takenContainer,
                        border = BorderStroke(1.dp, semantic.takenBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Ledger Impact Preview (+RESTOCK)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = semantic.taken
                            )
                            Text(
                                text = "${currentBalance.toDisplayString()} → ${newBalance.toDisplayString()} ${medication.stockUnit.abbreviation} (+${parsedQty.toDisplayString()})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.taken
                            )
                            Text(
                                text = "Batch Expiry: $computedExpiryDateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.taken.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Added (${medication.stockUnit.displayName}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lotNumber,
                    onValueChange = { lotNumber = it },
                    label = { Text("Batch / Lot Number (Optional)") },
                    placeholder = { Text("e.g. BATCH-2026-A") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = expiryDaysAheadStr,
                    onValueChange = { expiryDaysAheadStr = it },
                    label = { Text("Expiry (Days from Today — $computedExpiryDateStr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("e.g. 180 or 365") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Pharmacy repeat collection") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        val expiryEpoch = System.currentTimeMillis() + (daysAhead * 24 * 60 * 60 * 1000)
                        onConfirm(
                            parsedQty!!,
                            lotNumber.takeIf { it.isNotBlank() },
                            expiryEpoch,
                            notes.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = isValid && !isSubmitting
            ) {
                Text("Confirm Restock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdjustStockDialog(
    medication: Medication,
    currentBalance: ScaledQuantity,
    lots: List<StockLot>,
    onDismiss: () -> Unit,
    onConfirm: (
        targetBalance: ScaledQuantity,
        reason: String,
        targetLotId: String?
    ) -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    var targetBalanceStr by rememberSaveable { mutableStateOf(currentBalance.toDisplayString()) }
    var reason by rememberSaveable { mutableStateOf("") }
    var selectedLotId by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val parsedTarget = runCatching { ScaledQuantity.fromDisplayString(targetBalanceStr) }.getOrNull()
    val isValid = parsedTarget != null && reason.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Recount / Adjust Stock", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (parsedTarget != null) {
                    val delta = parsedTarget - currentBalance
                    val isIncrease = delta.isPositive()
                    val deltaPrefix = if (isIncrease) "+" else ""
                    val movLabel = when {
                        parsedTarget == currentBalance -> "No Balance Change"
                        isIncrease -> "Ledger Movement: +ADJUSTMENT_IN"
                        else -> "Ledger Movement: -ADJUSTMENT_OUT"
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = movLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${currentBalance.toDisplayString()} → ${parsedTarget.toDisplayString()} ${medication.stockUnit.abbreviation} ($deltaPrefix${delta.toDisplayString()})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetBalanceStr,
                    onValueChange = { targetBalanceStr = it },
                    label = { Text("Actual Observed Count (${medication.stockUnit.abbreviation}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Recount / Adjustment *") },
                    placeholder = { Text("e.g. Physical blister pack recount, dropped tablet") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (lots.isNotEmpty()) {
                    Text(
                        text = "Apply adjustment to batch lot:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLotId = null },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLotId == null,
                            onClick = { selectedLotId = null }
                        )
                        Text("Auto (FEFO / Earliest Expiring Batch)", style = MaterialTheme.typography.bodySmall)
                    }
                    for (lot in lots) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLotId = lot.id },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLotId == lot.id,
                                onClick = { selectedLotId = lot.id }
                            )
                            Text(
                                text = "${lot.lotNumber ?: "Batch"} (${lot.remainingQuantity.toDisplayString()} rem)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        onConfirm(parsedTarget!!, reason.trim(), selectedLotId)
                    }
                },
                enabled = isValid && !isSubmitting
            ) {
                Text("Save Adjustment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CorrectDoseDialog(
    dose: DoseOccurrence,
    medicationName: String,
    onDismiss: () -> Unit,
    onConfirm: (newStatus: DoseStatus, reason: String) -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    var selectedStatusName by rememberSaveable { mutableStateOf(dose.status.name) }
    var reason by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val selectedStatus = runCatching { DoseStatus.valueOf(selectedStatusName) }.getOrDefault(dose.status)
    val isValid = selectedStatus != dose.status && reason.isNotBlank()

    val wasDeducted = dose.status == DoseStatus.TAKEN || dose.status == DoseStatus.ASSUMED
    val willBeDeducted = selectedStatus == DoseStatus.TAKEN || selectedStatus == DoseStatus.ASSUMED
    val stockImpactDescription = when {
        wasDeducted && !willBeDeducted ->
            "+${dose.doseQuantity.toDisplayString()} ${dose.unit.abbreviation} restored via compensating DOSE_REVERSAL"
        !wasDeducted && willBeDeducted ->
            "-${dose.doseQuantity.toDisplayString()} ${dose.unit.abbreviation} deducted via DOSE_TAKEN"
        else ->
            "No net stock change (${dose.doseQuantity.toDisplayString()} ${dose.unit.abbreviation})"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Correct Historical Dose", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Revising record for $medicationName:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status & Stock Impact Preview Card
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DoseStatusBadge(status = dose.status)
                            Text("→", fontWeight = FontWeight.Bold)
                            DoseStatusBadge(status = selectedStatus)
                        }
                        Text(
                            text = "Stock Ledger Impact: $stockImpactDescription",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Select Revised Outcome:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                listOf(DoseStatus.TAKEN, DoseStatus.SKIPPED, DoseStatus.MISSED).forEach { statusOption ->
                    val isSelected = selectedStatus == statusOption
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { selectedStatusName = statusOption.name },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedStatusName = statusOption.name }
                                )
                                Text(
                                    text = statusOption.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DoseStatusBadge(status = statusOption)
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Audit Revision *") },
                    placeholder = { Text("e.g. Logged by mistake, took dose earlier without phone") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        onConfirm(selectedStatus, reason.trim())
                    }
                },
                enabled = isValid && !isSubmitting
            ) {
                Text("Confirm Revision")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PrescriptionOverviewSection(
    prescriptions: List<Prescription>,
    stockUnit: StockUnit,
    onAddPrescriptionClick: () -> Unit,
    onEditPrescriptionClick: (Prescription) -> Unit,
    onRequestRepeatClick: (Prescription) -> Unit,
    onCollectRepeatClick: (Prescription) -> Unit,
    onArchivePrescriptionClick: (Prescription) -> Unit,
    onViewHistoryClick: () -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    val todayEpochDay = LocalDate.now().toEpochDay()
    val primaryRx = prescriptions.firstOrNull { it.effectiveStatus(todayEpochDay) == PrescriptionStatus.ACTIVE }
        ?: prescriptions.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prescription & Repeat Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (prescriptions.isNotEmpty()) {
                        TextButton(onClick = onViewHistoryClick) {
                            Text("Audit Log")
                        }
                    }
                    TextButton(onClick = onAddPrescriptionClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Rx")
                    }
                }
            }

            if (primaryRx == null) {
                Text(
                    text = "No prescription recorded for this medication yet. Add prescription details to track authorised repeats, issue/expiry dates, and pharmacy collection workflow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val effectiveStatus = primaryRx.effectiveStatus(todayEpochDay)
                val issueDateStr = LocalDate.ofEpochDay(primaryRx.issueDateEpochDay).format(DATE_FORMATTER)
                val expiryDateStr = primaryRx.expiryDateEpochDay?.let { LocalDate.ofEpochDay(it).format(DATE_FORMATTER) }
                val daysToExpiry = primaryRx.daysUntilExpiry(todayEpochDay)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = primaryRx.referenceNumber?.let { "Ref: $it" } ?: "Prescription Record",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Issued: $issueDateStr" + (expiryDateStr?.let { " · Expires: $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    PrescriptionStatusBadge(status = effectiveStatus)
                }

                // Visual Repeat Progress Bar
                if (primaryRx.repeatsAuthorised > 0) {
                    val repeatRatio = (primaryRx.repeatsRemaining.toFloat() / primaryRx.repeatsAuthorised.toFloat()).coerceIn(0f, 1f)
                    val animatedRepeatProgress by animateFloatAsState(
                        targetValue = repeatRatio,
                        animationSpec = NoveloMotion.standardFloatSpring(),
                        label = "repeatProgress"
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Repeats Remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${primaryRx.repeatsRemaining} of ${primaryRx.repeatsAuthorised} authorised",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { animatedRepeatProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (primaryRx.repeatsRemaining <= 1) semantic.warning else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            if (primaryRx.hasPendingRepeatRequest) {
                                Text(
                                    text = "Status: Repeat requested — awaiting pharmacy collection",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.warning
                                )
                            }
                        }
                    }
                }

                // Upcoming Expiry / Expired Warning Banner
                if (effectiveStatus == PrescriptionStatus.EXPIRED) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = semantic.missedContainer,
                        border = BorderStroke(1.dp, semantic.missedBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Expired prescription warning",
                                tint = semantic.missed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "This prescription has expired and is no longer active. Contact your prescriber or pharmacy for a renewal.",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.missed
                            )
                        }
                    }
                } else if (primaryRx.isExpiringSoon(todayEpochDay)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = semantic.warningContainer,
                        border = BorderStroke(1.dp, semantic.warningBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Upcoming expiry warning",
                                tint = semantic.warning,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Prescription expires in ${daysToExpiry ?: 0} day(s) ($expiryDateStr).",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.warning
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        primaryRx.quantityPrescribed?.let { qty ->
                            Text(
                                text = "Quantity prescribed: ${qty.toDisplayString()} ${(primaryRx.unit ?: stockUnit).abbreviation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        primaryRx.pharmacyName?.let { pharm ->
                            Text(
                                text = "Pharmacy: $pharm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        primaryRx.notes?.let { note ->
                            Text(
                                text = "Notes: $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { onEditPrescriptionClick(primaryRx) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit prescription",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (effectiveStatus != PrescriptionStatus.ARCHIVED) {
                            IconButton(onClick = { onArchivePrescriptionClick(primaryRx) }) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Archive prescription",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Repeat Actions Row
                if (effectiveStatus == PrescriptionStatus.ACTIVE && primaryRx.repeatsAuthorised > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRequestRepeatClick(primaryRx) },
                            enabled = primaryRx.canRequestRepeat(todayEpochDay),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (primaryRx.hasPendingRepeatRequest) "Repeat Requested" else "1. Request Repeat",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Button(
                            onClick = { onCollectRepeatClick(primaryRx) },
                            enabled = primaryRx.canCollectRepeat(todayEpochDay),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "2. Collect Repeat",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Clinical & Manual Maintenance Disclaimer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Prescription information is manually maintained for personal tracking. Requesting a repeat here logs a local note only and does not contact a pharmacy or decrement repeats until marked collected.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrescriptionStatusBadge(status: PrescriptionStatus) {
    val semantic = MaterialTheme.noveloColors
    val (bg, fg, border, icon, text) = when (status) {
        PrescriptionStatus.ACTIVE -> PrescriptionBadgeSpec(semantic.takenContainer, semantic.taken, semantic.takenBorder, Icons.Default.Check, "Active")
        PrescriptionStatus.EXPIRED -> PrescriptionBadgeSpec(semantic.missedContainer, semantic.missed, semantic.missedBorder, Icons.Default.Warning, "Expired")
        PrescriptionStatus.COMPLETED -> PrescriptionBadgeSpec(semantic.skippedContainer, semantic.skipped, semantic.skippedBorder, Icons.Default.Check, "Completed")
        PrescriptionStatus.ARCHIVED -> PrescriptionBadgeSpec(semantic.skippedContainer, semantic.skipped, semantic.skippedBorder, Icons.Default.Archive, "Archived")
    }
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bg)
            .border(1.dp, border, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

private data class PrescriptionBadgeSpec(
    val bg: Color,
    val fg: Color,
    val border: Color,
    val icon: ImageVector,
    val text: String
)

@Composable
private fun AddEditPrescriptionDialog(
    existingPrescription: Prescription?,
    stockUnit: StockUnit,
    onDismiss: () -> Unit,
    onConfirm: (
        referenceNumber: String?,
        issueDateEpochDay: Long,
        expiryDateEpochDay: Long?,
        quantityPrescribed: ScaledQuantity?,
        repeatsAuthorised: Int,
        repeatsRemaining: Int,
        pharmacyName: String?,
        notes: String?
    ) -> Unit
) {
    val today = LocalDate.now()
    val initialIssueStr = existingPrescription?.let { LocalDate.ofEpochDay(it.issueDateEpochDay).toString() }
        ?: today.toString()
    val initialExpiryStr = existingPrescription?.expiryDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() }
        ?: today.plusMonths(6).toString()

    var referenceNumber by rememberSaveable { mutableStateOf(existingPrescription?.referenceNumber ?: "") }
    var issueDateStr by rememberSaveable { mutableStateOf(initialIssueStr) }
    var expiryDateStr by rememberSaveable { mutableStateOf(initialExpiryStr) }
    var quantityStr by rememberSaveable { mutableStateOf(existingPrescription?.quantityPrescribed?.toDisplayString() ?: "28") }
    var repeatsAuthStr by rememberSaveable { mutableStateOf((existingPrescription?.repeatsAuthorised ?: 6).toString()) }
    var repeatsRemStr by rememberSaveable { mutableStateOf((existingPrescription?.repeatsRemaining ?: 6).toString()) }
    var pharmacyName by rememberSaveable { mutableStateOf(existingPrescription?.pharmacyName ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingPrescription?.notes ?: "") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = if (existingPrescription == null) "Add Prescription" else "Edit Prescription",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Reference / Script ID (Optional)") },
                    placeholder = { Text("e.g. RX-2026-8410") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = issueDateStr,
                        onValueChange = {
                            issueDateStr = it
                            errorMessage = null
                        },
                        label = { Text("Issue (YYYY-MM-DD) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expiryDateStr,
                        onValueChange = {
                            expiryDateStr = it
                            errorMessage = null
                        },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Prescribed (${stockUnit.abbreviation}, Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = repeatsAuthStr,
                        onValueChange = {
                            repeatsAuthStr = it
                            errorMessage = null
                        },
                        label = { Text("Repeats Auth.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repeatsRemStr,
                        onValueChange = {
                            repeatsRemStr = it
                            errorMessage = null
                        },
                        label = { Text("Repeats Left") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = pharmacyName,
                    onValueChange = { pharmacyName = it },
                    label = { Text("Pharmacy Name (Optional)") },
                    placeholder = { Text("e.g. High Street Pharmacy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val issueDate = runCatching { LocalDate.parse(issueDateStr.trim()) }.getOrNull()
                    if (issueDate == null) {
                        errorMessage = "Please enter a valid issue date (YYYY-MM-DD)."
                        return@Button
                    }
                    val expiryDate = if (expiryDateStr.isBlank()) {
                        null
                    } else {
                        val parsed = runCatching { LocalDate.parse(expiryDateStr.trim()) }.getOrNull()
                        if (parsed == null) {
                            errorMessage = "Please enter a valid expiry date (YYYY-MM-DD) or leave blank."
                            return@Button
                        }
                        parsed
                    }
                    val repeatsAuth = repeatsAuthStr.trim().toIntOrNull()
                    val repeatsRem = repeatsRemStr.trim().toIntOrNull()
                    if (repeatsAuth == null || repeatsAuth < 0 || repeatsRem == null || repeatsRem < 0) {
                        errorMessage = "Repeats must be 0 or greater."
                        return@Button
                    }
                    if (repeatsRem > repeatsAuth) {
                        errorMessage = "Repeats remaining cannot exceed repeats authorised."
                        return@Button
                    }
                    val qty = quantityStr.trim().takeIf { it.isNotEmpty() }?.let {
                        ScaledQuantity.fromDisplayString(it)
                    }
                    onConfirm(
                        referenceNumber.trim().takeIf { it.isNotEmpty() },
                        issueDate.toEpochDay(),
                        expiryDate?.toEpochDay(),
                        qty,
                        repeatsAuth,
                        repeatsRem,
                        pharmacyName.trim().takeIf { it.isNotEmpty() },
                        notes.trim().takeIf { it.isNotEmpty() }
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PrescriptionHistoryDialog(
    prescriptions: List<Prescription>,
    events: List<PrescriptionEvent>,
    onDismiss: () -> Unit
) {
    val todayEpochDay = LocalDate.now().toEpochDay()
    val zone = ZoneId.systemDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prescription & Repeat Audit Log", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recorded Prescriptions (${prescriptions.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                prescriptions.forEach { rx ->
                    val effStatus = rx.effectiveStatus(todayEpochDay)
                    val issueStr = LocalDate.ofEpochDay(rx.issueDateEpochDay).format(DATE_FORMATTER)
                    val expStr = rx.expiryDateEpochDay?.let { LocalDate.ofEpochDay(it).format(DATE_FORMATTER) }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rx.referenceNumber ?: "Prescription",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                PrescriptionStatusBadge(status = effStatus)
                            }
                            Text(
                                text = "Issued: $issueStr" + (expStr?.let { " · Expires: $it" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Repeats: ${rx.repeatsRemaining} / ${rx.repeatsAuthorised} remaining",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Immutable Event Timeline (${events.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (events.isEmpty()) {
                    Text(
                        text = "No audit events recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    events.forEach { ev ->
                        val tsStr = Instant.ofEpochMilli(ev.timestampEpochMs).atZone(zone).format(AUDIT_TIMESTAMP_FORMATTER)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${ev.eventType.displayName} · $tsStr",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = ev.notes ?: "Repeats remaining: ${ev.repeatsRemainingAfter}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
