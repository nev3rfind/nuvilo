# Novelo: Pre-Merge Test Case Catalog (T001 – T200)

**Product:** Novelo – Medication, made manageable.  
**Repository:** https://github.com/nev3rfind/nuvilo  
**Target Branch:** `test/8-pre-merge-regression-200`  
**Total Cataloged Regression Cases:** 200 new automated test cases across 7 categories.

---

## Category A: Medication Domain & Ledger Mathematics (T001 – T055)

### Sub-Suite A.1: ScaledQuantity Exact Integer Arithmetic (`ScaledQuantityRegressionTest.kt`)
- **T001**: `t001_exactWholeUnitRepresentation` – Verifies that whole units represent exactly with raw scale factor 1,000.
- **T002**: `t002_fractionalUnitRepresentation` – Validates fractional values (`0.5`, `0.25`, `0.75`) retain exact precision.
- **T003**: `t003_additionMaintainsPrecision` – Verifies addition of fractional and whole quantities produces exact sums.
- **T004**: `t004_subtractionMaintainsPrecision` – Verifies subtraction without rounding distortion.
- **T005**: `t005_subtractionCanProduceNegative` – Validates negative balance detection.
- **T006**: `t006_multiplicationByInteger` – Verifies multiplying quantity by integer frequency produces exact totals.
- **T007**: `t007_displayStringFormattingWhole` – Verifies clean string formatting without decimal points for whole numbers.
- **T008**: `t008_displayStringFormattingFractional` – Validates string formatting for decimal fractions.
- **T009**: `t009_parseDisplayStringRoundTrip` – Validates string-to-ScaledQuantity parsing round-trip fidelity.
- **T010**: `t010_comparisonAndSignProperties` – Validates `isPositive()`, `isZero()`, `isNegative()`, and comparable ordering.

### Sub-Suite A.2: Stock Ledger Accounting & Deductions (`StockLedgerRegressionTest.kt`)
- **T011**: `t011_initialOpeningBalanceCalculation` – Validates ledger calculation from initial opening balance entry.
- **T012**: `t012_restockIncreasesBalance` – Verifies `RESTOCK` movement debits inventory ledger correctly.
- **T013**: `t013_doseTakenDecreasesBalance` – Confirms explicit `DOSE_TAKEN` deducts exact scheduled dose amount.
- **T014**: `t014_doseAssumedDecreasesBalance` – Verifies `DOSE_ASSUMED` adherence logs deduct exact scheduled quantity.
- **T015**: `t015_doseSkippedDoesNotDeductStock` – Validates that `SKIPPED` status generates zero ledger deductions.
- **T016**: `t016_doseReversalRestoresStock` – Validates compensating `DOSE_REVERSAL` restores deducted balance idempotently.
- **T017**: `t017_adjustmentInIncreasesBalance` – Verifies `ADJUSTMENT_IN` recount movements add stock.
- **T018**: `t018_adjustmentOutDecreasesBalance` – Verifies `ADJUSTMENT_OUT` recount movements subtract stock.
- **T019**: `t019_wastageDecreasesBalance` – Confirms `WASTAGE` movement deducts stock and records clinical reason.
- **T020**: `t020_lowStockThresholdDetection` – Verifies `isLowStock` boolean trigger when balance drops below threshold.
- **T021**: `t021_daysRemainingRunwayCalculation` – Verifies estimated days of supply remaining given daily dosing schedule.
- **T022**: `t022_incompatibleUnitsRejected` – Ensures arithmetic across differing `StockUnit`s throws `IncompatibleUnitsException`.

### Sub-Suite A.3: FEFO Batch Lot Allocation (`FefoLotAllocationRegressionTest.kt`)
- **T023**: `t023_singleLotFullDeduction` – Verifies deducting from single active batch lot.
- **T024**: `t024_fefoAllocationPrioritizesEarlierExpiry` – Validates earlier expiry batch is exhausted before newer batch.
- **T025**: `t025_multiLotSpanningDeduction` – Confirms single dose spanning two batch lots splits deduction accurately.
- **T026**: `t026_nonDatedLotsFallbackToFifo` – Validates fallback to creation order when expiry dates are absent.
- **T027**: `t027_fefoAllocationPreservesZeroBalanceLots` – Verifies exhausted lots remain in history with remaining balance 0.
- **T028**: `t028_fefoOverAllocationHandledGracefully` – Confirms allocating past available lot balances is handled cleanly.
- **T029**: `t029_lotAllocationReversalRestoresSpecificLot` – Validates dose correction restores quantity to the originating lot.
- **T030**: `t030_activeLotsExcludesDepletedLots` – Verifies active lot queries filter out fully consumed batches.

### Sub-Suite A.4: Schedule Occurrence Identity (`ScheduleOccurrenceRegressionTest.kt`)
- **T031**: `t031_dailyScheduleOccurrenceGeneration` – Verifies occurrence generation for daily dosing regimens.
- **T032**: `t032_specificWeekdaysScheduleOccurrenceGeneration` – Validates occurrences generate only on designated weekdays.
- **T033**: `t033_deterministicOccurrenceIdFormat` – Confirms occurrence ID format `{medId}_{date}_{minuteOfDay}`.
- **T034**: `t034_occurrenceIdIsUniqueAcrossDays` – Verifies unique identity across consecutive calendar days.
- **T035**: `t035_occurrenceIdIsUniqueAcrossTimesOfDay` – Verifies unique identity for multi-dose daily regimens.
- **T036**: `t036_occurrenceGenerationRespectsScheduleStartDate` – Ensures occurrences are not generated prior to start date.
- **T037**: `t037_occurrenceGenerationRespectsScheduleEndDate` – Ensures occurrences terminate on schedule end date.
- **T038**: `t038_multiScheduleOccurrencesMergedChronologically` – Validates chronological sorting of multiple active schedules.

### Sub-Suite A.5: Adherence Calculation Engine (`AdherenceCalculatorRegressionTest.kt`)
- **T039**: `t039_perfectAdherence100Percent` – Verifies 100% adherence when all due doses are confirmed taken.
- **T040**: `t040_zeroAdherenceWhenAllDosesMissed` – Validates 0% adherence calculation when all doses are missed.
- **T041**: `t041_assumedDosesCountAsAdherent` – Confirms `DOSE_ASSUMED` counts towards adherent numerator.
- **T042**: `t042_skippedDosesNotCountedAsAdherent` – Confirms `SKIPPED` doses are counted in denominator but not numerator.
- **T043**: `t043_futureScheduledDosesExcludedFromAdherence` – Verifies upcoming future doses do not penalize current score.
- **T044**: `t044_emptyDoseHistoryYieldsNullOrZeroAdherence` – Ensures no division-by-zero on empty history.
- **T045**: `t045_weeklyAdherenceSummaryCalculation` – Validates rolling 7-day adherence metric window.
- **T046**: `t046_monthlyCalendarDataAdherenceDayStatus` – Verifies day status categorization (PERFECT, PARTIAL, MISSED).
- **T047**: `t047_adherenceCalculationRespectsTimezoneBoundaries` – Ensures calendar day grouping respects local time boundaries.

### Sub-Suite A.6: Repeat Prescription Management (`PrescriptionManagerRegressionTest.kt`)
- **T048**: `t048_initialPrescriptionStatusActive` – Verifies new prescription with available repeats initializes as ACTIVE.
- **T049**: `t049_requestRepeatSetsPendingFlag` – Validates repeat request transition sets `hasPendingRepeatRequest`.
- **T050**: `t050_duplicateRepeatRequestRejectedWhenPending` – Enforces idempotency guard preventing duplicate repeat requests.
- **T051**: `t051_collectRepeatDecrementsRepeatsRemaining` – Confirms pharmacy collection decrements repeats and clears pending flag.
- **T052**: `t052_collectFinalRepeatTransitionsToCompleted` – Verifies collecting last repeat transitions prescription to COMPLETED.
- **T053**: `t053_collectRepeatWhenZeroRemainingRejected` – Validates guard preventing collection when 0 repeats remain.
- **T054**: `t054_prescriptionExpirationCalculation` – Verifies prescription status transitions to EXPIRED after validity period.
- **T055**: `t055_prescriptionAuditTrailPreservesEvents` – Validates append-only prescription event log.

---

## Category B: Room Persistence, DAOs & Migrations (T056 – T085)

### Sub-Suite B.1: Room DAOs (`RoomDaoRegressionTest.kt`)
- **T056**: `t056_insertMedicationAndRetrieveById` – Verifies entity persistence and retrieval via `MedicationDao`.
- **T057**: `t057_observeActiveMedicationsFiltersArchivedItems` – Confirms active medication query excludes archived rows.
- **T058**: `t058_insertScheduleAndRetrieveByMedicationId` – Verifies schedule persistence linked to parent medication.
- **T059**: `t059_insertDoseEventAndObserveInRange` – Confirms date range queries on `dose_events`.
- **T060**: `t060_insertStockLotWithInitialAndRemainingQuantities` – Verifies batch lot tracking entity operations.
- **T061**: `t061_insertStockLedgerEntryAndVerifyQueryByMedicationId` – Verifies stock movement entries query.
- **T062**: `t062_insertPrescriptionAndRetrieveByMedicationId` – Verifies prescription record persistence.
- **T063**: `t063_insertPrescriptionEventAndRetrieveByPrescriptionId` – Verifies prescription event audit history query.
- **T064**: `t064_updateMedicationChangesNameAndLowStockThreshold` – Validates entity update mutations.
- **T065**: `t065_softDeleteArchivingOfMedicationPreservesHistory` – Confirms soft-delete preserves historical ledger entries.

### Sub-Suite B.2: Database Invariants (`DatabaseInvariantsRegressionTest.kt`)
- **T066**: `t066_primaryKeyCollisionRejection` – Enforces primary key uniqueness in SQLite schema.
- **T067**: `t067_foreignKeyConstraintEnforcementBetweenScheduleAndMedication` – Confirms rejection of orphan child rows.
- **T068**: `t068_doseEventUniqueOccurrenceIdConstraintPreventsDuplicateDoses` – Enforces unique constraint on `occurrence_id`.
- **T069**: `t069_stockLedgerMovementTypesEnumSerialization` – Validates string enum serialization for all 8 movement types.
- **T070**: `t070_scaledQuantityRawLongStorageFidelityWithoutBitLoss` – Confirms 64-bit Long precision storage in Room.
- **T071**: `t071_prescriptionStatusStringConversion` – Verifies `PrescriptionStatus` enum serialization.
- **T072**: `t072_foreignKeyCascadeDeletesSchedulesWithoutLeavingOrphans` – Confirms CASCADE deletion removes dependent schedules.
- **T073**: `t073_transactionalAtomicCommitAcrossMultipleDaos` – Validates transaction rollback on mid-operation exception.

### Sub-Suite B.3: Schema Migrations (`RoomMigrationRegressionTest.kt`)
- **T074**: `t074_migration1To2CreatesStockLotsTable` – Verifies Migration 1->2 creates `stock_lots` table.
- **T075**: `t075_migration2To3CreatesPrescriptionsAndEventsTables` – Verifies Migration 2->3 creates prescriptions tables.
- **T076**: `t076_completeMigrationChainPreservesPreExistingData` – Confirms zero data loss across v1->v2->v3 chain.
- **T077**: `t077_migration1To2AddsCorrectionColumnsToDoseEvents` – Verifies addition of correction audit columns.
- **T078**: `t078_inMemoryDatabaseInitializationCreatesValidSchemaV3` – Confirms in-memory DB creates Schema v3 correctly.

### Sub-Suite B.4: Repository Behavior (`RepositoryBehaviourRegressionTest.kt`)
- **T079**: `t079_createMedicationAtomicallyInsertsMedicationScheduleAndOpeningBalance` – Verifies atomic repository creation.
- **T080**: `t080_recordDoseConfirmedDeductsStockViaLedgerEntry` – Verifies dose confirmation repository pipeline.
- **T081**: `t081_recordDoseSkippedDoesNotDeductStock` – Confirms skipped dose repository handling.
- **T082**: `t082_correctDoseCreatesCompensatingDoseReversal` – Verifies repository dose correction and ledger compensation.
- **T083**: `t083_requestPrescriptionRepeatUpdatesPrescriptionAndEmitsEvent` – Verifies repeat request repository workflow.
- **T084**: `t084_collectPrescriptionRepeatDecrementsRepeatsRemaining` – Confirms collection repository decrement and event logging.
- **T085**: `t085_observeStockBalanceCalculatesLiveBalanceReactively` – Validates reactive StateFlow updates on ledger mutations.

---

## Category C: Security, Privacy & Local Backup (T086 – T110)

### Sub-Suite C.1: Backup Cryptography (`BackupCryptoRegressionTest.kt`)
- **T086**: `t086_encryptAndDecryptRoundTripWithCorrectPassphrase` – Confirms AES-256-GCM + PBKDF2 encryption round-trip.
- **T087**: `t087_decryptWithIncorrectPassphraseFails` – Validates cryptographic failure on wrong passphrase.
- **T088**: `t088_ciphertextTamperingFailsAuthenticationTag` – Confirms authentication tag rejection on modified ciphertext.
- **T089**: `t089_randomSaltAndIvGeneratedPerEncryption` – Ensures distinct salts and IVs generated for identical plaintexts.
- **T090**: `t090_emptyPassphraseRejected` – Confirms validation prevents empty passphrases.
- **T091**: `t091_shortPassphraseUnder8CharactersRejected` – Enforces 8-character minimum passphrase policy.
- **T092**: `t092_pbkdf2IterationCountMeetsSecurityStandard` – Validates iteration count (600,000 iterations).
- **T093**: `t093_largePayloadEncryptionMaintainsIntegrity` – Verifies encryption of multi-megabyte backup payloads.

### Sub-Suite C.2: Backup Serialization (`BackupSerializationRegressionTest.kt`)
- **T094**: `t094_serializeCompleteDatabaseBackupToJson` – Confirms full JSON backup generation across all entities.
- **T095**: `t095_deserializeJsonBackupRestoresAllEntities` – Validates JSON deserialization restores all records.
- **T096**: `t096_backupMetadataIncludesSchemaVersionAndTimestamp` – Verifies metadata headers in backup envelope.
- **T097**: `t097_corruptedJsonPayloadFailsDeserialization` – Validates malformed JSON payloads fail with clean error.
- **T098**: `t098_csvExportContainsHeaderRows` – Verifies CSV export formats contain correct standard column headers.
- **T099**: `t099_csvExportEscapesSpecialCharactersAndCommas` – Confirms RFC 4180 CSV escaping for notes with commas/quotes.
- **T100**: `t100_zipArchiveContainsAllRequiredCsvFiles` – Verifies multi-table CSV zip export bundle contents.
- **T101**: `t101_emptyDatabaseProducesValidEmptyBackupArchive` – Confirms valid backup generation on empty databases.

### Sub-Suite C.3: Backup Validation (`BackupValidationRegressionTest.kt`)
- **T102**: `t102_restoreValidationRejectsFutureSchemaVersions` – Rejects backups with schema version higher than app version.
- **T103**: `t103_restoreValidationRejectsMismatchedEntityCounts` – Validates entity count parity check.
- **T104**: `t104_restoreValidationDetectsOrphanSchedulesBeforeCommit` – Rejects backups with foreign key referential violations.
- **T105**: `t105_restoreValidationDetectsOrphanDoseEventsBeforeCommit` – Rejects backups with orphan dose records.
- **T106**: `t106_restoreValidationDetectsNegativeOpeningStocks` – Rejects backups with invalid negative opening stock values.

### Sub-Suite C.4: Privacy & Manifest Constraints (`PrivacyAndManifestRegressionTest.kt`)
- **T107**: `t107_internetPermissionForbiddenFromManifest` – Enforces absolute zero-network invariant (`android.permission.INTERNET`).
- **T108**: `t108_autoBackupRulesExcludeDatabaseFiles` – Verifies exclusion of `novelo.db` in `backup_rules.xml`.
- **T109**: `t109_dataExtractionRulesExcludeDatabaseFiles` – Verifies exclusion of database in `data_extraction_rules.xml`.
- **T110**: `t110_noThirdPartyAnalyticsOrCrashTrackersPresent` – Verifies absence of Firebase, Crashlytics, and tracking SDKs.

---

## Category D: Scheduling, Notifications & Lifecycle (T111 – T135)

### Sub-Suite D.1: AlarmScheduler (`AlarmSchedulerRegressionTest.kt`)
- **T111**: `t111_scheduleOccurrenceForFutureDoseSucceedsWithoutException` – Verifies scheduling exact alarm for future dose.
- **T112**: `t112_scheduleOccurrenceInPastIsIgnoredSafely` – Validates past doses do not schedule alarms.
- **T113**: `t113_cancelOccurrenceSucceedsWithoutException` – Confirms alarm cancellation by occurrence ID.
- **T114**: `t114_snoozeOccurrenceSchedulesNewAlarm15MinutesLater` – Verifies 15-minute snooze alarm calculation.
- **T115**: `t115_reconcileUpcomingRemindersScansHorizonAndReconciles` – Validates 24-hour horizon alarm reconciliation.
- **T116**: `t116_multipleSchedulesForSameMedicationGenerateDistinctReminders` – Verifies distinct alarm request codes.
- **T117**: `t117_canScheduleExactAlarmsReturnsBoolean` – Validates exact alarm permission check API.

### Sub-Suite D.2: Notifications (`NotificationActionRegressionTest.kt`)
- **T118**: `t118_actionTakeRecordsDoseAsTakenAndDeductsStock` – Verifies Notification Action TAKE executes confirmation.
- **T119**: `t119_actionSnoozePreservesPendingStatusWithoutStockDeduction` – Verifies Notification Action SNOOZE preserves state.
- **T120**: `t120_actionSkipRecordsSkippedStatusWithoutDeductingStock` – Verifies Notification Action SKIP marks dose skipped.
- **T121**: `t121_notificationChannelsAreProperlyRegisteredWithSystem` – Validates notification channels registration.
- **T122**: `t122_notificationTextBuilderBuildsBigTextWithRemainingStock` – Verifies BigText body formatting.
- **T123**: `t123_notificationTextBuilderBuildsSubTextWithConciseStockCount` – Verifies SubText header pill formatting.
- **T124**: `t124_notificationTextBuilderShowsGpReorderRecommendationUnder7Days` – Verifies GP reorder warning for low runway.
- **T125**: `t125_notificationTextBuilderDisplaysPendingRepeatNoteWhenOrderPending` – Verifies pending order notification banner.

### Sub-Suite D.3: System Broadcasts (`SystemEventsRegressionTest.kt`)
- **T126**: `t126_bootCompletedBroadcastIsRegisteredInManifest` – Confirms receiver registration for `ACTION_BOOT_COMPLETED`.
- **T127**: `t127_timeAndZoneChangeBroadcastsAreRegisteredInManifest` – Confirms receiver registration for time/zone changes.
- **T128**: `t128_unrelatedSystemBroadcastsAreSafelyIgnored` – Confirms unknown broadcasts are safely ignored without action.
- **T129**: `t129_packageReplacedBroadcastIsRegisteredInManifest` – Confirms receiver registration for `MY_PACKAGE_REPLACED`.
- **T130**: `t130_bootReceiverIsDeclaredInManifest` – Verifies receiver component is exported and enabled in AndroidManifest.

### Sub-Suite D.4: Background Worker (`BackgroundWorkerRegressionTest.kt`)
- **T131**: `t131_workerExecutionReturnsSuccessOnCompletedRun` – Verifies `LowStockCheckWorker` execution returns Success.
- **T132**: `t132_workerIdentifiesLowStockMedicationAndUpdatesNotifiedSet` – Confirms worker triggers low stock notification.
- **T133**: `t133_workerDeduplicatesAlertsWhenAlreadyNotified` – Enforces alert deduplication preventing repeat notifications.
- **T134**: `t134_restockingAboveThresholdResetsNotifiedState` – Confirms restocking clears notified set for future alerts.
- **T135**: `t135_workerExecutionWithHealthyStockCompletesWithoutNotification` – Verifies zero notifications when stock is healthy.

---

## Category E: ViewModels & Application State (T136 – T155)

### Sub-Suite E.1: ViewModels (`ViewModelRegressionTest.kt`)
- **T136**: `t136_dashboardViewModelLoadsActiveMedicationsAndDoses` – Verifies `DashboardViewModel` loads active schedules.
- **T137**: `t137_dashboardViewModelCalculatesAccurateLowStockCount` – Confirms accurate low-stock warning count in state.
- **T138**: `t138_dashboardViewModelRecordsDoseConfirmed` – Confirms `onTakeDose` updates UI state reactively.
- **T139**: `t139_dashboardViewModelSkipsDoseWithoutDeductingStock` – Confirms `onSkipDose` updates state without stock deduction.
- **T140**: `t140_medicationViewModelLoadsActiveMedicationsList` – Verifies `MedicationViewModel` loads full active inventory.
- **T141**: `t141_medicationViewModelFiltersArchivedMedicationsWhenToggled` – Confirms toggle between active and archived views.
- **T142**: `t142_medicationViewModelCreateMedicationPersistsNewMedication` – Verifies multi-step medication creation via ViewModel.
- **T143**: `t143_medicationViewModelArchiveMedicationMovesMedicationToArchived` – Confirms archiving medication updates state.
- **T144**: `t144_medicationDetailViewModelLoadsBalanceAndRunwayDays` – Verifies detail screen loads stock runway and balance.
- **T145**: `t145_medicationDetailViewModelLoadsLotsAndLedgerMovements` – Confirms detail screen loads batch lots and movements.
- **T146**: `t146_medicationDetailViewModelRestockIncreasesBalanceAndCreatesLot` – Verifies ViewModel restock updates balance and lots.
- **T147**: `t147_medicationDetailViewModelAdjustStockCreatesAdjustmentEntry` – Confirms recount adjustment updates inventory state.
- **T148**: `t148_medicationDetailViewModelRecordWastageDebitsStock` – Confirms wastage action debits inventory state.
- **T149**: `t149_medicationDetailViewModelRequestRepeatMarksPrescriptionPending` – Confirms repeat order updates prescription state.
- **T150**: `t150_medicationDetailViewModelCollectRepeatDecrementsRepeats` – Confirms repeat collection decrements repeat counter.
- **T151**: `t151_adherenceCalendarViewModelInitializesWithCurrentMonth` – Verifies calendar ViewModel initializes on current month.
- **T152**: `t152_adherenceCalendarViewModelNavigatesMonths` – Confirms next/previous month transitions in calendar ViewModel.
- **T153**: `t153_adherenceCalendarViewModelSelectsPeriodDays` – Confirms rolling window period selection (`7D`, `14D`, `30D`).
- **T154**: `t154_settingsViewModelLoadsUserPreferences` – Verifies settings ViewModel loads DataStore preferences.
- **T155**: `t155_settingsViewModelTogglesPrivacyMasking` – Confirms toggling privacy masking setting updates preferences.

---

## Category F: Compose UI, Accessibility & Design Tokens (T156 – T190)

### Sub-Suite F.1: Dashboard UI (`ComposeDashboardRegressionTest.kt`)
- **T156**: `t156_heroDoseCardDisplaysMedicationDetailsAndDosage` – Verifies `HeroDoseCard` displays drug name, strength, dosage.
- **T157**: `t157_heroDoseCardTakeButtonTriggersCallback` – Verifies Take CTA button click triggers callback.
- **T158**: `t158_heroDoseCardSkipButtonTriggersCallback` – Verifies Skip CTA button click triggers callback.
- **T159**: `t159_completedDayCardDisplaysAllDosesTakenConfirmation` – Verifies `CompletedDayCard` displays celebration copy.
- **T160**: `t160_emptyStateCardDisplaysWhenNoDosesAreScheduled` – Verifies empty state card when schedule is empty.

### Sub-Suite F.2: Medication List UI (`ComposeMedicationListRegressionTest.kt`)
- **T161**: `t161_medicationItemCardDisplaysDetailsAndStock` – Verifies medication card displays name, strength, and form.
- **T162**: `t162_medicationItemCardClickInvokesCallback` – Verifies medication card click navigates to detail screen.
- **T163**: `t163_lowStockBadgeDisplayedWhenMedicationIsLowOnStock` – Confirms warning badge displays when supply is low.
- **T164**: `t164_estimatedConsumptionBadgeDisplayedForDropsOrInhalers` – Confirms `Est.` prefix displays for unmetered forms.
- **T165**: `t165_addMedicationDialogDisplaysRequiredFields` – Verifies Add Medication dialog displays required fields.
- **T166**: `t166_emptyInventoryMessageDisplayedWhenMedicationListEmpty` – Verifies empty inventory card displays guidance.

### Sub-Suite F.3: Medication Detail UI (`ComposeMedicationDetailRegressionTest.kt`)
- **T167**: `t167_restockDialogDisplaysQuantityAndExpiryFields` – Verifies Restock dialog displays quantity and expiry fields.
- **T168**: `t168_adjustStockDialogDisplaysCountIncreaseDecreaseOptions` – Verifies Adjust Stock dialog displays actual count input.
- **T169**: `t169_editMedicationDialogDisplaysEditableFields` – Verifies Edit Medication dialog displays editable properties.
- **T170**: `t170_requestRepeatDialogDisplaysPrescriptionReference` – Verifies repeat request dialog displays GP order guidance.
- **T171**: `t171_collectRepeatDialogDisplaysRemainingRepeats` – Verifies collection dialog displays confirmation prompt.
- **T172**: `t172_restockDialogDisplaysCurrentStockBalance` – Verifies Restock dialog shows live post-restock ledger impact.
- **T173**: `t173_adjustStockDialogDisplaysCurrentBalance` – Verifies Adjust Stock dialog displays observed count label.

### Sub-Suite F.4: Adherence Calendar UI (`ComposeCalendarRegressionTest.kt`)
- **T174**: `t174_calendarScreenDisplaysAdherenceHeader` – Verifies screen renders "Adherence & History" header.
- **T175**: `t175_calendarScreenDisplaysOverviewCards` – Verifies Today and Last 7 Days adherence metric cards.
- **T176**: `t176_calendarPreviousMonthButtonNavigatesMonth` – Verifies Previous Month button click navigates calendar.
- **T177**: `t177_calendarNextMonthButtonNavigatesMonth` – Verifies Next Month button click navigates calendar.
- **T178**: `t178_calendarScreenDisplaysOutcomeFilters` – Verifies rolling window chips (`7D`, `14D`, `30D`).

### Sub-Suite F.5: Prescription Management UI (`ComposePrescriptionRegressionTest.kt`)
- **T179**: `t179_requestRepeatDialogDisplaysPrescriptionReferenceAndRepeats` – Verifies repeat order dialog explanation.
- **T180**: `t180_requestRepeatDialogConfirmButtonTriggersCallback` – Verifies "Confirm Repeat Order" CTA button click.
- **T181**: `t181_collectRepeatDialogDisplaysPromptWithRemainingCount` – Verifies collection dialog renders Collect & Restock CTA.
- **T182**: `t182_collectRepeatDialogConfirmButtonTriggersCallback` – Verifies "Collect & Restock" CTA button click.

### Sub-Suite F.6: Settings UI (`ComposeSettingsRegressionTest.kt`)
- **T183**: `t183_settingsScreenDisplaysThemeSelectionOptions` – Verifies Theme options card (`System`, `Light`, `Dark`).
- **T184**: `t184_settingsScreenDisplaysNotificationPrivacyMaskingOption` – Verifies Privacy & App Lock section displays.
- **T185**: `t185_settingsScreenDisplaysAppLockOptions` – Verifies Biometric / Device Lock configuration section displays.
- **T186**: `t186_settingsScreenDisplaysBackupAndRestoreOptions` – Verifies Encrypted Local Backup & Restore cards display.

### Sub-Suite F.7: Accessibility (WCAG AA) (`ComposeAccessibilityRegressionTest.kt`)
- **T187**: `t187_heroDoseCardActionsMeetMinimum48dpTouchTargets` – Verifies minimum 48x48dp touch targets on CTA buttons.
- **T188**: `t188_contentDescriptionsArePresentOnAllActionIcons` – Verifies content descriptions on all interactive icons.
- **T189**: `t189_sectionHeadersDeclareSemanticsHeadingProperty` – Confirms section titles declare `semantics { heading() }`.
- **T190**: `t190_lockScreenMasksSensitiveHealthDataFromAccessibilityHierarchy` – Confirms LockScreen privacy shield masks data.

---

## Category G: End-to-End User Journeys (T191 – T200)

### Sub-Suite G.1: Cross-Screen User Journeys (`FullApplicationJourneyRegressionTest.kt`)
- **T191**: `t191_journeyDashboardDisplaysActiveScheduleSummary` – Full Journey: Launch app and render Dashboard schedule summary.
- **T192**: `t192_journeyNavigateToMedicationListShowsInventory` – Full Journey: Navigate from Dashboard to Medication List.
- **T193**: `t193_journeyOpenAddMedicationDialogAndDismiss` – Full Journey: Open Add Medication Wizard and safely dismiss.
- **T194**: `t194_journeyNavigateToCalendarDisplaysMonthlyAdherence` – Full Journey: Navigate to Adherence Calendar screen.
- **T195**: `t195_journeyNavigateToSettingsDisplaysConfiguration` – Full Journey: Navigate to Settings & Privacy screen.
- **T196**: `t196_journeySettingsThemeSelectionExecutesCleanly` – Full Journey: Toggle theme preference from Settings.
- **T197**: `t197_journeySettingsPrivacyMaskingToggleUpdatesPreferences` – Full Journey: Toggle notification privacy mask setting.
- **T198**: `t198_journeyMedicationDetailDisplaysRunwayAndHistory` – Full Journey: Inspect medication detail stock runway and history.
- **T199**: `t199_journeyStockRestockFlowIncreasesInventory` – Full Journey: Register new batch lot and restock inventory.
- **T200**: `t200_journeyDoseRecordingAndStockLedgerVerificationEndToEnd` – Full Journey: Record dose taken and verify stock ledger audit.
