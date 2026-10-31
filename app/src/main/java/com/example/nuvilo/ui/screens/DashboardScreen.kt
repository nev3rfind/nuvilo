package com.example.nuvilo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nuvilo.ui.components.AlertBanner
import com.example.nuvilo.ui.components.CompletedDayCard
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.HeroDoseCard
import com.example.nuvilo.ui.components.SectionHeader
import com.example.nuvilo.ui.components.TimelineDoseItem
import com.example.nuvilo.ui.viewmodel.DashboardUiState
import com.example.nuvilo.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToMedications: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onMedicationClick: (medicationId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header: Greeting and Today's Date
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Novelo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = uiState.formattedDate.ifEmpty { "Today" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${uiState.greeting}, here is your plan.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No doses scheduled for today",
                    description = "Add your medications to view your upcoming reminders, stock levels, and adherence timeline here.",
                    icon = Icons.Default.Medication,
                    actionButtonText = "Add Medication",
                    onActionClick = onNavigateToMedications
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Low-stock / Prescription alerts banner (if any)
                if (uiState.lowStockCount > 0 || uiState.expiringPrescriptionsCount > 0) {
                    item {
                        val alertMsg = when {
                            uiState.lowStockCount > 0 && uiState.expiringPrescriptionsCount > 0 ->
                                "${uiState.lowStockCount} item(s) low on stock · ${uiState.expiringPrescriptionsCount} prescription(s) need renewal"
                            uiState.lowStockCount > 0 ->
                                "${uiState.lowStockCount} medication(s) running low on stock"
                            else ->
                                "${uiState.expiringPrescriptionsCount} prescription(s) require repeat renewal"
                        }
                        AlertBanner(
                            icon = Icons.Default.Warning,
                            message = alertMsg,
                            actionText = "Manage",
                            onActionClick = onNavigateToMedications
                        )
                    }
                }

                // 1. Hero Dose Section (or Completed Day Card)
                val hero = uiState.heroItem
                if (hero != null) {
                    item {
                        HeroDoseCard(
                            occurrence = hero.occurrence,
                            medication = hero.medication,
                            stockBalance = hero.stockBalance,
                            onTake = { viewModel.onTakeDose(hero.occurrence) },
                            onSkip = { viewModel.onSkipDose(hero.occurrence) },
                            onSnooze = { viewModel.onSnoozeDose(hero.occurrence) },
                            onClick = { onMedicationClick(hero.medication.id) }
                        )
                    }
                } else {
                    item {
                        CompletedDayCard(
                            takenDoses = uiState.takenDoses,
                            totalDoses = uiState.totalDoses,
                            onViewCalendar = onNavigateToCalendar
                        )
                    }
                }

                // 2. Compact Daily Adherence Progress Card
                item {
                    val progressFraction = if (uiState.totalDoses > 0) {
                        uiState.takenDoses.toFloat() / uiState.totalDoses.toFloat()
                    } else 0f
                    val adherencePercent = (progressFraction * 100).toInt()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
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
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Today's Adherence",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$adherencePercent% (${uiState.takenDoses} of ${uiState.totalDoses} taken)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                    }
                }

                // 3. Chronological Today's Timeline Section
                item {
                    SectionHeader(
                        title = "Today's Schedule",
                        actionText = "Full Calendar",
                        onActionClick = onNavigateToCalendar
                    )
                }

                val timeline = uiState.items
                itemsIndexed(timeline, key = { _, item -> item.occurrence.occurrenceId }) { index, item ->
                    TimelineDoseItem(
                        occurrence = item.occurrence,
                        medicationName = item.medication.name,
                        medicationStrength = item.medication.strength,
                        stockBalance = item.stockBalance,
                        isFirst = index == 0,
                        isLast = index == timeline.lastIndex,
                        onTake = { viewModel.onTakeDose(item.occurrence) },
                        onSkip = { viewModel.onSkipDose(item.occurrence) },
                        onClick = { onMedicationClick(item.medication.id) }
                    )
                }
            }
        }
    }
}
