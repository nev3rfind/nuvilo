package com.example.nuvilo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nuvilo.data.backup.BackupPreview
import com.example.nuvilo.data.backup.BackupRestoreRepository
import com.example.nuvilo.data.backup.BackupValidationResult
import com.example.nuvilo.data.backup.BackupValidator
import com.example.nuvilo.data.backup.BackupCrypto
import com.example.nuvilo.data.backup.CsvZipBackupManager
import com.example.nuvilo.data.backup.JsonBackupManager
import com.example.nuvilo.data.backup.NoveloDataBackup
import com.example.nuvilo.data.preferences.ThemeMode
import com.example.nuvilo.data.preferences.UserPreferences
import com.example.nuvilo.data.preferences.UserPreferencesRepository
import com.example.nuvilo.data.repository.DoseRepository
import com.example.nuvilo.data.repository.MedicationRepository
import com.example.nuvilo.data.repository.PrescriptionRepository
import com.example.nuvilo.data.repository.StockRepository
import com.example.nuvilo.domain.adherence.AdherenceCalculator
import com.example.nuvilo.domain.adherence.AdherenceSummary
import com.example.nuvilo.domain.adherence.DailyAdherenceRecord
import com.example.nuvilo.domain.adherence.DoseOutcomeFilter
import com.example.nuvilo.domain.adherence.HistoryDateFilter
import com.example.nuvilo.domain.adherence.MonthlyCalendarData
import com.example.nuvilo.domain.adherence.ResolvedDoseOccurrence
import com.example.nuvilo.domain.model.DoseOccurrence
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.MovementType
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionEvent
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockBalance
import com.example.nuvilo.domain.model.StockLedgerEntry
import com.example.nuvilo.domain.model.StockLot
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculator
import com.example.nuvilo.domain.stock.StockLedgerCalculator
import com.example.nuvilo.security.BiometricAuthManager
import com.example.nuvilo.security.BiometricStatus
import com.example.nuvilo.security.LockGracePeriod
import com.example.nuvilo.system.alarm.AlarmScheduler
import com.example.nuvilo.system.notification.NotificationHelper
import java.io.ByteArrayInputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class DashboardItem(
    val occurrence: DoseOccurrence,
    val medication: Medication,
    val stockBalance: StockBalance?
)

data class DashboardUiState(
    val formattedDate: String = "",
    val greeting: String = "Welcome",
    val items: List<DashboardItem> = emptyList(),
    val heroItem: DashboardItem? = null,
    val remainingTimelineItems: List<DashboardItem> = emptyList(),
    val totalDoses: Int = 0,
    val takenDoses: Int = 0,
    val lowStockCount: Int = 0,
    val expiringPrescriptionsCount: Int = 0,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val stockRepository: StockRepository,
    private val prescriptionRepository: PrescriptionRepository? = null,
    private val alarmScheduler: AlarmScheduler? = null,
    private val scheduleCalculator: ScheduleOccurrenceCalculator = ScheduleOccurrenceCalculator(),
    private val stockCalculator: StockLedgerCalculator = StockLedgerCalculator()
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val today = LocalDate.now(zoneId)

    val uiState: StateFlow<DashboardUiState> = combine(
        medicationRepository.observeActiveMedications(),
        medicationRepository.observeAllSchedules(),
        doseRepository.observeDosesInRange(
            startEpochMs = today.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endEpochMs = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        ),
        stockRepository.observeAllMovements(),
        prescriptionRepository?.observeAllPrescriptions() ?: flowOf(emptyList())
    ) { medications, schedules, recordedDoses, movements, prescriptions ->
        val medMap = medications.associateBy { it.id }
        val schedulesByMed = schedules.groupBy { it.medicationId }
        val movementsByMed = movements.groupBy { it.medicationId }

        val allPlanned = mutableListOf<DoseOccurrence>()
        for (med in medications) {
            val medSchedules = schedulesByMed[med.id].orEmpty()
            for (sch in medSchedules) {
                val occs = scheduleCalculator.generateOccurrencesForDate(
                    schedule = sch,
                    date = today,
                    zoneId = zoneId,
                    unit = med.stockUnit
                )
                allPlanned.addAll(occs)
            }
        }

        val reconciled = scheduleCalculator.reconcileWithRecordedEvents(
            plannedOccurrences = allPlanned,
            recordedEvents = recordedDoses
        )

        var lowStockCount = 0
        val items = reconciled.mapNotNull { occ ->
            val med = medMap[occ.medicationId] ?: return@mapNotNull null
            val medMovements = movementsByMed[med.id].orEmpty()
            val hasOpening = medMovements.any { it.movementType == MovementType.OPENING_BALANCE }
            val baseOpening = if (hasOpening) ScaledQuantity.ZERO else med.openingStock
            val balance = stockCalculator.calculateBalance(
                openingStock = baseOpening,
                openingUnit = med.stockUnit,
                movements = medMovements,
                lowStockThreshold = med.lowStockThreshold,
                isEstimatedConsumption = med.form.isEstimatedConsumption
            )
            if (balance.isLowStock) {
                lowStockCount++
            }
            DashboardItem(
                occurrence = occ,
                medication = med,
                stockBalance = balance
            )
        }.sortedBy { it.occurrence.scheduledEpochMs }

        val todayEpochDay = today.toEpochDay()
        val expiringRxCount = prescriptions.count { rx ->
            rx.effectiveStatus(todayEpochDay) == PrescriptionStatus.ACTIVE &&
                    (rx.isExpiringSoon(todayEpochDay) || rx.isExpired(todayEpochDay) || rx.repeatsRemaining <= 1)
        }

        val heroItem = items.firstOrNull { it.occurrence.status == DoseStatus.PENDING }
        val remainingTimeline = if (heroItem != null) {
            items.filter { it.occurrence.occurrenceId != heroItem.occurrence.occurrenceId }
        } else {
            items
        }

        val takenCount = items.count { it.occurrence.status == DoseStatus.TAKEN || it.occurrence.status == DoseStatus.ASSUMED }
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")

        val currentHour = LocalTime.now(zoneId).hour
        val greeting = when (currentHour) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }

        DashboardUiState(
            formattedDate = today.format(dateFormatter),
            greeting = greeting,
            items = items,
            heroItem = heroItem,
            remainingTimelineItems = remainingTimeline,
            totalDoses = items.size,
            takenDoses = takenCount,
            lowStockCount = lowStockCount,
            expiringPrescriptionsCount = expiringRxCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onTakeDose(occurrence: DoseOccurrence) {
        viewModelScope.launch {
            doseRepository.recordDose(
                occurrenceId = occurrence.occurrenceId,
                medicationId = occurrence.medicationId,
                scheduledEpochMs = occurrence.scheduledEpochMs,
                quantity = occurrence.doseQuantity,
                unit = occurrence.unit,
                status = DoseStatus.TAKEN
            )
        }
    }

    fun onSkipDose(occurrence: DoseOccurrence) {
        viewModelScope.launch {
            doseRepository.recordDose(
                occurrenceId = occurrence.occurrenceId,
                medicationId = occurrence.medicationId,
                scheduledEpochMs = occurrence.scheduledEpochMs,
                quantity = occurrence.doseQuantity,
                unit = occurrence.unit,
                status = DoseStatus.SKIPPED
            )
        }
    }

    fun onSnoozeDose(occurrence: DoseOccurrence, snoozeMinutes: Int = 15) {
        viewModelScope.launch {
            val med = medicationRepository.getMedication(occurrence.medicationId)
            val recMode = med?.recordingMode ?: RecordingMode.CONFIRM
            alarmScheduler?.snoozeOccurrence(
                occurrenceId = occurrence.occurrenceId,
                medicationId = occurrence.medicationId,
                doseQuantity = occurrence.doseQuantity,
                unit = occurrence.unit,
                recordingMode = recMode,
                snoozeMinutes = snoozeMinutes
            )
        }
    }
}

data class MedicationListItem(
    val medication: Medication,
    val stockBalance: StockBalance?
)

data class MedicationListUiState(
    val activeMedications: List<MedicationListItem> = emptyList(),
    val archivedMedications: List<MedicationListItem> = emptyList(),
    val isLoading: Boolean = true
)

class MedicationViewModel(
    private val medicationRepository: MedicationRepository,
    private val stockRepository: StockRepository,
    private val alarmScheduler: AlarmScheduler? = null,
    private val stockCalculator: StockLedgerCalculator = StockLedgerCalculator()
) : ViewModel() {

    val uiState: StateFlow<MedicationListUiState> = combine(
        medicationRepository.observeAllMedications(),
        stockRepository.observeAllMovements()
    ) { allMeds, movements ->
        val movementsByMed = movements.groupBy { it.medicationId }

        fun buildItem(med: Medication): MedicationListItem {
            val medMovements = movementsByMed[med.id].orEmpty()
            val hasOpening = medMovements.any { it.movementType == MovementType.OPENING_BALANCE }
            val baseOpening = if (hasOpening) ScaledQuantity.ZERO else med.openingStock
            val balance = stockCalculator.calculateBalance(
                openingStock = baseOpening,
                openingUnit = med.stockUnit,
                movements = medMovements,
                lowStockThreshold = med.lowStockThreshold,
                isEstimatedConsumption = med.form.isEstimatedConsumption
            )
            return MedicationListItem(medication = med, stockBalance = balance)
        }

        val active = allMeds.filter { !it.isArchived }.map { buildItem(it) }
        val archived = allMeds.filter { it.isArchived }.map { buildItem(it) }

        MedicationListUiState(
            activeMedications = active,
            archivedMedications = archived,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MedicationListUiState(isLoading = true)
    )

    fun createMedication(
        name: String,
        strength: String,
        form: MedicationForm,
        unit: StockUnit,
        openingStockStr: String,
        lowStockThresholdStr: String,
        recordingMode: RecordingMode,
        targetTimesMinuteOfDay: List<Int>,
        daysOfWeek: Set<Int>,
        doseQuantityStr: String
    ) {
        viewModelScope.launch {
            val medId = UUID.randomUUID().toString()
            val opening = ScaledQuantity.fromDisplayString(openingStockStr)
            val threshold = ScaledQuantity.fromDisplayString(lowStockThresholdStr)
            val doseQuantity = ScaledQuantity.fromDisplayString(doseQuantityStr)

            val medication = Medication(
                id = medId,
                name = name,
                strength = strength,
                form = form,
                stockUnit = unit,
                openingStock = opening,
                lowStockThreshold = threshold,
                recordingMode = recordingMode
            )

            val schedule = ScheduleDefinition(
                id = UUID.randomUUID().toString(),
                medicationId = medId,
                scheduleType = if (daysOfWeek.isEmpty()) ScheduleType.DAILY else ScheduleType.SPECIFIC_WEEKDAYS,
                targetTimesMinuteOfDay = targetTimesMinuteOfDay,
                daysOfWeek = daysOfWeek,
                doseQuantityPerOccurrence = doseQuantity,
                startDateEpochDay = LocalDate.now().toEpochDay()
            )

            medicationRepository.createMedication(medication, schedule)
            alarmScheduler?.reconcileUpcomingReminders()
        }
    }

    fun setArchived(medicationId: String, archived: Boolean) {
        viewModelScope.launch {
            medicationRepository.setArchived(medicationId, archived)
            alarmScheduler?.reconcileUpcomingReminders()
        }
    }
}

data class MedicationDetailUiState(
    val medication: Medication? = null,
    val schedules: List<ScheduleDefinition> = emptyList(),
    val stockBalance: StockBalance? = null,
    val lots: List<StockLot> = emptyList(),
    val recentDoses: List<DoseOccurrence> = emptyList(),
    val recentMovements: List<StockLedgerEntry> = emptyList(),
    val prescriptions: List<Prescription> = emptyList(),
    val prescriptionEvents: List<PrescriptionEvent> = emptyList(),
    val fullDoseHistory: List<ResolvedDoseOccurrence> = emptyList(),
    val historyDateFilter: HistoryDateFilter = HistoryDateFilter.ALL_TIME,
    val historyOutcomeFilter: DoseOutcomeFilter = DoseOutcomeFilter.ALL,
    val medicationAdherenceSummary: AdherenceSummary? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

private data class CoreMedData(
    val med: Medication?,
    val allSchedules: List<ScheduleDefinition>,
    val lots: List<StockLot>,
    val movements: List<StockLedgerEntry>,
    val doses: List<DoseOccurrence>
)

private data class ExtraMedData(
    val prescriptions: List<Prescription>,
    val prescriptionEvents: List<PrescriptionEvent>,
    val dateFilter: HistoryDateFilter,
    val outcomeFilter: DoseOutcomeFilter
)

class MedicationDetailViewModel(
    val medicationId: String,
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val stockRepository: StockRepository,
    private val prescriptionRepository: PrescriptionRepository? = null,
    private val alarmScheduler: AlarmScheduler? = null,
    private val stockCalculator: StockLedgerCalculator = StockLedgerCalculator(),
    private val adherenceCalculator: AdherenceCalculator = AdherenceCalculator()
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _actionSuccessMessage = MutableStateFlow<String?>(null)
    private val _historyDateFilter = MutableStateFlow(HistoryDateFilter.ALL_TIME)
    private val _historyOutcomeFilter = MutableStateFlow(DoseOutcomeFilter.ALL)

    private val coreDataFlow = combine(
        medicationRepository.observeMedication(medicationId),
        medicationRepository.observeAllSchedules(),
        stockRepository.observeLotsForMedication(medicationId),
        stockRepository.observeMovementsForMedication(medicationId),
        doseRepository.observeDosesForMedication(medicationId)
    ) { med, allSchedules, lots, movements, doses ->
        CoreMedData(med, allSchedules, lots, movements, doses)
    }

    private val extraDataFlow = combine(
        prescriptionRepository?.observePrescriptionsForMedication(medicationId)
            ?: MutableStateFlow(emptyList()),
        prescriptionRepository?.observePrescriptionEventsForMedication(medicationId)
            ?: MutableStateFlow(emptyList()),
        _historyDateFilter,
        _historyOutcomeFilter
    ) { prescriptions, prescriptionEvents, dateFilter, outcomeFilter ->
        ExtraMedData(prescriptions, prescriptionEvents, dateFilter, outcomeFilter)
    }

    private val dataFlow = combine(
        coreDataFlow,
        extraDataFlow
    ) { core, extra ->
        val med = core.med
        if (med == null) {
            MedicationDetailUiState(
                isLoading = false,
                errorMessage = "Medication not found"
            )
        } else {
            val medSchedules = core.allSchedules.filter { it.medicationId == medicationId }

            var dailyConsumptionRaw = 0L
            for (sch in medSchedules) {
                if (sch.scheduleType == ScheduleType.DAILY && sch.targetTimesMinuteOfDay.isNotEmpty()) {
                    dailyConsumptionRaw += sch.doseQuantityPerOccurrence.rawScaledValue * sch.targetTimesMinuteOfDay.size
                }
            }
            val dailyRate = if (dailyConsumptionRaw > 0L) ScaledQuantity(dailyConsumptionRaw) else null

            val hasOpening = core.movements.any { it.movementType == MovementType.OPENING_BALANCE }
            val baseOpening = if (hasOpening) ScaledQuantity.ZERO else med.openingStock

            val balance = stockCalculator.calculateBalance(
                openingStock = baseOpening,
                openingUnit = med.stockUnit,
                movements = core.movements,
                lowStockThreshold = med.lowStockThreshold,
                dailyConsumptionRate = dailyRate,
                isEstimatedConsumption = med.form.isEstimatedConsumption
            )

            val nowMs = System.currentTimeMillis()
            val today = LocalDate.now(zoneId)
            val fullHistory = adherenceCalculator.buildMedicationDoseHistory(
                medication = med,
                schedules = medSchedules,
                recordedEvents = core.doses,
                dateFilter = extra.dateFilter,
                outcomeFilter = extra.outcomeFilter,
                zoneId = zoneId,
                evaluationEpochMs = nowMs
            )

            val adherenceSummary = adherenceCalculator.calculateSummary(
                periodLabel = "Last 30 Days",
                medications = listOf(med),
                schedules = medSchedules,
                recordedEvents = core.doses,
                startDate = today.minusDays(29),
                endDate = today,
                zoneId = zoneId,
                evaluationEpochMs = nowMs
            )

            MedicationDetailUiState(
                medication = med,
                schedules = medSchedules,
                stockBalance = balance,
                lots = core.lots,
                recentDoses = core.doses.take(20),
                recentMovements = core.movements.reversed().take(20),
                prescriptions = extra.prescriptions,
                prescriptionEvents = extra.prescriptionEvents,
                fullDoseHistory = fullHistory,
                historyDateFilter = extra.dateFilter,
                historyOutcomeFilter = extra.outcomeFilter,
                medicationAdherenceSummary = adherenceSummary,
                isLoading = false
            )
        }
    }

    val uiState: StateFlow<MedicationDetailUiState> = combine(
        dataFlow,
        _errorMessage,
        _actionSuccessMessage
    ) { data, errorMsg, successMsg ->
        data.copy(
            errorMessage = errorMsg ?: data.errorMessage,
            actionSuccessMessage = successMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MedicationDetailUiState(isLoading = true)
    )

    fun setHistoryDateFilter(filter: HistoryDateFilter) {
        _historyDateFilter.value = filter
    }

    fun setHistoryOutcomeFilter(filter: DoseOutcomeFilter) {
        _historyOutcomeFilter.value = filter
    }

    fun restock(
        quantity: ScaledQuantity,
        lotNumber: String?,
        expiryDateEpochMs: Long?,
        notes: String?
    ) {
        viewModelScope.launch {
            val unit = uiState.value.medication?.stockUnit ?: StockUnit.TABLETS
            val result = stockRepository.restockWithLot(
                medicationId = medicationId,
                quantity = quantity,
                unit = unit,
                lotNumber = lotNumber,
                expiryDateEpochMs = expiryDateEpochMs,
                notes = notes
            )
            if (result.isSuccess) {
                _actionSuccessMessage.value = "Restocked ${quantity.toDisplayString()} ${unit.abbreviation}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to restock"
            }
        }
    }

    fun adjustStock(
        targetBalance: ScaledQuantity,
        reason: String,
        targetLotId: String? = null
    ) {
        viewModelScope.launch {
            val current = uiState.value.stockBalance?.currentBalance ?: ScaledQuantity.ZERO
            val unit = uiState.value.medication?.stockUnit ?: StockUnit.TABLETS
            val result = stockRepository.adjustStockToTarget(
                medicationId = medicationId,
                currentBalance = current,
                targetBalance = targetBalance,
                unit = unit,
                reason = reason,
                targetLotId = targetLotId
            )
            if (result.isSuccess) {
                _actionSuccessMessage.value = "Stock adjusted to ${targetBalance.toDisplayString()} ${unit.abbreviation}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to adjust stock"
            }
        }
    }

    fun correctDose(
        occurrenceId: String,
        targetStatus: DoseStatus,
        reason: String,
        fallbackOccurrence: DoseOccurrence? = null
    ) {
        viewModelScope.launch {
            val existingRecorded = uiState.value.recentDoses.any { it.occurrenceId == occurrenceId }
            val result = if (existingRecorded || fallbackOccurrence == null) {
                doseRepository.correctDose(
                    occurrenceId = occurrenceId,
                    targetStatus = targetStatus,
                    reason = reason
                )
            } else {
                // First record the unrecorded past occurrence, then apply correction metadata if needed
                doseRepository.recordDose(
                    occurrenceId = fallbackOccurrence.occurrenceId,
                    medicationId = fallbackOccurrence.medicationId,
                    scheduledEpochMs = fallbackOccurrence.scheduledEpochMs,
                    quantity = fallbackOccurrence.doseQuantity,
                    unit = fallbackOccurrence.unit,
                    status = targetStatus,
                    notes = reason
                )
            }
            if (result.isSuccess) {
                _actionSuccessMessage.value = "Dose updated to ${targetStatus.displayName}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update dose"
            }
        }
    }

    fun savePrescription(
        existingPrescription: Prescription? = null,
        referenceNumber: String?,
        issueDateEpochDay: Long,
        expiryDateEpochDay: Long?,
        quantityPrescribed: ScaledQuantity?,
        repeatsAuthorised: Int,
        repeatsRemaining: Int,
        pharmacyName: String?,
        notes: String?,
        status: PrescriptionStatus = PrescriptionStatus.ACTIVE
    ) {
        viewModelScope.launch {
            val repo = prescriptionRepository ?: return@launch
            val unit = uiState.value.medication?.stockUnit
            val rx = Prescription(
                id = existingPrescription?.id ?: UUID.randomUUID().toString(),
                medicationId = medicationId,
                referenceNumber = referenceNumber?.trim()?.takeIf { it.isNotEmpty() },
                issueDateEpochDay = issueDateEpochDay,
                expiryDateEpochDay = expiryDateEpochDay,
                quantityPrescribed = quantityPrescribed,
                unit = unit,
                repeatsAuthorised = repeatsAuthorised.coerceAtLeast(0),
                repeatsRemaining = repeatsRemaining.coerceIn(0, repeatsAuthorised.coerceAtLeast(0)),
                hasPendingRepeatRequest = existingPrescription?.hasPendingRepeatRequest ?: false,
                pharmacyName = pharmacyName?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                status = status,
                createdAtEpochMs = existingPrescription?.createdAtEpochMs ?: System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            val result = if (existingPrescription == null) {
                repo.createPrescription(rx)
            } else {
                repo.updatePrescription(rx)
            }
            if (result.isSuccess) {
                _actionSuccessMessage.value = if (existingPrescription == null) "Prescription added" else "Prescription updated"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save prescription"
            }
        }
    }

    fun requestPrescriptionRepeat(prescriptionId: String, notes: String? = null) {
        viewModelScope.launch {
            val repo = prescriptionRepository ?: return@launch
            val todayEpochDay = LocalDate.now(zoneId).toEpochDay()
            val result = repo.requestPrescriptionRepeat(
                prescriptionId = prescriptionId,
                currentDateEpochDay = todayEpochDay,
                notes = notes
            )
            if (result.isSuccess) {
                _actionSuccessMessage.value = "Repeat request recorded (repeats remaining unchanged until collected)"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to record repeat request"
            }
        }
    }

    fun collectPrescriptionRepeat(prescriptionId: String, notes: String? = null) {
        viewModelScope.launch {
            val repo = prescriptionRepository ?: return@launch
            val todayEpochDay = LocalDate.now(zoneId).toEpochDay()
            val result = repo.collectPrescriptionRepeat(
                prescriptionId = prescriptionId,
                currentDateEpochDay = todayEpochDay,
                notes = notes
            )
            if (result.isSuccess) {
                _actionSuccessMessage.value = "Repeat recorded as collected"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to collect repeat"
            }
        }
    }

    fun archivePrescription(prescriptionId: String) {
        viewModelScope.launch {
            val repo = prescriptionRepository ?: return@launch
            val result = repo.archivePrescription(prescriptionId)
            if (result.isSuccess) {
                _actionSuccessMessage.value = "Prescription archived"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to archive prescription"
            }
        }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch {
            medicationRepository.setArchived(medicationId, archived)
            alarmScheduler?.reconcileUpcomingReminders()
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _actionSuccessMessage.value = null
    }
}

data class AdherenceCalendarUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedPeriodDays: Int = 30,
    val todaySummary: AdherenceSummary? = null,
    val weeklySummary: AdherenceSummary? = null,
    val monthlySummary: AdherenceSummary? = null,
    val selectedPeriodSummary: AdherenceSummary? = null,
    val calendarData: MonthlyCalendarData? = null,
    val selectedDayRecord: DailyAdherenceRecord? = null,
    val hasAnyScheduleOrHistory: Boolean = false,
    val isLoading: Boolean = true
)

class AdherenceCalendarViewModel(
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val adherenceCalculator: AdherenceCalculator = AdherenceCalculator()
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val _selectedYearMonth = MutableStateFlow(YearMonth.now(zoneId))
    private val _selectedDate = MutableStateFlow(LocalDate.now(zoneId))
    private val _selectedPeriodDays = MutableStateFlow(30)

    val uiState: StateFlow<AdherenceCalendarUiState> = combine(
        medicationRepository.observeAllMedications(),
        medicationRepository.observeAllSchedules(),
        doseRepository.observeAllDoses(),
        _selectedYearMonth,
        combine(_selectedDate, _selectedPeriodDays) { date, periodDays -> date to periodDays }
    ) { medications, schedules, recordedDoses, yearMonth, (selectedDate, periodDays) ->
        val activeOrHistoricalMeds = medications.filter { !it.isArchived || recordedDoses.any { d -> d.medicationId == it.id } }
        val nowMs = System.currentTimeMillis()
        val today = LocalDate.now(zoneId)

        val hasAny = schedules.isNotEmpty() || recordedDoses.isNotEmpty()

        val todaySummary = adherenceCalculator.calculateSummary(
            periodLabel = "Today",
            medications = activeOrHistoricalMeds,
            schedules = schedules,
            recordedEvents = recordedDoses,
            startDate = today,
            endDate = today,
            zoneId = zoneId,
            evaluationEpochMs = nowMs
        )

        val weeklySummary = adherenceCalculator.calculateSummary(
            periodLabel = "Last 7 Days",
            medications = activeOrHistoricalMeds,
            schedules = schedules,
            recordedEvents = recordedDoses,
            startDate = today.minusDays(6),
            endDate = today,
            zoneId = zoneId,
            evaluationEpochMs = nowMs
        )

        val selectedPeriodSummary = adherenceCalculator.calculateSummary(
            periodLabel = "Last $periodDays Days",
            medications = activeOrHistoricalMeds,
            schedules = schedules,
            recordedEvents = recordedDoses,
            startDate = today.minusDays((periodDays - 1).toLong()),
            endDate = today,
            zoneId = zoneId,
            evaluationEpochMs = nowMs
        )

        val calendarData = adherenceCalculator.buildMonthlyCalendar(
            yearMonth = yearMonth,
            medications = activeOrHistoricalMeds,
            schedules = schedules,
            recordedEvents = recordedDoses,
            zoneId = zoneId,
            evaluationEpochMs = nowMs
        )

        val effectiveDate = if (YearMonth.from(selectedDate) == yearMonth) {
            selectedDate
        } else if (yearMonth == YearMonth.from(today)) {
            today
        } else {
            yearMonth.atDay(1)
        }

        val dayRecord = calendarData.daysByDate[effectiveDate]

        AdherenceCalendarUiState(
            selectedYearMonth = yearMonth,
            selectedDate = effectiveDate,
            selectedPeriodDays = periodDays,
            todaySummary = todaySummary,
            weeklySummary = weeklySummary,
            monthlySummary = calendarData.monthSummary,
            selectedPeriodSummary = selectedPeriodSummary,
            calendarData = calendarData,
            selectedDayRecord = dayRecord,
            hasAnyScheduleOrHistory = hasAny,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdherenceCalendarUiState(isLoading = true)
    )

    fun onPreviousMonth() {
        val prev = _selectedYearMonth.value.minusMonths(1)
        _selectedYearMonth.value = prev
        _selectedDate.value = prev.atDay(1)
    }

    fun onNextMonth() {
        val next = _selectedYearMonth.value.plusMonths(1)
        _selectedYearMonth.value = next
        val today = LocalDate.now(zoneId)
        _selectedDate.value = if (next == YearMonth.from(today)) today else next.atDay(1)
    }

    fun onJumpToToday() {
        val today = LocalDate.now(zoneId)
        _selectedYearMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun onSelectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _selectedYearMonth.value) {
            _selectedYearMonth.value = YearMonth.from(date)
        }
    }

    fun onSelectPeriodDays(days: Int) {
        _selectedPeriodDays.value = days
    }

    fun onTakeDose(occurrence: DoseOccurrence) {
        viewModelScope.launch {
            doseRepository.recordDose(
                occurrenceId = occurrence.occurrenceId,
                medicationId = occurrence.medicationId,
                scheduledEpochMs = occurrence.scheduledEpochMs,
                quantity = occurrence.doseQuantity,
                unit = occurrence.unit,
                status = DoseStatus.TAKEN
            )
        }
    }

    fun onSkipDose(occurrence: DoseOccurrence) {
        viewModelScope.launch {
            doseRepository.recordDose(
                occurrenceId = occurrence.occurrenceId,
                medicationId = occurrence.medicationId,
                scheduledEpochMs = occurrence.scheduledEpochMs,
                quantity = occurrence.doseQuantity,
                unit = occurrence.unit,
                status = DoseStatus.SKIPPED
            )
        }
    }
}

sealed interface ExportState {
    object Idle : ExportState
    object InProgress : ExportState
    data class Success(val message: String) : ExportState
    data class Error(val message: String) : ExportState
}

sealed interface RestoreState {
    object Idle : RestoreState
    object Validating : RestoreState
    data class NeedsPassphrase(val envelopeJson: String) : RestoreState
    data class Preview(val backup: NoveloDataBackup, val preview: BackupPreview) : RestoreState
    object Restoring : RestoreState
    data class Success(val message: String) : RestoreState
    data class Error(val message: String) : RestoreState
}

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationHelper: NotificationHelper? = null,
    private val alarmScheduler: AlarmScheduler? = null,
    private val biometricAuthManager: BiometricAuthManager? = null,
    private val appLockManager: com.example.nuvilo.security.AppLockManager? = null,
    private val backupRestoreRepository: BackupRestoreRepository? = null
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

    fun canScheduleExactAlarms(): Boolean = alarmScheduler?.canScheduleExactAlarms() ?: true
    fun areNotificationsEnabled(): Boolean = notificationHelper?.areNotificationsEnabled() ?: true
    fun getBiometricStatus(): BiometricStatus = biometricAuthManager?.checkBiometricStatus() ?: BiometricStatus.UNAVAILABLE

    fun setSystemDialogActive(active: Boolean) {
        appLockManager?.setSystemDialogActive(active)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setNotificationPrivacy(masked: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationPrivacy(masked)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricLock(enabled)
        }
    }

    fun setLockGracePeriod(gracePeriod: LockGracePeriod) {
        viewModelScope.launch {
            preferencesRepository.setLockGracePeriod(gracePeriod)
        }
    }

    fun setRecentsPreviewMasked(masked: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRecentsPreviewMasked(masked)
        }
    }

    fun triggerTestNotification() {
        notificationHelper?.showTestNotification()
    }

    fun reconcileReminders() {
        viewModelScope.launch {
            alarmScheduler?.reconcileUpcomingReminders()
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun resetRestoreState() {
        _restoreState.value = RestoreState.Idle
    }

    fun exportEncryptedJson(passphrase: CharArray, outputStream: OutputStream) {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress
            try {
                val repo = backupRestoreRepository ?: throw IllegalStateException("Backup repository unavailable")
                val backup = repo.createBackup()
                val json = JsonBackupManager.serializeBackup(backup)
                val encryptedJson = BackupCrypto.encryptJson(json, passphrase)
                outputStream.use { out ->
                    out.write(encryptedJson.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                val now = System.currentTimeMillis()
                preferencesRepository.setLastExportEpochMs(now)
                _exportState.value = ExportState.Success("Encrypted backup exported (${backup.medications.size} medications)")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun exportCsvZip(outputStream: OutputStream) {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress
            try {
                val repo = backupRestoreRepository ?: throw IllegalStateException("Backup repository unavailable")
                val backup = repo.createBackup()
                CsvZipBackupManager.exportToZip(backup, outputStream)
                val now = System.currentTimeMillis()
                preferencesRepository.setLastExportEpochMs(now)
                _exportState.value = ExportState.Success("CSV bundle exported (${backup.medications.size} medications)")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun inspectImportFile(bytes: ByteArray, isZipHint: Boolean = false) {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Validating
            try {
                val repo = backupRestoreRepository ?: throw IllegalStateException("Backup repository unavailable")
                val isZip = isZipHint || (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte())
                if (isZip) {
                    val backup = CsvZipBackupManager.importFromZip(bytes)
                    when (val result = repo.validateBackup(backup)) {
                        is BackupValidationResult.Valid -> _restoreState.value = RestoreState.Preview(result.backup, result.preview)
                        is BackupValidationResult.Invalid -> _restoreState.value = RestoreState.Error("Invalid CSV backup: ${result.reason}")
                        is BackupValidationResult.Encrypted -> _restoreState.value = RestoreState.NeedsPassphrase(result.envelopeJson)
                    }
                } else {
                    val content = String(bytes, Charsets.UTF_8)
                    if (BackupCrypto.isEncryptedEnvelope(content)) {
                        _restoreState.value = RestoreState.NeedsPassphrase(content)
                    } else {
                        val backup = JsonBackupManager.deserializeBackup(content)
                        when (val result = repo.validateBackup(backup)) {
                            is BackupValidationResult.Valid -> _restoreState.value = RestoreState.Preview(result.backup, result.preview)
                            is BackupValidationResult.Invalid -> _restoreState.value = RestoreState.Error("Invalid JSON backup: ${result.reason}")
                            is BackupValidationResult.Encrypted -> _restoreState.value = RestoreState.NeedsPassphrase(result.envelopeJson)
                        }
                    }
                }
            } catch (e: Exception) {
                _restoreState.value = RestoreState.Error(e.message ?: "Failed to inspect file")
            }
        }
    }

    fun decryptAndInspectJson(envelopeJson: String, passphrase: CharArray) {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Validating
            try {
                val repo = backupRestoreRepository ?: throw IllegalStateException("Backup repository unavailable")
                val decrypted = BackupCrypto.decryptJson(envelopeJson, passphrase)
                val backup = JsonBackupManager.deserializeBackup(decrypted)
                when (val result = repo.validateBackup(backup)) {
                    is BackupValidationResult.Valid -> _restoreState.value = RestoreState.Preview(result.backup, result.preview)
                    is BackupValidationResult.Invalid -> _restoreState.value = RestoreState.Error("Invalid backup data: ${result.reason}")
                    is BackupValidationResult.Encrypted -> _restoreState.value = RestoreState.NeedsPassphrase(result.envelopeJson)
                }
            } catch (e: com.example.nuvilo.data.backup.InvalidPassphraseOrCorruptedDataException) {
                _restoreState.value = RestoreState.Error("Incorrect passphrase or corrupted backup")
            } catch (e: Exception) {
                _restoreState.value = RestoreState.Error(e.message ?: "Decryption failed")
            }
        }
    }

    fun confirmRestore(backup: NoveloDataBackup) {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Restoring
            try {
                val repo = backupRestoreRepository ?: throw IllegalStateException("Backup repository unavailable")
                val result = repo.executeRestore(backup)
                if (result.isSuccess) {
                    alarmScheduler?.reconcileUpcomingReminders()
                    _restoreState.value = RestoreState.Success("Database restored successfully (${backup.medications.size} medications)")
                } else {
                    _restoreState.value = RestoreState.Error(result.exceptionOrNull()?.message ?: "Restore failed")
                }
            } catch (e: Exception) {
                _restoreState.value = RestoreState.Error(e.message ?: "Restore failed")
            }
        }
    }
}

class ViewModelFactory(
    private val appContainer: com.example.nuvilo.AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    medicationRepository = appContainer.repository,
                    doseRepository = appContainer.repository,
                    stockRepository = appContainer.repository,
                    prescriptionRepository = appContainer.repository,
                    alarmScheduler = appContainer.alarmScheduler,
                    scheduleCalculator = appContainer.scheduleCalculator,
                    stockCalculator = appContainer.stockCalculator
                ) as T
            }
            modelClass.isAssignableFrom(MedicationViewModel::class.java) -> {
                MedicationViewModel(
                    medicationRepository = appContainer.repository,
                    stockRepository = appContainer.repository,
                    alarmScheduler = appContainer.alarmScheduler,
                    stockCalculator = appContainer.stockCalculator
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    preferencesRepository = appContainer.userPreferencesRepository,
                    notificationHelper = appContainer.notificationHelper,
                    alarmScheduler = appContainer.alarmScheduler,
                    biometricAuthManager = appContainer.biometricAuthManager,
                    appLockManager = appContainer.appLockManager,
                    backupRestoreRepository = appContainer.backupRestoreRepository
                ) as T
            }
            modelClass.isAssignableFrom(AdherenceCalendarViewModel::class.java) -> {
                AdherenceCalendarViewModel(
                    medicationRepository = appContainer.repository,
                    doseRepository = appContainer.repository,
                    adherenceCalculator = appContainer.adherenceCalculator
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    fun createMedicationDetailViewModel(medicationId: String): MedicationDetailViewModel {
        return MedicationDetailViewModel(
            medicationId = medicationId,
            medicationRepository = appContainer.repository,
            doseRepository = appContainer.repository,
            stockRepository = appContainer.repository,
            prescriptionRepository = appContainer.repository,
            alarmScheduler = appContainer.alarmScheduler,
            stockCalculator = appContainer.stockCalculator,
            adherenceCalculator = appContainer.adherenceCalculator
        )
    }
}
