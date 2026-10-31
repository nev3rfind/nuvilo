package com.example.nuvilo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nuvilo.domain.adherence.DoseOutcomeFilter
import com.example.nuvilo.domain.adherence.HistoryDateFilter
import com.example.nuvilo.domain.adherence.ResolvedDoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.ui.components.DoseStatusBadge
import com.example.nuvilo.ui.viewmodel.MedicationDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDoseHistoryScreen(
    viewModel: MedicationDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var itemToCorrect by remember { mutableStateOf<ResolvedDoseOccurrence?>(null) }

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
                    Column {
                        Text(
                            text = "Dose History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.medication?.let { med ->
                            Text(
                                text = "${med.name} · ${med.strength}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to medication details"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Adherence Summary Card for this Medication
            item {
                uiState.medicationAdherenceSummary?.let { summary ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "30-Day Adherence: ${summary.adherencePercentage?.let { "$it%" } ?: "—"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${summary.confirmedCount} Confirmed · ${summary.assumedCount} Assumed · ${summary.skippedCount} Skipped · ${summary.missedCount} Missed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Date Range Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filter by Date Range",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryDateFilter.values().forEach { filter ->
                            FilterChip(
                                selected = uiState.historyDateFilter == filter,
                                onClick = { viewModel.setHistoryDateFilter(filter) },
                                label = { Text(filter.displayName) }
                            )
                        }
                    }
                }
            }

            // Outcome Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filter by Outcome",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DoseOutcomeFilter.values().forEach { filter ->
                            FilterChip(
                                selected = uiState.historyOutcomeFilter == filter,
                                onClick = { viewModel.setHistoryOutcomeFilter(filter) },
                                label = { Text(filter.displayName) }
                            )
                        }
                    }
                }
            }

            if (uiState.fullDoseHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Matching Dose Records",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.historyDateFilter != HistoryDateFilter.ALL_TIME ||
                                    uiState.historyOutcomeFilter != DoseOutcomeFilter.ALL
                                ) {
                                    "No doses match the selected date range and outcome filters."
                                } else {
                                    "No past scheduled or recorded doses exist for this medication yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.historyDateFilter != HistoryDateFilter.ALL_TIME ||
                                uiState.historyOutcomeFilter != DoseOutcomeFilter.ALL
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.setHistoryDateFilter(HistoryDateFilter.ALL_TIME)
                                        viewModel.setHistoryOutcomeFilter(DoseOutcomeFilter.ALL)
                                    }
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.fullDoseHistory, key = { it.occurrence.occurrenceId }) { resolved ->
                    DetailedHistoryItemCard(
                        resolved = resolved,
                        onCorrectClick = { itemToCorrect = resolved }
                    )
                }
            }
        }
    }

    if (itemToCorrect != null) {
        val target = itemToCorrect!!
        HistoryCorrectDoseDialog(
            resolved = target,
            onDismiss = { itemToCorrect = null },
            onConfirm = { newStatus, reason ->
                viewModel.correctDose(
                    occurrenceId = target.occurrence.occurrenceId,
                    targetStatus = newStatus,
                    reason = reason,
                    fallbackOccurrence = target.occurrence
                )
                itemToCorrect = null
            }
        )
    }
}

@Composable
private fun DetailedHistoryItemCard(
    resolved: ResolvedDoseOccurrence,
    onCorrectClick: () -> Unit
) {
    val occ = resolved.occurrence
    val zone = ZoneId.systemDefault()
    val scheduledFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm")
    val actionFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")

    val scheduledStr = Instant.ofEpochMilli(occ.scheduledEpochMs).atZone(zone).format(scheduledFmt)
    val recordedStr = occ.recordedEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(actionFmt)
    }
    val correctedStr = occ.correctedAtEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(actionFmt)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scheduled: $scheduledStr",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dose: ${occ.doseQuantity.toDisplayString()} ${occ.unit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (resolved.isCorrected) {
                        Text(
                            text = "Corrected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    DoseStatusBadge(status = occ.status)
                    IconButton(onClick = onCorrectClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Correct dose record",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Explicit distinction between Scheduled Time and Action / Recording Time
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (recordedStr != null) {
                        "Action recorded time: $recordedStr"
                    } else {
                        "Action recorded time: Not recorded (determined from past schedule)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (correctedStr != null) {
                    Text(
                        text = "Corrected at: $correctedStr",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!occ.correctionNotes.isNullOrBlank()) {
                    Text(
                        text = "Correction audit note: ${occ.correctionNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (!occ.notes.isNullOrBlank()) {
                    Text(
                        text = "Note: ${occ.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCorrectDoseDialog(
    resolved: ResolvedDoseOccurrence,
    onDismiss: () -> Unit,
    onConfirm: (DoseStatus, String) -> Unit
) {
    val selectableStatuses = listOf(
        DoseStatus.TAKEN,
        DoseStatus.ASSUMED,
        DoseStatus.SKIPPED,
        DoseStatus.MISSED
    )
    var selectedStatus by remember { mutableStateOf(resolved.occurrence.status) }
    var reasonText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Correct Historical Dose") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Historical records are preserved auditably. Stock ledger movements will automatically compensate if changing between Taken/Assumed and Skipped/Missed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                selectableStatuses.forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = {
                        reasonText = it
                        errorText = null
                    },
                    label = { Text("Reason for correction (Required)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = reasonText.trim()
                    if (trimmed.isEmpty()) {
                        errorText = "Please enter an audit reason for this correction."
                    } else {
                        onConfirm(selectedStatus, trimmed)
                    }
                }
            ) {
                Text("Save Correction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
