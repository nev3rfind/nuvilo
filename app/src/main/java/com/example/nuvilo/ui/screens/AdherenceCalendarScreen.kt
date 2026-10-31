package com.example.nuvilo.ui.screens

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.example.nuvilo.domain.adherence.AdherenceSummary
import com.example.nuvilo.domain.adherence.DailyAdherenceRecord
import com.example.nuvilo.domain.adherence.MonthlyCalendarData
import com.example.nuvilo.domain.adherence.ResolvedDoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.ui.components.DoseStatusBadge
import com.example.nuvilo.ui.theme.StatusAssumed
import com.example.nuvilo.ui.theme.StatusAssumedContainer
import com.example.nuvilo.ui.theme.StatusMissed
import com.example.nuvilo.ui.theme.StatusMissedContainer
import com.example.nuvilo.ui.theme.StatusSkipped
import com.example.nuvilo.ui.theme.StatusSkippedContainer
import com.example.nuvilo.ui.theme.StatusTaken
import com.example.nuvilo.ui.theme.StatusTakenContainer
import com.example.nuvilo.ui.viewmodel.AdherenceCalendarViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AdherenceCalendarScreen(
    viewModel: AdherenceCalendarViewModel,
    onMedicationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Adherence & Calendar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Track confirmed, assumed, skipped, and missed doses across your schedule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!uiState.hasAnyScheduleOrHistory) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Adherence History Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add a medication schedule in the Medications tab to start tracking daily, weekly, and monthly adherence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // 1. Quick Period Overview Row (Today, 7-Day, Month)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickRateTile(
                        title = "Today",
                        summary = uiState.todaySummary,
                        modifier = Modifier.weight(1f)
                    )
                    QuickRateTile(
                        title = "Last 7 Days",
                        summary = uiState.weeklySummary,
                        modifier = Modifier.weight(1f)
                    )
                    QuickRateTile(
                        title = "Selected Month",
                        summary = uiState.monthlySummary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Selected Period Analytics & Calculation Breakdown Card
            item {
                uiState.selectedPeriodSummary?.let { periodSummary ->
                    PeriodAnalyticsCard(
                        summary = periodSummary,
                        selectedDays = uiState.selectedPeriodDays,
                        onSelectDays = viewModel::onSelectPeriodDays
                    )
                }
            }

            // 3. Monthly Calendar Card
            item {
                uiState.calendarData?.let { calendarData ->
                    MonthlyCalendarCard(
                        calendarData = calendarData,
                        selectedDate = uiState.selectedDate,
                        onPreviousMonth = viewModel::onPreviousMonth,
                        onNextMonth = viewModel::onNextMonth,
                        onJumpToToday = viewModel::onJumpToToday,
                        onSelectDate = viewModel::onSelectDate
                    )
                }
            }

            // 4. Selected Date Schedule & Recorded Outcomes
            item {
                val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                Text(
                    text = uiState.selectedDate.format(dateFmt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            val dayOccurrences = uiState.selectedDayRecord?.occurrences.orEmpty()
            if (dayOccurrences.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No scheduled or recorded doses on this date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(dayOccurrences, key = { it.occurrence.occurrenceId }) { resolved ->
                    CalendarDayOccurrenceCard(
                        resolved = resolved,
                        onClick = { onMedicationClick(resolved.occurrence.medicationId) },
                        onTake = { viewModel.onTakeDose(resolved.occurrence) },
                        onSkip = { viewModel.onSkipDose(resolved.occurrence) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickRateTile(
    title: String,
    summary: AdherenceSummary?,
    modifier: Modifier = Modifier
) {
    val pctText = summary?.adherencePercentage?.let { "$it%" } ?: "—"
    val subText = if (summary != null && summary.eligibleOccurrencesCount > 0) {
        "${summary.adherentCount}/${summary.eligibleOccurrencesCount} doses"
    } else {
        "No past doses"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = pctText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeriodAnalyticsCard(
    summary: AdherenceSummary,
    selectedDays: Int,
    onSelectDays: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                Column {
                    Text(
                        text = "Period Adherence Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (summary.adherencePercentage != null) {
                            "${summary.adherencePercentage}% overall (${summary.confirmedOnlyPercentage ?: 0}% confirmed)"
                        } else {
                            "No eligible past doses in period"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(14, 30, 90).forEach { days ->
                        FilterChip(
                            selected = selectedDays == days,
                            onClick = { onSelectDays(days) },
                            label = { Text("${days}d") }
                        )
                    }
                }
            }

            // Outcome Counts Grid (Confirmed, Assumed, Skipped, Missed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutcomeCountPill(
                    label = "Confirmed",
                    count = summary.confirmedCount,
                    icon = Icons.Default.Check,
                    tint = StatusTaken,
                    bg = StatusTakenContainer,
                    modifier = Modifier.weight(1f)
                )
                OutcomeCountPill(
                    label = "Assumed",
                    count = summary.assumedCount,
                    icon = Icons.Default.Schedule,
                    tint = StatusAssumed,
                    bg = StatusAssumedContainer,
                    modifier = Modifier.weight(1f)
                )
                OutcomeCountPill(
                    label = "Skipped",
                    count = summary.skippedCount,
                    icon = Icons.Default.Close,
                    tint = StatusSkipped,
                    bg = StatusSkippedContainer,
                    modifier = Modifier.weight(1f)
                )
                OutcomeCountPill(
                    label = "Missed",
                    count = summary.missedCount,
                    icon = Icons.Default.Warning,
                    tint = StatusMissed,
                    bg = StatusMissedContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Calculation Explanation Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = summary.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OutcomeCountPill(
    label: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun MonthlyCalendarCard(
    calendarData: MonthlyCalendarData,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    val firstDayOfMonth = calendarData.yearMonth.atDay(1)
    // Monday = 1 .. Sunday = 7 -> leading empty cells = dayOfWeek.value - 1
    val leadingEmptyCells = firstDayOfMonth.dayOfWeek.value - 1
    val daysInMonth = calendarData.yearMonth.lengthOfMonth()
    val totalCells = leadingEmptyCells + daysInMonth
    val rows = (totalCells + 6) / 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Header with Navigation
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
                        text = firstDayOfMonth.format(monthFmt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val mSummary = calendarData.monthSummary
                    Text(
                        text = if (mSummary.adherencePercentage != null) {
                            "Monthly adherence: ${mSummary.adherencePercentage}% (${mSummary.confirmedCount}✓ ${mSummary.assumedCount}~ ${mSummary.skippedCount}× ${mSummary.missedCount}!)"
                        } else {
                            "No past scheduled doses this month"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onJumpToToday) {
                        Text("Today", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }
            }

            // Accessible Legend (Symbols + Labels, not colour alone)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(symbol = "✓", label = "Confirmed", color = StatusTaken)
                LegendItem(symbol = "~", label = "Assumed", color = StatusAssumed)
                LegendItem(symbol = "×", label = "Skipped", color = StatusSkipped)
                LegendItem(symbol = "!", label = "Missed", color = StatusMissed)
            }

            // Weekday Header Row
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Calendar Grid Rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayOfMonth = cellIndex - leadingEmptyCells + 1
                            if (dayOfMonth in 1..daysInMonth) {
                                val date = calendarData.yearMonth.atDay(dayOfMonth)
                                val record = calendarData.daysByDate[date]
                                CalendarDayCell(
                                    date = date,
                                    record = record,
                                    isSelected = date == selectedDate,
                                    onClick = { onSelectDate(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    symbol: String,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
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
private fun CalendarDayCell(
    date: LocalDate,
    record: DailyAdherenceRecord?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDoses = record != null && record.hasAnyDoses
    // Build symbol string showing distinct outcomes on this day so mixed outcomes are never conflated
    val symbols = buildString {
        if (record != null) {
            if (record.confirmedCount > 0) append("✓")
            if (record.assumedCount > 0) append("~")
            if (record.skippedCount > 0) append("×")
            if (record.missedCount > 0) append("!")
            if (isEmpty() && record.pendingFutureCount > 0) append("•")
        }
    }

    val a11yDescription = buildString {
        append("${date.dayOfMonth} ${date.month.name.lowercase()}: ")
        if (!hasDoses) {
            append("no scheduled doses")
        } else {
            val parts = mutableListOf<String>()
            if (record!!.confirmedCount > 0) parts.add("${record.confirmedCount} confirmed")
            if (record.assumedCount > 0) parts.add("${record.assumedCount} assumed")
            if (record.skippedCount > 0) parts.add("${record.skippedCount} skipped")
            if (record.missedCount > 0) parts.add("${record.missedCount} missed")
            if (record.pendingFutureCount > 0) parts.add("${record.pendingFutureCount} upcoming")
            append(parts.joinToString(", "))
        }
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        hasDoses -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = a11yDescription }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        if (symbols.isNotEmpty()) {
            Text(
                text = symbols,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    record != null && record.confirmedCount > 0 && record.missedCount == 0 && record.skippedCount == 0 -> StatusTaken
                    record != null && record.assumedCount > 0 && record.missedCount == 0 && record.skippedCount == 0 -> StatusAssumed
                    record != null && (record.confirmedCount > 0 || record.assumedCount > 0) -> MaterialTheme.colorScheme.primary
                    record != null && record.missedCount > 0 -> StatusMissed
                    record != null && record.skippedCount > 0 -> StatusSkipped
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun CalendarDayOccurrenceCard(
    resolved: ResolvedDoseOccurrence,
    onClick: () -> Unit,
    onTake: () -> Unit,
    onSkip: () -> Unit
) {
    val occ = resolved.occurrence
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val dateTimeFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm")
    val zone = ZoneId.systemDefault()

    val scheduledStr = Instant.ofEpochMilli(occ.scheduledEpochMs).atZone(zone).format(timeFmt)
    val recordedStr = occ.recordedEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(dateTimeFmt)
    }
    val correctedStr = occ.correctedAtEpochMs?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(dateTimeFmt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = resolved.medicationName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${resolved.medicationStrength} · ${occ.doseQuantity.toDisplayString()} ${occ.unit.abbreviation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    DoseStatusBadge(status = occ.status)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Scheduled time: $scheduledStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (correctedStr != null) {
                        Text(
                            text = "Corrected at: $correctedStr${occ.correctionNotes?.let { " ($it)" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (recordedStr != null) {
                        Text(
                            text = "Action recorded: $recordedStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (occ.status == DoseStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSkip,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Skip", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = onTake,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Take", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
