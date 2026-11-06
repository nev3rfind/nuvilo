package com.example.nuvilo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.domain.adherence.AdherenceSummary
import com.example.nuvilo.domain.adherence.DoseOutcomeFilter
import com.example.nuvilo.domain.adherence.HistoryDateFilter
import com.example.nuvilo.domain.adherence.ResolvedDoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.ui.components.DoseStatusBadge
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.SectionHeader
import com.example.nuvilo.ui.theme.NoveloPillShape
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.theme.tabular
import com.example.nuvilo.ui.viewmodel.MedicationDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val GROUP_DATE_HEADER_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val RECORDED_TS_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationDoseHistoryScreen(
    viewModel: MedicationDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var resolvedToCorrect by remember { mutableStateOf<ResolvedDoseOccurrence?>(null) }

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
    val today = LocalDate.now()
    val groupedByDate = remember(uiState.fullDoseHistory) {
        val zone = ZoneId.systemDefault()
        uiState.fullDoseHistory.groupBy { resolved ->
            Instant.ofEpochMilli(resolved.occurrence.scheduledEpochMs).atZone(zone).toLocalDate()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = med?.let { "${it.name} Dose History" } ?: "Dose History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (med != null) {
                            Text(
                                text = "${med.strength} · ${med.recordingMode.displayName}",
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
                            contentDescription = "Back to medication details"
                        )
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
                    Text(uiState.errorMessage ?: "Medication not found")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                ) {
                    // 1. 30-Day Adherence Summary Banner
                    val summary = uiState.medicationAdherenceSummary
                    if (summary != null) {
                        item(key = "med_adherence_banner", contentType = "adherence_banner") {
                            MedicationAdherenceSummaryBanner(summary = summary)
                        }
                    }

                    // 2. Date & Outcome Filter Card
                    item(key = "filter_controls", contentType = "filter_card") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Date Range",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HistoryDateFilter.entries.forEach { df ->
                                        FilterChip(
                                            selected = uiState.historyDateFilter == df,
                                            onClick = { viewModel.setHistoryDateFilter(df) },
                                            label = { Text(df.displayName) }
                                        )
                                    }
                                }

                                Text(
                                    text = "Outcome Status",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    DoseOutcomeFilter.entries.forEach { of ->
                                        FilterChip(
                                            selected = uiState.historyOutcomeFilter == of,
                                            onClick = { viewModel.setHistoryOutcomeFilter(of) },
                                            label = { Text(of.displayName) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Grouped Chronological History List
                    if (uiState.fullDoseHistory.isEmpty()) {
                        item(key = "empty_history", contentType = "empty_state") {
                            EmptyStateCard(
                                icon = Icons.Default.History,
                                title = "No Matching Dose Occurrences",
                                description = "No scheduled or recorded doses match the selected date range and outcome filter."
                            )
                        }
                    } else {
                        groupedByDate.forEach { (date, itemsForDate) ->
                            val dateTitle = when (date) {
                                today -> "Today · ${date.format(GROUP_DATE_HEADER_FORMATTER)}"
                                today.minusDays(1) -> "Yesterday · ${date.format(GROUP_DATE_HEADER_FORMATTER)}"
                                else -> date.format(GROUP_DATE_HEADER_FORMATTER)
                            }
                            item(
                                key = "header_${date.toEpochDay()}",
                                contentType = "date_group_header"
                            ) {
                                Spacer(modifier = Modifier.height(4.dp))
                                SectionHeader(
                                    title = dateTitle,
                                    subtitle = "${itemsForDate.size} scheduled occurrence(s)"
                                )
                            }

                            items(
                                items = itemsForDate,
                                key = { it.occurrence.occurrenceId },
                                contentType = { "history_item_card" }
                            ) { item ->
                                DetailedHistoryItemCard(
                                    resolved = item,
                                    onCorrectClick = { resolvedToCorrect = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val currentTarget = resolvedToCorrect
    if (currentTarget != null && med != null) {
        CorrectDoseDialog(
            dose = currentTarget.occurrence,
            medicationName = med.name,
            onDismiss = { resolvedToCorrect = null },
            onConfirm = { newStatus, reason ->
                viewModel.correctDose(
                    occurrenceId = currentTarget.occurrence.occurrenceId,
                    targetStatus = newStatus,
                    reason = reason,
                    fallbackOccurrence = currentTarget.occurrence
                )
                resolvedToCorrect = null
            }
        )
    }
}

@Composable
private fun MedicationAdherenceSummaryBanner(summary: AdherenceSummary) {
    val semantic = MaterialTheme.noveloColors
    val pctText = summary.adherencePercentage?.let { "$it%" } ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "${summary.periodLabel} Adherence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${summary.adherentCount} of ${summary.eligibleOccurrencesCount} due doses fulfilled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = pctText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(label = "Confirmed", count = summary.confirmedCount, color = semantic.taken)
                StatPill(label = "Assumed", count = summary.assumedCount, color = semantic.assumed)
                StatPill(label = "Skipped", count = summary.skippedCount, color = semantic.skipped)
                StatPill(label = "Missed", count = summary.missedCount, color = semantic.missed)
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.tabular,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val scheduledStr = Instant.ofEpochMilli(occ.scheduledEpochMs).atZone(zone).format(TIME_FORMATTER)
    val recordedStr = occ.recordedEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(RECORDED_TS_FORMATTER)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = NoveloPillShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Scheduled $scheduledStr",
                                    style = MaterialTheme.typography.labelSmall.tabular,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "${occ.doseQuantity.toDisplayString()} ${occ.unit.abbreviation}",
                            style = MaterialTheme.typography.bodyMedium.tabular,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = when {
                            recordedStr != null -> "Recorded at: $recordedStr"
                            occ.status == DoseStatus.ASSUMED ->
                                "Auto-assumed per schedule"
                            occ.status == DoseStatus.MISSED && occ.recordedEpochMs == null ->
                                "Unrecorded past dose (evaluated as Missed)"
                            else -> "Not yet recorded"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DoseStatusBadge(status = occ.status)

                    OutlinedButton(
                        onClick = onCorrectClick,
                        shape = NoveloPillShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Revise dose outcome",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Correct", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (!occ.notes.isNullOrBlank()) {
                Text(
                    text = "Note: ${occ.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (occ.correctionNotes != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
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
                        text = "Audit Revision: ${occ.correctionNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
