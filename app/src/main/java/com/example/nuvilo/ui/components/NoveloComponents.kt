package com.example.nuvilo.ui.components

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.NoveloTheme
import com.example.nuvilo.ui.theme.noveloColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HERO_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Maps a [MedicationForm] to its natural default [StockUnit] to reduce manual form friction.
 */
fun defaultStockUnitForForm(form: MedicationForm): StockUnit = when (form) {
    MedicationForm.TABLET -> StockUnit.TABLETS
    MedicationForm.CAPSULE -> StockUnit.CAPSULES
    MedicationForm.LIQUID_ML -> StockUnit.ML
    MedicationForm.DROPS -> StockUnit.DROPS
    MedicationForm.INHALER_PUFFS -> StockUnit.PUFFS
    MedicationForm.TOPICAL_CREAM -> StockUnit.APPLICATIONS
    MedicationForm.INJECTION -> StockUnit.UNITS
    MedicationForm.PATCH -> StockUnit.PATCHES
    MedicationForm.OTHER -> StockUnit.TABLETS
}

/**
 * Parses a hex color tag safely with fallback to Novelo signature blue.
 */
fun parseAccentColor(hex: String, fallback: Color): Color {
    return runCatching {
        val cleaned = hex.trim().removePrefix("#")
        val value = cleaned.toLong(16)
        Color(0xFF000000 or value)
    }.getOrDefault(fallback)
}

/**
 * Formats a minute-of-day integer (0..1439) as `HH:mm`.
 */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, 1439)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

/**
 * Formats a concise human-readable summary of a medication's schedule list.
 */
fun formatScheduleSummaryPill(schedules: List<ScheduleDefinition>): String {
    val primary = schedules.firstOrNull() ?: return "No active schedule"
    val timesStr = primary.targetTimesMinuteOfDay.sorted().joinToString(", ") { formatMinuteOfDay(it) }
    val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    val freqPrefix = if (primary.scheduleType == ScheduleType.SPECIFIC_WEEKDAYS && primary.daysOfWeek.isNotEmpty()) {
        if (primary.daysOfWeek.size == 7) {
            "Daily"
        } else {
            primary.daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
        }
    } else {
        "Daily"
    }
    return if (timesStr.isNotEmpty()) "$freqPrefix · $timesStr" else freqPrefix
}

/**
 * Primary Hero card for the next pending dose on the Dashboard.
 * Features an editorial gradient header, high-contrast medication identity, stock pill,
 * and a clear separation between the primary "Take Dose" CTA and secondary actions.
 */
@Composable
fun HeroDoseCard(
    occurrence: DoseOccurrence,
    medication: Medication,
    stockBalance: StockBalance?,
    onTake: () -> Unit = {},
    onSkip: () -> Unit = {},
    onSnooze: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onTakeClick: () -> Unit = onTake,
    onSkipClick: () -> Unit = onSkip,
    onSnoozeClick: () -> Unit = onSnooze,
    onCardClick: () -> Unit = onClick
) {
    val haptics = LocalHapticFeedback.current
    val semantic = MaterialTheme.noveloColors
    val timeString = Instant.ofEpochMilli(occurrence.scheduledEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(HERO_TIME_FORMATTER)
    val accentColor = parseAccentColor(medication.colorTagHex, MaterialTheme.colorScheme.primary)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onCardClick,
                onClickLabel = "View details for ${medication.name}",
                role = Role.Button
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Gradient Accent Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(semantic.heroGradientStart, semantic.heroGradientEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
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
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "NEXT DUE DOSE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (medication.recordingMode == RecordingMode.CONFIRM) {
                                    "Manual confirmation required"
                                } else {
                                    "Auto-assumed schedule"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Prominent Time Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = timeString,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Main Hero Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = medication.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${medication.strength} · Take ${occurrence.doseQuantity.toDisplayString()} ${occurrence.unit.displayName.lowercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (stockBalance != null) {
                        StockBadge(balance = stockBalance)
                    }
                }

                if (stockBalance != null) {
                    val runwayStr = stockBalance.daysRemaining?.let { "~$it days supply left" }
                        ?: "${stockBalance.currentBalance.toDisplayString()} ${stockBalance.unit.abbreviation} left"
                    val isReorderNeeded = stockBalance.isLowStock || (stockBalance.daysRemaining != null && stockBalance.daysRemaining <= 7)

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isReorderNeeded) semantic.warningContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isReorderNeeded) semantic.warningBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReorderNeeded) Icons.Default.Warning else Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isReorderNeeded) semantic.warning else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isReorderNeeded) "Low Supply: $runwayStr · Reorder from GP soon" else "Supply Runway: $runwayStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isReorderNeeded) semantic.warning else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${stockBalance.currentBalance.toDisplayString()} ${stockBalance.unit.abbreviation}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isReorderNeeded) semantic.warning else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (medication.notes.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Instruction: ${medication.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Primary CTA + Secondary Actions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTakeClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .semantics {
                                contentDescription = "Record ${medication.name} as taken"
                            },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirm Dose Taken (${occurrence.doseQuantity.toDisplayString()} ${occurrence.unit.abbreviation})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onSnoozeClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = "Snooze ${medication.name} reminder"
                                },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Snooze 15m",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onSkipClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = "Skip ${medication.name} dose"
                                },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Skip Dose",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shown on the Dashboard when all scheduled doses for today have been resolved.
 */
@Composable
fun CompletedDayCard(
    takenDoses: Int = 0,
    totalDoses: Int = 0,
    onViewCalendar: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    takenCount: Int = takenDoses,
    totalCount: Int = totalDoses
) {
    val semantic = MaterialTheme.noveloColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = semantic.takenContainer),
        border = BorderStroke(1.dp, semantic.takenBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(semantic.taken.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = semantic.taken,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "All Caught Up for Today",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = semantic.taken
                    )
                    Text(
                        text = "$takenCount of $totalCount scheduled dose(s) recorded as taken. Your stock ledger and adherence history are up to date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = semantic.taken.copy(alpha = 0.9f)
                    )
                }
            }

            if (onViewCalendar != null) {
                OutlinedButton(
                    onClick = onViewCalendar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, semantic.taken.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.taken)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Adherence Calendar", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * Compact, theme-aware warning banner for low stock or prescription renewal alerts on the Dashboard.
 */
@Composable
fun AlertBanner(
    lowStockCount: Int,
    expiringRxCount: Int,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (lowStockCount <= 0 && expiringRxCount <= 0) return

    val semantic = MaterialTheme.noveloColors
    val messages = buildList {
        if (lowStockCount > 0) {
            add("$lowStockCount medication(s) low on stock")
        }
        if (expiringRxCount > 0) {
            add("$expiringRxCount prescription(s) due for renewal")
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (onActionClick != null) {
                    Modifier.clickable(
                        onClick = onActionClick,
                        role = Role.Button,
                        onClickLabel = "View medications needing attention"
                    )
                } else Modifier
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = semantic.warningContainer),
        border = BorderStroke(1.dp, semantic.warningBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(semantic.warning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Attention required",
                    tint = semantic.warning,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inventory & Prescription Attention",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = semantic.warning
                )
                Text(
                    text = messages.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = semantic.warning.copy(alpha = 0.92f)
                )
            }
            if (onActionClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = semantic.warning,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Actionable single-message AlertBanner overload used for inline contextual alerts and accessibility tests.
 */
@Composable
fun AlertBanner(
    icon: ImageVector,
    message: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.noveloColors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$message. $actionText"
            }
            .clickable(onClick = onActionClick, role = Role.Button),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = semantic.warningContainer),
        border = BorderStroke(1.dp, semantic.warningBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = semantic.warning,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = semantic.warning
                )
            }
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = semantic.warning
            )
        }
    }
}

/**
 * Chronological timeline row for today's schedule.
 * Features a left-hand time column, vertical timeline indicator bar, and smooth status/action controls.
 */
@Composable
fun TimelineDoseItem(
    occurrence: DoseOccurrence,
    medication: Medication,
    stockBalance: StockBalance?,
    onTakeClick: () -> Unit,
    onSkipClick: () -> Unit,
    onSnoozeClick: () -> Unit = {},
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val semantic = MaterialTheme.noveloColors
    val timeString = Instant.ofEpochMilli(occurrence.scheduledEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(HERO_TIME_FORMATTER)
    val isPending = occurrence.status == DoseStatus.PENDING

    val indicatorColor = when (occurrence.status) {
        DoseStatus.TAKEN -> semantic.taken
        DoseStatus.ASSUMED -> semantic.assumed
        DoseStatus.SKIPPED -> semantic.skipped
        DoseStatus.MISSED -> semantic.missed
        DoseStatus.PENDING -> parseAccentColor(medication.colorTagHex, MaterialTheme.colorScheme.primary)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onCardClick,
                onClickLabel = "View details for ${medication.name}",
                role = Role.Button
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPending) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPending) 1.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Time Column + Timeline Track Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPending) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(indicatorColor.copy(alpha = 0.45f))
                )
            }

            // Right Content Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = medication.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${medication.strength} · ${occurrence.doseQuantity.toDisplayString()} ${occurrence.unit.displayName.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedContent(
                        targetState = occurrence.status,
                        transitionSpec = { NoveloMotion.heroTransition() },
                        label = "timelineStatusTransition"
                    ) { status ->
                        DoseStatusBadge(status = status)
                    }
                }

                if (stockBalance != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StockBadge(balance = stockBalance)
                        if (stockBalance.daysRemaining != null) {
                            Text(
                                text = "~${stockBalance.daysRemaining}d supply runway",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isPending) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTakeClick()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = "Take ${medication.name} dose"
                                },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take", style = MaterialTheme.typography.labelLarge)
                        }

                        FilledTonalButton(
                            onClick = onSnoozeClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = "Snooze ${medication.name} reminder"
                                },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("15m", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = onSkipClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = "Skip ${medication.name} dose"
                                },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Skip", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overload accepting explicit `medicationName` and `medicationStrength` for standalone previews and tests.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun TimelineDoseItem(
    occurrence: DoseOccurrence,
    medicationName: String,
    medicationStrength: String,
    stockBalance: StockBalance?,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimelineDoseItem(
        occurrence = occurrence,
        medication = Medication(
            id = occurrence.medicationId,
            name = medicationName,
            strength = medicationStrength,
            form = MedicationForm.TABLET,
            stockUnit = occurrence.unit,
            openingStock = ScaledQuantity.ZERO,
            lowStockThreshold = ScaledQuantity.ZERO
        ),
        stockBalance = stockBalance,
        onTakeClick = onTake,
        onSkipClick = onSkip,
        onCardClick = onClick,
        modifier = modifier
    )
}

/**
 * Legacy-compatible DoseItemCard wrapper delegating to TimelineDoseItem.
 */
@Composable
fun DoseItemCard(
    occurrence: DoseOccurrence,
    medication: Medication,
    stockBalance: StockBalance?,
    onTakeClick: () -> Unit,
    onSkipClick: () -> Unit,
    onSnoozeClick: () -> Unit = {},
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TimelineDoseItem(
        occurrence = occurrence,
        medication = medication,
        stockBalance = stockBalance,
        onTakeClick = onTakeClick,
        onSkipClick = onSkipClick,
        onSnoozeClick = onSnoozeClick,
        onCardClick = onCardClick,
        modifier = modifier
    )
}

/**
 * Accessible, theme-aware status badge pairing semantic color, border, icon, and text label.
 */
@Composable
fun DoseStatusBadge(
    status: DoseStatus,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.noveloColors
    val (bgColor, textColor, borderColor, icon) = when (status) {
        DoseStatus.TAKEN -> Quadruple(
            semantic.takenContainer,
            semantic.taken,
            semantic.takenBorder,
            Icons.Default.Check
        )
        DoseStatus.ASSUMED -> Quadruple(
            semantic.assumedContainer,
            semantic.assumed,
            semantic.assumedBorder,
            Icons.Default.AutoAwesome
        )
        DoseStatus.SKIPPED -> Quadruple(
            semantic.skippedContainer,
            semantic.skipped,
            semantic.skippedBorder,
            Icons.Default.Remove
        )
        DoseStatus.MISSED -> Quadruple(
            semantic.missedContainer,
            semantic.missed,
            semantic.missedBorder,
            Icons.Default.Warning
        )
        DoseStatus.PENDING -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            Icons.Default.Schedule
        )
    }

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Dose status: ${status.displayName}"
            }
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bgColor)
            .border(1.dp, borderColor, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Accessible, theme-aware stock balance pill with low-stock warning and ESTIMATED indicator.
 */
@Composable
fun StockBadge(
    balance: StockBalance,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.noveloColors
    val isWarning = balance.isLowStock || balance.isNegative
    val bgColor = if (isWarning) semantic.warningContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isWarning) semantic.warning else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isWarning) semantic.warningBorder else MaterialTheme.colorScheme.outlineVariant

    val estimatePrefix = if (balance.isEstimate) "Est. " else ""
    val label = "$estimatePrefix${balance.currentBalance.toDisplayString()} ${balance.unit.abbreviation} left"
    val stateDesc = if (isWarning) "Low stock warning, $label" else "Stock available, $label"

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = stateDesc
            }
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bgColor)
            .border(1.dp, borderColor, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isWarning) Icons.Default.ErrorOutline else Icons.Default.Inventory2,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Directory card for a medication on MedicationListScreen.
 * Displays accent color tag, name, strength, dosage form, schedule summary pill, stock runway, and archive toggle.
 */
@Composable
fun MedicationItemCard(
    medication: Medication,
    stockBalance: StockBalance?,
    onArchiveToggle: () -> Unit,
    onCardClick: () -> Unit = {},
    onRestockClick: (() -> Unit)? = null,
    pendingPrescription: com.example.nuvilo.domain.model.Prescription? = null,
    modifier: Modifier = Modifier,
    schedules: List<ScheduleDefinition> = emptyList()
) {
    val semantic = MaterialTheme.noveloColors
    val accentColor = parseAccentColor(medication.colorTagHex, MaterialTheme.colorScheme.primary)
    val scheduleSummary = formatScheduleSummaryPill(schedules)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onCardClick,
                onClickLabel = "Open details for ${medication.name}",
                role = Role.Button
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.14f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = medication.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${medication.strength} · ${medication.form.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onArchiveToggle,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (medication.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (medication.isArchived) {
                                "Restore ${medication.name}"
                            } else {
                                "Archive ${medication.name}"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (pendingPrescription != null) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = semantic.warningContainer,
                    border = BorderStroke(1.dp, semantic.warningBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Pending GP repeat prescription order",
                            tint = semantic.warning,
                            modifier = Modifier.size(14.dp)
                        )
                        val qtyText = pendingPrescription.quantityPrescribed?.let { " (${it.toDisplayString()} ${medication.stockUnit.abbreviation})" } ?: ""
                        Text(
                            text = "GP Order Pending$qtyText · Awaiting Collection",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = semantic.warning
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // Bottom Metadata Row: Schedule Pill + Recording Mode + Restock Action + Stock Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Schedule summary pill
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = scheduleSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Recording Mode Pill
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (medication.recordingMode == RecordingMode.CONFIRM) {
                                Icons.Default.VerifiedUser
                            } else {
                                Icons.Default.AutoAwesome
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (medication.recordingMode == RecordingMode.CONFIRM) "Confirm" else "Auto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onRestockClick != null) {
                        OutlinedButton(
                            onClick = onRestockClick,
                            modifier = Modifier.heightIn(min = 36.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Stock",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (stockBalance != null) {
                        StockBadge(balance = stockBalance)
                    }
                }
            }
        }
    }
}

/**
 * Reusable ScheduleBuilder composable used by both the Add Medication Wizard (Step 2)
 * and the Edit Schedule Dialog on MedicationDetailScreen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleConfigurationContent(
    isSpecificWeekdays: Boolean,
    onScheduleModeChange: (Boolean) -> Unit,
    selectedDaysOfWeek: Set<Int>,
    onToggleDayOfWeek: (Int) -> Unit,
    selectedTimesMinuteOfDay: List<Int>,
    onToggleTimeMinuteOfDay: (Int) -> Unit,
    doseQuantityStr: String,
    onDoseQuantityChange: (String) -> Unit,
    stockUnit: StockUnit,
    instructions: String,
    onInstructionsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customHourStr by rememberSaveable { mutableStateOf("") }
    var customMinuteStr by rememberSaveable { mutableStateOf("00") }
    var customTimeError by rememberSaveable { mutableStateOf<String?>(null) }

    val presetTimes = listOf(
        480 to "08:00 Morning",
        720 to "12:00 Midday",
        840 to "14:00 Afternoon",
        1080 to "18:00 Evening",
        1200 to "20:00 Night",
        1320 to "22:00 Bedtime"
    )
    val weekdays = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Schedule Frequency",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !isSpecificWeekdays,
                onClick = { onScheduleModeChange(false) },
                label = { Text("Every Day") },
                leadingIcon = if (!isSpecificWeekdays) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = isSpecificWeekdays,
                onClick = { onScheduleModeChange(true) },
                label = { Text("Specific Weekdays") },
                leadingIcon = if (isSpecificWeekdays) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }

        if (isSpecificWeekdays) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Select Active Days *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weekdays.forEach { (isoDay, label) ->
                        val isSelected = selectedDaysOfWeek.contains(isoDay)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleDayOfWeek(isoDay) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Scheduled Dose Times * (${selectedTimesMinuteOfDay.size} selected)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap preset times or add a custom 24-hour time below:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetTimes.forEach { (minuteOfDay, label) ->
                    val selected = selectedTimesMinuteOfDay.contains(minuteOfDay)
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleTimeMinuteOfDay(minuteOfDay) },
                        label = { Text(label) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                        } else {
                            { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp)) }
                        }
                    )
                }

                val presetMinutes = presetTimes.map { it.first }.toSet()
                selectedTimesMinuteOfDay.filter { it !in presetMinutes }.sorted().forEach { customMin ->
                    FilterChip(
                        selected = true,
                        onClick = { onToggleTimeMinuteOfDay(customMin) },
                        label = { Text("${formatMinuteOfDay(customMin)} Custom") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove custom time", modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customHourStr,
                    onValueChange = {
                        if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                            customHourStr = it
                            customTimeError = null
                        }
                    },
                    label = { Text("Hour (0-23)") },
                    placeholder = { Text("09") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customMinuteStr,
                    onValueChange = {
                        if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                            customMinuteStr = it
                            customTimeError = null
                        }
                    },
                    label = { Text("Min (0-59)") },
                    placeholder = { Text("30") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = {
                        val h = customHourStr.toIntOrNull()
                        val m = customMinuteStr.toIntOrNull() ?: 0
                        if (h == null || h !in 0..23 || m !in 0..59) {
                            customTimeError = "Enter valid 24h time (00:00–23:59)"
                        } else {
                            val totalMin = h * 60 + m
                            if (!selectedTimesMinuteOfDay.contains(totalMin)) {
                                onToggleTimeMinuteOfDay(totalMin)
                            }
                            customHourStr = ""
                            customMinuteStr = "00"
                            customTimeError = null
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            if (customTimeError != null) {
                Text(
                    text = customTimeError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        OutlinedTextField(
            value = doseQuantityStr,
            onValueChange = onDoseQuantityChange,
            label = { Text("Dose Quantity per Scheduled Time (${stockUnit.displayName}) *") },
            placeholder = { Text("e.g. 1 or 2") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = instructions,
            onValueChange = onInstructionsChange,
            label = { Text("Dose Instructions (Optional)") },
            placeholder = { Text("e.g. Take with food and a full glass of water") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Polished empty state card with icon badge, clear title, reassuring description, and optional primary CTA.
 */
@Composable
fun EmptyStateCard(
    icon: ImageVector = Icons.Default.Medication,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionButtonText: String? = actionLabel,
    onActionClick: (() -> Unit)? = null
) {
    val resolvedActionLabel = actionButtonText ?: actionLabel
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (resolvedActionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onActionClick,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(resolvedActionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeroDoseCardPreview() {
    NoveloTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            HeroDoseCard(
                occurrence = DoseOccurrence(
                    occurrenceId = "med-1_2026-04-02_480",
                    medicationId = "med-1",
                    scheduledEpochMs = System.currentTimeMillis(),
                    doseQuantity = ScaledQuantity.ofWhole(1),
                    unit = StockUnit.TABLETS,
                    status = DoseStatus.PENDING
                ),
                medication = Medication(
                    id = "med-1",
                    name = "Atorvastatin",
                    strength = "20mg",
                    form = MedicationForm.TABLET,
                    stockUnit = StockUnit.TABLETS,
                    openingStock = ScaledQuantity.ofWhole(28),
                    lowStockThreshold = ScaledQuantity.ofWhole(7),
                    notes = "Take in the morning with water"
                ),
                stockBalance = StockBalance(
                    currentBalance = ScaledQuantity.ofWhole(14),
                    unit = StockUnit.TABLETS,
                    isEstimate = false,
                    isNegative = false,
                    daysRemaining = 14,
                    isLowStock = false
                ),
                onTakeClick = {},
                onSkipClick = {},
                onSnoozeClick = {},
                onCardClick = {}
            )
        }
    }
}
