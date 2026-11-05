package com.example.nuvilo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.domain.adherence.AdherenceSummary
import com.example.nuvilo.domain.adherence.DailyAdherenceRecord
import com.example.nuvilo.domain.adherence.MonthlyCalendarData
import com.example.nuvilo.domain.adherence.ResolvedDoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.ui.components.DoseStatusBadge
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.SectionHeader
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.viewmodel.AdherenceCalendarViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MONTH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val SELECTED_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
private val CELL_DATE_A11Y_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
private val TIME_ONLY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun AdherenceCalendarScreen(
    viewModel: AdherenceCalendarViewModel,
    onSelectMedication: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMethodologyInfo by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Adherence & History",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Monthly schedule outcomes & verified vs assumed adherence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = { viewModel.onJumpToToday() },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Today", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                !uiState.hasAnyScheduleOrHistory -> {
                    EmptyStateCard(
                        icon = Icons.Default.CalendarMonth,
                        title = "No Adherence Data Yet",
                        description = "Add a medication and schedule in the Medications tab to start tracking your daily and monthly adherence."
                    )
                }
                else -> {
                    val dayRecord = uiState.selectedDayRecord
                    val dayFormatStr = uiState.selectedDate.format(SELECTED_DAY_FORMATTER)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        // 1. Period Summary Strip + Period Selector
                        item(key = "summary_strip", contentType = "summary_strip") {
                            AdherenceOverviewStrip(
                                todaySummary = uiState.todaySummary,
                                weeklySummary = uiState.weeklySummary,
                                periodSummary = uiState.selectedPeriodSummary,
                                selectedPeriodDays = uiState.selectedPeriodDays,
                                onSelectPeriodDays = { viewModel.onSelectPeriodDays(it) }
                            )
                        }

                        // 2. Monthly Calendar Card with Animated Month Transitions
                        val calendarData = uiState.calendarData
                        if (calendarData != null) {
                            item(key = "calendar_card", contentType = "calendar_card") {
                                MonthlyCalendarCard(
                                    calendarData = calendarData,
                                    selectedDate = uiState.selectedDate,
                                    onPreviousMonth = { viewModel.onPreviousMonth() },
                                    onNextMonth = { viewModel.onNextMonth() },
                                    onSelectDate = { viewModel.onSelectDate(it) }
                                )
                            }
                        }

                        // 3. Selected Day Breakdown Header & Summary
                        item(key = "selected_day_header", contentType = "section_header") {
                            SectionHeader(
                                title = dayFormatStr,
                                subtitle = dayRecord?.let { rec ->
                                    if (rec.totalScheduledCount == 0) {
                                        "No scheduled doses on this date"
                                    } else {
                                        "${rec.confirmedCount} confirmed · ${rec.assumedCount} assumed · ${rec.skippedCount} skipped · ${rec.missedCount} missed"
                                    }
                                }
                            )
                        }

                        if (dayRecord == null || dayRecord.occurrences.isEmpty()) {
                            item(key = "empty_day_card", contentType = "empty_day") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "No doses were scheduled for this date.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            }
                        } else {
                            items(
                                items = dayRecord.occurrences,
                                key = { it.occurrence.occurrenceId },
                                contentType = { "calendar_day_occurrence" }
                            ) { resolved ->
                                CalendarDayOccurrenceCard(
                                    resolved = resolved,
                                    isToday = uiState.selectedDate == LocalDate.now(),
                                    onTakeClick = { viewModel.onTakeDose(resolved.occurrence) },
                                    onSkipClick = { viewModel.onSkipDose(resolved.occurrence) },
                                    onOpenMedicationClick = { onSelectMedication(resolved.occurrence.medicationId) }
                                )
                            }
                        }

                        // 4. Collapsible Calculation Methodology Card
                        item(key = "calculation_note", contentType = "methodology_card") {
                            CollapsibleMethodologyCard(
                                expanded = showMethodologyInfo,
                                onToggle = { showMethodologyInfo = !showMethodologyInfo }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdherenceOverviewStrip(
    todaySummary: AdherenceSummary?,
    weeklySummary: AdherenceSummary?,
    periodSummary: AdherenceSummary?,
    selectedPeriodDays: Int,
    onSelectPeriodDays: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricSummaryCard(
                title = "Today",
                summary = todaySummary,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Last 7 Days",
                summary = weeklySummary,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Last $selectedPeriodDays Days",
                summary = periodSummary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rolling Window:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            listOf(7, 14, 30, 90).forEach { days ->
                FilterChip(
                    selected = selectedPeriodDays == days,
                    onClick = { onSelectPeriodDays(days) },
                    label = { Text("${days}D") }
                )
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    summary: AdherenceSummary?,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.noveloColors
    val pctText = summary?.adherencePercentage?.let { "$it%" } ?: "—"
    val detailText = if (summary == null || summary.eligibleOccurrencesCount == 0) {
        "No due doses"
    } else if (summary.assumedCount > 0) {
        "${summary.confirmedCount}v + ${summary.assumedCount}a / ${summary.eligibleOccurrencesCount}"
    } else {
        "${summary.confirmedCount}/${summary.eligibleOccurrencesCount} taken"
    }

    val pctColor = when (val pct = summary?.adherencePercentage) {
        null -> MaterialTheme.colorScheme.onSurfaceVariant
        in 85..100 -> semantic.taken
        in 60..84 -> semantic.warning
        else -> semantic.missed
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = pctText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = pctColor
            )
            Text(
                text = detailText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    calendarData: MonthlyCalendarData,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val yearMonth = calendarData.yearMonth
    val today = LocalDate.now()

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Switcher Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = yearMonth.format(MONTH_YEAR_FORMATTER),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val monthPct = calendarData.monthSummary.adherencePercentage
                    if (monthPct != null) {
                        Text(
                            text = "Monthly Adherence: $monthPct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }

            // Weekday Headers (Mon..Sun)
            val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Animated Month Grid
            AnimatedContent(
                targetState = yearMonth,
                transitionSpec = {
                    NoveloMotion.directionalHorizontalTransition(targetState > initialState)
                },
                label = "calendarMonthGridTransition"
            ) { targetYm ->
                val firstOfMonth = targetYm.atDay(1)
                val leadingEmptyCells = firstOfMonth.dayOfWeek.value - 1
                val daysInMonth = targetYm.lengthOfMonth()
                val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (weekIdx in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (dowIdx in 0..6) {
                                val cellIndex = weekIdx * 7 + dowIdx
                                val dayOfMonth = cellIndex - leadingEmptyCells + 1
                                if (dayOfMonth in 1..daysInMonth) {
                                    val date = targetYm.atDay(dayOfMonth)
                                    val record = calendarData.daysByDate[date]
                                    CalendarDayCell(
                                        date = date,
                                        record = record,
                                        isSelected = date == selectedDate,
                                        isToday = date == today,
                                        onClick = { onSelectDate(date) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Clean Status Legend
            CalendarLegendRow()
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    record: DailyAdherenceRecord?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.noveloColors
    val (bgColor, textColor, dotColor, stateLabel) = when {
        record == null || !record.hasAnyDoses ->
            DayVisuals(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.onSurfaceVariant,
                Color.Transparent,
                "No scheduled doses"
            )
        record.eligiblePastCount == 0 && record.pendingFutureCount > 0 ->
            DayVisuals(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                MaterialTheme.colorScheme.onPrimaryContainer,
                MaterialTheme.colorScheme.primary,
                "Scheduled upcoming doses"
            )
        record.confirmedCount + record.assumedCount == record.eligiblePastCount && record.assumedCount == 0 ->
            DayVisuals(semantic.takenContainer, semantic.taken, semantic.taken, "All doses confirmed taken")
        record.confirmedCount + record.assumedCount == record.eligiblePastCount ->
            DayVisuals(semantic.assumedContainer, semantic.assumed, semantic.assumed, "All doses assumed or taken")
        record.confirmedCount + record.assumedCount > 0 ->
            DayVisuals(semantic.warningContainer, semantic.warning, semantic.warning, "Partial adherence")
        else ->
            DayVisuals(semantic.missedContainer, semantic.missed, semantic.missed, "Missed or skipped doses")
    }

    val borderStroke = when {
        isSelected -> 2.dp to MaterialTheme.colorScheme.primary
        isToday -> 1.5.dp to MaterialTheme.colorScheme.secondary
        else -> 0.dp to Color.Transparent
    }

    val dateDesc = date.format(CELL_DATE_A11Y_FORMATTER)

    Box(
        modifier = modifier
            .aspectRatio(0.95f)
            .semantics {
                contentDescription = "$dateDesc: $stateLabel" + if (isSelected) ", selected" else ""
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (borderStroke.first > 0.dp) {
                    Modifier.border(borderStroke.first, borderStroke.second, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = textColor
            )
            if (dotColor != Color.Transparent) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(dotColor)
                )
            }
        }
    }
}

private data class DayVisuals(
    val bgColor: Color,
    val textColor: Color,
    val dotColor: Color,
    val a11yLabel: String
)

@Composable
private fun CalendarLegendRow() {
    val semantic = MaterialTheme.noveloColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendChip(color = semantic.takenContainer, dotColor = semantic.taken, label = "Taken")
        LegendChip(color = semantic.assumedContainer, dotColor = semantic.assumed, label = "Assumed")
        LegendChip(color = semantic.warningContainer, dotColor = semantic.warning, label = "Partial")
        LegendChip(color = semantic.missedContainer, dotColor = semantic.missed, label = "Missed")
        LegendChip(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            dotColor = MaterialTheme.colorScheme.primary,
            label = "Planned"
        )
    }
}

@Composable
private fun LegendChip(
    color: Color,
    dotColor: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .border(1.dp, dotColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarDayOccurrenceCard(
    resolved: ResolvedDoseOccurrence,
    isToday: Boolean,
    onTakeClick: () -> Unit,
    onSkipClick: () -> Unit,
    onOpenMedicationClick: () -> Unit
) {
    val occ = resolved.occurrence
    val scheduledTimeStr = Instant.ofEpochMilli(occ.scheduledEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(TIME_ONLY_FORMATTER)

    val recordedTimeStr = occ.recordedEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(TIME_ONLY_FORMATTER)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenMedicationClick, role = Role.Button),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${resolved.medicationName} (${resolved.medicationStrength})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val recSub = if (recordedTimeStr != null) " · Recorded $recordedTimeStr" else ""
                    Text(
                        text = "Scheduled $scheduledTimeStr · ${occ.doseQuantity.toDisplayString()} ${occ.unit.abbreviation}$recSub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DoseStatusBadge(status = occ.status)
            }

            if (isToday && occ.status == DoseStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTakeClick,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take Dose", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onSkipClick,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skip", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleMethodologyCard(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = NoveloMotion.standardFloatSpring(),
        label = "methodologyChevron"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle, role = Role.Button)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "How Adherence is Calculated",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse explanation" else "Expand explanation",
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = NoveloMotion.sectionExpand(),
                exit = NoveloMotion.sectionCollapse()
            ) {
                Text(
                    text = "Adherence percentage = (Confirmed Taken + Auto-Assumed) ÷ Elapsed Scheduled Doses. Future upcoming doses are excluded until due. Manual CONFIRM doses require your explicit confirmation; AUTO_ASSUME schedules are visually distinguished as Assumed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                )
            }
        }
    }
}
