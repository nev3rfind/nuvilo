# Nuvilo Brand Correction Audit (`BRAND_AUDIT.md`)

**Official Application Name:** **Nuvilo**  
**Tagline:** *"Medication, made manageable."*  
**Package:** `com.nuvilomed.app`  
**Audit Date:** 2026-11-09  

---

## 1. Summary of Brand Corrections

All user-facing references to the legacy name `Novelo` have been systematically replaced with **`Nuvilo`** across the Android application UI, accessibility labels, biometric prompts, backup file labels, store marketing assets, screenshot editor project (`appName: "Nuvilo"`), and project documentation.

### Modified Application & Test Files
1. `app/src/main/res/values/strings.xml` — Verified `app_name` is `Nuvilo`.
2. `app/src/main/java/com/example/nuvilo/ui/screens/SplashScreen.kt` — Updated splash screen brand title (`"Nuvilo"`), logo content description (`"Nuvilo Logo"`), and startup TalkBack label.
3. `app/src/main/java/com/example/nuvilo/ui/screens/LockScreen.kt` — Updated lock screen heading (`"Nuvilo"`), unlock CTA (`"Unlock Nuvilo"`), logo content description (`"Nuvilo Logo"`), and security guidance copy.
4. `app/src/main/java/com/example/nuvilo/security/BiometricAuthManager.kt` — Updated biometric prompt title to `"Unlock Nuvilo"`.
5. `app/src/main/java/com/example/nuvilo/ui/screens/DashboardScreen.kt` — Updated top-bar brand icon content description to `"Nuvilo"`.
6. `app/src/main/java/com/example/nuvilo/ui/screens/MedicationDetailScreen.kt` — Updated GP repeat order dialog copy to `"Nuvilo will track this pending order..."`.
7. `app/src/main/java/com/example/nuvilo/ui/screens/SettingsScreen.kt` — Updated app lock subtitle (`"to open Nuvilo."`), encrypted backup title (`"Encrypted JSON Backup (.nuvilo.json)"`), SAF default export filenames (`nuvilo_backup_<timestamp>.nuvilo.json`, `nuvilo_export_<timestamp>.zip`), offline passphrase notice, and footer version label (`"Nuvilo 1.0"`).
8. `app/src/main/java/com/example/nuvilo/system/notification/NotificationHelper.kt` — Updated test notification title to `"Nuvilo Reminder Test"`.
9. `app/src/androidTest/java/com/example/nuvilo/ui/AccessibilityTest.kt` — Updated lock screen UI assertions to `"Nuvilo"` and `"Unlock Nuvilo"`.
10. `app/src/androidTest/java/com/example/nuvilo/regression/ComposeAccessibilityRegressionTest.kt` — Updated lock screen assertion to `"Unlock Nuvilo"`.
11. `app/src/test/java/com/example/nuvilo/regression/PrivacyAndManifestRegressionTest.kt` — Updated failure message string to `"Nuvilo"`.

### Modified Documentation & Store Asset Files
- Updated 45 documentation files (`README.md`, `HANDOFF.md`, `PROGRESS.md`, `CHANGELOG.md`, `ARCHITECTURE.md`, `SECURITY.md`, `TESTING.md`, `GLOSSARY.md`, `IMPLEMENTATION_PLAN.md`, `CONTRIBUTING.md`, `SKILLS_INVENTORY.md`, `AGENTS.md`, `design-system/novelo/MASTER.md`, and `docs/**`).
- Updated `store-assets/google-play/nuvilo/editor/app-store-screenshots.json` and `store-assets/google-play/editor/app-store-screenshots.json` to `appName: "Nuvilo"`.
- Generated `store-assets/google-play/nuvilo/exports/feature-graphic/nuvilo-feature-graphic.png` and all 16 phone/tablet screenshots with `Nuvilo` branding.

---

## 2. Intentionally Preserved Technical Identifiers

To guarantee zero database migration regressions, zero orphaned local SQLite files, and full backward compatibility, the following non-user-facing technical identifiers were preserved:
- **Room Database Filename**: `novelo.db` in `NoveloDatabase.kt`
- **Notification Channel IDs**: `novelo_medication_reminders` and `novelo_low_stock_alerts` in `NotificationHelper.kt`
- **WorkManager Periodic Job Tag**: `novelo_low_stock_check_periodic` in `LowStockCheckWorker.kt`
- **Internal Kotlin Classes & Theme Symbols**: `NoveloApplication`, `NoveloDatabase`, `NoveloRepository`, `NoveloDataBackup`, `NoveloTheme`, `NoveloMotion`, `NoveloShapes`, `NoveloPillShape`, `NoveloComponents`
- **Internal Resource IDs**: `R.drawable.ic_novelo_logo`, `@color/novelo_primary`, `@color/novelo_dark_surface`
- **Historical Archive**: `store-assets/google-play/previous-campaign-archive/` (preserved strictly as evidence of the superseded campaign).
