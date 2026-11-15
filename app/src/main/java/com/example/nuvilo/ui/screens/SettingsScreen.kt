package com.example.nuvilo.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nuvilo.R
import com.example.nuvilo.data.preferences.ThemeMode
import com.example.nuvilo.security.BiometricAuthManager
import com.example.nuvilo.security.BiometricAuthResult
import com.example.nuvilo.security.BiometricStatus
import com.example.nuvilo.security.LockGracePeriod
import com.example.nuvilo.system.notification.NuviloNotificationCategory
import com.example.nuvilo.ui.haptics.HapticIntensity
import com.example.nuvilo.ui.haptics.LocalNuviloHaptics
import com.example.nuvilo.ui.haptics.NuviloHapticEvent
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.viewmodel.ExportState
import com.example.nuvilo.ui.viewmodel.RestoreState
import com.example.nuvilo.ui.viewmodel.SettingsViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val EXPORT_TS_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
private val FILE_TS_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val semantic = MaterialTheme.noveloColors

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val nuviloHaptics = LocalNuviloHaptics.current

    var canExactAlarms by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var notificationsEnabled by remember { mutableStateOf(viewModel.areNotificationsEnabled()) }
    var biometricStatus by remember { mutableStateOf(viewModel.getBiometricStatus()) }
    var channelDiagnostics by remember(prefs) { mutableStateOf(viewModel.getChannelDiagnostics(prefs)) }
    var showTestSentMessage by rememberSaveable { mutableStateOf(false) }
    var showReconcileSuccess by rememberSaveable { mutableStateOf(false) }
    var biometricMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var diagnosticsExpanded by rememberSaveable { mutableStateOf(false) }
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }

    // Backup/Restore Dialog states
    var showCreatePassphraseDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }
    var showCsvExportConfirmDialog by rememberSaveable { mutableStateOf(false) }

    var decryptPassphraseInput by remember { mutableStateOf("") }
    var decryptPassphraseError by remember { mutableStateOf<String?>(null) }

    // SAF Launchers
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        viewModel.setSystemDialogActive(false)
        val pass = pendingExportPassphrase
        pendingExportPassphrase = null
        if (uri != null && pass != null) {
            val out = context.contentResolver.openOutputStream(uri)
            if (out != null) {
                viewModel.exportEncryptedJson(pass.toCharArray(), out)
            }
        }
    }

    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        viewModel.setSystemDialogActive(false)
        if (uri != null) {
            val out = context.contentResolver.openOutputStream(uri)
            if (out != null) {
                viewModel.exportCsvZip(out)
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.setSystemDialogActive(false)
        if (uri != null) {
            try {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val isZipHint = mime.contains("zip", ignoreCase = true) || uri.toString().endsWith(".zip", ignoreCase = true)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    viewModel.inspectImportFile(bytes, isZipHint)
                }
            } catch (_: Exception) {
                // Handled by ViewModel state
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canExactAlarms = viewModel.canScheduleExactAlarms()
                notificationsEnabled = viewModel.areNotificationsEnabled()
                biometricStatus = viewModel.getBiometricStatus()
                channelDiagnostics = viewModel.getChannelDiagnostics(prefs)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Column {
            Text(
                text = "Settings & Privacy",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Appearance, biometric lock, encrypted local backups, and system health",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Appearance & Theme Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose your preferred visual palette",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionCard(
                        title = "System",
                        subtitle = "Auto match",
                        icon = Icons.Default.BrightnessAuto,
                        selected = prefs.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Light",
                        subtitle = "NHS Blue",
                        icon = Icons.Default.LightMode,
                        selected = prefs.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Dark",
                        subtitle = "Slate Navy",
                        icon = Icons.Default.DarkMode,
                        selected = prefs.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Privacy & Security Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Privacy & App Lock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Protect sensitive prescription and adherence data on your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Biometric / Device Credential App Lock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Biometric / Device Lock",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val isAuthAvailable = biometricStatus == BiometricStatus.AVAILABLE ||
                            biometricStatus == BiometricStatus.DEVICE_CREDENTIAL_ONLY
                        val subText = when (biometricStatus) {
                            BiometricStatus.AVAILABLE -> "Require fingerprint, face, or device PIN to open Nuvilo."
                            BiometricStatus.DEVICE_CREDENTIAL_ONLY -> "Require device PIN, pattern, or password to open Nuvilo."
                            BiometricStatus.NONE_ENROLLED -> "No biometric or screen lock enrolled in Android Settings."
                            BiometricStatus.NO_HARDWARE, BiometricStatus.UNAVAILABLE -> "Device security authentication is unavailable on this device."
                        }
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAuthAvailable) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                semantic.warning
                            }
                        )
                    }
                    val isAuthAvailable = biometricStatus == BiometricStatus.AVAILABLE ||
                        biometricStatus == BiometricStatus.DEVICE_CREDENTIAL_ONLY
                    Switch(
                        checked = prefs.isBiometricLockEnabled,
                        enabled = isAuthAvailable || prefs.isBiometricLockEnabled,
                        onCheckedChange = { wantEnabled ->
                            biometricMessage = null
                            if (!wantEnabled) {
                                viewModel.setBiometricLock(false)
                            } else {
                                val activity = context as? FragmentActivity
                                if (activity != null && isAuthAvailable) {
                                    viewModel.setSystemDialogActive(true)
                                    val authManager = BiometricAuthManager(context)
                                    authManager.authenticate(
                                        activity = activity,
                                        onResult = { result ->
                                            viewModel.setSystemDialogActive(false)
                                            when (result) {
                                                is BiometricAuthResult.Success -> {
                                                    viewModel.setBiometricLock(true)
                                                    biometricMessage = "App Lock enabled"
                                                }
                                                is BiometricAuthResult.Failed -> {
                                                    biometricMessage = result.message
                                                }
                                                is BiometricAuthResult.Error -> {
                                                    biometricMessage = result.errString.toString()
                                                }
                                                is BiometricAuthResult.Cancelled -> {}
                                            }
                                        }
                                    )
                                } else {
                                    biometricMessage = "Please set up a screen lock or fingerprint in Android Settings first."
                                }
                            }
                        }
                    )
                }

                if (biometricStatus == BiometricStatus.NONE_ENROLLED) {
                    OutlinedButton(
                        onClick = {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                            } else {
                                Intent(Settings.ACTION_SECURITY_SETTINGS)
                            }
                            context.startActivity(intent)
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enrol Biometrics / Screen Lock")
                    }
                }

                if (biometricMessage != null) {
                    Text(
                        text = biometricMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (prefs.isBiometricLockEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Auto-Lock Grace Period",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LockGracePeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = prefs.lockGracePeriod == period,
                                    onClick = { viewModel.setLockGracePeriod(period) },
                                    label = { Text(period.displayName) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Mask App Switcher Preview",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Applies FLAG_SECURE to hide screen previews in recent apps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.isRecentsPreviewMasked,
                            onCheckedChange = { viewModel.setRecentsPreviewMasked(it) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Notification Privacy Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Mask Lock-Screen Notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hides medication names and dose strengths on reminders ('You have a scheduled medication reminder').",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.isNotificationPrivacyMasked,
                        onCheckedChange = { viewModel.setNotificationPrivacy(it) }
                    )
                }
            }
        }

        // 3. Local Data Backup & Restore (SAF)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Local Data Backup & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val lastExportText = prefs.lastExportEpochMs.takeIf { it > 0L }?.let { ts ->
                            val dt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
                            "Last backup: ${dt.format(EXPORT_TS_FORMATTER)}"
                        } ?: "No local backup exported yet"
                        Text(
                            text = lastExportText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Option A: Encrypted Full Backup Card
                BackupFormatCard(
                    icon = Icons.Default.Lock,
                    badge = "RECOMMENDED FOR MIGRATION",
                    title = "Encrypted JSON Backup (.nuvilo.json)",
                    description = "Protects medications, schedules, doses, stock ledger, and prescriptions with AES-256-GCM + PBKDF2 passphrase encryption.",
                    buttonLabel = "Export Encrypted Backup",
                    isPrimary = true,
                    onClick = {
                        viewModel.resetExportState()
                        showCreatePassphraseDialog = true
                    }
                )

                // Option B: Readable CSV ZIP Archive Card
                BackupFormatCard(
                    icon = Icons.Default.TableView,
                    badge = "UNENCRYPTED SPREADSHEET BUNDLE",
                    title = "Readable CSV Archive (.zip)",
                    description = "Exports 6 human-readable CSV tables for personal spreadsheet review or clinical printing.",
                    buttonLabel = "Export CSV Bundle (.zip)",
                    isPrimary = false,
                    onClick = {
                        viewModel.resetExportState()
                        showCsvExportConfirmDialog = true
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                FilledTonalButton(
                    onClick = {
                        viewModel.resetRestoreState()
                        viewModel.setSystemDialogActive(true)
                        openBackupLauncher.launch(arrayOf("*/*"))
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import & Restore from Backup File", style = MaterialTheme.typography.labelLarge)
                }

                // Export / Restore Status Feedback
                when (val s = exportState) {
                    is ExportState.InProgress -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Exporting backup...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is ExportState.Success -> {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.taken,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is ExportState.Error -> {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.missed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    ExportState.Idle -> {}
                }

                when (val r = restoreState) {
                    is RestoreState.Validating, is RestoreState.Restoring -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = if (r is RestoreState.Validating) "Validating backup file..." else "Restoring database...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is RestoreState.Success -> {
                        Text(
                            text = r.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.taken,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is RestoreState.Error -> {
                        Text(
                            text = r.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.missed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {}
                }
            }
        }

        // 3b. Tactile & Haptic Feedback Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Haptic & Tactile Feedback",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Subtle vibrations for dose confirmations, snoozes, package actions, and errors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.hapticsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setHapticsEnabled(enabled)
                            if (enabled) nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                        }
                    )
                }

                if (prefs.hapticsEnabled) {
                    Text(
                        text = "Feedback Strength",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HapticIntensity.entries.forEach { level ->
                            val label = when (level) {
                                HapticIntensity.LOW -> "Gentle"
                                HapticIntensity.MEDIUM -> "Balanced"
                                HapticIntensity.HIGH -> "Distinct"
                            }
                            FilterChip(
                                selected = prefs.hapticIntensity == level,
                                onClick = {
                                    viewModel.setHapticIntensity(level)
                                    nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                },
                                label = { Text(label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { nuviloHaptics.perform(NuviloHapticEvent.DOSE_TAKEN) },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Test Haptic Feel")
                        }
                        if (!nuviloHaptics.isHardwareVibratorAvailable()) {
                            Text(
                                text = "Device vibrator unavailable",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.warning
                            )
                        }
                    }
                }
            }
        }

        // 3c. Notification Categories Card (7 channels, silent+vibration default for reminders)
        val categoriesChevron by animateFloatAsState(
            targetValue = if (categoriesExpanded) 180f else 0f,
            animationSpec = NoveloMotion.standardFloatSpring(),
            label = "notifCategoriesChevron"
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                categoriesExpanded = !categoriesExpanded
                            },
                            role = Role.Button
                        )
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Notification Categories (7)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Medication reminders default to silent + vibration; customise per category",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (categoriesExpanded) "Collapse notification categories" else "Expand notification categories",
                        modifier = Modifier.rotate(categoriesChevron),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = categoriesExpanded,
                    enter = NoveloMotion.sectionExpand(),
                    exit = NoveloMotion.sectionCollapse()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val diagMap = channelDiagnostics.associateBy { it.category }
                        NuviloNotificationCategory.entries.forEach { category ->
                            val diag = diagMap[category]
                            Surface(
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
                                        Text(
                                            text = category.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val statusLabel = when {
                                            diag?.isBlockedInSystem == true -> "Blocked in OS"
                                            diag?.hasSoundInSystem == true && diag.hasVibrationInSystem -> "Sound + Vibrate"
                                            diag?.hasVibrationInSystem == true -> "Silent · Vibrate"
                                            else -> "Silent"
                                        }
                                        Text(
                                            text = statusLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (diag?.isBlockedInSystem == true) semantic.warning else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = category.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.buildChannelSettingsIntent(category.channelId)?.let { intent ->
                                                    context.startActivity(intent)
                                                }
                                            },
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Text("System Channel Settings", style = MaterialTheme.typography.labelMedium)
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                nuviloHaptics.perform(NuviloHapticEvent.SELECTION_TICK)
                                                viewModel.triggerTestNotification(category)
                                                showTestSentMessage = true
                                            },
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Test", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Collapsible System & Reminder Diagnostics
        val chevronRotation by animateFloatAsState(
            targetValue = if (diagnosticsExpanded) 180f else 0f,
            animationSpec = NoveloMotion.standardFloatSpring(),
            label = "diagnosticsChevron"
        )
        val allHealthy = canExactAlarms && notificationsEnabled

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = { diagnosticsExpanded = !diagnosticsExpanded },
                            role = Role.Button
                        )
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (allHealthy) MaterialTheme.colorScheme.primary else semantic.warning
                        )
                        Column {
                            Text(
                                text = "Reminder & Alarm Diagnostics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (allHealthy) {
                                    "All reminder permissions and exact alarms healthy"
                                } else {
                                    "Action needed: Check alarm or notification permissions"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (allHealthy) semantic.taken else semantic.warning
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (diagnosticsExpanded) "Collapse diagnostics" else "Expand diagnostics",
                        modifier = Modifier.rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = diagnosticsExpanded || !allHealthy,
                    enter = NoveloMotion.sectionExpand(),
                    exit = NoveloMotion.sectionCollapse()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        DiagnosticRow(
                            title = "Exact Alarm Permission",
                            isHealthy = canExactAlarms,
                            healthyLabel = "Granted (Exact timing active)",
                            warningLabel = "Restricted (Tap to grant exact alarm access)"
                        )
                        if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Exact Alarm Permission")
                            }
                        }

                        DiagnosticRow(
                            title = "Notification Channels",
                            isHealthy = notificationsEnabled,
                            healthyLabel = "Enabled",
                            warningLabel = "Blocked in System Settings"
                        )
                        if (!notificationsEnabled) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Notification Settings")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.reconcileReminders()
                                    showReconcileSuccess = true
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reconcile", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    viewModel.triggerTestNotification()
                                    showTestSentMessage = true
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Alert", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (showReconcileSuccess) {
                            Text(
                                text = "Upcoming alarms reconciled with active schedules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.taken,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (showTestSentMessage) {
                            Text(
                                text = "Test notification dispatched! Check your notification shade.",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.taken,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 5. About Novelo Footer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_novelo_logo),
                        contentDescription = "Nuvilo Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )
                    Column {
                        Text(
                            text = "Nuvilo 1.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Medication, made manageable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = "100% Offline SQLite Room · Zero Network Permissions · AES-256-GCM Backup",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Passphrase Creation Dialog for Encrypted JSON Backup
    if (showCreatePassphraseDialog) {
        var passphrase by rememberSaveable { mutableStateOf("") }
        var confirmPassphrase by rememberSaveable { mutableStateOf("") }
        var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
        var showPassphrase by rememberSaveable { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreatePassphraseDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Encrypt Backup File", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Your backup will be encrypted using AES-256-GCM with PBKDF2-HMAC-SHA256 key derivation. Keep this passphrase safe — Nuvilo operates offline and cannot recover forgotten passphrases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            errorMessage = null
                        },
                        label = { Text("Passphrase (min 8 chars)") },
                        visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassphrase = !showPassphrase }) {
                                Icon(
                                    imageVector = if (showPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassphrase) "Hide passphrase" else "Show passphrase"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = {
                            confirmPassphrase = it
                            errorMessage = null
                        },
                        label = { Text("Confirm Passphrase") },
                        visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passphrase.length < 8) {
                            errorMessage = "Passphrase must be at least 8 characters"
                        } else if (passphrase != confirmPassphrase) {
                            errorMessage = "Passphrases do not match"
                        } else {
                            pendingExportPassphrase = passphrase
                            showCreatePassphraseDialog = false
                            val timestamp = FILE_TS_FORMATTER.format(LocalDateTime.now())
                            viewModel.setSystemDialogActive(true)
                            createJsonLauncher.launch("nuvilo_backup_$timestamp.nuvilo.json")
                        }
                    }
                ) {
                    Text("Choose Destination & Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePassphraseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CSV Warning Dialog
    if (showCsvExportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCsvExportConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = semantic.warning) },
            title = { Text("Export Unencrypted CSV Archive?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "CSV archives are unencrypted plain text so you can open them in spreadsheet software. Anyone with access to the exported ZIP file can read your medication names, schedules, and adherence logs.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCsvExportConfirmDialog = false
                        val timestamp = FILE_TS_FORMATTER.format(LocalDateTime.now())
                        viewModel.setSystemDialogActive(true)
                        createZipLauncher.launch("nuvilo_export_$timestamp.zip")
                    }
                ) {
                    Text("Export Unencrypted ZIP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvExportConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Decrypt Passphrase Dialog for Encrypted JSON Restore
    val currentRestoreState = restoreState
    if (currentRestoreState is RestoreState.NeedsPassphrase) {
        var showDecryptPassphrase by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.resetRestoreState() },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Unlock Encrypted Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This backup is protected with AES-256-GCM encryption. Enter the passphrase used when it was exported:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = decryptPassphraseInput,
                        onValueChange = {
                            decryptPassphraseInput = it
                            decryptPassphraseError = null
                        },
                        label = { Text("Passphrase") },
                        visualTransformation = if (showDecryptPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showDecryptPassphrase = !showDecryptPassphrase }) {
                                Icon(
                                    imageVector = if (showDecryptPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showDecryptPassphrase) "Hide passphrase" else "Show passphrase"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (decryptPassphraseError != null) {
                        Text(
                            text = decryptPassphraseError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (decryptPassphraseInput.isBlank()) {
                            decryptPassphraseError = "Passphrase cannot be empty"
                        } else {
                            viewModel.decryptAndInspectJson(
                                currentRestoreState.envelopeJson,
                                decryptPassphraseInput.toCharArray()
                            )
                            decryptPassphraseInput = ""
                        }
                    }
                ) {
                    Text("Decrypt & Inspect")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    decryptPassphraseInput = ""
                    viewModel.resetRestoreState()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Preview Dialog
    if (currentRestoreState is RestoreState.Preview) {
        val preview = currentRestoreState.preview
        val backup = currentRestoreState.backup

        val exportTimeStr = if (preview.exportTimestampEpochMs > 0L) {
            val dt = Instant.ofEpochMilli(preview.exportTimestampEpochMs).atZone(ZoneId.systemDefault())
            dt.format(EXPORT_TS_FORMATTER)
        } else {
            "Unknown"
        }

        AlertDialog(
            onDismissRequest = { viewModel.resetRestoreState() },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Validated Backup Preview", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "All referential integrity and ledger checks passed. Review the contents before restoring:",
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreviewRow("Exported Date", exportTimeStr)
                            PreviewRow("Medications", "${preview.medicationCount}")
                            PreviewRow("Schedules", "${preview.scheduleCount}")
                            PreviewRow("Dose Records", "${preview.doseEventCount}")
                            PreviewRow("Stock Lots", "${preview.stockLotCount}")
                            PreviewRow("Ledger Entries", "${preview.stockLedgerEntryCount}")
                            PreviewRow("Prescriptions", "${preview.prescriptionCount}")
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = semantic.missedContainer,
                        border = BorderStroke(1.dp, semantic.missedBorder)
                    ) {
                        Text(
                            text = "Replacement Warning: Restoring will atomically replace all current medications, schedules, and history on this device. This cannot be undone.",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.missed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestore(backup) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetRestoreState() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick, role = Role.RadioButton),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun BackupFormatCard(
    icon: ImageVector,
    badge: String,
    title: String,
    description: String,
    buttonLabel: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            1.dp,
            if (isPrimary) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isPrimary) {
                Button(
                    onClick = onClick,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    isHealthy: Boolean,
    healthyLabel: String,
    warningLabel: String
) {
    val semantic = MaterialTheme.noveloColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isHealthy) healthyLabel else warningLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (isHealthy) semantic.taken else semantic.warning
            )
        }
        Icon(
            imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (isHealthy) "Healthy" else "Warning",
            tint = if (isHealthy) semantic.taken else semantic.warning,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
