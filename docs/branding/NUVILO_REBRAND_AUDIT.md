# Nuvilo Rebrand Audit (`Novelo` → `Nuvilo`)

**Official Application Name:** **Nuvilo**  
**Tagline:** *"Medication, made manageable."*  
**Package / Application ID:** `com.nuvilomed.app`  
**Audit Date:** 2026-11-09  

---

## 1. Executive Summary

An exhaustive repository-wide audit and brand correction was conducted to ensure **Nuvilo** is consistently presented as the official application name across all user-facing Android UI surfaces, strings, splash screen, lock screen, settings, backup file labels, Google Play Store screenshot campaigns, feature graphic, and project documentation.

At the same time, internal technical identifiers (Room database filename, persistent DataStore preferences, notification channel IDs, internal Kotlin class names, and historical schema/migration artifacts) were intentionally preserved to prevent breaking existing installations or requiring unnecessary database migrations.

---

## 2. User-Facing Branding Corrections Applied

| Category | File Path | Change Applied |
| :--- | :--- | :--- |
| **App Display Name** | `app/src/main/res/values/strings.xml` | Verified `<string name="app_name">Nuvilo</string>` is active and referenced by `AndroidManifest.xml` (`android:label="@string/app_name"`). |
| **Splash Screen UI** | `app/src/main/java/com/example/nuvilo/ui/screens/SplashScreen.kt` | Updated headline brand title from `"Novelo"` to `"Nuvilo"`, logo `contentDescription` to `"Nuvilo Logo"`, and screen TalkBack announcement to `"Nuvilo startup screen..."`. |
| **App Lock Screen UI** | `app/src/main/java/com/example/nuvilo/ui/screens/LockScreen.kt` | Updated lock screen title from `"Novelo"` to `"Nuvilo"`, primary CTA button from `"Unlock Novelo"` to `"Unlock Nuvilo"`, logo `contentDescription` to `"Nuvilo Logo"`, and enrollment guidance copy to `"unlock Nuvilo"`. |
| **Biometric Prompt** | `app/src/main/java/com/example/nuvilo/security/BiometricAuthManager.kt` | Updated system `BiometricPrompt.PromptInfo` title from `"Unlock Novelo"` to `"Unlock Nuvilo"`. |
| **Dashboard Top Bar** | `app/src/main/java/com/example/nuvilo/ui/screens/DashboardScreen.kt` | Updated brand logo `contentDescription` from `"Novelo"` to `"Nuvilo"`. |
| **Medication Detail / Repeat Dialog** | `app/src/main/java/com/example/nuvilo/ui/screens/MedicationDetailScreen.kt` | Updated repeat prescription request guidance text from `"Novelo will track this pending order..."` to `"Nuvilo will track this pending order..."`. |
| **Settings & Privacy UI** | `app/src/main/java/com/example/nuvilo/ui/screens/SettingsScreen.kt` | Updated app lock description (`"to open Nuvilo"`), encrypted backup card title (`"Encrypted JSON Backup (.nuvilo.json)"`), default SAF export filenames (`nuvilo_backup_$timestamp.nuvilo.json` and `nuvilo_export_$timestamp.zip`), passphrase warning (`"Nuvilo operates offline..."`), and footer version badge (`"Nuvilo 1.0"`). |
| **Notification Diagnostics** | `app/src/main/java/com/example/nuvilo/system/notification/NotificationHelper.kt` | Updated diagnostic test notification title from `"Novelo Reminder Test"` to `"Nuvilo Reminder Test"`. |
| **Automated UI & Unit Tests** | `app/src/androidTest/java/com/example/nuvilo/ui/AccessibilityTest.kt`<br>`app/src/androidTest/java/com/example/nuvilo/regression/ComposeAccessibilityRegressionTest.kt`<br>`app/src/test/java/com/example/nuvilo/regression/PrivacyAndManifestRegressionTest.kt` | Updated test assertions to verify `"Nuvilo"` and `"Unlock Nuvilo"` on `LockScreen` and manifest privacy assertions. |
| **Google Play Store Campaign** | `store-assets/google-play/nuvilo/editor/app-store-screenshots.json`<br>`store-assets/google-play/editor/app-store-screenshots.json`<br>`store-assets/google-play/nuvilo/exports/**` | Configured `appName: "Nuvilo"`, regenerated all 8 phone screenshots, 8 tablet screenshots, and `nuvilo-feature-graphic.png` (`1024 × 500`) with Nuvilo branding. |
| **Project Documentation** | `README.md`, `HANDOFF.md`, `PROGRESS.md`, `CHANGELOG.md`, `ARCHITECTURE.md`, `SECURITY.md`, `TESTING.md`, `GLOSSARY.md`, `IMPLEMENTATION_PLAN.md`, `CONTRIBUTING.md`, `SKILLS_INVENTORY.md`, `AGENTS.md`, `design-system/novelo/MASTER.md`, `docs/**` | Updated product prose and headings from `Novelo` to `Nuvilo` across 45 documentation files. |

---

## 3. Intentionally Preserved Technical Identifiers

In accordance with Section 1 of the campaign specification (*"Preserve stable technical identifiers"*), the following internal identifiers were intentionally preserved:

| Identifier / Symbol | Location | Reason for Preservation |
| :--- | :--- | :--- |
| `com.example.nuvilo` | `app/build.gradle.kts` (namespace), Kotlin package declarations | Internal code namespace for R and BuildConfig class generation. The production `applicationId` published to Google Play is set to `com.nuvilomed.app`. |
| `DATABASE_NAME = "novelo.db"` | `NoveloDatabase.kt` | Renaming the SQLite database file would orphan existing local user databases on upgrade. |
| `NoveloDatabase`, `NoveloRepository`, `NoveloApplication`, `NoveloDataBackup` | `app/src/main/java/com/example/nuvilo/**` | Internal Kotlin class and ProGuard keep-rule symbols (`-keep class com.example.nuvilo.data.local.NoveloDatabase_Impl`). Preserving them avoids unnecessary reflection/Room generated-code churn. |
| `CHANNEL_REMINDERS_ID = "novelo_medication_reminders"`<br>`CHANNEL_LOW_STOCK_ID = "novelo_low_stock_alerts"` | `NotificationHelper.kt` | Android notification channel IDs are immutable once registered with the OS; changing them creates duplicate orphaned channels in system settings. User-visible channel names (`"Medication Reminders"`, `"Low Stock Alerts"`) are already brand-neutral. |
| `WORK_NAME = "novelo_low_stock_check_periodic"` | `LowStockCheckWorker.kt` | Internal `WorkManager` unique periodic work tag; preserving prevents duplicate background jobs on app update. |
| `NoveloTheme`, `NoveloMotion`, `NoveloShapes`, `NoveloPillShape`, `NoveloComponents` | `app/src/main/java/com/example/nuvilo/ui/**` | Internal Compose theme and design-system symbol names invisible to end users. |
| `R.drawable.ic_novelo_logo`, `@color/novelo_primary` | `app/src/main/res/**` | Internal Android resource identifiers (`N` monogram 3D icon is visually the Nuvilo `N`). |
| `.agents/skills/novelo-medication-safety/` | `.agents/skills/` | Installed agent skill directory path referenced by local agent tooling. |

---

## 4. Verification Acceptance Check

- **Active Application UI**: Zero occurrences of `"Novelo"` remain in any user-visible string, Composable `Text`, dialog message, notification title, or accessibility `contentDescription`.
- **Launcher & Manifest**: `android:label="@string/app_name"` resolves to **`Nuvilo`**.
- **Google Play Store Campaign**: All 16 screenshots and the `1024 × 500` feature graphic display **`Nuvilo`**.
