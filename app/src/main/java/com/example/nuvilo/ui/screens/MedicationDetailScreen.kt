package com.example.nuvilo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEvent
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.components.DoseStatusBadge
import com.example.nuvilo.ui.components.StockBadge
import com.example.nuvilo.ui.theme.StatusMissed
import com.example.nuvilo.ui.theme.StatusMissedContainer
import com.example.nuvilo.ui.theme.StatusSkipped
import com.example.nuvilo.ui.theme.StatusSkippedContainer
import com.example.nuvilo.ui.theme.StatusTaken
import com.example.nuvilo.ui.theme.StatusTakenContainer
import com.example.nuvilo.ui.theme.StatusWarning
import com.example.nuvilo.ui.theme.StatusWarningContainer
import com.example.nuvilo.ui.viewmodel.MedicationDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    var showRestockDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var doseToCorrect by remember { mutableStateOf<DoseOccurrence?>(null) }
    var showPrescriptionFormDialog by remember { mutableStateOf(false) }
    var prescriptionToEdit by remember { mutableStateOf<Prescription?>(null) }
    var showPrescriptionHistoryDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.medication?.name ?: "Medication Details",
                        maxLines = 1
                    )
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
                    uiState.medication?.let { med ->
                        IconButton(
                            onClick = { viewModel.setArchived(!med.isArchived) }
                        ) {
                            Icon(
                                imageVector = if (med.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (med.isArchived) "Unarchive medication" else "Archive medication"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val med = uiState.medication
            if (med == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Medication not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Medication Header & Form Card
                    item {
                        MedicationHeaderCard(med = med)
                    }

                    // 2. Stock Balance & Runway Summary Card
                    item {
                        StockOverviewCard(
                            balance = uiState.stockBalance,
                            onRestockClick = { showRestockDialog = true },
                            onAdjustClick = { showAdjustDialog = true }
                        )
                    }

                    // 3. Schedule & Adherence Mode Card
                    item {
                        ScheduleSummaryCard(
                            schedules = uiState.schedules,
                            recordingMode = med.recordingMode
                        )
                    }

                    // 3b. Prescription & Repeat Tracking Section
                    item {
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

                    // 4. Tracked Batches & Supplies (Lots)
                    item {
                        LotsSectionHeader(lotsCount = uiState.lots.size)
                    }

                    if (uiState.lots.isEmpty()) {
                        item {
                            EmptyLotCard(onAddLotClick = { showRestockDialog = true })
                        }
                    } else {
                        items(uiState.lots, key = { it.id }) { lot ->
                            LotItemCard(lot = lot)
                        }
                    }

                    // 5. Recent Dose History & Corrections
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Dose Records",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onNavigateToDoseHistory) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Full History")
                            }
                        }
                    }

                    if (uiState.recentDoses.isEmpty()) {
                        item {
                            Text(
                                text = "No doses recorded yet for this treatment.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.recentDoses, key = { it.occurrenceId }) { dose ->
                            DoseHistoryRow(
                                dose = dose,
                                onCorrectClick = { doseToCorrect = dose }
                            )
                        }
                    }

                    // 6. Recent Stock Movements Audit Trail
                    item {
                        Text(
                            text = "Stock Ledger Audit Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (uiState.recentMovements.isEmpty()) {
                        item {
                            Text(
                                text = "No ledger movements recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.recentMovements, key = { it.id }) { movement ->
                            StockMovementRow(movement = movement)
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showRestockDialog && uiState.medication != null) {
        RestockDialog(
            medication = uiState.medication!!,
            onDismiss = { showRestockDialog = false },
            onConfirm = { quantity, lotNum, expiryEpoch, notes ->
                viewModel.restock(
                    quantity = quantity,
                    lotNumber = lotNum,
                    expiryDateEpochMs = expiryEpoch,
                    notes = notes
                )
                showRestockDialog = false
            }
        )
    }

    if (showAdjustDialog && uiState.medication != null) {
        AdjustStockDialog(
            medication = uiState.medication!!,
            currentBalance = uiState.stockBalance?.currentBalance ?: ScaledQuantity.ZERO,
            lots = uiState.lots,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { targetBalance, reason, targetLotId ->
                viewModel.adjustStock(
                    targetBalance = targetBalance,
                    reason = reason,
                    targetLotId = targetLotId
                )
                showAdjustDialog = false
            }
        )
    }

    doseToCorrect?.let { dose ->
        CorrectDoseDialog(
            dose = dose,
            medicationName = uiState.medication?.name ?: "",
            onDismiss = { doseToCorrect = null },
            onConfirm = { newStatus, reason ->
                viewModel.correctDose(
                    occurrenceId = dose.occurrenceId,
                    targetStatus = newStatus,
                    reason = reason
                )
                doseToCorrect = null
            }
        )
    }

    if (showPrescriptionFormDialog && uiState.medication != null) {
        AddEditPrescriptionDialog(
            existingPrescription = prescriptionToEdit,
            stockUnit = uiState.medication!!.stockUnit,
            onDismiss = {
                showPrescriptionFormDialog = false
                prescriptionToEdit = null
            },
            onConfirm = { refNum, issueDay, expiryDay, qtyPrescribed, repeatsAuth, repeatsRem, pharmacy, notes ->
                viewModel.savePrescription(
                    existingPrescription = prescriptionToEdit,
                    referenceNumber = refNum,
                    issueDateEpochDay = issueDay,
                    expiryDateEpochDay = expiryDay,
                    quantityPrescribed = qtyPrescribed,
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
private fun MedicationHeaderCard(med: Medication) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = med.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${med.strength} · ${med.form.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (med.notes.isNotBlank()) {
                    Text(
                        text = med.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (med.isArchived) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Archived",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StockOverviewCard(
    balance: StockBalance?,
    onRestockClick: () -> Unit,
    onAdjustClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventory Balance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (balance != null) {
                    StockBadge(balance = balance)
                }
            }

            if (balance != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = balance.currentBalance.toDisplayString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance.isLowStock) StatusWarning else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = balance.unit.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Low Stock Warning Banner
                if (balance.isLowStock) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusWarningContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (balance.isNegative) "Stock depleted below zero. Please adjust count." else "Low stock alert: Refill recommended soon.",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusWarning,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Days remaining runway
                if (balance.daysRemaining != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Estimated ${balance.daysRemaining} days remaining at scheduled consumption rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (balance.isEstimate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Estimated consumption (sprays/drops). Exact days remaining cannot be calculated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Actions: Restock and Adjust Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRestockClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Restock")
                }

                OutlinedButton(
                    onClick = onAdjustClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Adjust Count")
                }
            }
        }
    }
}

@Composable
private fun ScheduleSummaryCard(
    schedules: List<ScheduleDefinition>,
    recordingMode: RecordingMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Schedule & Adherence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (schedules.isEmpty()) {
                Text(
                    text = "No active reminders configured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                for (sch in schedules) {
                    val timesFormatted = sch.targetTimesMinuteOfDay.joinToString(", ") { minute ->
                        String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)
                    }
                    Text(
                        text = "${sch.scheduleType.displayName}: $timesFormatted (${sch.doseQuantityPerOccurrence.toDisplayString()} per dose)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Dose Recording Preference:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = recordingMode.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LotsSectionHeader(lotsCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tracked Supplies & Lots ($lotsCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (lotsCount > 1) {
            Text(
                text = "FEFO Policy Active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyLotCard(onAddLotClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "No batch lots tracked yet",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Track individual supplies, lot numbers, and expiration dates with FEFO auto-allocation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddLotClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Record New Supply")
            }
        }
    }
}

@Composable
private fun LotItemCard(lot: StockLot) {
    val expiryStr = if (lot.expiryDateEpochMs != null) {
        val date = Instant.ofEpochMilli(lot.expiryDateEpochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    } else {
        "No expiry date"
    }

    val isExpiringSoon = lot.expiryDateEpochMs != null &&
        (lot.expiryDateEpochMs - System.currentTimeMillis() < 30L * 24 * 60 * 60 * 1000)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = lot.lotNumber?.let { "Lot #$it" } ?: "Unnumbered Batch",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isExpiringSoon) StatusWarningContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Exp: $expiryStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpiringSoon) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
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
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm")
    val timeStr = Instant.ofEpochMilli(dose.scheduledEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Column {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
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
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
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

            // Correction annotation if present
            if (dose.correctionNotes != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        text = "Correction note: ${dose.correctionNotes}",
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
    val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm")
    val timeStr = Instant.ofEpochMilli(movement.timestampEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = movement.movementType.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$timeStr · ${movement.reason ?: "No reason noted"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val prefix = if (movement.movementType.isInflow) "+" else "-"
        val textColor = if (movement.movementType.isInflow) StatusTaken else MaterialTheme.colorScheme.error

        Text(
            text = "$prefix${movement.quantity.toDisplayString()} ${movement.unit.abbreviation}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun RestockDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (
        quantity: ScaledQuantity,
        lotNumber: String?,
        expiryDateEpochMs: Long?,
        notes: String?
    ) -> Unit
) {
    var quantityStr by remember { mutableStateOf("30") }
    var lotNumber by remember { mutableStateOf("") }
    var expiryDaysAheadStr by remember { mutableStateOf("365") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val parsedQty = runCatching { ScaledQuantity.fromDisplayString(quantityStr) }.getOrNull()
    val isValid = parsedQty != null && parsedQty.isPositive()

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
                    text = "Recording new supply for ${medication.name} (${medication.strength}). Appends an auditable RESTOCK ledger movement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Added (${medication.stockUnit.displayName}) *") },
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
                    label = { Text("Expiry (Days from Today)") },
                    placeholder = { Text("e.g. 180 or 365") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Prescription refill") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        val daysAhead = expiryDaysAheadStr.toLongOrNull() ?: 365L
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
    var targetBalanceStr by remember { mutableStateOf(currentBalance.toDisplayString()) }
    var reason by remember { mutableStateOf("") }
    var selectedLotId by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val parsedTarget = runCatching { ScaledQuantity.fromDisplayString(targetBalanceStr) }.getOrNull()
    val isValid = parsedTarget != null && reason.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Adjust Stock Count", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Current calculated balance: ${currentBalance.toDisplayString()} ${medication.stockUnit.abbreviation}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = targetBalanceStr,
                    onValueChange = { targetBalanceStr = it },
                    label = { Text("Actual Observed Balance *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (parsedTarget != null) {
                    val delta = parsedTarget - currentBalance
                    val deltaPrefix = if (delta.isPositive()) "+" else ""
                    Text(
                        text = "Ledger movement delta: $deltaPrefix${delta.toDisplayString()} ${medication.stockUnit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Correction *") },
                    placeholder = { Text("e.g. Recounted blister pack, dropped pill") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (lots.isNotEmpty()) {
                    Text(
                        text = "Apply to specific lot (Default: FEFO auto-allocation):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLotId == null,
                            onClick = { selectedLotId = null }
                        )
                        Text("Auto (FEFO / Oldest Lot)", style = MaterialTheme.typography.bodySmall)
                    }
                    for (lot in lots) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                        onConfirm(parsedTarget!!, reason, selectedLotId)
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
    var selectedStatus by remember { mutableStateOf(dose.status) }
    var reason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val isValid = selectedStatus != dose.status && reason.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Correct Dose Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Correcting historical record for $medicationName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Current Status: ${dose.status.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Select Revised Status:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                listOf(DoseStatus.TAKEN, DoseStatus.SKIPPED, DoseStatus.MISSED).forEach { statusOption ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == statusOption,
                            onClick = { selectedStatus = statusOption }
                        )
                        Text(text = statusOption.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Revision *") },
                    placeholder = { Text("e.g. Logged by mistake, forgot to record earlier") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Safety Note: If changing from Taken to Skipped/Missed, a compensating DOSE_REVERSAL will automatically be recorded in the stock ledger to restore inventory balance.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        isSubmitting = true
                        onConfirm(selectedStatus, reason)
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
    val todayEpochDay = LocalDate.now().toEpochDay()
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    val primaryRx = prescriptions.firstOrNull { it.effectiveStatus(todayEpochDay) == PrescriptionStatus.ACTIVE }
        ?: prescriptions.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (prescriptions.isNotEmpty()) {
                        TextButton(onClick = onViewHistoryClick) {
                            Text("History")
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
                    text = "No prescription recorded for this medication yet. Add prescription details to track authorised repeats, issue/expiry dates, and pharmacy information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val effectiveStatus = primaryRx.effectiveStatus(todayEpochDay)
                val issueDateStr = LocalDate.ofEpochDay(primaryRx.issueDateEpochDay).format(dateFmt)
                val expiryDateStr = primaryRx.expiryDateEpochDay?.let { LocalDate.ofEpochDay(it).format(dateFmt) }
                val daysToExpiry = primaryRx.daysUntilExpiry(todayEpochDay)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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

                // Upcoming Expiry / Expired Warning Banner
                if (effectiveStatus == PrescriptionStatus.EXPIRED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StatusMissedContainer)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Expired prescription warning",
                            tint = StatusMissed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "This prescription has expired and is no longer active. Contact your prescriber or pharmacy for a new prescription.",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusMissed
                        )
                    }
                } else if (primaryRx.isExpiringSoon(todayEpochDay)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StatusWarningContainer)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Upcoming expiry warning",
                            tint = StatusWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Prescription expires in ${daysToExpiry ?: 0} day(s) ($expiryDateStr).",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWarning
                        )
                    }
                }

                // Repeats & Pharmacy Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Repeats remaining: ${primaryRx.repeatsRemaining} of ${primaryRx.repeatsAuthorised} authorised",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (primaryRx.hasPendingRepeatRequest) {
                            Text(
                                text = "Status: Repeat requested — awaiting collection",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusWarning
                            )
                        }
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (primaryRx.hasPendingRepeatRequest) "Repeat Requested" else "Request Repeat",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Button(
                            onClick = { onCollectRepeatClick(primaryRx) },
                            enabled = primaryRx.canCollectRepeat(todayEpochDay),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Collect Repeat",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Mandatory Clinical & Manual Maintenance Disclaimer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
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
                    text = "Prescription information is manually maintained for personal tracking. Requesting a repeat here logs a local note only and does not contact a pharmacy or decrement repeats until marked collected. Always verify details with your pharmacy or prescriber.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrescriptionStatusBadge(status: PrescriptionStatus) {
    val (bg, fg, icon, text) = when (status) {
        PrescriptionStatus.ACTIVE -> listOf(StatusTakenContainer, StatusTaken, Icons.Default.Check, "Active")
        PrescriptionStatus.EXPIRED -> listOf(StatusMissedContainer, StatusMissed, Icons.Default.Warning, "Expired")
        PrescriptionStatus.COMPLETED -> listOf(StatusSkippedContainer, StatusSkipped, Icons.Default.Check, "Completed")
        PrescriptionStatus.ARCHIVED -> listOf(StatusSkippedContainer, StatusSkipped, Icons.Default.Archive, "Archived")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg as Color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
            contentDescription = null,
            tint = fg as Color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text as String,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

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
        ?: ""

    var referenceNumber by remember { mutableStateOf(existingPrescription?.referenceNumber ?: "") }
    var issueDateStr by remember { mutableStateOf(initialIssueStr) }
    var expiryDateStr by remember { mutableStateOf(initialExpiryStr) }
    var quantityStr by remember { mutableStateOf(existingPrescription?.quantityPrescribed?.toDisplayString() ?: "") }
    var repeatsAuthStr by remember { mutableStateOf((existingPrescription?.repeatsAuthorised ?: 0).toString()) }
    var repeatsRemStr by remember { mutableStateOf((existingPrescription?.repeatsRemaining ?: 0).toString()) }
    var pharmacyName by remember { mutableStateOf(existingPrescription?.pharmacyName ?: "") }
    var notes by remember { mutableStateOf(existingPrescription?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(if (existingPrescription == null) "Add Prescription" else "Edit Prescription", fontWeight = FontWeight.Bold)
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = issueDateStr,
                    onValueChange = {
                        issueDateStr = it
                        errorMessage = null
                    },
                    label = { Text("Issue Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiryDateStr,
                    onValueChange = {
                        expiryDateStr = it
                        errorMessage = null
                    },
                    label = { Text("Expiry Date (YYYY-MM-DD, Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Prescribed (${stockUnit.abbreviation}, Optional)") },
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
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = pharmacyName,
                    onValueChange = { pharmacyName = it },
                    label = { Text("Pharmacy Name (Optional)") },
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
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    val tsFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    val zone = ZoneId.systemDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prescription & Repeat History") },
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
                    val issueStr = LocalDate.ofEpochDay(rx.issueDateEpochDay).format(dateFmt)
                    val expStr = rx.expiryDateEpochDay?.let { LocalDate.ofEpochDay(it).format(dateFmt) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rx.referenceNumber ?: "Prescription",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Repeat & Status Audit Log (${events.size})",
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
                        val tsStr = Instant.ofEpochMilli(ev.timestampEpochMs).atZone(zone).format(tsFmt)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${ev.eventType.displayName} · $tsStr",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
