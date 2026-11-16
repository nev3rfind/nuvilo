package com.example.nuvilo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.domain.ppc.ExemptionCategory
import com.example.nuvilo.domain.ppc.PpcCalculator
import com.example.nuvilo.domain.ppc.PpcCertificate
import com.example.nuvilo.domain.ppc.PpcCoverageType
import com.example.nuvilo.domain.ppc.PpcEvaluation
import com.example.nuvilo.domain.ppc.PpcOverlapResult
import com.example.nuvilo.domain.ppc.PpcReminderConfig
import com.example.nuvilo.domain.ppc.PpcStatus
import com.example.nuvilo.ui.theme.NoveloCardShape
import com.example.nuvilo.ui.theme.NoveloPillShape
import com.example.nuvilo.ui.theme.NoveloSheetShape
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.viewmodel.PpcViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

private val ppcDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PpcManagementScreen(
    viewModel: PpcViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val calculator = remember { PpcCalculator() }

    var showWizardSheet by rememberSaveable { mutableStateOf(false) }
    var editingCertificate by remember { mutableStateOf<PpcCertificate?>(null) }
    var renewingFromId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteCert by remember { mutableStateOf<PpcCertificate?>(null) }
    var showArchivedSection by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.actionMessage, uiState.errorMessage) {
        val msg = uiState.actionMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NHS PPC & Exemptions",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            editingCertificate = null
                            renewingFromId = null
                            showWizardSheet = true
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PpcPrimaryStatusCard(
                    evaluation = uiState.primaryEvaluation,
                    onAddClick = {
                        editingCertificate = null
                        renewingFromId = null
                        showWizardSheet = true
                    },
                    onEditClick = { cert ->
                        editingCertificate = cert
                        renewingFromId = null
                        showWizardSheet = true
                    },
                    onRenewClick = { cert ->
                        val draft = calculator.buildRenewalDraft(
                            previous = cert,
                            newId = UUID.randomUUID().toString()
                        )
                        editingCertificate = draft
                        renewingFromId = cert.id
                        showWizardSheet = true
                    }
                )
            }

            if (uiState.activeCertificates.isNotEmpty()) {
                item {
                    Text(
                        text = "Recorded Certificates & Exemptions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                items(
                    items = uiState.activeCertificates,
                    key = { it.id }
                ) { cert ->
                    val eval = calculator.evaluateCertificate(cert)
                    PpcCertificateItemCard(
                        certificate = cert,
                        evaluation = eval,
                        onEdit = {
                            editingCertificate = cert
                            renewingFromId = null
                            showWizardSheet = true
                        },
                        onRenew = {
                            val draft = calculator.buildRenewalDraft(
                                previous = cert,
                                newId = UUID.randomUUID().toString()
                            )
                            editingCertificate = draft
                            renewingFromId = cert.id
                            showWizardSheet = true
                        },
                        onToggleArchive = { viewModel.archiveCertificate(cert.id) },
                        onDeleteRequest = { pendingDeleteCert = cert }
                    )
                }
            }

            if (uiState.archivedCertificates.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Archived History (${uiState.archivedCertificates.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { heading() }
                        )
                        TextButton(
                            onClick = { showArchivedSection = !showArchivedSection },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text(if (showArchivedSection) "Hide" else "Show")
                        }
                    }
                }

                if (showArchivedSection) {
                    items(
                        items = uiState.archivedCertificates,
                        key = { it.id }
                    ) { cert ->
                        val eval = calculator.evaluateCertificate(cert)
                        PpcCertificateItemCard(
                            certificate = cert,
                            evaluation = eval,
                            onEdit = {
                                editingCertificate = cert
                                renewingFromId = null
                                showWizardSheet = true
                            },
                            onRenew = {
                                val draft = calculator.buildRenewalDraft(
                                    previous = cert,
                                    newId = UUID.randomUUID().toString()
                                )
                                editingCertificate = draft
                                renewingFromId = cert.id
                                showWizardSheet = true
                            },
                            onToggleArchive = { viewModel.unarchiveCertificate(cert.id) },
                            onDeleteRequest = { pendingDeleteCert = cert }
                        )
                    }
                }
            }
        }
    }

    if (showWizardSheet) {
        PpcWizardBottomSheet(
            initialCertificate = editingCertificate,
            isRenewal = renewingFromId != null,
            findOverlaps = { start, expiry, excludeId ->
                viewModel.findOverlaps(start, expiry, excludeId)
            },
            onDismiss = {
                showWizardSheet = false
                editingCertificate = null
                renewingFromId = null
            },
            onSave = { saved ->
                val prevId = renewingFromId
                if (prevId != null) {
                    viewModel.renewCertificate(
                        previousId = prevId,
                        newCertificate = saved,
                        archivePrevious = true
                    )
                } else {
                    viewModel.saveCertificate(saved)
                }
                showWizardSheet = false
                editingCertificate = null
                renewingFromId = null
            }
        )
    }

    pendingDeleteCert?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCert = null },
            title = { Text("Delete certificate record?") },
            text = {
                Text(
                    "This will permanently remove \"${target.coverageType.shortLabel}\" " +
                        "(started ${target.startDate.format(ppcDateFormatter)}) and cancel any scheduled reminders. " +
                        "Consider archiving instead if you want to keep your history."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCertificate(target.id)
                        pendingDeleteCert = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCert = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PpcPrimaryStatusCard(
    evaluation: PpcEvaluation,
    onAddClick: () -> Unit,
    onEditClick: (PpcCertificate) -> Unit,
    onRenewClick: (PpcCertificate) -> Unit
) {
    val style = ppcStatusStyle(evaluation.status)
    val cert = evaluation.certificate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NoveloCardShape,
        colors = CardDefaults.cardColors(containerColor = style.containerColor),
        border = BorderStroke(1.dp, style.borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.contentColor
                )
                Surface(
                    shape = NoveloPillShape,
                    color = style.contentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = evaluation.status.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = style.contentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (cert == null) {
                Text(
                    text = "No Prescription Prepayment Certificate (PPC) or exemption recorded",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track a 3-month PPC, 12-month PPC, or Free NHS Prescription Exemption locally with renewal reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add PPC or Exemption")
                }
            } else {
                Text(
                    text = cert.coverageType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (cert.exemptionCategory != null) {
                    Text(
                        text = cert.exemptionCategory.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val countdownText = when (evaluation.status) {
                    PpcStatus.ACTIVE -> {
                        val days = evaluation.remainingDays
                        if (days == null) "Ongoing exemption (no expiry date)"
                        else "$days days remaining"
                    }
                    PpcStatus.EXPIRING_SOON -> {
                        val days = evaluation.remainingDays ?: 0L
                        if (days == 0L) "Expires today" else "$days days remaining"
                    }
                    PpcStatus.EXPIRED -> {
                        val days = kotlin.math.abs(evaluation.remainingDays ?: 0L)
                        if (days == 0L) "Expired today" else "Expired $days days ago"
                    }
                    PpcStatus.NOT_YET_VALID -> {
                        val days = evaluation.daysUntilStart ?: 0L
                        "Starts in $days days"
                    }
                    PpcStatus.NO_RECORD -> ""
                }

                if (countdownText.isNotEmpty()) {
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = style.contentColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                val expiryText = evaluation.effectiveExpiryDate?.format(ppcDateFormatter) ?: "No expiry date"
                Text(
                    text = "Start: ${cert.startDate.format(ppcDateFormatter)} · Expiry: $expiryText",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!cert.referenceNumber.isNullOrBlank()) {
                    Text(
                        text = "Reference: ${cert.referenceNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = evaluation.statusBasisNotice,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditClick(cert) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = { onRenewClick(cert) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark Renewed")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PpcCertificateItemCard(
    certificate: PpcCertificate,
    evaluation: PpcEvaluation,
    onEdit: () -> Unit,
    onRenew: () -> Unit,
    onToggleArchive: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val style = ppcStatusStyle(evaluation.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NoveloCardShape,
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
                        text = certificate.coverageType.shortLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (certificate.exemptionCategory != null) {
                        Text(
                            text = certificate.exemptionCategory.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = NoveloPillShape,
                    color = style.containerColor,
                    border = BorderStroke(1.dp, style.borderColor)
                ) {
                    Text(
                        text = if (certificate.isArchived) "Archived" else evaluation.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = style.contentColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            val expiryStr = evaluation.effectiveExpiryDate?.format(ppcDateFormatter) ?: "Ongoing"
            Text(
                text = "${certificate.startDate.format(ppcDateFormatter)} → $expiryStr",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!certificate.referenceNumber.isNullOrBlank()) {
                Text(
                    text = "Ref: ${certificate.referenceNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val offsets = certificate.reminderConfig.activeOffsetsDays()
            if (!certificate.isOngoing && offsets.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Reminders: ${offsets.joinToString(", ") { if (it == 0) "on expiry" else "${it}d before" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                TextButton(
                    onClick = onRenew,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Renew")
                }
                TextButton(
                    onClick = onToggleArchive,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (certificate.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (certificate.isArchived) "Restore" else "Archive")
                }
                TextButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PpcWizardBottomSheet(
    initialCertificate: PpcCertificate?,
    isRenewal: Boolean,
    findOverlaps: (LocalDate, LocalDate?, String?) -> List<PpcOverlapResult>,
    onDismiss: () -> Unit,
    onSave: (PpcCertificate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val calculator = remember { PpcCalculator() }

    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var coverageType by rememberSaveable {
        mutableStateOf(initialCertificate?.coverageType ?: PpcCoverageType.PPC_12_MONTH)
    }
    var exemptionCategory by rememberSaveable {
        mutableStateOf(initialCertificate?.exemptionCategory ?: ExemptionCategory.MEDICAL)
    }
    var startDate by remember {
        mutableStateOf(initialCertificate?.startDate ?: LocalDate.now())
    }
    var isOngoing by rememberSaveable {
        mutableStateOf(initialCertificate?.isOngoing ?: false)
    }
    var isManualOverride by rememberSaveable {
        mutableStateOf(initialCertificate?.isManualExpiryOverride ?: false)
    }
    var manualExpiryDate by remember {
        mutableStateOf(
            initialCertificate?.expiryDate
                ?: calculator.calculateExpiryDate(coverageType, startDate, isOngoing)
                ?: startDate.plusMonths(12).minusDays(1)
        )
    }
    var referenceNumber by rememberSaveable {
        mutableStateOf(initialCertificate?.referenceNumber.orEmpty())
    }
    var notes by rememberSaveable {
        mutableStateOf(initialCertificate?.notes.orEmpty())
    }
    var remind30 by rememberSaveable {
        mutableStateOf(initialCertificate?.reminderConfig?.remind30DaysBefore ?: true)
    }
    var remind14 by rememberSaveable {
        mutableStateOf(initialCertificate?.reminderConfig?.remind14DaysBefore ?: true)
    }
    var remind7 by rememberSaveable {
        mutableStateOf(initialCertificate?.reminderConfig?.remind7DaysBefore ?: true)
    }
    var remind0 by rememberSaveable {
        mutableStateOf(initialCertificate?.reminderConfig?.remindOnExpiryDay ?: true)
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    val effectiveExpiry = remember(coverageType, startDate, isOngoing, isManualOverride, manualExpiryDate) {
        if (coverageType == PpcCoverageType.FREE_EXEMPTION && isOngoing) {
            null
        } else if (isManualOverride || coverageType == PpcCoverageType.FREE_EXEMPTION) {
            manualExpiryDate
        } else {
            calculator.calculateExpiryDate(coverageType, startDate, isOngoing = false)
        }
    }

    val excludeId = if (isRenewal) null else initialCertificate?.id
    val overlaps = remember(startDate, effectiveExpiry, excludeId) {
        findOverlaps(startDate, effectiveExpiry, excludeId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = NoveloSheetShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = when {
                    isRenewal -> "Renew Certificate (Step $currentStep of 4)"
                    initialCertificate != null -> "Edit Certificate (Step $currentStep of 4)"
                    else -> "Add PPC or Exemption (Step $currentStep of 4)"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            LinearProgressIndicator(
                progress = { currentStep / 4f },
                modifier = Modifier.fillMaxWidth()
            )

            when (currentStep) {
                1 -> {
                    Text(
                        text = "Step 1: Coverage Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    PpcCoverageType.entries.forEach { option ->
                        val selected = coverageType == option
                        Card(
                            onClick = {
                                coverageType = option
                                if (option != PpcCoverageType.FREE_EXEMPTION) {
                                    isOngoing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = NoveloCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            ),
                            border = if (selected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = option.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (coverageType == PpcCoverageType.FREE_EXEMPTION) {
                        Text(
                            text = "Exemption Category",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExemptionCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = exemptionCategory == cat,
                                    onClick = { exemptionCategory = cat },
                                    label = { Text(cat.displayName) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                    }
                }

                2 -> {
                    Text(
                        text = "Step 2: Dates & Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Date: ${startDate.format(ppcDateFormatter)}")
                    }

                    if (coverageType == PpcCoverageType.FREE_EXEMPTION) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ongoing exemption (no expiry date)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Enable for age-related or permanent exemptions without an end date.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isOngoing,
                                onCheckedChange = { isOngoing = it }
                            )
                        }
                    }

                    if (!(coverageType == PpcCoverageType.FREE_EXEMPTION && isOngoing)) {
                        if (coverageType != PpcCoverageType.FREE_EXEMPTION) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Override calculated expiry date",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = isManualOverride,
                                    onCheckedChange = { checked ->
                                        isManualOverride = checked
                                        if (checked) {
                                            manualExpiryDate = calculator.calculateExpiryDate(coverageType, startDate)
                                                ?: startDate.plusMonths(3).minusDays(1)
                                        }
                                    }
                                )
                            }
                        }

                        if (isManualOverride || coverageType == PpcCoverageType.FREE_EXEMPTION) {
                            OutlinedButton(
                                onClick = { showExpiryDatePicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Expiry Date: ${manualExpiryDate.format(ppcDateFormatter)}")
                            }
                        } else {
                            Surface(
                                shape = NoveloCardShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Calculated Expiry Date",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = effectiveExpiry?.format(ppcDateFormatter) ?: "-",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    Text(
                        text = "Step 3: Reference, Notes & Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Certificate / Exemption Number (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!(coverageType == PpcCoverageType.FREE_EXEMPTION && isOngoing)) {
                        Text(
                            text = "Local Expiry Reminders",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        ReminderToggleRow("30 days before expiry", remind30) { remind30 = it }
                        ReminderToggleRow("14 days before expiry", remind14) { remind14 = it }
                        ReminderToggleRow("7 days before expiry", remind7) { remind7 = it }
                        ReminderToggleRow("On expiry day", remind0) { remind0 = it }
                    }
                }

                4 -> {
                    Text(
                        text = "Step 4: Review & Confirm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        shape = NoveloCardShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = coverageType.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (coverageType == PpcCoverageType.FREE_EXEMPTION) {
                                Text("Category: ${exemptionCategory.displayName}")
                            }
                            Text("Start Date: ${startDate.format(ppcDateFormatter)}")
                            Text(
                                text = "Expiry Date: ${effectiveExpiry?.format(ppcDateFormatter) ?: "Ongoing (no expiry)"}",
                                fontWeight = FontWeight.SemiBold
                            )
                            if (referenceNumber.isNotBlank()) {
                                Text("Reference: ${referenceNumber.trim()}")
                            }
                            if (notes.isNotBlank()) {
                                Text("Notes: ${notes.trim()}")
                            }
                            Text(
                                text = "Based on your recorded certificate dates",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (overlaps.isNotEmpty()) {
                        Surface(
                            shape = NoveloCardShape,
                            color = MaterialTheme.noveloColors.warningContainer,
                            border = BorderStroke(1.dp, MaterialTheme.noveloColors.warningBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.noveloColors.warning
                                )
                                Column {
                                    Text(
                                        text = "Date Overlap Warning",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.noveloColors.warning
                                    )
                                    val first = overlaps.first()
                                    Text(
                                        text = "These dates overlap your existing ${first.overlappingCertificate.coverageType.shortLabel} " +
                                            "(${first.overlapStart.format(ppcDateFormatter)} – ${first.overlapEnd?.format(ppcDateFormatter) ?: "Ongoing"}).",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Back")
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Cancel")
                    }
                }

                if (currentStep < 4) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val cert = PpcCertificate(
                                id = initialCertificate?.id ?: UUID.randomUUID().toString(),
                                coverageType = coverageType,
                                exemptionCategory = if (coverageType == PpcCoverageType.FREE_EXEMPTION) exemptionCategory else null,
                                startDate = startDate,
                                expiryDate = effectiveExpiry,
                                isOngoing = coverageType == PpcCoverageType.FREE_EXEMPTION && isOngoing,
                                isManualExpiryOverride = coverageType != PpcCoverageType.FREE_EXEMPTION && isManualOverride,
                                referenceNumber = referenceNumber.trim().takeIf { it.isNotEmpty() },
                                notes = notes.trim().takeIf { it.isNotEmpty() },
                                reminderConfig = PpcReminderConfig(
                                    remind30DaysBefore = remind30,
                                    remind14DaysBefore = remind14,
                                    remind7DaysBefore = remind7,
                                    remindOnExpiryDay = remind0
                                ),
                                isArchived = initialCertificate?.isArchived ?: false,
                                renewedFromId = initialCertificate?.renewedFromId,
                                createdAtEpochMs = initialCertificate?.createdAtEpochMs ?: now,
                                updatedAtEpochMs = now
                            )
                            onSave(cert)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Save Certificate")
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showStartDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showExpiryDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = manualExpiryDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            manualExpiryDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showExpiryDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ReminderToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private data class PpcStatusStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val icon: ImageVector
)

@Composable
private fun ppcStatusStyle(status: PpcStatus): PpcStatusStyle {
    val colors = MaterialTheme.noveloColors
    return when (status) {
        PpcStatus.ACTIVE -> PpcStatusStyle(
            containerColor = colors.takenContainer,
            contentColor = colors.taken,
            borderColor = colors.takenBorder,
            icon = Icons.Filled.VerifiedUser
        )
        PpcStatus.EXPIRING_SOON -> PpcStatusStyle(
            containerColor = colors.warningContainer,
            contentColor = colors.warning,
            borderColor = colors.warningBorder,
            icon = Icons.Filled.Warning
        )
        PpcStatus.EXPIRED -> PpcStatusStyle(
            containerColor = colors.missedContainer,
            contentColor = colors.missed,
            borderColor = colors.missedBorder,
            icon = Icons.Filled.ErrorOutline
        )
        PpcStatus.NOT_YET_VALID -> PpcStatusStyle(
            containerColor = colors.assumedContainer,
            contentColor = colors.assumed,
            borderColor = colors.assumedBorder,
            icon = Icons.Filled.Schedule
        )
        PpcStatus.NO_RECORD -> PpcStatusStyle(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            icon = Icons.Filled.Info
        )
    }
}
