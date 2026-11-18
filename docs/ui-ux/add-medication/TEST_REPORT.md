# Test Report: Add & Edit Medication Experience Redesign (Phase 12)

**Product**: Nuvilo (*"Medication, made manageable."*)  
**Package**: `com.example.nuvilo` (`com.nuvilomed.app`)  
**GitHub Issue**: `#22` | **Branch**: `feat/22-add-medication-redesign`

> [!NOTE]
> **Environment & Device Provenance**:
> - **Baseline ("Before") screenshots** (`before/01_before_step1.png`–`03_before_step3.png`): physical device `SM-S948B` (`R3GL5059FTZ`).
> - **Redesigned ("After") screenshots** (`01_step1_light.png`–`13_tablet_layout.png`) and **Android instrumentation tests**: Android Emulator `Pixel_10_Pro(AVD) - 17` (`emulator-5554`, API 37).

---

## 1. Automated Quality Gate Summary

| Gate | Command / Runner | Result | Details |
| :--- | :--- | :--- | :--- |
| **JVM Unit Tests** | `.\gradlew.bat testDebugUnitTest --no-daemon` | **219 / 219 PASSED** | 34 unit test suites executed (`0 failures, 0 errors, 0 skipped`). |
| **Android Lint** | `.\gradlew.bat lintDebug --no-daemon` | **0 ERRORS** | 43 pre-existing/non-blocking warnings (`lint-results-debug.txt`); fixed `UnusedContentLambdaTargetStateParameter` in `AddMedicationWizardComponents.kt:942`. |
| **Debug APK Build** | `.\gradlew.bat assembleDebug --no-daemon` | **BUILD SUCCESSFUL** | `app/build/outputs/apk/debug/app-debug.apk` (`26,009,753` bytes). |
| **Release APK Build** | `.\gradlew.bat assembleRelease --no-daemon` | **BUILD SUCCESSFUL** | `app/build/outputs/apk/release/app-release-unsigned.apk` (`4,579,293` bytes), R8 minification + ProGuard rules verified. |
| **Zero-Network Audit** | Merged release manifest inspection | **VERIFIED CLEAN** | `app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml` contains **0** `android.permission.INTERNET` and **0** `ACCESS_NETWORK_STATE` permissions. |
| **Connected Suite (Full)** | `.\gradlew.bat connectedDebugAndroidTest --no-daemon` (`ANDROID_SERIAL=emulator-5554`) | **202 / 203 PASSED** | Executed on `Pixel_10_Pro(AVD) - 17` (`0 skipped`). `AddMedicationRedesignUiTest` (30/30), `Phase9UiAndAccessibilityTest` (5/5), `AccessibilityTest` (11/11), `Phase11PpcUiTest` (2/2), `NuviloCampaignSeedTest` (1/1), and all Room/migration/alarm/regression tests passed. Sole failure: `Phase11MedicationCrudUiTest` timed out / lost focus under concurrent external emulator contention (`com.certpilot.app.test` & `com.dartscorepro.test` active on `emulator-5554`). |
| **Post-Fix UI Instrumentation** | `adb -s emulator-5554 shell am instrument` | **Partial / Contended** | `Phase9UiAndAccessibilityTest` **5 / 5 PASSED** (`459.689s` under load). Follow-up isolated `am instrument` invocations for `Phase11MedicationCrudUiTest`, `Phase11PpcUiTest`, and `AccessibilityTest` experienced external `bg anr` kills / process crashes (`logcat -b crash` empty; `ps -A` confirmed concurrent `com.certpilot.app.test` and `com.dartscorepro.test` runners active on `emulator-5554`, load avg `9.07`). |

---

## 2. Phase 12 Compose UI Test Suite (`AddMedicationRedesignUiTest` — 30 Tests)

All 30 new Compose UI tests in `app/src/androidTest/java/com/example/nuvilo/phase12/AddMedicationRedesignUiTest.kt` passed in full suite execution (`task-5089`):

- **Navigation, Stepper & State Persistence (`t01`–`t06`)**: Step 1–3 transitions, Back preserving fields/eye configuration/packs, disabled-to-enabled Save transition, Close dismissal, unvisited upcoming steps remaining muted until visited.
- **Form/Route/Package Filtering & Guardrails (`t07`–`t13`)**: All 9 `MedicationForm` options reachable, contextual `PackageType` filtering per form, `FormChangeConfirmationDialog` on custom package switch + Keep Current cancellation + discard preset chips.
- **Step 2 Administration, Dates & Recording (`t14`–`t17`)**: Eye-drop `Both eyes` 2-drop consumption callout, single-dose container decoupling notice, schedule Start/End Date picker dialogs (`endDate >= startDate`), `CONFIRM` vs `AUTO_ASSUME` mode switching.
- **Validation & Accessibility (`t18`–`t25`)**: Step 1/2/3 inline `isError` borders & messages, disabled-action warning banner, `onConfirmFull` persistence payload, `48dp` minimum touch targets, `imePadding` / scrollable long forms.
- **Edit Medication & Schedule Parity (`t26`–`t30`)**: Pre-populated full-screen `EditMedicationDialog`, inline error on blank name, contextual package reset protection, `History Protection & Supply Impact` banner, and `EditScheduleDialog` date persistence.

---

## 3. Remediation Applied During Verification

1. **`AddMedicationWizardComponents.kt:938-953`**: Bound `AnimatedContent` lambda parameter `supplyText ->` to fix Compose Lint error `UnusedContentLambdaTargetStateParameter`.
2. **`NuviloCampaignSeedTest.kt:444`**: Updated legacy `"3. Stock"` step selector to `onNodeWithContentDescription("03 Stock")`.
3. **`Phase11MedicationCrudUiTest.kt:99-118`**: Replaced immediate `waitForIdle()` with `waitUntil(timeoutMillis = 5_000) { !viewModel.uiState.value.isLoading && viewModel.uiState.value.medication != null }` (matching `FullApplicationJourneyRegressionTest`) and async archive wait. *(Pending uncontested device verification due to shared-emulator contention; physical hardware verification also tracked in CI + `PHYSICAL_DEVICE_CHECKLIST.md`.)*
