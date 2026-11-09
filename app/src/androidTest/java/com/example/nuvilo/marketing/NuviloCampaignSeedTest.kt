package com.example.nuvilo.marketing

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nuvilo.MainActivity
import com.example.nuvilo.NoveloApplication
import com.example.nuvilo.data.preferences.ThemeMode
import com.example.nuvilo.domain.model.DoseStatus
import com.example.nuvilo.domain.model.Medication
import com.example.nuvilo.domain.model.MedicationForm
import com.example.nuvilo.domain.model.Prescription
import com.example.nuvilo.domain.model.PrescriptionStatus
import com.example.nuvilo.domain.model.RecordingMode
import com.example.nuvilo.domain.model.ScaledQuantity
import com.example.nuvilo.domain.model.ScheduleDefinition
import com.example.nuvilo.domain.model.ScheduleType
import com.example.nuvilo.domain.model.StockUnit
import com.example.nuvilo.domain.schedule.ScheduleOccurrenceCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Deterministic Compose UI driver that seeds the synthetic Nuvilo clinical dataset
 * and captures all 8 genuine Nuvilo screens on the active device/emulator configuration.
 */
@RunWith(AndroidJUnit4::class)
class NuviloCampaignSeedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var app: NoveloApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        app = context.applicationContext as NoveloApplication
    }

    private fun hideKeyboardAndCapture(filename: String, closeKeyboard: Boolean = false) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (closeKeyboard) {
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            Thread.sleep(500)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        uiAutomation.executeShellCommand("screencap -p /sdcard/$filename").close()
        Thread.sleep(400)
    }

    @Test
    fun seedAndCaptureAllEightScreens() {
        runBlocking {
            val db = app.container.database
            val repo = app.container.repository
            val prefs = app.container.userPreferencesRepository
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val nowMs = System.currentTimeMillis()

            db.clearAllTables()

            prefs.setThemeMode(ThemeMode.LIGHT)
            prefs.setNotificationPrivacy(true)
            prefs.setBiometricLock(false)
            prefs.setRecentsPreviewMasked(true)
            prefs.setLastExportEpochMs(nowMs - 2 * 3600_000L)

            val startEpochDay = today.minusDays(18).toEpochDay()

            // 1. Amoxicillin 500mg Capsule — 3x Daily (08:00, 14:00, 20:00)
            val medAmox = Medication(
                id = "med_amox_500",
                name = "Amoxicillin",
                strength = "500mg",
                form = MedicationForm.CAPSULE,
                stockUnit = StockUnit.CAPSULES,
                openingStock = ScaledQuantity.ZERO,
                lowStockThreshold = ScaledQuantity.ofWhole(10),
                recordingMode = RecordingMode.CONFIRM,
                notes = "Take with water after meals. Complete full course.",
                colorTagHex = "#005EB8",
                createdAtEpochMs = today.minusDays(25).atStartOfDay(zone).toInstant().toEpochMilli()
            )
            val schAmox = ScheduleDefinition(
                id = "sch_amox_500",
                medicationId = medAmox.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60, 14 * 60, 20 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = today.minusDays(10).toEpochDay(),
                instructions = "Take 1 capsule three times daily after food"
            )
            repo.createMedication(medAmox, schAmox)

            val expiryLot1 = today.plusDays(42).atStartOfDay(zone).toInstant().toEpochMilli()
            val expiryLot2 = today.plusDays(165).atStartOfDay(zone).toInstant().toEpochMilli()
            repo.restockWithLot(
                medicationId = medAmox.id,
                quantity = ScaledQuantity.ofWhole(28),
                unit = StockUnit.CAPSULES,
                lotNumber = "NHS-BATCH-409A",
                expiryDateEpochMs = expiryLot1,
                notes = "Dispensed by St Mary's Pharmacy (Primary FEFO Lot)"
            )
            repo.restockWithLot(
                medicationId = medAmox.id,
                quantity = ScaledQuantity.ofWhole(28),
                unit = StockUnit.CAPSULES,
                lotNumber = "NHS-BATCH-512B",
                expiryDateEpochMs = expiryLot2,
                notes = "Repeat collection reserve pack"
            )

            val rxAmox = Prescription(
                id = "rx_amox_2026",
                medicationId = medAmox.id,
                referenceNumber = "NHS-RX-94820-B",
                issueDateEpochDay = today.minusDays(24).toEpochDay(),
                expiryDateEpochDay = today.plusDays(95).toEpochDay(),
                quantityPrescribed = ScaledQuantity.ofWhole(28),
                unit = StockUnit.CAPSULES,
                repeatsAuthorised = 6,
                repeatsRemaining = 5,
                hasPendingRepeatRequest = false,
                pharmacyName = "St Mary's High Street Pharmacy",
                notes = "Authorised by Dr S. Patel · Electronic Repeat Dispensing",
                status = PrescriptionStatus.ACTIVE,
                createdAtEpochMs = nowMs - 24L * 86400_000L,
                updatedAtEpochMs = nowMs - 5L * 86400_000L
            )
            repo.createPrescription(rxAmox, "Initial eRD prescription registered (6 repeats authorised)")
            repo.requestPrescriptionRepeat(
                prescriptionId = rxAmox.id,
                currentDateEpochDay = today.minusDays(7).toEpochDay(),
                actionIdempotencyKey = "req_amox_1",
                notes = "Ordered via NHS App online repeat service"
            )
            repo.collectPrescriptionRepeat(
                prescriptionId = rxAmox.id,
                currentDateEpochDay = today.minusDays(5).toEpochDay(),
                actionIdempotencyKey = "col_amox_1",
                notes = "Collected 28 capsules (Batch NHS-BATCH-512B)"
            )

            // 2. Atorvastatin 20mg Tablet — Daily at 21:00 (AUTO_ASSUME mode)
            val medAtorva = Medication(
                id = "med_atorva_20",
                name = "Atorvastatin",
                strength = "20mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ZERO,
                lowStockThreshold = ScaledQuantity.ofWhole(7),
                recordingMode = RecordingMode.AUTO_ASSUME,
                notes = "Evening cholesterol maintenance tablet.",
                colorTagHex = "#0284C7",
                createdAtEpochMs = today.minusDays(25).atStartOfDay(zone).toInstant().toEpochMilli()
            )
            val schAtorva = ScheduleDefinition(
                id = "sch_atorva_20",
                medicationId = medAtorva.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(21 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = startEpochDay,
                instructions = "Take 1 tablet every evening"
            )
            repo.createMedication(medAtorva, schAtorva)
            repo.restockWithLot(
                medicationId = medAtorva.id,
                quantity = ScaledQuantity.ofWhole(56),
                unit = StockUnit.TABLETS,
                lotNumber = "ATV-2026-09",
                expiryDateEpochMs = today.plusDays(210).atStartOfDay(zone).toInstant().toEpochMilli(),
                notes = "56-tablet NHS repeat pack"
            )

            // 3. Lisinopril 10mg Tablet — Daily at 08:00 (Low stock warning: 6 tablets left)
            val medLisinopril = Medication(
                id = "med_lisinopril_10",
                name = "Lisinopril",
                strength = "10mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ZERO,
                lowStockThreshold = ScaledQuantity.ofWhole(7),
                recordingMode = RecordingMode.CONFIRM,
                notes = "Morning blood pressure control.",
                colorTagHex = "#007F7B",
                createdAtEpochMs = today.minusDays(25).atStartOfDay(zone).toInstant().toEpochMilli()
            )
            val schLisinopril = ScheduleDefinition(
                id = "sch_lisinopril_10",
                medicationId = medLisinopril.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = startEpochDay,
                instructions = "Take 1 tablet every morning"
            )
            repo.createMedication(medLisinopril, schLisinopril)
            repo.restockWithLot(
                medicationId = medLisinopril.id,
                quantity = ScaledQuantity.ofWhole(24),
                unit = StockUnit.TABLETS,
                lotNumber = "LIS-8841-UK",
                expiryDateEpochMs = today.plusDays(120).atStartOfDay(zone).toInstant().toEpochMilli(),
                notes = "Morning blood pressure pack"
            )

            // 4. Salbutamol 100mcg Inhaler — Mon, Wed, Fri at 08:00 (Estimated Puffs)
            val medSalbutamol = Medication(
                id = "med_salbutamol_100",
                name = "Salbutamol",
                strength = "100mcg",
                form = MedicationForm.INHALER_PUFFS,
                stockUnit = StockUnit.PUFFS,
                openingStock = ScaledQuantity.ofWhole(180),
                lowStockThreshold = ScaledQuantity.ofWhole(30),
                recordingMode = RecordingMode.CONFIRM,
                notes = "2 puffs as scheduled; tracked as estimated actuation balance.",
                colorTagHex = "#0891B2",
                createdAtEpochMs = today.minusDays(25).atStartOfDay(zone).toInstant().toEpochMilli()
            )
            val schSalbutamol = ScheduleDefinition(
                id = "sch_salbutamol_100",
                medicationId = medSalbutamol.id,
                scheduleType = ScheduleType.SPECIFIC_WEEKDAYS,
                targetTimesMinuteOfDay = listOf(8 * 60),
                daysOfWeek = setOf(1, 3, 5),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(2),
                startDateEpochDay = startEpochDay,
                instructions = "2 puffs Mon, Wed, Fri morning"
            )
            repo.createMedication(medSalbutamol, schSalbutamol)

            // 5. Metformin 500mg Tablet — Daily at 08:00 and 19:00
            val medMetformin = Medication(
                id = "med_metformin_500",
                name = "Metformin",
                strength = "500mg",
                form = MedicationForm.TABLET,
                stockUnit = StockUnit.TABLETS,
                openingStock = ScaledQuantity.ZERO,
                lowStockThreshold = ScaledQuantity.ofWhole(14),
                recordingMode = RecordingMode.CONFIRM,
                notes = "Take with breakfast and evening meal.",
                colorTagHex = "#4F46E5",
                createdAtEpochMs = today.minusDays(25).atStartOfDay(zone).toInstant().toEpochMilli()
            )
            val schMetformin = ScheduleDefinition(
                id = "sch_metformin_500",
                medicationId = medMetformin.id,
                scheduleType = ScheduleType.DAILY,
                targetTimesMinuteOfDay = listOf(8 * 60, 19 * 60),
                daysOfWeek = emptySet(),
                doseQuantityPerOccurrence = ScaledQuantity.ofWhole(1),
                startDateEpochDay = startEpochDay,
                instructions = "Take with morning and evening meal"
            )
            repo.createMedication(medMetformin, schMetformin)
            repo.restockWithLot(
                medicationId = medMetformin.id,
                quantity = ScaledQuantity.ofWhole(84),
                unit = StockUnit.TABLETS,
                lotNumber = "MET-500-A2",
                expiryDateEpochMs = today.plusDays(300).atStartOfDay(zone).toInstant().toEpochMilli(),
                notes = "84-tablet quarterly supply"
            )

            // Record historical doses over the past 18 days
            for (daysAgo in 18 downTo 1) {
                val d = today.minusDays(daysAgo.toLong())
                val amox08 = d.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
                val amox14 = d.atTime(LocalTime.of(14, 0)).atZone(zone).toInstant().toEpochMilli()
                val amox20 = d.atTime(LocalTime.of(20, 0)).atZone(zone).toInstant().toEpochMilli()

                if (daysAgo <= 10) {
                    repo.recordDose(
                        occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medAmox.id, amox08),
                        medicationId = medAmox.id,
                        scheduledEpochMs = amox08,
                        quantity = ScaledQuantity.ofWhole(1),
                        unit = StockUnit.CAPSULES,
                        status = DoseStatus.TAKEN,
                        recordedEpochMs = amox08 + 7 * 60_000L,
                        notes = if (daysAgo == 1) "Taken with breakfast" else null
                    )
                    val status14 = if (daysAgo == 3) DoseStatus.SKIPPED else DoseStatus.TAKEN
                    repo.recordDose(
                        occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medAmox.id, amox14),
                        medicationId = medAmox.id,
                        scheduledEpochMs = amox14,
                        quantity = ScaledQuantity.ofWhole(1),
                        unit = StockUnit.CAPSULES,
                        status = status14,
                        recordedEpochMs = amox14 + 12 * 60_000L,
                        notes = if (daysAgo == 3) "Away from home at lunch" else null
                    )
                    repo.recordDose(
                        occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medAmox.id, amox20),
                        medicationId = medAmox.id,
                        scheduledEpochMs = amox20,
                        quantity = ScaledQuantity.ofWhole(1),
                        unit = StockUnit.CAPSULES,
                        status = DoseStatus.TAKEN,
                        recordedEpochMs = amox20 + 5 * 60_000L
                    )
                }

                val ator21 = d.atTime(LocalTime.of(21, 0)).atZone(zone).toInstant().toEpochMilli()
                repo.recordDose(
                    occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medAtorva.id, ator21),
                    medicationId = medAtorva.id,
                    scheduledEpochMs = ator21,
                    quantity = ScaledQuantity.ofWhole(1),
                    unit = StockUnit.TABLETS,
                    status = DoseStatus.ASSUMED,
                    recordedEpochMs = ator21 + 60_000L
                )

                val lis08 = d.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
                repo.recordDose(
                    occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medLisinopril.id, lis08),
                    medicationId = medLisinopril.id,
                    scheduledEpochMs = lis08,
                    quantity = ScaledQuantity.ofWhole(1),
                    unit = StockUnit.TABLETS,
                    status = DoseStatus.TAKEN,
                    recordedEpochMs = lis08 + 4 * 60_000L
                )

                val met08 = d.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
                val met19 = d.atTime(LocalTime.of(19, 0)).atZone(zone).toInstant().toEpochMilli()
                repo.recordDose(
                    occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medMetformin.id, met08),
                    medicationId = medMetformin.id,
                    scheduledEpochMs = met08,
                    quantity = ScaledQuantity.ofWhole(1),
                    unit = StockUnit.TABLETS,
                    status = DoseStatus.TAKEN,
                    recordedEpochMs = met08 + 9 * 60_000L
                )
                if (daysAgo != 6) {
                    repo.recordDose(
                        occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medMetformin.id, met19),
                        medicationId = medMetformin.id,
                        scheduledEpochMs = met19,
                        quantity = ScaledQuantity.ofWhole(1),
                        unit = StockUnit.TABLETS,
                        status = DoseStatus.TAKEN,
                        recordedEpochMs = met19 + 15 * 60_000L
                    )
                }
            }

            // Record TODAY's 08:00 and 14:00 doses as TAKEN so Dashboard shows 5/8 taken (62%)
            // and highlights the upcoming 19:00 Metformin or 20:00 Amoxicillin dose!
            val today08 = today.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
            for ((med, qty) in listOf(
                medAmox to ScaledQuantity.ofWhole(1),
                medLisinopril to ScaledQuantity.ofWhole(1),
                medMetformin to ScaledQuantity.ofWhole(1),
                medSalbutamol to ScaledQuantity.ofWhole(2)
            )) {
                repo.recordDose(
                    occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(med.id, today08),
                    medicationId = med.id,
                    scheduledEpochMs = today08,
                    quantity = qty,
                    unit = med.stockUnit,
                    status = DoseStatus.TAKEN,
                    recordedEpochMs = today08 + 6 * 60_000L,
                    notes = "Confirmed morning dose"
                )
            }
            val today14 = today.atTime(LocalTime.of(14, 0)).atZone(zone).toInstant().toEpochMilli()
            repo.recordDose(
                occurrenceId = ScheduleOccurrenceCalculator.generateOccurrenceId(medAmox.id, today14),
                medicationId = medAmox.id,
                scheduledEpochMs = today14,
                quantity = ScaledQuantity.ofWhole(1),
                unit = StockUnit.CAPSULES,
                status = DoseStatus.TAKEN,
                recordedEpochMs = today14 + 4 * 60_000L,
                notes = "Afternoon dose taken after lunch"
            )
            // Add an audit revision on yesterday's 14:00 dose so Dose History shows an Audit Revision badge too!
            val yest14 = today.minusDays(1).atTime(LocalTime.of(14, 0)).atZone(zone).toInstant().toEpochMilli()
            val yest14Id = ScheduleOccurrenceCalculator.generateOccurrenceId(medAmox.id, yest14)
            repo.correctDose(
                occurrenceId = yest14Id,
                targetStatus = DoseStatus.TAKEN,
                reason = "Verified taken at 14:15 (logged retroactively)"
            )
        }

        // Wait for Splash screen to complete (or tap to skip splash)
        Thread.sleep(1200)
        runCatching {
            composeTestRule.onNodeWithText("Nuvilo").performClick()
        }
        composeTestRule.waitForIdle()
        Thread.sleep(800)

        // 1. Capture 01: dashboard.png
        hideKeyboardAndCapture("dashboard.png")

        // 2. Navigate to Medications tab -> Capture 02: medications.png
        composeTestRule.onNodeWithText("Medications").performClick()
        composeTestRule.waitForIdle()
        hideKeyboardAndCapture("medications.png")

        // 3. Open Add Medication 3-Step Wizard -> Step 3 (Stock & Regimen Summary) -> Capture 03: setup.png
        composeTestRule.onNodeWithText("Add Medication", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Medication Name *").performTextInput("Ramipril")
        composeTestRule.onNodeWithText("Strength / Concentration *").performTextInput("5mg")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        Thread.sleep(400)
        composeTestRule.onNodeWithText("3. Stock", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        hideKeyboardAndCapture("setup.png", closeKeyboard = false)

        // Dismiss Add Medication dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // 4. Open Amoxicillin Medication Detail -> Capture 07: prescriptions.png & 04: stock.png & 06: history.png
        composeTestRule.onAllNodesWithText("Amoxicillin", substring = true)[0].performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(600)

        // Scroll to Prescription & Repeat Tracking section for 07: prescriptions.png
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Prescription & Repeat Tracking", substring = true))
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("2. Collect Repeat", substring = true))
        composeTestRule.waitForIdle()
        hideKeyboardAndCapture("prescriptions.png")

        // Expand Batch Lots & FEFO Allocation and Append-Only Stock Ledger for 04: stock.png
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Batch Lots & FEFO Allocation", substring = true))
        composeTestRule.onNodeWithText("Batch Lots & FEFO Allocation", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Append-Only Stock Ledger", substring = true))
        composeTestRule.onNodeWithText("Append-Only Stock Ledger", substring = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(400)
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Dispensed by St Mary's Pharmacy", substring = true))
        composeTestRule.waitForIdle()
        hideKeyboardAndCapture("stock.png")

        // 6. Navigate to Full Dose History screen -> Capture 06: history.png
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Recent Dose Records", substring = true))
        composeTestRule.onNodeWithText("Full History", substring = true).performClick()
        composeTestRule.waitForIdle()
        hideKeyboardAndCapture("history.png")

        // Navigate back from Dose History -> Medication Detail -> Medications List
        composeTestRule.onNodeWithContentDescription("Back to medication details").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // 5. Navigate to Adherence Calendar tab -> Capture 05: calendar.png
        composeTestRule.onNodeWithText("Adherence").performClick()
        composeTestRule.waitForIdle()
        hideKeyboardAndCapture("calendar.png")

        // 8. Navigate to Settings tab -> Scroll to Privacy & App Lock + Encrypted Backup -> Capture 08: privacy.png
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Export Encrypted Backup", substring = true))
        composeTestRule.waitForIdle()
        hideKeyboardAndCapture("privacy.png")
    }
}
