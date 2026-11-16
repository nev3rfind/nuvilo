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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import com.example.nuvilo.domain.model.AdministrationProfile
import com.example.nuvilo.domain.model.AdministrationRoute
import com.example.nuvilo.domain.model.AdministrationSite
import com.example.nuvilo.domain.model.DepletionReason
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.GelTrackingMode
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationDeleteOutcome
import com.example.nuvilo.domain.model.MedicationEditImpact
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.PackageStatus
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.PackagingConfig
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
import com.example.nuvilo.domain.model.TrackingPrecision
import com.example.nuvilo.domain.stock.SupplyForecast
import com.example.nuvilo.ui.components.DoseStatusBadge
import com.example.nuvilo.ui.components.PackageStatusChip
import com.example.nuvilo.ui.components.ScheduleConfigurationContent
import com.example.nuvilo.ui.components.StockBadge
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
    val nuviloHaptics = LocalNuviloHaptics.current

    var showRestockDialog by rememberSaveable { mutableStateOf(false) }
    var showAdjustDialog by rememberSaveable { mutableStateOf(false) }
    var lotToRecount by remember { mutableStateOf<StockLot?>(null) }
    var lotToDiscard by remember { mutableStateOf<StockLot?>(null) }
    var lotToDeplete by remember { mutableStateOf<StockLot?>(null) }
    var lotToReopen by remember { mutableStateOf<StockLot?>(null) }
    var lotToEditDetails by remember { mutableStateOf<StockLot?>(null) }
    var showEditMedicationDialog by rememberSaveable { mutableStateOf(false) }
    var showEditScheduleDialog by rememberSaveable { mutableStateOf(false) }
    var doseToCorrect by remember { mutableStateOf<DoseOccurrence?>(null) }
    var showPrescriptionFormDialog by rememberSaveable { mutableStateOf(false) }
    var prescriptionToEdit by remember { mutableStateOf<Prescription?>(null) }
    var showPrescriptionHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var rxForRepeatRequest by remember { mutableStateOf<Prescription?>(null) }
    var rxForCollection by remember { mutableStateOf<Prescription?>(null) }

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

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
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
                        IconButton(onClick = { viewModel.requestDeleteMedication(confirmCascadeWithHistory = false) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete medication",
                                tint = MaterialTheme.colorScheme.error
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
                val nowEpochMs = System.currentTimeMillis()
                val activeFefoLotId = uiState.lots
                    .filter { lot ->
                        val discardAt = lot.effectiveDiscardAfterOpeningEpochMs
                        (lot.packageStatus == PackageStatus.OPENED || lot.packageStatus == PackageStatus.UNOPENED) &&
                            lot.remainingQuantity.isPositive() &&
                            (lot.expiryDateEpochMs == null || lot.expiryDateEpochMs >= nowEpochMs) &&
                            (discardAt == null || discardAt >= nowEpochMs)
                    }
                    .minWithOrNull(
                        compareBy<StockLot> { if (it.packageStatus == PackageStatus.OPENED) 0 else 1 }
                            .thenBy { it.expiryDateEpochMs ?: Long.MAX_VALUE }
                            .thenBy { it.createdAtEpochMs }
                    )?.id

                val nextUnopenedFefoLot = uiState.lots
                    .filter { it.packageStatus == PackageStatus.UNOPENED && it.remainingQuantity.isPositive() }
                    .minWithOrNull(compareBy<StockLot> { it.expiryDateEpochMs ?: Long.MAX_VALUE }.thenBy { it.createdAtEpochMs })

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp)
                ) {
                    val editImpact = uiState.lastEditImpact
                    if (editImpact != null && editImpact.impactSummaries.isNotEmpty()) {
                        item(key = "edit_impact_banner", contentType = "edit_impact") {
                            EditImpactBannerCard(
                                impact = editImpact,
                                onDismiss = { viewModel.clearEditImpact() }
                            )
                        }
                    }

                    // 1. Unified Hero Medication & Stock Runway Card
                    item(key = "hero_stock_card", contentType = "hero_card") {
                        val todayEpochDay = LocalDate.now().toEpochDay()
                        val activeRx = uiState.prescriptions.firstOrNull {
                            it.effectiveStatus(todayEpochDay) == PrescriptionStatus.ACTIVE
                        } ?: uiState.prescriptions.firstOrNull()
                        val activeOpenLot = uiState.supplyForecast?.activeOpenedPackage
                            ?: uiState.lots.firstOrNull { it.packageStatus == PackageStatus.OPENED && it.remainingQuantity.isPositive() }
                        val unopenedCount = uiState.lots.count { it.packageStatus == PackageStatus.UNOPENED && it.remainingQuantity.isPositive() }
                        val educationalInsight = uiState.earlyDepletedLots.firstOrNull()?.let { depleted ->
                            val pkgSingular = med.packagingConfig?.packageType?.displayName ?: "package"
                            val initial = depleted.initialQuantity.toDisplayString()
                            val consumed = (depleted.initialQuantity - depleted.remainingQuantity).toDisplayString()
                            val unit = depleted.unit.abbreviation
                            "A previous $pkgSingular finished earlier than estimated ($consumed of $initial $unit recorded). Eye drops and topical medications can vary due to dispenser dynamics, double-squeezing, or missed drops. Nuvilo records variance honestly without penalising your adherence."
                        }

                        MedicationHeroAndStockCard(
                            medication = med,
                            balance = uiState.stockBalance,
                            supplyForecast = uiState.supplyForecast,
                            adherenceSummary = uiState.medicationAdherenceSummary,
                            primaryPrescription = activeRx,
                            nextUnopenedLot = nextUnopenedFefoLot,
                            stagedNextLot = uiState.stagedNextPackage ?: nextUnopenedFefoLot,
                            unopenedCount = unopenedCount,
                            lots = uiState.lots,
                            educationalInsight = educationalInsight,
                            onEditMedicationClick = { showEditMedicationDialog = true },
                            onRestockClick = { showRestockDialog = true },
                            onAdjustClick = {
                                if (activeOpenLot != null) {
                                    lotToRecount = activeOpenLot
                                } else {
                                    showAdjustDialog = true
                                }
                            },
                            onDepleteOpenedClick = activeOpenLot?.let { openLot ->
                                { lotToDeplete = openLot }
                            },
                            onOpenNextPackageClick = (uiState.stagedNextPackage ?: nextUnopenedFefoLot)?.let { lot ->
                                {
                                    nuviloHaptics.perform(NuviloHapticEvent.PACKAGE_OPENED)
                                    viewModel.openPackage(lot.id)
                                }
                            },
                            onRevertOpenedClick = { lotId ->
                                viewModel.revertOpenedPackage(lotId)
                            },
                            onDiscardOpenedClick = activeOpenLot?.let { openLot ->
                                { lotToDiscard = openLot }
                            },
                            onRequestRepeatClick = if (activeRx != null && activeRx.canRequestRepeat(todayEpochDay)) {
                                { rxForRepeatRequest = activeRx }
                            } else if (activeRx == null) {
                                {
                                    prescriptionToEdit = null
                                    showPrescriptionFormDialog = true
                                }
                            } else null
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
                                rxForRepeatRequest = rx
                            },
                            onCollectRepeatClick = { rx ->
                                rxForCollection = rx
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
                            onAddBatchClick = { showRestockDialog = true },
                            onOpenLotClick = { lot ->
                                nuviloHaptics.perform(NuviloHapticEvent.PACKAGE_OPENED)
                                viewModel.openPackage(lot.id)
                            },
                            onRecountLotClick = { lot -> lotToRecount = lot },
                            onDiscardLotClick = { lot -> lotToDiscard = lot },
                            onReopenLotClick = { lot -> lotToReopen = lot },
                            onEditLotClick = { lot -> lotToEditDetails = lot }
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
            onConfirmFull = { name, strength, form, unit, threshold, mode, notes, colorHex, packagingConfig, administrationProfile, lowStockDaysThreshold, administrationRoute, notificationsEnabled ->
                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                viewModel.updateMedication(
                    name = name,
                    strength = strength,
                    form = form,
                    unit = unit,
                    lowStockThresholdStr = threshold,
                    recordingMode = mode,
                    notes = notes,
                    colorTagHex = colorHex,
                    packagingConfig = packagingConfig,
                    administrationProfile = administrationProfile,
                    lowStockDaysThreshold = lowStockDaysThreshold,
                    administrationRoute = administrationRoute,
                    notificationsEnabled = notificationsEnabled
                )
                showEditMedicationDialog = false
            }
        )
    }

    if (showEditScheduleDialog && med != null) {
        EditScheduleDialog(
            existingSchedule = uiState.schedules.firstOrNull(),
            stockUnit = med.effectiveConsumptionUnit,
            onDismiss = { showEditScheduleDialog = false },
            onConfirm = { times, daysOfWeek, doseQty, instructions ->
                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
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
        val hasOpenedPack = uiState.lots.any { it.packageStatus == PackageStatus.OPENED }
        val dailyRate = uiState.schedules.sumOf {
            it.targetTimesMinuteOfDay.size.toLong() * it.doseQuantityPerOccurrence.rawScaledValue
        }.takeIf { it > 0L }?.let { ScaledQuantity(it) }
        RestockDialog(
            medication = med,
            currentBalance = uiState.stockBalance?.currentBalance ?: ScaledQuantity.ZERO,
            hasOpenedPackage = hasOpenedPack,
            dailyConsumptionRate = dailyRate,
            onDismiss = { showRestockDialog = false },
            onConfirmPackages = { packageCount, contentsPerPackage, consumptionUnit, packageType, discardDays, lotNumber, expiryEpoch, notes, openFirstImmediately ->
                nuviloHaptics.perform(NuviloHapticEvent.RESTOCK_SAVED)
                viewModel.restockPackages(
                    packageCount = packageCount,
                    contentsPerPackage = contentsPerPackage,
                    consumptionUnit = consumptionUnit,
                    packageType = packageType,
                    discardAfterOpeningDays = discardDays,
                    lotNumber = lotNumber,
                    expiryDateEpochMs = expiryEpoch,
                    notes = notes,
                    openFirstPackageImmediately = openFirstImmediately
                )
                showRestockDialog = false
            },
            onConfirm = { qty, lotNumber, expiryEpoch, notes ->
                nuviloHaptics.perform(NuviloHapticEvent.RESTOCK_SAVED)
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
                nuviloHaptics.perform(NuviloHapticEvent.RECOUNT_SAVED)
                viewModel.adjustStock(targetBal, reason, targetLotId)
                showAdjustDialog = false
            },
            onRecountPackageClick = { lot ->
                showAdjustDialog = false
                lotToRecount = lot
            },
            onDepletePackageClick = { lot ->
                showAdjustDialog = false
                lotToDeplete = lot
            },
            onRevertPackageClick = { lot ->
                showAdjustDialog = false
                viewModel.revertOpenedPackage(lot.id)
            }
        )
    }

    val currentLotToRecount = lotToRecount
    if (currentLotToRecount != null && med != null) {
        RecountOpenedPackageDialog(
            medication = med,
            openedLot = currentLotToRecount,
            onDismiss = { lotToRecount = null },
            onConfirm = { observedRemaining, reason ->
                nuviloHaptics.perform(NuviloHapticEvent.RECOUNT_SAVED)
                viewModel.recountOpenedPackage(currentLotToRecount.id, observedRemaining, reason)
                lotToRecount = null
            },
            onDepleteClick = {
                val lot = currentLotToRecount
                lotToRecount = null
                lotToDeplete = lot
            },
            onRevertClick = {
                val lotId = currentLotToRecount.id
                lotToRecount = null
                viewModel.revertOpenedPackage(lotId)
            }
        )
    }

    val currentLotToDiscard = lotToDiscard
    if (currentLotToDiscard != null && med != null) {
        DiscardPackageDialog(
            medication = med,
            lot = currentLotToDiscard,
            onDismiss = { lotToDiscard = null },
            onConfirm = { reason ->
                nuviloHaptics.perform(NuviloHapticEvent.PACKAGE_DISCARDED)
                viewModel.discardPackage(currentLotToDiscard.id, reason)
                lotToDiscard = null
            }
        )
    }

    val currentLotToDeplete = lotToDeplete
    if (currentLotToDeplete != null && med != null) {
        val unopenedCount = uiState.lots.count { it.packageStatus == PackageStatus.UNOPENED && it.remainingQuantity.isPositive() && it.id != currentLotToDeplete.id }
        EarlyPackageDepletionDialog(
            medication = med,
            lot = currentLotToDeplete,
            unopenedCount = unopenedCount,
            onDismiss = { lotToDeplete = null },
            onConfirm = { reason, userNote ->
                nuviloHaptics.perform(NuviloHapticEvent.PACKAGE_DEPLETED)
                viewModel.depletePackage(currentLotToDeplete.id, reason, userNote)
                lotToDeplete = null
            }
        )
    }

    val currentLotToReopen = lotToReopen
    if (currentLotToReopen != null && med != null) {
        ReopenPackageDialog(
            medication = med,
            lot = currentLotToReopen,
            onDismiss = { lotToReopen = null },
            onConfirm = { reason ->
                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                viewModel.reopenDepletedPackage(currentLotToReopen.id, reason)
                lotToReopen = null
            }
        )
    }

    val currentLotToEdit = lotToEditDetails
    if (currentLotToEdit != null && med != null) {
        EditPackageDetailsDialog(
            lot = currentLotToEdit,
            onDismiss = { lotToEditDetails = null },
            onConfirm = { label, lotNum, expiryMs, discardDays, notes ->
                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                viewModel.updatePackageDetails(
                    lotId = currentLotToEdit.id,
                    packageLabel = label,
                    lotNumber = lotNum,
                    expiryDateEpochMs = expiryMs,
                    discardAfterOpeningDays = discardDays,
                    notes = notes
                )
                lotToEditDetails = null
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
            onConfirm = { ref, issueDay, expiryDay, qty, repeatsAuth, repeatsRem, pharmacy, notes, repeatReqDay, dispensedDay ->
                viewModel.savePrescription(
                    existingPrescription = prescriptionToEdit,
                    referenceNumber = ref,
                    issueDateEpochDay = issueDay,
                    expiryDateEpochDay = expiryDay,
                    quantityPrescribed = qty,
                    repeatsAuthorised = repeatsAuth,
                    repeatsRemaining = repeatsRem,
                    pharmacyName = pharmacy,
                    notes = notes,
                    repeatRequestedDateEpochDay = repeatReqDay,
                    dispensedDateEpochDay = dispensedDay
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

    rxForRepeatRequest?.let { rx ->
        if (med != null) {
            RequestRepeatDialog(
                prescription = rx,
                medication = med,
                onDismiss = { rxForRepeatRequest = null },
                onConfirm = { requestedQty, notes ->
                    viewModel.requestPrescriptionRepeat(rx.id, requestedQty, notes)
                    rxForRepeatRequest = null
                }
            )
        }
    }

    rxForCollection?.let { rx ->
        if (med != null) {
            CollectRepeatDialog(
                prescription = rx,
                medication = med,
                onDismiss = { rxForCollection = null },
                onConfirm = { notes, autoRestockQty, lotNumber, expiryEpoch ->
                    viewModel.collectPrescriptionRepeat(
                        prescriptionId = rx.id,
                        notes = notes,
                        autoRestockQuantity = autoRestockQty,
                        lotNumber = lotNumber,
                        expiryDateEpochMs = expiryEpoch
                    )
                    rxForCollection = null
                }
            )
        }
    }

    uiState.pendingDeleteConsent?.let { consent ->
        SafeDeleteConsentDialog(
            consent = consent,
            onDismiss = { viewModel.dismissDeleteConsent() },
            onArchiveInstead = {
                viewModel.dismissDeleteConsent()
                viewModel.setArchived(true)
            },
            onConfirmPermanentDelete = {
                viewModel.requestDeleteMedication(confirmCascadeWithHistory = true)
            }
        )
    }
}

@Composable
private fun EditImpactBannerCard(
    impact: MedicationEditImpact,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NoveloCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Medication Update Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Dismiss")
                }
            }
            impact.impactSummaries.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SafeDeleteConsentDialog(
    consent: MedicationDeleteOutcome.RequiresExplicitConsent,
    onDismiss: () -> Unit,
    onArchiveInstead: () -> Unit,
    onConfirmPermanentDelete: () -> Unit
) {
    val counts = consent.counts
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Delete ${consent.medicationName}?",
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
                Text(
                    text = "This medication has recorded history. Deleting it permanently removes all associated records and cannot be undone. Consider archiving to preserve your audit trail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Records to be permanently removed:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("• Dose events: ${counts.doseEventCount}", style = MaterialTheme.typography.bodySmall.tabular)
                        Text("• Stock ledger entries: ${counts.stockMovementCount}", style = MaterialTheme.typography.bodySmall.tabular)
                        Text("• Stock packages/lots: ${counts.stockLotCount}", style = MaterialTheme.typography.bodySmall.tabular)
                        Text("• Prescriptions: ${counts.prescriptionCount}", style = MaterialTheme.typography.bodySmall.tabular)
                        Text("• Schedules: ${counts.scheduleCount}", style = MaterialTheme.typography.bodySmall.tabular)
                    }
                }
                OutlinedButton(
                    onClick = onArchiveInstead,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Archive Instead (Preserve History)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPermanentDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationHeroAndStockCard(
    medication: Medication,
    balance: StockBalance?,
    supplyForecast: SupplyForecast? = null,
    adherenceSummary: AdherenceSummary?,
    primaryPrescription: Prescription? = null,
    nextUnopenedLot: StockLot? = null,
    stagedNextLot: StockLot? = null,
    unopenedCount: Int = 0,
    lots: List<StockLot> = emptyList(),
    educationalInsight: String? = null,
    onEditMedicationClick: () -> Unit,
    onRestockClick: () -> Unit,
    onAdjustClick: () -> Unit,
    onDepleteOpenedClick: (() -> Unit)? = null,
    onOpenNextPackageClick: (() -> Unit)? = null,
    onRevertOpenedClick: ((lotId: String) -> Unit)? = null,
    onDiscardOpenedClick: (() -> Unit)? = null,
    onRequestRepeatClick: (() -> Unit)? = null
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
                    val pkgConfig = medication.effectivePackagingConfig
                    val pkgSingular = pkgConfig.packageType.displayName
                    val pkgPlural = pkgConfig.packageType.pluralName
                    val activeOpen = supplyForecast?.activeOpenedPackage
                    val unopenedCount = supplyForecast?.unopenedPackagesCount ?: 0
                    val totalPhysical = (if (activeOpen != null) 1 else 0) + unopenedCount

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "TOTAL ESTIMATED USABLE SUPPLY",
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
                            if (totalPhysical > 0) {
                                Text(
                                    text = "$totalPhysical total physical $pkgPlural ($unopenedCount unopened)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val daysLeft = supplyForecast?.daysOfSupply ?: balance.daysRemaining
                            if (daysLeft != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (isWarning) semantic.warningContainer else MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isWarning) semantic.warningBorder else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = "~$daysLeft day(s) supply",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWarning) semantic.warning else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            if (supplyForecast != null) {
                                TrackingPrecisionBadge(precision = supplyForecast.precision)
                            } else if (balance.isEstimate) {
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

                    // Redesigned Physical Package Inventory & Stock Overview
                    val packaging = pkgConfig
                    val openedLots = lots.filter { it.packageStatus == PackageStatus.OPENED }
                    val unopenedLots = lots.filter { it.packageStatus == PackageStatus.UNOPENED }
                    val openedLotsCount = if (lots.isNotEmpty()) openedLots.size else (supplyForecast?.openedPackagesCount ?: 0)
                    val unopenedLotsCount = if (lots.isNotEmpty()) unopenedLots.size else (supplyForecast?.unopenedPackagesCount ?: unopenedCount)
                    val totalPhysicalCount = openedLotsCount + unopenedLotsCount
                    val currentActiveOpen = openedLots.firstOrNull() ?: activeOpen
                    val usableUnits = supplyForecast?.totalUsableUnits ?: balance.currentBalance
                    val daysOfSupply = supplyForecast?.daysOfSupply ?: balance.daysRemaining
                    val dailyRate = if (daysOfSupply != null && daysOfSupply > 0) {
                        ScaledQuantity(usableUnits.rawScaledValue / daysOfSupply.toLong())
                    } else null

                    // 1. Total Physical Inventory Card
                    if (totalPhysicalCount > 0 || supplyForecast != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Total Physical Inventory",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "$totalPhysicalCount total ${if (totalPhysicalCount == 1) pkgSingular.lowercase() else pkgPlural.lowercase()}",
                                        style = MaterialTheme.typography.titleSmall.tabular,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "$openedLotsCount opened + $unopenedLotsCount unopened ${pkgPlural.lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "TOTAL ESTIMATED USABLE SUPPLY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${usableUnits.toDisplayString()} ${packaging.consumptionUnit.abbreviation}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (balance.isLowStock) semantic.warning else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Subject to package usability and expiry",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    if (daysOfSupply != null) {
                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Surface(
                                                shape = MaterialTheme.shapes.extraSmall,
                                                color = if (balance.isLowStock) semantic.warningContainer else MaterialTheme.colorScheme.primaryContainer,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (balance.isLowStock) semantic.warningBorder else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Text(
                                                    text = "~$daysOfSupply days supply",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (balance.isLowStock) semantic.warning else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            if (dailyRate != null && dailyRate.isPositive()) {
                                                Text(
                                                    text = "at ~${dailyRate.toDisplayString()} ${packaging.consumptionUnit.abbreviation}/day",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Current Opened Package Card
                    if (currentActiveOpen != null) {
                        val openLabel = currentActiveOpen.packageLabel ?: currentActiveOpen.lotNumber?.let { "Lot #$it" } ?: "Current $pkgSingular"
                        val initialRaw = currentActiveOpen.initialQuantity.rawScaledValue.coerceAtLeast(1L)
                        val fraction = (currentActiveOpen.remainingQuantity.rawScaledValue.toFloat() / initialRaw.toFloat()).coerceIn(0f, 1f)
                        val openedDateStr = currentActiveOpen.openedAtEpochMs?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
                        } ?: "Unknown date"

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Current Opened $pkgSingular",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = NoveloPillShape,
                                        color = semantic.takenContainer,
                                        border = BorderStroke(1.dp, semantic.takenBorder)
                                    ) {
                                        Text(
                                            text = "OPENED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = semantic.taken,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = openLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${currentActiveOpen.remainingQuantity.toDisplayString()} / ${currentActiveOpen.initialQuantity.toDisplayString()} ${currentActiveOpen.unit.abbreviation} remaining",
                                        style = MaterialTheme.typography.bodyMedium.tabular,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(NoveloPillShape)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Opened: $openedDateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val discardCutoffMs = currentActiveOpen.discardAfterOpeningEpochMs ?: supplyForecast?.daysUntilActivePackageDiscard?.let {
                                        System.currentTimeMillis() + it.toLong() * 86_400_000L
                                    }
                                    if (discardCutoffMs != null) {
                                        val discardDateStr = Instant.ofEpochMilli(discardCutoffMs).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
                                        val daysLeft = ((discardCutoffMs - System.currentTimeMillis()) / 86_400_000L).toInt()
                                        val isUrgent = daysLeft <= 3
                                        Text(
                                            text = if (daysLeft <= 0) "Discard deadline: EXPIRED" else "Discard deadline: $discardDateStr ($daysLeft days)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isUrgent) semantic.warning else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Quick actions on opened container: "Mark finished" and "Revert to sealed"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (onDepleteOpenedClick != null) {
                                        OutlinedButton(
                                            onClick = onDepleteOpenedClick,
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                            shape = MaterialTheme.shapes.small,
                                            border = BorderStroke(1.dp, semantic.warning.copy(alpha = 0.7f))
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = semantic.warning, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark finished", style = MaterialTheme.typography.labelMedium, color = semantic.warning, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (onRevertOpenedClick != null && currentActiveOpen.remainingQuantity == currentActiveOpen.initialQuantity) {
                                        OutlinedButton(
                                            onClick = { onRevertOpenedClick(currentActiveOpen.id) },
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Revert to sealed", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Unopened Reserve Supply Card
                    if (unopenedLotsCount > 0) {
                        val totalUnopenedRaw = if (unopenedLots.isNotEmpty()) {
                            unopenedLots.map { it.remainingQuantity.rawScaledValue }.sum()
                        } else {
                            (packaging.contentsPerPackage?.rawScaledValue ?: 0L) * unopenedLotsCount.toLong()
                        }
                        val totalUnopenedQtyStr = ScaledQuantity(totalUnopenedRaw).toDisplayString()
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AllInbox,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "$unopenedLotsCount ${if (unopenedLotsCount == 1) pkgSingular.lowercase() else pkgPlural.lowercase()} available",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$totalUnopenedQtyStr ${packaging.consumptionUnit.abbreviation} sealed in reserve",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = NoveloPillShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "SEALED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (supplyForecast?.needsAttentionForExpiredOrOpenLimit == true) {
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
                                    contentDescription = "Expired or post-opening discard warning",
                                    tint = semantic.warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "One or more packs are expired or past their opened discard limit and excluded from usable runway.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = semantic.warning
                                )
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

                    if (primaryPrescription?.hasPendingRepeatRequest == true) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = semantic.assumedContainer,
                            border = BorderStroke(1.dp, semantic.assumedBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = "GP Repeat Order Pending",
                                    tint = semantic.assumed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "GP Repeat Order Pending",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.assumed
                                    )
                                    val qtyText = primaryPrescription.quantityPrescribed?.let { "${it.toDisplayString()} ${medication.stockUnit.abbreviation} requested · " } ?: ""
                                    Text(
                                        text = "${qtyText}Order submitted to surgery. Awaiting GP authorisation & pharmacy preparation.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semantic.assumed.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    } else if (balance.isLowStock || (balance.daysRemaining != null && balance.daysRemaining <= 7)) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = semantic.warningContainer,
                            border = BorderStroke(1.dp, semantic.warningBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = semantic.warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "GP Reorder Recommended",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.warning
                                    )
                                }
                                val daysText = balance.daysRemaining?.let { "Only ~$it day(s) supply remaining. " } ?: ""
                                Text(
                                    text = "${daysText}GP surgeries and pharmacies typically need 2–3 working days to authorise and prepare repeat prescriptions. Request your next repeat now to avoid running out.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.warning
                                )
                                if (onRequestRepeatClick != null) {
                                    Button(
                                        onClick = onRequestRepeatClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = semantic.warning,
                                            contentColor = Color.White
                                        ),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 42.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (primaryPrescription != null) "Order Repeat from GP" else "Add Prescription & Order",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val hasOpenedPack = currentActiveOpen != null

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
                            Text(
                                text = if (medication.packagingConfig != null) "Add $pkgPlural" else "Restock Supply",
                                style = MaterialTheme.typography.labelLarge
                            )
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
                            Text(
                                text = if (hasOpenedPack) "Recount Open $pkgSingular" else "Recount / Adjust",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (onDepleteOpenedClick != null && hasOpenedPack) {
                        OutlinedButton(
                            onClick = onDepleteOpenedClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, semantic.warning.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = semantic.warning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mark $pkgSingular as finished",
                                style = MaterialTheme.typography.labelLarge,
                                color = semantic.warning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (educationalInsight != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Intelligent Consumption Insight",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = educationalInsight,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (!hasOpenedPack && stagedNextLot != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Next $pkgSingular Ready",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = NoveloPillShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "Ready to Open",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                val nextLabel = stagedNextLot.packageLabel ?: stagedNextLot.lotNumber?.let { "Lot #$it" } ?: "Next $pkgSingular"
                                val expStr = stagedNextLot.expiryDateEpochMs?.let {
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
                                } ?: "No expiry"
                                Text(
                                    text = "$nextLabel (${stagedNextLot.initialQuantity.toDisplayString()} ${stagedNextLot.unit.abbreviation}) · Exp: $expStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (onOpenNextPackageClick != null) {
                                    FilledTonalButton(
                                        onClick = onOpenNextPackageClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 44.dp),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Next $pkgSingular", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (!hasOpenedPack && unopenedCount == 0 && medication.packagingConfig != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = semantic.missedContainer,
                            border = BorderStroke(1.dp, semantic.missedBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = semantic.missed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "No unopened ${pkgPlural.lowercase()} remaining.",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.missed
                                    )
                                }
                                Text(
                                    text = "Current usable supply is 0 ${medication.effectiveConsumptionUnit.abbreviation}. Add new $pkgPlural to resume scheduled tracking.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = onRestockClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = semantic.missed),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add $pkgPlural", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (onOpenNextPackageClick != null || onDiscardOpenedClick != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (onOpenNextPackageClick != null && nextUnopenedLot != null && hasOpenedPack) {
                                FilledTonalButton(
                                    onClick = onOpenNextPackageClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Open Next $pkgSingular", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (onDiscardOpenedClick != null && hasOpenedPack) {
                                OutlinedButton(
                                    onClick = onDiscardOpenedClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "Discard Open $pkgSingular",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = semantic.warning
                                    )
                                }
                            }
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
                        text = "$freqDesc · ${sch.doseQuantityPerOccurrence.toDisplayString()} ${unit.formatLabel(sch.doseQuantityPerOccurrence)} per dose",
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
    onAddBatchClick: () -> Unit,
    onOpenLotClick: (StockLot) -> Unit = {},
    onRecountLotClick: (StockLot) -> Unit = {},
    onDiscardLotClick: (StockLot) -> Unit = {},
    onReopenLotClick: (StockLot) -> Unit = {},
    onEditLotClick: (StockLot) -> Unit = {}
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
                                isActiveFefoLot = lot.id == activeFefoLotId,
                                onOpenClick = { onOpenLotClick(lot) },
                                onRecountClick = { onRecountLotClick(lot) },
                                onDiscardClick = { onDiscardLotClick(lot) },
                                onReopenClick = { onReopenLotClick(lot) },
                                onEditClick = { onEditLotClick(lot) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockLotRow(
    lot: StockLot,
    isActiveFefoLot: Boolean = false,
    onOpenClick: () -> Unit = {},
    onRecountClick: () -> Unit = {},
    onDiscardClick: () -> Unit = {},
    onReopenClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    val semantic = MaterialTheme.noveloColors
    val expiryStr = lot.expiryDateEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
    } ?: "No expiry set"

    val isExpiringSoon = lot.expiryDateEpochMs?.let {
        val daysToExpiry = (it - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        daysToExpiry <= 30
    } ?: false

    val discardCutoffStr = lot.effectiveDiscardAfterOpeningEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActiveFefoLot) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = lot.packageLabel ?: lot.lotNumber?.let { "Lot #$it" } ?: "Unnumbered Batch",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PackageStatusChip(status = lot.packageStatus)
                        if (isActiveFefoLot) {
                            Surface(
                                shape = NoveloPillShape,
                                color = semantic.takenContainer,
                                border = BorderStroke(1.dp, semantic.takenBorder)
                            ) {
                                Text(
                                    text = "✓ Active FEFO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.taken,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Remaining: ${lot.remainingQuantity.toDisplayString()} of ${lot.initialQuantity.toDisplayString()} ${lot.unit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall.tabular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (discardCutoffStr != null) {
                        Text(
                            text = "Discard opened pack by: $discardCutoffStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.warning
                        )
                    }
                    if (!lot.lotNumber.isNullOrBlank()) {
                        Text(
                            text = "Lot: ${lot.lotNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (lot.notes != null) {
                        Text(
                            text = lot.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = NoveloPillShape,
                    color = if (isExpiringSoon) semantic.warningContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isExpiringSoon) semantic.warningBorder else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "Exp: $expiryStr",
                        style = MaterialTheme.typography.labelSmall.tabular,
                        color = if (isExpiringSoon) semantic.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            if (lot.packageStatus == PackageStatus.EMPTY && lot.depletionReason != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = NoveloPillShape,
                        color = semantic.assumedContainer,
                        border = BorderStroke(1.dp, semantic.assumedBorder)
                    ) {
                        Text(
                            text = "Depleted Early: ${lot.depletionReason ?: "Finished"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = semantic.assumed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    TextButton(
                        onClick = onReopenClick,
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reopen / Undo", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (lot.packageStatus == PackageStatus.UNOPENED || lot.packageStatus == PackageStatus.OPENED) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (lot.packageStatus == PackageStatus.UNOPENED) {
                        FilledTonalButton(
                            onClick = onOpenClick,
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Open Pack", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (lot.packageStatus == PackageStatus.OPENED) {
                        OutlinedButton(
                            onClick = onRecountClick,
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Recount Pack", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedButton(
                        onClick = onDiscardClick,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Discard", style = MaterialTheme.typography.labelMedium, color = semantic.warning)
                    }
                    TextButton(
                        onClick = onEditClick,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Edit Info", style = MaterialTheme.typography.labelMedium)
                    }
                }
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
                        shape = NoveloPillShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Correct dose record",
                            modifier = Modifier.size(13.dp)
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

    val isInflow = movement.movementType.isInflow
    val prefix = if (isInflow) "+" else "-"
    val textColor = if (isInflow) semantic.taken else semantic.missed
    val containerColor = if (isInflow) semantic.takenContainer else semantic.missedContainer
    val borderColor = if (isInflow) semantic.takenBorder else semantic.missedBorder

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = NoveloCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = movement.movementType.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$timeStr · ${movement.reason ?: "No reason noted"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = NoveloPillShape,
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "$prefix${movement.quantity.toDisplayString()} ${movement.unit.abbreviation}",
                    style = MaterialTheme.typography.labelMedium.tabular,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
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
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onConfirmWithPackaging: ((
        name: String,
        strength: String,
        form: MedicationForm,
        unit: StockUnit,
        lowStockThreshold: String,
        recordingMode: RecordingMode,
        notes: String,
        colorTagHex: String,
        packagingConfig: PackagingConfig?,
        administrationProfile: AdministrationProfile,
        lowStockDaysThreshold: Int?
    ) -> Unit)? = null,
    onConfirmFull: ((
        name: String,
        strength: String,
        form: MedicationForm,
        unit: StockUnit,
        lowStockThreshold: String,
        recordingMode: RecordingMode,
        notes: String,
        colorTagHex: String,
        packagingConfig: PackagingConfig?,
        administrationProfile: AdministrationProfile,
        lowStockDaysThreshold: Int?,
        administrationRoute: AdministrationRoute,
        notificationsEnabled: Boolean
    ) -> Unit)? = null
) {
    val existingPackaging = medication.packagingConfig
    val existingAdmin = medication.administrationProfile

    var name by rememberSaveable { mutableStateOf(medication.name) }
    var strength by rememberSaveable { mutableStateOf(medication.strength) }
    var selectedFormName by rememberSaveable { mutableStateOf(medication.form.name) }
    var selectedRouteName by rememberSaveable {
        mutableStateOf((medication.administrationRoute ?: AdministrationRoute.defaultForForm(medication.form)).name)
    }
    var notificationsEnabled by rememberSaveable { mutableStateOf(medication.notificationsEnabled) }
    var selectedUnitName by rememberSaveable { mutableStateOf(medication.stockUnit.name) }
    var lowStockThresholdStr by rememberSaveable { mutableStateOf(medication.lowStockThreshold.toDisplayString()) }
    var lowStockDaysStr by rememberSaveable { mutableStateOf(medication.lowStockDaysThreshold?.toString() ?: "") }
    var selectedModeName by rememberSaveable { mutableStateOf(medication.recordingMode.name) }
    var notes by rememberSaveable { mutableStateOf(medication.notes) }
    var selectedColorHex by rememberSaveable { mutableStateOf(medication.colorTagHex) }

    var selectedPackageTypeName by rememberSaveable {
        mutableStateOf(
            existingPackaging?.packageType?.name ?: when (medication.form) {
                MedicationForm.DROPS -> PackageType.BOTTLE.name
                MedicationForm.TOPICAL_CREAM -> PackageType.TUBE.name
                MedicationForm.INHALER_PUFFS -> PackageType.INHALER.name
                MedicationForm.LIQUID_ML -> PackageType.BOTTLE.name
                MedicationForm.PATCH -> PackageType.PATCH_BOX.name
                MedicationForm.INJECTION -> PackageType.VIAL.name
                else -> PackageType.BLISTER_PACK.name
            }
        )
    }
    var contentsPerPackageStr by rememberSaveable {
        mutableStateOf(existingPackaging?.contentsPerPackage?.toDisplayString() ?: "")
    }
    var netContentLabel by rememberSaveable { mutableStateOf(existingPackaging?.netContentLabel ?: "") }
    var discardAfterOpeningDaysStr by rememberSaveable {
        mutableStateOf(existingPackaging?.discardAfterOpeningDays?.toString() ?: "")
    }
    var gelTrackingModeName by rememberSaveable {
        mutableStateOf(existingPackaging?.gelTrackingMode?.name ?: GelTrackingMode.ESTIMATED_APPLICATIONS.name)
    }
    var adminSiteName by rememberSaveable { mutableStateOf(existingAdmin.site.name) }
    var leftEyeQtyStr by rememberSaveable {
        mutableStateOf(existingAdmin.leftEyeQuantity?.toDisplayString() ?: "1")
    }
    var rightEyeQtyStr by rememberSaveable {
        mutableStateOf(existingAdmin.rightEyeQuantity?.toDisplayString() ?: "1")
    }
    var containersPerAdminStr by rememberSaveable {
        mutableStateOf(existingAdmin.containersPerAdministration?.toDisplayString() ?: "1")
    }

    val selectedForm = runCatching { MedicationForm.valueOf(selectedFormName) }.getOrDefault(medication.form)
    val selectedRoute = runCatching { AdministrationRoute.valueOf(selectedRouteName) }
        .getOrDefault(medication.administrationRoute ?: AdministrationRoute.defaultForForm(medication.form))
    val selectedUnit = runCatching { StockUnit.valueOf(selectedUnitName) }.getOrDefault(medication.stockUnit)
    val selectedMode = runCatching { RecordingMode.valueOf(selectedModeName) }.getOrDefault(medication.recordingMode)
    val selectedPackageType = runCatching { PackageType.valueOf(selectedPackageTypeName) }
        .getOrDefault(existingPackaging?.packageType ?: PackageType.CUSTOM)
    val selectedGelMode = runCatching { GelTrackingMode.valueOf(gelTrackingModeName) }
        .getOrDefault(GelTrackingMode.ESTIMATED_APPLICATIONS)
    val selectedAdminSite = runCatching { AdministrationSite.valueOf(adminSiteName) }
        .getOrDefault(AdministrationSite.NONE)

    val parsedContents = contentsPerPackageStr.trim().takeIf { it.isNotEmpty() }?.let {
        runCatching { ScaledQuantity.fromDisplayString(it) }.getOrNull()?.takeIf { q -> q.isPositive() }
    }
    val effectiveConsumptionUnit = when {
        selectedForm == MedicationForm.DROPS &&
            (selectedPackageType == PackageType.BOX_SINGLE_DOSE || selectedPackageType == PackageType.SINGLE_DOSE_UNIT) ->
            StockUnit.CONTAINERS
        selectedForm == MedicationForm.DROPS -> StockUnit.DROPS
        selectedForm == MedicationForm.TOPICAL_CREAM && selectedGelMode == GelTrackingMode.ESTIMATED_APPLICATIONS ->
            StockUnit.APPLICATIONS
        selectedForm == MedicationForm.TOPICAL_CREAM && selectedGelMode == GelTrackingMode.GRAMS ->
            StockUnit.GRAMS
        selectedForm == MedicationForm.TOPICAL_CREAM && selectedGelMode == GelTrackingMode.TUBE_COUNT_ONLY ->
            StockUnit.BOTTLES
        else -> selectedUnit
    }
    val effectivePrecision = when {
        selectedForm == MedicationForm.TOPICAL_CREAM && selectedGelMode == GelTrackingMode.TUBE_COUNT_ONLY ->
            TrackingPrecision.UNKNOWN
        selectedForm == MedicationForm.TOPICAL_CREAM && selectedGelMode == GelTrackingMode.ESTIMATED_APPLICATIONS ->
            if (parsedContents != null) TrackingPrecision.ESTIMATED else TrackingPrecision.UNKNOWN
        selectedForm == MedicationForm.TOPICAL_CREAM && selectedGelMode == GelTrackingMode.GRAMS ->
            TrackingPrecision.APPROXIMATE
        selectedForm == MedicationForm.DROPS &&
            selectedPackageType == PackageType.BOTTLE ->
            if (parsedContents != null) TrackingPrecision.ESTIMATED else TrackingPrecision.UNKNOWN
        effectiveConsumptionUnit in setOf(
            StockUnit.TABLETS, StockUnit.CAPSULES, StockUnit.CONTAINERS, StockUnit.PATCHES, StockUnit.PUFFS
        ) -> TrackingPrecision.EXACT
        else -> if (parsedContents != null) TrackingPrecision.APPROXIMATE else TrackingPrecision.EXACT
    }

    val packageChoices = remember(selectedForm) {
        when (selectedForm) {
            MedicationForm.DROPS -> listOf(
                PackageType.BOTTLE,
                PackageType.BOX_SINGLE_DOSE,
                PackageType.SINGLE_DOSE_UNIT
            )
            MedicationForm.TOPICAL_CREAM -> listOf(PackageType.TUBE, PackageType.JAR, PackageType.CUSTOM)
            MedicationForm.INHALER_PUFFS -> listOf(PackageType.INHALER, PackageType.CUSTOM)
            MedicationForm.LIQUID_ML -> listOf(PackageType.BOTTLE, PackageType.VIAL, PackageType.CUSTOM)
            MedicationForm.PATCH -> listOf(PackageType.PATCH_BOX, PackageType.CUSTOM)
            MedicationForm.INJECTION -> listOf(PackageType.VIAL, PackageType.CUSTOM)
            else -> listOf(PackageType.BLISTER_PACK, PackageType.BOTTLE, PackageType.CUSTOM)
        }
    }

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
                                selectedPackageTypeName = when (form) {
                                    MedicationForm.DROPS -> PackageType.BOTTLE.name
                                    MedicationForm.TOPICAL_CREAM -> PackageType.TUBE.name
                                    MedicationForm.INHALER_PUFFS -> PackageType.INHALER.name
                                    MedicationForm.LIQUID_ML -> PackageType.BOTTLE.name
                                    MedicationForm.PATCH -> PackageType.PATCH_BOX.name
                                    MedicationForm.INJECTION -> PackageType.VIAL.name
                                    else -> PackageType.BLISTER_PACK.name
                                }
                                if (form != MedicationForm.DROPS) {
                                    adminSiteName = AdministrationSite.NONE.name
                                } else if (selectedAdminSite == AdministrationSite.NONE) {
                                    adminSiteName = AdministrationSite.BOTH_EYES.name
                                }
                            },
                            label = { Text(form.displayName) }
                        )
                    }
                }

                Text("Administration Route", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdministrationRoute.entries.forEach { route ->
                        FilterChip(
                            selected = selectedRoute == route,
                            onClick = { selectedRouteName = route.name },
                            label = { Text(route.displayName) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Dose Reminders",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (notificationsEnabled) "Notifications active for scheduled doses" else "Reminders muted for this medication",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }

                if (onConfirmWithPackaging != null || onConfirmFull != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Text("Packaging & Shelf Life", style = MaterialTheme.typography.labelLarge)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        packageChoices.forEach { pkgType ->
                            FilterChip(
                                selected = selectedPackageType == pkgType,
                                onClick = {
                                    selectedPackageTypeName = pkgType.name
                                    if (selectedForm == MedicationForm.DROPS) {
                                        selectedUnitName = if (
                                            pkgType == PackageType.BOX_SINGLE_DOSE ||
                                            pkgType == PackageType.SINGLE_DOSE_UNIT
                                        ) {
                                            StockUnit.CONTAINERS.name
                                        } else {
                                            StockUnit.DROPS.name
                                        }
                                    }
                                },
                                label = { Text(pkgType.displayName) }
                            )
                        }
                    }

                    if (selectedForm == MedicationForm.TOPICAL_CREAM) {
                        Text("Gel / Ointment Tracking Mode", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GelTrackingMode.entries.forEach { gelMode ->
                                FilterChip(
                                    selected = selectedGelMode == gelMode,
                                    onClick = { gelTrackingModeName = gelMode.name },
                                    label = { Text(gelMode.displayName) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = contentsPerPackageStr,
                            onValueChange = { contentsPerPackageStr = it },
                            label = { Text("Per ${selectedPackageType.displayName} (${effectiveConsumptionUnit.abbreviation})") },
                            placeholder = { Text("Optional") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discardAfterOpeningDaysStr,
                            onValueChange = { discardAfterOpeningDaysStr = it },
                            label = { Text("Discard Days") },
                            placeholder = { Text("e.g. 28") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.75f)
                        )
                    }

                    OutlinedTextField(
                        value = netContentLabel,
                        onValueChange = { netContentLabel = it },
                        label = { Text("Printed Package Size Label (Optional)") },
                        placeholder = { Text("e.g. 10 ml bottle or 5 g tube") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedForm == MedicationForm.DROPS) {
                        Text("Ophthalmic Eye Selection", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                AdministrationSite.LEFT_EYE,
                                AdministrationSite.RIGHT_EYE,
                                AdministrationSite.BOTH_EYES
                            ).forEach { eyeSite ->
                                FilterChip(
                                    selected = selectedAdminSite == eyeSite,
                                    onClick = { adminSiteName = eyeSite.name },
                                    label = { Text(eyeSite.displayName) }
                                )
                            }
                        }

                        if (selectedPackageType == PackageType.BOX_SINGLE_DOSE ||
                            selectedPackageType == PackageType.SINGLE_DOSE_UNIT
                        ) {
                            OutlinedTextField(
                                value = containersPerAdminStr,
                                onValueChange = { containersPerAdminStr = it },
                                label = { Text("Single-Dose Containers Used Per Administration") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedAdminSite == AdministrationSite.LEFT_EYE ||
                                    selectedAdminSite == AdministrationSite.BOTH_EYES
                                ) {
                                    OutlinedTextField(
                                        value = leftEyeQtyStr,
                                        onValueChange = { leftEyeQtyStr = it },
                                        label = { Text("Left Eye (drops)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (selectedAdminSite == AdministrationSite.RIGHT_EYE ||
                                    selectedAdminSite == AdministrationSite.BOTH_EYES
                                ) {
                                    OutlinedTextField(
                                        value = rightEyeQtyStr,
                                        onValueChange = { rightEyeQtyStr = it },
                                        label = { Text("Right Eye (drops)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lowStockThresholdStr,
                        onValueChange = { lowStockThresholdStr = it },
                        label = { Text("Low-Stock (${effectiveConsumptionUnit.abbreviation})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (onConfirmWithPackaging != null || onConfirmFull != null) {
                        OutlinedTextField(
                            value = lowStockDaysStr,
                            onValueChange = { lowStockDaysStr = it },
                            label = { Text("Runway Days") },
                            placeholder = { Text("e.g. 7") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }

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
                        val trimmedName = name.trim()
                        val trimmedStrength = strength.trim()
                        val trimmedNotes = notes.trim()
                        val isConfirmFull = onConfirmFull != null
                        if (isConfirmFull || onConfirmWithPackaging != null) {
                            val updatedPackaging = PackagingConfig(
                                packageType = selectedPackageType,
                                consumptionUnit = effectiveConsumptionUnit,
                                contentsPerPackage = parsedContents,
                                netContentLabel = netContentLabel.trim().takeIf { it.isNotEmpty() },
                                discardAfterOpeningDays = discardAfterOpeningDaysStr.trim().toIntOrNull()?.takeIf { it > 0 },
                                trackingPrecision = effectivePrecision,
                                gelTrackingMode = if (selectedForm == MedicationForm.TOPICAL_CREAM) selectedGelMode else null
                            )
                            val updatedAdmin = if (selectedForm == MedicationForm.DROPS) {
                                val leftParsed = runCatching { ScaledQuantity.fromDisplayString(leftEyeQtyStr) }.getOrNull()
                                    ?.takeIf { it.isPositive() }
                                val rightParsed = runCatching { ScaledQuantity.fromDisplayString(rightEyeQtyStr) }.getOrNull()
                                    ?.takeIf { it.isPositive() }
                                val containerParsed = runCatching { ScaledQuantity.fromDisplayString(containersPerAdminStr) }.getOrNull()
                                    ?.takeIf { it.isPositive() }
                                AdministrationProfile(
                                    site = if (selectedAdminSite == AdministrationSite.NONE) {
                                        AdministrationSite.BOTH_EYES
                                    } else {
                                        selectedAdminSite
                                    },
                                    leftEyeQuantity = if (
                                        selectedPackageType == PackageType.BOTTLE &&
                                        (selectedAdminSite == AdministrationSite.LEFT_EYE || selectedAdminSite == AdministrationSite.BOTH_EYES)
                                    ) leftParsed else null,
                                    rightEyeQuantity = if (
                                        selectedPackageType == PackageType.BOTTLE &&
                                        (selectedAdminSite == AdministrationSite.RIGHT_EYE || selectedAdminSite == AdministrationSite.BOTH_EYES)
                                    ) rightParsed else null,
                                    containersPerAdministration = if (
                                        selectedPackageType == PackageType.BOX_SINGLE_DOSE ||
                                        selectedPackageType == PackageType.SINGLE_DOSE_UNIT
                                    ) containerParsed else null
                                )
                            } else {
                                AdministrationProfile(site = AdministrationSite.NONE)
                            }
                            if (onConfirmFull != null) {
                                onConfirmFull(
                                    trimmedName,
                                    trimmedStrength,
                                    selectedForm,
                                    effectiveConsumptionUnit,
                                    lowStockThresholdStr,
                                    selectedMode,
                                    trimmedNotes,
                                    selectedColorHex,
                                    updatedPackaging,
                                    updatedAdmin,
                                    lowStockDaysStr.trim().toIntOrNull()?.takeIf { it > 0 },
                                    selectedRoute,
                                    notificationsEnabled
                                )
                            } else {
                                onConfirmWithPackaging?.invoke(
                                    trimmedName,
                                    trimmedStrength,
                                    selectedForm,
                                    effectiveConsumptionUnit,
                                    lowStockThresholdStr,
                                    selectedMode,
                                    trimmedNotes,
                                    selectedColorHex,
                                    updatedPackaging,
                                    updatedAdmin,
                                    lowStockDaysStr.trim().toIntOrNull()?.takeIf { it > 0 }
                                )
                            }
                        } else {
                            onConfirm(
                                trimmedName,
                                trimmedStrength,
                                selectedForm,
                                effectiveConsumptionUnit,
                                lowStockThresholdStr,
                                selectedMode,
                                trimmedNotes,
                                selectedColorHex
                            )
                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestockDialog(
    medication: Medication,
    currentBalance: ScaledQuantity = ScaledQuantity.ZERO,
    hasOpenedPackage: Boolean = false,
    dailyConsumptionRate: ScaledQuantity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        quantity: ScaledQuantity,
        lotNumber: String?,
        expiryDateEpochMs: Long?,
        notes: String?
    ) -> Unit = { _, _, _, _ -> },
    onConfirmPackages: ((
        packageCount: Int,
        contentsPerPackage: ScaledQuantity,
        consumptionUnit: StockUnit,
        packageType: PackageType,
        discardAfterOpeningDays: Int?,
        lotNumber: String?,
        expiryDateEpochMs: Long?,
        notes: String?,
        openFirstPackageImmediately: Boolean
    ) -> Unit)? = null
) {
    val semantic = MaterialTheme.noveloColors
    val packaging = medication.effectivePackagingConfig
    val pkgName = packaging.packageType.displayName
    val pkgPlural = packaging.packageType.pluralName
    val consumptionUnit = packaging.consumptionUnit

    val defaultInitialQty = packaging.contentsPerPackage?.toDisplayString() ?: when (consumptionUnit) {
        StockUnit.DROPS -> "300"
        StockUnit.CONTAINERS -> "30"
        StockUnit.ML -> "100"
        StockUnit.PUFFS -> "120"
        StockUnit.PATCHES -> "7"
        else -> "28"
    }

    var packageCountStr by rememberSaveable { mutableStateOf("1") }
    var contentsPerPackageStr by rememberSaveable { mutableStateOf(defaultInitialQty) }
    var discardDaysStr by rememberSaveable {
        mutableStateOf(packaging.discardAfterOpeningDays?.toString() ?: "")
    }
    var openFirstImmediately by rememberSaveable { mutableStateOf(!hasOpenedPackage) }
    var lotNumber by rememberSaveable { mutableStateOf("") }
    var expiryDaysAheadStr by rememberSaveable { mutableStateOf("365") }
    var notes by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val packagePresets = listOf(1, 2, 3, 5, 7)

    val parsedPackageCount = packageCountStr.trim().toIntOrNull()?.takeIf { it in 1..99 }
    val parsedContentsPerPackage = runCatching { ScaledQuantity.fromDisplayString(contentsPerPackageStr) }
        .getOrNull()?.takeIf { it.isPositive() }

    val effectiveAddedQty = if (parsedPackageCount != null && parsedContentsPerPackage != null) {
        parsedContentsPerPackage * parsedPackageCount.toLong()
    } else {
        null
    }

    val isValid = effectiveAddedQty != null && effectiveAddedQty.isPositive()
    val newBalance = if (effectiveAddedQty != null) currentBalance + effectiveAddedQty else currentBalance
    val daysAhead = expiryDaysAheadStr.toLongOrNull() ?: 365L
    val computedExpiryDateStr = LocalDate.now().plusDays(daysAhead.coerceAtLeast(1L)).format(DATE_FORMATTER)

    val estimatedDaysSupply = if (effectiveAddedQty != null && dailyConsumptionRate != null && dailyConsumptionRate.isPositive()) {
        (newBalance.rawScaledValue / dailyConsumptionRate.rawScaledValue).toInt()
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Restock Supply", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Purchase physical ${pkgPlural.lowercase()} for ${medication.name} (${medication.strength}). Nuvilo tracks consumption internally from your opened ${pkgName.lowercase()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Package Quantity and Capacity Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = packageCountStr,
                        onValueChange = { packageCountStr = it },
                        label = { Text("Number of $pkgPlural *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.9f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contentsPerPackageStr,
                        onValueChange = { contentsPerPackageStr = it },
                        label = { Text("${consumptionUnit.displayName} / $pkgName *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.1f),
                        singleLine = true
                    )
                }

                // Package-Aware Presets (Adapts to package type: bottles, tubes, packs, etc.)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    packagePresets.forEach { countPreset ->
                        FilterChip(
                            selected = packageCountStr == countPreset.toString(),
                            onClick = { packageCountStr = countPreset.toString() },
                            label = { Text("+$countPreset ${if (countPreset == 1) pkgName else pkgPlural}") }
                        )
                    }
                }

                // Immediate Opening Option
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (openFirstImmediately) semantic.takenContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (openFirstImmediately) semantic.takenBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openFirstImmediately = !openFirstImmediately }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open first ${pkgName.lowercase()} immediately",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (openFirstImmediately) semantic.taken else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasOpenedPackage) {
                                    "You already have an opened ${pkgName.lowercase()} in use. Leave unchecked unless starting this new container now."
                                } else {
                                    "Marks package #1 as OPENED and starts discard countdown (if applicable). Remaining containers stay UNOPENED in reserve."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = openFirstImmediately,
                            onCheckedChange = { openFirstImmediately = it }
                        )
                    }
                }

                // Live Post-Restock Impact Preview
                if (effectiveAddedQty != null && effectiveAddedQty.isPositive() && parsedPackageCount != null && parsedContentsPerPackage != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = semantic.takenContainer,
                        border = BorderStroke(1.dp, semantic.takenBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "LIVE RESTOCK SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = semantic.taken
                            )
                            Text(
                                text = "$parsedPackageCount ${if (parsedPackageCount == 1) pkgName else pkgPlural} received (${effectiveAddedQty.toDisplayString()} estimated ${consumptionUnit.abbreviation} in total)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.taken
                            )
                            val breakdownStr = if (openFirstImmediately) {
                                "1 opened ${pkgName.lowercase()} + ${parsedPackageCount - 1} unopened ${pkgPlural.lowercase()} in reserve"
                            } else {
                                "$parsedPackageCount unopened ${pkgPlural.lowercase()} in reserve"
                            }
                            Text(
                                text = breakdownStr,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = semantic.taken
                            )
                            if (estimatedDaysSupply != null) {
                                Text(
                                    text = "Estimated Supply: ~$estimatedDaysSupply days at current configured dosage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.taken.copy(alpha = 0.9f)
                                )
                            }
                            Text(
                                text = "Batch Expiry: $computedExpiryDateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.taken.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = discardDaysStr,
                    onValueChange = { discardDaysStr = it },
                    label = { Text("Discard Window After Opening (Days, Optional)") },
                    placeholder = { Text("e.g. 28 for eye drops") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        val cleanLot = lotNumber.takeIf { it.isNotBlank() }
                        val cleanNotes = notes.takeIf { it.isNotBlank() }
                        if (onConfirmPackages != null && parsedPackageCount != null && parsedContentsPerPackage != null) {
                            onConfirmPackages(
                                parsedPackageCount,
                                parsedContentsPerPackage,
                                consumptionUnit,
                                packaging.packageType,
                                discardDaysStr.trim().toIntOrNull()?.takeIf { it > 0 },
                                cleanLot,
                                expiryEpoch,
                                cleanNotes,
                                openFirstImmediately
                            )
                        } else {
                            onConfirm(
                                effectiveAddedQty!!,
                                cleanLot,
                                expiryEpoch,
                                cleanNotes
                            )
                        }
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
    ) -> Unit,
    onRecountPackageClick: ((lot: StockLot) -> Unit)? = null,
    onDepletePackageClick: ((lot: StockLot) -> Unit)? = null,
    onRevertPackageClick: ((lot: StockLot) -> Unit)? = null
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If physical packages exist, show package inspection list first
                if (lots.isNotEmpty()) {
                    Text(
                        text = "Physical Packages in Inventory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (lot in lots) {
                            val isOpened = lot.packageStatus == PackageStatus.OPENED
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (isOpened) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isOpened) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = lot.packageLabel ?: lot.lotNumber?.let { "Lot #$it" } ?: "Package",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        PackageStatusChip(status = lot.packageStatus)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Remaining: ${lot.remainingQuantity.toDisplayString()} / ${lot.initialQuantity.toDisplayString()} ${lot.unit.abbreviation}",
                                            style = MaterialTheme.typography.bodySmall.tabular
                                        )
                                        val expStr = lot.expiryDateEpochMs?.let {
                                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
                                        } ?: "No expiry"
                                        Text(
                                            text = "Exp: $expStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isOpened) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (onRecountPackageClick != null) {
                                                FilledTonalButton(
                                                    onClick = { onRecountPackageClick(lot) },
                                                    modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Recount This", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (onDepletePackageClick != null) {
                                                OutlinedButton(
                                                    onClick = { onDepletePackageClick(lot) },
                                                    modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    border = BorderStroke(1.dp, semantic.warning.copy(alpha = 0.7f))
                                                ) {
                                                    Text("Mark Finished", style = MaterialTheme.typography.labelSmall, color = semantic.warning)
                                                }
                                            }
                                            if (onRevertPackageClick != null && lot.remainingQuantity == lot.initialQuantity) {
                                                OutlinedButton(
                                                    onClick = { onRevertPackageClick(lot) },
                                                    modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Revert", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                Text(
                    text = "Direct Total Stock Balance Adjustment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

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
                    label = { Text("Actual Total Observed Count (${medication.stockUnit.abbreviation}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Recount / Adjustment *") },
                    placeholder = { Text("e.g. Physical container recount, spill") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (lots.isNotEmpty()) {
                    Text(
                        text = "Apply total adjustment to batch lot:",
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
                                text = "${lot.packageLabel ?: lot.lotNumber ?: "Batch"} (${lot.remainingQuantity.toDisplayString()} rem)",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecountOpenedPackageDialog(
    medication: Medication,
    openedLot: StockLot,
    onDismiss: () -> Unit,
    onConfirm: (observedRemaining: ScaledQuantity, reason: String) -> Unit,
    onDepleteClick: (() -> Unit)? = null,
    onRevertClick: (() -> Unit)? = null
) {
    val semantic = MaterialTheme.noveloColors
    val pkgType = openedLot.packageType ?: PackageType.CUSTOM
    val unitLabel = openedLot.unit.displayName
    val unitAbbr = openedLot.unit.abbreviation
    val pkgLabel = openedLot.packageLabel ?: pkgType.displayName

    var observedStr by rememberSaveable { mutableStateOf(openedLot.remainingQuantity.toDisplayString()) }
    var reason by rememberSaveable { mutableStateOf("Physical recount of opened ${pkgType.displayName.lowercase()}") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val parsedObserved = runCatching { ScaledQuantity.fromDisplayString(observedStr) }
        .getOrNull()?.takeIf { !it.isNegative() }
    val delta = parsedObserved?.let { it - openedLot.remainingQuantity }
    val isValid = parsedObserved != null && reason.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = "Recount Opened ${pkgType.displayName}",
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
                Text(
                    text = "Adjust remaining supply inside $pkgLabel (${medication.name}) only. Unopened packages remain untouched in sealed reserve.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$pkgLabel · Current Recorded: ${openedLot.remainingQuantity.toDisplayString()} / ${openedLot.initialQuantity.toDisplayString()} $unitAbbr",
                            style = MaterialTheme.typography.labelMedium.tabular,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (parsedObserved != null && delta != null) {
                            val sign = if (delta.isPositive()) "+" else ""
                            val deltaStr = "$sign${delta.toDisplayString()} $unitAbbr"
                            val movLabel = when {
                                delta.rawScaledValue == 0L -> "No Balance Change"
                                delta.isPositive() -> "Compensating Movement: +ADJUSTMENT_IN"
                                else -> "Compensating Movement: -ADJUSTMENT_OUT"
                            }
                            Text(
                                text = "${openedLot.remainingQuantity.toDisplayString()} → ${parsedObserved.toDisplayString()} $unitAbbr ($deltaStr)",
                                style = MaterialTheme.typography.titleSmall.tabular,
                                fontWeight = FontWeight.Bold,
                                color = if (delta.isPositive()) semantic.taken else if (delta.rawScaledValue < 0L) semantic.warning else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = movLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick actions on opened container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDepleteClick != null) {
                        OutlinedButton(
                            onClick = onDepleteClick,
                            modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, semantic.warning.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = semantic.warning, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark finished", style = MaterialTheme.typography.labelSmall, color = semantic.warning, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (onRevertClick != null && openedLot.remainingQuantity == openedLot.initialQuantity) {
                        OutlinedButton(
                            onClick = onRevertClick,
                            modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revert to sealed", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = observedStr,
                    onValueChange = { observedStr = it },
                    label = { Text("Observed Remaining in Opened ${pkgType.displayName} ($unitLabel) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Physical recount",
                        "Extra drop or spill",
                        "Nearly empty"
                    ).forEach { preset ->
                        FilterChip(
                            selected = reason == preset,
                            onClick = { reason = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Audit Reason *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        onConfirm(parsedObserved!!, reason.trim())
                    }
                },
                enabled = isValid && !isSubmitting
            ) {
                Text("Update Opened ${pkgType.displayName}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscardPackageDialog(
    medication: Medication,
    lot: StockLot,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    val pkgType = lot.packageType ?: PackageType.CUSTOM
    val pkgLabel = lot.packageLabel ?: pkgType.displayName
    var reason by rememberSaveable { mutableStateOf("Opened past discard window") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = "Discard ${pkgType.displayName}",
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
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = semantic.missedContainer,
                    border = BorderStroke(1.dp, semantic.missedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Wastage Ledger Deduction (-WASTAGE)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = semantic.missed
                        )
                        Text(
                            text = "Discarding $pkgLabel (${medication.name}) writes off ${lot.remainingQuantity.toDisplayString()} ${lot.unit.abbreviation} and excludes this package from active doses. Other unopened packages remain active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Opened past discard window",
                        "Contaminated / damaged",
                        "Expired",
                        "Finished early"
                    ).forEach { preset ->
                        FilterChip(
                            selected = reason == preset,
                            onClick = { reason = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Discard Reason *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isNotBlank() && !isSubmitting) {
                        isSubmitting = true
                        onConfirm(reason.trim())
                    }
                },
                enabled = reason.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = semantic.missed)
            ) {
                Text("Confirm Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EarlyPackageDepletionDialog(
    medication: Medication,
    lot: StockLot,
    unopenedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (reason: DepletionReason, userNote: String?) -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    val pkgType = lot.packageType ?: medication.packagingConfig?.packageType ?: PackageType.CUSTOM
    val pkgSingular = pkgType.displayName
    val pkgLabel = lot.packageLabel ?: lot.lotNumber?.let { "Lot #$it" } ?: "Current $pkgSingular"

    var selectedReason by remember { mutableStateOf(DepletionReason.EMPTY_NO_DROPS_LEFT) }
    var userNote by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val initialQtyStr = "${lot.initialQuantity.toDisplayString()} ${lot.unit.abbreviation}"
    val remainingQtyStr = "${lot.remainingQuantity.toDisplayString()} ${lot.unit.abbreviation}"
    val consumedRaw = (lot.initialQuantity.rawScaledValue - lot.remainingQuantity.rawScaledValue).coerceAtLeast(0L)
    val consumedQtyStr = "${ScaledQuantity(consumedRaw).toDisplayString()} ${lot.unit.abbreviation}"

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = "Has this $pkgSingular run out?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Disclaimer Banner
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "This action updates package inventory. It does not record a medication dose and does not change your adherence history.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Inventory Detail Card
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${medication.name} · $pkgLabel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Original estimated capacity:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(initialQtyStr, style = MaterialTheme.typography.bodySmall.tabular, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Doses recorded consumed:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(consumedQtyStr, style = MaterialTheme.typography.bodySmall.tabular, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated remaining to write off:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(remainingQtyStr, style = MaterialTheme.typography.bodySmall.tabular, fontWeight = FontWeight.Bold, color = semantic.warning)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Unopened packages available:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (unopenedCount > 0) "$unopenedCount unopened available" else "No unopened remaining",
                                style = MaterialTheme.typography.bodySmall.tabular,
                                fontWeight = FontWeight.SemiBold,
                                color = if (unopenedCount > 0) MaterialTheme.colorScheme.primary else semantic.warning
                            )
                        }
                    }
                }

                Text(
                    text = "Reason for finishing package:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DepletionReason.entries.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = userNote,
                    onValueChange = { userNote = it },
                    label = { Text("Optional note (e.g. leakage, dispenser variation)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSubmitting) {
                        isSubmitting = true
                        onConfirm(selectedReason, userNote.trim().ifBlank { null })
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = semantic.warning)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm Finished")
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
fun ReopenPackageDialog(
    medication: Medication,
    lot: StockLot,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    val pkgType = lot.packageType ?: medication.packagingConfig?.packageType ?: PackageType.CUSTOM
    val pkgSingular = pkgType.displayName
    val pkgLabel = lot.packageLabel ?: lot.lotNumber?.let { "Lot #$it" } ?: pkgSingular
    var reasonNote by rememberSaveable { mutableStateOf("Accidental finish marked in error") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = "Reopen $pkgLabel?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "This $pkgSingular was marked finished (${lot.depletionReason ?: "depleted"}). Reopening it will restore the written-off balance and reactivate it for dosing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "A compensating ledger adjustment will be logged to maintain an auditable balance trail. Adherence and dose history remain unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reasonNote,
                    onValueChange = { reasonNote = it },
                    label = { Text("Correction Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSubmitting) {
                        isSubmitting = true
                        onConfirm(reasonNote.trim().ifBlank { "Accidental finish reversal" })
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Reopen $pkgSingular")
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
fun EditPackageDetailsDialog(
    lot: StockLot,
    onDismiss: () -> Unit,
    onConfirm: (
        packageLabel: String?,
        lotNumber: String?,
        expiryDateEpochMs: Long?,
        discardAfterOpeningDays: Int?,
        notes: String?
    ) -> Unit
) {
    val initialDaysAhead = lot.expiryDateEpochMs?.let { exp ->
        ((exp - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0L).toString()
    } ?: ""

    var packageLabel by rememberSaveable { mutableStateOf(lot.packageLabel ?: "") }
    var lotNumber by rememberSaveable { mutableStateOf(lot.lotNumber ?: "") }
    var expiryDaysAheadStr by rememberSaveable { mutableStateOf(initialDaysAhead) }
    var expiryEdited by rememberSaveable { mutableStateOf(false) }
    var discardDaysStr by rememberSaveable { mutableStateOf(lot.discardAfterOpeningDays?.toString() ?: "") }
    var notes by rememberSaveable { mutableStateOf(lot.notes ?: "") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = "Edit ${(lot.packageType ?: PackageType.CUSTOM).displayName} Details",
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
                    value = packageLabel,
                    onValueChange = { packageLabel = it },
                    label = { Text("Package Label (Optional)") },
                    placeholder = { Text("e.g. Bottle #1 (Fridge)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lotNumber,
                    onValueChange = { lotNumber = it },
                    label = { Text("Batch / Lot Number (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = expiryDaysAheadStr,
                    onValueChange = {
                        expiryDaysAheadStr = it
                        expiryEdited = true
                    },
                    label = { Text("Expiry (Days from Today, Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = discardDaysStr,
                    onValueChange = { discardDaysStr = it },
                    label = { Text("Discard After Opening (Days, Optional)") },
                    placeholder = { Text("e.g. 28") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSubmitting) {
                        isSubmitting = true
                        val resolvedExpiry = if (!expiryEdited) {
                            lot.expiryDateEpochMs
                        } else {
                            expiryDaysAheadStr.trim().toLongOrNull()?.let { days ->
                                System.currentTimeMillis() + (days.coerceAtLeast(0L) * 86_400_000L)
                            }
                        }
                        onConfirm(
                            packageLabel.trim().takeIf { it.isNotEmpty() },
                            lotNumber.trim().takeIf { it.isNotEmpty() },
                            resolvedExpiry,
                            discardDaysStr.trim().toIntOrNull()?.takeIf { it > 0 },
                            notes.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Save Package")
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (prescriptions.isNotEmpty()) {
                        TextButton(
                            onClick = onViewHistoryClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Audit Log", maxLines = 1)
                        }
                    }
                    TextButton(
                        onClick = onAddPrescriptionClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add Rx", maxLines = 1)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        notes: String?,
        repeatRequestedDateEpochDay: Long?,
        dispensedDateEpochDay: Long?
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
    var repeatRequestedEpochDay by rememberSaveable { mutableStateOf(existingPrescription?.repeatRequestedDateEpochDay) }
    var dispensedEpochDay by rememberSaveable { mutableStateOf(existingPrescription?.dispensedDateEpochDay) }
    var showIssueDatePicker by rememberSaveable { mutableStateOf(false) }
    var showExpiryDatePicker by rememberSaveable { mutableStateOf(false) }
    var showRepeatReqDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDispensedDatePicker by rememberSaveable { mutableStateOf(false) }
    var quantityStr by rememberSaveable { mutableStateOf(existingPrescription?.quantityPrescribed?.toDisplayString() ?: "28") }
    var repeatsAuthStr by rememberSaveable { mutableStateOf((existingPrescription?.repeatsAuthorised ?: 6).toString()) }
    var repeatsRemStr by rememberSaveable { mutableStateOf((existingPrescription?.repeatsRemaining ?: 6).toString()) }
    var pharmacyName by rememberSaveable { mutableStateOf(existingPrescription?.pharmacyName ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingPrescription?.notes ?: "") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val parsedIssueDate = remember(issueDateStr) {
        runCatching { LocalDate.parse(issueDateStr.trim()) }.getOrNull()
    }
    val parsedExpiryDate = remember(expiryDateStr) {
        expiryDateStr.trim().takeIf { it.isNotEmpty() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
    }

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
                    OutlinedButton(
                        onClick = { showIssueDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Issue Date *",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = parsedIssueDate?.format(DATE_FORMATTER) ?: issueDateStr,
                                style = MaterialTheme.typography.labelLarge.tabular,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showExpiryDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Expiry Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = parsedExpiryDate?.format(DATE_FORMATTER)
                                    ?: if (expiryDateStr.isBlank()) "No expiry" else expiryDateStr,
                                style = MaterialTheme.typography.labelLarge.tabular,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val baseIssue = parsedIssueDate ?: today
                    listOf(
                        "+3m" to baseIssue.plusMonths(3).toString(),
                        "+6m" to baseIssue.plusMonths(6).toString(),
                        "+12m" to baseIssue.plusMonths(12).toString(),
                        "No expiry" to ""
                    ).forEach { (label, value) ->
                        FilterChip(
                            selected = expiryDateStr == value,
                            onClick = {
                                expiryDateStr = value
                                errorMessage = null
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRepeatReqDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Repeat Requested",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = repeatRequestedEpochDay?.let { LocalDate.ofEpochDay(it).format(DATE_FORMATTER) } ?: "Not set",
                                style = MaterialTheme.typography.labelMedium.tabular,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showDispensedDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Dispensed Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dispensedEpochDay?.let { LocalDate.ofEpochDay(it).format(DATE_FORMATTER) } ?: "Not set",
                                style = MaterialTheme.typography.labelMedium.tabular,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
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
                        notes.trim().takeIf { it.isNotEmpty() },
                        repeatRequestedEpochDay,
                        dispensedEpochDay
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

    if (showIssueDatePicker) {
        val initialMillis = (parsedIssueDate ?: today)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showIssueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            issueDateStr = picked.toString()
                            errorMessage = null
                        }
                        showIssueDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showIssueDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showExpiryDatePicker) {
        val initialMillis = (parsedExpiryDate ?: (parsedIssueDate ?: today).plusMonths(6))
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            expiryDateStr = picked.toString()
                            errorMessage = null
                        }
                        showExpiryDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRepeatReqDatePicker) {
        val initialMillis = (repeatRequestedEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showRepeatReqDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            repeatRequestedEpochDay = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()
                        }
                        showRepeatReqDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                Row {
                    if (repeatRequestedEpochDay != null) {
                        TextButton(onClick = {
                            repeatRequestedEpochDay = null
                            showRepeatReqDatePicker = false
                        }) { Text("Clear") }
                    }
                    TextButton(onClick = { showRepeatReqDatePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDispensedDatePicker) {
        val initialMillis = (dispensedEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDispensedDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dispensedEpochDay = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()
                        }
                        showDispensedDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                Row {
                    if (dispensedEpochDay != null) {
                        TextButton(onClick = {
                            dispensedEpochDay = null
                            showDispensedDatePicker = false
                        }) { Text("Clear") }
                    }
                    TextButton(onClick = { showDispensedDatePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestRepeatDialog(
    prescription: Prescription,
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (requestedQuantity: ScaledQuantity?, notes: String?) -> Unit
) {
    val initialQty = prescription.quantityPrescribed?.toDisplayString()
        ?: when (medication.stockUnit) {
            StockUnit.DROPS -> "30"
            StockUnit.ML -> "100"
            StockUnit.PUFFS -> "120"
            StockUnit.BOTTLES -> "1"
            StockUnit.PATCHES -> "7"
            else -> "28"
        }
    var quantityStr by rememberSaveable { mutableStateOf(initialQty) }
    var notes by rememberSaveable { mutableStateOf(prescription.notes ?: "") }
    var selectedChannel by rememberSaveable { mutableStateOf("NHS App / Online") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val parsedQty = runCatching { ScaledQuantity.fromDisplayString(quantityStr) }.getOrNull()
    val isValid = parsedQty != null && parsedQty.isPositive()

    val presets = when (medication.stockUnit) {
        StockUnit.DROPS -> listOf("15", "30", "60")
        StockUnit.ML -> listOf("50", "100", "150", "200")
        StockUnit.PUFFS -> listOf("60", "120", "200")
        StockUnit.BOTTLES -> listOf("1", "2", "3")
        StockUnit.PATCHES -> listOf("5", "7", "14", "28")
        else -> listOf("14", "28", "56", "84")
    }

    val channels = listOf("NHS App / Online", "Phone call to GP", "In-person at reception", "Pharmacy managed")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Request Repeat from GP", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Record an order submitted to your GP surgery for ${medication.name} (${medication.strength}). Nuvilo will track this pending order in reminders and stock forecasts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Requested (${medication.stockUnit.displayName}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = quantityStr == preset,
                            onClick = { quantityStr = preset },
                            label = { Text("$preset ${medication.stockUnit.abbreviation}") }
                        )
                    }
                }

                Text(
                    text = "Request Channel / Method:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    channels.forEach { ch ->
                        FilterChip(
                            selected = selectedChannel == ch,
                            onClick = { selectedChannel = ch },
                            label = { Text(ch) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Requested via Dr. Smith surgery portal") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        val combinedNotes = buildString {
                            append("Requested via $selectedChannel")
                            if (notes.isNotBlank() && notes.trim() != prescription.notes?.trim()) {
                                append(" · ").append(notes.trim())
                            }
                        }
                        onConfirm(parsedQty, combinedNotes)
                    }
                },
                enabled = isValid && !isSubmitting
            ) {
                Text("Confirm Repeat Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectRepeatDialog(
    prescription: Prescription,
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (
        notes: String?,
        autoRestockQuantity: ScaledQuantity?,
        lotNumber: String?,
        expiryDateEpochMs: Long?
    ) -> Unit
) {
    val semantic = MaterialTheme.noveloColors
    val initialQty = prescription.quantityPrescribed?.toDisplayString()
        ?: when (medication.stockUnit) {
            StockUnit.DROPS -> "30"
            StockUnit.ML -> "100"
            StockUnit.PUFFS -> "120"
            StockUnit.BOTTLES -> "1"
            StockUnit.PATCHES -> "7"
            else -> "28"
        }
    var autoRestock by rememberSaveable { mutableStateOf(true) }
    var quantityStr by rememberSaveable { mutableStateOf(initialQty) }
    var lotNumber by rememberSaveable { mutableStateOf("") }
    var expiryDaysAheadStr by rememberSaveable { mutableStateOf("365") }
    var notes by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    val parsedQty = runCatching { ScaledQuantity.fromDisplayString(quantityStr) }.getOrNull()
    val isValid = !autoRestock || (parsedQty != null && parsedQty.isPositive())
    val daysAhead = expiryDaysAheadStr.toLongOrNull() ?: 365L
    val computedExpiryDateStr = LocalDate.now().plusDays(daysAhead.coerceAtLeast(1L)).format(DATE_FORMATTER)

    val presets = when (medication.stockUnit) {
        StockUnit.DROPS -> listOf("15", "30", "60")
        StockUnit.ML -> listOf("50", "100", "150", "200")
        StockUnit.PUFFS -> listOf("60", "120", "200")
        StockUnit.BOTTLES -> listOf("1", "2", "3")
        StockUnit.PATCHES -> listOf("5", "7", "14", "28")
        else -> listOf("14", "28", "56", "84")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Collect Repeat Prescription", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Confirm receipt of repeat prescription from pharmacy for ${medication.name}. This decrements authorized repeats remaining from ${prescription.repeatsRemaining} to ${prescription.repeatsRemaining - 1} and marks the order as fulfilled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (autoRestock) semantic.takenContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (autoRestock) semantic.takenBorder else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoRestock = !autoRestock }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restock Inventory Automatically",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (autoRestock) semantic.taken else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Adds the collected medication straight to your current stock ledger",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoRestock,
                            onCheckedChange = { autoRestock = it }
                        )
                    }
                }

                if (autoRestock) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity Received (${medication.stockUnit.displayName}) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = quantityStr == preset,
                                onClick = { quantityStr = preset },
                                label = { Text("$preset ${medication.stockUnit.abbreviation}") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = lotNumber,
                        onValueChange = { lotNumber = it },
                        label = { Text("Batch / Lot Number (Optional)") },
                        placeholder = { Text("e.g. LOT-2026-X") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = expiryDaysAheadStr,
                        onValueChange = { expiryDaysAheadStr = it },
                        label = { Text("Expiry (Days Ahead — $computedExpiryDateStr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Collected from Boots High Street") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        val expiryEpoch = if (autoRestock) System.currentTimeMillis() + (daysAhead * 24 * 60 * 60 * 1000) else null
                        onConfirm(
                            notes.takeIf { it.isNotBlank() },
                            if (autoRestock) parsedQty else null,
                            lotNumber.takeIf { it.isNotBlank() },
                            expiryEpoch
                        )
                    }
                },
                enabled = isValid && !isSubmitting
            ) {
                Text(if (autoRestock) "Collect & Restock" else "Confirm Collected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
