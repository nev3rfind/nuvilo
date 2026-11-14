package com.example.nuvilo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.R
import com.example.nuvilo.ui.components.AlertBanner
import com.example.nuvilo.ui.components.CompletedDayCard
import com.example.nuvilo.ui.components.EmptyStateCard
import com.example.nuvilo.ui.components.HeroDoseCard
import com.example.nuvilo.ui.components.SectionHeader
import com.example.nuvilo.ui.components.TimelineDoseItem
import com.example.nuvilo.ui.theme.NoveloCardShape
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.theme.tabular
import com.example.nuvilo.ui.viewmodel.DashboardUiState
import com.example.nuvilo.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onSelectMedication: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToMedications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Editorial Header & Daily Adherence Summary
            DashboardHeaderSection(uiState = uiState)

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
                uiState.items.isEmpty() -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        if (uiState.lowStockCount > 0 || uiState.expiringPrescriptionsCount > 0 || uiState.discardWarningCount > 0) {
                            item(key = "alerts_banner", contentType = "alert_banner") {
                                AlertBanner(
                                    lowStockCount = uiState.lowStockCount,
                                    expiringRxCount = uiState.expiringPrescriptionsCount,
                                    discardWarningCount = uiState.discardWarningCount,
                                    onActionClick = onNavigateToMedications
                                )
                            }
                        }
                        item(key = "empty_today", contentType = "empty_state") {
                            EmptyStateCard(
                                icon = Icons.Default.CheckCircle,
                                title = "No Doses Scheduled Today",
                                description = "Add a medication and configure its daily or weekday schedule in the Medications tab to begin tracking your regimen.",
                                actionLabel = "Go to Medications",
                                onActionClick = onNavigateToMedications
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        // 1. Attention Alerts Banner
                        if (uiState.lowStockCount > 0 || uiState.expiringPrescriptionsCount > 0 || uiState.discardWarningCount > 0) {
                            item(key = "alerts_banner", contentType = "alert_banner") {
                                AlertBanner(
                                    lowStockCount = uiState.lowStockCount,
                                    expiringRxCount = uiState.expiringPrescriptionsCount,
                                    discardWarningCount = uiState.discardWarningCount,
                                    onActionClick = onNavigateToMedications
                                )
                            }
                        }

                        // 2. Hero Focal Card (Animated transition between Next Pending Dose and Completed Day)
                        item(key = "hero_section", contentType = "hero_card") {
                            AnimatedContent(
                                targetState = uiState.heroItem,
                                transitionSpec = { NoveloMotion.heroTransition() },
                                label = "dashboardHeroTransition"
                            ) { hero ->
                                if (hero != null) {
                                    HeroDoseCard(
                                        occurrence = hero.occurrence,
                                        medication = hero.medication,
                                        stockBalance = hero.stockBalance,
                                        consumption = hero.consumption,
                                        onTakeClick = {
                                            viewModel.onTakeDose(hero.occurrence)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Recorded ${hero.medication.name} as taken")
                                            }
                                        },
                                        onSkipClick = {
                                            viewModel.onSkipDose(hero.occurrence)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Marked ${hero.medication.name} as skipped")
                                            }
                                        },
                                        onSnoozeClick = {
                                            viewModel.onSnoozeDose(hero.occurrence)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Snoozed ${hero.medication.name} reminder for 15 minutes")
                                            }
                                        },
                                        onCardClick = { onSelectMedication(hero.medication.id) }
                                    )
                                } else {
                                    CompletedDayCard(
                                        takenCount = uiState.takenDoses,
                                        totalCount = uiState.totalDoses
                                    )
                                }
                            }
                        }

                        // 3. Remaining Daily Timeline Section
                        if (uiState.remainingTimelineItems.isNotEmpty()) {
                            item(key = "timeline_header", contentType = "section_header") {
                                Spacer(modifier = Modifier.height(2.dp))
                                SectionHeader(
                                    title = if (uiState.heroItem != null) "Rest of Today's Schedule" else "Today's Dose Log",
                                    subtitle = "${uiState.remainingTimelineItems.size} scheduled occurrence(s)"
                                )
                            }

                            items(
                                items = uiState.remainingTimelineItems,
                                key = { it.occurrence.occurrenceId },
                                contentType = { "timeline_dose_item" }
                            ) { item ->
                                TimelineDoseItem(
                                    occurrence = item.occurrence,
                                    medication = item.medication,
                                    stockBalance = item.stockBalance,
                                    consumption = item.consumption,
                                    onTakeClick = {
                                        viewModel.onTakeDose(item.occurrence)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Recorded ${item.medication.name} as taken")
                                        }
                                    },
                                    onSkipClick = {
                                        viewModel.onSkipDose(item.occurrence)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Marked ${item.medication.name} as skipped")
                                        }
                                    },
                                    onSnoozeClick = {
                                        viewModel.onSnoozeDose(item.occurrence)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Snoozed ${item.medication.name} for 15 minutes")
                                        }
                                    },
                                    onCardClick = { onSelectMedication(item.medication.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeaderSection(uiState: DashboardUiState) {
    val semantic = MaterialTheme.noveloColors
    val rawProgress = if (uiState.totalDoses > 0) {
        uiState.takenDoses.toFloat() / uiState.totalDoses.toFloat()
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = NoveloMotion.standardFloatSpring(),
        label = "dailyAdherenceProgress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Top Row: Brand Logo + Date Pill + Offline Privacy Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_novelo_logo),
                    contentDescription = "Nuvilo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
                Text(
                    text = "Nuvilo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = uiState.formattedDate.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Offline Protected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Greeting Title
        Text(
            text = uiState.greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )

        // Daily Adherence Progress Strip (when schedules exist)
        if (uiState.totalDoses > 0) {
            val isComplete = uiState.takenDoses == uiState.totalDoses
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NoveloCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Adherence Progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isComplete) {
                                        semantic.takenContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${uiState.takenDoses} / ${uiState.totalDoses} taken (${(rawProgress * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.tabular,
                                fontWeight = FontWeight.Bold,
                                color = if (isComplete) {
                                    semantic.taken
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isComplete) semantic.taken else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
