package com.example.nuvilo.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.nuvilo.domain.model.AdministrationRoute
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.PackageType
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.stock.SupplyForecast
import com.example.nuvilo.ui.theme.LocalMotionEnabled
import com.example.nuvilo.ui.theme.NoveloBannerShape
import com.example.nuvilo.ui.theme.NoveloCardShape
import com.example.nuvilo.ui.theme.NoveloMotion
import com.example.nuvilo.ui.theme.NoveloPillShape
import com.example.nuvilo.ui.theme.noveloColors
import com.example.nuvilo.ui.theme.tabular

/**
 * Maps a [MedicationForm] to its default [PackageType].
 */
fun defaultPackageTypeForForm(form: MedicationForm): PackageType = when (form) {
    MedicationForm.TABLET, MedicationForm.CAPSULE -> PackageType.BLISTER_PACK
    MedicationForm.LIQUID_ML, MedicationForm.DROPS -> PackageType.BOTTLE
    MedicationForm.INHALER_PUFFS -> PackageType.INHALER
    MedicationForm.TOPICAL_CREAM -> PackageType.TUBE
    MedicationForm.INJECTION -> PackageType.VIAL
    MedicationForm.PATCH -> PackageType.PATCH_BOX
    MedicationForm.OTHER -> PackageType.CUSTOM
}

/**
 * Returns contextually relevant purchasing/storage [PackageType] options for a given [MedicationForm].
 */
fun availablePackageTypesForForm(form: MedicationForm): List<PackageType> = when (form) {
    MedicationForm.DROPS -> listOf(
        PackageType.BOTTLE,
        PackageType.BOX_SINGLE_DOSE,
        PackageType.SINGLE_DOSE_UNIT,
        PackageType.CUSTOM
    )
    MedicationForm.TOPICAL_CREAM -> listOf(
        PackageType.TUBE,
        PackageType.JAR,
        PackageType.CUSTOM
    )
    MedicationForm.TABLET, MedicationForm.CAPSULE -> listOf(
        PackageType.BLISTER_PACK,
        PackageType.BOTTLE,
        PackageType.CUSTOM
    )
    MedicationForm.LIQUID_ML -> listOf(
        PackageType.BOTTLE,
        PackageType.SINGLE_DOSE_UNIT,
        PackageType.CUSTOM
    )
    MedicationForm.INHALER_PUFFS -> listOf(
        PackageType.INHALER,
        PackageType.CUSTOM
    )
    MedicationForm.INJECTION -> listOf(
        PackageType.VIAL,
        PackageType.SINGLE_DOSE_UNIT,
        PackageType.CUSTOM
    )
    MedicationForm.PATCH -> listOf(
        PackageType.PATCH_BOX,
        PackageType.CUSTOM
    )
    MedicationForm.OTHER -> listOf(
        PackageType.CUSTOM,
        PackageType.BOTTLE,
        PackageType.BLISTER_PACK
    )
}

/**
 * Sensible default package content capacity string for pre-filling Step 1 / Step 3.
 */
fun defaultContentsPerPackageStr(
    form: MedicationForm,
    packageType: PackageType,
    unit: StockUnit
): String = when {
    form == MedicationForm.DROPS && packageType == PackageType.BOTTLE -> "300"
    packageType == PackageType.BOX_SINGLE_DOSE -> "30"
    packageType == PackageType.SINGLE_DOSE_UNIT -> "1"
    form == MedicationForm.TOPICAL_CREAM && unit == StockUnit.APPLICATIONS -> "30"
    form == MedicationForm.TOPICAL_CREAM && unit == StockUnit.GRAMS -> "15"
    form == MedicationForm.LIQUID_ML -> "100"
    form == MedicationForm.INHALER_PUFFS -> "120"
    form == MedicationForm.PATCH -> "8"
    else -> "28"
}

/**
 * Contextual [AdministrationRoute] choices tailored to the selected [MedicationForm].
 */
fun availableRoutesForForm(form: MedicationForm): List<AdministrationRoute> = when (form) {
    MedicationForm.TABLET, MedicationForm.CAPSULE -> listOf(
        AdministrationRoute.ORAL,
        AdministrationRoute.SUBLINGUAL,
        AdministrationRoute.OTHER
    )
    MedicationForm.LIQUID_ML -> listOf(
        AdministrationRoute.ORAL,
        AdministrationRoute.NASAL,
        AdministrationRoute.OTHER
    )
    MedicationForm.DROPS -> listOf(
        AdministrationRoute.OPHTHALMIC,
        AdministrationRoute.OTIC,
        AdministrationRoute.NASAL,
        AdministrationRoute.OTHER
    )
    MedicationForm.INHALER_PUFFS -> listOf(
        AdministrationRoute.INHALATION,
        AdministrationRoute.NASAL,
        AdministrationRoute.OTHER
    )
    MedicationForm.TOPICAL_CREAM -> listOf(
        AdministrationRoute.TOPICAL,
        AdministrationRoute.OPHTHALMIC,
        AdministrationRoute.OTHER
    )
    MedicationForm.INJECTION -> listOf(
        AdministrationRoute.SUBCUTANEOUS,
        AdministrationRoute.INTRAMUSCULAR,
        AdministrationRoute.OTHER
    )
    MedicationForm.PATCH -> listOf(
        AdministrationRoute.TRANSDERMAL,
        AdministrationRoute.OTHER
    )
    MedicationForm.OTHER -> AdministrationRoute.entries
}

/**
 * Maps a [MedicationForm] to an intuitive vector icon and concise supporting label.
 */
fun medicationFormVisualMeta(form: MedicationForm): Pair<ImageVector, String> = when (form) {
    MedicationForm.TABLET -> Icons.Default.Medication to "Oral solid"
    MedicationForm.CAPSULE -> Icons.Default.LocalPharmacy to "Oral capsule"
    MedicationForm.LIQUID_ML -> Icons.Default.Science to "Measured ml"
    MedicationForm.DROPS -> Icons.Default.WaterDrop to "Ophthalmic / otic"
    MedicationForm.INHALER_PUFFS -> Icons.Default.Air to "Actuated puffs"
    MedicationForm.TOPICAL_CREAM -> Icons.Default.Healing to "Skin / gel"
    MedicationForm.INJECTION -> Icons.Default.Vaccines to "Prefilled / vial"
    MedicationForm.PATCH -> Icons.Default.Layers to "Transdermal"
    MedicationForm.OTHER -> Icons.Default.Category to "Custom format"
}

/**
 * Contextual icon and short helper description for a purchasing [PackageType].
 */
fun packageTypeVisualMeta(
    packageType: PackageType,
    form: MedicationForm
): Pair<String, Pair<ImageVector, String>> = when (packageType) {
    PackageType.BLISTER_PACK -> "Blister pack" to (Icons.Default.Layers to "Foil strip / box")
    PackageType.BOTTLE -> "Bottle" to (
        if (form == MedicationForm.DROPS) Icons.Default.WaterDrop to "Dropper bottle"
        else Icons.Default.Science to "Dispensing bottle"
    )
    PackageType.BOX_SINGLE_DOSE -> "Single-dose box" to (Icons.Default.Inventory2 to "Box of single vials")
    PackageType.SINGLE_DOSE_UNIT -> "Single-dose container" to (Icons.Default.Science to "1 unit per use")
    PackageType.TUBE -> "Tube" to (Icons.Default.Healing to "Squeeze tube")
    PackageType.JAR -> "Jar" to (Icons.Default.Inventory2 to "Tub / jar")
    PackageType.INHALER -> "Inhaler device" to (Icons.Default.Air to "Pressurised / DPI")
    PackageType.VIAL -> "Vial / Pen" to (Icons.Default.Vaccines to "Injectable container")
    PackageType.PATCH_BOX -> "Box of patches" to (Icons.Default.Layers to "Sealed patch pack")
    PackageType.CUSTOM -> (
        if (form == MedicationForm.TABLET || form == MedicationForm.CAPSULE) "Box / Other" else "Other package"
    ) to (Icons.Default.Category to "Custom container")
}

/**
 * Synchronizes full-screen [Dialog] window system bar icon appearance with Nuvilo's active light/dark theme
 * so status bar icons remain readable against the wizard header in both light and dark modes.
 */
@Composable
fun DialogSystemBarsEffect() {
    val view = LocalView.current
    val isDark = MaterialTheme.noveloColors.isDark
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
                ?: (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }
}

/**
 * Unified NHS-blue FilterChip palette for cohesive selection states across wizard steps.
 */
@Composable
fun noveloWizardFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
    selectedTrailingIconColor = MaterialTheme.colorScheme.primary
)

/**
 * Unified top bar and animated 3-step horizontal progress indicator for Add & Edit Medication wizards.
 */
@Composable
fun AddMedicationTopBarAndProgressHeader(
    title: String,
    subtitle: String,
    currentStep: Int,
    onClose: () -> Unit,
    onStepSelected: (Int) -> Unit,
    completedSteps: Set<Int> = emptySet(),
    modifier: Modifier = Modifier
) {
    val motionEnabled = LocalMotionEnabled.current
    val semantic = MaterialTheme.noveloColors
    val steps = listOf(
        0 to ("01" to "Identity"),
        1 to ("02" to "Schedule"),
        2 to ("03" to "Stock")
    )
    val animatedProgress by animateFloatAsState(
        targetValue = (currentStep + 1) / 3f,
        animationSpec = if (motionEnabled) tween(NoveloMotion.DURATION_STANDARD) else tween(0),
        label = "wizardProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("wizard_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = NoveloPillShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Step ${currentStep + 1} of 3",
                        style = MaterialTheme.typography.labelMedium.tabular,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Compact aligned 3-step indicator track
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                steps.forEachIndexed { index, (stepIdx, pair) ->
                    val (stepNum, stepName) = pair
                    val isActive = currentStep == stepIdx
                    val isDone = stepIdx < currentStep || completedSteps.contains(stepIdx) && !isActive

                    val badgeBg by animateColorAsState(
                        targetValue = when {
                            isActive -> semantic.interactionBlue
                            isDone -> semantic.taken
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        animationSpec = if (motionEnabled) tween(NoveloMotion.DURATION_FAST) else tween(0),
                        label = "stepBadgeBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isDone -> semantic.taken
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = if (motionEnabled) tween(NoveloMotion.DURATION_FAST) else tween(0),
                        label = "stepTextColor"
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                role = Role.Tab,
                                onClickLabel = "Go to step $stepNum $stepName"
                            ) { onStepSelected(stepIdx) }
                            .semantics {
                                selected = isActive
                                contentDescription = "$stepNum $stepName"
                                stateDescription = when {
                                    isActive -> "Current step"
                                    isDone -> "Completed"
                                    else -> "Upcoming"
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = stepNum,
                                    style = MaterialTheme.typography.labelSmall.tabular,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stepName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .clip(NoveloPillShape)
                                .background(
                                    if (stepIdx < currentStep) semantic.taken
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(NoveloPillShape),
                color = semantic.interactionBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Grouped section container replacing flat/scattered inputs with clean hierarchy.
 */
@Composable
fun WizardSectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NoveloCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

/**
 * Responsive 2-column card selection grid for [MedicationForm].
 */
@Composable
fun DosageFormSelectionGrid(
    selectedForm: MedicationForm,
    onSelectForm: (MedicationForm) -> Unit,
    modifier: Modifier = Modifier
) {
    val motionEnabled = LocalMotionEnabled.current
    val semantic = MaterialTheme.noveloColors
    val forms = MedicationForm.entries
    val rows = forms.chunked(2)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowForms ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowForms.forEach { form ->
                    val isSelected = selectedForm == form
                    val (icon, desc) = medicationFormVisualMeta(form)
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (semantic.isDark) 0.55f else 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        animationSpec = if (motionEnabled) tween(NoveloMotion.DURATION_FAST) else tween(0),
                        label = "formCardBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) semantic.interactionBlue else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = if (motionEnabled) tween(NoveloMotion.DURATION_FAST) else tween(0),
                        label = "formCardBorder"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                role = Role.RadioButton,
                                onClickLabel = "Select ${form.displayName}"
                            ) { onSelectForm(form) }
                            .semantics {
                                selected = isSelected
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = containerColor,
                        border = BorderStroke(if (isSelected) 1.75.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) semantic.interactionBlue.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) semantic.interactionBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = form.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = semantic.interactionBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                if (rowForms.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Contextual card selection grid for purchasing/storage [PackageType]s matching the chosen [MedicationForm].
 */
@Composable
fun PackageTypeSelectionGrid(
    form: MedicationForm,
    selectedPackageType: PackageType,
    onSelectPackageType: (PackageType) -> Unit,
    modifier: Modifier = Modifier
) {
    val motionEnabled = LocalMotionEnabled.current
    val semantic = MaterialTheme.noveloColors
    val options = availablePackageTypesForForm(form)
    val rows = options.chunked(2)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { pkgType ->
                    val isSelected = selectedPackageType == pkgType
                    val (labelOverride, iconDesc) = packageTypeVisualMeta(pkgType, form)
                    val (icon, desc) = iconDesc
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (semantic.isDark) 0.55f else 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        animationSpec = if (motionEnabled) tween(NoveloMotion.DURATION_FAST) else tween(0),
                        label = "pkgCardBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) semantic.interactionBlue else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = if (motionEnabled) tween(NoveloMotion.DURATION_FAST) else tween(0),
                        label = "pkgCardBorder"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                role = Role.RadioButton,
                                onClickLabel = "Select ${pkgType.displayName}"
                            ) { onSelectPackageType(pkgType) }
                            .semantics { selected = isSelected },
                        shape = RoundedCornerShape(14.dp),
                        color = containerColor,
                        border = BorderStroke(if (isSelected) 1.75.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) semantic.interactionBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pkgType.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = semantic.interactionBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Protects user-customized package capacities/rules when switching dosage form.
 */
@Composable
fun FormChangeConfirmationDialog(
    currentForm: MedicationForm,
    pendingForm: MedicationForm,
    onDismissKeepCurrent: () -> Unit,
    onConfirmSwitch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissKeepCurrent,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Change dosage form?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Switching from ${currentForm.displayName} to ${pendingForm.displayName} updates your package options and resets your customized package capacity and discard rule to ${pendingForm.displayName} defaults.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onConfirmSwitch) {
                Text("Switch Form")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissKeepCurrent) {
                Text("Keep ${currentForm.displayName}")
            }
        }
    )
}

/**
 * Accessible `-` / value / `+` integer quantity stepper control (>= 48dp buttons).
 */
@Composable
fun PackageQuantityStepper(
    label: String,
    value: Int,
    unitLabel: String,
    minValue: Int = 1,
    maxValue: Int = 99,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$value $unitLabel",
                style = MaterialTheme.typography.bodySmall.tabular,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { if (value > minValue) onValueChange(value - 1) },
                enabled = value > minValue,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.tabular,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = { if (value < maxValue) onValueChange(value + 1) },
                enabled = value < maxValue,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label")
            }
        }
    }
}

/**
 * Step 3 live inventory & supply preview card separating physical packages, internal capacity,
 * daily consumption rate, and honest supply runway.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveInventoryPreviewCard(
    medicationName: String,
    strength: String,
    packageType: PackageType,
    packageCount: Int,
    perPackageQty: ScaledQuantity,
    totalOpeningQty: ScaledQuantity,
    consumptionUnit: StockUnit,
    dosePerAdminQty: ScaledQuantity,
    administrationsPerDay: Int,
    dailyRateQty: ScaledQuantity?,
    forecast: SupplyForecast,
    discardAfterOpeningDays: Int?,
    expiryDateFormatted: String?,
    modifier: Modifier = Modifier
) {
    val motionEnabled = LocalMotionEnabled.current
    val semantic = MaterialTheme.noveloColors
    val pkgNoun = if (packageCount == 1) packageType.displayName.lowercase() else packageType.pluralName.lowercase()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NoveloCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = if (semantic.isDark) 0.25f else 0.38f
            )
        ),
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
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
                Text(
                    text = "Regimen Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TrackingPrecisionBadge(precision = forecast.precision)
            }

            Text(
                text = "${medicationName.ifBlank { "New Medication" }} · ${strength.ifBlank { "Strength needed" }} (${packageType.displayName})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            val initialSupplyText = if (perPackageQty.isPositive()) {
                "$packageCount $pkgNoun × ${perPackageQty.toDisplayString()} = ${totalOpeningQty.toDisplayString()} ${consumptionUnit.abbreviation}"
            } else {
                "$packageCount $pkgNoun"
            }
            AnimatedContent(
                targetState = initialSupplyText,
                transitionSpec = { NoveloMotion.screenTransition(motionEnabled) },
                label = "inventoryPreviewNumbers"
            ) { supplyText ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryMetricRow(
                        label = "Initial supply",
                        value = supplyText
                    )
                    SummaryMetricRow(
                        label = "Current dosage",
                        value = "${dosePerAdminQty.toDisplayString()} ${consumptionUnit.abbreviation} × $administrationsPerDay/day"
                    )
                    if (dailyRateQty != null && dailyRateQty.isPositive()) {
                        SummaryMetricRow(
                            label = "Daily consumption",
                            value = "${dailyRateQty.toDisplayString()} ${consumptionUnit.abbreviation}/day"
                        )
                    }
                    SummaryMetricRow(
                        label = "Estimated supply",
                        value = forecast.daysOfSupply?.let { "Approximately $it day(s)" }
                            ?: "Needs schedule & capacity",
                        highlight = forecast.daysOfSupply != null
                    )
                    if (expiryDateFormatted != null) {
                        SummaryMetricRow(
                            label = "Package expiry",
                            value = expiryDateFormatted
                        )
                    }
                    if (discardAfterOpeningDays != null) {
                        SummaryMetricRow(
                            label = "After-opening limit",
                            value = "Discard after $discardAfterOpeningDays days"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    val semantic = MaterialTheme.noveloColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.tabular,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) semantic.interactionBlue else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Keyboard-aware sticky bottom footer with clear missing-requirement guidance
 * so the user always knows why Continue or Save is disabled or blocked.
 */
@Composable
fun WizardBottomActionBar(
    currentStep: Int,
    primaryLabel: String,
    primaryEnabled: Boolean,
    missingRequirementHint: String?,
    isSubmitting: Boolean = false,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.noveloColors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(visible = !missingRequirementHint.isNullOrBlank()) {
                Surface(
                    shape = NoveloBannerShape,
                    color = semantic.warningContainer,
                    border = BorderStroke(1.dp, semantic.warningBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = semantic.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = missingRequirementHint.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = semantic.warning
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !isSubmitting,
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    TextButton(
                        onClick = onCancel,
                        enabled = !isSubmitting,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Button(
                    onClick = onPrimaryClick,
                    enabled = primaryEnabled && !isSubmitting,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(if (currentStep < 2) "wizard_continue_button" else "wizard_save_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = semantic.interactionBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    } else {
                        Text(primaryLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (currentStep < 2) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
